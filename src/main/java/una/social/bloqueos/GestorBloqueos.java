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

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "bloqueos";
    }

    /** {@inheritDoc} */
    @Override
    protected void cargarDesdeDisco() {
        List<Bloqueo> leidos = repositorio.cargarTodos();
        registro.limpiar();
        for (Bloqueo bloqueo : leidos) {
            String problema = problemaDelBloqueo(bloqueo.idBloqueador(), bloqueo.idBloqueado());
            if (problema == null) {
                registro.agregar(bloqueo);
            } else {
                auditarOmitido(ARCHIVO, "bloqueo " + bloqueo.idBloqueador() + "->" + bloqueo.idBloqueado()
                        + " omitido (" + problema + ")");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(registro.todos());
    }

    /** {@inheritDoc} */
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
        Bloqueo creado = ejecutarAuditado(bloqueador, "USUARIO_BLOQUEADO", () -> crearYGuardar(bloqueador.getId(), otro),
                b -> b.idBloqueador() + " bloqueó a " + b.idBloqueado());
        avisarOyentes(bloqueador, creado);
    }

    /** {@inheritDoc} */
    @Override
    public void desbloquear(Usuario bloqueador, String idBloqueado) {
        Objects.requireNonNull(bloqueador, "bloqueador");
        String otro = idBloqueado == null ? "" : idBloqueado.trim();
        ejecutarAuditado(bloqueador, "USUARIO_DESBLOQUEADO", () -> quitarYGuardar(bloqueador.getId(), otro),
                hecho -> bloqueador.getId() + " desbloqueó a " + otro);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean hayBloqueoEntre(String idA, String idB) {
        return registro.hayBloqueoEntre(idA, idB);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean bloqueoA(String idBloqueador, String idBloqueado) {
        return registro.bloqueo(idBloqueador, idBloqueado);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Usuario> listarBloqueados(String idBloqueador) {
        List<Usuario> bloqueados = new ArrayList<>();
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
        String problema = problemaDelBloqueo(idBloqueador, idBloqueado);
        if (problema != null) {
            throw new ExcepcionReglaNegocio(problema);
        }
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
        if (!registro.bloqueo(idBloqueador, idBloqueado)) {
            throw new ExcepcionReglaNegocio("Ese usuario no está bloqueado.");
        }
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
        if (idBloqueador.equals(idBloqueado)) {
            return "No puedes bloquearte a ti mismo.";
        }
        if (directorio.buscarPorId(idBloqueador).isEmpty() || directorio.buscarPorId(idBloqueado).isEmpty()) {
            return "El usuario indicado no existe.";
        }
        if (registro.bloqueo(idBloqueador, idBloqueado)) {
            return "Ya bloqueaste a este usuario.";
        }
        return null;
    }
}
