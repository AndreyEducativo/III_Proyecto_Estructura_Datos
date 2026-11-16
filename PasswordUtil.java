/**
 * Utilidades para hashing seguro de contraseñas.
 *
 * <p>Se usa PBKDF2WithHmacSHA256 con salt aleatorio para cumplir la recomendación del PDF y
 * proteger las credenciales incluso si un atacante obtiene el archivo de usuarios.</p>
 */
package una.social.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String password, String saltB64) {
        if (password == null || saltB64 == null) {
            throw new IllegalArgumentException("La contraseña y el salt son obligatorios.");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltB64), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo derivar el hash de la contraseña.", ex);
        }
    }

    public static boolean validatePassword(String password, String saltB64, String storedHash) {
        String candidateHash = hashPassword(password, saltB64);
        return MessageDigest.isEqual(candidateHash.getBytes(), storedHash.getBytes());
    }
}
