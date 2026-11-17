/*
 * GestorAmistades.java
 *
 * Qué hace: lógica de negocio de las amistades: cargar y guardar el grafo y las solicitudes,
 * enviar / aceptar / rechazar / cancelar solicitudes de amistad (con aceptación automática cuando
 * las dos personas se envían solicitud), eliminar amigos, listar amigos y solicitudes, y romper la
 * relación cuando un usuario bloquea a otro.
 * Rol en la arquitectura: capa de SERVICIO del módulo de amistades. La UI le habla por
 * ServicioAmistades; publicaciones y sugerencias por ConsultaAmistades; el buscador por
 * ConsultaSolicitudes; el módulo de bloqueos le avisa como OyenteBloqueo; Main lo registra como
 * ModuloPersistente para que se cargue al iniciar y se guarde al cerrar.
 * Principio SOLID: SRP (coordina los casos de uso; el grafo está en GrafoAmistades, las solicitudes
 * en BandejaSolicitudes y el disco en los repositorios), ISP (implementa interfaces pequeñas), DIP
 * (todas sus dependencias son abstracciones inyectadas por constructor).
 * Colabora con: GrafoAmistades, BandejaSolicitudes, RepositorioAmistades, RepositorioSolicitudes,
 * DirectorioUsuarios, ConsultaBloqueos, RegistroAuditoria.
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
import una.social.bloqueos.ConsultaBloqueos;
import una.social.bloqueos.OyenteBloqueo;
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
public final class GestorAmistades implements ServicioAmistades, ModuloPersistente, OyenteBloqueo {

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

    /** Solicitudes pendientes en memoria: una COLA (FIFO) por destinatario. */
    private final BandejaSolicitudes bandeja = new BandejaSolicitudes();

    /** Directorio de usuarios (solo lectura) para validar que los IDs existan. */
    private final DirectorioUsuarios directorio;

    /** Bloqueos (solo lectura): entre usuarios bloqueados no hay amistades ni solicitudes. */
    private final ConsultaBloqueos bloqueos;

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
     * @param bloqueos               consulta de bloqueos (deben cargarse ANTES que las amistades)
     * @param auditoria              bitácora
     * @param reloj                  reloj del sistema
     */
    public GestorAmistades(RepositorioAmistades repositorio, RepositorioSolicitudes repositorioSolicitudes,
                           GrafoAmistades grafo, DirectorioUsuarios directorio, ConsultaBloqueos bloqueos,
                           RegistroAuditoria auditoria, Clock reloj) {
        // Se guardan las dependencias; requireNonNull falla de inmediato si falta alguna.
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.repositorioSolicitudes = Objects.requireNonNull(repositorioSolicitudes, "repositorioSolicitudes");
        this.grafo = Objects.requireNonNull(grafo, "grafo");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== ModuloPersistente

    /**
     * Nombre corto del módulo para la auditoría y los avisos de carga.
     *
     * @return "amistades"
     */
    @Override
    public String nombreModulo() {
        return "amistades";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Además del formato (que revisan los repositorios), aquí se revisan las REGLAS: que los
     * usuarios existan, que no sea un usuario consigo mismo, que no haya repetidos ni bloqueos.
     * Las líneas que no cumplen se omiten y se auditan. Las amistades se cargan primero porque una
     * solicitud entre dos usuarios que ya son amigos no es válida. Las solicitudes se leen en
     * orden de fecha, así cada cola queda en el orden en que llegaron.</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si algún archivo no se puede leer: la carga queda marcada
        // como fallida (no se permitirá guardar), se audita y se relanza para avisar al usuario.
        try {
            // Lee los dos archivos: amistades.txt y solicitudes.txt.
            List<Amistad> leidas = repositorio.cargarTodas();
            List<SolicitudAmistad> solicitudesLeidas = new ArrayList<>(repositorioSolicitudes.cargarTodas());
            // Ordena las solicitudes por fecha para que cada cola FIFO quede en orden de llegada.
            solicitudesLeidas.sort(Comparator.comparing(SolicitudAmistad::fecha));
            // Vacía el grafo y la bandeja antes de llenarlos de nuevo.
            grafo.limpiar();
            bandeja.limpiar();
            // Primero las amistades (aristas del grafo)...
            for (Amistad amistad : leidas) {
                agregarAlGrafoSiEsValida(amistad);
            }
            // ...y después las solicitudes (se encolan en la bandeja de cada destinatario).
            for (SolicitudAmistad solicitud : solicitudesLeidas) {
                agregarSolicitudSiEsValida(solicitud);
            }
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Grafo de amistades en memoria: " + grafo.cantidadAmistades() + " amistades y "
                            + bandeja.tamano() + " solicitudes pendientes");
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
        // Revisa las reglas (usuarios existen, no es consigo mismo, sin bloqueo, sin repetir).
        String problema = problemaDeLaAmistad(amistad.idUsuarioA(), amistad.idUsuarioB());
        if (problema != null) {
            // La línea no cumple: se omite y se anota en la auditoría.
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "amistades.txt: amistad " + amistad.idUsuarioA() + "-" + amistad.idUsuarioB() + " omitida (" + problema + ")");
            return;
        }
        // Es válida: se agrega como arista en el grafo (lista de adyacencia).
        grafo.agregar(amistad);
    }

    /**
     * Agrega una solicitud leída del archivo solo si respeta las reglas (incluida la de no tener
     * ya una solicitud entre los dos en cualquier sentido).
     *
     * @param solicitud solicitud leída
     */
    private void agregarSolicitudSiEsValida(SolicitudAmistad solicitud) {
        // Mismas reglas que al enviar una solicitud desde la interfaz.
        String problema = problemaDeLaSolicitud(solicitud.idRemitente(), solicitud.idDestinatario());
        // Además, no puede haber dos solicitudes cruzadas (A->B y B->A) en el archivo.
        if (problema == null && bandeja.buscar(solicitud.idDestinatario(), solicitud.idRemitente()).isPresent()) {
            problema = "ya hay una solicitud en sentido contrario";
        }
        // Si hay algún problema se omite la línea y se audita.
        if (problema != null) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "solicitudes.txt: solicitud " + solicitud.idRemitente() + "->" + solicitud.idDestinatario()
                            + " omitida (" + problema + ")");
            return;
        }
        // Válida: entra al final de la cola FIFO del destinatario.
        bandeja.encolar(solicitud);
    }

    /**
     * Escribe las amistades en amistades.txt y las solicitudes pendientes en solicitudes.txt.
     *
     * @throws ExcepcionPersistencia si la carga inicial falló o el disco no permite escribir
     */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            repositorio.guardarTodas(grafo.todasLasAmistades());
            repositorioSolicitudes.guardarTodas(bandeja.todas());
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    grafo.cantidadAmistades() + " amistades y " + bandeja.tamano() + " solicitudes guardadas");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    "amistades: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    // ================================================================== ServicioAmistades: cambios

    /**
     * Crea una amistad directa, sin pasar por solicitud (lo usan los datos de prueba y las
     * pruebas automáticas; la interfaz usa {@link #enviarSolicitud}).
     *
     * @param solicitante  usuario que agrega
     * @param idNuevoAmigo ID del usuario agregado
     * @return la amistad creada
     * @throws ExcepcionAmistadInvalida si no cumple las reglas
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
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

    /**
     * Botón "Agregar amigo": envía una solicitud de amistad que queda en la cola (FIFO) del
     * destinatario hasta que la acepte o la rechace. Si el otro ya le había enviado una, se
     * hacen amigos de una vez.
     *
     * @param remitente      usuario con la sesión iniciada
     * @param idDestinatario ID de quien recibe la solicitud
     * @return la solicitud pendiente, o la amistad si se aceptó automáticamente
     * @throws ExcepcionAmistadInvalida si no cumple las reglas
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
    @Override
    public ResultadoEnvioSolicitud enviarSolicitud(Usuario remitente, String idDestinatario) {
        Objects.requireNonNull(remitente, "remitente");
        String destino = limpiar(idDestinatario);
        // registrarSolicitud hace el trabajo; conAuditoria anota el resultado (o el error).
        // El texto de la auditoría cambia si hubo aceptación automática.
        return conAuditoria(remitente, "SOLICITUD_ENVIADA", () -> registrarSolicitud(remitente.getId(), destino),
                r -> r.seAceptoAutomaticamente()
                        ? remitente.getId() + " y " + destino + " se enviaron solicitud mutuamente: amistad creada automáticamente"
                        : remitente.getId() + " envió una solicitud a " + destino);
    }

    /**
     * Botón "Aceptar" de la pestaña Solicitudes: crea la amistad y saca la solicitud de la cola.
     *
     * @param destinatario usuario con la sesión iniciada (quien recibió la solicitud)
     * @param idRemitente  ID de quien la envió
     * @return la amistad creada
     * @throws ExcepcionAmistadInvalida si la solicitud ya no está pendiente o no cumple las reglas
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
    @Override
    public Amistad aceptarSolicitud(Usuario destinatario, String idRemitente) {
        Objects.requireNonNull(destinatario, "destinatario");
        String origen = limpiar(idRemitente);
        // aceptar() crea la amistad y borra la solicitud; conAuditoria lo anota.
        return conAuditoria(destinatario, "SOLICITUD_ACEPTADA", () -> aceptar(origen, destinatario.getId()),
                a -> a.idUsuarioB() + " aceptó la solicitud de " + a.idUsuarioA());
    }

    /**
     * Botón "Rechazar" de la pestaña Solicitudes: borra la solicitud sin crear amistad.
     *
     * @param destinatario usuario con la sesión iniciada (quien recibió la solicitud)
     * @param idRemitente  ID de quien la envió
     * @throws ExcepcionAmistadInvalida si la solicitud ya no está pendiente
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
    @Override
    public void rechazarSolicitud(Usuario destinatario, String idRemitente) {
        Objects.requireNonNull(destinatario, "destinatario");
        String origen = limpiar(idRemitente);
        // La solicitud va de "origen" hacia el usuario activo; se borra de disco y de la cola.
        conAuditoria(destinatario, "SOLICITUD_RECHAZADA", () -> borrarSolicitud(origen, destinatario.getId()),
                s -> s.idDestinatario() + " rechazó la solicitud de " + s.idRemitente());
    }

    /**
     * Botón "Cancelar solicitud": quien la envió se arrepiente y la retira antes de que la respondan.
     *
     * @param remitente      usuario con la sesión iniciada (quien envió la solicitud)
     * @param idDestinatario ID de quien la iba a recibir
     * @throws ExcepcionAmistadInvalida si la solicitud ya no está pendiente
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
    @Override
    public void cancelarSolicitud(Usuario remitente, String idDestinatario) {
        Objects.requireNonNull(remitente, "remitente");
        String destino = limpiar(idDestinatario);
        // Aquí la solicitud va del usuario activo hacia "destino" (sentido contrario a rechazar).
        conAuditoria(remitente, "SOLICITUD_CANCELADA", () -> borrarSolicitud(remitente.getId(), destino),
                s -> s.idRemitente() + " canceló su solicitud a " + s.idDestinatario());
    }

    /**
     * Botón "Eliminar amigo": quita la arista del grafo en los dos sentidos y lo guarda.
     *
     * @param usuario usuario con la sesión iniciada
     * @param idAmigo ID del amigo a eliminar
     * @throws ExcepcionAmistadInvalida si ya no eran amigos
     * @throws ExcepcionPersistencia    si no se pudo guardar
     */
    @Override
    public void eliminarAmistad(Usuario usuario, String idAmigo) {
        Objects.requireNonNull(usuario, "usuario");
        String amigo = limpiar(idAmigo);
        // eliminar() borra de disco y del grafo; conAuditoria lo anota.
        conAuditoria(usuario, "AMISTAD_ELIMINADA", () -> eliminar(usuario.getId(), amigo),
                a -> usuario.getId() + " eliminó la amistad con " + amigo);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Al bloquear, la amistad entre los dos (si existía) y las solicitudes pendientes en los dos
     * sentidos se eliminan. Primero en disco, luego en memoria.</p>
     */
    @Override
    public synchronized void alBloquear(String idBloqueador, String idBloqueado) {
        exigirDatosCargados();
        // Paso 1: si eran amigos, se borra la amistad (archivo primero, después el grafo).
        boolean eranAmigos = grafo.sonAmigos(idBloqueador, idBloqueado);
        if (eranAmigos) {
            List<Amistad> sinElla = new ArrayList<>(grafo.todasLasAmistades());
            sinElla.removeIf(a -> a.involucraA(idBloqueador) && a.involucraA(idBloqueado));
            repositorio.guardarTodas(sinElla);
            grafo.eliminar(idBloqueador, idBloqueado);
        }
        // Paso 2: si había solicitudes pendientes en cualquier sentido, también se borran.
        boolean habiaSolicitudes = bandeja.buscar(idBloqueador, idBloqueado).isPresent()
                || bandeja.buscar(idBloqueado, idBloqueador).isPresent();
        if (habiaSolicitudes) {
            List<SolicitudAmistad> sinEllas = new ArrayList<>(bandeja.todas());
            sinEllas.removeIf(s -> s.va(idBloqueador, idBloqueado) || s.va(idBloqueado, idBloqueador));
            repositorioSolicitudes.guardarTodas(sinEllas);
            bandeja.quitarEntre(idBloqueador, idBloqueado);
        }
        // Paso 3: se deja constancia de lo que se rompió.
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "RELACION_ROTA_POR_BLOQUEO",
                idBloqueador + "-" + idBloqueado + ": amistad " + (eranAmigos ? "eliminada" : "inexistente")
                        + ", solicitudes " + (habiaSolicitudes ? "canceladas" : "inexistentes"));
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
        // Si alguna regla no se cumple, se lanza el error con el mensaje para el usuario.
        String problema = problemaDeLaAmistad(idSolicitante, idNuevoAmigo);
        if (problema != null) {
            throw new ExcepcionAmistadInvalida(problema);
        }
        // Nueva arista con la fecha de hoy.
        Amistad nueva = new Amistad(idSolicitante, idNuevoAmigo, Instant.now(reloj));
        // Se escribe la lista completa (actuales + nueva) en amistades.txt...
        List<Amistad> conNueva = new ArrayList<>(grafo.todasLasAmistades());
        conNueva.add(nueva);
        repositorio.guardarTodas(conNueva);
        // ...y solo si el disco no falló se agrega al grafo en memoria.
        grafo.agregar(nueva);
        return nueva;
    }

    /**
     * Envía una solicitud. Si el destinatario ya había enviado una al remitente, en vez de crear
     * otra se acepta la existente (aceptación automática).
     *
     * @param idRemitente    quien la envía
     * @param idDestinatario quien la recibe
     * @return solicitud pendiente o amistad creada
     */
    private synchronized ResultadoEnvioSolicitud registrarSolicitud(String idRemitente, String idDestinatario) {
        exigirDatosCargados();
        // Revisa reglas: no a sí mismo, usuario existe, sin bloqueo, no amigos ya, no repetida.
        String problema = problemaDeLaSolicitud(idRemitente, idDestinatario);
        if (problema != null) {
            throw new ExcepcionAmistadInvalida(problema);
        }
        // ¿El destinatario ya le había mandado una solicitud al remitente?
        if (bandeja.buscar(idDestinatario, idRemitente).isPresent()) {
            // El otro ya nos había invitado: los dos quieren ser amigos, así que se acepta su
            // solicitud. El remitente original de esa solicitud es ahora el "destinatario".
            return new ResultadoEnvioSolicitud(null, aceptar(idDestinatario, idRemitente));
        }
        // Caso normal: se crea la solicitud pendiente con la fecha de hoy.
        SolicitudAmistad nueva = new SolicitudAmistad(idRemitente, idDestinatario, Instant.now(reloj));
        // Se guarda primero en solicitudes.txt...
        List<SolicitudAmistad> conNueva = new ArrayList<>(bandeja.todas());
        conNueva.add(nueva);
        repositorioSolicitudes.guardarTodas(conNueva);
        // ...y luego entra al FINAL de la cola FIFO del destinatario.
        bandeja.encolar(nueva);
        // Resultado: solicitud pendiente, sin amistad todavía.
        return new ResultadoEnvioSolicitud(nueva, null);
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
        // La solicitud tiene que seguir en la cola (pudo haberse cancelado mientras tanto).
        if (bandeja.buscar(idRemitente, idDestinatario).isEmpty()) {
            throw new ExcepcionAmistadInvalida("La solicitud ya no está pendiente.");
        }
        // La amistad es lo importante: se guarda primero. A = quien envió la solicitud.
        Amistad creada = crearYGuardar(idRemitente, idDestinatario);
        // Ya son amigos: la solicitud sale de la cola del destinatario.
        bandeja.quitar(idRemitente, idDestinatario);
        // Se espera ExcepcionPersistencia si no se pudo reescribir solicitudes.txt. La amistad YA
        // quedó guardada, así que no se informa como error al usuario: la solicitud vieja que
        // quede en el archivo se descarta sola al cargar (ya son amigos) y se vuelve a intentar
        // guardar al cerrar sesión. Se deja constancia en la auditoría.
        try {
            repositorioSolicitudes.guardarTodas(bandeja.todas());
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
        // Busca la solicitud en la cola; si ya no está, avisa al usuario.
        SolicitudAmistad solicitud = bandeja.buscar(idRemitente, idDestinatario)
                .orElseThrow(() -> new ExcepcionAmistadInvalida("La solicitud ya no está pendiente."));
        // Reescribe solicitudes.txt sin ella...
        List<SolicitudAmistad> sinElla = new ArrayList<>(bandeja.todas());
        sinElla.remove(solicitud);
        repositorioSolicitudes.guardarTodas(sinElla);
        // ...y la quita de la cola en memoria.
        bandeja.quitar(idRemitente, idDestinatario);
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
        // Busca la arista entre los dos; si no existe, ya no eran amigos.
        Amistad amistad = grafo.buscarAmistad(idUsuario, idAmigo)
                .orElseThrow(() -> new ExcepcionAmistadInvalida("Ya no son amigos."));
        // Reescribe amistades.txt sin esa amistad...
        List<Amistad> sinElla = new ArrayList<>(grafo.todasLasAmistades());
        sinElla.remove(amistad);
        repositorio.guardarTodas(sinElla);
        // ...y la quita del grafo (de la lista de vecinos de los dos usuarios).
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
        // Regla 1: nadie puede ser amigo de sí mismo.
        if (idA.equals(idB)) {
            return "No puede agregarse a sí mismo como amigo.";
        }
        // Regla 2: los dos usuarios tienen que existir.
        if (directorio.buscarPorId(idA).isEmpty() || directorio.buscarPorId(idB).isEmpty()) {
            return "El usuario indicado no existe.";
        }
        // Regla 3: no puede haber un bloqueo entre ellos (en ningún sentido).
        if (bloqueos.hayBloqueoEntre(idA, idB)) {
            return "No es posible: hay un bloqueo entre ustedes.";
        }
        // Regla 4: no pueden ser amigos ya.
        if (grafo.sonAmigos(idA, idB)) {
            return "Ya son amigos.";
        }
        // Ningún problema.
        return null;
    }

    /**
     * Revisa las reglas de una solicitud nueva: las de la amistad más que no la haya enviado ya.
     * Una solicitud en sentido CONTRARIO no es un error: se resuelve aceptándola automáticamente.
     *
     * @param idRemitente    quien la envía
     * @param idDestinatario quien la recibe
     * @return descripción del problema, o {@code null} si la solicitud es válida
     */
    private String problemaDeLaSolicitud(String idRemitente, String idDestinatario) {
        // Mensaje propio para el caso "a sí mismo" (más claro que el de la amistad).
        if (idRemitente.equals(idDestinatario)) {
            return "No puede enviarse una solicitud a sí mismo.";
        }
        // Se reutilizan las reglas de la amistad (existen, sin bloqueo, no amigos ya).
        String problema = problemaDeLaAmistad(idRemitente, idDestinatario);
        if (problema != null) {
            return problema;
        }
        // Regla extra: no mandar dos veces la misma solicitud.
        if (bandeja.buscar(idRemitente, idDestinatario).isPresent()) {
            return "Ya enviaste una solicitud a este usuario. Espera a que la responda.";
        }
        return null;
    }

    // ================================================================== ServicioAmistades: consultas

    /**
     * Pestaña "Mis amigos": devuelve los amigos del usuario ordenados alfabéticamente por nombre.
     *
     * @param idUsuario ID del usuario
     * @return amigos con la fecha en que se hicieron amigos (lista no modificable)
     */
    @Override
    public synchronized List<Amigo> listarAmigos(String idUsuario) {
        List<Amigo> amigos = new ArrayList<>();
        // Recorre los vecinos del usuario en el grafo (lista de adyacencia).
        for (String idAmigo : grafo.amigosDe(idUsuario)) {
            // Por cada vecino se buscan sus datos y la fecha de la amistad.
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

    /**
     * {@inheritDoc}
     *
     * <p>Se recorre la cola del usuario desde el FRENTE: la primera es la que llegó primero.</p>
     */
    @Override
    public synchronized List<SolicitudRecibida> listarSolicitudesRecibidas(String idUsuario) {
        List<SolicitudRecibida> recibidas = new ArrayList<>();
        for (SolicitudAmistad solicitud : bandeja.recibidasPor(idUsuario)) {
            // Invariante: al cargar y al enviar solo entran solicitudes de usuarios existentes.
            directorio.buscarPorId(solicitud.idRemitente())
                    .ifPresent(remitente -> recibidas.add(new SolicitudRecibida(remitente, solicitud.fecha())));
        }
        return Collections.unmodifiableList(recibidas);
    }

    /**
     * Solicitudes que el usuario envió y que todavía nadie ha respondido (para mostrar
     * "Solicitud enviada" y el botón de cancelar).
     *
     * @param idUsuario ID del remitente
     * @return solicitudes enviadas pendientes (lista no modificable)
     */
    @Override
    public synchronized List<SolicitudEnviada> listarSolicitudesEnviadas(String idUsuario) {
        List<SolicitudEnviada> enviadas = new ArrayList<>();
        // Recorre todas las colas buscando las solicitudes que envió este usuario.
        for (SolicitudAmistad solicitud : bandeja.enviadasPor(idUsuario)) {
            // Se agregan los datos del destinatario (si todavía existe).
            directorio.buscarPorId(solicitud.idDestinatario())
                    .ifPresent(destino -> enviadas.add(new SolicitudEnviada(destino, solicitud.fecha())));
        }
        return Collections.unmodifiableList(enviadas);
    }

    /**
     * Indica si hay una solicitud pendiente de un usuario hacia otro.
     *
     * @param idRemitente    quien la envió
     * @param idDestinatario quien la recibe
     * @return {@code true} si está en la cola del destinatario
     */
    @Override
    public synchronized boolean haySolicitudPendiente(String idRemitente, String idDestinatario) {
        return bandeja.buscar(idRemitente, idDestinatario).isPresent();
    }

    // ================================================================== ConsultaAmistades

    /**
     * IDs de los amigos de un usuario (sus vecinos en el grafo). Lo usan el muro y las sugerencias.
     *
     * @param idUsuario ID del usuario
     * @return conjunto de IDs de amigos
     */
    @Override
    public synchronized Set<String> idsAmigos(String idUsuario) {
        return grafo.amigosDe(idUsuario);
    }

    /**
     * Indica si dos usuarios son amigos (si hay arista entre ellos en el grafo).
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si son amigos
     */
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
