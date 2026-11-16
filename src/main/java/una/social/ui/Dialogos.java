/*
 * Dialogos.java
 *
 * Qué hace: muestra las alertas gráficas de la aplicación (éxito, advertencia, error y error
 * inesperado) con textos y títulos consistentes.
 * Rol en la arquitectura: utilidad compartida de UI. La usan las ventanas, el controlador y el
 * manejador global de errores (y puede usarla Sebastián).
 * Principio SOLID: SRP (todas las alertas se arman en un solo lugar) y evita duplicar código de
 * JOptionPane en cada ventana.
 * Colabora con: VentanaLogin, VentanaRegistro, ControladorNavegacion, ManejadorErroresGlobal,
 * RegistroAuditoria.
 */
package una.social.ui;

import java.awt.Component;
import javax.swing.JOptionPane;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;

/**
 * Alertas gráficas amigables.
 */
public final class Dialogos {

    /** Mensaje para errores que el programa no esperaba (no revela detalles técnicos). */
    public static final String MENSAJE_INESPERADO = "Ocurrió un error inesperado. La aplicación sigue funcionando "
            + "y el detalle técnico quedó registrado en la auditoría.";

    /** Clase de utilidades: no se instancia. */
    private Dialogos() {
    }

    /**
     * Muestra un mensaje de éxito.
     *
     * @param padre   ventana sobre la que se centra el diálogo (puede ser nula)
     * @param titulo  título del diálogo
     * @param mensaje mensaje para el usuario
     */
    public static void exito(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Muestra una advertencia.
     *
     * @param padre   ventana sobre la que se centra el diálogo (puede ser nula)
     * @param titulo  título del diálogo
     * @param mensaje mensaje para el usuario
     */
    public static void advertencia(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Muestra un error conocido (con un mensaje ya pensado para el usuario).
     *
     * @param padre   ventana sobre la que se centra el diálogo (puede ser nula)
     * @param titulo  título del diálogo
     * @param mensaje mensaje para el usuario
     */
    public static void error(Component padre, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Audita un error que el programa no esperaba y muestra un mensaje genérico. El detalle
     * técnico (tipo y mensaje de la excepción) va a la auditoría, nunca a la pantalla.
     *
     * @param padre     ventana sobre la que se centra el diálogo (puede ser nula)
     * @param auditoria bitácora
     * @param accion    acción que se estaba haciendo (por ejemplo "LOGIN")
     * @param error     excepción ocurrida
     */
    public static void errorInesperado(Component padre, RegistroAuditoria auditoria, String accion, Throwable error) {
        auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "ERROR_INESPERADO",
                accion + ": " + RegistroAuditoria.describirError(error));
        error(padre, "Error inesperado", MENSAJE_INESPERADO);
    }
}
