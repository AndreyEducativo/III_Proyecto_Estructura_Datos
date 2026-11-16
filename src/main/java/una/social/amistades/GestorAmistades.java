/*
 * GestorAmistades.java
 *
 * Qué hace: lógica de negocio de las amistades: cargar y guardar el grafo, "Agregar como amigo"
 * (con sus reglas) y listar los amigos de un usuario.
 * Rol en la arquitectura: capa de SERVICIO del módulo de amistades. La UI le habla por
 * ServicioAmistades; el módulo de publicaciones por ConsultaAmistades; Main lo registra como
 * ModuloPersistente para que se cargue al iniciar y se guarde al cerrar.
 * Principio SOLID: SRP (coordina el caso de uso; el grafo está en GrafoAmistades y el disco en el
 * repositorio), ISP (implementa interfaces pequeñas), DIP (todas sus dependencias son
 * abstracciones inyectadas por constructor).
 * Colabora con: GrafoAmistades, RepositorioAmistades, DirectorioUsuarios, RegistroAuditoria.
 */
package una.social.amistades;

import java.text.Collator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionAmistadInvalida;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.modelo.Usuario;
import una.social.persistencia.ModuloPersistente;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de amistades.
 *
 * <p><b>Hilos:</b> la UI lo llama desde SwingWorker y el cierre de la app desde otro hilo, por eso
 * los métodos que tocan el grafo son {@code synchronized}.</p>
 */
public final class GestorAmistades implements ServicioAmistades, ModuloPersistente {

    /** Idioma para ordenar nombres alfabéticamente respetando tildes y ñ. */
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    /** Persistencia de las amistades. */
    private final RepositorioAmistades repositorio;

    /** Grafo en memoria (listas de adyacencia). */
    private final GrafoAmistades grafo;

    /** Directorio de usuarios (solo lectura) para validar que los IDs existan. */
    private final DirectorioUsuarios directorio;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /** Reloj para la fecha de cada amistad (inyectable para las pruebas). */
    private final Clock reloj;

    /**
     * {@code true} solo si la carga desde el disco salió bien. Mientras sea {@code false} no se
     * permite guardar, para no sobrescribir amistades.txt con un grafo vacío por error.
     */
    private boolean datosCargados;

    /**
     * Crea el gestor (no lee el disco hasta {@link #cargarDatos()}).
     *
     * @param repositorio persistencia
     * @param grafo       grafo vacío
     * @param directorio  usuarios registrados (deben cargarse ANTES que las amistades)
     * @param auditoria   bitácora
     * @param reloj       reloj del sistema
     */
    public GestorAmistades(RepositorioAmistades repositorio, GrafoAmistades grafo, DirectorioUsuarios directorio,
                           RegistroAuditoria auditoria, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.grafo = Objects.requireNonNull(grafo, "grafo");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== ModuloPersistente

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "amistades";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Además del formato (que revisa el repositorio), aquí se revisan las REGLAS: que ambos
     * usuarios existan, que no sea un usuario consigo mismo y que el par no esté repetido. Las
     * líneas que no cumplen se omiten y se auditan.</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si el archivo no se puede leer: la carga queda marcada
        // como fallida (no se permitirá guardar), se audita y se relanza para avisar al usuario.
        try {
            List<Amistad> leidas = repositorio.cargarTodas();
            grafo.limpiar();
            for (Amistad amistad : leidas) {
                agregarAlGrafoSiEsValida(amistad);
            }
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Grafo de amistades en memoria: " + grafo.cantidadAmistades() + " amistades");
        } catch (ExcepcionPersistencia ex) {
            datosCargados = false;
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "CARGA_FALLIDA",
                    "amistades: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Agrega una amistad leída del archivo solo si respeta las reglas.
     *
     * @param amistad amistad leída
     */
    private void agregarAlGrafoSiEsValida(Amistad amistad) {
        String problema = problemaDeLaAmistad(amistad.idUsuarioA(), amistad.idUsuarioB());
        if (problema != null) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "amistades.txt: amistad " + amistad.idUsuarioA() + "-" + amistad.idUsuarioB() + " omitida (" + problema + ")");
            return;
        }
        grafo.agregar(amistad);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            repositorio.guardarTodas(grafo.todasLasAmistades());
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    grafo.cantidadAmistades() + " amistades guardadas");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    "amistades: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    // ================================================================== ServicioAmistades

    /** {@inheritDoc} */
    @Override
    public Amistad agregarAmistad(Usuario solicitante, String idNuevoAmigo) {
        Objects.requireNonNull(solicitante, "solicitante");
        String actor = solicitante.getNombreUsuario();
        // Se esperan ExcepcionAmistadInvalida (regla no cumplida) y ExcepcionPersistencia (no se
        // pudo guardar). Se auditan aquí, en el límite del servicio, y se relanzan para que la
        // ventana muestre un aviso amigable.
        try {
            Amistad creada = crearYGuardar(solicitante.getId(), idNuevoAmigo == null ? "" : idNuevoAmigo.trim());
            auditoria.registrar(NivelAuditoria.INFO, actor, "AMISTAD_AGREGADA",
                    creada.idUsuarioA() + " agregó a " + creada.idUsuarioB());
            return creada;
        } catch (ExcepcionAmistadInvalida ex) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "AMISTAD_RECHAZADA", ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, actor, "AMISTAD_FALLIDA", RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Valida, guarda primero en disco y luego agrega al grafo.
     *
     * <p><b>Orden: disco y después memoria.</b> Si el guardado falla, el grafo no cambió y sigue
     * igual al archivo; no hay nada que deshacer.</p>
     *
     * @param idSolicitante ID del usuario activo
     * @param idNuevoAmigo  ID del usuario a agregar
     * @return amistad creada
     */
    private synchronized Amistad crearYGuardar(String idSolicitante, String idNuevoAmigo) {
        exigirDatosCargados();
        String problema = problemaDeLaAmistad(idSolicitante, idNuevoAmigo);
        if (problema != null) {
            throw new ExcepcionAmistadInvalida(problema);
        }
        Amistad nueva = new Amistad(idSolicitante, idNuevoAmigo, Instant.now(reloj));
        List<Amistad> conNueva = new ArrayList<>(grafo.todasLasAmistades());
        conNueva.add(nueva);
        repositorio.guardarTodas(conNueva);
        grafo.agregar(nueva);
        return nueva;
    }

    /**
     * Revisa las reglas de una amistad. Se usa tanto al agregar como al cargar el archivo, así
     * las reglas están en un solo lugar.
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return descripción del problema, o {@code null} si la amistad es válida
     */
    private String problemaDeLaAmistad(String idA, String idB) {
        if (idA.equals(idB)) {
            return "No puede agregarse a sí mismo como amigo.";
        }
        if (directorio.buscarPorId(idA).isEmpty() || directorio.buscarPorId(idB).isEmpty()) {
            return "El usuario indicado no existe.";
        }
        if (grafo.sonAmigos(idA, idB)) {
            return "Ya son amigos.";
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Amigo> listarAmigos(String idUsuario) {
        List<Amigo> amigos = new ArrayList<>();
        for (String idAmigo : grafo.amigosDe(idUsuario)) {
            Optional<Usuario> usuario = directorio.buscarPorId(idAmigo);
            Optional<Amistad> amistad = grafo.buscarAmistad(idUsuario, idAmigo);
            // Invariante: al cargar y al agregar solo entran amistades de usuarios existentes, y
            // cada vecino del grafo tiene su arista registrada.
            assert usuario.isPresent() && amistad.isPresent() : "Amigo sin usuario o sin arista: " + idAmigo;
            if (usuario.isPresent() && amistad.isPresent()) {
                amigos.add(new Amigo(usuario.get(), amistad.get().fecha()));
            }
        }
        // Collator ordena como un diccionario en español ("Ángela" junto a "Ana", no al final).
        Collator alfabetico = Collator.getInstance(ESPANOL);
        amigos.sort(Comparator.comparing((Amigo a) -> a.usuario().getNombre(), alfabetico));
        return Collections.unmodifiableList(amigos);
    }

    // ================================================================== ConsultaAmistades

    /** {@inheritDoc} */
    @Override
    public synchronized Set<String> idsAmigos(String idUsuario) {
        return grafo.amigosDe(idUsuario);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean sonAmigos(String idA, String idB) {
        return grafo.sonAmigos(idA, idB);
    }

    // ================================================================== utilidades

    /**
     * Impide guardar si la carga inicial falló.
     *
     * @throws ExcepcionPersistencia si los datos no se cargaron correctamente
     */
    private void exigirDatosCargados() {
        if (!datosCargados) {
            throw new ExcepcionPersistencia("Las amistades no se cargaron correctamente; no se guardará nada "
                    + "para no sobrescribir el archivo. Reinicie la aplicación.");
        }
    }
}
