/*
 * RepositorioPublicacionesArchivoTexto.java
 *
 * Qué hace: guarda y lee las publicaciones en data/publicaciones.txt con el formato
 * idPublicacion|idAutor|fechaISO|privacidad|fechaEdicionISO|texto. También entiende el formato
 * ANTIGUO de 4 columnas (idPublicacion|idAutor|fechaISO|texto) para no perder datos de versiones
 * anteriores: esas líneas se leen como "Solo amigos" y sin edición, y al guardar se escriben ya en
 * el formato nuevo (migración automática).
 * Rol en la arquitectura: implementación concreta de RepositorioPublicaciones; la crea el
 * ensamblador (EnsambladorSocial).
 * Principio SOLID: SRP (traduce Publicacion <-> línea; las reglas de negocio las aplica el
 * gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Publicacion, Privacidad.
 */
package una.social.publicaciones;

import java.nio.file.Path;
import java.time.Instant;
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

    /** Columnas del formato actual. */
    private static final int COLUMNAS_FORMATO_ACTUAL = 6;

    /** Columnas del formato antiguo (sin privacidad ni fecha de edición). */
    private static final int COLUMNAS_FORMATO_ANTIGUO = 4;

    /** Columna del ID (ambos formatos). */
    private static final int COL_ID = 0;

    /** Columna del autor (ambos formatos). */
    private static final int COL_AUTOR = 1;

    /** Columna de la fecha (ambos formatos). */
    private static final int COL_FECHA = 2;

    /** Formato actual: columna de la privacidad. */
    private static final int COL_PRIVACIDAD = 3;

    /** Formato actual: columna de la fecha de edición (vacía si no se editó). */
    private static final int COL_FECHA_EDICION = 4;

    /** Formato actual: columna del texto. */
    private static final int COL_TEXTO = 5;

    /** Formato antiguo: columna del texto. */
    private static final int COL_TEXTO_ANTIGUO = 3;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Publicaciones del muro (archivo generado por la aplicación)",
            "Una línea por publicación. Columnas separadas por '|':",
            "  1. idPublicacion   -> identificador único P0001, P0002, ... (lo asigna el sistema)",
            "  2. idAutor         -> ID del usuario que publicó (debe existir en usuarios.txt)",
            "  3. fechaISO        -> fecha y hora de publicación en ISO-8601 UTC, ej. 2026-11-15T18:00:00Z",
            "  4. privacidad      -> PUBLICA (todos la ven) o SOLO_AMIGOS (autor y sus amigos)",
            "  5. fechaEdicionISO -> fecha de la última edición; vacía si nunca se editó",
            "  6. texto           -> mensaje (máximo " + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres)",
            "Compatibilidad: también se aceptan líneas antiguas de 4 columnas (id|autor|fecha|texto);",
            "se leen como SOLO_AMIGOS sin edición y al guardar se escriben en el formato nuevo.",
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
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/publicaciones.txt y devuelve las publicaciones que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Publicacion> cargarTodas() {
        return ArchivoRegistros.leer(archivo, new int[] {COLUMNAS_FORMATO_ACTUAL, COLUMNAS_FORMATO_ANTIGUO},
                RepositorioPublicacionesArchivoTexto::convertir, auditoria);
    }

    /**
     * Reescribe data/publicaciones.txt completo con las publicaciones de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param publicaciones lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodas(List<Publicacion> publicaciones) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, publicaciones, p -> new String[] {
            p.getId(), p.getIdAutor(), p.getFecha().toString(), p.getPrivacidad().name(),
            p.getFechaEdicion().map(Instant::toString).orElse(""), p.getTexto()}, auditoria);
    }

    /**
     * Convierte las columnas de una línea (formato actual o antiguo) en una Publicacion.
     *
     * @param c columnas desescapadas
     * @return publicación leída
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Publicacion convertir(String[] c) {
        boolean antiguo = c.length == COLUMNAS_FORMATO_ANTIGUO;
        String texto = ArchivoRegistros.exigirTexto(c[antiguo ? COL_TEXTO_ANTIGUO : COL_TEXTO], "texto");
        if (texto.length() > Publicacion.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionDatosCorruptos("el texto supera los " + Publicacion.LARGO_MAXIMO_TEXTO + " caracteres");
        }
        String id = ArchivoRegistros.exigirPatron(c[COL_ID], Publicacion.PATRON_ID, "idPublicacion");
        String autor = ArchivoRegistros.exigirPatron(c[COL_AUTOR], Usuario.PATRON_ID, "idAutor");
        Instant fecha = ArchivoRegistros.leerFecha(c[COL_FECHA], "fechaISO");
        if (antiguo) {
            // Línea de la versión anterior: se asume la privacidad por defecto y sin edición.
            return new Publicacion(id, autor, fecha, texto, Privacidad.SOLO_AMIGOS, null);
        }
        Instant edicion = c[COL_FECHA_EDICION].isBlank() ? null : ArchivoRegistros.leerFecha(c[COL_FECHA_EDICION], "fechaEdicionISO");
        return new Publicacion(id, autor, fecha, texto, leerPrivacidad(c[COL_PRIVACIDAD]), edicion);
    }

    /**
     * @param valor texto de la columna
     * @return privacidad leída
     * @throws ExcepcionDatosCorruptos si no es PUBLICA ni SOLO_AMIGOS
     */
    private static Privacidad leerPrivacidad(String valor) {
        // Se espera IllegalArgumentException si el texto no es un valor del enum; se traduce para
        // que la línea se omita y se audite.
        try {
            return Privacidad.valueOf(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new ExcepcionDatosCorruptos("la columna 'privacidad' debe ser PUBLICA o SOLO_AMIGOS", ex);
        }
    }
}
