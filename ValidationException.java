/**
 * Excepción lanzada cuando la entrada del usuario o de un archivo no cumple reglas del sistema.
 */
package una.social.exceptions;

public class ValidationException extends SocialUnaException {
    public ValidationException(String message) {
        super(message);
    }
}
