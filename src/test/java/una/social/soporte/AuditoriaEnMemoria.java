/*
 * AuditoriaEnMemoria.java
 *
 * Qué hace: implementación de RegistroAuditoria para PRUEBAS que guarda los eventos en una
 * lista en vez de un archivo, para poder revisarlos con asserts.
 * Rol en la arquitectura: doble de prueba ("test double").
 * Principio SOLID: LSP y DIP en acción. Las clases reales reciben esta versión en lugar de la de
 * archivo y funcionan igual, porque dependen solo de la interfaz.
 * Colabora con: las pruebas de repositorio y de servicio.
 */
package una.social.soporte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;

/**
 * Bitácora en memoria para pruebas.
 */
public final class AuditoriaEnMemoria implements RegistroAuditoria {

    /**
     * Evento registrado.
     *
     * @param nivel   nivel
     * @param actor   actor
     * @param accion  acción
     * @param detalle detalle
     */
    public record Evento(NivelAuditoria nivel, String actor, String accion, String detalle) {
    }

    /** Eventos en orden de llegada. */
    private final List<Evento> eventos = new ArrayList<>();

    /** {@inheritDoc} */
    @Override
    public synchronized void registrar(NivelAuditoria nivel, String actor, String accion, String detalle) {
        eventos.add(new Evento(nivel, actor, accion, detalle));
    }

    /**
     * @return copia no modificable de los eventos
     */
    public synchronized List<Evento> eventos() {
        return Collections.unmodifiableList(new ArrayList<>(eventos));
    }

    /**
     * @param accion acción buscada
     * @return cuántos eventos tienen esa acción
     */
    public synchronized long contar(String accion) {
        return eventos.stream().filter(e -> e.accion().equals(accion)).count();
    }

    /**
     * @return todo el contenido de la bitácora en un solo texto (para buscar fugas de datos)
     */
    public synchronized String comoTexto() {
        StringBuilder texto = new StringBuilder();
        eventos.forEach(e -> texto.append(e).append('\n'));
        return texto.toString();
    }
}
