// Declaración del paquete encargado de las comunicaciones de red
package es.medac.skycollectorapp.network;

// Importación del modelo de datos para la respuesta general de vuelos
import es.medac.skycollectorapp.models.FlightResponse;
// Importación de la clase Call para gestionar peticiones asíncronas de Retrofit
import retrofit2.Call;
// Importación de la anotación para definir peticiones de tipo GET
import retrofit2.http.GET;
// Importación de la anotación para añadir parámetros de consulta en la URL
import retrofit2.http.Query;

// Definición de la interfaz que establece los puntos de conexión con la API externa
public interface FlightRadarService {

    // Definición de una petición GET para obtener el estado de todos los aviones
    @GET("states/all")
    // Declaración del método para consultar vuelos dentro de un área geográfica delimitada
    Call<FlightResponse> getVuelosEnZona(
            // Parámetro de consulta para establecer la latitud mínima del mapa
            @Query("lamin") double lamin,
            // Parámetro de consulta para establecer la longitud mínima del mapa
            @Query("lomin") double lomin,
            // Parámetro de consulta para establecer la latitud máxima del mapa
            @Query("lamax") double lamax,
            // Parámetro de consulta para establecer la longitud máxima del mapa
            @Query("lomax") double lomax
    );

    // Definición de una petición GET para obtener el rastro o camino de un avión
    @GET("tracks/")
    // Declaración del método para recuperar la trayectoria histórica de una aeronave específica
    Call<es.medac.skycollectorapp.activities.TrackResponse> getTrayectoria(
            // Parámetro de consulta para identificar el avión mediante su código ICAO
            @Query("icao24") String icao24,
            // Parámetro de consulta para definir el instante de tiempo de la consulta
            @Query("time") int time
    );
}