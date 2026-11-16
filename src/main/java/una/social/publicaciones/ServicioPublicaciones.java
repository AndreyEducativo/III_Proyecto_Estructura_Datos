/*
 * ServicioPublicaciones.java
 *
 * Qué hace: contrato del módulo de publicaciones: publicar un mensaje y obtener el muro.
 * Rol en la arquitectura: el PanelMuro depende solo de esta interfaz.
 * Principio SOLID: ISP (dos operaciones) y DIP.
 * Colabora con: GestorPublicaciones (implementación), PanelMuro.
 */
package una.social.publicaciones;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones del muro.
 */
public interface ServicioPublicaciones {

    /**
     * Publica un mensaje de texto y lo guarda inmediatamente en disco.
     *
     * @param autor usuario que publica (el usuario activo)
     * @param texto mensaje (se quitan los espacios sobrantes al inicio y al final)
     * @return la publicación creada, con su ID y fecha
     * @throws una.social.excepciones.ExcepcionPublicacionInvalida si el texto está vacío, es
     *                                                             demasiado largo o el autor no existe
     * @throws una.social.excepciones.ExcepcionPersistencia        si no se pudo guardar
     */
    Publicacion publicar(Usuario autor, String texto);

    /**
     * Arma el muro del usuario: sus publicaciones y las de sus amigos, de la más reciente a la
     * más antigua.
     *
     * @param usuario usuario dueño del muro
     * @return lista no modificable de entradas del muro
     */
    List<EntradaMuro> obtenerMuro(Usuario usuario);
}
