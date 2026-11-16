/*
 * VentanaLogin.java
 *
 * Qué hace: pantalla de inicio de sesión de Social UNA.
 * Rol en la arquitectura: capa de PRESENTACIÓN. Solo muestra datos y reacciona a eventos; las
 * reglas (validación, contraseñas, bloqueo) están en el servicio. Avisa al controlador cuando
 * alguien inicia sesión o quiere registrarse (mediante callbacks), sin saber qué ventana viene
 * después.
 * Principio SOLID: SRP (solo UI), ISP y DIP (depende de la interfaz ServicioAutenticacion, no del
 * GestorUsuarios; recibe todo por constructor).
 * Colabora con: ServicioAutenticacion, RegistroAuditoria, TemaUi, ConstructorFormulario,
 * TareaEnSegundoPlano, Dialogos, ControladorNavegacion.
 */
package una.social.ui;

import java.awt.Cursor;
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
import una.social.excepciones.ExcepcionAutenticacion;
import una.social.excepciones.ExcepcionCuentaBloqueada;
import una.social.excepciones.ExcepcionSocialUna;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;
import una.social.modelo.Usuario;
import una.social.servicio.ServicioAutenticacion;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoContrasenaTema;
import una.social.ui.tema.CampoTextoTema;
import una.social.ui.tema.TemaUi;

/**
 * Ventana de inicio de sesión.
 *
 * <p>Detalles de usabilidad: formulario centrado, texto de ayuda en los campos, errores en rojo
 * junto al campo, ENTER envía el formulario, TAB recorre los campos en orden lógico y el botón
 * muestra "Verificando…" mientras se comprueba la contraseña (sin congelar la ventana).</p>
 */
public final class VentanaLogin extends JFrame {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Texto normal del botón principal. */
    private static final String TEXTO_ENTRAR = "Iniciar sesión";

    /** Texto del botón principal mientras se verifica. */
    private static final String TEXTO_VERIFICANDO = "Verificando…";

    /** Servicio que verifica las credenciales. */
    private final transient ServicioAutenticacion servicio;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Qué hacer cuando el login es exitoso (lo define el controlador). */
    private final transient Consumer<Usuario> alAutenticar;

    /** Campo del nombre de usuario. */
    private final CampoTextoTema campoUsuario = TemaUi.crearCampoTexto("ej. ana");

    /** Campo de la contraseña (muestra puntos). */
    private final CampoContrasenaTema campoContrasena = TemaUi.crearCampoContrasena("Tu contraseña");

    /** Casilla para ver la contraseña escrita. */
    private final JCheckBox casillaMostrar = TemaUi.crearCasilla("Mostrar contraseña");

    /** Botón principal. */
    private final BotonTema botonEntrar = TemaUi.crearBotonPrimario(TEXTO_ENTRAR);

    /** Botón para ir al registro. */
    private final BotonTema botonCrearCuenta = TemaUi.crearBotonSecundario("Crear una cuenta nueva");

    /** Mensaje de error general (credenciales incorrectas, bloqueo). */
    private final JLabel errorGeneral = TemaUi.crearEtiquetaError();

    /** Relación campo del formulario → componente, para marcar en rojo el campo que falló. */
    private final transient Map<CampoFormulario, JTextComponent> campos = new EnumMap<>(CampoFormulario.class);

    /** Etiquetas de error de cada campo. */
    private final transient Map<CampoFormulario, JLabel> errores = new EnumMap<>(CampoFormulario.class);

    /**
     * Crea la ventana (sin mostrarla). Debe llamarse desde el hilo de Swing.
     *
     * @param servicio      servicio de autenticación
     * @param auditoria     bitácora
     * @param alAutenticar  acción a ejecutar con el usuario cuando el login es exitoso
     * @param alIrARegistro acción para ir a la ventana de registro
     */
    public VentanaLogin(ServicioAutenticacion servicio, RegistroAuditoria auditoria,
                        Consumer<Usuario> alAutenticar, Runnable alIrARegistro) {
        super("Social UNA - Iniciar sesión");
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.alAutenticar = Objects.requireNonNull(alAutenticar, "alAutenticar");
        construirInterfaz();
        configurarEventos(Objects.requireNonNull(alIrARegistro, "alIrARegistro"));
        configurarVentana();
    }

    /**
     * Deja la ventana lista para mostrarse: borra la contraseña y los errores, habilita los
     * controles y pone el foco donde corresponde.
     *
     * @param usuarioSugerido nombre de usuario a precargar (por ejemplo, recién registrado);
     *                        nulo para dejar el que estaba
     */
    public void prepararParaMostrar(String usuarioSugerido) {
        assert SwingUtilities.isEventDispatchThread() : "La UI solo se modifica desde el hilo de Swing";
        if (usuarioSugerido != null) {
            campoUsuario.setText(usuarioSugerido);
        }
        campoContrasena.setText("");
        casillaMostrar.setSelected(false);
        campoContrasena.setContrasenaVisible(false);
        limpiarErrores();
        ponerOcupado(false);
        JTextComponent conFoco = campoUsuario.getText().isBlank() ? campoUsuario : campoContrasena;
        // El foco se pide "más tarde" porque la ventana todavía no es visible en este punto.
        SwingUtilities.invokeLater(conFoco::requestFocusInWindow);
    }

    /**
     * Arma el formulario.
     */
    private void construirInterfaz() {
        ConstructorFormulario formulario = new ConstructorFormulario()
                .encabezado("Social UNA", "La red social estudiantil de la Universidad Nacional");
        registrarCampo(CampoFormulario.USUARIO, campoUsuario, formulario.campo("Usuario", campoUsuario));
        registrarCampo(CampoFormulario.CONTRASENA, campoContrasena, formulario.campo("Contraseña", campoContrasena));
        formulario.componente(casillaMostrar, 0)
                .componente(errorGeneral, TemaUi.ESPACIO)
                .botones(botonEntrar, botonCrearCuenta);
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
     * Conecta botones, casilla y tecla ENTER con sus acciones.
     *
     * @param alIrARegistro acción del botón "Crear una cuenta nueva"
     */
    private void configurarEventos(Runnable alIrARegistro) {
        botonEntrar.addActionListener(e -> intentarIniciarSesion());
        botonCrearCuenta.addActionListener(e -> alIrARegistro.run());
        casillaMostrar.addActionListener(e -> campoContrasena.setContrasenaVisible(casillaMostrar.isSelected()));
        // "Botón por defecto": ENTER en cualquier campo equivale a hacer clic en "Iniciar sesión".
        getRootPane().setDefaultButton(botonEntrar);
    }

    /**
     * Configura tamaño, cierre y posición de la ventana.
     */
    private void configurarVentana() {
        // Cerrar el login con la X cierra la aplicación (el gancho de cierre guarda y audita).
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        // El tamaño mínimo es el que necesita el formulario: así nunca se corta al achicar.
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /**
     * Lee el formulario y verifica las credenciales en segundo plano.
     */
    private void intentarIniciarSesion() {
        limpiarErrores();
        String usuario = campoUsuario.getText();
        // getPassword() devuelve una COPIA nueva en char[]; esta copia se borra con Arrays.fill
        // apenas termina la verificación (en el finally del trabajo en segundo plano).
        char[] contrasena = campoContrasena.getPassword();
        ponerOcupado(true);
        TareaEnSegundoPlano.ejecutar(
                () -> autenticarYBorrar(usuario, contrasena),
                alAutenticar,
                this::mostrarFallo);
    }

    /**
     * Autentica y borra la contraseña de la memoria pase lo que pase. Corre en segundo plano.
     *
     * @param usuario    nombre escrito
     * @param contrasena contraseña escrita
     * @return usuario autenticado
     */
    private Usuario autenticarYBorrar(String usuario, char[] contrasena) {
        // finally: se ejecuta tanto si el login sale bien como si lanza excepción, así la
        // contraseña nunca queda en memoria más tiempo del necesario.
        try {
            return servicio.autenticar(usuario, contrasena);
        } finally {
            Arrays.fill(contrasena, '\0');
        }
    }

    /**
     * Muestra el error del login en el lugar adecuado. Corre en el hilo de Swing.
     *
     * @param error excepción lanzada por el servicio
     */
    private void mostrarFallo(Throwable error) {
        ponerOcupado(false);
        if (error instanceof ExcepcionValidacion) {
            ExcepcionValidacion validacion = (ExcepcionValidacion) error;
            mostrarErrorEnCampo(validacion.getCampo(), validacion.getMessage());
        } else if (error instanceof ExcepcionCuentaBloqueada) {
            TemaUi.mostrarError(errorGeneral, "Cuenta bloqueada temporalmente.");
            Dialogos.advertencia(this, "Acceso bloqueado", error.getMessage());
        } else if (error instanceof ExcepcionAutenticacion) {
            campoContrasena.setText("");
            TemaUi.mostrarError(errorGeneral, error.getMessage());
            Dialogos.error(this, "No se pudo iniciar sesión", error.getMessage());
            campoContrasena.requestFocusInWindow();
        } else if (error instanceof ExcepcionSocialUna) {
            // Error conocido (por ejemplo, de persistencia): su mensaje ya es amigable y el
            // servicio ya lo auditó.
            Dialogos.error(this, "No se pudo iniciar sesión", error.getMessage());
        } else {
            Dialogos.errorInesperado(this, auditoria, "LOGIN", error);
        }
    }

    /**
     * Pinta un error en rojo junto al campo indicado y le pasa el foco.
     *
     * @param campo   campo con error
     * @param mensaje mensaje
     */
    private void mostrarErrorEnCampo(CampoFormulario campo, String mensaje) {
        JTextComponent componente = campos.get(campo);
        JLabel etiqueta = errores.get(campo);
        if (componente == null || etiqueta == null) {
            // El login solo tiene usuario y contraseña; si llegara otro campo, se muestra arriba.
            TemaUi.mostrarError(errorGeneral, mensaje);
            return;
        }
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
        TemaUi.mostrarError(errorGeneral, null);
    }

    /**
     * Activa o desactiva el estado "ocupado": mientras se verifica, los controles se
     * deshabilitan para evitar dobles clics y el cursor muestra que se está trabajando.
     *
     * @param ocupado {@code true} mientras se verifica
     */
    private void ponerOcupado(boolean ocupado) {
        campoUsuario.setEnabled(!ocupado);
        campoContrasena.setEnabled(!ocupado);
        casillaMostrar.setEnabled(!ocupado);
        botonEntrar.setEnabled(!ocupado);
        botonCrearCuenta.setEnabled(!ocupado);
        botonEntrar.setText(ocupado ? TEXTO_VERIFICANDO : TEXTO_ENTRAR);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }
}
