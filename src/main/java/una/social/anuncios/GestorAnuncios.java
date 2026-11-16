/*
 * GestorAnuncios.java
 *
 * Qué hace: lógica de negocio de la sección pública de anuncios: publicar (título y texto), borrar
 * (solo el autor) y listar del más reciente al más antiguo, ocultando los de usuarios bloqueados.
 * Rol en la arquitectura: capa de SERVICIO del módulo de anuncios. La UI le habla por
 * ServicioAnuncios; Main lo registra como ModuloPersistente.
 * Principio SOLID: SRP (solo anuncios), OCP (hereda el esqueleto de GestorPersistenteBase) y DIP.
 * Colabora con: TablonAnuncios, RepositorioAnuncios, DirectorioUsuarios, ConsultaBloqueos.
 */
package una.social.anuncios;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import una.social.auditoria.RegistroAuditoria;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.persistencia.GestorPersistenteBase;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de anuncios.
 */
public final class GestorAnuncios extends GestorPersistenteBase implements ServicioAnuncios {

    /** Nombre del archivo, para la auditoría. */
    private static final String ARCHIVO = "anuncios.txt";

    /** Formato de un ID nuevo: "A" + al menos 4 dígitos. */
    private static final String FORMATO_ID = Anuncio.PREFIJO_ID + "%04d";

    /** Persistencia. */
    private final RepositorioAnuncios repositorio;

    /** Estructura en memoria. */
    private final TablonAnuncios tablon;

    /** Usuarios registrados. */
    private final DirectorioUsuarios directorio;

    /** Bloqueos (para ocultar anuncios de usuarios bloqueados). */
    private final ConsultaBloqueos bloqueos;

    /** Reloj para la fecha. */
    private final Clock reloj;

    /**
     * Crea el gestor (no lee el disco hasta {@code cargarDatos()}).
     *
     * @param repositorio persistencia
     * @param tablon      estructura vacía
     * @param directorio  usuarios registrados (deben cargarse ANTES)
     * @param bloqueos    consulta de bloqueos
     * @param auditoria   bitácora
     * @param reloj       reloj del sistema
     */
    public GestorAnuncios(RepositorioAnuncios repositorio, TablonAnuncios tablon, DirectorioUsuarios directorio,
                          ConsultaBloqueos bloqueos, RegistroAuditoria auditoria, Clock reloj) {
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.tablon = Objects.requireNonNull(tablon, "tablon");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== persistencia

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "anuncios";
    }

    /** {@inheritDoc} */
    @Override
    protected void cargarDesdeDisco() {
        List<Anuncio> leidos = repositorio.cargarTodos();
        tablon.limpiar();
        for (Anuncio anuncio : leidos) {
            if (directorio.buscarPorId(anuncio.idAutor()).isEmpty()) {
                auditarOmitido(ARCHIVO, "anuncio " + anuncio.id() + " omitido (el autor no existe)");
            } else if (tablon.buscar(anuncio.id()).isPresent()) {
                auditarOmitido(ARCHIVO, "anuncio " + anuncio.id() + " omitido (ID repetido)");
            } else {
                tablon.agregar(anuncio);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(tablon.todos());
    }

    /** {@inheritDoc} */
    @Override
    protected String resumen() {
        return tablon.tamano() + " anuncios";
    }

    // ================================================================== ServicioAnuncios

    /** {@inheritDoc} */
    @Override
    public Anuncio publicar(Usuario autor, String titulo, String texto) {
        Objects.requireNonNull(autor, "autor");
        return ejecutarAuditado(autor, "ANUNCIO_PUBLICADO", () -> crearYGuardar(autor.getId(), titulo, texto),
                a -> a.id() + " (" + a.texto().length() + " caracteres)");
    }

    /** {@inheritDoc} */
    @Override
    public void borrar(Usuario autor, String idAnuncio) {
        Objects.requireNonNull(autor, "autor");
        String id = idAnuncio == null ? "" : idAnuncio.trim();
        ejecutarAuditado(autor, "ANUNCIO_BORRADO", () -> quitarYGuardar(autor.getId(), id), Anuncio::id);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<EntradaAnuncio> listar(Usuario visitante) {
        String yo = visitante.getId();
        List<EntradaAnuncio> visibles = new ArrayList<>();
        for (Anuncio anuncio : tablon.deMasRecienteAMasAntiguo()) {
            if (!bloqueos.hayBloqueoEntre(yo, anuncio.idAutor())) {
                directorio.buscarPorId(anuncio.idAutor()).ifPresent(autor -> visibles.add(new EntradaAnuncio(anuncio, autor)));
            }
        }
        return Collections.unmodifiableList(visibles);
    }

    /**
     * Valida y guarda un anuncio nuevo (disco primero, memoria después).
     *
     * @param idAutor autor
     * @param titulo  título escrito
     * @param texto   texto escrito
     * @return anuncio creado
     */
    private synchronized Anuncio crearYGuardar(String idAutor, String titulo, String texto) {
        exigirDatosCargados();
        String tituloLimpio = limpiar(titulo, Anuncio.LARGO_MAXIMO_TITULO, "El título");
        if (tituloLimpio.contains("\n")) {
            throw new ExcepcionReglaNegocio("El título debe ocupar una sola línea.");
        }
        String textoLimpio = limpiar(texto, Anuncio.LARGO_MAXIMO_TEXTO, "El texto del anuncio");
        Anuncio nuevo = new Anuncio(siguienteId(), idAutor, Instant.now(reloj), tituloLimpio, textoLimpio);
        List<Anuncio> conNuevo = new ArrayList<>(tablon.todos());
        conNuevo.add(nuevo);
        repositorio.guardarTodos(conNuevo);
        tablon.agregar(nuevo);
        return nuevo;
    }

    /**
     * Borra un anuncio propio (disco primero, memoria después).
     *
     * @param idAutor   quien pide borrar
     * @param idAnuncio anuncio
     * @return anuncio borrado
     */
    private synchronized Anuncio quitarYGuardar(String idAutor, String idAnuncio) {
        exigirDatosCargados();
        Anuncio anuncio = tablon.buscar(idAnuncio).orElseThrow(() -> new ExcepcionReglaNegocio("El anuncio ya no existe."));
        if (!anuncio.idAutor().equals(idAutor)) {
            throw new ExcepcionReglaNegocio("Solo el autor puede borrar este anuncio.");
        }
        List<Anuncio> sinEl = new ArrayList<>(tablon.todos());
        sinEl.remove(anuncio);
        repositorio.guardarTodos(sinEl);
        tablon.quitar(idAnuncio);
        return anuncio;
    }

    /**
     * Limpia y valida un texto escrito por el usuario (excepción, no assert: es entrada externa).
     *
     * @param valor  texto escrito
     * @param maximo largo máximo
     * @param campo  nombre del campo para el mensaje ("El título"...)
     * @return texto limpio
     * @throws ExcepcionReglaNegocio si queda vacío o supera el máximo
     */
    private static String limpiar(String valor, int maximo, String campo) {
        String limpio = valor == null ? "" : valor.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (limpio.isEmpty()) {
            throw new ExcepcionReglaNegocio(campo + " es obligatorio.");
        }
        if (limpio.length() > maximo) {
            throw new ExcepcionReglaNegocio(campo + " puede tener como máximo " + maximo + " caracteres.");
        }
        return limpio;
    }

    /**
     * @return siguiente ID de anuncio (el más alto + 1)
     */
    private String siguienteId() {
        assert Thread.holdsLock(this) : "siguienteId debe llamarse con el candado del gestor";
        int maximo = 0;
        for (Anuncio anuncio : tablon.todos()) {
            Matcher m = Anuncio.PATRON_ID.matcher(anuncio.id());
            // Invariante: todo ID en memoria fue validado al leerlo o al crearlo.
            assert m.matches() : "ID inválido en memoria: " + anuncio.id();
            maximo = Math.max(maximo, Integer.parseInt(anuncio.id().substring(Anuncio.PREFIJO_ID.length())));
        }
        return String.format(FORMATO_ID, maximo + 1);
    }
}
