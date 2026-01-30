package es.medac.skycollectorapp.models;

public class Mensaje {
    private String contenido;
    private boolean esMio;

    public Mensaje(String contenido, boolean esMio) {
        this.contenido = contenido;
        this.esMio = esMio;
    }

    public String getContenido() { return contenido; }
    public boolean isEsMio() { return esMio; }
}