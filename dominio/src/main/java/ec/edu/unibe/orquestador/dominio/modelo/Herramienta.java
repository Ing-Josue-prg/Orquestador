package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Herramienta de analisis estatico evaluada. Corresponde a una unidad de la
 * muestra: SonarQube, PMD, Checkstyle o ESLint.
 *
 * <p>Se declara como {@code record} porque es un objeto de valor: sus datos
 * no cambian una vez creado y dos herramientas con el mismo nombre y version
 * se consideran iguales.</p>
 *
 * @param nombre  nombre de la herramienta
 * @param version version exacta utilizada en la recoleccion
 */
public record Herramienta(String nombre, String version) {

    public Herramienta {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la herramienta es obligatorio.");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("La version de la herramienta es obligatoria.");
        }
        nombre = nombre.trim();
        version = version.trim();
    }

    @Override
    public String toString() {
        return nombre + " " + version;
    }
}
