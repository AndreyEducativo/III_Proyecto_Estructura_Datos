/*
 * ComentarioVisible.java
 *
 * Qué hace: un comentario listo para mostrarse: con su autor y si el usuario activo puede borrarlo.
 * Rol en la arquitectura: lo devuelve ServicioComentarios; el diálogo de comentarios lo muestra.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorComentarios, DialogoComentarios.
 */
package una.social.interacciones;

import una.social.modelo.Usuario;

/**
 * Comentario para mostrar.
 *
 * @param comentario  comentario
 * @param autor       autor del comentario
 * @param puedeBorrar {@code true} si el usuario activo es el autor del comentario o de la publicación
 */
public record ComentarioVisible(Comentario comentario, Usuario autor, boolean puedeBorrar) {
}
