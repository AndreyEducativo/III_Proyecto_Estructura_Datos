/*
 * TarjetasPublicacion.java
 *
 * Qué hace: arma las tarjetas de las publicaciones (y de los compartidos) con sus acciones:
 * "Me gusta" (solo muestra el NÚMERO), "Ver comentarios (N)", "Compartir" (solo públicas de otros),
 * "Editar" (solo mi última publicación), "Borrar" (mis publicaciones) y "Quitar" (mis compartidos).
 * Los nombres de los autores son enlaces que abren su perfil.
 * Rol en la arquitectura: componente de la UI compartido por la pestaña Muro, "Mi perfil" y la
 * ventana de perfil de otro usuario. Así las tres muestran las publicaciones exactamente igual.
 * Principio SOLID: SRP (solo arma tarjetas y conecta sus botones) y DIP (usa los servicios por sus
 * interfaces, a través de ServiciosSociales).
 * Colabora con: VistaPublicacion, ServiciosSociales, AccionesUi, DialogoComentarios, DialogosTexto,
 * ListaTarjetas, TextosUi, AvatarUsuario.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import una.social.interacciones.EstadoMeGusta;
import una.social.modelo.Usuario;
import una.social.publicaciones.EntradaMuro;
import una.social.publicaciones.Publicacion;
import una.social.ui.tema.TemaUi;

/**
 * Fábrica de tarjetas de publicación.
 */
final class TarjetasPublicacion {

    /** Diámetro del avatar del autor. */
    private static final int DIAMETRO_AVATAR = 40;

    /** Usuario activo (sus acciones y permisos). */
    private final Usuario yo;

    /** Servicios. */
    private final ServiciosSociales servicios;

    /** Componente sobre el que se muestran los diálogos. */
    private final Component padre;

    /** Qué hacer después de una acción que cambia el muro (normalmente volver a cargarlo). */
    private final Runnable refrescar;

    /** Qué hacer al hacer clic en el nombre de un usuario (abrir su perfil). */
    private final Consumer<Usuario> abrirPerfil;

    /**
     * Crea la fábrica.
     *
     * @param yo          usuario activo
     * @param servicios   servicios
     * @param padre       componente para los diálogos
     * @param refrescar   acción para volver a cargar la lista
     * @param abrirPerfil acción para abrir el perfil de un usuario
     */
    TarjetasPublicacion(Usuario yo, ServiciosSociales servicios, Component padre, Runnable refrescar,
                        Consumer<Usuario> abrirPerfil) {
        this.yo = Objects.requireNonNull(yo, "yo");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.padre = Objects.requireNonNull(padre, "padre");
        this.refrescar = Objects.requireNonNull(refrescar, "refrescar");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
    }

    /**
     * @param vistas vistas ya preparadas en segundo plano
     * @return una tarjeta por vista, en el mismo orden
     */
    List<JComponent> crear(List<VistaPublicacion> vistas) {
        List<JComponent> tarjetas = new ArrayList<>(vistas.size());
        for (VistaPublicacion vista : vistas) {
            tarjetas.add(crearTarjeta(vista));
        }
        return tarjetas;
    }

    /**
     * Arma la tarjeta de una vista (publicación normal o compartido).
     *
     * @param vista datos de la entrada
     * @return tarjeta
     */
    private JPanel crearTarjeta(VistaPublicacion vista) {
        EntradaMuro entrada = vista.entrada();
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        if (!entrada.esCompartido()) {
            agregarPublicacion(tarjeta, vista);
            return tarjeta;
        }
        // Compartido: arriba quién compartió; abajo la original dentro de un recuadro.
        tarjeta.add(crearCabeceraCompartido(entrada), BorderLayout.NORTH);
        JPanel original = new JPanel(new BorderLayout(ListaTarjetas.ESPACIO_INTERNO, ListaTarjetas.ESPACIO_INTERNO / 2));
        original.setOpaque(false);
        original.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, TemaUi.BORDE),
                BorderFactory.createEmptyBorder(TemaUi.ESPACIO, ListaTarjetas.ESPACIO_INTERNO, 0, 0)));
        if (entrada.originalDisponible()) {
            agregarPublicacion(original, vista);
        } else {
            original.add(TextosUi.textoSecundario("La publicación original ya no está disponible."), BorderLayout.CENTER);
        }
        tarjeta.add(original, BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * "X compartió una publicación de Y · fecha" y, si el compartido es mío, el botón "Quitar".
     *
     * @param entrada compartido
     * @return cabecera
     */
    private JPanel crearCabeceraCompartido(EntradaMuro entrada) {
        Usuario quien = entrada.quienCompartio();
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cabecera.setOpaque(false);
        cabecera.add(enlaceUsuario(quien, TemaUi.FUENTE_NEGRITA));
        String deQuien = entrada.originalDisponible() ? " compartió una publicación de " : " compartió una publicación";
        cabecera.add(TextosUi.textoSecundario(deQuien));
        if (entrada.originalDisponible()) {
            cabecera.add(enlaceUsuario(entrada.autor(), TemaUi.FUENTE_PEQUENA));
        }
        cabecera.add(TextosUi.textoSecundario("  ·  " + TextosUi.fechaHora(entrada.compartido().fecha())));
        if (quien.getId().equals(yo.getId())) {
            JButton quitar = TextosUi.botonEnlace("Quitar");
            quitar.addActionListener(e -> quitarCompartido(entrada, quitar));
            cabecera.add(quitar);
        }
        return cabecera;
    }

    /**
     * Agrega a un contenedor el avatar, la cabecera, el texto y las acciones de una publicación.
     *
     * @param contenedor tarjeta o recuadro
     * @param vista      datos de la entrada (con la original disponible)
     */
    private void agregarPublicacion(JPanel contenedor, VistaPublicacion vista) {
        Usuario autor = vista.entrada().autor();
        Publicacion publicacion = vista.entrada().publicacion();
        JPanel avatar = new JPanel(new BorderLayout());
        avatar.setOpaque(false);
        avatar.add(new AvatarUsuario(autor, DIAMETRO_AVATAR), BorderLayout.NORTH);
        contenedor.add(avatar, BorderLayout.WEST);

        String nombre = autor.getId().equals(yo.getId()) ? autor.getNombre() + " (tú)" : autor.getNombre();
        StringBuilder detalle = new StringBuilder("@").append(autor.getNombreUsuario()).append("  ·  ")
                .append(TextosUi.fechaHora(publicacion.getFecha())).append("  ·  ").append(publicacion.getPrivacidad().getEtiqueta());
        publicacion.getFechaEdicion().ifPresent(f -> detalle.append("  ·  (editada)"));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JPanel lineaNombre = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lineaNombre.setOpaque(false);
        lineaNombre.add(TextosUi.enlace(nombre, TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO, () -> abrirPerfil.accept(autor)));
        cabecera.add(lineaNombre, BorderLayout.NORTH);
        cabecera.add(TextosUi.textoSecundario(detalle.toString()), BorderLayout.SOUTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        cuerpo.setOpaque(false);
        cuerpo.add(cabecera, BorderLayout.NORTH);
        cuerpo.add(TextosUi.bloqueDeTexto(publicacion.getTexto()), BorderLayout.CENTER);
        cuerpo.add(crearAcciones(vista), BorderLayout.SOUTH);
        contenedor.add(cuerpo, BorderLayout.CENTER);
    }

    /**
     * Fila de acciones de una publicación.
     *
     * @param vista datos de la entrada
     * @return fila de botones
     */
    private JPanel crearAcciones(VistaPublicacion vista) {
        Publicacion publicacion = vista.entrada().publicacion();
        boolean esMia = publicacion.getIdAutor().equals(yo.getId());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        acciones.setOpaque(false);

        JButton meGusta = TextosUi.botonEnlace(textoMeGusta(vista.meGusta()));
        meGusta.addActionListener(e -> alternarMeGusta(publicacion, meGusta));
        acciones.add(meGusta);

        JButton comentarios = TextosUi.botonEnlace("Ver comentarios (" + vista.comentarios() + ")");
        comentarios.addActionListener(e -> new DialogoComentarios(padre, yo, publicacion, vista.entrada().autor(),
                servicios, abrirPerfil).mostrar(refrescar));
        acciones.add(comentarios);

        // Compartir: solo publicaciones públicas de OTRAS personas (las reglas finales las valida el servicio).
        if (publicacion.esPublica() && !esMia) {
            JButton compartir = TextosUi.botonEnlace("Compartir");
            compartir.addActionListener(e -> compartir(publicacion, compartir));
            acciones.add(compartir);
        }
        if (vista.esEditable()) {
            JButton editar = TextosUi.botonEnlace("Editar");
            editar.addActionListener(e -> editar(publicacion));
            acciones.add(editar);
        }
        if (esMia && !vista.entrada().esCompartido()) {
            JButton borrar = TextosUi.botonEnlace("Borrar");
            borrar.setForeground(TemaUi.ERROR);
            borrar.addActionListener(e -> borrar(publicacion, borrar));
            acciones.add(borrar);
        }
        return acciones;
    }

    /**
     * @param estado estado de "me gusta"
     * @return "Me gusta (N)" o "Te gusta (N)" si el usuario activo ya dio el suyo
     */
    private static String textoMeGusta(EstadoMeGusta estado) {
        return (estado.meGustaMio() ? "Te gusta" : "Me gusta") + " (" + estado.cantidad() + ")";
    }

    /**
     * Da o quita el "me gusta" y actualiza el botón en su lugar (sin recargar todo el muro).
     *
     * @param publicacion publicación
     * @param boton       botón a actualizar
     */
    private void alternarMeGusta(Publicacion publicacion, JButton boton) {
        boton.setEnabled(false);
        AccionesUi.ejecutar(padre, servicios.auditoria(), "ME_GUSTA", "No se pudo registrar el me gusta",
                () -> servicios.meGusta().alternarMeGusta(yo, publicacion.getId()), estado -> {
                    boton.setText(textoMeGusta(estado));
                    boton.setEnabled(true);
                }, () -> boton.setEnabled(true));
    }

    /**
     * Comparte una publicación pública en el perfil del usuario activo.
     *
     * @param publicacion publicación
     * @param boton       botón pulsado
     */
    private void compartir(Publicacion publicacion, JButton boton) {
        boton.setEnabled(false);
        AccionesUi.ejecutar(padre, servicios.auditoria(), "COMPARTIR", "No se pudo compartir",
                () -> servicios.publicaciones().compartir(yo, publicacion.getId()), compartido -> {
                    // Se refresca antes del aviso para que la lista ya muestre el compartido detrás.
                    refrescar.run();
                    Dialogos.exito(padre, "Publicación compartida", "La publicación se compartió en tu perfil.");
                }, () -> boton.setEnabled(true));
    }

    /**
     * Quita (con confirmación) un compartido propio.
     *
     * @param entrada compartido
     * @param boton   botón pulsado
     */
    private void quitarCompartido(EntradaMuro entrada, JButton boton) {
        if (!Dialogos.confirmar(padre, "Quitar compartido", "¿Quieres quitar esta publicación compartida de tu perfil?", "Quitar")) {
            return;
        }
        boton.setEnabled(false);
        AccionesUi.ejecutar(padre, servicios.auditoria(), "QUITAR_COMPARTIDO", "No se pudo quitar", () -> {
            servicios.publicaciones().quitarCompartido(yo, entrada.compartido().id());
            return Boolean.TRUE;
        }, hecho -> refrescar.run(), () -> boton.setEnabled(true));
    }

    /**
     * Edita (en un diálogo) la última publicación del usuario activo.
     *
     * @param publicacion publicación a editar
     */
    private void editar(Publicacion publicacion) {
        DialogosTexto.pedirTexto(padre, "Editar publicación", "Solo puedes editar tu última publicación:",
                publicacion.getTexto(), Publicacion.LARGO_MAXIMO_TEXTO).ifPresent(texto ->
                AccionesUi.ejecutar(padre, servicios.auditoria(), "EDITAR_PUBLICACION", "No se pudo editar",
                        () -> servicios.publicaciones().editarUltima(yo, texto), editada -> refrescar.run(), refrescar));
    }

    /**
     * Borra (con confirmación) una publicación propia, con sus "me gusta" y comentarios.
     *
     * @param publicacion publicación
     * @param boton       botón pulsado
     */
    private void borrar(Publicacion publicacion, JButton boton) {
        if (!Dialogos.confirmar(padre, "Borrar publicación", "¿Seguro que quieres borrar esta publicación?\n"
                + "También se borrarán sus \"me gusta\" y comentarios. No se puede deshacer.", "Borrar")) {
            return;
        }
        boton.setEnabled(false);
        AccionesUi.ejecutar(padre, servicios.auditoria(), "BORRAR_PUBLICACION", "No se pudo borrar", () -> {
            servicios.publicaciones().borrar(yo, publicacion.getId());
            return Boolean.TRUE;
        }, hecho -> refrescar.run(), refrescar);
    }

    /**
     * @param usuario usuario
     * @param fuente  fuente del enlace
     * @return nombre del usuario como enlace a su perfil
     */
    private JComponent enlaceUsuario(Usuario usuario, Font fuente) {
        return TextosUi.enlace(usuario.getNombre(), fuente, TemaUi.TEXTO, () -> abrirPerfil.accept(usuario));
    }
}
