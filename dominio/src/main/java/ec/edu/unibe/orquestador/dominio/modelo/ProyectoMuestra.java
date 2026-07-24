package ec.edu.unibe.orquestador.dominio.modelo;

/**
 * Proyecto open-source que integra el banco de pruebas de la investigacion.
 *
 * <p>Los proyectos no son el objeto de estudio: son el codigo real y comun
 * sobre el que se ejecutan todas las herramientas, de modo que las
 * diferencias observadas se atribuyan a las herramientas y no al codigo.</p>
 *
 * @param id        identificador corto usado internamente
 * @param nombre    nombre del repositorio
 * @param url       direccion publica del repositorio
 * @param lenguaje  lenguaje principal
 * @param licencia  licencia del proyecto
 */
public record ProyectoMuestra(String id, String nombre, String url, String lenguaje, String licencia) {

    public ProyectoMuestra {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del proyecto es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio.");
        }
        if (lenguaje == null || lenguaje.isBlank()) {
            throw new IllegalArgumentException("El lenguaje del proyecto es obligatorio.");
        }
    }

    @Override
    public String toString() {
        return nombre + " (" + lenguaje + ")";
    }
}
