/*
 * Compartido.java
 *
 * Qué hace: representa que un usuario compartió una publicación PÚBLICA de otra persona.
 * Rol en la arquitectura: modelo del módulo de publicaciones. Se guarda como una línea de
 * data/compartidos.txt. Solo guarda el ID de la publicación original (no la copia): si la original
 * se borra, el compartido muestra "La publicación original ya no está disponible".
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: RegistroCompartidos, GestorPublicaciones, RepositorioCompartidosArchivoTexto.
 */
package una.social.publicaciones;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Publicación compartida.
 *
 * @param id            identificador (CP0001, CP0002...)
 * @param idUsuario     quién compartió
 * @param idPublicacion ID de la publicación original
 * @param fecha         momento en que se compartió (define su lugar en el muro)
 */
public record Compartido(String id, String idUsuario, String idPublicacion, Instant fecha) {

    /** Formato válido de un ID de compartido: CP seguido de al menos 4 dígitos. */
    public static final Pattern PATRON_ID = Pattern.compile("CP\\d{4,}");

    /** Prefijo de los IDs de compartido. */
    public static final String PREFIJO_ID = "CP";

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param id            identificador
     * @param idUsuario     quién compartió
     * @param idPublicacion publicación original
     * @param fecha         fecha
     */
    public Compartido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(idPublicacion, "idPublicacion");
        Objects.requireNonNull(fecha, "fecha");
    }
}
