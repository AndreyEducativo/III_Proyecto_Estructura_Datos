/*
 * ExcepcionUsuarioDuplicado.java
 *
 * Qué hace: indica que se intentó registrar un nombre de usuario (o un ID) que ya existe.
 * Rol en la arquitectura: la lanza el CatalogoUsuarios / GestorUsuarios; la VentanaRegistro la
 * muestra junto al campo "Usuario".
 * Principio SOLID: LSP. Es una ExcepcionValidacion más específica (el usuario puede corregirla
 * cambiando el dato), por eso hereda de ella.
 * Colabora con: CatalogoUsuarios, GestorUsuarios, VentanaRegistro.
 */
package una.social.excepciones;

import una.social.modelo.CampoFormulario;

/**
 * El nombre de usuario ya está registrado (la comparación no distingue mayúsculas).
 */
public class ExcepcionUsuarioDuplicado extends ExcepcionValidacion {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje mensaje amigable para el usuario
     */
    public ExcepcionUsuarioDuplicado(String mensaje) {
        super(CampoFormulario.USUARIO, mensaje);
    }
}
