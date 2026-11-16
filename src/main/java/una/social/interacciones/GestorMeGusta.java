/*
 * GestorMeGusta.java
 *
 * Qué hace: lógica de negocio de los "me gusta": alternar (dar/quitar), consultar el contador y
 * borrar en cascada los de una publicación borrada.
 * Rol en la arquitectura: capa de SERVICIO de los "me gusta". La UI le habla por ServicioMeGusta;
 * el módulo de publicaciones le avisa como OyenteBorradoPublicacion.
 * Principio SOLID: SRP (solo "me gusta"; la memoria está en RegistroMeGusta y el disco en el
 * repositorio), OCP (hereda el esqueleto de GestorPersistenteBase) y DIP (todo inyectado).
 * Colabora con: RegistroMeGusta, RepositorioMeGusta, ConsultaPublicaciones, DirectorioUsuarios,
 * GestorPersistenteBase.
 */
package una.social.interacciones;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.persistencia.GestorPersistenteBase;
import una.social.publicaciones.ConsultaPublicaciones;
import una.social.publicaciones.OyenteBorradoPublicacion;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de "me gusta".
 */
public final class GestorMeGusta extends GestorPersistenteBase implements ServicioMeGusta, OyenteBorradoPublicacion {

    /** Nombre del archivo, para la auditoría. */
    private static final String ARCHIVO = "megusta.txt";

    /** Persistencia. */
    private final RepositorioMeGusta repositorio;

    /** Estructura en memoria. */
    private final RegistroMeGusta registro;

    /** Usuarios registrados (para validar al cargar). */
    private final DirectorioUsuarios directorio;

    /** Publicaciones (para saber si existen y si el usuario puede verlas). */
    private final ConsultaPublicaciones publicaciones;

    /** Reloj para la fecha. */
    private final Clock reloj;

    /**
     * Crea el gestor (no lee el disco hasta {@code cargarDatos()}).
     *
     * @param repositorio   persistencia
     * @param registro      estructura vacía
     * @param directorio    usuarios registrados
     * @param publicaciones consulta de publicaciones (deben cargarse ANTES)
     * @param auditoria     bitácora
     * @param reloj         reloj del sistema
     */
    public GestorMeGusta(RepositorioMeGusta repositorio, RegistroMeGusta registro, DirectorioUsuarios directorio,
                         ConsultaPublicaciones publicaciones, RegistroAuditoria auditoria, Clock reloj) {
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.publicaciones = Objects.requireNonNull(publicaciones, "publicaciones");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== persistencia

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "me gusta";
    }

    /** {@inheritDoc} */
    @Override
    protected void cargarDesdeDisco() {
        List<MeGusta> leidos = repositorio.cargarTodos();
        registro.limpiar();
        for (MeGusta meGusta : leidos) {
            String problema = null;
            if (directorio.buscarPorId(meGusta.idUsuario()).isEmpty()) {
                problema = "el usuario no existe";
            } else if (publicaciones.autorDe(meGusta.idPublicacion()).isEmpty()) {
                problema = "la publicación no existe";
            } else if (registro.dioMeGusta(meGusta.idPublicacion(), meGusta.idUsuario())) {
                problema = "repetido";
            }
            if (problema == null) {
                registro.agregar(meGusta);
            } else {
                auditarOmitido(ARCHIVO, meGusta.idUsuario() + "->" + meGusta.idPublicacion() + " omitido (" + problema + ")");
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
        return registro.tamano() + " \"me gusta\"";
    }

    // ================================================================== ServicioMeGusta

    /** {@inheritDoc} */
    @Override
    public EstadoMeGusta alternarMeGusta(Usuario usuario, String idPublicacion) {
        Objects.requireNonNull(usuario, "usuario");
        String id = idPublicacion == null ? "" : idPublicacion.trim();
        return ejecutarAuditado(usuario, "ME_GUSTA", () -> alternar(usuario.getId(), id),
                e -> (e.meGustaMio() ? "dio me gusta a " : "quitó su me gusta de ") + id);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized EstadoMeGusta estado(String idUsuario, String idPublicacion) {
        return new EstadoMeGusta(registro.contar(idPublicacion), registro.dioMeGusta(idPublicacion, idUsuario));
    }

    /**
     * Da o quita el "me gusta" (disco primero, memoria después).
     *
     * @param idUsuario     usuario
     * @param idPublicacion publicación
     * @return estado nuevo
     */
    private synchronized EstadoMeGusta alternar(String idUsuario, String idPublicacion) {
        exigirDatosCargados();
        List<MeGusta> nuevos = new ArrayList<>(registro.todos());
        if (registro.dioMeGusta(idPublicacion, idUsuario)) {
            // Quitar SIEMPRE se permite (aunque ya no pueda ver la publicación).
            nuevos.removeIf(m -> m.idPublicacion().equals(idPublicacion) && m.idUsuario().equals(idUsuario));
            repositorio.guardarTodos(nuevos);
            registro.quitar(idPublicacion, idUsuario);
        } else {
            if (!publicaciones.puedeVer(idUsuario, idPublicacion)) {
                throw new ExcepcionReglaNegocio("No puedes reaccionar a esta publicación.");
            }
            MeGusta nuevo = new MeGusta(idPublicacion, idUsuario, Instant.now(reloj));
            nuevos.add(nuevo);
            repositorio.guardarTodos(nuevos);
            registro.agregar(nuevo);
        }
        return new EstadoMeGusta(registro.contar(idPublicacion), registro.dioMeGusta(idPublicacion, idUsuario));
    }

    // ================================================================== OyenteBorradoPublicacion

    /**
     * {@inheritDoc}
     *
     * <p>Borrado en cascada: se quitan todos los "me gusta" de la publicación borrada (disco
     * primero). Si este módulo no llegó a cargarse, no hay nada en memoria: lo que quede en el
     * archivo se descartará solo en la próxima carga (la publicación ya no existe).</p>
     */
    @Override
    public synchronized void alBorrarPublicacion(String idPublicacion) {
        if (!estanDatosCargados() || registro.contar(idPublicacion) == 0) {
            return;
        }
        List<MeGusta> restantes = new ArrayList<>(registro.todos());
        restantes.removeIf(m -> m.idPublicacion().equals(idPublicacion));
        repositorio.guardarTodos(restantes);
        int quitados = registro.quitarDePublicacion(idPublicacion);
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA,
                "ME_GUSTA_BORRADOS_EN_CASCADA", quitados + " de " + idPublicacion);
    }
}
