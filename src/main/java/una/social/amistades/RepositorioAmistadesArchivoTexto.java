/*
 * RepositorioAmistadesArchivoTexto.java
 *
 * Qué hace: guarda y lee las amistades en data/amistades.txt con el formato
 * idUsuarioA|idUsuarioB|fechaISO.
 * Rol en la arquitectura: implementación concreta de RepositorioAmistades; solo la crea el
 * ensamblador (EnsambladorSocial).
 * Principio SOLID: SRP (traduce Amistad <-> línea de texto; las reglas de negocio, como "el
 * usuario debe existir", las aplica GestorAmistades) y LSP (cumple el contrato del repositorio).
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Amistad.
 */
package una.social.amistades;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de amistades sobre un archivo de texto.
 */
public final class RepositorioAmistadesArchivoTexto implements RepositorioAmistades {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 3;

    /** Columna del usuario que agregó. */
    private static final int COL_USUARIO_A = 0;

    /** Columna del usuario agregado. */
    private static final int COL_USUARIO_B = 1;

    /** Columna de la fecha. */
    private static final int COL_FECHA = 2;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Amistades (archivo generado por la aplicación)",
            "Una línea por amistad. La amistad es MUTUA: cada par aparece una sola vez.",
            "Columnas separadas por '|':",
            "  1. idUsuarioA -> ID del usuario que pulsó 'Agregar como amigo' (ej. U001)",
            "  2. idUsuarioB -> ID del usuario agregado (ej. U002)",
            "  3. fechaISO   -> fecha y hora de la amistad en ISO-8601 UTC, ej. 2026-11-15T18:00:00Z",
            "Los IDs deben existir en usuarios.txt; si no, la línea se omite y se audita.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de amistades.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de amistades.txt
     * @param auditoria bitácora
     */
    public RepositorioAmistadesArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/amistades.txt y devuelve las amistades que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Amistad> cargarTodas() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioAmistadesArchivoTexto::convertir, auditoria);
    }

    /**
     * Reescribe data/amistades.txt completo con las amistades de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param amistades lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodas(List<Amistad> amistades) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, amistades, amistad -> new String[] {
            amistad.idUsuarioA(), amistad.idUsuarioB(), amistad.fecha().toString()}, auditoria);
    }

    /**
     * Convierte las columnas de una línea en una Amistad, validando el formato de cada una.
     *
     * @param c columnas desescapadas
     * @return amistad leída
     */
    private static Amistad convertir(String[] c) {
        return new Amistad(
                ArchivoRegistros.exigirPatron(c[COL_USUARIO_A], Usuario.PATRON_ID, "idUsuarioA"),
                ArchivoRegistros.exigirPatron(c[COL_USUARIO_B], Usuario.PATRON_ID, "idUsuarioB"),
                ArchivoRegistros.leerFecha(c[COL_FECHA], "fechaISO"));
    }
}
