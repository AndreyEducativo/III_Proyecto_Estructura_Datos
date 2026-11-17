/*
 * HistorialPublicaciones.java
 *
 * Qué hace: es la ESTRUCTURA DE DATOS en memoria con todas las publicaciones, mantenidas en orden
 * cronológico. Permite armar los muros, buscar, reemplazar (al editar) y eliminar publicaciones.
 * Rol en la arquitectura: el "historial de publicaciones" en lista dinámica que pide el enunciado.
 * GestorPublicaciones lo llena al iniciar y lo consulta para mostrar los muros.
 * Principio SOLID: SRP (solo organiza publicaciones en memoria; no guarda en disco ni sabe de
 * amistades: recibe el filtro ya calculado).
 * Colabora con: Publicacion, GestorPublicaciones.
 */
package una.social.publicaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Historial de publicaciones.
 *
 * <p><b>Estructuras usadas y por qué:</b></p>
 * <ul>
 *   <li>{@code ArrayList<Publicacion>} (lista dinámica) ordenada de la MÁS ANTIGUA a la MÁS
 *       RECIENTE. Como casi siempre se publica "ahora", cada publicación nueva va al final:
 *       agregar cuesta O(1).</li>
 *   <li>{@code HashMap<String, Publicacion>} por ID: buscar, detectar repetidos y saber si existe
 *       cuesta O(1).</li>
 * </ul>
 *
 * <p><b>¿Cómo se arma un muro? (explicado simple)</b> Como la lista ya está en orden
 * cronológico, basta recorrerla DE ATRÁS HACIA ADELANTE: así salen primero las más recientes,
 * sin tener que ordenar nada. Durante el recorrido solo se toman las publicaciones que cumplen el
 * filtro (por ejemplo, "su autor está en yo + mis amigos"). Armar un muro cuesta O(n).</p>
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
     * Reemplaza una publicación por su versión editada (mismo ID y misma fecha, así que conserva
     * su lugar en la lista).
     *
     * @param editada versión nueva
     * @throws IllegalArgumentException si no existe una publicación con ese ID
     */
    public void reemplazar(Publicacion editada) {
        Publicacion anterior = indicePorId.get(editada.getId());
        if (anterior == null) {
            throw new IllegalArgumentException("No existe la publicación " + editada.getId());
        }
        // Precondición interna: editar no cambia la fecha, así el orden cronológico se mantiene.
        assert anterior.getFecha().equals(editada.getFecha()) : "Editar no debe cambiar la fecha";
        publicaciones.set(publicaciones.indexOf(anterior), editada);
        indicePorId.put(editada.getId(), editada);
    }

    /**
     * Elimina una publicación.
     *
     * @param id ID de la publicación
     * @return la publicación eliminada, o vacío si no existía
     */
    public Optional<Publicacion> eliminar(String id) {
        // Se quita del índice por ID (HashMap); si no estaba, no hay nada que hacer.
        Publicacion eliminada = indicePorId.remove(id);
        if (eliminada == null) {
            return Optional.empty();
        }
        // También se quita de la lista ordenada (ArrayList).
        publicaciones.remove(eliminada);
        assert publicaciones.size() == indicePorId.size() : "Lista e índice desincronizados";
        return Optional.of(eliminada);
    }

    /**
     * @param id ID buscado
     * @return la publicación si existe
     */
    public Optional<Publicacion> buscar(String id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /**
     * Busca la publicación MÁS RECIENTE de un autor, recorriendo desde el final de la lista.
     *
     * @param idAutor autor
     * @return su última publicación, o vacío si no tiene ninguna
     */
    public Optional<Publicacion> ultimaDe(String idAutor) {
        // La lista está en orden cronológico: la primera del autor que se encuentre desde el
        // final es la más reciente.
        for (int i = publicaciones.size() - 1; i >= 0; i--) {
            if (publicaciones.get(i).getIdAutor().equals(idAutor)) {
                return Optional.of(publicaciones.get(i));
            }
        }
        // El autor no tiene publicaciones.
        return Optional.empty();
    }

    /**
     * Devuelve, de la más reciente a la más antigua, las publicaciones que cumplen el filtro.
     *
     * @param filtro condición que debe cumplir cada publicación
     * @return lista no modificable, de la más reciente a la más antigua
     */
    public List<Publicacion> filtrarDeMasRecienteAMasAntigua(Predicate<Publicacion> filtro) {
        List<Publicacion> resultado = new ArrayList<>();
        // Se recorre de atrás hacia adelante (de la más nueva a la más vieja), así no hace falta ordenar.
        for (int i = publicaciones.size() - 1; i >= 0; i--) {
            Publicacion publicacion = publicaciones.get(i);
            // Solo entran las que cumplen el filtro (por ejemplo: autor es amigo, o es pública).
            if (filtro.test(publicacion)) {
                resultado.add(publicacion);
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    /**
     * Arma el muro: publicaciones de los autores indicados, de la más reciente a la más antigua.
     *
     * @param autoresVisibles IDs de los autores cuyas publicaciones se deben mostrar
     * @return lista no modificable, ordenada de la más reciente a la más antigua
     */
    public List<Publicacion> muro(Set<String> autoresVisibles) {
        return filtrarDeMasRecienteAMasAntigua(p -> autoresVisibles.contains(p.getIdAutor()));
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
