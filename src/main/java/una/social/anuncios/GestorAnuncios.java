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
        // La auditoría la guarda la clase base; el resto se guarda aquí (ninguna puede ser nula).
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.tablon = Objects.requireNonNull(tablon, "tablon");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== persistencia

    /**
     * Nombre corto del módulo; aparece en la auditoría y en los avisos de carga.
     *
     * @return "anuncios"
     */
    @Override
    public String nombreModulo() {
        return "anuncios";
    }

    /**
     * Lee el archivo de datos y reconstruye la estructura en memoria. Lo llama
     * {@code cargarDatos()} de la clase base al abrir el programa. Las líneas que no
     * cumplen las reglas (autor inexistente, ID repetido...) se omiten y se auditan.
     */
    @Override
    protected void cargarDesdeDisco() {
        // Lee anuncios.txt y vacía el tablón antes de llenarlo.
        List<Anuncio> leidos = repositorio.cargarTodos();
        tablon.limpiar();
        for (Anuncio anuncio : leidos) {
            // Se omite si el autor ya no existe o si el ID está repetido; si no, entra al tablón.
            if (directorio.buscarPorId(anuncio.idAutor()).isEmpty()) {
                auditarOmitido(ARCHIVO, "anuncio " + anuncio.id() + " omitido (el autor no existe)");
            } else if (tablon.buscar(anuncio.id()).isPresent()) {
                auditarOmitido(ARCHIVO, "anuncio " + anuncio.id() + " omitido (ID repetido)");
            } else {
                tablon.agregar(anuncio);
            }
        }
    }

    /**
     * Escribe en el archivo de datos todo lo que hay en memoria. Lo llama
     * {@code guardarDatos()} de la clase base al cerrar sesión o el programa.
     */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(tablon.todos());
    }

    /**
     * Resumen corto de cuántos datos hay en memoria, para la auditoría.
     *
     * @return texto como "4 elementos"
     */
    @Override
    protected String resumen() {
        return tablon.tamano() + " anuncios";
    }

    // ================================================================== ServicioAnuncios

    /**
     * Pestaña "Anuncios": publica un anuncio nuevo con título y texto.
     *
     * @param autor  usuario con la sesión iniciada
     * @param titulo título (una sola línea)
     * @param texto  contenido del anuncio
     * @return el anuncio creado
     * @throws ExcepcionReglaNegocio si el título o el texto están vacíos o son muy largos
     */
    @Override
    public Anuncio publicar(Usuario autor, String titulo, String texto) {
        Objects.requireNonNull(autor, "autor");
        // crearYGuardar valida y guarda; ejecutarAuditado anota el éxito o el error.
        return ejecutarAuditado(autor, "ANUNCIO_PUBLICADO", () -> crearYGuardar(autor.getId(), titulo, texto),
                a -> a.id() + " (" + a.texto().length() + " caracteres)");
    }

    /**
     * Borra un anuncio. Solo su autor puede hacerlo.
     *
     * @param autor     usuario con la sesión iniciada
     * @param idAnuncio ID del anuncio a borrar
     * @throws ExcepcionReglaNegocio si el anuncio no existe o no es del usuario
     */
    @Override
    public void borrar(Usuario autor, String idAnuncio) {
        Objects.requireNonNull(autor, "autor");
        // Limpia el ID recibido (nulo se vuelve "").
        String id = idAnuncio == null ? "" : idAnuncio.trim();
        // quitarYGuardar revisa que sea del autor y lo borra; ejecutarAuditado lo anota.
        ejecutarAuditado(autor, "ANUNCIO_BORRADO", () -> quitarYGuardar(autor.getId(), id), Anuncio::id);
    }

    /**
     * Anuncios que ve un usuario, del más reciente al más antiguo. Se ocultan los de usuarios
     * con los que hay un bloqueo.
     *
     * @param visitante usuario con la sesión iniciada
     * @return anuncios visibles con su autor (lista no modificable)
     */
    @Override
    public synchronized List<EntradaAnuncio> listar(Usuario visitante) {
        String yo = visitante.getId();
        List<EntradaAnuncio> visibles = new ArrayList<>();
        // Recorre el tablón desde el más nuevo.
        for (Anuncio anuncio : tablon.deMasRecienteAMasAntiguo()) {
            // Si hay bloqueo entre el visitante y el autor, el anuncio no se muestra.
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
        // Valida el título: no vacío, no muy largo y en una sola línea.
        String tituloLimpio = limpiar(titulo, Anuncio.LARGO_MAXIMO_TITULO, "El título");
        if (tituloLimpio.contains("\n")) {
            throw new ExcepcionReglaNegocio("El título debe ocupar una sola línea.");
        }
        // Valida el texto: no vacío y no muy largo.
        String textoLimpio = limpiar(texto, Anuncio.LARGO_MAXIMO_TEXTO, "El texto del anuncio");
        // Crea el anuncio con el siguiente ID libre y la fecha actual.
        Anuncio nuevo = new Anuncio(siguienteId(), idAutor, Instant.now(reloj), tituloLimpio, textoLimpio);
        // Guarda en anuncios.txt primero y, si salió bien, lo agrega al tablón en memoria.
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
        // El anuncio tiene que existir...
        Anuncio anuncio = tablon.buscar(idAnuncio).orElseThrow(() -> new ExcepcionReglaNegocio("El anuncio ya no existe."));
        // ...y ser de quien pide borrarlo.
        if (!anuncio.idAutor().equals(idAutor)) {
            throw new ExcepcionReglaNegocio("Solo el autor puede borrar este anuncio.");
        }
        // Reescribe anuncios.txt sin él y luego lo quita de la memoria.
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
        // Unifica los saltos de línea (Windows usa \r\n) y quita espacios al inicio y al final.
        String limpio = valor == null ? "" : valor.replace("\r\n", "\n").replace('\r', '\n').strip();
        // No puede quedar vacío...
        if (limpio.isEmpty()) {
            throw new ExcepcionReglaNegocio(campo + " es obligatorio.");
        }
        // ...ni pasarse del largo máximo.
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
