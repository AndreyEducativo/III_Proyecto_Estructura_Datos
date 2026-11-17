/*
 * PruebasDatosSemillaSocial.java
 *
 * Qué prueba: que los datos de ejemplo del módulo social demuestren todas las funciones (amistades,
 * solicitud pendiente, privacidad, me gusta, comentarios, compartido, biografías y anuncios) y que
 * solo se creen una vez.
 * Colabora con: HerramientaDatosSemillaSocial, EntornoSocial.
 */
package una.social.herramientas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.modelo.Usuario;
import una.social.soporte.EntornoSocial;

/**
 * Pruebas de {@link HerramientaDatosSemillaSocial}.
 */
class PruebasDatosSemillaSocial {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Prueba: Datos de ejemplo: red Ana-Luis-María, solicitud de María a Ana, muros, interacciones y anuncios. */
    @Test
    @DisplayName("Datos de ejemplo: red Ana-Luis-María, solicitud de María a Ana, muros, interacciones y anuncios")
    void datosDeEjemplo() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        EntornoSocial e = new EntornoSocial(carpeta);
        Usuario ana = e.u("U001");
        Usuario luis = e.u("U002");
        Usuario maria = e.u("U003");

        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(HerramientaDatosSemillaSocial.sembrarSiEstaVacio(e.directorio(), e.m(), e.auditoria()));

        assertTrue(e.m().amistades().sonAmigos("U001", "U002"));
        assertTrue(e.m().amistades().sonAmigos("U002", "U003"));
        assertFalse(e.m().amistades().sonAmigos("U001", "U003"));
        assertEquals(1, e.m().amistades().listarSolicitudesRecibidas("U001").size(), "María → Ana pendiente");
        assertEquals(5, e.m().publicaciones().obtenerMuro(ana).size(), "Ana: sus 2, las 2 de Luis y el compartido de Luis");
        assertEquals(3, e.m().publicaciones().obtenerMuroGeneral(ana).size(), "2 públicas + el compartido");
        assertEquals(7, e.m().publicaciones().obtenerMuro(luis).size());
        assertEquals(2, e.m().meGusta().estado("U001", "P0001").cantidad());
        assertEquals(1, e.m().comentarios().contar(ana, "P0001"));
        assertEquals(2, e.m().anuncios().listar(maria).size());
        assertFalse(e.m().perfiles().obtenerBiografia("U003").isEmpty());

        e.recargar();
        assertFalse(HerramientaDatosSemillaSocial.sembrarSiEstaVacio(e.directorio(), e.m(), e.auditoria()),
                "no se vuelven a crear si ya hay datos");
        assertEquals(7, e.m().publicaciones().obtenerMuro(luis).size());
    }
}
