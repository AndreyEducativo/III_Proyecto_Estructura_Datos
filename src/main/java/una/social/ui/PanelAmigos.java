/*
 * PanelAmigos.java
 *
 * Qué hace: pestaña "Mis amigos". Muestra la lista de amigos del usuario activo, en orden
 * alfabético, con la fecha desde la que son amigos, un botón para eliminar a cada uno y su nombre
 * como enlace a su perfil.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de amistades.
 * Principio SOLID: SRP (solo UI de la lista) y DIP (depende de interfaces a través de
 * ServiciosSociales).
 * Colabora con: ServicioAmistades, AccionesAmistad, FilaUsuario, ListaTarjetas, TareaEnSegundoPlano.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.amistades.Amigo;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña con la lista de amigos.
 */
final class PanelAmigos extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario dueño de la lista. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de un usuario. */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Título con la cantidad de amigos. */
    private final JLabel titulo = TemaUi.crearEtiqueta("Mis amigos");

    /** Lista de amigos. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario     usuario dueño de la lista
     * @param servicios   servicios
     * @param abrirPerfil acción para abrir el perfil de un usuario
     */
    PanelAmigos(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
        setOpaque(false);
        // Arriba: título "Tienes N amigos". Centro: la lista con barra de desplazamiento.
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
        // Pide los amigos en segundo plano (recorre el grafo) y luego los dibuja.
        TareaEnSegundoPlano.ejecutar(() -> servicios.amistades().listarAmigos(usuario.getId()), this::mostrarAmigos,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "LISTAR_AMIGOS", falla));
    }

    /**
     * Dibuja la lista de amigos.
     *
     * @param amigos amigos del usuario, en orden alfabético
     */
    private void mostrarAmigos(List<Amigo> amigos) {
        // Título en singular o plural.
        titulo.setText(amigos.size() == 1 ? "Tienes 1 amigo" : "Tienes " + amigos.size() + " amigos");
        List<JComponent> filas = new ArrayList<>(amigos.size());
        // Una fila por amigo: avatar, nombre, "Amigos desde..." y botón "Eliminar amigo".
        for (Amigo amigo : amigos) {
            BotonTema eliminar = TemaUi.crearBotonSecundario("Eliminar amigo");
            eliminar.addActionListener(e -> AccionesAmistad.eliminarAmigo(this, servicios.amistades(), usuario,
                    amigo.usuario(), servicios.auditoria(), eliminar, this::refrescar));
            filas.add(FilaUsuario.crear(amigo.usuario(), FilaUsuario.detalleBasico(amigo.usuario())
                    + "  ·  Amigos desde el " + TextosUi.fecha(amigo.amigosDesde()), eliminar, abrirPerfil));
        }
        // Muestra las filas, o el mensaje de lista vacía si no hay amigos.
        lista.mostrar(filas, "Todavía no tienes amigos. Ve a \"Buscar estudiantes\" y pulsa \"Enviar solicitud\".");
    }
}
