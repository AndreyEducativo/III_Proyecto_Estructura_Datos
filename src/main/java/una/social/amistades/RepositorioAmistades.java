/*
 * RepositorioAmistades.java
 *
 * Qué hace: contrato para leer y guardar las amistades en un almacenamiento.
 * Rol en la arquitectura: GestorAmistades depende de esta interfaz; no sabe si los datos están en
 * un .txt o en memoria (pruebas).
 * Principio SOLID: DIP, ISP (dos operaciones) y OCP (otro formato = otra implementación).
 * Colabora con: RepositorioAmistadesArchivoTexto, GestorAmistades.
 */
package una.social.amistades;

import java.util.List;

/**
 * Persistencia de las amistades.
 */
public interface RepositorioAmistades {

    /**
     * Lee todas las amistades guardadas. Las líneas dañadas se omiten y se auditan.
     *
     * @return amistades con formato válido, en el orden del archivo
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Amistad> cargarTodas();

    /**
     * Reemplaza todo lo guardado por la lista dada ("todo o nada").
     *
     * @param amistades lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodas(List<Amistad> amistades);
}
