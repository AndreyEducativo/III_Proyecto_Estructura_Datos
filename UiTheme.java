/**
 * Tema visual común para la interfaz del proyecto.
 *
 * <p>Centraliza la paleta de colores y los estilos de los componentes para mantener una UI
 * coherente, moderna y reutilizable por Sebastián y por las pantallas de Andrey.</p>
 */
package una.social.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.Border;

public final class UiTheme {
    public static final Color PRIMARY = new Color(13, 71, 161);
    public static final Color SECONDARY = new Color(198, 40, 40);
    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color PANEL = new Color(255, 255, 255);
    public static final Color TEXT = new Color(33, 37, 41);
    public static final Color MUTED = new Color(108, 117, 125);
    public static final Color SUCCESS = new Color(40, 167, 69);
    public static final Color ERROR = new Color(220, 53, 69);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Border DEFAULT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(204, 214, 224), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10));

    private UiTheme() {
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(LABEL_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(SECONDARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(LABEL_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return button;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(LABEL_FONT);
        field.setBorder(DEFAULT_BORDER);
        field.setBackground(PANEL);
        return field;
    }

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel createTitle(String text) {
        JLabel title = new JLabel(text, JLabel.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(PRIMARY);
        return title;
    }
}
