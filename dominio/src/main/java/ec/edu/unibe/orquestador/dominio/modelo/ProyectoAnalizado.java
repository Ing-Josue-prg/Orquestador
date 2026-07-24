package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Proyecto open-source usado como objeto de prueba. La confirmacion (commit)
 * fija el estado exacto del codigo, de modo que las tres corridas observen
 * la misma version, segun el procedimiento del marco metodologico.
 *
 * @param nombre   nombre del repositorio
 * @param lenguaje lenguaje principal del proyecto
 * @param commit   identificador de la confirmacion analizada
 */
public record ProyectoAnalizado(String nombre, String lenguaje, String commit) {

    public ProyectoAnalizado {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio.");
        }
        if (lenguaje == null || lenguaje.isBlank()) {
            throw new IllegalArgumentException("El lenguaje del proyecto es obligatorio.");
        }
        if (commit == null || commit.isBlank()) {
            throw new IllegalArgumentException("La confirmacion (commit) es obligatoria.");
        }
        nombre = nombre.trim();
        lenguaje = lenguaje.trim();
        commit = commit.trim();
    }

    @Override
    public String toString() {
        return nombre + " (" + lenguaje + ") @ " + commit;
    }
}
