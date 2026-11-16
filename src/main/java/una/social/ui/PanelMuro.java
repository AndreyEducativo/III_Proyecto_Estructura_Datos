/*
 * PanelMuro.java
 *
 * Qué hace: pestaña "Muro". Arriba, un cuadro para escribir y publicar un mensaje; abajo, el muro
 * con las publicaciones del usuario y de sus amigos, de la más reciente a la más antigua.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de publicaciones. Las reglas (texto
 * vacío, largo máximo, orden del muro) están en el servicio; aquí solo se muestran.
 * Principio SOLID: SRP (solo UI del muro) y DIP (depende de ServicioPublicaciones, inyectado).
 * Colabora con: ServicioPublicaciones, ListaTarjetas, TareaEnSegundoPlano, Dialogos, TemaUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.excepciones.ExcepcionSocialUna;
import una.social.modelo.Usuario;
import una.social.publicaciones.EntradaMuro;
import una.social.publicaciones.Publicacion;
import una.social.publicaciones.ServicioPublicaciones;
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

    /** Diámetro del avatar en cada publicación. */
    private static final int DIAMETRO_AVATAR = 40;

    /** Nombre de la acción de teclado Ctrl+Enter. */
    private static final String ACCION_PUBLICAR = "publicar";

    /** Texto normal del botón. */
    private static final String TEXTO_PUBLICAR = "Publicar";

    /** Mensaje del muro vacío. */
    private static final String MURO_VACIO = "Tu muro está vacío. Escribe tu primera publicación "
            + "o agrega amigos en \"Buscar estudiantes\" para ver lo que comparten.";

    /** Usuario dueño del muro. */
    private final transient Usuario usuario;

    /** Servicio de publicaciones. */
    private final transient ServicioPublicaciones servicio;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Cuadro para escribir. */
    private final AreaTextoTema cuadroTexto;

    /** Contador de caracteres "N/500". */
    private final JLabel contador = TextosUi.textoSecundario("0/" + Publicacion.LARGO_MAXIMO_TEXTO);

    /** Mensaje de error en línea bajo el cuadro. */
    private final JLabel error = TemaUi.crearEtiquetaError();

    /** Botón de publicar. */
    private final BotonTema botonPublicar = TemaUi.crearBotonPrimario(TEXTO_PUBLICAR);

    /** Botón para recargar el muro. */
    private final BotonTema botonActualizar = TemaUi.crearBotonSecundario("Actualizar muro");

    /** Lista de publicaciones. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario   usuario dueño del muro
     * @param servicio  servicio de publicaciones
     * @param auditoria bitácora
     */
    PanelMuro(Usuario usuario, ServicioPublicaciones servicio, RegistroAuditoria auditoria) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicio = Objects.requireNonNull(servicio, "servicio");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.cuadroTexto = new AreaTextoTema("¿Qué quieres compartir, " + primerNombre(usuario) + "?", FILAS_CUADRO);
        setOpaque(false);
        add(crearCompositor(), BorderLayout.NORTH);
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
        derecha.add(botonActualizar);
        derecha.add(botonPublicar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(izquierda, BorderLayout.CENTER);
        pie.add(derecha, BorderLayout.EAST);
        tarjeta.add(pie, BorderLayout.SOUTH);

        // Margen exterior para que la tarjeta no toque los bordes de la ventana.
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, 0, TemaUi.ESPACIO * 2));
        contenedor.add(tarjeta);
        return contenedor;
    }

    /**
     * Conecta botones, contador de caracteres y el atajo Ctrl+Enter.
     */
    private void configurarEventos() {
        botonPublicar.addActionListener(e -> publicar());
        botonActualizar.addActionListener(e -> refrescar());
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

    /** {@inheritDoc} */
    @Override
    public void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> servicio.obtenerMuro(usuario), this::mostrarMuro,
                falla -> Dialogos.errorInesperado(this, auditoria, "CARGAR_MURO", falla));
    }

    /**
     * Publica el texto escrito en segundo plano (se escribe en disco).
     */
    private void publicar() {
        TemaUi.mostrarError(error, null);
        String texto = cuadroTexto.getText();
        ponerOcupado(true);
        TareaEnSegundoPlano.ejecutar(() -> servicio.publicar(usuario, texto), publicada -> {
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
        } else if (falla instanceof ExcepcionSocialUna) {
            Dialogos.error(this, "No se pudo publicar", falla.getMessage());
        } else {
            Dialogos.errorInesperado(this, auditoria, "PUBLICAR", falla);
        }
    }

    /**
     * Dibuja las publicaciones del muro.
     *
     * @param entradas entradas del muro (de la más reciente a la más antigua)
     */
    private void mostrarMuro(List<EntradaMuro> entradas) {
        List<JComponent> tarjetas = new ArrayList<>(entradas.size());
        for (EntradaMuro entrada : entradas) {
            tarjetas.add(crearTarjetaPublicacion(entrada));
        }
        lista.mostrar(tarjetas, MURO_VACIO);
    }

    /**
     * Arma la tarjeta de una publicación: avatar, nombre, "@usuario · fecha" y el texto.
     *
     * @param entrada publicación con su autor
     * @return tarjeta
     */
    private JPanel crearTarjetaPublicacion(EntradaMuro entrada) {
        Usuario autor = entrada.autor();
        Publicacion publicacion = entrada.publicacion();
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        JPanel avatar = new JPanel(new BorderLayout());
        avatar.setOpaque(false);
        avatar.add(new AvatarUsuario(autor, DIAMETRO_AVATAR), BorderLayout.NORTH);
        tarjeta.add(avatar, BorderLayout.WEST);

        String nombre = autor.getId().equals(usuario.getId()) ? autor.getNombre() + " (tú)" : autor.getNombre();
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.add(TextosUi.etiquetaPlana(nombre, TemaUi.FUENTE_NEGRITA, TemaUi.TEXTO), BorderLayout.NORTH);
        cabecera.add(TextosUi.etiquetaPlana("@" + autor.getNombreUsuario() + "  ·  " + TextosUi.fechaHora(publicacion.getFecha()),
                TemaUi.FUENTE_PEQUENA, TemaUi.TEXTO_SECUNDARIO), BorderLayout.SOUTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, TemaUi.ESPACIO));
        cuerpo.setOpaque(false);
        cuerpo.add(cabecera, BorderLayout.NORTH);
        cuerpo.add(TextosUi.bloqueDeTexto(publicacion.getTexto()), BorderLayout.CENTER);
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Deshabilita los controles mientras se guarda, para evitar publicar dos veces.
     *
     * @param ocupado {@code true} mientras se publica
     */
    private void ponerOcupado(boolean ocupado) {
        cuadroTexto.setEnabled(!ocupado);
        botonPublicar.setEnabled(!ocupado);
        botonActualizar.setEnabled(!ocupado);
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
