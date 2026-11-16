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
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<Compartido> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> new Compartido(
                ArchivoRegistros.exigirPatron(c[0], Compartido.PATRON_ID, "idCompartido"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idUsuario"),
                ArchivoRegistros.exigirPatron(c[2], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.leerFecha(c[3], "fechaISO")), auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodos(List<Compartido> compartidos) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, compartidos,
                c -> new String[] {c.id(), c.idUsuario(), c.idPublicacion(), c.fecha().toString()}, auditoria);
    }
}
