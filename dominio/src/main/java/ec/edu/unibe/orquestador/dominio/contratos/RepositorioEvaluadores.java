package ec.edu.unibe.orquestador.dominio.contratos;

import ec.edu.unibe.orquestador.dominio.modelo.Evaluador;
import java.io.IOException;
import java.util.Optional;

/**
 * Contrato para guardar y recuperar los evaluadores registrados. El
 * dominio conoce que existen evaluadores y como se identifican, sin saber
 * en que formato ni donde quedan almacenados: ese detalle lo resuelve el
 * adaptador.
 */
public interface RepositorioEvaluadores {

    /** Busca un evaluador por su nombre de usuario, sin distinguir mayusculas. */
    Optional<Evaluador> buscarPorNombreUsuario(String nombreUsuario) throws IOException;

    /** Indica si ya existe un evaluador registrado con ese nombre de usuario. */
    boolean existe(String nombreUsuario) throws IOException;

    /** Registra un evaluador nuevo. */
    void guardar(Evaluador evaluador) throws IOException;
}
