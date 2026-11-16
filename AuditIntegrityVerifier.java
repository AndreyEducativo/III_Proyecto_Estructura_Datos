/**
 * Verificador de integridad de la cadena de hashes de auditoría.
 *
 * <p>Recorre el archivo de auditoría y comprueba que cada línea encadena correctamente con
 * la anterior. Si una línea está corrupta, informa el número de línea pero no bloquea la
 * ejecución del programa.</p>
 */
package una.social.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public class AuditIntegrityVerifier {
    private final Path logFile;

    public AuditIntegrityVerifier(Path logFile) {
        this.logFile = logFile;
    }

    public boolean verify() {
        if (!Files.exists(logFile)) {
            return true;
        }
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            String previousHash = "0";
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index).trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length < 7) {
                    return false;
                }
                String previousField = parts[5];
                String currentHash = parts[6];
                String payload = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3] + "|" + parts[4] + "|" + previousField;
                String expectedHash = hashSha256(payload);
                if (!expectedHash.equals(currentHash)) {
                    System.err.println("El registro de auditoría está corrupto en la línea " + (index + 1));
                    return false;
                }
                previousHash = currentHash;
            }
            return true;
        } catch (IOException ex) {
            System.err.println("No se pudo verificar la integridad de la auditoría: " + ex.getMessage());
            return false;
        }
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No existe SHA-256", ex);
        }
    }
}
