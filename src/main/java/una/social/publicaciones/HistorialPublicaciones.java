/*
 * HistorialPublicaciones.java
 *
 * Qué hace: es la ESTRUCTURA DE DATOS en memoria con todas las publicaciones, mantenidas en orden
 * cronológico, y arma el muro (feed) de un usuario.
 * Rol en la arquitectura: el "historial de publicaciones" en lista dinámica que pide el enunciado.
 * GestorPublicaciones lo llena al iniciar y lo consulta para mostrar el muro.
 * Principio SOLID: SRP (solo organiza publicaciones en memoria; no guarda en disco ni sabe de
 * amistades: recibe el conjunto de autores visibles ya calculado).
 * Colabora con: Publicacion, GestorPublicaciones.
 */
package una.social.publicaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Historial de publicaciones.
 *
 * <p><b>Estructuras usadas y por qué:</b></p>
 * <ul>
 *   <li>{@code ArrayList<Publicacion>} (lista dinámica) ordenada de la MÁS ANTIGUA a la MÁS
 *       RECIENTE. Como casi siempre se publica "ahora", cada publicación nueva va al final:
 *       agregar cuesta O(1).</li>
 *   <li>{@code HashMap<String, Publicacion>} por ID: detecta IDs repetidos en O(1) al cargar.</li>
 * </ul>
 *
 * <p><b>¿Cómo se arma el muro? (explicado simple)</b> Como la lista ya está en orden
 * cronológico, basta recorrerla DE ATRÁS HACIA ADELANTE: así salen primero las más recientes,
 * sin tener que ordenar nada. Durante el recorrido solo se toman las publicaciones cuyo autor
 * esté en el conjunto "yo + mis amigos". Preguntar si un autor está en un {@code Set} cuesta O(1),
 * así que armar el muro cuesta O(n), siendo n la cantidad de publicaciones.</p>
 *
 * <p><b>Invariante:</b> la lista siempre está en {@link Publicacion#ORDEN_CRONOLOGICO} y lista e
 * índice tienen el mismo tamaño. No es segura entre hilos por sí sola (la sincroniza el gestor).</p>
 */
public final class HistorialPublicaciones {

    /** Publicaciones en orden cronológico ascendente. */
    private final List<Publicacion> publicaciones = new ArrayList<>();

    /** Índice por ID. */
    private final Map<String, Publicacion> indicePorId = new HashMap<>();

    /**
     * Agrega una publicación en su posición cronológica.
     *
     * <p>Normalmente la nueva es la más reciente y queda al final (el ciclo no da ninguna vuelta).
     * Si llegara una más antigua (por ejemplo, si el reloj del equipo se atrasó), se busca su lugar
     * desde el final hacia atrás, como en el ordenamiento por inserción.</p>
     *
     * @param publicacion publicación a agregar
     * @throws IllegalArgumentException si ya existe una publicación con ese ID
     */
    public void agregar(Publicacion publicacion) {
        if (indicePorId.containsKey(publicacion.getId())) {
            throw new IllegalArgumentException("ID de publicación repetido: " + publicacion.getId());
        }
        int posicion = publicaciones.size();
        while (posicion > 0 && Publicacion.ORDEN_CRONOLOGICO.compare(publicaciones.get(posicion - 1), publicacion) > 0) {
            posicion--;
        }
        publicaciones.add(posicion, publicacion);
        indicePorId.put(publicacion.getId(), publicacion);
        // Invariantes internas: orden respecto a los vecinos y tamaños sincronizados.
        assert posicion == 0 || Publicacion.ORDEN_CRONOLOGICO.compare(publicaciones.get(posicion - 1), publicacion) <= 0
                : "El historial perdió el orden cronológico";
        assert publicaciones.size() == indicePorId.size() : "Lista e índice desincronizados";
    }

    /**
     * Arma el muro: publicaciones de los autores indicados, de la más reciente a la más antigua.
     *
     * @param autoresVisibles IDs de los autores cuyas publicaciones se deben mostrar
     *                        (el usuario y sus amigos)
     * @return lista no modificable, ordenada de la más reciente a la más antigua
     */
    public List<Publicacion> muro(Set<String> autoresVisibles) {
        List<Publicacion> resultado = new ArrayList<>();
        for (int i = publicaciones.size() - 1; i >= 0; i--) {
            Publicacion publicacion = publicaciones.get(i);
            if (autoresVisibles.contains(publicacion.getIdAutor())) {
                resultado.add(publicacion);
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    /**
     * @param id ID buscado
     * @return {@code true} si ya existe una publicación con ese ID
     */
    public boolean contieneId(String id) {
        return indicePorId.containsKey(id);
    }

    /**
     * @return copia no modificable de todas las publicaciones, en orden cronológico ascendente
     */
    public List<Publicacion> todas() {
        return Collections.unmodifiableList(new ArrayList<>(publicaciones));
    }

    /**
     * @return cantidad de publicaciones
     */
    public int tamano() {
        return publicaciones.size();
    }

    /**
     * Vacía el historial (antes de recargarlo desde el archivo).
     */
    public void limpiar() {
        publicaciones.clear();
        indicePorId.clear();
    }
}
