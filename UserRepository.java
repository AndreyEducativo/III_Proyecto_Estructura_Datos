/**
 * Repositorio de usuarios para persistencia y consulta con archivos de texto.
 */
package una.social.persistence;

import java.util.List;
import java.util.Optional;
import una.social.model.User;
import una.social.service.UserDirectory;

public interface UserRepository extends UserDirectory {
    void save(User user);

    void saveAll(List<User> users);

    Optional<User> findByUsername(String username);

    List<User> findAll();
}
