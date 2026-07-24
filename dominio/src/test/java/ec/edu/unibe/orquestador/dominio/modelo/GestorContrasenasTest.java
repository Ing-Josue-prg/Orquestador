package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GestorContrasenas")
class GestorContrasenasTest {

    @Test
    @DisplayName("la misma contrasena y sal siempre producen el mismo hash")
    void mismaContrasenaYSalProducenElMismoHash() {
        String sal = GestorContrasenas.generarSal();

        String hash1 = GestorContrasenas.calcularHash("miContrasena123", sal);
        String hash2 = GestorContrasenas.calcularHash("miContrasena123", sal);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("la misma contrasena con sales distintas produce hashes distintos")
    void mismaContrasenaConSalesDistintasProduceHashesDistintos() {
        String hash1 = GestorContrasenas.calcularHash("miContrasena123", GestorContrasenas.generarSal());
        String hash2 = GestorContrasenas.calcularHash("miContrasena123", GestorContrasenas.generarSal());

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("coincide() es verdadero con la contrasena correcta")
    void coincideConLaContrasenaCorrecta() {
        String sal = GestorContrasenas.generarSal();
        String hash = GestorContrasenas.calcularHash("miContrasena123", sal);

        assertTrue(GestorContrasenas.coincide("miContrasena123", sal, hash));
    }

    @Test
    @DisplayName("coincide() es falso con una contrasena incorrecta")
    void noCoincideConUnaContrasenaIncorrecta() {
        String sal = GestorContrasenas.generarSal();
        String hash = GestorContrasenas.calcularHash("miContrasena123", sal);

        assertFalse(GestorContrasenas.coincide("otraContrasena", sal, hash));
    }
}
