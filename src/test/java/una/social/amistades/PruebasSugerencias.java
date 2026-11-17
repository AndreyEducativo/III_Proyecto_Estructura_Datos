/*
 * PruebasSugerencias.java
 *
 * Qué prueba: "Personas que quizás conozcas" con BFS de 2 niveles: solo amigos de amigos (no el
 * nivel 3), orden por amigos en común y desempate alfabético, exclusión de amigos, de uno mismo, de
 * solicitudes pendientes y de bloqueados, límite de 10, y el conteo de amigos en común.
 * Colabora con: SugeridorAmistades, EntornoSocial.
 */
package una.social.amistades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.modelo.Usuario;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de {@link SugeridorAmistades}.
 */
class PruebasSugerencias {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Entorno social completo. */
    private EntornoSocial e;

    /** Ana, centro de la red de prueba. */
    private Usuario ana;

    /**
     * Red de prueba:
     * <pre>
     *   Ana ── Luis ── Pedro ── Diego        (Diego está a 3 pasos de Ana: no se sugiere)
     *    │      │       │
     *    └─ María ──────┘
     *           │
     *         Elena            Luis ── Sofía
     * </pre>
     * Para Ana: Pedro (2 en común: Luis y María), Elena (1: María), Sofía (1: Luis).
     */
    @BeforeEach
    void preparar() {
        // Usuarios extra para formar la red del dibujo de arriba.
        e = new EntornoSocial(carpeta);
        e.agregarUsuario("U004", "Pedro Solís", "pedro");
        e.agregarUsuario("U005", "Sofía Brenes", "sofia");
        e.agregarUsuario("U006", "Diego Castro", "diego");
        e.agregarUsuario("U007", "Elena Mora", "elena");
        ana = e.u("U001");
        // Aristas del grafo de amistades.
        amistad("U001", "U002");
        amistad("U001", "U003");
        amistad("U002", "U004");
        amistad("U003", "U004");
        amistad("U002", "U005");
        amistad("U004", "U006");
        amistad("U003", "U007");
    }

    /**
     * @param a un usuario
     * @param b otro usuario
     */
    private void amistad(String a, String b) {
        e.m().amistades().agregarAmistad(e.u(a), b);
    }

    /**
     * @return IDs sugeridos a Ana, en orden
     */
    private List<String> sugeridosParaAna() {
        return e.m().sugeridor().sugerir(ana).stream().map(s -> s.usuario().getId()).toList();
    }

    /** Prueba: BFS: sugiere solo el nivel 2, ordenado por amigos en común y luego alfabéticamente. */
    @Test
    @DisplayName("BFS: sugiere solo el nivel 2, ordenado por amigos en común y luego alfabéticamente")
    void ordenPorAmigosEnComun() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        List<Sugerencia> sugerencias = e.m().sugeridor().sugerir(ana);
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U004", "U007", "U005"), sugerencias.stream().map(s -> s.usuario().getId()).toList());
        assertEquals(List.of(2, 1, 1), sugerencias.stream().map(Sugerencia::amigosEnComun).toList());
    }

    /** Prueba: Excluye a quien tiene una solicitud pendiente conmigo (en cualquier sentido) y a los bloqueados. */
    @Test
    @DisplayName("Excluye a quien tiene una solicitud pendiente conmigo (en cualquier sentido) y a los bloqueados")
    void excluyeSolicitudesYBloqueos() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        e.m().amistades().enviarSolicitud(e.u("U005"), ana.getId());
        e.m().bloqueos().bloquear(ana, "U007");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U004"), sugeridosParaAna());
        e.m().amistades().enviarSolicitud(ana, "U004");
        assertTrue(sugeridosParaAna().isEmpty());
    }

    /** Prueba: Al hacerse amigos, la persona deja de sugerirse; amigos en común = intersección de listas. */
    @Test
    @DisplayName("Al hacerse amigos, la persona deja de sugerirse; amigos en común = intersección de listas")
    void amigosEnComunYAmistadNueva() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertEquals(2, e.m().sugeridor().amigosEnComun("U001", "U004"));
        assertEquals(0, e.m().sugeridor().amigosEnComun("U001", "U006"));
        amistad("U001", "U004");
        assertEquals(List.of("U006", "U007", "U005"), sugeridosParaAna(),
                "Pedro ya es amigo; ahora Diego queda a 2 pasos (a través de Pedro)");
    }

    /** Prueba: Muestra como máximo 10 sugerencias. */
    @Test
    @DisplayName("Muestra como máximo 10 sugerencias")
    void maximoDiez() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        for (int i = 10; i < 25; i++) {
            String id = String.format("U%03d", i);
            e.agregarUsuario(id, "Persona " + i, "persona" + i);
            amistad("U002", id);
        }
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(ServicioSugerencias.MAXIMO_SUGERENCIAS, e.m().sugeridor().sugerir(ana).size());
    }
}
