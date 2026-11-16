/*
 * ResultadoEnvioSolicitud.java
 *
 * Qué hace: resultado de "Enviar solicitud". Puede ser una solicitud nueva pendiente o, si el otro
 * usuario ya nos había enviado una, una AMISTAD creada automáticamente.
 * Rol en la arquitectura: lo devuelve ServicioAmistades.enviarSolicitud; la UI decide qué mensaje
 * mostrar ("Solicitud enviada" o "¡Ahora son amigos!").
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorAmistades, PanelBuscador, VentanaPerfil.
 */
package una.social.amistades;

/**
 * Resultado de enviar una solicitud. Exactamente UNO de los dos campos tiene valor.
 *
 * @param solicitud solicitud que quedó pendiente (o {@code null} si se aceptó automáticamente)
 * @param amistad   amistad creada automáticamente (o {@code null} si quedó pendiente)
 */
public record ResultadoEnvioSolicitud(SolicitudAmistad solicitud, Amistad amistad) {

    /**
     * Comprueba que exactamente uno de los dos valores exista (invariante del record).
     *
     * @param solicitud solicitud pendiente o nula
     * @param amistad   amistad creada o nula
     */
    public ResultadoEnvioSolicitud {
        if ((solicitud == null) == (amistad == null)) {
            throw new IllegalArgumentException("Debe haber una solicitud pendiente o una amistad, no ambas ni ninguna");
        }
    }

    /**
     * @return {@code true} si el otro usuario ya había enviado una solicitud y ahora son amigos
     */
    public boolean seAceptoAutomaticamente() {
        return amistad != null;
    }
}
