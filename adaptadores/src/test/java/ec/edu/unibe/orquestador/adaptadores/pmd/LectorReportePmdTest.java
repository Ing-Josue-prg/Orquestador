package ec.edu.unibe.orquestador.adaptadores.pmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LectorReportePmd")
class LectorReportePmdTest {

    /** Fragmento con la estructura real de un reporte XML de PMD. */
    private static final String REPORTE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pmd version="7.0.0" timestamp="2026-08-01T10:00:00">
              <file name="C:\\proyecto\\src\\Cuenta.java">
                <violation beginline="12" endline="14" rule="UnusedPrivateField"
                           ruleset="Best Practices" priority="2">
                  Se detecto un campo privado sin uso.
                </violation>
                <violation beginline="40" endline="52" rule="CyclomaticComplexity"
                           ruleset="Design" priority="3">
                  El metodo presenta una complejidad elevada.
                </violation>
              </file>
              <file name="C:\\proyecto\\src\\Util.java">
                <violation beginline="7" endline="7" rule="ShortVariable"
                           ruleset="Code Style" priority="5">
                  El nombre de la variable es demasiado corto.
                </violation>
              </file>
            </pmd>
            """;

    @Test
    @DisplayName("lee todas las violaciones del reporte")
    void leeTodasLasViolaciones() throws Exception {
        var lectura = new LectorReportePmd().leer(REPORTE);

        assertEquals(3, lectura.hallazgos().size());
        assertEquals(2, lectura.archivosConHallazgos());
    }

    @Test
    @DisplayName("traduce la prioridad de PMD a la escala de severidad del dominio")
    void traduceLaPrioridadASeveridad() throws Exception {
        var hallazgos = new LectorReportePmd().leer(REPORTE).hallazgos();

        assertEquals(Severidad.ALTA, hallazgos.get(0).severidad());   // prioridad 2
        assertEquals(Severidad.MEDIA, hallazgos.get(1).severidad());  // prioridad 3
        assertEquals(Severidad.BAJA, hallazgos.get(2).severidad());   // prioridad 5
    }

    @Test
    @DisplayName("conserva regla, categoria, archivo y linea de cada hallazgo")
    void conservaLosDatosDeCadaHallazgo() throws Exception {
        Hallazgo primero = new LectorReportePmd().leer(REPORTE).hallazgos().get(0);

        assertEquals("UnusedPrivateField", primero.regla());
        assertEquals("Best Practices", primero.categoria());
        assertEquals("Cuenta.java", primero.nombreArchivo());
        assertEquals(12, primero.linea());
        assertTrue(primero.mensaje().contains("campo privado sin uso"));
    }

    @Test
    @DisplayName("un reporte sin violaciones no produce hallazgos")
    void reporteVacioNoProduceHallazgos() throws Exception {
        String vacio = "<?xml version=\"1.0\"?><pmd version=\"7.0.0\"></pmd>";

        var lectura = new LectorReportePmd().leer(vacio);

        assertEquals(0, lectura.hallazgos().size());
    }
}
