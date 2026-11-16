/*
 * ModuloPersistente.java
 *
 * Qué hace: contrato para cualquier módulo que guarda sus datos en archivos (usuarios, y en el
 * futuro amistades y publicaciones de Sebastián).
 * Rol en la arquitectura: Main mantiene una lista de módulos persistentes y les pide
 * "cargarDatos" al iniciar y "guardarDatos" al cerrar sesión y al cerrar la aplicación, sin
 * conocer qué guarda cada uno.
 * Principio SOLID: OCP (Sebastián agrega sus módulos a la lista sin modificar Main ni el
 * controlador), ISP (solo tres métodos) y DIP (Main depende de esta abstracción).
 * Colabora con: GestorUsuarios (implementación), Main, ControladorNavegacion.
 */
package una.social.persistencia;

/**
 * Módulo con datos que se cargan desde archivos y se guardan en archivos.
 */
public interface ModuloPersistente {

    /**
     * @return nombre corto del módulo para la auditoría (por ejemplo "usuarios")
     */
    String nombreModulo();

    /**
     * Lee los archivos y reconstruye las estructuras en memoria. Las líneas corruptas se omiten
     * y se auditan, sin detener la carga.
     *
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo completo no se pudo leer
     */
    void cargarDatos();

    /**
     * Guarda en disco el estado actual de la memoria.
     *
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    void guardarDatos();
}
