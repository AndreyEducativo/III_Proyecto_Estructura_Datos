/*
 * ResultadoBusqueda.java
 *
 * Qué hace: un usuario encontrado por el buscador, con su relación con quien busca.
 * Rol en la arquitectura: lo devuelve ServicioBusqueda; el PanelBuscador decide con estos datos qué
 * mostrar: "Eres tú", "Ya son amigos", "Cancelar solicitud", "Responder solicitud" o
 * "Enviar solicitud".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: BuscadorUsuarios, PanelBuscador.
 */
package una.social.amistades;

import una.social.modelo.Usuario;

/**
 * Resultado de una búsqueda de usuarios.
 *
 * @param usuario           usuario encontrado
 * @param esAmigo           {@code true} si ya es amigo de quien busca
 * @param esElMismo         {@code true} si es el propio usuario que busca
 * @param solicitudEnviada  {@code true} si quien busca le envió una solicitud que sigue pendiente
 * @param solicitudRecibida {@code true} si este usuario le envió una solicitud pendiente a quien busca
 */
public record ResultadoBusqueda(Usuario usuario, boolean esAmigo, boolean esElMismo,
                                boolean solicitudEnviada, boolean solicitudRecibida) {
}
