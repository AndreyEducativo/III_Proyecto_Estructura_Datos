/*
 * ExcepcionAmistadInvalida.java
 *
 * Qué hace: indica que no se puede crear una amistad (agregarse a uno mismo, ya son amigos o el
 * usuario no existe).
 * Rol en la arquitectura: la lanza GestorAmistades y la muestra el PanelBuscador con un aviso.
 * Principio SOLID: LSP (es una ExcepcionReglaNegocio; quien maneja la clase base la maneja igual) y
 * OCP (se agregó heredando, sin modificar la jerarquía existente).
 * Colabora con: GestorAmistades, PanelBuscador.
 */
package una.social.excepciones;

/**
 * Regla de negocio de amistades no cumplida. Su mensaje es apto para mostrarse al usuario.
 */
public class ExcepcionAmistadInvalida extends ExcepcionReglaNegocio {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje explicación amigable (por ejemplo "Ya son amigos.")
     */
    public ExcepcionAmistadInvalida(String mensaje) {
        super(mensaje);
    }
}
