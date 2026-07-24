package ec.edu.unibe.orquestador.dominio.modelo;

import java.util.List;

/**
 * Proyectos livianos para probar el flujo completo (descarga, analisis,
 * tablero) durante el desarrollo, sin esperar la descarga ni la instalacion
 * de dependencias de los proyectos grandes de la muestra. Tambien incluye
 * un proyecto "poliglota" (Java + TypeScript en el mismo repositorio) para
 * mostrar las cuatro herramientas corriendo juntas sobre un mismo proyecto,
 * algo que no ocurre con la muestra oficial porque ningun proyecto de esa
 * muestra combina ambos lenguajes.
 *
 * <p><b>No son parte de la muestra de la investigacion.</b> La muestra fija
 * esta en {@link CatalogoMuestra} y es la unica que alimenta el capitulo de
 * resultados de la tesis; sus criterios de seleccion constan en el marco
 * metodologico. Estos proyectos solo sirven para verificar que el
 * instrumento funciona de punta a punta y para demostrarlo, por eso su
 * nombre queda marcado con el prefijo "[prueba]" en todas las pantallas e
 * informes.</p>
 */
public final class CatalogoPruebas {

    private static final List<ProyectoMuestra> PROYECTOS = List.of(
            new ProyectoMuestra("prueba-java", "[prueba] spring-guides/gs-rest-service",
                    "https://github.com/spring-guides/gs-rest-service", "Java", "Apache 2.0"),
            new ProyectoMuestra("prueba-js", "[prueba] expressjs/express",
                    "https://github.com/expressjs/express", "JavaScript", "MIT"),
            new ProyectoMuestra("prueba-poliglota", "[prueba] jhipster/jhipster-sample-app-react",
                    "https://github.com/jhipster/jhipster-sample-app-react",
                    "Java, TypeScript", "Apache-2.0"));

    private CatalogoPruebas() {
        // Clase de utilidad: no se instancia.
    }

    /** Proyectos livianos solo para pruebas de desarrollo, no para la investigacion. */
    public static List<ProyectoMuestra> proyectos() {
        return PROYECTOS;
    }
}
