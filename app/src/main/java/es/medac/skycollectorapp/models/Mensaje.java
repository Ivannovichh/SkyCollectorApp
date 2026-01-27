package es.medac.skycollectorapp.models;

public class Mensaje {
    private String texto;
    private boolean esMio;

    public Mensaje(String texto, boolean esMio) {
        this.texto = texto;
        this.esMio = esMio;
    }

    public String getTexto() { return texto; }
    public boolean isEsMio() { return esMio; }
}
