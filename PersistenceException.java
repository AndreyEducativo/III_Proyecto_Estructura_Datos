/**
 * Excepción usada para fallos en lectura o escritura de archivos y persistencia.
 */
package una.social.exceptions;

public class PersistenceException extends SocialUnaException {
    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
