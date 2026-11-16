/*
 * ExcepcionPublicacionInvalida.java
 *
 * Qué hace: indica que el texto de una publicación no es válido (vacío o demasiado largo) o que
 * el autor no existe.
 * Rol en la arquitectura: la lanza GestorPublicaciones y el PanelMuro muestra su mensaje en rojo
 * debajo del cuadro de texto.
 * Principio SOLID: LSP y OCP (nueva subclase de ExcepcionSocialUna).
 * Colabora con: GestorPublicaciones, PanelMuro.
 */
package una.social.excepciones;

/**
 * Publicación rechazada por no cumplir las reglas. Su mensaje es apto para el usuario.
 */
public class ExcepcionPublicacionInvalida extends ExcepcionSocialUna {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje explicación amigable (por ejemplo "Escriba algo antes de publicar.")
     */
    public ExcepcionPublicacionInvalida(String mensaje) {
        super(mensaje);
    }
}
