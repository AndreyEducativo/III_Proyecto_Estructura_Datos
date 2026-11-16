/*
 * EntradaMuro.java
 *
 * Qué hace: un elemento de un muro: una publicación con los datos de su autor o, si es un
 * compartido, además quién la compartió y cuándo.
 * Rol en la arquitectura: es lo que devuelven los métodos de muro y perfil de
 * ServicioPublicaciones; así la UI no tiene que buscar nombres por su cuenta.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorPublicaciones, PanelMuro, TarjetaPublicacion.
 */
package una.social.publicaciones;

import java.time.Instant;
import una.social.modelo.Usuario;

/**
 * Entrada de un muro.
 *
 * <p>Casos posibles:</p>
 * <ul>
 *   <li>Publicación normal: {@code compartido == null}; publicación y autor presentes.</li>
 *   <li>Compartido con original disponible: los cuatro campos presentes.</li>
 *   <li>Compartido cuya original se borró: {@code publicacion} y {@code autor} son {@code null};
 *       la UI muestra "La publicación original ya no está disponible".</li>
 * </ul>
 *
 * @param publicacion    publicación (la original, si es un compartido); nula si se borró
 * @param autor          autor de la publicación; nulo si se borró
 * @param compartido     datos del compartido, o {@code null} si es una publicación normal
 * @param quienCompartio usuario que compartió, o {@code null} si es una publicación normal
 */
public record EntradaMuro(Publicacion publicacion, Usuario autor, Compartido compartido, Usuario quienCompartio) {

    /**
     * Comprueba la coherencia de los campos (invariante del record).
     *
     * @param publicacion    publicación
     * @param autor          autor
     * @param compartido     compartido
     * @param quienCompartio quien compartió
     */
    public EntradaMuro {
        if ((compartido == null) != (quienCompartio == null)) {
            throw new IllegalArgumentException("Un compartido necesita a quien lo compartió");
        }
        if ((publicacion == null) != (autor == null) || (compartido == null && publicacion == null)) {
            throw new IllegalArgumentException("Publicación y autor deben venir juntos");
        }
    }

    /**
     * Crea la entrada de una publicación normal.
     *
     * @param publicacion publicación
     * @param autor       autor
     * @return entrada
     */
    public static EntradaMuro dePublicacion(Publicacion publicacion, Usuario autor) {
        return new EntradaMuro(publicacion, autor, null, null);
    }

    /**
     * @return {@code true} si es un compartido
     */
    public boolean esCompartido() {
        return compartido != null;
    }

    /**
     * @return {@code true} si hay una publicación para mostrar (falso si es un compartido de una
     *         publicación borrada)
     */
    public boolean originalDisponible() {
        return publicacion != null;
    }

    /**
     * @return fecha que define el lugar en el muro: la del compartido si es un compartido, si no,
     *         la de la publicación
     */
    public Instant fechaOrden() {
        return compartido != null ? compartido.fecha() : publicacion.getFecha();
    }
}
