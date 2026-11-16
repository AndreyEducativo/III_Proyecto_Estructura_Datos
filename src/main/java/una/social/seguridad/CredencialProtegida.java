/*
 * CredencialProtegida.java
 *
 * Qué hace: agrupa la sal y el hash de una contraseña (ambos en Base64).
 * Rol en la arquitectura: es lo que devuelve el hasheador al proteger una contraseña y lo que
 * recibe para verificarla. Nunca contiene la contraseña en claro.
 * Principio SOLID: SRP (solo transporta dos valores; record inmutable).
 * Colabora con: HasheadorContrasenas, HasheadorPbkdf2, GestorUsuarios.
 */
package una.social.seguridad;

/**
 * Sal + hash de una contraseña, codificados en Base64.
 *
 * @param salBase64  sal aleatoria en Base64
 * @param hashBase64 hash derivado en Base64
 */
public record CredencialProtegida(String salBase64, String hashBase64) {

    /**
     * Se sobrescribe para que, si alguien imprime este objeto por error, no aparezcan la sal ni
     * el hash en consola ni en la auditoría.
     *
     * @return texto sin datos sensibles
     */
    @Override
    public String toString() {
        return "CredencialProtegida[oculta]";
    }
}
