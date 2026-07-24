package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula la concordancia entre los hallazgos que distintas herramientas
 * reportan sobre el mismo proyecto y el mismo commit.
 *
 * <p>Dos hallazgos "coinciden" cuando senalan exactamente el mismo archivo y
 * la misma linea. No se intenta un "casi coincide" con tolerancia de lineas
 * cercanas: es una limitacion conocida y simple de explicar, mas defendible
 * que una heuristica de proximidad. Si una herramienta no informa la linea
 * (queda en cero), varios de sus hallazgos en el mismo archivo pueden verse
 * como si coincidieran entre si sin serlo realmente; es la misma limitacion,
 * no un caso aparte.</p>
 */
public final class CalculadoraConcordancia {

    private CalculadoraConcordancia() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Compara los hallazgos de cada herramienta y resume su concordancia.
     *
     * @param hallazgosPorHerramienta hallazgos de la misma corrida, agrupados
     *                                por la herramienta que los reporto
     * @return el resumen de concordancia calculado
     */
    public static ResumenConcordancia calcular(
            Map<HerramientaSoportada, List<Hallazgo>> hallazgosPorHerramienta) {

        if (hallazgosPorHerramienta == null) {
            throw new IllegalArgumentException("El mapa de hallazgos por herramienta es obligatorio.");
        }

        Map<String, Set<HerramientaSoportada>> ubicaciones = new LinkedHashMap<>();
        for (var entrada : hallazgosPorHerramienta.entrySet()) {
            for (Hallazgo hallazgo : entrada.getValue()) {
                String clave = hallazgo.archivo() + ":" + hallazgo.linea();
                ubicaciones.computeIfAbsent(clave, k -> new LinkedHashSet<>()).add(entrada.getKey());
            }
        }

        int ubicacionesEnComun = (int) ubicaciones.values().stream()
                .filter(herramientas -> herramientas.size() >= 2)
                .count();

        Map<HerramientaSoportada, Integer> exclusivos = new LinkedHashMap<>();
        for (HerramientaSoportada herramienta : hallazgosPorHerramienta.keySet()) {
            exclusivos.put(herramienta, 0);
        }
        for (Set<HerramientaSoportada> herramientas : ubicaciones.values()) {
            if (herramientas.size() == 1) {
                HerramientaSoportada unica = herramientas.iterator().next();
                exclusivos.merge(unica, 1, Integer::sum);
            }
        }

        return new ResumenConcordancia(ubicaciones.size(), ubicacionesEnComun, exclusivos);
    }
}
