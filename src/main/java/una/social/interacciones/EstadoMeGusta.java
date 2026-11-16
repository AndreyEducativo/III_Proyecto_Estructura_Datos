/*
 * EstadoMeGusta.java
 *
 * Qué hace: estado del botón "Me gusta" de una publicación para el usuario activo: cuántos
 * "me gusta" tiene y si él ya dio el suyo. NUNCA incluye quiénes los dieron.
 * Rol en la arquitectura: lo devuelve ServicioMeGusta; la tarjeta de publicación lo muestra.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorMeGusta, TarjetaPublicacion.
 */
package una.social.interacciones;

/**
 * Estado de "me gusta" de una publicación.
 *
 * @param cantidad   número total de "me gusta" (lo único que se muestra a los demás)
 * @param meGustaMio {@code true} si el usuario activo ya dio "me gusta"
 */
public record EstadoMeGusta(int cantidad, boolean meGustaMio) {
}
