/*
 * AlmacenArchivoTexto.java
 *
 * Qué hace: utilidades compartidas para TODOS los archivos .txt del proyecto: leer líneas de
 * datos, escribir de forma atómica, escapar/desescapar campos y dividir una línea en columnas
 * respetando el escape.
 * Rol en la arquitectura: contrato compartido. Lo usan RepositorioUsuariosArchivoTexto, la
 * auditoría y los repositorios de amistades y publicaciones de Sebastián. Así todos los archivos
 * siguen exactamente la misma convención y nadie reescribe esta lógica delicada.
 * Principio SOLID: SRP (solo sabe de formato y archivos, no de usuarios ni publicaciones) y OCP
 * (un archivo nuevo reutiliza estas funciones sin modificarlas).
 * Colabora con: LineaArchivo, ExcepcionPersistencia, ExcepcionDatosCorruptos.
 */
package una.social.persistencia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.excepciones.ExcepcionPersistencia;

/**
 * Utilidades de archivos de texto plano delimitados por {@code |}.
 *
 * <p><b>Convención de formato (igual para todos los archivos de data/):</b></p>
 * <ul>
 *   <li>Codificación UTF-8, un registro por línea, columnas separadas por {@code |}.</li>
 *   <li>Las líneas que empiezan con {@code #} son comentarios y las líneas vacías se ignoran.</li>
 *   <li>Caracteres especiales dentro de un campo se "escapan" con barra invertida:
 *       {@code \|} = barra vertical, {@code \\} = barra invertida,
 *       {@code \n} = salto de línea, {@code \r} = retorno de carro.</li>
 * </ul>
 *
 * <p><b>¿Por qué hace falta el escape? (explicado simple)</b> Si alguien escribe una publicación
 * como "hola | mundo", el {@code |} se confundiría con el separador de columnas y la línea
 * tendría una columna de más. Si escribe un mensaje de varias líneas, el salto de línea partiría
 * el registro en dos. Escapar es "disfrazar" esos caracteres con una barra invertida delante para
 * que el lector sepa que son parte del texto y no separadores. Y como la barra invertida pasa a
 * ser especial, ella misma también se escapa ({@code \\}).</p>
 */
public final class AlmacenArchivoTexto {

    /** Separador de columnas de todos los archivos de datos. */
    public static final char SEPARADOR = '|';

    /** Prefijo que marca una línea de comentario. */
    public static final String PREFIJO_COMENTARIO = "#";

    /** Carácter de escape. */
    private static final char ESCAPE = '\\';

    /** Marca de orden de bytes (BOM) que algunos editores de Windows agregan al inicio de UTF-8. */
    private static final char BOM_UTF8 = '\uFEFF';

    /** Fin de línea fijo para que los archivos sean idénticos en Windows y Linux. */
    private static final String FIN_DE_LINEA = "\n";

    /** Veces que se reintenta mover el archivo temporal si Windows lo tiene bloqueado. */
    private static final int INTENTOS_DE_MOVIMIENTO = 3;

    /** Espera entre reintentos (en milisegundos). */
    private static final long ESPERA_ENTRE_INTENTOS_MS = 150;

    /** Clase de utilidades: no se instancia. */
    private AlmacenArchivoTexto() {
    }

    // ------------------------------------------------------------------ lectura

    /**
     * Lee las líneas de DATOS de un archivo: ignora comentarios ({@code #}) y líneas vacías.
     * Si la carpeta o el archivo no existen, los crea vacíos (así la app arranca la primera vez
     * sin que nadie tenga que crear data/ a mano).
     *
     * @param archivo ruta del archivo
     * @return lista de líneas con su número real de línea (puede estar vacía)
     * @throws ExcepcionPersistencia si el archivo no se puede crear o leer, o no es UTF-8 válido
     */
    public static List<LineaArchivo> leerLineasDeDatos(Path archivo) {
        // Se esperan IOException (permisos, archivo bloqueado) y MalformedInputException (que
        // también es IOException) si el archivo no está en UTF-8. Se envuelven en
        // ExcepcionPersistencia para que quien llame no dependa de java.io; el usuario verá un
        // mensaje amigable y el detalle técnico queda como causa para la auditoría.
        try {
            crearSiNoExiste(archivo);
            List<String> todas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            List<LineaArchivo> datos = new ArrayList<>();
            for (int i = 0; i < todas.size(); i++) {
                String texto = i == 0 ? quitarBom(todas.get(i)) : todas.get(i);
                if (!texto.isBlank() && !texto.startsWith(PREFIJO_COMENTARIO)) {
                    datos.add(new LineaArchivo(i + 1, texto));
                }
            }
            return datos;
        } catch (IOException ex) {
            throw new ExcepcionPersistencia("No se pudo leer el archivo " + archivo.getFileName() + ".", ex);
        }
    }

    /**
     * Crea la carpeta y el archivo vacío si no existen.
     *
     * @param archivo ruta del archivo
     * @throws IOException si no se pueden crear
     */
    private static void crearSiNoExiste(Path archivo) throws IOException {
        Path carpeta = archivo.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        if (Files.notExists(archivo)) {
            Files.createFile(archivo);
        }
    }

    /**
     * Quita el BOM invisible que el Bloc de notas de Windows puede poner al inicio del archivo;
     * sin esto, el primer ID se leería como "\uFEFFU001" y no coincidiría con "U001".
     *
     * @param primeraLinea primera línea del archivo
     * @return la línea sin BOM
     */
    private static String quitarBom(String primeraLinea) {
        return !primeraLinea.isEmpty() && primeraLinea.charAt(0) == BOM_UTF8 ? primeraLinea.substring(1) : primeraLinea;
    }

    // ------------------------------------------------------------------ escritura atómica

    /**
     * Reemplaza TODO el contenido del archivo por las líneas dadas, de forma ATÓMICA.
     *
     * <p><b>¿Qué significa "atómica" y por qué importa? (explicado simple)</b> Si escribiéramos
     * directo sobre usuarios.txt y la aplicación se cerrara (o se fuera la luz) a la mitad, el
     * archivo quedaría cortado y se perderían usuarios. Para evitarlo:</p>
     * <ol>
     *   <li>Se escribe todo en un archivo TEMPORAL en la misma carpeta.</li>
     *   <li>Se fuerza a que esos bytes lleguen físicamente al disco ({@code force}).</li>
     *   <li>Se "renombra" el temporal sobre el archivo real con {@code ATOMIC_MOVE}. El sistema
     *       operativo garantiza que ese cambio es instantáneo: quien lea el archivo verá la
     *       versión vieja completa o la nueva completa, nunca una mezcla.</li>
     * </ol>
     * <p>El temporal se crea en la MISMA carpeta porque un movimiento atómico solo es posible
     * dentro del mismo disco.</p>
     *
     * @param archivo archivo destino (se crea la carpeta si no existe)
     * @param lineas  líneas a escribir, ya escapadas (no nulas)
     * @throws ExcepcionPersistencia si no se pudo escribir; en ese caso el archivo original
     *                               queda intacto
     */
    public static void escribirLineasAtomicamente(Path archivo, List<String> lineas) {
        Path destino = archivo.toAbsolutePath();
        Path temporal = null;
        // Se espera IOException (disco lleno, sin permisos, archivo bloqueado). El original no se
        // tocó, así que no hay pérdida: se informa con ExcepcionPersistencia y la UI muestra un
        // mensaje amigable. El bloque finally borra el temporal si quedó a medias.
        try {
            Files.createDirectories(destino.getParent());
            temporal = Files.createTempFile(destino.getParent(), destino.getFileName() + ".", ".tmp");
            escribirYForzarADisco(temporal, lineas);
            moverReemplazando(temporal, destino);
        } catch (IOException ex) {
            throw new ExcepcionPersistencia("No se pudo guardar el archivo " + destino.getFileName() + ".", ex);
        } finally {
            borrarTemporal(temporal);
        }
    }

    /**
     * Escribe las líneas en UTF-8 y obliga al sistema operativo a bajarlas al disco.
     *
     * @param archivo archivo temporal
     * @param lineas  líneas a escribir
     * @throws IOException si falla la escritura
     */
    private static void escribirYForzarADisco(Path archivo, List<String> lineas) throws IOException {
        try (BufferedWriter escritor = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            for (String linea : lineas) {
                escritor.write(linea);
                escritor.write(FIN_DE_LINEA);
            }
        }
        // Sin force(), los datos podrían quedarse en la memoria caché del sistema operativo y
        // perderse en un corte de luz aunque el "move" ya se hubiera hecho.
        try (FileChannel canal = FileChannel.open(archivo, StandardOpenOption.WRITE)) {
            canal.force(true);
        }
    }

    /**
     * Mueve el temporal sobre el destino. Primero intenta un movimiento atómico; si el sistema
     * de archivos no lo soporta, usa un reemplazo normal (mejor eso que no guardar).
     * Si Windows (o OneDrive / el antivirus) tiene el archivo bloqueado un instante, reintenta.
     *
     * @param temporal archivo temporal ya escrito
     * @param destino  archivo final
     * @throws IOException si no se pudo mover tras los reintentos
     */
    private static void moverReemplazando(Path temporal, Path destino) throws IOException {
        for (int intento = 1; ; intento++) {
            // Se espera AccessDeniedException cuando otro programa tiene el archivo abierto un
            // momento (muy común en carpetas sincronizadas con OneDrive). Se reintenta unas
            // pocas veces; si persiste, se propaga para que el usuario reciba un aviso.
            try {
                moverUnaVez(temporal, destino);
                return;
            } catch (AccessDeniedException ex) {
                if (intento >= INTENTOS_DE_MOVIMIENTO) {
                    throw ex;
                }
                esperarAntesDeReintentar(ex);
            }
        }
    }

    /**
     * Un solo intento de movimiento: atómico si se puede, normal si no.
     *
     * @param temporal archivo temporal
     * @param destino  archivo final
     * @throws IOException si falla el movimiento
     */
    private static void moverUnaVez(Path temporal, Path destino) throws IOException {
        // Se espera AtomicMoveNotSupportedException solo en sistemas de archivos que no soportan
        // renombrado atómico (raro). En ese caso se hace un reemplazo normal.
        try {
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Pausa breve entre reintentos.
     *
     * @param causa error que motivó el reintento (se relanza si el hilo es interrumpido)
     * @throws IOException si el hilo fue interrumpido mientras esperaba
     */
    private static void esperarAntesDeReintentar(IOException causa) throws IOException {
        // Se espera InterruptedException si alguien pide detener este hilo. Se restaura la marca
        // de interrupción (buena práctica) y se abandona el guardado con el error original.
        try {
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw causa;
        }
    }

    /**
     * Borra el temporal si quedó en disco (solo pasa si algo falló antes del movimiento).
     *
     * @param temporal archivo temporal, o {@code null} si ni siquiera se llegó a crear
     */
    private static void borrarTemporal(Path temporal) {
        if (temporal == null) {
            return;
        }
        // Se espera IOException si el temporal está bloqueado. No es grave (el original está
        // bien y .gitignore ignora *.tmp); se avisa por System.err y se continúa.
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException ex) {
            System.err.println("No se pudo borrar el temporal " + temporal.getFileName() + ": " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ escape de campos

    /**
     * Escapa un campo para poder guardarlo en una columna sin romper el formato.
     * Ejemplo: {@code hola|mundo} → {@code hola\|mundo}.
     *
     * @param valor texto original (nulo se trata como vacío)
     * @return texto escapado
     */
    public static String escaparCampo(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder resultado = new StringBuilder(valor.length() + 8);
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                // La barra invertida va primero en importancia: como es el carácter de escape,
                // si no se duplicara, "C:\nuevo" se leería después como "C:" + salto de línea.
                case '\\' -> resultado.append("\\\\");
                case '|' -> resultado.append("\\|");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                default -> resultado.append(c);
            }
        }
        return resultado.toString();
    }

    /**
     * Revierte {@link #escaparCampo}: convierte {@code \|}, {@code \\}, {@code \n}, {@code \r}
     * de vuelta a sus caracteres originales.
     *
     * @param campo texto escapado leído del archivo
     * @return texto original
     * @throws ExcepcionDatosCorruptos si hay una barra invertida sola al final o una secuencia de
     *                                 escape desconocida (por ejemplo {@code \x}): eso nunca lo
     *                                 produce {@link #escaparCampo}, así que la línea fue dañada
     */
    public static String desescaparCampo(String campo) {
        StringBuilder resultado = new StringBuilder(campo.length());
        for (int i = 0; i < campo.length(); i++) {
            char c = campo.charAt(i);
            if (c != ESCAPE) {
                resultado.append(c);
                continue;
            }
            if (i + 1 >= campo.length()) {
                throw new ExcepcionDatosCorruptos("barra invertida suelta al final de un campo");
            }
            resultado.append(traducirEscape(campo.charAt(++i)));
        }
        return resultado.toString();
    }

    /**
     * Traduce el carácter que sigue a una barra invertida.
     *
     * @param codigo carácter después de {@code \}
     * @return carácter original
     * @throws ExcepcionDatosCorruptos si el código no es uno de los permitidos
     */
    private static char traducirEscape(char codigo) {
        return switch (codigo) {
            case '\\' -> '\\';
            case '|' -> '|';
            case 'n' -> '\n';
            case 'r' -> '\r';
            default -> throw new ExcepcionDatosCorruptos("secuencia de escape inválida: \\" + codigo);
        };
    }

    // ------------------------------------------------------------------ dividir / unir registros

    /**
     * Divide una línea en columnas usando {@code |} como separador, PERO sin cortar en los
     * {@code \|} escapados. Los campos se devuelven todavía escapados.
     *
     * <p><b>Por qué no sirve {@code String.split("\\|")}:</b> split cortaría también en
     * {@code \|}, partiendo el texto de una publicación que contenga una barra vertical. Aquí se
     * recorre carácter por carácter: al ver una {@code \} se copia ella y el siguiente carácter
     * tal cual (sea lo que sea), y solo un {@code |} "desnudo" separa columnas.</p>
     *
     * @param linea línea completa
     * @return campos escapados, en orden (al menos uno)
     * @throws ExcepcionDatosCorruptos si la línea termina en una barra invertida suelta
     */
    public static List<String> dividirCampos(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == ESCAPE) {
                if (i + 1 >= linea.length()) {
                    throw new ExcepcionDatosCorruptos("la línea termina en una barra invertida suelta");
                }
                actual.append(c).append(linea.charAt(++i));
            } else if (c == SEPARADOR) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    /**
     * Escapa cada valor y los une con {@code |}. Es la operación inversa de {@link #leerRegistro}.
     *
     * @param valores valores originales de las columnas
     * @return línea lista para escribir en el archivo
     */
    public static String unirCampos(String... valores) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                linea.append(SEPARADOR);
            }
            linea.append(escaparCampo(valores[i]));
        }
        return linea.toString();
    }

    /**
     * Divide una línea, comprueba que tenga la cantidad de columnas esperada y desescapa cada
     * columna. Es la forma recomendada de leer un registro.
     *
     * @param linea           línea del archivo
     * @param camposEsperados cantidad exacta de columnas que debe tener
     * @return valores originales de las columnas
     * @throws ExcepcionDatosCorruptos si la cantidad de columnas no coincide o el escape es inválido
     */
    public static String[] leerRegistro(String linea, int camposEsperados) {
        List<String> crudos = dividirCampos(linea);
        if (crudos.size() != camposEsperados) {
            throw new ExcepcionDatosCorruptos("se esperaban " + camposEsperados + " columnas y hay " + crudos.size());
        }
        String[] valores = new String[crudos.size()];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = desescaparCampo(crudos.get(i));
        }
        return valores;
    }

    /**
     * Construye un encabezado de comentarios: antepone {@code "# "} a cada renglón.
     *
     * @param renglones textos del encabezado
     * @return líneas de comentario, en una lista no modificable
     */
    public static List<String> comoComentarios(String... renglones) {
        List<String> comentarios = new ArrayList<>(renglones.length);
        for (String renglon : renglones) {
            comentarios.add(PREFIJO_COMENTARIO + " " + renglon);
        }
        return Collections.unmodifiableList(comentarios);
    }
}
