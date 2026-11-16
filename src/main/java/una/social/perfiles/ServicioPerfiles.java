/*
 * ServicioPerfiles.java
 *
 * Qué hace: contrato de la biografía de los perfiles: consultarla y cambiarla.
 * Rol en la arquitectura: la pestaña "Mi perfil" y la ventana de perfil dependen de esta interfaz.
 * Principio SOLID: ISP (dos operaciones) y DIP.
 * Colabora con: GestorPerfiles (implementación), PanelMiPerfil, VentanaPerfil.
 */
package una.social.perfiles;

import una.social.modelo.Usuario;

/**
 * Operaciones de perfil.
 */
public interface ServicioPerfiles {

    /**
     * @param idUsuario usuario
     * @return su biografía, o cadena vacía si no tiene
     */
    String obtenerBiografia(String idUsuario);

    /**
     * Cambia la biografía del usuario activo (vacía = sin biografía). Se guarda inmediatamente.
     *
     * @param usuario   usuario activo
     * @param biografia texto nuevo (máximo {@link Perfil#LARGO_MAXIMO_BIOGRAFIA} caracteres)
     * @return la biografía guardada (ya limpia)
     * @throws una.social.excepciones.ExcepcionReglaNegocio si es demasiado larga
     * @throws una.social.excepciones.ExcepcionPersistencia si no se pudo guardar
     */
    String actualizarBiografia(Usuario usuario, String biografia);
}
