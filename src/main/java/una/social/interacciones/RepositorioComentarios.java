/*
 * RepositorioComentarios.java
 *
 * Qué hace: contrato para leer y guardar los comentarios.
 * Rol en la arquitectura: GestorComentarios depende de esta interfaz.
 * Principio SOLID: DIP, ISP y OCP.
 * Colabora con: RepositorioComentariosArchivoTexto, GestorComentarios.
 */
package una.social.interacciones;

import java.util.List;

/**
 * Persistencia de los comentarios.
 */
public interface RepositorioComentarios {

    /**
     * @return comentarios con formato válido (las líneas dañadas se omiten y se auditan)
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede leer el almacenamiento
     */
    List<Comentario> cargarTodos();

    /**
     * Reemplaza todo lo guardado ("todo o nada").
     *
     * @param comentarios lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Comentario> comentarios);
}
