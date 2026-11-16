/*
 * SolicitudRecibida.java
 *
 * Qué hace: une una solicitud pendiente con los datos del usuario que la envió.
 * Rol en la arquitectura: es lo que devuelve ServicioAmistades.listarSolicitudesRecibidas para que
 * la UI muestre "Ana Mora te envió una solicitud de amistad".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: ServicioAmistades, GestorAmistades, PanelSolicitudes, VentanaPrincipal.
 */
package una.social.amistades;

import java.time.Instant;
import una.social.modelo.Usuario;

/**
 * Solicitud de amistad recibida por el usuario activo.
 *
 * @param remitente usuario que envió la solicitud
 * @param fecha     momento en que la envió
 */
public record SolicitudRecibida(Usuario remitente, Instant fecha) {
}
