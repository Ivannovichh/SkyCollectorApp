package es.medac.skycollectorapp.models;

import java.io.Serializable;

public class Avion implements Serializable {

    // Usamos 'apodo' como nombre principal del avión (Ej: "Boeing 747")
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

    // --- NUEVO CAMPO PARA LA FOTO DE USUARIO ---
    private String uriFotoUsuario;

    // Constructor vacío (necesario para evitar errores de serialización)
    public Avion() {}

    // Constructor completo
    public Avion(String apodo, String fabricante, String rareza, int imagenResId, String velocidad, String pasajeros, String dimensiones, String pais, String peso) {
        this.apodo = apodo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;
        this.velocidad = velocidad;
        this.pasajeros = pasajeros;
        this.dimensiones = dimensiones;
        this.pais = pais;
        this.peso = peso;
        this.seleccionado = false;
        this.descripcion = "Sin descripción detallada.";
        this.uriFotoUsuario = null; // Inicializamos a null por defecto
    }

    // ==========================================
    //           GETTERS Y SETTERS
    // ==========================================

    // --- NOMBRE / APODO / MODELO ---
    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }

    // ¡¡¡ AQUÍ ESTÁ EL ARREGLO !!!
    // Creamos getModelo() como un "alias" de apodo.
    // Así si llamas a getModelo() te devuelve el nombre del avión.
    public String getModelo() { return apodo; }
    public void setModelo(String modelo) { this.apodo = modelo; }

    // --- FABRICANTE Y RAREZA ---
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    // --- FOTOS ---
    public int getImagenResId() { return imagenResId; }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    // Getter y Setter para la FOTO DE USUARIO
    public String getUriFotoUsuario() {
        return uriFotoUsuario;
    }
    public void setUriFotoUsuario(String uriFotoUsuario) {
        this.uriFotoUsuario = uriFotoUsuario;
    }

    // --- SELECCIÓN ---
    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    // --- DATOS TÉCNICOS (Con protección anti-cierre) ---
    // Si el valor es null, devolvemos un guión "-" para que no falle el texto

    public String getVelocidad() { return (velocidad != null) ? velocidad : "-"; }
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    public String getPasajeros() { return (pasajeros != null) ? pasajeros : "-"; }
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    public String getDimensiones() { return (dimensiones != null) ? dimensiones : "-"; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public String getPais() { return (pais != null) ? pais : "-"; }
    public void setPais(String pais) { this.pais = pais; }

    public String getPeso() { return (peso != null) ? peso : "-"; }
    public void setPeso(String peso) { this.peso = peso; }

    public String getDescripcion() { return (descripcion != null) ? descripcion : ""; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // Método extra para obtener color según rareza (útil para el adaptador)
    public int getColorRareza() {
        if (rareza == null) return 0xFF808080; // Gris
        switch (rareza.toUpperCase()) {
            case "LEGENDARIO": return 0xFFFFD700; // Dorado
            case "EPICO": return 0xFF9C27B0; // Morado
            case "RARO": return 0xFF2196F3; // Azul
            default: return 0xFF808080; // Gris
        }
    }
}