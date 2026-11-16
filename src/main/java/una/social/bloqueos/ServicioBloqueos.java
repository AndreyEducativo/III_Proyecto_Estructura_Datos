/*
 * ServicioBloqueos.java
 *
 * Qué hace: contrato para bloquear, desbloquear y listar usuarios bloqueados.
 * Rol en la arquitectura: lo usan la ventana de perfil (botón "Bloquear") y el diálogo de
 * bloqueados ("Desbloquear").
 * Principio SOLID: ISP (extiende la consulta mínima solo con lo que la UI necesita) y DIP.
 * Colabora con: GestorBloqueos (implementación), VentanaPerfil, DialogoBloqueados.
 */
package una.social.bloqueos;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones de bloqueo.
 */
public interface ServicioBloqueos extends ConsultaBloqueos {

    /**
     * Bloquea a otro usuario: se guarda en disco y además se elimina la amistad y se cancelan las
     * solicitudes entre ambos (a través de los oyentes).
     *
     * @param bloqueador usuario activo
     * @param idBloqueado ID del usuario a bloquear
     * @throws una.social.excepciones.ExcepcionAmistadInvalida si es él mismo, no existe o ya estaba bloqueado
     * @throws una.social.excepciones.ExcepcionPersistencia    si no se pudo guardar
     */
    void bloquear(Usuario bloqueador, String idBloqueado);

    /**
     * Quita un bloqueo hecho por el usuario activo. No restaura la amistad: si quieren volver a
     * ser amigos, deben enviarse una solicitud nueva.
     *
     * @param bloqueador  usuario activo (quien había bloqueado)
     * @param idBloqueado ID del usuario a desbloquear
     */
    void desbloquear(Usuario bloqueador, String idBloqueado);

    /**
     * @param idBloqueador usuario activo
     * @param idBloqueado  otro usuario
     * @return {@code true} si el usuario activo bloqueó al otro (solo en ese sentido)
     */
    boolean bloqueoA(String idBloqueador, String idBloqueado);

    /**
     * @param idBloqueador usuario activo
     * @return usuarios que bloqueó, en el orden en que los bloqueó (lista no modificable)
     */
    List<Usuario> listarBloqueados(String idBloqueador);
}
