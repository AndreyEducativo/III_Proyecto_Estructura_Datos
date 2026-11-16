/*
 * PruebasPublicacionesAvanzadas.java
 *
 * Qué prueba: privacidad ("Pública" / "Solo amigos") en el muro general, la vista "Mis amigos" y los
 * perfiles; filtros de bloqueo; migración del formato antiguo de publicaciones.txt; editar la
 * última publicación; la PILA de deshacer; borrar (propias sí, ajenas no) con borrado en cascada de
 * "me gusta" y comentarios; compartir (reglas, "no disponible" y que un ID borrado no se reutilice).
 * Colabora con: GestorPublicaciones, GestorMeGusta, GestorComentarios, GestorBloqueos, EntornoSocial.
 */
package una.social.publicaciones;

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
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de las funciones nuevas del módulo de publicaciones.
 */
class PruebasPublicacionesAvanzadas {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Entorno social completo. */
    private EntornoSocial e;

    /** Ana (U001), amiga de Luis. */
    private Usuario ana;
    /** Luis (U002), amigo de Ana. */
    private Usuario luis;
    /** María (U003), NO es amiga de Ana. */
    private Usuario maria;

    /** Prepara el entorno: Ana y Luis son amigos; María no es amiga de nadie. */
    @BeforeEach
    void preparar() {
        e = new EntornoSocial(carpeta);
        ana = e.u("U001");
        luis = e.u("U002");
        maria = e.u("U003");
        e.m().amistades().agregarAmistad(ana, luis.getId());
    }

    /**
     * Publica y adelanta el reloj, para que cada publicación tenga una fecha distinta.
     *
     * @param autor      autor
     * @param texto      texto
     * @param privacidad privacidad
     * @return ID de la publicación
     */
    private String publicar(Usuario autor, String texto, Privacidad privacidad) {
        String id = e.m().publicaciones().publicar(autor, texto, privacidad).getId();
        e.avanzar();
        return id;
    }

    /**
     * @param entradas entradas de un muro
     * @return IDs de las publicaciones (o "CP:id" para compartidos), en orden
     */
    private static List<String> ids(List<EntradaMuro> entradas) {
        return entradas.stream()
                .map(en -> en.esCompartido() ? "CP:" + en.compartido().idPublicacion() : en.publicacion().getId())
                .toList();
    }

    // ------------------------------------------------------------------ privacidad y vistas

    @Test
    @DisplayName("Privacidad: el muro general muestra solo públicas; 'Mis amigos' muestra todo lo de mi círculo")
    void privacidadEnLasVistas() {
        String a1 = publicar(ana, "A1", Privacidad.PUBLICA);
        String l1 = publicar(luis, "L1", Privacidad.SOLO_AMIGOS);
        String m1 = publicar(maria, "M1", Privacidad.PUBLICA);
        publicar(maria, "M2", Privacidad.SOLO_AMIGOS);

        assertEquals(List.of(m1, a1), ids(e.m().publicaciones().obtenerMuroGeneral(ana)));
        assertEquals(List.of(l1, a1), ids(e.m().publicaciones().obtenerMuro(ana)));
        assertEquals(List.of(m1), ids(e.m().publicaciones().obtenerPerfil(ana, maria.getId())),
                "en el perfil de alguien que no es mi amigo solo veo lo público");
        assertEquals(List.of(l1), ids(e.m().publicaciones().obtenerPerfil(ana, luis.getId())),
                "en el perfil de un amigo veo también lo de solo amigos");
    }

    @Test
    @DisplayName("Bloqueo: lo del usuario bloqueado desaparece de ambos muros y de su perfil, en los dos sentidos")
    void bloqueoFiltraLosMuros() {
        String a1 = publicar(ana, "A1", Privacidad.PUBLICA);
        String m1 = publicar(maria, "M1", Privacidad.PUBLICA);
        e.m().bloqueos().bloquear(ana, maria.getId());

        assertEquals(List.of(a1), ids(e.m().publicaciones().obtenerMuroGeneral(ana)));
        assertEquals(List.of(m1), ids(e.m().publicaciones().obtenerMuroGeneral(maria)));
        assertTrue(e.m().publicaciones().obtenerPerfil(ana, maria.getId()).isEmpty());
        assertFalse(e.m().publicaciones().puedeVer(maria.getId(), a1));
    }

    // ------------------------------------------------------------------ migración de formato

    @Test
    @DisplayName("Migración: líneas antiguas de 4 columnas se leen como 'Solo amigos' y se guardan con 6 columnas")
    void migracionDelFormatoAntiguo() throws IOException {
        Path archivo = e.archivo("publicaciones.txt");
        Files.writeString(archivo, String.join("\n",
                "# formato antiguo",
                "P0001|U001|2026-11-01T10:00:00Z|Texto viejo \\| con barra",
                "P0002|U002|2026-11-01T11:00:00Z|PUBLICA||Texto nuevo"
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();

        List<EntradaMuro> deAna = e.m().publicaciones().obtenerPerfil(ana, ana.getId());
        assertEquals(Privacidad.SOLO_AMIGOS, deAna.get(0).publicacion().getPrivacidad());
        assertEquals("Texto viejo | con barra", deAna.get(0).publicacion().getTexto());
        assertTrue(deAna.get(0).publicacion().getFechaEdicion().isEmpty());

        e.m().publicaciones().guardarDatos();
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8).stream().filter(l -> l.startsWith("P")).toList();
        assertEquals(2, lineas.size());
        lineas.forEach(l -> assertEquals(6, AlmacenArchivoTexto.dividirCampos(l).size(), "formato nuevo: " + l));
    }

    // ------------------------------------------------------------------ editar y deshacer

    @Test
    @DisplayName("Editar: solo la última publicación propia; queda marcada como editada y se guarda")
    void editarUltima() {
        String p1 = publicar(ana, "Primera", Privacidad.PUBLICA);
        String p2 = publicar(ana, "Segunda", Privacidad.PUBLICA);

        Publicacion editada = e.m().publicaciones().editarUltima(ana, "  Segunda corregida  ");
        assertEquals(p2, editada.getId());
        assertEquals("Segunda corregida", editada.getTexto());
        assertTrue(editada.getFechaEdicion().isPresent());
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().editarUltima(ana, "   "));
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().editarUltima(maria, "sin publicaciones"));

        e.recargar();
        List<EntradaMuro> perfil = e.m().publicaciones().obtenerPerfil(ana, ana.getId());
        assertEquals("Segunda corregida", perfil.get(0).publicacion().getTexto());
        assertTrue(perfil.get(0).publicacion().getFechaEdicion().isPresent());
        assertEquals(p1, perfil.get(1).publicacion().getId());
        assertTrue(perfil.get(1).publicacion().getFechaEdicion().isEmpty(), "la anterior no se tocó");
    }

    @Test
    @DisplayName("Pila de deshacer (LIFO): se deshace de la más reciente a la más antigua y salta las ya borradas")
    void pilaDeDeshacer() {
        String a = publicar(ana, "A", Privacidad.PUBLICA);
        String b = publicar(ana, "B", Privacidad.PUBLICA);
        String c = publicar(ana, "C", Privacidad.PUBLICA);

        assertEquals(c, e.m().publicaciones().deshacerUltima(ana).getId());
        e.m().publicaciones().borrar(ana, b);
        assertEquals(a, e.m().publicaciones().deshacerUltima(ana).getId(), "B ya no existía: se salta");
        assertFalse(e.m().publicaciones().puedeDeshacer(ana));
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().deshacerUltima(ana));
        assertTrue(e.m().publicaciones().obtenerPerfil(ana, ana.getId()).isEmpty());
    }

    // ------------------------------------------------------------------ borrar y cascada

    @Test
    @DisplayName("Borrar: solo las propias; borra en cascada sus 'me gusta' y comentarios, también en disco")
    void borradoEnCascada() {
        String p = publicar(ana, "Hola", Privacidad.PUBLICA);
        e.m().meGusta().alternarMeGusta(luis, p);
        e.m().meGusta().alternarMeGusta(maria, p);
        e.m().comentarios().comentar(luis, p, "¡Hola Ana!");
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().borrar(luis, p));

        e.m().publicaciones().borrar(ana, p);

        assertEquals(0, e.m().meGusta().estado(luis.getId(), p).cantidad());
        assertEquals(0, e.m().comentarios().contar(ana, p));
        e.recargar();
        assertEquals(0, e.m().meGusta().estado(luis.getId(), p).cantidad());
        assertEquals(1, e.auditoria().contar("ME_GUSTA_BORRADOS_EN_CASCADA"));
        assertEquals(1, e.auditoria().contar("COMENTARIOS_BORRADOS_EN_CASCADA"));
    }

    // ------------------------------------------------------------------ compartir

    @Test
    @DisplayName("Compartir: solo públicas de otros, una vez; aparece en los muros de los amigos de quien comparte")
    void reglasDeCompartir() {
        String m1 = publicar(maria, "Pública de María", Privacidad.PUBLICA);
        String m2 = publicar(maria, "Privada de María", Privacidad.SOLO_AMIGOS);
        String l1 = publicar(luis, "De Luis", Privacidad.PUBLICA);

        e.m().publicaciones().compartir(luis, m1);
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().compartir(luis, m1), "dos veces");
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().compartir(luis, l1), "la propia");
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().compartir(luis, m2), "no pública");

        List<EntradaMuro> muroAna = e.m().publicaciones().obtenerMuro(ana);
        assertEquals(List.of("CP:" + m1, l1), ids(muroAna), "Ana ve lo que compartió su amigo Luis");
        assertEquals("Luis Rojas", muroAna.get(0).quienCompartio().getNombre());
        assertEquals(List.of("CP:" + m1), ids(e.m().publicaciones().obtenerPerfil(maria, luis.getId())).subList(0, 1));

        e.m().bloqueos().bloquear(ana, maria.getId());
        assertEquals(List.of(l1), ids(e.m().publicaciones().obtenerMuro(ana)), "con bloqueo, el compartido de María se oculta");
        assertThrows(ExcepcionPublicacionInvalida.class, () -> e.m().publicaciones().compartir(ana, m1));
    }

    @Test
    @DisplayName("Si la original se borra, el compartido queda 'no disponible' y su ID nunca se reutiliza")
    void compartidoNoDisponibleYSinReutilizarId() {
        String m1 = publicar(maria, "Se va a borrar", Privacidad.PUBLICA);
        e.m().publicaciones().compartir(luis, m1);
        e.m().publicaciones().borrar(maria, m1);

        EntradaMuro compartido = e.m().publicaciones().obtenerPerfil(luis, luis.getId()).get(0);
        assertTrue(compartido.esCompartido());
        assertFalse(compartido.originalDisponible());

        assertEquals("P0002", publicar(ana, "Nueva", Privacidad.PUBLICA), "no se reutiliza P0001");
        e.recargar();
        assertEquals("P0003", publicar(ana, "Otra", Privacidad.PUBLICA));
        assertFalse(e.m().publicaciones().obtenerPerfil(luis, luis.getId()).get(0).originalDisponible(),
                "tras reabrir, el compartido sigue sin original");
    }
}
