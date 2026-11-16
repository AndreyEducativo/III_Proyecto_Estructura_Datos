/*
 * EnsambladorSocial.java
 *
 * Qué hace: crea y conecta los objetos del módulo social (amistades, buscador y publicaciones) a
 * partir de la carpeta de datos.
 * Rol en la arquitectura: parte de la "raíz de composición" junto con Main y
 * EnsambladorServicios. Es el único lugar del módulo de Sebastián donde se hace "new" de
 * implementaciones concretas.
 * Principio SOLID: DIP (el resto del módulo recibe abstracciones ya armadas) y OCP (se agrega sin
 * modificar el ensamblador de Andrey).
 * Colabora con: Main, GestorAmistades, BuscadorUsuarios, GestorPublicaciones.
 */
package una.social;

import java.nio.file.Path;
import java.time.Clock;
import una.social.amistades.BuscadorUsuarios;
import una.social.amistades.ConsultaAmistades;
import una.social.amistades.GestorAmistades;
import una.social.amistades.GrafoAmistades;
import una.social.amistades.RepositorioAmistadesArchivoTexto;
import una.social.auditoria.RegistroAuditoria;
import una.social.publicaciones.GestorPublicaciones;
import una.social.publicaciones.HistorialPublicaciones;
import una.social.publicaciones.RepositorioPublicacionesArchivoTexto;
import una.social.servicio.DirectorioUsuarios;

/**
 * Fábrica de los servicios de Sebastián.
 */
public final class EnsambladorSocial {

    /** Nombre del archivo de amistades dentro de data/. */
    public static final String ARCHIVO_AMISTADES = "amistades.txt";

    /** Nombre del archivo de publicaciones dentro de data/. */
    public static final String ARCHIVO_PUBLICACIONES = "publicaciones.txt";

    /** Clase de utilidades: no se instancia. */
    private EnsambladorSocial() {
    }

    /**
     * Crea el gestor de amistades (no carga datos todavía).
     *
     * @param carpetaDatos carpeta data/
     * @param directorio   usuarios registrados
     * @param auditoria    bitácora
     * @param reloj        reloj del sistema
     * @return gestor listo para {@code cargarDatos()}
     */
    public static GestorAmistades crearGestorAmistades(Path carpetaDatos, DirectorioUsuarios directorio,
                                                       RegistroAuditoria auditoria, Clock reloj) {
        return new GestorAmistades(
                new RepositorioAmistadesArchivoTexto(carpetaDatos.resolve(ARCHIVO_AMISTADES), auditoria),
                new GrafoAmistades(), directorio, auditoria, reloj);
    }

    /**
     * Crea el gestor de publicaciones (no carga datos todavía).
     *
     * @param carpetaDatos carpeta data/
     * @param directorio   usuarios registrados
     * @param amistades    consulta de amistades (para el muro)
     * @param auditoria    bitácora
     * @param reloj        reloj del sistema
     * @return gestor listo para {@code cargarDatos()}
     */
    public static GestorPublicaciones crearGestorPublicaciones(Path carpetaDatos, DirectorioUsuarios directorio,
                                                               ConsultaAmistades amistades,
                                                               RegistroAuditoria auditoria, Clock reloj) {
        return new GestorPublicaciones(
                new RepositorioPublicacionesArchivoTexto(carpetaDatos.resolve(ARCHIVO_PUBLICACIONES), auditoria),
                new HistorialPublicaciones(), directorio, amistades, auditoria, reloj);
    }

    /**
     * Crea el buscador de usuarios.
     *
     * @param directorio usuarios registrados
     * @param amistades  consulta de amistades
     * @return buscador
     */
    public static BuscadorUsuarios crearBuscador(DirectorioUsuarios directorio, ConsultaAmistades amistades) {
        return new BuscadorUsuarios(directorio, amistades);
    }
}
