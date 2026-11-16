/*
 * ServicioCuenta.java
 *
 * Qué hace: contrato para que un usuario cambie su nombre completo y su contraseña.
 * Rol en la arquitectura: interfaz NUEVA y pequeña del módulo de usuarios. La implementa
 * GestorUsuarios sin modificar sus interfaces existentes (ServicioAutenticacion, ServicioRegistro,
 * DirectorioUsuarios); la pestaña "Mi perfil" depende solo de esta.
 * Principio SOLID: ISP (dos operaciones), OCP (se agrega sin cambiar contratos anteriores) y DIP.
 * Colabora con: GestorUsuarios (implementación), PanelMiPerfil.
 */
package una.social.servicio;

import una.social.modelo.Usuario;

/**
 * Operaciones sobre la cuenta del usuario activo.
 *
 * <p>Las contraseñas se reciben como {@code char[]} (se pueden borrar de la memoria); este servicio
 * no las modifica: quien llama debe borrarlas con {@code Arrays.fill} al terminar.</p>
 */
public interface ServicioCuenta {

    /**
     * Cambia el nombre completo (mismas reglas que en el registro). El ID y el nombre de usuario no
     * cambian. Se guarda inmediatamente en disco.
     *
     * @param usuario     usuario activo
     * @param nombreNuevo nombre nuevo
     * @return el usuario actualizado (los objetos Usuario son inmutables: es uno nuevo)
     * @throws una.social.excepciones.ExcepcionValidacion   si el nombre no es válido
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    Usuario cambiarNombre(Usuario usuario, String nombreNuevo);

    /**
     * Cambia la contraseña: verifica la actual y protege la nueva con el hasheador configurado
     * (con PBKDF2 se genera una sal NUEVA).
     *
     * @param usuario      usuario activo
     * @param actual       contraseña actual
     * @param nueva        contraseña nueva
     * @param confirmacion repetición de la nueva
     * @throws una.social.excepciones.ExcepcionValidacion   si la actual es incorrecta o la nueva no cumple las reglas
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void cambiarContrasena(Usuario usuario, char[] actual, char[] nueva, char[] confirmacion);
}
