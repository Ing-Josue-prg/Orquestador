package ec.edu.unibe.orquestador.adaptadores.pmd;

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
 * Adaptador de PMD. Ejecuta la herramienta por linea de comandos sobre el
 * codigo del proyecto, interpreta su reporte XML y devuelve las mediciones
 * para la matriz comparativa junto con los hallazgos para el informe.
 *
 * <p>PMD termina con codigo 4 cuando encuentra violaciones, lo que no es un
 * error sino un resultado esperado; por eso la ejecucion usa la opcion que
 * evita el fallo por violaciones y ambos codigos se tratan como validos.</p>
 */
public final class AdaptadorPmd implements AnalizadorEstatico {

    private static final Duration TIEMPO_MAXIMO = Duration.ofMinutes(30);
    private static final String CONJUNTO_REGLAS = "rulesets/java/quickstart.xml";

    private final String rutaEjecutable;
    private String versionDetectada = "sin detectar";

    /**
     * @param rutaEjecutable ruta del ejecutable de PMD, por ejemplo
     *                       C:\herramientas\pmd\bin\pmd.bat
     */
    public AdaptadorPmd(String rutaEjecutable) {
        if (rutaEjecutable == null || rutaEjecutable.isBlank()) {
            throw new IllegalArgumentException("La ruta del ejecutable de PMD es obligatoria.");
        }
        this.rutaEjecutable = rutaEjecutable;
    }

    @Override
    public HerramientaSoportada herramienta() {
        return HerramientaSoportada.PMD;
    }

    @Override
    public VerificacionEntorno verificar() {
        try {
            var salida = EjecutorProceso.ejecutar(
                    List.of(rutaEjecutable, "--version"), Duration.ofMinutes(2));

            if (salida.exitoso()) {
                versionDetectada = salida.texto().trim();
                return new VerificacionEntorno(herramienta(), true, versionDetectada);
            }
            return new VerificacionEntorno(herramienta(), false,
                    "PMD respondio con codigo " + salida.codigo());

        } catch (Exception e) {
            return new VerificacionEntorno(herramienta(), false,
                    "No se encontro PMD en la ruta indicada: " + e.getMessage());
        }
    }

    @Override
    public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException {
        Path reporte = null;
        try {
            reporte = Files.createTempFile("pmd-reporte-", ".xml");

            List<String> comando = List.of(
                    rutaEjecutable, "check",
                    "-d", solicitud.carpeta().toString(),
                    "-R", CONJUNTO_REGLAS,
                    "-f", "xml",
                    "-r", reporte.toString(),
                    "--no-progress",
                    "--no-fail-on-violation");

            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO);

            // PMD devuelve 0 sin violaciones y 4 cuando encuentra alguna.
            if (salida.codigo() != 0 && salida.codigo() != 4) {
                throw new AnalisisException(
                        "PMD termino con codigo " + salida.codigo() + ": " + salida.texto());
            }

            String xml = Files.readString(reporte);
            var lectura = new LectorReportePmd().leer(xml);

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
            throw new AnalisisException("Fallo el analisis con PMD: " + e.getMessage(), e);
        } finally {
            borrarSilencioso(reporte);
        }
    }

    /**
     * Traduce los hallazgos a los registros que exige la ficha del
     * instrumento: los conteos por severidad alimentan la dimension de
     * metricas y el tiempo alimenta la de desempeno.
     */
    private List<RegistroMedicion> construirMediciones(
            SolicitudAnalisis solicitud, List<Hallazgo> hallazgos, Duration duracion) {

        Herramienta herramientaMedida = new Herramienta(
                HerramientaSoportada.PMD.nombre(), versionDetectada);

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
