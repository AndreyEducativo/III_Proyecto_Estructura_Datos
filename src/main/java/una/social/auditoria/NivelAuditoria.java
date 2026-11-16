/*
 * NivelAuditoria.java
 *
 * Qué hace: define la gravedad de cada evento de auditoría.
 * Rol en la arquitectura: contrato compartido; lo usan todos los módulos al auditar.
 * Principio SOLID: SRP (solo clasifica eventos).
 * Colabora con: RegistroAuditoria, RegistroAuditoriaArchivo, OyenteAuditoriaPruebas.
 */
package una.social.auditoria;

/**
 * Niveles de un evento de auditoría.
 */
public enum NivelAuditoria {
    /** Operación normal (inicio de la app, login exitoso, datos guardados...). */
    INFO,
    /** Algo sospechoso o rechazado, pero controlado (login fallido, línea corrupta omitida...). */
    WARN,
    /** Error real: una operación no se pudo completar o hubo una excepción inesperada. */
    ERROR,
    /** Eventos de las pruebas automáticas (cuándo y cuántas pruebas se ejecutaron). */
    TEST
}
