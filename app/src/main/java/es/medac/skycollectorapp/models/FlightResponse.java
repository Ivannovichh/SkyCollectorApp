package es.medac.skycollectorapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// Clase principal que representa la respuesta completa de la API
public class FlightResponse {

    // @SerializedName conecta el nombre "time" del JSON con nuestra variable Java.
    // Sirve para saber en qué segundo se tomaron estos datos.
    @SerializedName("time")
    private int time;

    // OpenSky devuelve una lista llamada "states".
    // Dentro, cada avión NO es un objeto {}, sino una lista de datos [] (List<Object>).
    // Por eso usamos List<List<Object>>: Una lista de listas.
    @SerializedName("states")
    private List<List<Object>> states;

    // Getter para poder acceder a la lista desde fuera
    public List<List<Object>> getStates() {
        return states;
    }

    // --- CLASE INTERNA ---
    // Esta clase sirve para convertir esa lista desordenada [ID, Nombre, País...]
    // en un objeto con nombre y apellidos: avion.callsign, avion.latitude...
    public static class OpenSkyAvion {
        public String icao24;       // Matrícula técnica única (hexadecimal)
        public String callsign;     // Nombre del vuelo (ej: IBE3452)
        public String originCountry;// País de registro
        public Double longitude;    // Coordenada X
        public Double latitude;     // Coordenada Y
        public Double velocity;     // Velocidad
        public Double altitude;     // Altura
        public Float trueTrack;     // RUMBO (Dirección de la nariz del avión 0-360º)

        // Constructor: Aquí ocurre la "magia" de la conversión manual.
        // Recibe la lista "sucia" (rawData) y saca los datos posición por posición.
        public OpenSkyAvion(List<Object> rawData) {
            try {
                // La API dice que en la posición 0 siempre viene el ID (String)
                this.icao24 = (String) rawData.get(0);

                // En la pos 1 viene el Callsign. A veces viene con espacios extra, el .trim() los limpia.
                this.callsign = rawData.get(1) != null ? ((String) rawData.get(1)).trim() : "N/A";

                // En la pos 2 viene el país
                this.originCountry = (String) rawData.get(2);

                // IMPORTANTE: Los números en JSON pueden venir como Integer o Double.
                // Usamos ((Number) ...).doubleValue() para evitar errores de cast si cambia el formato.
                this.longitude = rawData.get(5) != null ? ((Number) rawData.get(5)).doubleValue() : 0.0;
                this.latitude = rawData.get(6) != null ? ((Number) rawData.get(6)).doubleValue() : 0.0;
                this.altitude = rawData.get(7) != null ? ((Number) rawData.get(7)).doubleValue() : 0.0;
                this.velocity = rawData.get(9) != null ? ((Number) rawData.get(9)).doubleValue() : 0.0;

                // Posición 10: El rumbo para rotar el icono en el mapa.
                // Lo convertimos a float porque la rotación del mapa usa floats.
                this.trueTrack = rawData.get(10) != null ? ((Number) rawData.get(10)).floatValue() : 0.0f;

            } catch (Exception e) {
                // Si falla algo (datos corruptos), ponemos "Error" para que la app no se cierre (crash).
                this.callsign = "Error";
            }
        }
    }
}