/*
 * CampoFormulario.java
 *
 * Qué hace: enumera los campos de los formularios de autenticación (login y registro).
 * Rol en la arquitectura: permite que el servicio diga "falló el campo X" mediante
 * ExcepcionValidacion, y que la ventana pinte el error en rojo junto a ese campo.
 * Principio SOLID: SRP (solo identifica campos) y OCP (agregar un campo nuevo no obliga a
 * cambiar la lógica de validación existente).
 * Colabora con: ExcepcionValidacion, GestorUsuarios, VentanaLogin, VentanaRegistro.
 */
package una.social.modelo;

/**
 * Campos de los formularios de login y registro.
 */
public enum CampoFormulario {
    /** Nombre completo de la persona (solo en el registro). */
    NOMBRE,
    /** Nombre de usuario con el que se inicia sesión. */
    USUARIO,
    /** Contraseña. */
    CONTRASENA,
    /** Confirmación de la contraseña (en el registro y al cambiar la contraseña). */
    CONFIRMACION,
    /** Contraseña actual (solo al cambiar la contraseña desde el perfil). */
    CONTRASENA_ACTUAL
}
