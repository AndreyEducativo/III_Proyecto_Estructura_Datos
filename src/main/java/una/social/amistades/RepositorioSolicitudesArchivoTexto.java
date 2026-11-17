/*
 * RepositorioSolicitudesArchivoTexto.java
 *
 * Qué hace: guarda y lee las solicitudes de amistad pendientes en data/solicitudes.txt con el
 * formato idRemitente|idDestinatario|fechaISO.
 * Rol en la arquitectura: implementación concreta de RepositorioSolicitudes; solo la crea el
 * ensamblador (EnsambladorSocial).
 * Principio SOLID: SRP (traduce SolicitudAmistad <-> línea de texto; las reglas de negocio las
 * aplica GestorAmistades) y LSP (cumple el contrato del repositorio).
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, SolicitudAmistad.
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
 * Repositorio de solicitudes de amistad sobre un archivo de texto.
 */
public final class RepositorioSolicitudesArchivoTexto implements RepositorioSolicitudes {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 3;

    /** Columna del remitente. */
    private static final int COL_REMITENTE = 0;

    /** Columna del destinatario. */
    private static final int COL_DESTINATARIO = 1;

    /** Columna de la fecha. */
    private static final int COL_FECHA = 2;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Solicitudes de amistad PENDIENTES (archivo generado por la aplicación)",
            "Una línea por solicitud. Al aceptarla pasa a amistades.txt; al rechazarla o",
            "cancelarla se borra de aquí. Columnas separadas por '|':",
            "  1. idRemitente    -> ID del usuario que envió la solicitud (ej. U001)",
            "  2. idDestinatario -> ID del usuario que debe aceptarla o rechazarla (ej. U003)",
            "  3. fechaISO       -> fecha y hora del envío en ISO-8601 UTC, ej. 2026-11-15T18:00:00Z",
            "Los IDs deben existir en usuarios.txt; si no, la línea se omite y se audita.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de solicitudes.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de solicitudes.txt
     * @param auditoria bitácora
     */
    public RepositorioSolicitudesArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/solicitudes.txt y devuelve las solicitudes de amistad pendientes que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<SolicitudAmistad> cargarTodas() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioSolicitudesArchivoTexto::convertir, auditoria);
    }

    /**
     * Reescribe data/solicitudes.txt completo con las solicitudes de amistad pendientes de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param solicitudes lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodas(List<SolicitudAmistad> solicitudes) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, solicitudes, solicitud -> new String[] {
            solicitud.idRemitente(), solicitud.idDestinatario(), solicitud.fecha().toString()}, auditoria);
    }

    /**
     * Convierte las columnas de una línea en una solicitud, validando el formato de cada una.
     *
     * @param c columnas desescapadas
     * @return solicitud leída
     */
    private static SolicitudAmistad convertir(String[] c) {
        return new SolicitudAmistad(
                ArchivoRegistros.exigirPatron(c[COL_REMITENTE], Usuario.PATRON_ID, "idRemitente"),
                ArchivoRegistros.exigirPatron(c[COL_DESTINATARIO], Usuario.PATRON_ID, "idDestinatario"),
                ArchivoRegistros.leerFecha(c[COL_FECHA], "fechaISO"));
    }
}
