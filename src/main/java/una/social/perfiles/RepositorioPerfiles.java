/*
 * RepositorioPerfiles.java
 *
 * Qué hace: contrato para leer y guardar los perfiles (biografías).
 * Rol en la arquitectura: GestorPerfiles depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioPerfilesArchivoTexto, GestorPerfiles.
 */
package una.social.perfiles;

import java.util.List;

/**
 * Persistencia de los perfiles.
 */
public interface RepositorioPerfiles {

    /**
     * @return perfiles con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Perfil> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param perfiles lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Perfil> perfiles);
}
