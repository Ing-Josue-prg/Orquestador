package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Muestra fija de la investigacion: los cuatro proyectos open-source que
 * sirven de banco de pruebas y las herramientas aplicables a cada uno.
 *
 * <p>La muestra es intencional y sus criterios de seleccion constan en el
 * marco metodologico: mas de diez mil estrellas, actividad reciente,
 * licencia permisiva y tamano abordable.</p>
 */
public final class CatalogoMuestra {

    private static final List<ProyectoMuestra> PROYECTOS = List.of(
            new ProyectoMuestra("guava", "google/guava",
                    "https://github.com/google/guava", "Java", "Apache 2.0"),
            new ProyectoMuestra("retrofit", "square/retrofit",
                    "https://github.com/square/retrofit", "Java", "Apache 2.0"),
            new ProyectoMuestra("axios", "axios/axios",
                    "https://github.com/axios/axios", "JavaScript", "MIT"),
            new ProyectoMuestra("date-fns", "date-fns/date-fns",
                    "https://github.com/date-fns/date-fns", "TypeScript", "MIT"));

    private CatalogoMuestra() {
        // Clase de utilidad: no se instancia.
    }

    /** Los cuatro proyectos de la muestra. */
    public static List<ProyectoMuestra> proyectos() {
        return PROYECTOS;
    }

    /** Busca un proyecto de la muestra por su identificador. */
    public static Optional<ProyectoMuestra> porId(String id) {
        return PROYECTOS.stream().filter(p -> p.id().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Herramientas que pueden analizar el proyecto indicado, segun la
     * correspondencia herramienta-lenguaje del marco metodologico.
     */
    public static List<HerramientaSoportada> herramientasPara(ProyectoMuestra proyecto) {
        return Arrays.stream(HerramientaSoportada.values())
                .filter(h -> h.aplicaA(proyecto.lenguaje()))
                .toList();
    }
}
