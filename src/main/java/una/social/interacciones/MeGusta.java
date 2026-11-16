/*
 * MeGusta.java
 *
 * Qué hace: representa que un usuario marcó "me gusta" en una publicación (una línea de
 * data/megusta.txt).
 * Rol en la arquitectura: modelo del módulo de interacciones.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: RegistroMeGusta, GestorMeGusta, RepositorioMeGustaArchivoTexto.
 */
package una.social.interacciones;

import java.time.Instant;
import java.util.Objects;

/**
 * "Me gusta" de un usuario en una publicación.
 *
 * @param idPublicacion publicación
 * @param idUsuario     usuario que dio "me gusta"
 * @param fecha         momento en que lo dio
 */
public record MeGusta(String idPublicacion, String idUsuario, Instant fecha) {

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param idPublicacion publicación
     * @param idUsuario     usuario
     * @param fecha         fecha
     */
    public MeGusta {
        Objects.requireNonNull(idPublicacion, "idPublicacion");
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(fecha, "fecha");
    }
}
