/*
 * LogosUna.java
 *
 * Qué hace: carga los logos oficiales de la Universidad Nacional (incluidos en el JAR, en
 * src/main/resources/imagenes/) y los entrega listos para usar en la interfaz: el logo completo
 * "UNA - Universidad Nacional - Costa Rica" para el inicio de sesión y el registro, el logo
 * circular para la barra superior de la ventana principal, y los íconos de las ventanas.
 * Rol en la arquitectura: utilidad compartida del tema visual (como TemaUi).
 * Principio SOLID: SRP (solo carga y escala los logos; las ventanas deciden dónde ponerlos).
 * Colabora con: TemaUi, ConstructorFormulario, VentanaLogin, VentanaRegistro, VentanaPrincipal.
 */
package una.social.ui.tema;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Logos de la UNA.
 *
 * <p>Las imágenes se leen UNA sola vez (la primera vez que se piden) y se guardan en memoria. Si
 * por algún motivo no se encuentran en el JAR, la aplicación NO falla: se usa el logo dibujado
 * por {@link TemaUi#crearLogo()} y las ventanas quedan con el ícono por defecto de Java.</p>
 */
public final class LogosUna {

    /** Ruta del logo completo dentro del JAR. */
    private static final String RUTA_COMPLETO = "/imagenes/logo-una.png";

    /** Ruta del logo circular dentro del JAR. */
    private static final String RUTA_CIRCULO = "/imagenes/logo-una-circulo.png";

    /** Tamaños del ícono de ventana (Windows elige el que mejor se ve en la barra de tareas). */
    private static final int[] TAMANOS_ICONO = {16, 20, 24, 32, 40, 48, 64};

    /**
     * Escalados de pantalla de Windows para los que se prepara cada logo (100%, 125%, 150%,
     * 175%, 200%, 250% y 300%).
     */
    private static final double[] ESCALAS_PANTALLA = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0};

    /** Logo completo, leído la primera vez que se pide (vacío si no se pudo leer). */
    private static Optional<BufferedImage> completo;

    /** Logo circular, leído la primera vez que se pide (vacío si no se pudo leer). */
    private static Optional<BufferedImage> circulo;

    /** Clase de utilidades: no se instancia. */
    private LogosUna() {
    }

    /**
     * Crea el logo completo de la UNA con la altura indicada (el ancho respeta la proporción).
     *
     * @param alto altura en píxeles
     * @return etiqueta con el logo, o el logo dibujado si la imagen no está disponible
     */
    public static JComponent crearLogoCompleto(int alto) {
        Optional<BufferedImage> imagen = logoCompleto();
        if (imagen.isEmpty()) {
            return TemaUi.crearLogo();
        }
        BufferedImage original = imagen.get();
        int ancho = Math.round((float) original.getWidth() * alto / original.getHeight());
        return etiqueta(nitida(original, ancho, alto), "Universidad Nacional, Costa Rica");
    }

    /**
     * Prepara la imagen para que se vea NÍTIDA en cualquier pantalla.
     *
     * <p><b>¿Por qué?</b> Si Windows tiene el escalado en 125% o 150%, Java estira cada imagen
     * por ese factor: un logo de 48 px se dibuja con 60 o 72 px "inventados" y se ve pixeleado.
     * La solución es una imagen MULTI-RESOLUCIÓN: se entregan varias versiones (100%, 125%,
     * 150%, 200%...) y Java usa la que coincide con la pantalla, sin estirar nada.</p>
     *
     * @param original imagen original (grande)
     * @param ancho    ancho lógico deseado (a 100%)
     * @param alto     alto lógico deseado (a 100%)
     * @return imagen multi-resolución
     */
    private static Image nitida(BufferedImage original, int ancho, int alto) {
        List<Image> variantes = new ArrayList<>();
        for (double escala : ESCALAS_PANTALLA) {
            int w = (int) Math.round(ancho * escala);
            int h = (int) Math.round(alto * escala);
            variantes.add(escalarAltaCalidad(original, w, h));
        }
        return new BaseMultiResolutionImage(variantes.toArray(new Image[0]));
    }

    /**
     * Reduce una imagen con buena calidad: en vez de achicarla de golpe (lo que "salta" píxeles
     * y deja bordes dentados), la reduce a la mitad varias veces con interpolación bilineal y
     * hace el último paso con interpolación bicúbica.
     *
     * @param original imagen original
     * @param ancho    ancho final
     * @param alto     alto final
     * @return imagen reducida con transparencia
     */
    static BufferedImage escalarAltaCalidad(BufferedImage original, int ancho, int alto) {
        BufferedImage actual = original;
        int w = original.getWidth();
        int h = original.getHeight();
        while (w / 2 >= ancho && h / 2 >= alto) {
            w /= 2;
            h /= 2;
            actual = dibujar(actual, w, h, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        return dibujar(actual, ancho, alto, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /**
     * @param origen        imagen a dibujar
     * @param ancho         ancho del resultado
     * @param alto          alto del resultado
     * @param interpolacion tipo de interpolación
     * @return nueva imagen con {@code origen} dibujada a ese tamaño
     */
    private static BufferedImage dibujar(BufferedImage origen, int ancho, int alto, Object interpolacion) {
        BufferedImage destino = new BufferedImage(Math.max(1, ancho), Math.max(1, alto), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacion);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(origen, 0, 0, destino.getWidth(), destino.getHeight(), null);
        g.dispose();
        return destino;
    }

    /**
     * Crea el logo circular de la UNA.
     *
     * @param diametro diámetro en píxeles
     * @return etiqueta con el logo, o el logo dibujado si la imagen no está disponible
     */
    public static JComponent crearLogoCirculo(int diametro) {
        Optional<BufferedImage> imagen = logoCirculo();
        if (imagen.isEmpty()) {
            return TemaUi.crearLogo();
        }
        return etiqueta(nitida(imagen.get(), diametro, diametro), "UNA");
    }

    /**
     * @return el logo circular en varios tamaños, para {@code JFrame.setIconImages}; vacía si
     *         la imagen no está disponible (Java usa entonces su ícono por defecto)
     */
    public static List<Image> iconosVentana() {
        List<Image> iconos = new ArrayList<>();
        logoCirculo().ifPresent(imagen -> {
            for (int tamano : TAMANOS_ICONO) {
                iconos.add(escalarAltaCalidad(imagen, tamano, tamano));
            }
        });
        return iconos;
    }

    /**
     * @param imagen     imagen ya escalada
     * @param descripcion texto alternativo (accesibilidad: lo leen los lectores de pantalla)
     * @return etiqueta centrada que muestra la imagen
     */
    private static JLabel etiqueta(Image imagen, String descripcion) {
        JLabel etiqueta = new JLabel(new ImageIcon(imagen, descripcion));
        etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
        etiqueta.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        etiqueta.getAccessibleContext().setAccessibleName(descripcion);
        return etiqueta;
    }

    /** @return logo completo (se lee la primera vez) */
    private static synchronized Optional<BufferedImage> logoCompleto() {
        if (completo == null) {
            completo = leer(RUTA_COMPLETO);
        }
        return completo;
    }

    /** @return logo circular (se lee la primera vez) */
    private static synchronized Optional<BufferedImage> logoCirculo() {
        if (circulo == null) {
            circulo = leer(RUTA_CIRCULO);
        }
        return circulo;
    }

    /**
     * Lee una imagen incluida en el JAR.
     *
     * @param ruta ruta dentro del JAR (empieza con "/")
     * @return la imagen, o vacío si no existe o no se pudo leer
     */
    private static Optional<BufferedImage> leer(String ruta) {
        // Se espera IOException si el archivo está dañado. No es grave: es solo decoración, así
        // que se sigue con el logo dibujado en vez de detener la aplicación. Si el recurso no
        // existe, getResourceAsStream devuelve null (no lanza excepción).
        try (InputStream entrada = LogosUna.class.getResourceAsStream(ruta)) {
            return entrada == null ? Optional.empty() : Optional.ofNullable(ImageIO.read(entrada));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}
