/*
 * HasheadorContrasenas.java
 *
 * Qué hace: contrato para proteger contraseñas (convertirlas en sal + hash) y verificarlas.
 * Rol en la arquitectura: el GestorUsuarios depende de esta interfaz; la implementación real es
 * HasheadorPbkdf2. En las pruebas se usa la misma clase con menos iteraciones para que corran
 * rápido, sin cambiar el servicio.
 * Principio SOLID: DIP (el servicio no conoce el algoritmo), OCP (cambiar a otro algoritmo es
 * crear otra implementación), ISP (solo dos métodos).
 * Colabora con: HasheadorPbkdf2, CredencialProtegida, GestorUsuarios.
 */
package una.social.seguridad;

/**
 * Protección y verificación de contraseñas.
 *
 * <p>Las contraseñas se reciben como {@code char[]} y NO como {@code String}: un String no se
 * puede borrar de la memoria (es inmutable y queda ahí hasta que el recolector de basura lo
 * elimine), mientras que un arreglo se puede sobrescribir con ceros apenas se termina de usar.
 * Quien llama es el dueño del arreglo y debe borrarlo con {@code Arrays.fill} al terminar.</p>
 */
public interface HasheadorContrasenas {

    /**
     * Genera una sal aleatoria nueva y calcula el hash de la contraseña.
     *
     * @param contrasena contraseña en claro (no se modifica ni se guarda)
     * @return sal y hash en Base64
     */
    CredencialProtegida proteger(char[] contrasena);

    /**
     * Verifica si una contraseña corresponde a una credencial guardada. La comparación debe ser
     * en TIEMPO CONSTANTE para no filtrar información por cuánto tarda.
     *
     * @param contrasena contraseña escrita en el login (no se modifica)
     * @param guardada   sal y hash guardados del usuario
     * @return {@code true} si la contraseña es correcta
     */
    boolean verificar(char[] contrasena, CredencialProtegida guardada);
}
