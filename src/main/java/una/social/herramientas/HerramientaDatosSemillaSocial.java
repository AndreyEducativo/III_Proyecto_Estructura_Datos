/*
 * HerramientaDatosSemillaSocial.java
 *
 * Qué hace: crea datos de ejemplo del módulo social entre los 3 usuarios de prueba (U001 Ana,
 * U002 Luis, U003 Maria): amistades, una solicitud pendiente, publicaciones públicas y "solo
 * amigos", "me gusta", comentarios, un compartido, biografías y anuncios.
 * Rol en la arquitectura: Main la llama al arrancar, después de cargar los datos, SOLO si todavía
 * no hay ningún dato social. Usa los MISMOS servicios que la interfaz gráfica, así los datos de
 * ejemplo pasan por las mismas validaciones.
 * Principio SOLID: SRP (solo siembra datos de ejemplo) y DIP (usa los servicios ya armados).
 * Colabora con: ModulosSociales, DirectorioUsuarios, RegistroAuditoria.
 */
package una.social.herramientas;

import java.util.List;
import java.util.Optional;
import una.social.ModulosSociales;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.publicaciones.Privacidad;
import una.social.publicaciones.Publicacion;
import una.social.servicio.DirectorioUsuarios;

/**
 * Generador de datos sociales de ejemplo.
 *
 * <p>Red de ejemplo (pensada para la defensa):</p>
 * <pre>
 *   Ana (U001) ── Luis (U002) ── Maria (U003)        Maria → Ana: solicitud pendiente
 * </pre>
 * <ul>
 *   <li>Ana NO es amiga de Maria: en "Mis amigos" no ve sus publicaciones "solo amigos", pero sí ve
 *       las públicas en el "Muro general" y el compartido de Luis.</li>
 *   <li>Ana tiene una solicitud de Maria esperando en la pestaña "Solicitudes". Si la rechaza,
 *       Maria aparece en "Personas que quizás conozcas" (1 amigo en común: Luis).</li>
 * </ul>
 */
public final class HerramientaDatosSemillaSocial {

    /**
     * Publicación de ejemplo.
     *
     * @param idAutor    autor
     * @param privacidad privacidad
     * @param texto      texto (incluye '|' y saltos de línea a propósito, para demostrar el escape)
     */
    private record PublicacionSemilla(String idAutor, Privacidad privacidad, String texto) {
    }

    /** Publicaciones de ejemplo, en orden cronológico. */
    private static final List<PublicacionSemilla> PUBLICACIONES = List.of(
            new PublicacionSemilla("U001", Privacidad.PUBLICA, "¡Hola a todos! Estrenando Social UNA."),
            new PublicacionSemilla("U002", Privacidad.SOLO_AMIGOS, "¿Alguien tiene los apuntes de Estructuras de Datos de la semana pasada?"),
            new PublicacionSemilla("U003", Privacidad.PUBLICA, "Recordatorio: el proyecto final se entrega el 10 de noviembre. ¡Ánimo!"),
            new PublicacionSemilla("U001", Privacidad.SOLO_AMIGOS, "Luis, te paso los temas por aquí:\n- Listas dinámicas\n- Grafos y listas de adyacencia"),
            new PublicacionSemilla("U002", Privacidad.SOLO_AMIGOS, "Gracias, Ana | ya los revisé."),
            new PublicacionSemilla("U003", Privacidad.SOLO_AMIGOS, "Buenos días desde la Sede Regional Central - Occidente."));

    /** Clase de utilidades: no se instancia. */
    private HerramientaDatosSemillaSocial() {
    }

    /**
     * Crea los datos de ejemplo solo si existen los 3 usuarios de prueba y NO hay todavía ningún
     * dato social (amistades, solicitudes, publicaciones ni anuncios). En cualquier otro caso no
     * hace nada, para no mezclar datos de ejemplo con datos reales.
     *
     * @param directorio usuarios registrados
     * @param modulos    servicios sociales ya cargados
     * @param auditoria  bitácora
     * @return {@code true} si se crearon los datos de ejemplo
     */
    public static boolean sembrarSiEstaVacio(DirectorioUsuarios directorio, ModulosSociales modulos, RegistroAuditoria auditoria) {
        Optional<Usuario> ana = directorio.buscarPorId("U001");
        Optional<Usuario> luis = directorio.buscarPorId("U002");
        Optional<Usuario> maria = directorio.buscarPorId("U003");
        if (ana.isEmpty() || luis.isEmpty() || maria.isEmpty() || hayDatosSociales(directorio, modulos)) {
            return false;
        }
        crearRed(modulos, ana.get(), luis.get(), maria.get());
        List<Publicacion> creadas = crearPublicaciones(directorio, modulos);
        crearInteracciones(modulos, creadas, ana.get(), luis.get(), maria.get());
        crearPerfilesYAnuncios(modulos, ana.get(), luis.get(), maria.get());
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_SEMILLA_CREADOS",
                "Datos sociales de ejemplo: 2 amistades, 1 solicitud, " + creadas.size()
                        + " publicaciones, me gusta, comentarios, 1 compartido, 3 biografías y 2 anuncios");
        return true;
    }

    /**
     * Amistades Ana-Luis y Luis-Maria, y la solicitud pendiente Maria → Ana.
     *
     * @param modulos servicios
     * @param ana     U001
     * @param luis    U002
     * @param maria   U003
     */
    private static void crearRed(ModulosSociales modulos, Usuario ana, Usuario luis, Usuario maria) {
        modulos.amistades().agregarAmistad(ana, luis.getId());
        modulos.amistades().agregarAmistad(luis, maria.getId());
        modulos.amistades().enviarSolicitud(maria, ana.getId());
    }

    /**
     * Crea las publicaciones de ejemplo.
     *
     * @param directorio usuarios
     * @param modulos    servicios
     * @return publicaciones creadas, en el mismo orden que {@link #PUBLICACIONES}
     */
    private static List<Publicacion> crearPublicaciones(DirectorioUsuarios directorio, ModulosSociales modulos) {
        return PUBLICACIONES.stream()
                .map(s -> modulos.publicaciones().publicar(directorio.buscarPorId(s.idAutor()).orElseThrow(), s.texto(), s.privacidad()))
                .toList();
    }

    /**
     * Me gusta, comentarios y un compartido.
     *
     * @param modulos servicios
     * @param p       publicaciones creadas
     * @param ana     U001
     * @param luis    U002
     * @param maria   U003
     */
    private static void crearInteracciones(ModulosSociales modulos, List<Publicacion> p, Usuario ana, Usuario luis, Usuario maria) {
        modulos.meGusta().alternarMeGusta(luis, p.get(0).getId());
        modulos.meGusta().alternarMeGusta(maria, p.get(0).getId());
        modulos.meGusta().alternarMeGusta(ana, p.get(1).getId());
        modulos.meGusta().alternarMeGusta(luis, p.get(2).getId());
        modulos.comentarios().comentar(luis, p.get(0).getId(), "¡Bienvenida, Ana!");
        modulos.comentarios().comentar(ana, p.get(1).getId(), "Yo los tengo, te los paso por aquí.");
        // Luis comparte el recordatorio PÚBLICO de Maria: Ana lo verá en "Mis amigos" aunque no sea
        // amiga de Maria, porque lo compartió su amigo Luis.
        modulos.publicaciones().compartir(luis, p.get(2).getId());
    }

    /**
     * Biografías y anuncios.
     *
     * @param modulos servicios
     * @param ana     U001
     * @param luis    U002
     * @param maria   U003
     */
    private static void crearPerfilesYAnuncios(ModulosSociales modulos, Usuario ana, Usuario luis, Usuario maria) {
        modulos.perfiles().actualizarBiografia(ana, "Estudiante de Ingeniería en Sistemas. Me encantan las estructuras de datos.");
        modulos.perfiles().actualizarBiografia(luis, "Programador entre semana y futbolista los fines de semana.");
        modulos.perfiles().actualizarBiografia(maria, "Representante estudiantil de la Sede Regional Central - Occidente.");
        modulos.anuncios().publicar(maria, "Asamblea estudiantil",
                "El jueves a las 2:00 p. m. en el auditorio. ¡Todos invitados!");
        modulos.anuncios().publicar(luis, "Torneo de fútbol 5",
                "Inscripciones abiertas hasta el viernes. Busquen a Luis en la cancha.");
    }

    /**
     * @param directorio usuarios
     * @param modulos    servicios
     * @return {@code true} si ya existe algún dato social
     */
    private static boolean hayDatosSociales(DirectorioUsuarios directorio, ModulosSociales modulos) {
        for (Usuario usuario : directorio.listarTodos()) {
            if (!modulos.amistades().idsAmigos(usuario.getId()).isEmpty()
                    || !modulos.amistades().listarSolicitudesRecibidas(usuario.getId()).isEmpty()
                    || !modulos.publicaciones().obtenerPerfil(usuario, usuario.getId()).isEmpty()) {
                return true;
            }
        }
        return directorio.listarTodos().stream().findFirst()
                .map(u -> !modulos.anuncios().listar(u).isEmpty()).orElse(false);
    }
}
