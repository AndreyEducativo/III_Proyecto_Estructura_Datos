/*
 * TablonAnuncios.java
 *
 * Qué hace: ESTRUCTURA DE DATOS en memoria con todos los anuncios, en orden cronológico.
 * Rol en la arquitectura: GestorAnuncios la llena al iniciar (desde data/anuncios.txt) y la recorre
 * para mostrar la pestaña "Anuncios".
 * Principio SOLID: SRP (solo organiza anuncios en memoria).
 * Colabora con: Anuncio, GestorAnuncios.
 */
package una.social.anuncios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tablón de anuncios.
 *
 * <p><b>Estructuras:</b> lista dinámica ({@code ArrayList}) de más antiguo a más reciente, que se
 * recorre de atrás hacia adelante para mostrar primero lo más reciente (como el muro), y un
 * {@code HashMap} por ID para buscar y borrar en O(1). No es segura entre hilos: la sincroniza el
 * gestor.</p>
 */
public final class TablonAnuncios {

    /** Anuncios en orden cronológico ascendente. */
    private final List<Anuncio> anuncios = new ArrayList<>();

    /** Índice por ID. */
    private final Map<String, Anuncio> indicePorId = new HashMap<>();

    /**
     * Agrega un anuncio en su posición cronológica.
     *
     * @param anuncio anuncio a agregar
     * @throws IllegalArgumentException si el ID ya existe
     */
    public void agregar(Anuncio anuncio) {
        // No se permiten dos anuncios con el mismo ID.
        if (indicePorId.containsKey(anuncio.id())) {
            throw new IllegalArgumentException("ID de anuncio repetido: " + anuncio.id());
        }
        // Inserción ordenada: se empieza desde el final y se retrocede mientras el anterior sea
        // más nuevo. Casi siempre el anuncio es el más reciente y queda al final sin moverse.
        int posicion = anuncios.size();
        while (posicion > 0 && Anuncio.ORDEN_CRONOLOGICO.compare(anuncios.get(posicion - 1), anuncio) > 0) {
            posicion--;
        }
        // Se agrega a la lista (ArrayList) y al índice por ID (HashMap).
        anuncios.add(posicion, anuncio);
        indicePorId.put(anuncio.id(), anuncio);
        assert anuncios.size() == indicePorId.size() : "Lista e índice desincronizados";
    }

    /**
     * @param id ID del anuncio
     * @return el anuncio quitado, o vacío si no existía
     */
    public Optional<Anuncio> quitar(String id) {
        // Se quita del índice (HashMap) y, si existía, también de la lista.
        Anuncio quitado = indicePorId.remove(id);
        if (quitado != null) {
            anuncios.remove(quitado);
        }
        return Optional.ofNullable(quitado);
    }

    /**
     * @param id ID buscado
     * @return el anuncio si existe
     */
    public Optional<Anuncio> buscar(String id) {
        return Optional.ofNullable(indicePorId.get(id));
    }

    /**
     * @return anuncios del más reciente al más antiguo (lista no modificable)
     */
    public List<Anuncio> deMasRecienteAMasAntiguo() {
        List<Anuncio> resultado = new ArrayList<>(anuncios);
        Collections.reverse(resultado);
        return Collections.unmodifiableList(resultado);
    }

    /**
     * @return copia no modificable en orden cronológico ascendente (para guardar)
     */
    public List<Anuncio> todos() {
        return Collections.unmodifiableList(new ArrayList<>(anuncios));
    }

    /**
     * @return cantidad de anuncios
     */
    public int tamano() {
        return anuncios.size();
    }

    /**
     * Vacía el tablón (antes de recargar desde el archivo).
     */
    public void limpiar() {
        anuncios.clear();
        indicePorId.clear();
    }
}
