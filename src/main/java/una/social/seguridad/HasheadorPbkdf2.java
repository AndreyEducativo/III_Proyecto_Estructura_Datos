/*
 * HasheadorPbkdf2.java
 *
 * Qué hace: protege contraseñas con PBKDF2WithHmacSHA256 + sal aleatoria por usuario, y las
 * verifica comparando en tiempo constante.
 * Rol en la arquitectura: implementación de HasheadorContrasenas que Main crea e inyecta en el
 * GestorUsuarios.
 * Principio SOLID: SRP (solo criptografía de contraseñas), LSP (cumple el contrato de la
 * interfaz), DIP (recibe por constructor el generador aleatorio y las iteraciones).
 * Colabora con: CredencialProtegida, GestorUsuarios.
 */
package una.social.seguridad;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hasheador de contraseñas basado en PBKDF2.
 *
 * <p><b>¿Por qué no guardar la contraseña, ni un simple SHA-256 de ella? (explicado simple)</b></p>
 * <ul>
 *   <li>Si alguien roba usuarios.txt y las contraseñas están en claro, entra a todas las cuentas.</li>
 *   <li>Un hash simple (SHA-256) es MUY rápido: un atacante puede probar miles de millones de
 *       contraseñas por segundo hasta dar con la que produce ese hash.</li>
 *   <li><b>PBKDF2</b> aplica el hash cientos de miles de veces seguidas ("iteraciones"). Para el
 *       usuario legítimo eso tarda una fracción de segundo una vez; para un atacante que quiere
 *       probar millones de contraseñas, se vuelve cientos de miles de veces más lento.</li>
 *   <li>La <b>sal</b> es un valor aleatorio distinto para cada usuario que se mezcla con la
 *       contraseña. Gracias a ella, dos personas con la misma contraseña tienen hashes distintos,
 *       y las "tablas" de hashes precalculados del atacante no sirven.</li>
 * </ul>
 */
public final class HasheadorPbkdf2 implements HasheadorContrasenas {

    /**
     * Iteraciones para uso real. 310.000 es el valor que recomendó OWASP para
     * PBKDF2-HMAC-SHA256: suficientemente lento para un atacante y aceptable (~0,2-0,5 s) para un
     * login. IMPORTANTE: si se cambia, los hashes ya guardados dejan de coincidir.
     */
    public static final int ITERACIONES_PRODUCCION = 310_000;

    /** Algoritmo de derivación incluido en el JDK (javax.crypto); no requiere librerías externas. */
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";

    /** Largo del hash resultante, en bits (256 bits = 32 bytes). */
    private static final int LARGO_HASH_BITS = 256;

    /** Largo de la sal, en bytes (16 bytes = 128 bits, el mínimo recomendado). */
    private static final int LARGO_SAL_BYTES = 16;

    /** Cantidad de iteraciones con la que trabaja esta instancia. */
    private final int iteraciones;

    /** Generador de números aleatorios criptográficamente seguro, usado para las sales. */
    private final SecureRandom aleatorio;

    /**
     * Crea el hasheador.
     *
     * @param iteraciones número de iteraciones (en producción, {@link #ITERACIONES_PRODUCCION};
     *                    en pruebas puede ser menor para que corran rápido)
     * @param aleatorio   generador seguro para las sales ({@code new SecureRandom()})
     * @throws IllegalArgumentException si las iteraciones no son positivas
     */
    public HasheadorPbkdf2(int iteraciones, SecureRandom aleatorio) {
        // Sin iteraciones no hay hash: es un error de configuración.
        if (iteraciones <= 0) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        // Más iteraciones = más lento para un atacante que pruebe contraseñas.
        this.iteraciones = iteraciones;
        this.aleatorio = Objects.requireNonNull(aleatorio, "aleatorio");
    }

    /**
     * Protege una contraseña nueva: genera una sal aleatoria y calcula el hash PBKDF2. (No se usa
     * en la versión actual, que guarda las contraseñas en texto plano para la defensa.)
     *
     * @param contrasena contraseña escrita
     * @return sal y hash en Base64, listos para guardar en el archivo
     */
    @Override
    public CredencialProtegida proteger(char[] contrasena) {
        // SecureRandom (y no Random) porque la sal debe ser impredecible: Random es predecible si
        // se conoce su semilla.
        byte[] sal = new byte[LARGO_SAL_BYTES];
        aleatorio.nextBytes(sal);
        // Calcula el hash de la contraseña con esa sal.
        byte[] hash = derivar(contrasena, sal);
        // Base64 convierte los bytes en texto para poder escribirlos en usuarios.txt.
        Base64.Encoder base64 = Base64.getEncoder();
        CredencialProtegida credencial = new CredencialProtegida(base64.encodeToString(sal), base64.encodeToString(hash));
        // El hash en bytes ya no se necesita; se borra de la memoria.
        Arrays.fill(hash, (byte) 0);
        return credencial;
    }

    /**
     * Verifica una contraseña: recalcula el hash con la misma sal guardada y lo compara con el
     * hash guardado.
     *
     * @param contrasena contraseña escrita en el login
     * @param guardada   sal y hash leídos del archivo
     * @return {@code true} si la contraseña es correcta
     */
    @Override
    public boolean verificar(char[] contrasena, CredencialProtegida guardada) {
        byte[] sal;
        byte[] esperado;
        // Se espera IllegalArgumentException si la sal o el hash guardados no son Base64 válido.
        // El repositorio ya valida eso al leer el archivo, pero por seguridad, ante un dato raro
        // se responde "contraseña incorrecta" en vez de lanzar un error: el usuario ve el mensaje
        // genérico de login fallido.
        try {
            sal = Base64.getDecoder().decode(guardada.salBase64());
            esperado = Base64.getDecoder().decode(guardada.hashBase64());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        // Mismo cálculo que al registrarse: misma contraseña + misma sal = mismo hash.
        byte[] calculado = derivar(contrasena, sal);
        // MessageDigest.isEqual compara en TIEMPO CONSTANTE: revisa todos los bytes aunque el
        // primero ya sea distinto. Una comparación normal (equals) se detiene en el primer byte
        // diferente, y midiendo cuánto tarda un atacante podría ir adivinando el hash byte a byte.
        boolean coincide = MessageDigest.isEqual(calculado, esperado);
        Arrays.fill(calculado, (byte) 0);
        return coincide;
    }

    /**
     * Aplica PBKDF2 a la contraseña con la sal dada.
     *
     * @param contrasena contraseña en claro (no se modifica)
     * @param sal        sal en bytes
     * @return hash derivado de {@link #LARGO_HASH_BITS} bits
     */
    private byte[] derivar(char[] contrasena, byte[] sal) {
        // Invariante interna: quien llama (este mismo objeto) siempre pasa arreglos no nulos.
        assert contrasena != null && sal != null : "contraseña y sal no pueden ser nulas";
        // PBEKeySpec guarda internamente una COPIA de la contraseña; por eso se llama a
        // clearPassword() en el finally, para borrar también esa copia pase lo que pase.
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, LARGO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException ex) {
            // Todo JDK 8+ incluye PBKDF2WithHmacSHA256, así que esto indica un entorno roto. No se
            // puede seguir de forma segura (no hay con qué proteger contraseñas), por eso se
            // lanza un error de estado que el manejador global mostrará y auditará.
            throw new IllegalStateException("El JDK no soporta " + ALGORITMO, ex);
        } finally {
            especificacion.clearPassword();
        }
    }
}
