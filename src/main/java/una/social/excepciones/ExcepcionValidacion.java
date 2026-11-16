/*
 * ExcepcionValidacion.java
 *
 * Qué hace: indica que un dato escrito por el usuario no cumple una regla (campo vacío,
 * contraseña corta, confirmación distinta, etc.).
 * Rol en la arquitectura: la lanza la capa de servicio (única fuente de las reglas) y la UI la
 * usa para pintar el mensaje en rojo JUNTO al campo que falló.
 * Principio SOLID: SRP (las reglas viven en el servicio; la UI solo muestra) y LSP.
 * Colabora con: GestorUsuarios, CampoFormulario, VentanaLogin, VentanaRegistro.
 */
package una.social.excepciones;

import java.util.Objects;
import una.social.modelo.CampoFormulario;

/**
 * Error de validación de un campo de formulario.
 *
 * <p>Lleva el {@link CampoFormulario} afectado para que la ventana sepa en qué campo mostrar
 * el error, sin tener que repetir las reglas de validación en la UI (evita código duplicado).</p>
 */
public class ExcepcionValidacion extends ExcepcionSocialUna {

    /** Versión de serialización. */
    private static final long serialVersionUID = 1L;

    /** Campo del formulario que no pasó la validación. */
    private final CampoFormulario campo;

    /**
     * Crea la excepción.
     *
     * @param campo   campo que falló (no nulo)
     * @param mensaje mensaje corto y amigable que se mostrará junto al campo
     */
    public ExcepcionValidacion(CampoFormulario campo, String mensaje) {
        super(mensaje);
        this.campo = Objects.requireNonNull(campo, "campo");
    }

    /**
     * Devuelve el campo que no pasó la validación.
     *
     * @return campo afectado
     */
    public CampoFormulario getCampo() {
        return campo;
    }
}
