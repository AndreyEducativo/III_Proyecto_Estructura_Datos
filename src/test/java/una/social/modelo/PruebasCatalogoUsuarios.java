/*
 * PruebasCatalogoUsuarios.java
 *
 * Qué prueba: la estructura de datos en memoria (lista dinámica + índices HashMap): búsquedas
 * sin distinguir mayúsculas, rechazo de duplicados y lista no modificable.
 * Colabora con: CatalogoUsuarios, Usuario.
 */
package una.social.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import una.social.excepciones.ExcepcionUsuarioDuplicado;

/**
 * Pruebas de {@link CatalogoUsuarios}.
 */
class PruebasCatalogoUsuarios {

    /**
     * @param id      ID
     * @param usuario nombre de usuario
     * @return usuario de prueba
     */
    private static Usuario usuario(String id, String usuario) {
        return new Usuario(id, "Nombre " + usuario, usuario, "aGFzaA==", "c2Fs", Instant.EPOCH);
    }

    /** Prueba: Las assertions de Java están habilitadas durante las pruebas (-ea en el pom.xml). */
    @Test
    @DisplayName("Las assertions de Java están habilitadas durante las pruebas (-ea en el pom.xml)")
    void assertsHabilitados() {
        boolean habilitados = false;
        // Truco clásico: la asignación dentro del assert solo se ejecuta si -ea está activo.
        assert habilitados = true;
        assertTrue(habilitados, "Ejecute las pruebas con -ea (ya configurado en surefire)");
    }

    /** Prueba: Busca por nombre de usuario sin distinguir mayúsculas y por ID. */
    @Test
    @DisplayName("Busca por nombre de usuario sin distinguir mayúsculas y por ID")
    void buscaSinDistinguirMayusculas() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        CatalogoUsuarios catalogo = new CatalogoUsuarios();
        catalogo.agregar(usuario("U001", "Ana"));
        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(catalogo.buscarPorNombreUsuario("ana").isPresent());
        assertTrue(catalogo.buscarPorNombreUsuario("  ANA ").isPresent());
        assertEquals("Ana", catalogo.buscarPorId("U001").orElseThrow().getNombreUsuario());
        assertTrue(catalogo.buscarPorId("U999").isEmpty());
    }

    /** Prueba: Rechaza nombres de usuario e IDs repetidos. */
    @Test
    @DisplayName("Rechaza nombres de usuario e IDs repetidos")
    void rechazaDuplicados() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        CatalogoUsuarios catalogo = new CatalogoUsuarios();
        catalogo.agregar(usuario("U001", "ana"));
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionUsuarioDuplicado.class, () -> catalogo.agregar(usuario("U002", "ANA")));
        assertThrows(ExcepcionUsuarioDuplicado.class, () -> catalogo.agregar(usuario("U001", "otra")));
        assertEquals(1, catalogo.tamano());
    }

    /** Prueba: Lista en orden de registro y la lista devuelta no se puede modificar. */
    @Test
    @DisplayName("Lista en orden de registro y la lista devuelta no se puede modificar")
    void listaOrdenadaYNoModificable() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        CatalogoUsuarios catalogo = new CatalogoUsuarios();
        catalogo.agregar(usuario("U001", "ana"));
        catalogo.agregar(usuario("U002", "luis"));
        List<Usuario> todos = catalogo.listarTodos();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U001", "U002"), todos.stream().map(Usuario::getId).toList());
        assertThrows(UnsupportedOperationException.class, () -> todos.add(usuario("U003", "x")));
    }
}
