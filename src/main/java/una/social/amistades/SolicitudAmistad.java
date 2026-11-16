/*
 * SolicitudAmistad.java
 *
 * Qué hace: representa una solicitud de amistad PENDIENTE: un usuario (remitente) pidió ser amigo
 * de otro (destinatario) y este todavía no responde.
 * Rol en la arquitectura: modelo del módulo de amistades. Se guarda como una línea de
 * data/solicitudes.txt. Al aceptarla se convierte en una Amistad; al rechazarla o cancelarla se
 * elimina.
 * Principio SOLID: SRP (solo datos; record inmutable).
 * Colabora con: GestorAmistades, RepositorioSolicitudesArchivoTexto.
 */
package una.social.amistades;

import java.time.Instant;
import java.util.Objects;

/**
 * Solicitud de amistad pendiente. A diferencia de la amistad, SÍ tiene dirección: importa quién
 * la envió y quién la recibe.
 *
 * @param idRemitente    ID del usuario que envió la solicitud
 * @param idDestinatario ID del usuario que la recibe y debe aceptarla o rechazarla
 * @param fecha          momento en que se envió
 */
public record SolicitudAmistad(String idRemitente, String idDestinatario, Instant fecha) {

    /**
     * Valida que no haya valores nulos (errores de programación: los datos externos se validan
     * antes, en el repositorio y en el gestor).
     *
     * @param idRemitente    ID del remitente
     * @param idDestinatario ID del destinatario
     * @param fecha          fecha de envío
     */
    public SolicitudAmistad {
        Objects.requireNonNull(idRemitente, "idRemitente");
        Objects.requireNonNull(idDestinatario, "idDestinatario");
        Objects.requireNonNull(fecha, "fecha");
    }

    /**
     * @param remitente    ID del posible remitente
     * @param destinatario ID del posible destinatario
     * @return {@code true} si esta solicitud va exactamente de {@code remitente} a {@code destinatario}
     */
    public boolean va(String remitente, String destinatario) {
        return idRemitente.equals(remitente) && idDestinatario.equals(destinatario);
    }
}
