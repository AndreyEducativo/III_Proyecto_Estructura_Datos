/*
 * Bloqueo.java
 *
 * Qué hace: representa que un usuario bloqueó a otro (una línea de data/bloqueos.txt).
 * Rol en la arquitectura: modelo del módulo de bloqueos.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: RegistroBloqueos, GestorBloqueos, RepositorioBloqueosArchivoTexto.
 */
package una.social.bloqueos;

import java.time.Instant;
import java.util.Objects;

/**
 * Bloqueo de un usuario a otro.
 *
 * @param idBloqueador ID de quien bloqueó (el único que puede desbloquear)
 * @param idBloqueado  ID del usuario bloqueado
 * @param fecha        momento del bloqueo
 */
public record Bloqueo(String idBloqueador, String idBloqueado, Instant fecha) {

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param idBloqueador quien bloquea
     * @param idBloqueado  quien es bloqueado
     * @param fecha        fecha del bloqueo
     */
    public Bloqueo {
        Objects.requireNonNull(idBloqueador, "idBloqueador");
        Objects.requireNonNull(idBloqueado, "idBloqueado");
        Objects.requireNonNull(fecha, "fecha");
    }
}
