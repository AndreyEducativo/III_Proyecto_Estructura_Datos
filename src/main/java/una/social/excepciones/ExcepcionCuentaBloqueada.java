/*
 * ExcepcionCuentaBloqueada.java
 *
 * Qué hace: indica que el nombre de usuario está bloqueado temporalmente por demasiados
 * intentos fallidos de inicio de sesión.
 * Rol en la arquitectura: la lanza el GestorUsuarios (apoyado en ControlIntentosLogin) y la
 * muestra la VentanaLogin.
 * Principio SOLID: LSP. Es una ExcepcionAutenticacion más específica; quien solo conozca la
 * clase padre la sigue manejando correctamente.
 * Colabora con: GestorUsuarios, ControlIntentosLogin, VentanaLogin.
 */
package una.social.excepciones;

import java.time.Duration;

/**
 * Bloqueo temporal por intentos fallidos.
 *
 * <p>No revela si el usuario existe: el bloqueo se aplica a cualquier nombre escrito en el
 * login (exista o no), por lo que este mensaje tampoco da pistas a un atacante.</p>
 */
public class ExcepcionCuentaBloqueada extends ExcepcionAutenticacion {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Segundos que tiene un minuto; se usa para redondear el tiempo restante hacia arriba. */
    private static final long SEGUNDOS_POR_MINUTO = 60;

    /** Tiempo que falta para que se levante el bloqueo. */
    private final Duration tiempoRestante;

    /**
     * Crea la excepción indicando cuánto falta para poder intentar de nuevo.
     *
     * @param tiempoRestante duración restante del bloqueo (no nula)
     */
    public ExcepcionCuentaBloqueada(Duration tiempoRestante) {
        super(construirMensaje(tiempoRestante));
        this.tiempoRestante = tiempoRestante;
    }

    /**
     * Devuelve cuánto falta para que termine el bloqueo.
     *
     * @return duración restante
     */
    public Duration getTiempoRestante() {
        return tiempoRestante;
    }

    /**
     * Arma el mensaje para el usuario redondeando hacia arriba a minutos completos,
     * porque "0 minutos" confundiría si todavía faltan unos segundos.
     *
     * @param restante duración restante
     * @return mensaje amigable en español
     */
    private static String construirMensaje(Duration restante) {
        long minutos = Math.max(1, (restante.getSeconds() + SEGUNDOS_POR_MINUTO - 1) / SEGUNDOS_POR_MINUTO);
        return "Demasiados intentos fallidos. Por seguridad, espere " + minutos
                + (minutos == 1 ? " minuto" : " minutos") + " antes de intentarlo de nuevo.";
    }
}
