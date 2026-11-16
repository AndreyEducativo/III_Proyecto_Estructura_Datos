/*
 * VentanaPrincipal.java
 *
 * Qué hace: ventana principal de Social UNA, que se abre tras iniciar sesión. Tiene una barra
 * superior (marca, usuario activo y "Cerrar sesión") y seis pestañas: "Muro", "Anuncios",
 * "Buscar estudiantes", "Mis amigos", "Solicitudes" (con el contador de pendientes) y "Mi perfil".
 * Al abrirse, avisa de las solicitudes de amistad pendientes. Al hacer clic en el nombre de
 * cualquier usuario abre su perfil (o "Mi perfil" si es uno mismo).
 * Rol en la arquitectura: Main la registra en la FabricaVentanaPrincipal; el ControladorNavegacion
 * la muestra y gestiona el cierre de sesión.
 * Principio SOLID: SRP (solo arma la navegación; cada pestaña es su propia clase), OCP (se integró
 * cambiando la fábrica en Main, sin tocar el login ni el controlador), DIP (recibe interfaces).
 * Colabora con: PanelMuro, PanelAnuncios, PanelBuscador, PanelAmigos, PanelSolicitudes,
 * PanelMiPerfil, VentanaPerfil, ServiciosSociales, AccionesAmistad, TemaUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
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
import una.social.amistades.SolicitudRecibida;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Ventana principal de la red social.
 */
public final class VentanaPrincipal extends JFrame {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Tamaño inicial de la ventana. */
    private static final Dimension TAMANO_INICIAL = new Dimension(980, 740);

    /** Tamaño mínimo: por debajo de esto los controles no caben cómodamente. */
    private static final Dimension TAMANO_MINIMO = new Dimension(800, 560);

    /** Diámetro del avatar en la barra superior. */
    private static final int DIAMETRO_AVATAR = 36;

    /** Tamaño de letra de la marca en la barra superior. */
    private static final float TAMANO_MARCA = 22f;

    /** Margen vertical de la barra superior. */
    private static final int MARGEN_BARRA_VERTICAL = 10;

    /** Margen horizontal de la barra superior. */
    private static final int MARGEN_BARRA_HORIZONTAL = 18;

    /** Título de la pestaña de solicitudes (se le agrega el contador). */
    private static final String TITULO_SOLICITUDES = "Solicitudes";

    /** Pestañas de navegación. */
    private final JTabbedPane pestanas = new JTabbedPane();

    /** Secciones en el mismo orden que las pestañas. */
    private final transient List<SeccionPrincipal> secciones;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Pestaña de solicitudes (para actualizar su contador). */
    private final PanelSolicitudes panelSolicitudes;

    /** Pestaña "Mi perfil" (para abrirla al hacer clic en el propio nombre). */
    private final PanelMiPerfil panelMiPerfil;

    /** Etiqueta con el nombre del usuario en la barra superior (cambia si edita su nombre). */
    private final JLabel etiquetaNombre;

    /** Usuario activo (se reemplaza si cambia su nombre: Usuario es inmutable). */
    private transient Usuario usuarioActivo;

    /**
     * Crea la ventana (sin mostrarla). Se llama desde el hilo de Swing, a través de la
     * FabricaVentanaPrincipal registrada en Main.
     *
     * @param usuarioActivo  usuario que inició sesión
     * @param alCerrarSesion acción del botón "Cerrar sesión" (la da el controlador)
     * @param servicios      servicios de la red social
     */
    public VentanaPrincipal(Usuario usuarioActivo, Runnable alCerrarSesion, ServiciosSociales servicios) {
        super("Social UNA - " + usuarioActivo.getNombre());
        Objects.requireNonNull(alCerrarSesion, "alCerrarSesion");
        this.usuarioActivo = usuarioActivo;
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.etiquetaNombre = TextosUi.etiquetaPlana(usuarioActivo.getNombre(), TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO);
        PanelMuro muro = new PanelMuro(usuarioActivo, servicios, this::abrirPerfil);
        PanelAnuncios anuncios = new PanelAnuncios(usuarioActivo, servicios, this::abrirPerfil);
        PanelBuscador buscador = new PanelBuscador(usuarioActivo, servicios, this::abrirPerfil);
        PanelAmigos amigos = new PanelAmigos(usuarioActivo, servicios, this::abrirPerfil);
        this.panelSolicitudes = new PanelSolicitudes(usuarioActivo, servicios, this::abrirPerfil, this::mostrarCantidadSolicitudes);
        this.panelMiPerfil = new PanelMiPerfil(usuarioActivo, servicios, this::abrirPerfil, this::alCambiarNombre);
        this.secciones = List.of(muro, anuncios, buscador, amigos, panelSolicitudes, panelMiPerfil);

        configurarPestanas();
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
            /** Al abrirse la ventana: preguntar por las solicitudes pendientes. */
            @Override
            public void windowOpened(WindowEvent e) {
                avisarSolicitudesPendientes();
            }
        });
    }

    /**
     * Crea las pestañas con atajos de teclado y hace que cada una se actualice al abrirla.
     */
    private void configurarPestanas() {
        pestanas.setFont(TemaUi.FUENTE_NEGRITA);
        pestanas.setBackground(TemaUi.FONDO);
        agregarPestana("Muro", KeyEvent.VK_M, "Tus publicaciones, las de tus amigos y el muro general (Alt+M)");
        agregarPestana("Anuncios", KeyEvent.VK_N, "Anuncios públicos para toda la comunidad (Alt+N)");
        agregarPestana("Buscar estudiantes", KeyEvent.VK_B, "Encuentra estudiantes y personas que quizás conozcas (Alt+B)");
        agregarPestana("Mis amigos", KeyEvent.VK_A, "Lista de tus amigos (Alt+A)");
        agregarPestana(TITULO_SOLICITUDES, KeyEvent.VK_S, "Solicitudes de amistad recibidas y enviadas (Alt+S)");
        agregarPestana("Mi perfil", KeyEvent.VK_P, "Tu perfil, tus publicaciones y tu cuenta (Alt+P)");
        // Al cambiar de pestaña se refresca la elegida: así siempre se ven datos actualizados.
        pestanas.addChangeListener(e -> {
            int indice = pestanas.getSelectedIndex();
            if (indice >= 0) {
                secciones.get(indice).refrescar();
            }
        });
    }

    /**
     * Agrega la pestaña de la siguiente sección.
     *
     * @param titulo   título de la pestaña
     * @param mnemonico tecla del atajo Alt+...
     * @param ayuda    texto de ayuda
     */
    private void agregarPestana(String titulo, int mnemonico, String ayuda) {
        int indice = pestanas.getTabCount();
        pestanas.addTab(titulo, (Component) secciones.get(indice));
        pestanas.setMnemonicAt(indice, mnemonico);
        pestanas.setToolTipTextAt(indice, ayuda);
    }

    /**
     * Abre el perfil de un usuario: "Mi perfil" si es uno mismo; si no, la ventana de perfil (salvo
     * que haya un bloqueo entre ambos).
     *
     * @param usuario usuario cuyo perfil se quiere ver
     */
    private void abrirPerfil(Usuario usuario) {
        if (usuario.getId().equals(usuarioActivo.getId())) {
            pestanas.setSelectedComponent(panelMiPerfil);
            return;
        }
        TareaEnSegundoPlano.ejecutar(() -> servicios.bloqueos().hayBloqueoEntre(usuarioActivo.getId(), usuario.getId()), hayBloqueo -> {
            if (hayBloqueo) {
                Dialogos.advertencia(this, "Perfil no disponible", "No puedes ver este perfil porque hay un bloqueo entre ustedes.");
                return;
            }
            new VentanaPerfil(this, usuarioActivo, usuario, servicios, this::abrirPerfil).mostrar();
            // Al cerrar el perfil (pudo haber enviado una solicitud, eliminado o bloqueado): refrescar.
            refrescarVisibles();
        }, falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "ABRIR_PERFIL", falla));
    }

    /**
     * Refresca la pestaña visible y el contador de solicitudes.
     */
    private void refrescarVisibles() {
        secciones.get(pestanas.getSelectedIndex()).refrescar();
        if (pestanas.getSelectedComponent() != panelSolicitudes) {
            panelSolicitudes.refrescar();
        }
    }

    /**
     * Actualiza el título de la pestaña de solicitudes con la cantidad pendiente.
     *
     * @param cantidad solicitudes recibidas pendientes
     */
    private void mostrarCantidadSolicitudes(int cantidad) {
        int indice = pestanas.indexOfComponent(panelSolicitudes);
        pestanas.setTitleAt(indice, cantidad == 0 ? TITULO_SOLICITUDES : TITULO_SOLICITUDES + " (" + cantidad + ")");
    }

    /**
     * Actualiza el nombre mostrado cuando el usuario lo cambia desde "Mi perfil".
     *
     * @param actualizado usuario con el nombre nuevo
     */
    private void alCambiarNombre(Usuario actualizado) {
        usuarioActivo = actualizado;
        etiquetaNombre.setText(actualizado.getNombre());
        setTitle("Social UNA - " + actualizado.getNombre());
    }

    /**
     * Pregunta, una por una y en el orden en que llegaron, por las solicitudes pendientes.
     */
    private void avisarSolicitudesPendientes() {
        TareaEnSegundoPlano.ejecutar(() -> servicios.amistades().listarSolicitudesRecibidas(usuarioActivo.getId()),
                solicitudes -> preguntarDesde(solicitudes, 0),
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "AVISAR_SOLICITUDES", falla));
    }

    /**
     * Pregunta por la solicitud {@code indice} y, al responder, sigue con la siguiente.
     *
     * @param solicitudes solicitudes pendientes (frente de la cola primero)
     * @param indice      solicitud actual
     */
    private void preguntarDesde(List<SolicitudRecibida> solicitudes, int indice) {
        if (indice >= solicitudes.size()) {
            refrescarVisibles();
            return;
        }
        AccionesAmistad.preguntarRespuesta(this, servicios.amistades(), usuarioActivo, solicitudes.get(indice).remitente(),
                servicios.auditoria(), () -> preguntarDesde(solicitudes, indice + 1));
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
        datosUsuario.add(etiquetaNombre);
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
