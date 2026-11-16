/**
 * Contrato para registrar eventos de auditoría en el sistema Social UNA.
 *
 * <p>La intención de este contrato es evitar que la lógica de negocio tenga dependencias
 * concretas de la implementación real del archivo de auditoría. Así, la UI, el servicio de
 * usuarios y las pruebas pueden emitir eventos sin conocer detalles de almacenamiento.</p>
 */
package una.social.audit;

public interface AuditLogger {
    void log(AuditLevel nivel, String actor, String accion, String detalle);
}
