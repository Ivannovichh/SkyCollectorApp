// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación de la interfaz para permitir que el objeto se convierta en una secuencia de bytes
import java.io.Serializable;
// Importación de la utilidad para la generación de identificadores únicos universales
import java.util.UUID;

// Definición de la clase pública Avion que implementa la interfaz de serialización
public class Avion implements Serializable {

    // Declaración de la variable para el identificador único del objeto
    private String id;

    // Declaración de la variable para almacenar el nombre del modelo técnico
    private String modelo;

    // Declaración de la variable para el apodo o nombre descriptivo
    private String apodo;

    // Declaración de la variable para el nombre de la empresa fabricante
    private String fabricante;
    // Declaración de la variable para la categoría de rareza del avión
    private String rareza;
    // Declaración de la variable para el identificador del recurso de imagen
    private int imagenResId;

    // Declaración de la variable para la velocidad máxima del avión
    private String velocidad;
    // Declaración de la variable para la capacidad máxima de personas
    private String pasajeros;
    // Declaración de la variable para las medidas físicas de la aeronave
    private String dimensiones;
    // Declaración de la variable para el país de procedencia u origen
    private String pais;
    // Declaración de la variable para el peso total del avión
    private String peso;
    // Declaración de la variable para el texto descriptivo del avión
    private String descripcion;

    // Declaración de la variable booleana para gestionar el estado de selección
    private boolean seleccionado;

    // Declaración de la variable para la ruta de la imagen propia del usuario
    private String uriFotoUsuario;

    // Declaración de la variable para el código de identificación aeronáutico
    private String icao24;

    // Constructor vacío que se ejecuta al crear una instancia sin parámetros
    public Avion() {
        // Generación automática de un identificador aleatorio único
        this.id = UUID.randomUUID().toString();
        // Inicialización del estado de selección por defecto a falso
        this.seleccionado = false;
    }

    // Constructor con parámetros para inicializar las propiedades básicas del avión
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso) {

        // Generación de un identificador único para la nueva instancia
        this.id = UUID.randomUUID().toString();

        // Asignación del apodo recibido a la propiedad de la clase
        this.apodo = apodo;
        // Asignación del apodo como modelo por defecto para mantener compatibilidad
        this.modelo = apodo;

        // Asignación del fabricante recibido a la propiedad de la clase
        this.fabricante = fabricante;
        // Asignación de la rareza recibida a la propiedad de la clase
        this.rareza = rareza;
        // Asignación del recurso de imagen recibido a la propiedad de la clase
        this.imagenResId = imagenResId;

        // Asignación de la velocidad recibida a la propiedad de la clase
        this.velocidad = velocidad;
        // Asignación de la capacidad de pasajeros a la propiedad de la clase
        this.pasajeros = pasajeros;
        // Asignación de las dimensiones recibidas a la propiedad de la clase
        this.dimensiones = dimensiones;
        // Asignación del país de origen a la propiedad de la clase
        this.pais = pais;
        // Asignación del peso recibido a la propiedad de la clase
        this.peso = peso;

        // Inicialización del código ICAO como nulo por defecto
        this.icao24 = null;
        // Establecimiento del estado de selección inicial a falso
        this.seleccionado = false;
        // Asignación de un texto descriptivo genérico inicial
        this.descripcion = "Sin descripción detallada.";
        // Inicialización de la ruta de foto de usuario como nula
        this.uriFotoUsuario = null;
    }

    // Constructor que incluye el código ICAO además del resto de parámetros
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso, String icao24) {

        // Llamada al constructor previo para inicializar los atributos comunes
        this(apodo, fabricante, rareza, imagenResId,
                velocidad, pasajeros, dimensiones, pais, peso);

        // Asignación del código ICAO convirtiéndolo a mayúsculas si no es nulo
        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    // Método para obtener el identificador del avión
    public String getId() { return id; }
    // Método para asignar un identificador específico al avión
    public void setId(String id) { this.id = id; }

    // Método para obtener el apodo del avión
    public String getApodo() { return apodo; }
    // Método para establecer un nuevo apodo al avión
    public void setApodo(String apodo) {
        // Actualización de la variable de apodo
        this.apodo = apodo;
        // Sincronización del modelo con el apodo si aquel fuera nulo
        if (this.modelo == null) this.modelo = apodo;
    }

    // Método para obtener el modelo, priorizando la variable modelo sobre el apodo
    public String getModelo() { return (modelo != null && !modelo.isEmpty()) ? modelo : apodo; }

    // Método para establecer un nuevo modelo técnico
    public void setModelo(String modelo) {
        // Actualización de la variable de modelo
        this.modelo = modelo;
        // Sincronización del apodo con el modelo si aquel fuera nulo
        if (this.apodo == null) this.apodo = modelo;
    }

    // Método para obtener el fabricante del avión
    public String getFabricante() { return fabricante; }
    // Método para establecer el fabricante del avión
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    // Método para obtener la rareza del avión
    public String getRareza() { return rareza; }
    // Método para establecer la categoría de rareza
    public void setRareza(String rareza) { this.rareza = rareza; }

    // Método para obtener el identificador del recurso de imagen
    public int getImagenResId() { return imagenResId; }
    // Método para establecer un nuevo identificador de recurso de imagen
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    // Método para obtener la velocidad o un guion si es nula
    public String getVelocidad() { return velocidad != null ? velocidad : "-"; }
    // Método para establecer la velocidad
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    // Método para obtener la capacidad de pasajeros o un guion si es nula
    public String getPasajeros() { return pasajeros != null ? pasajeros : "-"; }
    // Método para establecer la capacidad de pasajeros
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    // Método para obtener las dimensiones o un guion si es nula
    public String getDimensiones() { return dimensiones != null ? dimensiones : "-"; }
    // Método para establecer las dimensiones físicas
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    // Método para obtener el país o un guion si es nulo
    public String getPais() { return pais != null ? pais : "-"; }
    // Método para establecer el país de origen
    public void setPais(String pais) { this.pais = pais; }

    // Método para obtener el peso o un guion si es nulo
    public String getPeso() { return peso != null ? peso : "-"; }
    // Método para establecer el peso
    public void setPeso(String peso) { this.peso = peso; }

    // Método para obtener la descripción del avión
    public String getDescripcion() { return descripcion != null ? descripcion : ""; }
    // Método para establecer la descripción detallada
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // Método para comprobar si el avión está marcado como seleccionado
    public boolean isSeleccionado() { return seleccionado; }
    // Método para cambiar el estado de selección del avión
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    // Método para obtener la ruta de la fotografía del usuario
    public String getUriFotoUsuario() { return uriFotoUsuario; }
    // Método para asignar una nueva ruta de fotografía de usuario
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    // Método para obtener el código ICAO del avión
    public String getIcao24() { return icao24; }
    // Método para establecer el código ICAO convirtiéndolo a mayúsculas
    public void setIcao24(String icao24) {
        // Asignación condicionada a la existencia de contenido en la cadena
        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    // Método de utilidad para verificar si el objeto posee un código ICAO válido
    public boolean tieneIcao() {
        // Retorno de verdadero si la variable no es nula ni está vacía
        return icao24 != null && !icao24.isEmpty();
    }

    // Método para obtener el valor del color correspondiente según la rareza definida
    public int getColorRareza() {
        // Retorno de un color gris genérico si la rareza no está definida
        if (rareza == null) return 0xFF808080;

        // Evaluación de la cadena de rareza para asignar el color hexadecimal adecuado
        switch (rareza.toUpperCase()) {
            // Caso para rareza común
            case "COMMON": case "COMUN": return 0xFF9E9E9E;
            // Caso para rareza rara
            case "RARE": case "RARO": return 0xFF2196F3;
            // Caso para rareza épica
            case "EPIC": case "EPICO": return 0xFF9C27B0;
            // Caso para rareza legendaria
            case "LEGENDARY": case "LEGENDARIO": return 0xFFFFD700;
            // Valor de retorno por defecto si no coincide con ninguna categoría
            default: return 0xFF808080;
        }
    }
}