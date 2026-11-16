/*
 * ModulosSociales.java
 *
 * Qué hace: agrupa todos los servicios del módulo social ya creados y conectados (bloqueos,
 * amistades, sugerencias, buscador, publicaciones, me gusta, comentarios, perfiles y anuncios).
 * Rol en la arquitectura: lo crea EnsambladorSocial y lo usa Main para cargar los datos en el orden
 * correcto, guardarlos y construir la ventana principal.
 * Principio SOLID: SRP (solo agrupa; no tiene lógica) y DIP (Main recibe todo ya armado).
 * Colabora con: EnsambladorSocial, Main.
 */
package una.social;

import java.util.List;
import una.social.amistades.BuscadorUsuarios;
import una.social.amistades.GestorAmistades;
import una.social.amistades.SugeridorAmistades;
import una.social.anuncios.GestorAnuncios;
import una.social.bloqueos.GestorBloqueos;
import una.social.interacciones.GestorComentarios;
import una.social.interacciones.GestorMeGusta;
import una.social.perfiles.GestorPerfiles;
import una.social.persistencia.ModuloPersistente;
import una.social.publicaciones.GestorPublicaciones;

/**
 * Servicios del módulo social.
 *
 * @param bloqueos      bloqueos entre usuarios
 * @param amistades     amistades y solicitudes
 * @param sugeridor     "Personas que quizás conozcas"
 * @param buscador      buscador de usuarios
 * @param publicaciones publicaciones, compartidos y muros
 * @param meGusta       "me gusta"
 * @param comentarios   comentarios
 * @param perfiles      biografías
 * @param anuncios      anuncios públicos
 */
public record ModulosSociales(GestorBloqueos bloqueos, GestorAmistades amistades, SugeridorAmistades sugeridor,
                              BuscadorUsuarios buscador, GestorPublicaciones publicaciones, GestorMeGusta meGusta,
                              GestorComentarios comentarios, GestorPerfiles perfiles, GestorAnuncios anuncios) {

    /**
     * Módulos que solo dependen de los usuarios, en el ORDEN en que deben cargarse: bloqueos antes
     * que amistades (una amistad entre usuarios bloqueados no es válida) y amistades antes que
     * publicaciones.
     *
     * @return lista de módulos de la primera fase social
     */
    public List<ModuloPersistente> fasePrincipal() {
        return List.of(bloqueos, amistades, publicaciones, perfiles, anuncios);
    }

    /**
     * Módulos que dependen de las publicaciones (deben cargarse DESPUÉS, porque validan que la
     * publicación exista).
     *
     * @return lista de módulos de interacciones
     */
    public List<ModuloPersistente> faseInteracciones() {
        return List.of(meGusta, comentarios);
    }

    /**
     * @return todos los módulos sociales, en orden de carga
     */
    public List<ModuloPersistente> todos() {
        return List.of(bloqueos, amistades, publicaciones, perfiles, anuncios, meGusta, comentarios);
    }
}
