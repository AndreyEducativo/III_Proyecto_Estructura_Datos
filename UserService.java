/**
 * Servicio de negocio para gestionar usuarios y autenticación.
 *
 * <p>Esta clase cumple la responsabilidad de validar datos, crear usuarios con hash seguro,
 * autenticar credenciales y mantener el bloqueo temporal por intentos fallidos.</p>
 */
package una.social.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import una.social.exceptions.AuthenticationException;
import una.social.exceptions.DuplicateUserException;
import una.social.exceptions.ValidationException;
import una.social.model.User;
import una.social.persistence.UserRepository;
import una.social.security.PasswordUtil;

public class UserService implements UserDirectory {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 5;
    private final UserRepository repository;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Instant> blockedUntil = new HashMap<>();

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User register(String nombre, String usuario, String password, String confirmPassword) {
        validateRegisterData(nombre, usuario, password, confirmPassword);
        String normalizedUsername = usuario.trim();
        String lower = normalizedUsername.toLowerCase();
        if (repository.findByUsername(lower).isPresent()) {
            throw new DuplicateUserException("El usuario ya existe.");
        }
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        User user = new User(generateNextId(), nombre.trim(), normalizedUsername, hash, salt, Instant.now());
        repository.save(user);
        return user;
    }

    public User authenticate(String usuario, String password) {
        if (usuario == null || password == null) {
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        String lower = usuario.trim().toLowerCase();
        if (isBlocked(lower)) {
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        Optional<User> existing = repository.findByUsername(lower);
        if (existing.isEmpty()) {
            registerFailedAttempt(lower);
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        User user = existing.get();
        boolean valid = PasswordUtil.validatePassword(password, user.getPasswordSalt(), user.getPasswordHash());
        if (!valid) {
            registerFailedAttempt(lower);
            throw new AuthenticationException("Usuario o contraseña incorrectos");
        }
        resetFailedAttempts(lower);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username == null ? "" : username.trim().toLowerCase());
    }

    @Override
    public List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(repository.findAll()));
    }

    public boolean isBlocked(String username) {
        String key = username == null ? "" : username.trim().toLowerCase();
        Instant until = blockedUntil.get(key);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            blockedUntil.remove(key);
            failedAttempts.remove(key);
            return false;
        }
        return true;
    }

    private void registerFailedAttempt(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            blockedUntil.put(username, Instant.now().plusSeconds(BLOCK_MINUTES * 60L));
        }
    }

    private void resetFailedAttempts(String username) {
        failedAttempts.remove(username);
        blockedUntil.remove(username);
    }

    private void validateRegisterData(String nombre, String usuario, String password, String confirmPassword) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidationException("El nombre es obligatorio.");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new ValidationException("El usuario es obligatorio.");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("La contraseña es obligatoria.");
        }
        if (password.length() < 6) {
            throw new ValidationException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            throw new ValidationException("La confirmación de contraseña no coincide.");
        }
    }

    private String generateNextId() {
        List<User> users = findAll();
        int max = 0;
        for (User user : users) {
            String id = user.getId();
            if (id != null && id.startsWith("U")) {
                try {
                    int number = Integer.parseInt(id.substring(1));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException ignored) {
                    // Se ignora un ID corrupto para continuar con la lógica.
                }
            }
        }
        return String.format("U%03d", max + 1);
    }
}
