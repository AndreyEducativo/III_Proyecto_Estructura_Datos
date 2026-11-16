/*
 * Publicacion.java
 *
 * Qué hace: representa un mensaje publicado por un usuario en el muro, con su privacidad y, si se
 * editó, la fecha de la edición.
 * Rol en la arquitectura: modelo del módulo de publicaciones. Se guarda como una línea de
 * data/publicaciones.txt.
 * Principio SOLID: SRP (solo datos; es inmutable).
 * Colabora con: HistorialPublicaciones, GestorPublicaciones, RepositorioPublicacionesArchivoTexto.
 */
package una.social.publicaciones;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Publicación del muro. Es INMUTABLE: editar el texto crea un objeto NUEVO con el mismo ID
 * ({@link #conTextoEditado}). Así se puede compartir entre hilos y ventanas sin riesgo.
 */
public final class Publicacion {

    /** Formato válido de un ID de publicación: P seguido de al menos 4 dígitos (P0001, P0002...). */
    public static final Pattern PATRON_ID = Pattern.compile("P\\d{4,}");

    /** Prefijo de los IDs de publicación. */
    public static final String PREFIJO_ID = "P";

    /** Largo máximo del texto de una publicación (en caracteres). */
    public static final int LARGO_MAXIMO_TEXTO = 500;

    /**
     * Orden cronológico ASCENDENTE (de la más antigua a la más reciente). Si dos publicaciones
     * tienen la misma fecha, desempata el ID (que también crece con el tiempo), para que el orden
     * sea siempre el mismo.
     */
    public static final Comparator<Publicacion> ORDEN_CRONOLOGICO =
            Comparator.comparing(Publicacion::getFecha).thenComparing(Publicacion::getId);

    /** Identificador único (P0001, P0002...). */
    private final String id;

    /** ID del usuario autor (U001...). */
    private final String idAutor;

    /** Momento de publicación (UTC). No cambia al editar: el orden del muro se mantiene. */
    private final Instant fecha;

    /** Texto publicado (puede tener varias líneas). */
    private final String texto;

    /** Quién puede verla. */
    private final Privacidad privacidad;

    /** Momento de la última edición, o {@code null} si nunca se editó. */
    private final Instant fechaEdicion;

    /**
     * Crea una publicación "Solo amigos" sin editar (atajo para pruebas y datos antiguos).
     *
     * @param id      identificador
     * @param idAutor ID del autor
     * @param fecha   fecha de publicación
     * @param texto   texto publicado
     */
    public Publicacion(String id, String idAutor, Instant fecha, String texto) {
        this(id, idAutor, fecha, texto, Privacidad.SOLO_AMIGOS, null);
    }

    /**
     * Crea una publicación. Los valores nulos (excepto la fecha de edición) son errores de
     * programación: los datos externos se validan antes, por eso se usa
     * {@link Objects#requireNonNull}.
     *
     * @param id           identificador
     * @param idAutor      ID del autor
     * @param fecha        fecha de publicación
     * @param texto        texto publicado
     * @param privacidad   quién puede verla
     * @param fechaEdicion fecha de la última edición, o {@code null} si no se editó
     */
    public Publicacion(String id, String idAutor, Instant fecha, String texto, Privacidad privacidad, Instant fechaEdicion) {
        this.id = Objects.requireNonNull(id, "id");
        this.idAutor = Objects.requireNonNull(idAutor, "idAutor");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.texto = Objects.requireNonNull(texto, "texto");
        this.privacidad = Objects.requireNonNull(privacidad, "privacidad");
        this.fechaEdicion = fechaEdicion;
    }

    /**
     * Crea la versión editada de esta publicación: mismo ID, autor, fecha y privacidad; texto nuevo
     * y fecha de edición.
     *
     * @param textoNuevo   texto ya validado
     * @param momentoEdicion momento de la edición
     * @return publicación nueva (esta no cambia)
     */
    public Publicacion conTextoEditado(String textoNuevo, Instant momentoEdicion) {
        return new Publicacion(id, idAutor, fecha, textoNuevo, privacidad, Objects.requireNonNull(momentoEdicion));
    }

    /**
     * @return identificador (P0001...)
     */
    public String getId() {
        return id;
    }

    /**
     * @return ID del autor (U001...)
     */
    public String getIdAutor() {
        return idAutor;
    }

    /**
     * @return fecha de publicación (UTC)
     */
    public Instant getFecha() {
        return fecha;
    }

    /**
     * @return texto publicado
     */
    public String getTexto() {
        return texto;
    }

    /**
     * @return privacidad de la publicación
     */
    public Privacidad getPrivacidad() {
        return privacidad;
    }

    /**
     * @return {@code true} si es pública
     */
    public boolean esPublica() {
        return privacidad == Privacidad.PUBLICA;
    }

    /**
     * @return fecha de la última edición, o vacío si nunca se editó
     */
    public Optional<Instant> getFechaEdicion() {
        return Optional.ofNullable(fechaEdicion);
    }

    /**
     * Dos publicaciones son la misma si tienen el mismo ID.
     *
     * @param otro objeto a comparar
     * @return {@code true} si representan la misma publicación
     */
    @Override
    public boolean equals(Object otro) {
        return this == otro || (otro instanceof Publicacion && id.equals(((Publicacion) otro).id));
    }

    /**
     * @return código hash basado en el ID (coherente con equals)
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * @return texto de depuración (sin el contenido completo, que puede ser largo)
     */
    @Override
    public String toString() {
        return "Publicacion{id=" + id + ", autor=" + idAutor + ", fecha=" + fecha + ", " + privacidad + "}";
    }
}
