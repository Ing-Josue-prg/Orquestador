package ec.edu.unibe.orquestador.adaptadores.sonarqube;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LectorReporteSonarQube")
class LectorReporteSonarQubeTest {

    /** Fragmento con la estructura real de una pagina de /api/issues/search. */
    private static final String PAGINA = """
            {
              "total": 3,
              "p": 1,
              "ps": 2,
              "paging": {"pageIndex": 1, "pageSize": 2, "total": 3},
              "issues": [
                {
                  "key": "AY1", "rule": "java:S1135", "severity": "MINOR",
                  "component": "mi-proyecto:src/main/java/Cuenta.java",
                  "line": 42, "message": "Complete la tarea pendiente."
                },
                {
                  "key": "AY2", "rule": "java:S1192", "severity": "CRITICAL",
                  "component": "mi-proyecto:src/main/java/Util.java",
                  "line": 7, "message": "Cadena literal duplicada."
                }
              ]
            }
            """;

    @Test
    @DisplayName("lee los hallazgos de la pagina")
    void leeHallazgosDeLaPagina() {
        var lectura = new LectorReporteSonarQube().leer(PAGINA, "mi-proyecto");

        assertEquals(2, lectura.hallazgos().size());
    }

    @Test
    @DisplayName("informa el total real, no solo los de esta pagina")
    void informaElTotalReal() {
        var lectura = new LectorReporteSonarQube().leer(PAGINA, "mi-proyecto");

        assertEquals(3, lectura.total());
    }

    @Test
    @DisplayName("traduce severidad y recorta el prefijo del proyecto en la ruta")
    void traduceSeveridadYRecortaPrefijo() {
        var lectura = new LectorReporteSonarQube().leer(PAGINA, "mi-proyecto");
        Hallazgo primero = lectura.hallazgos().get(0);
        Hallazgo segundo = lectura.hallazgos().get(1);

        assertEquals(Severidad.BAJA, primero.severidad());
        assertEquals("src/main/java/Cuenta.java", primero.archivo());
        assertEquals("java", primero.categoria());

        assertEquals(Severidad.ALTA, segundo.severidad());
    }

    @Test
    @DisplayName("una respuesta sin hallazgos no produce hallazgos")
    void respuestaVaciaNoProduceHallazgos() {
        var lectura = new LectorReporteSonarQube().leer(
                "{\"total\": 0, \"issues\": []}", "mi-proyecto");

        assertEquals(0, lectura.hallazgos().size());
        assertEquals(0, lectura.total());
    }
}
