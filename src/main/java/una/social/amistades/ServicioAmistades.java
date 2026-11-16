/*
 * ServicioAmistades.java
 *
 * Qué hace: contrato del módulo de amistades: solicitudes de amistad (enviar, aceptar, rechazar,
 * cancelar), eliminar amigos y listar amigos y solicitudes.
 * Rol en la arquitectura: la UI (PanelBuscador, PanelAmigos, PanelSolicitudes) depende de esta
 * interfaz.
 * Principio SOLID: ISP (extiende las consultas mínimas y agrega solo lo que la UI necesita) y DIP.
 * Colabora con: GestorAmistades (implementación), PanelBuscador, PanelAmigos, PanelSolicitudes.
 */
package una.social.amistades;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones de amistades.
 *
 * <p>Flujo normal: A envía una solicitud a B → B la ve al iniciar sesión y en "Solicitudes" →
 * B la acepta (se crea la amistad mutua) o la rechaza (se borra). Mientras está pendiente, A puede
 * cancelarla. Cualquiera de los dos amigos puede eliminar la amistad después.</p>
 *
 * <p>Todos los métodos que cambian datos los guardan inmediatamente en disco y lanzan
 * {@link una.social.excepciones.ExcepcionAmistadInvalida} si no se cumple una regla, o
 * {@link una.social.excepciones.ExcepcionPersistencia} si no se pudo guardar.</p>
 */
public interface ServicioAmistades extends ConsultaAmistades, ConsultaSolicitudes {

    /**
     * Crea DIRECTAMENTE una amistad mutua, sin solicitud. La UI no lo usa: sirve para los datos
     * de ejemplo (HerramientaDatosSemillaSocial) y las pruebas.
     *
     * @param solicitante  usuario que agrega
     * @param idNuevoAmigo ID del usuario agregado
     * @return la amistad creada
     */
    Amistad agregarAmistad(Usuario solicitante, String idNuevoAmigo);

    /**
     * "Enviar solicitud de amistad": queda pendiente hasta que el destinatario responda.
     *
     * <p>Si el destinatario YA le había enviado una solicitud al remitente, no se crea otra: se
     * acepta automáticamente la que existía y quedan como amigos.</p>
     *
     * @param remitente      usuario activo
     * @param idDestinatario ID del usuario al que se le envía
     * @return la solicitud pendiente o la amistad creada automáticamente
     */
    ResultadoEnvioSolicitud enviarSolicitud(Usuario remitente, String idDestinatario);

    /**
     * Acepta una solicitud recibida: se borra la solicitud y se crea la amistad mutua.
     *
     * @param destinatario usuario activo (quien recibió la solicitud)
     * @param idRemitente  ID de quien la envió
     * @return la amistad creada
     */
    Amistad aceptarSolicitud(Usuario destinatario, String idRemitente);

    /**
     * Rechaza una solicitud recibida: se borra sin crear amistad.
     *
     * @param destinatario usuario activo (quien recibió la solicitud)
     * @param idRemitente  ID de quien la envió
     */
    void rechazarSolicitud(Usuario destinatario, String idRemitente);

    /**
     * Cancela una solicitud que el usuario activo envió y aún no ha sido respondida.
     *
     * @param remitente      usuario activo (quien envió la solicitud)
     * @param idDestinatario ID de a quién se la envió
     */
    void cancelarSolicitud(Usuario remitente, String idDestinatario);

    /**
     * Elimina la amistad entre el usuario activo y uno de sus amigos (para los dos).
     *
     * @param usuario usuario activo
     * @param idAmigo ID del amigo a eliminar
     */
    void eliminarAmistad(Usuario usuario, String idAmigo);

    /**
     * Lista los amigos de un usuario, ordenados alfabéticamente por nombre.
     *
     * @param idUsuario usuario consultado
     * @return lista no modificable de amigos con su fecha de amistad
     */
    List<Amigo> listarAmigos(String idUsuario);

    /**
     * Lista las solicitudes pendientes que recibió un usuario, de la más antigua a la más reciente.
     *
     * @param idUsuario usuario consultado (destinatario)
     * @return lista no modificable de solicitudes con los datos del remitente
     */
    List<SolicitudRecibida> listarSolicitudesRecibidas(String idUsuario);

    /**
     * Lista las solicitudes que un usuario envió y siguen pendientes, de la más antigua a la más
     * reciente (para poder cancelarlas).
     *
     * @param idUsuario usuario consultado (remitente)
     * @return lista no modificable de solicitudes con los datos del destinatario
     */
    List<SolicitudEnviada> listarSolicitudesEnviadas(String idUsuario);
}
