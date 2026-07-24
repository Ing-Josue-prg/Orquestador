package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.RegistroMedicion;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;

/**
 * Resultado que devuelve un adaptador tras ejecutar una herramienta sobre un
 * proyecto. Reune las tres salidas de una corrida: las mediciones que
 * alimentan la matriz comparativa, los hallazgos que alimentan el informe de
 * calidad del proyecto y el desempeno observado.
 *
 * <p>La memoria se expresa como valor opcional: cuando la plataforma no
 * permite medirla, el resultado queda vacio en lugar de registrar un cero
 * que podria confundirse con una medicion real.</p>
 *
 * @param mediciones         registros para la matriz comparativa
 * @param hallazgos          incidencias detectadas en el codigo
 * @param archivosAnalizados numero de archivos que reviso la herramienta
 * @param duracion           tiempo que tardo el analisis
 * @param memoriaMaximaBytes memoria maxima del proceso, si pudo medirse
 */
public record ResultadoAnalisis(
        List<RegistroMedicion> mediciones,
        List<Hallazgo> hallazgos,
        int archivosAnalizados,
        Duration duracion,
        OptionalLong memoriaMaximaBytes) {

    public ResultadoAnalisis {
        if (mediciones == null || hallazgos == null) {
            throw new IllegalArgumentException("Las mediciones y los hallazgos son obligatorios.");
        }
        if (archivosAnalizados < 0) {
            throw new IllegalArgumentException("El numero de archivos no puede ser negativo.");
        }
        if (duracion == null || duracion.isNegative()) {
            throw new IllegalArgumentException("La duracion debe ser un intervalo valido.");
        }
        if (memoriaMaximaBytes == null) {
            throw new IllegalArgumentException("La memoria debe indicarse, aunque sea vacia.");
        }
        mediciones = List.copyOf(mediciones);
        hallazgos = List.copyOf(hallazgos);
    }
}
