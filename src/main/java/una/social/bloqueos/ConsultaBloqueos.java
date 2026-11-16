/*
 * ConsultaBloqueos.java
 *
 * Qué hace: contrato MÍNIMO de solo lectura sobre los bloqueos: "¿hay un bloqueo entre A y B?".
 * Rol en la arquitectura: lo usan amistades, buscador, sugerencias, publicaciones, comentarios y
 * anuncios para ocultar todo lo de un usuario bloqueado. Ninguno de ellos puede crear ni quitar
 * bloqueos: solo preguntar.
 * Principio SOLID: ISP (un solo método) y DIP (los módulos dependen de esta abstracción y no de
 * GestorBloqueos).
 * Colabora con: GestorBloqueos (implementación) y todos los módulos sociales.
 */
package una.social.bloqueos;

/**
 * Consulta de bloqueos (solo lectura).
 *
 * <p>El bloqueo es MUTUO a efectos de visibilidad: si A bloqueó a B, ni A ve lo de B ni B ve lo de
 * A. Por eso la pregunta es "¿hay un bloqueo entre ellos?", sin importar quién bloqueó a quién.</p>
 */
@FunctionalInterface
public interface ConsultaBloqueos {

    /** Consulta que nunca encuentra bloqueos (útil en pruebas y herramientas). */
    ConsultaBloqueos SIN_BLOQUEOS = (idA, idB) -> false;

    /**
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si A bloqueó a B o B bloqueó a A
     */
    boolean hayBloqueoEntre(String idA, String idB);
}
