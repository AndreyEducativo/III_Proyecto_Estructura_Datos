/*
 * ServiciosSociales.java
 *
 * Qué hace: agrupa en un solo objeto las INTERFACES de todos los servicios que necesita la ventana
 * principal (publicaciones, amistades, búsqueda, sugerencias, bloqueos, me gusta, comentarios,
 * perfiles, anuncios, cuenta, sesión y auditoría).
 * Rol en la arquitectura: Main lo arma una vez y la FabricaVentanaPrincipal se lo pasa a la
 * VentanaPrincipal, que a su vez lo reparte entre sus pestañas. Evita constructores con doce
 * parámetros y mantiene a la UI dependiendo SOLO de interfaces.
 * Principio SOLID: DIP (todo son abstracciones) e ISP (cada pestaña usa solo las que necesita).
 * Colabora con: Main, VentanaPrincipal y todas las pestañas.
 */
package una.social.ui;

import java.util.Objects;
import una.social.amistades.ServicioAmistades;
import una.social.amistades.ServicioBusqueda;
import una.social.amistades.ServicioSugerencias;
import una.social.anuncios.ServicioAnuncios;
import una.social.auditoria.RegistroAuditoria;
import una.social.bloqueos.ServicioBloqueos;
import una.social.interacciones.ServicioComentarios;
import una.social.interacciones.ServicioMeGusta;
import una.social.perfiles.ServicioPerfiles;
import una.social.publicaciones.ServicioPublicaciones;
import una.social.servicio.ServicioCuenta;
import una.social.sesion.ContextoSesion;

/**
 * Servicios que usa la interfaz de la ventana principal.
 *
 * @param publicaciones publicaciones, compartidos y muros
 * @param amistades     amistades y solicitudes
 * @param busqueda      buscador de usuarios
 * @param sugerencias   "Personas que quizás conozcas" y amigos en común
 * @param bloqueos      bloqueos
 * @param meGusta       "me gusta"
 * @param comentarios   comentarios
 * @param perfiles      biografías
 * @param anuncios      anuncios públicos
 * @param cuenta        cambio de nombre y contraseña
 * @param sesion        sesión activa (para actualizar el usuario al cambiar su nombre)
 * @param auditoria     bitácora (para errores inesperados)
 */
public record ServiciosSociales(ServicioPublicaciones publicaciones, ServicioAmistades amistades, ServicioBusqueda busqueda,
                                ServicioSugerencias sugerencias, ServicioBloqueos bloqueos, ServicioMeGusta meGusta,
                                ServicioComentarios comentarios, ServicioPerfiles perfiles, ServicioAnuncios anuncios,
                                ServicioCuenta cuenta, ContextoSesion sesion, RegistroAuditoria auditoria) {

    /**
     * Comprueba que no falte ningún servicio (error de programación en Main).
     *
     * @param publicaciones publicaciones
     * @param amistades     amistades
     * @param busqueda      búsqueda
     * @param sugerencias   sugerencias
     * @param bloqueos      bloqueos
     * @param meGusta       me gusta
     * @param comentarios   comentarios
     * @param perfiles      perfiles
     * @param anuncios      anuncios
     * @param cuenta        cuenta
     * @param sesion        sesión
     * @param auditoria     auditoría
     */
    public ServiciosSociales {
        Objects.requireNonNull(publicaciones, "publicaciones");
        Objects.requireNonNull(amistades, "amistades");
        Objects.requireNonNull(busqueda, "busqueda");
        Objects.requireNonNull(sugerencias, "sugerencias");
        Objects.requireNonNull(bloqueos, "bloqueos");
        Objects.requireNonNull(meGusta, "meGusta");
        Objects.requireNonNull(comentarios, "comentarios");
        Objects.requireNonNull(perfiles, "perfiles");
        Objects.requireNonNull(anuncios, "anuncios");
        Objects.requireNonNull(cuenta, "cuenta");
        Objects.requireNonNull(sesion, "sesion");
        Objects.requireNonNull(auditoria, "auditoria");
    }
}
