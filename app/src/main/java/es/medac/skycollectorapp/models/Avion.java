package es.medac.skycollectorapp.models;

import java.io.Serializable;
import java.util.Objects;

public class Avion implements Serializable {

    // VARIABLES PRINCIPALES
    private String modelo;
    private String fabricante; // Ej: Boeing, Airbus
    private String matricula;  // Ej: EC-MKL (NUEVO)
    private String rareza;     // Comun, Raro, Epico...

    // FOTO DEL USUARIO (Cámara o Galería)
    private String uriFotoUsuario;

    private boolean seleccionado = false;

    // DATOS TÉCNICOS (Para detalles)
    private int imagenResId; // Para fotos predefinidas (drawable)
    private String velocidad;
    private String pasajeros;
    private String dimensiones;
    private String pais;
    private String peso;
    private String descripcion;

    // --- CONSTRUCTOR 1 (El que usa AddAvionActivity) ---
    public Avion(String modelo, String matricula, String rareza) {
        this.modelo = modelo;
        this.matricula = matricula; // Guardamos la matrícula
        this.rareza = rareza;

        // Valores por defecto para lo que no rellenamos en el formulario simple
        this.fabricante = "Desconocido";
        this.velocidad = "Desconocida";
        this.pasajeros = "Desconocido";
        this.descripcion = "Avión añadido por el usuario.";
        this.imagenResId = 0;
    }

    // --- CONSTRUCTOR 2 (Automático/Generador para lista inicial) ---
    public Avion(String modelo, String fabricante, String rareza, int imagenResId,
                 String velocidad, String pasajeros, String dimensiones, String pais, String peso) {
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.matricula = "N/A"; // Los genéricos no suelen tener matrícula fija
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
    //       GETTERS Y SETTERS
    // ==========================================

    // --- MODELO ---
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    // Alias por si usas "Apodo" en algún sitio
    public String getApodo() { return modelo; }
    public void setApodo(String modelo) { this.modelo = modelo; }

    // --- MATRÍCULA (NUEVO) ---
    public String getMatricula() { return (matricula == null) ? "Sin Matrícula" : matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    // --- FABRICANTE ---
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    // --- RAREZA ---
    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }

    // --- FOTO (LO IMPORTANTE PARA TU CÁMARA) ---
    // Getter principal
    public String getUriFotoUsuario() { return uriFotoUsuario; }
    public void setUriFotoUsuario(String uriFotoUsuario) { this.uriFotoUsuario = uriFotoUsuario; }

    // ALIAS: Estos métodos hacen que funcione 'nuevoAvion.setFotoUri(...)'
    // sin cambiar el resto de tu código que use 'uriFotoUsuario'.
    public void setFotoUri(String uri) { this.uriFotoUsuario = uri; }
    public String getFotoUri() { return this.uriFotoUsuario; }

    // --- IMAGEN DRAWABLE (Iconos por defecto) ---
    public int getImagenResId() {
        if (imagenResId != 0) return imagenResId;
        // Si no hay imagen predefinida, devolvemos 0 o un icono genérico
        return 0;
    }
    public void setImagenResId(int imagenResId) { this.imagenResId = imagenResId; }

    // --- SELECCIÓN ---
    public boolean isSeleccionado() { return seleccionado; }
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }

    // ==========================================
    //   ALIAS DE SEGURIDAD (Para evitar errores viejos)
    // ==========================================

    public String getVelocidad() { return (velocidad == null) ? "N/A" : velocidad; }
    public String getVelocidadMax() { return getVelocidad(); }
    public void setVelocidad(String velocidad) { this.velocidad = velocidad; }

    public String getPasajeros() { return (pasajeros == null) ? "N/A" : pasajeros; }
    public String getCapacidad() { return getPasajeros(); }
    public void setPasajeros(String pasajeros) { this.pasajeros = pasajeros; }

    public String getPais() { return (pais == null) ? "N/A" : pais; }
    public String getPaisOrigen() { return getPais(); }
    public void setPais(String pais) { this.pais = pais; }

    public String getPeso() { return (peso == null) ? "N/A" : peso; }
    public String getPesoMax() { return getPeso(); }
    public void setPeso(String peso) { this.peso = peso; }

    public String getDimensiones() { return (dimensiones == null) ? "N/A" : dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public String getDescripcion() {
        return (descripcion == null || descripcion.isEmpty()) ? "Sin descripción." : descripcion;
    }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    // ==========================================
    //   MÉTODOS ÚTILES
    // ==========================================

    public int getColorRareza() {
        if (rareza == null) return 0xFFB0BEC5; // Gris por defecto

        switch (rareza.toUpperCase()) {
            case "LEGENDARIO":
            case "LEGENDARY": return 0xFFFFD700; // Dorado
            case "EPICO":
            case "EPIC": return 0xFF9C27B0; // Morado
            case "RARO":
            case "RARE": return 0xFF2196F3; // Azul
            default: return 0xFFB0BEC5; // Gris (Común)
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Avion avion = (Avion) o;
        // Dos aviones son iguales si tienen la misma matrícula (si existe) o el mismo modelo
        if (matricula != null && avion.matricula != null && !matricula.equals("N/A")) {
            return matricula.equalsIgnoreCase(avion.matricula);
        }
        return modelo != null && modelo.equalsIgnoreCase(avion.modelo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelo, matricula);
    }
}