package ec.edu.unibe.orquestador.dominio.modelo;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rutas y credenciales que el orquestador necesita para invocar a las
 * herramientas. Se mantiene en un solo lugar para que la verificacion del
 * entorno y las corridas trabajen siempre con los mismos valores.
 */
public final class ConfiguracionEntorno {

    private final Map<HerramientaSoportada, String> rutas = new HashMap<>();
    private final Map<String, Path> carpetasProyecto = new HashMap<>();
    private String urlSonarQube = "http://localhost:9000";
    private String tokenSonarQube = "";
    private String rutaGit = "git";
    private String rutaNpm = "npm";
    private String rutaDocker = "docker";

    /** Ruta del ejecutable o del archivo jar de la herramienta. */
    public Optional<String> ruta(HerramientaSoportada herramienta) {
        String valor = rutas.get(herramienta);
        return (valor == null || valor.isBlank()) ? Optional.empty() : Optional.of(valor);
    }

    public void definirRuta(HerramientaSoportada herramienta, String ruta) {
        rutas.put(herramienta, ruta);
    }

    /** Carpeta local donde esta clonado el proyecto de la muestra. */
    public Optional<Path> carpetaProyecto(String idProyecto) {
        return Optional.ofNullable(carpetasProyecto.get(idProyecto));
    }

    public void definirCarpetaProyecto(String idProyecto, Path carpeta) {
        carpetasProyecto.put(idProyecto, carpeta);
    }

    public String urlSonarQube() {
        return urlSonarQube;
    }

    public void definirUrlSonarQube(String url) {
        this.urlSonarQube = url;
    }

    public String tokenSonarQube() {
        return tokenSonarQube;
    }

    public void definirTokenSonarQube(String token) {
        this.tokenSonarQube = token;
    }

    /** Ruta del ejecutable de git; por defecto asume que esta en el PATH. */
    public String rutaGit() {
        return rutaGit;
    }

    public void definirRutaGit(String ruta) {
        if (ruta != null && !ruta.isBlank()) {
            this.rutaGit = ruta.trim();
        }
    }

    /** Ruta del ejecutable de npm, usado por ESLint para preparar el proyecto antes de analizarlo. */
    public String rutaNpm() {
        return rutaNpm;
    }

    public void definirRutaNpm(String ruta) {
        if (ruta != null && !ruta.isBlank()) {
            this.rutaNpm = ruta.trim();
        }
    }

    /** Ruta del ejecutable de docker, usado por SonarQube para levantar su contenedor. */
    public String rutaDocker() {
        return rutaDocker;
    }

    public void definirRutaDocker(String ruta) {
        if (ruta != null && !ruta.isBlank()) {
            this.rutaDocker = ruta.trim();
        }
    }
}
