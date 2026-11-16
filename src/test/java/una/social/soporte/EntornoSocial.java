/*
 * EntornoSocial.java
 *
 * Qué hace: arma, para las PRUEBAS, el módulo social completo y real (los mismos gestores,
 * repositorios de archivos y oyentes que usa la aplicación) sobre una carpeta temporal, con un
 * directorio de usuarios en memoria y un reloj controlable.
 * Rol en la arquitectura: soporte de pruebas. Permite probar reglas que cruzan módulos (bloqueos que
 * rompen amistades, borrado en cascada, muros con privacidad) y simular "cerrar y volver a abrir la
 * aplicación" con {@link #recargar()}.
 * Principio SOLID: DIP (usa EnsambladorSocial, la misma raíz de composición que Main).
 * Colabora con: EnsambladorSocial, ModulosSociales, DirectorioEnMemoria, AuditoriaEnMemoria,
 * RelojAjustable.
 */
package una.social.soporte;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import una.social.EnsambladorSocial;
import una.social.ModulosSociales;
import una.social.modelo.Usuario;
import una.social.persistencia.ModuloPersistente;

/**
 * Entorno social completo para pruebas.
 */
public final class EntornoSocial {

    /** Carpeta temporal con los archivos de datos. */
    private final Path carpeta;

    /** Usuarios de prueba (Ana U001, Luis U002, María U003, más los que se agreguen). */
    private final DirectorioEnMemoria directorio = DirectorioEnMemoria.conUsuariosDePrueba();

    /** Bitácora en memoria (para revisar qué se auditó). */
    private final AuditoriaEnMemoria auditoria = new AuditoriaEnMemoria();

    /** Reloj controlable (cada acción puede tener una fecha distinta). */
    private final RelojAjustable reloj = new RelojAjustable(Instant.parse("2026-11-01T12:00:00Z"));

    /** Módulos sociales cargados. */
    private ModulosSociales modulos;

    /**
     * Crea el entorno y carga los módulos (archivos vacíos).
     *
     * @param carpeta carpeta temporal de la prueba
     */
    public EntornoSocial(Path carpeta) {
        this.carpeta = carpeta;
        this.modulos = crearYCargar();
    }

    /**
     * Simula cerrar y volver a abrir la aplicación: crea módulos NUEVOS que leen los mismos archivos.
     *
     * @return los módulos recién cargados (también quedan como los actuales)
     */
    public ModulosSociales recargar() {
        modulos = crearYCargar();
        return modulos;
    }

    /**
     * Crea los módulos con el ensamblador real y los carga en el mismo orden que Main.
     *
     * @return módulos cargados
     */
    private ModulosSociales crearYCargar() {
        ModulosSociales nuevos = EnsambladorSocial.crear(carpeta, directorio, auditoria, reloj);
        nuevos.fasePrincipal().forEach(ModuloPersistente::cargarDatos);
        nuevos.faseInteracciones().forEach(ModuloPersistente::cargarDatos);
        return nuevos;
    }

    /**
     * Adelanta el reloj una hora (para que cada acción tenga una fecha distinta y el orden sea claro).
     */
    public void avanzar() {
        reloj.adelantar(Duration.ofHours(1));
    }

    /**
     * @return módulos actuales
     */
    public ModulosSociales m() {
        return modulos;
    }

    /**
     * @param id ID del usuario
     * @return el usuario (falla la prueba si no existe)
     */
    public Usuario u(String id) {
        return directorio.obtener(id);
    }

    /**
     * Agrega un usuario de prueba extra.
     *
     * @param id      ID
     * @param nombre  nombre completo
     * @param usuario nombre de usuario
     * @return el usuario agregado
     */
    public Usuario agregarUsuario(String id, String nombre, String usuario) {
        return directorio.agregar(id, nombre, usuario);
    }

    /**
     * @return directorio de usuarios
     */
    public DirectorioEnMemoria directorio() {
        return directorio;
    }

    /**
     * @return bitácora en memoria
     */
    public AuditoriaEnMemoria auditoria() {
        return auditoria;
    }

    /**
     * @param nombreArchivo nombre del archivo dentro de la carpeta de datos
     * @return ruta del archivo
     */
    public Path archivo(String nombreArchivo) {
        return carpeta.resolve(nombreArchivo);
    }
}
