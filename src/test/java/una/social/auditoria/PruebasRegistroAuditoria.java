/*
 * PruebasRegistroAuditoria.java
 *
 * Qué prueba: que la auditoría sea de solo agregar (append-only), que la cadena de hashes
 * continúe entre ejecuciones, que el verificador detecte ediciones, borrados e inserciones
 * indicando la línea, que sea segura entre hilos y que nunca lance excepciones.
 * Colabora con: RegistroAuditoriaArchivo, VerificadorIntegridadAuditoria.
 */
package una.social.auditoria;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pruebas de la auditoría append-only con hash encadenado.
 */
class PruebasRegistroAuditoria {

    /** Carpeta temporal de cada prueba. */
    @TempDir
    Path carpeta;

    /**
     * @param archivo ruta del log
     * @return registrador nuevo sobre ese archivo
     */
    private static RegistroAuditoriaArchivo nuevoRegistro(Path archivo) {
        return new RegistroAuditoriaArchivo(archivo, Clock.systemUTC());
    }

    /**
     * @param archivo ruta del log
     * @return resultado de verificar el archivo
     */
    private static ResultadoVerificacion verificar(Path archivo) {
        return new VerificadorIntegridadAuditoria(archivo).verificar();
    }

    /**
     * Escribe tres eventos de ejemplo.
     *
     * @param archivo ruta del log
     */
    private static void escribirTresEventos(Path archivo) {
        RegistroAuditoriaArchivo registro = nuevoRegistro(archivo);
        registro.registrar(NivelAuditoria.INFO, "SISTEMA", "APP_INICIO", "inicio");
        registro.registrar(NivelAuditoria.WARN, "ana", "LOGIN_FALLIDO", "intento 1 de 5");
        registro.registrar(NivelAuditoria.INFO, "ana", "LOGIN_EXITOSO", "U001");
    }

    /** Prueba: Append-only: un segundo registrador agrega al final sin tocar las líneas existentes y la cadena sigue íntegra. */
    @Test
    @DisplayName("Append-only: un segundo registrador agrega al final sin tocar las líneas existentes y la cadena sigue íntegra")
    void appendOnlyYCadenaEntreEjecuciones() throws IOException {
        Path archivo = carpeta.resolve("auditoria.log");
        escribirTresEventos(archivo);
        List<String> antes = Files.readAllLines(archivo, StandardCharsets.UTF_8);

        // Simula reabrir la aplicación: un registrador NUEVO sobre el mismo archivo.
        nuevoRegistro(archivo).registrar(NivelAuditoria.INFO, "SISTEMA", "APP_INICIO", "segunda ejecución");
        List<String> despues = Files.readAllLines(archivo, StandardCharsets.UTF_8);

        assertEquals(antes.size() + 1, despues.size());
        assertEquals(antes, despues.subList(0, antes.size()), "las líneas viejas no deben cambiar");
        ResultadoVerificacion resultado = verificar(archivo);
        assertTrue(resultado.integra(), resultado.descripcion());
        assertEquals(4, resultado.lineasVerificadas());
    }

    /** Prueba: La interfaz no ofrece métodos para editar, borrar ni truncar: solo 'registrar'. */
    @Test
    @DisplayName("La interfaz no ofrece métodos para editar, borrar ni truncar: solo 'registrar'")
    void interfazSinMetodosDeModificacion() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        List<String> abstractos = new ArrayList<>();
        for (Method metodo : RegistroAuditoria.class.getDeclaredMethods()) {
            if (Modifier.isAbstract(metodo.getModifiers())) {
                abstractos.add(metodo.getName());
            }
        }
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(List.of("registrar"), abstractos);
    }

    /** Prueba: Detalles con |, saltos de línea y barras invertidas no rompen el formato ni la cadena. */
    @Test
    @DisplayName("Detalles con |, saltos de línea y barras invertidas no rompen el formato ni la cadena")
    void detalleConCaracteresEspeciales() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("auditoria.log");
        nuevoRegistro(archivo).registrar(NivelAuditoria.ERROR, "SISTEMA", "ERROR", "a|b\nc\\d\r\ne");
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(1, Files.readAllLines(archivo, StandardCharsets.UTF_8).size(), "un evento = una línea");
        assertTrue(verificar(archivo).integra());
    }

    /** Prueba: Editar a mano una línea se detecta e indica el número de línea. */
    @Test
    @DisplayName("Editar a mano una línea se detecta e indica el número de línea")
    void detectaEdicion() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("auditoria.log");
        escribirTresEventos(archivo);
        List<String> lineas = new ArrayList<>(Files.readAllLines(archivo, StandardCharsets.UTF_8));
        lineas.set(1, lineas.get(1).replace("intento 1 de 5", "intento 0 de 5"));
        Files.write(archivo, lineas, StandardCharsets.UTF_8);

        ResultadoVerificacion resultado = verificar(archivo);
        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(resultado.integra());
        assertEquals(2, resultado.lineaRota());
    }

    /** Prueba: Borrar una línea intermedia se detecta en la línea siguiente. */
    @Test
    @DisplayName("Borrar una línea intermedia se detecta en la línea siguiente")
    void detectaBorrado() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("auditoria.log");
        escribirTresEventos(archivo);
        List<String> lineas = new ArrayList<>(Files.readAllLines(archivo, StandardCharsets.UTF_8));
        lineas.remove(1);
        Files.write(archivo, lineas, StandardCharsets.UTF_8);

        ResultadoVerificacion resultado = verificar(archivo);
        // Verificar: se revisa que el resultado sea el esperado.
        assertFalse(resultado.integra());
        assertEquals(2, resultado.lineaRota());
    }

    /** Prueba: Insertar una línea vacía o con formato inválido se detecta. */
    @Test
    @DisplayName("Insertar una línea vacía o con formato inválido se detecta")
    void detectaInsercion() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("auditoria.log");
        escribirTresEventos(archivo);
        List<String> lineas = new ArrayList<>(Files.readAllLines(archivo, StandardCharsets.UTF_8));
        lineas.add(1, "");
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
        // Verificar: se revisa que el resultado sea el esperado.
        assertEquals(2, verificar(archivo).lineaRota());

        lineas.set(1, "texto cualquiera");
        Files.write(archivo, lineas, StandardCharsets.UTF_8);
        assertEquals(2, verificar(archivo).lineaRota());
    }

    /** Prueba: Es segura entre hilos: 8 hilos x 50 eventos producen 400 líneas con la cadena íntegra. */
    @Test
    @DisplayName("Es segura entre hilos: 8 hilos x 50 eventos producen 400 líneas con la cadena íntegra")
    void seguraEntreHilos() throws Exception {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path archivo = carpeta.resolve("auditoria.log");
        RegistroAuditoriaArchivo registro = nuevoRegistro(archivo);
        int hilos = 8;
        int eventosPorHilo = 50;
        ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
        for (int h = 0; h < hilos; h++) {
            int numeroHilo = h;
            ejecutor.submit(() -> {
                for (int i = 0; i < eventosPorHilo; i++) {
                    registro.registrar(NivelAuditoria.INFO, "hilo" + numeroHilo, "EVENTO", "n" + i);
                }
            });
        }
        ejecutor.shutdown();
        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(ejecutor.awaitTermination(30, TimeUnit.SECONDS));

        ResultadoVerificacion resultado = verificar(archivo);
        assertTrue(resultado.integra(), resultado.descripcion());
        assertEquals(hilos * eventosPorHilo, resultado.lineasVerificadas());
    }

    /** Prueba: Nunca lanza excepciones aunque no pueda escribir (la ruta es una carpeta). */
    @Test
    @DisplayName("Nunca lanza excepciones aunque no pueda escribir (la ruta es una carpeta)")
    void nuncaLanzaExcepciones() throws IOException {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        Path rutaQueEsCarpeta = Files.createDirectory(carpeta.resolve("no-es-archivo"));
        RegistroAuditoriaArchivo registro = assertDoesNotThrow(() -> nuevoRegistro(rutaQueEsCarpeta));
        // Verificar: se revisa que el resultado sea el esperado.
        assertDoesNotThrow(() -> registro.registrar(NivelAuditoria.INFO, null, null, null));
    }

    /** Prueba: Un archivo que no existe se considera una cadena íntegra y vacía. */
    @Test
    @DisplayName("Un archivo que no existe se considera una cadena íntegra y vacía")
    void archivoInexistenteEsIntegro() {
        // Preparar y ejecutar: se arma el escenario de la prueba.
        ResultadoVerificacion resultado = verificar(carpeta.resolve("no-existe.log"));
        // Verificar: se revisa que el resultado sea el esperado.
        assertTrue(resultado.integra());
        assertEquals(0, resultado.lineasVerificadas());
    }
}
