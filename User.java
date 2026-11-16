/**
 * Modelo del usuario de la red social.
 *
 * <p>Representa un usuario del sistema con un identificador único, datos personales y los
 * metadatos necesarios para autenticarlo y persistirlo en archivos de texto.</p>
 */
package una.social.model;

import java.time.Instant;

public class User {
    private final String id;
    private final String nombre;
    private final String usuario;
    private final String passwordHash;
    private final String passwordSalt;
    private final Instant fechaRegistro;

    public User(String id, String nombre, String usuario, String passwordHash, String passwordSalt, Instant fechaRegistro) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.fechaRegistro = fechaRegistro;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return java.util.Objects.equals(id, user.id)
                && java.util.Objects.equals(nombre, user.nombre)
                && java.util.Objects.equals(usuario, user.usuario)
                && java.util.Objects.equals(passwordHash, user.passwordHash)
                && java.util.Objects.equals(passwordSalt, user.passwordSalt)
                && java.util.Objects.equals(fechaRegistro, user.fechaRegistro);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, nombre, usuario, passwordHash, passwordSalt, fechaRegistro);
    }
}
