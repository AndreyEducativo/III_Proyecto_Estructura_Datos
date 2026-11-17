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
        // La auditoría la guarda la clase base; el resto de dependencias se guarda aquí.
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.registro = Objects.requireNonNull(registro, "registro");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.publicaciones = Objects.requireNonNull(publicaciones, "publicaciones");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== persistencia

    /**
     * Nombre corto del módulo; aparece en la auditoría y en los avisos de carga.
     *
     * @return "me gusta"
     */
    @Override
    public String nombreModulo() {
        return "me gusta";
    }

    /**
     * Lee el archivo de datos y reconstruye la estructura en memoria. Lo llama
     * {@code cargarDatos()} de la clase base al abrir el programa. Las líneas que no
     * cumplen las reglas (autor inexistente, ID repetido...) se omiten y se auditan.
     */
    @Override
    protected void cargarDesdeDisco() {
        // Lee megusta.txt y vacía el registro en memoria.
        List<MeGusta> leidos = repositorio.cargarTodos();
        registro.limpiar();
        for (MeGusta meGusta : leidos) {
            // Debe existir el usuario, existir la publicación y no estar repetido.
            String problema = null;
            if (directorio.buscarPorId(meGusta.idUsuario()).isEmpty()) {
                problema = "el usuario no existe";
            } else if (publicaciones.autorDe(meGusta.idPublicacion()).isEmpty()) {
                problema = "la publicación no existe";
            } else if (registro.dioMeGusta(meGusta.idPublicacion(), meGusta.idUsuario())) {
                problema = "repetido";
            }
            // Válido: entra al registro. Inválido: se omite y queda en la auditoría.
            if (problema == null) {
                registro.agregar(meGusta);
            } else {
                auditarOmitido(ARCHIVO, meGusta.idUsuario() + "->" + meGusta.idPublicacion() + " omitido (" + problema + ")");
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
        return registro.tamano() + " \"me gusta\"";
    }

    // ================================================================== ServicioMeGusta

    /**
     * Botón "Me gusta" de una publicación: si el usuario no le había dado me gusta, lo da; si ya
     * lo había dado, lo quita.
     *
     * @param usuario       usuario con la sesión iniciada
     * @param idPublicacion publicación
     * @return estado nuevo (cantidad total y si el usuario le dio me gusta)
     * @throws ExcepcionReglaNegocio si quiere dar me gusta a algo que no puede ver
     */
    @Override
    public EstadoMeGusta alternarMeGusta(Usuario usuario, String idPublicacion) {
        Objects.requireNonNull(usuario, "usuario");
        String id = idPublicacion == null ? "" : idPublicacion.trim();
        // alternar() hace el cambio; ejecutarAuditado anota si lo dio o lo quitó.
        return ejecutarAuditado(usuario, "ME_GUSTA", () -> alternar(usuario.getId(), id),
                e -> (e.meGustaMio() ? "dio me gusta a " : "quitó su me gusta de ") + id);
    }

    /**
     * Estado actual del botón "Me gusta" de una publicación para un usuario.
     *
     * @param idUsuario     usuario que mira la publicación
     * @param idPublicacion publicación
     * @return cantidad total de me gusta y si este usuario dio el suyo
     */
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
        // Copia de todos los me gusta para armar la versión nueva del archivo.
        List<MeGusta> nuevos = new ArrayList<>(registro.todos());
        // ¿Ya le había dado me gusta? Entonces se QUITA.
        if (registro.dioMeGusta(idPublicacion, idUsuario)) {
            // Quitar SIEMPRE se permite (aunque ya no pueda ver la publicación).
            nuevos.removeIf(m -> m.idPublicacion().equals(idPublicacion) && m.idUsuario().equals(idUsuario));
            repositorio.guardarTodos(nuevos);
            registro.quitar(idPublicacion, idUsuario);
        } else {
            // No le había dado: se AGREGA, pero solo si puede ver la publicación.
            if (!publicaciones.puedeVer(idUsuario, idPublicacion)) {
                throw new ExcepcionReglaNegocio("No puedes reaccionar a esta publicación.");
            }
            // Disco primero, memoria después.
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
        // Nada que hacer si el módulo no cargó o la publicación no tenía me gusta.
        if (!estanDatosCargados() || registro.contar(idPublicacion) == 0) {
            return;
        }
        // Reescribe megusta.txt sin los de esa publicación...
        List<MeGusta> restantes = new ArrayList<>(registro.todos());
        restantes.removeIf(m -> m.idPublicacion().equals(idPublicacion));
        repositorio.guardarTodos(restantes);
        // ...los quita de la memoria y lo anota en la auditoría.
        int quitados = registro.quitarDePublicacion(idPublicacion);
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA,
                "ME_GUSTA_BORRADOS_EN_CASCADA", quitados + " de " + idPublicacion);
    }
}
