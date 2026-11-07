// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación de la anotación para vincular nombres de campos JSON con variables Java
import com.google.gson.annotations.SerializedName;

// Clase que representa la respuesta de la API adsbdb con los datos de la aeronave, la aerolínea y la ruta
public class AdsbdbResponse {

    // Contenedor principal de los datos devueltos por la API
    @SerializedName("response")
    private Respuesta respuesta;

    // Método para obtener el contenido de la respuesta
    public Respuesta getRespuesta() {
        return respuesta;
    }

    // Clase interna que agrupa los datos de la aeronave y de su ruta
    public static class Respuesta {
        // Datos de la aeronave concreta
        @SerializedName("aircraft")
        public Aeronave aircraft;
        // Datos de la ruta del vuelo (solo si el indicativo es conocido)
        @SerializedName("flightroute")
        public RutaVuelo flightroute;
    }

    // Clase interna con los datos de una aeronave
    public static class Aeronave {
        // Modelo completo de la aeronave (por ejemplo A320-252N)
        @SerializedName("type")
        public String type;
        // Código ICAO del tipo de aeronave
        @SerializedName("icao_type")
        public String icaoType;
        // Fabricante de la aeronave
        @SerializedName("manufacturer")
        public String manufacturer;
        // Matrícula de la aeronave
        @SerializedName("registration")
        public String registration;
        // Propietario registrado de la aeronave
        @SerializedName("registered_owner")
        public String registeredOwner;
    }

    // Clase interna con los datos de la ruta de un vuelo
    public static class RutaVuelo {
        // Indicativo del vuelo
        @SerializedName("callsign")
        public String callsign;
        // Aerolínea que opera el vuelo
        @SerializedName("airline")
        public Aerolinea airline;
        // Aeropuerto de origen
        @SerializedName("origin")
        public Aeropuerto origin;
        // Aeropuerto de destino
        @SerializedName("destination")
        public Aeropuerto destination;
    }

    // Clase interna con los datos de una aerolínea
    public static class Aerolinea {
        // Nombre de la aerolínea
        @SerializedName("name")
        public String name;
        // País de la aerolínea
        @SerializedName("country")
        public String country;
    }

    // Clase interna con los datos de un aeropuerto
    public static class Aeropuerto {
        // Ciudad del aeropuerto
        @SerializedName("municipality")
        public String municipality;
        // Nombre del aeropuerto
        @SerializedName("name")
        public String name;
        // Código IATA del aeropuerto
        @SerializedName("iata_code")
        public String iataCode;
        // Latitud del aeropuerto
        @SerializedName("latitude")
        public Double latitude;
        // Longitud del aeropuerto
        @SerializedName("longitude")
        public Double longitude;
    }
}