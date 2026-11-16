/*
 * GestorPublicaciones.java
 *
 * Qué hace: lógica de negocio de las publicaciones: cargar y guardar el historial, publicar un
 * mensaje (con sus validaciones) y armar el muro con las publicaciones propias y de los amigos,
 * de la más reciente a la más antigua.
 * Rol en la arquitectura: capa de SERVICIO del módulo de publicaciones. La UI le habla por
 * ServicioPublicaciones; Main lo registra como ModuloPersistente.
 * Principio SOLID: SRP (coordina; la lista está en HistorialPublicaciones y el disco en el
 * repositorio), ISP (depende solo de ConsultaAmistades, no del servicio completo de amistades) y
 * DIP (todas sus dependencias son abstracciones inyectadas por constructor).
 * Colabora con: HistorialPublicaciones, RepositorioPublicaciones, DirectorioUsuarios,
 * ConsultaAmistades, RegistroAuditoria.
 */
package una.social.publicaciones;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import una.social.amistades.ConsultaAmistades;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.modelo.Usuario;
import una.social.persistencia.ModuloPersistente;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de publicaciones y del muro.
 *
 * <p><b>Hilos:</b> los métodos que tocan el historial son {@code synchronized} porque la UI los
 * llama desde SwingWorker y el cierre de la app desde otro hilo.</p>
 */
public final class GestorPublicaciones implements ServicioPublicaciones, ModuloPersistente {

    /** Formato de un ID nuevo: "P" + número con al menos 4 dígitos (P0001...). */
    private static final String FORMATO_ID = Publicacion.PREFIJO_ID + "%04d";

    /** Persistencia de las publicaciones. */
    private final RepositorioPublicaciones repositorio;

    /** Lista dinámica en memoria. */
    private final HistorialPublicaciones historial;

    /** Usuarios registrados (para validar autores y mostrar sus nombres). */
    private final DirectorioUsuarios directorio;

    /** Consulta de amistades (para saber de quién mostrar publicaciones en el muro). */
    private final ConsultaAmistades amistades;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /** Reloj para la fecha de publicación (inyectable para las pruebas). */
    private final Clock reloj;

    /** {@code true} solo si la carga salió bien; si no, no se permite guardar (ver GestorAmistades). */
    private boolean datosCargados;

    /**
     * Crea el gestor (no lee el disco hasta {@link #cargarDatos()}).
     *
     * @param repositorio persistencia
     * @param historial   historial vacío
     * @param directorio  usuarios registrados (deben cargarse ANTES que las publicaciones)
     * @param amistades   consulta de amistades
     * @param auditoria   bitácora
     * @param reloj       reloj del sistema
     */
    public GestorPublicaciones(RepositorioPublicaciones repositorio, HistorialPublicaciones historial,
                               DirectorioUsuarios directorio, ConsultaAmistades amistades,
                               RegistroAuditoria auditoria, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.historial = Objects.requireNonNull(historial, "historial");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== ModuloPersistente

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "publicaciones";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Además del formato (repositorio), aquí se revisan las reglas: el autor debe existir y el
     * ID no puede repetirse. El historial se mantiene en orden cronológico aunque el archivo
     * viniera desordenado.</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si el archivo no se puede leer: se marca la carga como
        // fallida (no se permitirá guardar), se audita y se relanza para avisar al usuario.
        try {
            List<Publicacion> leidas = repositorio.cargarTodas();
            historial.limpiar();
            for (Publicacion publicacion : leidas) {
                agregarAlHistorialSiEsValida(publicacion);
            }
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Historial de publicaciones en memoria: " + historial.tamano() + " publicaciones");
        } catch (ExcepcionPersistencia ex) {
            datosCargados = false;
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "CARGA_FALLIDA",
                    "publicaciones: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Agrega una publicación leída del archivo solo si respeta las reglas.
     *
     * @param publicacion publicación leída
     */
    private void agregarAlHistorialSiEsValida(Publicacion publicacion) {
        String problema = null;
        if (directorio.buscarPorId(publicacion.getIdAutor()).isEmpty()) {
            problema = "el autor " + publicacion.getIdAutor() + " no existe";
        } else if (historial.contieneId(publicacion.getId())) {
            problema = "ID repetido";
        }
        if (problema != null) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "publicaciones.txt: publicación " + publicacion.getId() + " omitida (" + problema + ")");
            return;
        }
        historial.agregar(publicacion);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            repositorio.guardarTodas(historial.todas());
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    historial.tamano() + " publicaciones guardadas");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    "publicaciones: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    // ================================================================== ServicioPublicaciones

    /** {@inheritDoc} */
    @Override
    public Publicacion publicar(Usuario autor, String texto) {
        Objects.requireNonNull(autor, "autor");
        String actor = autor.getNombreUsuario();
        // Se esperan ExcepcionPublicacionInvalida (texto vacío o largo, autor inexistente) y
        // ExcepcionPersistencia (no se pudo guardar). Se auditan aquí y se relanzan para que el
        // muro muestre el mensaje en rojo o un aviso.
        try {
            Publicacion creada = crearYGuardar(autor.getId(), normalizarTexto(texto));
            // Solo se audita el ID y el largo: el contenido puede ser largo y no aporta a la auditoría.
            auditoria.registrar(NivelAuditoria.INFO, actor, "PUBLICACION_CREADA",
                    creada.getId() + " (" + creada.getTexto().length() + " caracteres)");
            return creada;
        } catch (ExcepcionPublicacionInvalida ex) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "PUBLICACION_RECHAZADA", ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, actor, "PUBLICACION_FALLIDA", RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Limpia el texto y valida su largo.
     *
     * <p>Los saltos de línea de Windows ({@code \r\n}) se unifican a {@code \n} para que el mismo
     * mensaje se guarde igual en cualquier sistema. Se usa EXCEPCIÓN (no assert) porque es texto
     * escrito por el usuario.</p>
     *
     * @param texto texto escrito
     * @return texto limpio
     * @throws ExcepcionPublicacionInvalida si queda vacío o supera el máximo
     */
    private static String normalizarTexto(String texto) {
        String limpio = texto == null ? "" : texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (limpio.isEmpty()) {
            throw new ExcepcionPublicacionInvalida("Escriba algo antes de publicar.");
        }
        if (limpio.length() > Publicacion.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionPublicacionInvalida("La publicación puede tener como máximo "
                    + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres (tiene " + limpio.length() + ").");
        }
        return limpio;
    }

    /**
     * Crea la publicación, la guarda primero en disco y luego en memoria (si el disco falla, la
     * memoria no cambió y sigue igual al archivo).
     *
     * @param idAutor ID del autor
     * @param texto   texto ya validado
     * @return publicación creada
     */
    private synchronized Publicacion crearYGuardar(String idAutor, String texto) {
        exigirDatosCargados();
        if (directorio.buscarPorId(idAutor).isEmpty()) {
            throw new ExcepcionPublicacionInvalida("El autor no está registrado.");
        }
        Publicacion nueva = new Publicacion(siguienteId(), idAutor, Instant.now(reloj), texto);
        List<Publicacion> conNueva = new ArrayList<>(historial.todas());
        conNueva.add(nueva);
        repositorio.guardarTodas(conNueva);
        historial.agregar(nueva);
        return nueva;
    }

    /**
     * Calcula el siguiente ID: el número más alto existente + 1 (así nunca se repite, aunque haya
     * huecos por líneas omitidas).
     *
     * @return nuevo ID (por ejemplo "P0007")
     */
    private String siguienteId() {
        // Invariante: se llama con el candado tomado, para que dos publicaciones simultáneas no
        // reciban el mismo número.
        assert Thread.holdsLock(this) : "siguienteId debe llamarse con el candado del gestor";
        int maximo = 0;
        for (Publicacion publicacion : historial.todas()) {
            Matcher m = Publicacion.PATRON_ID.matcher(publicacion.getId());
            // Invariante: todo ID del historial fue validado con PATRON_ID al leerlo o al crearlo.
            assert m.matches() : "ID inválido en el historial: " + publicacion.getId();
            maximo = Math.max(maximo, Integer.parseInt(publicacion.getId().substring(Publicacion.PREFIJO_ID.length())));
        }
        return String.format(FORMATO_ID, maximo + 1);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<EntradaMuro> obtenerMuro(Usuario usuario) {
        // Autores visibles = el propio usuario + sus amigos (vecinos en el grafo de amistades).
        Set<String> autoresVisibles = new HashSet<>(amistades.idsAmigos(usuario.getId()));
        autoresVisibles.add(usuario.getId());
        List<EntradaMuro> muro = new ArrayList<>();
        for (Publicacion publicacion : historial.muro(autoresVisibles)) {
            Optional<Usuario> autor = directorio.buscarPorId(publicacion.getIdAutor());
            // Invariante: al cargar y al publicar solo entran publicaciones de autores existentes.
            assert autor.isPresent() : "Publicación con autor inexistente: " + publicacion.getId();
            autor.ifPresent(a -> muro.add(new EntradaMuro(publicacion, a)));
        }
        return Collections.unmodifiableList(muro);
    }

    // ================================================================== utilidades

    /**
     * Impide guardar si la carga inicial falló.
     *
     * @throws ExcepcionPersistencia si los datos no se cargaron correctamente
     */
    private void exigirDatosCargados() {
        if (!datosCargados) {
            throw new ExcepcionPersistencia("Las publicaciones no se cargaron correctamente; no se guardará nada "
                    + "para no sobrescribir el archivo. Reinicie la aplicación.");
        }
    }
}
