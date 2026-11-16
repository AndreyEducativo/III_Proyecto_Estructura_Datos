/*
 * EnsambladorSocial.java
 *
 * Qué hace: crea y conecta TODOS los objetos del módulo social (bloqueos, amistades, sugerencias,
 * buscador, publicaciones, me gusta, comentarios, perfiles y anuncios) a partir de la carpeta de
 * datos, y registra los oyentes del patrón Observador.
 * Rol en la arquitectura: parte de la "raíz de composición" junto con Main y
 * EnsambladorServicios. Es el único lugar del módulo social donde se hace "new" de
 * implementaciones concretas.
 * Principio SOLID: DIP (el resto del módulo recibe abstracciones ya armadas) y OCP (se agrega sin
 * modificar el ensamblador de Andrey).
 * Colabora con: Main, ModulosSociales y todos los gestores sociales.
 */
package una.social;

import java.nio.file.Path;
import java.time.Clock;
import una.social.amistades.BuscadorUsuarios;
import una.social.amistades.GestorAmistades;
import una.social.amistades.GrafoAmistades;
import una.social.amistades.RepositorioAmistadesArchivoTexto;
import una.social.amistades.RepositorioSolicitudesArchivoTexto;
import una.social.amistades.SugeridorAmistades;
import una.social.anuncios.GestorAnuncios;
import una.social.anuncios.RepositorioAnunciosArchivoTexto;
import una.social.anuncios.TablonAnuncios;
import una.social.auditoria.RegistroAuditoria;
import una.social.bloqueos.GestorBloqueos;
import una.social.bloqueos.RegistroBloqueos;
import una.social.bloqueos.RepositorioBloqueosArchivoTexto;
import una.social.interacciones.GestorComentarios;
import una.social.interacciones.GestorMeGusta;
import una.social.interacciones.RegistroComentarios;
import una.social.interacciones.RegistroMeGusta;
import una.social.interacciones.RepositorioComentariosArchivoTexto;
import una.social.interacciones.RepositorioMeGustaArchivoTexto;
import una.social.perfiles.GestorPerfiles;
import una.social.perfiles.RepositorioPerfilesArchivoTexto;
import una.social.publicaciones.GestorPublicaciones;
import una.social.publicaciones.HistorialPublicaciones;
import una.social.publicaciones.RegistroCompartidos;
import una.social.publicaciones.RepositorioCompartidosArchivoTexto;
import una.social.publicaciones.RepositorioPublicacionesArchivoTexto;
import una.social.servicio.DirectorioUsuarios;

/**
 * Fábrica del módulo social.
 */
public final class EnsambladorSocial {

    /** Nombre del archivo de amistades dentro de data/. */
    public static final String ARCHIVO_AMISTADES = "amistades.txt";

    /** Nombre del archivo de solicitudes de amistad pendientes dentro de data/. */
    public static final String ARCHIVO_SOLICITUDES = "solicitudes.txt";

    /** Nombre del archivo de bloqueos dentro de data/. */
    public static final String ARCHIVO_BLOQUEOS = "bloqueos.txt";

    /** Nombre del archivo de publicaciones dentro de data/. */
    public static final String ARCHIVO_PUBLICACIONES = "publicaciones.txt";

    /** Nombre del archivo de compartidos dentro de data/. */
    public static final String ARCHIVO_COMPARTIDOS = "compartidos.txt";

    /** Nombre del archivo de "me gusta" dentro de data/. */
    public static final String ARCHIVO_ME_GUSTA = "megusta.txt";

    /** Nombre del archivo de comentarios dentro de data/. */
    public static final String ARCHIVO_COMENTARIOS = "comentarios.txt";

    /** Nombre del archivo de biografías dentro de data/. */
    public static final String ARCHIVO_PERFILES = "perfiles.txt";

    /** Nombre del archivo de anuncios dentro de data/. */
    public static final String ARCHIVO_ANUNCIOS = "anuncios.txt";

    /** Clase de utilidades: no se instancia. */
    private EnsambladorSocial() {
    }

    /**
     * Crea y conecta todos los servicios sociales (no carga datos todavía).
     *
     * <p>Orden de creación: primero los que no dependen de otros (bloqueos), luego amistades
     * (consulta bloqueos), publicaciones (consulta amistades y bloqueos) y al final me gusta y
     * comentarios (consultan publicaciones). Después se registran los OYENTES:</p>
     * <ul>
     *   <li>Al bloquear, amistades elimina la amistad y las solicitudes entre ambos.</li>
     *   <li>Al borrar una publicación, me gusta y comentarios borran los suyos (cascada).</li>
     * </ul>
     *
     * @param carpetaDatos carpeta data/
     * @param directorio   usuarios registrados (solo lectura)
     * @param auditoria    bitácora
     * @param reloj        reloj del sistema
     * @return todos los servicios sociales conectados
     */
    public static ModulosSociales crear(Path carpetaDatos, DirectorioUsuarios directorio,
                                        RegistroAuditoria auditoria, Clock reloj) {
        GestorBloqueos bloqueos = new GestorBloqueos(
                new RepositorioBloqueosArchivoTexto(carpetaDatos.resolve(ARCHIVO_BLOQUEOS), auditoria),
                new RegistroBloqueos(), directorio, auditoria, reloj);
        GestorAmistades amistades = new GestorAmistades(
                new RepositorioAmistadesArchivoTexto(carpetaDatos.resolve(ARCHIVO_AMISTADES), auditoria),
                new RepositorioSolicitudesArchivoTexto(carpetaDatos.resolve(ARCHIVO_SOLICITUDES), auditoria),
                new GrafoAmistades(), directorio, bloqueos, auditoria, reloj);
        GestorPublicaciones publicaciones = new GestorPublicaciones(
                new RepositorioPublicacionesArchivoTexto(carpetaDatos.resolve(ARCHIVO_PUBLICACIONES), auditoria),
                new RepositorioCompartidosArchivoTexto(carpetaDatos.resolve(ARCHIVO_COMPARTIDOS), auditoria),
                new HistorialPublicaciones(), new RegistroCompartidos(), directorio, amistades, bloqueos, auditoria, reloj);
        GestorMeGusta meGusta = new GestorMeGusta(
                new RepositorioMeGustaArchivoTexto(carpetaDatos.resolve(ARCHIVO_ME_GUSTA), auditoria),
                new RegistroMeGusta(), directorio, publicaciones, auditoria, reloj);
        GestorComentarios comentarios = new GestorComentarios(
                new RepositorioComentariosArchivoTexto(carpetaDatos.resolve(ARCHIVO_COMENTARIOS), auditoria),
                new RegistroComentarios(), directorio, publicaciones, bloqueos, auditoria, reloj);
        GestorPerfiles perfiles = new GestorPerfiles(
                new RepositorioPerfilesArchivoTexto(carpetaDatos.resolve(ARCHIVO_PERFILES), auditoria), directorio, auditoria);
        GestorAnuncios anuncios = new GestorAnuncios(
                new RepositorioAnunciosArchivoTexto(carpetaDatos.resolve(ARCHIVO_ANUNCIOS), auditoria),
                new TablonAnuncios(), directorio, bloqueos, auditoria, reloj);

        // Patrón Observador: se conectan las reacciones sin que los módulos se conozcan entre sí.
        bloqueos.agregarOyente(amistades);
        publicaciones.agregarOyente(meGusta);
        publicaciones.agregarOyente(comentarios);

        BuscadorUsuarios buscador = new BuscadorUsuarios(directorio, amistades, amistades, bloqueos);
        SugeridorAmistades sugeridor = new SugeridorAmistades(directorio, amistades, amistades, bloqueos);
        return new ModulosSociales(bloqueos, amistades, sugeridor, buscador, publicaciones, meGusta, comentarios,
                perfiles, anuncios);
    }
}
