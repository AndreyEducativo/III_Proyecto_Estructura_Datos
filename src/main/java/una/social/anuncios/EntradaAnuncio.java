/*
 * EntradaAnuncio.java
 *
 * Qué hace: un anuncio con los datos de su autor, listo para mostrarse.
 * Rol en la arquitectura: lo devuelve ServicioAnuncios.listar; la pestaña "Anuncios" lo muestra.
 * Principio SOLID: SRP (record inmutable de solo datos).
 * Colabora con: GestorAnuncios, PanelAnuncios.
 */
package una.social.anuncios;

import una.social.modelo.Usuario;

/**
 * Anuncio con su autor.
 *
 * @param anuncio anuncio
 * @param autor   autor
 */
public record EntradaAnuncio(Anuncio anuncio, Usuario autor) {
}
