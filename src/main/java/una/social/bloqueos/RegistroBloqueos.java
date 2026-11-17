/*
 * RegistroBloqueos.java
 *
 * Qué hace: ESTRUCTURA DE DATOS en memoria con los bloqueos entre usuarios.
 * Rol en la arquitectura: GestorBloqueos la llena al iniciar (desde data/bloqueos.txt) y la
 * consulta cada vez que otro módulo pregunta "¿hay un bloqueo entre A y B?".
 * Principio SOLID: SRP (solo organiza bloqueos en memoria; no valida usuarios ni guarda en disco).
 * Colabora con: Bloqueo, GestorBloqueos.
 */
package una.social.bloqueos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de bloqueos.
 *
 * <p><b>Estructura:</b> {@code Map<String, Map<String, Bloqueo>>}: para cada usuario que bloquea,
 * un mapa "usuario bloqueado → bloqueo". Es como una lista de adyacencia de un grafo DIRIGIDO
 * (A → B significa "A bloqueó a B"). Preguntar si A bloqueó a B cuesta O(1) promedio, y como se
 * consulta muchísimo (en cada publicación del muro), esa rapidez importa.</p>
 *
 * <p>Se usan {@link LinkedHashMap} para conservar el orden en que se bloqueó (la lista de
 * bloqueados se muestra en ese orden). No es segura entre hilos por sí sola: la sincroniza el
 * gestor.</p>
 */
public final class RegistroBloqueos {

    /** Bloqueador → (bloqueado → bloqueo). */
    private final Map<String, Map<String, Bloqueo>> porBloqueador = new LinkedHashMap<>();

    /** Cantidad total de bloqueos (para comprobar la invariante con assert). */
    private int cantidad;

    /**
     * Agrega un bloqueo.
     *
     * @param bloqueo bloqueo a agregar
     * @throws IllegalArgumentException si es un usuario consigo mismo o ya existía
     */
    public void agregar(Bloqueo bloqueo) {
        if (bloqueo.idBloqueador().equals(bloqueo.idBloqueado())) {
            throw new IllegalArgumentException("Un usuario no puede bloquearse a sí mismo");
        }
        if (bloqueo(bloqueo.idBloqueador(), bloqueo.idBloqueado())) {
            throw new IllegalArgumentException("El bloqueo ya existe");
        }
        porBloqueador.computeIfAbsent(bloqueo.idBloqueador(), id -> new LinkedHashMap<>())
                .put(bloqueo.idBloqueado(), bloqueo);
        cantidad++;
        // Invariante: el contador coincide con lo guardado en los mapas.
        assert cantidad == contarEnMapas() : "Contador de bloqueos desincronizado";
    }

    /**
     * Quita un bloqueo.
     *
     * @param idBloqueador quien bloqueó
     * @param idBloqueado  quien fue bloqueado
     * @return {@code true} si existía y se quitó
     */
    public boolean quitar(String idBloqueador, String idBloqueado) {
        // HashMap de dos niveles: bloqueador -> (bloqueado -> Bloqueo).
        Map<String, Bloqueo> bloqueados = porBloqueador.get(idBloqueador);
        if (bloqueados == null || bloqueados.remove(idBloqueado) == null) {
            return false; // No existía ese bloqueo.
        }
        // Si el bloqueador ya no bloquea a nadie, se quita su entrada.
        if (bloqueados.isEmpty()) {
            porBloqueador.remove(idBloqueador);
        }
        cantidad--;
        assert cantidad == contarEnMapas() : "Contador de bloqueos desincronizado";
        return true;
    }

    /**
     * @param idBloqueador posible bloqueador
     * @param idBloqueado  posible bloqueado
     * @return {@code true} si el primero bloqueó al segundo (en ese sentido)
     */
    public boolean bloqueo(String idBloqueador, String idBloqueado) {
        Map<String, Bloqueo> bloqueados = porBloqueador.get(idBloqueador);
        return bloqueados != null && bloqueados.containsKey(idBloqueado);
    }

    /**
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si hay un bloqueo en cualquiera de los dos sentidos
     */
    public boolean hayBloqueoEntre(String idA, String idB) {
        return bloqueo(idA, idB) || bloqueo(idB, idA);
    }

    /**
     * @param idBloqueador usuario consultado
     * @return bloqueos que hizo, en el orden en que los hizo (lista no modificable)
     */
    public List<Bloqueo> bloqueosDe(String idBloqueador) {
        Map<String, Bloqueo> bloqueados = porBloqueador.get(idBloqueador);
        return bloqueados == null ? List.of() : List.copyOf(bloqueados.values());
    }

    /**
     * @return todos los bloqueos (para guardarlos en el archivo), en una lista no modificable
     */
    public List<Bloqueo> todos() {
        List<Bloqueo> todos = new ArrayList<>(cantidad);
        porBloqueador.values().forEach(mapa -> todos.addAll(mapa.values()));
        return Collections.unmodifiableList(todos);
    }

    /**
     * @return cantidad de bloqueos
     */
    public int tamano() {
        return cantidad;
    }

    /**
     * Vacía el registro (antes de recargar desde el archivo).
     */
    public void limpiar() {
        porBloqueador.clear();
        cantidad = 0;
    }

    /**
     * Cuenta los bloqueos recorriendo los mapas. Solo se usa en {@code assert}.
     *
     * @return cantidad real de bloqueos guardados
     */
    private int contarEnMapas() {
        // Suma el tamaño del mapa interno de cada bloqueador.
        int total = 0;
        for (Map<String, Bloqueo> mapa : porBloqueador.values()) {
            total += mapa.size();
        }
        return total;
    }
}
