/**
 * Excepción usada para marcar registros corruptos que deben ignorarse con auditoría.
 */
package una.social.exceptions;

public class DataCorruptionException extends SocialUnaException {
    public DataCorruptionException(String message) {
        super(message);
    }
}
