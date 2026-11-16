/**
 * Ventana de autenticación del sistema Social UNA.
 *
 * <p>Esta interfaz permite iniciar sesión con usuario y contraseña, abre la pantalla de
 * registro y redirige a la ventana principal tras una autenticación exitosa.</p>
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
import javax.swing.SwingWorker;
import una.social.MainWindowFactory;
import una.social.exceptions.AuthenticationException;
import una.social.model.User;
import una.social.service.UserService;
import una.social.session.SessionContext;
import una.social.ui.theme.UiTheme;

public class LoginFrame extends JFrame {
    private final UserService userService;
    private final SessionContext sessionContext;
    private final MainWindowFactory mainWindowFactory;
    private final JTextField usuarioField;
    private final JPasswordField passwordField;
    private final JCheckBox showPasswordCheckBox;

    public LoginFrame(UserService userService, SessionContext sessionContext, MainWindowFactory mainWindowFactory) {
        this.userService = userService;
        this.sessionContext = sessionContext;
        this.mainWindowFactory = mainWindowFactory;

        setTitle("Social UNA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 320));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(16, 16));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JLabel title = UiTheme.createTitle("Social UNA");
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;

        JLabel userLabel = UiTheme.createLabel("Usuario");
        form.add(userLabel, gbc);
        gbc.gridy = 1;
        usuarioField = UiTheme.createTextField();
        usuarioField.setToolTipText("Ingrese su nombre de usuario");
        form.add(usuarioField, gbc);

        gbc.gridy = 2;
        JLabel passwordLabel = UiTheme.createLabel("Contraseña");
        form.add(passwordLabel, gbc);
        gbc.gridy = 3;
        passwordField = new JPasswordField();
        passwordField.setEchoChar('*');
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(204, 214, 224), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        form.add(passwordField, gbc);

        gbc.gridy = 4;
        showPasswordCheckBox = new JCheckBox("Mostrar contraseña");
        form.add(showPasswordCheckBox, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setBackground(UiTheme.BACKGROUND);
        JButton loginButton = UiTheme.createPrimaryButton("Iniciar sesión");
        JButton registerButton = UiTheme.createSecondaryButton("Registrarse");

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> openRegister());
        showPasswordCheckBox.addActionListener(e -> passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? '\0' : '*'));

        actions.add(loginButton);
        actions.add(registerButton);
        root.add(actions, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton);
        add(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void handleLogin() {
        String username = usuarioField.getText() == null ? "" : usuarioField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        try {
            if (username.isBlank()) {
                throw new AuthenticationException("El usuario es obligatorio.");
            }
            if (passwordChars.length == 0) {
                throw new AuthenticationException("La contraseña es obligatoria.");
            }
            String passwordValue = new String(passwordChars);
            SwingWorker<User, Void> worker = new SwingWorker<>() {
                @Override
                protected User doInBackground() {
                    return userService.authenticate(username, passwordValue);
                }

                @Override
                protected void done() {
                    Arrays.fill(passwordChars, '\0');
                    try {
                        User user = get();
                        sessionContext.setCurrentUser(user);
                        JFrame mainFrame = mainWindowFactory.create(user, () -> {
                            sessionContext.clear();
                            setVisible(true);
                        });
                        mainFrame.setVisible(true);
                        dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(LoginFrame.this, "Usuario o contraseña incorrectos", "Error de acceso", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de acceso", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(passwordChars, '\0');
        }
    }

    private void openRegister() {
        new RegisterFrame(userService, sessionContext, this, mainWindowFactory).setVisible(true);
        setVisible(false);
    }
}
