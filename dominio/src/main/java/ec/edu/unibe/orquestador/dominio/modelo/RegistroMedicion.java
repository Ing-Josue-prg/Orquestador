package ec.edu.unibe.orquestador.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Una fila de la ficha de registro (matriz comparativa) definida en el
 * instrumento del marco metodologico. Cada objeto representa una medicion
 * individual y contiene todos los campos fijos del instrumento.
 *
 * <p>Es inmutable: una vez registrada, la medicion no se altera.</p>
 *
 * @param fechaHora     momento exacto de la ejecucion
 * @param herramienta   herramienta que produjo el dato
 * @param proyecto      proyecto sobre el que se ejecuto el analisis
 * @param numeroCorrida numero de corrida (1, 2 o 3)
 * @param dimension     dimension de la operacionalizacion a la que pertenece
 * @param indicador     nombre del indicador medido
 * @param valor         resultado numerico registrado
 * @param unidad        unidad de medida del valor
 * @param fuente        origen del dato, para su trazabilidad
 */
public record RegistroMedicion(
        LocalDateTime fechaHora,
        Herramienta herramienta,
        ProyectoAnalizado proyecto,
        int numeroCorrida,
        Dimension dimension,
        String indicador,
        double valor,
        String unidad,
        FuenteDato fuente) {

    public RegistroMedicion {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias.");
        }
        if (herramienta == null) {
            throw new IllegalArgumentException("La herramienta es obligatoria.");
        }
        if (proyecto == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio.");
        }
        if (numeroCorrida < 1) {
            throw new IllegalArgumentException("El numero de corrida debe ser mayor o igual a 1.");
        }
        if (dimension == null) {
            throw new IllegalArgumentException("La dimension es obligatoria.");
        }
        if (indicador == null || indicador.isBlank()) {
            throw new IllegalArgumentException("El indicador es obligatorio.");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new IllegalArgumentException("La unidad es obligatoria.");
        }
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException("El valor debe ser un numero finito.");
        }
        if (fuente == null) {
            throw new IllegalArgumentException("La fuente del dato es obligatoria.");
        }
        indicador = indicador.trim();
        unidad = unidad.trim();
    }
}
