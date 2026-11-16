/*
 * PruebasCuenta.java
 *
 * Qué prueba: cambiar el nombre y la contraseña de la cuenta (ServicioCuenta): validaciones, que la
 * contraseña vieja deja de servir y la nueva funciona, que se guarda en disco y que con PBKDF2 se
 * genera una sal NUEVA.
 * Colabora con: GestorUsuarios, HasheadorTextoPlano, HasheadorPbkdf2, AuditoriaEnMemoria.
 */
package una.social.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import una.social.excepciones.ExcepcionAutenticacion;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;
import una.social.modelo.CatalogoUsuarios;
import una.social.modelo.Usuario;
import una.social.persistencia.RepositorioUsuariosArchivoTexto;
import una.social.seguridad.ControlIntentosLogin;
import una.social.seguridad.HasheadorContrasenas;
import una.social.seguridad.HasheadorPbkdf2;
import una.social.seguridad.HasheadorTextoPlano;
import una.social.soporte.AuditoriaEnMemoria;
import una.social.soporte.RelojAjustable;

/**
 * Pruebas de {@link ServicioCuenta} implementado por {@link GestorUsuarios}.
 */
class PruebasCuenta {

    /** Pocas iteraciones de PBKDF2 para que las pruebas sean rápidas. */
    private static final int ITERACIONES_PRUEBA = 1_000;

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Bitácora en memoria. */
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();

    /**
     * Crea y carga un gestor sobre usuarios.txt de la carpeta temporal.
     *
     * @param hasheador hasheador a usar
     * @return gestor cargado
     */
    private GestorUsuarios gestor(HasheadorContrasenas hasheador) {
        RelojAjustable reloj = new RelojAjustable(Instant.parse("2026-11-15T18:00:00Z"));
        GestorUsuarios gestor = new GestorUsuarios(new RepositorioUsuariosArchivoTexto(carpeta.resolve("usuarios.txt"), auditoria),
                new CatalogoUsuarios(), hasheador, new ControlIntentosLogin(5, Duration.ofMinutes(5), reloj), auditoria, reloj);
        gestor.cargarDatos();
        return gestor;
    }

    /**
     * @param texto texto
     * @return el texto como arreglo de caracteres
     */
    private static char[] c(String texto) {
        return texto.toCharArray();
    }

    @Test
    @DisplayName("Cambiar contraseña: la vieja deja de servir, la nueva funciona y se conserva al reabrir")
    void cambiarContrasena() {
        GestorUsuarios gestor = gestor(new HasheadorTextoPlano());
        Usuario ana = gestor.registrar("Ana Mora", "ana", c("ana12345"), c("ana12345"));

        gestor.cambiarContrasena(ana, c("ana12345"), c("nueva123"), c("nueva123"));

        assertThrows(ExcepcionAutenticacion.class, () -> gestor.autenticar("ana", c("ana12345")));
        assertEquals(ana.getId(), gestor.autenticar("ana", c("nueva123")).getId());
        GestorUsuarios reabierto = gestor(new HasheadorTextoPlano());
        assertEquals(ana.getId(), reabierto.autenticar("ana", c("nueva123")).getId());
        assertEquals(1, auditoria.contar("CONTRASENA_CAMBIADA"));
        assertFalse(auditoria.comoTexto().contains("nueva123"), "la contraseña nunca va a la auditoría");
    }

    @Test
    @DisplayName("Cambiar contraseña: actual incorrecta, nueva corta o confirmación distinta se rechazan en su campo")
    void validacionesDeContrasena() {
        GestorUsuarios gestor = gestor(new HasheadorTextoPlano());
        Usuario ana = gestor.registrar("Ana Mora", "ana", c("ana12345"), c("ana12345"));

        assertCampo(CampoFormulario.CONTRASENA_ACTUAL, () -> gestor.cambiarContrasena(ana, c("mala"), c("nueva123"), c("nueva123")));
        assertCampo(CampoFormulario.CONTRASENA_ACTUAL, () -> gestor.cambiarContrasena(ana, c(""), c("nueva123"), c("nueva123")));
        assertCampo(CampoFormulario.CONTRASENA, () -> gestor.cambiarContrasena(ana, c("ana12345"), c("123"), c("123")));
        assertCampo(CampoFormulario.CONFIRMACION, () -> gestor.cambiarContrasena(ana, c("ana12345"), c("nueva123"), c("otra1234")));
        assertEquals(ana.getId(), gestor.autenticar("ana", c("ana12345")).getId(), "nada cambió");
    }

    @Test
    @DisplayName("Con PBKDF2, cambiar la contraseña genera una sal NUEVA")
    void salNuevaConPbkdf2() {
        GestorUsuarios gestor = gestor(new HasheadorPbkdf2(ITERACIONES_PRUEBA, new SecureRandom()));
        Usuario ana = gestor.registrar("Ana Mora", "ana", c("ana12345"), c("ana12345"));
        String salAnterior = gestor.buscarPorId(ana.getId()).orElseThrow().getSal();

        gestor.cambiarContrasena(ana, c("ana12345"), c("nueva123"), c("nueva123"));

        Usuario actualizado = gestor.buscarPorId(ana.getId()).orElseThrow();
        assertNotEquals(salAnterior, actualizado.getSal());
        assertEquals(ana.getId(), gestor.autenticar("ana", c("nueva123")).getId());
    }

    @Test
    @DisplayName("Cambiar nombre: mismas reglas que el registro; ID y usuario no cambian; se guarda")
    void cambiarNombre() {
        GestorUsuarios gestor = gestor(new HasheadorTextoPlano());
        Usuario ana = gestor.registrar("Ana Mora", "ana", c("ana12345"), c("ana12345"));

        Usuario actualizado = gestor.cambiarNombre(ana, "  Ana María Mora  ");
        assertEquals("Ana María Mora", actualizado.getNombre());
        assertEquals(ana.getId(), actualizado.getId());
        assertEquals("ana", actualizado.getNombreUsuario());
        assertCampo(CampoFormulario.NOMBRE, () -> gestor.cambiarNombre(ana, "   "));
        assertCampo(CampoFormulario.NOMBRE, () -> gestor.cambiarNombre(ana, "x".repeat(61)));

        assertEquals("Ana María Mora", gestor(new HasheadorTextoPlano()).buscarPorNombreUsuario("ANA").orElseThrow().getNombre());
    }

    /**
     * Comprueba que la acción lance ExcepcionValidacion sobre el campo indicado.
     *
     * @param campo  campo esperado
     * @param accion acción a ejecutar
     */
    private static void assertCampo(CampoFormulario campo, org.junit.jupiter.api.function.Executable accion) {
        ExcepcionValidacion ex = assertThrows(ExcepcionValidacion.class, accion);
        assertEquals(campo, ex.getCampo(), ex.getMessage());
    }
}
