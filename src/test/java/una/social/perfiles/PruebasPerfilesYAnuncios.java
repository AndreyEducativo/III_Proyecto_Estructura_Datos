/*
 * PruebasPerfilesYAnuncios.java
 *
 * Qué prueba: la biografía (guardar, limpiar, largo máximo, persistencia, archivo corrupto) y los
 * anuncios públicos (publicar con validaciones, orden, borrar solo el autor, ocultar bloqueados,
 * persistencia y archivo corrupto).
 * Colabora con: GestorPerfiles, GestorAnuncios, EntornoSocial.
 */
package una.social.perfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.anuncios.Anuncio;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de biografías y anuncios.
 */
class PruebasPerfilesYAnuncios {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Entorno social completo. */
    private EntornoSocial e;

    /** Ana, Luis y María. */
    private Usuario ana;
    /** Luis. */
    private Usuario luis;
    /** María. */
    private Usuario maria;

    /** Prepara el entorno vacío. */
    @BeforeEach
    void preparar() {
        e = new EntornoSocial(carpeta);
        ana = e.u("U001");
        luis = e.u("U002");
        maria = e.u("U003");
    }

    // ------------------------------------------------------------------ biografía

    @Test
    @DisplayName("Biografía: se guarda limpia, se puede borrar dejándola vacía, tiene máximo 160 y sobrevive a reabrir")
    void biografia() {
        assertEquals("", e.m().perfiles().obtenerBiografia(ana.getId()));
        e.m().perfiles().actualizarBiografia(ana, "  Me gustan los grafos | y las colas  ");
        e.m().perfiles().actualizarBiografia(luis, "Luis");
        assertThrows(ExcepcionReglaNegocio.class,
                () -> e.m().perfiles().actualizarBiografia(ana, "x".repeat(Perfil.LARGO_MAXIMO_BIOGRAFIA + 1)));
        e.m().perfiles().actualizarBiografia(luis, "");

        e.recargar();
        assertEquals("Me gustan los grafos | y las colas", e.m().perfiles().obtenerBiografia(ana.getId()));
        assertEquals("", e.m().perfiles().obtenerBiografia(luis.getId()));
    }

    @Test
    @DisplayName("perfiles.txt corrupto: se omiten usuarios inexistentes, repetidos, vacíos y demasiado largos")
    void perfilesCorruptos() throws IOException {
        Files.writeString(e.archivo("perfiles.txt"), String.join("\n",
                "U001|Válida",
                "U001|Repetida",
                "U999|Usuario inexistente",
                "U002|   ",
                "U003|" + "x".repeat(Perfil.LARGO_MAXIMO_BIOGRAFIA + 1)
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();
        assertEquals("Válida", e.m().perfiles().obtenerBiografia("U001"));
        assertEquals(4, e.auditoria().contar("LINEA_CORRUPTA_OMITIDA"));
    }

    // ------------------------------------------------------------------ anuncios

    @Test
    @DisplayName("Anuncios: del más reciente al más antiguo, con validaciones de título y texto, y se guardan")
    void publicarAnuncios() {
        e.m().anuncios().publicar(ana, "Primero", "Texto 1");
        e.avanzar();
        e.m().anuncios().publicar(luis, "  Segundo  ", "Texto 2\ncon salto");
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().anuncios().publicar(ana, "  ", "Sin título"));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().anuncios().publicar(ana, "Sin texto", " "));
        assertThrows(ExcepcionReglaNegocio.class,
                () -> e.m().anuncios().publicar(ana, "x".repeat(Anuncio.LARGO_MAXIMO_TITULO + 1), "Texto"));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().anuncios().publicar(ana, "Dos\nlíneas", "Texto"));

        e.recargar();
        assertEquals(List.of("Segundo", "Primero"),
                e.m().anuncios().listar(maria).stream().map(a -> a.anuncio().titulo()).toList(), "todos los ven");
        assertEquals("A0002", e.m().anuncios().listar(maria).get(0).anuncio().id());
    }

    @Test
    @DisplayName("Anuncios: solo el autor puede borrar; los de usuarios bloqueados no se ven")
    void borrarYBloquearAnuncios() {
        String deAna = e.m().anuncios().publicar(ana, "De Ana", "Texto").id();
        e.m().anuncios().publicar(maria, "De María", "Texto");
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().anuncios().borrar(luis, deAna));
        e.m().bloqueos().bloquear(luis, maria.getId());
        assertEquals(List.of("De Ana"), e.m().anuncios().listar(luis).stream().map(a -> a.anuncio().titulo()).toList());
        e.m().anuncios().borrar(ana, deAna);
        assertTrue(e.m().anuncios().listar(luis).isEmpty());
        assertEquals(1, e.m().anuncios().listar(ana).size());
    }

    @Test
    @DisplayName("anuncios.txt corrupto: se omiten autores inexistentes, IDs repetidos y líneas mal formadas")
    void anunciosCorruptos() throws IOException {
        Files.writeString(e.archivo("anuncios.txt"), String.join("\n",
                "A0001|U001|2026-11-01T12:00:00Z|Válido|Texto",
                "A0001|U002|2026-11-01T12:00:00Z|Repetido|Texto",
                "A0002|U999|2026-11-01T12:00:00Z|Autor inexistente|Texto",
                "A0003|U001|2026-11-01T12:00:00Z|Sin texto",
                "A0004|U001|ayer|Fecha mala|Texto"
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();
        assertEquals(1, e.m().anuncios().listar(ana).size());
        assertEquals(4, e.auditoria().contar("LINEA_CORRUPTA_OMITIDA"));
    }
}
