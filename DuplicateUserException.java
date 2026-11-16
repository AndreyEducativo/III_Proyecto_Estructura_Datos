/**
 * Excepción para usuarios duplicados durante el registro.
 */
package una.social.exceptions;

public class DuplicateUserException extends SocialUnaException {
    public DuplicateUserException(String message) {
        super(message);
    }
}
