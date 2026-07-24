package ec.edu.unibe.orquestador.adaptadores.checkstyle;

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
 * Adaptador de Checkstyle. Ejecuta la herramienta con {@code java -jar} sobre
 * el codigo del proyecto, interpreta su reporte XML y devuelve las
 * mediciones para la matriz comparativa junto con los hallazgos para el
 * informe.
 *
 * <p>El conjunto de reglas es fijo ({@code /sun_checks.xml}, empaquetado
 * dentro del propio jar de Checkstyle), igual que PMD usa un unico
 * "quickstart.xml": ningun analisis de la muestra usa una configuracion
 * distinta, para que la comparacion entre proyectos sea valida.</p>
 */
public final class AdaptadorCheckstyle implements AnalizadorEstatico {

    private static final Duration TIEMPO_MAXIMO = Duration.ofMinutes(30);
    private static final String CONJUNTO_REGLAS = "/sun_checks.xml";

    private final String rutaJar;
    private String versionDetectada = "sin detectar";

    /**
     * @param rutaJar ruta del archivo jar de Checkstyle, por ejemplo
     *                C:\herramientas\checkstyle\checkstyle-all.jar
     */
    public AdaptadorCheckstyle(String rutaJar) {
        if (rutaJar == null || rutaJar.isBlank()) {
            throw new IllegalArgumentException("La ruta del jar de Checkstyle es obligatoria.");
        }
        this.rutaJar = rutaJar;
    }

    @Override
    public HerramientaSoportada herramienta() {
        return HerramientaSoportada.CHECKSTYLE;
    }

    @Override
    public VerificacionEntorno verificar() {
        try {
            var salida = EjecutorProceso.ejecutar(
                    List.of("java", "-jar", rutaJar, "-V"), Duration.ofMinutes(2));

            if (salida.exitoso()) {
                versionDetectada = salida.texto().trim();
                return new VerificacionEntorno(herramienta(), true, versionDetectada);
            }
            return new VerificacionEntorno(herramienta(), false,
                    "Checkstyle respondio con codigo " + salida.codigo());

        } catch (Exception e) {
            return new VerificacionEntorno(herramienta(), false,
                    "No se encontro el jar de Checkstyle en la ruta indicada: " + e.getMessage());
        }
    }

    @Override
    public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException {
        Path reporte = null;
        try {
            reporte = Files.createTempFile("checkstyle-reporte-", ".xml");

            List<String> comando = List.of(
                    "java", "-jar", rutaJar,
                    "-c", CONJUNTO_REGLAS,
                    "-f", "xml",
                    "-o", reporte.toString(),
                    solicitud.carpeta().toString());

            var salida = EjecutorProceso.ejecutar(comando, TIEMPO_MAXIMO);

            // El codigo de salida de Checkstyle varia entre versiones cuando
            // encuentra violaciones; lo que importa es si el reporte se escribio.
            if (!Files.exists(reporte) || Files.size(reporte) == 0) {
                throw new AnalisisException(
                        "Checkstyle no genero el reporte (codigo " + salida.codigo() + "): "
                                + salida.texto());
            }

            String xml = Files.readString(reporte);
            var lectura = new LectorReporteCheckstyle().leer(xml);

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
            throw new AnalisisException("Fallo el analisis con Checkstyle: " + e.getMessage(), e);
        } finally {
            borrarSilencioso(reporte);
        }
    }

    private List<RegistroMedicion> construirMediciones(
            SolicitudAnalisis solicitud, List<Hallazgo> hallazgos, Duration duracion) {

        Herramienta herramientaMedida = new Herramienta(
                HerramientaSoportada.CHECKSTYLE.nombre(), versionDetectada);

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
