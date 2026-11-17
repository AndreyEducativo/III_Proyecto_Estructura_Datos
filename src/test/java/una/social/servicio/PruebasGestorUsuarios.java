/*
 * PruebasGestorUsuarios.java
 *
 * Qué prueba: la lógica de negocio de usuarios con archivos reales en una carpeta temporal:
 * registro e IDs, login correcto e incorrecto, mensajes genéricos, usuario duplicado,
 * validaciones, bloqueo por intentos, persistencia (guardar y recargar), protección ante carga
 * fallida, que la contraseña se guarde en texto plano en usuarios.txt y que nunca se audite.
 * Colabora con: GestorUsuarios, RepositorioUsuariosArchivoTexto, HasheadorTextoPlano,
 * ControlIntentosLogin, AuditoriaEnMemoria, RelojAjustable.
 */
package una.social.servicio;

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
import una.social.excepciones.ExcepcionAutenticacion;
import una.social.excepciones.ExcepcionCuentaBloqueada;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionUsuarioDuplicado;
import una.social.excepciones.ExcepcionValidacion;
import una.social.herramientas.HerramientaDatosSemilla;
import una.social.modelo.CampoFormulario;
import una.social.modelo.CatalogoUsuarios;
import una.social.modelo.Usuario;
import una.social.persistencia.RepositorioUsuariosArchivoTexto;
import una.social.seguridad.ControlIntentosLogin;
import una.social.seguridad.HasheadorTextoPlano;
import una.social.soporte.AuditoriaEnMemoria;
import una.social.soporte.RelojAjustable;

/**
 * Pruebas de {@link GestorUsuarios}.
 */
class PruebasGestorUsuarios {

    /** Intentos antes del bloqueo (igual que en producción). */
    private static final int MAXIMO_INTENTOS = 5;

    /** Duración del bloqueo (igual que en producción). */
    private static final Duration BLOQUEO = Duration.ofMinutes(5);

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /** Archivo de usuarios dentro de la carpeta temporal. */
    private Path archivo;

    /** Bitácora en memoria para revisar qué se auditó. */
    private AuditoriaEnMemoria auditoria;

    /** Reloj controlable. */
    private RelojAjustable reloj;

    /** Gestor bajo prueba (ya cargado). */
    private GestorUsuarios gestor;

    /** Crea un gestor nuevo y vacío antes de cada prueba. */
    @BeforeEach
    void preparar() {
        archivo = carpeta.resolve("usuarios.txt");
        auditoria = new AuditoriaEnMemoria();
        reloj = new RelojAjustable(Instant.parse("2026-11-15T18:00:00Z"));
        gestor = nuevoGestorCargado();
    }

    /**
     * Arma un gestor con dependencias reales (salvo el reloj y la auditoría) y carga el archivo.
     *
     * @return gestor cargado
     */
    private GestorUsuarios nuevoGestorCargado() {
        // Las mismas piezas que arma EnsambladorServicios, pero sobre el archivo temporal.
        GestorUsuarios nuevo = new GestorUsuarios(
                new RepositorioUsuariosArchivoTexto(archivo, auditoria),
                new CatalogoUsuarios(),
                new HasheadorTextoPlano(),
                new ControlIntentosLogin(MAXIMO_INTENTOS, BLOQUEO, reloj),
                auditoria,
                reloj);
        // Lee usuarios.txt, como al abrir el programa.
        nuevo.cargarDatos();
        return nuevo;
    }

    /**
     * Registra un usuario con contraseña y confirmación iguales.
     *
     * @param nombre     nombre completo
     * @param usuario    nombre de usuario
     * @param contrasena contraseña
     * @return usuario creado
     */
    private Usuario registrar(String nombre, String usuario, String contrasena) {
        return gestor.registrar(nombre, usuario, contrasena.toCharArray(), contrasena.toCharArray());
    }

    // ------------------------------------------------------------------ registro

    /** Prueba: Registro: asigna IDs U001, U002... y guarda la contraseña en texto plano (no en la auditoría). */
    @Test
    @DisplayName("Registro: asigna IDs U001, U002... y guarda la contraseña en texto plano (no en la auditoría)")
    void registroAsignaIdsYGuardaContrasena() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Usuario ana = registrar("Ana Mora", "ana", "ana12345");
        Usuario luis = registrar("Luis Rojas", "luis", "luis12345");

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals("U001", ana.getId());
        assertEquals("U002", luis.getId());
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains("|ana|ana12345|"), "la contraseña se ve en usuarios.txt");
        assertFalse(auditoria.comoTexto().contains("ana12345"), "pero nunca en la auditoría");
        assertEquals(2, auditoria.contar("REGISTRO_EXITOSO"));
    }

    /** Prueba: Usuario duplicado: se rechaza sin distinguir mayúsculas y no se guarda. */
    @Test
    @DisplayName("Usuario duplicado: se rechaza sin distinguir mayúsculas y no se guarda")
    void usuarioDuplicado() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        registrar("Maria Vargas", "maria", "maria12345");
        ExcepcionUsuarioDuplicado ex = assertThrows(ExcepcionUsuarioDuplicado.class,
                () -> registrar("Otra Maria", "MARIA", "otra123456"));
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(CampoFormulario.USUARIO, ex.getCampo());
        assertEquals(1, gestor.listarTodos().size());
        assertEquals(1, auditoria.contar("REGISTRO_RECHAZADO"));
    }

    /** Prueba: Validaciones: campos vacíos, usuario inválido, contraseña corta y confirmación distinta. */
    @Test
    @DisplayName("Validaciones: campos vacíos, usuario inválido, contraseña corta y confirmación distinta")
    void validacionesDeRegistro() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertCampoInvalido(CampoFormulario.NOMBRE, () -> registrar("  ", "usuario", "clave123"));
        assertCampoInvalido(CampoFormulario.USUARIO, () -> registrar("Nombre", "", "clave123"));
        assertCampoInvalido(CampoFormulario.USUARIO, () -> registrar("Nombre", "ab", "clave123"));
        assertCampoInvalido(CampoFormulario.USUARIO, () -> registrar("Nombre", "con espacio", "clave123"));
        assertCampoInvalido(CampoFormulario.CONTRASENA, () -> registrar("Nombre", "usuario", ""));
        assertCampoInvalido(CampoFormulario.CONTRASENA, () -> registrar("Nombre", "usuario", "123"));
        assertCampoInvalido(CampoFormulario.CONTRASENA, () -> registrar("Nombre", "usuario", "        "));
        assertCampoInvalido(CampoFormulario.CONFIRMACION,
                () -> gestor.registrar("Nombre", "usuario", "clave123".toCharArray(), "clave124".toCharArray()));
        assertCampoInvalido(CampoFormulario.CONFIRMACION,
                () -> gestor.registrar("Nombre", "usuario", "clave123".toCharArray(), new char[0]));
        assertTrue(gestor.listarTodos().isEmpty(), "ningún registro inválido debe guardarse");
    }

    /**
     * Comprueba que la acción lance ExcepcionValidacion sobre el campo indicado.
     *
     * @param campo  campo esperado
     * @param accion acción a ejecutar
     */
    private static void assertCampoInvalido(CampoFormulario campo, org.junit.jupiter.api.function.Executable accion) {
        ExcepcionValidacion ex = assertThrows(ExcepcionValidacion.class, accion);
        assertEquals(campo, ex.getCampo(), ex.getMessage());
    }

    // ------------------------------------------------------------------ login

    /** Prueba: Login correcto: funciona sin distinguir mayúsculas en el usuario y se audita. */
    @Test
    @DisplayName("Login correcto: funciona sin distinguir mayúsculas en el usuario y se audita")
    void loginCorrecto() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Usuario ana = registrar("Ana Mora", "ana", "ana12345");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(ana, gestor.autenticar("ANA", "ana12345".toCharArray()));
        assertEquals(1, auditoria.contar("LOGIN_EXITOSO"));
    }

    /** Prueba: Login incorrecto: contraseña mala y usuario inexistente dan el MISMO mensaje genérico. */
    @Test
    @DisplayName("Login incorrecto: contraseña mala y usuario inexistente dan el MISMO mensaje genérico")
    void loginIncorrectoMensajeGenerico() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        registrar("Ana Mora", "ana", "ana12345");
        ExcepcionAutenticacion malaClave = assertThrows(ExcepcionAutenticacion.class,
                () -> gestor.autenticar("ana", "otra-clave".toCharArray()));
        ExcepcionAutenticacion noExiste = assertThrows(ExcepcionAutenticacion.class,
                () -> gestor.autenticar("nadie", "ana12345".toCharArray()));
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(ExcepcionAutenticacion.MENSAJE_GENERICO, malaClave.getMessage());
        assertEquals(malaClave.getMessage(), noExiste.getMessage());
        assertEquals(2, auditoria.contar("LOGIN_FALLIDO"));
    }

    /** Prueba: Login con campos vacíos: error de validación, sin contar como intento fallido. */
    @Test
    @DisplayName("Login con campos vacíos: error de validación, sin contar como intento fallido")
    void loginCamposVacios() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertCampoInvalido(CampoFormulario.USUARIO, () -> gestor.autenticar(" ", "x".toCharArray()));
        assertCampoInvalido(CampoFormulario.CONTRASENA, () -> gestor.autenticar("ana", new char[0]));
        assertEquals(0, auditoria.contar("LOGIN_FALLIDO"));
    }

    /** Prueba: Bloqueo: tras 5 fallos se bloquea (aun con la clave correcta) y se libera a los 5 minutos. */
    @Test
    @DisplayName("Bloqueo: tras 5 fallos se bloquea (aun con la clave correcta) y se libera a los 5 minutos")
    void bloqueoTemporal() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        registrar("Ana Mora", "ana", "ana12345");
        for (int i = 1; i < MAXIMO_INTENTOS; i++) {
            assertThrows(ExcepcionAutenticacion.class, () -> gestor.autenticar("ana", "mala".toCharArray()));
        }
        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionCuentaBloqueada.class, () -> gestor.autenticar("ana", "mala".toCharArray()));
        assertThrows(ExcepcionCuentaBloqueada.class, () -> gestor.autenticar("ana", "ana12345".toCharArray()));
        assertEquals(1, auditoria.contar("CUENTA_BLOQUEADA"));
        assertEquals(1, auditoria.contar("LOGIN_BLOQUEADO"));

        reloj.adelantar(BLOQUEO.plusSeconds(1));
        assertEquals("U001", gestor.autenticar("ana", "ana12345".toCharArray()).getId());
    }

    // ------------------------------------------------------------------ persistencia

    /** Prueba: Persistencia: lo registrado sobrevive a un reinicio y el siguiente ID continúa la numeración. */
    @Test
    @DisplayName("Persistencia: lo registrado sobrevive a un reinicio y el siguiente ID continúa la numeración")
    void guardarYRecargar() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        registrar("Ana Mora", "ana", "ana12345");
        registrar("Luis Rojas", "luis", "luis12345");

        gestor = nuevoGestorCargado(); // simula cerrar y volver a abrir la aplicación

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(2, gestor.listarTodos().size());
        assertEquals("U001", gestor.autenticar("ana", "ana12345".toCharArray()).getId());
        assertEquals("U003", registrar("Maria Vargas", "maria", "maria12345").getId());
        gestor.guardarDatos();
        assertEquals(3, nuevoGestorCargado().listarTodos().size());
    }

    /** Prueba: Archivo corrupto: se cargan los usuarios válidos, se omiten los dañados o repetidos y los IDs no se repiten. */
    @Test
    @DisplayName("Archivo corrupto: se cargan los usuarios válidos, se omiten los dañados o repetidos y los IDs no se repiten")
    void archivoCorruptoNoTumbaLaCarga() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        registrar("Ana Mora", "ana", "ana12345");
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        String lineaAna = lineas.get(lineas.size() - 1);
        Files.write(archivo, List.of(lineaAna, "basura sin columnas", lineaAna.replace("U001", "U007"),
                lineaAna.replace("U001", "U005").replace("|ana|", "|pepe|")), StandardCharsets.UTF_8);

        gestor = nuevoGestorCargado();

        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("U001", "U005"), gestor.listarTodos().stream().map(Usuario::getId).toList());
        assertEquals(2, auditoria.contar("LINEA_CORRUPTA_OMITIDA"), "la línea basura y el usuario 'ana' repetido");
        assertEquals("U006", registrar("Luis Rojas", "luis", "luis12345").getId());
    }

    /** Prueba: Si la carga falla, no se permite guardar (para no borrar el archivo real). */
    @Test
    @DisplayName("Si la carga falla, no se permite guardar (para no borrar el archivo real)")
    void cargaFallidaImpideGuardar() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Files.delete(archivo);
        Files.createDirectory(archivo); // ahora "usuarios.txt" es una carpeta: no se puede leer
        GestorUsuarios conError = new GestorUsuarios(
                new RepositorioUsuariosArchivoTexto(archivo, auditoria), new CatalogoUsuarios(),
                new HasheadorTextoPlano(),
                new ControlIntentosLogin(MAXIMO_INTENTOS, BLOQUEO, reloj), auditoria, reloj);

        // Verificar: se revisa que el resultado sea el esperado.
        assertThrows(ExcepcionPersistencia.class, conError::cargarDatos);
        assertThrows(ExcepcionPersistencia.class, conError::guardarDatos);
        assertThrows(ExcepcionPersistencia.class,
                () -> conError.registrar("Ana", "ana", "ana12345".toCharArray(), "ana12345".toCharArray()));
        assertEquals(1, auditoria.contar("CARGA_FALLIDA"));
    }

    /** Prueba: Datos semilla: crea U001 Ana, U002 Luis y U003 Maria solo si el catálogo está vacío. */
    @Test
    @DisplayName("Datos semilla: crea U001 Ana, U002 Luis y U003 Maria solo si el catálogo está vacío")
    void datosSemilla() {
        // Verificar: se revisa que cada caso dé el resultado esperado.
        assertEquals(3, HerramientaDatosSemilla.sembrarSiEstaVacio(gestor, gestor, auditoria));
        assertEquals(List.of("U001", "U002", "U003"), gestor.listarTodos().stream().map(Usuario::getId).toList());
        assertEquals("ana", gestor.buscarPorId("U001").orElseThrow().getNombreUsuario());
        assertEquals("U003", gestor.autenticar("maria", "maria12345".toCharArray()).getId());
        assertEquals(0, HerramientaDatosSemilla.sembrarSiEstaVacio(gestor, gestor, auditoria));
    }
}
