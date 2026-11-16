/*
 * RegistroCompartidos.java
 *
 * Qué hace: ESTRUCTURA DE DATOS en memoria con todas las publicaciones compartidas, en orden
 * cronológico.
 * Rol en la arquitectura: GestorPublicaciones la llena al iniciar (desde data/compartidos.txt) y la
 * mezcla con el historial de publicaciones para armar los muros.
 * Principio SOLID: SRP (solo organiza compartidos en memoria).
 * Colabora con: Compartido, GestorPublicaciones.
 */
package una.social.publicaciones;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro de publicaciones compartidas.
 *
 * <p><b>Estructuras:</b> un {@code ArrayList} ordenado de más antiguo a más reciente (igual que el
 * historial de publicaciones, para poder recorrerlo de atrás hacia adelante al armar el muro), un
 * {@code HashMap} por ID y un {@code HashSet} de claves "usuario|publicación" para responder en
 * O(1) "¿ya compartió esta publicación?". No es segura entre hilos: la sincroniza el gestor.</p>
 */
public final class RegistroCompartidos {

    /** Orden cronológico ascendente (desempate por ID). */
    private static final Comparator<Compartido> ORDEN = Comparator.comparing(Compartido::fecha).thenComparing(Compartido::id);

    /** Compartidos en orden cronológico ascendente. */
    private final List<Compartido> compartidos = new ArrayList<>();

    /** Índice por ID. */
    private final Map<String, Compartido> indicePorId = new HashMap<>();

    /** Claves "idUsuario|idPublicacion" ya compartidas. */
    private final Set<String> pares = new HashSet<>();

    /**
     * Agrega un compartido en su posición cronológica.
     *
     * @param compartido compartido a agregar
     * @throws IllegalArgumentException si el ID o el par usuario-publicación ya existen
     */
    public void agregar(Compartido compartido) {
        if (indicePorId.containsKey(compartido.id())) {
            throw new IllegalArgumentException("ID de compartido repetido: " + compartido.id());
        }
        if (!pares.add(clave(compartido.idUsuario(), compartido.idPublicacion()))) {
            throw new IllegalArgumentException("El usuario ya compartió esa publicación");
        }
        int posicion = compartidos.size();
        while (posicion > 0 && ORDEN.compare(compartidos.get(posicion - 1), compartido) > 0) {
            posicion--;
        }
        compartidos.add(posicion, compartido);
        indicePorId.put(compartido.id(), compartido);
        // Invariante: las tres estructuras tienen la misma cantidad de elementos.
        assert compartidos.size() == indicePorId.size() && compartidos.size() == pares.size()
                : "Estructuras de compartidos desincronizadas";
    }

    /**
     * Quita un compartido.
     *
     * @param id ID del compartido
     * @return el compartido quitado, o vacío si no existía
     */
    public Optional<Compartido> quitar(String id) {
        Compartido quitado = indicePorId.remove(id);
        if (quitado == null) {
            return Optional.empty();
        }
        compartidos.remove(quitado);
        pares.remove(clave(quitado.idUsuario(), quitado.idPublicacion()));
        return Optional.of(quitado);
    }

    /**
     * @param id ID buscado
     * @return el compartido si existe
     */
    public Optional<Compartido> buscar(String id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /**
     * @param idUsuario     usuario
     * @param idPublicacion publicación original
     * @return {@code true} si ese usuario ya compartió esa publicación
     */
    public boolean yaCompartio(String idUsuario, String idPublicacion) {
        return pares.contains(clave(idUsuario, idPublicacion));
    }

    /**
     * @return copia no modificable, en orden cronológico ascendente
     */
    public List<Compartido> todos() {
        return Collections.unmodifiableList(new ArrayList<>(compartidos));
    }

    /**
     * @return cantidad de compartidos
     */
    public int tamano() {
        return compartidos.size();
    }

    /**
     * Vacía el registro (antes de recargar desde el archivo).
     */
    public void limpiar() {
        compartidos.clear();
        indicePorId.clear();
        pares.clear();
    }

    /**
     * @param idUsuario     usuario
     * @param idPublicacion publicación
     * @return clave única del par
     */
    private static String clave(String idUsuario, String idPublicacion) {
        return idUsuario + "|" + idPublicacion;
    }
}
