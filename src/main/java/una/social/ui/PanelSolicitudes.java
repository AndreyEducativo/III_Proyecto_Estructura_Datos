/*
 * PanelSolicitudes.java
 *
 * Qué hace: pestaña "Solicitudes". Muestra las solicitudes de amistad que recibió el usuario
 * activo, con los botones "Aceptar" y "Rechazar" en cada una.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de amistades.
 * Principio SOLID: SRP (solo UI de las solicitudes) y DIP (depende de ServicioAmistades, inyectado).
 * Colabora con: ServicioAmistades, AccionesAmistad, FilaUsuario, ListaTarjetas, VentanaPrincipal.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.amistades.ServicioAmistades;
import una.social.amistades.SolicitudRecibida;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña con las solicitudes de amistad recibidas.
 */
final class PanelSolicitudes extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario que recibió las solicitudes. */
    private final transient Usuario usuario;

    /** Servicio de amistades. */
    private final transient ServicioAmistades amistades;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Avisa a la ventana cuántas solicitudes hay, para mostrarlo en el título de la pestaña. */
    private final transient IntConsumer alCambiarCantidad;

    /** Título con la cantidad de solicitudes. */
    private final JLabel titulo = TemaUi.crearEtiqueta("Solicitudes de amistad");

    /** Lista de solicitudes. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario           usuario que recibió las solicitudes
     * @param amistades         servicio de amistades
     * @param auditoria         bitácora
     * @param alCambiarCantidad recibe la cantidad de solicitudes cada vez que se refresca
     */
    PanelSolicitudes(Usuario usuario, ServicioAmistades amistades, RegistroAuditoria auditoria,
                     IntConsumer alCambiarCantidad) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.alCambiarCantidad = Objects.requireNonNull(alCambiarCantidad, "alCambiarCantidad");
        setOpaque(false);
        titulo.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 3, 0, 0));
        add(titulo, BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
    }

    /** {@inheritDoc} */
    @Override
    public void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> amistades.listarSolicitudesRecibidas(usuario.getId()), this::mostrar,
                falla -> Dialogos.errorInesperado(this, auditoria, "LISTAR_SOLICITUDES", falla));
    }

    /**
     * Dibuja las solicitudes.
     *
     * @param solicitudes solicitudes recibidas, de la más antigua a la más reciente
     */
    private void mostrar(List<SolicitudRecibida> solicitudes) {
        int cantidad = solicitudes.size();
        titulo.setText(cantidad == 1 ? "Tienes 1 solicitud de amistad"
                : "Tienes " + cantidad + " solicitudes de amistad");
        List<JComponent> filas = new ArrayList<>(cantidad);
        for (SolicitudRecibida solicitud : solicitudes) {
            Usuario remitente = solicitud.remitente();
            filas.add(FilaUsuario.crear(remitente, FilaUsuario.detalleBasico(remitente)
                    + "  ·  Te la envió el " + TextosUi.fechaHora(solicitud.fecha()), crearBotones(remitente)));
        }
        lista.mostrar(filas, "No tienes solicitudes de amistad pendientes.");
        alCambiarCantidad.accept(cantidad);
    }

    /**
     * @param remitente quien envió la solicitud
     * @return panel con los botones "Aceptar" y "Rechazar"
     */
    private JPanel crearBotones(Usuario remitente) {
        BotonTema aceptar = TemaUi.crearBotonPrimario("Aceptar");
        BotonTema rechazar = TemaUi.crearBotonSecundario("Rechazar");
        aceptar.addActionListener(e -> {
            aceptar.setEnabled(false);
            rechazar.setEnabled(false);
            AccionesAmistad.aceptar(this, amistades, usuario, remitente, auditoria, this::refrescar);
        });
        rechazar.addActionListener(e -> {
            if (!Dialogos.confirmar(this, "Rechazar solicitud",
                    "¿Quieres rechazar la solicitud de amistad de " + remitente.getNombre() + "?", "Rechazar")) {
                return;
            }
            aceptar.setEnabled(false);
            rechazar.setEnabled(false);
            AccionesAmistad.rechazar(this, amistades, usuario, remitente, auditoria, this::refrescar);
        });
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(aceptar);
        botones.add(rechazar);
        return botones;
    }
}
