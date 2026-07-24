package ec.edu.unibe.orquestador.adaptadores.checkstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LectorReporteCheckstyle")
class LectorReporteCheckstyleTest {

    /** Fragmento con la estructura real de un reporte XML de Checkstyle. */
    private static final String REPORTE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <checkstyle version="10.17.0">
              <file name="C:\\proyecto\\src\\Cuenta.java">
                <error line="12" column="5" severity="error"
                       message="El nombre del miembro no sigue la convencion."
                       source="com.puppycrawl.tools.checkstyle.checks.naming.MemberNameCheck"/>
                <error line="40" severity="warning"
                       message="Falta un comentario Javadoc."
                       source="com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocMethodCheck"/>
              </file>
              <file name="C:\\proyecto\\src\\Util.java"/>
              <file name="C:\\proyecto\\src\\Vacio.java">
                <error line="1" severity="info"
                       message="Sugerencia de estilo."
                       source="com.puppycrawl.tools.checkstyle.checks.SomeCheck"/>
              </file>
            </checkstyle>
            """;

    @Test
    @DisplayName("lee todos los errores del reporte")
    void leeTodosLosErrores() throws Exception {
        var lectura = new LectorReporteCheckstyle().leer(REPORTE);

        assertEquals(3, lectura.hallazgos().size());
    }

    @Test
    @DisplayName("cuenta solo los archivos con al menos un error")
    void cuentaSoloArchivosConErrores() throws Exception {
        var lectura = new LectorReporteCheckstyle().leer(REPORTE);

        assertEquals(2, lectura.archivosConHallazgos());
    }

    @Test
    @DisplayName("traduce severidad y separa regla/categoria del atributo source")
    void traduceSeveridadYFuente() throws Exception {
        var lectura = new LectorReporteCheckstyle().leer(REPORTE);
        Hallazgo primero = lectura.hallazgos().get(0);

        assertEquals(Severidad.ALTA, primero.severidad());
        assertEquals("MemberNameCheck", primero.regla());
        assertEquals("naming", primero.categoria());
        assertEquals(12, primero.linea());
        assertTrue(primero.mensaje().contains("convencion"));
    }

    @Test
    @DisplayName("un reporte sin archivos no produce hallazgos")
    void reporteVacioNoProduceHallazgos() throws Exception {
        var lectura = new LectorReporteCheckstyle().leer(
                "<?xml version=\"1.0\"?><checkstyle version=\"10.17.0\"></checkstyle>");

        assertEquals(0, lectura.hallazgos().size());
        assertEquals(0, lectura.archivosConHallazgos());
    }
}
