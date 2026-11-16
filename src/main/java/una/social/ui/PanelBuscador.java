/*
 * PanelBuscador.java
 *
 * Qué hace: pestaña "Buscar estudiantes". Muestra arriba "Personas que quizás conozcas" (amigos
 * de mis amigos, calculados con BFS) y debajo los resultados de buscar por nombre, usuario o ID.
 * Cada persona muestra el estado de la relación ("Enviar solicitud", "Solicitud enviada",
 * "Responder solicitud", "Amigos") y su nombre abre su perfil.
 * Rol en la arquitectura: capa de PRESENTACIÓN del módulo social. La búsqueda, las sugerencias y
 * las reglas de amistad están en los servicios.
 * Principio SOLID: SRP (solo UI de búsqueda) y DIP (depende de interfaces a través de
 * ServiciosSociales).
 * Colabora con: ServicioBusqueda, ServicioSugerencias, ServicioAmistades, AccionesAmistad,
 * EstadoRelacion, FilaUsuario, ListaTarjetas, TareaEnSegundoPlano, Dialogos.
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
import una.social.amistades.ResultadoBusqueda;
import una.social.amistades.Sugerencia;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.CampoTextoTema;
import una.social.ui.tema.TemaUi;

/**
 * Pestaña del buscador de usuarios y de sugerencias de amistad.
 */
final class PanelBuscador extends JPanel implements SeccionPrincipal {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Usuario que busca. */
    private final transient Usuario usuario;

    /** Servicios. */
    private final transient ServiciosSociales servicios;

    /** Abre el perfil de un usuario. */
    private final transient Consumer<Usuario> abrirPerfil;

    /** Campo de búsqueda. */
    private final CampoTextoTema campoBusqueda = TemaUi.crearCampoTexto("Nombre, usuario o ID (ej. luis, Vargas, U003)");

    /** Botón de buscar. */
    private final BotonTema botonBuscar = TemaUi.crearBotonPrimario("Buscar");

    /** Texto con la cantidad de resultados. */
    private final JLabel resumen = TextosUi.textoSecundario("Deja el campo vacío y pulsa Buscar para ver a todos los estudiantes.");

    /** Lista de sugerencias y resultados. */
    private final ListaTarjetas lista = new ListaTarjetas();

    /**
     * Resultado de una carga: sugerencias y resultados de búsqueda.
     *
     * @param sugerencias personas sugeridas
     * @param resultados  resultados de la búsqueda
     */
    private record Datos(List<Sugerencia> sugerencias, List<ResultadoBusqueda> resultados) {
    }

    /**
     * Crea la pestaña. Debe llamarse desde el hilo de Swing.
     *
     * @param usuario     usuario que busca
     * @param servicios   servicios
     * @param abrirPerfil acción para abrir el perfil de un usuario
     */
    PanelBuscador(Usuario usuario, ServiciosSociales servicios, Consumer<Usuario> abrirPerfil) {
        super(new BorderLayout(0, TemaUi.ESPACIO));
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.servicios = Objects.requireNonNull(servicios, "servicios");
        this.abrirPerfil = Objects.requireNonNull(abrirPerfil, "abrirPerfil");
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
     * Carga (en segundo plano) las sugerencias y los resultados de buscar el texto actual.
     */
    @Override
    public void refrescar() {
        String texto = campoBusqueda.getText();
        botonBuscar.setEnabled(false);
        TareaEnSegundoPlano.ejecutar(() -> new Datos(servicios.sugerencias().sugerir(usuario),
                servicios.busqueda().buscar(texto, usuario)), datos -> {
                    botonBuscar.setEnabled(true);
                    mostrar(texto, datos);
                }, falla -> {
                    botonBuscar.setEnabled(true);
                    Dialogos.errorInesperado(this, servicios.auditoria(), "BUSCAR_USUARIOS", falla);
                });
    }

    /**
     * Dibuja las sugerencias (si hay) y los resultados.
     *
     * @param texto texto buscado
     * @param datos sugerencias y resultados
     */
    private void mostrar(String texto, Datos datos) {
        List<JComponent> filas = new ArrayList<>();
        if (!datos.sugerencias().isEmpty()) {
            filas.add(titulo("Personas que quizás conozcas"));
            for (Sugerencia sugerencia : datos.sugerencias()) {
                Usuario otro = sugerencia.usuario();
                String comunes = sugerencia.amigosEnComun() == 1 ? "1 amigo en común" : sugerencia.amigosEnComun() + " amigos en común";
                filas.add(FilaUsuario.crear(otro, FilaUsuario.detalleBasico(otro) + "  ·  " + comunes,
                        accion(otro, EstadoRelacion.NINGUNA), abrirPerfil));
            }
            filas.add(titulo("Resultados de la búsqueda"));
        }
        for (ResultadoBusqueda resultado : datos.resultados()) {
            filas.add(FilaUsuario.crear(resultado.usuario(), FilaUsuario.detalleBasico(resultado.usuario()),
                    accion(resultado.usuario(), EstadoRelacion.de(resultado)), abrirPerfil));
        }
        int cantidad = datos.resultados().size();
        String encontrados = cantidad == 1 ? "1 estudiante encontrado" : cantidad + " estudiantes encontrados";
        resumen.setText(texto.isBlank() ? encontrados + " (todos los registrados)." : encontrados + ".");
        lista.mostrar(filas, "No se encontraron estudiantes con ese nombre, usuario o ID.");
    }

    /**
     * @param otro   otro usuario
     * @param estado relación con él
     * @return botón o etiqueta de estado de la amistad
     */
    private JComponent accion(Usuario otro, EstadoRelacion estado) {
        return AccionesAmistad.componenteEstado(this, servicios.amistades(), servicios.auditoria(), usuario, otro,
                estado, this::refrescar);
    }

    /**
     * @param texto título de sección
     * @return etiqueta de sección dentro de la lista
     */
    private static JComponent titulo(String texto) {
        JLabel etiqueta = TemaUi.crearEtiqueta(texto);
        etiqueta.setBorder(BorderFactory.createEmptyBorder(TemaUi.ESPACIO, TemaUi.ESPACIO, 0, 0));
        return etiqueta;
    }
}
