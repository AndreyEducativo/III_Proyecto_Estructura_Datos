/*
 * RepositorioSolicitudes.java
 *
 * Qué hace: contrato para leer y guardar las solicitudes de amistad pendientes.
 * Rol en la arquitectura: GestorAmistades depende de esta interfaz; no sabe si los datos están en
 * un .txt o en memoria.
 * Principio SOLID: DIP, ISP (dos operaciones) y OCP (otro formato = otra implementación).
 * Colabora con: RepositorioSolicitudesArchivoTexto, GestorAmistades.
 */
package una.social.amistades;

import java.util.List;

/**
 * Persistencia de las solicitudes de amistad pendientes.
 */
public interface RepositorioSolicitudes {

    /**
     * Lee todas las solicitudes guardadas. Las líneas dañadas se omiten y se auditan.
     *
     * @return solicitudes con formato válido, en el orden del archivo
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<SolicitudAmistad> cargarTodas();

    /**
     * Reemplaza todo lo guardado por la lista dada ("todo o nada").
     *
     * @param solicitudes lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodas(List<SolicitudAmistad> solicitudes);
}
