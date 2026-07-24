package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Nivel de gravedad de un hallazgo, normalizado a tres niveles para poder
 * comparar herramientas que usan escalas distintas. PMD trabaja con
 * prioridades de 1 a 5, Checkstyle con error, warning e info, y ESLint con
 * error y warning; todas se traducen a esta escala comun.
 */
public enum Severidad {

    ALTA("Alta", "Requiere correccion inmediata"),
    MEDIA("Media", "Conviene corregir en el proximo ciclo"),
    BAJA("Baja", "Mejora opcional de estilo o claridad");

    private final String etiqueta;
    private final String recomendacion;

    Severidad(String etiqueta, String recomendacion) {
        this.etiqueta = etiqueta;
        this.recomendacion = recomendacion;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String recomendacion() {
        return recomendacion;
    }
}
