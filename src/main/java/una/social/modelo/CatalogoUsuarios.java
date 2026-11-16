/*
 * CatalogoUsuarios.java
 *
 * Qué hace: es la ESTRUCTURA DE DATOS en memoria que guarda a todos los usuarios registrados.
 * Rol en la arquitectura: es la "memoria principal" que pide el enunciado. Al iniciar, el
 * GestorUsuarios la llena con lo leído del archivo; durante la ejecución todas las búsquedas
 * se hacen aquí, no en el disco.
 * Principio SOLID: SRP. Solo organiza usuarios en memoria; no valida contraseñas ni toca
 * archivos.
 * Colabora con: Usuario, GestorUsuarios.
 */
package una.social.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import una.social.excepciones.ExcepcionUsuarioDuplicado;

/**
 * Catálogo de usuarios en memoria.
 *
 * <p><b>Estructuras de datos usadas y por qué:</b></p>
 * <ul>
 *   <li>{@code ArrayList<Usuario>} (lista dinámica): guarda a los usuarios en el orden en que se
 *       registraron. Crece sola y permite recorrerlos para listarlos o guardarlos. Es la "lista
 *       dinámica para el catálogo de usuarios" que exige el enunciado.</li>
 *   <li>{@code HashMap<String, Usuario>} por nombre de usuario en minúsculas (índice auxiliar):
 *       buscar en una lista es O(n) porque hay que revisar uno por uno; un HashMap encuentra la
 *       clave en O(1) promedio. Se usa para el login y para detectar duplicados al registrar.
 *       La clave va en minúsculas para que "Ana" y "ana" se consideren el mismo usuario.</li>
 *   <li>{@code HashMap<String, Usuario>} por ID: búsqueda O(1) por ID, que es lo que usará el
 *       módulo de Sebastián para mostrar el autor de una publicación o un amigo.</li>
 * </ul>
 *
 * <p><b>Invariante</b> (siempre debe cumplirse): la lista y los dos índices contienen
 * exactamente los mismos usuarios. Se comprueba con {@code assert} después de cada cambio.</p>
 *
 * <p>No es seguro entre hilos por sí solo; el {@code GestorUsuarios} sincroniza su acceso.</p>
 */
public final class CatalogoUsuarios {

    /** Lista dinámica con todos los usuarios, en orden de registro. */
    private final List<Usuario> usuarios = new ArrayList<>();

    /** Índice: nombre de usuario en minúsculas → usuario. */
    private final Map<String, Usuario> indicePorNombreUsuario = new HashMap<>();

    /** Índice: ID → usuario. */
    private final Map<String, Usuario> indicePorId = new HashMap<>();

    /**
     * Convierte un nombre de usuario a su clave de búsqueda: sin espacios a los lados y en
     * minúsculas. Se usa {@link Locale#ROOT} para que el resultado no dependa del idioma del
     * sistema operativo (en turco, por ejemplo, "I".toLowerCase() no da "i").
     *
     * @param nombreUsuario nombre escrito por la persona (puede ser nulo)
     * @return clave normalizada; cadena vacía si era nulo
     */
    public static String normalizarClave(String nombreUsuario) {
        return nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Agrega un usuario al catálogo.
     *
     * <p>Poscondición: el usuario queda en la lista y en ambos índices.</p>
     *
     * @param usuario usuario a agregar (no nulo)
     * @throws ExcepcionUsuarioDuplicado si ya existe el mismo nombre de usuario (sin distinguir
     *                                   mayúsculas) o el mismo ID
     */
    public void agregar(Usuario usuario) {
        String clave = normalizarClave(usuario.getNombreUsuario());
        if (indicePorNombreUsuario.containsKey(clave)) {
            throw new ExcepcionUsuarioDuplicado("Ese nombre de usuario ya está en uso.");
        }
        if (indicePorId.containsKey(usuario.getId())) {
            throw new ExcepcionUsuarioDuplicado("Ya existe un usuario con el ID " + usuario.getId() + ".");
        }
        usuarios.add(usuario);
        indicePorNombreUsuario.put(clave, usuario);
        indicePorId.put(usuario.getId(), usuario);
        // Invariante: lista e índices deben tener el mismo tamaño. Si no, hay un error de
        // programación en esta clase (no un error del usuario), por eso es un assert.
        assert estructurasSincronizadas() : "La lista y los índices del catálogo quedaron desincronizados";
    }

    /**
     * Vacía el catálogo (se usa antes de recargar los datos desde el archivo).
     *
     * <p>Poscondición: lista e índices quedan vacíos.</p>
     */
    public void limpiar() {
        usuarios.clear();
        indicePorNombreUsuario.clear();
        indicePorId.clear();
    }

    /**
     * Busca por nombre de usuario sin distinguir mayúsculas. Costo O(1) promedio gracias al índice.
     *
     * @param nombreUsuario nombre a buscar (puede ser nulo o vacío)
     * @return el usuario si existe; vacío en caso contrario
     */
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return Optional.ofNullable(indicePorNombreUsuario.get(normalizarClave(nombreUsuario)));
    }

    /**
     * Busca por ID exacto. Costo O(1) promedio gracias al índice.
     *
     * @param id ID a buscar (puede ser nulo)
     * @return el usuario si existe; vacío en caso contrario
     */
    public Optional<Usuario> buscarPorId(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(indicePorId.get(id.trim()));
    }

    /**
     * Indica si el nombre de usuario ya está tomado.
     *
     * @param nombreUsuario nombre a revisar
     * @return {@code true} si existe
     */
    public boolean contieneNombreUsuario(String nombreUsuario) {
        return indicePorNombreUsuario.containsKey(normalizarClave(nombreUsuario));
    }

    /**
     * Devuelve una COPIA no modificable de la lista. Es copia para que quien la reciba no pueda
     * alterar el catálogo interno (y para poder recorrerla en otro hilo sin problemas).
     *
     * @return lista inmutable de usuarios en orden de registro
     */
    public List<Usuario> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(usuarios));
    }

    /**
     * @return cantidad de usuarios en el catálogo
     */
    public int tamano() {
        return usuarios.size();
    }

    /**
     * Comprueba la invariante interna. Solo se usa dentro de {@code assert}.
     *
     * @return {@code true} si la lista y los índices tienen el mismo tamaño
     */
    private boolean estructurasSincronizadas() {
        return usuarios.size() == indicePorNombreUsuario.size() && usuarios.size() == indicePorId.size();
    }
}
