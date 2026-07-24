package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CalculadoraConcordancia")
class CalculadoraConcordanciaTest {

    private static Hallazgo hallazgo(String archivo, int linea) {
        return new Hallazgo("regla-x", "categoria", Severidad.MEDIA, archivo, linea, "mensaje");
    }

    @Test
    @DisplayName("detecta una ubicacion en comun y los hallazgos exclusivos de cada herramienta")
    void detectaComunesYExclusivos() {
        Map<HerramientaSoportada, List<Hallazgo>> hallazgos = Map.of(
                HerramientaSoportada.PMD, List.of(
                        hallazgo("Cuenta.java", 10),
                        hallazgo("Cuenta.java", 20)),
                HerramientaSoportada.CHECKSTYLE, List.of(
                        hallazgo("Cuenta.java", 10),
                        hallazgo("Util.java", 5)));

        ResumenConcordancia resumen = CalculadoraConcordancia.calcular(hallazgos);

        assertEquals(3, resumen.totalUbicaciones());
        assertEquals(1, resumen.ubicacionesEnComun());
        assertEquals(1, resumen.hallazgosExclusivos().get(HerramientaSoportada.PMD));
        assertEquals(1, resumen.hallazgosExclusivos().get(HerramientaSoportada.CHECKSTYLE));
    }

    @Test
    @DisplayName("sin hallazgos, no hay ubicaciones ni exclusivos")
    void sinHallazgosNoHayUbicaciones() {
        Map<HerramientaSoportada, List<Hallazgo>> hallazgos = Map.of(
                HerramientaSoportada.PMD, List.of(),
                HerramientaSoportada.CHECKSTYLE, List.of());

        ResumenConcordancia resumen = CalculadoraConcordancia.calcular(hallazgos);

        assertEquals(0, resumen.totalUbicaciones());
        assertEquals(0, resumen.ubicacionesEnComun());
        assertEquals(0, resumen.hallazgosExclusivos().get(HerramientaSoportada.PMD));
    }

    @Test
    @DisplayName("una ubicacion senalada por las tres herramientas cuenta como una sola coincidencia")
    void ubicacionEnTresHerramientas() {
        Map<HerramientaSoportada, List<Hallazgo>> hallazgos = Map.of(
                HerramientaSoportada.PMD, List.of(hallazgo("Cuenta.java", 10)),
                HerramientaSoportada.CHECKSTYLE, List.of(hallazgo("Cuenta.java", 10)),
                HerramientaSoportada.ESLINT, List.of(hallazgo("Cuenta.java", 10)));

        ResumenConcordancia resumen = CalculadoraConcordancia.calcular(hallazgos);

        assertEquals(1, resumen.totalUbicaciones());
        assertEquals(1, resumen.ubicacionesEnComun());
    }

    @Test
    @DisplayName("rechaza un mapa nulo")
    void rechazaMapaNulo() {
        assertThrows(IllegalArgumentException.class, () -> CalculadoraConcordancia.calcular(null));
    }
}
