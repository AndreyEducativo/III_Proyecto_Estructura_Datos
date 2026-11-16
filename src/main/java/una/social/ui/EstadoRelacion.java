/*
 * EstadoRelacion.java
 *
 * Qué hace: describe la relación del usuario activo con otro usuario (él mismo, amigos, solicitud
 * enviada, solicitud recibida o ninguna) para decidir qué botón mostrar.
 * Rol en la arquitectura: lo usan el buscador, las sugerencias y la ventana de perfil, junto con
 * AccionesAmistad.componenteEstado.
 * Principio SOLID: SRP (solo clasifica la relación) y evita repetir los mismos "if" en cada panel.
 * Colabora con: ResultadoBusqueda, ConsultaAmistades, ConsultaSolicitudes, AccionesAmistad.
 */
package una.social.ui;

import una.social.amistades.ResultadoBusqueda;
import una.social.amistades.ServicioAmistades;

/**
 * Relación entre el usuario activo y otro usuario.
 */
enum EstadoRelacion {
    /** Es el propio usuario activo. */
    UNO_MISMO,
    /** Ya son amigos. */
    AMIGOS,
    /** El usuario activo le envió una solicitud pendiente. */
    SOLICITUD_ENVIADA,
    /** El otro usuario le envió una solicitud pendiente al usuario activo. */
    SOLICITUD_RECIBIDA,
    /** Ninguna relación: se puede enviar una solicitud. */
    NINGUNA;

    /**
     * @param resultado resultado del buscador
     * @return la relación que indica
     */
    static EstadoRelacion de(ResultadoBusqueda resultado) {
        if (resultado.esElMismo()) {
            return UNO_MISMO;
        }
        if (resultado.esAmigo()) {
            return AMIGOS;
        }
        if (resultado.solicitudRecibida()) {
            return SOLICITUD_RECIBIDA;
        }
        return resultado.solicitudEnviada() ? SOLICITUD_ENVIADA : NINGUNA;
    }

    /**
     * Calcula la relación consultando el servicio (se debe llamar fuera del hilo de Swing).
     *
     * @param amistades servicio de amistades
     * @param yo        ID del usuario activo
     * @param otro      ID del otro usuario
     * @return la relación actual
     */
    static EstadoRelacion calcular(ServicioAmistades amistades, String yo, String otro) {
        if (yo.equals(otro)) {
            return UNO_MISMO;
        }
        if (amistades.sonAmigos(yo, otro)) {
            return AMIGOS;
        }
        if (amistades.haySolicitudPendiente(otro, yo)) {
            return SOLICITUD_RECIBIDA;
        }
        return amistades.haySolicitudPendiente(yo, otro) ? SOLICITUD_ENVIADA : NINGUNA;
    }
}
