// Declaración del paquete donde se define la clase de respuesta de vuelos
package es.medac.skycollectorapp.models;

// Importación de la anotación para vincular nombres de campos JSON con variables Java
import com.google.gson.annotations.SerializedName;
// Importación de la interfaz para manejar colecciones de elementos ordenados
import java.util.List;

// Definición de la clase pública que actúa como contenedor de la respuesta de la API de vuelos
public class FlightResponse {

    // Especificación del nombre del campo en el JSON original
    @SerializedName("states")
    // Declaración de una lista de listas que contiene los datos crudos de los aviones
    private List<List<Object>> states;

    // Método público para obtener la lista de estados de los vuelos
    public List<List<Object>> getStates() {
        // Devuelve la colección de datos de los aviones recibida
        return states;
    }

    // Definición de una clase estática interna para procesar los datos de un avión individual
    public static class OpenSkyAvion {

        // Variable para el identificador único hexadecimal de la aeronave
        public String icao24;
        // Variable para el código de llamada o identificador de vuelo
        public String callsign;
        // Variable para el nombre del país de origen de la aeronave
        public String originCountry;

        // Variable para la coordenada de longitud geográfica
        public Double longitude;
        // Variable para la coordenada de latitud geográfica
        public Double latitude;
        // Variable para la altitud de la aeronave
        public Double altitude;

        // Variable para la velocidad horizontal de la aeronave
        public Double velocity;
        // Variable para el rumbo o dirección de movimiento respecto al norte
        public Float trueTrack;

        // Variable booleana para indicar si los datos mínimos requeridos son correctos
        public boolean valido = false;

        // Constructor que recibe una lista de objetos y los mapea a las propiedades de la clase
        public OpenSkyAvion(List<Object> raw) {
            // Verificación de seguridad para evitar procesar listas nulas o incompletas
            if (raw == null || raw.size() < 11) return;

            // Bloque de control para gestionar posibles errores durante la conversión de tipos
            try {
                // Extracción y conversión a texto del identificador ICAO
                icao24 = raw.get(0) != null ? raw.get(0).toString() : null;

                // Extracción, conversión y limpieza de espacios del código de llamada
                callsign = raw.get(1) != null
                        ? raw.get(1).toString().trim()
                        : null;

                // Extracción y conversión a texto del país de origen
                originCountry = raw.get(2) != null
                        ? raw.get(2).toString()
                        : null;

                // Validación y extracción de la longitud como un valor numérico doble
                longitude = raw.get(5) instanceof Number
                        ? ((Number) raw.get(5)).doubleValue()
                        : null;

                // Validación y extracción de la latitud como un valor numérico doble
                latitude = raw.get(6) instanceof Number
                        ? ((Number) raw.get(6)).doubleValue()
                        : null;

                // Obtención provisional de la altitud barométrica en la posición ocho
                Double baroAlt = raw.get(7) instanceof Number
                        ? ((Number) raw.get(7)).doubleValue()
                        : null;

                // Obtención de la altitud geométrica si la lista es extensa y el dato existe
                Double geoAlt = raw.size() > 13 && raw.get(13) instanceof Number
                        ? ((Number) raw.get(13)).doubleValue()
                        : null;

                // Priorización de la altitud geométrica sobre la barométrica
                altitude = geoAlt != null ? geoAlt : baroAlt;

                // Validación y extracción de la velocidad con valor por defecto cero
                velocity = raw.get(9) instanceof Number
                        ? ((Number) raw.get(9)).doubleValue()
                        : 0.0;

                // Validación y extracción del rumbo con valor por defecto cero
                trueTrack = raw.get(10) instanceof Number
                        ? ((Number) raw.get(10)).floatValue()
                        : 0f;

                // Evaluación final para determinar si el objeto tiene coordenadas e ID válidos
                valido = latitude != null && longitude != null && icao24 != null;

            } catch (Exception e) {
                // Desactivación de la validez del objeto en caso de fallo inesperado
                valido = false;
            }
        }

        // Método para consultar si el procesamiento del avión fue exitoso
        public boolean esValido() {
            // Retorna el estado actual de la bandera de validación
            return valido;
        }

        // Método para obtener el código de llamada evitando valores nulos
        public String getCallsignSeguro() {
            // Devuelve el código si existe, o una cadena vacía en su lugar
            return callsign != null ? callsign : "";
        }
    }
}