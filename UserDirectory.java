/**
 * Contrato de lectura del catálogo de usuarios.
 *
 * <p>El contrato define solo operaciones de consulta para que la capa de UI o la capa de
 * negocio pueda depender de una abstracción sin conocer la implementación concreta del
 * repositorio.</p>
 */
package una.social.service;

import java.util.List;
import java.util.Optional;
import una.social.model.User;

public interface UserDirectory {
    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    List<User> findAll();
}
