/*
 * BandejaSolicitudes.java
 *
 * Qué hace: ESTRUCTURA DE DATOS con las solicitudes de amistad pendientes, organizadas como una
 * COLA (FIFO) por cada usuario que las recibe.
 * Rol en la arquitectura: memoria de las solicitudes del GestorAmistades. Se llena al iniciar con
 * data/solicitudes.txt y se consulta para mostrar la pestaña "Solicitudes".
 * Principio SOLID: SRP (solo organiza solicitudes en memoria; no valida usuarios ni guarda en disco).
 * Colabora con: SolicitudAmistad, GestorAmistades.
 */
package una.social.amistades;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bandeja de solicitudes de amistad pendientes.
 *
 * <p><b>¿Por qué una COLA? (explicado simple)</b> Una cola funciona como la fila de un banco: el
 * primero que llega es el primero en ser atendido (FIFO, "First In, First Out"). Cada usuario tiene
 * su propia cola de solicitudes recibidas: las nuevas se agregan AL FINAL ({@code offerLast}) y se
 * le muestran empezando por el PRINCIPIO, es decir, en el orden en que llegaron.</p>
 *
 * <p><b>Estructura:</b> {@code Map<String, Deque<SolicitudAmistad>>}: ID del destinatario → su
 * cola. Se usa {@link ArrayDeque}, la implementación de cola más eficiente del JDK (agregar al final
 * y sacar del principio cuestan O(1)). Aceptar, rechazar o cancelar una solicitud concreta la saca
 * de su cola aunque no esté al frente (el usuario elige cuál responder).</p>
 *
 * <p>No es segura entre hilos por sí sola: la sincroniza el GestorAmistades.</p>
 */
public final class BandejaSolicitudes {

    /** Destinatario → cola de solicitudes recibidas (en orden de llegada). */
    private final Map<String, Deque<SolicitudAmistad>> colasPorDestinatario = new LinkedHashMap<>();

    /** Cantidad total de solicitudes pendientes. */
    private int cantidad;

    /**
     * Agrega una solicitud AL FINAL de la cola de su destinatario.
     *
     * <p>Precondición: no existe ya una solicitud con el mismo remitente y destinatario (lo valida
     * el GestorAmistades antes de llamar).</p>
     *
     * @param solicitud solicitud nueva
     */
    public void encolar(SolicitudAmistad solicitud) {
        // Invariante interna: el gestor nunca encola duplicados.
        assert buscar(solicitud.idRemitente(), solicitud.idDestinatario()).isEmpty() : "Solicitud duplicada";
        colasPorDestinatario.computeIfAbsent(solicitud.idDestinatario(), id -> new ArrayDeque<>()).offerLast(solicitud);
        cantidad++;
    }

    /**
     * Busca la solicitud pendiente de un remitente a un destinatario.
     *
     * @param idRemitente    quien la envió
     * @param idDestinatario quien la recibió
     * @return la solicitud si está pendiente
     */
    public Optional<SolicitudAmistad> buscar(String idRemitente, String idDestinatario) {
        // Busca la cola del destinatario en el HashMap; si no tiene cola, no hay solicitud.
        Deque<SolicitudAmistad> cola = colasPorDestinatario.get(idDestinatario);
        if (cola == null) {
            return Optional.empty();
        }
        // Recorre su cola buscando la que envió ese remitente.
        return cola.stream().filter(s -> s.idRemitente().equals(idRemitente)).findFirst();
    }

    /**
     * Saca de la cola la solicitud de un remitente a un destinatario (al aceptarla, rechazarla o
     * cancelarla).
     *
     * @param idRemitente    quien la envió
     * @param idDestinatario quien la recibió
     * @return {@code true} si existía y se quitó
     */
    public boolean quitar(String idRemitente, String idDestinatario) {
        // Toma la cola del destinatario y quita la solicitud de ese remitente (esté donde esté).
        Deque<SolicitudAmistad> cola = colasPorDestinatario.get(idDestinatario);
        if (cola == null || !cola.removeIf(s -> s.idRemitente().equals(idRemitente))) {
            return false; // No existía.
        }
        // Si la cola quedó vacía se borra del mapa para no guardar colas vacías.
        if (cola.isEmpty()) {
            colasPorDestinatario.remove(idDestinatario);
        }
        // Se actualiza el contador total de solicitudes pendientes.
        cantidad--;
        return true;
    }

    /**
     * Quita las solicitudes entre dos usuarios en los DOS sentidos (al bloquear).
     *
     * @param idA un usuario
     * @param idB otro usuario
     * @return cuántas solicitudes se quitaron (0, 1 o 2)
     */
    public int quitarEntre(String idA, String idB) {
        int quitadas = 0;
        // Sentido A -> B.
        if (quitar(idA, idB)) {
            quitadas++;
        }
        // Sentido B -> A.
        if (quitar(idB, idA)) {
            quitadas++;
        }
        return quitadas;
    }

    /**
     * @param idDestinatario usuario consultado
     * @return sus solicitudes recibidas, desde el FRENTE de la cola (la más antigua primero)
     */
    public List<SolicitudAmistad> recibidasPor(String idDestinatario) {
        // Se devuelve una copia en el orden de la cola (frente primero); sin cola, lista vacía.
        Deque<SolicitudAmistad> cola = colasPorDestinatario.get(idDestinatario);
        return cola == null ? List.of() : List.copyOf(cola);
    }

    /**
     * @param idRemitente usuario consultado
     * @return solicitudes que envió y siguen pendientes, de la más antigua a la más reciente
     */
    public List<SolicitudAmistad> enviadasPor(String idRemitente) {
        List<SolicitudAmistad> enviadas = new ArrayList<>();
        // Las colas están organizadas por DESTINATARIO, así que hay que revisar todas las colas
        // para encontrar las que envió este remitente.
        for (Deque<SolicitudAmistad> cola : colasPorDestinatario.values()) {
            for (SolicitudAmistad solicitud : cola) {
                if (solicitud.idRemitente().equals(idRemitente)) {
                    enviadas.add(solicitud);
                }
            }
        }
        // Se ordenan por fecha (de la más antigua a la más reciente).
        enviadas.sort(Comparator.comparing(SolicitudAmistad::fecha));
        return Collections.unmodifiableList(enviadas);
    }

    /**
     * @return todas las solicitudes, ordenadas por fecha de envío (así se guardan en el archivo y,
     *         al volver a cargarlas en ese orden, cada cola queda igual que antes)
     */
    public List<SolicitudAmistad> todas() {
        List<SolicitudAmistad> todas = new ArrayList<>(cantidad);
        colasPorDestinatario.values().forEach(todas::addAll);
        todas.sort(Comparator.comparing(SolicitudAmistad::fecha));
        return Collections.unmodifiableList(todas);
    }

    /**
     * @return cantidad total de solicitudes pendientes
     */
    public int tamano() {
        return cantidad;
    }

    /**
     * Vacía la bandeja (antes de recargar desde el archivo).
     */
    public void limpiar() {
        colasPorDestinatario.clear();
        cantidad = 0;
    }
}
