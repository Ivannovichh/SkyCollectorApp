// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación de la interfaz para permitir que el objeto se convierta en una secuencia de bytes
import java.io.Serializable;
// Importación de la utilidad para la generación de identificadores únicos universales
import java.util.UUID;

// Definición de la clase pública Avion que implementa la interfaz de serialización
public class Avion implements Serializable {

    // Identificador único universal (UUID) del avistamiento registrado
    private String id;
    // Nombre comercial oficial del modelo de aeronave (ej. "Boeing 737-800")
    private String modelo;
    // Nombre corto o personalizado asignado al avión en la colección
    private String apodo;
    // Compañía fabricante o constructora de la aeronave (ej. "Airbus", "Boeing")
    private String fabricante;
    // Nivel de rareza del modelo ("COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")
    private String rareza;
    // Identificador del recurso gráfico local (silueta vectorial) en R.drawable
    private int imagenResId;
    // URL de respaldo de la imagen del avión en internet
    private String imagenUrl;
    // Velocidad máxima o de crucero expresada como texto formateado
    private String velocidad;
    // Capacidad estándar de pasajeros expresada como texto
    private String pasajeros;
    // Medidas de envergadura, longitud y altura de la aeronave
    private String dimensiones;
    // País de origen del fabricante o registro
    private String pais;
    // Peso máximo al despegue (MTOW) o peso vacío formateado
    private String peso;
    // Reseña o notas descriptivas adicionales del ejemplar
    private String descripcion;
    // Bandera temporal de interfaz para selección múltiple en la papelera de borrado
    private boolean seleccionado;
    // URI local o cadena Base64 de la fotografía capturada por el usuario
    private String uriFotoUsuario;
    // URL de fotografía real descargada desde fuentes externas (Wikipedia/Wikimedia)
    private String urlImagenRemota;
    // Código hexadecimal único de 24 bits del transpondedor Mode-S / ADS-B
    private String icao24;
    // Matrícula aeronáutica oficial de la aeronave (ej. "EC-MUA")
    private String matricula;
    // Marca temporal en milisegundos del momento en que se registró el avistamiento
    private long fechaAvistamiento;
    // Designador oficial de tipo de aeronave de la OACI (ej. "B738", "A320")
    private String tipoIcao;
    // Aeropuerto o procedencia registrada del vuelo
    private String origen;
    // Distancia en kilómetros entre el piloto y el avión en el instante de captura
    private float distanciaKm;
    // Latitud geográfica exacta donde se avistó la aeronave
    private double latAvion;
    // Longitud geográfica exacta donde se avistó la aeronave
    private double lonAvion;
    // Indica si la fotografía aportada pasó la validación de verificación
    private boolean esFotoVerificada = false;

    // Constructor vacío requerido por Firebase Firestore y Gson para deserialización automática
    public Avion() {
        this.id = UUID.randomUUID().toString();
        this.seleccionado = false;
    }

    // Constructor principal para instanciar aeronaves del catálogo con sus especificaciones técnicas base
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso) {

        this.id = UUID.randomUUID().toString();
        this.apodo = apodo;
        this.modelo = apodo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;
        this.velocidad = velocidad;
        this.pasajeros = pasajeros;
        this.dimensiones = dimensiones;
        this.pais = pais;
        this.peso = peso;

        this.icao24 = null;
        this.seleccionado = false;
        this.descripcion = ""; // Sin texto hardcodeado; la UI usa R.string si está vacío
        this.uriFotoUsuario = null;
        this.imagenUrl = null;
        this.urlImagenRemota = null;
    }

    // Constructor sobrecargado que además vincula un código transpondedor ICAO24 normalizado en mayúsculas
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso, String icao24) {

        this(apodo, fabricante, rareza, imagenResId,
                velocidad, pasajeros, dimensiones, pais, peso);

        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    // Devuelve el identificador único del registro
    public String getId() { return id; }
    // Establece el identificador único del registro
    public void setId(String id) { this.id = id; }

    // Devuelve el apodo o nombre corto del avión
    public String getApodo() { return apodo; }
    // Establece el apodo y lo sincroniza con modelo si este aún no estaba definido
    public void setApodo(String apodo) {
        this.apodo = apodo;
        if (this.modelo == null) this.modelo = apodo;
    }

    // Devuelve el modelo comercial o recurre al apodo si el modelo está vacío
    public String getModelo() { return (modelo != null && !modelo.isEmpty()) ? modelo : apodo; }
    // Establece el modelo comercial y lo sincroniza con apodo si este era nulo
    public void setModelo(String modelo) {
        this.modelo = modelo;
        if (this.apodo == null) this.apodo = modelo;
    }

    // Devuelve el nombre del fabricante
    public String getFabricante() { return fabricante; }
    // Establece el nombre del fabricante
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    // Devuelve la categoría de rareza asignada
    public String getRareza() { return rareza; }
    // Establece la categoría de rareza
    public void setRareza(String rareza) { this.rareza = rareza; }

    // Devuelve el ID del recurso drawable de la silueta local
    public int getImagenResId() { return imagenResId; }
    // Establece el ID del recurso drawable de la silueta local
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    // Devuelve la URL secundaria de imagen
    public String getImagenUrl() { return imagenUrl; }
    // Establece la URL de imagen y la replica en urlImagenRemota si estaba vacía
    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
        if (this.urlImagenRemota == null) {
            this.urlImagenRemota = imagenUrl;
        }
    }

    // Devuelve la velocidad formateada o un guion si no está disponible
    public String getVelocidad() { return velocidad != null ? velocidad : "-"; }
    // Establece la velocidad formateada
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    // Devuelve la capacidad de pasajeros o un guion si no está disponible
    public String getPasajeros() { return pasajeros != null ? pasajeros : "-"; }
    // Establece la capacidad de pasajeros
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    // Devuelve las dimensiones técnicas o un guion si no están disponibles
    public String getDimensiones() { return dimensiones != null ? dimensiones : "-"; }
    // Establece las dimensiones técnicas
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    // Devuelve el país de origen o un guion si no está disponible
    public String getPais() { return pais != null ? pais : "-"; }
    // Establece el país de origen
    public void setPais(String pais) { this.pais = pais; }

    // Devuelve el peso de la aeronave o un guion si no está disponible
    public String getPeso() { return peso != null ? peso : "-"; }
    // Establece el peso de la aeronave
    public void setPeso(String peso) { this.peso = peso; }

    // Devuelve la descripción del avión o cadena vacía si es nula
    public String getDescripcion() { return descripcion != null ? descripcion : ""; }
    // Establece la descripción del avión
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // Indica si la tarjeta está marcada actualmente para borrado en la interfaz
    public boolean isSeleccionado() { return seleccionado; }
    // Cambia el estado de selección múltiple de la tarjeta
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    // Devuelve la URI o Base64 de la foto propia subida por el usuario
    public String getUriFotoUsuario() { return uriFotoUsuario; }
    // Establece la URI o Base64 de la foto propia del usuario
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    // Devuelve la URL remota principal de la fotografía de la aeronave
    public String getUrlImagenRemota() { return urlImagenRemota != null ? urlImagenRemota : imagenUrl; }
    // Establece la URL remota principal y sincroniza imagenUrl si estaba vacía
    public void setUrlImagenRemota(String urlImagenRemota) {
        this.urlImagenRemota = urlImagenRemota;
        if (this.imagenUrl == null) {
            this.imagenUrl = urlImagenRemota;
        }
    }

    // Devuelve el código hexadecimal ICAO24 del transpondedor
    public String getIcao24() { return icao24; }
    // Establece el código ICAO24 normalizándolo siempre en mayúsculas
    public void setIcao24(String icao24) {
        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    // Devuelve la matrícula del avión
    public String getMatricula() { return matricula; }
    // Establece la matrícula del avión
    public void setMatricula(String matricula) { this.matricula = matricula; }

    // Devuelve la fecha de captura en milisegundos (Unix timestamp)
    public long getFechaAvistamiento() { return fechaAvistamiento; }
    // Establece la fecha de captura en milisegundos
    public void setFechaAvistamiento(long fechaAvistamiento) { this.fechaAvistamiento = fechaAvistamiento; }

    // Devuelve el código de tipo de aeronave OACI (ej. "A359")
    public String getTipoIcao() { return tipoIcao; }
    // Establece el código de tipo de aeronave OACI
    public void setTipoIcao(String tipoIcao) { this.tipoIcao = tipoIcao; }

    // Devuelve el origen asociado al avistamiento
    public String getOrigen() { return origen; }
    // Establece el origen asociado al avistamiento
    public void setOrigen(String origen) { this.origen = origen; }

    // Devuelve la distancia en kilómetros a la que se capturó el avión
    public float getDistanciaKm() { return distanciaKm; }
    // Establece la distancia en kilómetros a la que se capturó el avión
    public void setDistanciaKm(float distanciaKm) { this.distanciaKm = distanciaKm; }

    // Devuelve la latitud de captura del avión
    public double getLatAvion() { return latAvion; }
    // Establece la latitud de captura del avión
    public void setLatAvion(double latAvion) { this.latAvion = latAvion; }

    // Devuelve la longitud de captura del avión
    public double getLonAvion() { return lonAvion; }
    // Establece la longitud de captura del avión
    public void setLonAvion(double lonAvion) { this.lonAvion = lonAvion; }

    // Comprueba si el ejemplar dispone de un identificador ICAO24 válido registrado
    public boolean tieneIcao() {
        return icao24 != null && !icao24.isEmpty();
    }

    // Devuelve el código de color ARGB correspondiente a la rareza (tanto en inglés como en español)
    public int getColorRareza() {
        if (rareza == null) return 0xFF808080;

        switch (rareza.toUpperCase()) {
            case "COMMON": case "COMUN": return 0xFF9E9E9E;
            case "RARE": case "RARO": return 0xFF2196F3;
            case "EPIC": case "EPICO": return 0xFF9C27B0;
            case "LEGENDARY": case "LEGENDARIO": return 0xFFFFD700;
            case "MYTHIC": case "MITICO": return 0xFFFF1744;
            default: return 0xFF808080;
        }
    }

    // Indica si la foto asociada al avistamiento está verificada
    public boolean isEsFotoVerificada() { return esFotoVerificada; }
    // Establece el estado de verificación de la fotografía
    public void setEsFotoVerificada(boolean esFotoVerificada) { this.esFotoVerificada = esFotoVerificada; }
}