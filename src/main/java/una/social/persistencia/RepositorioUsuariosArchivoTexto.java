/*
 * RepositorioUsuariosArchivoTexto.java
 *
 * Qué hace: guarda y lee el catálogo de usuarios en data/usuarios.txt con el formato
 * id|nombre|usuario|contrasena|fechaISO. La contraseña va en texto plano a propósito (proyecto
 * académico: permite verla e iniciar sesión durante la defensa).
 * Rol en la arquitectura: implementación concreta de RepositorioUsuarios. Solo Main la crea; el
 * resto del sistema la conoce únicamente por su interfaz.
 * Principio SOLID: SRP (traduce Usuario <-> línea de texto; no valida contraseñas ni guarda
 * nada en memoria), LSP (cumple el contrato de RepositorioUsuarios: omite líneas corruptas y
 * escribe "todo o nada"), DIP (recibe la auditoría por constructor).
 * Colabora con: AlmacenArchivoTexto, RegistroAuditoria, Usuario.
 */
package una.social.persistencia;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.modelo.Usuario;

/**
 * Repositorio de usuarios sobre un archivo de texto plano.
 */
public final class RepositorioUsuariosArchivoTexto implements RepositorioUsuarios {

    /** Cantidad de columnas de una línea de usuario. */
    private static final int CANTIDAD_COLUMNAS = 5;

    /** Posiciones de cada columna dentro de la línea. */
    private static final int COL_ID = 0;
    /** Columna del nombre completo. */
    private static final int COL_NOMBRE = 1;
    /** Columna del nombre de usuario. */
    private static final int COL_USUARIO = 2;
    /** Columna de la contraseña (texto plano). */
    private static final int COL_CONTRASENA = 3;
    /** Columna de la fecha de registro. */
    private static final int COL_FECHA = 4;

    /** Sal con la que se reconstruye el usuario al leer: vacía, porque no se usa hash. */
    private static final String SIN_SAL = "";

    /**
     * Encabezado que se escribe en cada guardado. Documenta el formato dentro del propio archivo;
     * el lector lo ignora porque sus líneas empiezan con '#'.
     */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Catálogo de usuarios (archivo generado por la aplicación)",
            "Una línea por usuario. Columnas separadas por '|':",
            "  1. id         -> identificador único U001, U002, ... (lo asigna el sistema)",
            "  2. nombre     -> nombre completo",
            "  3. usuario    -> nombre de usuario para iniciar sesión (único, sin distinguir mayúsculas)",
            "  4. contrasena -> contraseña en texto plano (a propósito: proyecto académico)",
            "  5. fechaISO   -> fecha y hora de registro en ISO-8601 UTC, ej. 2026-11-15T18:00:00Z",
            "Escape dentro de un campo: \\| = barra vertical, \\\\ = barra invertida, \\n y \\r = saltos de línea.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de usuarios.txt. */
    private final Path archivo;

    /** Bitácora donde se anotan lecturas, guardados y líneas corruptas omitidas. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio. No lee nada todavía: la lectura ocurre en {@link #cargarTodos()}.
     *
     * @param archivo   ruta de usuarios.txt (no nula)
     * @param auditoria bitácora de auditoría (no nula)
     */
    public RepositorioUsuariosArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cada línea dañada se omite y se audita con su número de línea; la carga continúa con el
     * resto. El motivo auditado nunca incluye el contenido de la línea (podría traer una contraseña).</p>
     */
    @Override
    public List<Usuario> cargarTodos() {
        List<LineaArchivo> lineas = AlmacenArchivoTexto.leerLineasDeDatos(archivo);
        List<Usuario> usuarios = new ArrayList<>(lineas.size());
        int omitidas = 0;
        for (LineaArchivo linea : lineas) {
            // Se espera ExcepcionDatosCorruptos cuando la línea no respeta el formato. Política:
            // omitir esa línea, auditarla y seguir con las demás, para no perder TODOS los
            // usuarios por culpa de una sola línea dañada. El usuario no ve nada en la UI.
            try {
                usuarios.add(convertirLinea(linea.texto()));
            } catch (ExcepcionDatosCorruptos ex) {
                omitidas++;
                auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                        archivo.getFileName() + " línea " + linea.numero() + ": " + ex.getMessage());
            }
        }
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "ARCHIVO_LEIDO",
                archivo.getFileName() + ": " + usuarios.size() + " usuarios válidos, " + omitidas + " líneas omitidas");
        return usuarios;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Usa la escritura atómica de {@link AlmacenArchivoTexto}: si falla, el archivo anterior
     * queda intacto.</p>
     */
    @Override
    public void guardarTodos(List<Usuario> usuarios) {
        List<String> lineas = new ArrayList<>(ENCABEZADO);
        for (Usuario usuario : usuarios) {
            lineas.add(convertirUsuario(usuario));
        }
        AlmacenArchivoTexto.escribirLineasAtomicamente(archivo, lineas);
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "ARCHIVO_GUARDADO",
                archivo.getFileName() + ": " + usuarios.size() + " usuarios");
    }

    /**
     * Convierte un usuario en una línea del archivo (con cada campo escapado).
     *
     * @param usuario usuario a convertir
     * @return línea lista para escribir
     */
    private static String convertirUsuario(Usuario usuario) {
        return AlmacenArchivoTexto.unirCampos(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getNombreUsuario(),
                usuario.getHashContrasena(),
                usuario.getFechaRegistro().toString());
    }

    /**
     * Convierte una línea del archivo en un usuario, validando cada columna. Aquí se usan
     * EXCEPCIONES (no asserts) porque el archivo es entrada externa: alguien pudo editarlo a mano
     * y los asserts podrían estar desactivados en producción.
     *
     * @param linea texto de la línea
     * @return usuario reconstruido
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Usuario convertirLinea(String linea) {
        String[] c = AlmacenArchivoTexto.leerRegistro(linea, CANTIDAD_COLUMNAS);
        String id = c[COL_ID].trim();
        if (!Usuario.PATRON_ID.matcher(id).matches()) {
            throw new ExcepcionDatosCorruptos("ID con formato inválido (se esperaba U seguido de 3 o más dígitos)");
        }
        exigirTexto(c[COL_NOMBRE], "nombre");
        exigirTexto(c[COL_USUARIO], "usuario");
        // La contraseña no se recorta (trim): los espacios podrían ser parte de ella.
        if (c[COL_CONTRASENA].isEmpty()) {
            throw new ExcepcionDatosCorruptos("la columna 'contrasena' está vacía");
        }
        return new Usuario(id, c[COL_NOMBRE].trim(), c[COL_USUARIO].trim(), c[COL_CONTRASENA], SIN_SAL,
                leerFecha(c[COL_FECHA]));
    }

    /**
     * Exige que una columna de texto no esté vacía.
     *
     * @param valor   valor de la columna
     * @param columna nombre de la columna (para el mensaje)
     * @throws ExcepcionDatosCorruptos si está vacía
     */
    private static void exigirTexto(String valor, String columna) {
        if (valor.isBlank()) {
            throw new ExcepcionDatosCorruptos("la columna '" + columna + "' está vacía");
        }
    }

    /**
     * Lee la fecha ISO-8601.
     *
     * @param valor texto de la fecha
     * @return instante leído
     * @throws ExcepcionDatosCorruptos si la fecha no tiene formato ISO-8601
     */
    private static Instant leerFecha(String valor) {
        // Se espera DateTimeParseException si la fecha está mal escrita; se traduce para omitir
        // la línea en lugar de detener toda la carga.
        try {
            return Instant.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new ExcepcionDatosCorruptos("la fecha no tiene formato ISO-8601", ex);
        }
    }
}
