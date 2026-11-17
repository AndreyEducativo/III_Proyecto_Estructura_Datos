/*
 * ControladorNavegacion.java
 *
 * Qué hace: decide qué ventana se muestra en cada momento: login → registro → login →
 * ventana principal → (cerrar sesión) → login.
 * Rol en la arquitectura: es el "director" de la UI. Las ventanas no se conocen entre sí; solo
 * avisan al controlador mediante callbacks y él hace el cambio. También maneja la sesión
 * (ContextoSesion) y el guardado de datos al cerrar sesión.
 * Principio SOLID: SRP (la navegación no está mezclada dentro de las ventanas), OCP (la ventana
 * principal llega por FabricaVentanaPrincipal, así que se cambia sin tocar esta clase) y DIP
 * (todo llega por constructor como abstracción).
 * Colabora con: VentanaLogin, VentanaRegistro, FabricaVentanaPrincipal, ContextoSesion,
 * CoordinadorPersistencia, RegistroAuditoria, Dialogos.
 */
package una.social.ui;

import java.awt.Cursor;
import java.util.List;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.persistencia.CoordinadorPersistencia;
import una.social.servicio.ServicioAutenticacion;
import una.social.servicio.ServicioRegistro;
import una.social.sesion.ContextoSesion;

/**
 * Controlador de navegación entre ventanas.
 *
 * <p>Todos sus métodos trabajan en el hilo de Swing (EDT). Las ventanas se crean una sola vez y
 * se reutilizan (se ocultan y se vuelven a mostrar), lo que hace la navegación instantánea.</p>
 */
public final class ControladorNavegacion {

    /** Servicio de inicio de sesión. */
    private final ServicioAutenticacion servicioAutenticacion;

    /** Servicio de registro. */
    private final ServicioRegistro servicioRegistro;

    /** Sesión activa. */
    private final ContextoSesion sesion;

    /** Fábrica de la ventana principal (punto de integración con Sebastián). */
    private final FabricaVentanaPrincipal fabricaVentanaPrincipal;

    /** Guarda todos los módulos al cerrar sesión. */
    private final CoordinadorPersistencia persistencia;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /** Ventana de login (se crea la primera vez que se necesita). */
    private VentanaLogin ventanaLogin;

    /** Ventana de registro (se crea la primera vez que se necesita). */
    private VentanaRegistro ventanaRegistro;

    /** Ventana principal abierta; {@code null} si no hay sesión. */
    private JFrame ventanaPrincipal;

    /**
     * Crea el controlador.
     *
     * @param servicioAutenticacion   servicio de login
     * @param servicioRegistro        servicio de registro
     * @param sesion                  contexto de sesión
     * @param fabricaVentanaPrincipal fábrica de la ventana principal
     * @param persistencia            coordinador de guardado
     * @param auditoria               bitácora
     */
    public ControladorNavegacion(ServicioAutenticacion servicioAutenticacion, ServicioRegistro servicioRegistro,
                                 ContextoSesion sesion, FabricaVentanaPrincipal fabricaVentanaPrincipal,
                                 CoordinadorPersistencia persistencia, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.servicioAutenticacion = Objects.requireNonNull(servicioAutenticacion, "servicioAutenticacion");
        this.servicioRegistro = Objects.requireNonNull(servicioRegistro, "servicioRegistro");
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.fabricaVentanaPrincipal = Objects.requireNonNull(fabricaVentanaPrincipal, "fabricaVentanaPrincipal");
        this.persistencia = Objects.requireNonNull(persistencia, "persistencia");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Muestra la primera pantalla (login). Debe llamarse desde el hilo de Swing.
     */
    public void iniciar() {
        assert SwingUtilities.isEventDispatchThread() : "La navegación ocurre en el hilo de Swing";
        mostrarLogin(null);
    }

    /**
     * @return la ventana de login, creándola si aún no existe
     */
    private VentanaLogin login() {
        if (ventanaLogin == null) {
            ventanaLogin = new VentanaLogin(servicioAutenticacion, auditoria, this::abrirVentanaPrincipal, this::irARegistro);
        }
        return ventanaLogin;
    }

    /**
     * @return la ventana de registro, creándola si aún no existe
     */
    private VentanaRegistro registro() {
        // Se crea una sola vez y se reutiliza. Al registrarse vuelve al login con el usuario
        // escrito; al pulsar "Volver" vuelve al login vacío.
        if (ventanaRegistro == null) {
            ventanaRegistro = new VentanaRegistro(servicioRegistro, auditoria,
                    creado -> volverAlLogin(creado.getNombreUsuario()), () -> volverAlLogin(null));
        }
        return ventanaRegistro;
    }

    /**
     * Muestra el login.
     *
     * @param usuarioSugerido nombre a precargar, o {@code null}
     */
    private void mostrarLogin(String usuarioSugerido) {
        VentanaLogin login = login();
        login.prepararParaMostrar(usuarioSugerido);
        login.setVisible(true);
        login.toFront();
    }

    /**
     * Pasa del login al registro, en la misma posición de pantalla.
     */
    private void irARegistro() {
        // Limpia el formulario de registro, lo pone donde estaba el login y lo muestra...
        VentanaRegistro registro = registro();
        registro.prepararParaMostrar();
        registro.setLocationRelativeTo(login());
        registro.setVisible(true);
        // ...y oculta el login (no se destruye, se reutiliza al volver).
        login().setVisible(false);
    }

    /**
     * Pasa del registro al login.
     *
     * @param usuarioSugerido usuario recién creado para precargarlo, o {@code null}
     */
    private void volverAlLogin(String usuarioSugerido) {
        registro().setVisible(false);
        mostrarLogin(usuarioSugerido);
    }

    /**
     * Inicia la sesión y abre la ventana principal creada por la fábrica.
     *
     * @param usuario usuario autenticado
     */
    private void abrirVentanaPrincipal(Usuario usuario) {
        sesion.iniciarSesion(usuario);
        JFrame ventana;
        // Se espera cualquier RuntimeException al construir la ventana principal (es código de
        // otro módulo). Se deshace la sesión, se audita y se muestra un aviso; el usuario sigue
        // en el login y la app no se cae.
        try {
            ventana = fabricaVentanaPrincipal.crear(usuario, this::cerrarSesion);
        } catch (RuntimeException ex) {
            sesion.cerrarSesion();
            Dialogos.errorInesperado(login(), auditoria, "ABRIR_VENTANA_PRINCIPAL", ex);
            login().prepararParaMostrar(null);
            return;
        }
        // Cerrar la ventana principal con la X cierra la aplicación; el gancho de cierre de Main
        // guarda los datos y audita el cierre.
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLocationRelativeTo(null);
        ventanaPrincipal = ventana;
        ventana.setVisible(true);
        login().setVisible(false);
    }

    /**
     * Cierra la sesión: guarda TODOS los datos en segundo plano, audita el cierre, cierra la
     * ventana principal y vuelve al login. Se puede llamar desde cualquier hilo.
     */
    public void cerrarSesion() {
        if (!SwingUtilities.isEventDispatchThread()) {
            // Si el módulo de Sebastián la llamara desde otro hilo, se "reenvía" al EDT.
            SwingUtilities.invokeLater(this::cerrarSesion);
            return;
        }
        if (ventanaPrincipal == null) {
            return; // Ya se está cerrando (doble clic): no se hace nada.
        }
        JFrame cerrando = ventanaPrincipal;
        ventanaPrincipal = null;
        cerrando.setEnabled(false);
        cerrando.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String actor = sesion.getUsuarioActivo().map(Usuario::getNombreUsuario).orElse(RegistroAuditoria.ACTOR_SISTEMA);
        // Guardar escribe archivos: se hace fuera del EDT para no congelar la ventana.
        TareaEnSegundoPlano.ejecutar(
                persistencia::guardarTodos,
                fallidos -> terminarCierreDeSesion(cerrando, actor, fallidos),
                error -> {
                    Dialogos.errorInesperado(cerrando, auditoria, "GUARDAR_AL_CERRAR_SESION", error);
                    terminarCierreDeSesion(cerrando, actor, List.of("desconocido"));
                });
    }

    /**
     * Segunda mitad del cierre de sesión (en el EDT, cuando ya terminó el guardado).
     *
     * @param cerrando ventana principal que se cierra
     * @param actor    usuario que cierra sesión
     * @param fallidos módulos que no se pudieron guardar
     */
    private void terminarCierreDeSesion(JFrame cerrando, String actor, List<String> fallidos) {
        // Anota el cierre de sesión (WARN si algún módulo no se guardó).
        String detalle = fallidos.isEmpty() ? "Sesión cerrada; datos guardados"
                : "Sesión cerrada; no se pudieron guardar: " + String.join(", ", fallidos);
        auditoria.registrar(fallidos.isEmpty() ? NivelAuditoria.INFO : NivelAuditoria.WARN, actor, "LOGOUT", detalle);
        // Olvida al usuario activo, destruye la ventana principal y vuelve al login.
        sesion.cerrarSesion();
        cerrando.dispose();
        mostrarLogin(null);
        // Si algo no se pudo guardar, se avisa al usuario.
        if (!fallidos.isEmpty()) {
            Dialogos.advertencia(login(), "Datos sin guardar", "Se cerró la sesión, pero no se pudieron guardar los datos de: "
                    + String.join(", ", fallidos) + ".\nRevise que la carpeta data/ no esté bloqueada o en solo lectura.");
        }
    }
}
