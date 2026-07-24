package ec.edu.unibe.orquestador.dominio.modelo;

import java.time.LocalDateTime;

/**
 * Persona que usa el orquestador para ejecutar y revisar corridas de
 * analisis. Registrar quien ejecuta cada corrida es parte de la
 * trazabilidad del instrumento, igual que la version exacta de cada
 * herramienta.
 *
 * <p>La contrasena nunca se guarda en texto plano: {@code hashContrasena}
 * es el resultado de {@link GestorContrasenas#calcularHash} sobre la
 * contrasena real y la {@code sal} de este mismo evaluador.</p>
 *
 * @param nombreUsuario  identificador con el que inicia sesion
 * @param hashContrasena contrasena verificada por hash, nunca en texto plano
 * @param sal            sal aleatoria usada para calcular el hash
 * @param fechaRegistro  momento en que se creo la cuenta
 */
public record Evaluador(String nombreUsuario, String hashContrasena, String sal, LocalDateTime fechaRegistro) {

    public Evaluador {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (hashContrasena == null || hashContrasena.isBlank()) {
            throw new IllegalArgumentException("El hash de la contrasena es obligatorio.");
        }
        if (sal == null || sal.isBlank()) {
            throw new IllegalArgumentException("La sal es obligatoria.");
        }
        if (fechaRegistro == null) {
            throw new IllegalArgumentException("La fecha de registro es obligatoria.");
        }
        nombreUsuario = nombreUsuario.trim();
    }
}
