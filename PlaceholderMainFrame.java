/**
 * Ventana principal temporal para la integración con Sebastián.
 *
 * <p>Es una implementación mínima del contrato MainWindowFactory que permite abrir la
 * aplicación y mostrar un estado de bienvenida mientras Sebastián reemplaza este placeholder
 * por su MainFrame real.</p>
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import una.social.model.User;
import una.social.ui.theme.UiTheme;

public class PlaceholderMainFrame extends JFrame {
    public PlaceholderMainFrame(User usuarioActivo, Runnable onLogout) {
        setTitle("Social UNA");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));

        JLabel welcome = new JLabel("Bienvenido, " + usuarioActivo.getNombre(), SwingConstants.CENTER);
        welcome.setFont(UiTheme.createTitle("Bienvenido").getFont());
        welcome.setForeground(UiTheme.PRIMARY);

        JButton logoutButton = UiTheme.createSecondaryButton("Cerrar sesión");
        logoutButton.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
            dispose();
        });

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UiTheme.BACKGROUND);
        center.add(welcome, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(UiTheme.BACKGROUND);
        footer.add(logoutButton);

        add(center, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }
}
