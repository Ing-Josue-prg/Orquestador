package ec.edu.unibe.orquestador.adaptadores.matriz;

import ec.edu.unibe.orquestador.dominio.contratos.ExportadorMatriz;
import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import ec.edu.unibe.orquestador.dominio.modelo.RegistroMedicion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exporta la matriz comparativa a un archivo CSV plano, abrible en Excel o
 * en Google Sheets sin ninguna libreria adicional: una fila por
 * {@link RegistroMedicion}, con todos los campos fijos del instrumento.
 */
public final class ExportadorMatrizCsv implements ExportadorMatriz {

    private static final String ENCABEZADO = "fechaHora,herramienta,version,proyecto,lenguaje,"
            + "commit,corrida,dimension,indicador,valor,unidad,fuente";

    @Override
    public void exportar(MatrizComparativa matriz, Path rutaDestino) throws IOException {
        StringBuilder csv = new StringBuilder(ENCABEZADO).append('\n');

        for (RegistroMedicion r : matriz.registros()) {
            csv.append(campo(r.fechaHora().toString())).append(',')
               .append(campo(r.herramienta().nombre())).append(',')
               .append(campo(r.herramienta().version())).append(',')
               .append(campo(r.proyecto().nombre())).append(',')
               .append(campo(r.proyecto().lenguaje())).append(',')
               .append(campo(r.proyecto().commit())).append(',')
               .append(r.numeroCorrida()).append(',')
               .append(campo(r.dimension().name())).append(',')
               .append(campo(r.indicador())).append(',')
               .append(r.valor()).append(',')
               .append(campo(r.unidad())).append(',')
               .append(campo(r.fuente().name())).append('\n');
        }

        Files.writeString(rutaDestino, csv.toString(), StandardCharsets.UTF_8);
    }

    /** Encierra el campo entre comillas si contiene coma, comilla o salto de linea. */
    private String campo(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
