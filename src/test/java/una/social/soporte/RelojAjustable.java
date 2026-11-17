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

    /**
     * Hora "actual" del reloj: la que fijó la prueba (no la del sistema), así los resultados
     * no dependen de cuándo se corre la prueba.
     *
     * @return instante actual simulado
     */
    @Override
    public synchronized Instant instant() {
        return ahora;
    }

    /**
     * @return zona horaria UTC (fija, para que las pruebas den igual en cualquier computadora)
     */
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /**
     * Las pruebas no necesitan otras zonas: devuelve el mismo reloj.
     *
     * @param zona zona pedida (se ignora)
     * @return este mismo reloj
     */
    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
