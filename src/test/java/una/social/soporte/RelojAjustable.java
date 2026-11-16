/*
 * RelojAjustable.java
 *
 * Qué hace: reloj de PRUEBAS cuya hora se puede adelantar a voluntad.
 * Rol en la arquitectura: doble de prueba. Permite probar el bloqueo de 5 minutos sin esperar
 * 5 minutos reales.
 * Principio SOLID: DIP (las clases reales reciben un java.time.Clock por constructor; en las
 * pruebas se les pasa este).
 * Colabora con: PruebasGestorUsuarios.
 */
package una.social.soporte;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj controlable para pruebas.
 */
public final class RelojAjustable extends Clock {

    /** Instante actual simulado. */
    private Instant ahora;

    /**
     * @param inicio instante inicial
     */
    public RelojAjustable(Instant inicio) {
        this.ahora = inicio;
    }

    /**
     * Adelanta el reloj.
     *
     * @param duracion cuánto adelantar
     */
    public synchronized void adelantar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Instant instant() {
        return ahora;
    }

    /** {@inheritDoc} */
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** {@inheritDoc} */
    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
