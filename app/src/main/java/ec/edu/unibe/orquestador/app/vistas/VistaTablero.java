package ec.edu.unibe.orquestador.app.vistas;

import ec.edu.unibe.orquestador.adaptadores.OrquestadorAnalisis;
import ec.edu.unibe.orquestador.adaptadores.informe.GeneradorInformeComparativoHtml;
import ec.edu.unibe.orquestador.adaptadores.matriz.ExportadorMatrizCsv;
import ec.edu.unibe.orquestador.dominio.contratos.ResultadoAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.CalculadoraDescriptiva;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.InformeProyecto;
import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.ResumenConcordancia;
import ec.edu.unibe.orquestador.dominio.modelo.ResumenEstadistico;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Cuarta seccion: el tablero comparativo de una corrida conjunta. Muestra
 * que encontro cada herramienta por separado ("como analizo la estructura
 * del codigo") y un resumen agregado de concordancia entre ellas ("que tan
 * de acuerdo estuvieron").
 *
 * <p>No declara ninguna herramienta ganadora: eso exigiria un criterio de
 * verdad que esta investigacion no tiene. Lo que muestra es evidencia
 * observable — cuanto encontro cada una, en cuanto tiempo, y en que
 * ubicaciones (archivo + linea) coincidieron o no coincidieron.</p>
 */
public final class VistaTablero {

    private final MatrizComparativa matriz;
    private final VistaInforme vistaInforme;
    private final Runnable irAInforme;

    private final Label encabezado = new Label("Aun no hay un analisis conjunto.");
    private final VBox contenedorExitosos = new VBox(12);
    private final VBox contenedorFallidos = new VBox(12);
    private final VBox contenedorConcordancia = new VBox(8);
    private final Button exportar = new Button("Exportar informe comparativo");
    private final Button exportarMatriz = new Button("Exportar matriz completa (CSV)");
    private final Label mensajeExportar = new Label();

    private ProyectoMuestra ultimoProyecto;
    private OrquestadorAnalisis.ResultadoConjunto ultimosResultados;
    private ResumenConcordancia ultimaConcordancia;

    public VistaTablero(MatrizComparativa matriz, VistaInforme vistaInforme, Runnable irAInforme) {
        this.matriz = matriz;
        this.vistaInforme = vistaInforme;
        this.irAInforme = irAInforme;
        exportar.getStyleClass().add("boton-principal");
        exportar.setDisable(true);
        exportar.setOnAction(e -> exportarInformeComparativo());
        exportarMatriz.getStyleClass().add("boton-secundario");
        exportarMatriz.setDisable(true);
        exportarMatriz.setOnAction(e -> exportarMatrizCompleta());
        mensajeExportar.getStyleClass().add("ayuda");
    }

    public Region contenido() {
        Label titulo = new Label("Tablero comparativo");
        titulo.getStyleClass().add("titulo-vista");

        Label ayuda = new Label("Resume la corrida conjunta: que encontro cada herramienta, "
                + "cuales no pudieron correr, y en que ubicaciones del codigo coincidieron o "
                + "no coincidieron entre si.");
        ayuda.getStyleClass().add("ayuda");
        ayuda.setWrapText(true);

        encabezado.getStyleClass().add("subtitulo");

        Label tituloResultados = new Label("Resultado por herramienta");
        tituloResultados.getStyleClass().add("subtitulo");

        Label tituloConcordancia = new Label("Concordancia entre herramientas");
        tituloConcordancia.getStyleClass().add("subtitulo");
        contenedorConcordancia.getStyleClass().add("tarjeta");

        VBox caja = new VBox(16, titulo, ayuda, encabezado,
                tituloResultados, contenedorExitosos, contenedorFallidos,
                tituloConcordancia, contenedorConcordancia,
                new HBox(12, exportar, exportarMatriz, mensajeExportar));
        caja.setPadding(new Insets(28, 32, 28, 32));
        caja.getStyleClass().add("panel");

        ScrollPane scroll = new ScrollPane(caja);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    /** Carga en el tablero los resultados de la ultima corrida conjunta. */
    public void mostrar(ProyectoMuestra proyecto,
                         OrquestadorAnalisis.ResultadoConjunto resultados,
                         Map<HerramientaSoportada, InformeProyecto> informesPorHerramienta,
                         ResumenConcordancia concordancia) {

        encabezado.setText("Ultimo analisis conjunto: " + proyecto.nombre());

        contenedorExitosos.getChildren().setAll(
                resultados.exitosos().stream()
                        .map(r -> tarjetaExitosa(proyecto, r, informesPorHerramienta.get(r.herramienta())))
                        .toList());

        contenedorFallidos.getChildren().setAll(
                resultados.fallidos().stream().map(this::tarjetaFallida).toList());

        contenedorConcordancia.getChildren().setAll(
                filasConcordancia(concordancia, resultados.exitosos().size()));

        this.ultimoProyecto = proyecto;
        this.ultimosResultados = resultados;
        this.ultimaConcordancia = concordancia;
        mensajeExportar.setText("");
        exportar.setDisable(false);
        exportarMatriz.setDisable(matriz.cantidad() == 0);
    }

    private void exportarInformeComparativo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar informe comparativo");
        selector.setInitialFileName("informe-comparativo-" + ultimoProyecto.id() + ".html");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Pagina HTML", "*.html"));

        File destino = selector.showSaveDialog(exportar.getScene().getWindow());
        if (destino == null) {
            return;
        }
        try {
            new GeneradorInformeComparativoHtml().generar(
                    ultimoProyecto, ultimosResultados, ultimaConcordancia, destino.toPath());
            mensajeExportar.setText("Informe exportado en " + destino.getAbsolutePath());
        } catch (Exception e) {
            mensajeExportar.setText("No se pudo exportar el informe: " + e.getMessage());
        }
    }

    private void exportarMatrizCompleta() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar matriz comparativa");
        selector.setInitialFileName("matriz-comparativa.csv");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));

        File destino = selector.showSaveDialog(exportarMatriz.getScene().getWindow());
        if (destino == null) {
            return;
        }
        try {
            new ExportadorMatrizCsv().exportar(matriz, destino.toPath());
            mensajeExportar.setText("Matriz exportada en " + destino.getAbsolutePath()
                    + " (" + matriz.cantidad() + " mediciones).");
        } catch (Exception e) {
            mensajeExportar.setText("No se pudo exportar la matriz: " + e.getMessage());
        }
    }

    private Region tarjetaExitosa(ProyectoMuestra proyecto, OrquestadorAnalisis.ResultadoCorrida corrida,
                                   InformeProyecto informe) {
        ResultadoAnalisis resultado = corrida.resultado();

        Label nombre = new Label(corrida.herramienta().nombre());
        nombre.getStyleClass().add("subtitulo");

        Label detalle = new Label(resultado.hallazgos().size() + " hallazgos en "
                + resultado.archivosAnalizados() + " archivos, en "
                + String.format("%.2f", resultado.duracion().toMillis() / 1000.0) + " s.");
        detalle.getStyleClass().add("ayuda");

        Label variabilidad = new Label(textoVariabilidad(proyecto, corrida.herramienta()));
        variabilidad.getStyleClass().add("ayuda");
        variabilidad.setWrapText(true);

        Button verDetalle = new Button("Ver detalle");
        verDetalle.getStyleClass().add("boton-secundario");
        verDetalle.setOnAction(e -> {
            vistaInforme.mostrar(informe);
            irAInforme.run();
        });

        VBox caja = new VBox(6, nombre, detalle, variabilidad, verDetalle);
        caja.getStyleClass().add("tarjeta");
        return caja;
    }

    /**
     * Resume la variabilidad del tiempo de analisis de una herramienta
     * sobre las corridas ya acumuladas en la matriz para este proyecto,
     * con {@link CalculadoraDescriptiva} — el calculo que el dominio ya
     * tenia listo desde el Incremento 1 y que este tablero es el primero
     * en usar.
     */
    private String textoVariabilidad(ProyectoMuestra proyecto, HerramientaSoportada herramienta) {
        List<Double> tiempos = matriz.filtrar(herramienta.nombre(), proyecto.nombre(), "Tiempo de analisis")
                .stream().map(r -> r.valor()).toList();
        if (tiempos.isEmpty()) {
            return "";
        }
        ResumenEstadistico resumen = CalculadoraDescriptiva.resumir(tiempos);
        return String.format("Tiempo: promedio %.2f s, CV %.1f %% (%d corrida%s)",
                resumen.media(), resumen.coeficienteVariacion(), resumen.cantidad(),
                resumen.cantidad() == 1 ? "" : "s");
    }

    private Region tarjetaFallida(OrquestadorAnalisis.ResultadoFallido fallido) {
        Label nombre = new Label(fallido.herramienta().nombre() + " - no disponible");
        nombre.getStyleClass().add("subtitulo");

        Label motivo = new Label(fallido.motivo());
        motivo.getStyleClass().add("ayuda");
        motivo.setWrapText(true);

        VBox caja = new VBox(6, nombre, motivo);
        caja.getStyleClass().add("tarjeta");
        return caja;
    }

    private List<Region> filasConcordancia(ResumenConcordancia concordancia, int herramientasExitosas) {
        List<Region> filas = new ArrayList<>();

        Label resumen = new Label();
        resumen.getStyleClass().add("ayuda");
        resumen.setWrapText(true);
        if (herramientasExitosas < 2) {
            // Con 0 o 1 herramienta exitosa, "0 de N coincidieron" es cierto
            // pero enganoso: no es que las herramientas hayan discrepado, es
            // que no hubo una segunda herramienta con la que comparar.
            resumen.setText("La concordancia necesita al menos 2 herramientas exitosas en la misma "
                    + "corrida; esta vez hubo " + herramientasExitosas + ". Revise las tarjetas de "
                    + "arriba para ver por que las demas no corrieron.");
        } else {
            resumen.setText(concordancia.ubicacionesEnComun() + " de "
                    + concordancia.totalUbicaciones() + " ubicaciones (archivo + linea) fueron "
                    + "senaladas por dos o mas herramientas.");
        }
        filas.add(resumen);

        HBox chips = new HBox(10);
        concordancia.hallazgosExclusivos().forEach((herramienta, cantidad) -> {
            Label chip = new Label(herramienta.nombre() + ": " + cantidad + " exclusivos");
            chip.getStyleClass().addAll("chip", "chip-baja");
            chips.getChildren().add(chip);
        });
        filas.add(chips);

        return filas;
    }
}
