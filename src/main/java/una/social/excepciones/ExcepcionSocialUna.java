/*
 * ExcepcionSocialUna.java
 *
 * Qué hace: es la excepción RAÍZ de todos los errores propios del dominio de Social UNA.
 * Rol en la arquitectura: contrato compartido. Tanto el módulo de Andrey como el de Sebastián
 * heredan de ella, de modo que la UI puede capturar "cualquier error conocido del sistema"
 * con un solo catch y mostrar un mensaje amigable.
 * Principio SOLID: LSP (Liskov). Cualquier subclase puede usarse donde se espera esta clase
 * sin cambiar el manejo de errores. OCP: se agregan errores nuevos creando subclases, sin
 * modificar las existentes.
 * Colabora con: todas las capas (servicio, persistencia, UI, auditoría).
 */
package una.social.excepciones;

/**
 * Excepción base del dominio Social UNA.
 *
 * <p>Es una excepción NO comprobada ({@link RuntimeException}) por una decisión consciente:
 * las interfaces (por ejemplo {@code RepositorioUsuarios}) no quedan "contaminadas" con
 * {@code throws IOException}, y cada capa decide en su límite (UI o servicio) dónde capturarla.
 * El mensaje de estas excepciones está pensado para poder mostrarse al usuario final; el detalle
 * técnico (la causa) se guarda en la auditoría.</p>
 */
public class ExcepcionSocialUna extends RuntimeException {

    /** Versión de serialización: requerida por {@link java.io.Serializable}, que hereda Throwable. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje apto para el usuario.
     *
     * @param mensaje descripción amigable del problema (en español, sin datos sensibles)
     */
    public ExcepcionSocialUna(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción conservando la causa técnica original.
     *
     * @param mensaje descripción amigable del problema
     * @param causa   excepción técnica que la originó (por ejemplo una {@code IOException});
     *                se conserva para auditarla, nunca para mostrarla al usuario
     */
    public ExcepcionSocialUna(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
