/*
 * PruebasSolicitudesYBloqueos.java
 *
 * Qué prueba: la COLA de solicitudes (orden de llegada, también tras reabrir la app), la aceptación
 * automática cuando las dos personas se envían solicitud, la lista de enviadas, y los bloqueos:
 * reglas, que rompen la amistad y las solicitudes, que impiden nuevas solicitudes, que se guardan y
 * que el archivo corrupto no tumba la carga.
 * Colabora con: GestorAmistades, BandejaSolicitudes, GestorBloqueos, EntornoSocial.
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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.excepciones.ExcepcionAmistadInvalida;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de solicitudes (cola y aceptación automática) y de bloqueos.
 */
class PruebasSolicitudesYBloqueos {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Entorno social completo. */
    private EntornoSocial e;

    /** Ana (U001), Luis (U002), María (U003) y Pedro (U004). */
    private Usuario ana;
    /** Luis. */
    private Usuario luis;
    /** María. */
    private Usuario maria;
    /** Pedro. */
    private Usuario pedro;

    /** Prepara el entorno antes de cada prueba. */
    @BeforeEach
    void preparar() {
        // Entorno completo con Ana, Luis y María, más un cuarto usuario (Pedro).
        e = new EntornoSocial(carpeta);
        pedro = e.agregarUsuario("U004", "Pedro Solís", "pedro");
        ana = e.u("U001");
        luis = e.u("U002");
        maria = e.u("U003");
    }

    /**
     * @param idUsuario destinatario
     * @return IDs de los remitentes de sus solicitudes, en el orden mostrado
     */
    private List<String> remitentes(String idUsuario) {
        return e.m().amistades().listarSolicitudesRecibidas(idUsuario).stream().map(s -> s.remitente().getId()).toList();
    }

    // ------------------------------------------------------------------ cola de solicitudes

    /** Prueba: Cola FIFO: las solicitudes recibidas se muestran en el orden en que llegaron, también tras reabrir. */
    @Test
    @DisplayName("Cola FIFO: las solicitudes recibidas se muestran en el orden en que llegaron, también tras reabrir")
    void colaEnOrdenDeLlegada() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().enviarSolicitud(pedro, ana.getId());
        e.avanzar();
        e.m().amistades().enviarSolicitud(luis, ana.getId());
        e.avanzar();
        e.m().amistades().enviarSolicitud(maria, ana.getId());

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U004", "U002", "U003"), remitentes(ana.getId()));
        e.m().amistades().rechazarSolicitud(ana, luis.getId());
        assertEquals(List.of("U004", "U003"), remitentes(ana.getId()), "quitar una del medio conserva el orden");
        e.recargar();
        assertEquals(List.of("U004", "U003"), remitentes(ana.getId()));
    }

    /** Prueba: BandejaSolicitudes: encolar al final, quitar y quitar entre dos usuarios. */
    @Test
    @DisplayName("BandejaSolicitudes: encolar al final, quitar y quitar entre dos usuarios")
    void bandejaComoCola() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        BandejaSolicitudes bandeja = new BandejaSolicitudes();
        Instant t = Instant.parse("2026-11-01T12:00:00Z");
        bandeja.encolar(new SolicitudAmistad("U002", "U001", t));
        bandeja.encolar(new SolicitudAmistad("U003", "U001", t.plusSeconds(1)));
        bandeja.encolar(new SolicitudAmistad("U001", "U003", t.plusSeconds(2)));
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U002", "U003"), bandeja.recibidasPor("U001").stream().map(SolicitudAmistad::idRemitente).toList());
        assertEquals(1, bandeja.enviadasPor("U001").size());
        assertEquals(2, bandeja.quitarEntre("U001", "U003"));
        assertEquals(1, bandeja.tamano());
        assertFalse(bandeja.quitar("U009", "U001"));
    }

    /** Prueba: Aceptación automática: si B ya le envió solicitud a A y A le envía a B, quedan como amigos. */
    @Test
    @DisplayName("Aceptación automática: si B ya le envió solicitud a A y A le envía a B, quedan como amigos")
    void aceptacionAutomatica() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().enviarSolicitud(maria, ana.getId());
        ResultadoEnvioSolicitud resultado = e.m().amistades().enviarSolicitud(ana, maria.getId());

        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(resultado.seAceptoAutomaticamente());
        assertTrue(e.m().amistades().sonAmigos(ana.getId(), maria.getId()));
        assertTrue(remitentes(ana.getId()).isEmpty());
        assertTrue(e.m().amistades().listarSolicitudesEnviadas(maria.getId()).isEmpty());
    }

    /** Prueba: Solicitudes enviadas: se listan con su destinatario y se pueden cancelar. */
    @Test
    @DisplayName("Solicitudes enviadas: se listan con su destinatario y se pueden cancelar")
    void solicitudesEnviadas() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().enviarSolicitud(ana, luis.getId());
        e.avanzar();
        e.m().amistades().enviarSolicitud(ana, maria.getId());
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U002", "U003"), e.m().amistades().listarSolicitudesEnviadas(ana.getId()).stream()
                .map(s -> s.destinatario().getId()).toList());
        e.m().amistades().cancelarSolicitud(ana, luis.getId());
        assertEquals(1, e.m().amistades().listarSolicitudesEnviadas(ana.getId()).size());
    }

    // ------------------------------------------------------------------ bloqueos

    /** Prueba: Bloquear elimina la amistad y las solicitudes entre ambos, e impide enviar nuevas. */
    @Test
    @DisplayName("Bloquear elimina la amistad y las solicitudes entre ambos, e impide enviar nuevas")
    void bloquearRompeLaRelacion() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().agregarAmistad(ana, luis.getId());
        e.m().amistades().enviarSolicitud(maria, ana.getId());
        e.m().amistades().enviarSolicitud(ana, pedro.getId());

        e.m().bloqueos().bloquear(ana, luis.getId());
        e.m().bloqueos().bloquear(ana, maria.getId());

        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(e.m().amistades().sonAmigos(ana.getId(), luis.getId()));
        assertTrue(remitentes(ana.getId()).isEmpty(), "la solicitud de María se canceló");
        assertEquals(1, e.m().amistades().listarSolicitudesEnviadas(ana.getId()).size(), "la de Pedro sigue");
        assertTrue(e.m().bloqueos().hayBloqueoEntre(luis.getId(), ana.getId()), "el bloqueo se ve desde los dos lados");
        assertThrows(ExcepcionAmistadInvalida.class, () -> e.m().amistades().enviarSolicitud(luis, ana.getId()));
        assertEquals(2, e.auditoria().contar("USUARIO_BLOQUEADO"), "se auditaron los dos bloqueos");
    }

    /** Prueba: Reglas de bloqueo: no a sí mismo, no a inexistentes, no dos veces; desbloquear solo lo bloqueado. */
    @Test
    @DisplayName("Reglas de bloqueo: no a sí mismo, no a inexistentes, no dos veces; desbloquear solo lo bloqueado")
    void reglasDeBloqueo() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().bloqueos().bloquear(ana, ana.getId()));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().bloqueos().bloquear(ana, "U999"));
        e.m().bloqueos().bloquear(ana, luis.getId());
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().bloqueos().bloquear(ana, luis.getId()));
        assertThrows(ExcepcionReglaNegocio.class, () -> e.m().bloqueos().desbloquear(luis, ana.getId()),
                "solo quien bloqueó puede desbloquear");
        assertTrue(e.m().bloqueos().bloqueoA(ana.getId(), luis.getId()));
        assertFalse(e.m().bloqueos().bloqueoA(luis.getId(), ana.getId()));
    }

    /** Prueba: Desbloquear NO restaura la amistad; los bloqueos sobreviven a reabrir la app. */
    @Test
    @DisplayName("Desbloquear NO restaura la amistad; los bloqueos sobreviven a reabrir la app")
    void desbloquearYPersistencia() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().agregarAmistad(ana, luis.getId());
        e.m().bloqueos().bloquear(ana, luis.getId());
        e.m().bloqueos().bloquear(ana, maria.getId());
        e.recargar();
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U002", "U003"), e.m().bloqueos().listarBloqueados(ana.getId()).stream().map(Usuario::getId).toList());

        e.m().bloqueos().desbloquear(ana, luis.getId());
        assertFalse(e.m().bloqueos().hayBloqueoEntre(ana.getId(), luis.getId()));
        assertFalse(e.m().amistades().sonAmigos(ana.getId(), luis.getId()));
        e.recargar();
        assertEquals(1, e.m().bloqueos().listarBloqueados(ana.getId()).size());
    }

    /** Prueba: bloqueos.txt corrupto: se omiten y auditan líneas mal formadas, inexistentes, lazos y repetidas. */
    @Test
    @DisplayName("bloqueos.txt corrupto: se omiten y auditan líneas mal formadas, inexistentes, lazos y repetidas")
    void archivoDeBloqueosCorrupto() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.writeString(e.archivo("bloqueos.txt"), String.join("\n",
                "# encabezado",
                "U001|U002|2026-11-01T12:00:00Z",   // válida
                "U001|U002",                        // faltan columnas
                "U001|U999|2026-11-01T12:00:00Z",   // usuario inexistente
                "U003|U003|2026-11-01T12:00:00Z",   // lazo
                "U001|U002|2026-11-02T12:00:00Z",   // repetida
                "U003|U001|ayer"                    // fecha inválida
        ) + "\n", StandardCharsets.UTF_8);
        e.recargar();
        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(e.m().bloqueos().hayBloqueoEntre("U001", "U002"));
        assertEquals(1, e.m().bloqueos().listarBloqueados("U001").size());
        assertEquals(5, e.auditoria().contar("LINEA_CORRUPTA_OMITIDA"));
    }
}
