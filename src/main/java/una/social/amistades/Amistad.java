/*
 * Amistad.java
 *
 * Qué hace: representa una amistad entre dos usuarios (una ARISTA del grafo de amistades).
 * Rol en la arquitectura: modelo del módulo de amistades. Se guarda como una línea de
 * data/amistades.txt y se usa para reconstruir el grafo al iniciar.
 * Principio SOLID: SRP (solo datos; record inmutable).
 * Colabora con: GrafoAmistades, GestorAmistades, RepositorioAmistadesArchivoTexto.
 */
package una.social.amistades;

import java.time.Instant;
import java.util.Objects;

/**
 * Amistad NO dirigida entre dos usuarios: si A es amigo de B, B es amigo de A.
 *
 * <p>Se guarda quién la creó primero ({@code idUsuarioA} = quien pulsó "Agregar como amigo"),
 * pero para el grafo el orden no importa.</p>
 *
 * @param idUsuarioA ID del usuario que agregó al otro
 * @param idUsuarioB ID del usuario agregado
 * @param fecha      momento en que se creó la amistad
 */
public record Amistad(String idUsuarioA, String idUsuarioB, Instant fecha) {

    /**
     * Valida que no haya valores nulos (son errores de programación: los datos externos se
     * validan antes, en el repositorio y en el gestor).
     *
     * @param idUsuarioA ID del usuario A
     * @param idUsuarioB ID del usuario B
     * @param fecha      fecha de la amistad
     */
    public Amistad {
        Objects.requireNonNull(idUsuarioA, "idUsuarioA");
        Objects.requireNonNull(idUsuarioB, "idUsuarioB");
        Objects.requireNonNull(fecha, "fecha");
    }

    /**
     * @param idUsuario ID a revisar
     * @return {@code true} si el usuario es uno de los dos extremos de la amistad
     */
    public boolean involucraA(String idUsuario) {
        return idUsuarioA.equals(idUsuario) || idUsuarioB.equals(idUsuario);
    }

    /**
     * Devuelve el "otro" usuario de la amistad.
     *
     * @param idUsuario uno de los dos extremos
     * @return el ID del otro extremo
     */
    public String otroExtremo(String idUsuario) {
        // Precondición interna: solo se llama con uno de los dos extremos.
        assert involucraA(idUsuario) : "El usuario " + idUsuario + " no participa en esta amistad";
        return idUsuarioA.equals(idUsuario) ? idUsuarioB : idUsuarioA;
    }
}
