/*
 * BordeRedondeado.java
 *
 * Qué hace: borde con esquinas redondeadas que cambia de color según el estado del campo:
 * gris en reposo, azul (más grueso) con el foco y rojo si tiene un error.
 * Rol en la arquitectura: lo usan los campos del tema para dar "foco visible" y marcar errores.
 * Principio SOLID: SRP (solo dibuja el borde) y OCP (extiende AbstractBorder de Swing sin
 * modificarlo).
 * Colabora con: CampoTextoTema, CampoContrasenaTema, TemaUi.
 */
package una.social.ui.tema;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;

/**
 * Borde redondeado sensible al foco y al estado de error.
 */
final class BordeRedondeado extends AbstractBorder {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Margen interno vertical (espacio entre el borde y el texto). */
    private static final int RELLENO_VERTICAL = 9;

    /** Margen interno horizontal. */
    private static final int RELLENO_HORIZONTAL = 12;

    /** Grosor del borde en reposo. */
    private static final float GROSOR_NORMAL = 1f;

    /** Grosor del borde con foco: más grueso para que el foco se vea con claridad. */
    private static final float GROSOR_FOCO = 2f;

    /** {@inheritDoc} */
    @Override
    public void paintBorder(Component componente, Graphics g, int x, int y, int ancho, int alto) {
        Graphics2D g2 = PintorTema.prepararGraficos(g);
        boolean conFoco = componente.hasFocus();
        g2.setColor(colorSegunEstado(componente, conFoco));
        g2.setStroke(new BasicStroke(conFoco ? GROSOR_FOCO : GROSOR_NORMAL));
        // Se resta 1 (o el grosor) para que la línea quede DENTRO del componente y no se corte.
        int ajuste = conFoco ? 2 : 1;
        g2.drawRoundRect(x + ajuste / 2, y + ajuste / 2, ancho - ajuste, alto - ajuste, TemaUi.RADIO, TemaUi.RADIO);
        g2.dispose();
    }

    /**
     * Elige el color del borde: el error tiene prioridad, luego el foco, luego el reposo.
     *
     * @param componente componente que tiene el borde
     * @param conFoco    si tiene el foco del teclado
     * @return color a usar
     */
    private static Color colorSegunEstado(Component componente, boolean conFoco) {
        boolean conError = componente instanceof JComponent
                && Boolean.TRUE.equals(((JComponent) componente).getClientProperty(TemaUi.PROPIEDAD_ERROR));
        if (conError) {
            return TemaUi.ERROR;
        }
        return conFoco ? TemaUi.AZUL_UNA : TemaUi.BORDE;
    }

    /** {@inheritDoc} */
    @Override
    public Insets getBorderInsets(Component componente) {
        return new Insets(RELLENO_VERTICAL, RELLENO_HORIZONTAL, RELLENO_VERTICAL, RELLENO_HORIZONTAL);
    }

    /** {@inheritDoc} */
    @Override
    public Insets getBorderInsets(Component componente, Insets margenes) {
        margenes.set(RELLENO_VERTICAL, RELLENO_HORIZONTAL, RELLENO_VERTICAL, RELLENO_HORIZONTAL);
        return margenes;
    }
}
