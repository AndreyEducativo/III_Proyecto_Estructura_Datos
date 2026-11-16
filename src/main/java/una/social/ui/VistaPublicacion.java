/*
 * VistaPublicacion.java
 *
 * Qué hace: todo lo necesario para dibujar una entrada del muro: la entrada (publicación o
 * compartido), el estado de "me gusta", la cantidad de comentarios visibles y si el usuario activo
 * puede editarla (solo su última publicación).
 * Rol en la arquitectura: se calcula en SEGUNDO PLANO (consulta varios servicios) y luego la usa
 * TarjetasPublicacion en el hilo de Swing para armar las tarjetas sin volver a consultar.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: TarjetasPublicacion, EntradaMuro, EstadoMeGusta.
 */
package una.social.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import una.social.interacciones.EstadoMeGusta;
import una.social.modelo.Usuario;
import una.social.publicaciones.EntradaMuro;
import una.social.publicaciones.Publicacion;

/**
 * Datos para dibujar una entrada del muro.
 *
 * @param entrada      publicación o compartido
 * @param meGusta      estado de "me gusta" de la publicación (null si la original no está disponible)
 * @param comentarios  cantidad de comentarios visibles
 * @param esEditable   {@code true} si es la última publicación del usuario activo
 */
record VistaPublicacion(EntradaMuro entrada, EstadoMeGusta meGusta, int comentarios, boolean esEditable) {

    /**
     * Prepara las vistas de una lista de entradas. Consulta servicios: llamar FUERA del hilo de Swing.
     *
     * @param servicios servicios de la ventana principal
     * @param yo        usuario activo
     * @param entradas  entradas del muro o del perfil
     * @return vistas en el mismo orden (lista no modificable)
     */
    static List<VistaPublicacion> preparar(ServiciosSociales servicios, Usuario yo, List<EntradaMuro> entradas) {
        Optional<String> idUltimaMia = servicios.publicaciones().ultimaPublicacionDe(yo.getId()).map(Publicacion::getId);
        List<VistaPublicacion> vistas = new ArrayList<>(entradas.size());
        for (EntradaMuro entrada : entradas) {
            if (!entrada.originalDisponible()) {
                vistas.add(new VistaPublicacion(entrada, null, 0, false));
                continue;
            }
            String id = entrada.publicacion().getId();
            boolean editable = !entrada.esCompartido() && idUltimaMia.map(id::equals).orElse(false);
            vistas.add(new VistaPublicacion(entrada, servicios.meGusta().estado(yo.getId(), id),
                    servicios.comentarios().contar(yo, id), editable));
        }
        return Collections.unmodifiableList(vistas);
    }
}
