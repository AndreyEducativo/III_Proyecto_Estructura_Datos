/*
 * BotonTema.java
 *
 * Qué hace: botón con esquinas redondeadas, cambio de color al pasar el mouse (hover), estado
 * presionado, estado deshabilitado y anillo de foco visible para quien navega con teclado.
 * Rol en la arquitectura: componente del tema; se crea con TemaUi.crearBotonPrimario(...) o
 * TemaUi.crearBotonSecundario(...).
 * Principio SOLID: OCP y LSP (extiende JButton sin modificarlo; se usa como cualquier JButton).
 * Colabora con: TemaUi, PintorTema.
 */
package una.social.ui.tema;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.ButtonModel;
import javax.swing.JButton;

/**
 * Botón del tema.
 *
 * <p>Se dibuja a mano (en {@link #paintComponent}) en lugar de usar el aspecto del sistema
 * operativo, para que se vea igual en Windows, Linux y macOS.</p>
 */
public final class BotonTema extends JButton {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Alto mínimo del botón (cómodo para hacer clic). */
    private static final int ALTO = 42;

    /** Margen horizontal entre el texto y el borde. */
    private static final int MARGEN_HORIZONTAL = 22;

    /** Separación entre el botón y su anillo de foco. */
    private static final int SEPARACION_FOCO = 3;

    /** Variante visual del botón. */
    public enum Estilo {
        /** Relleno rojo y texto blanco: la acción principal. */
        PRIMARIO,
        /** Fondo blanco, borde y texto azules: acción secundaria. */
        SECUNDARIO
    }

    /** Variante de este botón. */
    private final Estilo estilo;

    /**
     * Crea el botón.
     *
     * @param texto  texto del botón
     * @param estilo variante visual
     */
    public BotonTema(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setFont(TemaUi.FUENTE_NEGRITA);
        // Se desactiva el pintado estándar de Swing: el fondo, el borde y el foco los dibuja
        // paintComponent con el estilo del tema.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        // "rolloverEnabled" hace que el modelo del botón sepa cuándo el mouse está encima.
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Tamaño preferido: el ancho del texto más márgenes, con un alto mínimo cómodo.
     *
     * @return tamaño preferido
     */
    @Override
    public Dimension getPreferredSize() {
        FontMetrics medidas = getFontMetrics(getFont());
        return new Dimension(medidas.stringWidth(getText()) + MARGEN_HORIZONTAL * 2, ALTO);
    }

    /**
     * Dibuja el fondo según el estado (normal, hover, presionado, deshabilitado), el texto
     * centrado y, si tiene el foco del teclado, un anillo azul alrededor.
     *
     * @param g contexto gráfico
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = PintorTema.prepararGraficos(g);
        int ancho = getWidth();
        int alto = getHeight();
        int x = SEPARACION_FOCO;
        int anchoInterno = ancho - SEPARACION_FOCO * 2;
        int altoInterno = alto - SEPARACION_FOCO * 2;

        g2.setColor(colorFondo());
        g2.fillRoundRect(x, x, anchoInterno, altoInterno, TemaUi.RADIO, TemaUi.RADIO);
        if (estilo == Estilo.SECUNDARIO) {
            g2.setColor(isEnabled() ? TemaUi.AZUL_UNA : TemaUi.DESHABILITADO);
            g2.drawRoundRect(x, x, anchoInterno - 1, altoInterno - 1, TemaUi.RADIO, TemaUi.RADIO);
        }
        if (isFocusOwner()) {
            g2.setColor(TemaUi.AZUL_UNA);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, ancho - 3, alto - 3, TemaUi.RADIO + SEPARACION_FOCO, TemaUi.RADIO + SEPARACION_FOCO);
        }
        pintarTexto(g2, ancho, alto);
        g2.dispose();
    }

    /**
     * Elige el color de fondo según el estado del botón.
     *
     * @return color de fondo
     */
    private Color colorFondo() {
        ButtonModel modelo = getModel();
        if (estilo == Estilo.PRIMARIO) {
            if (!isEnabled()) {
                return TemaUi.DESHABILITADO;
            }
            return modelo.isPressed() || modelo.isRollover() ? TemaUi.ROJO_UNA_OSCURO : TemaUi.ROJO_UNA;
        }
        return isEnabled() && (modelo.isPressed() || modelo.isRollover()) ? TemaUi.AZUL_CLARO : TemaUi.SUPERFICIE;
    }

    /**
     * Dibuja el texto centrado.
     *
     * @param g2    contexto gráfico
     * @param ancho ancho del botón
     * @param alto  alto del botón
     */
    private void pintarTexto(Graphics2D g2, int ancho, int alto) {
        Color colorTexto;
        if (estilo == Estilo.PRIMARIO) {
            colorTexto = Color.WHITE;
        } else {
            colorTexto = isEnabled() ? TemaUi.AZUL_UNA : TemaUi.DESHABILITADO;
        }
        g2.setColor(colorTexto);
        g2.setFont(getFont());
        FontMetrics medidas = g2.getFontMetrics();
        int textoX = (ancho - medidas.stringWidth(getText())) / 2;
        int textoY = (alto - medidas.getHeight()) / 2 + medidas.getAscent();
        g2.drawString(getText(), textoX, textoY);
    }
}
