package ec.edu.unibe.orquestador.adaptadores.checkstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Solo prueba la validacion del constructor: ejecutar Checkstyle de verdad
 * requiere el jar instalado, igual que {@code AdaptadorPmd} no tiene prueba
 * de integracion para su ejecucion de proceso externo.
 */
@DisplayName("AdaptadorCheckstyle")
class AdaptadorCheckstyleTest {

    @Test
    @DisplayName("rechaza una ruta de jar en blanco")
    void rechazaRutaEnBlanco() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorCheckstyle(" "));
    }

    @Test
    @DisplayName("rechaza una ruta de jar nula")
    void rechazaRutaNula() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorCheckstyle(null));
    }

    @Test
    @DisplayName("se identifica como la herramienta Checkstyle")
    void seIdentificaComoCheckstyle() {
        var adaptador = new AdaptadorCheckstyle("C:\\herramientas\\checkstyle-all.jar");

        assertEquals(HerramientaSoportada.CHECKSTYLE, adaptador.herramienta());
    }
}
