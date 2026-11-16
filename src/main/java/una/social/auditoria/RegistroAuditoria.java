/*
 * RegistroAuditoria.java
 *
 * Qué hace: contrato para dejar constancia de lo que pasa en el sistema (bitácora de auditoría).
 * Rol en la arquitectura: contrato compartido. El servicio, los repositorios, la UI y el módulo
 * de Sebastián reciben esta interfaz por constructor y nunca la implementación concreta.
 * Principio SOLID:
 *   - ISP: tiene UN solo método de escritura. A propósito NO existen métodos para editar,
 *     borrar, truncar ni reescribir entradas: la bitácora es de "solo agregar".
 *   - DIP: las clases dependen de esta abstracción; en las pruebas se sustituye por una versión
 *     en memoria sin cambiar el código que la usa (LSP).
 * Colabora con: RegistroAuditoriaArchivo (implementación real), NivelAuditoria.
 */
package una.social.auditoria;

/**
 * Bitácora de auditoría de solo agregar (append-only).
 *
 * <p><b>Contrato que toda implementación debe cumplir:</b></p>
 * <ul>
 *   <li>{@link #registrar} NUNCA lanza excepciones hacia afuera: si falla, reporta por
 *       {@code System.err} y sigue. Auditar jamás debe tumbar la aplicación.</li>
 *   <li>Es segura entre hilos (la llaman la UI y los SwingWorker al mismo tiempo).</li>
 *   <li>Quien la llama NUNCA debe enviar contraseñas, hashes ni sales en el detalle.</li>
 * </ul>
 */
public interface RegistroAuditoria {

    /** Actor que se usa cuando el evento lo genera el propio sistema y no un usuario. */
    String ACTOR_SISTEMA = "SISTEMA";

    /**
     * Agrega un evento al final de la bitácora.
     *
     * @param nivel   gravedad del evento
     * @param actor   nombre de usuario que provocó el evento, o {@link #ACTOR_SISTEMA}
     * @param accion  código corto de la acción, en MAYÚSCULAS (por ejemplo {@code LOGIN_EXITOSO})
     * @param detalle descripción legible, sin datos sensibles
     */
    void registrar(NivelAuditoria nivel, String actor, String accion, String detalle);

    /**
     * Construye una descripción segura de una excepción: solo su tipo y su mensaje, sin la pila
     * completa (la pila podría ser enorme y no aporta a la bitácora).
     *
     * <p>Es un método estático de utilidad, NO un segundo método de escritura.</p>
     *
     * @param error excepción a describir (puede ser nula)
     * @return texto con el formato {@code "TipoDeExcepcion: mensaje"}
     */
    static String describirError(Throwable error) {
        if (error == null) {
            return "Error desconocido";
        }
        String mensaje = error.getMessage() == null ? "(sin mensaje)" : error.getMessage();
        return error.getClass().getSimpleName() + ": " + mensaje;
    }
}
