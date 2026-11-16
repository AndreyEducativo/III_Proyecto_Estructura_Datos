/**
 * Excepción base del dominio Social UNA.
 *
 * <p>Esta clase actúa como raíz para todas las excepciones controladas del proyecto y
 * permite que la capa de UI y la capa de servicios manejen errores de forma uniforme.
 * Además, se usa en la auditoría para registrar el tipo y el mensaje sin exponer detalle
 * sensible a los usuarios finales.</p>
 *
 * <p>Principio SOLID aplicado: SRP y LSP. La responsabilidad del dominio de errores está
 * centralizada en esta jerarquía, y las subclases pueden sustituir a la base sin romper
 * la lógica de manejo de errores.</p>
 */
package una.social.exceptions;

public class SocialUnaException extends RuntimeException {
    public SocialUnaException(String message) {
        super(message);
    }

    public SocialUnaException(String message, Throwable cause) {
        super(message, cause);
    }
}
