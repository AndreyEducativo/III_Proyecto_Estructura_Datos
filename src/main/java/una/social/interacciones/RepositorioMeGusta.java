/*
 * RepositorioMeGusta.java
 *
 * Qué hace: contrato para leer y guardar los "me gusta".
 * Rol en la arquitectura: GestorMeGusta depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioMeGustaArchivoTexto, GestorMeGusta.
 */
package una.social.interacciones;

import java.util.List;

/**
 * Persistencia de los "me gusta".
 */
public interface RepositorioMeGusta {

    /**
     * @return "me gusta" con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<MeGusta> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param meGustas lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<MeGusta> meGustas);
}
