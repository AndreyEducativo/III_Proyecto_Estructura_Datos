/*
 * RepositorioPublicaciones.java
 *
 * Qué hace: contrato para leer y guardar las publicaciones.
 * Rol en la arquitectura: GestorPublicaciones depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioPublicacionesArchivoTexto, GestorPublicaciones.
 */
package una.social.publicaciones;

import java.util.List;

/**
 * Persistencia de las publicaciones.
 */
public interface RepositorioPublicaciones {

    /**
     * Lee todas las publicaciones guardadas. Las líneas dañadas se omiten y se auditan.
     *
     * @return publicaciones con formato válido, en el orden del archivo
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Publicacion> cargarTodas();

    /**
     * Reemplaza todo lo guardado por la lista dada ("todo o nada").
     *
     * @param publicaciones lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodas(List<Publicacion> publicaciones);
}
