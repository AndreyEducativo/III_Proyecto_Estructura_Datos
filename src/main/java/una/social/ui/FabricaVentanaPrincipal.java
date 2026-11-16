/*
 * FabricaVentanaPrincipal.java
 *
 * Qué hace: contrato para crear la ventana principal que se abre después de iniciar sesión.
 * Rol en la arquitectura: es el PUNTO DE INTEGRACIÓN con Sebastián. Hoy Main registra
 * VentanaPrincipalTemporal::new; Sebastián solo cambia esa línea por su propia fábrica
 * (por ejemplo, la que crea su VentanaPrincipal con muro, buscador y amigos) sin tocar el login,
 * el registro ni el controlador de navegación.
 * Principio SOLID: OCP (se cambia la ventana principal sin modificar el flujo existente), DIP
 * (el controlador depende de esta abstracción) e ISP (un solo método).
 * Colabora con: ControladorNavegacion, VentanaPrincipalTemporal, Main.
 */
package una.social.ui;

import javax.swing.JFrame;
import una.social.modelo.Usuario;

/**
 * Fábrica de la ventana principal.
 *
 * <p>Es una interfaz funcional (un solo método), así que se puede implementar con una
 * referencia a constructor: {@code FabricaVentanaPrincipal f = MiVentana::new;} siempre que
 * {@code MiVentana} tenga un constructor {@code (Usuario, Runnable)}.</p>
 */
@FunctionalInterface
public interface FabricaVentanaPrincipal {

    /**
     * Crea (sin mostrarla) la ventana principal del usuario que acaba de iniciar sesión.
     *
     * <p>Se llama siempre desde el hilo de Swing (EDT). El controlador se encarga de mostrarla,
     * centrarla y de que cerrarla con la X cierre la aplicación guardando los datos.</p>
     *
     * @param usuarioActivo  usuario autenticado (inmutable)
     * @param alCerrarSesion acción que la ventana debe ejecutar cuando el usuario pulse
     *                       "Cerrar sesión": guarda los datos, cierra la ventana y vuelve al login
     * @return la ventana principal, todavía no visible
     */
    JFrame crear(Usuario usuarioActivo, Runnable alCerrarSesion);
}
