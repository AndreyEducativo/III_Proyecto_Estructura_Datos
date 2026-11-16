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

    /** {@inheritDoc} */
    @Override
    public CredencialProtegida proteger(char[] contrasena) {
        return new CredencialProtegida(SIN_SAL, new String(contrasena));
    }

    /** {@inheritDoc} */
    @Override
    public boolean verificar(char[] contrasena, CredencialProtegida guardada) {
        byte[] escrita = new String(contrasena).getBytes(StandardCharsets.UTF_8);
        byte[] esperada = guardada.hashBase64().getBytes(StandardCharsets.UTF_8);
        // Comparación en tiempo constante, igual que en HasheadorPbkdf2.
        return MessageDigest.isEqual(escrita, esperada);
    }
}
