/*
 * RepositorioComentariosArchivoTexto.java
 *
 * Qué hace: guarda y lee los comentarios en data/comentarios.txt con el formato
 * idComentario|idPublicacion|idAutor|fechaISO|texto.
 * Rol en la arquitectura: implementación concreta de RepositorioComentarios; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce Comentario <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Comentario.
 */
package una.social.interacciones;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;
import una.social.publicaciones.Publicacion;

/**
 * Repositorio de comentarios sobre un archivo de texto.
 */
public final class RepositorioComentariosArchivoTexto implements RepositorioComentarios {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 5;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Comentarios de las publicaciones (archivo generado por la aplicación)",
            "Una línea por comentario. Columnas separadas por '|':",
            "  1. idComentario  -> identificador único CM0001, CM0002, ... (lo asigna el sistema)",
            "  2. idPublicacion -> publicación comentada (P0001...)",
            "  3. idAutor       -> usuario que comentó (U001...)",
            "  4. fechaISO      -> fecha y hora del comentario en ISO-8601 UTC",
            "  5. texto         -> comentario (máximo " + Comentario.LARGO_MAXIMO_TEXTO + " caracteres)",
            "Escape dentro del texto: \\| = barra vertical, \\\\ = barra invertida, \\n = salto de línea.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de comentarios.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de comentarios.txt
     * @param auditoria bitácora
     */
    public RepositorioComentariosArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/comentarios.txt y devuelve los comentarios que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Comentario> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioComentariosArchivoTexto::convertir, auditoria);
    }

    /**
     * Reescribe data/comentarios.txt completo con los comentarios de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param comentarios lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodos(List<Comentario> comentarios) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, comentarios, c -> new String[] {
            c.id(), c.idPublicacion(), c.idAutor(), c.fecha().toString(), c.texto()}, auditoria);
    }

    /**
     * Convierte las 5 columnas de una línea de comentarios.txt en un Comentario, validando cada una.
     *
     * @param c columnas desescapadas
     * @return comentario leído
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Comentario convertir(String[] c) {
        // Columna 4: el texto (no vacío y no más largo que el máximo).
        String texto = ArchivoRegistros.exigirTexto(c[4], "texto");
        if (texto.length() > Comentario.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionDatosCorruptos("el comentario supera los " + Comentario.LARGO_MAXIMO_TEXTO + " caracteres");
        }
        // Columnas 0 a 3: ID del comentario, ID de la publicación, ID del autor y fecha.
        return new Comentario(
                ArchivoRegistros.exigirPatron(c[0], Comentario.PATRON_ID, "idComentario"),
                ArchivoRegistros.exigirPatron(c[1], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.exigirPatron(c[2], Usuario.PATRON_ID, "idAutor"),
                ArchivoRegistros.leerFecha(c[3], "fechaISO"),
                texto);
    }
}
