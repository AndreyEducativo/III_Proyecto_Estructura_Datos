/*
 * ConsultaSolicitudes.java
 *
 * Qué hace: contrato MÍNIMO de solo lectura sobre las solicitudes de amistad: "¿hay una solicitud
 * pendiente de X hacia Y?".
 * Rol en la arquitectura: el buscador solo necesita esa pregunta para decidir qué botón mostrar
 * ("Enviar solicitud", "Solicitud enviada" o "Responder solicitud"); por eso depende de esta
 * interfaz pequeña y no del servicio completo.
 * Principio SOLID: ISP y DIP.
 * Colabora con: ServicioAmistades (la extiende), BuscadorUsuarios.
 */
package una.social.amistades;

/**
 * Consulta de solicitudes de amistad (solo lectura).
 */
@FunctionalInterface
public interface ConsultaSolicitudes {

    /**
     * @param idRemitente    posible remitente
     * @param idDestinatario posible destinatario
     * @return {@code true} si hay una solicitud pendiente de {@code idRemitente} a {@code idDestinatario}
     */
    boolean haySolicitudPendiente(String idRemitente, String idDestinatario);
}
