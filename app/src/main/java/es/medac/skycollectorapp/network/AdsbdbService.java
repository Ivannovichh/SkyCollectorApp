// Declaración del paquete encargado de las comunicaciones de red
package es.medac.skycollectorapp.network;

// Importación del modelo de datos para la respuesta de la API de detalles
import es.medac.skycollectorapp.models.AdsbdbResponse;
// Importación de la clase Call para gestionar peticiones asíncronas de Retrofit
import retrofit2.Call;
// Importación de la anotación para definir peticiones de tipo GET
import retrofit2.http.GET;
// Importación de la anotación para insertar valores en la ruta de la URL
import retrofit2.http.Path;

// Definición de la interfaz que establece los puntos de conexión con la API de detalles de aeronaves
public interface AdsbdbService {

    // Definición de una petición GET para obtener los datos de una aeronave concreta
    @GET("v0/aircraft/{mode_s}")
    // Declaración del método para consultar una aeronave por su código ICAO de 24 bits
    Call<AdsbdbResponse> getAeronave(
            // Código ICAO de 24 bits de la aeronave
            @Path("mode_s") String modeS
    );

    // Definición de una petición GET para obtener la aerolínea y la ruta de un vuelo
    @GET("v0/callsign/{callsign}")
    // Declaración del método para consultar una ruta por el indicativo del vuelo
    Call<AdsbdbResponse> getRuta(
            // Indicativo del vuelo (por ejemplo VLG1234)
            @Path("callsign") String callsign
    );
}