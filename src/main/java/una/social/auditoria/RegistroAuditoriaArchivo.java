/*
 * RegistroAuditoriaArchivo.java
 *
 * Qué hace: implementación real de la bitácora de auditoría sobre el archivo
 * data/auditoria.log. Cada evento es una línea con hash SHA-256 encadenado.
 * Rol en la arquitectura: se crea UNA sola vez en Main y se inyecta por constructor a quien la
 * necesite (servicio, repositorio, UI, módulo de Sebastián).
 * Principio SOLID: SRP (solo escribe la bitácora), LSP (cumple todo el contrato de
 * RegistroAuditoria: nunca lanza excepciones y es segura entre hilos), DIP (los demás dependen
 * de la interfaz, no de esta clase).
 * Colabora con: FormatoLineaAuditoria, NivelAuditoria, VerificadorIntegridadAuditoria.
 */
package una.social.auditoria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Bitácora de auditoría append-only sobre un archivo de texto.
 *
 * <p><b>¿Por qué "append-only"?</b> El archivo se abre SIEMPRE con
 * {@link StandardOpenOption#APPEND}: el sistema operativo escribe al final y nunca sobre lo
 * existente. Esta clase no tiene ningún método para borrar ni reescribir. Y si alguien edita el
 * archivo a mano con un editor, la cadena de hashes lo delata (ver {@link FormatoLineaAuditoria}).</p>
 */
public final class RegistroAuditoriaArchivo implements RegistroAuditoria {

    /** Largo máximo del detalle; evita que un mensaje enorme infle la bitácora. */
    private static final int LARGO_MAXIMO_DETALLE = 500;

    /** Acción que se anota si quien llama no indica ninguna. */
    private static final String ACCION_POR_DEFECTO = "SIN_ACCION";

    /** Fin de línea fijo para que el archivo sea igual en Windows y Linux. */
    private static final String FIN_DE_LINEA = "\n";

    /** Ruta del archivo de auditoría. */
    private final Path archivo;

    /** Reloj del que se toma la hora; inyectado para poder controlarlo en las pruebas. */
    private final Clock reloj;

    /**
     * Candado privado para que solo un hilo escriba a la vez. Es un objeto privado (y no
     * {@code this}) para que ningún código externo pueda bloquearlo por accidente.
     */
    private final Object candado = new Object();

    /** Hash de la última línea escrita: el "eslabón" al que se engancha la siguiente línea. */
    private String hashUltimaLinea;

    /**
     * Crea la bitácora. Si el archivo ya existe, continúa la cadena desde su última línea, de
     * modo que la cadena NO se reinicia cada vez que se abre la aplicación.
     *
     * <p>Nunca lanza excepciones por problemas de disco: si no puede preparar el archivo, lo
     * informa por {@code System.err} y sigue (cada {@link #registrar} volverá a intentarlo).</p>
     *
     * @param archivo ruta del archivo (no nula)
     * @param reloj   reloj del sistema (no nulo)
     * @throws NullPointerException si algún parámetro es nulo (error de programación)
     */
    public RegistroAuditoriaArchivo(Path archivo, Clock reloj) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        prepararArchivo();
        this.hashUltimaLinea = leerHashDeUltimaLinea();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es {@code synchronized} (mediante el candado) por dos razones: (1) que dos hilos no
     * mezclen sus líneas, y (2) que cada línea tome el hash de la anterior sin que otro hilo
     * escriba en medio, lo que rompería la cadena.</p>
     */
    @Override
    public void registrar(NivelAuditoria nivel, String actor, String accion, String detalle) {
        // Este try/catch es la "red de seguridad" que exige el contrato: capture lo que capture
        // (disco lleno, archivo bloqueado o hasta un error inesperado), la auditoría NO debe
        // tumbar la aplicación. El usuario no ve nada; el problema queda en System.err como
        // último recurso, porque no hay otro lugar donde dejarlo.
        try {
            // Solo un hilo a la vez puede escribir en la auditoría.
            synchronized (candado) {
                // Arma la línea: fecha|nivel|actor|acción|detalle|hash (encadenado con el hash anterior).
                String linea = FormatoLineaAuditoria.construirLinea(
                        fechaActualIso(),
                        nivel == null ? NivelAuditoria.INFO : nivel,
                        textoSeguro(actor, ACTOR_SISTEMA),
                        textoSeguro(accion, ACCION_POR_DEFECTO),
                        recortarDetalle(detalle),
                        hashUltimaLinea);
                // CREATE: crea el archivo si no existe. APPEND: escribe SIEMPRE al final.
                Files.writeString(archivo, linea + FIN_DE_LINEA, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                // El hash solo avanza si la escritura tuvo éxito; si falló, la próxima línea se
                // engancha a la última que sí quedó en disco y la cadena sigue siendo válida.
                String[] partes = FormatoLineaAuditoria.separarContenidoYHash(linea);
                assert partes != null : "construirLinea siempre produce una línea con separador";
                hashUltimaLinea = partes[1];
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("[AUDITORÍA] No se pudo registrar el evento '" + accion + "': "
                    + RegistroAuditoria.describirError(ex));
        }
    }

    /**
     * Crea la carpeta y el archivo si no existen.
     */
    private void prepararArchivo() {
        // Se espera IOException si no hay permisos o la ruta es inválida. No se relanza: la
        // aplicación debe poder abrir igual; solo se avisa por System.err.
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            if (Files.notExists(archivo)) {
                Files.createFile(archivo);
            }
        } catch (IOException ex) {
            System.err.println("[AUDITORÍA] No se pudo preparar el archivo: " + RegistroAuditoria.describirError(ex));
        }
    }

    /**
     * Busca el hash de la última línea del archivo para continuar la cadena.
     *
     * <p>Si el archivo está vacío, o su última línea está dañada, se usa el hash génesis. En el
     * caso dañado la cadena ya está rota, y el verificador lo reporta al iniciar la app.</p>
     *
     * @return hash de la última línea o {@link FormatoLineaAuditoria#HASH_GENESIS}
     */
    private String leerHashDeUltimaLinea() {
        // Se espera IOException si el archivo no se puede leer. Se continúa con el hash génesis:
        // la bitácora sigue funcionando y el verificador mostrará la ruptura.
        try {
            if (Files.notExists(archivo)) {
                return FormatoLineaAuditoria.HASH_GENESIS;
            }
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            for (int i = lineas.size() - 1; i >= 0; i--) {
                if (!lineas.get(i).isEmpty()) {
                    String[] partes = FormatoLineaAuditoria.separarContenidoYHash(lineas.get(i));
                    return partes == null ? FormatoLineaAuditoria.HASH_GENESIS : partes[1];
                }
            }
            return FormatoLineaAuditoria.HASH_GENESIS;
        } catch (IOException ex) {
            System.err.println("[AUDITORÍA] No se pudo leer la última línea: " + RegistroAuditoria.describirError(ex));
            return FormatoLineaAuditoria.HASH_GENESIS;
        }
    }

    /**
     * @return fecha y hora actual en ISO-8601 con zona horaria y milisegundos
     *         (por ejemplo {@code 2026-11-15T18:30:00.123-06:00})
     */
    private String fechaActualIso() {
        return OffsetDateTime.now(reloj).truncatedTo(ChronoUnit.MILLIS).toString();
    }

    /**
     * Devuelve el texto sin espacios a los lados o un valor por defecto si viene vacío.
     *
     * @param texto       texto recibido
     * @param porDefecto  valor a usar si el texto es nulo o vacío
     * @return texto seguro para la bitácora
     */
    private static String textoSeguro(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto.trim();
    }

    /**
     * Limita el detalle a {@link #LARGO_MAXIMO_DETALLE} caracteres.
     *
     * @param detalle detalle recibido (puede ser nulo)
     * @return detalle recortado (nunca nulo)
     */
    private static String recortarDetalle(String detalle) {
        if (detalle == null) {
            return "";
        }
        return detalle.length() <= LARGO_MAXIMO_DETALLE ? detalle : detalle.substring(0, LARGO_MAXIMO_DETALLE) + "…";
    }
}
