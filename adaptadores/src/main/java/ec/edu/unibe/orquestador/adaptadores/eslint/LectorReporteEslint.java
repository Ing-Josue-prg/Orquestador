package ec.edu.unibe.orquestador.adaptadores.eslint;

import ec.edu.unibe.orquestador.adaptadores.LectorJson;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interpreta el reporte JSON que produce ESLint y lo traduce al modelo de
 * hallazgos del dominio, usando {@link LectorJson} para no depender de
 * ninguna libreria externa.
 *
 * <p>ESLint solo distingue dos niveles de severidad (1 = advertencia, 2 =
 * error), asi que nunca se produce {@code Severidad.BAJA} desde esta
 * herramienta: es una limitacion de su propia escala, no una decision de
 * este lector.</p>
 */
public final class LectorReporteEslint {

    /** Resultado de interpretar un reporte: los hallazgos y los archivos con hallazgos. */
    public record Lectura(List<Hallazgo> hallazgos, int archivosConHallazgos) {
    }

    /**
     * Traduce el contenido JSON de un reporte de ESLint.
     *
     * @param json contenido del reporte (un arreglo de resultados por archivo)
     * @return los hallazgos encontrados
     */
    @SuppressWarnings("unchecked")
    public Lectura leer(String json) {
        List<Object> archivos = (List<Object>) LectorJson.leer(json);
        List<Hallazgo> hallazgos = new ArrayList<>();
        int archivosConHallazgos = 0;

        for (Object elemento : archivos) {
            Map<String, Object> archivo = (Map<String, Object>) elemento;
            String rutaArchivo = String.valueOf(archivo.get("filePath"));
            List<Object> mensajes = (List<Object>) archivo.getOrDefault("messages", List.of());

            if (!mensajes.isEmpty()) {
                archivosConHallazgos++;
            }
            for (Object mensaje : mensajes) {
                hallazgos.add(construirHallazgo((Map<String, Object>) mensaje, rutaArchivo));
            }
        }

        return new Lectura(hallazgos, archivosConHallazgos);
    }

    private Hallazgo construirHallazgo(Map<String, Object> mensaje, String rutaArchivo) {
        Object idCrudo = mensaje.get("ruleId");
        String regla = idCrudo == null ? "error-sintaxis" : idCrudo.toString();
        String categoria = regla.contains("/") ? regla.substring(0, regla.indexOf('/')) : "eslint-core";
        Severidad severidad = enteroDe(mensaje.get("severity")) >= 2 ? Severidad.ALTA : Severidad.MEDIA;
        int linea = enteroDe(mensaje.get("line"));
        String texto = String.valueOf(mensaje.getOrDefault("message", ""));

        return new Hallazgo(regla, categoria, severidad, rutaArchivo, linea, texto);
    }

    private int enteroDe(Object valor) {
        return valor instanceof Double numero ? numero.intValue() : 0;
    }
}
