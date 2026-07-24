package ec.edu.unibe.orquestador.adaptadores.matriz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.Dimension;
import ec.edu.unibe.orquestador.dominio.modelo.FuenteDato;
import ec.edu.unibe.orquestador.dominio.modelo.Herramienta;
import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoAnalizado;
import ec.edu.unibe.orquestador.dominio.modelo.RegistroMedicion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ExportadorMatrizCsv")
class ExportadorMatrizCsvTest {

    private MatrizComparativa matrizDeEjemplo() {
        MatrizComparativa matriz = new MatrizComparativa();
        Herramienta pmd = new Herramienta("PMD", "7.26.0");
        ProyectoAnalizado proyecto = new ProyectoAnalizado("square/retrofit", "Java", "abc123");

        matriz.agregar(new RegistroMedicion(LocalDateTime.now(), pmd, proyecto, 1,
                Dimension.METRICAS, "Incidencias totales, con coma", 10.0, "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));
        matriz.agregar(new RegistroMedicion(LocalDateTime.now(), pmd, proyecto, 2,
                Dimension.METRICAS, "Incidencias totales", 12.0, "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));
        return matriz;
    }

    @Test
    @DisplayName("escribe el encabezado y una fila por registro")
    void escribeEncabezadoYFilas(@TempDir Path carpeta) throws Exception {
        Path destino = carpeta.resolve("matriz.csv");
        new ExportadorMatrizCsv().exportar(matrizDeEjemplo(), destino);

        List<String> lineas = Files.readAllLines(destino);

        assertEquals("fechaHora,herramienta,version,proyecto,lenguaje,commit,corrida,"
                + "dimension,indicador,valor,unidad,fuente", lineas.get(0));
        assertEquals(3, lineas.size());
    }

    @Test
    @DisplayName("encierra entre comillas un campo que contiene una coma")
    void escapaCamposConComa(@TempDir Path carpeta) throws Exception {
        Path destino = carpeta.resolve("matriz.csv");
        new ExportadorMatrizCsv().exportar(matrizDeEjemplo(), destino);

        String contenido = Files.readString(destino);

        assertTrue(contenido.contains("\"Incidencias totales, con coma\""));
    }
}
