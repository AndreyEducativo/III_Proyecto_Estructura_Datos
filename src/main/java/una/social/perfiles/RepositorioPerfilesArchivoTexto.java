/*
 * RepositorioPerfilesArchivoTexto.java
 *
 * Qué hace: guarda y lee las biografías en data/perfiles.txt con el formato idUsuario|biografia.
 * Rol en la arquitectura: implementación concreta de RepositorioPerfiles; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce Perfil <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Perfil.
 */
package una.social.perfiles;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de perfiles sobre un archivo de texto.
 */
public final class RepositorioPerfilesArchivoTexto implements RepositorioPerfiles {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 2;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Biografías de los perfiles (archivo generado por la aplicación)",
            "Una línea por usuario que escribió su biografía. Columnas separadas por '|':",
            "  1. idUsuario -> ID del usuario (U001...)",
            "  2. biografia -> texto de presentación (máximo " + Perfil.LARGO_MAXIMO_BIOGRAFIA + " caracteres)",
            "Se guarda aparte de usuarios.txt para no cambiar el formato de ese archivo.",
            "Escape dentro del texto: \\| = barra vertical, \\\\ = barra invertida, \\n = salto de línea.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de perfiles.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de perfiles.txt
     * @param auditoria bitácora
     */
    public RepositorioPerfilesArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/perfiles.txt y devuelve los perfiles (biografías) que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Perfil> cargarTodos() {
        // Cada línea tiene 2 columnas: ID del usuario y su biografía.
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> {
            String biografia = ArchivoRegistros.exigirTexto(c[1], "biografia");
            // Una biografía más larga que el máximo indica que el archivo se editó a mano.
            if (biografia.length() > Perfil.LARGO_MAXIMO_BIOGRAFIA) {
                throw new ExcepcionDatosCorruptos("la biografía supera los " + Perfil.LARGO_MAXIMO_BIOGRAFIA + " caracteres");
            }
            return new Perfil(ArchivoRegistros.exigirPatron(c[0], Usuario.PATRON_ID, "idUsuario"), biografia);
        }, auditoria);
    }

    /**
     * Reescribe data/perfiles.txt completo con los perfiles (biografías) de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param perfiles lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodos(List<Perfil> perfiles) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, perfiles, p -> new String[] {p.idUsuario(), p.biografia()}, auditoria);
    }
}
