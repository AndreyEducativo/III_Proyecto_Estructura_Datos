/*
 * ContextoSesion.java
 *
 * Qué hace: guarda quién es el usuario que inició sesión en este momento.
 * Rol en la arquitectura: contrato compartido. Main crea UNA instancia y la inyecta por
 * constructor (no es un singleton oculto). El ControladorNavegacion la llena al iniciar sesión
 * y la vacía al cerrarla; el módulo de Sebastián puede consultarla para saber quién publica.
 * Principio SOLID: SRP (solo guarda el usuario activo) y DIP (se inyecta, no se busca global).
 * Colabora con: Usuario, ControladorNavegacion, módulo de Sebastián.
 */
package una.social.sesion;

import java.util.Objects;
import java.util.Optional;
import una.social.modelo.Usuario;

/**
 * Sesión activa de la aplicación.
 *
 * <p>Seguro entre hilos: los métodos son {@code synchronized} porque la sesión se cierra desde
 * un SwingWorker y se consulta desde la UI. Como {@link Usuario} es inmutable, devolverlo no
 * pone en riesgo el estado de la sesión.</p>
 */
public final class ContextoSesion {

    /** Usuario autenticado; {@code null} cuando no hay sesión. */
    private Usuario usuarioActivo;

    /**
     * Inicia la sesión de un usuario.
     *
     * @param usuario usuario autenticado (no nulo)
     * @throws IllegalStateException si ya había una sesión abierta (el flujo de la UI nunca
     *                               debería permitirlo: primero se cierra la anterior)
     */
    public synchronized void iniciarSesion(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario");
        if (usuarioActivo != null) {
            throw new IllegalStateException("Ya hay una sesión activa: ciérrela antes de iniciar otra");
        }
        usuarioActivo = usuario;
    }

    /**
     * Cierra la sesión actual (si no había, no hace nada).
     */
    public synchronized void cerrarSesion() {
        usuarioActivo = null;
    }

    /**
     * @return el usuario activo, o vacío si nadie inició sesión
     */
    public synchronized Optional<Usuario> getUsuarioActivo() {
        return Optional.ofNullable(usuarioActivo);
    }

    /**
     * @return {@code true} si hay una sesión iniciada
     */
    public synchronized boolean haySesionActiva() {
        return usuarioActivo != null;
    }
}
