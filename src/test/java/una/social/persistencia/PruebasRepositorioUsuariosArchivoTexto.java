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

    /** Sal válida en Base64 para los datos de prueba. */
    private static final String SAL = "c2FsLWRlLXBydWViYQ==";

    /** Hash válido en Base64 para los datos de prueba. */
    private static final String HASH = "aGFzaC1kZS1wcnVlYmE=";

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    @Test
    @DisplayName("Guardar y recargar conserva todos los datos, incluso nombres con | y saltos de línea")
    void guardarYRecargar() {
        Path archivo = carpeta.resolve("usuarios.txt");
        AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();
        Usuario raro = new Usuario("U001", "Ana | \"la\" \\ Mora\nSegunda", "ana", HASH, SAL, Instant.parse("2026-11-15T18:00:00Z"));
        Usuario normal = new Usuario("U002", "Luis Rojas", "luis", HASH, SAL, Instant.parse("2026-11-15T18:05:00Z"));

        new RepositorioUsuariosArchivoTexto(archivo, auditoria).guardarTodos(List.of(raro, normal));
        List<Usuario> leidos = new RepositorioUsuariosArchivoTexto(archivo, auditoria).cargarTodos();

        assertEquals(2, leidos.size());
        Usuario primero = leidos.get(0);
        assertEquals(raro.getNombre(), primero.getNombre());
        assertEquals("ana", primero.getNombreUsuario());
        assertEquals(SAL, primero.getSal());
        assertEquals(HASH, primero.getHashContrasena());
        assertEquals(raro.getFechaRegistro(), primero.getFechaRegistro());
        assertEquals(1, auditoria.contar("ARCHIVO_GUARDADO"));
    }

    @Test
    @DisplayName("El archivo guardado lleva un encabezado de comentarios que explica el formato")
    void escribeEncabezadoComentado() throws IOException {
        Path archivo = carpeta.resolve("usuarios.txt");
        new RepositorioUsuariosArchivoTexto(archivo, new AuditoriaEnMemoria()).guardarTodos(List.of());
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.startsWith("#"));
        assertTrue(contenido.contains("id") && contenido.contains("hashBase64") && contenido.contains("fechaISO"));
    }

    @Test
    @DisplayName("Archivo corrupto: las líneas dañadas se omiten, se auditan con su número y se cargan las válidas")
    void omiteLineasCorruptas() throws IOException {
        Path archivo = carpeta.resolve("usuarios.txt");
        String valida = "U001|Ana Mora|ana|" + SAL + "|" + HASH + "|2026-11-15T18:00:00Z";
        Files.writeString(archivo, String.join("\n",
                "# encabezado",
                valida,                                                          // línea 2: válida
                "U002|Luis|luis|" + SAL,                                         // línea 3: faltan columnas
                "X9|Pedro|pedro|" + SAL + "|" + HASH + "|2026-11-15T18:00:00Z",  // línea 4: ID inválido
                "U004|Eva|eva|" + SAL + "|no-es-base64!!|2026-11-15T18:00:00Z",  // línea 5: hash inválido
                "U005|Rita|rita|" + SAL + "|" + HASH + "|ayer",                  // línea 6: fecha inválida
                "U006|Bad\\x|bad|" + SAL + "|" + HASH + "|2026-11-15T18:00:00Z"   // línea 7: escape inválido
        ) + "\n", StandardCharsets.UTF_8);
        AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();

        List<Usuario> leidos = new RepositorioUsuariosArchivoTexto(archivo, auditoria).cargarTodos();

        assertEquals(1, leidos.size());
        assertEquals("U001", leidos.get(0).getId());
        assertEquals(5, auditoria.contar("LINEA_CORRUPTA_OMITIDA"));
        assertTrue(auditoria.comoTexto().contains("línea 3"));
        assertTrue(auditoria.comoTexto().contains("línea 7"));
        assertTrue(!auditoria.comoTexto().contains(HASH), "la auditoría no debe contener hashes");
    }
}
