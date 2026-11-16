/*
 * EntradaMuro.java
 *
 * Qué hace: une una publicación con los datos de su autor, listos para mostrarse en el muro.
 * Rol en la arquitectura: es lo que devuelve ServicioPublicaciones.obtenerMuro; así la UI no tiene
 * que buscar el nombre de cada autor por su cuenta.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorPublicaciones, PanelMuro.
 */
package una.social.publicaciones;

import una.social.modelo.Usuario;

/**
 * Una publicación del muro con su autor.
 *
 * @param publicacion publicación
 * @param autor       usuario que la escribió
 */
public record EntradaMuro(Publicacion publicacion, Usuario autor) {
}
