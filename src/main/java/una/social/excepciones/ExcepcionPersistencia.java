/*
 * ExcepcionPersistencia.java
 *
 * Qué hace: indica que falló la lectura o escritura de un archivo de datos (disco lleno,
 * permisos, archivo bloqueado por otro programa, etc.).
 * Rol en la arquitectura: la lanzan AlmacenArchivoTexto y los repositorios; la capturan los
 * servicios y la UI para mostrar un mensaje amigable y auditar el detalle técnico.
 * Principio SOLID: LSP; además envuelve la IOException para que las interfaces de repositorio
 * no dependan de detalles de java.io (DIP).
 * Colabora con: AlmacenArchivoTexto, RepositorioUsuariosArchivoTexto, GestorUsuarios.
 */
package una.social.excepciones;

/**
 * Error de entrada/salida al leer o guardar datos.
 */
public class ExcepcionPersistencia extends ExcepcionSocialUna {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción sin causa técnica.
     *
     * @param mensaje mensaje amigable
     */
    public ExcepcionPersistencia(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción conservando la causa técnica (normalmente una IOException).
     *
     * @param mensaje mensaje amigable
     * @param causa   error técnico original, que se registra en la auditoría
     */
    public ExcepcionPersistencia(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
