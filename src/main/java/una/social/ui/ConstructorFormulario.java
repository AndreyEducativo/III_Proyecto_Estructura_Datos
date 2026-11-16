/*
 * ConstructorFormulario.java
 *
 * Qué hace: arma una "tarjeta" de formulario (logo, título, campos con su etiqueta y su mensaje
 * de error en línea, botones) con un diseño consistente.
 * Rol en la arquitectura: utilidad de la UI usada por VentanaLogin y VentanaRegistro para no
 * repetir el mismo código de GridBagLayout en cada ventana. Sigue el patrón "Builder".
 * Principio SOLID: SRP (solo arma la disposición visual) y evita duplicación.
 * Colabora con: TemaUi, VentanaLogin, VentanaRegistro.
 */
package una.social.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import una.social.ui.tema.TemaUi;

/**
 * Constructor de formularios en una columna.
 *
 * <p>Usa {@link java.awt.GridBagLayout} con una sola columna: cada elemento ocupa una fila y se
 * estira a lo ancho. La tarjeta queda centrada en la ventana (ver
 * {@link TemaUi#crearFondoCentrado}), por lo que al redimensionar el formulario sigue en el centro.
 * El orden en que se agregan los componentes es también el orden de la tecla TAB.</p>
 */
final class ConstructorFormulario {

    /** Espacio extra antes de cada etiqueta de campo, para separar visualmente los campos. */
    private static final int ESPACIO_ENTRE_CAMPOS = 8;

    /** Espacio antes del bloque de botones. */
    private static final int ESPACIO_ANTES_BOTONES = 14;

    /** Tarjeta que se va llenando. */
    private final JPanel tarjeta = TemaUi.crearTarjeta();

    /** Restricciones reutilizadas para cada fila. */
    private final GridBagConstraints restricciones = new GridBagConstraints();

    /** Siguiente fila libre. */
    private int fila;

    /**
     * Prepara una tarjeta vacía con el ancho estándar del tema.
     */
    ConstructorFormulario() {
        restricciones.gridx = 0;
        restricciones.weightx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        // Un "puntal" invisible fija el ancho mínimo de la columna.
        agregar(Box.createHorizontalStrut(TemaUi.ANCHO_FORMULARIO), 0);
    }

    /**
     * Agrega el encabezado de marca: logo, título y subtítulo.
     *
     * @param titulo    título grande
     * @param subtitulo texto descriptivo bajo el título
     * @return este mismo constructor (para encadenar llamadas)
     */
    ConstructorFormulario encabezado(String titulo, String subtitulo) {
        agregar(TemaUi.crearLogo(), 0);
        agregar(TemaUi.crearTitulo(titulo), TemaUi.ESPACIO * 2);
        agregar(TemaUi.crearSubtitulo(subtitulo), TemaUi.ESPACIO);
        agregar(Box.createVerticalStrut(TemaUi.ESPACIO * 2), 0);
        return this;
    }

    /**
     * Agrega un campo con su etiqueta arriba y su etiqueta de error (roja) abajo. Al escribir en
     * el campo, su error se borra solo, para que el usuario vea que ya lo está corrigiendo.
     *
     * @param textoEtiqueta texto de la etiqueta (por ejemplo "Usuario")
     * @param campo         campo de texto o de contraseña
     * @return la etiqueta de error del campo, para mostrar mensajes en ella
     */
    JLabel campo(String textoEtiqueta, JTextComponent campo) {
        JLabel etiqueta = TemaUi.crearEtiqueta(textoEtiqueta);
        // setLabelFor asocia la etiqueta al campo (accesibilidad: lectores de pantalla).
        etiqueta.setLabelFor(campo);
        JLabel error = TemaUi.crearEtiquetaError();
        agregar(etiqueta, ESPACIO_ENTRE_CAMPOS);
        agregar(campo, TemaUi.ESPACIO);
        agregar(error, 2);
        limpiarErrorAlEscribir(campo, error);
        return error;
    }

    /**
     * Agrega cualquier componente en una fila propia.
     *
     * @param componente   componente a agregar
     * @param espacioArriba separación con la fila anterior, en píxeles
     * @return este mismo constructor
     */
    ConstructorFormulario componente(JComponent componente, int espacioArriba) {
        agregar(componente, espacioArriba);
        return this;
    }

    /**
     * Agrega los botones uno debajo del otro, a todo lo ancho (primero el principal).
     *
     * @param botones botones en orden de importancia
     * @return este mismo constructor
     */
    ConstructorFormulario botones(JComponent... botones) {
        int espacio = ESPACIO_ANTES_BOTONES;
        for (JComponent boton : botones) {
            agregar(boton, espacio);
            espacio = TemaUi.ESPACIO;
        }
        return this;
    }

    /**
     * @return la tarjeta terminada
     */
    JPanel construir() {
        return tarjeta;
    }

    /**
     * Coloca un componente en la siguiente fila.
     *
     * @param componente    componente
     * @param espacioArriba separación superior
     */
    private void agregar(Component componente, int espacioArriba) {
        restricciones.gridy = fila++;
        restricciones.insets = new Insets(espacioArriba, 0, 0, 0);
        tarjeta.add(componente, restricciones);
    }

    /**
     * Borra el mensaje de error y el borde rojo apenas el usuario modifica el campo.
     *
     * @param campo campo observado
     * @param error etiqueta de error del campo
     */
    private static void limpiarErrorAlEscribir(JTextComponent campo, JLabel error) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            /** Se escribió texto: el usuario está corrigiendo, se quita el error. */
            @Override
            public void insertUpdate(DocumentEvent e) {
                limpiar();
            }

            /** Se borró texto: también cuenta como corrección. */
            @Override
            public void removeUpdate(DocumentEvent e) {
                limpiar();
            }

            /** Cambió el formato del texto (no ocurre en campos simples, pero se trata igual). */
            @Override
            public void changedUpdate(DocumentEvent e) {
                limpiar();
            }

            /** Quita el error del campo. */
            private void limpiar() {
                TemaUi.mostrarError(error, null);
                TemaUi.marcarError(campo, false);
            }
        });
    }
}
