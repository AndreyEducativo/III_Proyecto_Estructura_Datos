/*
 * ArchivoRegistros.java
 *
 * Qué hace: lectura y escritura GENÉRICA de archivos de registros ("una línea = un objeto"),
 * con la política del proyecto: omitir y auditar las líneas dañadas, y guardar de forma atómica
 * con un encabezado de comentarios. Incluye validadores de columnas reutilizables.
 * Rol en la arquitectura: utilidad compartida que usan RepositorioAmistadesArchivoTexto y
 * RepositorioPublicacionesArchivoTexto, para no repetir el mismo ciclo de lectura en cada uno.
 * Se apoya en AlmacenArchivoTexto (escape, división de columnas y escritura atómica).
 * Principio SOLID: SRP (solo el ciclo leer/convertir/auditar y escribir), OCP (un archivo nuevo
 * solo aporta su "convertidor", sin modificar esta clase) y DIP (recibe la auditoría).
 * Colabora con: AlmacenArchivoTexto, RegistroAuditoria, los repositorios de Sebastián.
 */
package una.social.persistencia;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;

/**
 * Lectura/escritura genérica de archivos de registros delimitados por {@code |}.
 */
public final class ArchivoRegistros {

    /**
     * Convierte las columnas (ya desescapadas) de una línea en un objeto.
     *
     * @param <T> tipo del objeto
     */
    @FunctionalInterface
    public interface ConvertidorRegistro<T> {
        /**
         * @param columnas valores de las columnas, ya desescapados
         * @return el objeto reconstruido
         * @throws ExcepcionDatosCorruptos si alguna columna no es válida
         */
        T convertir(String[] columnas);
    }

    /** Clase de utilidades: no se instancia. */
    private ArchivoRegistros() {
    }

    /**
     * Lee todos los registros válidos de un archivo. Las líneas dañadas se omiten y se auditan
     * con su número de línea; al final se audita un resumen.
     *
     * @param archivo     archivo a leer (se crea vacío si no existe)
     * @param columnas    cantidad exacta de columnas por línea
     * @param convertidor cómo pasar de columnas a objeto
     * @param auditoria   bitácora
     * @param <T>         tipo de los registros
     * @return registros válidos, en el orden del archivo
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    public static <T> List<T> leer(Path archivo, int columnas, ConvertidorRegistro<T> convertidor,
                                   RegistroAuditoria auditoria) {
        List<LineaArchivo> lineas = AlmacenArchivoTexto.leerLineasDeDatos(archivo);
        List<T> registros = new ArrayList<>(lineas.size());
        int omitidas = 0;
        for (LineaArchivo linea : lineas) {
            // Se espera ExcepcionDatosCorruptos (columnas de más o de menos, escape inválido, ID o
            // fecha mal escritos...). Política del proyecto: omitir esa línea, auditarla y seguir,
            // para que una sola línea dañada no impida cargar el resto. El usuario no ve nada.
            try {
                registros.add(convertidor.convertir(AlmacenArchivoTexto.leerRegistro(linea.texto(), columnas)));
            } catch (ExcepcionDatosCorruptos ex) {
                omitidas++;
                auditarLineaOmitida(auditoria, archivo, linea.numero(), ex.getMessage());
            }
        }
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "ARCHIVO_LEIDO",
                archivo.getFileName() + ": " + registros.size() + " registros válidos, " + omitidas + " líneas omitidas");
        return registros;
    }

    /**
     * Reemplaza el contenido del archivo por el encabezado y los registros, de forma atómica.
     *
     * @param archivo     archivo destino
     * @param encabezado  líneas de comentario (ya con '#')
     * @param registros   objetos a guardar
     * @param aColumnas   cómo pasar de objeto a columnas (sin escapar; aquí se escapan)
     * @param auditoria   bitácora
     * @param <T>         tipo de los registros
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar (el archivo
     *                                                      anterior queda intacto)
     */
    public static <T> void escribir(Path archivo, List<String> encabezado, List<T> registros,
                                    Function<T, String[]> aColumnas, RegistroAuditoria auditoria) {
        List<String> lineas = new ArrayList<>(encabezado.size() + registros.size());
        lineas.addAll(encabezado);
        for (T registro : registros) {
            lineas.add(AlmacenArchivoTexto.unirCampos(aColumnas.apply(registro)));
        }
        AlmacenArchivoTexto.escribirLineasAtomicamente(archivo, lineas);
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "ARCHIVO_GUARDADO",
                archivo.getFileName() + ": " + registros.size() + " registros");
    }

    /**
     * Audita una línea omitida por formato inválido.
     *
     * @param auditoria bitácora
     * @param archivo   archivo de origen
     * @param numero    número de línea dentro del archivo
     * @param motivo    explicación, sin datos sensibles
     */
    private static void auditarLineaOmitida(RegistroAuditoria auditoria, Path archivo, int numero, String motivo) {
        auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                archivo.getFileName() + " línea " + numero + ": " + motivo);
    }

    // ------------------------------------------------------------------ validadores de columnas
    // Se usan EXCEPCIONES (no asserts) porque el archivo es entrada externa que alguien pudo
    // editar a mano, y los asserts podrían estar desactivados.

    /**
     * Exige que una columna cumpla un patrón (por ejemplo, el formato de un ID).
     *
     * @param valor   valor de la columna
     * @param patron  expresión regular que debe cumplir completa
     * @param columna nombre de la columna para el mensaje
     * @return el valor sin espacios a los lados
     * @throws ExcepcionDatosCorruptos si no cumple el patrón
     */
    public static String exigirPatron(String valor, Pattern patron, String columna) {
        String limpio = valor.trim();
        if (!patron.matcher(limpio).matches()) {
            throw new ExcepcionDatosCorruptos("la columna '" + columna + "' tiene un formato inválido");
        }
        return limpio;
    }

    /**
     * Exige que una columna de texto no esté vacía.
     *
     * @param valor   valor de la columna
     * @param columna nombre de la columna para el mensaje
     * @return el valor tal cual
     * @throws ExcepcionDatosCorruptos si está vacía
     */
    public static String exigirTexto(String valor, String columna) {
        if (valor.isBlank()) {
            throw new ExcepcionDatosCorruptos("la columna '" + columna + "' está vacía");
        }
        return valor;
    }

    /**
     * Lee una fecha ISO-8601 (por ejemplo {@code 2026-11-15T18:00:00Z}).
     *
     * @param valor   texto de la fecha
     * @param columna nombre de la columna para el mensaje
     * @return instante leído
     * @throws ExcepcionDatosCorruptos si la fecha no tiene formato ISO-8601
     */
    public static Instant leerFecha(String valor, String columna) {
        // Se espera DateTimeParseException si la fecha está mal escrita; se traduce para que la
        // línea se omita y se audite.
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new ExcepcionDatosCorruptos("la columna '" + columna + "' no tiene formato de fecha ISO-8601", ex);
        }
    }
}
