package ec.edu.unibe.orquestador.adaptadores.persistencia;

import ec.edu.unibe.orquestador.dominio.modelo.ConfiguracionEntorno;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Guarda y recupera la {@link ConfiguracionEntorno} en un archivo de
 * propiedades local, para que las rutas de las herramientas no se pierdan
 * cada vez que se cierra la aplicacion.
 *
 * <p>La carpeta de cada proyecto descargado no se guarda aqui: se
 * reconstruye sola en cada arranque comprobando contra el disco si el
 * proyecto ya esta clonado (ver {@code AdaptadorGit.estaDescargado}). El
 * token de SonarQube queda en texto plano en este archivo: es una
 * simplificacion deliberada para una aplicacion de escritorio de un solo
 * usuario; un uso con varios usuarios exigiria un almacen de credenciales
 * del sistema operativo, fuera del alcance de esta investigacion.</p>
 */
public final class ConfiguracionEntornoArchivo {

    private static final Path ARCHIVO_POR_DEFECTO =
            Path.of(System.getProperty("user.home"), ".orquestador-calidad", "configuracion.properties");

    private ConfiguracionEntornoArchivo() {
        // Clase de utilidad: no se instancia.
    }

    public static Path archivoPorDefecto() {
        return ARCHIVO_POR_DEFECTO;
    }

    /** Guarda el estado actual de la configuracion en el archivo por defecto. */
    public static void guardar(ConfiguracionEntorno configuracion) {
        guardar(configuracion, ARCHIVO_POR_DEFECTO);
    }

    /** Guarda el estado actual de la configuracion en el archivo indicado. */
    public static void guardar(ConfiguracionEntorno configuracion, Path archivo) {
        Properties propiedades = new Properties();
        for (HerramientaSoportada herramienta : HerramientaSoportada.values()) {
            configuracion.ruta(herramienta).ifPresent(
                    ruta -> propiedades.setProperty("ruta." + herramienta.name(), ruta));
        }
        propiedades.setProperty("rutaGit", configuracion.rutaGit());
        propiedades.setProperty("rutaNpm", configuracion.rutaNpm());
        propiedades.setProperty("rutaDocker", configuracion.rutaDocker());
        propiedades.setProperty("sonarUrl", configuracion.urlSonarQube());
        propiedades.setProperty("sonarToken", configuracion.tokenSonarQube());

        try {
            if (archivo.getParent() != null) {
                Files.createDirectories(archivo.getParent());
            }
            try (OutputStream salida = Files.newOutputStream(archivo)) {
                propiedades.store(salida, "Configuracion del Orquestador de Evaluacion de Calidad");
            }
        } catch (IOException e) {
            // Si no se puede guardar, la sesion actual sigue funcionando en
            // memoria; solo se pierde la persistencia entre sesiones.
        }
    }

    /** Carga en el objeto recibido la configuracion guardada en el archivo por defecto, si existe. */
    public static void cargarEn(ConfiguracionEntorno configuracion) {
        cargarEn(configuracion, ARCHIVO_POR_DEFECTO);
    }

    /** Carga en el objeto recibido la configuracion guardada en el archivo indicado, si existe. */
    public static void cargarEn(ConfiguracionEntorno configuracion, Path archivo) {
        if (!Files.isRegularFile(archivo)) {
            return;
        }
        Properties propiedades = new Properties();
        try (InputStream entrada = Files.newInputStream(archivo)) {
            propiedades.load(entrada);
        } catch (IOException e) {
            return;
        }

        for (HerramientaSoportada herramienta : HerramientaSoportada.values()) {
            String ruta = propiedades.getProperty("ruta." + herramienta.name());
            if (ruta != null && !ruta.isBlank()) {
                configuracion.definirRuta(herramienta, ruta);
            }
        }
        configuracion.definirRutaGit(propiedades.getProperty("rutaGit", ""));
        configuracion.definirRutaNpm(propiedades.getProperty("rutaNpm", ""));
        configuracion.definirRutaDocker(propiedades.getProperty("rutaDocker", ""));

        String sonarUrl = propiedades.getProperty("sonarUrl");
        if (sonarUrl != null && !sonarUrl.isBlank()) {
            configuracion.definirUrlSonarQube(sonarUrl);
        }
        String sonarToken = propiedades.getProperty("sonarToken");
        if (sonarToken != null) {
            configuracion.definirTokenSonarQube(sonarToken);
        }
    }
}
