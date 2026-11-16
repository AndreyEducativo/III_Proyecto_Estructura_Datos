/*
 * DirectorioUsuarios.java
 *
 * Qué hace: contrato de SOLO LECTURA del catálogo de usuarios: buscar por ID, por nombre de
 * usuario y listar todos.
 * Rol en la arquitectura: contrato compartido. Es lo ÚNICO que el módulo de Sebastián necesita
 * del catálogo (buscador de usuarios, mostrar el nombre de un amigo o del autor de una
 * publicación). Como no tiene métodos de escritura, su módulo no puede alterar usuarios por error.
 * Principio SOLID: ISP (interfaz pequeña y específica) y DIP (los módulos dependen de esta
 * abstracción y no de GestorUsuarios).
 * Colabora con: GestorUsuarios (implementación), Usuario.
 */
package una.social.servicio;

import java.util.List;
import java.util.Optional;
import una.social.modelo.Usuario;

/**
 * Consulta de usuarios registrados (solo lectura).
 */
public interface DirectorioUsuarios {

    /**
     * Busca un usuario por su ID exacto (por ejemplo "U001").
     *
     * @param id ID a buscar (nulo devuelve vacío)
     * @return el usuario si existe
     */
    Optional<Usuario> buscarPorId(String id);

    /**
     * Busca un usuario por nombre de usuario, sin distinguir mayúsculas ("Ana" = "ana").
     *
     * @param nombreUsuario nombre a buscar (nulo devuelve vacío)
     * @return el usuario si existe
     */
    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario);

    /**
     * Lista todos los usuarios en orden de registro.
     *
     * @return lista NO modificable (intentar cambiarla lanza UnsupportedOperationException)
     */
    List<Usuario> listarTodos();
}
