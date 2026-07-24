package ec.edu.unibe.orquestador.adaptadores.seguridad;

import ec.edu.unibe.orquestador.dominio.contratos.RepositorioEvaluadores;
import ec.edu.unibe.orquestador.dominio.modelo.Evaluador;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Guarda los evaluadores registrados en un archivo de texto local, una
 * linea por evaluador con el formato
 * {@code nombreUsuario;sal;hashContrasena;fechaRegistro}.
 *
 * <p>Es deliberadamente simple: no es una base de datos ni un almacen de
 * credenciales del sistema operativo, porque esta aplicacion corre en el
 * equipo de un investigador para un numero pequeno de evaluadores.</p>
 */
public final class RepositorioEvaluadoresArchivo implements RepositorioEvaluadores {

    private static final String SEPARADOR = ";";

    private final Path archivo;

    public RepositorioEvaluadoresArchivo(Path archivo) {
        if (archivo == null) {
            throw new IllegalArgumentException("El archivo de evaluadores es obligatorio.");
        }
        this.archivo = archivo;
    }

    /** Carpeta y archivo por defecto donde se guardan los evaluadores registrados. */
    public static Path archivoPorDefecto() {
        return Path.of(System.getProperty("user.home"), ".orquestador-calidad", "evaluadores.dat");
    }

    @Override
    public Optional<Evaluador> buscarPorNombreUsuario(String nombreUsuario) throws IOException {
        return leerTodos().stream()
                .filter(e -> e.nombreUsuario().equalsIgnoreCase(nombreUsuario))
                .findFirst();
    }

    @Override
    public boolean existe(String nombreUsuario) throws IOException {
        return buscarPorNombreUsuario(nombreUsuario).isPresent();
    }

    @Override
    public void guardar(Evaluador evaluador) throws IOException {
        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        String linea = String.join(SEPARADOR,
                evaluador.nombreUsuario(), evaluador.sal(), evaluador.hashContrasena(),
                evaluador.fechaRegistro().toString()) + System.lineSeparator();
        Files.writeString(archivo, linea, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private List<Evaluador> leerTodos() throws IOException {
        if (!Files.isRegularFile(archivo)) {
            return List.of();
        }
        return Files.readAllLines(archivo).stream()
                .filter(linea -> !linea.isBlank())
                .map(this::interpretar)
                .toList();
    }

    private Evaluador interpretar(String linea) {
        String[] partes = linea.split(SEPARADOR, 4);
        return new Evaluador(partes[0], partes[2], partes[1], LocalDateTime.parse(partes[3]));
    }
}
