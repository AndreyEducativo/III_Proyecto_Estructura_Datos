/*
 * GestorPersistenteBase.java
 *
 * Qué hace: esqueleto común de los gestores que guardan sus datos en archivos: cargar y guardar
 * con auditoría, impedir guardados si la carga falló, auditar cada acción del usuario y auditar
 * los registros omitidos al cargar.
 * Rol en la arquitectura: clase base de GestorBloqueos, GestorMeGusta, GestorComentarios,
 * GestorPerfiles y GestorAnuncios. Evita repetir en cada uno el mismo código de try/catch y de
 * auditoría.
 * Principio SOLID: patrón "Template Method": esta clase fija los PASOS (cargar → marcar como
 * cargado → auditar) y cada subclase solo aporta su parte (qué leer y qué escribir). Cumple OCP
 * (un módulo nuevo hereda sin modificar esta clase) y SRP (cada subclase solo tiene su lógica).
 * Colabora con: ModuloPersistente, RegistroAuditoria, ExcepcionReglaNegocio, ExcepcionPersistencia.
 */
package una.social.persistencia;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionReglaNegocio;
import una.social.modelo.Usuario;

/**
 * Base de los gestores con persistencia en archivos.
 *
 * <p><b>Hilos:</b> {@link #cargarDatos()} y {@link #guardarDatos()} son {@code synchronized} sobre
 * el propio gestor; las subclases deben sincronizar también sus métodos que tocan la memoria,
 * así todo usa el MISMO candado.</p>
 */
public abstract class GestorPersistenteBase implements ModuloPersistente {

    /** Bitácora de auditoría (la usan también las subclases). */
    protected final RegistroAuditoria auditoria;

    /**
     * {@code true} solo si la última carga desde el disco salió bien. Mientras sea {@code false}
     * no se permite guardar, para no sobrescribir el archivo real con datos vacíos.
     */
    private boolean datosCargados;

    /**
     * Crea la base.
     *
     * @param auditoria bitácora (no nula)
     */
    protected GestorPersistenteBase(RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee el disco y reconstruye la memoria. Lo implementa cada subclase.
     *
     * @throws ExcepcionPersistencia si el archivo no se puede leer
     */
    protected abstract void cargarDesdeDisco();

    /**
     * Escribe en disco el estado de la memoria. Lo implementa cada subclase.
     *
     * @throws ExcepcionPersistencia si no se pudo escribir
     */
    protected abstract void guardarEnDisco();

    /**
     * @return resumen corto para la auditoría (por ejemplo "4 comentarios")
     */
    protected abstract String resumen();

    /**
     * {@inheritDoc}
     *
     * <p>Plantilla: cargar → marcar como cargado → auditar. Si falla, queda marcado como NO
     * cargado (así no se guarda nada encima) y se relanza para que Main avise al usuario.</p>
     */
    @Override
    public final synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si el archivo no se puede leer: se audita y se relanza.
        try {
            cargarDesdeDisco();
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    nombreModulo() + ": " + resumen());
        } catch (ExcepcionPersistencia ex) {
            datosCargados = false;
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "CARGA_FALLIDA",
                    nombreModulo() + ": " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Plantilla de guardado: revisa que la carga haya salido bien, llama a
     * {@link #guardarEnDisco()} y lo anota en la auditoría (o el error, y lo relanza).
     *
     * @throws ExcepcionPersistencia si la carga falló o el disco no permite escribir
     */
    @Override
    public final synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla: se audita y se relanza.
        try {
            guardarEnDisco();
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    nombreModulo() + ": " + resumen());
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO",
                    nombreModulo() + ": " + RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Impide modificar o guardar si la carga inicial falló.
     *
     * @throws ExcepcionPersistencia si los datos no se cargaron correctamente
     */
    protected final void exigirDatosCargados() {
        if (!datosCargados) {
            throw new ExcepcionPersistencia("Los datos de " + nombreModulo() + " no se cargaron correctamente; "
                    + "no se guardará nada para no sobrescribir el archivo. Reinicie la aplicación.");
        }
    }

    /**
     * @return {@code true} si la última carga salió bien
     */
    protected final synchronized boolean estanDatosCargados() {
        return datosCargados;
    }

    /**
     * Ejecuta una acción del usuario y la audita: INFO si sale bien, WARN si no cumple una regla
     * (el usuario verá el aviso) y ERROR si no se pudo guardar. Así cada caso de uso no repite el
     * mismo try/catch.
     *
     * @param actor     usuario que hace la acción
     * @param accion    nombre de la acción en MAYÚSCULAS (por ejemplo "COMENTARIO_CREADO")
     * @param operacion la acción en sí (normalmente un método synchronized de la subclase)
     * @param detalle   texto para la auditoría a partir del resultado (sin datos sensibles)
     * @param <T>       tipo del resultado
     * @return resultado de la operación
     */
    protected final <T> T ejecutarAuditado(Usuario actor, String accion, Supplier<T> operacion, Function<T, String> detalle) {
        String nombreActor = actor.getNombreUsuario();
        // Se esperan ExcepcionReglaNegocio (regla no cumplida) y ExcepcionPersistencia (no se pudo
        // guardar). Se auditan aquí, en el límite del servicio, y se relanzan para que la UI muestre
        // un aviso (regla) o un error (disco).
        try {
            T resultado = operacion.get();
            auditoria.registrar(NivelAuditoria.INFO, nombreActor, accion, detalle.apply(resultado));
            return resultado;
        } catch (ExcepcionReglaNegocio ex) {
            auditoria.registrar(NivelAuditoria.WARN, nombreActor, accion + "_RECHAZADO", ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, nombreActor, accion + "_FALLIDO", RegistroAuditoria.describirError(ex));
            throw ex;
        }
    }

    /**
     * Audita un registro que se omitió al cargar porque no cumple las reglas (por ejemplo, un
     * usuario que ya no existe).
     *
     * @param archivo nombre del archivo de origen
     * @param motivo  explicación, sin datos sensibles
     */
    protected final void auditarOmitido(String archivo, String motivo) {
        auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                archivo + ": " + motivo);
    }
}
