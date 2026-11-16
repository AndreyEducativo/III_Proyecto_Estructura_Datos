/*
 * ResultadoVerificacion.java
 *
 * Qué hace: guarda el resultado de revisar la cadena de hashes de la auditoría.
 * Rol en la arquitectura: lo produce VerificadorIntegridadAuditoria y lo consume Main (para
 * auditar el resultado y mostrar una advertencia gráfica si la cadena está rota).
 * Principio SOLID: SRP (solo transporta el resultado; es un "record" inmutable).
 * Colabora con: VerificadorIntegridadAuditoria, Main.
 */
package una.social.auditoria;

/**
 * Resultado de la verificación de integridad.
 *
 * @param integra           {@code true} si toda la cadena es válida
 * @param lineasVerificadas cuántas líneas se revisaron (las válidas antes de la ruptura)
 * @param lineaRota         número de línea (empezando en 1) donde se rompe; 0 si está íntegra
 * @param motivo            explicación legible del problema; vacío si está íntegra
 */
public record ResultadoVerificacion(boolean integra, int lineasVerificadas, int lineaRota, String motivo) {

    /**
     * Crea un resultado de cadena íntegra.
     *
     * @param lineasVerificadas cantidad de líneas revisadas
     * @return resultado íntegro
     */
    public static ResultadoVerificacion integra(int lineasVerificadas) {
        return new ResultadoVerificacion(true, lineasVerificadas, 0, "");
    }

    /**
     * Crea un resultado de cadena rota.
     *
     * @param lineasVerificadas líneas válidas antes de la ruptura
     * @param lineaRota         línea donde se detectó el problema (0 si no se pudo leer el archivo)
     * @param motivo            explicación del problema
     * @return resultado no íntegro
     */
    public static ResultadoVerificacion rota(int lineasVerificadas, int lineaRota, String motivo) {
        return new ResultadoVerificacion(false, lineasVerificadas, lineaRota, motivo);
    }

    /**
     * @return frase lista para auditar o mostrar en un diálogo
     */
    public String descripcion() {
        if (integra) {
            return "Cadena de auditoría íntegra (" + lineasVerificadas + " líneas verificadas).";
        }
        String donde = lineaRota > 0 ? " en la línea " + lineaRota : "";
        return "Cadena de auditoría ROTA" + donde + ": " + motivo;
    }
}
