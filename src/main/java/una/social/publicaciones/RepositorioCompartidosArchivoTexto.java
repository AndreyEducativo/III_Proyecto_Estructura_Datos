/*
 * RepositorioCompartidosArchivoTexto.java
 *
 * Qué hace: guarda y lee los compartidos en data/compartidos.txt con el formato
 * idCompartido|idUsuario|idPublicacion|fechaISO.
 * Rol en la arquitectura: implementación concreta de RepositorioCompartidos; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce Compartido <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Compartido.
 */
package una.social.publicaciones;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de compartidos sobre un archivo de texto.
 */
public final class RepositorioCompartidosArchivoTexto implements RepositorioCompartidos {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 4;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Publicaciones compartidas (archivo generado por la aplicación)",
            "Una línea por compartido. Columnas separadas por '|':",
            "  1. idCompartido  -> identificador único CP0001, CP0002, ... (lo asigna el sistema)",
            "  2. idUsuario     -> ID del usuario que compartió",
            "  3. idPublicacion -> ID de la publicación PÚBLICA original (P0001...)",
            "  4. fechaISO      -> fecha y hora en que se compartió, en ISO-8601 UTC",
            "Si la publicación original se borra, el compartido se conserva y se muestra como",
            "\"La publicación original ya no está disponible\".",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de compartidos.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de compartidos.txt
     * @param auditoria bitácora
     */
    public RepositorioCompartidosArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/compartidos.txt y devuelve las publicaciones compartidas que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Compartido> cargarTodos() {
        // Cada línea tiene 4 columnas: ID del compartido, ID de quien comparte, ID de la
        // publicación original y fecha. Cada una se valida con su formato.
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> new Compartido(
                ArchivoRegistros.exigirPatron(c[0], Compartido.PATRON_ID, "idCompartido"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idUsuario"),
                ArchivoRegistros.exigirPatron(c[2], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.leerFecha(c[3], "fechaISO")), auditoria);
    }

    /**
     * Reescribe data/compartidos.txt completo con las publicaciones compartidas de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param compartidos lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodos(List<Compartido> compartidos) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, compartidos,
                c -> new String[] {c.id(), c.idUsuario(), c.idPublicacion(), c.fecha().toString()}, auditoria);
    }
}
