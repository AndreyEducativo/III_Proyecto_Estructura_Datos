/*
 * Perfil.java
 *
 * Qué hace: datos extra del perfil de un usuario que no están en usuarios.txt (por ahora, la
 * biografía). Es una línea de data/perfiles.txt.
 * Rol en la arquitectura: modelo del módulo de perfiles. Se guarda aparte para NO cambiar el formato
 * de usuarios.txt.
 * Principio SOLID: SRP (record inmutable de solo datos) y OCP (el perfil crece sin tocar Usuario).
 * Colabora con: GestorPerfiles, RepositorioPerfilesArchivoTexto.
 */
package una.social.perfiles;

import java.util.Objects;

/**
 * Datos de perfil de un usuario.
 *
 * @param idUsuario  usuario dueño del perfil
 * @param biografia  texto de presentación (máximo {@link #LARGO_MAXIMO_BIOGRAFIA} caracteres)
 */
public record Perfil(String idUsuario, String biografia) {

    /** Largo máximo de la biografía. */
    public static final int LARGO_MAXIMO_BIOGRAFIA = 160;

    /**
     * Valida que no haya nulos (error de programación: los datos externos se validan antes).
     *
     * @param idUsuario usuario
     * @param biografia biografía
     */
    public Perfil {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(biografia, "biografia");
    }
}
