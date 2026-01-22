package es.medac.skycollectorapp;
// INTERFAZ DE RETROFIT PARA LA API

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface FlightRadarService {
    // Ejemplo de llamada para obtener vuelos en un área (latitud/longitud)
    @GET("/flights/listInBounds")
    Call<FlightResponse> getVuelosCercanos(
            @Query("bounds") String bounds // coordenadas del mapa
    );
}

