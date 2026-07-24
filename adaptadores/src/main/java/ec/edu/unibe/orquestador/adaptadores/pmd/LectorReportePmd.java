package ec.edu.unibe.orquestador.adaptadores.pmd;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Interpreta el reporte XML que produce PMD y lo traduce al modelo de
 * hallazgos del dominio.
 *
 * <p>Se eligio el formato XML porque Java lo procesa con las bibliotecas que
 * ya trae incorporadas, sin necesidad de sumar dependencias externas al
 * proyecto. El lector desactiva la resolucion de entidades externas, una
 * precaucion habitual al procesar XML de origen automatico.</p>
 */
public final class LectorReportePmd {

    /** Resultado de interpretar un reporte: los hallazgos y los archivos revisados. */
    public record Lectura(List<Hallazgo> hallazgos, int archivosConHallazgos) {
    }

    /**
     * Traduce el contenido XML de un reporte de PMD.
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
        NodeList archivos = documento.getElementsByTagName("file");

        for (int i = 0; i < archivos.getLength(); i++) {
            Element archivo = (Element) archivos.item(i);
            String rutaArchivo = archivo.getAttribute("name");

            NodeList violaciones = archivo.getElementsByTagName("violation");
            for (int j = 0; j < violaciones.getLength(); j++) {
                Element violacion = (Element) violaciones.item(j);
                hallazgos.add(construirHallazgo(violacion, rutaArchivo));
            }
        }

        return new Lectura(hallazgos, archivos.getLength());
    }

    private Hallazgo construirHallazgo(Element violacion, String rutaArchivo) {
        String regla = violacion.getAttribute("rule");
        String categoria = violacion.getAttribute("ruleset");
        int linea = enteroDe(violacion.getAttribute("beginline"));
        Severidad severidad = severidadDesdePrioridad(violacion.getAttribute("priority"));
        String mensaje = textoDe(violacion);

        return new Hallazgo(regla, categoria, severidad, rutaArchivo, linea, mensaje);
    }

    /**
     * Traduce la prioridad de PMD, que va de 1 a 5 siendo 1 la mas grave, a
     * la escala de tres niveles del dominio.
     */
    private Severidad severidadDesdePrioridad(String prioridad) {
        int valor = enteroDe(prioridad);
        if (valor == 1 || valor == 2) {
            return Severidad.ALTA;
        }
        if (valor == 3) {
            return Severidad.MEDIA;
        }
        return Severidad.BAJA;
    }

    private int enteroDe(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    private String textoDe(Node nodo) {
        String texto = nodo.getTextContent();
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }
}
