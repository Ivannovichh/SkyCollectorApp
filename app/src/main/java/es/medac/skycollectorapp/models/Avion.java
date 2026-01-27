package es.medac.skycollectorapp.models;

import java.io.Serializable;
import java.util.Objects;

public class Avion implements Serializable {

    // VARIABLES
    private String modelo;
    private String fabricante;
    private String rareza;
    private String uriFotoUsuario;
    private boolean seleccionado = false;

    // DATOS TÉCNICOS
    private int imagenResId;
    private String velocidad;
    private String pasajeros;
    private String dimensiones;
    private String pais;
    private String peso;
    private String descripcion;

    // --- CONSTRUCTOR 1 (Manual) ---
    public Avion(String modelo, String fabricante, String rareza) {
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.velocidad = "Desconocida";
        this.pasajeros = "Desconocido";
        this.descripcion = "Sin descripción.";
        this.imagenResId = 0;
    }

    // --- CONSTRUCTOR 2 (Automático/Generador) ---
    public Avion(String modelo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones, String pais, String peso) {
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;
        this.velocidad = velocidad;
        this.pasajeros = pasajeros;
        this.dimensiones = dimensiones;
        this.pais = pais;
        this.peso = peso;
        this.descripcion = "Avión del catálogo oficial.";
    }

    // ==========================================
    //       GETTERS Y SETTERS (UNIVERSALES)
    // ==========================================

    // --- MODELO ---
    public String getModelo() { return modelo; }
    public String getApodo() { return modelo; } // Alias
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setApodo(String modelo) { this.modelo = modelo; }

    // --- FABRICANTE ---
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    // --- RAREZA ---
    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    // --- FOTO ---
    public String getUriFotoUsuario() { return uriFotoUsuario; }
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    public int getImagenResId() {
        if (imagenResId != 0) return imagenResId;
        return android.R.drawable.ic_menu_gallery;
    }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    // ==========================================
    //   AQUÍ ESTÁ LA SOLUCIÓN A TUS ERRORES
    //   (Métodos dobles para que funcione siempre)
    // ==========================================

    // --- VELOCIDAD ---
    public String getVelocidad() { return (velocidad == null) ? "N/A" : velocidad; }
    public String getVelocidadMax() { return getVelocidad(); } // <--- ALIAS PARA CORREGIR EL ERROR
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    // --- CAPACIDAD / PASAJEROS ---
    public String getPasajeros() { return (pasajeros == null) ? "N/A" : pasajeros; }
    public String getCapacidad() { return getPasajeros(); } // <--- ALIAS PARA CORREGIR EL ERROR
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    // --- PAÍS ---
    public String getPais() { return (pais == null) ? "N/A" : pais; }
    public String getPaisOrigen() { return getPais(); } // <--- ALIAS PARA CORREGIR EL ERROR
    public void setPais(String pais) { this.pais = pais; }

    // --- PESO ---
    public String getPeso() { return (peso == null) ? "N/A" : peso; }
    public String getPesoMax() { return getPeso(); } // <--- ALIAS PARA CORREGIR EL ERROR
    public void setPeso(String peso) { this.peso = peso; }

    // --- DIMENSIONES ---
    public String getDimensiones() { return (dimensiones == null) ? "N/A" : dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    // --- DESCRIPCIÓN ---
    public String getDescripcion() {
        return (descripcion == null || descripcion.isEmpty()) ? "Sin descripción." : descripcion;
    }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // ==========================================

    public int getColorRareza() {
        switch (rareza.toUpperCase()) {
            case "LEGENDARY": return 0xFFFFD700;
            case "EPIC": return 0xFF9C27B0;
            case "RARE": return 0xFF2196F3;
            default: return 0xFFB0BEC5;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avion avion = (Avion) o;
        return modelo != null && modelo.equalsIgnoreCase(avion.modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelo);
    }
}