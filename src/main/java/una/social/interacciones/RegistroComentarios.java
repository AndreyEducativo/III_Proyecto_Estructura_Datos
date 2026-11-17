/*
 * RegistroComentarios.java
 *
 * Qué hace: ESTRUCTURA DE DATOS en memoria con los comentarios de cada publicación.
 * Rol en la arquitectura: GestorComentarios la llena al iniciar (desde data/comentarios.txt) y la
 * consulta para mostrar "Comentarios (N)" y la lista de comentarios.
 * Principio SOLID: SRP (solo organiza comentarios en memoria).
 * Colabora con: Comentario, GestorComentarios.
 */
package una.social.interacciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de comentarios.
 *
 * <p><b>Estructura:</b> {@code Map<idPublicacion, List<Comentario>>}: cada publicación tiene su
 * LISTA DINÁMICA de comentarios en orden cronológico (del más antiguo al más reciente, que es el
 * orden en que se leen las conversaciones). Además, un {@code HashMap} por ID para encontrar un
 * comentario en O(1) al borrarlo. No es segura entre hilos: la sincroniza el gestor.</p>
 */
public final class RegistroComentarios {

    /** Publicación → sus comentarios en orden cronológico. */
    private final Map<String, List<Comentario>> porPublicacion = new LinkedHashMap<>();

    /** Índice por ID. */
    private final Map<String, Comentario> indicePorId = new HashMap<>();

    /**
     * Agrega un comentario en su posición cronológica dentro de su publicación.
     *
     * @param comentario comentario a agregar
     * @throws IllegalArgumentException si el ID ya existe
     */
    public void agregar(Comentario comentario) {
        // No se permiten dos comentarios con el mismo ID.
        if (indicePorId.containsKey(comentario.id())) {
            throw new IllegalArgumentException("ID de comentario repetido: " + comentario.id());
        }
        // Lista de comentarios de esa publicación (se crea vacía si es el primero).
        List<Comentario> lista = porPublicacion.computeIfAbsent(comentario.idPublicacion(), id -> new ArrayList<>());
        // Inserción ordenada por fecha: se retrocede desde el final mientras el anterior sea más nuevo.
        int posicion = lista.size();
        while (posicion > 0 && Comentario.ORDEN_CRONOLOGICO.compare(lista.get(posicion - 1), comentario) > 0) {
            posicion--;
        }
        // Se guarda en la lista de la publicación y en el índice por ID.
        lista.add(posicion, comentario);
        indicePorId.put(comentario.id(), comentario);
    }

    /**
     * Quita un comentario.
     *
     * @param id ID del comentario
     * @return el comentario quitado, o vacío si no existía
     */
    public Optional<Comentario> quitar(String id) {
        // Se quita del índice por ID; si no estaba, no hay nada más que hacer.
        Comentario quitado = indicePorId.remove(id);
        if (quitado == null) {
            return Optional.empty();
        }
        // También se quita de la lista de su publicación.
        List<Comentario> lista = porPublicacion.get(quitado.idPublicacion());
        lista.remove(quitado);
        // Si la publicación se quedó sin comentarios, se borra su entrada del mapa.
        if (lista.isEmpty()) {
            porPublicacion.remove(quitado.idPublicacion());
        }
        return Optional.of(quitado);
    }

    /**
     * Quita todos los comentarios de una publicación (borrado en cascada).
     *
     * @param idPublicacion publicación borrada
     * @return cuántos se quitaron
     */
    public int quitarDePublicacion(String idPublicacion) {
        // Saca de una vez toda la lista de la publicación.
        List<Comentario> lista = porPublicacion.remove(idPublicacion);
        if (lista == null) {
            return 0;
        }
        // Y borra cada uno de esos comentarios del índice por ID.
        lista.forEach(c -> indicePorId.remove(c.id()));
        return lista.size();
    }

    /**
     * @param id ID buscado
     * @return el comentario si existe
     */
    public Optional<Comentario> buscar(String id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /**
     * @param idPublicacion publicación
     * @return sus comentarios, del más antiguo al más reciente (lista no modificable)
     */
    public List<Comentario> dePublicacion(String idPublicacion) {
        List<Comentario> lista = porPublicacion.get(idPublicacion);
        return lista == null ? List.of() : List.copyOf(lista);
    }

    /**
     * @return todos los comentarios en orden cronológico (para guardarlos), lista no modificable
     */
    public List<Comentario> todos() {
        List<Comentario> todos = new ArrayList<>(indicePorId.values());
        todos.sort(Comentario.ORDEN_CRONOLOGICO);
        return Collections.unmodifiableList(todos);
    }

    /**
     * @return cantidad total de comentarios
     */
    public int tamano() {
        return indicePorId.size();
    }

    /**
     * Vacía el registro (antes de recargar desde el archivo).
     */
    public void limpiar() {
        porPublicacion.clear();
        indicePorId.clear();
    }
}
