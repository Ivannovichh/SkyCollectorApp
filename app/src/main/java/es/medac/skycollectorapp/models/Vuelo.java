// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Clase que representa un vuelo recibido de la API, ya convertido a las unidades que usa la app
public class Vuelo {

    // Identificador hexadecimal único de la aeronave
    public String icao24;
    // Indicativo del vuelo (por ejemplo IBE1234)
    public String callsign;
    // Matrícula de la aeronave
    public String matricula;
    // Código ICAO del tipo de aeronave (por ejemplo A320)
    public String tipo;
    // Descripción del modelo de la aeronave
    public String descripcion;

    // Latitud en grados
    public Double latitude;
    // Longitud en grados
    public Double longitude;
    // Altitud en metros
    public Double altitude;
    // Velocidad en metros por segundo
    public Double velocity;
    // Rumbo en grados respecto al norte
    public Float trueTrack;
    // Indica si el avión está en tierra
    public boolean enTierra;
}