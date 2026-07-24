package ec.edu.unibe.orquestador.adaptadores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector minimo de JSON, del tamano justo para leer los reportes de las
 * herramientas que responden en este formato (ESLint, y mas adelante la Web
 * API de SonarQube).
 *
 * <p>No es un parser JSON de proposito general: alcanza para objetos,
 * arreglos, cadenas, numeros, booleanos y nulo, usando solo colecciones de la
 * biblioteca estandar. Se escribio a mano porque el JDK no incluye un lector
 * JSON en su biblioteca estandar, y sumar una libreria externa solo para esto
 * no se justifica (ver CLAUDE.md, "sin dependencias externas innecesarias").</p>
 */
public final class LectorJson {

    private final String texto;
    private int posicion;

    private LectorJson(String texto) {
        this.texto = texto;
        this.posicion = 0;
    }

    /**
     * Interpreta un texto JSON.
     *
     * @return {@code Map<String, Object>} para un objeto, {@code List<Object>}
     *         para un arreglo, o {@code String}/{@code Double}/{@code Boolean}/
     *         {@code null} para el resto de los valores
     */
    public static Object leer(String json) {
        return new LectorJson(json).leerValor();
    }

    private Object leerValor() {
        saltarEspacios();
        char actual = texto.charAt(posicion);
        return switch (actual) {
            case '{' -> leerObjeto();
            case '[' -> leerArreglo();
            case '"' -> leerCadena();
            case 't', 'f' -> leerBooleano();
            case 'n' -> leerNulo();
            default -> leerNumero();
        };
    }

    private Map<String, Object> leerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        posicion++; // consume '{'
        saltarEspacios();
        if (texto.charAt(posicion) == '}') {
            posicion++;
            return objeto;
        }
        while (true) {
            saltarEspacios();
            String clave = leerCadena();
            saltarEspacios();
            posicion++; // consume ':'
            objeto.put(clave, leerValor());
            saltarEspacios();
            if (texto.charAt(posicion++) == '}') {
                break;
            }
        }
        return objeto;
    }

    private List<Object> leerArreglo() {
        List<Object> arreglo = new ArrayList<>();
        posicion++; // consume '['
        saltarEspacios();
        if (texto.charAt(posicion) == ']') {
            posicion++;
            return arreglo;
        }
        while (true) {
            arreglo.add(leerValor());
            saltarEspacios();
            if (texto.charAt(posicion++) == ']') {
                break;
            }
        }
        return arreglo;
    }

    private String leerCadena() {
        posicion++; // consume la comilla de apertura
        StringBuilder resultado = new StringBuilder();
        while (texto.charAt(posicion) != '"') {
            char actual = texto.charAt(posicion);
            if (actual == '\\') {
                posicion++;
                resultado.append(leerEscape());
            } else {
                resultado.append(actual);
            }
            posicion++;
        }
        posicion++; // consume la comilla de cierre
        return resultado.toString();
    }

    private char leerEscape() {
        char escape = texto.charAt(posicion);
        return switch (escape) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'u' -> leerUnicode();
            default -> escape;
        };
    }

    private char leerUnicode() {
        String hex = texto.substring(posicion + 1, posicion + 5);
        posicion += 4;
        return (char) Integer.parseInt(hex, 16);
    }

    private Boolean leerBooleano() {
        boolean esVerdadero = texto.startsWith("true", posicion);
        posicion += esVerdadero ? 4 : 5;
        return esVerdadero;
    }

    private Object leerNulo() {
        posicion += 4;
        return null;
    }

    private Double leerNumero() {
        int inicio = posicion;
        while (posicion < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(posicion)) >= 0) {
            posicion++;
        }
        return Double.parseDouble(texto.substring(inicio, posicion));
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }
}
