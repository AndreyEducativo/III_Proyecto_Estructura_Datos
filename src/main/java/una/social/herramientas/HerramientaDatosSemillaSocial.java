/*
 * HerramientaDatosSemillaSocial.java
 *
 * Qué hace: crea amistades y publicaciones de ejemplo entre los 3 usuarios de prueba (U001 Ana,
 * U002 Luis, U003 Maria) cuando data/amistades.txt y data/publicaciones.txt están vacíos.
 * Rol en la arquitectura: Main la llama al arrancar, después de cargar los datos. Usa los MISMOS
 * servicios que la interfaz gráfica, así los datos de ejemplo pasan por las mismas validaciones.
 * Principio SOLID: DIP (trabaja con las interfaces DirectorioUsuarios, ServicioAmistades y
 * ServicioPublicaciones) y SRP (solo siembra datos de ejemplo).
 * Colabora con: ServicioAmistades, ServicioPublicaciones, DirectorioUsuarios, RegistroAuditoria.
 */
package una.social.herramientas;

import java.util.List;
import java.util.Optional;
import una.social.amistades.ServicioAmistades;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.publicaciones.ServicioPublicaciones;
import una.social.servicio.DirectorioUsuarios;

/**
 * Generador de amistades y publicaciones de ejemplo.
 *
 * <p>Red de ejemplo (pensada para demostrar el muro cruzado):</p>
 * <pre>
 *   Ana (U001) ── Luis (U002) ── Maria (U003)
 * </pre>
 * <p>Ana ve en su muro sus publicaciones y las de Luis, pero NO las de Maria (no son amigas).
 * Luis ve las de los tres. Maria ve las suyas y las de Luis.</p>
 */
public final class HerramientaDatosSemillaSocial {

    /**
     * Una publicación de ejemplo.
     *
     * @param idAutor ID del autor
     * @param texto   texto de la publicación
     */
    private record PublicacionSemilla(String idAutor, String texto) {
    }

    /** Pares de amigos de ejemplo: {quien agrega, agregado}. */
    private static final List<String[]> AMISTADES_SEMILLA = List.of(
            new String[] {"U001", "U002"},
            new String[] {"U002", "U003"});

    /** Publicaciones de ejemplo, en orden cronológico. Incluyen '|' y saltos de línea a propósito
     *  para demostrar que el escape del archivo funciona. */
    private static final List<PublicacionSemilla> PUBLICACIONES_SEMILLA = List.of(
            new PublicacionSemilla("U001", "¡Hola a todos! Estrenando Social UNA."),
            new PublicacionSemilla("U002", "¿Alguien tiene los apuntes de Estructuras de Datos de la semana pasada?"),
            new PublicacionSemilla("U003", "Recordatorio: el proyecto final se entrega el 10 de noviembre. ¡Ánimo!"),
            new PublicacionSemilla("U001", "Luis, te paso los temas por aquí:\n- Listas dinámicas\n- Grafos y listas de adyacencia"),
            new PublicacionSemilla("U002", "Gracias, Ana | ya los revisé."),
            new PublicacionSemilla("U003", "Buenos días desde la Sede Regional Central - Occidente."));

    /** Clase de utilidades: no se instancia. */
    private HerramientaDatosSemillaSocial() {
    }

    /**
     * Crea las amistades y publicaciones de ejemplo solo si existen los 3 usuarios de prueba y
     * NINGÚN usuario tiene todavía amigos ni publicaciones. En cualquier otro caso no hace nada,
     * para no mezclar datos de ejemplo con datos reales.
     *
     * @param directorio    usuarios registrados
     * @param amistades     servicio de amistades (ya cargado)
     * @param publicaciones servicio de publicaciones (ya cargado)
     * @param auditoria     bitácora
     * @return {@code true} si se crearon los datos de ejemplo
     */
    public static boolean sembrarSiEstaVacio(DirectorioUsuarios directorio, ServicioAmistades amistades,
                                             ServicioPublicaciones publicaciones, RegistroAuditoria auditoria) {
        Optional<Usuario> ana = directorio.buscarPorId("U001");
        Optional<Usuario> luis = directorio.buscarPorId("U002");
        Optional<Usuario> maria = directorio.buscarPorId("U003");
        if (ana.isEmpty() || luis.isEmpty() || maria.isEmpty() || hayDatosSociales(directorio, amistades, publicaciones)) {
            return false;
        }
        for (String[] par : AMISTADES_SEMILLA) {
            amistades.agregarAmistad(directorio.buscarPorId(par[0]).orElseThrow(), par[1]);
        }
        for (PublicacionSemilla semilla : PUBLICACIONES_SEMILLA) {
            publicaciones.publicar(directorio.buscarPorId(semilla.idAutor()).orElseThrow(), semilla.texto());
        }
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_SEMILLA_CREADOS",
                AMISTADES_SEMILLA.size() + " amistades y " + PUBLICACIONES_SEMILLA.size() + " publicaciones de ejemplo");
        return true;
    }

    /**
     * @param directorio    usuarios registrados
     * @param amistades     servicio de amistades
     * @param publicaciones servicio de publicaciones
     * @return {@code true} si algún usuario ya tiene amigos o publicaciones visibles
     */
    private static boolean hayDatosSociales(DirectorioUsuarios directorio, ServicioAmistades amistades,
                                            ServicioPublicaciones publicaciones) {
        for (Usuario usuario : directorio.listarTodos()) {
            if (!amistades.idsAmigos(usuario.getId()).isEmpty() || !publicaciones.obtenerMuro(usuario).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
