/*
 * PruebasGrafoAmistades.java
 *
 * Qué prueba: la estructura del grafo de amistades (listas de adyacencia): que la amistad sea
 * mutua, que no admita lazos ni aristas repetidas y que conserve las fechas.
 * Colabora con: GrafoAmistades, Amistad.
 */
package una.social.amistades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link GrafoAmistades}.
 */
class PruebasGrafoAmistades {

    /** Fecha fija para las pruebas. */
    private static final Instant FECHA = Instant.parse("2026-11-01T14:00:00Z");

    /** Prueba: La amistad es mutua: aparece en la lista de adyacencia de ambos usuarios. */
    @Test
    @DisplayName("La amistad es mutua: aparece en la lista de adyacencia de ambos usuarios")
    void amistadMutua() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        GrafoAmistades grafo = new GrafoAmistades();
        grafo.agregar(new Amistad("U001", "U002", FECHA));
        grafo.agregar(new Amistad("U002", "U003", FECHA));

        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(grafo.sonAmigos("U001", "U002"));
        assertTrue(grafo.sonAmigos("U002", "U001"));
        assertFalse(grafo.sonAmigos("U001", "U003"), "la amistad no es transitiva");
        assertEquals(Set.of("U001", "U003"), grafo.amigosDe("U002"));
        assertEquals(Set.of(), grafo.amigosDe("U999"));
        assertEquals(2, grafo.cantidadAmistades(), "cada amistad se guarda una sola vez como arista");
    }

    /** Prueba: No admite que un usuario sea amigo de sí mismo ni aristas repetidas (en cualquier orden). */
    @Test
    @DisplayName("No admite que un usuario sea amigo de sí mismo ni aristas repetidas (en cualquier orden)")
    void rechazaLazosYRepetidos() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        GrafoAmistades grafo = new GrafoAmistades();
        grafo.agregar(new Amistad("U001", "U002", FECHA));
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(IllegalArgumentException.class, () -> grafo.agregar(new Amistad("U001", "U001", FECHA)));
        assertThrows(IllegalArgumentException.class, () -> grafo.agregar(new Amistad("U002", "U001", FECHA)));
        assertEquals(1, grafo.cantidadAmistades());
    }

    /** Prueba: Conserva la fecha de cada amistad y la encuentra sin importar el orden del par. */
    @Test
    @DisplayName("Conserva la fecha de cada amistad y la encuentra sin importar el orden del par")
    void conservaFechas() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        GrafoAmistades grafo = new GrafoAmistades();
        grafo.agregar(new Amistad("U003", "U001", FECHA));
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(FECHA, grafo.buscarAmistad("U001", "U003").orElseThrow().fecha());
        assertEquals("U001", grafo.buscarAmistad("U003", "U001").orElseThrow().otroExtremo("U003"));
    }

    /** Prueba: Los conjuntos devueltos no se pueden modificar desde afuera. */
    @Test
    @DisplayName("Los conjuntos devueltos no se pueden modificar desde afuera")
    void consultasNoModificables() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        GrafoAmistades grafo = new GrafoAmistades();
        grafo.agregar(new Amistad("U001", "U002", FECHA));
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(UnsupportedOperationException.class, () -> grafo.amigosDe("U001").add("U003"));
        assertThrows(UnsupportedOperationException.class, () -> grafo.todasLasAmistades().clear());
    }
}
