package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Origen del dato registrado. Permite rastrear cada valor hasta su
 * procedencia, requisito de la validez y confiabilidad declarada en el
 * marco metodologico de la investigacion.
 */
public enum FuenteDato {

    /** Reporte de archivo emitido por la herramienta (JSON, SARIF o XML). */
    REPORTE_HERRAMIENTA("Reporte de la herramienta"),

    /** Respuesta de la Web API del servidor de SonarQube. */
    WEB_API_SONARQUBE("Web API de SonarQube"),

    /** Medicion tomada por el propio orquestador desde fuera de la herramienta. */
    MEDICION_EXTERNA("Medicion externa del orquestador");

    private final String etiqueta;

    FuenteDato(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre legible de la fuente, usado en los reportes exportados. */
    public String etiqueta() {
        return etiqueta;
    }
}
