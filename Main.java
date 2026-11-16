/**
 * Punto de entrada principal de la aplicación Social UNA.
 *
 * <p>Este componente inicializa la persistencia, crea la auditoría, verifica la integridad del
 * archivo de log y abre la pantalla de login. La lógica de la ventana principal queda detrás del
 * contrato MainWindowFactory para que Sebastián pueda reemplazarla sin tocar la lógica.</p>
 */
package una.social;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import una.social.audit.AuditIntegrityVerifier;
import una.social.audit.AuditLevel;
import una.social.audit.FileAuditLogger;
import una.social.model.User;
import una.social.persistence.TextFileUserRepository;
import una.social.service.UserService;
import una.social.session.SessionContext;
import una.social.ui.LoginFrame;
import una.social.ui.PlaceholderMainFrame;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Excepción no controlada: " + throwable.getMessage());
            JOptionPane.showMessageDialog(null, "La aplicación encontró un error y se cerrará de forma segura.", "Error crítico", JOptionPane.ERROR_MESSAGE);
        });

        Path dataDir = Path.of("data");
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ex) {
            System.err.println("No se pudo crear la carpeta data: " + ex.getMessage());
        }

        Path usuariosPath = dataDir.resolve("usuarios.txt");
        Path auditoriaPath = dataDir.resolve("auditoria.log");
        FileAuditLogger auditLogger = new FileAuditLogger(auditoriaPath);
        try {
            AuditIntegrityVerifier verifier = new AuditIntegrityVerifier(auditoriaPath);
            boolean integrity = verifier.verify();
            if (!integrity) {
                JOptionPane.showMessageDialog(null, "La auditoría tiene una línea corrupta; la aplicación continuará, pero se recomienda revisar el archivo.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
            auditLogger.log(AuditLevel.INFO, "SISTEMA", "APP_START", "Inicio de la aplicación Social UNA");
        } catch (Exception ex) {
            System.err.println("No fue posible verificar la auditoría: " + ex.getMessage());
        }

        SeedDataTool.generate(dataDir);
        TextFileUserRepository repository = new TextFileUserRepository(usuariosPath);
        UserService userService = new UserService(repository);
        SessionContext sessionContext = new SessionContext();
        MainWindowFactory factory = (usuarioActivo, onLogout) -> new PlaceholderMainFrame(usuarioActivo, onLogout);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("No se pudo cargar el look and feel: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(userService, sessionContext, factory);
            loginFrame.setVisible(true);
        });
    }
}
