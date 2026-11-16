/*
 * ExcepcionReglaNegocio.java
 *
 * Qué hace: indica que una acción del usuario no cumple una regla de la red social (por ejemplo
 * "no puedes comentar una publicación que no ves", "ya bloqueaste a este usuario",
 * "solo puedes editar tu última publicación").
 * Rol en la arquitectura: base de los errores "esperables" de los módulos sociales. La UI la
 * muestra como AVISO con su mensaje (no como error grave), porque el usuario puede corregirlo.
 * Principio SOLID: LSP (sus subclases se manejan igual) y OCP (cada módulo agrega su subclase sin
 * modificar el manejo de errores existente).
 * Colabora con: ExcepcionAmistadInvalida, ExcepcionPublicacionInvalida, los gestores sociales y la UI.
 */
package una.social.excepciones;

/**
 * Regla de negocio no cumplida. El mensaje siempre es apto para mostrarse al usuario.
 */
public class ExcepcionReglaNegocio extends ExcepcionSocialUna {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje explicación amigable de la regla que no se cumplió
     */
    public ExcepcionReglaNegocio(String mensaje) {
        super(mensaje);
    }
}
