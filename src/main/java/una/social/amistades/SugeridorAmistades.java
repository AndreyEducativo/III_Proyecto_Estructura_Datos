/*
 * SugeridorAmistades.java
 *
 * Qué hace: calcula "Personas que quizás conozcas" con un RECORRIDO EN ANCHURA (BFS) de 2 niveles
 * sobre el grafo de amistades, y cuenta los amigos en común entre dos usuarios.
 * Rol en la arquitectura: servicio del módulo de amistades que usan el buscador y los perfiles.
 * Principio SOLID: SRP (solo sugiere; no crea amistades), ISP y DIP (depende únicamente de
 * consultas de solo lectura: directorio, amistades, solicitudes y bloqueos).
 * Colabora con: ConsultaAmistades, ConsultaSolicitudes, ConsultaBloqueos, DirectorioUsuarios.
 */
package una.social.amistades;

import java.text.Collator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import una.social.bloqueos.ConsultaBloqueos;
import una.social.modelo.Usuario;
import una.social.servicio.DirectorioUsuarios;

/**
 * Sugeridor de amistades basado en BFS.
 *
 * <p><b>¿Qué es un recorrido en anchura (BFS)? (explicado simple)</b> Es la forma de explorar un
 * grafo "por capas", como las ondas que se forman al tirar una piedra al agua:</p>
 * <ol>
 *   <li>Nivel 0: yo.</li>
 *   <li>Nivel 1: mis amigos (los vecinos de mi nodo).</li>
 *   <li>Nivel 2: los amigos de mis amigos que NO están en los niveles anteriores.</li>
 * </ol>
 * <p>Se usa una COLA: se saca un usuario del frente, se miran sus vecinos y los que no se habían
 * visto se meten al final con nivel + 1. Un mapa "distancia" recuerda el nivel de cada usuario ya
 * visto, para no visitarlo dos veces. Aquí el recorrido se detiene en el nivel 2: esos son los
 * candidatos a sugerencia.</p>
 *
 * <p><b>Amigos en común:</b> cada vez que, desde uno de mis amigos (nivel 1), se llega a un
 * candidato, ese amigo es un amigo en común. Se cuentan con un {@code HashMap<candidato, contador>}.
 * Costo: O(V + E) de la parte del grafo explorada.</p>
 */
public final class SugeridorAmistades implements ServicioSugerencias {

    /** Idioma para el desempate alfabético. */
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    /** Profundidad máxima del recorrido: 2 = amigos de mis amigos. */
    private static final int PROFUNDIDAD_MAXIMA = 2;

    /** Usuarios registrados. */
    private final DirectorioUsuarios directorio;

    /** Grafo de amistades (solo lectura). */
    private final ConsultaAmistades amistades;

    /** Solicitudes pendientes (para excluir a quien ya tiene una conmigo). */
    private final ConsultaSolicitudes solicitudes;

    /** Bloqueos (para excluir a usuarios bloqueados en cualquier sentido). */
    private final ConsultaBloqueos bloqueos;

    /**
     * Crea el sugeridor.
     *
     * @param directorio  usuarios registrados
     * @param amistades   consulta del grafo
     * @param solicitudes consulta de solicitudes pendientes
     * @param bloqueos    consulta de bloqueos
     */
    public SugeridorAmistades(DirectorioUsuarios directorio, ConsultaAmistades amistades,
                              ConsultaSolicitudes solicitudes, ConsultaBloqueos bloqueos) {
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.solicitudes = Objects.requireNonNull(solicitudes, "solicitudes");
        this.bloqueos = Objects.requireNonNull(bloqueos, "bloqueos");
    }

    /** {@inheritDoc} */
    @Override
    public List<Sugerencia> sugerir(Usuario usuario) {
        String yo = Objects.requireNonNull(usuario, "usuario").getId();
        Map<String, Integer> enComun = recorrerEnAnchura(yo);
        List<Sugerencia> sugerencias = new ArrayList<>();
        for (Map.Entry<String, Integer> candidato : enComun.entrySet()) {
            String id = candidato.getKey();
            if (esSugerible(yo, id)) {
                directorio.buscarPorId(id).ifPresent(u -> sugerencias.add(new Sugerencia(u, candidato.getValue())));
            }
        }
        // Orden: más amigos en común primero; si empatan, alfabético por nombre.
        Collator alfabetico = Collator.getInstance(ESPANOL);
        sugerencias.sort(Comparator.comparingInt(Sugerencia::amigosEnComun).reversed()
                .thenComparing(s -> s.usuario().getNombre(), alfabetico));
        return Collections.unmodifiableList(sugerencias.subList(0, Math.min(MAXIMO_SUGERENCIAS, sugerencias.size())));
    }

    /**
     * BFS desde {@code origen} hasta el nivel 2.
     *
     * @param origen usuario desde el que se recorre
     * @return candidatos del nivel 2 → cantidad de amigos en común con el origen
     */
    private Map<String, Integer> recorrerEnAnchura(String origen) {
        Map<String, Integer> distancia = new HashMap<>();
        Map<String, Integer> enComun = new HashMap<>();
        Queue<String> cola = new ArrayDeque<>();
        distancia.put(origen, 0);
        cola.add(origen);
        while (!cola.isEmpty()) {
            String actual = cola.poll();
            int nivel = distancia.get(actual);
            if (nivel == PROFUNDIDAD_MAXIMA) {
                continue; // Del nivel 2 no se sigue explorando: los amigos de nivel 3 no se sugieren.
            }
            for (String vecino : amistades.idsAmigos(actual)) {
                if (!distancia.containsKey(vecino)) {
                    distancia.put(vecino, nivel + 1);
                    cola.add(vecino);
                }
                // Llegar a un usuario de nivel 2 desde uno de mis amigos (nivel 1) = 1 amigo en común.
                if (nivel == 1 && distancia.get(vecino) == PROFUNDIDAD_MAXIMA) {
                    enComun.merge(vecino, 1, Integer::sum);
                }
            }
        }
        return enComun;
    }

    /**
     * @param yo  usuario activo
     * @param otro candidato
     * @return {@code true} si se le puede sugerir (sin bloqueo ni solicitud pendiente)
     */
    private boolean esSugerible(String yo, String otro) {
        return !bloqueos.hayBloqueoEntre(yo, otro)
                && !solicitudes.haySolicitudPendiente(yo, otro)
                && !solicitudes.haySolicitudPendiente(otro, yo);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es la INTERSECCIÓN de los dos conjuntos de amigos (las dos listas de adyacencia).</p>
     */
    @Override
    public int amigosEnComun(String idA, String idB) {
        Set<String> comunes = new HashSet<>(amistades.idsAmigos(idA));
        comunes.retainAll(amistades.idsAmigos(idB));
        return comunes.size();
    }
}
