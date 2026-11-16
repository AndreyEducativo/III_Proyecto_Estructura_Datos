/*
 * GestorPerfiles.java
 *
 * Qué hace: lógica de negocio de las biografías: consultarlas y cambiarlas (máximo 160 caracteres).
 * Rol en la arquitectura: capa de SERVICIO del módulo de perfiles. La UI le habla por
 * ServicioPerfiles; Main lo registra como ModuloPersistente.
 * Principio SOLID: SRP (solo biografías), OCP (hereda el esqueleto de GestorPersistenteBase) y DIP.
 * Colabora con: RepositorioPerfiles, DirectorioUsuarios, GestorPersistenteBase.
 */
package una.social.perfiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;
import una.social.persistencia.GestorPersistenteBase;
import una.social.servicio.DirectorioUsuarios;

/**
 * Servicio de perfiles.
 *
 * <p><b>Estructura:</b> {@code HashMap<idUsuario, biografía>}: consultar la biografía de alguien
 * cuesta O(1). Solo se guardan los usuarios que escribieron una biografía.</p>
 */
public final class GestorPerfiles extends GestorPersistenteBase implements ServicioPerfiles {

    /** Nombre del archivo, para la auditoría. */
    private static final String ARCHIVO = "perfiles.txt";

    /** Persistencia. */
    private final RepositorioPerfiles repositorio;

    /** Usuarios registrados (para validar al cargar). */
    private final DirectorioUsuarios directorio;

    /** Usuario → biografía. */
    private final Map<String, String> biografias = new LinkedHashMap<>();

    /**
     * Crea el gestor (no lee el disco hasta {@code cargarDatos()}).
     *
     * @param repositorio persistencia
     * @param directorio  usuarios registrados (deben cargarse ANTES)
     * @param auditoria   bitácora
     */
    public GestorPerfiles(RepositorioPerfiles repositorio, DirectorioUsuarios directorio, RegistroAuditoria auditoria) {
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
    }

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "perfiles";
    }

    /** {@inheritDoc} */
    @Override
    protected void cargarDesdeDisco() {
        List<Perfil> leidos = repositorio.cargarTodos();
        biografias.clear();
        for (Perfil perfil : leidos) {
            if (directorio.buscarPorId(perfil.idUsuario()).isEmpty()) {
                auditarOmitido(ARCHIVO, "perfil de " + perfil.idUsuario() + " omitido (el usuario no existe)");
            } else if (biografias.putIfAbsent(perfil.idUsuario(), perfil.biografia()) != null) {
                auditarOmitido(ARCHIVO, "perfil de " + perfil.idUsuario() + " omitido (repetido)");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(comoLista(biografias));
    }

    /** {@inheritDoc} */
    @Override
    protected String resumen() {
        return biografias.size() + " biografías";
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String obtenerBiografia(String idUsuario) {
        return biografias.getOrDefault(idUsuario, "");
    }

    /** {@inheritDoc} */
    @Override
    public String actualizarBiografia(Usuario usuario, String biografia) {
        Objects.requireNonNull(usuario, "usuario");
        return ejecutarAuditado(usuario, "BIOGRAFIA_ACTUALIZADA", () -> guardar(usuario.getId(), biografia),
                b -> b.isEmpty() ? "biografía borrada" : b.length() + " caracteres");
    }

    /**
     * Valida y guarda la biografía (disco primero, memoria después).
     *
     * @param idUsuario usuario
     * @param biografia texto escrito
     * @return biografía guardada
     */
    private synchronized String guardar(String idUsuario, String biografia) {
        exigirDatosCargados();
        String limpia = biografia == null ? "" : biografia.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (limpia.length() > Perfil.LARGO_MAXIMO_BIOGRAFIA) {
            throw new ExcepcionReglaNegocio("La biografía puede tener como máximo " + Perfil.LARGO_MAXIMO_BIOGRAFIA
                    + " caracteres (tiene " + limpia.length() + ").");
        }
        Map<String, String> nuevas = new LinkedHashMap<>(biografias);
        if (limpia.isEmpty()) {
            nuevas.remove(idUsuario);
        } else {
            nuevas.put(idUsuario, limpia);
        }
        repositorio.guardarTodos(comoLista(nuevas));
        biografias.clear();
        biografias.putAll(nuevas);
        return limpia;
    }

    /**
     * @param mapa usuario → biografía
     * @return lista de perfiles para guardar
     */
    private static List<Perfil> comoLista(Map<String, String> mapa) {
        List<Perfil> perfiles = new ArrayList<>(mapa.size());
        mapa.forEach((id, bio) -> perfiles.add(new Perfil(id, bio)));
        return perfiles;
    }
}
