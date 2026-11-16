/*
 * EnsambladorServicios.java
 *
 * Qué hace: crea y conecta los objetos del módulo de usuarios (repositorio, catálogo, hasheador,
 * control de intentos y gestor) a partir de la carpeta de datos.
 * Rol en la arquitectura: es parte de la "raíz de composición": el ÚNICO lugar (junto con Main)
 * donde se hace "new" de implementaciones concretas. Lo usan Main y HerramientaDatosSemilla,
 * así ambos arman el sistema exactamente igual sin duplicar código.
 * Principio SOLID: DIP. El resto del código recibe abstracciones ya armadas y nunca decide qué
 * implementación usar.
 * Colabora con: Main, HerramientaDatosSemilla, GestorUsuarios y sus dependencias.
 */
package una.social;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import una.social.auditoria.RegistroAuditoria;
import una.social.auditoria.RegistroAuditoriaArchivo;
import una.social.modelo.CatalogoUsuarios;
import una.social.persistencia.RepositorioUsuariosArchivoTexto;
import una.social.seguridad.ControlIntentosLogin;
import una.social.seguridad.HasheadorTextoPlano;
import una.social.servicio.GestorUsuarios;

/**
 * Fábrica de los servicios de Andrey.
 */
public final class EnsambladorServicios {

    /** Nombre del archivo del catálogo de usuarios dentro de data/. */
    public static final String ARCHIVO_USUARIOS = "usuarios.txt";

    /** Nombre del archivo de auditoría dentro de data/. */
    public static final String ARCHIVO_AUDITORIA = "auditoria.log";

    /** Intentos fallidos de login permitidos antes del bloqueo. */
    public static final int MAXIMO_INTENTOS_LOGIN = 5;

    /** Duración del bloqueo temporal tras agotar los intentos. */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);

    /** Clase de utilidades: no se instancia. */
    private EnsambladorServicios() {
    }

    /**
     * @param carpetaDatos carpeta data/
     * @return ruta del archivo de auditoría
     */
    public static Path rutaAuditoria(Path carpetaDatos) {
        return carpetaDatos.resolve(ARCHIVO_AUDITORIA);
    }

    /**
     * Crea la bitácora de auditoría sobre data/auditoria.log.
     *
     * @param carpetaDatos carpeta data/
     * @param reloj        reloj del sistema
     * @return bitácora lista para usar
     */
    public static RegistroAuditoria crearAuditoria(Path carpetaDatos, Clock reloj) {
        return new RegistroAuditoriaArchivo(rutaAuditoria(carpetaDatos), reloj);
    }

    /**
     * Crea el gestor de usuarios con todas sus dependencias reales. No carga datos todavía.
     *
     * @param carpetaDatos carpeta data/
     * @param auditoria    bitácora
     * @param reloj        reloj del sistema
     * @return gestor listo para {@code cargarDatos()}
     */
    public static GestorUsuarios crearGestorUsuarios(Path carpetaDatos, RegistroAuditoria auditoria, Clock reloj) {
        return new GestorUsuarios(
                new RepositorioUsuariosArchivoTexto(carpetaDatos.resolve(ARCHIVO_USUARIOS), auditoria),
                new CatalogoUsuarios(),
                // Contraseñas en texto plano en usuarios.txt (decisión académica, para la defensa).
                // Para volver a protegerlas: new HasheadorPbkdf2(HasheadorPbkdf2.ITERACIONES_PRODUCCION, new SecureRandom())
                new HasheadorTextoPlano(),
                new ControlIntentosLogin(MAXIMO_INTENTOS_LOGIN, DURACION_BLOQUEO, reloj),
                auditoria,
                reloj);
    }
}
