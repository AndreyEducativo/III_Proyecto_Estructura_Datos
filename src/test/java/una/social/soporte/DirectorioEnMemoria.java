/*
 * DirectorioEnMemoria.java
 *
 * Qué hace: implementación de DirectorioUsuarios para PRUEBAS, con usuarios fijos en memoria.
 * Rol en la arquitectura: doble de prueba; permite probar amistades, publicaciones y buscador
 * sin depender del módulo de usuarios ni de archivos.
 * Principio SOLID: LSP y DIP en acción (los gestores sociales solo conocen la interfaz).
 * Colabora con: las pruebas del módulo social.
 */
package una.social.soporte;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import una.social.modelo.Usuario;
import una.social.servicio.DirectorioUsuarios;

/**
 * Directorio de usuarios en memoria para pruebas.
 */
public final class DirectorioEnMemoria implements DirectorioUsuarios {

    /** Usuarios registrados en el directorio. */
    private final List<Usuario> usuarios = new ArrayList<>();

    /**
     * Crea el directorio con los 3 usuarios de prueba del proyecto.
     *
     * @return directorio con U001 Ana Mora, U002 Luis Rojas y U003 María Vargas
     */
    public static DirectorioEnMemoria conUsuariosDePrueba() {
        DirectorioEnMemoria directorio = new DirectorioEnMemoria();
        directorio.agregar("U001", "Ana Mora", "ana");
        directorio.agregar("U002", "Luis Rojas", "luis");
        directorio.agregar("U003", "María Vargas", "maria");
        return directorio;
    }

    /**
     * Agrega un usuario (con hash y sal ficticios: no se usan en estas pruebas).
     *
     * @param id      ID
     * @param nombre  nombre completo
     * @param usuario nombre de usuario
     * @return el usuario agregado
     */
    public Usuario agregar(String id, String nombre, String usuario) {
        Usuario nuevo = new Usuario(id, nombre, usuario, "aGFzaA==", "c2Fs", Instant.EPOCH);
        usuarios.add(nuevo);
        return nuevo;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Usuario> buscarPorId(String id) {
        return usuarios.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        String clave = nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase(Locale.ROOT);
        return usuarios.stream().filter(u -> u.getNombreUsuario().toLowerCase(Locale.ROOT).equals(clave)).findFirst();
    }

    /** {@inheritDoc} */
    @Override
    public List<Usuario> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(usuarios));
    }

    /**
     * @param id ID buscado
     * @return el usuario (falla la prueba si no existe)
     */
    public Usuario obtener(String id) {
        return buscarPorId(id).orElseThrow();
    }
}
