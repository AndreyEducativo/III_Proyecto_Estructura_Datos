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
        // La auditoría la guarda la clase base; el resto de dependencias se guarda aquí.
        super(auditoria);
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.directorio = Objects.requireNonNull(directorio, "directorio");
    }

    /**
     * Nombre corto del módulo; aparece en la auditoría y en los avisos de carga.
     *
     * @return "perfiles"
     */
    @Override
    public String nombreModulo() {
        return "perfiles";
    }

    /**
     * Lee el archivo de datos y reconstruye la estructura en memoria. Lo llama
     * {@code cargarDatos()} de la clase base al abrir el programa. Las líneas que no
     * cumplen las reglas (autor inexistente, ID repetido...) se omiten y se auditan.
     */
    @Override
    protected void cargarDesdeDisco() {
        // Lee perfiles.txt y vacía el HashMap usuario -> biografía.
        List<Perfil> leidos = repositorio.cargarTodos();
        biografias.clear();
        for (Perfil perfil : leidos) {
            // Se omite si el usuario no existe o si ya tenía biografía (putIfAbsent no reemplaza).
            if (directorio.buscarPorId(perfil.idUsuario()).isEmpty()) {
                auditarOmitido(ARCHIVO, "perfil de " + perfil.idUsuario() + " omitido (el usuario no existe)");
            } else if (biografias.putIfAbsent(perfil.idUsuario(), perfil.biografia()) != null) {
                auditarOmitido(ARCHIVO, "perfil de " + perfil.idUsuario() + " omitido (repetido)");
            }
        }
    }

    /**
     * Escribe en el archivo de datos todo lo que hay en memoria. Lo llama
     * {@code guardarDatos()} de la clase base al cerrar sesión o el programa.
     */
    @Override
    protected void guardarEnDisco() {
        repositorio.guardarTodos(comoLista(biografias));
    }

    /**
     * Resumen corto de cuántos datos hay en memoria, para la auditoría.
     *
     * @return texto como "4 elementos"
     */
    @Override
    protected String resumen() {
        return biografias.size() + " biografías";
    }

    /**
     * Biografía de un usuario (texto "Acerca de mí" del perfil).
     *
     * @param idUsuario usuario
     * @return su biografía, o "" si no tiene
     */
    @Override
    public synchronized String obtenerBiografia(String idUsuario) {
        return biografias.getOrDefault(idUsuario, "");
    }

    /**
     * Botón "Editar biografía" de "Mi perfil". Si el texto queda vacío, la biografía se borra.
     *
     * @param usuario   usuario con la sesión iniciada
     * @param biografia texto nuevo
     * @return la biografía guardada (limpia)
     * @throws ExcepcionReglaNegocio si supera el largo máximo
     */
    @Override
    public String actualizarBiografia(Usuario usuario, String biografia) {
        Objects.requireNonNull(usuario, "usuario");
        // guardar() valida y escribe; ejecutarAuditado lo anota (solo el largo, no el texto).
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
        // Unifica saltos de línea y quita espacios al inicio y al final.
        String limpia = biografia == null ? "" : biografia.replace("\r\n", "\n").replace('\r', '\n').strip();
        // No puede pasarse del largo máximo.
        if (limpia.length() > Perfil.LARGO_MAXIMO_BIOGRAFIA) {
            throw new ExcepcionReglaNegocio("La biografía puede tener como máximo " + Perfil.LARGO_MAXIMO_BIOGRAFIA
                    + " caracteres (tiene " + limpia.length() + ").");
        }
        // Copia del mapa con el cambio: vacía = se borra; con texto = se guarda o reemplaza.
        Map<String, String> nuevas = new LinkedHashMap<>(biografias);
        if (limpia.isEmpty()) {
            nuevas.remove(idUsuario);
        } else {
            nuevas.put(idUsuario, limpia);
        }
        // Disco primero; si salió bien, se reemplaza el contenido en memoria.
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
