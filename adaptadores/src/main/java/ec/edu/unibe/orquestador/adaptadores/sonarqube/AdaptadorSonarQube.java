package ec.edu.unibe.orquestador.adaptadores.sonarqube;

import ec.edu.unibe.orquestador.adaptadores.EjecutorProceso;
import ec.edu.unibe.orquestador.adaptadores.LectorJson;
import ec.edu.unibe.orquestador.dominio.contratos.AnalisisException;
import ec.edu.unibe.orquestador.dominio.contratos.AnalizadorEstatico;
import ec.edu.unibe.orquestador.dominio.contratos.ResultadoAnalisis;
import ec.edu.unibe.orquestador.dominio.contratos.SolicitudAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.Dimension;
import ec.edu.unibe.orquestador.dominio.modelo.FuenteDato;
import ec.edu.unibe.orquestador.dominio.modelo.Hallazgo;
import ec.edu.unibe.orquestador.dominio.modelo.Herramienta;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoAnalizado;
import ec.edu.unibe.orquestador.dominio.modelo.RegistroMedicion;
import ec.edu.unibe.orquestador.dominio.modelo.Severidad;
import ec.edu.unibe.orquestador.dominio.modelo.VerificacionEntorno;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Adaptador de SonarQube. A diferencia de PMD, Checkstyle y ESLint, que son
 * un solo ejecutable, SonarQube es cliente-servidor: un contenedor Docker
 * con el servidor, un escaner de linea de comandos que sube los resultados,
 * y la Web API para leerlos de vuelta.
 *
 * <p>El paso que le corresponde al usuario, no a la aplicacion: la primera
 * vez hay que entrar a la direccion del servidor, cambiar la contrasena por
 * defecto del usuario {@code admin} y generar un token de acceso propio de
 * SonarQube para pegarlo en la tarjeta de esta herramienta. Automatizar la
 * gestion de esa contrasena por la Web API excede el alcance de este
 * proyecto.</p>
 *
 * <p>Usa {@code /api/system/status} para verificar el servidor,
 * {@code /api/issues/search} para los hallazgos y las incidencias de tipo
 * vulnerabilidad, {@code /api/hotspots/search} para los puntos criticos de
 * seguridad, y {@code /api/measures/component} para las metricas agregadas
 * del proyecto (complejidad ciclomatica, duplicacion y deuda tecnica segun
 * el metodo SQALE). Estas ultimas dos consultas son las unicas de las cuatro
 * herramientas que exponen esas metricas por fuera de los hallazgos
 * puntuales, porque son propias de la plataforma de SonarQube.</p>
 *
 * <p>Si el proyecto tiene codigo Java, se compila antes de escanear (ver
 * {@link #compilarJavaSiHaceFalta}): el sensor de Java de SonarQube, a
 * diferencia de PMD y Checkstyle, exige clases compiladas y no solo texto
 * fuente.</p>
 */
public final class AdaptadorSonarQube implements AnalizadorEstatico {

    private static final String NOMBRE_CONTENEDOR = "orquestador-sonarqube";
    private static final String IMAGEN = "sonarqube:community";
    private static final Duration TIEMPO_MAXIMO_ESPERA = Duration.ofMinutes(3);
    private static final Duration INTERVALO_SONDEO = Duration.ofSeconds(5);
    private static final Duration TIEMPO_MAXIMO_ESCANEO = Duration.ofMinutes(20);
    private static final Duration TIEMPO_MAXIMO_COMPILACION = Duration.ofMinutes(20);
    private static final Duration TIEMPO_MAXIMO_PROCESAMIENTO = Duration.ofMinutes(5);
    private static final Duration TIEMPO_MAXIMO_DOCKER = Duration.ofMinutes(2);
    private static final Duration TIEMPO_MAXIMO_HTTP = Duration.ofSeconds(10);
    private static final int TAMANO_PAGINA = 500;

    private final String rutaScanner;
    private final String rutaDocker;
    private final String urlServidor;
    private final String token;
    private final HttpClient http = HttpClient.newHttpClient();

    private String versionDetectada = "sin detectar";

    /**
     * @param rutaScanner comando o ruta del ejecutable de sonar-scanner
     * @param rutaDocker  comando o ruta del ejecutable de docker, usado
     *                    para levantar el contenedor del servidor
     * @param urlServidor direccion del servidor, por ejemplo http://localhost:9000
     * @param token       token de acceso generado en la propia interfaz de SonarQube
     */
    public AdaptadorSonarQube(String rutaScanner, String rutaDocker, String urlServidor, String token) {
        if (rutaScanner == null || rutaScanner.isBlank()) {
            throw new IllegalArgumentException("La ruta de sonar-scanner es obligatoria.");
        }
        if (rutaDocker == null || rutaDocker.isBlank()) {
            throw new IllegalArgumentException("La ruta de docker es obligatoria.");
        }
        if (urlServidor == null || urlServidor.isBlank()) {
            throw new IllegalArgumentException("La direccion del servidor de SonarQube es obligatoria.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token de acceso de SonarQube es obligatorio.");
        }
        this.rutaScanner = rutaScanner;
        this.rutaDocker = rutaDocker;
        this.urlServidor = urlServidor.endsWith("/")
                ? urlServidor.substring(0, urlServidor.length() - 1) : urlServidor;
        this.token = token;
    }

    @Override
    public HerramientaSoportada herramienta() {
        return HerramientaSoportada.SONARQUBE;
    }

    @Override
    public VerificacionEntorno verificar() {
        try {
            String estado = consultarEstado();
            if ("UP".equals(estado)) {
                return new VerificacionEntorno(herramienta(), true,
                        "Servidor disponible en " + urlServidor + " (" + versionDetectada + ")");
            }
            return new VerificacionEntorno(herramienta(), false,
                    "El servidor respondio con estado: " + estado);
        } catch (Exception e) {
            return new VerificacionEntorno(herramienta(), false,
                    "No se pudo conectar a " + urlServidor + ": " + e.getMessage());
        }
    }

    @Override
    public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException {
        try {
            asegurarContenedorActivo();
            esperarDisponibilidad();

            String claveProyecto = solicitud.proyecto().id();
            String idTareaAnterior = idTareaActual(claveProyecto);
            List<String> propiedades = new ArrayList<>(List.of(
                    "-Dsonar.projectKey=" + claveProyecto,
                    "-Dsonar.sources=.",
                    // Algunos proyectos traen su propio sonar-project.properties
                    // apuntando a reportes de pruebas o cobertura de su
                    // integracion continua (por ejemplo, un XML de Jest/Vitest)
                    // que nunca se generan aqui porque este instrumento no
                    // corre la suite de pruebas del proyecto, solo analisis
                    // estatico. Sin esto, el escaner falla tratando de leer un
                    // reporte que no existe en vez de simplemente omitirlo.
                    "-Dsonar.testExecutionReportPaths=",
                    "-Dsonar.coverageReportPaths=",
                    "-Dsonar.host.url=" + urlServidor,
                    "-Dsonar.token=" + token));

            compilarJavaSiHaceFalta(solicitud.carpeta())
                    .ifPresent(binarios -> propiedades.add("-Dsonar.java.binaries=" + binarios));

            List<String> comando = EjecutorProceso.resolverComando(
                    rutaScanner, propiedades.toArray(new String[0]));

            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO_ESCANEO, solicitud.carpeta());
            if (!salida.exitoso()) {
                throw new AnalisisException(
                        "sonar-scanner termino con codigo " + salida.codigo() + ": " + salida.texto());
            }

            esperarProcesamiento(claveProyecto, idTareaAnterior);

            BusquedaHallazgos busqueda = buscarTodosLosHallazgos(claveProyecto);
            List<Hallazgo> hallazgos = busqueda.hallazgos();
            Map<String, Double> medidas = buscarMedidas(claveProyecto);
            int vulnerabilidades = contarPorTipo(claveProyecto, "VULNERABILITY");
            int puntosCriticos = contarPuntosCriticos(claveProyecto);
            List<RegistroMedicion> mediciones = construirMediciones(
                    solicitud, hallazgos, busqueda.total(), salida.duracion(), medidas,
                    vulnerabilidades, puntosCriticos);
            int archivosConHallazgos = (int) hallazgos.stream().map(Hallazgo::archivo).distinct().count();

            return new ResultadoAnalisis(mediciones, hallazgos, archivosConHallazgos,
                    salida.duracion(), OptionalLong.empty());

        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException("Fallo el analisis con SonarQube: " + e.getMessage(), e);
        }
    }

    /** Crea el contenedor si no existe, o lo arranca si esta detenido; nunca lo borra. */
    private void asegurarContenedorActivo() throws Exception {
        var inspeccion = EjecutorProceso.ejecutar(
                List.of(rutaDocker, "inspect", "-f", "{{.State.Running}}", NOMBRE_CONTENEDOR),
                TIEMPO_MAXIMO_DOCKER);

        if (inspeccion.exitoso()) {
            if (!inspeccion.texto().trim().equalsIgnoreCase("true")) {
                EjecutorProceso.ejecutar(
                        List.of(rutaDocker, "start", NOMBRE_CONTENEDOR), TIEMPO_MAXIMO_DOCKER);
            }
            return;
        }

        int puerto = puertoDelServidor();
        EjecutorProceso.ejecutar(List.of(rutaDocker, "run", "-d", "--name", NOMBRE_CONTENEDOR,
                "-p", puerto + ":9000", IMAGEN), TIEMPO_MAXIMO_DOCKER);
    }

    private int puertoDelServidor() {
        int puerto = URI.create(urlServidor).getPort();
        return puerto > 0 ? puerto : 9000;
    }

    /**
     * A diferencia de PMD y Checkstyle, que analizan directamente el texto
     * fuente, el sensor de Java de SonarQube exige clases ya compiladas
     * (rechaza el analisis si encuentra archivos <code>.java</code> sin
     * <code>sonar.java.binaries</code>). Se compila con el wrapper que el
     * propio proyecto trae (<code>mvnw</code> o <code>gradlew</code>) — el
     * mismo criterio que ya usa ESLint con su configuracion local — y se
     * devuelven las carpetas de clases resultantes para pasarselas al
     * escaner. Si el proyecto no tiene codigo Java (por ejemplo, un
     * proyecto JavaScript puro), no hace falta compilar nada.
     *
     * <p>En un proyecto Maven multimodulo, como Google Guava o Square
     * Retrofit, no existe un unico "target/classes" en la raiz: cada
     * submodulo tiene el suyo, y la raiz (un pom agregador sin fuente
     * propia) no tiene ninguno. Por eso, tras compilar, se recorre el
     * proyecto y se recogen todas las carpetas de clases que realmente
     * contienen un ".class", en vez de asumir una unica ruta fija.</p>
     */
    private Optional<String> compilarJavaSiHaceFalta(Path carpeta) throws AnalisisException {
        if (Files.isRegularFile(carpeta.resolve("pom.xml"))) {
            // Se invoca la ejecucion "default-compile" del propio proyecto
            // (compiler:compile@default-compile) en vez de la fase "compile"
            // del ciclo de vida completo: en proyectos con frontend
            // integrado (como los que genera JHipster) la fase "compile"
            // tambien dispara el build completo de webpack, que no hace
            // falta para tener las clases Java y solo suma tiempo y puntos
            // de falla. Apuntar a la ejecucion concreta, en vez del objetivo
            // desnudo, hace que Maven aplique la configuracion propia que
            // cada proyecto le dio a esa ejecucion (por ejemplo, excluir un
            // "module-info.java" del classpath clasico), que de otro modo se
            // pierde. "-fae" (fail at end) deja que los demas modulos del
            // reactor terminen de compilar aunque uno falle, para no perder
            // las clases de los que si compilaron.
            ejecutarBuildJava(carpeta, "mvnw.cmd", "mvnw", "mvn",
                    "-fae", "compiler:compile@default-compile");
            return construirRutaBinarios(carpeta, "target", "classes");
        }
        if (Files.isRegularFile(carpeta.resolve("build.gradle"))
                || Files.isRegularFile(carpeta.resolve("build.gradle.kts"))) {
            ejecutarBuildJava(carpeta, "gradlew.bat", "gradlew", "gradle",
                    "--continue", "compileJava");
            return construirRutaBinarios(carpeta, "build", "classes", "java", "main");
        }
        return Optional.empty();
    }

    /**
     * Recorre el proyecto buscando, en cada subcarpeta, la ruta relativa
     * indicada (por ejemplo "target/classes") y la incluye solo si contiene
     * al menos un ".class". Devuelve la lista unida por comas, el formato
     * que acepta "sonar.java.binaries" para varias carpetas a la vez.
     */
    private Optional<String> construirRutaBinarios(Path carpeta, String... segmentosRelativos)
            throws AnalisisException {
        Path relativa = Path.of(segmentosRelativos[0],
                Arrays.copyOfRange(segmentosRelativos, 1, segmentosRelativos.length));
        List<String> encontradas = new ArrayList<>();
        try (var flujo = Files.walk(carpeta, 6)) {
            for (Path candidata : flujo
                    .filter(Files::isDirectory)
                    .filter(p -> p.endsWith(relativa))
                    .toList()) {
                try (var archivos = Files.walk(candidata)) {
                    boolean tieneClases = archivos.anyMatch(a -> a.toString().endsWith(".class"));
                    if (tieneClases) {
                        encontradas.add(carpeta.relativize(candidata).toString());
                    }
                }
            }
        } catch (IOException e) {
            throw new AnalisisException(
                    "Fallo al buscar las clases compiladas del proyecto: " + e.getMessage(), e);
        }
        if (encontradas.isEmpty()) {
            throw new AnalisisException(
                    "La compilacion no dejo ninguna carpeta de clases (" + relativa
                            + ") con archivos .class en " + carpeta);
        }
        return Optional.of(String.join(",", encontradas));
    }

    private void ejecutarBuildJava(Path carpeta, String wrapperWindows, String wrapperUnix,
                                    String comandoGlobal, String... tarea) throws AnalisisException {
        boolean esWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path wrapper = carpeta.resolve(esWindows ? wrapperWindows : wrapperUnix);
        String ejecutable = Files.isRegularFile(wrapper) ? wrapper.toString() : comandoGlobal;

        try {
            List<String> comando = EjecutorProceso.resolverComando(ejecutable, tarea);
            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO_COMPILACION, carpeta);
            if (!salida.exitoso()) {
                throw new AnalisisException(
                        "No se pudo compilar el proyecto Java antes de analizarlo con SonarQube "
                                + "(codigo " + salida.codigo() + "): " + salida.texto());
            }
        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException("Fallo al compilar el proyecto Java: " + e.getMessage(), e);
        }
    }

    private void esperarDisponibilidad() throws AnalisisException {
        long limite = System.nanoTime() + TIEMPO_MAXIMO_ESPERA.toNanos();
        while (System.nanoTime() < limite) {
            try {
                if ("UP".equals(consultarEstado())) {
                    return;
                }
            } catch (Exception ignorada) {
                // El servidor todavia no responde; se reintenta hasta el limite.
            }
            try {
                Thread.sleep(INTERVALO_SONDEO.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AnalisisException("Se interrumpio la espera del servidor de SonarQube.", e);
            }
        }
        throw new AnalisisException("El servidor de SonarQube no respondio 'UP' dentro de "
                + TIEMPO_MAXIMO_ESPERA.toMinutes() + " minutos.");
    }

    @SuppressWarnings("unchecked")
    private String consultarEstado() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor + "/api/system/status"))
                .timeout(TIEMPO_MAXIMO_HTTP)
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> cuerpo = (Map<String, Object>) LectorJson.leer(respuesta.body());

        Object version = cuerpo.get("version");
        if (version != null) {
            versionDetectada = String.valueOf(version);
        }
        return String.valueOf(cuerpo.get("status"));
    }

    /**
     * El escaner solo sube el reporte; el motor de computo de SonarQube lo
     * procesa en segundo plano y hasta que no termina esa tarea, la Web API
     * de hallazgos ({@code /api/issues/search}) sigue devolviendo el estado
     * anterior del proyecto (vacio, la primera vez). Se identifica la tarea
     * vigente antes de escanear para poder distinguirla de la que genere
     * este analisis (ver {@link #esperarProcesamiento}).
     */
    private String idTareaActual(String claveProyecto) {
        try {
            Object actual = consultarTareaDelComponente(claveProyecto).get("current");
            if (actual instanceof Map<?, ?> mapa) {
                return String.valueOf(mapa.get("id"));
            }
        } catch (Exception ignorada) {
            // Proyecto nunca analizado antes, o el servidor no respondio a
            // tiempo: se trata igual que "sin tarea anterior".
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> consultarTareaDelComponente(String claveProyecto) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor
                        + "/api/ce/component?component=" + claveProyecto))
                .timeout(TIEMPO_MAXIMO_HTTP)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        return (Map<String, Object>) LectorJson.leer(respuesta.body());
    }

    /**
     * Espera a que el motor de computo termine de procesar el reporte que
     * acaba de subir el escaner: sondea {@code /api/ce/component} hasta que
     * aparezca una tarea nueva (distinta a {@code idTareaAnterior}) con la
     * cola vacia y estado terminal. Sin esta espera, {@link #buscarTodosLosHallazgos}
     * puede ejecutarse contra el estado previo del proyecto y devolver cero
     * hallazgos aunque el analisis haya sido exitoso.
     */
    @SuppressWarnings("unchecked")
    private void esperarProcesamiento(String claveProyecto, String idTareaAnterior) throws AnalisisException {
        long limite = System.nanoTime() + TIEMPO_MAXIMO_PROCESAMIENTO.toNanos();
        while (System.nanoTime() < limite) {
            try {
                Map<String, Object> cuerpo = consultarTareaDelComponente(claveProyecto);
                List<Object> cola = (List<Object>) cuerpo.getOrDefault("queue", List.of());
                Object actualObj = cuerpo.get("current");

                if (cola.isEmpty() && actualObj instanceof Map<?, ?> actual) {
                    String idActual = String.valueOf(actual.get("id"));
                    String estado = String.valueOf(actual.get("status"));
                    if (!idActual.equals(idTareaAnterior)) {
                        if ("SUCCESS".equals(estado)) {
                            return;
                        }
                        if ("FAILED".equals(estado) || "CANCELED".equals(estado)) {
                            throw new AnalisisException(
                                    "El motor de computo de SonarQube no pudo procesar el analisis "
                                            + "(estado " + estado + ").");
                        }
                    }
                }
            } catch (AnalisisException e) {
                throw e;
            } catch (Exception ignorada) {
                // El servidor todavia no responde; se reintenta hasta el limite.
            }
            try {
                Thread.sleep(INTERVALO_SONDEO.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AnalisisException("Se interrumpio la espera del procesamiento en SonarQube.", e);
            }
        }
        throw new AnalisisException("SonarQube no termino de procesar el analisis dentro de "
                + TIEMPO_MAXIMO_PROCESAMIENTO.toMinutes() + " minutos.");
    }

    /**
     * Hallazgos recuperados y el total real que informa el servidor. Se
     * distinguen porque la Web API de SonarQube no deja traer mas de diez
     * mil incidencias por busqueda (limite de su motor de indexado), asi que
     * en un proyecto muy grande la lista puede quedar incompleta aunque el
     * total informado sea mayor: el total se usa para el conteo agregado y
     * la lista, aun truncada, para la severidad y la concordancia.
     */
    private record BusquedaHallazgos(List<Hallazgo> hallazgos, int total) {
    }

    private BusquedaHallazgos buscarTodosLosHallazgos(String claveProyecto) throws Exception {
        List<Hallazgo> hallazgos = new ArrayList<>();
        int totalReal = 0;
        int pagina = 1;
        while (true) {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor
                            + "/api/issues/search?componentKeys=" + claveProyecto
                            + "&p=" + pagina + "&ps=" + TAMANO_PAGINA))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            var lectura = new LectorReporteSonarQube().leer(respuesta.body(), claveProyecto);
            hallazgos.addAll(lectura.hallazgos());
            // El total solo se toma de la primera pagina: si el proyecto
            // supera el limite de diez mil resultados que impone el motor de
            // indexado de SonarQube, la ultima pagina pedida (la que cae
            // fuera de ese limite) responde sin "paging" ni "total", y
            // tomar el total de esa respuesta pisaria el valor correcto con
            // un cero.
            if (pagina == 1) {
                totalReal = Math.max(lectura.total(), hallazgos.size());
            }

            if (lectura.hallazgos().isEmpty() || hallazgos.size() >= lectura.total()) {
                break;
            }
            pagina++;
        }
        return new BusquedaHallazgos(hallazgos, totalReal);
    }

    /**
     * Consulta {@code /api/measures/component} por la complejidad
     * ciclomatica, la densidad de duplicacion y la deuda tecnica (metodo
     * SQALE) del proyecto. A diferencia de los hallazgos, que describen
     * incidencias puntuales, estas son metricas agregadas de todo el
     * proyecto: por eso se piden aparte, no se derivan de la lista de
     * hallazgos.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> buscarMedidas(String claveProyecto) throws Exception {
        String claves = "complexity,duplicated_lines_density,sqale_index,sqale_debt_ratio";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor
                        + "/api/measures/component?component=" + claveProyecto
                        + "&metricKeys=" + claves))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> cuerpo = (Map<String, Object>) LectorJson.leer(respuesta.body());
        Object componente = cuerpo.get("component");
        Map<String, Double> medidas = new java.util.HashMap<>();
        if (componente instanceof Map<?, ?> mapaComponente) {
            Object listaMedidas = mapaComponente.get("measures");
            if (listaMedidas instanceof List<?> lista) {
                for (Object elemento : lista) {
                    if (elemento instanceof Map<?, ?> medida) {
                        String metrica = String.valueOf(medida.get("metric"));
                        Object valor = medida.get("value");
                        if (valor != null) {
                            try {
                                medidas.put(metrica, Double.parseDouble(String.valueOf(valor)));
                            } catch (NumberFormatException ignorada) {
                                // Metrica sin valor numerico util (por ejemplo, no calculada
                                // todavia para este proyecto); se omite en vez de fallar.
                            }
                        }
                    }
                }
            }
        }
        return medidas;
    }

    /**
     * Cuenta cuantas incidencias de un tipo (por ejemplo, VULNERABILITY) hay
     * en el proyecto, sin traer la lista completa: alcanza con leer el total
     * de la primera pagina.
     */
    @SuppressWarnings("unchecked")
    private int contarPorTipo(String claveProyecto, String tipo) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor
                        + "/api/issues/search?componentKeys=" + claveProyecto
                        + "&types=" + tipo + "&ps=1"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> cuerpo = (Map<String, Object>) LectorJson.leer(respuesta.body());
        Object paginacion = cuerpo.get("paging");
        if (paginacion instanceof Map<?, ?> mapa && mapa.get("total") instanceof Double total) {
            return total.intValue();
        }
        return 0;
    }

    /**
     * Cuenta los puntos criticos de seguridad (security hotspots), que
     * SonarQube gestiona por separado de las incidencias comunes y expone en
     * su propio extremo de la Web API.
     */
    @SuppressWarnings("unchecked")
    private int contarPuntosCriticos(String claveProyecto) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlServidor
                        + "/api/hotspots/search?projectKey=" + claveProyecto + "&ps=1"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> cuerpo = (Map<String, Object>) LectorJson.leer(respuesta.body());
        Object paginacion = cuerpo.get("paging");
        if (paginacion instanceof Map<?, ?> mapa && mapa.get("total") instanceof Double total) {
            return total.intValue();
        }
        return 0;
    }

    private List<RegistroMedicion> construirMediciones(
            SolicitudAnalisis solicitud, List<Hallazgo> hallazgos, int totalReal, Duration duracion,
            Map<String, Double> medidas, int vulnerabilidades, int puntosCriticos) {

        Herramienta herramientaMedida = new Herramienta(
                HerramientaSoportada.SONARQUBE.nombre(), versionDetectada);

        ProyectoAnalizado proyecto = new ProyectoAnalizado(
                solicitud.proyecto().nombre(),
                solicitud.proyecto().lenguaje(),
                solicitud.commit());

        LocalDateTime ahora = LocalDateTime.now();
        List<RegistroMedicion> mediciones = new ArrayList<>();

        // Se usa el total que informa el servidor, no el tamano de la lista
        // recuperada: la Web API no deja traer mas de diez mil incidencias
        // por busqueda, asi que en un proyecto muy grande la lista puede
        // quedar truncada aunque el conteo agregado sea exacto.
        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.METRICAS,
                "Incidencias totales", totalReal, "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));

        for (Severidad severidad : Severidad.values()) {
            long conteo = hallazgos.stream().filter(h -> h.severidad() == severidad).count();
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Incidencias de severidad " + severidad.etiqueta().toLowerCase(),
                    conteo, "conteo", FuenteDato.REPORTE_HERRAMIENTA));
        }

        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.METRICAS,
                "Vulnerabilidades", vulnerabilidades, "conteo", FuenteDato.REPORTE_HERRAMIENTA));

        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.METRICAS,
                "Puntos criticos de seguridad", puntosCriticos, "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));

        if (medidas.containsKey("complexity")) {
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Complejidad ciclomatica", medidas.get("complexity"), "conteo",
                    FuenteDato.REPORTE_HERRAMIENTA));
        }
        if (medidas.containsKey("duplicated_lines_density")) {
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Duplicacion", medidas.get("duplicated_lines_density"), "porcentaje",
                    FuenteDato.REPORTE_HERRAMIENTA));
        }
        if (medidas.containsKey("sqale_index")) {
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Deuda tecnica", medidas.get("sqale_index"), "minutos",
                    FuenteDato.REPORTE_HERRAMIENTA));
        }
        if (medidas.containsKey("sqale_debt_ratio")) {
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Razon de deuda tecnica", medidas.get("sqale_debt_ratio"), "porcentaje",
                    FuenteDato.REPORTE_HERRAMIENTA));
        }

        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.DESEMPENO,
                "Tiempo de analisis", duracion.toMillis() / 1000.0, "segundos",
                FuenteDato.MEDICION_EXTERNA));

        return mediciones;
    }
}
