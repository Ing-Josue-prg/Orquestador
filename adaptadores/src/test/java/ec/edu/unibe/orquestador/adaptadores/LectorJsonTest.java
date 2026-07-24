package ec.edu.unibe.orquestador.adaptadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LectorJson")
class LectorJsonTest {

    @Test
    @DisplayName("lee un objeto con cadenas, numeros y booleanos")
    @SuppressWarnings("unchecked")
    void leeObjetoSimple() {
        Object valor = LectorJson.leer("""
                {"nombre": "Cuenta", "lineas": 42, "activo": true, "detalle": null}
                """);

        Map<String, Object> objeto = (Map<String, Object>) valor;
        assertEquals("Cuenta", objeto.get("nombre"));
        assertEquals(42.0, objeto.get("lineas"));
        assertEquals(true, objeto.get("activo"));
        assertNull(objeto.get("detalle"));
    }

    @Test
    @DisplayName("lee un arreglo de objetos anidados")
    @SuppressWarnings("unchecked")
    void leeArregloAnidado() {
        Object valor = LectorJson.leer("""
                [{"archivo": "a.js", "mensajes": [1, 2, 3]}, {"archivo": "b.js", "mensajes": []}]
                """);

        List<Object> arreglo = (List<Object>) valor;
        assertEquals(2, arreglo.size());

        Map<String, Object> primero = (Map<String, Object>) arreglo.get(0);
        assertEquals("a.js", primero.get("archivo"));
        List<Object> mensajes = (List<Object>) primero.get("mensajes");
        assertEquals(3, mensajes.size());
    }

    @Test
    @DisplayName("interpreta escapes de cadenas, incluida la barra invertida y el unicode")
    @SuppressWarnings("unchecked")
    void interpretaEscapes() {
        Object valor = LectorJson.leer("{\"mensaje\": \"Linea 1\\nRuta: C:\\\\proyecto\\nEmoji: \\u0041\"}");

        Map<String, Object> objeto = (Map<String, Object>) valor;
        String mensaje = (String) objeto.get("mensaje");
        assertTrue(mensaje.contains("Linea 1\nRuta: C:\\proyecto"));
        assertTrue(mensaje.endsWith("A"));
    }

    @Test
    @DisplayName("un arreglo vacio se lee como lista vacia")
    void arregloVacio() {
        assertEquals(List.of(), LectorJson.leer("[]"));
    }
}
