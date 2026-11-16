/*
 * FilaUsuario.java
 *
 * Qué hace: arma la tarjeta de un usuario para una lista: avatar, nombre, una línea de detalle
 * (por ejemplo "@luis · U002") y, a la derecha, un botón o etiqueta de acción.
 * Rol en la arquitectura: utilidad de la UI compartida por PanelBuscador y PanelAmigos (evita
 * repetir el mismo diseño en ambos).
 * Principio SOLID: SRP (solo arma la fila) y evita duplicación.
 * Colabora con: ListaTarjetas, AvatarUsuario, TextosUi, PanelBuscador, PanelAmigos.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import una.social.modelo.Usuario;
import una.social.ui.tema.TemaUi;

/**
 * Fábrica de filas de usuario.
 */
final class FilaUsuario {

    /** Diámetro del avatar en las filas. */
    private static final int DIAMETRO_AVATAR = 44;

    /** Clase de utilidades: no se instancia. */
    private FilaUsuario() {
    }

    /**
     * Crea la fila.
     *
     * @param usuario usuario a mostrar
     * @param detalle texto secundario bajo el nombre
     * @param accion  componente de la derecha (botón o etiqueta); puede ser nulo
     * @return tarjeta lista para agregar a una {@link ListaTarjetas}
     */
    static JPanel crear(Usuario usuario, String detalle, JComponent accion) {
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        tarjeta.add(new AvatarUsuario(usuario, DIAMETRO_AVATAR), BorderLayout.WEST);

        // Dos renglones: nombre en negrita y detalle en gris.
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(TextosUi.etiquetaPlana(usuario.getNombre(), TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO));
        textos.add(TextosUi.etiquetaPlana(detalle, TemaUi.FUENTE_PEQUENA, TemaUi.TEXTO_SECUNDARIO));
        tarjeta.add(textos, BorderLayout.CENTER);

        if (accion != null) {
            // GridBagLayout sin restricciones centra a su único hijo: así el botón o la etiqueta
            // queda centrado verticalmente respecto a la fila.
            JPanel contenedorAccion = new JPanel(new GridBagLayout());
            contenedorAccion.setOpaque(false);
            contenedorAccion.add(accion);
            tarjeta.add(contenedorAccion, BorderLayout.EAST);
        }
        return tarjeta;
    }

    /**
     * @param usuario usuario
     * @return detalle estándar: "@usuario · ID"
     */
    static String detalleBasico(Usuario usuario) {
        return "@" + usuario.getNombreUsuario() + "  ·  " + usuario.getId();
    }
}
