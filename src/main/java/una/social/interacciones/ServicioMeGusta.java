/*
 * ServicioMeGusta.java
 *
 * Qué hace: contrato de los "me gusta": dar o quitar (alternar) y consultar el contador.
 * Rol en la arquitectura: la tarjeta de publicación depende solo de esta interfaz.
 * Principio SOLID: ISP (dos operaciones) y DIP.
 * Colabora con: GestorMeGusta (implementación), TarjetaPublicacion.
 */
package una.social.interacciones;

import una.social.modelo.Usuario;

/**
 * Operaciones de "me gusta".
 */
public interface ServicioMeGusta {

    /**
     * Si el usuario no había dado "me gusta", lo da; si ya lo había dado, lo quita. Se guarda
     * inmediatamente en disco.
     *
     * @param usuario       usuario activo
     * @param idPublicacion publicación
     * @return estado nuevo (cantidad y si es mío)
     * @throws una.social.excepciones.ExcepcionReglaNegocio si no puede ver la publicación
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    EstadoMeGusta alternarMeGusta(Usuario usuario, String idPublicacion);

    /**
     * @param idUsuario     usuario activo
     * @param idPublicacion publicación
     * @return cantidad de "me gusta" y si el usuario activo dio el suyo
     */
    EstadoMeGusta estado(String idUsuario, String idPublicacion);
}
