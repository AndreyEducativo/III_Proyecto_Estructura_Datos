/**
 * Excepción usada para errores de autenticación y acceso.
 */
package una.social.exceptions;

public class AuthenticationException extends SocialUnaException {
    public AuthenticationException(String message) {
        super(message);
    }
}
