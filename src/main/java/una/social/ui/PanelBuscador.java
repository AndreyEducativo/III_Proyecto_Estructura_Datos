/*
 * PanelBuscador.java
 *
 * Qué hace: pestaña "Buscar estudiantes". Permite buscar usuarios registrados por nombre, usuario
 * o ID y agregarlos como amigos con un botón.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo social. La búsqueda y las reglas de
 * amistad están en los servicios.
 * Principio SOLID: SRP (solo UI de búsqueda), ISP y DIP (depende de ServicioBusqueda y
 * ServicioAmistades, inyectados).
 * Colabora con: ServicioBusqueda, ServicioAmistades, FilaUsuario, ListaTarjetas,
 * TareaEnSegundoPlano, Dialogos.
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
import una.social.amistades.ResultadoBusqueda;
import una.social.amistades.ServicioAmistades;
import una.social.amistades.ServicioBusqueda;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionAmistadInvalida;
import una.social.excepciones.ExcepcionSocialUna;
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

    /** Servicio de amistades (para "Agregar como amigo"). */
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
     * @return etiqueta "Eres tú", "Ya son amigos" o el botón "Agregar como amigo"
     */
    private JComponent crearAccion(ResultadoBusqueda resultado) {
        if (resultado.esElMismo()) {
            return TextosUi.textoSecundario("Eres tú");
        }
        if (resultado.esAmigo()) {
            return TextosUi.etiquetaPlana("Ya son amigos", TemaUi.FUENTE_NEGRITA, TemaUi.AZUL_UNA);
        }
        BotonTema boton = TemaUi.crearBotonSecundario("Agregar como amigo");
        boton.addActionListener(e -> agregarAmigo(resultado.usuario(), boton));
        return boton;
    }

    /**
     * "Agregar como amigo": guarda la amistad en segundo plano y vuelve a mostrar los resultados.
     *
     * @param amigo usuario a agregar
     * @param boton botón pulsado (se deshabilita para evitar doble clic)
     */
    private void agregarAmigo(Usuario amigo, BotonTema boton) {
        boton.setEnabled(false);
        TareaEnSegundoPlano.ejecutar(() -> amistades.agregarAmistad(usuario, amigo.getId()), creada -> {
            Dialogos.exito(this, "¡Nuevo amigo!", "Ahora tú y " + amigo.getNombre() + " son amigos.\n"
                    + "Sus publicaciones aparecerán en tu muro.");
            refrescar();
        }, falla -> {
            boton.setEnabled(true);
            if (falla instanceof ExcepcionAmistadInvalida) {
                Dialogos.advertencia(this, "No se pudo agregar", falla.getMessage());
                refrescar();
            } else if (falla instanceof ExcepcionSocialUna) {
                Dialogos.error(this, "No se pudo agregar", falla.getMessage());
            } else {
                Dialogos.errorInesperado(this, auditoria, "AGREGAR_AMIGO", falla);
            }
        });
    }
}
