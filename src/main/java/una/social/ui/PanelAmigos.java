/*
 * PanelAmigos.java
 *
 * Qué hace: pestaña "Mis amigos". Muestra la lista de amigos del usuario activo, en orden
 * alfabético, con la fecha desde la que son amigos y un botón para eliminar a cada uno.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo de amistades.
 * Principio SOLID: SRP (solo UI de la lista) y DIP (depende de ServicioAmistades, inyectado).
 * Colabora con: ServicioAmistades, FilaUsuario, ListaTarjetas, TareaEnSegundoPlano, Dialogos.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.amistades.Amigo;
import una.social.amistades.ServicioAmistades;
import una.social.auditoria.RegistroAuditoria;
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

    /** Servicio de amistades. */
    private final transient ServicioAmistades amistades;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Título con la cantidad de amigos. */
    private final JLabel titulo = TemaUi.crearEtiqueta("Mis amigos");

    /** Lista de amigos. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario   usuario dueño de la lista
     * @param amistades servicio de amistades
     * @param auditoria bitácora
     */
    PanelAmigos(Usuario usuario, ServicioAmistades amistades, RegistroAuditoria auditoria) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        setOpaque(false);
        titulo.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 3, 0, 0));
        add(titulo, BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
    }

    /** {@inheritDoc} */
    @Override
    public void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> amistades.listarAmigos(usuario.getId()), this::mostrarAmigos,
                falla -> Dialogos.errorInesperado(this, auditoria, "LISTAR_AMIGOS", falla));
    }

    /**
     * Dibuja la lista de amigos.
     *
     * @param amigos amigos del usuario, en orden alfabético
     */
    private void mostrarAmigos(List<Amigo> amigos) {
        titulo.setText(amigos.size() == 1 ? "Tienes 1 amigo" : "Tienes " + amigos.size() + " amigos");
        List<JComponent> filas = new ArrayList<>(amigos.size());
        for (Amigo amigo : amigos) {
            filas.add(FilaUsuario.crear(amigo.usuario(), FilaUsuario.detalleBasico(amigo.usuario())
                    + "  ·  Amigos desde el " + TextosUi.fecha(amigo.amigosDesde()), crearBotonEliminar(amigo.usuario())));
        }
        lista.mostrar(filas, "Todavía no tienes amigos. Ve a \"Buscar estudiantes\" y pulsa \"Enviar solicitud\".");
    }

    /**
     * @param amigo amigo de la fila
     * @return botón "Eliminar amigo" que pide confirmación antes de eliminar
     */
    private BotonTema crearBotonEliminar(Usuario amigo) {
        BotonTema boton = TemaUi.crearBotonSecundario("Eliminar amigo");
        boton.addActionListener(e -> eliminarAmigo(amigo, boton));
        return boton;
    }

    /**
     * "Eliminar amigo": pide confirmación, elimina la amistad (para los dos) en segundo plano y
     * vuelve a mostrar la lista.
     *
     * @param amigo amigo a eliminar
     * @param boton botón pulsado (se deshabilita para evitar doble clic)
     */
    private void eliminarAmigo(Usuario amigo, BotonTema boton) {
        if (!Dialogos.confirmar(this, "Eliminar amigo", "¿Seguro que quieres eliminar a " + amigo.getNombre()
                + " de tus amigos?\nYa no verán las publicaciones del otro en el muro.", "Eliminar")) {
            return;
        }
        boton.setEnabled(false);
        AccionesAmistad.ejecutar(this, auditoria, "ELIMINAR_AMIGO", "No se pudo eliminar", () -> {
            amistades.eliminarAmistad(usuario, amigo.getId());
            return Boolean.TRUE;
        }, hecho -> {
            Dialogos.exito(this, "Amigo eliminado", amigo.getNombre() + " ya no está en tu lista de amigos.");
            refrescar();
        }, this::refrescar);
    }
}
