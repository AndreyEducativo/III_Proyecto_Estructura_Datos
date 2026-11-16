/*
 * PanelAmigos.java
 *
 * Qué hace: pestaña "Mis amigos". Muestra la lista de amigos del usuario activo, en orden
 * alfabético, con la fecha desde la que son amigos.
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
            filas.add(FilaUsuario.crear(amigo.usuario(), FilaUsuario.detalleBasico(amigo.usuario()),
                    TextosUi.textoSecundario("Amigos desde el " + TextosUi.fecha(amigo.amigosDesde()))));
        }
        lista.mostrar(filas, "Todavía no tienes amigos. Ve a \"Buscar estudiantes\" y pulsa \"Agregar como amigo\".");
    }
}
