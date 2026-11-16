/*
 * VentanaPerfil.java
 *
 * Qué hace: ventana (diálogo) con el perfil de OTRO usuario: nombre, @usuario, ID, biografía,
 * cantidad de amigos en común, estado de la amistad (con su botón), "Eliminar amigo" si lo es,
 * "Bloquear", y sus publicaciones visibles (públicas; y si son amigos, también las "solo amigos").
 * Rol en la arquitectura: capa de PRESENTACIÓN. Se abre al hacer clic en el nombre de un usuario
 * en el muro, el buscador, los amigos, las solicitudes, los comentarios o los anuncios.
 * Principio SOLID: SRP (solo UI del perfil ajeno) y DIP (depende de interfaces a través de
 * ServiciosSociales).
 * Colabora con: ServicioPublicaciones, ServicioAmistades, ServicioSugerencias, ServicioBloqueos,
 * ServicioPerfiles, CabeceraPerfil, TarjetasPublicacion, AccionesAmistad, EstadoRelacion.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Ventana de perfil de otro usuario.
 */
final class VentanaPerfil extends JDialog {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Tamaño inicial. */
    private static final Dimension TAMANO = new Dimension(760, 640);

    /** Usuario activo. */
    private final transient Usuario yo;

    /** Dueño del perfil. */
    private final transient Usuario otro;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Arma las tarjetas de sus publicaciones. */
    private final transient TarjetasPublicacion tarjetas;

    /** Contenedor de la cabecera (se reconstruye en cada refresco). */
    private final JPanel contenedorCabecera = new JPanel(new BorderLayout());

    /** Sus publicaciones visibles. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Datos del perfil cargados en segundo plano.
     *
     * @param biografia     biografía
     * @param estado        relación con el usuario activo
     * @param amigosEnComun cantidad de amigos en común
     * @param vistas        publicaciones visibles, listas para dibujar
     */
    private record Datos(String biografia, EstadoRelacion estado, int amigosEnComun, List<VistaPublicacion> vistas) {
    }

    /**
     * Crea la ventana (sin mostrarla).
     *
     * @param padre       componente dueño
     * @param yo          usuario activo
     * @param otro        dueño del perfil
     * @param servicios   servicios
     * @param abrirPerfil acción para abrir otro perfil (desde los nombres de las publicaciones)
     */
    VentanaPerfil(Component padre, Usuario yo, Usuario otro, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil) {
        super(SwingUtilities.getWindowAncestor(padre), "Perfil de " + otro.getNombre(), ModalityType.APPLICATION_MODAL);
        this.yo = Objects.requireNonNull(yo, "yo");
        this.otro = Objects.requireNonNull(otro, "otro");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.tarjetas = new TarjetasPublicacion(yo, servicios, this, this::refrescar, abrirPerfil);
        JPanel raiz = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        raiz.setBackground(TemaUi.FONDO);
        contenedorCabecera.setOpaque(false);
        raiz.add(contenedorCabecera, BorderLayout.NORTH);
        raiz.add(lista.conDesplazamiento(), BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(TAMANO);
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(padre));
    }

    /**
     * Carga el perfil y muestra la ventana (modal: espera hasta que se cierre).
     */
    void mostrar() {
        refrescar();
        setVisible(true);
    }

    /**
     * Vuelve a cargar el perfil (en segundo plano).
     */
    private void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> new Datos(
                servicios.perfiles().obtenerBiografia(otro.getId()),
                EstadoRelacion.calcular(servicios.amistades(), yo.getId(), otro.getId()),
                servicios.sugerencias().amigosEnComun(yo.getId(), otro.getId()),
                VistaPublicacion.preparar(servicios, yo, servicios.publicaciones().obtenerPerfil(yo, otro.getId()))),
                this::mostrarDatos,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "CARGAR_PERFIL", falla));
    }

    /**
     * Dibuja la cabecera y las publicaciones.
     *
     * @param datos datos cargados
     */
    private void mostrarDatos(Datos datos) {
        String comunes = datos.amigosEnComun() == 1 ? "1 amigo en común" : datos.amigosEnComun() + " amigos en común";
        contenedorCabecera.removeAll();
        contenedorCabecera.add(CabeceraPerfil.crear(otro, datos.biografia(), comunes, crearAcciones(datos.estado())));
        contenedorCabecera.revalidate();
        contenedorCabecera.repaint();
        String vacio = datos.estado() == EstadoRelacion.AMIGOS ? otro.getNombre() + " todavía no ha publicado nada."
                : "No hay publicaciones públicas. Si se hacen amigos, verás también las de \"solo amigos\".";
        lista.mostrar(tarjetas.crear(datos.vistas()), vacio);
    }

    /**
     * Botones del perfil: estado de amistad, "Eliminar amigo" (si lo es) y "Bloquear".
     *
     * @param estado relación actual
     * @return panel con los botones
     */
    private JPanel crearAcciones(EstadoRelacion estado) {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, TemaUi.ESPACIO, 0));
        acciones.setOpaque(false);
        acciones.add(AccionesAmistad.componenteEstado(this, servicios.amistades(), servicios.auditoria(), yo, otro,
                estado, this::refrescar));
        if (estado == EstadoRelacion.AMIGOS) {
            BotonTema eliminar = TemaUi.crearBotonSecundario("Eliminar amigo");
            eliminar.addActionListener(e -> AccionesAmistad.eliminarAmigo(this, servicios.amistades(), yo, otro,
                    servicios.auditoria(), eliminar, this::refrescar));
            acciones.add(eliminar);
        }
        BotonTema bloquear = TemaUi.crearBotonSecundario("Bloquear");
        bloquear.setForeground(TemaUi.ERROR);
        bloquear.addActionListener(e -> bloquear(bloquear));
        acciones.add(bloquear);
        return acciones;
    }

    /**
     * Bloquea (con confirmación) al dueño del perfil y cierra la ventana.
     *
     * @param boton botón pulsado
     */
    private void bloquear(BotonTema boton) {
        if (!Dialogos.confirmar(this, "Bloquear usuario", "¿Seguro que quieres bloquear a " + otro.getNombre() + "?\n\n"
                + "- Se eliminará la amistad y las solicitudes entre ustedes.\n"
                + "- Ninguno verá las publicaciones, comentarios ni anuncios del otro.\n"
                + "- Puedes desbloquear desde \"Mi perfil\" > \"Usuarios bloqueados\".", "Bloquear")) {
            return;
        }
        boton.setEnabled(false);
        AccionesUi.ejecutar(this, servicios.auditoria(), "BLOQUEAR", "No se pudo bloquear", () -> {
            servicios.bloqueos().bloquear(yo, otro.getId());
            return Boolean.TRUE;
        }, hecho -> {
            Dialogos.exito(this, "Usuario bloqueado", otro.getNombre() + " quedó bloqueado.");
            dispose();
        }, this::refrescar);
    }
}
