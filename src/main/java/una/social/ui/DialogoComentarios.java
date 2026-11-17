/*
 * DialogoComentarios.java
 *
 * Qué hace: ventana (diálogo) con los comentarios de una publicación, del más antiguo al más
 * reciente, y un cuadro para escribir un comentario nuevo. Permite borrar un comentario a su autor
 * o al autor de la publicación.
 * Rol en la arquitectura: capa de PRESENTACIÓN de los comentarios. Las reglas (visibilidad, largo,
 * permisos, bloqueos) están en ServicioComentarios.
 * Principio SOLID: SRP (solo UI de comentarios) y DIP (usa ServiciosSociales).
 * Colabora con: ServicioComentarios, ListaTarjetas, AccionesUi, TextosUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.interacciones.Comentario;
import una.social.interacciones.ComentarioVisible;
import una.social.modelo.Usuario;
import una.social.publicaciones.Publicacion;
import una.social.ui.tema.AreaTextoTema;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Diálogo de comentarios de una publicación.
 */
final class DialogoComentarios extends JDialog {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Tamaño inicial del diálogo. */
    private static final Dimension TAMANO = new Dimension(560, 560);

    /** Filas visibles del cuadro para comentar. */
    private static final int FILAS_CUADRO = 2;

    /** Usuario activo. */
    private final transient Usuario yo;

    /** Publicación comentada. */
    private final transient Publicacion publicacion;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de un usuario (al hacer clic en su nombre). */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Lista de comentarios. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /** Cuadro para escribir. */
    private final AreaTextoTema cuadro = new AreaTextoTema("Escribe un comentario…", FILAS_CUADRO);

    /** Error en línea bajo el cuadro. */
    private final JLabel error = TemaUi.crearEtiquetaError();

    /** Botón de comentar. */
    private final BotonTema botonComentar = TemaUi.crearBotonPrimario("Comentar");

    /** Qué hacer al cerrar (refrescar el contador del muro). */
    private transient Runnable alCerrar = () -> { };

    /**
     * Crea el diálogo (sin mostrarlo).
     *
     * @param padre        componente dueño (se usa su ventana)
     * @param yo           usuario activo
     * @param publicacion  publicación comentada
     * @param autor        autor de la publicación (para el título)
     * @param servicios    servicios
     * @param abrirPerfil  acción para abrir un perfil
     */
    DialogoComentarios(Component padre, Usuario yo, Publicacion publicacion, Usuario autor, ServiciosSociales servicios,
                       Consumer<Usuario> abrirPerfil) {
        // Diálogo modal con el nombre del autor de la publicación en el título.
        super(SwingUtilities.getWindowAncestor(padre), "Comentarios de la publicación de " + autor.getNombre(),
                ModalityType.APPLICATION_MODAL);
        this.yo = Objects.requireNonNull(yo, "yo");
        this.publicacion = Objects.requireNonNull(publicacion, "publicacion");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
        // Centro: lista de comentarios. Abajo: cuadro para escribir uno nuevo.
        JPanel raiz = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        raiz.setBackground(TemaUi.FONDO);
        raiz.add(lista.conDesplazamiento(), BorderLayout.CENTER);
        raiz.add(crearCompositor(), BorderLayout.SOUTH);
        setContentPane(raiz);
        // Tamaño fijo y centrado sobre la ventana principal.
        setSize(TAMANO);
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(padre));
        // El botón "Comentar" publica el comentario.
        botonComentar.addActionListener(e -> comentar());
    }

    /**
     * Muestra el diálogo (modal: bloquea la ventana de atrás hasta cerrarlo) y al cerrarlo ejecuta
     * {@code alCerrar}.
     *
     * @param alCerrarDialogo qué hacer al cerrar (normalmente refrescar el muro)
     */
    void mostrar(Runnable alCerrarDialogo) {
        // Guarda qué hacer al cerrar, carga los comentarios y muestra el diálogo.
        this.alCerrar = Objects.requireNonNull(alCerrarDialogo, "alCerrarDialogo");
        refrescar();
        setVisible(true);
        // setVisible(true) de un diálogo modal "espera" aquí hasta que se cierre.
        alCerrar.run();
    }

    /**
     * Arma el cuadro para comentar.
     *
     * @return panel inferior
     */
    private JPanel crearCompositor() {
        // Tarjeta con el cuadro de texto.
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        tarjeta.add(cuadro, BorderLayout.CENTER);
        // Pie: mensaje de error a la izquierda y botón "Comentar" a la derecha.
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(error, BorderLayout.CENTER);
        pie.add(botonComentar, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);
        // Margen alrededor de la tarjeta.
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(0, TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2));
        contenedor.add(tarjeta);
        return contenedor;
    }

    /**
     * Vuelve a cargar los comentarios (en segundo plano).
     */
    private void refrescar() {
        // Pide los comentarios visibles en segundo plano y luego los dibuja.
        TareaEnSegundoPlano.ejecutar(() -> servicios.comentarios().listar(yo, publicacion.getId()), this::mostrarComentarios,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "LISTAR_COMENTARIOS", falla));
    }

    /**
     * Dibuja los comentarios.
     *
     * @param comentarios comentarios visibles, del más antiguo al más reciente
     */
    private void mostrarComentarios(List<ComentarioVisible> comentarios) {
        // Una fila por comentario; si no hay, se muestra el mensaje de lista vacía.
        List<JComponent> filas = new ArrayList<>(comentarios.size());
        for (ComentarioVisible visible : comentarios) {
            filas.add(crearFila(visible));
        }
        lista.mostrar(filas, "Todavía no hay comentarios. ¡Sé el primero en comentar!");
    }

    /**
     * Arma la fila de un comentario: autor (enlace), fecha, texto y, si se puede, "Borrar".
     *
     * @param visible comentario con su autor
     * @return fila
     */
    private JPanel crearFila(ComentarioVisible visible) {
        Comentario comentario = visible.comentario();
        Usuario autor = visible.autor();
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cabecera.setOpaque(false);
        // Nombre del autor como enlace: cierra este diálogo y abre su perfil.
        cabecera.add(TextosUi.enlace(autor.getNombre(), TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO, () -> {
            dispose();
            abrirPerfil.accept(autor);
        }));
        // Fecha del comentario.
        cabecera.add(TextosUi.textoSecundario("  ·  " + TextosUi.fechaHora(comentario.fecha())));
        // "Borrar" solo si el comentario es mío o la publicación es mía.
        if (visible.puedeBorrar()) {
            JButton borrar = TextosUi.botonEnlace("Borrar");
            borrar.setForeground(TemaUi.ERROR);
            borrar.addActionListener(e -> borrar(comentario, borrar));
            cabecera.add(borrar);
        }
        // Cabecera arriba y texto del comentario debajo.
        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(TextosUi.bloqueDeTexto(comentario.texto()), BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Publica el comentario escrito.
     */
    private void comentar() {
        // Limpia el error anterior, toma el texto y desactiva el botón mientras se guarda.
        TemaUi.mostrarError(error, null);
        String texto = cuadro.getText();
        botonComentar.setEnabled(false);
        // Guarda en segundo plano; si sale bien, vacía el cuadro y recarga la lista.
        TareaEnSegundoPlano.ejecutar(() -> servicios.comentarios().comentar(yo, publicacion.getId(), texto), creado -> {
            botonComentar.setEnabled(true);
            cuadro.setText("");
            refrescar();
        }, falla -> {
            botonComentar.setEnabled(true);
            if (falla instanceof ExcepcionReglaNegocio) {
                // Error del usuario (vacío, muy largo, ya no puede verla): en rojo bajo el cuadro.
                TemaUi.mostrarError(error, falla.getMessage());
            } else {
                AccionesUi.mostrarFallo(this, servicios.auditoria(), "COMENTAR", "No se pudo comentar", falla);
            }
        });
    }

    /**
     * Borra (con confirmación) un comentario.
     *
     * @param comentario comentario
     * @param boton      botón pulsado
     */
    private void borrar(Comentario comentario, JButton boton) {
        // Pide confirmación antes de borrar.
        if (!Dialogos.confirmar(this, "Borrar comentario", "¿Quieres borrar este comentario?", "Borrar")) {
            return;
        }
        // Borra en segundo plano y recarga la lista.
        boton.setEnabled(false);
        AccionesUi.ejecutar(this, servicios.auditoria(), "BORRAR_COMENTARIO", "No se pudo borrar", () -> {
            servicios.comentarios().borrar(yo, comentario.id());
            return Boolean.TRUE;
        }, hecho -> refrescar(), this::refrescar);
    }
}
