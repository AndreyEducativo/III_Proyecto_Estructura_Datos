/*
 * SolicitudEnviada.java
 *
 * Qué hace: una solicitud que el usuario activo envió y sigue pendiente, con los datos de a quién
 * se la envió.
 * Rol en la arquitectura: la devuelve ServicioAmistades.listarSolicitudesEnviadas; la pestaña
 * "Solicitudes" la muestra con el botón "Cancelar".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorAmistades, PanelSolicitudes.
 */
package una.social.amistades;

import java.time.Instant;
import una.social.modelo.Usuario;

/**
 * Solicitud enviada y pendiente.
 *
 * @param destinatario usuario que debe responderla
 * @param fecha        momento del envío
 */
public record SolicitudEnviada(Usuario destinatario, Instant fecha) {
}
