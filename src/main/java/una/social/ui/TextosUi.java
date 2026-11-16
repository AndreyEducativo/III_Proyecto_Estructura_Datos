/*
 * TextosUi.java
 *
 * Qué hace: crea etiquetas y bloques de texto SEGUROS para mostrar datos escritos por usuarios
 * (nombres, publicaciones) y da formato a las fechas.
 * Rol en la arquitectura: utilidad de la UI del módulo social (muro, buscador, amigos).
 * Principio SOLID: SRP (todo lo relativo a mostrar texto de usuarios está en un solo lugar) y
 * evita duplicar código en cada panel.
 * Colabora con: PanelMuro, PanelBuscador, PanelAmigos, VentanaPrincipal, TemaUi.
 */
package una.social.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;
import una.social.ui.tema.TemaUi;

/**
 * Utilidades de texto para la UI.
 *
 * <p><b>¿Por qué "texto seguro"?</b> En Swing, si el texto de un JLabel empieza con
 * {@code <html>}, se interpreta como HTML (y podría, por ejemplo, cargar imágenes de Internet).
 * Como los nombres y publicaciones los escriben los usuarios, se desactiva esa función con la
 * propiedad {@code "html.disable"} y el texto se muestra tal cual, letra por letra.</p>
 */
final class TextosUi {

    /** Propiedad de Swing que desactiva la interpretación de HTML en un componente. */
    private static final String DESACTIVAR_HTML = "html.disable";

    /** Formato de fecha y hora para el muro (hora local del equipo). */
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Formato de solo fecha. */
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Margen interno (vertical) de los botones tipo enlace. */
    private static final int MARGEN_BOTON_ENLACE = 4;

    /** Clase de utilidades: no se instancia. */
    private TextosUi() {
    }

    /**
     * Crea una etiqueta de texto plano (sin interpretar HTML).
     *
     * @param texto  texto a mostrar
     * @param fuente fuente
     * @param color  color del texto
     * @return etiqueta segura
     */
    static JLabel etiquetaPlana(String texto, Font fuente, Color color) {
        JLabel etiqueta = new JLabel();
        etiqueta.putClientProperty(DESACTIVAR_HTML, Boolean.TRUE);
        etiqueta.setText(texto);
        etiqueta.setFont(fuente);
        etiqueta.setForeground(color);
        return etiqueta;
    }

    /**
     * Crea una etiqueta pequeña en gris (texto secundario: contadores, fechas, ayudas cortas).
     * A diferencia de {@code TemaUi.crearAyuda}, no tiene ancho fijo, así ocupa solo lo necesario.
     *
     * @param texto texto a mostrar
     * @return etiqueta segura
     */
    static JLabel textoSecundario(String texto) {
        return etiquetaPlana(texto, TemaUi.FUENTE_PEQUENA, TemaUi.TEXTO_SECUNDARIO);
    }

    /**
     * Crea un "enlace": una etiqueta de texto plano en la que se puede hacer clic (por ejemplo, el
     * nombre de un usuario para abrir su perfil). Al pasar el mouse se subraya y el cursor cambia a
     * una mano, como en una página web.
     *
     * @param texto      texto a mostrar
     * @param fuente     fuente
     * @param color      color del texto
     * @param alHacerClic acción al hacer clic
     * @return etiqueta clicable
     */
    static JLabel enlace(String texto, Font fuente, Color color, Runnable alHacerClic) {
        JLabel enlace = etiquetaPlana(texto, fuente, color);
        Font subrayada = fuente.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
        enlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enlace.setToolTipText("Ver perfil");
        enlace.addMouseListener(new MouseAdapter() {
            /** Clic: ejecuta la acción. */
            @Override
            public void mouseClicked(MouseEvent e) {
                alHacerClic.run();
            }

            /** Mouse encima: subrayar. */
            @Override
            public void mouseEntered(MouseEvent e) {
                enlace.setFont(subrayada);
            }

            /** Mouse fuera: quitar el subrayado. */
            @Override
            public void mouseExited(MouseEvent e) {
                enlace.setFont(fuente);
            }
        });
        return enlace;
    }

    /**
     * Crea un botón pequeño con aspecto de enlace (texto azul, sin fondo), para las acciones de una
     * publicación: "Me gusta", "Comentarios", "Compartir"... Es un JButton de verdad, así que
     * también se puede usar con el teclado (TAB + barra espaciadora).
     *
     * @param texto texto del botón
     * @return botón compacto
     */
    static JButton botonEnlace(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(TemaUi.FUENTE_NEGRITA.deriveFont(TemaUi.FUENTE_PEQUENA.getSize2D()));
        boton.setForeground(TemaUi.AZUL_UNA);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setOpaque(false);
        boton.setBorder(BorderFactory.createEmptyBorder(MARGEN_BOTON_ENLACE, MARGEN_BOTON_ENLACE * 2,
                MARGEN_BOTON_ENLACE, MARGEN_BOTON_ENLACE * 2));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    /**
     * Crea un bloque de texto de solo lectura con salto de línea automático (para el contenido
     * de una publicación). Se usa JTextArea porque respeta los saltos de línea del mensaje y nunca
     * interpreta HTML.
     *
     * @param texto texto a mostrar
     * @return área de texto no editable, transparente y sin borde
     */
    static JTextArea bloqueDeTexto(String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(TemaUi.FUENTE_NORMAL);
        area.setForeground(TemaUi.TEXTO);
        area.setBorder(BorderFactory.createEmptyBorder());
        // No recibe el foco con TAB: es solo para leer, así la navegación por teclado no se detiene aquí.
        area.setFocusable(false);
        // Por defecto el cursor (caret) de un JTextArea se mueve al final del texto y Swing
        // desplaza la vista para mostrarlo: en una lista eso haría que el muro "salte" hacia abajo.
        // NEVER_UPDATE evita ese desplazamiento automático.
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }

    /**
     * @param instante momento a mostrar
     * @return fecha y hora local, por ejemplo "15/11/2026 18:30"
     */
    static String fechaHora(Instant instante) {
        return FORMATO_FECHA_HORA.format(instante.atZone(ZoneId.systemDefault()));
    }

    /**
     * @param instante momento a mostrar
     * @return fecha local, por ejemplo "15/11/2026"
     */
    static String fecha(Instant instante) {
        return FORMATO_FECHA.format(instante.atZone(ZoneId.systemDefault()));
    }
}
