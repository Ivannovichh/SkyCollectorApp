// Declaración del paquete encargado de las comunicaciones de red
package es.medac.skycollectorapp.network;

// Importación del modelo de datos para la respuesta de la API de tráfico aéreo
import es.medac.skycollectorapp.models.AdsbResponse;
// Importación de la clase Call para gestionar peticiones asíncronas de Retrofit
import retrofit2.Call;
// Importación de la anotación para definir peticiones de tipo GET
import retrofit2.http.GET;
// Importación de la anotación para insertar valores en la ruta de la URL
import retrofit2.http.Path;

// Definición de la interfaz que establece los puntos de conexión con la API de tráfico aéreo
public interface AdsbService {

    // Definición de una petición GET para obtener los aviones dentro de un radio alrededor de un punto
    @GET("v2/point/{lat}/{lon}/{radio}")
    // Declaración del método para consultar vuelos alrededor de unas coordenadas
    Call<AdsbResponse> getVuelosEnZona(
            // Latitud del centro de la zona
            @Path("lat") String lat,
            // Longitud del centro de la zona
            @Path("lon") String lon,
            // Radio de la zona en millas náuticas (máximo 250)
            @Path("radio") int radio
    );
}