/*
 * PruebasInteracciones.java
 *
 * Qué prueba: "me gusta" (alterna, solo cuenta, solo en publicaciones visibles, persistencia y
 * archivo corrupto) y comentarios (reglas de texto, visibilidad, orden, permisos de borrado,
 * ocultar a bloqueados, persistencia y archivo corrupto).
 * Colabora con: GestorMeGusta, GestorComentarios, EntornoSocial.
 */
package una.social.interacciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.publicaciones.Privacidad;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de "me gusta" y comentarios.
 */
class PruebasInteracciones {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Entorno social completo. */
    private EntornoSocial e;

    /** Ana (amiga de Luis), Luis y María (sin amigos). */
    private Usuario ana;
    /** Luis. */
    private Usuario luis;
    /** María. */
    private Usuario maria;

    /** Publicación pública de Ana. */
    private String publicaDeAna;

    /** Publicación "solo amigos" de Luis. */
    private String privadaDeLuis;

    /** Prepara: Ana y Luis amigos; una publicación pública de Ana y una "solo amigos" de Luis. */
    @BeforeEach
    void preparar() {
        // Entorno con los 3 usuarios de prueba; Ana y Luis se hacen amigos.
        e = new EntornoSocial(carpeta);
        ana = e.u("U001");
        luis = e.u("U002");
        maria = e.u("U003");
        e.m().amistades().agregarAmistad(ana, luis.getId());
        // Dos publicaciones: una pública de Ana y una "solo amigos" de Luis.
        publicaDeAna = e.m().publicaciones().publicar(ana, "Pública de Ana", Privacidad.PUBLICA).getId();
        privadaDeLuis = e.m().publicaciones().publicar(luis, "Solo amigos de Luis", Privacidad.SOLO_AMIGOS).getId();
    }

    // ------------------------------------------------------------------ me gusta

    /** Prueba: Me gusta alterna (dar/quitar), solo informa la cantidad y si es mío, y se guarda. */
    @Test
    @DisplayName("Me gusta alterna (dar/quitar), solo informa la cantidad y si es mío, y se guarda")
    void meGustaAlterna() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertEquals(new EstadoMeGusta(1, true), e.m().meGusta().alternarMeGusta(luis, publicaDeAna));
        assertEquals(new EstadoMeGusta(2, true), e.m().meGusta().alternarMeGusta(maria, publicaDeAna));
        assertEquals(new EstadoMeGusta(1, false), e.m().meGusta().alternarMeGusta(luis, publicaDeAna));
        assertEquals(new EstadoMeGusta(1, false), e.m().meGusta().estado(ana.getId(), publicaDeAna));
        e.recargar();
        assertEquals(new EstadoMeGusta(1, true), e.m().meGusta().estado(maria.getId(), publicaDeAna));
    }

    /** Prueba: No se puede dar me gusta a una publicación que no se ve (solo amigos de un no amigo o inexistente). */
    @Test
    @DisplayName("No se puede dar me gusta a una publicación que no se ve (solo amigos de un no amigo o inexistente)")
    void meGustaSoloEnVisibles() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().meGusta().alternarMeGusta(maria, privadaDeLuis));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().meGusta().alternarMeGusta(ana, "P9999"));
        assertEquals(new EstadoMeGusta(1, true), e.m().meGusta().alternarMeGusta(ana, privadaDeLuis), "Ana es amiga de Luis");
    }

    /** Prueba: megusta.txt corrupto: se omiten líneas mal formadas, de usuarios o publicaciones inexistentes y repetidas. */
    @Test
    @DisplayName("megusta.txt corrupto: se omiten líneas mal formadas, de usuarios o publicaciones inexistentes y repetidas")
    void meGustaArchivoCorrupto() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(e.archivo("megusta.txt"), String.join("\n",
                publicaDeAna + "|U002|2026-11-01T12:00:00Z",   // válida
                publicaDeAna + "|U002|2026-11-01T13:00:00Z",   // repetida
                publicaDeAna + "|U999|2026-11-01T12:00:00Z",   // usuario inexistente
                "P9999|U003|2026-11-01T12:00:00Z",             // publicación inexistente
                "basura"                                      // formato
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(1, e.m().meGusta().estado(ana.getId(), publicaDeAna).cantidad());
        assertEquals(4, e.auditoria().contar("LINEA_CORRUPTA_OMITIDA"));
    }

    // ------------------------------------------------------------------ comentarios

    /** Prueba: Comentarios: del más antiguo al más reciente, con reglas de texto y de visibilidad, y se guardan. */
    @Test
    @DisplayName("Comentarios: del más antiguo al más reciente, con reglas de texto y de visibilidad, y se guardan")
    void comentar() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().comentarios().comentar(luis, publicaDeAna, "Primero");
        e.avanzar();
        e.m().comentarios().comentar(maria, publicaDeAna, "Segundo | con barra\ny salto");

        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().comentarios().comentar(luis, publicaDeAna, "   "));
        assertThrows(ExcepcionReglaNegocio.class,
                () -> e.m().comentarios().comentar(luis, publicaDeAna, "x".repeat(Comentario.LARGO_MAXIMO_TEXTO + 1)));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().comentarios().comentar(maria, privadaDeLuis, "No la veo"));

        e.recargar();
        List<ComentarioVisible> lista = e.m().comentarios().listar(ana, publicaDeAna);
        assertEquals(List.of("Primero", "Segundo | con barra\ny salto"), lista.stream().map(c -> c.comentario().texto()).toList());
        assertEquals("CM0002", lista.get(1).comentario().id());
        assertTrue(e.m().comentarios().listar(maria, privadaDeLuis).isEmpty(), "sin acceso a la publicación no ve sus comentarios");
    }

    /** Prueba: Borrar comentario: pueden su autor o el autor de la publicación; nadie más. */
    @Test
    @DisplayName("Borrar comentario: pueden su autor o el autor de la publicación; nadie más")
    void permisosDeBorrado() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        String deLuis = e.m().comentarios().comentar(luis, publicaDeAna, "De Luis").id();
        String deMaria = e.m().comentarios().comentar(maria, publicaDeAna, "De María").id();
        String otroDeLuis = e.m().comentarios().comentar(luis, publicaDeAna, "Otro de Luis").id();

        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().comentarios().borrar(maria, deLuis));
        assertFalse(e.m().comentarios().listar(maria, publicaDeAna).get(0).puedeBorrar());
        assertTrue(e.m().comentarios().listar(ana, publicaDeAna).get(0).puedeBorrar(), "la autora de la publicación");

        e.m().comentarios().borrar(luis, deLuis);
        e.m().comentarios().borrar(ana, deMaria);
        assertEquals(List.of(otroDeLuis), e.m().comentarios().listar(ana, publicaDeAna).stream().map(c -> c.comentario().id()).toList());
    }

    /** Prueba: Los comentarios de un usuario bloqueado no se ven (ni se cuentan). */
    @Test
    @DisplayName("Los comentarios de un usuario bloqueado no se ven (ni se cuentan)")
    void comentariosDeBloqueados() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().comentarios().comentar(luis, publicaDeAna, "De Luis");
        e.m().comentarios().comentar(maria, publicaDeAna, "De María");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(2, e.m().comentarios().contar(luis, publicaDeAna));
        e.m().bloqueos().bloquear(luis, maria.getId());
        assertEquals(1, e.m().comentarios().contar(luis, publicaDeAna));
        assertEquals(2, e.m().comentarios().contar(ana, publicaDeAna), "Ana no bloqueó a nadie");
    }

    /** Prueba: comentarios.txt corrupto: se omiten líneas mal formadas, de autores o publicaciones inexistentes. */
    @Test
    @DisplayName("comentarios.txt corrupto: se omiten líneas mal formadas, de autores o publicaciones inexistentes")
    void comentariosArchivoCorrupto() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(e.archivo("comentarios.txt"), String.join("\n",
                "CM0001|" + publicaDeAna + "|U002|2026-11-01T12:00:00Z|Válido",
                "CM0002|" + publicaDeAna + "|U999|2026-11-01T12:00:00Z|Autor inexistente",
                "CM0003|P9999|U002|2026-11-01T12:00:00Z|Publicación inexistente",
                "CM0001|" + publicaDeAna + "|U003|2026-11-01T12:00:00Z|ID repetido",
                "CM0004|" + publicaDeAna + "|U002|2026-11-01T12:00:00Z|   ",
                "XX|" + publicaDeAna + "|U002|2026-11-01T12:00:00Z|ID inválido"
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(1, e.m().comentarios().contar(ana, publicaDeAna));
        assertEquals(5, e.auditoria().contar("LINEA_CORRUPTA_OMITIDA"));
    }
}
