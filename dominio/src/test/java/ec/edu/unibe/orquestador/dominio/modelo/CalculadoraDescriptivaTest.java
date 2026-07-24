package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalculadoraDescriptiva")
class CalculadoraDescriptivaTest {

    private static final double DELTA = 0.000001;

    @Test
    @DisplayName("con tres corridas calcula media, minimo y maximo")
    void conTresCorridasCalculaMediaYRango() {
        ResumenEstadistico resumen = CalculadoraDescriptiva.resumir(List.of(4.0, 4.2, 4.4));

        assertEquals(3, resumen.cantidad());
        assertEquals(4.2, resumen.media(), DELTA);
        assertEquals(4.0, resumen.minimo(), DELTA);
        assertEquals(4.4, resumen.maximo(), DELTA);
    }

    @Test
    @DisplayName("usa desviacion estandar muestral, dividiendo entre n-1")
    void usaDesviacionMuestral() {
        // Valores 2, 4 y 6: media 4, desviacion muestral 2, coeficiente 50 %.
        ResumenEstadistico resumen = CalculadoraDescriptiva.resumir(List.of(2.0, 4.0, 6.0));

        assertEquals(2.0, resumen.desviacionEstandar(), DELTA);
        assertEquals(50.0, resumen.coeficienteVariacion(), DELTA);
    }

    @Test
    @DisplayName("con un solo valor la desviacion es cero")
    void conUnSoloValorLaDesviacionEsCero() {
        ResumenEstadistico resumen = CalculadoraDescriptiva.resumir(List.of(7.5));

        assertEquals(0.0, resumen.desviacionEstandar(), DELTA);
        assertEquals(0.0, resumen.coeficienteVariacion(), DELTA);
    }

    @Test
    @DisplayName("sin valores lanza excepcion")
    void sinValoresLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculadoraDescriptiva.resumir(List.of()));
    }
}
