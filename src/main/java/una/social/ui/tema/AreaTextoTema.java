/*
 * AreaTextoTema.java
 *
 * Qué hace: área de texto de varias líneas con el estilo del tema (esquinas redondeadas, foco
 * visible, texto de ayuda gris y salto de línea automático).
 * Rol en la arquitectura: componente del tema que usa el PanelMuro para escribir publicaciones.
 * Reutiliza la misma configuración visual que CampoTextoTema.
 * Principio SOLID: OCP (extiende JTextArea sin modificarlo) y LSP (sirve donde se espere un
 * JTextArea).
 * Colabora con: CampoTextoTema, PintorTema, PanelMuro.
 */
package una.social.ui.tema;

import java.awt.Graphics;
import javax.swing.JTextArea;

/**
 * Área de texto del tema.
 */
public final class AreaTextoTema extends JTextArea {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Texto de ayuda que se pinta mientras el área está vacía. */
    private final String marcador;

    /**
     * Crea el área.
     *
     * @param marcador texto de ayuda (por ejemplo "¿Qué quieres compartir?")
     * @param filas    cantidad de filas visibles inicialmente
     */
    public AreaTextoTema(String marcador, int filas) {
        super(filas, 0);
        this.marcador = marcador;
        // Salto de línea automático por palabras completas, como en cualquier red social.
        setLineWrap(true);
        setWrapStyleWord(true);
        CampoTextoTema.configurarEstilo(this);
    }

    /**
     * Pinta el fondo redondeado, el texto y, si está vacía, el texto de ayuda.
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
