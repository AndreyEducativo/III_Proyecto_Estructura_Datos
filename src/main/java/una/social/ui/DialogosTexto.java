/*
 * DialogosTexto.java
 *
 * Qué hace: muestra un diálogo para escribir o editar un texto de varias líneas (editar la última
 * publicación, escribir la biografía) con un contador de caracteres.
 * Rol en la arquitectura: utilidad de la UI del módulo social. Solo PIDE el texto; las reglas
 * (largo máximo, no vacío) las valida el servicio correspondiente.
 * Principio SOLID: SRP (solo el diálogo) y evita repetir el mismo armado en varias pantallas.
 * Colabora con: AreaTextoTema, TextosUi, PanelMiPerfil, TarjetasPublicacion.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import una.social.ui.tema.AreaTextoTema;
import una.social.ui.tema.TemaUi;

/**
 * Diálogo de texto de varias líneas.
 */
final class DialogosTexto {

    /** Tamaño del área de texto dentro del diálogo. */
    private static final Dimension TAMANO_AREA = new Dimension(440, 140);

    /** Clase de utilidades: no se instancia. */
    private DialogosTexto() {
    }

    /**
     * Pide un texto al usuario.
     *
     * @param padre        componente sobre el que se centra el diálogo
     * @param titulo       título del diálogo
     * @param instruccion  texto que se muestra arriba del área
     * @param textoInicial texto con el que empieza el área (para editar)
     * @param maximo       largo máximo (solo para el contador; el servicio es quien valida)
     * @return el texto escrito, o vacío si el usuario canceló
     */
    static Optional<String> pedirTexto(Component padre, String titulo, String instruccion, String textoInicial, int maximo) {
        AreaTextoTema area = new AreaTextoTema("", 0);
        area.setText(textoInicial);
        JLabel contador = TextosUi.textoSecundario("");
        Runnable actualizarContador = () -> {
            int largo = area.getDocument().getLength();
            contador.setText(largo + "/" + maximo);
            contador.setForeground(largo > maximo ? TemaUi.ERROR : TemaUi.TEXTO_SECUNDARIO);
        };
        actualizarContador.run();
        area.getDocument().addDocumentListener(new DocumentListener() {
            /** Texto agregado: actualizar contador. */
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContador.run();
            }

            /** Texto borrado: actualizar contador. */
            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContador.run();
            }

            /** Cambio de formato (no ocurre en texto simple). */
            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContador.run();
            }
        });
        JScrollPane desplazamiento = new JScrollPane(area);
        desplazamiento.setPreferredSize(TAMANO_AREA);
        desplazamiento.setBorder(null);

        JPanel panel = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        panel.add(TemaUi.crearEtiqueta(instruccion), BorderLayout.NORTH);
        panel.add(desplazamiento, BorderLayout.CENTER);
        panel.add(contador, BorderLayout.SOUTH);
        // El foco se pide "más tarde", cuando el diálogo ya esté visible.
        SwingUtilities.invokeLater(area::requestFocusInWindow);
        int opcion = JOptionPane.showConfirmDialog(padre, panel, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return opcion == JOptionPane.OK_OPTION ? Optional.of(area.getText()) : Optional.empty();
    }
}
