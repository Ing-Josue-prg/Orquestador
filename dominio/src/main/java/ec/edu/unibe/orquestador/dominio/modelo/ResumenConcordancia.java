package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.Map;

/**
 * Resultado de comparar los hallazgos de varias herramientas sobre el mismo
 * proyecto y el mismo commit: en cuantas ubicaciones coincidieron y cuantos
 * hallazgos son exclusivos de cada una.
 *
 * <p>No declara ninguna herramienta "mejor" que otra: eso exigiria un
 * criterio de verdad (codigo etiquetado a mano) que esta investigacion, de
 * caracter descriptivo, no tiene. La concordancia es un dato observable y
 * reproducible; la precision de cada herramienta no lo es sin ese criterio.</p>
 *
 * @param totalUbicaciones     cantidad de ubicaciones (archivo + linea)
 *                             distintas senaladas por al menos una herramienta
 * @param ubicacionesEnComun   cuantas de esas ubicaciones fueron senaladas
 *                             por dos o mas herramientas
 * @param hallazgosExclusivos  por herramienta, cuantas de sus ubicaciones no
 *                             senalo ninguna otra
 */
public record ResumenConcordancia(
        int totalUbicaciones,
        int ubicacionesEnComun,
        Map<HerramientaSoportada, Integer> hallazgosExclusivos) {

    public ResumenConcordancia {
        if (totalUbicaciones < 0 || ubicacionesEnComun < 0) {
            throw new IllegalArgumentException("Los conteos de ubicaciones no pueden ser negativos.");
        }
        if (hallazgosExclusivos == null) {
            throw new IllegalArgumentException("El mapa de hallazgos exclusivos es obligatorio.");
        }
        hallazgosExclusivos = Map.copyOf(hallazgosExclusivos);
    }
}
