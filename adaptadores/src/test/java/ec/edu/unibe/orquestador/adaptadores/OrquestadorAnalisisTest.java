package ec.edu.unibe.orquestador.adaptadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.edu.unibe.orquestador.dominio.contratos.AnalisisException;
import ec.edu.unibe.orquestador.dominio.contratos.AnalizadorEstatico;
import ec.edu.unibe.orquestador.dominio.contratos.ResultadoAnalisis;
import ec.edu.unibe.orquestador.dominio.contratos.SolicitudAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.VerificacionEntorno;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrquestadorAnalisis")
class OrquestadorAnalisisTest {

    private static final ProyectoMuestra RETROFIT = new ProyectoMuestra(
            "retrofit", "square/retrofit", "https://github.com/square/retrofit", "Java", "Apache 2.0");
    private static final SolicitudAnalisis SOLICITUD = new SolicitudAnalisis(
            RETROFIT, Path.of("."), "abc1234", 1);

    /** Adaptador de prueba que siempre tiene exito, sin invocar ningun proceso. */
    private static AnalizadorEstatico adaptadorExitoso(HerramientaSoportada herramienta) {
        return new AnalizadorEstatico() {
            @Override
            public HerramientaSoportada herramienta() {
                return herramienta;
            }

            @Override
            public VerificacionEntorno verificar() {
                return new VerificacionEntorno(herramienta, true, "prueba");
            }

            @Override
            public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) {
                return new ResultadoAnalisis(
                        List.of(), List.of(), 0, Duration.ofSeconds(1), OptionalLong.empty());
            }
        };
    }

    /** Adaptador de prueba que siempre falla, sin invocar ningun proceso. */
    private static AnalizadorEstatico adaptadorFallido(HerramientaSoportada herramienta) {
        return new AnalizadorEstatico() {
            @Override
            public HerramientaSoportada herramienta() {
                return herramienta;
            }

            @Override
            public VerificacionEntorno verificar() {
                return new VerificacionEntorno(herramienta, false, "prueba");
            }

            @Override
            public ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException {
                throw new AnalisisException("fallo simulado de " + herramienta.nombre());
            }
        };
    }

    @Test
    @DisplayName("un fallo de una herramienta no detiene a las demas")
    void unFalloNoDetieneALasDemas() {
        var orquestador = new OrquestadorAnalisis();
        var resultado = orquestador.ejecutarTodas(
                List.of(adaptadorExitoso(HerramientaSoportada.PMD),
                        adaptadorFallido(HerramientaSoportada.CHECKSTYLE),
                        adaptadorExitoso(HerramientaSoportada.ESLINT)),
                SOLICITUD);

        assertEquals(2, resultado.exitosos().size());
        assertEquals(1, resultado.fallidos().size());
    }

    @Test
    @DisplayName("separa correctamente cual herramienta exitosa y cual fallida")
    void separaHerramientasPorResultado() {
        var orquestador = new OrquestadorAnalisis();
        var resultado = orquestador.ejecutarTodas(
                List.of(adaptadorExitoso(HerramientaSoportada.PMD),
                        adaptadorFallido(HerramientaSoportada.CHECKSTYLE)),
                SOLICITUD);

        assertEquals(HerramientaSoportada.PMD, resultado.exitosos().get(0).herramienta());
        assertEquals(HerramientaSoportada.CHECKSTYLE, resultado.fallidos().get(0).herramienta());
        assertTrue(resultado.fallidos().get(0).motivo().contains("Checkstyle"));
    }

    @Test
    @DisplayName("una lista vacia de adaptadores no produce ni exitosos ni fallidos")
    void listaVaciaNoProduceResultados() {
        var orquestador = new OrquestadorAnalisis();
        var resultado = orquestador.ejecutarTodas(List.of(), SOLICITUD);

        assertEquals(0, resultado.exitosos().size());
        assertEquals(0, resultado.fallidos().size());
    }
}
