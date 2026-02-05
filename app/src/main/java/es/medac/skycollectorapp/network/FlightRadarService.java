package es.medac.skycollectorapp.network; // O .network

import es.medac.skycollectorapp.models.FlightResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FlightRadarService {

    // 1. Obtener todos los aviones en la zona (Lo que ya usábamos)
    @GET("states/all")
    Call<FlightResponse> getVuelosEnZona(
            @Query("lamin") double lamin,
            @Query("lomin") double lomin,
            @Query("lamax") double lamax,
            @Query("lomax") double lomax
    );

    // 2. NUEVO: Obtener la ruta completa de un avión específico
    // time=0 le dice a la API: "Dame la trayectoria del vuelo actual/último"
    @GET("tracks/")
    Call<es.medac.skycollectorapp.activities.TrackResponse> getTrayectoria(
            @Query("icao24") String icao24,
            @Query("time") int time
    );
}