/*
 * ServicioAmistades.java
 *
 * Qué hace: contrato del módulo de amistades: agregar un amigo y listar los amigos.
 * Rol en la arquitectura: la UI (PanelBuscador, PanelAmigos) depende de esta interfaz.
 * Principio SOLID: ISP (extiende la consulta mínima y agrega solo lo que la UI necesita) y DIP.
 * Colabora con: GestorAmistades (implementación), PanelBuscador, PanelAmigos.
 */
package una.social.amistades;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones de amistades.
 */
public interface ServicioAmistades extends ConsultaAmistades {

    /**
     * "Agregar como amigo": crea una amistad mutua entre el solicitante y otro usuario y la
     * guarda inmediatamente en disco.
     *
     * @param solicitante  usuario que pulsa el botón (el usuario activo)
     * @param idNuevoAmigo ID del usuario a agregar
     * @return la amistad creada
     * @throws una.social.excepciones.ExcepcionAmistadInvalida si es él mismo, ya son amigos o el
     *                                                         usuario no existe
     * @throws una.social.excepciones.ExcepcionPersistencia    si no se pudo guardar
     */
    Amistad agregarAmistad(Usuario solicitante, String idNuevoAmigo);

    /**
     * Lista los amigos de un usuario, ordenados alfabéticamente por nombre.
     *
     * @param idUsuario usuario consultado
     * @return lista no modificable de amigos con su fecha de amistad
     */
    List<Amigo> listarAmigos(String idUsuario);
}
