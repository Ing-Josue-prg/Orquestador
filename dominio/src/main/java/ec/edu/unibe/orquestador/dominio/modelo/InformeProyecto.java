package ec.edu.unibe.orquestador.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Informe de calidad de un proyecto analizado por una herramienta: el
 * conjunto de hallazgos con sus resumenes por severidad y por regla.
 *
 * <p>Este informe es el resultado visible del analisis y responde a la
 * pregunta practica de que conviene mejorar en el codigo revisado. Los datos
 * para la matriz comparativa de la investigacion se derivan aparte, a partir
 * de los conteos y del desempeno de la corrida.</p>
 */
public final class InformeProyecto {

    private final ProyectoMuestra proyecto;
    private final Herramienta herramienta;
    private final LocalDateTime fechaHora;
    private final List<Hallazgo> hallazgos;
    private final int archivosAnalizados;

    public InformeProyecto(ProyectoMuestra proyecto, Herramienta herramienta,
                           LocalDateTime fechaHora, List<Hallazgo> hallazgos,
                           int archivosAnalizados) {
        if (proyecto == null || herramienta == null || fechaHora == null || hallazgos == null) {
            throw new IllegalArgumentException("Todos los datos del informe son obligatorios.");
        }
        if (archivosAnalizados < 0) {
            throw new IllegalArgumentException("El numero de archivos no puede ser negativo.");
        }
        this.proyecto = proyecto;
        this.herramienta = herramienta;
        this.fechaHora = fechaHora;
        this.hallazgos = List.copyOf(hallazgos);
        this.archivosAnalizados = archivosAnalizados;
    }

    public ProyectoMuestra proyecto() {
        return proyecto;
    }

    public Herramienta herramienta() {
        return herramienta;
    }

    public LocalDateTime fechaHora() {
        return fechaHora;
    }

    public List<Hallazgo> hallazgos() {
        return hallazgos;
    }

    public int archivosAnalizados() {
        return archivosAnalizados;
    }

    public int total() {
        return hallazgos.size();
    }

    /** Cantidad de hallazgos de la severidad indicada. */
    public long conteoPorSeveridad(Severidad severidad) {
        return hallazgos.stream().filter(h -> h.severidad() == severidad).count();
    }

    /**
     * Reglas incumplidas con mayor frecuencia, de mayor a menor. Es la base
     * de la seccion de prioridades del informe: corregir primero lo que mas
     * se repite rinde mas que corregir casos aislados.
     */
    public Map<String, Long> reglasMasFrecuentes(int limite) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        hallazgos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Hallazgo::regla, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limite)
                .forEach(e -> conteo.put(e.getKey(), e.getValue()));
        return conteo;
    }

    /** Archivos con mas hallazgos, de mayor a menor. */
    public Map<String, Long> archivosMasAfectados(int limite) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        hallazgos.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Hallazgo::nombreArchivo, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limite)
                .forEach(e -> conteo.put(e.getKey(), e.getValue()));
        return conteo;
    }
}
