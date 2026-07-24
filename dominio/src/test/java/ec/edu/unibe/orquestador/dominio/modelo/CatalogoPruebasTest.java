package ec.edu.unibe.orquestador.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogoPruebas")
class CatalogoPruebasTest {

    @Test
    @DisplayName("tiene proyectos de prueba, todos marcados como tal")
    void tieneProyectosMarcadosComoPrueba() {
        var proyectos = CatalogoPruebas.proyectos();

        assertEquals(3, proyectos.size());
        assertTrue(proyectos.stream().allMatch(p -> p.nombre().startsWith("[prueba]")));
    }

    @Test
    @DisplayName("el proyecto poliglota aplica a las 4 herramientas")
    void proyectoPoliglotaAplicaALasCuatroHerramientas() {
        ProyectoMuestra poliglota = CatalogoPruebas.proyectos().stream()
                .filter(p -> p.id().equals("prueba-poliglota"))
                .findFirst().orElseThrow();

        var herramientas = CatalogoMuestra.herramientasPara(poliglota);

        assertEquals(4, herramientas.size());
    }

    @Test
    @DisplayName("no comparte identificadores con la muestra oficial de la investigacion")
    void noComparteIdentificadoresConLaMuestra() {
        Set<String> idsPrueba = CatalogoPruebas.proyectos().stream()
                .map(ProyectoMuestra::id).collect(Collectors.toSet());
        Set<String> idsMuestra = CatalogoMuestra.proyectos().stream()
                .map(ProyectoMuestra::id).collect(Collectors.toSet());

        assertTrue(idsPrueba.stream().noneMatch(idsMuestra::contains));
    }
}
