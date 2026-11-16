/*
 * DialogoCambioContrasena.java
 *
 * Qué hace: diálogo para cambiar la contraseña: pide la actual, la nueva y la confirmación, con
 * errores en rojo junto al campo que falló.
 * Rol en la arquitectura: capa de PRESENTACIÓN del cambio de contraseña. Las reglas (verificar la
 * actual, largo mínimo, confirmación) están en ServicioCuenta.
 * Principio SOLID: SRP (solo UI) y DIP (usa ServicioCuenta).
 * Colabora con: ServicioCuenta, ConstructorFormulario, TareaEnSegundoPlano, CampoContrasenaTema.
 */
package una.social.ui;

import java.awt.Component;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoContrasenaTema;
import una.social.ui.tema.TemaUi;

/**
 * Diálogo de cambio de contraseña.
 *
 * <p>Seguridad: las contraseñas se leen con {@code getPassword()} (char[]) y se borran con
 * {@code Arrays.fill} apenas termina el cambio, igual que en el login y el registro.</p>
 */
final class DialogoCambioContrasena extends JDialog {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario activo. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Contraseña actual. */
    private final CampoContrasenaTema campoActual = TemaUi.crearCampoContrasena("Tu contraseña actual");

    /** Contraseña nueva. */
    private final CampoContrasenaTema campoNueva = TemaUi.crearCampoContrasena("Mínimo 6 caracteres");

    /** Confirmación de la nueva. */
    private final CampoContrasenaTema campoConfirmacion = TemaUi.crearCampoContrasena("Repite la contraseña nueva");

    /** Casilla para ver lo escrito. */
    private final JCheckBox casillaMostrar = TemaUi.crearCasilla("Mostrar contraseñas");

    /** Botón para guardar. */
    private final BotonTema botonGuardar = TemaUi.crearBotonPrimario("Cambiar contraseña");

    /** Campo → etiqueta de error. */
    private final transient Map<CampoFormulario, JLabel> errores = new EnumMap<>(CampoFormulario.class);

    /** Campo → componente (para marcarlo en rojo). */
    private final transient Map<CampoFormulario, CampoContrasenaTema> campos = new EnumMap<>(CampoFormulario.class);

    /**
     * Crea el diálogo (sin mostrarlo).
     *
     * @param padre     componente dueño
     * @param usuario   usuario activo
     * @param servicios servicios
     */
    DialogoCambioContrasena(Component padre, Usuario usuario, ServiciosSociales servicios) {
        super(SwingUtilities.getWindowAncestor(padre), "Cambiar contraseña", ModalityType.APPLICATION_MODAL);
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        ConstructorFormulario formulario = new ConstructorFormulario();
        registrar(CampoFormulario.CONTRASENA_ACTUAL, campoActual, formulario.campo("Contraseña actual", campoActual));
        registrar(CampoFormulario.CONTRASENA, campoNueva, formulario.campo("Contraseña nueva", campoNueva));
        registrar(CampoFormulario.CONFIRMACION, campoConfirmacion, formulario.campo("Confirmar contraseña nueva", campoConfirmacion));
        formulario.componente(casillaMostrar, 0).botones(botonGuardar);
        setContentPane(TemaUi.crearFondoCentrado(formulario.construir()));
        casillaMostrar.addActionListener(e -> campos.values().forEach(c -> c.setContrasenaVisible(casillaMostrar.isSelected())));
        botonGuardar.addActionListener(e -> guardar());
        getRootPane().setDefaultButton(botonGuardar);
        pack();
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(padre));
    }

    /**
     * Relaciona un campo con su componente y su etiqueta de error.
     *
     * @param campo      identificador
     * @param componente campo de contraseña
     * @param error      etiqueta de error
     */
    private void registrar(CampoFormulario campo, CampoContrasenaTema componente, JLabel error) {
        campos.put(campo, componente);
        errores.put(campo, error);
    }

    /**
     * Cambia la contraseña en segundo plano.
     */
    private void guardar() {
        errores.values().forEach(etiqueta -> TemaUi.mostrarError(etiqueta, null));
        char[] actual = campoActual.getPassword();
        char[] nueva = campoNueva.getPassword();
        char[] confirmacion = campoConfirmacion.getPassword();
        botonGuardar.setEnabled(false);
        TareaEnSegundoPlano.ejecutar(() -> {
            // finally: las tres copias de las contraseñas se borran pase lo que pase.
            try {
                servicios.cuenta().cambiarContrasena(usuario, actual, nueva, confirmacion);
                return Boolean.TRUE;
            } finally {
                Arrays.fill(actual, '\0');
                Arrays.fill(nueva, '\0');
                Arrays.fill(confirmacion, '\0');
            }
        }, hecho -> {
            Dialogos.exito(this, "Contraseña cambiada", "Tu contraseña se cambió correctamente.\n"
                    + "Úsala la próxima vez que inicies sesión.");
            dispose();
        }, falla -> {
            botonGuardar.setEnabled(true);
            if (falla instanceof ExcepcionValidacion && errores.containsKey(((ExcepcionValidacion) falla).getCampo())) {
                // Error del usuario: en rojo junto al campo que falló.
                CampoFormulario campo = ((ExcepcionValidacion) falla).getCampo();
                TemaUi.mostrarError(errores.get(campo), falla.getMessage());
                TemaUi.marcarError(campos.get(campo), true);
                campos.get(campo).requestFocusInWindow();
            } else {
                AccionesUi.mostrarFallo(this, servicios.auditoria(), "CAMBIAR_CONTRASENA", "No se pudo cambiar la contraseña", falla);
            }
        });
    }
}
