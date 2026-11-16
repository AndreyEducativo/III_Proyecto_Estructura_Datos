import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import una.social.audit.AuditIntegrityVerifier;
import una.social.audit.AuditLevel;
import una.social.audit.FileAuditLogger;

class AuditLoggerTest {
    @Test
    void loggerShouldAppendAndVerifyIntegrity() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-audit");
        Path logFile = tempDir.resolve("auditoria.log");
        FileAuditLogger logger = new FileAuditLogger(logFile);

        logger.log(AuditLevel.INFO, "SISTEMA", "APP_START", "inicio");
        logger.log(AuditLevel.WARN, "ana", "LOGIN_FAIL", "usuario o contraseña incorrectos");

        AuditIntegrityVerifier verifier = new AuditIntegrityVerifier(logFile);
        assertTrue(verifier.verify());
        assertTrue(Files.readAllLines(logFile).size() >= 2);
    }
}
