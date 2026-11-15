// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación de la interfaz para permitir que el objeto se convierta en una secuencia de bytes
import java.io.Serializable;
// Importación de la utilidad para la generación de identificadores únicos universales
import java.util.UUID;

// Definición de la clase pública Avion que implementa la interfaz de serialización
public class Avion implements Serializable {

    private String id;
    private String modelo;
    private String apodo;
    private String fabricante;
    private String rareza;
    private int imagenResId;
    private String imagenUrl;
    private String velocidad;
    private String pasajeros;
    private String dimensiones;
    private String pais;
    private String peso;
    private String descripcion;
    private boolean seleccionado;
    private String uriFotoUsuario;
    private String urlImagenRemota;
    private String icao24;
    private String matricula;
    private long fechaAvistamiento;
    private String tipoIcao;
    private String origen;
    private float distanciaKm;
    private double latAvion;
    private double lonAvion;
    private boolean esFotoVerificada = false;

    public Avion() {
        this.id = UUID.randomUUID().toString();
        this.seleccionado = false;
    }

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

    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso, String icao24) {

        this(apodo, fabricante, rareza, imagenResId,
                velocidad, pasajeros, dimensiones, pais, peso);

        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApodo() { return apodo; }
    public void setApodo(String apodo) {
        this.apodo = apodo;
        if (this.modelo == null) this.modelo = apodo;
    }

    public String getModelo() { return (modelo != null && !modelo.isEmpty()) ? modelo : apodo; }
    public void setModelo(String modelo) {
        this.modelo = modelo;
        if (this.apodo == null) this.apodo = modelo;
    }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    public int getImagenResId() { return imagenResId; }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
        if (this.urlImagenRemota == null) {
            this.urlImagenRemota = imagenUrl;
        }
    }

    public String getVelocidad() { return velocidad != null ? velocidad : "-"; }
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    public String getPasajeros() { return pasajeros != null ? pasajeros : "-"; }
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    public String getDimensiones() { return dimensiones != null ? dimensiones : "-"; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public String getPais() { return pais != null ? pais : "-"; }
    public void setPais(String pais) { this.pais = pais; }

    public String getPeso() { return peso != null ? peso : "-"; }
    public void setPeso(String peso) { this.peso = peso; }

    public String getDescripcion() { return descripcion != null ? descripcion : ""; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    public String getUriFotoUsuario() { return uriFotoUsuario; }
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    public String getUrlImagenRemota() { return urlImagenRemota != null ? urlImagenRemota : imagenUrl; }
    public void setUrlImagenRemota(String urlImagenRemota) {
        this.urlImagenRemota = urlImagenRemota;
        if (this.imagenUrl == null) {
            this.imagenUrl = urlImagenRemota;
        }
    }

    public String getIcao24() { return icao24; }
    public void setIcao24(String icao24) {
        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;
    }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public long getFechaAvistamiento() { return fechaAvistamiento; }
    public void setFechaAvistamiento(long fechaAvistamiento) { this.fechaAvistamiento = fechaAvistamiento; }

    public String getTipoIcao() { return tipoIcao; }
    public void setTipoIcao(String tipoIcao) { this.tipoIcao = tipoIcao; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public float getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(float distanciaKm) { this.distanciaKm = distanciaKm; }

    public double getLatAvion() { return latAvion; }
    public void setLatAvion(double latAvion) { this.latAvion = latAvion; }

    public double getLonAvion() { return lonAvion; }
    public void setLonAvion(double lonAvion) { this.lonAvion = lonAvion; }

    public boolean tieneIcao() {
        return icao24 != null && !icao24.isEmpty();
    }

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

    public boolean isEsFotoVerificada() { return esFotoVerificada; }
    public void setEsFotoVerificada(boolean esFotoVerificada) { this.esFotoVerificada = esFotoVerificada; }
}