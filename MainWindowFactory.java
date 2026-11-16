/**
 * Contrato para crear la ventana principal del sistema.
 *
 * <p>El PDF exige que Sebastián pueda sustituir su ventana real sin tocar el flujo de login o
 * la lógica del sistema. Por eso la UI principal se expone como una abstracción.</p>
 */
package una.social;

import javax.swing.JFrame;
import una.social.model.User;

public interface MainWindowFactory {
    JFrame create(User usuarioActivo, Runnable onLogout);
}
