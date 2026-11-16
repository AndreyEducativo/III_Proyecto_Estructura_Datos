/*
 * TemaUi.java
 *
 * Qué hace: define la identidad visual de Social UNA (colores, tipografías, márgenes) y ofrece
 * métodos de fábrica para crear componentes con estilo consistente: botones, campos, etiquetas,
 * tarjetas, logo y mensajes de error en línea.
 * Rol en la arquitectura: contrato compartido de UI. Las ventanas de Andrey y las de Sebastián
 * crean sus componentes con estos métodos para que toda la app se vea igual.
 * Principio SOLID: SRP (todo lo visual está centralizado aquí, no repartido en cada ventana) y
 * OCP (para cambiar el estilo de toda la app se cambia este archivo, no cada ventana).
 * Colabora con: BotonTema, CampoTextoTema, CampoContrasenaTema, BordeRedondeado, PintorTema, y
 * todas las ventanas.
 */
package una.social.ui.tema;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Tema visual compartido.
 *
 * <p>Paleta inspirada en los colores institucionales de la UNA: rojo como color principal de
 * acción, azul oscuro para títulos y acentos, sobre fondos claros para buena legibilidad.</p>
 */
public final class TemaUi {

    // ------------------------------------------------------------------ paleta

    /** Rojo institucional: botones principales y marca. */
    public static final Color ROJO_UNA = new Color(190, 18, 40);
    /** Rojo más oscuro para el estado "mouse encima" de los botones principales. */
    public static final Color ROJO_UNA_OSCURO = new Color(150, 12, 30);
    /** Azul institucional: títulos, enlaces y anillo de foco. */
    public static final Color AZUL_UNA = new Color(20, 52, 102);
    /** Azul muy claro para el "mouse encima" de los botones secundarios. */
    public static final Color AZUL_CLARO = new Color(232, 238, 248);
    /** Fondo general de las ventanas. */
    public static final Color FONDO = new Color(243, 245, 249);
    /** Fondo de las tarjetas y campos. */
    public static final Color SUPERFICIE = Color.WHITE;
    /** Color de los bordes en reposo. */
    public static final Color BORDE = new Color(210, 216, 225);
    /** Texto principal. */
    public static final Color TEXTO = new Color(33, 37, 41);
    /** Texto secundario (subtítulos, textos de ayuda, placeholders). */
    public static final Color TEXTO_SECUNDARIO = new Color(108, 117, 125);
    /** Rojo de los mensajes de error en línea. */
    public static final Color ERROR = new Color(198, 40, 40);
    /** Gris de los botones deshabilitados. */
    public static final Color DESHABILITADO = new Color(189, 195, 203);

    // ------------------------------------------------------------------ tipografía

    /** Familia de letra: Segoe UI si existe (Windows); si no, la sans-serif del sistema. */
    public static final String FAMILIA = elegirFamilia("Segoe UI", "Helvetica Neue", "Arial");
    /** Título grande (nombre de la app). */
    public static final Font FUENTE_TITULO = new Font(FAMILIA, Font.BOLD, 28);
    /** Subtítulos. */
    public static final Font FUENTE_SUBTITULO = new Font(FAMILIA, Font.PLAIN, 14);
    /** Texto normal, etiquetas y campos. */
    public static final Font FUENTE_NORMAL = new Font(FAMILIA, Font.PLAIN, 14);
    /** Texto de botones y etiquetas de campo. */
    public static final Font FUENTE_NEGRITA = new Font(FAMILIA, Font.BOLD, 14);
    /** Texto pequeño (errores en línea, ayudas). */
    public static final Font FUENTE_PEQUENA = new Font(FAMILIA, Font.PLAIN, 12);

    // ------------------------------------------------------------------ medidas

    /** Radio de las esquinas redondeadas (en píxeles). */
    public static final int RADIO = 12;
    /** Margen interno de las tarjetas. */
    public static final int MARGEN_TARJETA = 28;
    /** Separación vertical estándar entre elementos de un formulario. */
    public static final int ESPACIO = 6;
    /** Ancho del contenido de los formularios (la tarjeta queda centrada en la ventana). */
    public static final int ANCHO_FORMULARIO = 340;
    /** Diámetro del logo circular. */
    private static final int DIAMETRO_LOGO = 64;
    /** Tamaño de la letra dentro del logo. */
    private static final int TAMANO_LETRA_LOGO = 30;

    /**
     * Nombre de la propiedad cliente que marca un campo con error (borde rojo).
     * Se usa con {@link #marcarError(JComponent, boolean)}.
     */
    static final String PROPIEDAD_ERROR = "socialuna.error";

    /** Texto que mantiene la altura de una etiqueta de error vacía (evita "saltos" del diseño). */
    private static final String SIN_ERROR = " ";

    /** Clase de utilidades: no se instancia. */
    private TemaUi() {
    }

    // ------------------------------------------------------------------ fábricas

    /**
     * @param texto texto del botón
     * @return botón principal (relleno rojo), para la acción más importante de la pantalla
     */
    public static BotonTema crearBotonPrimario(String texto) {
        return new BotonTema(texto, BotonTema.Estilo.PRIMARIO);
    }

    /**
     * @param texto texto del botón
     * @return botón secundario (fondo blanco, borde azul), para acciones alternativas
     */
    public static BotonTema crearBotonSecundario(String texto) {
        return new BotonTema(texto, BotonTema.Estilo.SECUNDARIO);
    }

    /**
     * @param marcador texto de ayuda gris que se ve mientras el campo está vacío (placeholder)
     * @return campo de texto con estilo del tema
     */
    public static CampoTextoTema crearCampoTexto(String marcador) {
        return new CampoTextoTema(marcador);
    }

    /**
     * @param marcador texto de ayuda gris que se ve mientras el campo está vacío
     * @return campo de contraseña (muestra puntos) con estilo del tema
     */
    public static CampoContrasenaTema crearCampoContrasena(String marcador) {
        return new CampoContrasenaTema(marcador);
    }

    /**
     * @param texto texto de la etiqueta
     * @return etiqueta de campo de formulario (negrita)
     */
    public static JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(FUENTE_NEGRITA);
        etiqueta.setForeground(TEXTO);
        return etiqueta;
    }

    /**
     * @param texto texto del título
     * @return título grande y centrado en azul institucional
     */
    public static JLabel crearTitulo(String texto) {
        JLabel titulo = new JLabel(texto, SwingConstants.CENTER);
        titulo.setFont(FUENTE_TITULO);
        titulo.setForeground(AZUL_UNA);
        return titulo;
    }

    /**
     * @param texto texto plano del subtítulo (si es largo, se parte en varias líneas)
     * @return subtítulo centrado en gris
     */
    public static JLabel crearSubtitulo(String texto) {
        JLabel subtitulo = new JLabel(envolverEnHtml(texto, "center"), SwingConstants.CENTER);
        subtitulo.setFont(FUENTE_SUBTITULO);
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        return subtitulo;
    }

    /**
     * @param texto texto plano de ayuda (si es largo, se parte en varias líneas)
     * @return texto pequeño gris para indicaciones bajo un campo
     */
    public static JLabel crearAyuda(String texto) {
        JLabel ayuda = new JLabel(envolverEnHtml(texto, "left"));
        ayuda.setFont(FUENTE_PEQUENA);
        ayuda.setForeground(TEXTO_SECUNDARIO);
        return ayuda;
    }

    /**
     * Envuelve un texto plano en HTML con ancho fijo para que JLabel haga salto de línea
     * automático (un JLabel normal muestra todo en una sola línea y ensancharía la ventana).
     * Los caracteres {@code & < >} se escapan porque el texto puede contener datos escritos por
     * usuarios (por ejemplo su nombre) y no deben interpretarse como etiquetas HTML.
     *
     * @param texto      texto plano
     * @param alineacion "center" o "left"
     * @return texto en HTML seguro
     */
    private static String envolverEnHtml(String texto, String alineacion) {
        String seguro = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='text-align:" + alineacion + ";width:" + ANCHO_FORMULARIO + "px'>"
                + seguro + "</div></html>";
    }

    /**
     * Crea una etiqueta de error EN LÍNEA (texto rojo bajo un campo). Empieza "vacía" pero
     * conservando su altura, para que el formulario no salte cuando aparece un error.
     *
     * @return etiqueta de error
     */
    public static JLabel crearEtiquetaError() {
        JLabel error = new JLabel(SIN_ERROR);
        error.setFont(FUENTE_PEQUENA);
        error.setForeground(ERROR);
        return error;
    }

    /**
     * Muestra u oculta un mensaje en una etiqueta de error.
     *
     * @param etiqueta etiqueta creada con {@link #crearEtiquetaError()}
     * @param mensaje  mensaje a mostrar; nulo o vacío lo oculta
     */
    public static void mostrarError(JLabel etiqueta, String mensaje) {
        etiqueta.setText(mensaje == null || mensaje.isBlank() ? SIN_ERROR : mensaje);
    }

    /**
     * Marca o desmarca visualmente un campo con error (su borde se pinta en rojo).
     *
     * @param campo   campo a marcar
     * @param conError {@code true} para marcarlo
     */
    public static void marcarError(JComponent campo, boolean conError) {
        campo.putClientProperty(PROPIEDAD_ERROR, conError);
        campo.repaint();
    }

    /**
     * @param texto texto de la casilla
     * @return casilla de verificación con el estilo del tema
     */
    public static JCheckBox crearCasilla(String texto) {
        JCheckBox casilla = new JCheckBox(texto);
        casilla.setFont(FUENTE_PEQUENA);
        casilla.setForeground(TEXTO_SECUNDARIO);
        casilla.setOpaque(false);
        return casilla;
    }

    /**
     * Crea una "tarjeta": panel blanco con esquinas redondeadas y margen interno, donde se
     * colocan los formularios.
     *
     * @return tarjeta vacía con {@link GridBagLayout}
     */
    public static JPanel crearTarjeta() {
        JPanel tarjeta = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            /** Pinta el fondo blanco redondeado y un borde suave. */
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = PintorTema.prepararGraficos(g);
                g2.setColor(SUPERFICIE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIO * 2, RADIO * 2);
                g2.setColor(BORDE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIO * 2, RADIO * 2);
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA));
        return tarjeta;
    }

    /**
     * Crea el panel de fondo de una ventana con el contenido CENTRADO. Al redimensionar la
     * ventana, el contenido se mantiene en el centro (GridBagLayout sin restricciones centra a
     * su único hijo).
     *
     * @param contenido componente a centrar (normalmente una tarjeta)
     * @return panel de fondo
     */
    public static JPanel crearFondoCentrado(JComponent contenido) {
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(FONDO);
        fondo.setBorder(BorderFactory.createEmptyBorder(MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA, MARGEN_TARJETA));
        fondo.add(contenido);
        return fondo;
    }

    /**
     * Crea el logo de la marca: un círculo rojo con la letra "S" de Social UNA.
     *
     * @return componente del logo
     */
    public static JComponent crearLogo() {
        JComponent logo = new JComponent() {
            private static final long serialVersionUID = 1L;

            /** Dibuja el círculo y la letra centrada. */
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = PintorTema.prepararGraficos(g);
                int x = (getWidth() - DIAMETRO_LOGO) / 2;
                g2.setColor(ROJO_UNA);
                g2.fillOval(x, 0, DIAMETRO_LOGO, DIAMETRO_LOGO);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(x + 4, 4, DIAMETRO_LOGO - 8, DIAMETRO_LOGO - 8);
                g2.setFont(new Font(FAMILIA, Font.BOLD, TAMANO_LETRA_LOGO));
                FontMetrics medidas = g2.getFontMetrics();
                String letra = "S";
                int letraX = x + (DIAMETRO_LOGO - medidas.stringWidth(letra)) / 2;
                int letraY = (DIAMETRO_LOGO - medidas.getHeight()) / 2 + medidas.getAscent();
                g2.drawString(letra, letraX, letraY);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(DIAMETRO_LOGO, DIAMETRO_LOGO));
        return logo;
    }

    /**
     * Elige la primera familia de letra instalada de la lista; si ninguna existe, usa la
     * sans-serif lógica de Java (siempre disponible).
     *
     * @param preferidas familias en orden de preferencia
     * @return nombre de la familia elegida
     */
    private static String elegirFamilia(String... preferidas) {
        // En un entorno sin pantalla (servidor de pruebas) no se puede consultar el sistema de
        // fuentes; se usa la lógica por defecto.
        if (GraphicsEnvironment.isHeadless()) {
            return Font.SANS_SERIF;
        }
        List<String> instaladas = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String familia : preferidas) {
            if (instaladas.contains(familia)) {
                return familia;
            }
        }
        return Font.SANS_SERIF;
    }
}
