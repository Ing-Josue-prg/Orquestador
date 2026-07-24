package ec.edu.unibe.orquestador.adaptadores.eslint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Solo prueba la validacion del constructor: ejecutar ESLint de verdad
 * requiere Node/npm instalados y acceso a red, igual que {@code AdaptadorPmd}
 * no tiene prueba de integracion para su ejecucion de proceso externo.
 */
@DisplayName("AdaptadorEslint")
class AdaptadorEslintTest {

    @Test
    @DisplayName("rechaza un comando de ESLint en blanco")
    void rechazaEslintEnBlanco() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorEslint(" ", "npm"));
    }

    @Test
    @DisplayName("rechaza un comando de npm en blanco")
    void rechazaNpmEnBlanco() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorEslint("eslint", " "));
    }

    @Test
    @DisplayName("se identifica como la herramienta ESLint")
    void seIdentificaComoEslint() {
        var adaptador = new AdaptadorEslint("eslint", "npm");

        assertEquals(HerramientaSoportada.ESLINT, adaptador.herramienta());
    }
}
