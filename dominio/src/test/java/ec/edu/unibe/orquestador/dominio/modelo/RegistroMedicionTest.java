package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RegistroMedicion")
class RegistroMedicionTest {

    private static Herramienta herramientaValida() {
        return new Herramienta("SonarQube", "26.7");
    }

    private static ProyectoAnalizado proyectoValido() {
        return new ProyectoAnalizado("guava", "Java", "a1b2c3d");
    }

    @Test
    @DisplayName("con datos validos asigna todos los campos")
    void conDatosValidosAsignaTodosLosCampos() {
        LocalDateTime fecha = LocalDateTime.of(2026, 8, 1, 10, 30);

        RegistroMedicion registro = new RegistroMedicion(
                fecha, herramientaValida(), proyectoValido(), 1,
                Dimension.METRICAS, "Complejidad ciclomatica", 1250, "conteo",
                FuenteDato.WEB_API_SONARQUBE);

        assertEquals(fecha, registro.fechaHora());
        assertEquals("SonarQube", registro.herramienta().nombre());
        assertEquals("guava", registro.proyecto().nombre());
        assertEquals(1, registro.numeroCorrida());
        assertEquals(Dimension.METRICAS, registro.dimension());
        assertEquals("Complejidad ciclomatica", registro.indicador());
        assertEquals(1250, registro.valor());
        assertEquals("conteo", registro.unidad());
        assertEquals(FuenteDato.WEB_API_SONARQUBE, registro.fuente());
    }

    @Test
    @DisplayName("con numero de corrida cero lanza excepcion")
    void conCorridaCeroLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new RegistroMedicion(
                LocalDateTime.now(), herramientaValida(), proyectoValido(), 0,
                Dimension.DESEMPENO, "Tiempo", 5.2, "segundos",
                FuenteDato.MEDICION_EXTERNA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("con indicador vacio lanza excepcion")
    void conIndicadorVacioLanzaExcepcion(String indicador) {
        assertThrows(IllegalArgumentException.class, () -> new RegistroMedicion(
                LocalDateTime.now(), herramientaValida(), proyectoValido(), 1,
                Dimension.METRICAS, indicador, 10, "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));
    }

    @Test
    @DisplayName("con valor no finito lanza excepcion")
    void conValorNoFinitoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new RegistroMedicion(
                LocalDateTime.now(), herramientaValida(), proyectoValido(), 1,
                Dimension.DESEMPENO, "Tiempo", Double.NaN, "segundos",
                FuenteDato.MEDICION_EXTERNA));
    }
}
