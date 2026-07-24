package ec.edu.unibe.orquestador.app.vistas;

import ec.edu.unibe.orquestador.adaptadores.checkstyle.AdaptadorCheckstyle;
import ec.edu.unibe.orquestador.adaptadores.eslint.AdaptadorEslint;
import ec.edu.unibe.orquestador.adaptadores.git.AdaptadorGit;
import ec.edu.unibe.orquestador.adaptadores.persistencia.ConfiguracionEntornoArchivo;
import ec.edu.unibe.orquestador.adaptadores.pmd.AdaptadorPmd;
import ec.edu.unibe.orquestador.adaptadores.sonarqube.AdaptadorSonarQube;
import ec.edu.unibe.orquestador.dominio.modelo.ConfiguracionEntorno;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.VerificacionEntorno;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Primera seccion de la aplicacion: registra donde estan instaladas las
 * herramientas y comprueba que respondan antes de iniciar una corrida.
 *
 * <p>Cada herramienta es una tarjeta autocontenida: su campo y su estado de
 * verificacion viven en el mismo lugar, para no tener que cruzar un
 * formulario con una tabla de resultados aparte. La verificacion no es un
 * adorno: detecta la version exacta de cada herramienta, dato que la
 * investigacion exige registrar en la bitacora porque estos productos
 * publican versiones nuevas con frecuencia.</p>
 *
 * <p>Los proyectos de la muestra no se configuran aqui: se descargan solos
 * en la seccion Analisis (ver {@link VistaAnalisis}). Esta pantalla es solo
 * sobre donde estan instaladas las herramientas.</p>
 */
public final class VistaConfiguracion {

    private final ConfiguracionEntorno configuracion;

    private final TextField campoPmd = new TextField();
    private final TextField campoCheckstyle = new TextField();
    private final TextField campoEslint = new TextField();
    private final TextField campoNpm = new TextField();
    private final TextField campoGit = new TextField();
    private final TextField campoSonarScanner = new TextField();
    private final TextField campoDocker = new TextField();
    private final TextField campoSonarUrl = new TextField();
    private final TextField campoSonarToken = new TextField();

    private final Label chipPmd = crearChip();
    private final Label chipCheckstyle = crearChip();
    private final Label chipEslint = crearChip();
    private final Label chipSonar = crearChip();
    private final Label chipGit = crearChip();
    private final Label mensaje = new Label();

    public VistaConfiguracion(ConfiguracionEntorno configuracion) {
        this.configuracion = configuracion;
        // Si ya hay algo cargado (persistido de una sesion anterior) se usa
        // eso; solo se cae al valor por defecto de este equipo en la
        // primera vez que se abre la aplicacion.
        campoPmd.setText(configuracion.ruta(HerramientaSoportada.PMD)
                .orElse("C:\\herramientas\\pmd\\bin\\pmd.bat"));
        campoCheckstyle.setText(configuracion.ruta(HerramientaSoportada.CHECKSTYLE)
                .orElse("C:\\herramientas\\checkstyle\\checkstyle-all.jar"));
        campoEslint.setText(configuracion.ruta(HerramientaSoportada.ESLINT).orElse("eslint"));
        campoNpm.setText(configuracion.rutaNpm());
        campoGit.setText(configuracion.rutaGit());
        campoSonarScanner.setText(configuracion.ruta(HerramientaSoportada.SONARQUBE)
                .orElse("C:\\herramientas\\sonar-scanner\\bin\\sonar-scanner.bat"));
        campoDocker.setText(configuracion.rutaDocker());
        campoSonarUrl.setText(configuracion.urlSonarQube());
        campoSonarToken.setText(configuracion.tokenSonarQube());

        // La configuracion se guarda sola (en memoria y en disco) en cuanto
        // cambia un campo: la seccion Analisis no debe depender de que el
        // usuario haya pulsado "Verificar entorno" primero, y las rutas no
        // deben perderse al cerrar la aplicacion.
        for (TextField campo : List.of(campoPmd, campoCheckstyle, campoEslint, campoNpm, campoGit,
                campoSonarScanner, campoDocker, campoSonarUrl, campoSonarToken)) {
            campo.textProperty().addListener((obs, viejo, nuevo) -> guardarConfiguracion());
        }
        guardarConfiguracion();
    }

    private static Label crearChip() {
        Label chip = new Label("Sin verificar");
        chip.getStyleClass().addAll("chip", "chip-pendiente");
        return chip;
    }

    public Region contenido() {
        Label titulo = new Label("Configuracion de herramientas");
        titulo.getStyleClass().add("titulo-vista");

        Label ayuda = new Label("Cada tarjeta es una herramienta: indique donde esta instalada "
                + "y pulse Verificar entorno para comprobarla. Los proyectos de la muestra no "
                + "se configuran aqui, se descargan solos en la seccion Analisis.");
        ayuda.getStyleClass().add("ayuda");
        ayuda.setWrapText(true);

        Button verificar = new Button("Verificar entorno");
        verificar.getStyleClass().add("boton-principal");
        verificar.setOnAction(e -> verificarEntorno());
        mensaje.getStyleClass().add("ayuda");

        VBox tarjetas = new VBox(14,
                tarjetaPmd(), tarjetaCheckstyle(), tarjetaEslint(), tarjetaSonar(), tarjetaGit());

        VBox caja = new VBox(18, titulo, ayuda, new HBox(12, verificar, mensaje), tarjetas);
        caja.setPadding(new Insets(28, 32, 28, 32));
        caja.getStyleClass().add("panel");

        ScrollPane scroll = new ScrollPane(caja);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-transparente");
        return scroll;
    }

    private Region tarjetaPmd() {
        return tarjeta("PMD", campoConBoton("Ejecutable", campoPmd, true), chipPmd);
    }

    private Region tarjetaCheckstyle() {
        return tarjeta("Checkstyle", campoConBoton("Archivo jar", campoCheckstyle, true), chipCheckstyle);
    }

    private Region tarjetaEslint() {
        VBox campos = new VBox(8,
                campoConBoton("Comando de ESLint", campoEslint, false),
                campoConBoton("Comando de npm", campoNpm, false));
        return tarjeta("ESLint", campos, chipEslint);
    }

    private Region tarjetaSonar() {
        VBox campos = new VBox(8,
                campoConBoton("Comando de sonar-scanner", campoSonarScanner, false),
                campoConBoton("Comando de docker", campoDocker, false),
                campoConBoton("Direccion del servidor", campoSonarUrl, false),
                campoConBoton("Token de acceso", campoSonarToken, false));
        return tarjeta("SonarQube", campos, chipSonar);
    }

    private Region tarjetaGit() {
        return tarjeta("Git (descarga de proyectos)",
                campoConBoton("Comando de git", campoGit, false), chipGit);
    }

    private VBox tarjeta(String nombre, Region campos, Label chip) {
        Label titulo = new Label(nombre);
        titulo.getStyleClass().add("subtitulo");

        VBox caja = new VBox(10, titulo, campos, chip);
        caja.getStyleClass().add("tarjeta");
        return caja;
    }

    private HBox campoConBoton(String etiqueta, TextField campo, boolean permiteExaminar) {
        Label titulo = new Label(etiqueta);
        titulo.setPrefWidth(170);
        titulo.getStyleClass().add("etiqueta-campo");
        HBox.setHgrow(campo, Priority.ALWAYS);

        HBox fila = new HBox(10, titulo, campo);
        if (permiteExaminar) {
            Button examinar = new Button("Examinar...");
            examinar.getStyleClass().add("boton-secundario");
            examinar.setOnAction(e -> elegirCarpeta(campo));
            fila.getChildren().add(examinar);
        }
        return fila;
    }

    private void elegirCarpeta(TextField campo) {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Seleccione la carpeta de la herramienta");
        File carpeta = selector.showDialog(campo.getScene().getWindow());
        if (carpeta != null) {
            campo.setText(carpeta.getAbsolutePath());
        }
    }

    private void actualizarChip(Label chip, boolean disponible, String texto) {
        chip.setText(texto);
        chip.getStyleClass().setAll("chip", disponible ? "chip-disponible" : "chip-no-disponible");
    }

    private void verificarEntorno() {
        guardarConfiguracion();

        VerificacionEntorno pmd = configuracion.ruta(HerramientaSoportada.PMD)
                .map(AdaptadorPmd::new).map(AdaptadorPmd::verificar)
                .orElse(new VerificacionEntorno(HerramientaSoportada.PMD, false, "Ruta no indicada."));
        actualizarChip(chipPmd, pmd.disponible(), pmd.estado() + " - " + pmd.detalle());

        VerificacionEntorno checkstyle = configuracion.ruta(HerramientaSoportada.CHECKSTYLE)
                .map(AdaptadorCheckstyle::new).map(AdaptadorCheckstyle::verificar)
                .orElse(new VerificacionEntorno(HerramientaSoportada.CHECKSTYLE, false, "Ruta no indicada."));
        actualizarChip(chipCheckstyle, checkstyle.disponible(),
                checkstyle.estado() + " - " + checkstyle.detalle());

        VerificacionEntorno eslint = configuracion.ruta(HerramientaSoportada.ESLINT)
                .map(ruta -> new AdaptadorEslint(ruta, configuracion.rutaNpm()))
                .map(AdaptadorEslint::verificar)
                .orElse(new VerificacionEntorno(HerramientaSoportada.ESLINT, false, "Comando no indicado."));
        actualizarChip(chipEslint, eslint.disponible(), eslint.estado() + " - " + eslint.detalle());

        VerificacionEntorno sonar;
        Optional<String> rutaScanner = configuracion.ruta(HerramientaSoportada.SONARQUBE);
        if (rutaScanner.isPresent() && !configuracion.tokenSonarQube().isBlank()) {
            sonar = new AdaptadorSonarQube(rutaScanner.get(), configuracion.rutaDocker(),
                    configuracion.urlSonarQube(), configuracion.tokenSonarQube()).verificar();
        } else {
            sonar = new VerificacionEntorno(HerramientaSoportada.SONARQUBE, false,
                    "Falta el comando del escaner o el token de acceso.");
        }
        actualizarChip(chipSonar, sonar.disponible(), sonar.estado() + " - " + sonar.detalle());

        var git = new AdaptadorGit(configuracion.rutaGit(), AdaptadorGit.carpetaBasePorDefecto())
                .verificar();
        actualizarChip(chipGit, git.disponible(),
                (git.disponible() ? "Disponible" : "No disponible") + " - " + git.detalle());

        int disponibles = 0;
        disponibles += pmd.disponible() ? 1 : 0;
        disponibles += checkstyle.disponible() ? 1 : 0;
        disponibles += eslint.disponible() ? 1 : 0;
        disponibles += sonar.disponible() ? 1 : 0;
        disponibles += git.disponible() ? 1 : 0;
        mensaje.setText(disponibles + " de 5 disponibles.");
    }

    private void guardarConfiguracion() {
        if (!campoPmd.getText().isBlank()) {
            configuracion.definirRuta(HerramientaSoportada.PMD, campoPmd.getText().trim());
        }
        if (!campoCheckstyle.getText().isBlank()) {
            configuracion.definirRuta(HerramientaSoportada.CHECKSTYLE, campoCheckstyle.getText().trim());
        }
        if (!campoEslint.getText().isBlank()) {
            configuracion.definirRuta(HerramientaSoportada.ESLINT, campoEslint.getText().trim());
        }
        if (!campoSonarScanner.getText().isBlank()) {
            configuracion.definirRuta(HerramientaSoportada.SONARQUBE, campoSonarScanner.getText().trim());
        }
        configuracion.definirRutaGit(campoGit.getText().trim());
        configuracion.definirRutaNpm(campoNpm.getText().trim());
        configuracion.definirRutaDocker(campoDocker.getText().trim());
        configuracion.definirUrlSonarQube(campoSonarUrl.getText().trim());
        configuracion.definirTokenSonarQube(campoSonarToken.getText().trim());

        ConfiguracionEntornoArchivo.guardar(configuracion);
    }
}
