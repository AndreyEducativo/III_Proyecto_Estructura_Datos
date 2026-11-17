/*
 * ListaTarjetas.java
 *
 * Qué hace: panel vertical con desplazamiento que muestra una lista de "tarjetas" (publicaciones,
 * resultados de búsqueda o amigos) o un mensaje cuando la lista está vacía.
 * Rol en la arquitectura: componente reutilizado por PanelMuro, PanelBuscador y PanelAmigos.
 * Principio SOLID: SRP (solo acomoda tarjetas) y evita repetir el mismo código en tres paneles.
 * Colabora con: TemaUi, PanelMuro, PanelBuscador, PanelAmigos.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import una.social.ui.tema.TemaUi;

/**
 * Lista vertical de tarjetas con barra de desplazamiento.
 *
 * <p><b>Detalle técnico:</b> implementa {@link Scrollable} con
 * {@code getScrollableTracksViewportWidth() = true}: el ancho de la lista siempre es el de la
 * ventana visible, así las tarjetas se estiran a lo ancho y el texto largo de una publicación hace
 * salto de línea en vez de producir una barra horizontal.</p>
 */
final class ListaTarjetas extends JPanel implements Scrollable {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Separación vertical entre tarjetas. */
    private static final int SEPARACION = 10;

    /** Píxeles que avanza la rueda del mouse por cada "clic". */
    private static final int INCREMENTO_DESPLAZAMIENTO = 24;

    /** Margen superior del mensaje de lista vacía. */
    private static final int MARGEN_MENSAJE_VACIO = 40;

    /** Margen interno de cada tarjeta de la lista. */
    private static final int MARGEN_TARJETA = 14;

    /** Separación entre las partes de una tarjeta (avatar, texto, botón). */
    static final int ESPACIO_INTERNO = 12;

    /** Ancho de la última distribución, para detectar cambios de ancho. */
    private int ultimoAncho = -1;

    /**
     * Crea la lista vacía.
     */
    ListaTarjetas() {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(SEPARACION, SEPARACION, SEPARACION, SEPARACION));
        // Al cambiar el ANCHO, los textos con salto de línea cambian de alto; se pide redistribuir.
        // Se compara con el ancho anterior para no entrar en un ciclo (cambiar el alto también
        // dispara este evento).
        addComponentListener(new ComponentAdapter() {
            /** Redistribuye solo si cambió el ancho. */
            @Override
            public void componentResized(ComponentEvent e) {
                // Solo si el ancho cambió de verdad se recalcula la distribución.
                if (getWidth() != ultimoAncho) {
                    ultimoAncho = getWidth();
                    SwingUtilities.invokeLater(ListaTarjetas.this::revalidate);
                }
            }
        });
    }

    /**
     * Reemplaza el contenido por las tarjetas dadas, o por el mensaje si la lista está vacía.
     *
     * @param tarjetas     tarjetas a mostrar, en orden
     * @param mensajeVacio texto a mostrar si no hay tarjetas
     */
    void mostrar(List<? extends JComponent> tarjetas, String mensajeVacio) {
        assert SwingUtilities.isEventDispatchThread() : "La UI solo se modifica desde el hilo de Swing";
        removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        int fila = 0;
        if (tarjetas.isEmpty()) {
            JLabel vacio = TemaUi.crearSubtitulo(mensajeVacio);
            vacio.setHorizontalAlignment(SwingConstants.CENTER);
            c.insets = new Insets(MARGEN_MENSAJE_VACIO, 0, 0, 0);
            c.gridy = fila++;
            add(vacio, c);
        }
        for (JComponent tarjeta : tarjetas) {
            c.gridy = fila++;
            c.insets = new Insets(fila == 1 ? 0 : SEPARACION, 0, 0, 0);
            add(tarjeta, c);
        }
        // Relleno final que absorbe el espacio sobrante, así las tarjetas quedan arriba y con su
        // alto natural en lugar de estirarse.
        c.gridy = fila;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), c);
        revalidate();
        repaint();
        // Al mostrar una lista nueva se vuelve al principio (lo más reciente del muro arriba).
        // Se hace "más tarde" para que ocurra después de que Swing termine de acomodar las tarjetas.
        SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
    }

    /**
     * Crea una tarjeta vacía para una fila de la lista: panel blanco redondeado (del tema) con
     * márgenes más pequeños que los de un formulario y {@link BorderLayout}.
     *
     * @return tarjeta lista para llenar
     */
    static JPanel crearTarjeta() {
        JPanel tarjeta = TemaUi.crearTarjeta();
        tarjeta.setLayout(new BorderLayout(ESPACIO_INTERNO, ESPACIO_INTERNO / 2));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA));
        return tarjeta;
    }

    /**
     * Envuelve la lista en un panel con barra de desplazamiento vertical y sin borde.
     *
     * @return panel con desplazamiento
     */
    JScrollPane conDesplazamiento() {
        // Barra vertical solo cuando hace falta; nunca barra horizontal (el texto se ajusta al ancho).
        JScrollPane desplazamiento = new JScrollPane(this,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Sin borde, con el mismo fondo de la ventana y una rueda del ratón más rápida.
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.getViewport().setBackground(TemaUi.FONDO);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(INCREMENTO_DESPLAZAMIENTO);
        return desplazamiento;
    }

    /** @return tamaño preferido de la ventana visible: el natural de la lista */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /** @return avance por "clic" de la rueda del mouse */
    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
        return INCREMENTO_DESPLAZAMIENTO;
    }

    /** @return avance por "página" (lo visible menos un poco, para no perder contexto) */
    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
        return Math.max(INCREMENTO_DESPLAZAMIENTO, visible.height - INCREMENTO_DESPLAZAMIENTO);
    }

    /** @return siempre {@code true}: la lista toma el ancho de la ventana visible */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /** @return {@code false}: el alto lo decide el contenido (para poder desplazarse) */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
