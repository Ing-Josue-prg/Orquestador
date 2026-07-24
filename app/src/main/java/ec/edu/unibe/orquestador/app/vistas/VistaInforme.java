package ec.edu.unibe.orquestador.app.vistas;

import ec.edu.unibe.orquestador.adaptadores.informe.GeneradorInformeHtml;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.InformeProyecto;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.List;

/**
 * Tercera seccion: presenta el informe de calidad del ultimo analisis y
 * permite exportarlo en formato HTML para adjuntarlo a los anexos.
 *
 * <p>El informe responde a la pregunta practica de que conviene mejorar en
 * el codigo revisado. Es distinto de la matriz comparativa, que caracteriza
 * a las herramientas y constituye el dato central de la investigacion.</p>
 */
public final class VistaInforme {

    private final ObservableList<Hallazgo> hallazgos = FXCollections.observableArrayList();
    private final FilteredList<Hallazgo> hallazgosFiltrados = new FilteredList<>(hallazgos, h -> true);

    private final Label encabezado = new Label("Aun no hay resultados.");
    private final Label resumen = new Label("Ejecute un analisis en la seccion anterior.");
    private final ComboBox<String> filtroSeveridad = new ComboBox<>();
    private final Button exportar = new Button("Exportar informe HTML");

    private InformeProyecto informeActual;

    public VistaInforme() {
        filtroSeveridad.getItems().addAll("Todas", "Alta", "Media", "Baja");
        filtroSeveridad.getSelectionModel().selectFirst();
        filtroSeveridad.setOnAction(e -> aplicarFiltro());

        exportar.getStyleClass().add("boton-principal");
        exportar.setDisable(true);
        exportar.setOnAction(e -> exportarInforme());
    }

    public Region contenido() {
        Label titulo = new Label("Informe de calidad del proyecto");
        titulo.getStyleClass().add("titulo-vista");

        encabezado.getStyleClass().add("subtitulo");
        resumen.getStyleClass().add("ayuda");
        resumen.setWrapText(true);

        VBox tarjetaResumen = new VBox(6, encabezado, resumen);
        tarjetaResumen.getStyleClass().add("tarjeta");

        HBox acciones = new HBox(12, new Label("Severidad:"), filtroSeveridad, exportar);

        VBox caja = new VBox(14, titulo, tarjetaResumen, acciones, tablaHallazgos());
        caja.setPadding(new Insets(28, 32, 28, 32));
        caja.getStyleClass().add("panel");
        return caja;
    }

    private TableView<Hallazgo> tablaHallazgos() {
        TableView<Hallazgo> tabla = new TableView<>(hallazgosFiltrados);
        tabla.setPlaceholder(new Label("Sin hallazgos que mostrar."));

        TableColumn<Hallazgo, Severidad> severidad = new TableColumn<>("Severidad");
        severidad.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().severidad()));
        severidad.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(Severidad valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(valor.etiqueta());
                chip.getStyleClass().addAll("chip", "chip-" + valor.name().toLowerCase());
                setGraphic(chip);
            }
        });
        severidad.setPrefWidth(100);

        TableColumn<Hallazgo, String> regla = new TableColumn<>("Regla");
        regla.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().regla()));
        regla.setPrefWidth(220);

        TableColumn<Hallazgo, String> categoria = new TableColumn<>("Categoria");
        categoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().categoria()));
        categoria.setPrefWidth(150);

        TableColumn<Hallazgo, String> archivo = new TableColumn<>("Archivo");
        archivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreArchivo()));
        archivo.setPrefWidth(200);

        TableColumn<Hallazgo, String> linea = new TableColumn<>("Linea");
        linea.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().linea())));
        linea.setPrefWidth(70);

        TableColumn<Hallazgo, String> mensaje = new TableColumn<>("Que conviene mejorar");
        mensaje.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().mensaje()));
        mensaje.setPrefWidth(420);

        tabla.getColumns().addAll(List.of(severidad, regla, categoria, archivo, linea, mensaje));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return tabla;
    }

    /** Carga en la vista el informe producido por el ultimo analisis. */
    public void mostrar(InformeProyecto informe) {
        this.informeActual = informe;
        hallazgos.setAll(informe.hallazgos());

        encabezado.setText(informe.proyecto().nombre() + "  |  "
                + informe.herramienta().nombre());
        resumen.setText(String.format(
                "%d hallazgos en %d archivos. Alta: %d, media: %d, baja: %d.",
                informe.total(), informe.archivosAnalizados(),
                informe.conteoPorSeveridad(Severidad.ALTA),
                informe.conteoPorSeveridad(Severidad.MEDIA),
                informe.conteoPorSeveridad(Severidad.BAJA)));

        exportar.setDisable(informe.total() == 0);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String seleccion = filtroSeveridad.getValue();
        if (seleccion == null || "Todas".equals(seleccion)) {
            hallazgosFiltrados.setPredicate(h -> true);
        } else {
            hallazgosFiltrados.setPredicate(h -> h.severidad().etiqueta().equals(seleccion));
        }
    }

    private void exportarInforme() {
        if (informeActual == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe");
        selector.setInitialFileName("informe-" + informeActual.proyecto().id() + ".html");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Pagina HTML", "*.html"));

        File destino = selector.showSaveDialog(exportar.getScene().getWindow());
        if (destino == null) {
            return;
        }
        try {
            new GeneradorInformeHtml().generar(informeActual, destino.toPath());
            resumen.setText("Informe exportado en " + destino.getAbsolutePath());
        } catch (Exception e) {
            resumen.setText("No se pudo exportar el informe: " + e.getMessage());
        }
    }
}
