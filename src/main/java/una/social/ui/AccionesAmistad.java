/*
 * AccionesAmistad.java
 *
 * Qué hace: ejecuta en segundo plano las acciones de amistad que dispara un botón (enviar,
 * aceptar, rechazar o cancelar una solicitud, eliminar un amigo) y muestra el aviso adecuado si
 * algo sale mal. También arma el diálogo "X te envió una solicitud de amistad" y el componente con
 * el ESTADO de la relación ("Enviar solicitud", "Solicitud enviada", "Responder solicitud",
 * "Amigos", "Eres tú") que se muestra en el buscador, las sugerencias y los perfiles.
 * Rol en la arquitectura: utilidad de PRESENTACIÓN compartida por PanelBuscador, PanelAmigos,
 * PanelSolicitudes, VentanaPerfil y VentanaPrincipal, para no repetir el mismo código en cada uno.
 * Principio SOLID: SRP (solo coordina "botón → servicio → aviso") y DIP (recibe el servicio).
 * Colabora con: ServicioAmistades, AccionesUi, Dialogos, EstadoRelacion.
 */
package una.social.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import una.social.amistades.ServicioAmistades;
import una.social.auditoria.RegistroAuditoria;
import una.social.modelo.Usuario;
import una.social.ui.tema.BotonTema;
import una.social.ui.tema.TemaUi;

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
     * Ejecuta una acción en segundo plano y maneja sus errores (delega en {@link AccionesUi}).
     *
     * @param padre           componente sobre el que se muestran los avisos
     * @param auditoria       bitácora (para errores inesperados)
     * @param accion          nombre de la acción para la auditoría (por ejemplo "ENVIAR_SOLICITUD")
     * @param tituloError     título del aviso si la acción no se pudo hacer
     * @param trabajo         llamada al servicio
     * @param alTerminar      qué hacer si salió bien (en el hilo de Swing)
     * @param despuesDeFallar qué hacer después de mostrar el aviso de error (por ejemplo, refrescar)
     * @param <T>             tipo del resultado
     */
    static <T> void ejecutar(Component padre, RegistroAuditoria auditoria, String accion, String tituloError,
                             Supplier<T> trabajo, Consumer<T> alTerminar, Runnable despuesDeFallar) {
        AccionesUi.ejecutar(padre, auditoria, accion, tituloError, trabajo, alTerminar, despuesDeFallar);
    }

    /**
     * Crea el componente que muestra el estado de la relación con otro usuario y permite actuar:
     * <ul>
     *   <li>Uno mismo: texto "Eres tú".</li>
     *   <li>Amigos: texto "Amigos".</li>
     *   <li>Solicitud enviada: texto "Solicitud enviada" + botón "Cancelar".</li>
     *   <li>Solicitud recibida: botón "Responder solicitud".</li>
     *   <li>Ninguna: botón "Enviar solicitud".</li>
     * </ul>
     *
     * @param padre      componente para los diálogos
     * @param amistades  servicio de amistades
     * @param auditoria  bitácora
     * @param yo         usuario activo
     * @param otro       otro usuario
     * @param estado     relación actual
     * @param alCambiar  qué hacer después de cualquier acción (normalmente refrescar)
     * @return componente para colocar a la derecha de una fila o en un perfil
     */
    static JComponent componenteEstado(Component padre, ServicioAmistades amistades, RegistroAuditoria auditoria,
                                       Usuario yo, Usuario otro, EstadoRelacion estado, Runnable alCambiar) {
        switch (estado) {
            case UNO_MISMO:
                return TextosUi.textoSecundario("Eres tú");
            case AMIGOS:
                return TextosUi.etiquetaPlana("Amigos", TemaUi.FUENTE_NEGRITA, TemaUi.AZUL_UNA);
            case SOLICITUD_RECIBIDA: {
                BotonTema responder = TemaUi.crearBotonPrimario("Responder solicitud");
                responder.addActionListener(e -> preguntarRespuesta(padre, amistades, yo, otro, auditoria, alCambiar));
                return responder;
            }
            case SOLICITUD_ENVIADA: {
                BotonTema cancelar = TemaUi.crearBotonSecundario("Cancelar");
                cancelar.addActionListener(e -> cancelarSolicitud(padre, amistades, yo, otro, auditoria, cancelar, alCambiar));
                JPanel pendiente = new JPanel(new FlowLayout(FlowLayout.RIGHT, TemaUi.ESPACIO, 0));
                pendiente.setOpaque(false);
                pendiente.add(TextosUi.textoSecundario("Solicitud enviada"));
                pendiente.add(cancelar);
                return pendiente;
            }
            default: {
                BotonTema enviar = TemaUi.crearBotonSecundario("Enviar solicitud");
                enviar.addActionListener(e -> enviarSolicitud(padre, amistades, yo, otro, auditoria, enviar, alCambiar));
                return enviar;
            }
        }
    }

    /**
     * Envía una solicitud. Si el otro ya nos había enviado una, el servicio la acepta
     * automáticamente y se avisa que ahora son amigos.
     *
     * @param padre     componente para los diálogos
     * @param amistades servicio de amistades
     * @param yo        usuario activo
     * @param otro      destinatario
     * @param auditoria bitácora
     * @param boton     botón pulsado (se deshabilita para evitar doble clic)
     * @param alCambiar qué hacer al final
     */
    static void enviarSolicitud(Component padre, ServicioAmistades amistades, Usuario yo, Usuario otro,
                                RegistroAuditoria auditoria, BotonTema boton, Runnable alCambiar) {
        boton.setEnabled(false);
        ejecutar(padre, auditoria, "ENVIAR_SOLICITUD", "No se pudo enviar la solicitud",
                () -> amistades.enviarSolicitud(yo, otro.getId()), resultado -> {
                    // Se refresca antes del aviso para que el botón ya muestre el estado nuevo detrás.
                    alCambiar.run();
                    if (resultado.seAceptoAutomaticamente()) {
                        Dialogos.exito(padre, "¡Ahora son amigos!", otro.getNombre() + " ya te había enviado una solicitud, "
                                + "así que se aceptó automáticamente.\nSus publicaciones aparecerán en tu muro.");
                    } else {
                        Dialogos.exito(padre, "Solicitud enviada", "Se le envió una solicitud de amistad a " + otro.getNombre()
                                + ".\nCuando la acepte, sus publicaciones aparecerán en tu muro.");
                    }
                }, alCambiar);
    }

    /**
     * Cancela (con confirmación) una solicitud enviada que sigue pendiente.
     *
     * @param padre     componente para los diálogos
     * @param amistades servicio de amistades
     * @param yo        usuario activo (remitente)
     * @param otro      destinatario
     * @param auditoria bitácora
     * @param boton     botón pulsado
     * @param alCambiar qué hacer al final
     */
    static void cancelarSolicitud(Component padre, ServicioAmistades amistades, Usuario yo, Usuario otro,
                                  RegistroAuditoria auditoria, BotonTema boton, Runnable alCambiar) {
        if (!Dialogos.confirmar(padre, "Cancelar solicitud",
                "¿Quieres cancelar la solicitud de amistad que le enviaste a " + otro.getNombre() + "?", "Sí, cancelarla")) {
            return;
        }
        boton.setEnabled(false);
        ejecutar(padre, auditoria, "CANCELAR_SOLICITUD", "No se pudo cancelar", () -> {
            amistades.cancelarSolicitud(yo, otro.getId());
            return Boolean.TRUE;
        }, hecho -> alCambiar.run(), alCambiar);
    }

    /**
     * Elimina (con confirmación) a un amigo.
     *
     * @param padre     componente para los diálogos
     * @param amistades servicio de amistades
     * @param yo        usuario activo
     * @param amigo     amigo a eliminar
     * @param auditoria bitácora
     * @param boton     botón pulsado
     * @param alCambiar qué hacer al final
     */
    static void eliminarAmigo(Component padre, ServicioAmistades amistades, Usuario yo, Usuario amigo,
                              RegistroAuditoria auditoria, BotonTema boton, Runnable alCambiar) {
        if (!Dialogos.confirmar(padre, "Eliminar amigo", "¿Seguro que quieres eliminar a " + amigo.getNombre()
                + " de tus amigos?\nYa no verán las publicaciones \"solo amigos\" del otro.", "Eliminar")) {
            return;
        }
        boton.setEnabled(false);
        ejecutar(padre, auditoria, "ELIMINAR_AMIGO", "No se pudo eliminar", () -> {
            amistades.eliminarAmistad(yo, amigo.getId());
            return Boolean.TRUE;
        }, hecho -> {
            // Se refresca antes del aviso para que, detrás de él, la lista ya no muestre al amigo.
            alCambiar.run();
            Dialogos.exito(padre, "Amigo eliminado", amigo.getNombre() + " ya no está en tu lista de amigos.");
        }, alCambiar);
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
