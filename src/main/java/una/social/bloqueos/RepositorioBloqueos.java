/*
 * RepositorioBloqueos.java
 *
 * Qué hace: contrato para leer y guardar los bloqueos.
 * Rol en la arquitectura: GestorBloqueos depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioBloqueosArchivoTexto, GestorBloqueos.
 */
package una.social.bloqueos;

import java.util.List;

/**
 * Persistencia de los bloqueos.
 */
public interface RepositorioBloqueos {

    /**
     * @return bloqueos con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Bloqueo> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param bloqueos lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Bloqueo> bloqueos);
}
