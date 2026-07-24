package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatrizComparativa")
class MatrizComparativaTest {

    private static RegistroMedicion registro(String herramienta, String proyecto,
                                             int corrida, String indicador, double valor) {
        return new RegistroMedicion(
                LocalDateTime.now(),
                new Herramienta(herramienta, "1.0"),
                new ProyectoAnalizado(proyecto, "Java", "abc123"),
                corrida, Dimension.DESEMPENO, indicador, valor, "segundos",
                FuenteDato.MEDICION_EXTERNA);
    }

    @Test
    @DisplayName("al agregar un registro incrementa la cantidad")
    void alAgregarIncrementaLaCantidad() {
        MatrizComparativa matriz = new MatrizComparativa();
        matriz.agregar(registro("PMD", "guava", 1, "Tiempo", 4.0));

        assertEquals(1, matriz.cantidad());
    }

    @Test
    @DisplayName("filtrar devuelve solo las corridas del par indicado, ordenadas")
    void filtrarDevuelveSoloElParIndicadoOrdenado() {
        MatrizComparativa matriz = new MatrizComparativa();
        matriz.agregar(registro("PMD", "guava", 2, "Tiempo", 4.2));
        matriz.agregar(registro("PMD", "guava", 1, "Tiempo", 4.0));
        matriz.agregar(registro("PMD", "retrofit", 1, "Tiempo", 2.0));
        matriz.agregar(registro("ESLint", "guava", 1, "Tiempo", 9.0));

        List<RegistroMedicion> resultado = matriz.filtrar("PMD", "guava", "Tiempo");

        assertEquals(2, resultado.size());
        assertEquals(1, resultado.get(0).numeroCorrida());
        assertEquals(2, resultado.get(1).numeroCorrida());
    }

    @Test
    @DisplayName("la vista de registros no permite modificacion externa")
    void laVistaDeRegistrosEsInmutable() {
        MatrizComparativa matriz = new MatrizComparativa();
        matriz.agregar(registro("PMD", "guava", 1, "Tiempo", 4.0));

        List<RegistroMedicion> vista = matriz.registros();

        assertThrows(UnsupportedOperationException.class,
                () -> vista.add(registro("PMD", "guava", 2, "Tiempo", 4.1)));
    }
}
