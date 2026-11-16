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
        this.archivo = Objects.requireNonNull(archivo, "archivo");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
    }

    /** {@inheritDoc} */
    @Override
    public List<Anuncio> cargarTodos() {
        return ArchivoRegistros.leer(archivo, CANTIDAD_COLUMNAS, RepositorioAnunciosArchivoTexto::convertir, auditoria);
    }

    /** {@inheritDoc} */
    @Override
    public void guardarTodos(List<Anuncio> anuncios) {
        ArchivoRegistros.escribir(archivo, ENCABEZADO, anuncios, a -> new String[] {
            a.id(), a.idAutor(), a.fecha().toString(), a.titulo(), a.texto()}, auditoria);
    }

    /**
     * @param c columnas desescapadas
     * @return anuncio leído
     * @throws ExcepcionDatosCorruptos si alguna columna es inválida
     */
    private static Anuncio convertir(String[] c) {
        String titulo = ArchivoRegistros.exigirTexto(c[3], "titulo").strip();
        String texto = ArchivoRegistros.exigirTexto(c[4], "texto");
        if (titulo.length() > Anuncio.LARGO_MAXIMO_TITULO || texto.length() > Anuncio.LARGO_MAXIMO_TEXTO) {
            throw new ExcepcionDatosCorruptos("el título o el texto superan el largo máximo");
        }
        return new Anuncio(
                ArchivoRegistros.exigirPatron(c[0], Anuncio.PATRON_ID, "idAnuncio"),
                ArchivoRegistros.exigirPatron(c[1], Usuario.PATRON_ID, "idAutor"),
                ArchivoRegistros.leerFecha(c[2], "fechaISO"),
                titulo, texto);
    }
}
