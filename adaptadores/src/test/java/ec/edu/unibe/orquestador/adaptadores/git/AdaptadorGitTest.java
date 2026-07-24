package ec.edu.unibe.orquestador.adaptadores.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prueba solo la logica que no exige invocar al binario real de git (calculo
 * de rutas y deteccion de una copia local), igual que {@code AdaptadorPmd}
 * no tiene prueba de integracion para su ejecucion de proceso externo.
 */
@DisplayName("AdaptadorGit")
class AdaptadorGitTest {

    private static final ProyectoMuestra RETROFIT = new ProyectoMuestra(
            "retrofit", "square/retrofit", "https://github.com/square/retrofit.git",
            "Java", "Apache-2.0");

    @Test
    @DisplayName("rechaza una ruta de git en blanco")
    void rechazaRutaGitEnBlanco(@TempDir Path carpetaBase) {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorGit(" ", carpetaBase));
    }

    @Test
    @DisplayName("rechaza una carpeta base nula")
    void rechazaCarpetaBaseNula() {
        assertThrows(IllegalArgumentException.class, () -> new AdaptadorGit("git", null));
    }

    @Test
    @DisplayName("calcula la carpeta local a partir del identificador del proyecto")
    void calculaCarpetaLocal(@TempDir Path carpetaBase) {
        AdaptadorGit adaptador = new AdaptadorGit("git", carpetaBase);

        assertEquals(carpetaBase.resolve("retrofit"), adaptador.carpetaLocal(RETROFIT));
    }

    @Test
    @DisplayName("no considera descargado un proyecto sin carpeta local")
    void noDescargadoSinCarpeta(@TempDir Path carpetaBase) {
        AdaptadorGit adaptador = new AdaptadorGit("git", carpetaBase);

        assertFalse(adaptador.estaDescargado(RETROFIT));
    }

    @Test
    @DisplayName("considera descargado un proyecto con carpeta .git local")
    void descargadoConCarpetaGit(@TempDir Path carpetaBase) throws Exception {
        AdaptadorGit adaptador = new AdaptadorGit("git", carpetaBase);
        Files.createDirectories(adaptador.carpetaLocal(RETROFIT).resolve(".git"));

        assertTrue(adaptador.estaDescargado(RETROFIT));
    }
}
