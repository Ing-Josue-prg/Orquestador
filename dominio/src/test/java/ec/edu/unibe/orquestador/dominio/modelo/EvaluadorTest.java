package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Evaluador")
class EvaluadorTest {

    @Test
    @DisplayName("rechaza un nombre de usuario en blanco")
    void rechazaNombreUsuarioEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new Evaluador(" ", "hash", "sal", LocalDateTime.now()));
    }

    @Test
    @DisplayName("rechaza un hash de contrasena en blanco")
    void rechazaHashEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new Evaluador("usuario", " ", "sal", LocalDateTime.now()));
    }

    @Test
    @DisplayName("rechaza una fecha de registro nula")
    void rechazaFechaNula() {
        assertThrows(IllegalArgumentException.class,
                () -> new Evaluador("usuario", "hash", "sal", null));
    }

    @Test
    @DisplayName("recorta espacios del nombre de usuario")
    void recortaEspaciosDelNombreUsuario() {
        Evaluador evaluador = new Evaluador("  usuario  ", "hash", "sal", LocalDateTime.now());

        assertEquals("usuario", evaluador.nombreUsuario());
    }
}
