/*
 * ServicioAnuncios.java
 *
 * Qué hace: contrato de la sección pública de anuncios: publicar, borrar y listar.
 * Rol en la arquitectura: la pestaña "Anuncios" depende solo de esta interfaz.
 * Principio SOLID: ISP y DIP.
 * Colabora con: GestorAnuncios (implementación), PanelAnuncios.
 */
package una.social.anuncios;

import java.util.List;
import una.social.modelo.Usuario;

/**
 * Operaciones de anuncios.
 */
public interface ServicioAnuncios {

    /**
     * Publica un anuncio visible para todos. Se guarda inmediatamente en disco.
     *
     * @param autor  usuario activo
     * @param titulo título (obligatorio, máximo {@link Anuncio#LARGO_MAXIMO_TITULO} caracteres)
     * @param texto  texto (obligatorio, máximo {@link Anuncio#LARGO_MAXIMO_TEXTO} caracteres)
     * @return el anuncio creado
     * @throws una.social.excepciones.ExcepcionReglaNegocio si el título o el texto no son válidos
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    Anuncio publicar(Usuario autor, String titulo, String texto);

    /**
     * Borra un anuncio. Solo puede hacerlo su autor.
     *
     * @param autor     usuario activo
     * @param idAnuncio anuncio a borrar
     */
    void borrar(Usuario autor, String idAnuncio);

    /**
     * @param visitante usuario activo
     * @return anuncios del más reciente al más antiguo, sin los de usuarios bloqueados
     */
    List<EntradaAnuncio> listar(Usuario visitante);
}
