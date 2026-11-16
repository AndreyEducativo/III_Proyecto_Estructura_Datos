/*
 * ExcepcionDatosCorruptos.java
 *
 * Qué hace: indica que una línea de un archivo de datos no tiene el formato esperado
 * (columnas de más o de menos, escape inválido, fecha o Base64 mal escritos, etc.).
 * Rol en la arquitectura: la lanzan AlmacenArchivoTexto y los repositorios al interpretar una
 * línea. El repositorio la captura, OMITE esa línea, la audita y sigue con las demás, de modo
 * que un archivo dañado parcialmente no tumba la aplicación.
 * Principio SOLID: LSP.
 * Colabora con: AlmacenArchivoTexto, RepositorioUsuariosArchivoTexto (y los repositorios de
 * Sebastián).
 */
package una.social.excepciones;

/**
 * Una línea de un archivo de datos está dañada o incompleta.
 */
public class ExcepcionDatosCorruptos extends ExcepcionPersistencia {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param mensaje descripción del problema de formato (sin incluir datos sensibles)
     */
    public ExcepcionDatosCorruptos(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción conservando la causa (por ejemplo un error al leer una fecha).
     *
     * @param mensaje descripción del problema de formato
     * @param causa   error original
     */
    public ExcepcionDatosCorruptos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
