// Se mantiene esta clase para permitir la escalabilidad en un futuro.


// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.activities;

// Importación de la anotación para mapear nombres de claves JSON a variables Java
import com.google.gson.annotations.SerializedName;
// Importación de la interfaz para el manejo de listas de elementos
import java.util.List;

// Definición de la clase pública que representa la respuesta de trayectoria de un vuelo
public class TrackResponse {

    // Indica que el campo JSON "icao24" se asignará a la variable siguiente
    @SerializedName("icao24")
    // Variable para almacenar el identificador hexadecimal único de la aeronave
    public String icao24;

    // Indica que el campo JSON "startTime" se asignará a la variable siguiente
    @SerializedName("startTime")
    // Variable para almacenar el tiempo de inicio del seguimiento en formato numérico
    public int startTime;

    // Indica que el campo JSON "endTime" se asignará a la variable siguiente
    @SerializedName("endTime")
    // Variable para almacenar el tiempo de finalización del seguimiento en formato numérico
    public int endTime;

    // Indica que el campo JSON "path" se asignará a la variable siguiente
    @SerializedName("path")
    // Lista anidada de objetos que contiene los puntos geográficos y datos técnicos del trayecto
    public List<List<Object>> path;
}