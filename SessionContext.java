/**
 * Contexto de sesión del usuario activo en la aplicación.
 *
 * <p>Almacena el usuario autenticado para que la capa de UI y el servicio puedan compartir la
 * información del usuario actual sin ocultar dependencias de implementación.</p>
 */
package una.social.session;

import java.util.Objects;
import una.social.model.User;

public class SessionContext {
    private User currentUser;

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void clear() {
        this.currentUser = null;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }
}
