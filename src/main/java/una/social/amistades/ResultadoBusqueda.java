/*
 * ResultadoBusqueda.java
 *
 * Qué hace: un usuario encontrado por el buscador, con su relación con quien busca.
 * Rol en la arquitectura: lo devuelve ServicioBusqueda; el PanelBuscador decide con estos datos si
 * mostrar el botón "Agregar como amigo", "Ya son amigos" o "Eres tú".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: BuscadorUsuarios, PanelBuscador.
 */
package una.social.amistades;

import una.social.modelo.Usuario;

/**
 * Resultado de una búsqueda de usuarios.
 *
 * @param usuario      usuario encontrado
 * @param esAmigo      {@code true} si ya es amigo de quien busca
 * @param esElMismo    {@code true} si es el propio usuario que busca
 */
public record ResultadoBusqueda(Usuario usuario, boolean esAmigo, boolean esElMismo) {
}
