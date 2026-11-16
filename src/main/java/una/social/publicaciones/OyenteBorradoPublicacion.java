/*
 * OyenteBorradoPublicacion.java
 *
 * Qué hace: contrato para los módulos que deben reaccionar cuando se borra una publicación (los
 * "me gusta" y los comentarios de esa publicación también se deben borrar: borrado en cascada).
 * Rol en la arquitectura: parte del patrón OBSERVADOR. GestorPublicaciones no conoce a los módulos
 * de me gusta y comentarios (que a su vez dependen de publicaciones para saber qué se puede ver):
 * solo avisa a sus oyentes. Así no hay dependencias circulares.
 * Principio SOLID: OCP (nuevas reacciones = nuevos oyentes), DIP e ISP (un solo método).
 * Colabora con: GestorPublicaciones (avisa), GestorMeGusta y GestorComentarios (escuchan), Main.
 */
package una.social.publicaciones;

/**
 * Oyente del evento "se borró una publicación".
 */
@FunctionalInterface
public interface OyenteBorradoPublicacion {

    /**
     * Se llama DESPUÉS de que la publicación se borró y se guardó.
     *
     * @param idPublicacion ID de la publicación borrada
     * @throws una.social.excepciones.ExcepcionPersistencia si el oyente no pudo guardar su parte
     */
    void alBorrarPublicacion(String idPublicacion);
}
