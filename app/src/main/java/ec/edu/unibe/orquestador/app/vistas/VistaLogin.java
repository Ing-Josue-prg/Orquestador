package ec.edu.unibe.orquestador.app.vistas;

import ec.edu.unibe.orquestador.dominio.contratos.RepositorioEvaluadores;
import ec.edu.unibe.orquestador.dominio.modelo.Evaluador;
import ec.edu.unibe.orquestador.dominio.modelo.GestorContrasenas;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Pantalla inicial de la aplicacion: inicio de sesion y registro del
 * evaluador, en un solo formulario que alterna de modo con el enlace
 * inferior ("Iniciar sesion" / "Crear cuenta"). Ninguna otra seccion es
 * visible hasta que se autentique.
 *
 * <p>Deliberadamente simple: sin roles ni recuperacion de contrasena. Es una
 * aplicacion de escritorio para el equipo del propio investigador, no un
 * producto multiusuario expuesto en red; el login existe para que cada
 * corrida quede asociada a quien la ejecuto, no para proteger datos de
 * terceros.</p>
 */
public final class VistaLogin {

    private static final String[] HERRAMIENTAS = {"PMD", "Checkstyle", "ESLint", "SonarQube"};

    private final RepositorioEvaluadores repositorio;
    private final Consumer<Evaluador> alIniciarSesion;

    private final TextField campoUsuario = new TextField();
    private final PasswordField campoContrasena = new PasswordField();
    private final PasswordField campoConfirmar = new PasswordField();
    private final VBox contenedorConfirmar = campoLabel("Confirmar contrasena", campoConfirmar);
    private final Label tituloFormulario = new Label();
    private final Label subtituloFormulario = new Label();
    private final Button botonPrincipal = new Button();
    private final Hyperlink enlaceAlternar = new Hyperlink();
    private final Label mensaje = new Label();

    private boolean modoRegistro;

    public VistaLogin(RepositorioEvaluadores repositorio, Consumer<Evaluador> alIniciarSesion) {
        this.repositorio = repositorio;
        this.alIniciarSesion = alIniciarSesion;
    }

    public Region contenido() {
        HBox raiz = new HBox(panelMarca(), panelFormulario());
        Platform.runLater(campoUsuario::requestFocus);
        return raiz;
    }

    /** Panel izquierdo: la misma identidad visual del menu lateral de la app. */
    private Region panelMarca() {
        Label marca = new Label("Orquestador");
        marca.getStyleClass().add("marca");

        Label marcaSub = new Label("Evaluacion de calidad\nde software");
        marcaSub.getStyleClass().add("marca-sub");

        FlowPane chips = new FlowPane(8, 8);
        for (String herramienta : HERRAMIENTAS) {
            Label chip = new Label(herramienta);
            chip.getStyleClass().addAll("chip", "chip-marca");
            chips.getChildren().add(chip);
        }

        Label descripcion = new Label("Instrumento de evaluacion comparativa de estas cuatro "
                + "herramientas sobre un banco de proyectos open-source.");
        descripcion.getStyleClass().add("pie-menu");
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-font-size: 13px;");

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Label pie = new Label("UNIB.E\nIngenieria de Software");
        pie.getStyleClass().add("pie-menu");

        VBox panel = new VBox(14, marca, marcaSub, chips, descripcion, espacio, pie);
        panel.getStyleClass().add("menu");
        panel.setPadding(new Insets(44, 36, 28, 36));
        panel.setPrefWidth(400);
        panel.setMinWidth(320);
        panel.setAlignment(Pos.TOP_LEFT);
        return panel;
    }

    /**
     * Panel derecho: un solo formulario centrado que alterna entre "Iniciar
     * sesion" y "Crear cuenta". El campo de confirmacion de contrasena solo
     * aparece (y solo ocupa espacio) en modo registro — ver
     * {@link #actualizarModo()}.
     */
    private Region panelFormulario() {
        tituloFormulario.getStyleClass().add("titulo-login");

        subtituloFormulario.getStyleClass().add("ayuda");
        subtituloFormulario.setWrapText(true);

        campoUsuario.setPromptText("Nombre de usuario");
        campoUsuario.getStyleClass().add("campo-login");
        campoContrasena.setPromptText("Contrasena");
        campoContrasena.getStyleClass().add("campo-login");
        campoConfirmar.setPromptText("Repita la contrasena");
        campoConfirmar.getStyleClass().add("campo-login");
        contenedorConfirmar.managedProperty().bind(contenedorConfirmar.visibleProperty());

        botonPrincipal.getStyleClass().add("boton-principal");
        botonPrincipal.setMaxWidth(Double.MAX_VALUE);
        botonPrincipal.setDefaultButton(true);
        botonPrincipal.setOnAction(e -> {
            if (modoRegistro) {
                registrarse();
            } else {
                iniciarSesion();
            }
        });

        enlaceAlternar.getStyleClass().add("enlace");
        enlaceAlternar.setOnAction(e -> alternarModo());
        HBox pieFormulario = new HBox(enlaceAlternar);
        pieFormulario.setAlignment(Pos.CENTER);

        mensaje.setWrapText(true);
        mensaje.setMaxWidth(Double.MAX_VALUE);
        mensaje.managedProperty().bind(mensaje.visibleProperty());
        mensaje.setVisible(false);

        VBox formulario = new VBox(16, tituloFormulario, subtituloFormulario,
                campoLabel("Usuario", campoUsuario), campoLabel("Contrasena", campoContrasena),
                contenedorConfirmar, botonPrincipal, mensaje, pieFormulario);
        formulario.getStyleClass().add("tarjeta-login");
        formulario.setPadding(new Insets(40));
        formulario.setMaxWidth(380);
        formulario.setMaxHeight(Region.USE_PREF_SIZE);

        actualizarModo();

        VBox panel = new VBox(formulario);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(40));
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private VBox campoLabel(String etiqueta, Region campo) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("etiqueta-campo");
        return new VBox(6, titulo, campo);
    }

    private void alternarModo() {
        modoRegistro = !modoRegistro;
        campoConfirmar.clear();
        mensaje.setVisible(false);
        actualizarModo();
    }

    /** Refleja modoRegistro en el texto y en que campos son visibles. */
    private void actualizarModo() {
        contenedorConfirmar.setVisible(modoRegistro);
        if (modoRegistro) {
            tituloFormulario.setText("Crear cuenta");
            subtituloFormulario.setText("Registrese con un usuario y una contrasena para que sus "
                    + "corridas de analisis queden identificadas.");
            botonPrincipal.setText("Crear cuenta");
            enlaceAlternar.setText("¿Ya tiene cuenta? Inicie sesion");
        } else {
            tituloFormulario.setText("Iniciar sesion");
            subtituloFormulario.setText("Identificarse deja registrado quien ejecuto cada corrida "
                    + "de analisis.");
            botonPrincipal.setText("Iniciar sesion");
            enlaceAlternar.setText("¿No tiene cuenta? Registrese");
        }
    }

    private void mostrarError(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("mensaje-error");
        mensaje.setVisible(true);
    }

    private void mostrarInfo(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("mensaje-info");
        mensaje.setVisible(true);
    }

    private void iniciarSesion() {
        String usuario = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Indique usuario y contrasena.");
            return;
        }
        try {
            Optional<Evaluador> evaluador = repositorio.buscarPorNombreUsuario(usuario);
            if (evaluador.isEmpty()) {
                mostrarInfo("No existe ese usuario. Puede registrarse.");
                return;
            }
            if (!GestorContrasenas.coincide(
                    contrasena, evaluador.get().sal(), evaluador.get().hashContrasena())) {
                mostrarError("Contrasena incorrecta.");
                return;
            }
            alIniciarSesion.accept(evaluador.get());
        } catch (IOException e) {
            mostrarError("No se pudo leer el registro de evaluadores: " + e.getMessage());
        }
    }

    private void registrarse() {
        String usuario = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();
        String confirmacion = campoConfirmar.getText();
        if (usuario.isEmpty() || contrasena.isEmpty() || confirmacion.isEmpty()) {
            mostrarError("Indique usuario, contrasena y su confirmacion.");
            return;
        }
        if (contrasena.length() < 6) {
            mostrarError("La contrasena debe tener al menos 6 caracteres.");
            return;
        }
        if (!contrasena.equals(confirmacion)) {
            mostrarError("Las contrasenas no coinciden.");
            return;
        }
        try {
            if (repositorio.existe(usuario)) {
                mostrarInfo("Ese usuario ya existe. Inicie sesion.");
                return;
            }
            String sal = GestorContrasenas.generarSal();
            String hash = GestorContrasenas.calcularHash(contrasena, sal);
            Evaluador nuevo = new Evaluador(usuario, hash, sal, LocalDateTime.now());
            repositorio.guardar(nuevo);
            alIniciarSesion.accept(nuevo);
        } catch (IOException e) {
            mostrarError("No se pudo registrar: " + e.getMessage());
        }
    }
}
