package es.medac.skycollectorapp.models;

import java.io.Serializable;
import java.util.UUID;

public class Avion implements Serializable {

    private String id;
    private String apodo;
    private String fabricante;
    private String rareza;
    private int imagenResId;

    // Datos técnicos
    private String velocidad;
    private String pasajeros;
    private String dimensiones;
    private String pais;
    private String peso;
    private String descripcion;

    private boolean seleccionado;

    // Foto del usuario
    private String uriFotoUsuario;

    // GOD MODE: ICAO24
    private String icao24;

    // -------------------------------
    // CONSTRUCTORES
    // -------------------------------

    // Constructor vacío
    public Avion() {
        this.id = UUID.randomUUID().toString();
    }

    // Constructor antiguo (compatibilidad)
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso) {

        this.id = UUID.randomUUID().toString();
        this.apodo = apodo;
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
        this.descripcion = "Sin descripción detallada.";
        this.uriFotoUsuario = null;
    }

    // Constructor GOD MODE completo
    public Avion(String apodo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones,
                 String pais, String peso, String icao24) {

        this.id = UUID.randomUUID().toString();
        this.apodo = apodo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;

        this.velocidad = velocidad;
        this.pasajeros = pasajeros;
        this.dimensiones = dimensiones;
        this.pais = pais;
        this.peso = peso;

        this.icao24 = icao24 != null ? icao24.toUpperCase() : null;

        this.seleccionado = false;
        this.descripcion = "Sin descripción detallada.";
        this.uriFotoUsuario = null;
    }

    // -------------------------------
    // GETTERS Y SETTERS
    // -------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }

    public String getModelo() { return apodo; }
    public void setModelo(String modelo) { this.apodo = modelo; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    public int getImagenResId() { return imagenResId; }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

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

    public String getIcao24() { return icao24; }
    public void setIcao24(String icao24) { this.icao24 = icao24 != null ? icao24.toUpperCase() : null; }

    public int getColorRareza() {
        if (rareza == null) return 0xFF808080;

        switch (rareza.toUpperCase()) {
            case "COMMON": case "COMUN": return 0xFF9E9E9E; // Gris
            case "RARE": case "RARO": return 0xFF2196F3;     // Azul
            case "EPIC": case "EPICO": return 0xFF9C27B0;    // Morado
            case "LEGENDARY": case "LEGENDARIO": return 0xFFFFD700; // Dorado
            default: return 0xFF808080;
        }
    }
}
