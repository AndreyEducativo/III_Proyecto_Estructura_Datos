/*
 * RepositorioAnuncios.java
 *
 * Qué hace: contrato para leer y guardar los anuncios.
 * Rol en la arquitectura: GestorAnuncios depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioAnunciosArchivoTexto, GestorAnuncios.
 */
package una.social.anuncios;

import java.util.List;

/**
 * Persistencia de los anuncios.
 */
public interface RepositorioAnuncios {

    /**
     * @return anuncios con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Anuncio> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param anuncios lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Anuncio> anuncios);
}
