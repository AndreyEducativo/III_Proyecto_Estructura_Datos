/*
 * ConsultaAmistades.java
 *
 * Qué hace: contrato MÍNIMO de solo lectura sobre las amistades: "¿quiénes son los amigos de X?"
 * y "¿son amigos X e Y?".
 * Rol en la arquitectura: el módulo de publicaciones solo necesita saber quiénes son los amigos
 * para armar el muro; por eso depende de esta interfaz pequeña y no del servicio completo.
 * Principio SOLID: ISP (interfaz pequeña y específica) y DIP (publicaciones depende de una
 * abstracción, no de GestorAmistades).
 * Colabora con: ServicioAmistades (la extiende), GestorPublicaciones, BuscadorUsuarios.
 */
package una.social.amistades;

import java.util.Set;

/**
 * Consulta de amistades (solo lectura).
 */
public interface ConsultaAmistades {

    /**
     * @param idUsuario usuario consultado
     * @return IDs de sus amigos (conjunto no modificable; vacío si no tiene)
     */
    Set<String> idsAmigos(String idUsuario);

    /**
     * @param idA un usuario
     * @param idB otro usuario
     * @return {@code true} si son amigos
     */
    boolean sonAmigos(String idA, String idB);
}
