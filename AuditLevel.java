/**
 * Niveles de auditoría permitidos por el proyecto.
 *
 * <p>La auditoría se mantiene como append-only y cada evento se registra con el nivel
 * correspondiente para facilitar la revisión operativa y la depuración.</p>
 */
package una.social.audit;

public enum AuditLevel {
    INFO,
    WARN,
    ERROR,
    TEST
}
