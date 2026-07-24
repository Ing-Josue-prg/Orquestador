package ec.edu.unibe.orquestador.adaptadores;

import ec.edu.unibe.orquestador.dominio.contratos.AnalisisException;
import ec.edu.unibe.orquestador.dominio.contratos.AnalizadorEstatico;
import ec.edu.unibe.orquestador.dominio.contratos.ResultadoAnalisis;
import ec.edu.unibe.orquestador.dominio.contratos.SolicitudAnalisis;
import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import java.util.ArrayList;
import java.util.List;

/**
 * Corre varios adaptadores de analisis sobre la misma solicitud y en una
 * sola corrida, para que el usuario compare las herramientas aplicables a un
 * proyecto con un solo boton en vez de una a la vez.
 *
 * <p>Si una herramienta falla, no detiene a las demas: cada resultado o
 * fallo queda registrado por separado. Un analisis comparativo con tres de
 * cuatro herramientas exitosas sigue siendo evidencia valida.</p>
 */
public final class OrquestadorAnalisis {

    /** Resultado exitoso de una herramienta dentro de la corrida conjunta. */
    public record ResultadoCorrida(HerramientaSoportada herramienta, ResultadoAnalisis resultado) {
    }

    /** Herramienta que no pudo completar el analisis, y por que. */
    public record ResultadoFallido(HerramientaSoportada herramienta, String motivo) {
    }

    /** Los resultados de la corrida conjunta, separados en exitosos y fallidos. */
    public record ResultadoConjunto(List<ResultadoCorrida> exitosos, List<ResultadoFallido> fallidos) {
    }

    /**
     * Ejecuta cada adaptador recibido sobre la misma solicitud.
     *
     * @param adaptadores adaptadores ya construidos y listos para usarse
     * @param solicitud   proyecto, carpeta, commit y numero de corrida comunes
     * @return los resultados exitosos y los fallidos de la corrida conjunta
     */
    public ResultadoConjunto ejecutarTodas(List<AnalizadorEstatico> adaptadores,
                                           SolicitudAnalisis solicitud) {
        List<ResultadoCorrida> exitosos = new ArrayList<>();
        List<ResultadoFallido> fallidos = new ArrayList<>();

        for (AnalizadorEstatico adaptador : adaptadores) {
            try {
                ResultadoAnalisis resultado = adaptador.analizar(solicitud);
                exitosos.add(new ResultadoCorrida(adaptador.herramienta(), resultado));
            } catch (AnalisisException e) {
                fallidos.add(new ResultadoFallido(adaptador.herramienta(), e.getMessage()));
            }
        }

        return new ResultadoConjunto(exitosos, fallidos);
    }
}
