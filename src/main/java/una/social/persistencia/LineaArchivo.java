/*
 * LineaArchivo.java
 *
 * Qué hace: representa una línea de datos leída de un archivo .txt junto con su número de línea.
 * Rol en la arquitectura: contrato compartido. AlmacenArchivoTexto la devuelve al leer, y los
 * repositorios (de Andrey y de Sebastián) usan el número para auditar exactamente qué línea
 * estaba corrupta.
 * Principio SOLID: SRP (es solo un dato inmutable).
 * Colabora con: AlmacenArchivoTexto, RepositorioUsuariosArchivoTexto.
 */
package una.social.persistencia;

/**
 * Línea de datos de un archivo de texto.
 *
 * @param numero número de línea real dentro del archivo, empezando en 1 (cuenta también los
 *               comentarios y las líneas vacías, para que coincida con lo que se ve en un editor)
 * @param texto  contenido de la línea, sin el salto de línea final
 */
public record LineaArchivo(int numero, String texto) {
}
