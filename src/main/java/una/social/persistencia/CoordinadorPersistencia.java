/*
 * CoordinadorPersistencia.java
 *
 * Qué hace: carga y guarda TODOS los módulos persistentes de la aplicación de una sola vez
 * (hoy: usuarios; luego también amistades y publicaciones de Sebastián).
 * Rol en la arquitectura: Main lo usa al iniciar (cargar) y en el cierre de la app (guardar);
 * el ControladorNavegacion lo usa al cerrar sesión (guardar).
 * Principio SOLID: SRP (solo coordina la carga/guardado), OCP (para agregar un módulo se agrega
 * a la lista, sin modificar esta clase) y DIP (depende de ModuloPersistente, no de clases
 * concretas).
 * Colabora con: ModuloPersistente, RegistroAuditoria, Main, ControladorNavegacion.
 */
package una.social.persistencia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;

/**
 * Coordinador de carga y guardado de módulos.
 *
 * <p>Si un módulo falla, se sigue con los demás (un error en publicaciones no debe impedir que
 * se guarden los usuarios) y al final se informa cuáles fallaron.</p>
 */
public final class CoordinadorPersistencia {

    /** Módulos registrados, en orden de carga. */
    private final List<ModuloPersistente> modulos;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el coordinador.
     *
     * @param modulos   módulos a coordinar (se copia la lista)
     * @param auditoria bitácora
     */
    public CoordinadorPersistencia(List<ModuloPersistente> modulos, RegistroAuditoria auditoria) {
        this.modulos = List.copyOf(modulos);
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Carga todos los módulos.
     *
     * @return nombres de los módulos que NO se pudieron cargar (vacía si todo salió bien)
     */
    public List<String> cargarTodos() {
        return ejecutarEnCadaModulo("CARGA", ModuloPersistente::cargarDatos);
    }

    /**
     * Guarda todos los módulos.
     *
     * @return nombres de los módulos que NO se pudieron guardar (vacía si todo salió bien)
     */
    public List<String> guardarTodos() {
        return ejecutarEnCadaModulo("GUARDADO", ModuloPersistente::guardarDatos);
    }

    /**
     * Operación sobre un módulo (cargar o guardar).
     */
    @FunctionalInterface
    private interface OperacionModulo {
        /**
         * @param modulo módulo sobre el que se opera
         */
        void ejecutar(ModuloPersistente modulo);
    }

    /**
     * Ejecuta una operación en cada módulo, aislando los fallos.
     *
     * @param nombreOperacion nombre para la auditoría ("CARGA" o "GUARDADO")
     * @param operacion       operación a ejecutar
     * @return nombres de los módulos que fallaron
     */
    private List<String> ejecutarEnCadaModulo(String nombreOperacion, OperacionModulo operacion) {
        List<String> fallidos = new ArrayList<>();
        for (ModuloPersistente modulo : modulos) {
            // Se espera ExcepcionPersistencia (o cualquier error inesperado del módulo). Se
            // audita, se anota el módulo como fallido y se continúa con los siguientes: así un
            // módulo dañado no impide guardar los demás. La UI decide qué mostrar con la lista.
            try {
                operacion.ejecutar(modulo);
            } catch (RuntimeException ex) {
                fallidos.add(modulo.nombreModulo());
                auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, nombreOperacion + "_MODULO_FALLIDO",
                        modulo.nombreModulo() + ": " + RegistroAuditoria.describirError(ex));
            }
        }
        return Collections.unmodifiableList(fallidos);
    }
}
