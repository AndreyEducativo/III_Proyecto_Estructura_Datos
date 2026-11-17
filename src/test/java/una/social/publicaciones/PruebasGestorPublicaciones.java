/*
 * PruebasGestorPublicaciones.java
 *
 * Qué prueba: publicar (validaciones e IDs), el muro cruzado (propias + amigos, de la más
 * reciente a la más antigua; sin publicaciones de no amigos), la persistencia con caracteres
 * especiales, las líneas corruptas y la protección ante una carga fallida.
 * Colabora con: GestorPublicaciones, GestorAmistades, repositorios de texto, DirectorioEnMemoria,
 * AuditoriaEnMemoria, RelojAjustable.
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.amistades.GestorAmistades;
import una.social.amistades.GrafoAmistades;
import una.social.amistades.RepositorioAmistadesArchivoTexto;
import una.social.amistades.RepositorioSolicitudesArchivoTexto;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.modelo.Usuario;
import una.social.soporte.AuditoriaEnMemoria;
import una.social.soporte.DirectorioEnMemoria;
import una.social.soporte.RelojAjustable;

/**
 * Pruebas de {@link GestorPublicaciones}.
 */
class PruebasGestorPublicaciones {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Archivo de publicaciones. */
    private Path archivo;

    /** Usuarios de prueba. */
    private DirectorioEnMemoria directorio;

    /** Bitácora en memoria. */
    private AuditoriaEnMemoria auditoria;

    /** Reloj controlable. */
    private RelojAjustable reloj;

    /** Gestor de amistades real (para el muro). */
    private GestorAmistades amistades;

    /** Gestor bajo prueba (ya cargado). */
    private GestorPublicaciones gestor;

    /** Ana, Luis y María. */
    private Usuario ana;
    /** Luis Rojas. */
    private Usuario luis;
    /** María Vargas. */
    private Usuario maria;

    /** Prepara gestores vacíos antes de cada prueba. */
    @BeforeEach
    void preparar() {
        // Archivo temporal y los 3 usuarios de prueba.
        archivo = carpeta.resolve("publicaciones.txt");
        directorio = DirectorioEnMemoria.conUsuariosDePrueba();
        ana = directorio.obtener("U001");
        luis = directorio.obtener("U002");
        maria = directorio.obtener("U003");
        // Auditoría en memoria, reloj fijo y un gestor de amistades real (el muro lo necesita).
        auditoria = new AuditoriaEnMemoria();
        reloj = new RelojAjustable(Instant.parse("2026-11-01T12:00:00Z"));
        amistades = new GestorAmistades(new RepositorioAmistadesArchivoTexto(carpeta.resolve("amistades.txt"), auditoria),
                new RepositorioSolicitudesArchivoTexto(carpeta.resolve("solicitudes.txt"), auditoria),
                new GrafoAmistades(), directorio, ConsultaBloqueos.SIN_BLOQUEOS, auditoria, reloj);
        amistades.cargarDatos();
        gestor = nuevoGestorCargado();
    }

    /**
     * @return gestor nuevo sobre el mismo archivo, ya cargado (simula reabrir la app)
     */
    private GestorPublicaciones nuevoGestorCargado() {
        // Mismas piezas que el programa real, sobre la carpeta temporal y sin bloqueos.
        GestorPublicaciones nuevo = new GestorPublicaciones(new RepositorioPublicacionesArchivoTexto(archivo, auditoria),
                new RepositorioCompartidosArchivoTexto(carpeta.resolve("compartidos.txt"), auditoria),
                new HistorialPublicaciones(), new RegistroCompartidos(), directorio, amistades,
                ConsultaBloqueos.SIN_BLOQUEOS, auditoria, reloj);
        nuevo.cargarDatos();
        return nuevo;
    }

    /**
     * Publica y adelanta el reloj una hora, para que cada publicación tenga una fecha distinta.
     *
     * @param autor autor
     * @param texto texto
     * @return publicación creada
     */
    private Publicacion publicar(Usuario autor, String texto) {
        Publicacion creada = gestor.publicar(autor, texto);
        reloj.adelantar(Duration.ofHours(1));
        return creada;
    }

    /**
     * @param usuario dueño del muro
     * @return IDs de las publicaciones de su muro, en el orden mostrado
     */
    private List<String> muro(Usuario usuario) {
        return gestor.obtenerMuro(usuario).stream().map(e -> e.publicacion().getId()).toList();
    }

    /** Prueba: Publicar: asigna IDs P0001, P0002..., limpia espacios y se audita sin el contenido. */
    @Test
    @DisplayName("Publicar: asigna IDs P0001, P0002..., limpia espacios y se audita sin el contenido")
    void publicarAsignaIds() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Publicacion primera = publicar(ana, "   Hola mundo   ");
        Publicacion segunda = publicar(ana, "Segunda");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals("P0001", primera.getId());
        assertEquals("P0002", segunda.getId());
        assertEquals("Hola mundo", primera.getTexto());
        assertEquals(2, auditoria.contar("PUBLICACION_CREADA"));
        assertFalse(auditoria.comoTexto().contains("Hola mundo"), "la auditoría no copia el contenido");
    }

    /** Prueba: Validaciones: texto vacío o de más de 500 caracteres se rechaza; justo 500 se acepta. */
    @Test
    @DisplayName("Validaciones: texto vacío o de más de 500 caracteres se rechaza; justo 500 se acepta")
    void validaciones() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertThrows(ExcepcionPublicacionInvalida.class, () -> gestor.publicar(ana, "   \n  "));
        assertThrows(ExcepcionPublicacionInvalida.class, () -> gestor.publicar(ana, null));
        assertThrows(ExcepcionPublicacionInvalida.class,
                () -> gestor.publicar(ana, "x".repeat(Publicacion.LARGO_MAXIMO_TEXTO + 1)));
        assertEquals(Publicacion.LARGO_MAXIMO_TEXTO,
                gestor.publicar(ana, "x".repeat(Publicacion.LARGO_MAXIMO_TEXTO)).getTexto().length());
        assertEquals(3, auditoria.contar("PUBLICACION_RECHAZADA"));
    }

    /** Prueba: Muro cruzado: propias + de amigos, de la más reciente a la más antigua; nunca de no amigos. */
    @Test
    @DisplayName("Muro cruzado: propias + de amigos, de la más reciente a la más antigua; nunca de no amigos")
    void muroCruzado() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        amistades.agregarAmistad(ana, "U002");   // Ana - Luis
        amistades.agregarAmistad(luis, "U003");  // Luis - María (Ana y María NO son amigas)
        publicar(ana, "A1");     // P0001
        publicar(luis, "L1");    // P0002
        publicar(maria, "M1");   // P0003
        publicar(ana, "A2");     // P0004

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("P0004", "P0002", "P0001"), muro(ana));
        assertEquals(List.of("P0004", "P0003", "P0002", "P0001"), muro(luis));
        assertEquals(List.of("P0003", "P0002"), muro(maria));
        assertEquals("Luis Rojas", gestor.obtenerMuro(ana).get(1).autor().getNombre());
    }

    /** Prueba: Al agregar un amigo, sus publicaciones anteriores aparecen en el muro. */
    @Test
    @DisplayName("Al agregar un amigo, sus publicaciones anteriores aparecen en el muro")
    void nuevoAmigoApareceEnMuro() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        publicar(maria, "Antes de ser amigas");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of(), muro(ana));
        amistades.agregarAmistad(ana, "U003");
        assertEquals(List.of("P0001"), muro(ana));
    }

    /** Prueba: Persistencia: textos con |, \\ y saltos de línea sobreviven a un reinicio y el ID continúa. */
    @Test
    @DisplayName("Persistencia: textos con |, \\ y saltos de línea sobreviven a un reinicio y el ID continúa")
    void guardarYRecargar() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        String raro = "Línea 1 | con barra\nLínea 2 con \\ invertida";
        publicar(ana, raro);
        publicar(luis, "Otra");

        GestorPublicaciones recargado = nuevoGestorCargado();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(raro, recargado.obtenerMuro(ana).get(0).publicacion().getTexto());
        assertEquals(2, Files.readAllLines(archivo, StandardCharsets.UTF_8).stream().filter(l -> l.startsWith("P")).count(),
                "cada publicación ocupa una sola línea");
        gestor = recargado;
        assertEquals("P0003", publicar(maria, "Nueva").getId());
    }

    /** Prueba: Archivo corrupto: omite líneas mal formadas, autores inexistentes e IDs repetidos. */
    @Test
    @DisplayName("Archivo corrupto: omite líneas mal formadas, autores inexistentes e IDs repetidos")
    void archivoCorrupto() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(archivo, String.join("\n",
                "# encabezado",
                "P0001|U001|2026-11-01T12:00:00Z|válida",
                "P0002|U001|2026-11-01T12:00:00Z",                 // faltan columnas
                "X1|U001|2026-11-01T12:00:00Z|id inválido",
                "P0003|U001|ayer|fecha inválida",
                "P0004|U001|2026-11-01T12:00:00Z|   ",              // texto vacío
                "P0005|U999|2026-11-01T12:00:00Z|autor inexistente",
                "P0001|U002|2026-11-02T12:00:00Z|id repetido",
                "P0006|U001|2026-11-03T12:00:00Z|válida \\| con escape"
        ) + "\n", StandardCharsets.UTF_8);

        GestorPublicaciones cargado = nuevoGestorCargado();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("P0006", "P0001"), cargado.obtenerMuro(ana).stream().map(e -> e.publicacion().getId()).toList());
        assertEquals("válida | con escape", cargado.obtenerMuro(ana).get(0).publicacion().getTexto());
        assertEquals(6, auditoria.contar("LINEA_CORRUPTA_OMITIDA"));
    }

    /** Prueba: Si la carga falla, no se permite guardar ni publicar (para no borrar publicaciones.txt). */
    @Test
    @DisplayName("Si la carga falla, no se permite guardar ni publicar (para no borrar publicaciones.txt)")
    void cargaFallidaImpideGuardar() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.delete(archivo);
        Files.createDirectory(archivo);
        GestorPublicaciones conError = new GestorPublicaciones(new RepositorioPublicacionesArchivoTexto(archivo, auditoria),
                new RepositorioCompartidosArchivoTexto(carpeta.resolve("compartidos.txt"), auditoria),
                new HistorialPublicaciones(), new RegistroCompartidos(), directorio, amistades,
                ConsultaBloqueos.SIN_BLOQUEOS, auditoria, reloj);

        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionPersistencia.class, conError::cargarDatos);
        assertThrows(ExcepcionPersistencia.class, conError::guardarDatos);
        assertThrows(ExcepcionPersistencia.class, () -> conError.publicar(ana, "hola"));
    }
}
