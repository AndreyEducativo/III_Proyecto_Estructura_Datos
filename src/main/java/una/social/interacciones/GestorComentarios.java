/*
 * GestorComentarios.java
 *
 * Qué hace: lógica de negocio de los comentarios: comentar (solo publicaciones visibles), borrar
 * (autor del comentario o de la publicación), listar y contar ocultando a los usuarios bloqueados,
 * y borrar en cascada los comentarios de una publicación borrada.
 * Rol en la arquitectura: capa de SERVICIO de los comentarios. La UI le habla por
 * ServicioComentarios; el módulo de publicaciones le avisa como OyenteBorradoPublicacion.
 * Principio SOLID: SRP (solo comentarios; memoria en RegistroComentarios y disco en el
 * repositorio), OCP (hereda el esqueleto de GestorPersistenteBase) y DIP (todo inyectado).
 * Colabora con: RegistroComentarios, RepositorioComentarios, ConsultaPublicaciones,
 * ConsultaBloqueos, DirectorioUsuarios, GestorPersistenteBase.
 */
package una.social.interacciones;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.persistencia.GestorPersistenteBase;
import una.social.publicaciones.ConsultaPublicaciones;
import una.social.publicaciones.OyenteBorradoPublicacion;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de comentarios.
 */
public final class GestorComentarios extends GestorPersistenteBase implements ServicioComentarios, OyenteBorradoPublicacion {

    /** Nombre del archivo, para la auditoría. */
    private static final String ARCHIVO = "comentarios.txt";

    /** Formato de un ID nuevo: "CM" + al menos 4 dígitos. */
    private static final String FORMATO_ID = Comentario.PREFIJO_ID + "%04d";

    /** Persistencia. */
    private final RepositorioComentarios repositorio;

    /** Estructura en memoria. */
    private final RegistroComentarios registro;

    /** Usuarios registrados. */
    private final DirectorioUsuarios directorio;

    /** Publicaciones (existencia, autor y visibilidad). */
    private final ConsultaPublicaciones publicaciones;

    /** Bloqueos (para ocultar comentarios de usuarios bloqueados). */
    private final ConsultaBloqueos bloqueos;

    /** Reloj para la fecha. */
    private final Clock reloj;

    /**
     * Crea el gestor (no lee el disco hasta {@code cargarDatos()}).
     *
     * @param repositorio   persistencia
     * @param registro      estructura vacía
     * @param directorio    usuarios registrados
     * @param publicaciones consulta de publicaciones (deben cargarse ANTES)
     * @param bloqueos      consulta de bloqueos
     * @param auditoria     bitácora
     * @param reloj         reloj del sistema
     */
    public GestorComentarios(RepositorioComentarios repositorio, RegistroComentarios registro, DirectorioUsuarios directorio,
                             ConsultaPublicaciones publicaciones, ConsultaBloqueos bloqueos,
                             RegistroAuditoria auditoria, Clock reloj) {
        // La auditoría la guarda la clase base; el resto de dependencias se guarda aquí.
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.publicaciones = Objects.requireNonNull(publicaciones, "publicaciones");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== persistencia

    /**
     * Nombre corto del módulo; aparece en la auditoría y en los avisos de carga.
     *
     * @return "comentarios"
     */
    @Override
    public String nombreModulo() {
        return "comentarios";
    }

    /**
     * Lee el archivo de datos y reconstruye la estructura en memoria. Lo llama
     * {@code cargarDatos()} de la clase base al abrir el programa. Las líneas que no
     * cumplen las reglas (autor inexistente, ID repetido...) se omiten y se auditan.
     */
    @Override
    protected void cargarDesdeDisco() {
        // Lee comentarios.txt y vacía el registro en memoria.
        List<Comentario> leidos = repositorio.cargarTodos();
        registro.limpiar();
        for (Comentario comentario : leidos) {
            // Cada comentario debe tener autor existente, publicación existente e ID único.
            String problema = null;
            if (directorio.buscarPorId(comentario.idAutor()).isEmpty()) {
                problema = "el autor no existe";
            } else if (publicaciones.autorDe(comentario.idPublicacion()).isEmpty()) {
                problema = "la publicación no existe";
            } else if (registro.buscar(comentario.id()).isPresent()) {
                problema = "ID repetido";
            }
            // Válido: entra al registro. Inválido: se omite y queda en la auditoría.
            if (problema == null) {
                registro.agregar(comentario);
            } else {
                auditarOmitido(ARCHIVO, "comentario " + comentario.id() + " omitido (" + problema + ")");
            }
        }
    }

    /**
     * Escribe en el archivo de datos todo lo que hay en memoria. Lo llama
     * {@code guardarDatos()} de la clase base al cerrar sesión o el programa.
     */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(registro.todos());
    }

    /**
     * Resumen corto de cuántos datos hay en memoria, para la auditoría.
     *
     * @return texto como "4 elementos"
     */
    @Override
    protected String resumen() {
        return registro.tamano() + " comentarios";
    }

    // ================================================================== ServicioComentarios

    /**
     * Ventana de comentarios: agrega un comentario a una publicación que el autor puede ver.
     *
     * @param autor         usuario con la sesión iniciada
     * @param idPublicacion publicación comentada
     * @param texto         texto del comentario
     * @return el comentario creado
     * @throws ExcepcionReglaNegocio si el texto está vacío o es muy largo, o no puede ver la publicación
     */
    @Override
    public Comentario comentar(Usuario autor, String idPublicacion, String texto) {
        Objects.requireNonNull(autor, "autor");
        // Limpia el ID recibido; el texto se valida con normalizar().
        String id = idPublicacion == null ? "" : idPublicacion.trim();
        // Solo se audita el ID y el largo: el contenido no aporta a la auditoría.
        return ejecutarAuditado(autor, "COMENTARIO_CREADO", () -> crearYGuardar(autor.getId(), id, normalizar(texto)),
                c -> c.id() + " en " + c.idPublicacion() + " (" + c.texto().length() + " caracteres)");
    }

    /**
     * Borra un comentario. Pueden hacerlo el autor del comentario o el autor de la publicación.
     *
     * @param usuario      usuario con la sesión iniciada
     * @param idComentario comentario a borrar
     * @throws ExcepcionReglaNegocio si no existe o el usuario no tiene permiso
     */
    @Override
    public void borrar(Usuario usuario, String idComentario) {
        Objects.requireNonNull(usuario, "usuario");
        String id = idComentario == null ? "" : idComentario.trim();
        // quitarYGuardar revisa el permiso y lo borra; ejecutarAuditado lo anota.
        ejecutarAuditado(usuario, "COMENTARIO_BORRADO", () -> quitarYGuardar(usuario.getId(), id),
                c -> c.id() + " de " + c.idPublicacion());
    }

    /**
     * Comentarios de una publicación que puede ver un usuario, del más antiguo al más reciente.
     *
     * @param visitante     usuario con la sesión iniciada
     * @param idPublicacion publicación consultada
     * @return comentarios visibles, cada uno marcado con si el visitante puede borrarlo
     */
    @Override
    public synchronized List<ComentarioVisible> listar(Usuario visitante, String idPublicacion) {
        String yo = visitante.getId();
        // Si no puede ver la publicación (privacidad o bloqueo), tampoco ve sus comentarios.
        if (!publicaciones.puedeVer(yo, idPublicacion)) {
            return List.of();
        }
        Optional<String> autorPublicacion = publicaciones.autorDe(idPublicacion);
        List<ComentarioVisible> visibles = new ArrayList<>();
        // Recorre los comentarios de esa publicación (HashMap publicación -> lista).
        for (Comentario comentario : registro.dePublicacion(idPublicacion)) {
            if (bloqueos.hayBloqueoEntre(yo, comentario.idAutor())) {
                continue; // Los comentarios de usuarios bloqueados (en cualquier sentido) no se ven.
            }
            // Puede borrarlo si es suyo o si la publicación es suya (para mostrar el botón "Borrar").
            boolean puedeBorrar = comentario.idAutor().equals(yo) || autorPublicacion.map(yo::equals).orElse(false);
            directorio.buscarPorId(comentario.idAutor())
                    .ifPresent(autor -> visibles.add(new ComentarioVisible(comentario, autor, puedeBorrar)));
        }
        return Collections.unmodifiableList(visibles);
    }

    /**
     * Cantidad de comentarios visibles de una publicación (el número del botón "Comentarios").
     *
     * @param visitante     usuario con la sesión iniciada
     * @param idPublicacion publicación consultada
     * @return cantidad de comentarios que ese usuario puede ver
     */
    @Override
    public synchronized int contar(Usuario visitante, String idPublicacion) {
        return listar(visitante, idPublicacion).size();
    }

    /**
     * Valida y guarda un comentario nuevo (disco primero, memoria después).
     *
     * @param idAutor       autor
     * @param idPublicacion publicación
     * @param texto         texto ya validado
     * @return comentario creado
     */
    private synchronized Comentario crearYGuardar(String idAutor, String idPublicacion, String texto) {
        exigirDatosCargados();
        // Solo se comenta lo que se puede ver.
        if (!publicaciones.puedeVer(idAutor, idPublicacion)) {
            throw new ExcepcionReglaNegocio("No puedes comentar esta publicación.");
        }
        // Crea el comentario; se guarda en comentarios.txt y después en memoria.
        Comentario nuevo = new Comentario(siguienteId(), idPublicacion, idAutor, Instant.now(reloj), texto);
        List<Comentario> conNuevo = new ArrayList<>(registro.todos());
        conNuevo.add(nuevo);
        repositorio.guardarTodos(conNuevo);
        registro.agregar(nuevo);
        return nuevo;
    }

    /**
     * Borra un comentario si el usuario tiene permiso (disco primero, memoria después).
     *
     * @param idUsuario    quien pide borrar
     * @param idComentario comentario
     * @return comentario borrado
     */
    private synchronized Comentario quitarYGuardar(String idUsuario, String idComentario) {
        exigirDatosCargados();
        // El comentario tiene que existir.
        Comentario comentario = registro.buscar(idComentario)
                .orElseThrow(() -> new ExcepcionReglaNegocio("El comentario ya no existe."));
        // Permiso: autor del comentario o autor de la publicación.
        boolean esAutorDelComentario = comentario.idAutor().equals(idUsuario);
        boolean esAutorDeLaPublicacion = publicaciones.autorDe(comentario.idPublicacion()).map(idUsuario::equals).orElse(false);
        if (!esAutorDelComentario && !esAutorDeLaPublicacion) {
            throw new ExcepcionReglaNegocio("Solo el autor del comentario o de la publicación puede borrarlo.");
        }
        // Reescribe comentarios.txt sin él y luego lo quita de la memoria.
        List<Comentario> sinEl = new ArrayList<>(registro.todos());
        sinEl.remove(comentario);
        repositorio.guardarTodos(sinEl);
        registro.quitar(idComentario);
        return comentario;
    }

    /**
     * Limpia y valida el texto (excepción, no assert: es texto escrito por el usuario).
     *
     * @param texto texto escrito
     * @return texto limpio
     * @throws ExcepcionReglaNegocio si queda vacío o supera el máximo
     */
    private static String normalizar(String texto) {
        // Unifica los saltos de línea y quita espacios al inicio y al final.
        String limpio = texto == null ? "" : texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        // No se permiten comentarios vacíos...
        if (limpio.isEmpty()) {
            throw new ExcepcionReglaNegocio("Escribe algo antes de comentar.");
        }
        // ...ni más largos que el máximo.
        if (limpio.length() > Comentario.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionReglaNegocio("El comentario puede tener como máximo " + Comentario.LARGO_MAXIMO_TEXTO
                    + " caracteres (tiene " + limpio.length() + ").");
        }
        return limpio;
    }

    /**
     * @return siguiente ID de comentario (el más alto + 1)
     */
    private String siguienteId() {
        // Invariante: se llama con el candado tomado (dos comentarios simultáneos no comparten ID).
        assert Thread.holdsLock(this) : "siguienteId debe llamarse con el candado del gestor";
        int maximo = 0;
        for (Comentario comentario : registro.todos()) {
            Matcher m = Comentario.PATRON_ID.matcher(comentario.id());
            // Invariante: todo ID en memoria fue validado al leerlo o al crearlo.
            assert m.matches() : "ID inválido en memoria: " + comentario.id();
            maximo = Math.max(maximo, Integer.parseInt(comentario.id().substring(Comentario.PREFIJO_ID.length())));
        }
        return String.format(FORMATO_ID, maximo + 1);
    }

    // ================================================================== OyenteBorradoPublicacion

    /**
     * {@inheritDoc}
     *
     * <p>Borrado en cascada de los comentarios de la publicación borrada (disco primero).</p>
     */
    @Override
    public synchronized void alBorrarPublicacion(String idPublicacion) {
        // Nada que hacer si el módulo no cargó o la publicación no tenía comentarios.
        if (!estanDatosCargados() || registro.dePublicacion(idPublicacion).isEmpty()) {
            return;
        }
        // Reescribe comentarios.txt sin los de esa publicación...
        List<Comentario> restantes = new ArrayList<>(registro.todos());
        restantes.removeIf(c -> c.idPublicacion().equals(idPublicacion));
        repositorio.guardarTodos(restantes);
        // ...los quita de la memoria y lo anota en la auditoría.
        int quitados = registro.quitarDePublicacion(idPublicacion);
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "COMENTARIOS_BORRADOS_EN_CASCADA",
                quitados + " de " + idPublicacion);
    }
}
