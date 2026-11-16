/*
 * VentanaPrincipalTemporal.java
 *
 * Qué hace: ventana principal PROVISIONAL que se abre tras iniciar sesión. Muestra
 * "Bienvenido, <nombre>", los datos básicos del usuario y un botón para cerrar sesión.
 * Rol en la arquitectura: implementación por defecto de FabricaVentanaPrincipal (Main la
 * registra con VentanaPrincipalTemporal::new). Sebastián la REEMPLAZA por su ventana real
 * (muro, buscador y amigos) cambiando una sola línea en Main; no hace falta borrar esta clase.
 * Principio SOLID: SRP (solo da la bienvenida) y LSP (cualquier ventana con el mismo
 * constructor puede sustituirla a través de la fábrica).
 * Colabora con: Usuario, TemaUi, ControladorNavegacion.
 */
package una.social.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.JFrame;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Ventana de bienvenida provisional.
 */
public final class VentanaPrincipalTemporal extends JFrame {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Formato de fecha para "miembro desde". */
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Crea la ventana (sin mostrarla). Su firma coincide con
     * {@link FabricaVentanaPrincipal#crear(Usuario, Runnable)}, por eso se puede registrar como
     * {@code VentanaPrincipalTemporal::new}.
     *
     * @param usuarioActivo  usuario que inició sesión
     * @param alCerrarSesion acción del botón "Cerrar sesión"
     */
    public VentanaPrincipalTemporal(Usuario usuarioActivo, Runnable alCerrarSesion) {
        super("Social UNA - " + usuarioActivo.getNombre());
        String miembroDesde = FORMATO_FECHA.format(usuarioActivo.getFechaRegistro().atZone(ZoneId.systemDefault()));
        BotonTema botonCerrarSesion = TemaUi.crearBotonPrimario("Cerrar sesión");
        botonCerrarSesion.addActionListener(e -> {
            // Se deshabilita para evitar un doble clic mientras se guardan los datos.
            botonCerrarSesion.setEnabled(false);
            alCerrarSesion.run();
        });
        ConstructorFormulario contenido = new ConstructorFormulario()
                .encabezado("Bienvenido, " + usuarioActivo.getNombre(),
                        "@" + usuarioActivo.getNombreUsuario() + "  ·  ID " + usuarioActivo.getId()
                                + "  ·  miembro desde " + miembroDesde)
                .componente(TemaUi.crearSubtitulo("Aquí se mostrarán el muro, el buscador de estudiantes "
                        + "y la lista de amigos (módulo en construcción)."), 0)
                .botones(botonCerrarSesion);
        setContentPane(TemaUi.crearFondoCentrado(contenido.construir()));
        getRootPane().setDefaultButton(botonCerrarSesion);
        pack();
        setMinimumSize(getSize());
    }
}
