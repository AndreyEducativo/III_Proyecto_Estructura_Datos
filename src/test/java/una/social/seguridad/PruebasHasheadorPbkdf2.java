/*
 * PruebasHasheadorPbkdf2.java
 *
 * Qué prueba: que la contraseña se transforme en un hash con sal aleatoria, que la verificación
 * acepte la contraseña correcta y rechace las incorrectas, y que no se modifique el arreglo.
 * Colabora con: HasheadorPbkdf2.
 */
package una.social.seguridad;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link HasheadorPbkdf2}.
 */
class PruebasHasheadorPbkdf2 {

    /** Pocas iteraciones para que las pruebas sean rápidas (el algoritmo es el mismo). */
    private static final int ITERACIONES_PRUEBA = 1_000;

    /** Hasheador bajo prueba. */
    private final HasheadorPbkdf2 hasheador = new HasheadorPbkdf2(ITERACIONES_PRUEBA, new SecureRandom());

    @Test
    @DisplayName("La misma contraseña produce hashes distintos gracias a la sal aleatoria")
    void salAleatoria() {
        CredencialProtegida a = hasheador.proteger("secreto1".toCharArray());
        CredencialProtegida b = hasheador.proteger("secreto1".toCharArray());
        assertNotEquals(a.salBase64(), b.salBase64());
        assertNotEquals(a.hashBase64(), b.hashBase64());
        assertEquals(16, Base64.getDecoder().decode(a.salBase64()).length);
        assertEquals(32, Base64.getDecoder().decode(a.hashBase64()).length);
    }

    @Test
    @DisplayName("Verifica la contraseña correcta y rechaza las incorrectas")
    void verificaCorrectaRechazaIncorrecta() {
        CredencialProtegida credencial = hasheador.proteger("secreto1".toCharArray());
        assertTrue(hasheador.verificar("secreto1".toCharArray(), credencial));
        assertFalse(hasheador.verificar("secreto2".toCharArray(), credencial));
        assertFalse(hasheador.verificar("".toCharArray(), credencial));
    }

    @Test
    @DisplayName("No modifica el arreglo de la contraseña (quien llama decide cuándo borrarlo)")
    void noModificaElArreglo() {
        char[] contrasena = "secreto1".toCharArray();
        hasheador.verificar(contrasena, hasheador.proteger(contrasena));
        assertArrayEquals("secreto1".toCharArray(), contrasena);
    }

    @Test
    @DisplayName("Datos guardados que no son Base64 se tratan como contraseña incorrecta, sin lanzar")
    void base64InvalidoEsFalso() {
        assertFalse(hasheador.verificar("x".toCharArray(), new CredencialProtegida("%%%", "%%%")));
    }

    @Test
    @DisplayName("toString de la credencial no expone la sal ni el hash")
    void toStringOculto() {
        CredencialProtegida credencial = hasheador.proteger("secreto1".toCharArray());
        assertFalse(credencial.toString().contains(credencial.hashBase64()));
    }

    @Test
    @DisplayName("Iteraciones no positivas se rechazan")
    void iteracionesInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> new HasheadorPbkdf2(0, new SecureRandom()));
    }
}
