package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.Arrays;
import java.util.Set;

/**
 * Herramientas que conforman la muestra de la investigacion, con el lenguaje
 * que analiza cada una. La correspondencia herramienta-lenguaje esta fijada
 * en el marco metodologico y evita comparaciones invalidas.
 */
public enum HerramientaSoportada {

    SONARQUBE("SonarQube", Set.of("Java", "JavaScript", "TypeScript")),
    PMD("PMD", Set.of("Java")),
    CHECKSTYLE("Checkstyle", Set.of("Java")),
    ESLINT("ESLint", Set.of("JavaScript", "TypeScript"));

    private final String nombre;
    private final Set<String> lenguajes;

    HerramientaSoportada(String nombre, Set<String> lenguajes) {
        this.nombre = nombre;
        this.lenguajes = lenguajes;
    }

    public String nombre() {
        return nombre;
    }

    public Set<String> lenguajes() {
        return lenguajes;
    }

    /**
     * Indica si esta herramienta puede analizar proyectos del lenguaje dado.
     * Un proyecto puede declarar varios lenguajes separados por coma (por
     * ejemplo, "Java, TypeScript" para un proyecto con backend Java y
     * frontend TypeScript en el mismo repositorio); alcanza con que uno
     * coincida.
     */
    public boolean aplicaA(String lenguajeProyecto) {
        if (lenguajeProyecto == null || lenguajeProyecto.isBlank()) {
            return false;
        }
        return Arrays.stream(lenguajeProyecto.split(","))
                .map(String::trim)
                .anyMatch(unoDelProyecto -> lenguajes.stream()
                        .anyMatch(unoDeLaHerramienta -> unoDeLaHerramienta.equalsIgnoreCase(unoDelProyecto)));
    }

    @Override
    public String toString() {
        return nombre;
    }
}
