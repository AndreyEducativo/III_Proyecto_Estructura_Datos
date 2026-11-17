/*
 * PanelMiPerfil.java
 *
 * Qué hace: pestaña "Mi perfil". Muestra mi nombre, @usuario, ID y biografía, y SOLO mis
 * publicaciones y mis compartidos (de lo más reciente a lo más antiguo). Desde aquí se puede editar
 * la biografía y el nombre, cambiar la contraseña, editar o deshacer la última publicación y ver
 * los usuarios bloqueados.
 * Rol en la arquitectura: capa de PRESENTACIÓN del perfil propio.
 * Principio SOLID: SRP (solo UI del perfil; las publicaciones las dibuja TarjetasPublicacion) y DIP
 * (depende de interfaces a través de ServiciosSociales).
 * Colabora con: ServicioPerfiles, ServicioCuenta, ServicioPublicaciones, CabeceraPerfil,
 * TarjetasPublicacion, DialogosTexto, DialogoCambioContrasena, DialogoBloqueados.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import una.social.modelo.Usuario;
import una.social.perfiles.Perfil;
import una.social.publicaciones.Publicacion;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña del perfil propio.
 */
final class PanelMiPerfil extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de otro usuario (desde los nombres de las publicaciones). */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Avisa a la ventana que el nombre cambió (para actualizar la barra superior). */
    private final transient Consumer<Usuario> alCambiarNombre;

    /** Arma las tarjetas de mis publicaciones. */
    private final transient TarjetasPublicacion tarjetas;

    /** Usuario activo (se reemplaza al cambiar el nombre, porque Usuario es inmutable). */
    private transient Usuario usuario;

    /** Contenedor de la cabecera (se reconstruye en cada refresco). */
    private final JPanel contenedorCabecera = new JPanel(new BorderLayout());

    /** Mis publicaciones y compartidos. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Datos del perfil cargados en segundo plano.
     *
     * @param biografia     biografía
     * @param puedeDeshacer si hay publicaciones de esta sesión para deshacer
     * @param ultima        mi última publicación (si tengo)
     * @param vistas        mis publicaciones y compartidos, listos para dibujar
     */
    private record Datos(String biografia, boolean puedeDeshacer, Optional<Publicacion> ultima, List<VistaPublicacion> vistas) {
    }

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario         usuario activo
     * @param servicios       servicios
     * @param abrirPerfil     acción para abrir el perfil de otro usuario
     * @param alCambiarNombre acción para avisar que el usuario cambió de nombre
     */
    PanelMiPerfil(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil, Consumer<Usuario> alCambiarNombre) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
        this.alCambiarNombre = Objects.requireNonNull(alCambiarNombre, "alCambiarNombre");
        // Fábrica de tarjetas para mostrar mis publicaciones.
        this.tarjetas = new TarjetasPublicacion(usuario, servicios, this, this::refrescar, abrirPerfil);
        setOpaque(false);
        // Arriba la cabecera (avatar, nombre, biografía y botones); al centro mis publicaciones.
        contenedorCabecera.setOpaque(false);
        add(contenedorCabecera, BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
    }

    /**
     * Vuelve a pedir los datos a los servicios y redibuja el panel. Se llama cada vez
     * que el usuario abre esta pestaña, para que siempre muestre lo más reciente.
     */
    @Override
    public void refrescar() {
        Usuario yo = usuario;
        // En segundo plano se piden: biografía, si hay algo para deshacer, la última publicación
        // y mis publicaciones con sus contadores. Luego se dibuja todo junto.
        TareaEnSegundoPlano.ejecutar(() -> new Datos(
                servicios.perfiles().obtenerBiografia(yo.getId()),
                servicios.publicaciones().puedeDeshacer(yo),
                servicios.publicaciones().ultimaPublicacionDe(yo.getId()),
                VistaPublicacion.preparar(servicios, yo, servicios.publicaciones().obtenerPerfil(yo, yo.getId()))),
                this::mostrar,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "CARGAR_MI_PERFIL", falla));
    }

    /**
     * Dibuja la cabecera y mis publicaciones.
     *
     * @param datos datos cargados
     */
    private void mostrar(Datos datos) {
        // Reemplaza la cabecera vieja por una nueva con los datos actuales.
        contenedorCabecera.removeAll();
        contenedorCabecera.add(CabeceraPerfil.crear(usuario, datos.biografia(), null, crearAcciones(datos)));
        contenedorCabecera.revalidate();
        contenedorCabecera.repaint();
        // Debajo, mis publicaciones y lo que compartí.
        lista.mostrar(tarjetas.crear(datos.vistas()), "Todavía no has publicado ni compartido nada.");
    }

    /**
     * Botones del perfil propio.
     *
     * @param datos datos cargados (para habilitar "Editar" y "Deshacer" solo cuando aplican)
     * @return panel con los botones
     */
    private JPanel crearAcciones(Datos datos) {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        acciones.setOpaque(false);
        // Datos de la cuenta: biografía, nombre y contraseña.
        JButton bio = TextosUi.botonEnlace("Editar biografía");
        bio.addActionListener(e -> editarBiografia(datos.biografia()));
        JButton nombre = TextosUi.botonEnlace("Editar nombre");
        nombre.addActionListener(e -> editarNombre());
        JButton contrasena = TextosUi.botonEnlace("Cambiar contraseña");
        contrasena.addActionListener(e -> new DialogoCambioContrasena(this, usuario, servicios).setVisible(true));
        // "Editar última" solo se activa si tengo alguna publicación.
        JButton editar = TextosUi.botonEnlace("Editar última publicación");
        editar.setEnabled(datos.ultima().isPresent());
        editar.addActionListener(e -> datos.ultima().ifPresent(this::editarUltima));
        // "Deshacer" solo se activa si la pila de esta sesión tiene algo.
        JButton deshacer = TextosUi.botonEnlace("Deshacer última publicación");
        deshacer.setEnabled(datos.puedeDeshacer());
        deshacer.setToolTipText("Borra la última publicación que hiciste desde que abriste la aplicación");
        deshacer.addActionListener(e -> deshacer());
        // Abre la lista de bloqueados; al cerrarla se recarga el perfil.
        JButton bloqueados = TextosUi.botonEnlace("Usuarios bloqueados");
        bloqueados.addActionListener(e -> {
            new DialogoBloqueados(this, usuario, servicios).mostrar();
            refrescar();
        });
        // Se agregan todos los botones en una fila.
        for (JButton boton : List.of(bio, nombre, contrasena, editar, deshacer, bloqueados)) {
            acciones.add(boton);
        }
        return acciones;
    }

    /**
     * Edita la biografía en un diálogo.
     *
     * @param actual biografía actual
     */
    private void editarBiografia(String actual) {
        DialogosTexto.pedirTexto(this, "Editar biografía", "Cuéntale a la comunidad sobre ti (déjala vacía para quitarla):",
                actual, Perfil.LARGO_MAXIMO_BIOGRAFIA).ifPresent(texto ->
                AccionesUi.ejecutar(this, servicios.auditoria(), "EDITAR_BIOGRAFIA", "No se pudo guardar la biografía",
                        () -> servicios.perfiles().actualizarBiografia(usuario, texto), guardada -> refrescar(), this::refrescar));
    }

    /**
     * Cambia el nombre completo (mismas reglas que el registro).
     */
    private void editarNombre() {
        Object respuesta = JOptionPane.showInputDialog(this, "Nombre completo:", "Editar nombre",
                JOptionPane.PLAIN_MESSAGE, null, null, usuario.getNombre());
        if (respuesta == null) {
            return; // El usuario canceló.
        }
        String nuevo = respuesta.toString();
        AccionesUi.ejecutar(this, servicios.auditoria(), "EDITAR_NOMBRE", "No se pudo cambiar el nombre",
                () -> servicios.cuenta().cambiarNombre(usuario, nuevo), actualizado -> {
                    usuario = actualizado;
                    // La sesión y la barra superior deben mostrar el nombre nuevo.
                    servicios.sesion().actualizarUsuarioActivo(actualizado);
                    alCambiarNombre.accept(actualizado);
                    refrescar();
                }, this::refrescar);
    }

    /**
     * Edita la última publicación en un diálogo.
     *
     * @param ultima mi última publicación
     */
    private void editarUltima(Publicacion ultima) {
        DialogosTexto.pedirTexto(this, "Editar última publicación", "Solo puedes editar tu última publicación:",
                ultima.getTexto(), Publicacion.LARGO_MAXIMO_TEXTO).ifPresent(texto ->
                AccionesUi.ejecutar(this, servicios.auditoria(), "EDITAR_PUBLICACION", "No se pudo editar",
                        () -> servicios.publicaciones().editarUltima(usuario, texto), editada -> refrescar(), this::refrescar));
    }

    /**
     * Deshace (con confirmación) la última publicación de esta sesión, sacándola de la pila.
     */
    private void deshacer() {
        if (!Dialogos.confirmar(this, "Deshacer última publicación", "Se borrará la última publicación que hiciste "
                + "desde que abriste la aplicación,\ncon sus \"me gusta\" y comentarios. ¿Continuar?", "Deshacer")) {
            return;
        }
        AccionesUi.ejecutar(this, servicios.auditoria(), "DESHACER_PUBLICACION", "No se pudo deshacer",
                () -> servicios.publicaciones().deshacerUltima(usuario), borrada -> {
                    // Primero se refresca (en segundo plano) y luego se avisa: así, detrás del
                    // aviso, la lista y el botón "Deshacer" ya muestran el estado nuevo.
                    refrescar();
                    Dialogos.exito(this, "Publicación deshecha", "Se borró tu publicación " + borrada.getId() + ".");
                }, this::refrescar);
    }
}
