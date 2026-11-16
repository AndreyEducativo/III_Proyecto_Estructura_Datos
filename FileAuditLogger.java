/**
 * Implementación de auditoría basada en archivos de texto.
 *
 * <p>La auditoría se escribe en append-only y cada línea se encadena con SHA-256 para detectar
 * alteraciones manuales. Este logger nunca lanza excepciones hacia afuera; cuando falla la
 * escritura, reporta el problema por System.err y continúa con la ejecución.</p>
 */
package una.social.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

public class FileAuditLogger implements AuditLogger {
    private static final String DEFAULT_ACTOR = "SISTEMA";
    private final Path logFile;
    private String lastHash = "0";

    public FileAuditLogger(Path logFile) {
        this.logFile = logFile;
        if (logFile == null) {
            throw new IllegalArgumentException("La ruta del archivo de auditoría no puede ser nula.");
        }
        ensureParentDirectory();
    }

    private void ensureParentDirectory() {
        try {
            Path parent = logFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(logFile)) {
                Files.createFile(logFile);
            }
        } catch (IOException ex) {
            System.err.println("No se pudo preparar el archivo de auditoría: " + ex.getMessage());
        }
    }

    @Override
    public synchronized void log(AuditLevel nivel, String actor, String accion, String detalle) {
        String actorValue = actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor;
        String safeAction = accion == null ? "SIN_ACCION" : accion;
        String safeDetail = detalle == null ? "" : detalle;
        String timestamp = Instant.now().toString();
        String payload = timestamp + "|" + nivel + "|" + actorValue + "|" + safeAction + "|" + safeDetail + "|" + lastHash;
        String currentHash = hashSha256(payload);
        String line = payload + "|" + currentHash;

        try {
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            lastHash = currentHash;
        } catch (IOException ex) {
            System.err.println("Fallo al escribir la auditoría: " + ex.getMessage());
        }
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("No existe el algoritmo SHA-256: " + ex.getMessage());
            return "HASH_ERROR";
        }
    }
}
