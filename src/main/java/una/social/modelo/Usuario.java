/*
 * Usuario.java
 *
 * Qué hace: representa a un usuario registrado en Social UNA.
 * Rol en la arquitectura: es el MODELO del dominio y también un contrato compartido: Sebastián
 * recibe un Usuario (inmutable) como "usuario activo" en su ventana principal y usa su ID para
 * relacionar amistades y publicaciones.
 * Principio SOLID: SRP (solo guarda datos del usuario; no sabe guardarse en disco ni validar
 * contraseñas: eso lo hacen el repositorio y el hasheador).
 * Colabora con: CatalogoUsuarios, RepositorioUsuariosArchivoTexto, GestorUsuarios, ContextoSesion.
 */
package una.social.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuario de la red social. Es INMUTABLE: todos sus atributos son {@code final} y no tiene
 * métodos "set".
 *
 * <p>¿Por qué inmutable? Porque se comparte entre ventanas, hilos (SwingWorker) y el módulo de
 * Sebastián. Si nadie lo puede modificar, nadie puede dejarlo en un estado inconsistente por
 * accidente y no hacen falta candados para leerlo desde varios hilos.</p>
 *
 * <p>La contraseña NUNCA se guarda aquí: solo su hash (PBKDF2) y la sal, ambos en Base64.</p>
 */
public final class Usuario {

    /**
     * Formato válido de un ID: la letra U seguida de al menos 3 dígitos (U001, U002, ..., U1000).
     * Se comparte aquí para que el repositorio (al leer) y el servicio (al generar) usen la misma
     * regla. {@link Pattern} es inmutable y seguro entre hilos.
     */
    public static final Pattern PATRON_ID = Pattern.compile("U\\d{3,}");

    /** Prefijo de los IDs de usuario. */
    public static final String PREFIJO_ID = "U";

    /** Identificador único con formato U001, U002, ... Lo asigna el sistema, no la persona. */
    private final String id;

    /** Nombre completo para mostrar (por ejemplo "Ana Mora"). */
    private final String nombre;

    /** Nombre de usuario para iniciar sesión, tal como lo escribió la persona al registrarse. */
    private final String nombreUsuario;

    /** Hash PBKDF2 de la contraseña, codificado en Base64. */
    private final String hashContrasena;

    /** Sal aleatoria usada al calcular el hash, codificada en Base64. */
    private final String sal;

    /** Momento exacto (UTC) en que se registró el usuario. */
    private final Instant fechaRegistro;

    /**
     * Crea un usuario.
     *
     * <p>Precondición: ningún parámetro es nulo. Si alguno lo es, es un error de programación
     * (los datos del usuario y del archivo se validan ANTES de llegar aquí), por eso se usa
     * {@link Objects#requireNonNull}, que falla de inmediato y con un mensaje claro.</p>
     *
     * @param id             identificador (formato U###)
     * @param nombre         nombre completo
     * @param nombreUsuario  nombre de usuario para el login
     * @param hashContrasena hash de la contraseña en Base64
     * @param sal            sal en Base64
     * @param fechaRegistro  fecha y hora del registro
     * @throws NullPointerException si algún parámetro es nulo
     */
    public Usuario(String id, String nombre, String nombreUsuario, String hashContrasena,
                   String sal, Instant fechaRegistro) {
        // Se guardan los datos recibidos; requireNonNull falla de inmediato si falta alguno.
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.nombreUsuario = Objects.requireNonNull(nombreUsuario, "nombreUsuario");
        this.hashContrasena = Objects.requireNonNull(hashContrasena, "hashContrasena");
        this.sal = Objects.requireNonNull(sal, "sal");
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }

    /**
     * @return identificador único (U001, U002, ...)
     */
    public String getId() {
        return id;
    }

    /**
     * @return nombre completo para mostrar
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return nombre de usuario tal como se registró (respeta mayúsculas originales)
     */
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    /**
     * @return hash PBKDF2 de la contraseña en Base64 (solo para persistencia y verificación)
     */
    public String getHashContrasena() {
        return hashContrasena;
    }

    /**
     * @return sal en Base64 (solo para persistencia y verificación)
     */
    public String getSal() {
        return sal;
    }

    /**
     * @return fecha y hora del registro (UTC)
     */
    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Dos usuarios son el mismo si tienen el mismo ID, porque el ID es su identidad en todo el
     * sistema (amistades y publicaciones se relacionan por ID).
     *
     * @param otro objeto a comparar
     * @return {@code true} si representan al mismo usuario
     */
    @Override
    public boolean equals(Object otro) {
        // Es exactamente el mismo objeto.
        if (this == otro) {
            return true;
        }
        // No es un Usuario (o es null): no pueden ser iguales.
        if (!(otro instanceof Usuario)) {
            return false;
        }
        // Se comparan solo los IDs.
        return id.equals(((Usuario) otro).id);
    }

    /**
     * Coherente con {@link #equals(Object)}: se basa solo en el ID.
     *
     * @return código hash del ID
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Texto para depuración. NO incluye el hash ni la sal a propósito, para que nunca terminen
     * en la consola ni en la auditoría por descuido.
     *
     * @return representación segura del usuario
     */
    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nombreUsuario=" + nombreUsuario + ", nombre=" + nombre + "}";
    }
}
