/*
 * HasheadorTextoPlano.java
 *
 * Qué hace: "protege" la contraseña dejándola tal cual (texto plano) y la verifica comparándola
 * directamente. NO aplica ningún hash.
 * Rol en la arquitectura: implementación de HasheadorContrasenas que usa la aplicación para que la
 * contraseña se vea en data/usuarios.txt. Decisión tomada a propósito porque el proyecto es solo
 * académico y así se puede mostrar el inicio de sesión durante la defensa. Volver a la versión
 * segura es cambiar una línea en EnsambladorServicios (usar HasheadorPbkdf2).
 * Principio SOLID: OCP y LSP (se cambia el comportamiento creando otra implementación de la
 * interfaz, sin modificar GestorUsuarios), DIP.
 * Colabora con: CredencialProtegida, GestorUsuarios, EnsambladorServicios.
 */
package una.social.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Contraseñas guardadas en texto plano (solo para fines académicos).
 *
 * <p>La contraseña se guarda en el campo "hash" de {@link CredencialProtegida} y la sal queda
 * vacía, porque no se usa.</p>
 */
public final class HasheadorTextoPlano implements HasheadorContrasenas {

    /** Sal que se guarda: vacía, porque sin hash no hace falta. */
    private static final String SIN_SAL = "";

    /**
     * "Protege" la contraseña dejándola igual: es lo que se escribe en la columna de contraseña de
     * data/usuarios.txt al registrarse o cambiar la contraseña.
     *
     * @param contrasena contraseña escrita
     * @return credencial con la sal vacía y la contraseña tal cual
     */
    @Override
    public CredencialProtegida proteger(char[] contrasena) {
        // Sin hash: la contraseña se guarda como texto para poder mostrarla en la defensa.
        return new CredencialProtegida(SIN_SAL, new String(contrasena));
    }

    /**
     * Verifica el login comparando la contraseña escrita con la guardada en usuarios.txt.
     *
     * @param contrasena contraseña escrita en el login
     * @param guardada   credencial leída del archivo (la contraseña está en el campo hash)
     * @return {@code true} si son iguales
     */
    @Override
    public boolean verificar(char[] contrasena, CredencialProtegida guardada) {
        // Se pasan las dos a bytes para compararlas.
        byte[] escrita = new String(contrasena).getBytes(StandardCharsets.UTF_8);
        byte[] esperada = guardada.hashBase64().getBytes(StandardCharsets.UTF_8);
        // Comparación en tiempo constante, igual que en HasheadorPbkdf2.
        return MessageDigest.isEqual(escrita, esperada);
    }
}
