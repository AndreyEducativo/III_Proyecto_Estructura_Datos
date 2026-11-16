/**
 * Ventana de registro del usuario en Social UNA.
 *
 * <p>Permite registrar un nuevo usuario con validación de campos, confirmación de contraseña,
 * y redirección de vuelta al login tras un registro correcto.</p>
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import una.social.MainWindowFactory;
import una.social.model.User;
import una.social.service.UserService;
import una.social.session.SessionContext;
import una.social.ui.theme.UiTheme;

public class RegisterFrame extends JFrame {
    private final UserService userService;
    private final SessionContext sessionContext;
    private final LoginFrame loginFrame;
    private final MainWindowFactory mainWindowFactory;

    public RegisterFrame(UserService userService, SessionContext sessionContext, LoginFrame loginFrame, MainWindowFactory mainWindowFactory) {
        this.userService = userService;
        this.sessionContext = sessionContext;
        this.loginFrame = loginFrame;
        this.mainWindowFactory = mainWindowFactory;

        setTitle("Registro - Social UNA");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(420, 400));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel title = UiTheme.createTitle("Crear cuenta");
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JTextField nombreField = UiTheme.createTextField();
        JTextField usuarioField = UiTheme.createTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        JCheckBox showPasswordCheck = new JCheckBox("Mostrar contraseñas");

        addField(form, gbc, 0, "Nombre");
        gbc.gridy = 1;
        form.add(nombreField, gbc);
        addField(form, gbc, 2, "Usuario");
        gbc.gridy = 3;
        form.add(usuarioField, gbc);
        addField(form, gbc, 4, "Contraseña");
        gbc.gridy = 5;
        passwordField.setEchoChar('*');
        form.add(passwordField, gbc);
        addField(form, gbc, 6, "Confirmar contraseña");
        gbc.gridy = 7;
        confirmField.setEchoChar('*');
        form.add(confirmField, gbc);
        gbc.gridy = 8;
        form.add(showPasswordCheck, gbc);

        showPasswordCheck.addActionListener(e -> {
            char echo = showPasswordCheck.isSelected() ? '\0' : '*';
            passwordField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setBackground(UiTheme.BACKGROUND);
        JButton registerButton = UiTheme.createPrimaryButton("Crear cuenta");
        JButton backButton = UiTheme.createSecondaryButton("Volver al login");

        registerButton.addActionListener(e -> {
            char[] passwordChars = passwordField.getPassword();
            char[] confirmChars = confirmField.getPassword();
            try {
                User user = userService.register(
                        nombreField.getText(),
                        usuarioField.getText(),
                        new String(passwordChars),
                        new String(confirmChars));
                JOptionPane.showMessageDialog(this, "Usuario registrado con éxito. Su ID es: " + user.getId(), "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
                Arrays.fill(passwordChars, '\0');
                Arrays.fill(confirmChars, '\0');
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    loginFrame.setVisible(true);
                    loginFrame.toFront();
                });
            } catch (Exception ex) {
                Arrays.fill(passwordChars, '\0');
                Arrays.fill(confirmChars, '\0');
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de registro", JOptionPane.ERROR_MESSAGE);
            }
        });

        backButton.addActionListener(e -> {
            dispose();
            loginFrame.setVisible(true);
        });

        actions.add(registerButton);
        actions.add(backButton);
        root.add(actions, BorderLayout.SOUTH);

        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void addField(JPanel form, GridBagConstraints gbc, int row, String labelText) {
        gbc.gridy = row;
        JLabel label = UiTheme.createLabel(labelText);
        form.add(label, gbc);
    }
}
