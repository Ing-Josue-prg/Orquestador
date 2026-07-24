package ec.edu.unibe.orquestador.adaptadores.checkstyle;

import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Interpreta el reporte XML que produce Checkstyle y lo traduce al modelo de
 * hallazgos del dominio.
 *
 * <p>Checkstyle escribe un elemento {@code <file>} por cada archivo que
 * revisa, tenga o no errores; para que "archivos analizados" signifique lo
 * mismo en las tres herramientas de codigo (PMD, Checkstyle, ESLint), aqui
 * solo se cuentan los archivos con al menos un {@code <error>}.</p>
 */
public final class LectorReporteCheckstyle {

    /** Resultado de interpretar un reporte: los hallazgos y los archivos con hallazgos. */
    public record Lectura(List<Hallazgo> hallazgos, int archivosConHallazgos) {
    }

    /**
     * Traduce el contenido XML de un reporte de Checkstyle.
     *
     * @param xml contenido del reporte
     * @return los hallazgos encontrados
     * @throws Exception si el XML no puede interpretarse
     */
    public Lectura leer(String xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        fabrica.setExpandEntityReferences(false);

        DocumentBuilder constructor = fabrica.newDocumentBuilder();
        Document documento = constructor.parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        List<Hallazgo> hallazgos = new ArrayList<>();
        int archivosConHallazgos = 0;
        NodeList archivos = documento.getElementsByTagName("file");

        for (int i = 0; i < archivos.getLength(); i++) {
            Element archivo = (Element) archivos.item(i);
            String rutaArchivo = archivo.getAttribute("name");

            NodeList errores = archivo.getElementsByTagName("error");
            if (errores.getLength() > 0) {
                archivosConHallazgos++;
            }
            for (int j = 0; j < errores.getLength(); j++) {
                hallazgos.add(construirHallazgo((Element) errores.item(j), rutaArchivo));
            }
        }

        return new Lectura(hallazgos, archivosConHallazgos);
    }

    private Hallazgo construirHallazgo(Element error, String rutaArchivo) {
        String fuente = error.getAttribute("source");
        String regla = ultimoSegmento(fuente, "regla-desconocida");
        String categoria = penultimoSegmento(fuente);
        int linea = enteroDe(error.getAttribute("line"));
        Severidad severidad = severidadDesdeTexto(error.getAttribute("severity"));
        String mensaje = error.getAttribute("message");

        return new Hallazgo(regla, categoria, severidad, rutaArchivo, linea, mensaje);
    }

    /**
     * El atributo "source" de Checkstyle es el nombre completo de la clase
     * del check, por ejemplo
     * "com.puppycrawl.tools.checkstyle.checks.naming.MemberNameCheck". El
     * ultimo segmento es un nombre de regla legible y el anterior es la
     * categoria del check (el paquete que lo agrupa).
     */
    private String ultimoSegmento(String fuente, String pordefecto) {
        if (fuente == null || fuente.isBlank()) {
            return pordefecto;
        }
        String[] partes = fuente.split("\\.");
        return partes[partes.length - 1];
    }

    private String penultimoSegmento(String fuente) {
        if (fuente == null || fuente.isBlank()) {
            return "";
        }
        String[] partes = fuente.split("\\.");
        return partes.length >= 2 ? partes[partes.length - 2] : "";
    }

    private Severidad severidadDesdeTexto(String severidad) {
        return switch (severidad == null ? "" : severidad.trim().toLowerCase()) {
            case "error" -> Severidad.ALTA;
            case "warning" -> Severidad.MEDIA;
            default -> Severidad.BAJA;
        };
    }

    private int enteroDe(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}
