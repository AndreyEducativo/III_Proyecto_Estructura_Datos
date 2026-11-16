/*
 * RegistroMeGusta.java
 *
 * Qué hace: ESTRUCTURA DE DATOS en memoria con los "me gusta" de cada publicación.
 * Rol en la arquitectura: GestorMeGusta la llena al iniciar (desde data/megusta.txt) y la consulta
 * para mostrar el contador de cada publicación.
 * Principio SOLID: SRP (solo organiza "me gusta" en memoria).
 * Colabora con: MeGusta, GestorMeGusta.
 */
package una.social.interacciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de "me gusta".
 *
 * <p><b>Estructura:</b> {@code HashMap<idPublicacion, Map<idUsuario, MeGusta>>}. Es un "conjunto
 * de usuarios por publicación": como las claves de un mapa no se repiten, un usuario no puede dar
 * dos "me gusta" a la misma publicación. Contar los "me gusta" de una publicación es simplemente
 * el tamaño de su mapa (O(1)), y saber si YO di "me gusta" también es O(1).</p>
 *
 * <p>No es segura entre hilos: la sincroniza el gestor.</p>
 */
public final class RegistroMeGusta {

    /** Publicación → (usuario → me gusta). */
    private final Map<String, Map<String, MeGusta>> porPublicacion = new LinkedHashMap<>();

    /** Cantidad total de "me gusta". */
    private int cantidad;

    /**
     * Agrega un "me gusta".
     *
     * @param meGusta "me gusta" a agregar
     * @throws IllegalArgumentException si ese usuario ya había dado "me gusta" a esa publicación
     */
    public void agregar(MeGusta meGusta) {
        Map<String, MeGusta> usuarios = porPublicacion.computeIfAbsent(meGusta.idPublicacion(), id -> new LinkedHashMap<>());
        if (usuarios.putIfAbsent(meGusta.idUsuario(), meGusta) != null) {
            throw new IllegalArgumentException("Me gusta repetido");
        }
        cantidad++;
    }

    /**
     * Quita el "me gusta" de un usuario en una publicación.
     *
     * @param idPublicacion publicación
     * @param idUsuario     usuario
     * @return {@code true} si existía y se quitó
     */
    public boolean quitar(String idPublicacion, String idUsuario) {
        Map<String, MeGusta> usuarios = porPublicacion.get(idPublicacion);
        if (usuarios == null || usuarios.remove(idUsuario) == null) {
            return false;
        }
        if (usuarios.isEmpty()) {
            porPublicacion.remove(idPublicacion);
        }
        cantidad--;
        return true;
    }

    /**
     * Quita todos los "me gusta" de una publicación (borrado en cascada).
     *
     * @param idPublicacion publicación borrada
     * @return cuántos se quitaron
     */
    public int quitarDePublicacion(String idPublicacion) {
        Map<String, MeGusta> usuarios = porPublicacion.remove(idPublicacion);
        int quitados = usuarios == null ? 0 : usuarios.size();
        cantidad -= quitados;
        return quitados;
    }

    /**
     * @param idPublicacion publicación
     * @param idUsuario     usuario
     * @return {@code true} si el usuario dio "me gusta" a la publicación
     */
    public boolean dioMeGusta(String idPublicacion, String idUsuario) {
        Map<String, MeGusta> usuarios = porPublicacion.get(idPublicacion);
        return usuarios != null && usuarios.containsKey(idUsuario);
    }

    /**
     * @param idPublicacion publicación
     * @return cantidad de "me gusta" que tiene
     */
    public int contar(String idPublicacion) {
        Map<String, MeGusta> usuarios = porPublicacion.get(idPublicacion);
        return usuarios == null ? 0 : usuarios.size();
    }

    /**
     * @return todos los "me gusta" ordenados por fecha (para guardarlos), lista no modificable
     */
    public List<MeGusta> todos() {
        List<MeGusta> todos = new ArrayList<>(cantidad);
        porPublicacion.values().forEach(mapa -> todos.addAll(mapa.values()));
        todos.sort(Comparator.comparing(MeGusta::fecha));
        return Collections.unmodifiableList(todos);
    }

    /**
     * @return cantidad total de "me gusta"
     */
    public int tamano() {
        return cantidad;
    }

    /**
     * Vacía el registro (antes de recargar desde el archivo).
     */
    public void limpiar() {
        porPublicacion.clear();
        cantidad = 0;
    }
}
