/*
 * PruebasRepositorioUsuariosArchivoTexto.java
 *
 * Qué prueba: guardar y recargar usuarios (incluidos caracteres especiales), el encabezado de
 * comentarios y que las líneas corruptas se omitan y se auditen sin detener la carga.
 * Colabora con: RepositorioUsuariosArchivoTexto, AuditoriaEnMemoria.
 */
package una.social.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.modelo.Usuario;
import una.social.soporte.AuditoriaEnMemoria;

/**
 * Pruebas de {@link RepositorioUsuariosArchivoTexto}.
 */
class PruebasRepositorioUsuariosArchivoTexto {

    /** Contraseña (texto plano) para los datos de prueba; incluye '|' para probar el escape. */
    private static final String CONTRASENA = "clave|de prueba";

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Prueba: Guardar y recargar conserva todos los datos, incluso nombres con | y saltos de línea. */
    @Test
    @DisplayName("Guardar y recargar conserva todos los datos, incluso nombres con | y saltos de línea")
    void guardarYRecargar() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("usuarios.txt");
        AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
        Usuario raro = new Usuario("U001", "Ana | \"la\" \\ Mora\nSegunda", "ana", CONTRASENA, "", Instant.parse("2026-11-15T18:00:00Z"));
        Usuario normal = new Usuario("U002", "Luis Rojas", "luis", CONTRASENA, "", Instant.parse("2026-11-15T18:05:00Z"));

        new RepositorioUsuariosArchivoTexto(archivo, auditoria).guardarTodos(List.of(raro, normal));
        List<Usuario> leidos = new RepositorioUsuariosArchivoTexto(archivo, auditoria).cargarTodos();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(2, leidos.size());
        Usuario primero = leidos.get(0);
        assertEquals(raro.getNombre(), primero.getNombre());
        assertEquals("ana", primero.getNombreUsuario());
        assertEquals(CONTRASENA, primero.getHashContrasena());
        assertEquals(raro.getFechaRegistro(), primero.getFechaRegistro());
        assertEquals(1, auditoria.contar("ARCHIVO_GUARDADO"));
    }

    /** Prueba: El archivo guardado lleva un encabezado de comentarios que explica el formato. */
    @Test
    @DisplayName("El archivo guardado lleva un encabezado de comentarios que explica el formato")
    void escribeEncabezadoComentado() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("usuarios.txt");
        new RepositorioUsuariosArchivoTexto(archivo, new AuditoriaEnMemoria()).guardarTodos(List.of());
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(contenido.startsWith("#"));
        assertTrue(contenido.contains("id") && contenido.contains("contrasena") && contenido.contains("fechaISO"));
    }

    /** Prueba: Archivo corrupto: las líneas dañadas se omiten, se auditan con su número y se cargan las válidas. */
    @Test
    @DisplayName("Archivo corrupto: las líneas dañadas se omiten, se auditan con su número y se cargan las válidas")
    void omiteLineasCorruptas() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("usuarios.txt");
        String valida = "U001|Ana Mora|ana|ana12345|2026-11-15T18:00:00Z";
        Files.writeString(archivo, String.join("\n",
                "# encabezado",
                valida,                                                // línea 2: válida
                "U002|Luis|luis|luis12345",                            // línea 3: faltan columnas
                "X9|Pedro|pedro|pedro123|2026-11-15T18:00:00Z",        // línea 4: ID inválido
                "U004|Eva|eva||2026-11-15T18:00:00Z",                  // línea 5: contraseña vacía
                "U005|Rita|rita|rita1234|ayer",                        // línea 6: fecha inválida
                "U006|Bad\\x|bad|bad12345|2026-11-15T18:00:00Z"         // línea 7: escape inválido
        ) + "\n", StandardCharsets.UTF_8);
        AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();

        List<Usuario> leidos = new RepositorioUsuariosArchivoTexto(archivo, auditoria).cargarTodos();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(1, leidos.size());
        assertEquals("U001", leidos.get(0).getId());
        assertEquals(5, auditoria.contar("LINEA_CORRUPTA_OMITIDA"));
        assertTrue(auditoria.comoTexto().contains("línea 3"));
        assertTrue(auditoria.comoTexto().contains("línea 7"));
        assertTrue(!auditoria.comoTexto().contains("ana12345"), "la auditoría no debe contener contraseñas");
    }
}
