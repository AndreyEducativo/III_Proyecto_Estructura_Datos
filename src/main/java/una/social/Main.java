/*
 * Main.java
 *
 * Qué hace: punto de entrada de Social UNA. Arranca la aplicación en este orden:
 *   1. Verifica la integridad de la auditoría (antes de escribir nada nuevo).
 *   2. Crea la auditoría e instala el manejador global de errores.
 *   3. Arma los servicios de usuarios (Andrey) y del módulo social (Sebastián).
 *   4. Carga los datos desde data/ (reconstruye la memoria): primero usuarios, luego amistades y
 *      publicaciones. Si están vacíos, crea los datos de prueba.
 *   5. Registra el gancho de cierre (guardar + auditar al salir).
 *   6. Muestra el login en el hilo de Swing; al autenticar, abre la VentanaPrincipal (muro,
 *      buscador y amigos) mediante FabricaVentanaPrincipal.
 * Rol en la arquitectura: "raíz de composición". Es el lugar donde se crean las
 * implementaciones concretas y se inyectan por constructor; ninguna otra clase decide qué
 * implementación usar.
 * Principio SOLID: DIP (todo se conecta aquí mediante abstracciones) y OCP (el módulo social se
 * integró aquí, sin modificar el login, el controlador ni los servicios de usuarios).
 * Colabora con: EnsambladorServicios, EnsambladorSocial, VerificadorIntegridadAuditoria,
 * ManejadorErroresGlobal, CoordinadorPersistencia, HerramientaDatosSemilla,
 * HerramientaDatosSemillaSocial, ContextoSesion, ControladorNavegacion, FabricaVentanaPrincipal,
 * VentanaPrincipal.
 */
package una.social;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import una.social.amistades.BuscadorUsuarios;
import una.social.amistades.GestorAmistades;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.auditoria.ResultadoVerificacion;
import una.social.auditoria.VerificadorIntegridadAuditoria;
import una.social.herramientas.HerramientaDatosSemilla;
import una.social.herramientas.HerramientaDatosSemillaSocial;
import una.social.persistencia.CoordinadorPersistencia;
import una.social.persistencia.ModuloPersistente;
import una.social.publicaciones.GestorPublicaciones;
import una.social.servicio.GestorUsuarios;
import una.social.sesion.ContextoSesion;
import una.social.ui.ControladorNavegacion;
import una.social.ui.Dialogos;
import una.social.ui.FabricaVentanaPrincipal;
import una.social.ui.ManejadorErroresGlobal;
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

        // 3. Servicios. Usuarios (Andrey) y módulo social (Sebastián): amistades, buscador y
        //    publicaciones. Los módulos sociales reciben el directorio de usuarios (solo lectura).
        GestorUsuarios gestorUsuarios = EnsambladorServicios.crearGestorUsuarios(CARPETA_DATOS, auditoria, reloj);
        GestorAmistades gestorAmistades =
                EnsambladorSocial.crearGestorAmistades(CARPETA_DATOS, gestorUsuarios, auditoria, reloj);
        GestorPublicaciones gestorPublicaciones = EnsambladorSocial.crearGestorPublicaciones(
                CARPETA_DATOS, gestorUsuarios, gestorAmistades, auditoria, reloj);
        BuscadorUsuarios buscador = EnsambladorSocial.crearBuscador(gestorUsuarios, gestorAmistades);

        // 4. Carga en DOS fases. Primero los usuarios (y sus datos de prueba si está vacío),
        //    porque amistades y publicaciones validan que los IDs que leen existan.
        List<String> modulosSinCargar = new ArrayList<>(
                new CoordinadorPersistencia(List.of(gestorUsuarios), auditoria).cargarTodos());
        if (modulosSinCargar.isEmpty()) {
            // Solo si la carga salió bien: si falló, el catálogo vacío NO significa "no hay
            // usuarios", y sembrar podría pisar el archivo real.
            sembrarDatosDePrueba(gestorUsuarios, auditoria);
            modulosSinCargar.addAll(new CoordinadorPersistencia(
                    List.of(gestorAmistades, gestorPublicaciones), auditoria).cargarTodos());
        } else {
            // Sin usuarios no se pueden validar amistades ni publicaciones: NO se cargan, y como
            // quedan "sin cargar", tampoco se guardarán (así sus archivos no se pierden).
            modulosSinCargar.add(gestorAmistades.nombreModulo());
            modulosSinCargar.add(gestorPublicaciones.nombreModulo());
        }
        if (modulosSinCargar.isEmpty()) {
            sembrarDatosSociales(gestorUsuarios, gestorAmistades, gestorPublicaciones, auditoria);
        }
        // Este coordinador guarda TODOS los módulos al cerrar sesión y al cerrar la aplicación.
        List<ModuloPersistente> modulos = List.of(gestorUsuarios, gestorAmistades, gestorPublicaciones);
        CoordinadorPersistencia persistencia = new CoordinadorPersistencia(modulos, auditoria);

        // 5. Guardar y auditar al cerrar la aplicación (por la X, por el sistema operativo o Ctrl+C).
        ContextoSesion sesion = new ContextoSesion();
        registrarGanchoDeCierre(persistencia, sesion, auditoria);

        // 6. Interfaz gráfica. La ventana principal real (Sebastián) se registra en la fábrica:
        //    el login y el controlador no saben qué ventana se abre (OCP).
        FabricaVentanaPrincipal fabricaVentanaPrincipal = (usuario, alCerrarSesion) -> new VentanaPrincipal(
                usuario, alCerrarSesion, gestorPublicaciones, gestorAmistades, buscador, auditoria);
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
     * Crea amistades y publicaciones de ejemplo si todavía no hay ninguna.
     *
     * @param gestorUsuarios      usuarios (ya cargados)
     * @param gestorAmistades     amistades (ya cargadas)
     * @param gestorPublicaciones publicaciones (ya cargadas)
     * @param auditoria           bitácora
     */
    private static void sembrarDatosSociales(GestorUsuarios gestorUsuarios, GestorAmistades gestorAmistades,
                                             GestorPublicaciones gestorPublicaciones, RegistroAuditoria auditoria) {
        // Se espera ExcepcionSocialUna (por ejemplo, no se pudo escribir el archivo). La app sigue
        // abriendo sin los datos de ejemplo y el error queda auditado.
        try {
            HerramientaDatosSemillaSocial.sembrarSiEstaVacio(gestorUsuarios, gestorAmistades, gestorPublicaciones, auditoria);
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sesion.getUsuarioActivo().ifPresent(usuario -> auditoria.registrar(NivelAuditoria.INFO,
                    usuario.getNombreUsuario(), "LOGOUT", "Sesión cerrada al salir de la aplicación"));
            List<String> fallidos = persistencia.guardarTodos();
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
        if (!verificacion.integra()) {
            Dialogos.advertencia(null, "Auditoría alterada",
                    verificacion.descripcion() + "\n\nLa aplicación seguirá funcionando normalmente.");
        }
        if (!modulosSinCargar.isEmpty()) {
            Dialogos.error(null, "Datos no disponibles", "No se pudieron leer los datos de: "
                    + String.join(", ", modulosSinCargar) + ".\nPara proteger la información no se guardarán "
                    + "cambios en esos módulos. Cierre la aplicación, revise la carpeta data/ y vuelva a abrirla.");
        }
    }
}
