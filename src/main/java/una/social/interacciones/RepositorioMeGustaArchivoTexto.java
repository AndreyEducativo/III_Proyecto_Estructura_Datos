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
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/megusta.txt y devuelve los me gusta que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<MeGusta> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> new MeGusta(
                ArchivoRegistros.exigirPatron(c[0], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idUsuario"),
                ArchivoRegistros.leerFecha(c[2], "fechaISO")), auditoria);
    }

    /**
     * Reescribe data/megusta.txt completo con los me gusta de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param meGustas lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodos(List<MeGusta> meGustas) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, meGustas,
                m -> new String[] {m.idPublicacion(), m.idUsuario(), m.fecha().toString()}, auditoria);
    }
}
