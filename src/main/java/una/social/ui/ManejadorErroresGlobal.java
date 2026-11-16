/*
 * ManejadorErroresGlobal.java
 *
 * Qué hace: atrapa cualquier excepción que nadie haya capturado (en cualquier hilo, incluido el
 * hilo de Swing), la registra en la auditoría y muestra un diálogo amigable. La aplicación NO se
 * cierra.
 * Rol en la arquitectura: es la última red de seguridad. Main lo instala al arrancar, antes de
 * crear cualquier ventana.
 * Principio SOLID: SRP (solo maneja errores no capturados) y DIP (recibe la auditoría por
 * constructor).
 * Colabora con: RegistroAuditoria, Dialogos, Main.
 */
package una.social.ui;

import java.awt.GraphicsEnvironment;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;

/**
 * Manejador de excepciones no capturadas.
 *
 * <p><b>¿Cómo funciona con Swing? (explicado simple)</b> Swing ejecuta todo lo de la interfaz
 * (clics, pintado, eventos) en un único hilo llamado EDT ("Event Dispatch Thread"). Si un
 * botón lanza una excepción que nadie captura, esa excepción "sube" hasta el EDT. Java le pasa
 * entonces la excepción al manejador del hilo; si el hilo no tiene uno propio, al manejador
 * POR DEFECTO de todos los hilos. Después Swing crea un EDT nuevo y la ventana sigue funcionando.
 * Por eso se instala este manejador de dos formas:</p>
 * <ol>
 *   <li>Como manejador por defecto ({@code Thread.setDefaultUncaughtExceptionHandler}): cubre
 *       el hilo principal, los hilos de fondo y cualquier EDT nuevo que Swing cree.</li>
 *   <li>Directamente en el EDT actual (desde {@code invokeLater}): el "manejo equivalente para
 *       el hilo de Swing".</li>
 * </ol>
 */
public final class ManejadorErroresGlobal implements Thread.UncaughtExceptionHandler {

    /** Bitácora donde se registra el error. */
    private final RegistroAuditoria auditoria;

    /**
     * Evita mostrar un diálogo encima de otro si ocurren muchos errores seguidos (por ejemplo,
     * un error dentro del pintado se repetiría en cada repintado y llenaría la pantalla).
     */
    private final AtomicBoolean mostrandoDialogo = new AtomicBoolean(false);

    /**
     * Crea el manejador.
     *
     * @param auditoria bitácora (no nula)
     */
    public ManejadorErroresGlobal(RegistroAuditoria auditoria) {
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Instala el manejador como predeterminado de todos los hilos y en el hilo de Swing.
     */
    public void instalar() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        SwingUtilities.invokeLater(() -> Thread.currentThread().setUncaughtExceptionHandler(this));
    }

    /**
     * Recibe la excepción no capturada: la audita (tipo y mensaje, sin datos sensibles), imprime
     * la pila en la consola para el programador y muestra un diálogo amigable.
     *
     * @param hilo  hilo donde ocurrió
     * @param error excepción no capturada
     */
    @Override
    public void uncaughtException(Thread hilo, Throwable error) {
        auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "EXCEPCION_NO_CONTROLADA",
                "Hilo '" + hilo.getName() + "': " + RegistroAuditoria.describirError(error));
        // La pila completa solo va a la consola (útil al programar); no a la auditoría ni a la UI.
        error.printStackTrace();
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        // Mostrar un diálogo es trabajo de la UI: debe hacerse en el EDT. Si el error ocurrió en
        // otro hilo, se "encarga" el diálogo al EDT con invokeLater.
        if (SwingUtilities.isEventDispatchThread()) {
            mostrarDialogo();
        } else {
            SwingUtilities.invokeLater(this::mostrarDialogo);
        }
    }

    /**
     * Muestra el diálogo, salvo que ya haya uno abierto.
     */
    private void mostrarDialogo() {
        if (!mostrandoDialogo.compareAndSet(false, true)) {
            return;
        }
        // finally garantiza que la bandera se libere aunque el propio diálogo falle; si no, nunca
        // más se mostraría otro aviso.
        try {
            Dialogos.error(null, "Error inesperado", Dialogos.MENSAJE_INESPERADO);
        } finally {
            mostrandoDialogo.set(false);
        }
    }
}
