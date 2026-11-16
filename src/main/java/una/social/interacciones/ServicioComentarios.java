/*
 * ServicioComentarios.java
 *
 * Qué hace: contrato de los comentarios: comentar, borrar, listar y contar.
 * Rol en la arquitectura: la tarjeta de publicación y el diálogo de comentarios dependen solo de
 * esta interfaz.
 * Principio SOLID: ISP y DIP.
 * Colabora con: GestorComentarios (implementación), TarjetaPublicacion, DialogoComentarios.
 */
package una.social.interacciones;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones de comentarios.
 */
public interface ServicioComentarios {

    /**
     * Comenta una publicación visible para el usuario. Se guarda inmediatamente en disco.
     *
     * @param autor         usuario activo
     * @param idPublicacion publicación a comentar
     * @param texto         texto (máximo {@link Comentario#LARGO_MAXIMO_TEXTO} caracteres)
     * @return el comentario creado
     * @throws una.social.excepciones.ExcepcionReglaNegocio si el texto no es válido o no puede ver la publicación
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    Comentario comentar(Usuario autor, String idPublicacion, String texto);

    /**
     * Borra un comentario. Pueden hacerlo su autor o el autor de la publicación.
     *
     * @param usuario      usuario activo
     * @param idComentario comentario a borrar
     */
    void borrar(Usuario usuario, String idComentario);

    /**
     * @param visitante     usuario activo
     * @param idPublicacion publicación
     * @return comentarios visibles para él (sin los de usuarios bloqueados), del más antiguo al
     *         más reciente; vacía si no puede ver la publicación
     */
    List<ComentarioVisible> listar(Usuario visitante, String idPublicacion);

    /**
     * @param visitante     usuario activo
     * @param idPublicacion publicación
     * @return cantidad de comentarios visibles para él
     */
    int contar(Usuario visitante, String idPublicacion);
}
