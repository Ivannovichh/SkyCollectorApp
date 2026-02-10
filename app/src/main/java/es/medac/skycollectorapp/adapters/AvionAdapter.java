// Declaración del paquete al que pertenece la clase del adaptador
package es.medac.skycollectorapp.adapters;

// Importación para la gestión y manipulación de colores en Android
import android.graphics.Color;
// Importación para convertir archivos de diseño XML en objetos de vista
import android.view.LayoutInflater;
// Importación de la clase base para todos los componentes visuales
import android.view.View;
// Importación para el contenedor que agrupa otras vistas
import android.view.ViewGroup;
// Importación del componente de casilla de selección
import android.widget.CheckBox;
// Importación del componente para visualizar imágenes
import android.widget.ImageView;
// Importación del componente para visualizar cadenas de texto
import android.widget.TextView;

// Importación para marcar parámetros que no pueden recibir valores nulos
import androidx.annotation.NonNull;
// Importación de la clase base para la gestión de listas eficientes
import androidx.recyclerview.widget.RecyclerView;

// Importación de la librería Glide para la carga y gestión de imágenes
import com.bumptech.glide.Glide;
// Importación para configurar la estrategia de almacenamiento en caché de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// Importación del componente de tarjeta con soporte para estilos Material Design
import com.google.android.material.card.MaterialCardView;

// Importación para el uso de listas de tamaño dinámico
import java.util.ArrayList;
// Importación de la interfaz base para colecciones de tipo lista
import java.util.List;

// Importación del acceso a los recursos del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de datos de tipo Avión
import es.medac.skycollectorapp.models.Avion;

// Definición de la clase del adaptador que vincula datos con la interfaz de lista
public class AvionAdapter extends RecyclerView.Adapter<AvionAdapter.AvionViewHolder> {

    // Lista que contiene los objetos de tipo Avión a mostrar
    private  List<Avion> listaAviones;
    // Referencia al gestor de eventos de clic en los elementos
    private final OnItemClickListener listener;
    // Referencia al gestor de eventos para cambios en la selección
    private final OnSelectionChangedListener selectionListener;

    // Interfaz definida para capturar el evento de pulsación en un elemento
    public interface OnItemClickListener {
        // Firma del método para gestionar el clic sobre un avión concreto
        void onItemClick(Avion avion, int position);
    }

    // Interfaz definida para capturar cambios en el estado de las casillas de selección
    public interface OnSelectionChangedListener {
        // Firma del método para notificar que la selección general ha cambiado
        void onSelectionChanged();
    }

    // Constructor de la clase que inicializa la lista y los escuchadores de eventos
    public AvionAdapter(List<Avion> listaAviones, OnItemClickListener listener, OnSelectionChangedListener selectionListener) {
        // Asignación de la lista de datos recibida
        this.listaAviones = listaAviones;
        // Asignación del escuchador de clics
        this.listener = listener;
        // Asignación del escuchador de cambios de selección
        this.selectionListener = selectionListener;
    }

    // Método que se encarga de inflar el diseño XML para cada fila de la lista
    @NonNull
    @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Generación del objeto de vista a partir del recurso de diseño específico
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        // Retorno de una nueva instancia del contenedor de vistas
        return new AvionViewHolder(view);
    }

    // Método que asocia los datos de un objeto específico con los componentes de la vista
    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        // Recuperación del objeto Avión correspondiente a la posición actual
        Avion avion = listaAviones.get(position);

        // Asignación del texto del apodo al componente de modelo
        holder.txtModelo.setText(avion.getApodo());
        // Asignación del texto del fabricante al componente correspondiente
        holder.txtFabricante.setText(avion.getFabricante());
        // Asignación del texto que describe la rareza del objeto
        holder.txtRareza.setText(avion.getRareza());

        // Obtención del código de color asociado a la rareza del avión
        int color = avion.getColorRareza();
        // Validación para asignar un color gris por defecto si el valor es nulo
        if (color == 0) color = Color.GRAY;

        // Configuración del color del borde de la tarjeta según la rareza
        holder.cardView.setStrokeColor(color);
        // Definición del grosor del borde de la tarjeta
        holder.cardView.setStrokeWidth(4);

        // Configuración del color de fondo de la etiqueta de rareza
        holder.txtRareza.setBackgroundColor(color);

        // Declaración de una variable genérica para la fuente de la imagen
        Object imagenCarga;

        // Comprobación de si el objeto cuenta con un identificador de recurso oficial
        if (avion.getImagenResId() != 0) {
            // Asignación del recurso oficial para su carga
            imagenCarga = avion.getImagenResId();
        } else {
            // Asignación de un icono genérico del sistema como respaldo
            imagenCarga = android.R.drawable.ic_menu_gallery;
        }

        // Ejecución de la carga de imagen optimizada mediante Glide
        Glide.with(holder.itemView.getContext())
                .load(imagenCarga) // Carga de la fuente de datos
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Desactivación de caché en disco
                .skipMemoryCache(true) // Omisión de almacenamiento en memoria
                .fitCenter() // Ajuste centrado de la imagen al contenedor
                .placeholder(android.R.drawable.ic_menu_gallery) // Imagen de carga temporal
                .error(android.R.drawable.ic_delete) // Imagen a mostrar en caso de fallo
                .into(holder.imgAvion); // Destino final de la imagen

        // Desvinculación temporal del escuchador de cambios para evitar disparos accidentales
        holder.chkSeleccion.setOnCheckedChangeListener(null);
        // Establecimiento del estado visual de la casilla según el modelo de datos
        holder.chkSeleccion.setChecked(avion.isSeleccionado());

        // Configuración de la acción al interactuar con la casilla de selección
        holder.chkSeleccion.setOnClickListener(v -> {
            // Obtención del nuevo estado de la casilla
            boolean isChecked = holder.chkSeleccion.isChecked();
            // Actualización del estado de selección en el objeto de datos
            avion.setSeleccionado(isChecked);
            // Notificación al escuchador externo si está configurado
            if (selectionListener != null) selectionListener.onSelectionChanged();
        });

        // Configuración de la acción al pulsar sobre el elemento completo de la lista
        holder.itemView.setOnClickListener(v -> {
            // Invocación del evento de clic pasando el objeto y su posición
            if (listener != null) listener.onItemClick(avion, position);
        });
    }

    // Método que devuelve la cantidad total de elementos en la lista
    @Override
    public int getItemCount() {
        // Validación para devolver el tamaño de la lista o cero si es nula
        return listaAviones != null ? listaAviones.size() : 0;
    }

    // Método para realizar una limpieza masiva de elementos seleccionados
    public void borrarSeleccionados() {
        // Interrupción si la lista no ha sido inicializada
        if (listaAviones == null) return;

        // Creación de una lista temporal para almacenar los elementos que no serán borrados
        List<Avion> aConservar = new ArrayList<>();
        // Recorrido de la lista actual para filtrar por estado de selección
        for (Avion a : listaAviones) {
            // Inclusión en la lista de permanencia si el avión no está marcado
            if (!a.isSeleccionado()) {
                // Adición del objeto a la lista de conservación
                aConservar.add(a);
            }
        }

        // Vaciado total de la lista original
        listaAviones.clear();
        // Inserción de los elementos que se han decidido conservar
        listaAviones.addAll(aConservar);
        // Notificación de cambio general para refrescar toda la vista
        notifyDataSetChanged();
    }

    // Clase interna que actúa como contenedor de las referencias visuales de cada fila
    public static class AvionViewHolder extends RecyclerView.ViewHolder {

        // Declaración de variables para los componentes de texto
        TextView txtModelo, txtFabricante, txtRareza;
        // Declaración de variable para el componente de imagen
        ImageView imgAvion;
        // Declaración de variable para el contenedor de tarjeta
        MaterialCardView cardView;
        // Declaración de variable para la casilla de selección
        CheckBox chkSeleccion;

        // Constructor del contenedor que asocia las variables con el XML
        public AvionViewHolder(@NonNull View v) {
            // Llamada al constructor de la clase base
            super(v);
            // Vinculación del texto del modelo
            txtModelo = v.findViewById(R.id.txtModelo);
            // Vinculación del texto del fabricante
            txtFabricante = v.findViewById(R.id.txtFabricante);
            // Vinculación del texto de rareza
            txtRareza = v.findViewById(R.id.txtRareza);
            // Vinculación de la imagen del avión
            imgAvion = v.findViewById(R.id.imgAvion);
            // Vinculación de la tarjeta contenedora
            cardView = v.findViewById(R.id.cardContainer);
            // Vinculación de la casilla de marcado
            chkSeleccion = v.findViewById(R.id.chkSeleccion);
        }
    }

    // Método para eliminar secuencialmente los elementos marcados con efecto visual
    public  void borrarSeleccionadosConAnimacion() {
        // Interrupción si la lista está vacía o es nula
        if (listaAviones == null) return;

        // Recorrido inverso de la lista para gestionar los índices correctamente durante el borrado
        for (int i = listaAviones.size() - 1; i >= 0; i--) {
            // Comprobación de si el elemento en la posición actual está marcado
            if (listaAviones.get(i).isSeleccionado()) {
                // Eliminación física del objeto de la lista en memoria
                listaAviones.remove(i);
                // Notificación específica al sistema para ejecutar la animación de salida
                notifyItemRemoved(i);
            }
        }
    }
}