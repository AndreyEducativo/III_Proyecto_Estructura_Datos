/*
 * Main.java
 *
 * Qué hace: punto de entrada de Social UNA. Arranca la aplicación en este orden:
 *   1. Verifica la integridad de la auditoría (antes de escribir nada nuevo).
 *   2. Crea la auditoría e instala el manejador global de errores.
 *   3. Arma los servicios de usuarios (Andrey) y del módulo social (Sebastián): bloqueos,
 *      amistades y solicitudes, sugerencias, buscador, publicaciones, me gusta, comentarios,
 *      perfiles y anuncios (EnsambladorSocial los devuelve juntos en ModulosSociales).
 *   4. Carga los datos desde data/ (reconstruye la memoria) en tres fases: usuarios; luego
 *      bloqueos, amistades, publicaciones, perfiles y anuncios; al final me gusta y comentarios.
 *      Si todo está vacío, crea los datos de prueba.
 *   5. Registra el gancho de cierre (guardar + auditar al salir).
 *   6. Muestra el login en el hilo de Swing; al autenticar, abre la VentanaPrincipal (muro,
 *      anuncios, buscador, amigos, solicitudes y mi perfil) mediante FabricaVentanaPrincipal.
 * Rol en la arquitectura: "raíz de composición". Es el lugar donde se crean las
 * implementaciones concretas y se inyectan por constructor; ninguna otra clase decide qué
 * implementación usar.
 * Principio SOLID: DIP (todo se conecta aquí mediante abstracciones) y OCP (el módulo social se
 * integró aquí, sin modificar el login ni el controlador; del módulo de usuarios solo se agregó la
 * interfaz ServicioCuenta).
 * Colabora con: EnsambladorServicios, EnsambladorSocial, ModulosSociales,
 * VerificadorIntegridadAuditoria, ManejadorErroresGlobal, CoordinadorPersistencia,
 * HerramientaDatosSemilla, HerramientaDatosSemillaSocial, ContextoSesion, ControladorNavegacion,
 * FabricaVentanaPrincipal, ServiciosSociales, VentanaPrincipal.
 */
package una.social;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.auditoria.ResultadoVerificacion;
import una.social.auditoria.VerificadorIntegridadAuditoria;
import una.social.herramientas.HerramientaDatosSemilla;
import una.social.herramientas.HerramientaDatosSemillaSocial;
import una.social.persistencia.CoordinadorPersistencia;
import una.social.persistencia.ModuloPersistente;
import una.social.servicio.GestorUsuarios;
import una.social.sesion.ContextoSesion;
import una.social.ui.ControladorNavegacion;
import una.social.ui.Dialogos;
import una.social.ui.FabricaVentanaPrincipal;
import una.social.ui.ManejadorErroresGlobal;
import una.social.ui.ServiciosSociales;
import una.social.ui.VentanaPrincipal;

/**
 * Clase principal de Social UNA.
 */
public final class Main {

    /** Carpeta de datos, relativa a la carpeta desde donde se ejecuta la app (run.bat / run.sh). */
    private static final Path CARPETA_DATOS = Path.of("data");

    /** Clase de arranque: no se instancia. */
    private Main() {
    }

    /**
     * Arranca la aplicación.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        Clock reloj = Clock.systemDefaultZone();

        // 1. Se verifica ANTES de crear el registrador para revisar el archivo tal como quedó en
        //    la ejecución anterior (incluida cualquier edición manual hecha mientras la app estaba cerrada).
        ResultadoVerificacion verificacion =
                new VerificadorIntegridadAuditoria(EnsambladorServicios.rutaAuditoria(CARPETA_DATOS)).verificar();

        // 2. Auditoría y red de seguridad para errores no capturados.
        RegistroAuditoria auditoria = EnsambladorServicios.crearAuditoria(CARPETA_DATOS, reloj);
        new ManejadorErroresGlobal(auditoria).instalar();
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "APP_INICIO",
                "Social UNA iniciada con Java " + Runtime.version());
        auditoria.registrar(verificacion.integra() ? NivelAuditoria.INFO : NivelAuditoria.WARN,
                RegistroAuditoria.ACTOR_SISTEMA, "VERIFICACION_AUDITORIA", verificacion.descripcion());

        // 3. Servicios. Usuarios (Andrey) y módulo social completo (bloqueos, amistades, sugerencias,
        //    buscador, publicaciones, me gusta, comentarios, perfiles y anuncios). Los módulos
        //    sociales reciben el directorio de usuarios (solo lectura).
        GestorUsuarios gestorUsuarios = EnsambladorServicios.crearGestorUsuarios(CARPETA_DATOS, auditoria, reloj);
        ModulosSociales social = EnsambladorSocial.crear(CARPETA_DATOS, gestorUsuarios, auditoria, reloj);

        // 4. Carga por fases, porque cada fase valida referencias a la anterior.
        List<String> modulosSinCargar = cargarPorFases(gestorUsuarios, social, auditoria);
        if (modulosSinCargar.isEmpty()) {
            sembrarDatosSociales(gestorUsuarios, social, auditoria);
        }
        // Este coordinador guarda TODOS los módulos al cerrar sesión y al cerrar la aplicación.
        List<ModuloPersistente> modulos = new ArrayList<>();
        modulos.add(gestorUsuarios);
        modulos.addAll(social.todos());
        CoordinadorPersistencia persistencia = new CoordinadorPersistencia(modulos, auditoria);

        // 5. Guardar y auditar al cerrar la aplicación (por la X, por el sistema operativo o Ctrl+C).
        ContextoSesion sesion = new ContextoSesion();
        registrarGanchoDeCierre(persistencia, sesion, auditoria);

        // 6. Interfaz gráfica. La ventana principal real se registra en la fábrica: el login y el
        //    controlador no saben qué ventana se abre (OCP). Recibe los servicios como interfaces.
        ServiciosSociales servicios = new ServiciosSociales(social.publicaciones(), social.amistades(),
                social.buscador(), social.sugeridor(), social.bloqueos(), social.meGusta(), social.comentarios(),
                social.perfiles(), social.anuncios(), gestorUsuarios, sesion, auditoria);
        FabricaVentanaPrincipal fabricaVentanaPrincipal =
                (usuario, alCerrarSesion) -> new VentanaPrincipal(usuario, alCerrarSesion, servicios);
        ControladorNavegacion controlador = new ControladorNavegacion(gestorUsuarios, gestorUsuarios, sesion,
                fabricaVentanaPrincipal, persistencia, auditoria);

        // Todo lo de Swing se crea en el hilo de Swing (EDT); invokeLater "encarga" ese trabajo.
        SwingUtilities.invokeLater(() -> {
            aplicarAspectoDelSistema(auditoria);
            controlador.iniciar();
            mostrarAvisosIniciales(verificacion, modulosSinCargar);
        });
    }

    /**
     * Crea los usuarios de prueba si el catálogo está vacío.
     *
     * @param gestor    gestor de usuarios ya cargado
     * @param auditoria bitácora
     */
    private static void sembrarDatosDePrueba(GestorUsuarios gestor, RegistroAuditoria auditoria) {
        // Se espera ExcepcionPersistencia (u otro error) si no se pudo escribir usuarios.txt. La
        // app sigue abriendo (sin usuarios de prueba) y el error queda auditado.
        try {
            HerramientaDatosSemilla.sembrarSiEstaVacio(gestor, gestor, auditoria);
        } catch (RuntimeException ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_SEMILLA_FALLIDOS",
                    RegistroAuditoria.describirError(ex));
        }
    }

    /**
     * Carga los datos en tres fases:
     * <ol>
     *   <li>Usuarios (y los 3 de prueba si el catálogo está vacío).</li>
     *   <li>Bloqueos, amistades, publicaciones, perfiles y anuncios (validan que los usuarios existan).</li>
     *   <li>Me gusta y comentarios (validan que las publicaciones existan).</li>
     * </ol>
     * <p>Si una fase falla, las que dependen de ella NO se cargan y quedan marcadas "sin cargar":
     * así tampoco se guardarán y sus archivos no se pierden.</p>
     *
     * @param gestorUsuarios usuarios
     * @param social         módulos sociales
     * @param auditoria      bitácora
     * @return nombres de los módulos que no se pudieron cargar (vacía si todo salió bien)
     */
    private static List<String> cargarPorFases(GestorUsuarios gestorUsuarios, ModulosSociales social,
                                               RegistroAuditoria auditoria) {
        List<String> sinCargar = new ArrayList<>(new CoordinadorPersistencia(List.of(gestorUsuarios), auditoria).cargarTodos());
        if (!sinCargar.isEmpty()) {
            social.todos().forEach(modulo -> sinCargar.add(modulo.nombreModulo()));
            return sinCargar;
        }
        // Solo si la carga de usuarios salió bien: si falló, el catálogo vacío NO significa "no
        // hay usuarios", y sembrar podría pisar el archivo real.
        sembrarDatosDePrueba(gestorUsuarios, auditoria);
        sinCargar.addAll(new CoordinadorPersistencia(social.fasePrincipal(), auditoria).cargarTodos());
        if (sinCargar.contains(social.publicaciones().nombreModulo())) {
            social.faseInteracciones().forEach(modulo -> sinCargar.add(modulo.nombreModulo()));
        } else {
            sinCargar.addAll(new CoordinadorPersistencia(social.faseInteracciones(), auditoria).cargarTodos());
        }
        return sinCargar;
    }

    /**
     * Crea los datos sociales de ejemplo si todavía no hay ninguno.
     *
     * @param gestorUsuarios usuarios (ya cargados)
     * @param social         módulos sociales (ya cargados)
     * @param auditoria      bitácora
     */
    private static void sembrarDatosSociales(GestorUsuarios gestorUsuarios, ModulosSociales social,
                                             RegistroAuditoria auditoria) {
        // Se espera ExcepcionSocialUna (por ejemplo, no se pudo escribir un archivo). La app sigue
        // abriendo sin los datos de ejemplo y el error queda auditado.
        try {
            HerramientaDatosSemillaSocial.sembrarSiEstaVacio(gestorUsuarios, social, auditoria);
        } catch (RuntimeException ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_SEMILLA_FALLIDOS",
                    RegistroAuditoria.describirError(ex));
        }
    }

    /**
     * Registra un "gancho de cierre": código que Java ejecuta justo antes de terminar el
     * programa. Garantiza que se guarde y se audite el cierre sin importar cómo se cierre la app.
     *
     * @param persistencia coordinador de guardado
     * @param sesion       sesión activa
     * @param auditoria    bitácora
     */
    private static void registrarGanchoDeCierre(CoordinadorPersistencia persistencia, ContextoSesion sesion,
                                                RegistroAuditoria auditoria) {
        // Este hilo se ejecuta solo cuando el programa se está cerrando (X de la ventana, Ctrl+C...).
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 1. Si había alguien conectado, se anota su cierre de sesión.
            sesion.getUsuarioActivo().ifPresent(usuario -> auditoria.registrar(NivelAuditoria.INFO,
                    usuario.getNombreUsuario(), "LOGOUT", "Sesión cerrada al salir de la aplicación"));
            // 2. Se guardan TODOS los módulos en sus archivos .txt.
            List<String> fallidos = persistencia.guardarTodos();
            // 3. Se anota el cierre, indicando si algún módulo no se pudo guardar.
            auditoria.registrar(fallidos.isEmpty() ? NivelAuditoria.INFO : NivelAuditoria.WARN,
                    RegistroAuditoria.ACTOR_SISTEMA, "APP_CIERRE", fallidos.isEmpty()
                            ? "Aplicación cerrada; datos guardados"
                            : "Aplicación cerrada; no se pudieron guardar: " + String.join(", ", fallidos));
        }, "cierre-social-una"));
    }

    /**
     * Usa el aspecto visual del sistema operativo para diálogos y casillas (los botones y campos
     * del tema se dibujan con su propio estilo).
     *
     * @param auditoria bitácora
     */
    private static void aplicarAspectoDelSistema(RegistroAuditoria auditoria) {
        // Se esperan las excepciones de UIManager si el aspecto del sistema no está disponible.
        // No es grave: se sigue con el aspecto por defecto de Java y se deja constancia.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException ex) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "ASPECTO_NO_DISPONIBLE",
                    RegistroAuditoria.describirError(ex));
        }
    }

    /**
     * Muestra advertencias de arranque SIN bloquear la app: el usuario las lee y sigue.
     *
     * @param verificacion     resultado de la verificación de la auditoría
     * @param modulosSinCargar módulos cuyos datos no se pudieron cargar
     */
    private static void mostrarAvisosIniciales(ResultadoVerificacion verificacion, List<String> modulosSinCargar) {
        // Aviso 1: alguien editó a mano el archivo de auditoría.
        if (!verificacion.integra()) {
            Dialogos.advertencia(null, "Auditoría alterada",
                    verificacion.descripcion() + "\n\nLa aplicación seguirá funcionando normalmente.");
        }
        // Aviso 2: algún archivo de datos no se pudo leer (esos módulos no guardarán cambios).
        if (!modulosSinCargar.isEmpty()) {
            Dialogos.error(null, "Datos no disponibles", "No se pudieron leer los datos de: "
                    + String.join(", ", modulosSinCargar) + ".\nPara proteger la información no se guardarán "
                    + "cambios en esos módulos. Cierre la aplicación, revise la carpeta data/ y vuelva a abrirla.");
        }
    }
}
