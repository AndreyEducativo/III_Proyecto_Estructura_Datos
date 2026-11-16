/*
 * CampoTextoTema.java
 *
 * Qué hace: campo de texto con esquinas redondeadas, foco visible y texto de ayuda gris
 * ("placeholder") mientras está vacío.
 * Rol en la arquitectura: componente del tema; se crea con TemaUi.crearCampoTexto(...).
 * Principio SOLID: OCP (extiende JTextField sin modificarlo; la clase es final para que nadie
 * altere su construcción) y LSP (se puede usar en cualquier lugar donde se espere un JTextField).
 * Colabora con: TemaUi, PintorTema, BordeRedondeado.
 */
package una.social.ui.tema;

import java.awt.Graphics;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * Campo de texto del tema.
 */
public final class CampoTextoTema extends JTextField {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Texto de ayuda que se pinta cuando el campo está vacío. */
    private final String marcador;

    /**
     * Crea el campo.
     *
     * @param marcador texto de ayuda (por ejemplo "ej. ana")
     */
    public CampoTextoTema(String marcador) {
        this.marcador = marcador;
        configurarEstilo(this);
    }

    /**
     * Aplica fuente, colores, borde y repintado al cambiar el foco. Es estático y de paquete para
     * que CampoContrasenaTema reutilice exactamente la misma configuración.
     *
     * @param campo campo a configurar
     */
    static void configurarEstilo(JTextComponent campo) {
        // Al ganar o perder el foco se repinta para que el borde cambie de color (foco visible).
        PintorTema.repintarAlCambiarFoco(campo);
        campo.setFont(TemaUi.FUENTE_NORMAL);
        campo.setForeground(TemaUi.TEXTO);
        campo.setBackground(TemaUi.SUPERFICIE);
        campo.setCaretColor(TemaUi.AZUL_UNA);
        campo.setBorder(new BordeRedondeado());
        // No opaco: el fondo lo pinta PintorTema con esquinas redondeadas.
        campo.setOpaque(false);
    }

    /**
     * Pinta el fondo redondeado, el texto normal y, si está vacío, el texto de ayuda.
     *
     * @param g contexto gráfico
     */
    @Override
    protected void paintComponent(Graphics g) {
        PintorTema.pintarFondoRedondeado(g, this);
        super.paintComponent(g);
        PintorTema.pintarMarcadorSiVacio(g, this, marcador);
    }
}
