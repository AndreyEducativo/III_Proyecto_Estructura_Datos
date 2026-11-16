/**
 * Utilidades compartidas para la lectura y escritura de archivos de texto delimitados.
 *
 * <p>Estas funciones encapsulan la lógica de escape de campos, lectura de líneas y escritura
 * atómica para que Sebastián pueda reutilizar la misma convención de serialización.</p>
 */
package una.social.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class TextFileStore {
    private TextFileStore() {
        // Clase utilitaria sin instancias.
    }

    public static List<String> readAllLines(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return List.of();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank() && !line.startsWith("#")) {
                result.add(line);
            }
        }
        return result;
    }

    public static void writeLinesAtomic(Path path, List<String> lines) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Path tempFile = Files.createTempFile(parent == null ? Path.of(".") : parent, "social-una-", ".tmp");
        Files.write(tempFile, lines, StandardCharsets.UTF_8);
        try {
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String escapeField(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '|' -> builder.append("\\|");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String unescapeField(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (char ch : value.toCharArray()) {
            if (escaped) {
                switch (ch) {
                    case '\\' -> builder.append('\\');
                    case '|' -> builder.append('|');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    default -> builder.append(ch);
                }
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String[] splitEscapedRecord(String record) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < record.length(); i++) {
            char ch = record.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                current.append(ch);
                escaped = true;
            } else if (ch == '|') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
