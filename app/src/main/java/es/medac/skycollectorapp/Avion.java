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
    @Exclude // Para que Firestore no intente guardar este campo dentro del documento
    private int imagenResId;        // La foto oficial (el dibujo PNG en la app)
    private String uriFotoUsuario;  // La foto de tu galería (ruta del archivo)

    // --- VARIABLES TÉCNICAS ---
    private String velocidadMax;
    private String capacidad;
    private String dimensiones;
    private String paisOrigen;
    private String pesoMax;
    @Exclude // Para que Firestore no intente guardar este campo dentro del documento
    private String documentId;

    // ==========================================
    // 1. CONSTRUCTOR VACÍO (OBLIGATORIO PARA FIREBASE)
    // ==========================================
    public Avion() {
        // Firebase necesita esto vacío para rellenarlo después con los setters
        modelo = "";
        apodo = null;
        fabricante = null;
        rareza = null;
        imagenResId = 0;
        velocidadMax = null;
        capacidad = null;
        dimensiones = null;
        paisOrigen = null;
        pesoMax = null;
        uriFotoUsuario = null;
    }

    // ==========================================
    // 2. CONSTRUCTOR COMPLETO (Para crear aviones nuevos)
    // ==========================================
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
    // GETTERS Y SETTERS
    // ==========================================

    // --- FOTOS ---
    public String getUriFotoUsuario() { return uriFotoUsuario; }
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }
    @Exclude
    public int getImagenResId() { return imagenResId; }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    // --- DATOS BÁSICOS ---
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    // --- DATOS TÉCNICOS ---
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
    public String getDocumentId() {
        return documentId;
    }
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // ==========================================
    // MÉTODOS DE AYUDA
    // ==========================================

    // COLOR DE LA TARJETA SEGÚN RAREZA
    public int getColorRareza() {
        if (rareza == null) return 0xFFB0BEC5; // Por si acaso es null
        switch (rareza) {
            case "LEGENDARY": return 0xFFFFD600; // Dorado
            case "EPIC": return 0xFFD500F9;      // Morado
            case "RARE": return 0xFF2979FF;      // Azul
            case "COMMON": return 0xFF00C853;    // Verde (Añado common por si acaso)
            default: return 0xFFB0BEC5;          // Gris
        }
    }

    // PARA COMPARAR AVIONES
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avion avion = (Avion) o;
        // Comparamos por ID si existe, si no por modelo
        if (documentId != null && avion.documentId != null) return Objects.equals(documentId, avion.documentId);
        return Objects.equals(modelo, avion.modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, modelo);
    }
}