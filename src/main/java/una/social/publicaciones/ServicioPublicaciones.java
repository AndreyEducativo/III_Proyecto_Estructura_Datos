/*
 * ServicioPublicaciones.java
 *
 * Qué hace: contrato del módulo de publicaciones: publicar, ver los muros (general y de amigos) y
 * los perfiles, editar o deshacer la última publicación, borrar y compartir.
 * Rol en la arquitectura: la UI (PanelMuro, PanelMiPerfil, VentanaPerfil) depende solo de esta
 * interfaz.
 * Principio SOLID: ISP (extiende la consulta mínima solo con lo que la UI necesita) y DIP.
 * Colabora con: GestorPublicaciones (implementación) y la UI.
 */
package una.social.publicaciones;

import java.util.List;
import java.util.Optional;
import una.social.modelo.Usuario;

/**
 * Operaciones de publicaciones y muros.
 *
 * <p>Todos los métodos que cambian datos los guardan inmediatamente en disco y lanzan
 * {@link una.social.excepciones.ExcepcionPublicacionInvalida} si no se cumple una regla, o
 * {@link una.social.excepciones.ExcepcionPersistencia} si no se pudo guardar.</p>
 */
public interface ServicioPublicaciones extends ConsultaPublicaciones {

    /**
     * Publica un mensaje de texto.
     *
     * @param autor      usuario que publica (el usuario activo)
     * @param texto      mensaje (se quitan los espacios sobrantes al inicio y al final)
     * @param privacidad quién puede verla
     * @return la publicación creada, con su ID y fecha
     */
    Publicacion publicar(Usuario autor, String texto, Privacidad privacidad);

    /**
     * Publica un mensaje "Solo amigos" (la privacidad por defecto).
     *
     * @param autor usuario que publica
     * @param texto mensaje
     * @return la publicación creada
     */
    default Publicacion publicar(Usuario autor, String texto) {
        return publicar(autor, texto, Privacidad.SOLO_AMIGOS);
    }

    /**
     * Vista "Mis amigos": publicaciones del usuario y de sus amigos (públicas y solo amigos) y lo
     * que ellos compartieron, de lo más reciente a lo más antiguo. Sin nada de usuarios bloqueados.
     *
     * @param usuario usuario dueño del muro
     * @return lista no modificable de entradas
     */
    List<EntradaMuro> obtenerMuro(Usuario usuario);

    /**
     * Vista "Muro general": todas las publicaciones PÚBLICAS de todos los usuarios y todo lo
     * compartido, de lo más reciente a lo más antiguo. Sin nada de usuarios bloqueados.
     *
     * @param usuario usuario que mira el muro
     * @return lista no modificable de entradas
     */
    List<EntradaMuro> obtenerMuroGeneral(Usuario usuario);

    /**
     * Perfil de un usuario: SUS publicaciones visibles para quien mira (todas si es él mismo o un
     * amigo; solo las públicas si no) y SUS compartidos, de lo más reciente a lo más antiguo.
     *
     * @param visitante usuario que mira el perfil
     * @param idDueno   dueño del perfil
     * @return lista no modificable de entradas (vacía si hay un bloqueo entre ellos)
     */
    List<EntradaMuro> obtenerPerfil(Usuario visitante, String idDueno);

    /**
     * @param idAutor autor
     * @return su publicación más reciente (la única que puede editar), o vacío si no tiene
     */
    Optional<Publicacion> ultimaPublicacionDe(String idAutor);

    /**
     * Edita el texto de la publicación MÁS RECIENTE del autor (mismas validaciones que publicar).
     *
     * @param autor      usuario activo
     * @param textoNuevo texto nuevo
     * @return la publicación editada
     */
    Publicacion editarUltima(Usuario autor, String textoNuevo);

    /**
     * @param autor usuario activo
     * @return {@code true} si tiene publicaciones de esta sesión que se pueden deshacer
     */
    boolean puedeDeshacer(Usuario autor);

    /**
     * "Deshacer última publicación": saca de la PILA del usuario la última que publicó desde que se
     * abrió la aplicación y la borra (con sus me gusta y comentarios).
     *
     * @param autor usuario activo
     * @return la publicación borrada
     */
    Publicacion deshacerUltima(Usuario autor);

    /**
     * Borra una publicación propia (cualquiera). Se borran también sus me gusta y comentarios; lo
     * que otros compartieron de ella queda como "no disponible".
     *
     * @param autor         usuario activo (debe ser el autor)
     * @param idPublicacion publicación a borrar
     */
    void borrar(Usuario autor, String idPublicacion);

    /**
     * Comparte una publicación PÚBLICA de otra persona.
     *
     * @param usuario       usuario activo
     * @param idPublicacion publicación a compartir
     * @return el compartido creado
     */
    Compartido compartir(Usuario usuario, String idPublicacion);

    /**
     * Quita un compartido propio.
     *
     * @param usuario      usuario activo (debe ser quien compartió)
     * @param idCompartido compartido a quitar
     */
    void quitarCompartido(Usuario usuario, String idCompartido);
}
