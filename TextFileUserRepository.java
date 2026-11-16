/**
 * Implementación del repositorio de usuarios usando archivos de texto delimitados.
 *
 * <p>Este repositorio cumple la conversión de objetos User a texto plano y viceversa, con
 * manejo de líneas corruptas y escritura atómica para minimizar pérdida de información.</p>
 */
package una.social.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import una.social.exceptions.DataCorruptionException;
import una.social.model.User;
import una.social.service.UserDirectory;

public class TextFileUserRepository implements UserRepository {
    private final Path filePath;
    private final List<User> users = new ArrayList<>();
    private final java.util.Map<String, User> byUsername = new ConcurrentHashMap<>();

    public TextFileUserRepository(Path filePath) {
        this.filePath = filePath;
        if (filePath == null) {
            throw new IllegalArgumentException("La ruta del archivo de usuarios no puede ser nula.");
        }
        load();
    }

    @Override
    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        List<User> all = new ArrayList<>(users);
        boolean exists = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(user.getId())) {
                all.set(i, user);
                exists = true;
                break;
            }
        }
        if (!exists) {
            all.add(user);
        }
        saveAll(all);
    }

    @Override
    public void saveAll(List<User> usersToPersist) {
        List<String> lines = new ArrayList<>();
        lines.add("# id|nombre|usuario|saltBase64|hashBase64|fechaISO");
        for (User user : usersToPersist) {
            lines.add(serialize(user));
        }
        try {
            TextFileStore.writeLinesAtomic(filePath, lines);
            this.users.clear();
            this.users.addAll(usersToPersist);
            this.byUsername.clear();
            for (User user : this.users) {
                this.byUsername.put(user.getUsuario().toLowerCase(), user);
            }
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar el catálogo de usuarios en disco.", ex);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String key = username.trim().toLowerCase();
        return Optional.ofNullable(byUsername.get(key));
    }

    @Override
    public List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(users));
    }

    public void load() {
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                return;
            }
            List<String> rawLines = TextFileStore.readAllLines(filePath);
            this.users.clear();
            this.byUsername.clear();
            for (String line : rawLines) {
                User parsed = parseLine(line);
                if (parsed != null) {
                    this.users.add(parsed);
                    this.byUsername.put(parsed.getUsuario().toLowerCase(), parsed);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo cargar el catálogo de usuarios.", ex);
        }
    }

    private User parseLine(String line) {
        try {
            String[] parts = TextFileStore.splitEscapedRecord(line);
            if (parts.length != 6) {
                throw new DataCorruptionException("Línea de usuarios corrupta: " + line);
            }
            String id = TextFileStore.unescapeField(parts[0]);
            String nombre = TextFileStore.unescapeField(parts[1]);
            String usuario = TextFileStore.unescapeField(parts[2]);
            String salt = TextFileStore.unescapeField(parts[3]);
            String hash = TextFileStore.unescapeField(parts[4]);
            String fecha = TextFileStore.unescapeField(parts[5]);
            if (id.isBlank() || nombre.isBlank() || usuario.isBlank() || salt.isBlank() || hash.isBlank()) {
                throw new DataCorruptionException("Registro incompleto de usuario");
            }
            return new User(id, nombre, usuario, hash, salt, Instant.parse(fecha));
        } catch (DataCorruptionException | RuntimeException ex) {
            System.err.println("Se omite una línea corrupta en usuarios.txt: " + ex.getMessage());
            return null;
        }
    }

    private String serialize(User user) {
        return String.join("|",
                TextFileStore.escapeField(user.getId()),
                TextFileStore.escapeField(user.getNombre()),
                TextFileStore.escapeField(user.getUsuario()),
                TextFileStore.escapeField(user.getPasswordSalt()),
                TextFileStore.escapeField(user.getPasswordHash()),
                TextFileStore.escapeField(user.getFechaRegistro().toString()));
    }
}
