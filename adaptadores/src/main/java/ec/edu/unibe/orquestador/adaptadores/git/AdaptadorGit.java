package ec.edu.unibe.orquestador.adaptadores.git;

import ec.edu.unibe.orquestador.adaptadores.EjecutorProceso;
import ec.edu.unibe.orquestador.dominio.contratos.AnalisisException;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Descarga y actualiza los proyectos del banco de pruebas con git, y detecta
 * el commit exacto que quedo analizado.
 *
 * <p>No implementa {@code AnalizadorEstatico}: no es una herramienta de
 * analisis y no produce mediciones ni hallazgos. Su unico papel es dejar el
 * codigo de un {@link ProyectoMuestra} disponible en una carpeta local, para
 * que el usuario ya no tenga que clonar a mano ni navegar carpetas.</p>
 *
 * <p>Una vez descargado un proyecto se reutiliza la misma copia local en
 * corridas posteriores, de modo que todas las herramientas de una sesion
 * analicen siempre el mismo commit. Actualizar a una version mas reciente es
 * una accion explicita y separada ({@link #actualizar}).</p>
 */
public final class AdaptadorGit {

    private static final Duration TIEMPO_MAXIMO_CLONADO = Duration.ofMinutes(15);
    private static final Duration TIEMPO_MAXIMO_CORTO = Duration.ofSeconds(30);

    /** Resultado de comprobar si git esta instalado y accesible. */
    public record Estado(boolean disponible, String detalle) {
    }

    private final String rutaGit;
    private final Path carpetaBase;

    /**
     * @param rutaGit     ruta del ejecutable de git, o simplemente "git" si
     *                    esta en el PATH del sistema
     * @param carpetaBase carpeta donde se descargan los proyectos, uno por
     *                    subcarpeta segun su identificador en el catalogo
     */
    public AdaptadorGit(String rutaGit, Path carpetaBase) {
        if (rutaGit == null || rutaGit.isBlank()) {
            throw new IllegalArgumentException("La ruta del ejecutable de git es obligatoria.");
        }
        if (carpetaBase == null) {
            throw new IllegalArgumentException("La carpeta base de descargas es obligatoria.");
        }
        this.rutaGit = rutaGit;
        this.carpetaBase = carpetaBase;
    }

    /**
     * Carpeta donde se descargan los proyectos si no se indica otra: una
     * subcarpeta fija en el directorio del usuario, para que la aplicacion
     * funcione sin pedir esta ruta en la configuracion.
     */
    public static Path carpetaBasePorDefecto() {
        return Path.of(System.getProperty("user.home"), ".orquestador-calidad", "repos");
    }

    /** Comprueba que git responda, sin depender de ningun proyecto concreto. */
    public Estado verificar() {
        try {
            var salida = EjecutorProceso.ejecutar(
                    List.of(rutaGit, "--version"), TIEMPO_MAXIMO_CORTO);

            if (salida.exitoso()) {
                return new Estado(true, salida.texto().trim());
            }
            return new Estado(false, "git respondio con codigo " + salida.codigo());

        } catch (Exception e) {
            return new Estado(false, "No se encontro git en la ruta indicada: " + e.getMessage());
        }
    }

    /** Carpeta local donde se descarga (o ya esta) el codigo del proyecto. */
    public Path carpetaLocal(ProyectoMuestra proyecto) {
        return carpetaBase.resolve(proyecto.id());
    }

    /** Indica si el proyecto ya tiene una copia local descargada. */
    public boolean estaDescargado(ProyectoMuestra proyecto) {
        return Files.isDirectory(carpetaLocal(proyecto).resolve(".git"));
    }

    /**
     * Descarga el proyecto si todavia no existe una copia local. Si ya esta
     * descargado devuelve la misma carpeta sin volver a clonar, para no
     * cambiar el commit analizado entre una herramienta y otra.
     */
    public Path descargar(ProyectoMuestra proyecto) throws AnalisisException {
        Path destino = carpetaLocal(proyecto);
        if (estaDescargado(proyecto)) {
            return destino;
        }
        return clonar(proyecto, destino);
    }

    /**
     * Vuelve a descargar el proyecto desde cero, aunque ya exista una copia
     * local. Es una accion explicita: cambia el commit que se analizara.
     */
    public Path actualizar(ProyectoMuestra proyecto) throws AnalisisException {
        return clonar(proyecto, carpetaLocal(proyecto));
    }

    /** Commit exacto ({@code HEAD}) de la copia local ya descargada. */
    public String commitActual(ProyectoMuestra proyecto) throws AnalisisException {
        Path destino = carpetaLocal(proyecto);
        try {
            List<String> comando = List.of(rutaGit, "-C", destino.toString(), "rev-parse", "HEAD");
            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO_CORTO);
            if (!salida.exitoso()) {
                throw new AnalisisException(
                        "No se pudo determinar el commit actual: " + salida.texto());
            }
            return salida.texto().trim();
        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException(
                    "Fallo al consultar el commit de " + proyecto.nombre() + ": " + e.getMessage(), e);
        }
    }

    private Path clonar(ProyectoMuestra proyecto, Path destino) throws AnalisisException {
        try {
            borrarSiExiste(destino);
            Files.createDirectories(carpetaBase);

            // "core.longpaths=true" evita que la clonacion falle en Windows con
            // "Filename too long" en proyectos con rutas de archivo profundas.
            List<String> comando = List.of(
                    rutaGit, "-c", "core.longpaths=true",
                    "clone", "--depth", "1", proyecto.url(), destino.toString());

            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO_CLONADO);
            if (!salida.exitoso()) {
                throw new AnalisisException(
                        "git clone termino con codigo " + salida.codigo() + ": " + salida.texto());
            }
            return destino;

        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException(
                    "Fallo al descargar " + proyecto.nombre() + ": " + e.getMessage(), e);
        }
    }

    private void borrarSiExiste(Path carpeta) throws IOException {
        if (!Files.exists(carpeta)) {
            return;
        }
        // git deja los objetos de ".git/objects" como solo lectura; hay que
        // quitar esa marca antes de poder borrarlos.
        try (Stream<Path> flujo = Files.walk(carpeta)) {
            for (Path ruta : flujo.sorted(Comparator.reverseOrder()).toList()) {
                ruta.toFile().setWritable(true);
                Files.delete(ruta);
            }
        }
    }
}
