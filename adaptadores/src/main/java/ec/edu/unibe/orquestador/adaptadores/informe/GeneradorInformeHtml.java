package ec.edu.unibe.orquestador.adaptadores.informe;

import ec.edu.unibe.orquestador.dominio.contratos.GeneradorInforme;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.InformeProyecto;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Genera el informe de calidad en formato HTML, apto para revisarse en el
 * navegador, imprimirse en PDF y adjuntarse a los anexos de la
 * investigacion.
 *
 * <p>El informe se organiza por prioridad de correccion: primero el resumen
 * general, luego las reglas mas repetidas y los archivos mas afectados, y al
 * final el detalle completo de los hallazgos.</p>
 */
public final class GeneradorInformeHtml implements GeneradorInforme {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int LIMITE_DETALLE = 500;

    @Override
    public void generar(InformeProyecto informe, Path destino) throws IOException {
        String html = construir(informe);
        Files.writeString(destino, html, StandardCharsets.UTF_8);
    }

    private String construir(InformeProyecto informe) {
        StringBuilder h = new StringBuilder();

        h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
         .append("<title>Informe de calidad - ").append(escapar(informe.proyecto().nombre()))
         .append("</title>").append(estilos()).append("</head><body>");

        h.append("<header><h1>Informe de calidad de codigo</h1>")
         .append("<p class=\"sub\">Orquestador de Evaluacion de Calidad de Software</p></header>");

        h.append("<section class=\"ficha\"><table>")
         .append(fila("Proyecto analizado", informe.proyecto().nombre()))
         .append(fila("Lenguaje", informe.proyecto().lenguaje()))
         .append(fila("Repositorio", informe.proyecto().url()))
         .append(fila("Herramienta", informe.herramienta().toString()))
         .append(fila("Fecha del analisis", informe.fechaHora().format(FORMATO)))
         .append(fila("Archivos con hallazgos", String.valueOf(informe.archivosAnalizados())))
         .append("</table></section>");

        h.append("<section><h2>Resumen</h2><div class=\"tarjetas\">")
         .append(tarjeta("Total de hallazgos", String.valueOf(informe.total()), "total"))
         .append(tarjeta("Severidad alta", String.valueOf(informe.conteoPorSeveridad(Severidad.ALTA)), "alta"))
         .append(tarjeta("Severidad media", String.valueOf(informe.conteoPorSeveridad(Severidad.MEDIA)), "media"))
         .append(tarjeta("Severidad baja", String.valueOf(informe.conteoPorSeveridad(Severidad.BAJA)), "baja"))
         .append("</div></section>");

        h.append(seccionConteo("Reglas incumplidas con mayor frecuencia",
                "Corregir primero lo que mas se repite rinde mas que corregir casos aislados.",
                informe.reglasMasFrecuentes(10), "Regla"));

        h.append(seccionConteo("Archivos con mas hallazgos",
                "Estos archivos concentran la deuda y son los candidatos naturales a refactorizacion.",
                informe.archivosMasAfectados(10), "Archivo"));

        h.append(seccionDetalle(informe));

        h.append("<footer><p>Informe generado automaticamente por el orquestador. ")
         .append("Los hallazgos corresponden a lo reportado por la herramienta y pueden incluir ")
         .append("falsas alarmas, limitacion conocida del analisis estatico.</p></footer>");

        h.append("</body></html>");
        return h.toString();
    }

    private String seccionConteo(String titulo, String nota, Map<String, Long> datos, String columna) {
        if (datos.isEmpty()) {
            return "";
        }
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>").append(titulo).append("</h2>")
         .append("<p class=\"nota\">").append(nota).append("</p>")
         .append("<table class=\"datos\"><thead><tr><th>").append(columna)
         .append("</th><th>Cantidad</th></tr></thead><tbody>");

        datos.forEach((clave, valor) -> h.append("<tr><td>").append(escapar(clave))
                .append("</td><td class=\"num\">").append(valor).append("</td></tr>"));

        return h.append("</tbody></table></section>").toString();
    }

    private String seccionDetalle(InformeProyecto informe) {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Detalle de los hallazgos</h2>");

        if (informe.total() > LIMITE_DETALLE) {
            h.append("<p class=\"nota\">Se listan los primeros ").append(LIMITE_DETALLE)
             .append(" hallazgos de ").append(informe.total())
             .append(". El conteo completo consta en el resumen.</p>");
        }

        h.append("<table class=\"datos\"><thead><tr><th>Severidad</th><th>Regla</th>")
         .append("<th>Categoria</th><th>Archivo</th><th>Linea</th><th>Recomendacion</th>")
         .append("</tr></thead><tbody>");

        informe.hallazgos().stream().limit(LIMITE_DETALLE).forEach(hallazgo -> h
                .append("<tr><td><span class=\"etiqueta ")
                .append(hallazgo.severidad().name().toLowerCase()).append("\">")
                .append(hallazgo.severidad().etiqueta()).append("</span></td><td>")
                .append(escapar(hallazgo.regla())).append("</td><td>")
                .append(escapar(hallazgo.categoria())).append("</td><td>")
                .append(escapar(hallazgo.nombreArchivo())).append("</td><td class=\"num\">")
                .append(hallazgo.linea()).append("</td><td>")
                .append(escapar(hallazgo.mensaje())).append("</td></tr>"));

        return h.append("</tbody></table></section>").toString();
    }

    private String fila(String etiqueta, String valor) {
        return "<tr><th>" + escapar(etiqueta) + "</th><td>" + escapar(valor) + "</td></tr>";
    }

    private String tarjeta(String titulo, String valor, String clase) {
        return "<div class=\"tarjeta " + clase + "\"><span class=\"valor\">" + valor
                + "</span><span class=\"titulo\">" + escapar(titulo) + "</span></div>";
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String estilos() {
        return """
                <style>
                  body { font-family: Segoe UI, Arial, sans-serif; color: #1f2933;
                         margin: 0; padding: 0 40px 60px; background: #fff; }
                  header { border-bottom: 3px solid #1f4e79; padding: 28px 0 16px; margin-bottom: 28px; }
                  h1 { margin: 0; font-size: 26px; color: #1f4e79; }
                  h2 { font-size: 19px; color: #1f4e79; margin-top: 34px;
                       border-bottom: 1px solid #d7dde3; padding-bottom: 6px; }
                  .sub { margin: 6px 0 0; color: #62727f; font-size: 14px; }
                  .nota { color: #62727f; font-size: 13px; margin: 4px 0 12px; }
                  .ficha table { border-collapse: collapse; }
                  .ficha th { text-align: left; padding: 5px 24px 5px 0; color: #62727f;
                              font-weight: 600; font-size: 14px; }
                  .ficha td { padding: 5px 0; font-size: 14px; }
                  .tarjetas { display: flex; gap: 14px; flex-wrap: wrap; margin-top: 12px; }
                  .tarjeta { border: 1px solid #d7dde3; border-radius: 8px; padding: 16px 22px;
                             min-width: 150px; border-left-width: 5px; }
                  .tarjeta .valor { display: block; font-size: 30px; font-weight: 700; }
                  .tarjeta .titulo { display: block; font-size: 13px; color: #62727f; margin-top: 4px; }
                  .tarjeta.total { border-left-color: #1f4e79; }
                  .tarjeta.alta  { border-left-color: #c0392b; }
                  .tarjeta.media { border-left-color: #d68910; }
                  .tarjeta.baja  { border-left-color: #7f8c8d; }
                  table.datos { width: 100%; border-collapse: collapse; font-size: 13px; }
                  table.datos th { background: #f2f5f8; text-align: left; padding: 8px;
                                   border-bottom: 2px solid #d7dde3; }
                  table.datos td { padding: 7px 8px; border-bottom: 1px solid #eceff2;
                                   vertical-align: top; }
                  table.datos tr:nth-child(even) td { background: #fafbfc; }
                  .num { text-align: right; }
                  .etiqueta { padding: 2px 9px; border-radius: 10px; font-size: 12px;
                              font-weight: 600; color: #fff; }
                  .etiqueta.alta { background: #c0392b; }
                  .etiqueta.media { background: #d68910; }
                  .etiqueta.baja { background: #7f8c8d; }
                  footer { margin-top: 40px; border-top: 1px solid #d7dde3; padding-top: 14px;
                           color: #62727f; font-size: 12px; }
                </style>
                """;
    }
}
