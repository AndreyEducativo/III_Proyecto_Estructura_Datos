/*
 * DialogoBloqueados.java
 *
 * Qué hace: diálogo con la lista de usuarios que el usuario activo bloqueó, con el botón
 * "Desbloquear" en cada uno.
 * Rol en la arquitectura: capa de PRESENTACIÓN de los bloqueos (se abre desde "Mi perfil").
 * Principio SOLID: SRP (solo UI) y DIP (usa ServicioBloqueos por medio de ServiciosSociales).
 * Colabora con: ServicioBloqueos, FilaUsuario, ListaTarjetas, AccionesUi.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

/**
 * Diálogo de usuarios bloqueados.
 */
final class DialogoBloqueados extends JDialog {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Tamaño inicial. */
    private static final Dimension TAMANO = new Dimension(560, 420);

    /** Usuario activo. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Lista de bloqueados. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea el diálogo (sin mostrarlo).
     *
     * @param padre     componente dueño
     * @param usuario   usuario activo
     * @param servicios servicios
     */
    DialogoBloqueados(Component padre, Usuario usuario, ServiciosSociales servicios) {
        // Diálogo modal: bloquea la ventana principal hasta cerrarlo.
        super(SwingUtilities.getWindowAncestor(padre), "Usuarios bloqueados", ModalityType.APPLICATION_MODAL);
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        // Contenido: solo la lista de bloqueados con barra de desplazamiento.
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TemaUi.FONDO);
        raiz.add(lista.conDesplazamiento(), BorderLayout.CENTER);
        setContentPane(raiz);
        // Tamaño fijo, centrado sobre la ventana que lo abrió.
        setSize(TAMANO);
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(padre));
    }

    /**
     * Carga la lista y muestra el diálogo (modal).
     */
    void mostrar() {
        refrescar();
        setVisible(true);
    }

    /**
     * Vuelve a cargar los bloqueados (en segundo plano).
     */
    private void refrescar() {
        TareaEnSegundoPlano.ejecutar(() -> servicios.bloqueos().listarBloqueados(usuario.getId()), this::mostrarLista,
                falla -> Dialogos.errorInesperado(this, servicios.auditoria(), "LISTAR_BLOQUEADOS", falla));
    }

    /**
     * Dibuja la lista.
     *
     * @param bloqueados usuarios bloqueados
     */
    private void mostrarLista(List<Usuario> bloqueados) {
        // Una fila por usuario bloqueado, con su botón "Desbloquear".
        List<JComponent> filas = new ArrayList<>(bloqueados.size());
        for (Usuario bloqueado : bloqueados) {
            BotonTema desbloquear = TemaUi.crearBotonSecundario("Desbloquear");
            desbloquear.addActionListener(e -> desbloquear(bloqueado, desbloquear));
            filas.add(FilaUsuario.crear(bloqueado, FilaUsuario.detalleBasico(bloqueado), desbloquear));
        }
        lista.mostrar(filas, "No has bloqueado a nadie.");
    }

    /**
     * Desbloquea (con confirmación) a un usuario.
     *
     * @param bloqueado usuario a desbloquear
     * @param boton     botón pulsado
     */
    private void desbloquear(Usuario bloqueado, BotonTema boton) {
        if (!Dialogos.confirmar(this, "Desbloquear", "¿Quieres desbloquear a " + bloqueado.getNombre() + "?\n"
                + "Podrán volver a ver el contenido del otro. La amistad NO se restaura: tendrían que "
                + "enviarse una solicitud nueva.", "Desbloquear")) {
            return; // Eligió no desbloquear.
        }
        // Desbloquea en segundo plano y recarga la lista.
        boton.setEnabled(false);
        AccionesUi.ejecutar(this, servicios.auditoria(), "DESBLOQUEAR", "No se pudo desbloquear", () -> {
            servicios.bloqueos().desbloquear(usuario, bloqueado.getId());
            return Boolean.TRUE;
        }, hecho -> refrescar(), this::refrescar);
    }
}
