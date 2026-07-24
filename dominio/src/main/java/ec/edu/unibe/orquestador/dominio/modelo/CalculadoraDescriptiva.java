package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.Collection;

/**
 * Calcula la estadistica descriptiva de un conjunto de valores.
 *
 * <p>La desviacion estandar es muestral, es decir, se divide entre
 * (n - 1), y el coeficiente de variacion se expresa en porcentaje. Ambas
 * decisiones siguen la interpretacion de la dispersion descrita en el
 * marco metodologico.</p>
 */
public final class CalculadoraDescriptiva {

    private CalculadoraDescriptiva() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Resume un conjunto de valores.
     *
     * @param valores valores a resumir; debe contener al menos uno
     * @return el resumen estadistico calculado
     * @throws IllegalArgumentException si la coleccion es nula o esta vacia
     */
    public static ResumenEstadistico resumir(Collection<Double> valores) {
        if (valores == null || valores.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un valor.");
        }

        int cantidad = valores.size();
        double media = valores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double minimo = valores.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double maximo = valores.stream().mapToDouble(Double::doubleValue).max().orElseThrow();

        double desviacion = 0.0;
        if (cantidad > 1) {
            double sumaCuadrados = valores.stream()
                    .mapToDouble(v -> Math.pow(v - media, 2))
                    .sum();
            desviacion = Math.sqrt(sumaCuadrados / (cantidad - 1));
        }

        double coeficienteVariacion = media != 0.0
                ? (desviacion / Math.abs(media)) * 100.0
                : 0.0;

        return new ResumenEstadistico(cantidad, media, minimo, maximo, desviacion, coeficienteVariacion);
    }
}
