package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.ProyectoMuestra;
import java.nio.file.Path;

/**
 * Peticion de analisis dirigida a un adaptador: que proyecto analizar, donde
 * esta su codigo en el equipo, sobre que confirmacion y en que corrida.
 *
 * @param proyecto      proyecto del catalogo de la muestra
 * @param carpeta       carpeta local donde esta clonado el codigo
 * @param commit        confirmacion analizada, para la trazabilidad
 * @param numeroCorrida numero de corrida (1, 2 o 3)
 */
public record SolicitudAnalisis(ProyectoMuestra proyecto, Path carpeta, String commit, int numeroCorrida) {

    public SolicitudAnalisis {
        if (proyecto == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio.");
        }
        if (carpeta == null) {
            throw new IllegalArgumentException("La carpeta del codigo es obligatoria.");
        }
        if (numeroCorrida < 1) {
            throw new IllegalArgumentException("El numero de corrida debe ser mayor o igual a 1.");
        }
        commit = (commit == null || commit.isBlank()) ? "sin-registrar" : commit.trim();
    }
}
