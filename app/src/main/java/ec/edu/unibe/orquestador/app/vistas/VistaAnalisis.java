package ec.edu.unibe.orquestador.app.vistas;

import ec.edu.unibe.orquestador.adaptadores.OrquestadorAnalisis;
import ec.edu.unibe.orquestador.adaptadores.checkstyle.AdaptadorCheckstyle;
import ec.edu.unibe.orquestador.adaptadores.eslint.AdaptadorEslint;
import ec.edu.unibe.orquestador.adaptadores.git.AdaptadorGit;
import ec.edu.unibe.orquestador.adaptadores.pmd.AdaptadorPmd;
import ec.edu.unibe.orquestador.adaptadores.sonarqube.AdaptadorSonarQube;
import ec.edu.unibe.orquestador.dominio.contratos.AnalizadorEstatico;
import ec.edu.unibe.orquestador.dominio.contratos.SolicitudAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.CalculadoraConcordancia;
import ec.edu.unibe.orquestador.dominio.modelo.CatalogoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.CatalogoPruebas;
import ec.edu.unibe.orquestador.dominio.modelo.ConfiguracionEntorno;
import ec.edu.unibe.orquestador.dominio.modelo.Dimension;
import ec.edu.unibe.orquestador.dominio.modelo.FuenteDato;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Herramienta;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.InformeProyecto;
import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoAnalizado;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.RegistroMedicion;
import ec.edu.unibe.orquestador.dominio.modelo.ResumenConcordancia;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Segunda seccion: selecciona un proyecto del banco de pruebas, lo descarga
 * automaticamente con git y lo analiza con todas las herramientas aplicables
 * a su lenguaje en una sola accion.
 *
 * <p>El usuario ya no clona el repositorio a mano ni elige una herramienta a
 * la vez: la descarga y la deteccion del commit exacto quedan a cargo de
 * {@link AdaptadorGit}, y {@link OrquestadorAnalisis} corre todas las
 * herramientas disponibles sobre el mismo commit. Al terminar, el resultado
 * conjunto y la concordancia entre herramientas se muestran en
 * {@link VistaTablero}.</p>
 */
public final class VistaAnalisis {

    /**
     * Numero de corridas por par herramienta-proyecto que exige el marco
     * metodologico, para poder calcular la variabilidad (desviacion
     * estandar y coeficiente de variacion) de cada indicador.
     */
    private static final int CORRIDAS_TOTALES = 3;

    private final ConfiguracionEntorno configuracion;
    private final MatrizComparativa matriz;
    private final VistaTablero vistaTablero;
    private final Runnable irATablero;
    private final String nombreEvaluador;

    private final ComboBox<ProyectoMuestra> selectorProyecto = new ComboBox<>();
    private final CheckBox incluirPruebas =
            new CheckBox("Incluir proyectos de prueba de desarrollo (no son la muestra de la tesis)");
    private final Label etiquetaHerramientas = new Label();
    private final Label etiquetaCarpeta = new Label();
    private final Label etiquetaCommit = new Label();
    private final TextArea bitacora = new TextArea();
    private final ProgressIndicator progreso = new ProgressIndicator();
    private final Button descargar = new Button("Descargar proyecto");
    private final Button actualizar = new Button("Actualizar (volver a descargar)");
    private final Button ejecutar = new Button("Analizar con todas las herramientas");

    private boolean tareaEnCurso = false;

    public VistaAnalisis(ConfiguracionEntorno configuracion, MatrizComparativa matriz,
                         VistaTablero vistaTablero, Runnable irATablero, String nombreEvaluador) {
        this.configuracion = configuracion;
        this.matriz = matriz;
        this.vistaTablero = vistaTablero;
        this.irATablero = irATablero;
        this.nombreEvaluador = nombreEvaluador;
        prepararControles();
    }

    private void prepararControles() {
        selectorProyecto.setPrefWidth(320);
        selectorProyecto.valueProperty().addListener((obs, previo, actual) -> {
            refrescarHerramientasAplicables();
            refrescarEstadoDescarga();
        });
        incluirPruebas.setOnAction(e -> refrescarCatalogoProyectos());
        refrescarCatalogoProyectos();

        etiquetaHerramientas.getStyleClass().add("ayuda");
        etiquetaCarpeta.getStyleClass().add("ayuda");
        etiquetaCommit.getStyleClass().add("ayuda");
        refrescarHerramientasAplicables();

        progreso.setVisible(false);
        progreso.setPrefSize(24, 24);

        descargar.getStyleClass().add("boton-principal");
        descargar.setOnAction(e -> descargarProyecto(false));

        actualizar.getStyleClass().add("boton-secundario");
        actualizar.setOnAction(e -> confirmarActualizacion());

        ejecutar.getStyleClass().add("boton-principal");
        ejecutar.setOnAction(e -> iniciarAnalisis());

        refrescarEstadoDescarga();
    }

    /**
     * Reconstruye la lista del selector: la muestra oficial siempre esta, y
     * los proyectos de prueba de desarrollo se agregan solo si el usuario
     * marca la casilla. Intenta conservar la seleccion actual.
     */
    private void refrescarCatalogoProyectos() {
        ProyectoMuestra actual = selectorProyecto.getValue();
        selectorProyecto.getItems().setAll(CatalogoMuestra.proyectos());
        if (incluirPruebas.isSelected()) {
            selectorProyecto.getItems().addAll(CatalogoPruebas.proyectos());
        }
        if (actual != null && selectorProyecto.getItems().contains(actual)) {
            selectorProyecto.setValue(actual);
        } else {
            selectorProyecto.getSelectionModel().selectFirst();
        }
    }

    /** Actualiza que herramientas aplican al lenguaje del proyecto elegido. */
    private void refrescarHerramientasAplicables() {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        if (proyecto == null) {
            etiquetaHerramientas.setText("-");
            return;
        }
        // Se muestran las 4 herramientas siempre, marcando las que no
        // aplican al lenguaje elegido, para que no parezca que faltan o que
        // hay un error de configuracion cuando en realidad es la
        // correspondencia fija herramienta-lenguaje del marco metodologico.
        String texto = Arrays.stream(HerramientaSoportada.values())
                .map(h -> h.aplicaA(proyecto.lenguaje()) ? h.nombre() : h.nombre() + " (no aplica)")
                .collect(Collectors.joining(", "));
        etiquetaHerramientas.setText(texto);
    }

    public Region contenido() {
        Label titulo = new Label("Analisis de un proyecto del banco de pruebas");
        titulo.getStyleClass().add("titulo-vista");

        Label ayuda = new Label("Elija un proyecto y descarguelo con un clic: la aplicacion "
                + "clona el repositorio y detecta el commit exacto, sin buscar carpetas a "
                + "mano. Al analizar, se corren todas las herramientas aplicables al lenguaje "
                + "del proyecto sobre ese mismo commit, y el resultado conjunto aparece en la "
                + "seccion Tablero.");
        ayuda.getStyleClass().add("ayuda");
        ayuda.setWrapText(true);

        incluirPruebas.getStyleClass().add("ayuda");

        VBox formulario = new VBox(10,
                fila("Proyecto", selectorProyecto),
                incluirPruebas,
                fila("Herramientas aplicables", etiquetaHerramientas));

        VBox tarjetaDescarga = new VBox(8,
                fila("Carpeta local", etiquetaCarpeta),
                fila("Commit analizado", etiquetaCommit));
        tarjetaDescarga.getStyleClass().add("tarjeta");

        HBox accionesDescarga = new HBox(12, descargar, actualizar);
        HBox acciones = new HBox(12, ejecutar, progreso);

        Label tituloBitacora = new Label("Bitacora de la corrida");
        tituloBitacora.getStyleClass().add("subtitulo");

        VBox caja = new VBox(16, titulo, ayuda, formulario, tarjetaDescarga, accionesDescarga,
                acciones, tituloBitacora, bitacora);
        caja.setPadding(new Insets(28, 32, 28, 32));
        caja.getStyleClass().add("panel");
        VBox.setVgrow(bitacora, Priority.ALWAYS);
        return caja;
    }

    private HBox fila(String etiqueta, Region control) {
        Label titulo = new Label(etiqueta);
        titulo.setPrefWidth(180);
        titulo.getStyleClass().add("etiqueta-campo");
        return new HBox(10, titulo, control);
    }

    private AdaptadorGit crearAdaptadorGit() {
        return new AdaptadorGit(configuracion.rutaGit(), AdaptadorGit.carpetaBasePorDefecto());
    }

    /** Refresca las etiquetas de carpeta/commit segun si el proyecto ya esta descargado. */
    private void refrescarEstadoDescarga() {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        if (proyecto == null) {
            return;
        }
        AdaptadorGit git = crearAdaptadorGit();
        if (git.estaDescargado(proyecto)) {
            Path carpeta = git.carpetaLocal(proyecto);
            configuracion.definirCarpetaProyecto(proyecto.id(), carpeta);
            etiquetaCarpeta.setText(carpeta.toString());
            etiquetaCommit.setText(consultarCommit(git, proyecto).orElse("no se pudo detectar"));
        } else {
            etiquetaCarpeta.setText("Aun no descargado.");
            etiquetaCommit.setText("-");
        }
        actualizarControles();
    }

    private Optional<String> consultarCommit(AdaptadorGit git, ProyectoMuestra proyecto) {
        try {
            return Optional.of(git.commitActual(proyecto));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void confirmarActualizacion() {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        if (proyecto == null) {
            return;
        }
        Alert confirmacion = new Alert(AlertType.CONFIRMATION,
                "Esto borra la copia local de " + proyecto.nombre()
                        + " y la vuelve a descargar. ¿Continuar?", ButtonType.YES, ButtonType.NO);
        confirmacion.setTitle("Actualizar proyecto");
        confirmacion.setHeaderText(null);
        confirmacion.showAndWait().filter(boton -> boton == ButtonType.YES)
                .ifPresent(boton -> descargarProyecto(true));
    }

    private void descargarProyecto(boolean forzarActualizacion) {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        if (proyecto == null) {
            registrar("Seleccione un proyecto.");
            return;
        }

        AdaptadorGit git = crearAdaptadorGit();
        registrar((forzarActualizacion ? "Actualizando " : "Descargando ")
                + proyecto.nombre() + "...");
        iniciarTarea();

        Task<Path> tarea = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return forzarActualizacion ? git.actualizar(proyecto) : git.descargar(proyecto);
            }
        };

        tarea.setOnSucceeded(e -> {
            Path carpeta = tarea.getValue();
            configuracion.definirCarpetaProyecto(proyecto.id(), carpeta);
            etiquetaCarpeta.setText(carpeta.toString());
            etiquetaCommit.setText(consultarCommit(git, proyecto).orElse("no se pudo detectar"));
            registrar("Proyecto listo en " + carpeta + ".");
            finalizarTarea();
        });

        tarea.setOnFailed(e -> {
            Throwable causa = tarea.getException();
            registrar("La descarga fallo: "
                    + (causa == null ? "causa desconocida" : causa.getMessage()));
            finalizarTarea();
        });

        Thread hilo = new Thread(tarea, "descarga-git");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void iniciarAnalisis() {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        if (proyecto == null) {
            registrar("Seleccione un proyecto.");
            return;
        }

        Optional<Path> carpeta = configuracion.carpetaProyecto(proyecto.id());
        if (carpeta.isEmpty()) {
            registrar("Descargue el proyecto antes de iniciar el analisis.");
            return;
        }

        String commit = consultarCommit(crearAdaptadorGit(), proyecto).orElse("");

        List<AnalizadorEstatico> listos = new ArrayList<>();
        List<OrquestadorAnalisis.ResultadoFallido> sinAdaptador = new ArrayList<>();
        for (HerramientaSoportada herramienta : CatalogoMuestra.herramientasPara(proyecto)) {
            AnalizadorEstatico adaptador = crearAdaptador(herramienta);
            if (adaptador == null) {
                sinAdaptador.add(new OrquestadorAnalisis.ResultadoFallido(herramienta,
                        "Sin ruta configurada, o adaptador aun no disponible (ver Herramientas)."));
            } else {
                listos.add(adaptador);
            }
        }

        if (listos.isEmpty()) {
            registrar("Ninguna herramienta aplicable tiene su ruta configurada. Revise Herramientas.");
            return;
        }

        ejecutarEnSegundoPlano(listos, proyecto, carpeta.get(), commit, sinAdaptador);
    }

    private AnalizadorEstatico crearAdaptador(HerramientaSoportada herramienta) {
        return switch (herramienta) {
            case PMD -> configuracion.ruta(HerramientaSoportada.PMD)
                    .map(AdaptadorPmd::new).orElse(null);
            case CHECKSTYLE -> configuracion.ruta(HerramientaSoportada.CHECKSTYLE)
                    .map(AdaptadorCheckstyle::new).orElse(null);
            case ESLINT -> configuracion.ruta(HerramientaSoportada.ESLINT)
                    .map(ruta -> new AdaptadorEslint(ruta, configuracion.rutaNpm())).orElse(null);
            case SONARQUBE -> crearAdaptadorSonarQube();
        };
    }

    private AnalizadorEstatico crearAdaptadorSonarQube() {
        if (configuracion.tokenSonarQube().isBlank()) {
            return null;
        }
        return configuracion.ruta(HerramientaSoportada.SONARQUBE)
                .map(ruta -> new AdaptadorSonarQube(ruta, configuracion.rutaDocker(),
                        configuracion.urlSonarQube(), configuracion.tokenSonarQube()))
                .orElse(null);
    }

    private void ejecutarEnSegundoPlano(List<AnalizadorEstatico> adaptadores,
                                        ProyectoMuestra proyecto,
                                        Path carpeta,
                                        String commit,
                                        List<OrquestadorAnalisis.ResultadoFallido> sinAdaptador) {
        registrar("Analizando " + proyecto.nombre() + " con " + adaptadores.size()
                + " herramienta(s), " + CORRIDAS_TOTALES + " corridas (evaluador: "
                + nombreEvaluador + ")...");
        iniciarTarea();

        Task<List<OrquestadorAnalisis.ResultadoConjunto>> tarea = new Task<>() {
            @Override
            protected List<OrquestadorAnalisis.ResultadoConjunto> call() {
                List<OrquestadorAnalisis.ResultadoConjunto> porCorrida = new ArrayList<>();
                for (int corrida = 1; corrida <= CORRIDAS_TOTALES; corrida++) {
                    registrar("Corrida " + corrida + " de " + CORRIDAS_TOTALES + "...");
                    SolicitudAnalisis solicitud = new SolicitudAnalisis(proyecto, carpeta, commit, corrida);
                    porCorrida.add(new OrquestadorAnalisis().ejecutarTodas(adaptadores, solicitud));
                }
                return porCorrida;
            }
        };

        tarea.setOnSucceeded(e -> {
            manejarResultadoConjunto(proyecto, carpeta, commit, tarea.getValue(), sinAdaptador);
            finalizarTarea();
            irATablero.run();
        });

        tarea.setOnFailed(e -> {
            Throwable causa = tarea.getException();
            registrar("El analisis conjunto fallo: "
                    + (causa == null ? "causa desconocida" : causa.getMessage()));
            finalizarTarea();
        });

        Thread hilo = new Thread(tarea, "analisis-conjunto");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Agrega a la matriz las mediciones de las {@value #CORRIDAS_TOTALES}
     * corridas (asi queda el dato completo para la estadistica de
     * variabilidad), y arma el tablero comparativo con la ultima corrida:
     * los hallazgos y la concordancia no varian de forma relevante entre
     * corridas identicas, a diferencia del tiempo, asi que no hace falta
     * triplicarlos.
     */
    private void manejarResultadoConjunto(ProyectoMuestra proyecto, Path carpeta, String commit,
                                          List<OrquestadorAnalisis.ResultadoConjunto> porCorrida,
                                          List<OrquestadorAnalisis.ResultadoFallido> sinAdaptador) {

        for (var conjuntoDeUnaCorrida : porCorrida) {
            for (var corrida : conjuntoDeUnaCorrida.exitosos()) {
                matriz.agregarTodos(corrida.resultado().mediciones());
            }
        }

        OrquestadorAnalisis.ResultadoConjunto ultimaCorrida = porCorrida.get(porCorrida.size() - 1);
        SolicitudAnalisis solicitudUltimaCorrida =
                new SolicitudAnalisis(proyecto, carpeta, commit, CORRIDAS_TOTALES);

        Map<HerramientaSoportada, List<Hallazgo>> hallazgosPorHerramienta = new LinkedHashMap<>();
        Map<HerramientaSoportada, InformeProyecto> informes = new LinkedHashMap<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (var corrida : ultimaCorrida.exitosos()) {
            hallazgosPorHerramienta.put(corrida.herramienta(), corrida.resultado().hallazgos());
            informes.put(corrida.herramienta(), new InformeProyecto(
                    proyecto,
                    new Herramienta(corrida.herramienta().nombre(), "detectada en la corrida"),
                    ahora,
                    corrida.resultado().hallazgos(),
                    corrida.resultado().archivosAnalizados()));
        }

        ResumenConcordancia concordancia = CalculadoraConcordancia.calcular(hallazgosPorHerramienta);
        registrarMedicionesDeConcordancia(solicitudUltimaCorrida, ultimaCorrida, concordancia, ahora);

        List<OrquestadorAnalisis.ResultadoFallido> todosFallidos = new ArrayList<>(sinAdaptador);
        todosFallidos.addAll(ultimaCorrida.fallidos());

        vistaTablero.mostrar(proyecto,
                new OrquestadorAnalisis.ResultadoConjunto(ultimaCorrida.exitosos(), todosFallidos),
                informes, concordancia);

        registrar(CORRIDAS_TOTALES + " corridas completas: " + ultimaCorrida.exitosos().size()
                + " herramienta(s) exitosa(s), " + todosFallidos.size() + " sin resultado.");
        registrar(concordancia.ubicacionesEnComun() + " de " + concordancia.totalUbicaciones()
                + " ubicaciones coincidieron entre dos o mas herramientas.");
        registrar("Revise la seccion Tablero para el detalle y la variabilidad entre corridas.");
    }

    /**
     * Deja en la matriz, por cada herramienta exitosa, cuantos de sus
     * hallazgos quedaron en ubicaciones que ninguna otra herramienta senalo.
     * Es la unica forma de concordancia que se puede atribuir a una sola
     * herramienta con el esquema fijo de {@link RegistroMedicion}.
     */
    private void registrarMedicionesDeConcordancia(SolicitudAnalisis solicitud,
                                                    OrquestadorAnalisis.ResultadoConjunto conjunto,
                                                    ResumenConcordancia concordancia,
                                                    LocalDateTime ahora) {
        ProyectoAnalizado proyectoAnalizado = new ProyectoAnalizado(
                solicitud.proyecto().nombre(), solicitud.proyecto().lenguaje(), solicitud.commit());

        for (var corrida : conjunto.exitosos()) {
            Herramienta herramientaMedida = new Herramienta(
                    corrida.herramienta().nombre(), "detectada en la corrida");
            int exclusivos = concordancia.hallazgosExclusivos().getOrDefault(corrida.herramienta(), 0);

            matriz.agregar(new RegistroMedicion(ahora, herramientaMedida, proyectoAnalizado,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Hallazgos en ubicaciones exclusivas", exclusivos, "conteo",
                    FuenteDato.REPORTE_HERRAMIENTA));
        }
    }

    private void iniciarTarea() {
        tareaEnCurso = true;
        actualizarControles();
    }

    private void finalizarTarea() {
        tareaEnCurso = false;
        actualizarControles();
    }

    /** Habilita o deshabilita los controles segun si hay una tarea en curso y si el proyecto ya esta descargado. */
    private void actualizarControles() {
        boolean descargado = proyectoActualDescargado();
        descargar.setDisable(tareaEnCurso);
        actualizar.setDisable(tareaEnCurso || !descargado);
        selectorProyecto.setDisable(tareaEnCurso);
        ejecutar.setDisable(tareaEnCurso || !descargado);
        progreso.setVisible(tareaEnCurso);
    }

    private boolean proyectoActualDescargado() {
        ProyectoMuestra proyecto = selectorProyecto.getValue();
        return proyecto != null && configuracion.carpetaProyecto(proyecto.id()).isPresent();
    }

    private void registrar(String texto) {
        Platform.runLater(() -> bitacora.appendText(
                "[" + LocalDateTime.now().toLocalTime().withNano(0) + "] " + texto + "\n"));
    }
}
