/*
 * GestorPublicaciones.java
 *
 * Qué hace: lógica de negocio de las publicaciones: cargar y guardar el historial y los
 * compartidos; publicar (con privacidad); armar el "Muro general", la vista "Mis amigos" y los
 * perfiles; editar la última publicación; deshacer con una PILA; borrar; compartir y quitar
 * compartidos. Respeta los bloqueos: nada de un usuario bloqueado aparece.
 * Rol en la arquitectura: capa de SERVICIO del módulo de publicaciones. La UI le habla por
 * ServicioPublicaciones; me gusta y comentarios por ConsultaPublicaciones; Main lo registra como
 * ModuloPersistente y le conecta los oyentes de borrado.
 * Principio SOLID: SRP (coordina; las listas están en HistorialPublicaciones y RegistroCompartidos y
 * el disco en los repositorios), ISP (depende de consultas pequeñas: ConsultaAmistades y
 * ConsultaBloqueos), OCP (patrón Observador para el borrado en cascada) y DIP (todo inyectado).
 * Colabora con: HistorialPublicaciones, RegistroCompartidos, RepositorioPublicaciones,
 * RepositorioCompartidos, DirectorioUsuarios, ConsultaAmistades, ConsultaBloqueos,
 * OyenteBorradoPublicacion, RegistroAuditoria.
 */
package una.social.publicaciones;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import una.social.amistades.ConsultaAmistades;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionPublicacionInvalida;
import una.social.modelo.Usuario;
import una.social.persistencia.ModuloPersistente;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de publicaciones y muros.
 *
 * <p><b>Hilos:</b> los métodos que tocan las listas son {@code synchronized} porque la UI los llama
 * desde SwingWorker y el cierre de la app desde otro hilo. A los oyentes se les avisa FUERA del
 * candado, para no bloquear a otros módulos mientras guardan.</p>
 */
public final class GestorPublicaciones implements ServicioPublicaciones, ModuloPersistente {

    /** Formato de un ID nuevo de publicación: "P" + al menos 4 dígitos (P0001...). */
    private static final String FORMATO_ID = Publicacion.PREFIJO_ID + "%04d";

    /** Formato de un ID nuevo de compartido: "CP" + al menos 4 dígitos (CP0001...). */
    private static final String FORMATO_ID_COMPARTIDO = Compartido.PREFIJO_ID + "%04d";

    /** Acción de auditoría cuando una operación no cumple una regla. */
    private static final String RECHAZADA = "PUBLICACION_RECHAZADA";

    /** Acción de auditoría cuando una operación no se pudo guardar. */
    private static final String FALLIDA = "PUBLICACION_FALLIDA";

    /** Persistencia de las publicaciones. */
    private final RepositorioPublicaciones repositorio;

    /** Persistencia de los compartidos. */
    private final RepositorioCompartidos repositorioCompartidos;

    /** Lista dinámica de publicaciones en memoria. */
    private final HistorialPublicaciones historial;

    /** Lista dinámica de compartidos en memoria. */
    private final RegistroCompartidos compartidos;

    /** Usuarios registrados (para validar autores y mostrar sus nombres). */
    private final DirectorioUsuarios directorio;

    /** Consulta de amistades (quién es amigo de quién). */
    private final ConsultaAmistades amistades;

    /** Consulta de bloqueos (para ocultar a los usuarios bloqueados). */
    private final ConsultaBloqueos bloqueos;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /** Reloj para las fechas (inyectable para las pruebas). */
    private final Clock reloj;

    /**
     * PILAS de "deshacer": ID de usuario → IDs de las publicaciones que hizo desde que se abrió la
     * aplicación. {@link ArrayDeque} usado como pila: {@code push} pone arriba y {@code pop} saca
     * la de arriba (LIFO: la última en entrar es la primera en salir).
     */
    private final Map<String, Deque<String>> pilasDeshacer = new HashMap<>();

    /** Oyentes del borrado de publicaciones (me gusta y comentarios). */
    private final List<OyenteBorradoPublicacion> oyentes = new CopyOnWriteArrayList<>();

    /**
     * Número más alto de publicación ya usado en esta ejecución. Evita REUTILIZAR el ID de una
     * publicación recién borrada: si se reutilizara, un compartido que apuntaba a la borrada
     * pasaría a mostrar otra publicación distinta.
     */
    private int ultimoNumeroUsado;

    /** {@code true} solo si la carga salió bien; si no, no se permite guardar. */
    private boolean datosCargados;

    /**
     * Crea el gestor (no lee el disco hasta {@link #cargarDatos()}).
     *
     * @param repositorio            persistencia de publicaciones
     * @param repositorioCompartidos persistencia de compartidos
     * @param historial              historial vacío
     * @param compartidos            registro de compartidos vacío
     * @param directorio             usuarios registrados (deben cargarse ANTES)
     * @param amistades              consulta de amistades
     * @param bloqueos               consulta de bloqueos
     * @param auditoria              bitácora
     * @param reloj                  reloj del sistema
     */
    public GestorPublicaciones(RepositorioPublicaciones repositorio, RepositorioCompartidos repositorioCompartidos,
                               HistorialPublicaciones historial, RegistroCompartidos compartidos,
                               DirectorioUsuarios directorio, ConsultaAmistades amistades, ConsultaBloqueos bloqueos,
                               RegistroAuditoria auditoria, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.repositorioCompartidos = Objects.requireNonNull(repositorioCompartidos, "repositorioCompartidos");
        this.historial = Objects.requireNonNull(historial, "historial");
        this.compartidos = Objects.requireNonNull(compartidos, "compartidos");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Registra un oyente del borrado de publicaciones (patrón Observador).
     *
     * @param oyente oyente a registrar
     */
    public void agregarOyente(OyenteBorradoPublicacion oyente) {
        oyentes.add(Objects.requireNonNull(oyente, "oyente"));
    }

    // ================================================================== ModuloPersistente

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "publicaciones";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Además del formato (repositorios), aquí se revisan las reglas: el autor debe existir y los
     * IDs no pueden repetirse. El historial se mantiene en orden cronológico aunque el archivo
     * viniera desordenado. Un compartido cuya publicación original ya no existe SÍ se conserva
     * (se muestra como "no disponible").</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si algún archivo no se puede leer: se marca la carga como
        // fallida (no se permitirá guardar), se audita y se relanza para avisar al usuario.
        try {
            List<Publicacion> leidas = repositorio.cargarTodas();
            List<Compartido> compartidosLeidos = repositorioCompartidos.cargarTodos();
            historial.limpiar();
            compartidos.limpiar();
            pilasDeshacer.clear();
            for (Publicacion publicacion : leidas) {
                agregarAlHistorialSiEsValida(publicacion);
            }
            for (Compartido compartido : compartidosLeidos) {
                agregarCompartidoSiEsValido(compartido);
            }
            ultimoNumeroUsado = 0;
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Historial de publicaciones en memoria: " + historial.tamano() + " publicaciones y "
                            + compartidos.tamano() + " compartidos");
        } catch (ExcepcionPersistencia ex) {
            datosCargados = false;
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "CARGA_FALLIDA",
                    "publicaciones: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Agrega una publicación leída del archivo solo si respeta las reglas.
     *
     * @param publicacion publicación leída
     */
    private void agregarAlHistorialSiEsValida(Publicacion publicacion) {
        String problema = null;
        if (directorio.buscarPorId(publicacion.getIdAutor()).isEmpty()) {
            problema = "el autor " + publicacion.getIdAutor() + " no existe";
        } else if (historial.contieneId(publicacion.getId())) {
            problema = "ID repetido";
        }
        if (problema != null) {
            auditarOmitida("publicaciones.txt: publicación " + publicacion.getId() + " omitida (" + problema + ")");
            return;
        }
        historial.agregar(publicacion);
    }

    /**
     * Agrega un compartido leído del archivo solo si respeta las reglas.
     *
     * @param compartido compartido leído
     */
    private void agregarCompartidoSiEsValido(Compartido compartido) {
        String problema = null;
        if (directorio.buscarPorId(compartido.idUsuario()).isEmpty()) {
            problema = "el usuario " + compartido.idUsuario() + " no existe";
        } else if (compartidos.buscar(compartido.id()).isPresent()) {
            problema = "ID repetido";
        } else if (compartidos.yaCompartio(compartido.idUsuario(), compartido.idPublicacion())) {
            problema = "el usuario ya había compartido esa publicación";
        }
        if (problema != null) {
            auditarOmitida("compartidos.txt: compartido " + compartido.id() + " omitido (" + problema + ")");
            return;
        }
        compartidos.agregar(compartido);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            repositorio.guardarTodas(historial.todas());
            repositorioCompartidos.guardarTodos(compartidos.todos());
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    historial.tamano() + " publicaciones y " + compartidos.tamano() + " compartidos guardados");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    "publicaciones: " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    // ================================================================== publicar / editar / deshacer / borrar

    /** {@inheritDoc} */
    @Override
    public Publicacion publicar(Usuario autor, String texto, Privacidad privacidad) {
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(privacidad, "privacidad");
        // Solo se audita el ID, el largo y la privacidad: el contenido no aporta a la auditoría.
        return auditado(autor, "PUBLICACION_CREADA", () -> crearYGuardar(autor.getId(), normalizarTexto(texto), privacidad),
                p -> p.getId() + " (" + p.getTexto().length() + " caracteres, " + p.getPrivacidad().getEtiqueta() + ")");
    }

    /** {@inheritDoc} */
    @Override
    public Publicacion editarUltima(Usuario autor, String textoNuevo) {
        Objects.requireNonNull(autor, "autor");
        return auditado(autor, "PUBLICACION_EDITADA", () -> editarYGuardar(autor.getId(), normalizarTexto(textoNuevo)),
                p -> p.getId() + " (" + p.getTexto().length() + " caracteres)");
    }

    /** {@inheritDoc} */
    @Override
    public Publicacion deshacerUltima(Usuario autor) {
        Objects.requireNonNull(autor, "autor");
        Publicacion deshecha = auditado(autor, "PUBLICACION_DESHECHA", () -> sacarDeLaPilaYBorrar(autor.getId()),
                Publicacion::getId);
        avisarBorrado(autor, deshecha.getId());
        return deshecha;
    }

    /** {@inheritDoc} */
    @Override
    public void borrar(Usuario autor, String idPublicacion) {
        Objects.requireNonNull(autor, "autor");
        String id = idPublicacion == null ? "" : idPublicacion.trim();
        auditado(autor, "PUBLICACION_BORRADA", () -> borrarPropia(autor.getId(), id), Publicacion::getId);
        avisarBorrado(autor, id);
    }

    /**
     * Ejecuta una operación y la audita: INFO si sale bien, WARN si no cumple una regla y ERROR si
     * no se pudo guardar. Así cada caso de uso no repite el mismo try/catch.
     *
     * @param actor       usuario que hace la operación
     * @param accionExito acción de auditoría si sale bien
     * @param operacion   operación (normalmente un método synchronized)
     * @param detalle     detalle de auditoría a partir del resultado
     * @param <T>         tipo del resultado
     * @return resultado de la operación
     */
    private <T> T auditado(Usuario actor, String accionExito, Supplier<T> operacion, Function<T, String> detalle) {
        String nombreActor = actor.getNombreUsuario();
        // Se esperan ExcepcionPublicacionInvalida (regla no cumplida: el usuario verá el mensaje) y
        // ExcepcionPersistencia (no se pudo guardar). Se auditan y se relanzan para que la UI
        // muestre el aviso en rojo o un diálogo de error.
        try {
            T resultado = operacion.get();
            auditoria.registrar(NivelAuditoria.INFO, nombreActor, accionExito, detalle.apply(resultado));
            return resultado;
        } catch (ExcepcionPublicacionInvalida ex) {
            auditoria.registrar(NivelAuditoria.WARN, nombreActor, RECHAZADA, accionExito + ": " + ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, nombreActor, FALLIDA, accionExito + ": " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Limpia el texto y valida su largo.
     *
     * <p>Los saltos de línea de Windows ({@code \r\n}) se unifican a {@code \n} para que el mismo
     * mensaje se guarde igual en cualquier sistema. Se usa EXCEPCIÓN (no assert) porque es texto
     * escrito por el usuario.</p>
     *
     * @param texto texto escrito
     * @return texto limpio
     * @throws ExcepcionPublicacionInvalida si queda vacío o supera el máximo
     */
    private static String normalizarTexto(String texto) {
        String limpio = texto == null ? "" : texto.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (limpio.isEmpty()) {
            throw new ExcepcionPublicacionInvalida("Escriba algo antes de publicar.");
        }
        if (limpio.length() > Publicacion.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionPublicacionInvalida("La publicación puede tener como máximo "
                    + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres (tiene " + limpio.length() + ").");
        }
        return limpio;
    }

    /**
     * Crea la publicación, la guarda primero en disco y luego en memoria, y la apila para poder
     * deshacerla.
     *
     * @param idAutor    ID del autor
     * @param texto      texto ya validado
     * @param privacidad privacidad elegida
     * @return publicación creada
     */
    private synchronized Publicacion crearYGuardar(String idAutor, String texto, Privacidad privacidad) {
        exigirDatosCargados();
        if (directorio.buscarPorId(idAutor).isEmpty()) {
            throw new ExcepcionPublicacionInvalida("El autor no está registrado.");
        }
        Publicacion nueva = new Publicacion(siguienteIdPublicacion(), idAutor, Instant.now(reloj), texto, privacidad, null);
        List<Publicacion> conNueva = new ArrayList<>(historial.todas());
        conNueva.add(nueva);
        repositorio.guardarTodas(conNueva);
        historial.agregar(nueva);
        // push: la nueva queda ARRIBA de la pila; será la primera en deshacerse.
        pilasDeshacer.computeIfAbsent(idAutor, id -> new ArrayDeque<>()).push(nueva.getId());
        return nueva;
    }

    /**
     * Edita la publicación más reciente del autor (disco primero, memoria después).
     *
     * @param idAutor   autor
     * @param textoNuevo texto ya validado
     * @return publicación editada
     */
    private synchronized Publicacion editarYGuardar(String idAutor, String textoNuevo) {
        exigirDatosCargados();
        Publicacion ultima = historial.ultimaDe(idAutor)
                .orElseThrow(() -> new ExcepcionPublicacionInvalida("No tienes publicaciones para editar."));
        Publicacion editada = ultima.conTextoEditado(textoNuevo, Instant.now(reloj));
        List<Publicacion> conEditada = new ArrayList<>(historial.todas());
        conEditada.set(conEditada.indexOf(ultima), editada);
        repositorio.guardarTodas(conEditada);
        historial.reemplazar(editada);
        return editada;
    }

    /**
     * Saca de la pila del autor su última publicación de esta sesión y la borra.
     *
     * @param idAutor autor
     * @return publicación borrada
     */
    private synchronized Publicacion sacarDeLaPilaYBorrar(String idAutor) {
        exigirDatosCargados();
        Deque<String> pila = pilaLimpia(idAutor);
        if (pila.isEmpty()) {
            throw new ExcepcionPublicacionInvalida("No hay publicaciones tuyas de esta sesión para deshacer.");
        }
        // peek mira la de ARRIBA sin sacarla; solo se saca (pop) si el borrado en disco salió bien.
        Publicacion borrada = eliminarYGuardar(pila.peek());
        pila.pop();
        return borrada;
    }

    /**
     * Devuelve la pila del autor quitando de arriba los IDs que ya no existen (por ejemplo, si la
     * publicación se borró con el botón "Borrar"). Así la cima siempre es una publicación real.
     *
     * @param idAutor autor
     * @return su pila (puede quedar vacía)
     */
    private Deque<String> pilaLimpia(String idAutor) {
        Deque<String> pila = pilasDeshacer.computeIfAbsent(idAutor, id -> new ArrayDeque<>());
        while (!pila.isEmpty() && !historial.contieneId(pila.peek())) {
            pila.pop();
        }
        return pila;
    }

    /**
     * Borra una publicación propia.
     *
     * @param idAutor       quien pide borrar
     * @param idPublicacion publicación a borrar
     * @return publicación borrada
     */
    private synchronized Publicacion borrarPropia(String idAutor, String idPublicacion) {
        exigirDatosCargados();
        Publicacion publicacion = historial.buscar(idPublicacion)
                .orElseThrow(() -> new ExcepcionPublicacionInvalida("La publicación ya no existe."));
        if (!publicacion.getIdAutor().equals(idAutor)) {
            throw new ExcepcionPublicacionInvalida("Solo puedes borrar tus propias publicaciones.");
        }
        return eliminarYGuardar(idPublicacion);
    }

    /**
     * Quita una publicación del disco y luego de la memoria.
     *
     * @param idPublicacion publicación a quitar (debe existir)
     * @return publicación quitada
     */
    private Publicacion eliminarYGuardar(String idPublicacion) {
        // Invariante: solo se llama con el candado tomado y con una publicación que existe.
        assert Thread.holdsLock(this) : "eliminarYGuardar debe llamarse con el candado del gestor";
        Publicacion publicacion = historial.buscar(idPublicacion).orElseThrow();
        List<Publicacion> sinElla = new ArrayList<>(historial.todas());
        sinElla.remove(publicacion);
        repositorio.guardarTodas(sinElla);
        historial.eliminar(idPublicacion);
        return publicacion;
    }

    /**
     * Avisa a los oyentes que se borró una publicación (borrado en cascada de me gusta y
     * comentarios). Se avisa a TODOS aunque uno falle; al final se informa el problema.
     *
     * @param actor         usuario que borró
     * @param idPublicacion publicación borrada
     * @throws ExcepcionPersistencia si algún oyente no pudo guardar su parte
     */
    private void avisarBorrado(Usuario actor, String idPublicacion) {
        ExcepcionPersistencia primerFallo = null;
        for (OyenteBorradoPublicacion oyente : oyentes) {
            // Se espera ExcepcionPersistencia si el oyente no pudo guardar. La publicación ya se
            // borró; se audita, se sigue con los demás oyentes y al final se informa al usuario.
            try {
                oyente.alBorrarPublicacion(idPublicacion);
            } catch (ExcepcionPersistencia ex) {
                auditoria.registrar(NivelAuditoria.ERROR, actor.getNombreUsuario(), "BORRADO_INCOMPLETO",
                        idPublicacion + ": " + RegistroAuditoria.describirError(ex));
                primerFallo = primerFallo == null ? ex : primerFallo;
            }
        }
        if (primerFallo != null) {
            throw new ExcepcionPersistencia("La publicación se borró, pero no se pudieron borrar todos sus "
                    + "me gusta o comentarios. Se reintentará al cerrar sesión.", primerFallo);
        }
    }

    // ================================================================== compartir

    /** {@inheritDoc} */
    @Override
    public Compartido compartir(Usuario usuario, String idPublicacion) {
        Objects.requireNonNull(usuario, "usuario");
        String id = idPublicacion == null ? "" : idPublicacion.trim();
        return auditado(usuario, "PUBLICACION_COMPARTIDA", () -> crearCompartido(usuario.getId(), id),
                c -> c.id() + " comparte " + c.idPublicacion());
    }

    /** {@inheritDoc} */
    @Override
    public void quitarCompartido(Usuario usuario, String idCompartido) {
        Objects.requireNonNull(usuario, "usuario");
        String id = idCompartido == null ? "" : idCompartido.trim();
        auditado(usuario, "COMPARTIDO_QUITADO", () -> quitarCompartidoPropio(usuario.getId(), id), Compartido::id);
    }

    /**
     * Valida y guarda un compartido nuevo (disco primero, memoria después).
     *
     * @param idUsuario     quien comparte
     * @param idPublicacion publicación original
     * @return compartido creado
     */
    private synchronized Compartido crearCompartido(String idUsuario, String idPublicacion) {
        exigirDatosCargados();
        Publicacion original = historial.buscar(idPublicacion)
                .orElseThrow(() -> new ExcepcionPublicacionInvalida("La publicación ya no existe."));
        if (original.getIdAutor().equals(idUsuario)) {
            throw new ExcepcionPublicacionInvalida("No puedes compartir tu propia publicación.");
        }
        if (!original.esPublica()) {
            throw new ExcepcionPublicacionInvalida("Solo se pueden compartir publicaciones públicas.");
        }
        if (bloqueos.hayBloqueoEntre(idUsuario, original.getIdAutor())) {
            throw new ExcepcionPublicacionInvalida("No puedes compartir esta publicación.");
        }
        if (compartidos.yaCompartio(idUsuario, idPublicacion)) {
            throw new ExcepcionPublicacionInvalida("Ya compartiste esta publicación.");
        }
        Compartido nuevo = new Compartido(siguienteIdCompartido(), idUsuario, idPublicacion, Instant.now(reloj));
        List<Compartido> conNuevo = new ArrayList<>(compartidos.todos());
        conNuevo.add(nuevo);
        repositorioCompartidos.guardarTodos(conNuevo);
        compartidos.agregar(nuevo);
        return nuevo;
    }

    /**
     * Quita un compartido propio (disco primero, memoria después).
     *
     * @param idUsuario    quien pide quitarlo
     * @param idCompartido compartido
     * @return compartido quitado
     */
    private synchronized Compartido quitarCompartidoPropio(String idUsuario, String idCompartido) {
        exigirDatosCargados();
        Compartido compartido = compartidos.buscar(idCompartido)
                .orElseThrow(() -> new ExcepcionPublicacionInvalida("Ese compartido ya no existe."));
        if (!compartido.idUsuario().equals(idUsuario)) {
            throw new ExcepcionPublicacionInvalida("Solo puedes quitar lo que tú compartiste.");
        }
        List<Compartido> sinEl = new ArrayList<>(compartidos.todos());
        sinEl.remove(compartido);
        repositorioCompartidos.guardarTodos(sinEl);
        compartidos.quitar(idCompartido);
        return compartido;
    }

    // ================================================================== consultas: muros y perfiles

    /** {@inheritDoc} */
    @Override
    public synchronized List<EntradaMuro> obtenerMuro(Usuario usuario) {
        String yo = usuario.getId();
        // Círculo = yo + mis amigos (vecinos en el grafo de amistades), sin nadie bloqueado.
        Set<String> circulo = new HashSet<>(amistades.idsAmigos(yo));
        circulo.add(yo);
        circulo.removeIf(id -> bloqueos.hayBloqueoEntre(yo, id));
        return armar(yo, p -> circulo.contains(p.getIdAutor()), c -> circulo.contains(c.idUsuario()));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<EntradaMuro> obtenerMuroGeneral(Usuario usuario) {
        String yo = usuario.getId();
        return armar(yo, p -> p.esPublica() && !bloqueos.hayBloqueoEntre(yo, p.getIdAutor()),
                c -> !bloqueos.hayBloqueoEntre(yo, c.idUsuario()));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<EntradaMuro> obtenerPerfil(Usuario visitante, String idDueno) {
        String yo = visitante.getId();
        if (bloqueos.hayBloqueoEntre(yo, idDueno)) {
            return List.of();
        }
        return armar(yo, p -> p.getIdAutor().equals(idDueno) && puedeVerPublicacion(yo, p),
                c -> c.idUsuario().equals(idDueno));
    }

    /**
     * Arma un muro uniendo publicaciones y compartidos, de lo más reciente a lo más antiguo.
     *
     * <p><b>Mezcla de dos listas ordenadas (como en Merge Sort):</b> las publicaciones y los
     * compartidos ya están cada uno ordenado de más reciente a más antiguo. Para unirlos no hace
     * falta ordenar todo de nuevo: se comparan los primeros de cada lista, se toma el más reciente
     * y se avanza en esa lista. Cuesta O(n + m).</p>
     *
     * @param yo                 usuario que mira
     * @param filtroPublicaciones qué publicaciones incluir
     * @param filtroCompartidos   qué compartidos incluir (además se exige que la original sea visible)
     * @return entradas del muro (lista no modificable)
     */
    private List<EntradaMuro> armar(String yo, Predicate<Publicacion> filtroPublicaciones,
                                    Predicate<Compartido> filtroCompartidos) {
        List<EntradaMuro> deP = new ArrayList<>();
        for (Publicacion publicacion : historial.filtrarDeMasRecienteAMasAntigua(filtroPublicaciones)) {
            directorio.buscarPorId(publicacion.getIdAutor()).ifPresent(autor -> deP.add(EntradaMuro.dePublicacion(publicacion, autor)));
        }
        List<EntradaMuro> deC = new ArrayList<>();
        List<Compartido> todos = compartidos.todos();
        for (int i = todos.size() - 1; i >= 0; i--) {
            Compartido compartido = todos.get(i);
            if (filtroCompartidos.test(compartido) && compartidoVisible(yo, compartido)) {
                entradaDeCompartido(compartido).ifPresent(deC::add);
            }
        }
        return Collections.unmodifiableList(mezclar(deP, deC));
    }

    /**
     * Mezcla dos listas ordenadas de más reciente a más antigua en una sola, también ordenada.
     *
     * @param a primera lista
     * @param b segunda lista
     * @return lista mezclada
     */
    private static List<EntradaMuro> mezclar(List<EntradaMuro> a, List<EntradaMuro> b) {
        List<EntradaMuro> resultado = new ArrayList<>(a.size() + b.size());
        int i = 0;
        int j = 0;
        while (i < a.size() && j < b.size()) {
            // Si empatan en fecha, va primero el compartido (b): se hizo después de publicar.
            if (a.get(i).fechaOrden().isAfter(b.get(j).fechaOrden())) {
                resultado.add(a.get(i++));
            } else {
                resultado.add(b.get(j++));
            }
        }
        resultado.addAll(a.subList(i, a.size()));
        resultado.addAll(b.subList(j, b.size()));
        return resultado;
    }

    /**
     * Arma la entrada de un compartido (con su original o como "no disponible").
     *
     * @param compartido compartido
     * @return la entrada, o vacío si quien compartió ya no existe
     */
    private Optional<EntradaMuro> entradaDeCompartido(Compartido compartido) {
        Optional<Usuario> quien = directorio.buscarPorId(compartido.idUsuario());
        if (quien.isEmpty()) {
            return Optional.empty();
        }
        Optional<Publicacion> original = historial.buscar(compartido.idPublicacion());
        Optional<Usuario> autor = original.flatMap(p -> directorio.buscarPorId(p.getIdAutor()));
        if (original.isEmpty() || autor.isEmpty()) {
            return Optional.of(new EntradaMuro(null, null, compartido, quien.get()));
        }
        return Optional.of(new EntradaMuro(original.get(), autor.get(), compartido, quien.get()));
    }

    /**
     * Un compartido se muestra si su original no existe (se verá como "no disponible") o si quien
     * mira puede ver la original (por ejemplo, NO si quien mira bloqueó al autor original).
     *
     * @param yo         usuario que mira
     * @param compartido compartido
     * @return {@code true} si se debe mostrar
     */
    private boolean compartidoVisible(String yo, Compartido compartido) {
        return historial.buscar(compartido.idPublicacion()).map(p -> puedeVerPublicacion(yo, p)).orElse(true);
    }

    /**
     * Regla de visibilidad de una publicación.
     *
     * @param yo          usuario que mira
     * @param publicacion publicación
     * @return {@code true} si no hay bloqueo y es pública, propia o de un amigo
     */
    private boolean puedeVerPublicacion(String yo, Publicacion publicacion) {
        String autor = publicacion.getIdAutor();
        if (bloqueos.hayBloqueoEntre(yo, autor)) {
            return false;
        }
        return publicacion.esPublica() || autor.equals(yo) || amistades.sonAmigos(yo, autor);
    }

    // ================================================================== ConsultaPublicaciones y otras consultas

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<String> autorDe(String idPublicacion) {
        return historial.buscar(idPublicacion).map(Publicacion::getIdAutor);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean puedeVer(String idUsuario, String idPublicacion) {
        return historial.buscar(idPublicacion).map(p -> puedeVerPublicacion(idUsuario, p)).orElse(false);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<Publicacion> ultimaPublicacionDe(String idAutor) {
        return historial.ultimaDe(idAutor);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean puedeDeshacer(Usuario autor) {
        return !pilaLimpia(autor.getId()).isEmpty();
    }

    // ================================================================== utilidades

    /**
     * Calcula el siguiente ID de publicación: el número más alto conocido + 1. "Conocido" incluye
     * las publicaciones actuales, las que referencian los compartidos (aunque se hayan borrado) y
     * las ya usadas en esta ejecución, para que un ID nunca se reutilice.
     *
     * @return nuevo ID (por ejemplo "P0007")
     */
    private String siguienteIdPublicacion() {
        // Invariante: se llama con el candado tomado, para que dos publicaciones simultáneas no
        // reciban el mismo número.
        assert Thread.holdsLock(this) : "siguienteIdPublicacion debe llamarse con el candado del gestor";
        int maximo = ultimoNumeroUsado;
        for (Publicacion publicacion : historial.todas()) {
            maximo = Math.max(maximo, numeroDe(publicacion.getId(), Publicacion.PATRON_ID, Publicacion.PREFIJO_ID));
        }
        for (Compartido compartido : compartidos.todos()) {
            maximo = Math.max(maximo, numeroDe(compartido.idPublicacion(), Publicacion.PATRON_ID, Publicacion.PREFIJO_ID));
        }
        ultimoNumeroUsado = maximo + 1;
        return String.format(FORMATO_ID, ultimoNumeroUsado);
    }

    /**
     * @return siguiente ID de compartido (el más alto + 1)
     */
    private String siguienteIdCompartido() {
        assert Thread.holdsLock(this) : "siguienteIdCompartido debe llamarse con el candado del gestor";
        int maximo = 0;
        for (Compartido compartido : compartidos.todos()) {
            maximo = Math.max(maximo, numeroDe(compartido.id(), Compartido.PATRON_ID, Compartido.PREFIJO_ID));
        }
        return String.format(FORMATO_ID_COMPARTIDO, maximo + 1);
    }

    /**
     * Extrae el número de un ID ("P0012" → 12).
     *
     * @param id      ID
     * @param patron  formato que debe cumplir
     * @param prefijo prefijo a quitar
     * @return el número
     */
    private static int numeroDe(String id, Pattern patron, String prefijo) {
        Matcher m = patron.matcher(id);
        // Invariante: todo ID en memoria fue validado con su patrón al leerlo o al crearlo.
        assert m.matches() : "ID con formato inválido en memoria: " + id;
        return Integer.parseInt(id.substring(prefijo.length()));
    }

    /**
     * Audita una línea omitida al cargar.
     *
     * @param detalle explicación
     */
    private void auditarOmitida(String detalle) {
        auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA", detalle);
    }

    /**
     * Impide guardar si la carga inicial falló.
     *
     * @throws ExcepcionPersistencia si los datos no se cargaron correctamente
     */
    private void exigirDatosCargados() {
        if (!datosCargados) {
            throw new ExcepcionPersistencia("Las publicaciones no se cargaron correctamente; no se guardará nada "
                    + "para no sobrescribir el archivo. Reinicie la aplicación.");
        }
    }
}
