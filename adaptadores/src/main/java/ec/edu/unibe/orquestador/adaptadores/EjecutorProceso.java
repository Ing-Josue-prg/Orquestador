package ec.edu.unibe.orquestador.adaptadores;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta programas externos y devuelve su salida, su codigo de terminacion
 * y el tiempo que tardaron.
 *
 * <p>Todas las herramientas de la muestra se invocan por linea de comandos,
 * de modo que esta clase concentra el manejo de procesos y garantiza que el
 * cronometraje se realice siempre de la misma forma. La salida estandar y la
 * de error se combinan para no perder mensajes ni bloquear el proceso.</p>
 */
public final class EjecutorProceso {

    /** Resultado de ejecutar un programa externo. */
    public record Salida(int codigo, String texto, Duration duracion) {

        public boolean exitoso() {
            return codigo == 0;
        }
    }

    private EjecutorProceso() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Ejecuta el comando indicado y espera a que termine.
     *
     * @param comando       programa y argumentos
     * @param tiempoMaximo  tiempo maximo de espera
     * @return el codigo de terminacion, la salida combinada y la duracion
     * @throws IOException          si el programa no puede iniciarse
     * @throws InterruptedException si la espera se interrumpe
     */
    public static Salida ejecutar(List<String> comando, Duration tiempoMaximo)
            throws IOException, InterruptedException {
        return ejecutar(comando, tiempoMaximo, null);
    }

    /**
     * Igual que {@link #ejecutar(List, Duration)}, pero corriendo el proceso
     * con el directorio de trabajo indicado. Lo necesitan las herramientas
     * que resuelven su configuracion en base al directorio actual (ESLint y
     * npm), a diferencia de PMD o Checkstyle, que reciben la carpeta del
     * proyecto como argumento explicito.
     *
     * @param directorio directorio de trabajo del proceso, o {@code null}
     *                    para heredar el del proceso actual
     */
    public static Salida ejecutar(List<String> comando, Duration tiempoMaximo, Path directorio)
            throws IOException, InterruptedException {

        ProcessBuilder constructor = new ProcessBuilder(comando);
        constructor.redirectErrorStream(true);
        if (directorio != null) {
            constructor.directory(directorio.toFile());
        }

        long inicio = System.nanoTime();
        Process proceso = constructor.start();

        String texto;
        try (var flujo = proceso.getInputStream()) {
            texto = new String(flujo.readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean termino = proceso.waitFor(tiempoMaximo.toMillis(), TimeUnit.MILLISECONDS);
        long fin = System.nanoTime();

        if (!termino) {
            proceso.destroyForcibly();
            throw new IOException("El proceso supero el tiempo maximo de espera.");
        }

        return new Salida(proceso.exitValue(), texto, Duration.ofNanos(fin - inicio));
    }

    /**
     * Arma la lista de comando para invocar un ejecutable que en Windows
     * suele ser un script {@code .cmd}/{@code .bat} (por ejemplo
     * {@code npm}, {@code eslint} o {@code sonar-scanner}). A diferencia de
     * un ejecutable nativo (como {@code git}), {@link ProcessBuilder} no
     * resuelve esos scripts por si solo cuando se los nombra sin ruta
     * completa; invocarlos a traves de {@code cmd /c} reproduce la misma
     * resolucion que usaria una terminal interactiva. En otros sistemas
     * operativos se invoca el ejecutable tal cual.
     */
    public static List<String> resolverComando(String ejecutable, String... argumentos) {
        List<String> partes = new ArrayList<>();
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            partes.add("cmd");
            partes.add("/c");
        }
        partes.add(ejecutable);
        partes.addAll(List.of(argumentos));
        return partes;
    }
}
