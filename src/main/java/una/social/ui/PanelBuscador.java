/*
 * PanelBuscador.java
 *
 * Qué hace: pestaña "Buscar estudiantes". Permite buscar usuarios registrados por nombre, usuario
 * o ID y enviarles una solicitud de amistad (o cancelarla / responderla si ya hay una pendiente).
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo social. La búsqueda y las reglas de
 * amistad están en los servicios.
 * Principio SOLID: SRP (solo UI de búsqueda), ISP y DIP (depende de ServicioBusqueda y
 * ServicioAmistades, inyectados).
 * Colabora con: ServicioBusqueda, ServicioAmistades, FilaUsuario, ListaTarjetas,
 * AccionesAmistad, Dialogos.
 */
package una.social.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import una.social.amistades.ResultadoBusqueda;
import una.social.amistades.ServicioAmistades;
import una.social.amistades.ServicioBusqueda;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoTextoTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña del buscador de usuarios.
 */
final class PanelBuscador extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario que busca. */
    private final transient Usuario usuario;

    /** Servicio de búsqueda. */
    private final transient ServicioBusqueda busqueda;

    /** Servicio de amistades (para enviar, cancelar y responder solicitudes). */
    private final transient ServicioAmistades amistades;

    /** Bitácora (para errores inesperados). */
    private final transient RegistroAuditoria auditoria;

    /** Campo de búsqueda. */
    private final CampoTextoTema campoBusqueda = TemaUi.crearCampoTexto("Nombre, usuario o ID (ej. luis, Vargas, U003)");

    /** Botón de buscar. */
    private final BotonTema botonBuscar = TemaUi.crearBotonPrimario("Buscar");

    /** Texto con la cantidad de resultados. */
    private final JLabel resumen = TextosUi.textoSecundario("Deja el campo vacío y pulsa Buscar para ver a todos los estudiantes.");

    /** Lista de resultados. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario   usuario que busca
     * @param busqueda  servicio de búsqueda
     * @param amistades servicio de amistades
     * @param auditoria bitácora
     */
    PanelBuscador(Usuario usuario, ServicioBusqueda busqueda, ServicioAmistades amistades, RegistroAuditoria auditoria) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.busqueda = Objects.requireNonNull(busqueda, "busqueda");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        setOpaque(false);
        add(crearBarraBusqueda(), BorderLayout.NORTH);
        add(lista.conDesplazamiento(), BorderLayout.CENTER);
        // ENTER en el campo (evento "action" de JTextField) también busca.
        campoBusqueda.addActionListener(e -> refrescar());
        botonBuscar.addActionListener(e -> refrescar());
    }

    /**
     * Arma la tarjeta con el campo de búsqueda, el botón y el resumen.
     *
     * @return barra de búsqueda
     */
    private JPanel crearBarraBusqueda() {
        JPanel tarjeta = ListaTarjetas.crearTarjeta();
        tarjeta.add(TemaUi.crearEtiqueta("Buscar estudiantes registrados"), BorderLayout.NORTH);
        tarjeta.add(campoBusqueda, BorderLayout.CENTER);
        tarjeta.add(botonBuscar, BorderLayout.EAST);
        tarjeta.add(resumen, BorderLayout.SOUTH);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO * 2, TemaUi.ESPACIO * 2, 0, TemaUi.ESPACIO * 2));
        contenedor.add(tarjeta);
        return contenedor;
    }

    /**
     * Ejecuta la búsqueda con el texto actual (en segundo plano) y muestra los resultados.
     */
    @Override
    public void refrescar() {
        String texto = campoBusqueda.getText();
        botonBuscar.setEnabled(false);
        TareaEnSegundoPlano.ejecutar(() -> busqueda.buscar(texto, usuario), resultados -> {
            botonBuscar.setEnabled(true);
            mostrarResultados(texto, resultados);
        }, falla -> {
            botonBuscar.setEnabled(true);
            Dialogos.errorInesperado(this, auditoria, "BUSCAR_USUARIOS", falla);
        });
    }

    /**
     * Dibuja los resultados.
     *
     * @param texto      texto buscado
     * @param resultados resultados de la búsqueda
     */
    private void mostrarResultados(String texto, List<ResultadoBusqueda> resultados) {
        List<JComponent> filas = new ArrayList<>(resultados.size());
        for (ResultadoBusqueda resultado : resultados) {
            filas.add(FilaUsuario.crear(resultado.usuario(), FilaUsuario.detalleBasico(resultado.usuario()),
                    crearAccion(resultado)));
        }
        String cantidad = resultados.size() == 1 ? "1 estudiante encontrado" : resultados.size() + " estudiantes encontrados";
        resumen.setText(texto.isBlank() ? cantidad + " (todos los registrados)." : cantidad + ".");
        lista.mostrar(filas, "No se encontraron estudiantes con ese nombre, usuario o ID.");
    }

    /**
     * Elige qué mostrar a la derecha de cada resultado.
     *
     * @param resultado resultado de la búsqueda
     * @return etiqueta "Eres tú" / "Ya son amigos", o el botón que corresponda según haya o no
     *         una solicitud pendiente
     */
    private JComponent crearAccion(ResultadoBusqueda resultado) {
        Usuario otro = resultado.usuario();
        if (resultado.esElMismo()) {
            return TextosUi.textoSecundario("Eres tú");
        }
        if (resultado.esAmigo()) {
            return TextosUi.etiquetaPlana("Ya son amigos", TemaUi.FUENTE_NEGRITA, TemaUi.AZUL_UNA);
        }
        if (resultado.solicitudRecibida()) {
            BotonTema responder = TemaUi.crearBotonPrimario("Responder solicitud");
            responder.addActionListener(e -> AccionesAmistad.preguntarRespuesta(this, amistades, usuario, otro,
                    auditoria, this::refrescar));
            return responder;
        }
        if (resultado.solicitudEnviada()) {
            BotonTema cancelar = TemaUi.crearBotonSecundario("Cancelar solicitud");
            cancelar.addActionListener(e -> cancelarSolicitud(otro, cancelar));
            JPanel pendiente = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO, 0));
            pendiente.setOpaque(false);
            pendiente.add(TextosUi.textoSecundario("Solicitud enviada"));
            pendiente.add(cancelar);
            return pendiente;
        }
        BotonTema enviar = TemaUi.crearBotonSecundario("Enviar solicitud");
        enviar.addActionListener(e -> enviarSolicitud(otro, enviar));
        return enviar;
    }

    /**
     * "Enviar solicitud": la guarda en segundo plano; la otra persona decide si la acepta.
     *
     * @param otro  usuario al que se le envía
     * @param boton botón pulsado (se deshabilita para evitar doble clic)
     */
    private void enviarSolicitud(Usuario otro, BotonTema boton) {
        boton.setEnabled(false);
        AccionesAmistad.ejecutar(this, auditoria, "ENVIAR_SOLICITUD", "No se pudo enviar la solicitud",
                () -> amistades.enviarSolicitud(usuario, otro.getId()), enviada -> {
                    Dialogos.exito(this, "Solicitud enviada", "Se le envió una solicitud de amistad a " + otro.getNombre()
                            + ".\nCuando la acepte, sus publicaciones aparecerán en tu muro.");
                    refrescar();
                }, this::refrescar);
    }

    /**
     * "Cancelar solicitud": retira una solicitud enviada que aún no fue respondida.
     *
     * @param otro  usuario al que se le había enviado
     * @param boton botón pulsado (se deshabilita para evitar doble clic)
     */
    private void cancelarSolicitud(Usuario otro, BotonTema boton) {
        if (!Dialogos.confirmar(this, "Cancelar solicitud",
                "¿Quieres cancelar la solicitud de amistad que le enviaste a " + otro.getNombre() + "?", "Sí, cancelarla")) {
            return;
        }
        boton.setEnabled(false);
        AccionesAmistad.ejecutar(this, auditoria, "CANCELAR_SOLICITUD", "No se pudo cancelar", () -> {
            amistades.cancelarSolicitud(usuario, otro.getId());
            return Boolean.TRUE;
        }, hecho -> refrescar(), this::refrescar);
    }
}
