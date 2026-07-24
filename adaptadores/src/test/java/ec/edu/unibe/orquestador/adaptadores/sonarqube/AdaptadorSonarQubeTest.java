package ec.edu.unibe.orquestador.adaptadores.sonarqube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Solo prueba la validacion del constructor: ejecutar SonarQube de verdad
 * requiere Docker y un servidor real, igual que los demas adaptadores no
 * tienen prueba de integracion para su ejecucion de proceso externo.
 */
@DisplayName("AdaptadorSonarQube")
class AdaptadorSonarQubeTest {

    private static final String SCANNER = "sonar-scanner";
    private static final String DOCKER = "docker";
    private static final String URL = "http://localhost:9000";
    private static final String TOKEN = "un-token";

    @Test
    @DisplayName("rechaza una ruta de scanner en blanco")
    void rechazaScannerEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptadorSonarQube(" ", DOCKER, URL, TOKEN));
    }

    @Test
    @DisplayName("rechaza una ruta de docker en blanco")
    void rechazaDockerEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptadorSonarQube(SCANNER, " ", URL, TOKEN));
    }

    @Test
    @DisplayName("rechaza una url de servidor en blanco")
    void rechazaUrlEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptadorSonarQube(SCANNER, DOCKER, " ", TOKEN));
    }

    @Test
    @DisplayName("rechaza un token en blanco")
    void rechazaTokenEnBlanco() {
        assertThrows(IllegalArgumentException.class,
                () -> new AdaptadorSonarQube(SCANNER, DOCKER, URL, " "));
    }

    @Test
    @DisplayName("se identifica como la herramienta SonarQube")
    void seIdentificaComoSonarQube() {
        var adaptador = new AdaptadorSonarQube(SCANNER, DOCKER, URL, TOKEN);

        assertEquals(HerramientaSoportada.SONARQUBE, adaptador.herramienta());
    }
}
