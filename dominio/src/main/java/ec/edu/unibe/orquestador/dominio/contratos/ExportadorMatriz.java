package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrato para exportar la matriz comparativa a un formato externo, como
 * una hoja de calculo. Implementado en el Incremento 8 por
 * {@code ExportadorMatrizCsv}.
 */
public interface ExportadorMatriz {

    /**
     * Exporta la matriz al destino indicado.
     *
     * @param matriz       matriz con las mediciones acumuladas
     * @param rutaDestino  archivo de salida
     * @throws IOException si el archivo no puede escribirse
     */
    void exportar(MatrizComparativa matriz, Path rutaDestino) throws IOException;
}
