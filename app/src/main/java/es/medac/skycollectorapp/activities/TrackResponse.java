package es.medac.skycollectorapp.activities; // O es.medac.skycollectorapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TrackResponse {

    @SerializedName("icao24")
    public String icao24;

    @SerializedName("startTime")
    public int startTime;

    @SerializedName("endTime")
    public int endTime;

    // OpenSky devuelve el camino como una lista de listas:
    // [Time, Latitude, Longitude, Altitude, TrueTrack, OnGround]
    @SerializedName("path")
    public List<List<Object>> path;
}