package ec.edu.unibe.orquestador.adaptadores.informe;

import ec.edu.unibe.orquestador.adaptadores.OrquestadorAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.ResumenConcordancia;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera el informe comparativo final de una corrida conjunta: los numeros
 * de cada herramienta lado a lado y la concordancia entre ellas, en HTML
 * autocontenido, apto para adjuntarse a los anexos de la investigacion.
 *
 * <p>No declara ninguna herramienta ganadora: eso exigiria un criterio de
 * verdad (codigo etiquetado a mano) que esta investigacion, de caracter
 * descriptivo, no tiene. El informe deja la evidencia completa —
 * hallazgos, severidad, archivos, tiempo y concordancia — para que las
 * conclusiones se saquen en el capitulo de discusion de la tesis, no
 * dentro de la propia aplicacion.</p>
 */
public final class GeneradorInformeComparativoHtml {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Genera el informe comparativo de la ultima corrida conjunta.
     *
     * @param proyecto     proyecto analizado
     * @param resultados   resultados exitosos y fallidos de la corrida
     * @param concordancia concordancia calculada entre las herramientas exitosas
     * @param destino      archivo HTML de salida
     */
    public void generar(ProyectoMuestra proyecto, OrquestadorAnalisis.ResultadoConjunto resultados,
                        ResumenConcordancia concordancia, Path destino) throws IOException {
        String html = construir(proyecto, resultados, concordancia);
        Files.writeString(destino, html, StandardCharsets.UTF_8);
    }

    private String construir(ProyectoMuestra proyecto, OrquestadorAnalisis.ResultadoConjunto resultados,
                             ResumenConcordancia concordancia) {
        StringBuilder h = new StringBuilder();

        h.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
         .append("<title>Informe comparativo - ").append(escapar(proyecto.nombre()))
         .append("</title>").append(estilos()).append("</head><body>");

        h.append("<header><h1>Informe comparativo final</h1>")
         .append("<p class=\"sub\">Orquestador de Evaluacion de Calidad de Software</p></header>");

        h.append("<section class=\"ficha\"><table>")
         .append(fila("Proyecto analizado", proyecto.nombre()))
         .append(fila("Lenguaje", proyecto.lenguaje()))
         .append(fila("Repositorio", proyecto.url()))
         .append(fila("Fecha del informe", LocalDateTime.now().format(FORMATO)))
         .append("</table></section>");

        h.append(seccionResultadoPorHerramienta(resultados));
        h.append(seccionConcordancia(concordancia));

        h.append("<section><h2>Que compara este informe (y que no)</h2>")
         .append("<p class=\"nota\">Este informe no declara ninguna herramienta \"mejor\" que otra: ")
         .append("eso exigiria un criterio de verdad (codigo etiquetado a mano como defecto real o ")
         .append("falsa alarma) que este diseno, descriptivo, no tiene. Lo que se compara con rigor ")
         .append("es la concordancia: cuando dos o mas herramientas senalan la misma ubicacion del ")
         .append("codigo (archivo y linea), y en que divergen. De ahi salen los criterios de ")
         .append("seleccion de la investigacion, no de un ranking de precision inventado.</p></section>");

        h.append("<footer><p>Informe generado automaticamente por el orquestador a partir de la ")
         .append("ultima corrida conjunta del proyecto.</p></footer>");

        h.append("</body></html>");
        return h.toString();
    }

    private String seccionResultadoPorHerramienta(OrquestadorAnalisis.ResultadoConjunto resultados) {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Resultado por herramienta</h2>");

        if (!resultados.exitosos().isEmpty()) {
            h.append("<table class=\"datos\"><thead><tr><th>Herramienta</th><th>Total</th>")
             .append("<th>Alta</th><th>Media</th><th>Baja</th><th>Archivos</th>")
             .append("<th>Tiempo (s)</th></tr></thead><tbody>");

            for (var corrida : resultados.exitosos()) {
                var hallazgos = corrida.resultado().hallazgos();
                long alta = hallazgos.stream().filter(x -> x.severidad() == Severidad.ALTA).count();
                long media = hallazgos.stream().filter(x -> x.severidad() == Severidad.MEDIA).count();
                long baja = hallazgos.stream().filter(x -> x.severidad() == Severidad.BAJA).count();

                h.append("<tr><td>").append(escapar(corrida.herramienta().nombre())).append("</td>")
                 .append("<td class=\"num\">").append(hallazgos.size()).append("</td>")
                 .append("<td class=\"num\">").append(alta).append("</td>")
                 .append("<td class=\"num\">").append(media).append("</td>")
                 .append("<td class=\"num\">").append(baja).append("</td>")
                 .append("<td class=\"num\">").append(corrida.resultado().archivosAnalizados()).append("</td>")
                 .append("<td class=\"num\">")
                 .append(String.format("%.2f", corrida.resultado().duracion().toMillis() / 1000.0))
                 .append("</td></tr>");
            }
            h.append("</tbody></table>");
        }

        if (!resultados.fallidos().isEmpty()) {
            h.append("<p class=\"nota\">Herramientas sin resultado en esta corrida:</p>")
             .append("<table class=\"datos\"><thead><tr><th>Herramienta</th><th>Motivo</th>")
             .append("</tr></thead><tbody>");
            for (var fallido : resultados.fallidos()) {
                h.append("<tr><td>").append(escapar(fallido.herramienta().nombre())).append("</td>")
                 .append("<td>").append(escapar(fallido.motivo())).append("</td></tr>");
            }
            h.append("</tbody></table>");
        }

        return h.append("</section>").toString();
    }

    private String seccionConcordancia(ResumenConcordancia concordancia) {
        StringBuilder h = new StringBuilder();
        h.append("<section><h2>Concordancia entre herramientas</h2>")
         .append("<p class=\"nota\">").append(concordancia.ubicacionesEnComun()).append(" de ")
         .append(concordancia.totalUbicaciones()).append(" ubicaciones (archivo + linea) fueron ")
         .append("senaladas por dos o mas herramientas.</p>");

        if (!concordancia.hallazgosExclusivos().isEmpty()) {
            h.append("<table class=\"datos\"><thead><tr><th>Herramienta</th>")
             .append("<th>Hallazgos en ubicaciones exclusivas</th></tr></thead><tbody>");
            for (HerramientaSoportada herramienta : concordancia.hallazgosExclusivos().keySet()) {
                h.append("<tr><td>").append(escapar(herramienta.nombre())).append("</td>")
                 .append("<td class=\"num\">")
                 .append(concordancia.hallazgosExclusivos().get(herramienta)).append("</td></tr>");
            }
            h.append("</tbody></table>");
        }

        return h.append("</section>").toString();
    }

    private String fila(String etiqueta, String valor) {
        return "<tr><th>" + escapar(etiqueta) + "</th><td>" + escapar(valor) + "</td></tr>";
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
                  table.datos { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 8px; }
                  table.datos th { background: #f2f5f8; text-align: left; padding: 8px;
                                   border-bottom: 2px solid #d7dde3; }
                  table.datos td { padding: 7px 8px; border-bottom: 1px solid #eceff2;
                                   vertical-align: top; }
                  table.datos tr:nth-child(even) td { background: #fafbfc; }
                  .num { text-align: right; }
                  footer { margin-top: 40px; border-top: 1px solid #d7dde3; padding-top: 14px;
                           color: #62727f; font-size: 12px; }
                </style>
                """;
    }
}
