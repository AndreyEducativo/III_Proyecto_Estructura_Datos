/*
 * AvatarUsuario.java
 *
 * Qué hace: dibuja un círculo de color con la inicial del nombre del usuario (como las fotos de
 * perfil por defecto de las redes sociales).
 * Rol en la arquitectura: componente visual del módulo social (muro, buscador, amigos, barra
 * superior).
 * Principio SOLID: SRP (solo dibuja el avatar) y OCP (extiende JComponent).
 * Colabora con: Usuario, TemaUi, PanelMuro, PanelBuscador, PanelAmigos, VentanaPrincipal.
 */
package una.social.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;
import una.social.modelo.Usuario;
import una.social.ui.tema.TemaUi;

/**
 * Avatar circular con la inicial del usuario.
 */
final class AvatarUsuario extends JComponent {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Colores posibles del círculo; cada usuario recibe siempre el mismo (según su ID). */
    private static final List<Color> COLORES = List.of(
            TemaUi.ROJO_UNA, TemaUi.AZUL_UNA, new Color(46, 125, 50), new Color(123, 31, 162),
            new Color(230, 81, 0), new Color(0, 121, 107));

    /** Proporción del tamaño de la letra respecto al diámetro. */
    private static final float PROPORCION_LETRA = 0.45f;

    /** Diámetro del círculo en píxeles. */
    private final int diametro;

    /** Inicial que se dibuja. */
    private final String inicial;

    /** Color de fondo del círculo. */
    private final Color color;

    /**
     * Crea el avatar.
     *
     * @param usuario  usuario representado
     * @param diametro diámetro en píxeles
     */
    AvatarUsuario(Usuario usuario, int diametro) {
        this.diametro = diametro;
        String nombre = usuario.getNombre().strip();
        // codePointAt maneja bien letras fuera del rango básico; se pasa a mayúscula.
        this.inicial = nombre.isEmpty() ? "?" : new String(Character.toChars(nombre.codePointAt(0))).toUpperCase();
        // floorMod evita índices negativos (hashCode puede ser negativo).
        this.color = COLORES.get(Math.floorMod(usuario.getId().hashCode(), COLORES.size()));
        Dimension tamano = new Dimension(diametro, diametro);
        setPreferredSize(tamano);
        setMinimumSize(tamano);
        setMaximumSize(tamano);
    }

    /**
     * Dibuja el círculo y la inicial centrada.
     *
     * @param g contexto gráfico
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, diametro, diametro);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(TemaUi.FAMILIA, Font.BOLD, Math.round(diametro * PROPORCION_LETRA)));
        FontMetrics medidas = g2.getFontMetrics();
        int x = (diametro - medidas.stringWidth(inicial)) / 2;
        int y = (diametro - medidas.getHeight()) / 2 + medidas.getAscent();
        g2.drawString(inicial, x, y);
        g2.dispose();
    }
}
