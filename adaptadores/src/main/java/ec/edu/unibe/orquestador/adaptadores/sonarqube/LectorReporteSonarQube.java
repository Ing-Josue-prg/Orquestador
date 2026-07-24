package ec.edu.unibe.orquestador.adaptadores.sonarqube;

import ec.edu.unibe.orquestador.adaptadores.LectorJson;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interpreta una pagina de la respuesta JSON de
 * {@code GET /api/issues/search} de la Web API de SonarQube y la traduce al
 * modelo de hallazgos del dominio, usando {@link LectorJson}.
 */
public final class LectorReporteSonarQube {

    /** Resultado de interpretar una pagina: sus hallazgos y el total real informado por el servidor. */
    public record Lectura(List<Hallazgo> hallazgos, int total) {
    }

    /**
     * Traduce una pagina de {@code /api/issues/search}.
     *
     * @param json     contenido de la respuesta de esa pagina
     * @param claveProyecto identificador del proyecto en SonarQube, para
     *                      recortar el prefijo de la ruta de cada archivo
     * @return los hallazgos de la pagina y el total de hallazgos del proyecto
     */
    @SuppressWarnings("unchecked")
    public Lectura leer(String json, String claveProyecto) {
        Map<String, Object> raiz = (Map<String, Object>) LectorJson.leer(json);
        List<Object> issues = (List<Object>) raiz.getOrDefault("issues", List.of());

        List<Hallazgo> hallazgos = new ArrayList<>();
        for (Object elemento : issues) {
            hallazgos.add(construirHallazgo((Map<String, Object>) elemento, claveProyecto));
        }

        int total = extraerTotal(raiz);
        return new Lectura(hallazgos, total);
    }

    @SuppressWarnings("unchecked")
    private int extraerTotal(Map<String, Object> raiz) {
        Object paginacion = raiz.get("paging");
        if (paginacion instanceof Map) {
            Object total = ((Map<String, Object>) paginacion).get("total");
            if (total instanceof Double numero) {
                return numero.intValue();
            }
        }
        Object total = raiz.get("total");
        return total instanceof Double numero ? numero.intValue() : hallazgosSinPaginacion(raiz);
    }

    @SuppressWarnings("unchecked")
    private int hallazgosSinPaginacion(Map<String, Object> raiz) {
        Object issues = raiz.get("issues");
        return issues instanceof List ? ((List<Object>) issues).size() : 0;
    }

    private Hallazgo construirHallazgo(Map<String, Object> issue, String claveProyecto) {
        String regla = String.valueOf(issue.getOrDefault("rule", "regla-desconocida"));
        String categoria = regla.contains(":") ? regla.substring(0, regla.indexOf(':')) : "sonarqube";
        Severidad severidad = severidadDesdeTexto(String.valueOf(issue.get("severity")));
        String archivo = archivoSinPrefijo(String.valueOf(issue.get("component")), claveProyecto);
        int linea = enteroDe(issue.get("line"));
        String mensaje = String.valueOf(issue.getOrDefault("message", ""));

        return new Hallazgo(regla, categoria, severidad, archivo, linea, mensaje);
    }

    /**
     * El campo "component" de SonarQube viene como
     * {@code "<claveDeProyecto>:<ruta/relativa>"}; se recorta el prefijo
     * para dejar una ruta tan legible como la de las demas herramientas.
     */
    private String archivoSinPrefijo(String componente, String claveProyecto) {
        String prefijo = claveProyecto + ":";
        return componente.startsWith(prefijo) ? componente.substring(prefijo.length()) : componente;
    }

    private Severidad severidadDesdeTexto(String severidad) {
        return switch (severidad == null ? "" : severidad.trim().toUpperCase()) {
            case "BLOCKER", "CRITICAL" -> Severidad.ALTA;
            case "MAJOR" -> Severidad.MEDIA;
            default -> Severidad.BAJA;
        };
    }

    private int enteroDe(Object valor) {
        return valor instanceof Double numero ? numero.intValue() : 0;
    }
}
