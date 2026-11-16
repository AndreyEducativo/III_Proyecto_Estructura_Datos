/*
 * AccionesUi.java
 *
 * Qué hace: ejecuta en segundo plano una acción del usuario que cambia datos (publicar, comentar,
 * bloquear, enviar solicitud...) y, si falla, muestra el aviso adecuado: AVISO si no se cumplió una
 * regla (el usuario puede corregirlo), ERROR si falló el disco y "error inesperado" (auditado) en
 * cualquier otro caso.
 * Rol en la arquitectura: utilidad compartida por todas las pestañas y diálogos del módulo social,
 * para no repetir el mismo manejo de errores en cada botón.
 * Principio SOLID: SRP (solo el patrón "ejecutar y avisar") y evita código duplicado.
 * Colabora con: TareaEnSegundoPlano, Dialogos, ExcepcionReglaNegocio, ExcepcionSocialUna.
 */
package una.social.ui;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.excepciones.ExcepcionSocialUna;
import una.social.excepciones.ExcepcionValidacion;

/**
 * Ejecución de acciones con manejo de errores estándar.
 */
final class AccionesUi {

    /** Clase de utilidades: no se instancia. */
    private AccionesUi() {
    }

    /**
     * Ejecuta {@code trabajo} en segundo plano; al terminar llama a {@code alTerminar} y, si falla,
     * muestra el aviso que corresponde y llama a {@code despuesDeFallar}.
     *
     * @param padre           componente sobre el que se centran los diálogos
     * @param auditoria       bitácora (para errores inesperados)
     * @param accion          nombre de la acción para la auditoría de errores inesperados
     * @param tituloError     título del diálogo de error
     * @param trabajo         operación (se ejecuta fuera del hilo de Swing)
     * @param alTerminar      qué hacer con el resultado (en el hilo de Swing)
     * @param despuesDeFallar qué hacer después de mostrar el error (por ejemplo, refrescar)
     * @param <T>             tipo del resultado
     */
    static <T> void ejecutar(Component padre, RegistroAuditoria auditoria, String accion, String tituloError,
                             Supplier<T> trabajo, Consumer<T> alTerminar, Runnable despuesDeFallar) {
        TareaEnSegundoPlano.ejecutar(trabajo, alTerminar, falla -> {
            mostrarFallo(padre, auditoria, accion, tituloError, falla);
            despuesDeFallar.run();
        });
    }

    /**
     * Muestra un fallo con el tipo de diálogo adecuado.
     *
     * @param padre       componente sobre el que se centra el diálogo
     * @param auditoria   bitácora
     * @param accion      nombre de la acción
     * @param tituloError título del diálogo
     * @param falla       excepción ocurrida
     */
    static void mostrarFallo(Component padre, RegistroAuditoria auditoria, String accion, String tituloError, Throwable falla) {
        if (falla instanceof ExcepcionReglaNegocio || falla instanceof ExcepcionValidacion) {
            // Regla no cumplida o dato inválido (por ejemplo, "Ya compartiste esta publicación" o
            // "El nombre es obligatorio"): el usuario puede corregirlo, se explica como aviso.
            Dialogos.advertencia(padre, tituloError, falla.getMessage());
        } else if (falla instanceof ExcepcionSocialUna) {
            // Error conocido (por ejemplo, de disco): su mensaje ya es amigable y ya se auditó.
            Dialogos.error(padre, tituloError, falla.getMessage());
        } else {
            Dialogos.errorInesperado(padre, auditoria, accion, falla);
        }
    }
}
