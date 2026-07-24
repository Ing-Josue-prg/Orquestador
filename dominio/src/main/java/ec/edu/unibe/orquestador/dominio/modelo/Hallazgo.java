package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Incidencia concreta que una herramienta reporta sobre el codigo fuente:
 * la regla incumplida, donde ocurre y que recomienda corregir.
 *
 * <p>Los hallazgos alimentan el informe de calidad del proyecto analizado.
 * Son distintos de las metricas de la matriz comparativa: aquellas
 * caracterizan a la herramienta, estos describen el codigo revisado.</p>
 *
 * @param regla     identificador de la regla incumplida
 * @param categoria familia de la regla, segun la clasifique la herramienta
 * @param severidad gravedad normalizada del hallazgo
 * @param archivo   ruta del archivo donde se detecto
 * @param linea     linea de inicio; cero si la herramienta no la informa
 * @param mensaje   explicacion que entrega la herramienta
 */
public record Hallazgo(
        String regla,
        String categoria,
        Severidad severidad,
        String archivo,
        int linea,
        String mensaje) {

    public Hallazgo {
        if (regla == null || regla.isBlank()) {
            throw new IllegalArgumentException("La regla es obligatoria.");
        }
        if (severidad == null) {
            throw new IllegalArgumentException("La severidad es obligatoria.");
        }
        if (archivo == null || archivo.isBlank()) {
            throw new IllegalArgumentException("El archivo es obligatorio.");
        }
        if (linea < 0) {
            throw new IllegalArgumentException("La linea no puede ser negativa.");
        }
        categoria = (categoria == null || categoria.isBlank()) ? "Sin categoria" : categoria.trim();
        mensaje = (mensaje == null) ? "" : mensaje.trim();
    }

    /** Nombre del archivo sin su ruta, para mostrarlo en tablas. */
    public String nombreArchivo() {
        int corte = Math.max(archivo.lastIndexOf('/'), archivo.lastIndexOf('\\'));
        return corte >= 0 ? archivo.substring(corte + 1) : archivo;
    }
}
