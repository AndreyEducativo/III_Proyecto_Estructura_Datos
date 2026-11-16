/*
 * BuscadorUsuarios.java
 *
 * Qué hace: busca estudiantes registrados por nombre, usuario o ID, sin distinguir mayúsculas ni
 * tildes, e indica si cada resultado ya es amigo de quien busca o si hay una solicitud pendiente.
 * Rol en la arquitectura: servicio del módulo social que usa el PanelBuscador.
 * Principio SOLID: SRP (solo busca; las solicitudes son de GestorAmistades), ISP y DIP (depende de
 * DirectorioUsuarios, ConsultaAmistades y ConsultaSolicitudes, tres interfaces de solo lectura).
 * Colabora con: DirectorioUsuarios, ConsultaAmistades, ConsultaSolicitudes, ResultadoBusqueda.
 */
package una.social.amistades;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import una.social.modelo.Usuario;
import una.social.servicio.DirectorioUsuarios;

/**
 * Buscador de usuarios.
 *
 * <p><b>Búsqueda lineal:</b> recorre la lista de usuarios una vez (O(n)) y compara el texto con
 * cada uno. Para el tamaño de una red de curso es instantáneo y permite buscar por "contiene"
 * (por ejemplo "var" encuentra a "Maria Vargas"), cosa que un índice exacto no permitiría.</p>
 */
public final class BuscadorUsuarios implements ServicioBusqueda {

    /** Idioma para ordenar alfabéticamente respetando tildes y ñ. */
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CR");

    /** Marcas diacríticas (tildes, diéresis) que quedan separadas tras normalizar a NFD. */
    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");

    /** Usuarios registrados. */
    private final DirectorioUsuarios directorio;

    /** Amistades (para marcar quién ya es amigo). */
    private final ConsultaAmistades amistades;

    /** Solicitudes pendientes (para mostrar "Solicitud enviada" o "Responder solicitud"). */
    private final ConsultaSolicitudes solicitudes;

    /**
     * Crea el buscador.
     *
     * @param directorio  usuarios registrados
     * @param amistades   consulta de amistades
     * @param solicitudes consulta de solicitudes pendientes
     */
    public BuscadorUsuarios(DirectorioUsuarios directorio, ConsultaAmistades amistades, ConsultaSolicitudes solicitudes) {
        this.directorio = Objects.requireNonNull(directorio, "directorio");
        this.amistades = Objects.requireNonNull(amistades, "amistades");
        this.solicitudes = Objects.requireNonNull(solicitudes, "solicitudes");
    }

    /** {@inheritDoc} */
    @Override
    public List<ResultadoBusqueda> buscar(String texto, Usuario solicitante) {
        Objects.requireNonNull(solicitante, "solicitante");
        String buscado = normalizar(texto);
        Set<String> idsAmigos = amistades.idsAmigos(solicitante.getId());
        List<ResultadoBusqueda> resultados = new ArrayList<>();
        for (Usuario usuario : directorio.listarTodos()) {
            if (buscado.isEmpty() || coincide(usuario, buscado)) {
                String id = usuario.getId();
                String yo = solicitante.getId();
                resultados.add(new ResultadoBusqueda(usuario, idsAmigos.contains(id), id.equals(yo),
                        solicitudes.haySolicitudPendiente(yo, id), solicitudes.haySolicitudPendiente(id, yo)));
            }
        }
        Collator alfabetico = Collator.getInstance(ESPANOL);
        resultados.sort(Comparator.comparing((ResultadoBusqueda r) -> r.usuario().getNombre(), alfabetico));
        return Collections.unmodifiableList(resultados);
    }

    /**
     * @param usuario usuario a revisar
     * @param buscado texto buscado, ya normalizado
     * @return {@code true} si el nombre, el usuario o el ID contienen el texto
     */
    private static boolean coincide(Usuario usuario, String buscado) {
        return normalizar(usuario.getNombre()).contains(buscado)
                || normalizar(usuario.getNombreUsuario()).contains(buscado)
                || normalizar(usuario.getId()).contains(buscado);
    }

    /**
     * Normaliza un texto para comparar: sin espacios a los lados, en minúsculas y SIN tildes.
     *
     * <p><b>¿Cómo se quitan las tildes?</b> La forma Unicode NFD separa cada letra acentuada en
     * dos partes: la letra base y la tilde ("á" → "a" + "´"). Luego se borran esas marcas con una
     * expresión regular y queda solo la letra base.</p>
     *
     * @param texto texto original (nulo se trata como vacío)
     * @return texto normalizado
     */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String separado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return MARCAS_DIACRITICAS.matcher(separado).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
