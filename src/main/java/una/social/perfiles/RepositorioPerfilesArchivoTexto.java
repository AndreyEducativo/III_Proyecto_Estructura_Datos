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
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<Perfil> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, c -> {
            String biografia = ArchivoRegistros.exigirTexto(c[1], "biografia");
            if (biografia.length() > Perfil.LARGO_MAXIMO_BIOGRAFIA) {
                throw new ExcepcionDatosCorruptos("la biografía supera los " + Perfil.LARGO_MAXIMO_BIOGRAFIA + " caracteres");
            }
            return new Perfil(ArchivoRegistros.exigirPatron(c[0], Usuario.PATRON_ID, "idUsuario"), biografia);
        }, auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodos(List<Perfil> perfiles) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, perfiles, p -> new String[] {p.idUsuario(), p.biografia()}, auditoria);
    }
}
