package ec.edu.unibe.orquestador.adaptadores.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.ConfiguracionEntorno;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ConfiguracionEntornoArchivo")
class ConfiguracionEntornoArchivoTest {

    @Test
    @DisplayName("guarda y vuelve a cargar las rutas de las herramientas")
    void guardaYCargaLasRutas(@TempDir Path carpeta) {
        Path archivo = carpeta.resolve("configuracion.properties");

        ConfiguracionEntorno original = new ConfiguracionEntorno();
        original.definirRuta(HerramientaSoportada.PMD, "C:\\herramientas\\pmd\\bin\\pmd.bat");
        original.definirRutaGit("C:\\herramientas\\git.exe");
        original.definirUrlSonarQube("http://localhost:9001");
        original.definirTokenSonarQube("token-secreto");
        ConfiguracionEntornoArchivo.guardar(original, archivo);

        ConfiguracionEntorno cargada = new ConfiguracionEntorno();
        ConfiguracionEntornoArchivo.cargarEn(cargada, archivo);

        assertEquals("C:\\herramientas\\pmd\\bin\\pmd.bat", cargada.ruta(HerramientaSoportada.PMD).orElseThrow());
        assertEquals("C:\\herramientas\\git.exe", cargada.rutaGit());
        assertEquals("http://localhost:9001", cargada.urlSonarQube());
        assertEquals("token-secreto", cargada.tokenSonarQube());
    }

    @Test
    @DisplayName("si el archivo no existe, no cambia nada en la configuracion")
    void archivoInexistenteNoCambiaNada(@TempDir Path carpeta) {
        ConfiguracionEntorno configuracion = new ConfiguracionEntorno();

        ConfiguracionEntornoArchivo.cargarEn(configuracion, carpeta.resolve("no-existe.properties"));

        assertTrue(configuracion.ruta(HerramientaSoportada.PMD).isEmpty());
        assertEquals("git", configuracion.rutaGit());
    }
}
