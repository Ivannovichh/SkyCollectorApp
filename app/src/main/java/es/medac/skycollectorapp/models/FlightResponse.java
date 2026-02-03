package es.medac.skycollectorapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlightResponse {

    @SerializedName("states")
    private List<List<Object>> states;

    public List<List<Object>> getStates() {
        return states;
    }

    public static class OpenSkyAvion {

        // Identificadores
        public String icao24;           // ID único del transpondedor
        public String callsign;         // Código de vuelo (puede ser null)
        public String originCountry;    // País de origen

        // Posición y movimiento
        public Double longitude;        // null si no disponible
        public Double latitude;         // null si no disponible
        public Double altitude;         // metros (geo si existe)
        public Double velocity;         // m/s
        public Float trueTrack;          // grados (rumbo)

        public OpenSkyAvion(List<Object> rawData) {

            // Seguridad básica
            if (rawData == null || rawData.size() < 7) return;

            try {
                // 0 → ICAO24
                this.icao24 = rawData.get(0) != null ? (String) rawData.get(0) : null;

                // 1 → Callsign (a veces con espacios)
                this.callsign = rawData.get(1) != null
                        ? ((String) rawData.get(1)).trim()
                        : "N/A";

                // 2 → País de origen
                this.originCountry = rawData.get(2) != null
                        ? (String) rawData.get(2)
                        : "N/A";

                // 5 → Longitud
                this.longitude = rawData.get(5) != null
                        ? ((Number) rawData.get(5)).doubleValue()
                        : null;

                // 6 → Latitud
                this.latitude = rawData.get(6) != null
                        ? ((Number) rawData.get(6)).doubleValue()
                        : null;

                // 13 → Altitud geométrica (preferida)
                Double geoAlt = (rawData.size() > 13 && rawData.get(13) != null)
                        ? ((Number) rawData.get(13)).doubleValue()
                        : null;

                // 7 → Altitud barométrica (fallback)
                Double baroAlt = rawData.get(7) != null
                        ? ((Number) rawData.get(7)).doubleValue()
                        : null;

                this.altitude = geoAlt != null ? geoAlt : baroAlt;

                // 9 → Velocidad (m/s)
                this.velocity = rawData.get(9) != null
                        ? ((Number) rawData.get(9)).doubleValue()
                        : 0.0;

                // 10 → Rumbo real
                this.trueTrack = rawData.get(10) != null
                        ? ((Number) rawData.get(10)).floatValue()
                        : 0.0f;

            } catch (Exception e) {
                // Si algo falla, dejamos el avión inválido (no se pintará)
                this.latitude = null;
                this.longitude = null;
            }
        }
    }

}