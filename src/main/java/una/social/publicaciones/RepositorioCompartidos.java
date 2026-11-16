/*
 * RepositorioCompartidos.java
 *
 * Qué hace: contrato para leer y guardar las publicaciones compartidas.
 * Rol en la arquitectura: GestorPublicaciones depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioCompartidosArchivoTexto, GestorPublicaciones.
 */
package una.social.publicaciones;

import java.util.List;

/**
 * Persistencia de los compartidos.
 */
public interface RepositorioCompartidos {

    /**
     * @return compartidos con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Compartido> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param compartidos lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Compartido> compartidos);
}
