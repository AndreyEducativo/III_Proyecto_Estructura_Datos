/*
 * PanelMuro.java
 *
 * Qué hace: pestaña "Muro". Arriba, un cuadro para escribir y publicar un mensaje eligiendo su
 * privacidad ("Solo amigos" o "Pública"); debajo, un selector de vista: "Mis amigos" (mis
 * publicaciones y las de mis amigos) o "Muro general" (todas las públicas); y el muro, de lo más
 * reciente a lo más antiguo, con "me gusta", comentarios, compartir, editar y borrar.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de publicaciones. Las reglas (texto
 * vacío, largo máximo, privacidad, bloqueos, orden del muro) están en el servicio.
 * Principio SOLID: SRP (solo UI del muro; las tarjetas las arma TarjetasPublicacion) y DIP
 * (depende de interfaces a través de ServiciosSociales).
 * Colabora con: ServicioPublicaciones, TarjetasPublicacion, VistaPublicacion, ListaTarjetas,
 * TareaEnSegundoPlano, Dialogos, TemaUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.modelo.Usuario;
import una.social.publicaciones.EntradaMuro;
import una.social.publicaciones.Privacidad;
import una.social.publicaciones.Publicacion;
import una.social.ui.tema.AreaTextoTema;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña del muro (feed).
 */
final class PanelMuro extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Filas visibles del cuadro de texto. */
    private static final int FILAS_CUADRO = 3;

    /** Nombre de la acción de teclado Ctrl+Enter. */
    private static final String ACCION_PUBLICAR = "publicar";

    /** Texto normal del botón. */
    private static final String TEXTO_PUBLICAR = "Publicar";

    /** Mensaje de la vista "Mis amigos" vacía. */
    private static final String MURO_AMIGOS_VACIO = "Tu muro está vacío. Escribe tu primera publicación "
            + "o envía solicitudes de amistad en \"Buscar estudiantes\" para ver lo que comparten.";

    /** Mensaje del muro general vacío. */
    private static final String MURO_GENERAL_VACIO = "Todavía no hay publicaciones públicas.";

    /** Usuario dueño del muro. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Arma las tarjetas de las publicaciones. */
    private final transient TarjetasPublicacion tarjetas;

    /** Cuadro para escribir. */
    private final AreaTextoTema cuadroTexto;

    /** Privacidad de la publicación nueva. "Solo amigos" es la opción por defecto. */
    private final JComboBox<Privacidad> selectorPrivacidad = new JComboBox<>(new Privacidad[] {Privacidad.SOLO_AMIGOS, Privacidad.PUBLICA});

    /** Contador de caracteres "N/500". */
    private final JLabel contador = TextosUi.textoSecundario("0/" + Publicacion.LARGO_MAXIMO_TEXTO);

    /** Mensaje de error en línea bajo el cuadro. */
    private final JLabel error = TemaUi.crearEtiquetaError();

    /** Botón de publicar. */
    private final BotonTema botonPublicar = TemaUi.crearBotonPrimario(TEXTO_PUBLICAR);

    /** Vista "Mis amigos" (seleccionada al inicio). */
    private final JToggleButton vistaAmigos = new JToggleButton("Mis amigos", true);

    /** Vista "Muro general". */
    private final JToggleButton vistaGeneral = new JToggleButton("Muro general");

    /** Lista de publicaciones. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario     usuario dueño del muro
     * @param servicios   servicios
     * @param abrirPerfil acción para abrir el perfil de un usuario
     */
    PanelMuro(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.tarjetas = new TarjetasPublicacion(usuario, servicios, this, this::refrescar, abrirPerfil);
        this.cuadroTexto = new AreaTextoTema("¿Qué quieres compartir, " + primerNombre(usuario) + "?", FILAS_CUADRO);
        setOpaque(false);
        JPanel arriba = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        arriba.setOpaque(false);
        arriba.add(crearCompositor(), BorderLayout.NORTH);
        arriba.add(crearSelectorDeVista(), BorderLayout.SOUTH);
        add(arriba, BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
        configurarEventos();
    }

    /**
     * Arma la tarjeta para escribir una publicación.
     *
     * @return tarjeta del compositor
     */
    private JPanel crearCompositor() {
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        tarjeta.add(TemaUi.crearEtiqueta("Nueva publicación"), BorderLayout.NORTH);
        tarjeta.add(cuadroTexto, BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, TemaUi.ESPACIO * 2, 0));
        izquierda.setOpaque(false);
        izquierda.add(contador);
        izquierda.add(TextosUi.textoSecundario("Ctrl+Enter para publicar"));
        izquierda.add(error);
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO, 0));
        derecha.setOpaque(false);
        selectorPrivacidad.setFont(TemaUi.FUENTE_NORMAL);
        selectorPrivacidad.setToolTipText("Quién podrá ver la publicación");
        derecha.add(TextosUi.textoSecundario("Visible para:"));
        derecha.add(selectorPrivacidad);
        derecha.add(botonPublicar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(izquierda, BorderLayout.CENTER);
        pie.add(derecha, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);
        return conMargen(tarjeta);
    }

    /**
     * Arma el selector "Mis amigos" / "Muro general". Los dos botones están en un
     * {@link ButtonGroup}: elegir uno des-selecciona el otro (como botones de radio).
     *
     * @return fila del selector
     */
    private JPanel crearSelectorDeVista() {
        ButtonGroup grupo = new ButtonGroup();
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila.setOpaque(false);
        fila.add(TextosUi.textoSecundario("Ver:  "));
        for (JToggleButton boton : List.of(vistaAmigos, vistaGeneral)) {
            boton.setFont(TemaUi.FUENTE_NEGRITA);
            boton.setFocusPainted(true);
            grupo.add(boton);
            fila.add(boton);
            boton.addActionListener(e -> refrescar());
        }
        vistaAmigos.setToolTipText("Tus publicaciones y las de tus amigos (públicas y solo amigos)");
        vistaGeneral.setToolTipText("Todas las publicaciones públicas de la red");
        return conMargen(fila);
    }

    /**
     * @param componente componente a envolver
     * @return el componente con margen a los lados, para que no toque los bordes de la ventana
     */
    private static JPanel conMargen(JComponent componente) {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, 0, TemaUi.ESPACIO * 2));
        contenedor.add(componente);
        return contenedor;
    }

    /**
     * Conecta el botón, el contador de caracteres y el atajo Ctrl+Enter.
     */
    private void configurarEventos() {
        botonPublicar.addActionListener(e -> publicar());
        // En un área de texto ENTER hace un salto de línea, así que publicar va con Ctrl+Enter.
        cuadroTexto.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), ACCION_PUBLICAR);
        cuadroTexto.getActionMap().put(ACCION_PUBLICAR, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            /** Ctrl+Enter equivale a pulsar "Publicar" (si el botón está habilitado). */
            @Override
            public void actionPerformed(ActionEvent e) {
                if (botonPublicar.isEnabled()) {
                    publicar();
                }
            }
        });
        cuadroTexto.getDocument().addDocumentListener(new DocumentListener() {
            /** Texto agregado: actualizar contador. */
            @Override
            public void insertUpdate(DocumentEvent e) {
                actualizarContador();
            }

            /** Texto borrado: actualizar contador. */
            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarContador();
            }

            /** Cambio de formato (no ocurre en texto simple). */
            @Override
            public void changedUpdate(DocumentEvent e) {
                actualizarContador();
            }
        });
    }

    /**
     * Muestra "N/500" y lo pinta en rojo si se pasa del máximo. También borra el error anterior,
     * porque el usuario ya está corrigiendo.
     */
    private void actualizarContador() {
        int largo = cuadroTexto.getDocument().getLength();
        contador.setText(largo + "/" + Publicacion.LARGO_MAXIMO_TEXTO);
        contador.setForeground(largo > Publicacion.LARGO_MAXIMO_TEXTO ? TemaUi.ERROR : TemaUi.TEXTO_SECUNDARIO);
        TemaUi.mostrarError(error, null);
        TemaUi.marcarError(cuadroTexto, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Carga la vista elegida y prepara (en segundo plano) los contadores de cada publicación.</p>
     */
    @Override
    public void refrescar() {
        boolean general = vistaGeneral.isSelected();
        TareaEnSegundoPlano.ejecutar(() -> {
            List<EntradaMuro> entradas = general ? servicios.publicaciones().obtenerMuroGeneral(usuario)
                    : servicios.publicaciones().obtenerMuro(usuario);
            return VistaPublicacion.preparar(servicios, usuario, entradas);
        }, vistas -> lista.mostrar(tarjetas.crear(vistas), general ? MURO_GENERAL_VACIO : MURO_AMIGOS_VACIO),
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "CARGAR_MURO", falla));
    }

    /**
     * Publica el texto escrito en segundo plano (se escribe en disco).
     */
    private void publicar() {
        TemaUi.mostrarError(error, null);
        String texto = cuadroTexto.getText();
        Privacidad privacidad = (Privacidad) selectorPrivacidad.getSelectedItem();
        ponerOcupado(true);
        TareaEnSegundoPlano.ejecutar(() -> servicios.publicaciones().publicar(usuario, texto, privacidad), publicada -> {
            ponerOcupado(false);
            cuadroTexto.setText("");
            refrescar();
        }, this::mostrarFallo);
    }

    /**
     * Muestra el error de publicación. Corre en el hilo de Swing.
     *
     * @param falla excepción lanzada por el servicio
     */
    private void mostrarFallo(Throwable falla) {
        ponerOcupado(false);
        if (falla instanceof ExcepcionPublicacionInvalida) {
            // Error del usuario (vacío o muy largo): en rojo, junto al cuadro.
            TemaUi.mostrarError(error, falla.getMessage());
            TemaUi.marcarError(cuadroTexto, true);
            cuadroTexto.requestFocusInWindow();
        } else {
            AccionesUi.mostrarFallo(this, servicios.auditoria(), "PUBLICAR", "No se pudo publicar", falla);
        }
    }

    /**
     * Deshabilita los controles mientras se guarda, para evitar publicar dos veces.
     *
     * @param ocupado {@code true} mientras se publica
     */
    private void ponerOcupado(boolean ocupado) {
        cuadroTexto.setEnabled(!ocupado);
        botonPublicar.setEnabled(!ocupado);
        selectorPrivacidad.setEnabled(!ocupado);
        botonPublicar.setText(ocupado ? "Publicando…" : TEXTO_PUBLICAR);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /**
     * @param usuario usuario
     * @return primer nombre (para un saludo más cercano: "¿Qué quieres compartir, Ana?")
     */
    private static String primerNombre(Usuario usuario) {
        String nombre = usuario.getNombre().strip();
        int espacio = nombre.indexOf(' ');
        return espacio > 0 ? nombre.substring(0, espacio) : nombre;
    }
}
