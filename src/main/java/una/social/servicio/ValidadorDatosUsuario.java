/*
 * ValidadorDatosUsuario.java
 *
 * Qué hace: concentra TODAS las reglas de validación de los formularios de login y registro.
 * Rol en la arquitectura: el GestorUsuarios lo usa antes de registrar o autenticar. La UI no
 * repite estas reglas: solo muestra junto al campo el mensaje de la ExcepcionValidacion.
 * Principio SOLID: SRP (las reglas de validación están separadas de la lógica de registro y
 * login) y OCP (agregar una regla no cambia el flujo del gestor).
 * Colabora con: GestorUsuarios, ExcepcionValidacion, CampoFormulario.
 */
package una.social.servicio;

import java.util.Arrays;
import java.util.regex.Pattern;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;

/**
 * Reglas de validación de datos de usuario.
 *
 * <p>Aquí se usan EXCEPCIONES y no {@code assert}: lo que escribe una persona es entrada
 * externa, que puede venir mal, y además los asserts se pueden desactivar al ejecutar.</p>
 *
 * <p>Las reglas se revisan de arriba hacia abajo en el orden del formulario y se informa el
 * primer problema encontrado.</p>
 */
final class ValidadorDatosUsuario {

    /** Largo mínimo de la contraseña (pedido por el enunciado de calidad). */
    static final int LARGO_MINIMO_CONTRASENA = 6;

    /** Largo máximo de la contraseña: limita el costo de PBKDF2 ante textos gigantes. */
    static final int LARGO_MAXIMO_CONTRASENA = 128;

    /** Largo mínimo del nombre de usuario. */
    static final int LARGO_MINIMO_USUARIO = 3;

    /** Largo máximo del nombre de usuario. */
    static final int LARGO_MAXIMO_USUARIO = 20;

    /** Largo máximo del nombre completo. */
    static final int LARGO_MAXIMO_NOMBRE = 60;

    /**
     * Caracteres permitidos en el nombre de usuario: letras sin tilde, números, punto, guion y
     * guion bajo. Sin espacios, para que sea fácil de escribir y de buscar.
     */
    private static final Pattern PATRON_USUARIO = Pattern.compile("[A-Za-z0-9._-]+");

    /** Detecta caracteres de control (saltos de línea, tabuladores, etc.) en el nombre. */
    private static final Pattern PATRON_CONTROL = Pattern.compile("\\p{Cntrl}");

    /** Clase de utilidades: no se instancia. */
    private ValidadorDatosUsuario() {
    }

    /**
     * Valida todos los campos del registro.
     *
     * @param nombre        nombre completo
     * @param nombreUsuario nombre de usuario
     * @param contrasena    contraseña
     * @param confirmacion  confirmación de la contraseña
     * @throws ExcepcionValidacion con el campo y mensaje del primer problema encontrado
     */
    static void validarRegistro(String nombre, String nombreUsuario, char[] contrasena, char[] confirmacion) {
        // Se revisan los campos en el mismo orden en que aparecen en la ventana; el primero que
        // falle lanza la excepción y la ventana marca ese campo en rojo.
        validarNombre(nombre);
        validarNombreUsuario(nombreUsuario);
        validarContrasenaNueva(contrasena);
        validarConfirmacion(contrasena, confirmacion);
    }

    /**
     * Valida que los campos del login no estén vacíos. En el login NO se revisan largos ni
     * formatos: hacerlo revelaría las reglas y no aporta seguridad.
     *
     * @param nombreUsuario nombre escrito
     * @param contrasena    contraseña escrita
     * @throws ExcepcionValidacion si algún campo está vacío
     */
    static void validarLogin(String nombreUsuario, char[] contrasena) {
        // Campo "Usuario" vacío o solo con espacios.
        if (estaVacio(nombreUsuario)) {
            throw new ExcepcionValidacion(CampoFormulario.USUARIO, "Escriba su nombre de usuario.");
        }
        // Campo "Contraseña" vacío.
        if (contrasena == null || contrasena.length == 0) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA, "Escriba su contraseña.");
        }
    }

    /**
     * Valida un nombre completo nuevo (al editarlo desde el perfil). Usa exactamente la misma
     * regla que el registro, para no duplicar validaciones.
     *
     * @param nombre nombre nuevo
     * @throws ExcepcionValidacion si está vacío, es muy largo o tiene caracteres de control
     */
    static void validarCambioNombre(String nombre) {
        validarNombre(nombre);
    }

    /**
     * Valida un cambio de contraseña: la actual no puede estar vacía y la nueva sigue las mismas
     * reglas que en el registro (largo, no solo espacios, confirmación igual).
     *
     * @param actual       contraseña actual escrita
     * @param nueva        contraseña nueva
     * @param confirmacion confirmación de la nueva
     * @throws ExcepcionValidacion con el campo y mensaje del primer problema encontrado
     */
    static void validarCambioContrasena(char[] actual, char[] nueva, char[] confirmacion) {
        // La contraseña actual es obligatoria (que sea CORRECTA lo revisa GestorUsuarios).
        if (actual == null || actual.length == 0) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA_ACTUAL, "Escriba su contraseña actual.");
        }
        // La nueva y su confirmación usan las mismas reglas que el registro.
        validarContrasenaNueva(nueva);
        validarConfirmacion(nueva, confirmacion);
    }

    /**
     * Valida el nombre completo.
     *
     * @param nombre nombre escrito
     * @throws ExcepcionValidacion si está vacío, es muy largo o tiene caracteres de control
     */
    private static void validarNombre(String nombre) {
        // Regla 1: no puede estar vacío.
        if (estaVacio(nombre)) {
            throw new ExcepcionValidacion(CampoFormulario.NOMBRE, "Escriba su nombre completo.");
        }
        // Regla 2: largo máximo (sin contar espacios a los lados).
        if (nombre.trim().length() > LARGO_MAXIMO_NOMBRE) {
            throw new ExcepcionValidacion(CampoFormulario.NOMBRE,
                    "El nombre puede tener como máximo " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        // Regla 3: sin caracteres de control (tabuladores, saltos de línea...) que dañarían el archivo.
        if (PATRON_CONTROL.matcher(nombre).find()) {
            throw new ExcepcionValidacion(CampoFormulario.NOMBRE, "El nombre contiene caracteres no válidos.");
        }
    }

    /**
     * Valida el nombre de usuario nuevo.
     *
     * @param nombreUsuario nombre de usuario escrito
     * @throws ExcepcionValidacion si está vacío, tiene un largo fuera de rango o caracteres no permitidos
     */
    private static void validarNombreUsuario(String nombreUsuario) {
        // Regla 1: no puede estar vacío.
        if (estaVacio(nombreUsuario)) {
            throw new ExcepcionValidacion(CampoFormulario.USUARIO, "Elija un nombre de usuario.");
        }
        // Regla 2: largo entre el mínimo y el máximo permitidos.
        String limpio = nombreUsuario.trim();
        if (limpio.length() < LARGO_MINIMO_USUARIO || limpio.length() > LARGO_MAXIMO_USUARIO) {
            throw new ExcepcionValidacion(CampoFormulario.USUARIO, "El usuario debe tener entre "
                    + LARGO_MINIMO_USUARIO + " y " + LARGO_MAXIMO_USUARIO + " caracteres.");
        }
        // Regla 3: solo caracteres permitidos (letras sin tilde, números, punto, guion, guion bajo).
        if (!PATRON_USUARIO.matcher(limpio).matches()) {
            throw new ExcepcionValidacion(CampoFormulario.USUARIO,
                    "Use solo letras sin tilde, números, punto, guion o guion bajo (sin espacios).");
        }
    }

    /**
     * Valida la contraseña nueva.
     *
     * @param contrasena contraseña escrita
     * @throws ExcepcionValidacion si está vacía, es solo espacios o tiene un largo fuera de rango
     */
    private static void validarContrasenaNueva(char[] contrasena) {
        // Regla 1: no puede estar vacía.
        if (contrasena == null || contrasena.length == 0) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA, "Escriba una contraseña.");
        }
        // Regla 2: no puede ser solo espacios.
        if (soloEspacios(contrasena)) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA, "La contraseña no puede ser solo espacios.");
        }
        // Regla 3: largo mínimo.
        if (contrasena.length < LARGO_MINIMO_CONTRASENA) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA,
                    "La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.");
        }
        // Regla 4: largo máximo.
        if (contrasena.length > LARGO_MAXIMO_CONTRASENA) {
            throw new ExcepcionValidacion(CampoFormulario.CONTRASENA,
                    "La contraseña puede tener como máximo " + LARGO_MAXIMO_CONTRASENA + " caracteres.");
        }
    }

    /**
     * Valida que la confirmación coincida con la contraseña.
     *
     * @param contrasena   contraseña
     * @param confirmacion confirmación
     * @throws ExcepcionValidacion si la confirmación está vacía o no coincide
     */
    private static void validarConfirmacion(char[] contrasena, char[] confirmacion) {
        if (confirmacion == null || confirmacion.length == 0) {
            throw new ExcepcionValidacion(CampoFormulario.CONFIRMACION, "Repita la contraseña.");
        }
        // Arrays.equals compara el contenido sin convertir a String (así no quedan copias de la
        // contraseña en memoria que no se puedan borrar).
        if (!Arrays.equals(contrasena, confirmacion)) {
            throw new ExcepcionValidacion(CampoFormulario.CONFIRMACION, "Las contraseñas no coinciden.");
        }
    }

    /**
     * @param texto texto a revisar
     * @return {@code true} si es nulo o solo tiene espacios
     */
    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * @param caracteres arreglo a revisar (no vacío)
     * @return {@code true} si todos los caracteres son espacios en blanco
     */
    private static boolean soloEspacios(char[] caracteres) {
        // Basta un carácter que no sea espacio para que la contraseña sea aceptable.
        for (char c : caracteres) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        // Se recorrió todo y solo había espacios.
        return true;
    }
}
