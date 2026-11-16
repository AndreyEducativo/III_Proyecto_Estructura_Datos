/*
 * ControlIntentosLogin.java
 *
 * Qué hace: cuenta los intentos fallidos de inicio de sesión por nombre de usuario y aplica un
 * bloqueo temporal (en memoria) al llegar al máximo.
 * Rol en la arquitectura: lo usa el GestorUsuarios antes y después de verificar credenciales.
 * Principio SOLID: SRP (la política de bloqueo está separada de la verificación de contraseñas)
 * y DIP (recibe el reloj por constructor; en las pruebas se usa un reloj controlable para no
 * tener que esperar 5 minutos reales).
 * Colabora con: GestorUsuarios, ExcepcionCuentaBloqueada.
 */
package una.social.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Política de bloqueo temporal por intentos fallidos.
 *
 * <p><b>¿Por qué?</b> Sin límite, un atacante podría probar miles de contraseñas seguidas
 * ("fuerza bruta"). Con 5 intentos y 5 minutos de bloqueo, eso se vuelve impráctico.</p>
 *
 * <p>Se cuenta por nombre de usuario escrito (exista o no). Así el bloqueo tampoco revela si un
 * usuario existe. El estado vive solo en memoria: al reiniciar la app se olvida (decisión
 * simple y suficiente para el proyecto).</p>
 *
 * <p>Todos los métodos son {@code synchronized} porque los inicios de sesión se procesan en
 * hilos de fondo (SwingWorker) y podrían coincidir.</p>
 */
public final class ControlIntentosLogin {

    /** Fallos permitidos antes de bloquear. */
    private final int maximoIntentos;

    /** Cuánto dura el bloqueo. */
    private final Duration duracionBloqueo;

    /** Reloj del que se toma la hora actual. */
    private final Clock reloj;

    /** Estado por nombre de usuario normalizado (minúsculas). */
    private final Map<String, Estado> estados = new HashMap<>();

    /**
     * Estado de intentos de un nombre de usuario.
     */
    private static final class Estado {
        /** Fallos consecutivos. */
        private int fallos;
        /** Hasta cuándo está bloqueado; {@code null} si no lo está. */
        private Instant bloqueadoHasta;
    }

    /**
     * Crea el control.
     *
     * @param maximoIntentos  fallos permitidos antes de bloquear (mayor que cero)
     * @param duracionBloqueo duración del bloqueo (positiva)
     * @param reloj           reloj del sistema
     * @throws IllegalArgumentException si los valores no son positivos
     */
    public ControlIntentosLogin(int maximoIntentos, Duration duracionBloqueo, Clock reloj) {
        if (maximoIntentos <= 0 || duracionBloqueo.isNegative() || duracionBloqueo.isZero()) {
            throw new IllegalArgumentException("El máximo de intentos y la duración deben ser positivos");
        }
        this.maximoIntentos = maximoIntentos;
        this.duracionBloqueo = duracionBloqueo;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Indica si la clave está bloqueada y cuánto falta. Si el bloqueo ya venció, lo levanta y
     * reinicia el contador.
     *
     * @param clave nombre de usuario normalizado
     * @return tiempo restante si está bloqueada; vacío si puede intentar
     */
    public synchronized Optional<Duration> tiempoBloqueoRestante(String clave) {
        Estado estado = estados.get(clave);
        if (estado == null || estado.bloqueadoHasta == null) {
            return Optional.empty();
        }
        Instant ahora = reloj.instant();
        if (!ahora.isBefore(estado.bloqueadoHasta)) {
            estados.remove(clave);
            return Optional.empty();
        }
        return Optional.of(Duration.between(ahora, estado.bloqueadoHasta));
    }

    /**
     * Anota un intento fallido.
     *
     * @param clave nombre de usuario normalizado
     * @return {@code true} si ESTE fallo provocó el bloqueo
     */
    public synchronized boolean registrarFallo(String clave) {
        Estado estado = estados.computeIfAbsent(clave, k -> new Estado());
        if (estado.bloqueadoHasta != null) {
            // Dos intentos simultáneos pudieron pasar el chequeo de bloqueo al mismo tiempo; el
            // segundo ya no suma: la clave ya quedó bloqueada por el primero.
            return false;
        }
        estado.fallos++;
        // Invariante: nunca se cuentan más fallos que el máximo, porque al alcanzarlo se bloquea
        // y a partir de ahí el "if" de arriba impide seguir sumando.
        assert estado.fallos <= maximoIntentos : "se contaron más fallos que el máximo";
        if (estado.fallos >= maximoIntentos) {
            estado.bloqueadoHasta = reloj.instant().plus(duracionBloqueo);
            return true;
        }
        return false;
    }

    /**
     * @param clave nombre de usuario normalizado
     * @return fallos consecutivos acumulados
     */
    public synchronized int intentosFallidos(String clave) {
        Estado estado = estados.get(clave);
        return estado == null ? 0 : estado.fallos;
    }

    /**
     * Borra el historial de fallos (se llama tras un login exitoso).
     *
     * @param clave nombre de usuario normalizado
     */
    public synchronized void reiniciar(String clave) {
        estados.remove(clave);
    }

    /**
     * @return máximo de intentos configurado
     */
    public int getMaximoIntentos() {
        return maximoIntentos;
    }

    /**
     * @return duración del bloqueo configurada
     */
    public Duration getDuracionBloqueo() {
        return duracionBloqueo;
    }
}
