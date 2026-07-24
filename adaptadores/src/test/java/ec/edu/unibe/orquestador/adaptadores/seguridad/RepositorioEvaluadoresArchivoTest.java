package ec.edu.unibe.orquestador.adaptadores.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.modelo.Evaluador;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RepositorioEvaluadoresArchivo")
class RepositorioEvaluadoresArchivoTest {

    @Test
    @DisplayName("un evaluador guardado se puede volver a encontrar")
    void guardaYEncuentraUnEvaluador(@TempDir Path carpeta) throws Exception {
        var repositorio = new RepositorioEvaluadoresArchivo(carpeta.resolve("evaluadores.dat"));
        Evaluador evaluador = new Evaluador("ana", "hash123", "sal123", LocalDateTime.now());

        repositorio.guardar(evaluador);

        var encontrado = repositorio.buscarPorNombreUsuario("ana");
        assertTrue(encontrado.isPresent());
        assertEquals("hash123", encontrado.get().hashContrasena());
        assertEquals("sal123", encontrado.get().sal());
    }

    @Test
    @DisplayName("la busqueda no distingue mayusculas")
    void busquedaNoDistingueMayusculas(@TempDir Path carpeta) throws Exception {
        var repositorio = new RepositorioEvaluadoresArchivo(carpeta.resolve("evaluadores.dat"));
        repositorio.guardar(new Evaluador("Ana", "hash123", "sal123", LocalDateTime.now()));

        assertTrue(repositorio.buscarPorNombreUsuario("ana").isPresent());
        assertTrue(repositorio.existe("ANA"));
    }

    @Test
    @DisplayName("un usuario que no existe no se encuentra")
    void usuarioInexistenteNoSeEncuentra(@TempDir Path carpeta) throws Exception {
        var repositorio = new RepositorioEvaluadoresArchivo(carpeta.resolve("evaluadores.dat"));

        assertFalse(repositorio.existe("nadie"));
        assertTrue(repositorio.buscarPorNombreUsuario("nadie").isEmpty());
    }

    @Test
    @DisplayName("guarda varios evaluadores en el mismo archivo")
    void guardaVariosEvaluadores(@TempDir Path carpeta) throws Exception {
        var repositorio = new RepositorioEvaluadoresArchivo(carpeta.resolve("evaluadores.dat"));
        repositorio.guardar(new Evaluador("ana", "hashA", "salA", LocalDateTime.now()));
        repositorio.guardar(new Evaluador("beto", "hashB", "salB", LocalDateTime.now()));

        assertTrue(repositorio.existe("ana"));
        assertTrue(repositorio.existe("beto"));
    }
}
