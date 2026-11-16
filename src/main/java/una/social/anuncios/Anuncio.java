/*
 * Anuncio.java
 *
 * Qué hace: representa un anuncio PÚBLICO (con título y texto) que ven todos los usuarios en la
 * pestaña "Anuncios". Es una línea de data/anuncios.txt.
 * Rol en la arquitectura: modelo del módulo de anuncios.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: TablonAnuncios, GestorAnuncios, RepositorioAnunciosArchivoTexto.
 */
package una.social.anuncios;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Anuncio público.
 *
 * @param id      identificador (A0001, A0002...)
 * @param idAutor autor
 * @param fecha   momento de publicación
 * @param titulo  título (máximo {@link #LARGO_MAXIMO_TITULO} caracteres)
 * @param texto   texto (máximo {@link #LARGO_MAXIMO_TEXTO} caracteres)
 */
public record Anuncio(String id, String idAutor, Instant fecha, String titulo, String texto) {

    /** Formato válido de un ID de anuncio: A seguido de al menos 4 dígitos. */
    public static final Pattern PATRON_ID = Pattern.compile("A\\d{4,}");

    /** Prefijo de los IDs de anuncio. */
    public static final String PREFIJO_ID = "A";

    /** Largo máximo del título. */
    public static final int LARGO_MAXIMO_TITULO = 80;

    /** Largo máximo del texto. */
    public static final int LARGO_MAXIMO_TEXTO = 500;

    /** Orden cronológico ascendente (desempate por ID). */
    public static final Comparator<Anuncio> ORDEN_CRONOLOGICO = Comparator.comparing(Anuncio::fecha).thenComparing(Anuncio::id);

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param id      identificador
     * @param idAutor autor
     * @param fecha   fecha
     * @param titulo  título
     * @param texto   texto
     */
    public Anuncio {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idAutor, "idAutor");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(texto, "texto");
    }
}
