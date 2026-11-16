/*
 * Amigo.java
 *
 * Qué hace: une a un usuario con la fecha desde la que es amigo del usuario activo.
 * Rol en la arquitectura: es lo que devuelve ServicioAmistades.listarAmigos para que el
 * PanelAmigos muestre "Amigos desde dd/MM/yyyy".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: ServicioAmistades, GestorAmistades, PanelAmigos.
 */
package una.social.amistades;

import java.time.Instant;
import una.social.modelo.Usuario;

/**
 * Un amigo del usuario activo.
 *
 * @param usuario      datos del amigo
 * @param amigosDesde  fecha en que se creó la amistad
 */
public record Amigo(Usuario usuario, Instant amigosDesde) {
}
