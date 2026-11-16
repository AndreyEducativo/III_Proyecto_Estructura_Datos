/*
 * TareaEnSegundoPlano.java
 *
 * Qué hace: ejecuta un trabajo pesado (PBKDF2, leer o escribir archivos) en un hilo de fondo con
 * SwingWorker y, al terminar, vuelve al hilo de Swing para actualizar la pantalla con el
 * resultado o con el error.
 * Rol en la arquitectura: utilidad de la UI usada por VentanaLogin, VentanaRegistro y
 * ControladorNavegacion. Centraliza el manejo de SwingWorker para no repetirlo en cada ventana.
 * Principio SOLID: SRP (solo maneja el ir y venir entre hilos) y evita código duplicado.
 * Colabora con: VentanaLogin, VentanaRegistro, ControladorNavegacion.
 */
package una.social.ui;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Ejecución de trabajos fuera del hilo de Swing.
 *
 * <p><b>¿Por qué hace falta? (explicado simple)</b> Swing dibuja la ventana y atiende los clics
 * en UN solo hilo (el EDT). Si en ese hilo se hace algo lento —calcular PBKDF2 con 310.000
 * iteraciones o escribir un archivo—, la ventana se "congela": no se repinta ni responde hasta
 * que termine. {@link SwingWorker} resuelve esto: {@code doInBackground} corre en otro hilo, y
 * {@code done} vuelve a correr en el EDT, que es el ÚNICO hilo desde el que se pueden tocar los
 * componentes de forma segura.</p>
 */
final class TareaEnSegundoPlano {

    /** Clase de utilidades: no se instancia. */
    private TareaEnSegundoPlano() {
    }

    /**
     * Ejecuta {@code trabajo} en segundo plano.
     *
     * @param trabajo    lo que se hace en el hilo de fondo (NO debe tocar componentes Swing)
     * @param alTerminar qué hacer en el EDT con el resultado si todo salió bien
     * @param alFallar   qué hacer en el EDT con la excepción si el trabajo falló
     * @param <T>        tipo del resultado
     */
    static <T> void ejecutar(Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        // Invariante: se llama desde un botón o evento, es decir, desde el EDT.
        assert SwingUtilities.isEventDispatchThread() : "ejecutar debe llamarse desde el hilo de Swing";
        new SwingWorker<T, Void>() {
            /** Corre en un hilo de fondo: aquí está permitido lo lento. */
            @Override
            protected T doInBackground() {
                return trabajo.get();
            }

            /** Corre en el EDT cuando doInBackground terminó (bien o mal). */
            @Override
            protected void done() {
                T resultado;
                // get() devuelve el resultado o lanza ExecutionException si doInBackground lanzó
                // una excepción (la excepción real viene en getCause()). Se separa el resultado
                // del callback alTerminar para que un error DENTRO de alTerminar no se confunda
                // con un error del trabajo: ese error subiría al manejador global.
                try {
                    resultado = get();
                } catch (ExecutionException ex) {
                    alFallar.accept(ex.getCause() == null ? ex : ex.getCause());
                    return;
                } catch (InterruptedException ex) {
                    // No debería pasar (done() se llama cuando ya terminó), pero si el hilo fue
                    // interrumpido se restaura la marca y se informa como fallo.
                    Thread.currentThread().interrupt();
                    alFallar.accept(ex);
                    return;
                }
                alTerminar.accept(resultado);
            }
        }.execute();
    }
}
