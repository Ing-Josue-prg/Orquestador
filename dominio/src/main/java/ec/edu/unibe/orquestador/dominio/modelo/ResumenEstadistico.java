package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Resultado del analisis descriptivo de un conjunto de corridas: las medidas
 * que exige la guia institucional para el capitulo de resultados.
 *
 * @param cantidad             numero de valores resumidos
 * @param media                promedio de los valores
 * @param minimo               valor minimo observado
 * @param maximo               valor maximo observado
 * @param desviacionEstandar   desviacion estandar muestral
 * @param coeficienteVariacion coeficiente de variacion, en porcentaje
 */
public record ResumenEstadistico(
        int cantidad,
        double media,
        double minimo,
        double maximo,
        double desviacionEstandar,
        double coeficienteVariacion) {
}
