/*
 * CampoContrasenaTema.java
 *
 * Qué hace: campo de contraseña (muestra puntos) con el estilo del tema y un método para
 * mostrar u ocultar lo escrito (casilla "Mostrar contraseña").
 * Rol en la arquitectura: componente del tema; se crea con TemaUi.crearCampoContrasena(...).
 * Principio SOLID: OCP y LSP (extiende JPasswordField y puede usarse donde se espere uno).
 * Colabora con: TemaUi, PintorTema, CampoTextoTema (reutiliza su configuración de estilo).
 */
package una.social.ui.tema;

import java.awt.Graphics;
import javax.swing.JPasswordField;

/**
 * Campo de contraseña del tema.
 *
 * <p>Recordatorio de seguridad: la contraseña se obtiene con {@link #getPassword()} (devuelve
 * un {@code char[]} que se puede borrar), nunca con {@code getText()}.</p>
 */
public final class CampoContrasenaTema extends JPasswordField {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Carácter "nulo" que, usado como eco, hace visible el texto. */
    private static final char SIN_OCULTAR = (char) 0;

    /** Texto de ayuda que se pinta cuando el campo está vacío. */
    private final String marcador;

    /** Carácter de eco original del aspecto visual (normalmente un punto); se guarda para restaurarlo. */
    private final char ecoOriginal;

    /**
     * Crea el campo.
     *
     * @param marcador texto de ayuda (por ejemplo "Tu contraseña")
     */
    public CampoContrasenaTema(String marcador) {
        this.marcador = marcador;
        this.ecoOriginal = getEchoChar();
        // Misma configuración visual que el campo de texto (incluye el foco visible).
        CampoTextoTema.configurarEstilo(this);
    }

    /**
     * Muestra u oculta la contraseña escrita.
     *
     * @param visible {@code true} para ver el texto; {@code false} para ver puntos
     */
    public void setContrasenaVisible(boolean visible) {
        setEchoChar(visible ? SIN_OCULTAR : ecoOriginal);
    }

    /**
     * Pinta el fondo redondeado, los puntos y, si está vacío, el texto de ayuda.
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
