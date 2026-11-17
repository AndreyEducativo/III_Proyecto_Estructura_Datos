/*
 * PanelSolicitudes.java
 *
 * Qué hace: pestaña "Solicitudes". Muestra las solicitudes de amistad RECIBIDAS en el orden en que
 * llegaron (la cola de cada usuario) con los botones Aceptar y Rechazar, y las solicitudes
 * ENVIADAS que siguen pendientes con el botón Cancelar. Avisa a la ventana cuántas recibidas hay,
 * para mostrar el contador en el título de la pestaña.
 * Rol en la arquitectura: capa de PRESENTACIÓN de las solicitudes de amistad.
 * Principio SOLID: SRP (solo UI de solicitudes) y DIP (depende de interfaces a través de
 * ServiciosSociales).
 * Colabora con: ServicioAmistades, AccionesAmistad, FilaUsuario, ListaTarjetas, TareaEnSegundoPlano.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.amistades.SolicitudEnviada;
import una.social.amistades.SolicitudRecibida;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña de solicitudes de amistad.
 */
final class PanelSolicitudes extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario activo. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de un usuario. */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Avisa cuántas solicitudes recibidas hay (para el título de la pestaña). */
    private final transient IntConsumer alCambiarCantidad;

    /** Título con la cantidad de solicitudes recibidas. */
    private final JLabel titulo = TemaUi.crearEtiqueta("Solicitudes de amistad");

    /** Lista de solicitudes. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Solicitudes recibidas y enviadas, cargadas juntas en segundo plano.
     *
     * @param recibidas solicitudes recibidas (frente de la cola primero)
     * @param enviadas  solicitudes enviadas pendientes
     */
    private record Datos(List<SolicitudRecibida> recibidas, List<SolicitudEnviada> enviadas) {
    }

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario           usuario activo
     * @param servicios         servicios
     * @param abrirPerfil       acción para abrir el perfil de un usuario
     * @param alCambiarCantidad recibe la cantidad de solicitudes recibidas cada vez que se refresca
     */
    PanelSolicitudes(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil, IntConsumer alCambiarCantidad) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
        this.alCambiarCantidad = Objects.requireNonNull(alCambiarCantidad, "alCambiarCantidad");
        setOpaque(false);
        // Arriba: título "Tienes N solicitudes". Centro: la lista con barra de desplazamiento.
        titulo.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 3, 0, 0));
        add(titulo, BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
    }

    /**
     * Vuelve a pedir los datos a los servicios y redibuja el panel. Se llama cada vez
     * que el usuario abre esta pestaña, para que siempre muestre lo más reciente.
     */
    @Override
    public void refrescar() {
        String id = usuario.getId();
        // En segundo plano pide las recibidas (cola FIFO) y las enviadas; luego las dibuja.
        TareaEnSegundoPlano.ejecutar(() -> new Datos(servicios.amistades().listarSolicitudesRecibidas(id),
                servicios.amistades().listarSolicitudesEnviadas(id)), this::mostrar,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "LISTAR_SOLICITUDES", falla));
    }

    /**
     * Dibuja las solicitudes recibidas (en orden de llegada) y las enviadas.
     *
     * @param datos solicitudes
     */
    private void mostrar(Datos datos) {
        // Título en singular o plural según cuántas solicitudes recibió.
        int cantidad = datos.recibidas().size();
        titulo.setText(cantidad == 1 ? "Tienes 1 solicitud de amistad" : "Tienes " + cantidad + " solicitudes de amistad");
        List<JComponent> filas = new ArrayList<>();
        // Sección 1: solicitudes recibidas, en el orden de la cola (la más antigua primero),
        // cada una con los botones Aceptar y Rechazar.
        for (SolicitudRecibida solicitud : datos.recibidas()) {
            Usuario remitente = solicitud.remitente();
            filas.add(FilaUsuario.crear(remitente, FilaUsuario.detalleBasico(remitente)
                    + "  ·  Te la envió el " + TextosUi.fechaHora(solicitud.fecha()), crearBotones(remitente), abrirPerfil));
        }
        // Sección 2 (solo si hay): solicitudes que este usuario envió, con botón "Cancelar".
        if (!datos.enviadas().isEmpty()) {
            JLabel seccion = TemaUi.crearEtiqueta("Solicitudes que enviaste (pendientes)");
            seccion.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO, TemaUi.ESPACIO, 0, 0));
            filas.add(seccion);
            for (SolicitudEnviada enviada : datos.enviadas()) {
                Usuario destino = enviada.destinatario();
                BotonTema cancelar = TemaUi.crearBotonSecundario("Cancelar");
                cancelar.addActionListener(e -> AccionesAmistad.cancelarSolicitud(this, servicios.amistades(), usuario,
                        destino, servicios.auditoria(), cancelar, this::refrescar));
                filas.add(FilaUsuario.crear(destino, FilaUsuario.detalleBasico(destino)
                        + "  ·  Enviada el " + TextosUi.fechaHora(enviada.fecha()), cancelar, abrirPerfil));
            }
        }
        lista.mostrar(filas, "No tienes solicitudes de amistad pendientes.");
        // Avisa a la ventana principal para actualizar el número en la pestaña "Solicitudes (N)".
        alCambiarCantidad.accept(cantidad);
    }

    /**
     * Botones Aceptar / Rechazar de una solicitud recibida.
     *
     * @param remitente quien envió la solicitud
     * @return panel con los botones
     */
    private JPanel crearBotones(Usuario remitente) {
        BotonTema aceptar = TemaUi.crearBotonPrimario("Aceptar");
        BotonTema rechazar = TemaUi.crearBotonSecundario("Rechazar");
        // "Aceptar": desactiva los dos botones (evita doble clic) y acepta la solicitud.
        aceptar.addActionListener(e -> {
            aceptar.setEnabled(false);
            rechazar.setEnabled(false);
            AccionesAmistad.aceptar(this, servicios.amistades(), usuario, remitente, servicios.auditoria(), this::refrescar);
        });
        // "Rechazar": pide confirmación antes de rechazar.
        rechazar.addActionListener(e -> {
            if (!Dialogos.confirmar(this, "Rechazar solicitud",
                    "¿Quieres rechazar la solicitud de amistad de " + remitente.getNombre() + "?", "Rechazar")) {
                return;
            }
            aceptar.setEnabled(false);
            rechazar.setEnabled(false);
            AccionesAmistad.rechazar(this, servicios.amistades(), usuario, remitente, servicios.auditoria(), this::refrescar);
        });
        // Los dos botones juntos, alineados a la derecha de la fila.
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO, 0));
        botones.setOpaque(false);
        botones.add(aceptar);
        botones.add(rechazar);
        return botones;
    }
}
