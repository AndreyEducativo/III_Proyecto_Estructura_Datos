/*
 * PanelAnuncios.java
 *
 * Qué hace: pestaña "Anuncios": sección PÚBLICA donde cualquier usuario publica un anuncio (título
 * y texto) que ven todos, del más reciente al más antiguo. El autor puede borrar los suyos.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de anuncios.
 * Principio SOLID: SRP (solo UI de anuncios) y DIP (depende de ServicioAnuncios por medio de
 * ServiciosSociales).
 * Colabora con: ServicioAnuncios, ListaTarjetas, AccionesUi, TextosUi, TemaUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.anuncios.Anuncio;
import una.social.anuncios.EntradaAnuncio;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.ui.tema.AreaTextoTema;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoTextoTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña de anuncios públicos.
 */
final class PanelAnuncios extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Filas visibles del texto del anuncio. */
    private static final int FILAS_TEXTO = 2;

    /** Usuario activo. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de un usuario. */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Campo del título. */
    private final CampoTextoTema campoTitulo = TemaUi.crearCampoTexto("Título del anuncio (máx. " + Anuncio.LARGO_MAXIMO_TITULO + ")");

    /** Texto del anuncio. */
    private final AreaTextoTema cuadroTexto = new AreaTextoTema("¿Qué quieres anunciar a toda la comunidad?", FILAS_TEXTO);

    /** Error en línea. */
    private final JLabel error = TemaUi.crearEtiquetaError();

    /** Botón de publicar. */
    private final BotonTema botonPublicar = TemaUi.crearBotonPrimario("Publicar anuncio");

    /** Lista de anuncios. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario     usuario activo
     * @param servicios   servicios
     * @param abrirPerfil acción para abrir el perfil de un usuario
     */
    PanelAnuncios(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
        setOpaque(false);
        add(crearCompositor(), BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
        botonPublicar.addActionListener(e -> publicar());
    }

    /**
     * Arma la tarjeta para escribir un anuncio.
     *
     * @return compositor
     */
    private JPanel crearCompositor() {
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        tarjeta.add(TemaUi.crearEtiqueta("Nuevo anuncio (público: lo ven todos)"), BorderLayout.NORTH);
        JPanel campos = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        campos.setOpaque(false);
        campos.add(campoTitulo, BorderLayout.NORTH);
        campos.add(cuadroTexto, BorderLayout.CENTER);
        tarjeta.add(campos, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(error, BorderLayout.CENTER);
        pie.add(botonPublicar, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, 0, TemaUi.ESPACIO * 2));
        contenedor.add(tarjeta);
        return contenedor;
    }

    /** {@inheritDoc} */
    @Override
    public void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> servicios.anuncios().listar(usuario), this::mostrar,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "LISTAR_ANUNCIOS", falla));
    }

    /**
     * Dibuja los anuncios.
     *
     * @param anuncios anuncios del más reciente al más antiguo
     */
    private void mostrar(List<EntradaAnuncio> anuncios) {
        List<JComponent> tarjetas = new ArrayList<>(anuncios.size());
        for (EntradaAnuncio entrada : anuncios) {
            tarjetas.add(crearTarjeta(entrada));
        }
        lista.mostrar(tarjetas, "Todavía no hay anuncios. ¡Publica el primero!");
    }

    /**
     * Tarjeta de un anuncio: título, autor (enlace), fecha, texto y "Borrar" si es mío.
     *
     * @param entrada anuncio con su autor
     * @return tarjeta
     */
    private JPanel crearTarjeta(EntradaAnuncio entrada) {
        Anuncio anuncio = entrada.anuncio();
        Usuario autor = entrada.autor();
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(TextosUi.etiquetaPlana(anuncio.titulo(), TemaUi.FUENTE_NEGRITA, TemaUi.ROJO_UNA), BorderLayout.NORTH);
        JPanel linea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linea.setOpaque(false);
        linea.add(TextosUi.textoSecundario("Por "));
        linea.add(TextosUi.enlace(autor.getNombre(), TemaUi.FUENTE_PEQUENA, TemaUi.TEXTO, () -> abrirPerfil.accept(autor)));
        linea.add(TextosUi.textoSecundario("  ·  " + TextosUi.fechaHora(anuncio.fecha())));
        if (autor.getId().equals(usuario.getId())) {
            JButton borrar = TextosUi.botonEnlace("Borrar");
            borrar.setForeground(TemaUi.ERROR);
            borrar.addActionListener(e -> borrar(anuncio, borrar));
            linea.add(borrar);
        }
        cabecera.add(linea, BorderLayout.SOUTH);
        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(TextosUi.bloqueDeTexto(anuncio.texto()), BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Publica el anuncio escrito.
     */
    private void publicar() {
        TemaUi.mostrarError(error, null);
        String titulo = campoTitulo.getText();
        String texto = cuadroTexto.getText();
        botonPublicar.setEnabled(false);
        TareaEnSegundoPlano.ejecutar(() -> servicios.anuncios().publicar(usuario, titulo, texto), creado -> {
            botonPublicar.setEnabled(true);
            campoTitulo.setText("");
            cuadroTexto.setText("");
            refrescar();
        }, falla -> {
            botonPublicar.setEnabled(true);
            if (falla instanceof ExcepcionReglaNegocio) {
                // Error del usuario (título o texto vacío o largo): en rojo, en la tarjeta.
                TemaUi.mostrarError(error, falla.getMessage());
            } else {
                AccionesUi.mostrarFallo(this, servicios.auditoria(), "PUBLICAR_ANUNCIO", "No se pudo publicar", falla);
            }
        });
    }

    /**
     * Borra (con confirmación) un anuncio propio.
     *
     * @param anuncio anuncio
     * @param boton   botón pulsado
     */
    private void borrar(Anuncio anuncio, JButton boton) {
        if (!Dialogos.confirmar(this, "Borrar anuncio", "¿Quieres borrar el anuncio \"" + anuncio.titulo() + "\"?", "Borrar")) {
            return;
        }
        boton.setEnabled(false);
        AccionesUi.ejecutar(this, servicios.auditoria(), "BORRAR_ANUNCIO", "No se pudo borrar", () -> {
            servicios.anuncios().borrar(usuario, anuncio.id());
            return Boolean.TRUE;
        }, hecho -> refrescar(), this::refrescar);
    }
}
