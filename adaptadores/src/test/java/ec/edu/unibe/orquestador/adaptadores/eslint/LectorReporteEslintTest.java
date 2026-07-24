package ec.edu.unibe.orquestador.adaptadores.eslint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LectorReporteEslint")
class LectorReporteEslintTest {

    /** Fragmento con la estructura real de un reporte JSON de ESLint. */
    private static final String REPORTE = """
            [
              {
                "filePath": "/proyecto/src/index.js",
                "messages": [
                  {"ruleId": "no-unused-vars", "severity": 1, "message": "'x' esta definida pero no se usa.", "line": 10, "column": 5},
                  {"ruleId": "@typescript-eslint/no-explicit-any", "severity": 2, "message": "Evite el tipo any.", "line": 22, "column": 3}
                ]
              },
              {
                "filePath": "/proyecto/src/roto.js",
                "messages": [
                  {"ruleId": null, "severity": 2, "message": "Error de sintaxis inesperado.", "line": 1, "column": 1}
                ]
              },
              {
                "filePath": "/proyecto/src/limpio.js",
                "messages": []
              }
            ]
            """;

    @Test
    @DisplayName("lee todos los mensajes del reporte")
    void leeTodosLosMensajes() {
        var lectura = new LectorReporteEslint().leer(REPORTE);

        assertEquals(3, lectura.hallazgos().size());
    }

    @Test
    @DisplayName("cuenta solo los archivos con al menos un mensaje")
    void cuentaSoloArchivosConMensajes() {
        var lectura = new LectorReporteEslint().leer(REPORTE);

        assertEquals(2, lectura.archivosConHallazgos());
    }

    @Test
    @DisplayName("traduce severidad 1 y 2, y separa la categoria de una regla con plugin")
    void traduceSeveridadYCategoria() {
        var lectura = new LectorReporteEslint().leer(REPORTE);
        Hallazgo advertencia = lectura.hallazgos().get(0);
        Hallazgo errorPlugin = lectura.hallazgos().get(1);

        assertEquals(Severidad.MEDIA, advertencia.severidad());
        assertEquals("eslint-core", advertencia.categoria());

        assertEquals(Severidad.ALTA, errorPlugin.severidad());
        assertEquals("@typescript-eslint", errorPlugin.categoria());
    }

    @Test
    @DisplayName("un ruleId nulo se traduce como error de sintaxis")
    void ruleIdNuloEsErrorDeSintaxis() {
        var lectura = new LectorReporteEslint().leer(REPORTE);
        Hallazgo errorSintaxis = lectura.hallazgos().get(2);

        assertEquals("error-sintaxis", errorSintaxis.regla());
        assertEquals(Severidad.ALTA, errorSintaxis.severidad());
    }

    @Test
    @DisplayName("un reporte vacio no produce hallazgos")
    void reporteVacioNoProduceHallazgos() {
        var lectura = new LectorReporteEslint().leer("[]");

        assertEquals(0, lectura.hallazgos().size());
        assertEquals(0, lectura.archivosConHallazgos());
    }
}
