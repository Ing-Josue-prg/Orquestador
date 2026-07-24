package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogoMuestra")
class CatalogoMuestraTest {

    @Test
    @DisplayName("contiene los cuatro proyectos del banco de pruebas")
    void contieneLosCuatroProyectos() {
        assertEquals(4, CatalogoMuestra.proyectos().size());
    }

    @Test
    @DisplayName("a un proyecto Java le aplican SonarQube, PMD y Checkstyle")
    void herramientasParaProyectoJava() {
        ProyectoMuestra guava = CatalogoMuestra.porId("guava").orElseThrow();

        var herramientas = CatalogoMuestra.herramientasPara(guava);

        assertEquals(3, herramientas.size());
        assertTrue(herramientas.contains(HerramientaSoportada.PMD));
        assertTrue(herramientas.contains(HerramientaSoportada.CHECKSTYLE));
        assertFalse(herramientas.contains(HerramientaSoportada.ESLINT));
    }

    @Test
    @DisplayName("a un proyecto JavaScript le aplican SonarQube y ESLint")
    void herramientasParaProyectoJavaScript() {
        ProyectoMuestra axios = CatalogoMuestra.porId("axios").orElseThrow();

        var herramientas = CatalogoMuestra.herramientasPara(axios);

        assertEquals(2, herramientas.size());
        assertTrue(herramientas.contains(HerramientaSoportada.ESLINT));
        assertFalse(herramientas.contains(HerramientaSoportada.PMD));
    }
}
