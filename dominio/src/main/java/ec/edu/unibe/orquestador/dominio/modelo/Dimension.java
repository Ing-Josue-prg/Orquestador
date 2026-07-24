package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Dimensiones de la variable de estudio, segun la operacionalizacion del
 * marco metodologico. Toda medicion pertenece a una de ellas.
 */
public enum Dimension {

    /** Metricas de calidad reportadas por la herramienta. */
    METRICAS("Metricas de calidad reportadas"),

    /** Funcionalidades que ofrece la herramienta. */
    FUNCIONALIDADES("Funcionalidades"),

    /** Desempeno de la herramienta: tiempo de analisis y memoria. */
    DESEMPENO("Desempeno");

    private final String etiqueta;

    Dimension(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre legible de la dimension, usado en los reportes exportados. */
    public String etiqueta() {
        return etiqueta;
    }
}
