// Declaración del paquete al que pertenece este modelo de datos
package es.medac.skycollectorapp.models;

// Importación para el uso de listas de tamaño dinámico
import java.util.ArrayList;
// Importación de la interfaz base para colecciones de tipo lista
import java.util.List;

// Clase que agrupa todos los avistamientos de un mismo modelo de avión
public class GrupoAvion {

    // Nombre del modelo del catálogo que identifica al grupo
    private final String modelo;
    // Lista de avistamientos de ese modelo, del más antiguo al más reciente
    private final List<Avion> avistamientos = new ArrayList<>();
    // Estado de selección del grupo en la lista
    private boolean seleccionado = false;

    // Constructor que recibe el nombre del modelo del grupo
    public GrupoAvion(String modelo) {
        // Asignación del modelo recibido
        this.modelo = modelo;
    }

    // Método para obtener el nombre del modelo del grupo
    public String getModelo() { return modelo; }

    // Método para añadir un avistamiento al grupo
    public void agregar(Avion avion) { avistamientos.add(avion); }

    // Método para obtener todos los avistamientos del grupo
    public List<Avion> getAvistamientos() { return avistamientos; }

    // Método para obtener cuántos avistamientos tiene el grupo
    public int getCantidad() { return avistamientos.size(); }

    // Método para obtener el avión que representa al grupo en la lista (el avistamiento más reciente)
    public Avion getRepresentante() { return avistamientos.get(avistamientos.size() - 1); }

    // Método para comprobar si el grupo está marcado como seleccionado
    public boolean isSeleccionado() { return seleccionado; }
    // Método para cambiar el estado de selección del grupo
    public void setSeleccionado(boolean seleccionado) { this.seleccionado = seleccionado; }
}