/*
 * CabeceraPerfil.java
 *
 * Qué hace: arma la tarjeta superior de un perfil: avatar grande, nombre, "@usuario · ID", una línea
 * extra opcional (por ejemplo "2 amigos en común"), la biografía y una fila de botones.
 * Rol en la arquitectura: utilidad de la UI compartida por "Mi perfil" y la ventana de perfil de
 * otro usuario, para que ambos se vean igual sin repetir código.
 * Principio SOLID: SRP (solo arma la cabecera) y evita duplicación.
 * Colabora con: PanelMiPerfil, VentanaPerfil, AvatarUsuario, TextosUi, ListaTarjetas.
 */
package una.social.ui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import una.social.modelo.Usuario;
import una.social.ui.tema.TemaUi;

/**
 * Fábrica de la cabecera de un perfil.
 */
final class CabeceraPerfil {

    /** Diámetro del avatar del perfil. */
    private static final int DIAMETRO_AVATAR = 72;

    /** Tamaño de letra del nombre en el perfil. */
    private static final float TAMANO_NOMBRE = 22f;

    /** Clase de utilidades: no se instancia. */
    private CabeceraPerfil() {
    }

    /**
     * Crea la cabecera.
     *
     * @param usuario   dueño del perfil
     * @param biografia biografía (vacía = "Sin biografía")
     * @param extra     línea extra bajo el usuario (puede ser nula)
     * @param acciones  fila de botones (puede ser nula)
     * @return tarjeta con margen exterior
     */
    static JPanel crear(Usuario usuario, String biografia, String extra, JComponent acciones) {
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        // Izquierda: avatar grande con la inicial.
        JPanel avatar = new JPanel(new BorderLayout());
        avatar.setOpaque(false);
        avatar.add(new AvatarUsuario(usuario, DIAMETRO_AVATAR), BorderLayout.NORTH);
        tarjeta.add(avatar, BorderLayout.WEST);

        // Datos uno debajo del otro: nombre en grande, @usuario · ID y la línea extra (si hay).
        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.setOpaque(false);
        datos.add(TextosUi.etiquetaPlana(usuario.getNombre(), TemaUi.FUENTE_TITULO.deriveFont(TAMANO_NOMBRE), TemaUi.AZUL_UNA));
        datos.add(TextosUi.textoSecundario(FilaUsuario.detalleBasico(usuario)));
        if (extra != null) {
            datos.add(TextosUi.textoSecundario(extra));
        }
        // Cuerpo: datos arriba, biografía al centro y botones abajo (si se pasaron).
        JPanel cuerpo = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        cuerpo.setOpaque(false);
        cuerpo.add(datos, BorderLayout.NORTH);
        cuerpo.add(biografia.isBlank()
                ? TextosUi.textoSecundario("Sin biografía.")
                : TextosUi.bloqueDeTexto(biografia), BorderLayout.CENTER);
        if (acciones != null) {
            cuerpo.add(acciones, BorderLayout.SOUTH);
        }
        tarjeta.add(cuerpo, BorderLayout.CENTER);

        // Margen exterior para que la tarjeta no toque los bordes.
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, 0, TemaUi.ESPACIO * 2));
        contenedor.add(tarjeta);
        return contenedor;
    }
}
