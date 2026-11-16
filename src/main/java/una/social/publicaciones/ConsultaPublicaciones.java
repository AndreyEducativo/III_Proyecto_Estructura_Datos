/*
 * ConsultaPublicaciones.java
 *
 * Qué hace: contrato MÍNIMO de solo lectura sobre publicaciones: si existe, quién es su autor y si
 * un usuario puede verla.
 * Rol en la arquitectura: lo usan los módulos de me gusta y comentarios para validar que solo se
 * interactúe con publicaciones visibles.
 * Principio SOLID: ISP (interfaz pequeña) y DIP.
 * Colabora con: GestorPublicaciones (implementación), GestorMeGusta, GestorComentarios.
 */
package una.social.publicaciones;

import java.util.Optional;

/**
 * Consulta de publicaciones (solo lectura).
 */
public interface ConsultaPublicaciones {

    /**
     * @param idPublicacion ID buscado
     * @return ID del autor, o vacío si la publicación no existe
     */
    Optional<String> autorDe(String idPublicacion);

    /**
     * Regla de visibilidad: no hay bloqueo entre el usuario y el autor, y la publicación es pública,
     * o el usuario es el autor, o es amigo del autor.
     *
     * @param idUsuario     usuario que quiere verla
     * @param idPublicacion publicación
     * @return {@code true} si existe y el usuario puede verla
     */
    boolean puedeVer(String idUsuario, String idPublicacion);
}
