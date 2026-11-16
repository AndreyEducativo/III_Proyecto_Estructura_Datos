/*
 * AccionesAmistad.java
 *
 * Qué hace: ejecuta en segundo plano las acciones de amistad que dispara un botón (enviar,
 * aceptar, rechazar o cancelar una solicitud, eliminar un amigo) y muestra el aviso adecuado si
 * algo sale mal. También arma el diálogo "X te envió una solicitud de amistad".
 * Rol en la arquitectura: utilidad de PRESENTACIÓN compartida por PanelBuscador, PanelAmigos,
 * PanelSolicitudes y VentanaPrincipal, para no repetir el mismo manejo de errores en cada uno.
 * Principio SOLID: SRP (solo coordina "botón → servicio → aviso") y DIP (recibe el servicio).
 * Colabora con: ServicioAmistades, TareaEnSegundoPlano, Dialogos.
 */
package una.social.ui;

import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;
import una.social.amistades.ServicioAmistades;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionAmistadInvalida;
import una.social.excepciones.ExcepcionSocialUna;
import una.social.modelo.Usuario;

/**
 * Acciones de amistad lanzadas desde la interfaz.
 */
final class AccionesAmistad {

    /** Opción del diálogo de solicitud recibida: aceptar. */
    private static final int OPCION_ACEPTAR = 0;

    /** Opción del diálogo de solicitud recibida: rechazar. */
    private static final int OPCION_RECHAZAR = 1;

    /** Clase de utilidades: no se instancia. */
    private AccionesAmistad() {
    }

    /**
     * Ejecuta una acción en segundo plano y maneja sus errores.
     *
     * @param padre       componente sobre el que se muestran los avisos
     * @param auditoria   bitácora (para errores inesperados)
     * @param accion      nombre de la acción para la auditoría (por ejemplo "ENVIAR_SOLICITUD")
     * @param tituloError título del aviso si la acción no se pudo hacer
     * @param trabajo     llamada al servicio
     * @param alTerminar  qué hacer si salió bien (en el hilo de Swing)
     * @param despuesDeFallar qué hacer después de mostrar el aviso de error (por ejemplo, refrescar)
     * @param <T>         tipo del resultado
     */
    static <T> void ejecutar(Component padre, RegistroAuditoria auditoria, String accion, String tituloError,
                             Supplier<T> trabajo, Consumer<T> alTerminar, Runnable despuesDeFallar) {
        TareaEnSegundoPlano.ejecutar(trabajo, alTerminar, falla -> {
            if (falla instanceof ExcepcionAmistadInvalida) {
                // Regla no cumplida (por ejemplo, la otra persona ya respondió): se explica y se
                // refresca para mostrar el estado real.
                Dialogos.advertencia(padre, tituloError, falla.getMessage());
            } else if (falla instanceof ExcepcionSocialUna) {
                Dialogos.error(padre, tituloError, falla.getMessage());
            } else {
                Dialogos.errorInesperado(padre, auditoria, accion, falla);
            }
            despuesDeFallar.run();
        });
    }

    /**
     * Muestra "X te envió una solicitud de amistad" con las opciones Aceptar / Rechazar /
     * Decidir después, y ejecuta la elegida.
     *
     * @param padre        componente sobre el que se muestra el diálogo
     * @param amistades    servicio de amistades
     * @param usuario      usuario activo (destinatario de la solicitud)
     * @param remitente    usuario que envió la solicitud
     * @param auditoria    bitácora
     * @param alTerminar   se llama al final en todos los casos (para refrescar o seguir con la
     *                     siguiente solicitud)
     */
    static void preguntarRespuesta(Component padre, ServicioAmistades amistades, Usuario usuario, Usuario remitente,
                                   RegistroAuditoria auditoria, Runnable alTerminar) {
        int opcion = Dialogos.elegir(padre, "Solicitud de amistad",
                remitente.getNombre() + " (@" + remitente.getNombreUsuario() + ") te envió una solicitud de amistad.\n\n"
                        + "¿Quieres aceptarla?",
                "Aceptar", "Rechazar", "Decidir después");
        if (opcion == OPCION_ACEPTAR) {
            aceptar(padre, amistades, usuario, remitente, auditoria, alTerminar);
        } else if (opcion == OPCION_RECHAZAR) {
            rechazar(padre, amistades, usuario, remitente, auditoria, alTerminar);
        } else {
            alTerminar.run();
        }
    }

    /**
     * Acepta la solicitud de {@code remitente} y avisa que ahora son amigos.
     *
     * @param padre      componente sobre el que se muestran los avisos
     * @param amistades  servicio de amistades
     * @param usuario    usuario activo
     * @param remitente  quien envió la solicitud
     * @param auditoria  bitácora
     * @param alTerminar se llama al final (salga bien o mal)
     */
    static void aceptar(Component padre, ServicioAmistades amistades, Usuario usuario, Usuario remitente,
                        RegistroAuditoria auditoria, Runnable alTerminar) {
        ejecutar(padre, auditoria, "ACEPTAR_SOLICITUD", "No se pudo aceptar",
                () -> amistades.aceptarSolicitud(usuario, remitente.getId()), creada -> {
                    Dialogos.exito(padre, "¡Nuevo amigo!", "Ahora tú y " + remitente.getNombre() + " son amigos.\n"
                            + "Sus publicaciones aparecerán en tu muro.");
                    alTerminar.run();
                }, alTerminar);
    }

    /**
     * Rechaza la solicitud de {@code remitente} (sin avisarle a esa persona).
     *
     * @param padre      componente sobre el que se muestran los avisos
     * @param amistades  servicio de amistades
     * @param usuario    usuario activo
     * @param remitente  quien envió la solicitud
     * @param auditoria  bitácora
     * @param alTerminar se llama al final (salga bien o mal)
     */
    static void rechazar(Component padre, ServicioAmistades amistades, Usuario usuario, Usuario remitente,
                         RegistroAuditoria auditoria, Runnable alTerminar) {
        ejecutar(padre, auditoria, "RECHAZAR_SOLICITUD", "No se pudo rechazar", () -> {
            amistades.rechazarSolicitud(usuario, remitente.getId());
            return Boolean.TRUE;
        }, hecho -> alTerminar.run(), alTerminar);
    }
}
