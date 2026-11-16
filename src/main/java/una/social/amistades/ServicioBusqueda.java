/*
 * ServicioBusqueda.java
 *
 * Qué hace: contrato del buscador de usuarios registrados.
 * Rol en la arquitectura: el PanelBuscador depende solo de esta interfaz.
 * Principio SOLID: ISP (una sola operación) y DIP.
 * Colabora con: BuscadorUsuarios (implementación), PanelBuscador.
 */
package una.social.amistades;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Buscador de usuarios registrados.
 */
public interface ServicioBusqueda {

    /**
     * Busca usuarios cuyo nombre completo, nombre de usuario o ID contenga el texto, sin
     * distinguir mayúsculas ni tildes ("maría" encuentra a "Maria").
     *
     * @param texto       texto buscado; vacío devuelve todos los usuarios
     * @param solicitante usuario que busca (para marcar amigos y a sí mismo)
     * @return resultados ordenados alfabéticamente por nombre (lista no modificable)
     */
    List<ResultadoBusqueda> buscar(String texto, Usuario solicitante);
}
