/*
 * PruebasBuscadorUsuarios.java
 *
 * Qué prueba: el buscador de usuarios: coincidencias por nombre, usuario e ID sin distinguir
 * mayúsculas ni tildes, orden alfabético y marcas de "amigo" y "es usted".
 * Colabora con: BuscadorUsuarios, DirectorioEnMemoria.
 */
package una.social.amistades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import una.social.soporte.DirectorioEnMemoria;

/**
 * Pruebas de {@link BuscadorUsuarios}.
 */
class PruebasBuscadorUsuarios {

    /** Usuarios de prueba. */
    private final DirectorioEnMemoria directorio = DirectorioEnMemoria.conUsuariosDePrueba();

    /** Consulta de amistades fija: Ana (U001) es amiga de Luis (U002). */
    private final ConsultaAmistades amistades = new ConsultaAmistades() {
        @Override
        public Set<String> idsAmigos(String idUsuario) {
            return switch (idUsuario) {
                case "U001" -> Set.of("U002");
                case "U002" -> Set.of("U001");
                default -> Set.of();
            };
        }

        @Override
        public boolean sonAmigos(String idA, String idB) {
            return idsAmigos(idA).contains(idB);
        }
    };

    /** Buscador bajo prueba. */
    private final BuscadorUsuarios buscador = new BuscadorUsuarios(directorio, amistades, (remitente, destinatario) -> false);

    /**
     * @param texto texto buscado
     * @return nombres encontrados, en el orden devuelto
     */
    private List<String> nombres(String texto) {
        return buscador.buscar(texto, directorio.obtener("U001")).stream().map(r -> r.usuario().getNombre()).toList();
    }

    @Test
    @DisplayName("Busca por nombre, usuario o ID sin distinguir mayúsculas ni tildes")
    void buscaSinMayusculasNiTildes() {
        assertEquals(List.of("María Vargas"), nombres("MARIA"));
        assertEquals(List.of("María Vargas"), nombres("vargas"));
        assertEquals(List.of("Luis Rojas"), nombres("luis"));
        assertEquals(List.of("Luis Rojas"), nombres("u002"));
        assertEquals(List.of(), nombres("nadie"));
    }

    @Test
    @DisplayName("Texto vacío devuelve a todos, en orden alfabético")
    void vacioDevuelveTodos() {
        assertEquals(List.of("Ana Mora", "Luis Rojas", "María Vargas"), nombres("   "));
    }

    @Test
    @DisplayName("Marca quién ya es amigo y quién es el propio usuario")
    void marcaAmigosYUnoMismo() {
        List<ResultadoBusqueda> resultados = buscador.buscar("", directorio.obtener("U001"));
        ResultadoBusqueda ana = resultados.get(0);
        ResultadoBusqueda luis = resultados.get(1);
        ResultadoBusqueda maria = resultados.get(2);
        assertTrue(ana.esElMismo());
        assertTrue(luis.esAmigo() && !luis.esElMismo());
        assertFalse(maria.esAmigo());
    }
}
