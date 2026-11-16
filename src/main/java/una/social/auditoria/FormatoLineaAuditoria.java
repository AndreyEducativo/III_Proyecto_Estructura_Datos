/*
 * FormatoLineaAuditoria.java
 *
 * Qué hace: define en UN solo lugar cómo se ve una línea de la bitácora y cómo se calcula su
 * hash SHA-256.
 * Rol en la arquitectura: clase interna del paquete de auditoría (no es pública). La usan tanto
 * quien ESCRIBE (RegistroAuditoriaArchivo) como quien VERIFICA (VerificadorIntegridadAuditoria).
 * Si cada uno tuviera su propia copia del formato, un cambio en uno rompería al otro; por eso
 * se centraliza aquí (evita código duplicado).
 * Principio SOLID: SRP (solo conoce el formato de una línea).
 * Colabora con: AlmacenArchivoTexto (escape de campos), RegistroAuditoriaArchivo,
 * VerificadorIntegridadAuditoria.
 */
package una.social.auditoria;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import una.social.persistencia.AlmacenArchivoTexto;

/**
 * Formato de una línea de auditoría:
 *
 * <pre>
 * fechaISO|nivel|actor|accion|detalle|hashAnterior|hashActual
 * </pre>
 *
 * <p><b>Cómo funciona la cadena de hashes (explicado simple):</b> un hash SHA-256 es una
 * "huella digital" de un texto: 64 caracteres hexadecimales que cambian por completo si se
 * cambia una sola letra del texto. Cada línea guarda:</p>
 * <ol>
 *   <li>{@code hashAnterior}: la huella de la línea de ARRIBA.</li>
 *   <li>{@code hashActual}: la huella de su propio contenido (los 6 primeros campos, incluido el
 *       hashAnterior).</li>
 * </ol>
 * <p>Así las líneas quedan "encadenadas" como eslabones. Si alguien edita una línea a mano, su
 * huella ya no coincide. Si borra una línea del medio, la siguiente apunta a una huella que ya
 * no está arriba. En ambos casos el verificador lo detecta e indica el número de línea.</p>
 *
 * <p>La primera línea apunta a un hash "génesis" de 64 ceros, porque no tiene línea anterior.</p>
 */
final class FormatoLineaAuditoria {

    /** Hash que usa la primera línea como "anterior" (no hay línea antes de ella). */
    static final String HASH_GENESIS = "0".repeat(64);

    /** Cantidad de campos de una línea válida. */
    static final int CANTIDAD_CAMPOS = 7;

    /** Posición del campo hashAnterior (empezando en 0). */
    static final int INDICE_HASH_ANTERIOR = 5;

    /** Posición del campo hashActual (empezando en 0). */
    static final int INDICE_HASH_ACTUAL = 6;

    /** Longitud de un hash SHA-256 escrito en hexadecimal (32 bytes × 2 caracteres). */
    private static final int LONGITUD_HASH_HEX = 64;

    /** Separador de campos (el mismo de todos los archivos .txt del proyecto). */
    private static final char SEPARADOR = '|';

    /** Clase de utilidades: no se instancia. */
    private FormatoLineaAuditoria() {
    }

    /**
     * Arma una línea completa, lista para agregarse al archivo.
     *
     * @param fechaIso     fecha y hora en formato ISO-8601
     * @param nivel        nivel del evento
     * @param actor        actor del evento
     * @param accion       acción
     * @param detalle      detalle
     * @param hashAnterior hash de la línea anterior (o {@link #HASH_GENESIS})
     * @return la línea con sus 7 campos, ya escapados
     */
    static String construirLinea(String fechaIso, NivelAuditoria nivel, String actor, String accion,
                                 String detalle, String hashAnterior) {
        // Precondición interna: el hash anterior siempre lo calcula este mismo paquete, así que
        // si no mide 64 caracteres es un error de programación, no un dato del usuario.
        assert hashAnterior.length() == LONGITUD_HASH_HEX : "hashAnterior con longitud inválida";
        // Se escapan los campos porque el detalle puede traer '|' o saltos de línea (por
        // ejemplo, el mensaje de una excepción). Sin escape, una sola entrada podría partirse
        // en dos líneas o correr las columnas, y la cadena de hashes se rompería.
        String contenido = AlmacenArchivoTexto.unirCampos(
                fechaIso, nivel.name(), actor, accion, detalle, hashAnterior);
        return contenido + SEPARADOR + calcularHash(contenido);
    }

    /**
     * Separa una línea en "contenido" (lo que se hashea) y "hash guardado".
     *
     * <p>El hash es hexadecimal y nunca contiene '|', por lo que el ÚLTIMO '|' de la línea es
     * siempre el separador entre el contenido y el hash.</p>
     *
     * @param linea línea completa leída del archivo
     * @return arreglo {contenido, hashGuardado}; o {@code null} si la línea no tiene separador
     */
    static String[] separarContenidoYHash(String linea) {
        int posicion = linea.lastIndexOf(SEPARADOR);
        if (posicion < 0) {
            return null;
        }
        return new String[] {linea.substring(0, posicion), linea.substring(posicion + 1)};
    }

    /**
     * Calcula la huella SHA-256 de un texto y la devuelve en hexadecimal (64 caracteres).
     *
     * @param texto texto a "resumir"
     * @return hash en hexadecimal, en minúsculas
     */
    static String calcularHash(String texto) {
        try {
            // Se crea un MessageDigest nuevo en cada llamada porque NO es seguro entre hilos;
            // crearlo es barato comparado con escribir en disco.
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] huella = sha256.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(huella);
        } catch (NoSuchAlgorithmException ex) {
            // La especificación de Java OBLIGA a que todo JDK incluya SHA-256, así que este caso
            // "no puede ocurrir". Si ocurriera, el entorno está roto: se informa con un error de
            // estado ilegal en vez de seguir con un hash falso que invalidaría la cadena.
            throw new IllegalStateException("El JDK no ofrece SHA-256", ex);
        }
    }
}
