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
        public String icao24;
        public String callsign;
        public String originCountry;

        // Posición
        public Double longitude;
        public Double latitude;
        public Double altitude;

        // Movimiento
        public Double velocity;
        public Float trueTrack;

        public boolean valido = false;

        public OpenSkyAvion(List<Object> raw) {
            if (raw == null || raw.size() < 11) return;

            try {
                icao24 = raw.get(0) != null ? raw.get(0).toString() : null;

                callsign = raw.get(1) != null
                        ? raw.get(1).toString().trim()
                        : null;

                originCountry = raw.get(2) != null
                        ? raw.get(2).toString()
                        : null;

                longitude = raw.get(5) instanceof Number
                        ? ((Number) raw.get(5)).doubleValue()
                        : null;

                latitude = raw.get(6) instanceof Number
                        ? ((Number) raw.get(6)).doubleValue()
                        : null;

                Double baroAlt = raw.get(7) instanceof Number
                        ? ((Number) raw.get(7)).doubleValue()
                        : null;

                Double geoAlt = raw.size() > 13 && raw.get(13) instanceof Number
                        ? ((Number) raw.get(13)).doubleValue()
                        : null;

                altitude = geoAlt != null ? geoAlt : baroAlt;

                velocity = raw.get(9) instanceof Number
                        ? ((Number) raw.get(9)).doubleValue()
                        : 0.0;

                trueTrack = raw.get(10) instanceof Number
                        ? ((Number) raw.get(10)).floatValue()
                        : 0f;

                valido = latitude != null && longitude != null && icao24 != null;

            } catch (Exception e) {
                valido = false;
            }
        }

        public boolean esValido() {
            return valido;
        }

        public String getCallsignSeguro() {
            return callsign != null ? callsign : "";
        }
    }
}
