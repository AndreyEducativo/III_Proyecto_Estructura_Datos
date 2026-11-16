/**
 * Herramienta para generar usuarios de prueba del sistema.
 *
 * <p>Se usa para crear la base inicial de datos con los IDs y las credenciales documentadas en
 * el README. El archivo generado sigue la convención de texto y escape del proyecto.</p>
 */
package una.social;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import una.social.model.User;
import una.social.persistence.TextFileUserRepository;
import una.social.security.PasswordUtil;

public final class SeedDataTool {
    private SeedDataTool() {
    }

    public static void generate(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
            Path usersPath = dataDir.resolve("usuarios.txt");
            TextFileUserRepository repository = new TextFileUserRepository(usersPath);
            if (!repository.findAll().isEmpty()) {
                return;
            }
            List<User> sampleUsers = List.of(
                    createUser("U001", "Ana Mora", "ana", "ana12345"),
                    createUser("U002", "Luis Rojas", "luis", "luis12345"),
                    createUser("U003", "Maria Vargas", "maria", "maria12345"));
            repository.saveAll(sampleUsers);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el archivo de usuarios de prueba.", ex);
        }
    }

    public static void main(String[] args) {
        generate(Path.of("data"));
        System.out.println("Se generó la base de usuarios de prueba en data/usuarios.txt");
    }

    private static User createUser(String id, String nombre, String usuario, String password) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        return new User(id, nombre, usuario, hash, salt, Instant.now());
    }
}
