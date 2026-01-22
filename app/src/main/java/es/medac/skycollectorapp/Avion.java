package es.medac.skycollectorapp;

import java.io.Serializable;
import java.util.Objects;

public class Avion implements Serializable {

    // --- VARIABLES DE DATOS ---
    private String modelo;      // Nombre original (ej: Boeing 737)
    private String apodo;       // Nombre editable (ej: Mi Avión Favorito)
    private String fabricante;
    private String rareza;

    // --- VARIABLES DE IMAGEN ---
    private int imagenResId;        // La foto oficial (el dibujo PNG)
    private String uriFotoUsuario;  // La foto de tu galería (ruta del archivo)

    // --- VARIABLES TÉCNICAS ---
    private String velocidadMax;
    private String capacidad;
    private String dimensiones;
    private String paisOrigen;
    private String pesoMax;

    // --- CONSTRUCTOR ---
    public Avion(String modelo, String fabricante, String rareza, int imagenResId,
                 String velocidadMax, String capacidad, String dimensiones, String paisOrigen, String pesoMax) {
        this.modelo = modelo;
        this.apodo = modelo; // Al principio, el apodo es igual al modelo
        this.fabricante = fabricante;
        this.rareza = rareza;
        this.imagenResId = imagenResId;
        this.velocidadMax = velocidadMax;
        this.capacidad = capacidad;
        this.dimensiones = dimensiones;
        this.paisOrigen = paisOrigen;
        this.pesoMax = pesoMax;
        this.uriFotoUsuario = null; // Al principio no hay foto tuya
    }

    // ==========================================
    // GETTERS Y SETTERS QUE NECESITAS
    // ==========================================

    // 1. PARA LA FOTO DE USUARIO (TU GALERÍA)
    public String getUriFotoUsuario() {
        return uriFotoUsuario;
    }

    public void setUriFotoUsuario(String uri) {
        this.uriFotoUsuario = uri;
    }

    // 2. PARA EL APODO (EL NOMBRE EDITABLE)
    public String getApodo() {
        return apodo;
    }

    public void setApodo(String apodo) {
        this.apodo = apodo;
    }

    // 3. PARA LA FOTO OFICIAL (EL DIBUJO)
    public int getImagenResId() {
        return imagenResId;
    }

    // 4. RESTO DE GETTERS (DATOS FIJOS)
    public String getModelo() { return modelo; }
    public String getFabricante() { return fabricante; }
    public String getRareza() { return rareza; }
    public String getVelocidadMax() { return velocidadMax; }
    public String getCapacidad() { return capacidad; }
    public String getDimensiones() { return dimensiones; }
    public String getPaisOrigen() { return paisOrigen; }
    public String getPesoMax() { return pesoMax; }

    // COLOR DE LA TARJETA SEGÚN RAREZA
    public int getColorRareza() {
        switch (rareza) {
            case "LEGENDARY": return 0xFFFFD600; // Dorado
            case "EPIC": return 0xFFD500F9;      // Morado
            case "RARE": return 0xFF2979FF;      // Azul
            default: return 0xFFB0BEC5;          // Gris
        }
    }

    // PARA EVITAR DUPLICADOS (IMPORTANTE)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avion avion = (Avion) o;
        return Objects.equals(modelo, avion.modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelo);
    }
}