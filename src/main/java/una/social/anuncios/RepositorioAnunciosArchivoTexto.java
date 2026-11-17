/*
 * RepositorioAnunciosArchivoTexto.java
 *
 * Qué hace: guarda y lee los anuncios en data/anuncios.txt con el formato
 * idAnuncio|idAutor|fechaISO|titulo|texto.
 * Rol en la arquitectura: implementación concreta de RepositorioAnuncios; la crea EnsambladorSocial.
 * Principio SOLID: SRP (traduce Anuncio <-> línea; las reglas las aplica el gestor) y LSP.
 * Colabora con: ArchivoRegistros, AlmacenArchivoTexto, RegistroAuditoria, Anuncio.
 */
package una.social.anuncios;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionDatosCorruptos;
import una.social.modelo.Usuario;
import una.social.persistencia.AlmacenArchivoTexto;
import una.social.persistencia.ArchivoRegistros;

/**
 * Repositorio de anuncios sobre un archivo de texto.
 */
public final class RepositorioAnunciosArchivoTexto implements RepositorioAnuncios {

    /** Cantidad de columnas de una línea. */
    private static final int CANTIDAD_COLUMNAS = 5;

    /** Encabezado que documenta el formato dentro del propio archivo (el lector lo ignora). */
    private static final List<String> ENCABEZADO = AlmacenArchivoTexto.comoComentarios(
            "=====================================================================",
            "Social UNA - Anuncios públicos (archivo generado por la aplicación)",
            "Una línea por anuncio. Todos los usuarios los ven. Columnas separadas por '|':",
            "  1. idAnuncio -> identificador único A0001, A0002, ... (lo asigna el sistema)",
            "  2. idAutor   -> usuario que publicó el anuncio (U001...)",
            "  3. fechaISO  -> fecha y hora de publicación en ISO-8601 UTC",
            "  4. titulo    -> título (máximo " + Anuncio.LARGO_MAXIMO_TITULO + " caracteres)",
            "  5. texto     -> texto del anuncio (máximo " + Anuncio.LARGO_MAXIMO_TEXTO + " caracteres)",
            "Escape dentro del texto: \\| = barra vertical, \\\\ = barra invertida, \\n = salto de línea.",
            "Las líneas que empiezan con '#' son comentarios y el programa las ignora.",
            "=====================================================================");

    /** Ruta de anuncios.txt. */
    private final Path archivo;

    /** Bitácora. */
    private final RegistroAuditoria auditoria;

    /**
     * Crea el repositorio.
     *
     * @param archivo   ruta de anuncios.txt
     * @param auditoria bitácora
     */
    public RepositorioAnunciosArchivoTexto(Path archivo, RegistroAuditoria auditoria) {
        // Se guardan las dependencias recibidas; requireNonNull falla de inmediato si falta alguna.
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /**
     * Lee data/anuncios.txt y devuelve los anuncios que contiene. Las líneas dañadas
     * (columnas de más o de menos, IDs o fechas inválidas) se saltan y se anotan en la auditoría.
     *
     * @return lista leída, en el orden del archivo (vacía si el archivo no existe)
     * @throws una.social.excepciones.ExcepcionPersistencia si el archivo no se puede leer
     */
    @Override
    public List<Anuncio> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioAnunciosArchivoTexto::convertir, auditoria);
    }

    /**
     * Reescribe data/anuncios.txt completo con los anuncios de la lista. Se escribe primero
     * en un archivo temporal y luego se reemplaza, así nunca queda un archivo a medias.
     *
     * @param anuncios lista completa a guardar
     * @throws una.social.excepciones.ExcepcionPersistencia si no se puede escribir
     */
    @Override
    public void guardarTodos(List<Anuncio> anuncios) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, anuncios, a -> new String[] {
            a.id(), a.idAutor(), a.fecha().toString(), a.titulo(), a.texto()}, auditoria);
    }

    /**
     * Convierte las 5 columnas de una línea de anuncios.txt en un Anuncio, validando cada una.
     *
     * @param c columnas desescapadas
     * @return anuncio leído
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Anuncio convertir(String[] c) {
        // Columnas 3 y 4: título y texto (no pueden venir vacíos).
        String titulo = ArchivoRegistros.exigirTexto(c[3], "titulo").strip();
        String texto = ArchivoRegistros.exigirTexto(c[4], "texto");
        // Si alguien editó el archivo y puso textos más largos que lo permitido, la línea se rechaza.
        if (titulo.length() > Anuncio.LARGO_MAXIMO_TITULO || texto.length() > Anuncio.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionDatosCorruptos("el título o el texto superan el largo máximo");
        }
        // Columnas 0, 1 y 2: ID del anuncio, ID del autor y fecha (con su formato validado).
        return new Anuncio(
                ArchivoRegistros.exigirPatron(c[0], Anuncio.PATRON_ID, "idAnuncio"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idAutor"),
                ArchivoRegistros.leerFecha(c[2], "fechaISO"),
                titulo, texto);
    }
}
