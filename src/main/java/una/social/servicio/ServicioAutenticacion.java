/*
 * ServicioAutenticacion.java
 *
 * Qué hace: contrato para iniciar sesión.
 * Rol en la arquitectura: la VentanaLogin depende SOLO de esta interfaz (no puede registrar ni
 * listar usuarios, porque no lo necesita).
 * Principio SOLID: ISP (una ventana no depende de métodos que no usa) y DIP.
 * Colabora con: GestorUsuarios (implementación), VentanaLogin.
 */
package una.social.servicio;

import una.social.modelo.Usuario;

/**
 * Inicio de sesión.
 */
public interface ServicioAutenticacion {

    /**
     * Verifica las credenciales.
     *
     * <p>La contraseña se recibe como {@code char[]}; esta operación NO la modifica. Quien llama
     * es responsable de borrarla con {@code Arrays.fill} cuando termine.</p>
     *
     * @param nombreUsuario nombre escrito (sin distinguir mayúsculas)
     * @param contrasena    contraseña escrita
     * @return el usuario autenticado
     * @throws una.social.excepciones.ExcepcionValidacion      si algún campo está vacío
     * @throws una.social.excepciones.ExcepcionCuentaBloqueada si hay demasiados intentos fallidos
     * @throws una.social.excepciones.ExcepcionAutenticacion   si las credenciales no son válidas
     *                                                         (mensaje siempre genérico)
     */
    Usuario autenticar(String nombreUsuario, char[] contrasena);
}
