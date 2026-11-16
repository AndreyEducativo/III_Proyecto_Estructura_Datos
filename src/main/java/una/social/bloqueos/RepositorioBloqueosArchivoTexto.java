/*
 * RepositorioBloqueosArchivoTexto.java
 *
 * Qué hace: guarda y lee los bloqueos en data/bloqueos.txt con el formato
 * idBloqueador|idBloqueado|fechaISO.
 * Rol en la arquitectura: implementación concreta de RepositorioBloqueos; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce Bloqueo <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Bloqueo.
 */
package una.social.bloqueos;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de bloqueos sobre un archivo de texto.
 */
public final class RepositorioBloqueosArchivoTexto implements RepositorioBloqueos {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 3;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Usuarios bloqueados (archivo generado por la aplicación)",
            "Una línea por bloqueo. Columnas separadas por '|':",
            "  1. idBloqueador -> ID del usuario que bloqueó (solo él puede desbloquear)",
            "  2. idBloqueado  -> ID del usuario bloqueado",
            "  3. fechaISO     -> fecha y hora del bloqueo en ISO-8601 UTC",
            "Mientras exista el bloqueo, ninguno de los dos ve el contenido del otro.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de bloqueos.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de bloqueos.txt
     * @param auditoria bitácora
     */
    public RepositorioBloqueosArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<Bloqueo> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> new Bloqueo(
                ArchivoRegistros.exigirPatron(c[0], Usuario.PATRON_ID, "idBloqueador"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idBloqueado"),
                ArchivoRegistros.leerFecha(c[2], "fechaISO")), auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodos(List<Bloqueo> bloqueos) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, bloqueos,
                b -> new String[] {b.idBloqueador(), b.idBloqueado(), b.fecha().toString()}, auditoria);
    }
}
