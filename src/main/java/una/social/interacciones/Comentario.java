/*
 * Comentario.java
 *
 * Qué hace: representa un comentario de un usuario en una publicación (una línea de
 * data/comentarios.txt).
 * Rol en la arquitectura: modelo del módulo de interacciones.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: RegistroComentarios, GestorComentarios, RepositorioComentariosArchivoTexto.
 */
package una.social.interacciones;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Comentario en una publicación.
 *
 * @param id            identificador (CM0001, CM0002...)
 * @param idPublicacion publicación comentada
 * @param idAutor       autor del comentario
 * @param fecha         momento del comentario
 * @param texto         texto del comentario (máximo {@link #LARGO_MAXIMO_TEXTO} caracteres)
 */
public record Comentario(String id, String idPublicacion, String idAutor, Instant fecha, String texto) {

    /** Formato válido de un ID de comentario: CM seguido de al menos 4 dígitos. */
    public static final Pattern PATRON_ID = Pattern.compile("CM\\d{4,}");

    /** Prefijo de los IDs de comentario. */
    public static final String PREFIJO_ID = "CM";

    /** Largo máximo del texto de un comentario. */
    public static final int LARGO_MAXIMO_TEXTO = 300;

    /** Orden cronológico ascendente (desempate por ID). */
    public static final Comparator<Comentario> ORDEN_CRONOLOGICO =
            Comparator.comparing(Comentario::fecha).thenComparing(Comentario::id);

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param id            identificador
     * @param idPublicacion publicación
     * @param idAutor       autor
     * @param fecha         fecha
     * @param texto         texto
     */
    public Comentario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idPublicacion, "idPublicacion");
        Objects.requireNonNull(idAutor, "idAutor");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(texto, "texto");
    }
}
