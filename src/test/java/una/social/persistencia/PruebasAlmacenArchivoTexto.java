/*
 * PruebasAlmacenArchivoTexto.java
 *
 * Qué prueba: las utilidades compartidas de archivos: escape/desescape de campos, división de
 * líneas respetando el escape, lectura que ignora comentarios y escritura atómica.
 * Colabora con: AlmacenArchivoTexto.
 */
package una.social.persistencia;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.excepciones.ExcepcionDatosCorruptos;

/**
 * Pruebas de {@link AlmacenArchivoTexto}.
 */
class PruebasAlmacenArchivoTexto {

    /** Carpeta temporal que JUnit crea y borra en cada prueba. */
    @TempDir
    Path carpeta;

    @Test
    @DisplayName("Escapar y desescapar devuelve el texto original con |, \\ y saltos de línea")
    void escaparYDesescaparEsReversible() {
        String original = "hola | mundo \\ ruta C:\\nuevo\nsegunda línea\r\nfin";
        String escapado = AlmacenArchivoTexto.escaparCampo(original);
        assertTrue(!escapado.contains("\n") && !escapado.contains("\r"), "el escapado no debe tener saltos reales");
        assertEquals(original, AlmacenArchivoTexto.desescaparCampo(escapado));
    }

    @Test
    @DisplayName("Dividir respeta los | escapados y unir/leer son operaciones inversas")
    void dividirRespetaEscape() {
        String linea = AlmacenArchivoTexto.unirCampos("U001", "a|b", "c\\d", "");
        assertEquals(4, AlmacenArchivoTexto.dividirCampos(linea).size());
        assertArrayEquals(new String[] {"U001", "a|b", "c\\d", ""}, AlmacenArchivoTexto.leerRegistro(linea, 4));
    }

    @Test
    @DisplayName("Un escape inválido, una barra suelta o columnas de más se reportan como datos corruptos")
    void escapeInvalidoEsCorrupto() {
        assertThrows(ExcepcionDatosCorruptos.class, () -> AlmacenArchivoTexto.desescaparCampo("abc\\x"));
        assertThrows(ExcepcionDatosCorruptos.class, () -> AlmacenArchivoTexto.dividirCampos("abc\\"));
        assertThrows(ExcepcionDatosCorruptos.class, () -> AlmacenArchivoTexto.leerRegistro("a|b|c", 2));
    }

    @Test
    @DisplayName("Leer crea el archivo si no existe e ignora comentarios, líneas vacías y el BOM")
    void leerIgnoraComentariosYCreaArchivo() throws IOException {
        Path archivo = carpeta.resolve("sub/datos.txt");
        assertTrue(AlmacenArchivoTexto.leerLineasDeDatos(archivo).isEmpty());
        assertTrue(Files.exists(archivo), "debe crear carpeta y archivo");

        Files.writeString(archivo, "\uFEFF# comentario\n\nA|1\n# otro\nB|2\n", StandardCharsets.UTF_8);
        List<LineaArchivo> lineas = AlmacenArchivoTexto.leerLineasDeDatos(archivo);
        assertEquals(List.of(new LineaArchivo(3, "A|1"), new LineaArchivo(5, "B|2")), lineas);
    }

    @Test
    @DisplayName("La escritura atómica reemplaza el contenido y no deja archivos temporales")
    void escrituraAtomicaReemplazaSinTemporales() throws IOException {
        Path archivo = carpeta.resolve("datos.txt");
        AlmacenArchivoTexto.escribirLineasAtomicamente(archivo, List.of("viejo"));
        AlmacenArchivoTexto.escribirLineasAtomicamente(archivo, List.of("nuevo1", "nuevo2"));

        assertEquals(List.of("nuevo1", "nuevo2"), Files.readAllLines(archivo, StandardCharsets.UTF_8));
        try (Stream<Path> contenido = Files.list(carpeta)) {
            assertEquals(1, contenido.count(), "solo debe quedar el archivo final, sin .tmp");
        }
    }
}
