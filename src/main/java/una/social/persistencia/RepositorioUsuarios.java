/*
 * RepositorioUsuarios.java
 *
 * Qué hace: contrato para leer y guardar el catálogo de usuarios en un almacenamiento.
 * Rol en la arquitectura: el GestorUsuarios depende de esta interfaz y no sabe si los datos
 * están en un .txt, en memoria (pruebas) o en otro formato.
 * Principio SOLID: DIP (el servicio depende de esta abstracción), ISP (solo dos operaciones),
 * OCP (otro formato de archivo = otra implementación, sin tocar el servicio) y LSP (cualquier
 * implementación debe respetar lo que dice este Javadoc).
 * Colabora con: RepositorioUsuariosArchivoTexto, GestorUsuarios, Usuario.
 */
package una.social.persistencia;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Persistencia del catálogo de usuarios.
 *
 * <p>Solo se ocupa de PERSISTIR (leer y escribir). Las búsquedas durante la ejecución se hacen
 * en memoria, en el {@code CatalogoUsuarios}; así cada clase tiene una sola responsabilidad.</p>
 */
public interface RepositorioUsuarios {

    /**
     * Lee todos los usuarios guardados.
     *
     * <p>Contrato: los registros dañados se OMITEN y se auditan; nunca hacen fallar la carga
     * completa. Si el almacenamiento no existe, se crea vacío.</p>
     *
     * @return usuarios válidos, en el orden en que estaban guardados
     * @throws una.social.excepciones.ExcepcionPersistencia si el almacenamiento no se puede leer
     */
    List<Usuario> cargarTodos();

    /**
     * Reemplaza todo lo guardado por la lista dada. Debe ser "todo o nada": si falla, lo que
     * estaba guardado antes queda intacto.
     *
     * @param usuarios lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarTodos(List<Usuario> usuarios);
}
