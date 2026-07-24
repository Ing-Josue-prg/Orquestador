package ec.edu.unibe.orquestador.app;

import ec.edu.unibe.orquestador.adaptadores.persistencia.ConfiguracionEntornoArchivo;
import ec.edu.unibe.orquestador.adaptadores.seguridad.RepositorioEvaluadoresArchivo;
import ec.edu.unibe.orquestador.app.vistas.VistaAnalisis;
import ec.edu.unibe.orquestador.app.vistas.VistaConfiguracion;
import ec.edu.unibe.orquestador.app.vistas.VistaInforme;
import ec.edu.unibe.orquestador.app.vistas.VistaLogin;
import ec.edu.unibe.orquestador.app.vistas.VistaTablero;
import ec.edu.unibe.orquestador.dominio.contratos.RepositorioEvaluadores;
import ec.edu.unibe.orquestador.dominio.modelo.ConfiguracionEntorno;
import ec.edu.unibe.orquestador.dominio.modelo.Evaluador;
import ec.edu.unibe.orquestador.dominio.modelo.MatrizComparativa;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Punto de entrada de la interfaz de escritorio.
 *
 * <p>Antes de mostrar cualquier seccion, exige iniciar sesion o registrarse
 * ({@link VistaLogin}): cada corrida queda asociada a quien la ejecuto. Ya
 * autenticado, organiza la aplicacion en cuatro secciones que siguen el
 * flujo de trabajo de la investigacion: configurar las herramientas,
 * ejecutar el analisis conjunto, revisar el detalle de una herramienta y
 * comparar el tablero de concordancia. La configuracion del entorno se
 * carga y se guarda en disco para no perderse entre sesiones.</p>
 */
public class AplicacionOrquestador extends Application {

    private final ConfiguracionEntorno configuracion = new ConfiguracionEntorno();
    private final MatrizComparativa matriz = new MatrizComparativa();
    private final RepositorioEvaluadores repositorioEvaluadores =
            new RepositorioEvaluadoresArchivo(RepositorioEvaluadoresArchivo.archivoPorDefecto());

    private Scene escena;
    private BorderPane raiz;
    private Evaluador evaluadorActual;
    private VistaConfiguracion vistaConfiguracion;
    private VistaAnalisis vistaAnalisis;
    private VistaInforme vistaInforme;
    private VistaTablero vistaTablero;
    private ToggleButton informeBoton;
    private ToggleButton tableroBoton;

    @Override
    public void start(Stage escenario) {
        ConfiguracionEntornoArchivo.cargarEn(configuracion);

        VistaLogin vistaLogin = new VistaLogin(repositorioEvaluadores, this::iniciarSesion);
        escena = new Scene(vistaLogin.contenido(), 1180, 720);
        escena.getStylesheets().add(getClass().getResource("/estilos.css").toExternalForm());

        escenario.setTitle("Orquestador de Evaluacion de Calidad de Software");
        escenario.setScene(escena);
        escenario.show();
    }

    private void iniciarSesion(Evaluador evaluador) {
        this.evaluadorActual = evaluador;
        construirAplicacionPrincipal();
        escena.setRoot(raiz);
    }

    private void cerrarSesion() {
        this.evaluadorActual = null;
        VistaLogin vistaLogin = new VistaLogin(repositorioEvaluadores, this::iniciarSesion);
        escena.setRoot(vistaLogin.contenido());
    }

    private void construirAplicacionPrincipal() {
        vistaInforme = new VistaInforme();
        vistaTablero = new VistaTablero(matriz, vistaInforme, this::mostrarInforme);
        vistaAnalisis = new VistaAnalisis(
                configuracion, matriz, vistaTablero, this::mostrarTablero, evaluadorActual.nombreUsuario());
        vistaConfiguracion = new VistaConfiguracion(configuracion);

        raiz = new BorderPane();
        raiz.setLeft(construirMenu());
        raiz.setCenter(vistaConfiguracion.contenido());
    }

    private void mostrarInforme() {
        raiz.setCenter(vistaInforme.contenido());
        informeBoton.setSelected(true);
    }

    private void mostrarTablero() {
        raiz.setCenter(vistaTablero.contenido());
        tableroBoton.setSelected(true);
    }

    private VBox construirMenu() {
        Label titulo = new Label("Orquestador");
        titulo.getStyleClass().add("marca");

        Label subtitulo = new Label("Evaluacion de calidad\nde software");
        subtitulo.getStyleClass().add("marca-sub");

        ToggleGroup grupo = new ToggleGroup();
        ToggleButton configuracionBoton = crearOpcion("1. Herramientas", grupo, true);
        ToggleButton analisisBoton = crearOpcion("2. Analisis", grupo, false);
        informeBoton = crearOpcion("3. Informe", grupo, false);
        tableroBoton = crearOpcion("4. Tablero", grupo, false);

        configuracionBoton.setOnAction(e -> raiz.setCenter(vistaConfiguracion.contenido()));
        analisisBoton.setOnAction(e -> raiz.setCenter(vistaAnalisis.contenido()));
        informeBoton.setOnAction(e -> raiz.setCenter(vistaInforme.contenido()));
        tableroBoton.setOnAction(e -> raiz.setCenter(vistaTablero.contenido()));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Label sesion = new Label("Sesion: " + evaluadorActual.nombreUsuario());
        sesion.getStyleClass().add("pie-menu");

        Hyperlink cerrarSesion = new Hyperlink("Cerrar sesion");
        cerrarSesion.getStyleClass().add("enlace-menu");
        cerrarSesion.setOnAction(e -> cerrarSesion());

        Label pie = new Label("UNIB.E\nIngenieria de Software");
        pie.getStyleClass().add("pie-menu");

        VBox menu = new VBox(titulo, subtitulo, configuracionBoton, analisisBoton,
                informeBoton, tableroBoton, espacio, sesion, cerrarSesion, pie);
        menu.getStyleClass().add("menu");
        menu.setPadding(new Insets(24, 18, 18, 18));
        menu.setSpacing(6);
        menu.setPrefWidth(230);
        menu.setAlignment(Pos.TOP_LEFT);
        return menu;
    }

    private ToggleButton crearOpcion(String texto, ToggleGroup grupo, boolean seleccionado) {
        ToggleButton boton = new ToggleButton(texto);
        boton.setToggleGroup(grupo);
        boton.setSelected(seleccionado);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.getStyleClass().add("opcion-menu");
        return boton;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
