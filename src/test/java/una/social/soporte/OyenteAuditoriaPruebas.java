/*
 * OyenteAuditoriaPruebas.java
 *
 * Qué hace: "escucha" la ejecución de las pruebas JUnit y deja constancia con nivel TEST de
 * cuándo empezaron, cuántas se ejecutaron, cuántas pasaron, fallaron o se omitieron, y el nombre
 * de cada prueba que falló.
 * Rol en la arquitectura: JUnit lo descubre solo gracias al archivo
 * src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener.
 * Escribe en un log de pruebas APARTE (target/auditoria-pruebas.log) usando la misma
 * auditoría encadenada, para no mezclar las pruebas con data/auditoria.log de la app real.
 * Principio SOLID: SRP (solo audita la ejecución de pruebas) y OCP (se agrega sin modificar
 * ninguna prueba).
 * Colabora con: RegistroAuditoriaArchivo, NivelAuditoria, JUnit Platform.
 */
package una.social.soporte;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.auditoria.RegistroAuditoriaArchivo;

/**
 * Oyente de JUnit que audita las ejecuciones de pruebas con nivel TEST.
 */
public final class OyenteAuditoriaPruebas implements TestExecutionListener {

    /** Propiedad del sistema (definida en el pom.xml) con la ruta del log de pruebas. */
    private static final String PROPIEDAD_RUTA = "social.auditoria.pruebas";

    /** Ruta por defecto si se ejecuta fuera de Maven (por ejemplo, desde el IDE). */
    private static final String RUTA_POR_DEFECTO = "target/auditoria-pruebas.log";

    /** Actor de los eventos de pruebas. */
    private static final String ACTOR = "JUNIT";

    /** Bitácora de pruebas (append-only con hash encadenado). */
    private final RegistroAuditoria auditoria =
            new RegistroAuditoriaArchivo(Path.of(System.getProperty(PROPIEDAD_RUTA, RUTA_POR_DEFECTO)), Clock.systemDefaultZone());

    /** Pruebas que pasaron. */
    private final AtomicInteger exitosas = new AtomicInteger();

    /** Pruebas que fallaron. */
    private final AtomicInteger fallidas = new AtomicInteger();

    /** Pruebas omitidas. */
    private final AtomicInteger omitidas = new AtomicInteger();

    /**
     * Se llama una vez antes de correr las pruebas: pone los contadores en cero y lo anota.
     *
     * @param plan pruebas que se van a ejecutar
     */
    @Override
    public void testPlanExecutionStarted(TestPlan plan) {
        // Contadores en cero.
        exitosas.set(0);
        fallidas.set(0);
        omitidas.set(0);
        auditoria.registrar(NivelAuditoria.TEST, ACTOR, "PRUEBAS_INICIO", "Inicia la ejecución de las pruebas unitarias");
    }

    /**
     * Se llama cuando una prueba se salta (por ejemplo, desactivada).
     *
     * @param prueba prueba o clase omitida
     * @param motivo por qué se omitió
     */
    @Override
    public void executionSkipped(TestIdentifier prueba, String motivo) {
        // Solo cuentan las pruebas, no las clases que las contienen.
        if (prueba.isTest()) {
            omitidas.incrementAndGet();
        }
    }

    /**
     * Se llama al terminar cada prueba: cuenta si pasó o falló; si falló, lo anota en la auditoría.
     *
     * @param prueba    prueba o clase que terminó
     * @param resultado resultado (éxito, fallo o abortada)
     */
    @Override
    public void executionFinished(TestIdentifier prueba, TestExecutionResult resultado) {
        if (!prueba.isTest()) {
            return; // Las clases contenedoras no cuentan como pruebas.
        }
        if (resultado.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
            exitosas.incrementAndGet();
            return;
        }
        // Falló: se cuenta y se anota el nombre de la prueba con el error.
        fallidas.incrementAndGet();
        auditoria.registrar(NivelAuditoria.TEST, ACTOR, "PRUEBA_FALLIDA", prueba.getDisplayName() + " ("
                + resultado.getThrowable().map(RegistroAuditoria::describirError).orElse(resultado.getStatus().name()) + ")");
    }

    /**
     * Se llama una vez al final: anota el resumen (total, exitosas, fallidas y omitidas).
     *
     * @param plan pruebas ejecutadas
     */
    @Override
    public void testPlanExecutionFinished(TestPlan plan) {
        // Resumen final en la auditoría.
        int total = exitosas.get() + fallidas.get() + omitidas.get();
        auditoria.registrar(NivelAuditoria.TEST, ACTOR, "PRUEBAS_FIN", "Ejecutadas: " + total + ", exitosas: "
                + exitosas.get() + ", fallidas: " + fallidas.get() + ", omitidas: " + omitidas.get());
    }
}
