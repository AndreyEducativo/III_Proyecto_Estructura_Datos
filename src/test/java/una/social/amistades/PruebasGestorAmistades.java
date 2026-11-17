/*
 * PruebasGestorAmistades.java
 *
 * Qué prueba: "Agregar como amigo" con sus reglas, la lista de amigos, la persistencia en
 * amistades.txt (guardar y recargar), las líneas corruptas o inválidas y la protección ante una
 * carga fallida.
 * Colabora con: GestorAmistades, RepositorioAmistadesArchivoTexto, DirectorioEnMemoria,
 * AuditoriaEnMemoria, RelojAjustable.
 */
package una.social.amistades;

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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.excepciones.ExcepcionAmistadInvalida;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.soporte.AuditoriaEnMemoria;
import una.social.soporte.DirectorioEnMemoria;
import una.social.soporte.RelojAjustable;

/**
 * Pruebas de {@link GestorAmistades}.
 */
class PruebasGestorAmistades {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Archivo de amistades. */
    private Path archivo;

    /** Usuarios de prueba. */
    private DirectorioEnMemoria directorio;

    /** Bitácora en memoria. */
    private AuditoriaEnMemoria auditoria;

    /** Reloj controlable. */
    private RelojAjustable reloj;

    /** Gestor bajo prueba (ya cargado). */
    private GestorAmistades gestor;

    /** Prepara un gestor vacío antes de cada prueba. */
    @BeforeEach
    void preparar() {
        // Archivo en una carpeta temporal, 3 usuarios de prueba, auditoría en memoria y reloj fijo.
        archivo = carpeta.resolve("amistades.txt");
        directorio = DirectorioEnMemoria.conUsuariosDePrueba();
        auditoria = new AuditoriaEnMemoria();
        reloj = new RelojAjustable(Instant.parse("2026-11-01T14:00:00Z"));
        gestor = nuevoGestorCargado();
    }

    /**
     * @return gestor nuevo sobre el mismo archivo, ya cargado (simula reabrir la app)
     */
    private GestorAmistades nuevoGestorCargado() {
        // Mismas piezas que usa el programa real, pero sobre la carpeta temporal y sin bloqueos.
        GestorAmistades nuevo = new GestorAmistades(new RepositorioAmistadesArchivoTexto(archivo, auditoria),
                new RepositorioSolicitudesArchivoTexto(carpeta.resolve("solicitudes.txt"), auditoria),
                new GrafoAmistades(), directorio, ConsultaBloqueos.SIN_BLOQUEOS, auditoria, reloj);
        nuevo.cargarDatos();
        return nuevo;
    }

    /** Prueba: Agregar como amigo crea una amistad mutua, se guarda en disco y se audita. */
    @Test
    @DisplayName("Agregar como amigo crea una amistad mutua, se guarda en disco y se audita")
    void agregarAmigo() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Amistad creada = gestor.agregarAmistad(directorio.obtener("U001"), "U002");

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals("U001", creada.idUsuarioA());
        assertTrue(gestor.sonAmigos("U001", "U002") && gestor.sonAmigos("U002", "U001"));
        assertEquals(Set.of("U002"), gestor.idsAmigos("U001"));
        assertTrue(Files.readString(archivo, StandardCharsets.UTF_8).contains("U001|U002|2026-11-01T14:00:00Z"));
        assertEquals(1, auditoria.contar("AMISTAD_AGREGADA"));
    }

    /** Prueba: Reglas: no puede agregarse a sí mismo, ni a un amigo existente, ni a un usuario que no existe. */
    @Test
    @DisplayName("Reglas: no puede agregarse a sí mismo, ni a un amigo existente, ni a un usuario que no existe")
    void reglasDeAmistad() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.agregarAmistad(directorio.obtener("U001"), "U002");
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.agregarAmistad(directorio.obtener("U001"), "U001"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.agregarAmistad(directorio.obtener("U002"), "U001"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.agregarAmistad(directorio.obtener("U001"), "U999"));
        assertEquals(3, auditoria.contar("AMISTAD_RECHAZADA"));
        assertEquals(1, gestor.idsAmigos("U001").size());
    }

    /** Prueba: Listar amigos: orden alfabético y fecha de la amistad. */
    @Test
    @DisplayName("Listar amigos: orden alfabético y fecha de la amistad")
    void listarAmigos() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.agregarAmistad(directorio.obtener("U002"), "U003");
        reloj.adelantar(Duration.ofDays(1));
        gestor.agregarAmistad(directorio.obtener("U002"), "U001");

        List<Amigo> amigos = gestor.listarAmigos("U002");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("Ana Mora", "María Vargas"), amigos.stream().map(a -> a.usuario().getNombre()).toList());
        assertEquals(Instant.parse("2026-11-02T14:00:00Z"), amigos.get(0).amigosDesde());
        assertTrue(gestor.listarAmigos("U001").stream().anyMatch(a -> a.usuario().getId().equals("U002")));
    }

    /** Prueba: Persistencia: las amistades sobreviven a un reinicio de la aplicación. */
    @Test
    @DisplayName("Persistencia: las amistades sobreviven a un reinicio de la aplicación")
    void guardarYRecargar() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.agregarAmistad(directorio.obtener("U001"), "U002");
        gestor.agregarAmistad(directorio.obtener("U002"), "U003");

        GestorAmistades recargado = nuevoGestorCargado();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(Set.of("U001", "U003"), recargado.idsAmigos("U002"));
        recargado.guardarDatos();
        assertEquals(Set.of("U002"), nuevoGestorCargado().idsAmigos("U003"));
    }

    /** Prueba: Archivo corrupto: omite y audita líneas mal formadas, usuarios inexistentes, lazos y repetidos. */
    @Test
    @DisplayName("Archivo corrupto: omite y audita líneas mal formadas, usuarios inexistentes, lazos y repetidos")
    void archivoCorrupto() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(archivo, String.join("\n",
                "# encabezado",
                "U001|U002|2026-11-01T14:00:00Z",   // válida
                "U001|U002",                        // faltan columnas
                "U001|X9|2026-11-01T14:00:00Z",     // ID inválido
                "U001|U003|ayer",                   // fecha inválida
                "U001|U999|2026-11-01T14:00:00Z",   // usuario inexistente
                "U003|U003|2026-11-01T14:00:00Z",   // lazo
                "U002|U001|2026-11-01T14:00:00Z",   // repetida (orden inverso)
                "U002|U003|2026-11-01T14:00:00Z"    // válida
        ) + "\n", StandardCharsets.UTF_8);

        GestorAmistades cargado = nuevoGestorCargado();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(Set.of("U001", "U003"), cargado.idsAmigos("U002"));
        assertEquals(6, auditoria.contar("LINEA_CORRUPTA_OMITIDA"));
    }

    /** Prueba: Si la carga falla, no se permite guardar ni agregar (para no borrar amistades.txt). */
    @Test
    @DisplayName("Si la carga falla, no se permite guardar ni agregar (para no borrar amistades.txt)")
    void cargaFallidaImpideGuardar() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.delete(archivo);
        Files.createDirectory(archivo); // ahora no se puede leer como archivo
        GestorAmistades conError = new GestorAmistades(new RepositorioAmistadesArchivoTexto(archivo, auditoria),
                new RepositorioSolicitudesArchivoTexto(carpeta.resolve("solicitudes.txt"), auditoria),
                new GrafoAmistades(), directorio, ConsultaBloqueos.SIN_BLOQUEOS, auditoria, reloj);

        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionPersistencia.class, conError::cargarDatos);
        assertThrows(ExcepcionPersistencia.class, conError::guardarDatos);
        assertThrows(ExcepcionPersistencia.class, () -> conError.agregarAmistad(directorio.obtener("U001"), "U002"));
        assertThrows(ExcepcionPersistencia.class, () -> conError.enviarSolicitud(directorio.obtener("U001"), "U002"));
    }

    // ------------------------------------------------------------------ solicitudes de amistad

    /** Prueba: Enviar solicitud NO crea la amistad: queda pendiente para el destinatario y sobrevive a reabrir la app. */
    @Test
    @DisplayName("Enviar solicitud NO crea la amistad: queda pendiente para el destinatario y sobrevive a reabrir la app")
    void enviarSolicitudQuedaPendiente() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");

        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(gestor.sonAmigos("U001", "U003"), "todavía no son amigos");
        assertTrue(gestor.haySolicitudPendiente("U001", "U003"));
        assertFalse(gestor.haySolicitudPendiente("U003", "U001"), "la solicitud tiene dirección");
        GestorAmistades reabierto = nuevoGestorCargado();
        List<SolicitudRecibida> deMaria = reabierto.listarSolicitudesRecibidas("U003");
        assertEquals(1, deMaria.size());
        assertEquals("U001", deMaria.get(0).remitente().getId());
        assertTrue(reabierto.listarSolicitudesRecibidas("U001").isEmpty());
        assertEquals(1, auditoria.contar("SOLICITUD_ENVIADA"));
    }

    /** Prueba: Aceptar una solicitud crea la amistad mutua y borra la solicitud (también en disco). */
    @Test
    @DisplayName("Aceptar una solicitud crea la amistad mutua y borra la solicitud (también en disco)")
    void aceptarSolicitud() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");

        Amistad creada = gestor.aceptarSolicitud(directorio.obtener("U003"), "U001");

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals("U001", creada.idUsuarioA());
        assertTrue(gestor.sonAmigos("U003", "U001"));
        assertTrue(gestor.listarSolicitudesRecibidas("U003").isEmpty());
        GestorAmistades reabierto = nuevoGestorCargado();
        assertTrue(reabierto.sonAmigos("U001", "U003"));
        assertFalse(reabierto.haySolicitudPendiente("U001", "U003"));
        assertEquals(1, auditoria.contar("SOLICITUD_ACEPTADA"));
    }

    /** Prueba: Rechazar una solicitud la borra sin crear amistad; luego se puede volver a enviar. */
    @Test
    @DisplayName("Rechazar una solicitud la borra sin crear amistad; luego se puede volver a enviar")
    void rechazarSolicitud() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");

        gestor.rechazarSolicitud(directorio.obtener("U003"), "U001");

        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(gestor.sonAmigos("U001", "U003"));
        assertFalse(gestor.haySolicitudPendiente("U001", "U003"));
        assertFalse(nuevoGestorCargado().haySolicitudPendiente("U001", "U003"));
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");
        assertTrue(gestor.haySolicitudPendiente("U001", "U003"));
        assertEquals(1, auditoria.contar("SOLICITUD_RECHAZADA"));
    }

    /** Prueba: Quien envió la solicitud puede cancelarla mientras esté pendiente. */
    @Test
    @DisplayName("Quien envió la solicitud puede cancelarla mientras esté pendiente")
    void cancelarSolicitud() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");

        gestor.cancelarSolicitud(directorio.obtener("U001"), "U003");

        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(gestor.listarSolicitudesRecibidas("U003").isEmpty());
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.aceptarSolicitud(directorio.obtener("U003"), "U001"),
                "ya no se puede aceptar una solicitud cancelada");
    }

    /** Prueba: Reglas: no a sí mismo, no repetida, no si ya son amigos, no a usuarios inexistentes. */
    @Test
    @DisplayName("Reglas: no a sí mismo, no repetida, no si ya son amigos, no a usuarios inexistentes")
    void reglasDeSolicitudes() {
        gestor.agregarAmistad(directorio.obtener("U001"), "U002");
        gestor.enviarSolicitud(directorio.obtener("U001"), "U003");

        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.enviarSolicitud(directorio.obtener("U001"), "U001"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.enviarSolicitud(directorio.obtener("U001"), "U003"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.enviarSolicitud(directorio.obtener("U002"), "U001"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.enviarSolicitud(directorio.obtener("U001"), "U999"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.rechazarSolicitud(directorio.obtener("U002"), "U001"),
                "no se puede rechazar una solicitud que no existe");
        assertEquals(5, auditoria.contar("OPERACION_AMISTAD_INVALIDA"));
        // Una solicitud en sentido CONTRARIO ya no es un error: se acepta automáticamente (ver
        // PruebasSolicitudesYBloqueos).
    }

    /** Prueba: Al cargar se omiten solicitudes inválidas (usuario inexistente, consigo mismo, ya son amigos). */
    @Test
    @DisplayName("Al cargar se omiten solicitudes inválidas (usuario inexistente, consigo mismo, ya son amigos)")
    void omiteSolicitudesInvalidasAlCargar() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(archivo, "U001|U002|2026-11-01T14:00:00Z\n", StandardCharsets.UTF_8);
        Files.writeString(carpeta.resolve("solicitudes.txt"), String.join("\n",
                "U001|U003|2026-11-01T14:00:00Z",   // válida
                "U001|U002|2026-11-01T14:00:00Z",   // ya son amigos
                "U002|U002|2026-11-01T14:00:00Z",   // consigo mismo
                "U009|U001|2026-11-01T14:00:00Z",   // remitente no existe
                "U001|U003|2026-11-02T14:00:00Z",   // repetida
                "basura") + "\n", StandardCharsets.UTF_8);

        GestorAmistades cargado = nuevoGestorCargado();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(1, cargado.listarSolicitudesRecibidas("U003").size());
        assertTrue(cargado.listarSolicitudesRecibidas("U001").isEmpty());
        assertEquals(5, auditoria.contar("LINEA_CORRUPTA_OMITIDA"));
    }

    // ------------------------------------------------------------------ eliminar amigos

    /** Prueba: Eliminar amigo borra la amistad para los dos y se guarda en disco. */
    @Test
    @DisplayName("Eliminar amigo borra la amistad para los dos y se guarda en disco")
    void eliminarAmigo() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.agregarAmistad(directorio.obtener("U001"), "U002");
        gestor.agregarAmistad(directorio.obtener("U002"), "U003");

        gestor.eliminarAmistad(directorio.obtener("U002"), "U001");

        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(gestor.sonAmigos("U001", "U002"));
        assertTrue(gestor.idsAmigos("U001").isEmpty(), "Ana ya no tiene a Luis");
        assertEquals(Set.of("U003"), gestor.idsAmigos("U002"), "Luis conserva a Maria");
        GestorAmistades reabierto = nuevoGestorCargado();
        assertFalse(reabierto.sonAmigos("U001", "U002"));
        assertTrue(reabierto.sonAmigos("U002", "U003"));
        assertEquals(1, auditoria.contar("AMISTAD_ELIMINADA"));
        assertThrows(ExcepcionAmistadInvalida.class, () -> gestor.eliminarAmistad(directorio.obtener("U001"), "U002"),
                "no se puede eliminar a alguien que ya no es amigo");
    }

    /** Prueba: Después de eliminar a un amigo se le puede volver a enviar una solicitud. */
    @Test
    @DisplayName("Después de eliminar a un amigo se le puede volver a enviar una solicitud")
    void volverAEnviarTrasEliminar() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        gestor.agregarAmistad(directorio.obtener("U001"), "U002");
        gestor.eliminarAmistad(directorio.obtener("U001"), "U002");

        gestor.enviarSolicitud(directorio.obtener("U002"), "U001");
        gestor.aceptarSolicitud(directorio.obtener("U001"), "U002");

        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(gestor.sonAmigos("U001", "U002"));
    }
}
