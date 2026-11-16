import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import una.social.exceptions.AuthenticationException;
import una.social.exceptions.DuplicateUserException;
import una.social.exceptions.ValidationException;
import una.social.model.User;
import una.social.persistence.TextFileUserRepository;
import una.social.service.UserService;

class UserServiceTest {
    @Test
    void registerAndAuthenticateShouldWork() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-users");
        Path usersPath = tempDir.resolve("usuarios.txt");
        TextFileUserRepository repository = new TextFileUserRepository(usersPath);
        UserService service = new UserService(repository);

        User created = service.register("Ana Mora", "ana", "ana12345", "ana12345");

        assertEquals("U001", created.getId());
        assertEquals("ana", created.getUsuario());
        assertNotEquals("ana12345", created.getPasswordHash());
        assertEquals(created, repository.findByUsername("ana").orElse(null));

        User authenticated = service.authenticate("ana", "ana12345");
        assertEquals(created.getId(), authenticated.getId());
    }

    @Test
    void invalidLoginShouldThrowAuthenticationException() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-login");
        TextFileUserRepository repo = new TextFileUserRepository(tempDir.resolve("usuarios.txt"));
        UserService service = new UserService(repo);
        service.register("Luis Rojas", "luis", "luis12345", "luis12345");

        assertThrows(AuthenticationException.class, () -> service.authenticate("luis", "clave-mala"));
    }

    @Test
    void duplicateUsernameShouldThrow() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-dupe");
        TextFileUserRepository repo = new TextFileUserRepository(tempDir.resolve("usuarios.txt"));
        UserService service = new UserService(repo);
        service.register("Maria Vargas", "maria", "maria12345", "maria12345");

        assertThrows(DuplicateUserException.class, () -> service.register("Maria Vargas", "MARIA", "otra123", "otra123"));
    }

    @Test
    void invalidDataShouldThrowValidationException() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-validation");
        TextFileUserRepository repo = new TextFileUserRepository(tempDir.resolve("usuarios.txt"));
        UserService service = new UserService(repo);

        assertThrows(ValidationException.class, () -> service.register("", "usuario", "pass123", "pass123"));
        assertThrows(ValidationException.class, () -> service.register("Test User", "u", "pass123", "pass123"));
        assertThrows(ValidationException.class, () -> service.register("Test User", "usuario", "123", "123"));
    }

    @Test
    void repositoryShouldPersistAndReload() throws Exception {
        Path tempDir = Files.createTempDirectory("social-una-persist");
        Path usersPath = tempDir.resolve("usuarios.txt");
        TextFileUserRepository repo1 = new TextFileUserRepository(usersPath);
        UserService service = new UserService(repo1);
        service.register("Ana Mora", "ana", "ana12345", "ana12345");

        TextFileUserRepository repo2 = new TextFileUserRepository(usersPath);
        assertFalse(repo2.findAll().isEmpty());
        assertTrue(repo2.findByUsername("ana").isPresent());
    }
}
