package es.medac.skycollectorapp;

import java.io.Serializable;
import java.util.Objects;
import com.google.firebase.firestore.Exclude;

public class Avion implements Serializable {

    // --- VARIABLES DE DATOS ---
    private String modelo;      // Nombre original (ej: Boeing 737)
    private String apodo;       // Nombre editable (ej: Mi Avión Favorito)
    private String fabricante;
    private String rareza;

    // --- VARIABLES DE IMAGEN ---
    // ¡OJO! He quitado el @Exclude aquí para que SE GUARDE la foto oficial en Firebase
    private int imagenResId;
    private String uriFotoUsuario;  // La foto de tu galería

    // --- VARIABLES TÉCNICAS ---
    private String velocidadMax;
    private String capacidad;
    private String dimensiones;
    private String paisOrigen;
    private String pesoMax;

    // Este sí lleva Exclude porque es el ID del documento, no un dato del avión
    @Exclude
    private String documentId;

    // ==========================================
    // 1. CONSTRUCTOR VACÍO (OBLIGATORIO)
    // ==========================================
    public Avion() {
    }

    // ==========================================
    // 2. CONSTRUCTOR COMPLETO
    // ==========================================
    public Avion(String modelo, String fabricante, String rareza, int imagenResId,
                 String velocidadMax, String capacidad, String dimensiones, String paisOrigen, String pesoMax) {
        this.modelo = modelo;
        this.apodo = modelo;
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;
        this.velocidadMax = velocidadMax;
        this.capacidad = capacidad;
        this.dimensiones = dimensiones;
        this.paisOrigen = paisOrigen;
        this.pesoMax = pesoMax;
        this.uriFotoUsuario = null;
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public String getUriFotoUsuario() { return uriFotoUsuario; }
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    public int getImagenResId() { return imagenResId; }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    public String getVelocidadMax() { return velocidadMax; }
    public void setVelocidadMax(String velocidadMax) { this.velocidadMax = velocidadMax; }

    public String getCapacidad() { return capacidad; }
    public void setCapacidad(String capacidad) { this.capacidad = capacidad; }

    public String getDimensiones() { return dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public String getPaisOrigen() { return paisOrigen; }
    public void setPaisOrigen(String paisOrigen) { this.paisOrigen = paisOrigen; }

    public String getPesoMax() { return pesoMax; }
    public void setPesoMax(String pesoMax) { this.pesoMax = pesoMax; }

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // ==========================================
    // MÉTODOS DE AYUDA
    // ==========================================
    public int getColorRareza() {
        if (rareza == null) return 0xFFB0BEC5;
        switch (rareza) {
            case "LEGENDARY": return 0xFFFFD600;
            case "EPIC": return 0xFFD500F9;
            case "RARE": return 0xFF2979FF;
            case "COMMON": return 0xFF00C853;
            default: return 0xFFB0BEC5;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avion avion = (Avion) o;
        // Dos aviones son iguales si tienen el mismo MODELO (ignorando mayúsculas)
        return modelo != null && modelo.equalsIgnoreCase(avion.modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelo);
    }
    private boolean seleccionado = false;

    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }
}