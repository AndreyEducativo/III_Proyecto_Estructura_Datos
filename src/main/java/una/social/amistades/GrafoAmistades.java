/*
 * GrafoAmistades.java
 *
 * Qué hace: es la ESTRUCTURA DE DATOS que modela la red de amistades como un grafo no dirigido
 * representado con LISTAS DE ADYACENCIA.
 * Rol en la arquitectura: memoria principal del módulo de amistades. GestorAmistades lo llena al
 * iniciar (con lo leído de data/amistades.txt) y lo consulta para "¿quiénes son mis amigos?".
 * Principio SOLID: SRP (solo organiza las relaciones en memoria; no valida usuarios ni guarda en
 * disco).
 * Colabora con: Amistad, GestorAmistades.
 */
package una.social.amistades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Grafo de amistades.
 *
 * <p><b>¿Qué es un grafo y una lista de adyacencia? (explicado simple)</b> Un grafo es un
 * conjunto de "nodos" (aquí, usuarios) unidos por "aristas" (aquí, amistades). Una lista de
 * adyacencia guarda, para CADA nodo, la lista de nodos con los que está conectado:</p>
 * <pre>
 *   U001 → [U002]
 *   U002 → [U001, U003]
 *   U003 → [U002]
 * </pre>
 * <p>Así, para saber los amigos de alguien no hay que recorrer todas las amistades: se consulta
 * directamente su lista. Como la amistad es mutua (grafo NO dirigido), cada amistad aparece en
 * las DOS listas: U002 está en la lista de U001 y U001 en la de U002.</p>
 *
 * <p><b>Estructuras usadas:</b></p>
 * <ul>
 *   <li>{@code Map<String, Set<String>> adyacencia}: ID de usuario → conjunto de IDs de amigos.
 *       Se usa {@link LinkedHashSet} porque (1) no permite repetidos, (2) responde "¿son
 *       amigos?" en O(1) promedio y (3) conserva el orden en que se agregaron.</li>
 *   <li>{@code List<Amistad> aristas}: cada amistad UNA sola vez, con su fecha. Es lo que se
 *       guarda en el archivo (si se guardara la adyacencia, cada amistad saldría dos veces).</li>
 * </ul>
 *
 * <p><b>Invariante:</b> la suma de los tamaños de todas las listas de adyacencia es exactamente
 * el doble de la cantidad de aristas. Se comprueba con {@code assert} después de cada cambio.</p>
 *
 * <p>No es seguro entre hilos por sí solo; el {@code GestorAmistades} sincroniza su acceso.</p>
 */
public final class GrafoAmistades {

    /** Listas de adyacencia: usuario → amigos. */
    private final Map<String, Set<String>> adyacencia = new LinkedHashMap<>();

    /** Aristas del grafo (cada amistad una vez), en el orden en que se agregaron. */
    private final List<Amistad> aristas = new ArrayList<>();

    /** Fecha de cada amistad, para consultar "amigos desde". Clave: par de IDs ordenado. */
    private final Map<String, Amistad> aristaPorPar = new LinkedHashMap<>();

    /**
     * Agrega una amistad (una arista) al grafo.
     *
     * <p>Precondiciones (las garantiza GestorAmistades antes de llamar): los dos IDs son
     * distintos y aún no son amigos. Se verifican con excepciones porque, durante la carga, los
     * datos vienen de un archivo que alguien pudo editar.</p>
     *
     * @param amistad amistad a agregar
     * @throws IllegalArgumentException si es un lazo (un usuario consigo mismo) o ya existe
     */
    public void agregar(Amistad amistad) {
        String a = amistad.idUsuarioA();
        String b = amistad.idUsuarioB();
        if (a.equals(b)) {
            throw new IllegalArgumentException("Un usuario no puede ser amigo de sí mismo");
        }
        if (sonAmigos(a, b)) {
            throw new IllegalArgumentException("La amistad " + a + "-" + b + " ya existe");
        }
        // computeIfAbsent crea la lista vacía la primera vez que aparece un usuario.
        adyacencia.computeIfAbsent(a, id -> new LinkedHashSet<>()).add(b);
        adyacencia.computeIfAbsent(b, id -> new LinkedHashSet<>()).add(a);
        aristas.add(amistad);
        aristaPorPar.put(clavePar(a, b), amistad);
        // Invariante del grafo no dirigido: cada arista suma 2 entradas en la adyacencia.
        assert sumaDeGrados() == 2 * aristas.size() : "Adyacencia y aristas desincronizadas";
    }

    /**
     * Indica si dos usuarios son amigos. Costo O(1) promedio.
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si existe la arista entre ambos
     */
    public boolean sonAmigos(String idA, String idB) {
        Set<String> amigos = adyacencia.get(idA);
        return amigos != null && amigos.contains(idB);
    }

    /**
     * Devuelve los amigos (vecinos) de un usuario.
     *
     * @param idUsuario usuario consultado
     * @return copia no modificable de los IDs de sus amigos (vacía si no tiene)
     */
    public Set<String> amigosDe(String idUsuario) {
        Set<String> amigos = adyacencia.get(idUsuario);
        return amigos == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(amigos));
    }

    /**
     * Busca la amistad (con su fecha) entre dos usuarios.
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return la amistad si existe
     */
    public Optional<Amistad> buscarAmistad(String idA, String idB) {
        return Optional.ofNullable(aristaPorPar.get(clavePar(idA, idB)));
    }

    /**
     * @return copia no modificable de todas las amistades, en orden de creación
     */
    public List<Amistad> todasLasAmistades() {
        return Collections.unmodifiableList(new ArrayList<>(aristas));
    }

    /**
     * @return cantidad de amistades (aristas)
     */
    public int cantidadAmistades() {
        return aristas.size();
    }

    /**
     * Vacía el grafo (antes de recargarlo desde el archivo).
     */
    public void limpiar() {
        adyacencia.clear();
        aristas.clear();
        aristaPorPar.clear();
    }

    /**
     * Clave única para un par de usuarios sin importar el orden ({U001,U002} = {U002,U001}).
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return clave con los IDs ordenados alfabéticamente
     */
    private static String clavePar(String idA, String idB) {
        return idA.compareTo(idB) <= 0 ? idA + "|" + idB : idB + "|" + idA;
    }

    /**
     * Suma de los grados (cantidad de amigos) de todos los nodos. Solo se usa en {@code assert}.
     *
     * @return suma de los tamaños de las listas de adyacencia
     */
    private int sumaDeGrados() {
        int suma = 0;
        for (Set<String> amigos : adyacencia.values()) {
            suma += amigos.size();
        }
        return suma;
    }
}
