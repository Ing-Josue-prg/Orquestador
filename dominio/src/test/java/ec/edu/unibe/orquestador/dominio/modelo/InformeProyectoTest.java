package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InformeProyecto")
class InformeProyectoTest {

    private static Hallazgo hallazgo(String regla, Severidad severidad, String archivo) {
        return new Hallazgo(regla, "Design", severidad, "C:\\p\\" + archivo, 10, "mensaje");
    }

    private static InformeProyecto informeDePrueba() {
        ProyectoMuestra proyecto = CatalogoMuestra.porId("guava").orElseThrow();
        List<Hallazgo> hallazgos = List.of(
                hallazgo("ReglaA", Severidad.ALTA, "Uno.java"),
                hallazgo("ReglaA", Severidad.MEDIA, "Uno.java"),
                hallazgo("ReglaA", Severidad.BAJA, "Dos.java"),
                hallazgo("ReglaB", Severidad.ALTA, "Dos.java"));

        return new InformeProyecto(proyecto, new Herramienta("PMD", "7.0.0"),
                LocalDateTime.now(), hallazgos, 2);
    }

    @Test
    @DisplayName("cuenta los hallazgos por severidad")
    void cuentaPorSeveridad() {
        InformeProyecto informe = informeDePrueba();

        assertEquals(4, informe.total());
        assertEquals(2, informe.conteoPorSeveridad(Severidad.ALTA));
        assertEquals(1, informe.conteoPorSeveridad(Severidad.MEDIA));
        assertEquals(1, informe.conteoPorSeveridad(Severidad.BAJA));
    }

    @Test
    @DisplayName("ordena las reglas mas frecuentes de mayor a menor")
    void ordenaLasReglasMasFrecuentes() {
        var reglas = informeDePrueba().reglasMasFrecuentes(5);

        assertEquals(List.of("ReglaA", "ReglaB"), List.copyOf(reglas.keySet()));
        assertEquals(3L, reglas.get("ReglaA"));
        assertEquals(1L, reglas.get("ReglaB"));
    }

    @Test
    @DisplayName("identifica los archivos mas afectados")
    void identificaLosArchivosMasAfectados() {
        var archivos = informeDePrueba().archivosMasAfectados(5);

        assertEquals(2, archivos.size());
        assertEquals(2L, archivos.get("Uno.java"));
        assertEquals(2L, archivos.get("Dos.java"));
    }
}
