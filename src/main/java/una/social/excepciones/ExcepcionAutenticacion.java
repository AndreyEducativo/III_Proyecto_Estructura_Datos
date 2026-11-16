/*
 * ExcepcionAutenticacion.java
 *
 * Qué hace: indica que un intento de inicio de sesión no tuvo éxito.
 * Rol en la arquitectura: la lanza el servicio de usuarios y la captura la VentanaLogin.
 * Principio SOLID: LSP (sustituye a ExcepcionSocialUna sin romper el manejo de errores).
 * Colabora con: GestorUsuarios, VentanaLogin, ExcepcionCuentaBloqueada (subclase).
 */
package una.social.excepciones;

/**
 * Error de autenticación (credenciales inválidas).
 *
 * <p>Por seguridad, su mensaje es SIEMPRE genérico ("Usuario o contraseña incorrectos"):
 * no dice si el usuario existe o si lo que falló fue la contraseña. Así un atacante no puede
 * usar el login para averiguar qué nombres de usuario están registrados.</p>
 */
public class ExcepcionAutenticacion extends ExcepcionSocialUna {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Mensaje único que se muestra ante cualquier falla de credenciales. */
    public static final String MENSAJE_GENERICO = "Usuario o contraseña incorrectos.";

    /** Crea la excepción con el mensaje genérico. */
    public ExcepcionAutenticacion() {
        super(MENSAJE_GENERICO);
    }

    /**
     * Crea la excepción con un mensaje específico. Solo la usan las subclases
     * (por ejemplo, el aviso de bloqueo temporal).
     *
     * @param mensaje mensaje apto para el usuario
     */
    protected ExcepcionAutenticacion(String mensaje) {
        super(mensaje);
    }
}
