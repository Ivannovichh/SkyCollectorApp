package es.medac.skycollectorapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlightResponse {

    @SerializedName("states")
    private List<List<Object>> states;

    public List<List<Object>> getStates() {
        return states;
    }

    // Clase interna para procesar los datos crudos de la API
    public static class OpenSkyAvion {
        public String icao24;       // MATRÍCULA TÉCNICA (REAL)
        public String callsign;     // CÓDIGO DE VUELO (REAL)
        public String originCountry;
        public Double longitude;
        public Double latitude;
        public Double velocity;
        public Double altitude;
        public Float trueTrack;     // Rumbo

        public OpenSkyAvion(List<Object> rawData) {
            try {
                // Posición 0: ICAO24 (El ID único del transpondedor del avión)
                this.icao24 = (String) rawData.get(0);

                // Posición 1: Callsign (El número de vuelo, ej: IBE32S)
                // Usamos trim() porque la API a veces mete espacios en blanco al final
                this.callsign = rawData.get(1) != null ? ((String) rawData.get(1)).trim() : "N/A";

                // Posición 2: País de origen
                this.originCountry = (String) rawData.get(2);

                // Posiciones numéricas (seguridad para evitar errores si vienen nulos)
                this.longitude = rawData.get(5) != null ? ((Number) rawData.get(5)).doubleValue() : 0.0;
                this.latitude = rawData.get(6) != null ? ((Number) rawData.get(6)).doubleValue() : 0.0;
                this.altitude = rawData.get(7) != null ? ((Number) rawData.get(7)).doubleValue() : 0.0;
                this.velocity = rawData.get(9) != null ? ((Number) rawData.get(9)).doubleValue() : 0.0;
                this.trueTrack = rawData.get(10) != null ? ((Number) rawData.get(10)).floatValue() : 0.0f;

            } catch (Exception e) {
                this.callsign = "ErrorDatos";
            }
        }
    }
}