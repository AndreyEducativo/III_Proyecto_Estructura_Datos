/*
 * RepositorioPublicacionesArchivoTexto.java
 *
 * Qué hace: guarda y lee las publicaciones en data/publicaciones.txt con el formato
 * idPublicacion|idAutor|fechaISO|texto.
 * Rol en la arquitectura: implementación concreta de RepositorioPublicaciones; la crea el
 * ensamblador (EnsambladorSocial).
 * Principio SOLID: SRP (traduce Publicacion <-> línea; las reglas de negocio las aplica el
 * gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Publicacion.
 */
package una.social.publicaciones;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de publicaciones sobre un archivo de texto.
 *
 * <p>El texto de una publicación puede traer {@code |}, barras invertidas y saltos de línea; el
 * escape de {@link AlmacenArchivoTexto} los convierte en {@code \|}, {@code \\} y {@code \n} para
 * que cada publicación ocupe siempre UNA sola línea del archivo.</p>
 */
public final class RepositorioPublicacionesArchivoTexto implements RepositorioPublicaciones {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 4;

    /** Columna del ID. */
    private static final int COL_ID = 0;

    /** Columna del autor. */
    private static final int COL_AUTOR = 1;

    /** Columna de la fecha. */
    private static final int COL_FECHA = 2;

    /** Columna del texto. */
    private static final int COL_TEXTO = 3;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Publicaciones del muro (archivo generado por la aplicación)",
            "Una línea por publicación. Columnas separadas por '|':",
            "  1. idPublicacion -> identificador único P0001, P0002, ... (lo asigna el sistema)",
            "  2. idAutor       -> ID del usuario que publicó (debe existir en usuarios.txt)",
            "  3. fechaISO      -> fecha y hora de publicación en ISO-8601 UTC, ej. 2026-11-15T18:00:00Z",
            "  4. texto         -> mensaje (máximo " + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres)",
            "Escape dentro del texto: \\| = barra vertical, \\\\ = barra invertida, \\n = salto de línea.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de publicaciones.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de publicaciones.txt
     * @param auditoria bitácora
     */
    public RepositorioPublicacionesArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<Publicacion> cargarTodas() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioPublicacionesArchivoTexto::convertir, auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodas(List<Publicacion> publicaciones) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, publicaciones, p -> new String[] {
            p.getId(), p.getIdAutor(), p.getFecha().toString(), p.getTexto()}, auditoria);
    }

    /**
     * Convierte las columnas de una línea en una Publicacion, validando cada una.
     *
     * @param c columnas desescapadas
     * @return publicación leída
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Publicacion convertir(String[] c) {
        String texto = ArchivoRegistros.exigirTexto(c[COL_TEXTO], "texto");
        if (texto.length() > Publicacion.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionDatosCorruptos("el texto supera los " + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres");
        }
        return new Publicacion(
                ArchivoRegistros.exigirPatron(c[COL_ID], Publicacion.PATRON_ID, "idPublicacion"),
                ArchivoRegistros.exigirPatron(c[COL_AUTOR], Usuario.PATRON_ID, "idAutor"),
                ArchivoRegistros.leerFecha(c[COL_FECHA], "fechaISO"),
                texto);
    }
}
