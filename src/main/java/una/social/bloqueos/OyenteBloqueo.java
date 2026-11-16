/*
 * OyenteBloqueo.java
 *
 * Qué hace: contrato para los módulos que deben reaccionar cuando un usuario bloquea a otro
 * (por ejemplo, amistades: hay que eliminar la amistad y las solicitudes entre ellos).
 * Rol en la arquitectura: parte del patrón OBSERVADOR. GestorBloqueos no conoce a GestorAmistades;
 * solo avisa a sus "oyentes". Así se evita una dependencia circular (amistades ya depende de
 * bloqueos para consultar) y se pueden agregar más reacciones sin modificar GestorBloqueos.
 * Principio SOLID: OCP (nuevas reacciones = nuevos oyentes), DIP e ISP (un solo método).
 * Colabora con: GestorBloqueos (avisa), GestorAmistades (escucha), Main (los conecta).
 */
package una.social.bloqueos;

/**
 * Oyente del evento "un usuario bloqueó a otro".
 */
@FunctionalInterface
public interface OyenteBloqueo {

    /**
     * Se llama DESPUÉS de que el bloqueo quedó guardado.
     *
     * @param idBloqueador quien bloqueó
     * @param idBloqueado  quien fue bloqueado
     * @throws una.social.excepciones.ExcepcionPersistencia si el oyente no pudo guardar su parte
     */
    void alBloquear(String idBloqueador, String idBloqueado);
}
