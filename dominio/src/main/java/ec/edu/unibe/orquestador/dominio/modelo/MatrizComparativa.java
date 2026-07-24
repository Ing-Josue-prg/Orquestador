package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Coleccion de todas las mediciones de la investigacion. Es el instrumento
 * completo: acumula los registros que producen las corridas y ofrece las
 * consultas necesarias para el analisis descriptivo del capitulo de
 * resultados.
 */
public final class MatrizComparativa {

    private final List<RegistroMedicion> registros = new ArrayList<>();

    /** Vista de solo lectura de todos los registros acumulados. */
    public List<RegistroMedicion> registros() {
        return Collections.unmodifiableList(registros);
    }

    /** Cantidad de mediciones acumuladas. */
    public int cantidad() {
        return registros.size();
    }

    /**
     * Anade una medicion a la matriz.
     *
     * @param registro medicion a registrar, no nula
     */
    public void agregar(RegistroMedicion registro) {
        if (registro == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo.");
        }
        registros.add(registro);
    }

    /**
     * Anade varias mediciones a la matriz.
     *
     * @param nuevos mediciones a registrar
     */
    public void agregarTodos(Collection<RegistroMedicion> nuevos) {
        if (nuevos == null) {
            throw new IllegalArgumentException("La coleccion no puede ser nula.");
        }
        nuevos.forEach(this::agregar);
    }

    /**
     * Devuelve las corridas de un mismo indicador para un par
     * herramienta-proyecto, ordenadas por numero de corrida. Es el insumo
     * del calculo de promedios y de dispersion.
     *
     * @param herramienta nombre de la herramienta
     * @param proyecto    nombre del proyecto
     * @param indicador   nombre del indicador
     * @return las mediciones coincidentes, ordenadas por corrida
     */
    public List<RegistroMedicion> filtrar(String herramienta, String proyecto, String indicador) {
        return registros.stream()
                .filter(r -> r.herramienta().nombre().equalsIgnoreCase(herramienta))
                .filter(r -> r.proyecto().nombre().equalsIgnoreCase(proyecto))
                .filter(r -> r.indicador().equalsIgnoreCase(indicador))
                .sorted(Comparator.comparingInt(RegistroMedicion::numeroCorrida))
                .toList();
    }
}
