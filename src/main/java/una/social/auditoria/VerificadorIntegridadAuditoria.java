/*
 * VerificadorIntegridadAuditoria.java
 *
 * Qué hace: recorre data/auditoria.log y comprueba que la cadena de hashes no se haya roto.
 * Rol en la arquitectura: Main lo ejecuta al iniciar la aplicación, ANTES de escribir nuevos
 * eventos; el resultado se anota en la propia bitácora y, si está rota, se muestra una
 * advertencia gráfica (sin bloquear la app).
 * Principio SOLID: SRP (escribir la bitácora y verificarla son responsabilidades distintas, en
 * clases distintas).
 * Colabora con: FormatoLineaAuditoria, ResultadoVerificacion, AlmacenArchivoTexto, Main.
 */
package una.social.auditoria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.persistencia.AlmacenArchivoTexto;

/**
 * Verificador de la cadena de hashes de la auditoría.
 *
 * <p><b>Qué revisa en cada línea, en este orden:</b></p>
 * <ol>
 *   <li>Que no esté vacía (el registrador nunca escribe líneas vacías: una línea vacía en medio
 *       significa que alguien editó el archivo).</li>
 *   <li>Que tenga exactamente 7 campos bien escapados.</li>
 *   <li>Que su {@code hashAnterior} sea igual al {@code hashActual} de la línea de arriba
 *       → detecta líneas BORRADAS, insertadas o cambiadas de orden.</li>
 *   <li>Que el SHA-256 de su contenido sea igual a su {@code hashActual}
 *       → detecta líneas EDITADAS.</li>
 * </ol>
 *
 * <p><b>Limitación honesta:</b> si alguien borra las ÚLTIMAS líneas del archivo, lo que queda
 * sigue siendo una cadena válida. Detectar eso requeriría guardar el último hash en otro lugar
 * seguro (fuera del alcance del proyecto).</p>
 */
public final class VerificadorIntegridadAuditoria {

    /** Archivo de auditoría a verificar. */
    private final Path archivo;

    /**
     * Crea el verificador.
     *
     * @param archivo ruta del archivo de auditoría (no nula)
     */
    public VerificadorIntegridadAuditoria(Path archivo) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
    }

    /**
     * Verifica la cadena completa. Nunca lanza excepciones: cualquier problema se devuelve
     * dentro del resultado.
     *
     * @return resultado con el estado de la cadena y, si está rota, en qué línea y por qué
     */
    public ResultadoVerificacion verificar() {
        if (Files.notExists(archivo)) {
            // Un archivo que aún no existe es una cadena vacía: válida por definición.
            return ResultadoVerificacion.integra(0);
        }
        // Se espera IOException si el archivo no se puede leer (permisos, bloqueado) o si tiene
        // bytes que no son UTF-8 válido (señal de edición binaria). Se informa como "no íntegra"
        // en lugar de lanzar, para que Main pueda advertir al usuario y seguir.
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            return verificarLineas(lineas);
        } catch (IOException ex) {
            return ResultadoVerificacion.rota(0, 0, "no se pudo leer el archivo (" + RegistroAuditoria.describirError(ex) + ")");
        }
    }

    /**
     * Recorre las líneas y se detiene en la primera ruptura.
     *
     * @param lineas contenido del archivo
     * @return resultado de la verificación
     */
    private ResultadoVerificacion verificarLineas(List<String> lineas) {
        String hashEsperado = FormatoLineaAuditoria.HASH_GENESIS;
        for (int indice = 0; indice < lineas.size(); indice++) {
            int numeroLinea = indice + 1;
            String problema = revisarLinea(lineas.get(indice), hashEsperado);
            if (problema != null) {
                return ResultadoVerificacion.rota(indice, numeroLinea, problema);
            }
            // La línea es válida: la siguiente debe apuntar al hash de esta.
            hashEsperado = FormatoLineaAuditoria.separarContenidoYHash(lineas.get(indice))[1];
        }
        return ResultadoVerificacion.integra(lineas.size());
    }

    /**
     * Revisa una sola línea contra el hash que debería tener como "anterior".
     *
     * @param linea        texto de la línea
     * @param hashEsperado hash de la línea previa (o génesis si es la primera)
     * @return descripción del problema, o {@code null} si la línea es válida
     */
    private static String revisarLinea(String linea, String hashEsperado) {
        if (linea.isEmpty()) {
            return "línea vacía insertada";
        }
        String[] campos;
        // Se espera ExcepcionDatosCorruptos si alguien dejó un '\' suelto o un escape inválido.
        // No se relanza: es justamente una evidencia de manipulación y se reporta como tal.
        try {
            campos = AlmacenArchivoTexto.leerRegistro(linea, FormatoLineaAuditoria.CANTIDAD_CAMPOS);
        } catch (ExcepcionDatosCorruptos ex) {
            return "formato inválido (" + ex.getMessage() + ")";
        }
        if (!hashEsperado.equals(campos[FormatoLineaAuditoria.INDICE_HASH_ANTERIOR])) {
            return "no enlaza con la línea anterior (se borró, insertó o reordenó una línea)";
        }
        String[] contenidoYHash = FormatoLineaAuditoria.separarContenidoYHash(linea);
        String hashCalculado = FormatoLineaAuditoria.calcularHash(contenidoYHash[0]);
        if (!hashCalculado.equals(contenidoYHash[1])) {
            return "el contenido fue modificado (el hash no coincide)";
        }
        return null;
    }
}
