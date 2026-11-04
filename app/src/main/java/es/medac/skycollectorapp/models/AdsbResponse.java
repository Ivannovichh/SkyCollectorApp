// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación de la anotación para vincular nombres de campos JSON con variables Java
import com.google.gson.annotations.SerializedName;
// Importación de la interfaz para manejar listas de elementos
import java.util.List;

// Clase que representa la respuesta de las APIs de tráfico aéreo con formato ADSBExchange v2 (adsb.lol, airplanes.live)
public class AdsbResponse {

    // Lista de aviones de la respuesta (la clave habitual es "ac")
    @SerializedName(value = "ac", alternate = {"aircraft"})
    private List<AdsbAircraft> aviones;

    // Método para obtener la lista de aviones de la respuesta
    public List<AdsbAircraft> getAviones() {
        return aviones;
    }

    // Clase interna que representa un avión tal y como lo envía la API
    public static class AdsbAircraft {

        // Factor para convertir pies a metros
        private static final double PIES_A_METROS = 0.3048;
        // Factor para convertir nudos a metros por segundo
        private static final double NUDOS_A_MS = 0.514444;

        // Identificador ICAO de 24 bits en hexadecimal
        @SerializedName("hex")
        public String hex;
        // Indicativo del vuelo (viene con espacios al final)
        @SerializedName("flight")
        public String flight;
        // Matrícula de la aeronave
        @SerializedName("r")
        public String r;
        // Código ICAO del tipo de aeronave
        @SerializedName("t")
        public String t;
        // Descripción del modelo de la aeronave
        @SerializedName("desc")
        public String desc;
        // Latitud en grados
        @SerializedName("lat")
        public Double lat;
        // Longitud en grados
        @SerializedName("lon")
        public Double lon;
        // Altitud barométrica en pies, o el texto "ground" si está en tierra
        @SerializedName("alt_baro")
        public Object altBaro;
        // Velocidad respecto al suelo en nudos
        @SerializedName("gs")
        public Double gs;
        // Rumbo sobre el suelo en grados
        @SerializedName("track")
        public Double track;

        // Convertir el avión de la API al modelo Vuelo, con las unidades que usa la app
        public Vuelo toVuelo() {
            Vuelo v = new Vuelo(); // Crear el vuelo vacío

            v.icao24 = hex; // Identificador del avión
            v.callsign = flight != null ? flight.trim() : null; // Quitar los espacios sobrantes del indicativo
            v.matricula = r; // Matrícula
            v.tipo = t; // Tipo de aeronave
            v.descripcion = desc; // Descripción del modelo
            v.latitude = lat; // Latitud
            v.longitude = lon; // Longitud

            if (altBaro instanceof Number) { // Si la altitud es un número
                v.altitude = ((Number) altBaro).doubleValue() * PIES_A_METROS; // Convertir de pies a metros
            } else if ("ground".equals(altBaro)) { // Si la API indica que está en tierra
                v.enTierra = true; // Marcar el avión como en tierra
                v.altitude = 0.0; // Altitud cero
            }

            v.velocity = gs != null ? gs * NUDOS_A_MS : 0.0; // Convertir de nudos a metros por segundo
            v.trueTrack = track != null ? track.floatValue() : 0f; // Rumbo, cero si no hay dato

            return v; // Devolver el vuelo convertido
        }
    }
}