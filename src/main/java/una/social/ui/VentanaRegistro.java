/*
 * VentanaRegistro.java
 *
 * Qué hace: pantalla para crear una cuenta nueva (nombre, usuario, contraseña y confirmación).
 * Al registrarse, muestra el ID asignado (U001, U002, ...) y vuelve al login.
 * Rol en la arquitectura: capa de PRESENTACIÓN. Las reglas de validación están en el servicio;
 * esta ventana solo muestra los errores junto al campo que corresponde.
 * Principio SOLID: SRP (solo UI), ISP y DIP (depende de ServicioRegistro; todo por constructor).
 * Colabora con: ServicioRegistro, RegistroAuditoria, TemaUi, ConstructorFormulario,
 * TareaEnSegundoPlano, Dialogos, ControladorNavegacion.
 */
package una.social.ui;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionSocialUna;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;
import una.social.modelo.Usuario;
import una.social.servicio.ServicioRegistro;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoContrasenaTema;
import una.social.ui.tema.CampoTextoTema;
import una.social.ui.tema.LogosUna;
import una.social.ui.tema.TemaUi;

/**
 * Ventana de registro de usuarios.
 */
public final class VentanaRegistro extends JFrame {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Texto normal del botón principal. */
    private static final String TEXTO_CREAR = "Crear cuenta";

    /** Texto del botón principal mientras se guarda. */
    private static final String TEXTO_CREANDO = "Creando cuenta…";

    /** Servicio de registro. */
    private final transient ServicioRegistro servicio;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Qué hacer con el usuario recién creado (volver al login con su usuario escrito). */
    private final transient Consumer<Usuario> alRegistrar;

    /** Qué hacer si la persona decide volver sin registrarse. */
    private final transient Runnable alVolver;

    /** Campo del nombre completo. */
    private final CampoTextoTema campoNombre = TemaUi.crearCampoTexto("ej. Ana Mora");

    /** Campo del nombre de usuario. */
    private final CampoTextoTema campoUsuario = TemaUi.crearCampoTexto("ej. ana (sin espacios)");

    /** Campo de la contraseña. */
    private final CampoContrasenaTema campoContrasena = TemaUi.crearCampoContrasena("Mínimo 6 caracteres");

    /** Campo de la confirmación. */
    private final CampoContrasenaTema campoConfirmacion = TemaUi.crearCampoContrasena("Repita la contraseña");

    /** Casilla para ver las contraseñas escritas. */
    private final JCheckBox casillaMostrar = TemaUi.crearCasilla("Mostrar contraseñas");

    /** Botón principal. */
    private final BotonTema botonCrear = TemaUi.crearBotonPrimario(TEXTO_CREAR);

    /** Botón para volver al login. */
    private final BotonTema botonVolver = TemaUi.crearBotonSecundario("Ya tengo cuenta: iniciar sesión");

    /** Campos del formulario, por identificador. */
    private final transient Map<CampoFormulario, JTextComponent> campos = new EnumMap<>(CampoFormulario.class);

    /** Etiquetas de error de cada campo. */
    private final transient Map<CampoFormulario, JLabel> errores = new EnumMap<>(CampoFormulario.class);

    /**
     * Crea la ventana (sin mostrarla). Debe llamarse desde el hilo de Swing.
     *
     * @param servicio    servicio de registro
     * @param auditoria   bitácora
     * @param alRegistrar acción a ejecutar con el usuario creado
     * @param alVolver    acción para volver al login sin registrarse
     */
    public VentanaRegistro(ServicioRegistro servicio, RegistroAuditoria auditoria,
                           Consumer<Usuario> alRegistrar, Runnable alVolver) {
        super("Social UNA - Crear cuenta");
        // Logo de la UNA en la barra de título y en la barra de tareas.
        setIconImages(LogosUna.iconosVentana());
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.alRegistrar = Objects.requireNonNull(alRegistrar, "alRegistrar");
        this.alVolver = Objects.requireNonNull(alVolver, "alVolver");
        // 1. Arma el formulario, 2. conecta los botones y 3. ajusta tamaño y posición.
        construirInterfaz();
        configurarEventos();
        configurarVentana();
    }

    /**
     * Deja el formulario vacío y listo para usarse.
     */
    public void prepararParaMostrar() {
        assert SwingUtilities.isEventDispatchThread() : "La UI solo se modifica desde el hilo de Swing";
        // Vacía todos los campos y oculta las contraseñas.
        campos.values().forEach(campo -> campo.setText(""));
        casillaMostrar.setSelected(false);
        mostrarContrasenas(false);
        // Quita errores anteriores y reactiva los controles.
        limpiarErrores();
        ponerOcupado(false);
        // El cursor empieza en "Nombre completo" (se pide después porque la ventana aún no es visible).
        SwingUtilities.invokeLater(campoNombre::requestFocusInWindow);
    }

    /**
     * Arma el formulario.
     */
    private void construirInterfaz() {
        // Encabezado con el logo y la explicación de que el ID se asigna solo.
        ConstructorFormulario formulario = new ConstructorFormulario()
                .encabezado("Crear cuenta", "Únete a Social UNA. Tu ID (U001, U002…) se asigna automáticamente.");
        // Los cuatro campos, cada uno con su etiqueta de error debajo.
        registrarCampo(CampoFormulario.NOMBRE, campoNombre, formulario.campo("Nombre completo", campoNombre));
        registrarCampo(CampoFormulario.USUARIO, campoUsuario, formulario.campo("Usuario", campoUsuario));
        registrarCampo(CampoFormulario.CONTRASENA, campoContrasena, formulario.campo("Contraseña", campoContrasena));
        registrarCampo(CampoFormulario.CONFIRMACION, campoConfirmacion,
                formulario.campo("Confirmar contraseña", campoConfirmacion));
        // Casilla "Mostrar contraseñas" y botones Crear cuenta / Volver.
        formulario.componente(casillaMostrar, 0).botones(botonCrear, botonVolver);
        setContentPane(TemaUi.crearFondoCentrado(formulario.construir()));
    }

    /**
     * Guarda la relación campo ↔ componente ↔ etiqueta de error.
     *
     * @param campo      identificador del campo
     * @param componente componente de texto
     * @param error      etiqueta de error
     */
    private void registrarCampo(CampoFormulario campo, JTextComponent componente, JLabel error) {
        campos.put(campo, componente);
        errores.put(campo, error);
    }

    /**
     * Conecta los eventos.
     */
    private void configurarEventos() {
        botonCrear.addActionListener(e -> intentarRegistrar());
        botonVolver.addActionListener(e -> alVolver.run());
        casillaMostrar.addActionListener(e -> mostrarContrasenas(casillaMostrar.isSelected()));
        getRootPane().setDefaultButton(botonCrear);
    }

    /**
     * Configura tamaño, cierre y posición. Cerrar esta ventana con la X NO cierra la app: vuelve
     * al login, que es lo que el usuario espera de una ventana secundaria.
     */
    private void configurarVentana() {
        // La X no cierra la ventana directamente: lo decide el listener de abajo.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /** Clic en la X: volver al login, salvo que se esté guardando (botones deshabilitados). */
            @Override
            public void windowClosing(WindowEvent e) {
                if (botonVolver.isEnabled()) {
                    alVolver.run();
                }
            }
        });
        // Tamaño justo para el contenido, sin poder achicarse más, y centrada en la pantalla.
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /**
     * Lee el formulario y registra en segundo plano (PBKDF2 + escritura del archivo).
     */
    private void intentarRegistrar() {
        // Quita los errores anteriores y lee lo que el usuario escribió.
        limpiarErrores();
        String nombre = campoNombre.getText();
        String usuario = campoUsuario.getText();
        char[] contrasena = campoContrasena.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();
        // Bloquea el formulario y registra en segundo plano (la ventana no se congela).
        ponerOcupado(true);
        TareaEnSegundoPlano.ejecutar(
                () -> registrarYBorrar(nombre, usuario, contrasena, confirmacion),
                this::mostrarExito,
                this::mostrarFallo);
    }

    /**
     * Registra y borra las contraseñas de la memoria pase lo que pase. Corre en segundo plano.
     *
     * @param nombre       nombre completo
     * @param usuario      nombre de usuario
     * @param contrasena   contraseña
     * @param confirmacion confirmación
     * @return usuario creado
     */
    private Usuario registrarYBorrar(String nombre, String usuario, char[] contrasena, char[] confirmacion) {
        // finally: las copias de las contraseñas se borran aunque el registro falle.
        try {
            return servicio.registrar(nombre, usuario, contrasena, confirmacion);
        } finally {
            Arrays.fill(contrasena, '\0');
            Arrays.fill(confirmacion, '\0');
        }
    }

    /**
     * Informa el éxito mostrando el ID asignado y vuelve al login. Corre en el hilo de Swing.
     *
     * @param creado usuario creado
     */
    private void mostrarExito(Usuario creado) {
        ponerOcupado(false);
        // Mensaje con el ID asignado (U004, U005...) y luego vuelve al login con el usuario escrito.
        Dialogos.exito(this, "¡Cuenta creada!", "Bienvenido(a), " + creado.getNombre() + ".\n\n"
                + "Tu ID de usuario es: " + creado.getId() + "\n"
                + "Ya puedes iniciar sesión con el usuario «" + creado.getNombreUsuario() + "».");
        alRegistrar.accept(creado);
    }

    /**
     * Muestra el error del registro. Corre en el hilo de Swing.
     *
     * @param error excepción lanzada por el servicio
     */
    private void mostrarFallo(Throwable error) {
        ponerOcupado(false);
        if (error instanceof ExcepcionValidacion) {
            // Incluye ExcepcionUsuarioDuplicado, que es una subclase y apunta al campo USUARIO.
            ExcepcionValidacion validacion = (ExcepcionValidacion) error;
            mostrarErrorEnCampo(validacion.getCampo(), validacion.getMessage());
        } else if (error instanceof ExcepcionSocialUna) {
            Dialogos.error(this, "No se pudo crear la cuenta", error.getMessage());
        } else {
            Dialogos.errorInesperado(this, auditoria, "REGISTRO", error);
        }
    }

    /**
     * Pinta un error en rojo junto al campo y le pasa el foco.
     *
     * @param campo   campo con error
     * @param mensaje mensaje
     */
    private void mostrarErrorEnCampo(CampoFormulario campo, String mensaje) {
        JTextComponent componente = campos.get(campo);
        JLabel etiqueta = errores.get(campo);
        // Invariante: el registro tiene los cuatro campos del enum, así que siempre existen.
        assert componente != null && etiqueta != null : "Campo sin componente en el registro: " + campo;
        TemaUi.mostrarError(etiqueta, mensaje);
        TemaUi.marcarError(componente, true);
        componente.requestFocusInWindow();
    }

    /**
     * Borra todos los mensajes de error.
     */
    private void limpiarErrores() {
        errores.values().forEach(etiqueta -> TemaUi.mostrarError(etiqueta, null));
        campos.values().forEach(componente -> TemaUi.marcarError(componente, false));
    }

    /**
     * Muestra u oculta ambas contraseñas.
     *
     * @param visibles {@code true} para ver el texto
     */
    private void mostrarContrasenas(boolean visibles) {
        campoContrasena.setContrasenaVisible(visibles);
        campoConfirmacion.setContrasenaVisible(visibles);
    }

    /**
     * Activa o desactiva el estado "ocupado" mientras se guarda.
     *
     * @param ocupado {@code true} mientras se registra
     */
    private void ponerOcupado(boolean ocupado) {
        // Activa o desactiva todos los campos y botones.
        campos.values().forEach(campo -> campo.setEnabled(!ocupado));
        casillaMostrar.setEnabled(!ocupado);
        botonCrear.setEnabled(!ocupado);
        botonVolver.setEnabled(!ocupado);
        // El botón dice "Creando…" y el cursor es de espera mientras tanto.
        botonCrear.setText(ocupado ? TEXTO_CREANDO : TEXTO_CREAR);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
