package ec.edu.unibe.orquestador.dominio.modelo;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Calcula y verifica el hash de las contrasenas de los evaluadores con
 * PBKDF2, un algoritmo de la biblioteca estandar de Java (paquete
 * {@code javax.crypto}), sin sumar dependencias externas. La contrasena en
 * texto plano nunca se guarda ni se compara directamente.
 */
public final class GestorContrasenas {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_CLAVE_BITS = 256;
    private static final int LONGITUD_SAL_BYTES = 16;

    private GestorContrasenas() {
        // Clase de utilidad: no se instancia.
    }

    /** Genera una sal aleatoria distinta para cada evaluador nuevo. */
    public static String generarSal() {
        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        new SecureRandom().nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal);
    }

    /** Calcula el hash de una contrasena con la sal indicada. */
    public static String calcularHash(String contrasena, String sal) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("La contrasena es obligatoria.");
        }
        if (sal == null || sal.isBlank()) {
            throw new IllegalArgumentException("La sal es obligatoria.");
        }
        try {
            PBEKeySpec especificacion = new PBEKeySpec(
                    contrasena.toCharArray(), Base64.getDecoder().decode(sal),
                    ITERACIONES, LONGITUD_CLAVE_BITS);
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            byte[] hash = fabrica.generateSecret(especificacion).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contrasena.", e);
        }
    }

    /** Compara una contrasena en texto plano con el hash y la sal ya almacenados. */
    public static boolean coincide(String contrasena, String sal, String hashEsperado) {
        return calcularHash(contrasena, sal).equals(hashEsperado);
    }
}
