/*
 * GestorAmistades.java
 *
 * Qué hace: lógica de negocio de las amistades: cargar y guardar el grafo y las solicitudes,
 * enviar / aceptar / rechazar / cancelar solicitudes de amistad, eliminar amigos y listar amigos
 * y solicitudes.
 * Rol en la arquitectura: capa de SERVICIO del módulo de amistades. La UI le habla por
 * ServicioAmistades; el módulo de publicaciones por ConsultaAmistades; el buscador por
 * ConsultaSolicitudes; Main lo registra como ModuloPersistente para que se cargue al iniciar y se
 * guarde al cerrar.
 * Principio SOLID: SRP (coordina los casos de uso; el grafo está en GrafoAmistades y el disco en
 * los repositorios), ISP (implementa interfaces pequeñas), DIP (todas sus dependencias son
 * abstracciones inyectadas por constructor).
 * Colabora con: GrafoAmistades, RepositorioAmistades, RepositorioSolicitudes, DirectorioUsuarios,
 * RegistroAuditoria.
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
import java.util.function.Function;
import java.util.function.Supplier;
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
 * los métodos que tocan el grafo o las solicitudes son {@code synchronized}.</p>
 *
 * <p><b>Orden de guardado: disco y después memoria.</b> Cada cambio arma primero la lista nueva,
 * la guarda en disco y solo si eso salió bien actualiza la memoria. Si el disco falla, la memoria
 * sigue igual al archivo y no hay nada que deshacer.</p>
 */
public final class GestorAmistades implements ServicioAmistades, ModuloPersistente {

    /** Idioma para ordenar nombres alfabéticamente respetando tildes y ñ. */
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    /** Acción de auditoría cuando una operación sobre solicitudes/amistades no cumple una regla. */
    private static final String OPERACION_INVALIDA = "OPERACION_AMISTAD_INVALIDA";

    /** Acción de auditoría cuando una operación sobre solicitudes/amistades no se pudo guardar. */
    private static final String OPERACION_FALLIDA = "OPERACION_AMISTAD_FALLIDA";

    /** Persistencia de las amistades. */
    private final RepositorioAmistades repositorio;

    /** Persistencia de las solicitudes pendientes. */
    private final RepositorioSolicitudes repositorioSolicitudes;

    /** Grafo en memoria (listas de adyacencia). */
    private final GrafoAmistades grafo;

    /**
     * Solicitudes pendientes en memoria, en orden de envío (lista dinámica). Son pocas, así que
     * recorrerlas es suficiente; no hace falta un índice.
     */
    private final List<SolicitudAmistad> solicitudes = new ArrayList<>();

    /** Directorio de usuarios (solo lectura) para validar que los IDs existan. */
    private final DirectorioUsuarios directorio;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /** Reloj para las fechas (inyectable para las pruebas). */
    private final Clock reloj;

    /**
     * {@code true} solo si la carga desde el disco salió bien. Mientras sea {@code false} no se
     * permite guardar, para no sobrescribir los archivos con datos vacíos por error.
     */
    private boolean datosCargados;

    /**
     * Crea el gestor (no lee el disco hasta {@link #cargarDatos()}).
     *
     * @param repositorio            persistencia de amistades
     * @param repositorioSolicitudes persistencia de solicitudes pendientes
     * @param grafo                  grafo vacío
     * @param directorio             usuarios registrados (deben cargarse ANTES que las amistades)
     * @param auditoria              bitácora
     * @param reloj                  reloj del sistema
     */
    public GestorAmistades(RepositorioAmistades repositorio, RepositorioSolicitudes repositorioSolicitudes,
                           GrafoAmistades grafo, DirectorioUsuarios directorio, RegistroAuditoria auditoria, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.repositorioSolicitudes = Objects.requireNonNull(repositorioSolicitudes, "repositorioSolicitudes");
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
     * <p>Además del formato (que revisan los repositorios), aquí se revisan las REGLAS: que los
     * usuarios existan, que no sea un usuario consigo mismo y que no haya repetidos. Las líneas
     * que no cumplen se omiten y se auditan. Las amistades se cargan primero porque una solicitud
     * entre dos usuarios que ya son amigos no es válida.</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si algún archivo no se puede leer: la carga queda marcada
        // como fallida (no se permitirá guardar), se audita y se relanza para avisar al usuario.
        try {
            List<Amistad> leidas = repositorio.cargarTodas();
            List<SolicitudAmistad> solicitudesLeidas = repositorioSolicitudes.cargarTodas();
            grafo.limpiar();
            solicitudes.clear();
            for (Amistad amistad : leidas) {
                agregarAlGrafoSiEsValida(amistad);
            }
            for (SolicitudAmistad solicitud : solicitudesLeidas) {
                agregarSolicitudSiEsValida(solicitud);
            }
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Grafo de amistades en memoria: " + grafo.cantidadAmistades() + " amistades y "
                            + solicitudes.size() + " solicitudes pendientes");
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

    /**
     * Agrega una solicitud leída del archivo solo si respeta las reglas.
     *
     * @param solicitud solicitud leída
     */
    private void agregarSolicitudSiEsValida(SolicitudAmistad solicitud) {
        String problema = problemaDeLaSolicitud(solicitud.idRemitente(), solicitud.idDestinatario());
        if (problema != null) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "solicitudes.txt: solicitud " + solicitud.idRemitente() + "->" + solicitud.idDestinatario()
                            + " omitida (" + problema + ")");
            return;
        }
        solicitudes.add(solicitud);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            repositorio.guardarTodas(grafo.todasLasAmistades());
            repositorioSolicitudes.guardarTodas(List.copyOf(solicitudes));
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    grafo.cantidadAmistades() + " amistades y " + solicitudes.size() + " solicitudes guardadas");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    "amistades: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    // ================================================================== ServicioAmistades: cambios

    /** {@inheritDoc} */
    @Override
    public Amistad agregarAmistad(Usuario solicitante, String idNuevoAmigo) {
        Objects.requireNonNull(solicitante, "solicitante");
        // Se esperan ExcepcionAmistadInvalida (regla no cumplida) y ExcepcionPersistencia (no se
        // pudo guardar). Se auditan aquí, en el límite del servicio, y se relanzan.
        try {
            Amistad creada = crearYGuardar(solicitante.getId(), limpiar(idNuevoAmigo));
            auditoria.registrar(NivelAuditoria.INFO, solicitante.getNombreUsuario(), "AMISTAD_AGREGADA",
                    creada.idUsuarioA() + " agregó a " + creada.idUsuarioB());
            return creada;
        } catch (ExcepcionAmistadInvalida ex) {
            auditoria.registrar(NivelAuditoria.WARN, solicitante.getNombreUsuario(), "AMISTAD_RECHAZADA", ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, solicitante.getNombreUsuario(), "AMISTAD_FALLIDA",
                    RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SolicitudAmistad enviarSolicitud(Usuario remitente, String idDestinatario) {
        Objects.requireNonNull(remitente, "remitente");
        String destino = limpiar(idDestinatario);
        return conAuditoria(remitente, "SOLICITUD_ENVIADA", () -> registrarSolicitud(remitente.getId(), destino),
                s -> s.idRemitente() + " envió una solicitud a " + s.idDestinatario());
    }

    /** {@inheritDoc} */
    @Override
    public Amistad aceptarSolicitud(Usuario destinatario, String idRemitente) {
        Objects.requireNonNull(destinatario, "destinatario");
        String origen = limpiar(idRemitente);
        return conAuditoria(destinatario, "SOLICITUD_ACEPTADA", () -> aceptar(origen, destinatario.getId()),
                a -> a.idUsuarioB() + " aceptó la solicitud de " + a.idUsuarioA());
    }

    /** {@inheritDoc} */
    @Override
    public void rechazarSolicitud(Usuario destinatario, String idRemitente) {
        Objects.requireNonNull(destinatario, "destinatario");
        String origen = limpiar(idRemitente);
        conAuditoria(destinatario, "SOLICITUD_RECHAZADA", () -> borrarSolicitud(origen, destinatario.getId()),
                s -> s.idDestinatario() + " rechazó la solicitud de " + s.idRemitente());
    }

    /** {@inheritDoc} */
    @Override
    public void cancelarSolicitud(Usuario remitente, String idDestinatario) {
        Objects.requireNonNull(remitente, "remitente");
        String destino = limpiar(idDestinatario);
        conAuditoria(remitente, "SOLICITUD_CANCELADA", () -> borrarSolicitud(remitente.getId(), destino),
                s -> s.idRemitente() + " canceló su solicitud a " + s.idDestinatario());
    }

    /** {@inheritDoc} */
    @Override
    public void eliminarAmistad(Usuario usuario, String idAmigo) {
        Objects.requireNonNull(usuario, "usuario");
        String amigo = limpiar(idAmigo);
        conAuditoria(usuario, "AMISTAD_ELIMINADA", () -> eliminar(usuario.getId(), amigo),
                a -> usuario.getId() + " eliminó la amistad con " + amigo);
    }

    /**
     * Ejecuta una operación que cambia datos y la audita: éxito (INFO), regla no cumplida (WARN)
     * o error al guardar (ERROR). Así cada caso de uso no repite el mismo try/catch.
     *
     * @param actor       usuario que hace la operación
     * @param accionExito nombre de la acción en la auditoría si sale bien
     * @param operacion   operación a ejecutar
     * @param detalle     texto del detalle de auditoría a partir del resultado
     * @param <T>         tipo del resultado
     * @return resultado de la operación
     */
    private <T> T conAuditoria(Usuario actor, String accionExito, Supplier<T> operacion, Function<T, String> detalle) {
        String nombreActor = actor.getNombreUsuario();
        // Se esperan ExcepcionAmistadInvalida (regla no cumplida: se avisa al usuario con su
        // mensaje) y ExcepcionPersistencia (no se pudo guardar). Se auditan y se relanzan para que
        // la ventana muestre un aviso amigable.
        try {
            T resultado = operacion.get();
            auditoria.registrar(NivelAuditoria.INFO, nombreActor, accionExito, detalle.apply(resultado));
            return resultado;
        } catch (ExcepcionAmistadInvalida ex) {
            auditoria.registrar(NivelAuditoria.WARN, nombreActor, OPERACION_INVALIDA, accionExito + ": " + ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, nombreActor, OPERACION_FALLIDA,
                    accionExito + ": " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Valida y guarda una amistad nueva (primero en disco, luego en el grafo).
     *
     * @param idSolicitante ID de quien agrega
     * @param idNuevoAmigo  ID del usuario agregado
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
     * Valida y guarda una solicitud nueva (primero en disco, luego en memoria).
     *
     * @param idRemitente    quien la envía
     * @param idDestinatario quien la recibe
     * @return solicitud creada
     */
    private synchronized SolicitudAmistad registrarSolicitud(String idRemitente, String idDestinatario) {
        exigirDatosCargados();
        String problema = problemaDeLaSolicitud(idRemitente, idDestinatario);
        if (problema != null) {
            throw new ExcepcionAmistadInvalida(problema);
        }
        SolicitudAmistad nueva = new SolicitudAmistad(idRemitente, idDestinatario, Instant.now(reloj));
        List<SolicitudAmistad> conNueva = new ArrayList<>(solicitudes);
        conNueva.add(nueva);
        repositorioSolicitudes.guardarTodas(conNueva);
        solicitudes.add(nueva);
        return nueva;
    }

    /**
     * Acepta una solicitud: crea la amistad (la guarda primero) y luego borra la solicitud.
     *
     * @param idRemitente    quien envió la solicitud
     * @param idDestinatario quien la acepta
     * @return amistad creada
     */
    private synchronized Amistad aceptar(String idRemitente, String idDestinatario) {
        exigirDatosCargados();
        SolicitudAmistad solicitud = buscarSolicitud(idRemitente, idDestinatario)
                .orElseThrow(() -> new ExcepcionAmistadInvalida("La solicitud ya no está pendiente."));
        // La amistad es lo importante: se guarda primero. A = quien envió la solicitud.
        Amistad creada = crearYGuardar(idRemitente, idDestinatario);
        solicitudes.remove(solicitud);
        // Se espera ExcepcionPersistencia si no se pudo reescribir solicitudes.txt. La amistad YA
        // quedó guardada, así que no se informa como error al usuario: la solicitud vieja que
        // quede en el archivo se descarta sola al cargar (ya son amigos) y se vuelve a intentar
        // guardar al cerrar sesión. Se deja constancia en la auditoría.
        try {
            repositorioSolicitudes.guardarTodas(List.copyOf(solicitudes));
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, OPERACION_FALLIDA,
                    "No se pudo borrar la solicitud aceptada de solicitudes.txt: " + RegistroAuditoria.describirError(ex));
        }
        return creada;
    }

    /**
     * Borra una solicitud pendiente (rechazo o cancelación): primero en disco, luego en memoria.
     *
     * @param idRemitente    quien la envió
     * @param idDestinatario quien la recibió
     * @return solicitud borrada
     */
    private synchronized SolicitudAmistad borrarSolicitud(String idRemitente, String idDestinatario) {
        exigirDatosCargados();
        SolicitudAmistad solicitud = buscarSolicitud(idRemitente, idDestinatario)
                .orElseThrow(() -> new ExcepcionAmistadInvalida("La solicitud ya no está pendiente."));
        List<SolicitudAmistad> sinElla = new ArrayList<>(solicitudes);
        sinElla.remove(solicitud);
        repositorioSolicitudes.guardarTodas(sinElla);
        solicitudes.remove(solicitud);
        return solicitud;
    }

    /**
     * Elimina una amistad: primero en disco, luego en el grafo.
     *
     * @param idUsuario usuario activo
     * @param idAmigo   amigo a eliminar
     * @return amistad eliminada
     */
    private synchronized Amistad eliminar(String idUsuario, String idAmigo) {
        exigirDatosCargados();
        Amistad amistad = grafo.buscarAmistad(idUsuario, idAmigo)
                .orElseThrow(() -> new ExcepcionAmistadInvalida("Ya no son amigos."));
        List<Amistad> sinElla = new ArrayList<>(grafo.todasLasAmistades());
        sinElla.remove(amistad);
        repositorio.guardarTodas(sinElla);
        grafo.eliminar(idUsuario, idAmigo);
        return amistad;
    }

    // ================================================================== reglas

    /**
     * Revisa las reglas de una amistad. Se usa al agregar y al cargar el archivo, así las reglas
     * están en un solo lugar.
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

    /**
     * Revisa las reglas de una solicitud nueva: las de la amistad más que no haya ya una
     * solicitud pendiente entre los dos (en cualquier sentido).
     *
     * @param idRemitente    quien la envía
     * @param idDestinatario quien la recibe
     * @return descripción del problema, o {@code null} si la solicitud es válida
     */
    private String problemaDeLaSolicitud(String idRemitente, String idDestinatario) {
        if (idRemitente.equals(idDestinatario)) {
            return "No puede enviarse una solicitud a sí mismo.";
        }
        String problema = problemaDeLaAmistad(idRemitente, idDestinatario);
        if (problema != null) {
            return problema;
        }
        if (buscarSolicitud(idRemitente, idDestinatario).isPresent()) {
            return "Ya enviaste una solicitud a este usuario. Espera a que la responda.";
        }
        if (buscarSolicitud(idDestinatario, idRemitente).isPresent()) {
            return "Este usuario ya te envió una solicitud. Acéptala en la pestaña \"Solicitudes\".";
        }
        return null;
    }

    /**
     * @param idRemitente    quien la envió
     * @param idDestinatario quien la recibió
     * @return la solicitud pendiente en ese sentido, si existe
     */
    private Optional<SolicitudAmistad> buscarSolicitud(String idRemitente, String idDestinatario) {
        for (SolicitudAmistad solicitud : solicitudes) {
            if (solicitud.va(idRemitente, idDestinatario)) {
                return Optional.of(solicitud);
            }
        }
        return Optional.empty();
    }

    // ================================================================== ServicioAmistades: consultas

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

    /** {@inheritDoc} */
    @Override
    public synchronized List<SolicitudRecibida> listarSolicitudesRecibidas(String idUsuario) {
        List<SolicitudRecibida> recibidas = new ArrayList<>();
        for (SolicitudAmistad solicitud : solicitudes) {
            if (solicitud.idDestinatario().equals(idUsuario)) {
                // Invariante: al cargar y al enviar solo entran solicitudes de usuarios existentes.
                directorio.buscarPorId(solicitud.idRemitente())
                        .ifPresent(remitente -> recibidas.add(new SolicitudRecibida(remitente, solicitud.fecha())));
            }
        }
        return Collections.unmodifiableList(recibidas);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean haySolicitudPendiente(String idRemitente, String idDestinatario) {
        return buscarSolicitud(idRemitente, idDestinatario).isPresent();
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
     * @param id ID recibido de la UI (puede ser nulo o tener espacios)
     * @return ID sin espacios a los lados ("" si era nulo)
     */
    private static String limpiar(String id) {
        return id == null ? "" : id.trim();
    }

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
