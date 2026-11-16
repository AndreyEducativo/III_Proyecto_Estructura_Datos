/*
 * ServicioSugerencias.java
 *
 * Qué hace: contrato de "Personas que quizás conozcas" y de "amigos en común".
 * Rol en la arquitectura: lo usan el PanelBuscador (sugerencias) y la VentanaPerfil (amigos en
 * común).
 * Principio SOLID: ISP (solo dos consultas) y DIP.
 * Colabora con: SugeridorAmistades (implementación), PanelBuscador, VentanaPerfil.
 */
package una.social.amistades;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Sugerencias de amistad.
 */
public interface ServicioSugerencias {

    /** Cantidad máxima de sugerencias que se muestran. */
    int MAXIMO_SUGERENCIAS = 10;

    /**
     * Sugiere amigos de mis amigos que todavía no son mis amigos, ordenados por cantidad de amigos
     * en común (mayor primero) y luego alfabéticamente. Excluye al propio usuario, a sus amigos, a
     * los usuarios con bloqueo y a quienes ya tienen una solicitud pendiente con él.
     *
     * @param usuario usuario activo
     * @return como máximo {@link #MAXIMO_SUGERENCIAS} sugerencias (lista no modificable)
     */
    List<Sugerencia> sugerir(Usuario usuario);

    /**
     * @param idA un usuario
     * @param idB otro usuario
     * @return cantidad de amigos que A y B tienen en común
     */
    int amigosEnComun(String idA, String idB);
}
