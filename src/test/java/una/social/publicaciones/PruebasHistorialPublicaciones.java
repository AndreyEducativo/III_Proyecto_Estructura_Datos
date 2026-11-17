/*
 * PruebasHistorialPublicaciones.java
 *
 * Qué prueba: la lista dinámica de publicaciones: orden cronológico (aunque lleguen
 * desordenadas), muro filtrado por autores y de la más reciente a la más antigua, IDs repetidos.
 * Colabora con: HistorialPublicaciones, Publicacion.
 */
package una.social.publicaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link HistorialPublicaciones}.
 */
class PruebasHistorialPublicaciones {

    /**
     * @param id      ID
     * @param autor   ID del autor
     * @param dia     día de noviembre de 2026
     * @return publicación de prueba
     */
    private static Publicacion pub(String id, String autor, int dia) {
        return new Publicacion(id, autor, Instant.parse(String.format("2026-11-%02dT12:00:00Z", dia)), "texto " + id);
    }

    /**
     * @param publicaciones publicaciones
     * @return sus IDs en orden
     */
    private static List<String> ids(List<Publicacion> publicaciones) {
        return publicaciones.stream().map(Publicacion::getId).toList();
    }

    /** Prueba: Mantiene el orden cronológico aunque las publicaciones lleguen desordenadas. */
    @Test
    @DisplayName("Mantiene el orden cronológico aunque las publicaciones lleguen desordenadas")
    void mantieneOrdenCronologico() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        HistorialPublicaciones historial = new HistorialPublicaciones();
        historial.agregar(pub("P0002", "U001", 5));
        historial.agregar(pub("P0001", "U001", 1));
        historial.agregar(pub("P0003", "U002", 9));
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("P0001", "P0002", "P0003"), ids(historial.todas()));
    }

    /** Prueba: El muro trae solo los autores visibles, de la más reciente a la más antigua. */
    @Test
    @DisplayName("El muro trae solo los autores visibles, de la más reciente a la más antigua")
    void muroFiltradoYDescendente() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        HistorialPublicaciones historial = new HistorialPublicaciones();
        historial.agregar(pub("P0001", "U001", 1));
        historial.agregar(pub("P0002", "U002", 2));
        historial.agregar(pub("P0003", "U003", 3));
        historial.agregar(pub("P0004", "U001", 4));

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("P0004", "P0002", "P0001"), ids(historial.muro(Set.of("U001", "U002"))));
        assertEquals(List.of(), ids(historial.muro(Set.of("U999"))));
    }

    /** Prueba: Rechaza IDs repetidos. */
    @Test
    @DisplayName("Rechaza IDs repetidos")
    void rechazaIdsRepetidos() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        HistorialPublicaciones historial = new HistorialPublicaciones();
        historial.agregar(pub("P0001", "U001", 1));
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(IllegalArgumentException.class, () -> historial.agregar(pub("P0001", "U002", 2)));
        assertEquals(1, historial.tamano());
    }
}
