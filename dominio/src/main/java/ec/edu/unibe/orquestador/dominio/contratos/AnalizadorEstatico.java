package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.HerramientaSoportada;
import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import ec.edu.unibe.orquestador.dominio.modelo.VerificacionEntorno;

/**
 * Contrato comun que implementa cada adaptador de herramienta: SonarQube,
 * PMD, Checkstyle y ESLint.
 *
 * <p>Es la pieza que hace extensible el sistema: incorporar una herramienta
 * nueva equivale a escribir una clase que implemente esta interfaz, sin
 * modificar el dominio ni la interfaz de usuario.</p>
 */
public interface AnalizadorEstatico {

    /** Herramienta de la muestra que representa este adaptador. */
    HerramientaSoportada herramienta();

    /** Comprueba que la herramienta este instalada y detecta su version. */
    VerificacionEntorno verificar();

    /** Indica si el adaptador puede analizar el proyecto dado. */
    default boolean soporta(ProyectoMuestra proyecto) {
        return herramienta().aplicaA(proyecto.lenguaje());
    }

    /**
     * Ejecuta el analisis y devuelve mediciones, hallazgos y desempeno.
     *
     * @param solicitud datos de la corrida solicitada
     * @return el resultado completo de la corrida
     * @throws AnalisisException si la herramienta falla o su reporte no puede leerse
     */
    ResultadoAnalisis analizar(SolicitudAnalisis solicitud) throws AnalisisException;
}
