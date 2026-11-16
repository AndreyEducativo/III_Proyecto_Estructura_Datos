/*
 * RepositorioMeGustaArchivoTexto.java
 *
 * Qué hace: guarda y lee los "me gusta" en data/megusta.txt con el formato
 * idPublicacion|idUsuario|fechaISO.
 * Rol en la arquitectura: implementación concreta de RepositorioMeGusta; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce MeGusta <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, MeGusta.
 */
package una.social.interacciones;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;
import una.social.publicaciones.Publicacion;

/**
 * Repositorio de "me gusta" sobre un archivo de texto.
 */
public final class RepositorioMeGustaArchivoTexto implements RepositorioMeGusta {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 3;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - \"Me gusta\" de las publicaciones (archivo generado por la aplicación)",
            "Una línea por \"me gusta\". Columnas separadas por '|':",
            "  1. idPublicacion -> publicación que recibió el \"me gusta\" (P0001...)",
            "  2. idUsuario     -> usuario que dio el \"me gusta\" (U001...)",
            "  3. fechaISO      -> fecha y hora en ISO-8601 UTC",
            "Un usuario solo puede dar un \"me gusta\" por publicación. En la aplicación solo se",
            "muestra la CANTIDAD de \"me gusta\", nunca quién los dio.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de megusta.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de megusta.txt
     * @param auditoria bitácora
     */
    public RepositorioMeGustaArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<MeGusta> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> new MeGusta(
                ArchivoRegistros.exigirPatron(c[0], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idUsuario"),
                ArchivoRegistros.leerFecha(c[2], "fechaISO")), auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodos(List<MeGusta> meGustas) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, meGustas,
                m -> new String[] {m.idPublicacion(), m.idUsuario(), m.fecha().toString()}, auditoria);
    }
}
