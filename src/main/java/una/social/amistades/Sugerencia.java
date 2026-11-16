/*
 * Sugerencia.java
 *
 * Qué hace: una persona sugerida en "Personas que quizás conozcas", con cuántos amigos tienen en
 * común.
 * Rol en la arquitectura: la devuelve ServicioSugerencias; el PanelBuscador la muestra.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: SugeridorAmistades, PanelBuscador.
 */
package una.social.amistades;

import una.social.modelo.Usuario;

/**
 * Sugerencia de amistad.
 *
 * @param usuario        persona sugerida
 * @param amigosEnComun  cantidad de amigos que tienen en común (siempre 1 o más)
 */
public record Sugerencia(Usuario usuario, int amigosEnComun) {
}
