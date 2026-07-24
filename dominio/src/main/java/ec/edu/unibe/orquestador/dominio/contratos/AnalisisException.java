package ec.edu.unibe.orquestador.dominio.contratos;

/**
 * Error ocurrido durante la ejecucion de una herramienta de analisis:
 * la herramienta no esta instalada, termino con codigo de error, o su
 * reporte no pudo interpretarse.
 */
public class AnalisisException extends Exception {

    public AnalisisException(String mensaje) {
        super(mensaje);
    }

    public AnalisisException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
