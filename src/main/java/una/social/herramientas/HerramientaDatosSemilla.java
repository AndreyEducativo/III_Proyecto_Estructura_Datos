/*
 * HerramientaDatosSemilla.java
 *
 * Qué hace: crea los 3 usuarios de prueba (U001 Ana Mora, U002 Luis Rojas, U003 Maria Vargas)
 * con contraseñas conocidas, documentadas en el README.
 * Rol en la arquitectura: herramienta para desarrolladores. Se usa de dos formas:
 *   1. Main la llama automáticamente si el catálogo está vacío (primera ejecución).
 *   2. Se puede ejecutar sola (tiene main) para regenerar data/usuarios.txt.
 * Principio SOLID: DIP (sembrar usa las interfaces DirectorioUsuarios y ServicioRegistro, así
 * reutiliza las mismas validaciones y el mismo hash que un registro normal, sin duplicar lógica).
 * Colabora con: ServicioRegistro, DirectorioUsuarios, EnsambladorServicios, RegistroAuditoria.
 */
package una.social.herramientas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import una.social.EnsambladorServicios;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.servicio.DirectorioUsuarios;
import una.social.servicio.GestorUsuarios;
import una.social.servicio.ServicioRegistro;

/**
 * Generador de los usuarios de prueba.
 *
 * <p>Uso por consola (solo para desarrolladores; la aplicación en sí es 100 % gráfica):</p>
 * <pre>
 * java -cp target/social-una.jar una.social.herramientas.HerramientaDatosSemilla             (crea si está vacío)
 * java -cp target/social-una.jar una.social.herramientas.HerramientaDatosSemilla --reiniciar (respalda y recrea)
 * </pre>
 */
public final class HerramientaDatosSemilla {

    /** Argumento para respaldar el archivo actual y recrearlo solo con los 3 usuarios de prueba. */
    private static final String ARGUMENTO_REINICIAR = "--reiniciar";

    /** Extensión del respaldo que se hace antes de reiniciar. */
    private static final String EXTENSION_RESPALDO = ".bak";

    /**
     * Datos de un usuario de prueba. Las contraseñas están aquí A PROPÓSITO y documentadas en el
     * README porque son cuentas de demostración, no cuentas reales.
     *
     * @param nombre     nombre completo
     * @param usuario    nombre de usuario
     * @param contrasena contraseña de prueba
     */
    private record UsuarioSemilla(String nombre, String usuario, String contrasena) {
    }

    /** Usuarios de prueba, en el orden en que deben recibir U001, U002 y U003. */
    private static final List<UsuarioSemilla> USUARIOS_SEMILLA = List.of(
            new UsuarioSemilla("Ana Mora", "ana", "ana12345"),
            new UsuarioSemilla("Luis Rojas", "luis", "luis12345"),
            new UsuarioSemilla("Maria Vargas", "maria", "maria12345"));

    /** Clase de utilidades: no se instancia. */
    private HerramientaDatosSemilla() {
    }

    /**
     * Si no hay ningún usuario registrado, crea los 3 de prueba. Como el catálogo está vacío,
     * reciben los IDs U001, U002 y U003 en ese orden.
     *
     * @param directorio consulta de usuarios
     * @param registro   servicio de registro
     * @param auditoria  bitácora
     * @return cuántos usuarios se crearon (0 si ya había usuarios)
     */
    public static int sembrarSiEstaVacio(DirectorioUsuarios directorio, ServicioRegistro registro, RegistroAuditoria auditoria) {
        if (!directorio.listarTodos().isEmpty()) {
            return 0;
        }
        for (UsuarioSemilla semilla : USUARIOS_SEMILLA) {
            char[] contrasena = semilla.contrasena().toCharArray();
            char[] confirmacion = semilla.contrasena().toCharArray();
            // finally: se borran las copias de las contraseñas aunque el registro falle.
            try {
                registro.registrar(semilla.nombre(), semilla.usuario(), contrasena, confirmacion);
            } finally {
                Arrays.fill(contrasena, '\0');
                Arrays.fill(confirmacion, '\0');
            }
        }
        auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_SEMILLA_CREADOS",
                USUARIOS_SEMILLA.size() + " usuarios de prueba (U001-U003)");
        return USUARIOS_SEMILLA.size();
    }

    /**
     * Punto de entrada de la herramienta.
     *
     * @param args {@code --reiniciar} para respaldar usuarios.txt y recrearlo desde cero
     * @throws IOException si no se pudo hacer el respaldo
     */
    public static void main(String[] args) throws IOException {
        Path carpetaDatos = Path.of("data");
        Clock reloj = Clock.systemDefaultZone();
        RegistroAuditoria auditoria = EnsambladorServicios.crearAuditoria(carpetaDatos, reloj);
        if (Arrays.asList(args).contains(ARGUMENTO_REINICIAR)) {
            respaldarYVaciar(carpetaDatos.resolve(EnsambladorServicios.ARCHIVO_USUARIOS), auditoria);
        }
        GestorUsuarios gestor = EnsambladorServicios.crearGestorUsuarios(carpetaDatos, auditoria, reloj);
        gestor.cargarDatos();
        int creados = sembrarSiEstaVacio(gestor, gestor, auditoria);
        System.out.println(creados > 0
                ? "Se crearon " + creados + " usuarios de prueba en data/usuarios.txt"
                : "data/usuarios.txt ya tenía usuarios; no se cambió nada (use --reiniciar para recrearlo).");
    }

    /**
     * Mueve el archivo actual a un respaldo .bak, para que la siguiente carga empiece vacía sin
     * destruir los datos anteriores.
     *
     * @param archivoUsuarios ruta de usuarios.txt
     * @param auditoria       bitácora
     * @throws IOException si no se pudo mover
     */
    private static void respaldarYVaciar(Path archivoUsuarios, RegistroAuditoria auditoria) throws IOException {
        if (Files.exists(archivoUsuarios)) {
            Path respaldo = archivoUsuarios.resolveSibling(archivoUsuarios.getFileName() + EXTENSION_RESPALDO);
            Files.move(archivoUsuarios, respaldo, StandardCopyOption.REPLACE_EXISTING);
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_REINICIADOS",
                    "usuarios.txt respaldado en " + respaldo.getFileName());
        }
    }
}
