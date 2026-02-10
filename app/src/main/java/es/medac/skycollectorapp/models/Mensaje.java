// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Definición de la clase pública que representa un mensaje individual en el chat
public class Mensaje {

    // Variable para almacenar el texto o cuerpo del mensaje
    private String contenido;
    // Variable booleana para identificar si el mensaje fue enviado por el usuario o por el bot
    private boolean esMio;

    // Constructor de la clase para inicializar el contenido y la procedencia del mensaje
    public Mensaje(String contenido, boolean esMio) {
        // Asignación del texto recibido a la propiedad de la clase
        this.contenido = contenido;
        // Asignación del estado de autoría recibido a la propiedad de la clase
        this.esMio = esMio;
    }

    // Método para obtener el texto almacenado en el mensaje
    public String getContenido() { return contenido; }
    // Método para consultar si el mensaje pertenece al usuario actual
    public boolean isEsMio() { return esMio; }
}