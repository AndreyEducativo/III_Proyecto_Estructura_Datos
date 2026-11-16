/*
 * ServicioRegistro.java
 *
 * Qué hace: contrato para registrar usuarios nuevos.
 * Rol en la arquitectura: la VentanaRegistro y la HerramientaDatosSemilla dependen solo de esta
 * interfaz.
 * Principio SOLID: ISP y DIP.
 * Colabora con: GestorUsuarios (implementación), VentanaRegistro, HerramientaDatosSemilla.
 */
package una.social.servicio;

import una.social.modelo.Usuario;

/**
 * Registro de usuarios nuevos.
 */
public interface ServicioRegistro {

    /**
     * Valida los datos, asigna un ID nuevo (U001, U002, ...), protege la contraseña con PBKDF2,
     * guarda en disco y agrega el usuario al catálogo en memoria.
     *
     * <p>Las contraseñas no se modifican; quien llama debe borrarlas al terminar.</p>
     *
     * @param nombre        nombre completo
     * @param nombreUsuario nombre de usuario deseado
     * @param contrasena    contraseña
     * @param confirmacion  repetición de la contraseña
     * @return el usuario creado (con su ID asignado)
     * @throws una.social.excepciones.ExcepcionValidacion       si un dato no cumple las reglas
     * @throws una.social.excepciones.ExcepcionUsuarioDuplicado si el nombre de usuario ya existe
     * @throws una.social.excepciones.ExcepcionPersistencia     si no se pudo guardar en disco
     */
    Usuario registrar(String nombre, String nombreUsuario, char[] contrasena, char[] confirmacion);
}
