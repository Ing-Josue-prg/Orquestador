package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.InformeProyecto;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Contrato para exportar el informe de calidad de un proyecto a un formato
 * presentable, apto para adjuntarse a los anexos de la investigacion.
 */
public interface GeneradorInforme {

    /**
     * Escribe el informe en el archivo indicado.
     *
     * @param informe informe a exportar
     * @param destino archivo de salida
     * @throws IOException si el archivo no puede escribirse
     */
    void generar(InformeProyecto informe, Path destino) throws IOException;
}
