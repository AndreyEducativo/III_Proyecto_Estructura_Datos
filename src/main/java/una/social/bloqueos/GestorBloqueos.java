/*
 * GestorBloqueos.java
 *
 * Qué hace: lógica de negocio de los bloqueos: bloquear, desbloquear, listar bloqueados y
 * responder "¿hay un bloqueo entre A y B?". Al bloquear, avisa a sus oyentes (amistades) para que
 * eliminen la amistad y las solicitudes entre ambos.
 * Rol en la arquitectura: capa de SERVICIO del módulo de bloqueos. Los demás módulos solo lo
 * conocen como ConsultaBloqueos; la UI como ServicioBloqueos.
 * Principio SOLID: SRP (solo bloqueos; la memoria está en RegistroBloqueos y el disco en el
 * repositorio), OCP (patrón Observador: nuevas reacciones al bloquear sin modificar esta clase) y
 * DIP (dependencias inyectadas por constructor).
 * Colabora con: RegistroBloqueos, RepositorioBloqueos, DirectorioUsuarios, OyenteBloqueo,
 * GestorPersistenteBase.
 */
package una.social.bloqueos;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.persistencia.GestorPersistenteBase;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de bloqueos.
 */
public final class GestorBloqueos extends GestorPersistenteBase implements ServicioBloqueos {

    /** Nombre del archivo, para los mensajes de auditoría. */
    private static final String ARCHIVO = "bloqueos.txt";

    /** Persistencia. */
    private final RepositorioBloqueos repositorio;

    /** Estructura en memoria. */
    private final RegistroBloqueos registro;

    /** Usuarios registrados (para validar IDs). */
    private final DirectorioUsuarios directorio;

    /** Reloj para la fecha del bloqueo. */
    private final Clock reloj;

    /**
     * Oyentes del evento "bloqueo". {@link CopyOnWriteArrayList} porque se registran una vez al
     * arrancar y se recorren muchas veces, posiblemente desde hilos distintos.
     */
    private final List<OyenteBloqueo> oyentes = new CopyOnWriteArrayList<>();

    /**
     * Crea el gestor (no lee el disco hasta {@code cargarDatos()}).
     *
     * @param repositorio persistencia
     * @param registro    estructura vacía
     * @param directorio  usuarios registrados (deben cargarse antes)
     * @param auditoria   bitácora
     * @param reloj       reloj del sistema
     */
    public GestorBloqueos(RepositorioBloqueos repositorio, RegistroBloqueos registro, DirectorioUsuarios directorio,
                          RegistroAuditoria auditoria, Clock reloj) {
        // La auditoría la guarda la clase base; el resto se guarda aquí (ninguna puede ser nula).
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * Registra un oyente que reaccionará a cada bloqueo nuevo (patrón Observador). Main lo usa
     * para conectar el módulo de amistades.
     *
     * @param oyente oyente a registrar
     */
    public void agregarOyente(OyenteBloqueo oyente) {
        oyentes.add(Objects.requireNonNull(oyente, "oyente"));
    }

    // ================================================================== persistencia

    /**
     * Nombre corto del módulo; aparece en la auditoría y en los avisos de carga.
     *
     * @return "bloqueos"
     */
    @Override
    public String nombreModulo() {
        return "bloqueos";
    }

    /**
     * Lee el archivo de datos y reconstruye la estructura en memoria. Lo llama
     * {@code cargarDatos()} de la clase base al abrir el programa. Las líneas que no
     * cumplen las reglas (autor inexistente, ID repetido...) se omiten y se auditan.
     */
    @Override
    protected void cargarDesdeDisco() {
        // Lee bloqueos.txt y vacía el registro en memoria.
        List<Bloqueo> leidos = repositorio.cargarTodos();
        registro.limpiar();
        for (Bloqueo bloqueo : leidos) {
            // Mismas reglas que al bloquear desde la interfaz; si falla alguna, se omite y se audita.
            String problema = problemaDelBloqueo(bloqueo.idBloqueador(), bloqueo.idBloqueado());
            if (problema == null) {
                registro.agregar(bloqueo);
            } else {
                auditarOmitido(ARCHIVO, "bloqueo " + bloqueo.idBloqueador() + "->" + bloqueo.idBloqueado()
                        + " omitido (" + problema + ")");
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
        return registro.tamano() + " bloqueos";
    }

    // ================================================================== ServicioBloqueos

    /**
     * {@inheritDoc}
     *
     * <p>Orden: (1) se guarda el bloqueo en disco y en memoria; (2) se avisa a los oyentes. Si un
     * oyente falla, el bloqueo YA quedó guardado y sigue protegiendo al usuario (todo lo del otro
     * queda oculto); solo se informa del problema.</p>
     */
    @Override
    public void bloquear(Usuario bloqueador, String idBloqueado) {
        Objects.requireNonNull(bloqueador, "bloqueador");
        String otro = idBloqueado == null ? "" : idBloqueado.trim();
        // Paso 1: valida y guarda el bloqueo (y lo anota en la auditoría).
        Bloqueo creado = ejecutarAuditado(bloqueador, "USUARIO_BLOQUEADO", () -> crearYGuardar(bloqueador.getId(), otro),
                b -> b.idBloqueador() + " bloqueó a " + b.idBloqueado());
        // Paso 2 (patrón Observador): avisa a los demás módulos para que rompan la amistad y
        // borren las solicitudes entre los dos.
        avisarOyentes(bloqueador, creado);
    }

    /**
     * Quita un bloqueo desde la lista de "Bloqueados" del perfil. La amistad que se rompió al
     * bloquear NO vuelve; habría que enviar otra solicitud.
     *
     * @param bloqueador  usuario con la sesión iniciada
     * @param idBloqueado ID del usuario a desbloquear
     * @throws ExcepcionReglaNegocio si ese usuario no estaba bloqueado
     */
    @Override
    public void desbloquear(Usuario bloqueador, String idBloqueado) {
        Objects.requireNonNull(bloqueador, "bloqueador");
        String otro = idBloqueado == null ? "" : idBloqueado.trim();
        // quitarYGuardar lo borra de disco y memoria; ejecutarAuditado lo anota.
        ejecutarAuditado(bloqueador, "USUARIO_DESBLOQUEADO", () -> quitarYGuardar(bloqueador.getId(), otro),
                hecho -> bloqueador.getId() + " desbloqueó a " + otro);
    }

    /**
     * Indica si hay un bloqueo entre dos usuarios, en cualquiera de los dos sentidos. Lo usan el
     * muro, el buscador, los comentarios y los anuncios para ocultar contenido.
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si A bloqueó a B o B bloqueó a A
     */
    @Override
    public synchronized boolean hayBloqueoEntre(String idA, String idB) {
        return registro.hayBloqueoEntre(idA, idB);
    }

    /**
     * Indica si un usuario bloqueó a otro (solo en ese sentido).
     *
     * @param idBloqueador posible bloqueador
     * @param idBloqueado  posible bloqueado
     * @return {@code true} si el primero bloqueó al segundo
     */
    @Override
    public synchronized boolean bloqueoA(String idBloqueador, String idBloqueado) {
        return registro.bloqueo(idBloqueador, idBloqueado);
    }

    /**
     * Usuarios que bloqueó alguien (para la ventana "Bloqueados" del perfil).
     *
     * @param idBloqueador usuario consultado
     * @return usuarios bloqueados, en el orden en que los bloqueó (lista no modificable)
     */
    @Override
    public synchronized List<Usuario> listarBloqueados(String idBloqueador) {
        List<Usuario> bloqueados = new ArrayList<>();
        // Convierte cada bloqueo (que solo tiene IDs) en el Usuario completo.
        for (Bloqueo bloqueo : registro.bloqueosDe(idBloqueador)) {
            directorio.buscarPorId(bloqueo.idBloqueado()).ifPresent(bloqueados::add);
        }
        return Collections.unmodifiableList(bloqueados);
    }

    // ================================================================== operaciones internas

    /**
     * Valida y guarda un bloqueo nuevo: primero en disco, luego en memoria.
     *
     * @param idBloqueador quien bloquea
     * @param idBloqueado  quien es bloqueado
     * @return bloqueo creado
     */
    private synchronized Bloqueo crearYGuardar(String idBloqueador, String idBloqueado) {
        exigirDatosCargados();
        // Revisa las reglas (no a sí mismo, usuario existe, no repetido).
        String problema = problemaDelBloqueo(idBloqueador, idBloqueado);
        if (problema != null) {
            throw new ExcepcionReglaNegocio(problema);
        }
        // Crea el bloqueo con la fecha actual; disco primero, memoria después.
        Bloqueo nuevo = new Bloqueo(idBloqueador, idBloqueado, Instant.now(reloj));
        List<Bloqueo> conNuevo = new ArrayList<>(registro.todos());
        conNuevo.add(nuevo);
        repositorio.guardarTodos(conNuevo);
        registro.agregar(nuevo);
        return nuevo;
    }

    /**
     * Quita un bloqueo: primero en disco, luego en memoria.
     *
     * @param idBloqueador quien había bloqueado
     * @param idBloqueado  quien estaba bloqueado
     * @return {@code Boolean.TRUE} (para poder usarlo con ejecutarAuditado)
     */
    private synchronized Boolean quitarYGuardar(String idBloqueador, String idBloqueado) {
        exigirDatosCargados();
        // Solo se puede desbloquear a alguien que de verdad está bloqueado.
        if (!registro.bloqueo(idBloqueador, idBloqueado)) {
            throw new ExcepcionReglaNegocio("Ese usuario no está bloqueado.");
        }
        // Reescribe bloqueos.txt sin ese bloqueo y luego lo quita de la memoria.
        List<Bloqueo> sinEl = new ArrayList<>(registro.todos());
        sinEl.removeIf(b -> b.idBloqueador().equals(idBloqueador) && b.idBloqueado().equals(idBloqueado));
        repositorio.guardarTodos(sinEl);
        registro.quitar(idBloqueador, idBloqueado);
        return Boolean.TRUE;
    }

    /**
     * Avisa a los oyentes del bloqueo nuevo (fuera del candado, para no bloquear otros módulos).
     *
     * @param bloqueador usuario que bloqueó
     * @param bloqueo    bloqueo creado
     * @throws ExcepcionPersistencia si algún oyente no pudo guardar su parte
     */
    private void avisarOyentes(Usuario bloqueador, Bloqueo bloqueo) {
        for (OyenteBloqueo oyente : oyentes) {
            // Se espera ExcepcionPersistencia si el oyente (amistades) no pudo guardar. El bloqueo
            // ya está guardado y vigente; se audita y se informa al usuario con un mensaje claro.
            try {
                oyente.alBloquear(bloqueo.idBloqueador(), bloqueo.idBloqueado());
            } catch (ExcepcionPersistencia ex) {
                auditoria.registrar(NivelAuditoria.ERROR, bloqueador.getNombreUsuario(), "BLOQUEO_INCOMPLETO",
                        RegistroAuditoria.describirError(ex));
                throw new ExcepcionPersistencia("El usuario quedó bloqueado, pero no se pudo actualizar la lista "
                        + "de amigos o solicitudes. Se reintentará al cerrar sesión.", ex);
            }
        }
    }

    /**
     * Reglas de un bloqueo (se usan al bloquear y al cargar el archivo).
     *
     * @param idBloqueador quien bloquea
     * @param idBloqueado  quien es bloqueado
     * @return descripción del problema, o {@code null} si es válido
     */
    private String problemaDelBloqueo(String idBloqueador, String idBloqueado) {
        // Regla 1: no bloquearse a sí mismo.
        if (idBloqueador.equals(idBloqueado)) {
            return "No puedes bloquearte a ti mismo.";
        }
        // Regla 2: los dos usuarios deben existir.
        if (directorio.buscarPorId(idBloqueador).isEmpty() || directorio.buscarPorId(idBloqueado).isEmpty()) {
            return "El usuario indicado no existe.";
        }
        // Regla 3: no bloquear dos veces al mismo.
        if (registro.bloqueo(idBloqueador, idBloqueado)) {
            return "Ya bloqueaste a este usuario.";
        }
        return null;
    }
}
