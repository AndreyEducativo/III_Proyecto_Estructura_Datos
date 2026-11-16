/*
 * VentanaPrincipal.java
 *
 * Qué hace: ventana principal REAL de Social UNA, que se abre tras iniciar sesión. Tiene una
 * barra superior (marca, usuario activo y "Cerrar sesión") y cuatro pestañas: "Muro",
 * "Buscar estudiantes", "Mis amigos" y "Solicitudes". Al abrirse, si el usuario tiene solicitudes
 * de amistad pendientes, le pregunta por cada una si quiere aceptarla o rechazarla.
 * Rol en la arquitectura: reemplaza a VentanaPrincipalTemporal. Main la registra en la
 * FabricaVentanaPrincipal; el ControladorNavegacion la muestra y gestiona el cierre de sesión.
 * Principio SOLID: SRP (solo arma la navegación; cada pestaña es su propia clase), OCP (se
 * integró cambiando la fábrica en Main, sin tocar el login ni el controlador), DIP (recibe los
 * servicios como interfaces).
 * Colabora con: PanelMuro, PanelBuscador, PanelAmigos, PanelSolicitudes, AccionesAmistad,
 * ServicioPublicaciones, ServicioAmistades,
 * ServicioBusqueda, ControladorNavegacion, TemaUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import una.social.amistades.ServicioAmistades;
import una.social.amistades.ServicioBusqueda;
import una.social.amistades.SolicitudRecibida;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.publicaciones.ServicioPublicaciones;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Ventana principal con muro, buscador y amigos.
 */
public final class VentanaPrincipal extends JFrame {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Tamaño inicial de la ventana. */
    private static final Dimension TAMANO_INICIAL = new Dimension(920, 720);

    /** Tamaño mínimo: por debajo de esto los controles no caben cómodamente. */
    private static final Dimension TAMANO_MINIMO = new Dimension(720, 540);

    /** Diámetro del avatar en la barra superior. */
    private static final int DIAMETRO_AVATAR = 36;

    /** Tamaño de letra de la marca en la barra superior. */
    private static final float TAMANO_MARCA = 22f;

    /** Margen vertical de la barra superior. */
    private static final int MARGEN_BARRA_VERTICAL = 10;

    /** Margen horizontal de la barra superior. */
    private static final int MARGEN_BARRA_HORIZONTAL = 18;

    /** Posición de la pestaña "Solicitudes" (la última). */
    private static final int PESTANA_SOLICITUDES = 3;

    /** Título base de la pestaña de solicitudes (se le agrega la cantidad pendiente). */
    private static final String TITULO_SOLICITUDES = "Solicitudes";

    /** Pestañas de navegación. */
    private final JTabbedPane pestanas = new JTabbedPane();

    /** Secciones en el mismo orden que las pestañas. */
    private final transient List<SeccionPrincipal> secciones;

    /** Usuario que inició sesión. */
    private final transient Usuario usuarioActivo;

    /** Servicio de amistades (para el aviso de solicitudes al abrir la ventana). */
    private final transient ServicioAmistades amistades;

    /** Bitácora. */
    private final transient RegistroAuditoria auditoria;

    /**
     * Crea la ventana (sin mostrarla). Se llama desde el hilo de Swing, a través de la
     * FabricaVentanaPrincipal registrada en Main.
     *
     * @param usuarioActivo  usuario que inició sesión
     * @param alCerrarSesion acción del botón "Cerrar sesión" (la da el controlador)
     * @param publicaciones  servicio de publicaciones
     * @param amistades      servicio de amistades
     * @param busqueda       servicio de búsqueda
     * @param auditoria      bitácora
     */
    public VentanaPrincipal(Usuario usuarioActivo, Runnable alCerrarSesion, ServicioPublicaciones publicaciones,
                            ServicioAmistades amistades, ServicioBusqueda busqueda, RegistroAuditoria auditoria) {
        super("Social UNA - " + usuarioActivo.getNombre());
        Objects.requireNonNull(alCerrarSesion, "alCerrarSesion");
        this.usuarioActivo = usuarioActivo;
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        PanelMuro muro = new PanelMuro(usuarioActivo, publicaciones, auditoria);
        PanelBuscador buscador = new PanelBuscador(usuarioActivo, busqueda, amistades, auditoria);
        PanelAmigos panelAmigos = new PanelAmigos(usuarioActivo, amistades, auditoria);
        PanelSolicitudes panelSolicitudes = new PanelSolicitudes(usuarioActivo, amistades, auditoria,
                this::mostrarCantidadSolicitudes);
        this.secciones = List.of(muro, buscador, panelAmigos, panelSolicitudes);

        configurarPestanas(muro, buscador, panelAmigos, panelSolicitudes);
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TemaUi.FONDO);
        raiz.add(crearBarraSuperior(usuarioActivo, alCerrarSesion), BorderLayout.NORTH);
        raiz.add(pestanas, BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(TAMANO_INICIAL);
        setMinimumSize(TAMANO_MINIMO);
        // Se carga la primera pestaña (el muro) apenas se crea la ventana, y la de solicitudes
        // para que su título muestre cuántas hay pendientes.
        muro.refrescar();
        panelSolicitudes.refrescar();
        // Cuando la ventana ya está visible, se avisa de las solicitudes pendientes.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                avisarSolicitudesPendientes();
            }
        });
    }

    /**
     * Pone la cantidad de solicitudes pendientes en el título de la pestaña, por ejemplo
     * "Solicitudes (2)".
     *
     * @param cantidad solicitudes pendientes
     */
    private void mostrarCantidadSolicitudes(int cantidad) {
        pestanas.setTitleAt(PESTANA_SOLICITUDES, cantidad == 0 ? TITULO_SOLICITUDES : TITULO_SOLICITUDES + " (" + cantidad + ")");
    }

    /**
     * Busca las solicitudes pendientes y, si hay, pregunta por cada una: "X te envió una solicitud
     * de amistad" con Aceptar / Rechazar / Decidir después.
     */
    private void avisarSolicitudesPendientes() {
        TareaEnSegundoPlano.ejecutar(() -> amistades.listarSolicitudesRecibidas(usuarioActivo.getId()),
                solicitudes -> preguntarDesde(solicitudes, 0),
                falla -> Dialogos.errorInesperado(this, auditoria, "AVISAR_SOLICITUDES", falla));
    }

    /**
     * Pregunta por la solicitud número {@code indice} y, al terminar, sigue con la siguiente. Se
     * hace una a la vez (y no en un ciclo) porque cada respuesta se guarda en segundo plano y hay
     * que esperar a que termine antes de mostrar el siguiente diálogo.
     *
     * @param solicitudes solicitudes pendientes
     * @param indice      posición de la solicitud a preguntar
     */
    private void preguntarDesde(List<SolicitudRecibida> solicitudes, int indice) {
        if (indice >= solicitudes.size()) {
            // Al terminar se refresca la pestaña visible y el contador de solicitudes.
            secciones.get(pestanas.getSelectedIndex()).refrescar();
            secciones.get(PESTANA_SOLICITUDES).refrescar();
            return;
        }
        AccionesAmistad.preguntarRespuesta(this, amistades, usuarioActivo, solicitudes.get(indice).remitente(),
                auditoria, () -> preguntarDesde(solicitudes, indice + 1));
    }

    /**
     * Crea las pestañas, con atajos de teclado (Alt+M, Alt+B, Alt+A, Alt+S), y hace que cada
     * pestaña se actualice al abrirla.
     *
     * @param muro        pestaña del muro
     * @param buscador    pestaña del buscador
     * @param amigos      pestaña de amigos
     * @param solicitudes pestaña de solicitudes recibidas
     */
    private void configurarPestanas(PanelMuro muro, PanelBuscador buscador, PanelAmigos amigos,
                                    PanelSolicitudes solicitudes) {
        pestanas.setFont(TemaUi.FUENTE_NEGRITA);
        pestanas.setBackground(TemaUi.FONDO);
        pestanas.addTab("Muro", muro);
        pestanas.addTab("Buscar estudiantes", buscador);
        pestanas.addTab("Mis amigos", amigos);
        pestanas.addTab(TITULO_SOLICITUDES, solicitudes);
        pestanas.setMnemonicAt(0, KeyEvent.VK_M);
        pestanas.setMnemonicAt(1, KeyEvent.VK_B);
        pestanas.setMnemonicAt(2, KeyEvent.VK_A);
        pestanas.setMnemonicAt(PESTANA_SOLICITUDES, KeyEvent.VK_S);
        pestanas.setToolTipTextAt(0, "Tus publicaciones y las de tus amigos (Alt+M)");
        pestanas.setToolTipTextAt(1, "Encuentra estudiantes y envíales una solicitud de amistad (Alt+B)");
        pestanas.setToolTipTextAt(2, "Lista de tus amigos (Alt+A)");
        pestanas.setToolTipTextAt(PESTANA_SOLICITUDES, "Solicitudes de amistad que recibiste (Alt+S)");
        // Al cambiar de pestaña se refresca la elegida: así, por ejemplo, tras agregar un amigo
        // en el buscador, el muro y la lista de amigos ya lo incluyen al abrirlos.
        pestanas.addChangeListener(e -> {
            int indice = pestanas.getSelectedIndex();
            if (indice >= 0) {
                secciones.get(indice).refrescar();
            }
        });
    }

    /**
     * Crea la barra superior: marca a la izquierda; avatar, nombre y "Cerrar sesión" a la derecha.
     *
     * @param usuario        usuario activo
     * @param alCerrarSesion acción de cerrar sesión
     * @return barra superior
     */
    private JPanel crearBarraSuperior(Usuario usuario, Runnable alCerrarSesion) {
        JLabel marca = new JLabel("Social UNA");
        marca.setFont(TemaUi.FUENTE_TITULO.deriveFont(TAMANO_MARCA));
        marca.setForeground(TemaUi.ROJO_UNA);

        JPanel datosUsuario = new JPanel(new GridLayout(2, 1));
        datosUsuario.setOpaque(false);
        datosUsuario.add(TextosUi.etiquetaPlana(usuario.getNombre(), TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO));
        datosUsuario.add(TextosUi.textoSecundario(FilaUsuario.detalleBasico(usuario)));

        BotonTema botonCerrarSesion = TemaUi.crearBotonSecundario("Cerrar sesión");
        botonCerrarSesion.addActionListener(e -> {
            // Se deshabilita para evitar un doble clic mientras se guardan los datos.
            botonCerrarSesion.setEnabled(false);
            alCerrarSesion.run();
        });

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO * 2, 0));
        derecha.setOpaque(false);
        derecha.add(new AvatarUsuario(usuario, DIAMETRO_AVATAR));
        derecha.add(datosUsuario);
        derecha.add(botonCerrarSesion);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(TemaUi.SUPERFICIE);
        // Borde inferior fino para separar la barra del contenido.
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TemaUi.BORDE),
                BorderFactory.createEmptyBorder(MARGEN_BARRA_VERTICAL, MARGEN_BARRA_HORIZONTAL,
                        MARGEN_BARRA_VERTICAL, MARGEN_BARRA_HORIZONTAL)));
        barra.add(marca, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }
}
