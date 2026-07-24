package ec.edu.unibe.orquestador.adaptadores.eslint;

import ec.edu.unibe.orquestador.adaptadores.EjecutorProceso;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Adaptador de ESLint. A diferencia de PMD y Checkstyle, que revisan el
 * codigo con un conjunto de reglas fijo y autocontenido, ESLint analiza
 * proyectos JavaScript/TypeScript reales usando la configuracion que esos
 * proyectos ya traen consigo: por eso primero instala sus dependencias
 * ({@code npm install}) y recien despues ejecuta el analisis, en vez de
 * imponer una configuracion propia que no seria comparable con como esos
 * proyectos realmente se analizan.
 *
 * <p>Es la unica herramienta de la muestra con un paso previo de
 * preparacion; puede tardar bastante mas que PMD o Checkstyle la primera
 * vez que se analiza un proyecto.</p>
 */
public final class AdaptadorEslint implements AnalizadorEstatico {

    private static final Duration TIEMPO_MAXIMO_INSTALACION = Duration.ofMinutes(10);
    private static final Duration TIEMPO_MAXIMO_ANALISIS = Duration.ofMinutes(15);

    private final String rutaEslint;
    private final String rutaNpm;
    private String versionDetectada = "sin detectar";

    /**
     * @param rutaEslint comando o ruta del ejecutable de ESLint
     * @param rutaNpm    comando o ruta del ejecutable de npm, usado para
     *                   instalar las dependencias del proyecto antes de analizarlo
     */
    public AdaptadorEslint(String rutaEslint, String rutaNpm) {
        if (rutaEslint == null || rutaEslint.isBlank()) {
            throw new IllegalArgumentException("La ruta de ESLint es obligatoria.");
        }
        if (rutaNpm == null || rutaNpm.isBlank()) {
            throw new IllegalArgumentException("La ruta de npm es obligatoria.");
        }
        this.rutaEslint = rutaEslint;
        this.rutaNpm = rutaNpm;
    }

    @Override
    public HerramientaSoportada herramienta() {
        return HerramientaSoportada.ESLINT;
    }

    @Override
    public VerificacionEntorno verificar() {
        try {
            var salida = EjecutorProceso.ejecutar(
                    EjecutorProceso.resolverComando(rutaEslint, "--version"), Duration.ofMinutes(2));

            if (salida.exitoso()) {
                versionDetectada = salida.texto().trim();
                return new VerificacionEntorno(herramienta(), true, versionDetectada);
            }
            return new VerificacionEntorno(herramienta(), false,
                    "ESLint respondio con codigo " + salida.codigo());

        } catch (Exception e) {
            return new VerificacionEntorno(herramienta(), false,
                    "No se encontro ESLint en la ruta indicada: " + e.getMessage());
        }
    }

    @Override
    public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException {
        Path reporte = null;
        try {
            instalarDependencias(solicitud.carpeta());

            reporte = Files.createTempFile("eslint-reporte-", ".json");

            List<String> comando = EjecutorProceso.resolverComando(
                    rutaEslintDelProyecto(solicitud.carpeta()), "-f", "json", "-o",
                    reporte.toString(), ".");
            var salida = EjecutorProceso.ejecutar(
                    comando, TIEMPO_MAXIMO_ANALISIS, solicitud.carpeta());

            // Igual que PMD y Checkstyle: ESLint devuelve un codigo distinto
            // de cero cuando encuentra problemas, eso no es una falla. Pero
            // un reporte vacio si lo es: ocurre cuando el proyecto no trae
            // ninguna configuracion de ESLint propia (por ejemplo, porque
            // usa otro analizador) y ESLint termina sin poder analizar nada,
            // dejando un archivo de salida en blanco en vez de un JSON
            // valido. Se distingue de proposito de "no genero el reporte"
            // para que el mensaje explique la causa real.
            if (!Files.exists(reporte)) {
                throw new AnalisisException(
                        "ESLint no genero el reporte (codigo " + salida.codigo() + "): "
                                + salida.texto());
            }
            if (Files.size(reporte) == 0) {
                throw new AnalisisException(
                        "ESLint devolvio un reporte vacio (codigo " + salida.codigo() + "). "
                                + "Es probable que el proyecto no traiga una configuracion propia "
                                + "de ESLint para que el adaptador la use: " + salida.texto());
            }

            String json = Files.readString(reporte);
            var lectura = new LectorReporteEslint().leer(json);

            List<RegistroMedicion> mediciones = construirMediciones(
                    solicitud, lectura.hallazgos(), salida.duracion());

            return new ResultadoAnalisis(
                    mediciones,
                    lectura.hallazgos(),
                    lectura.archivosConHallazgos(),
                    salida.duracion(),
                    OptionalLong.empty());

        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException("Fallo el analisis con ESLint: " + e.getMessage(), e);
        } finally {
            borrarSilencioso(reporte);
        }
    }

    private void instalarDependencias(Path carpeta) throws AnalisisException {
        try {
            // --legacy-peer-deps: proyectos reales de terceros a veces
            // declaran dependencias entre si con versiones que el algoritmo
            // estricto de npm (desde npm 7) rechaza con ERESOLVE, aunque el
            // proyecto funcione bien en la practica. No controlamos esas
            // dependencias, asi que se instala con la resolucion mas
            // permisiva en vez de fallar antes de poder analizar el codigo.
            List<String> comando = EjecutorProceso.resolverComando(
                    rutaNpm, "install", "--no-audit", "--no-fund", "--legacy-peer-deps");
            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO_INSTALACION, carpeta);
            if (!salida.exitoso()) {
                throw new AnalisisException(
                        "No se pudieron instalar las dependencias del proyecto (npm install "
                                + "termino con codigo " + salida.codigo() + "): " + salida.texto());
            }
        } catch (AnalisisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalisisException("Fallo al preparar las dependencias con npm: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Usa el ESLint que {@code npm install} acaba de instalar dentro del
     * propio proyecto, en vez del configurado globalmente: cada proyecto
     * fija su propia version y su propio formato de configuracion (por
     * ejemplo, uno puede seguir el formato clasico y otro el nuevo formato
     * "flat"), y solo su version local sabe leerla. Si el proyecto no
     * instalo ESLint como dependencia, se usa el configurado como reserva.
     */
    private String rutaEslintDelProyecto(Path carpeta) {
        boolean esWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path local = carpeta.resolve("node_modules").resolve(".bin")
                .resolve(esWindows ? "eslint.cmd" : "eslint");
        return Files.isRegularFile(local) ? local.toString() : rutaEslint;
    }

    private List<RegistroMedicion> construirMediciones(
            SolicitudAnalisis solicitud, List<Hallazgo> hallazgos, Duration duracion) {

        Herramienta herramientaMedida = new Herramienta(
                HerramientaSoportada.ESLINT.nombre(), versionDetectada);

        ProyectoAnalizado proyecto = new ProyectoAnalizado(
                solicitud.proyecto().nombre(),
                solicitud.proyecto().lenguaje(),
                solicitud.commit());

        LocalDateTime ahora = LocalDateTime.now();
        List<RegistroMedicion> mediciones = new ArrayList<>();

        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.METRICAS,
                "Incidencias totales", hallazgos.size(), "conteo",
                FuenteDato.REPORTE_HERRAMIENTA));

        for (Severidad severidad : Severidad.values()) {
            long conteo = hallazgos.stream().filter(h -> h.severidad() == severidad).count();
            mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                    solicitud.numeroCorrida(), Dimension.METRICAS,
                    "Incidencias de severidad " + severidad.etiqueta().toLowerCase(),
                    conteo, "conteo", FuenteDato.REPORTE_HERRAMIENTA));
        }

        mediciones.add(new RegistroMedicion(ahora, herramientaMedida, proyecto,
                solicitud.numeroCorrida(), Dimension.DESEMPENO,
                "Tiempo de analisis", duracion.toMillis() / 1000.0, "segundos",
                FuenteDato.MEDICION_EXTERNA));

        return mediciones;
    }

    private void borrarSilencioso(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (Exception ignorada) {
            // El archivo temporal quedara a cargo del sistema operativo.
        }
    }
}
