/*
 * PintorTema.java
 *
 * Qué hace: rutinas de dibujo compartidas por los componentes del tema (fondo redondeado,
 * texto de ayuda "placeholder" y suavizado de bordes).
 * Rol en la arquitectura: clase interna del paquete del tema. Existe para que CampoTextoTema y
 * CampoContrasenaTema no dupliquen el mismo código de dibujo.
 * Principio SOLID: SRP (solo dibuja) y evita duplicación.
 * Colabora con: CampoTextoTema, CampoContrasenaTema, BotonTema, BordeRedondeado, TemaUi.
 */
package una.social.ui.tema;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * Utilidades de dibujo del tema.
 */
final class PintorTema {

    /** Clase de utilidades: no se instancia. */
    private PintorTema() {
    }

    /**
     * Crea una copia del contexto gráfico con suavizado (antialiasing) activado, para que las
     * curvas y el texto no se vean "dentados". Quien la recibe debe llamar a {@code dispose()}.
     *
     * @param g contexto gráfico original
     * @return copia configurada
     */
    static Graphics2D prepararGraficos(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /**
     * Hace que el componente se repinte al ganar o perder el foco del teclado, para que su
     * borde cambie de color (foco visible). Se comparte para no repetir el mismo oyente en cada
     * tipo de campo.
     *
     * @param componente componente a observar
     */
    static void repintarAlCambiarFoco(JComponent componente) {
        componente.addFocusListener(new FocusAdapter() {
            /** Al recibir el foco: repintar para mostrar el borde azul. */
            @Override
            public void focusGained(FocusEvent e) {
                componente.repaint();
            }

            /** Al perder el foco: repintar para volver al borde gris. */
            @Override
            public void focusLost(FocusEvent e) {
                componente.repaint();
            }
        });
    }

    /**
     * Pinta el fondo redondeado de un componente no opaco.
     *
     * @param g          contexto gráfico
     * @param componente componente cuyo fondo se pinta
     */
    static void pintarFondoRedondeado(Graphics g, JComponent componente) {
        Graphics2D g2 = prepararGraficos(g);
        g2.setColor(componente.getBackground());
        g2.fillRoundRect(0, 0, componente.getWidth() - 1, componente.getHeight() - 1, TemaUi.RADIO, TemaUi.RADIO);
        g2.dispose();
    }

    /**
     * Pinta el texto de ayuda gris si el campo está vacío (como el "placeholder" de una web).
     *
     * @param g        contexto gráfico
     * @param campo    campo de texto
     * @param marcador texto de ayuda
     */
    static void pintarMarcadorSiVacio(Graphics g, JTextComponent campo, String marcador) {
        // Se mira el largo del documento (y no getText()) porque en un campo de contraseña
        // getText() está desaconsejado: crearía un String con la contraseña.
        if (marcador == null || campo.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = prepararGraficos(g);
        g2.setColor(TemaUi.TEXTO_SECUNDARIO);
        g2.setFont(campo.getFont());
        Insets margenes = campo.getInsets();
        FontMetrics medidas = g2.getFontMetrics();
        int y = margenes.top + (campo.getHeight() - margenes.top - margenes.bottom - medidas.getHeight()) / 2 + medidas.getAscent();
        g2.drawString(marcador, margenes.left, y);
        g2.dispose();
    }
}
