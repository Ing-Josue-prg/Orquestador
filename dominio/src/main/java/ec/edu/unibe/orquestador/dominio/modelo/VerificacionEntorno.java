package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Resultado de comprobar si una herramienta esta disponible y operativa en
 * el equipo. Se ejecuta antes de cada corrida para evitar analisis
 * incompletos y para dejar registrada la version exacta en la bitacora.
 *
 * @param herramienta herramienta comprobada
 * @param disponible  verdadero si respondio correctamente
 * @param detalle     version detectada o motivo del fallo
 */
public record VerificacionEntorno(HerramientaSoportada herramienta, boolean disponible, String detalle) {

    public VerificacionEntorno {
        if (herramienta == null) {
            throw new IllegalArgumentException("La herramienta es obligatoria.");
        }
        detalle = (detalle == null) ? "" : detalle.trim();
    }

    public String estado() {
        return disponible ? "Disponible" : "No disponible";
    }
}
