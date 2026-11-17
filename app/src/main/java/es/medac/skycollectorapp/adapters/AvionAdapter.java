// Declaración del paquete al que pertenece el adaptador
package es.medac.skycollectorapp.adapters;

// Importaciones para gestión de gráficos y formato visual
import android.graphics.Color; // Utilidades de color
import android.net.Uri; // Para parsear URIs de fotos de usuario
import android.view.LayoutInflater; // Conversor de XML a objetos de vista Java
import android.view.View; // Componente base de vista
import android.view.ViewGroup; // Contenedor padre de vistas
import android.widget.CheckBox; // Casilla de selección
import android.widget.ImageView; // Componente para mostrar imágenes
import android.widget.TextView; // Componente para texto

// Importaciones de AndroidX
import androidx.annotation.NonNull; // Marcador de parámetros no nulos
import androidx.recyclerview.widget.RecyclerView; // Componente de lista optimizado

// Importaciones de librerías externas
import com.bumptech.glide.Glide; // Gestión y carga eficiente de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Estrategias de caché
import com.google.android.material.card.MaterialCardView; // Tarjeta con soporte Material Design

// Importaciones de Java estándar
import java.util.List; // Interfaz para manejo de listas

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos del proyecto
import es.medac.skycollectorapp.models.Avion; // Modelo de datos de Avión
import es.medac.skycollectorapp.models.GrupoAvion; // Modelo de agrupación por modelo

// Adaptador encargado de conectar la lista de grupos de aviones con la interfaz RecyclerView
public class AvionAdapter extends RecyclerView.Adapter<AvionAdapter.AvionViewHolder> {

    // ------------------------------
    // VARIABLES Y LISTENERS
    // ------------------------------
    private final List<GrupoAvion> listaGrupos; // Colección de grupos de aviones a mostrar
    private final OnItemClickListener listener; // Escuchador para eventos de clic en los elementos
    private final OnSelectionChangedListener selectionListener; // Escuchador para cambios en la selección

    // Interfaz para gestionar las pulsaciones sobre los elementos de la lista
    public interface OnItemClickListener {
        void onItemClick(GrupoAvion grupo, int position); // Método ejecutado al pulsar una tarjeta
    }

    // Interfaz para notificar cambios en el estado de las casillas de verificación
    public interface OnSelectionChangedListener {
        void onSelectionChanged(); // Método ejecutado al marcar o desmarcar cualquier casilla
    }

    // Constructor que inicializa los datos y los escuchadores del adaptador
    public AvionAdapter(List<GrupoAvion> listaGrupos, OnItemClickListener listener, OnSelectionChangedListener selectionListener) {
        // Asigna la lista de grupos recibida al atributo interno
        this.listaGrupos = listaGrupos;
        // Asigna el callback de pulsación sobre tarjeta
        this.listener = listener;
        // Asigna el callback de cambio de selección múltiple
        this.selectionListener = selectionListener;
    }

    // ------------------------------
    // MÉTODOS SOBREESCRITOS DE RECYCLERVIEW
    // ------------------------------

    @NonNull
    @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el diseño XML del elemento individual de la lista sin adjuntarlo aún al padre
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        // Devuelve una nueva instancia del ViewHolder con las vistas ya enlazadas
        return new AvionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        // Recupera el grupo de aviones correspondiente a la posición actual
        GrupoAvion grupo = listaGrupos.get(position);
        // Obtiene el avión representante del grupo (el más reciente o destacado)
        Avion avion = grupo.getRepresentante();

        // Muestra el apodo personalizado si hay un único avistamiento o el nombre del modelo si hay varios
        holder.txtModelo.setText(grupo.getCantidad() == 1 ? avion.getApodo() : avion.getModelo());

        // Si el grupo contiene más de un avistamiento, muestra la insignia multiplicadora (ej. "×3")
        if (grupo.getCantidad() > 1) {
            holder.txtCantidad.setText("×" + grupo.getCantidad());
            holder.txtCantidad.setVisibility(View.VISIBLE);
        } else {
            // Oculta la insignia de cantidad cuando solo hay un ejemplar
            holder.txtCantidad.setVisibility(View.GONE);
        }

        // Asigna el nombre del fabricante en la etiqueta inferior
        holder.txtFabricante.setText(avion.getFabricante());
        // Asigna el texto de la categoría de rareza en la etiqueta superior
        holder.txtRareza.setText(avion.getRareza());

        // Obtiene el color asociado a la rareza del avión
        int color = avion.getColorRareza();
        // Si no tiene un color definido (0), asigna gris por defecto
        if (color == 0) color = Color.GRAY;

        // Aplica el color de rareza al borde de la tarjeta y establece su grosor en 4px
        holder.cardView.setStrokeColor(color);
        holder.cardView.setStrokeWidth(4);
        // Aplica el mismo color de rareza como fondo de la etiqueta superior
        holder.txtRareza.setBackgroundColor(color);

        // ----------------------------------------------------
        // SELECCIÓN INTELIGENTE DE LA FUENTE DE IMAGEN (COLECCIÓN)
        // ----------------------------------------------------
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            // 1. Prioridad: Carga la foto propia tomada o elegida por el usuario omitiendo cachés antiguas
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);

        } else if (avion.getUrlImagenRemota() != null && !avion.getUrlImagenRemota().isEmpty()) {
            // 2. Segunda prioridad: Carga la fotografía real descargada de internet (Wikimedia/Wikipedia)
            Glide.with(holder.itemView.getContext())
                    .load(avion.getUrlImagenRemota())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);

        } else {
            // 3. Última opción: Selecciona el recurso vectorial del catálogo o la silueta genérica sil_jet
            Object imagenCarga = (avion.getImagenResId() != 0) ? avion.getImagenResId() : R.drawable.sil_jet;

            // Renderiza la silueta ajustada al centro del contenedor
            Glide.with(holder.itemView.getContext())
                    .load(imagenCarga)
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);
        }

        // Desvincula cualquier listener previo antes de asignar el estado para evitar disparos falsos al reciclar
        holder.chkSeleccion.setOnCheckedChangeListener(null);
        // Sincroniza el estado visual de la casilla con el estado booleano del grupo
        holder.chkSeleccion.setChecked(grupo.isSeleccionado());

        // Configura el evento al pulsar directamente sobre la casilla de verificación
        holder.chkSeleccion.setOnClickListener(v -> {
            // Lee el nuevo estado marcado o desmarcado
            boolean isChecked = holder.chkSeleccion.isChecked();
            // Actualiza el estado de selección dentro del objeto GrupoAvion
            grupo.setSeleccionado(isChecked);
            // Notifica a MainActivity para mostrar u ocultar el botón de papelera
            if (selectionListener != null) selectionListener.onSelectionChanged();
        });

        // Configura el evento al pulsar sobre el cuerpo de la tarjeta
        holder.itemView.setOnClickListener(v -> {
            // Delega la acción al listener principal pasando el grupo pulsado y su índice
            if (listener != null) listener.onItemClick(grupo, position);
        });
    }

    @Override
    public int getItemCount() {
        // Devuelve el número total de grupos en la lista o 0 si la lista es nula
        return listaGrupos != null ? listaGrupos.size() : 0;
    }

    // ------------------------------
    // VIEWHOLDER INTERNO
    // ------------------------------

    // Clase interna que mantiene las referencias visuales de los elementos del layout
    public static class AvionViewHolder extends RecyclerView.ViewHolder {

        // Referencias a las etiquetas de texto del modelo, fabricante, rareza y contador
        TextView txtModelo, txtFabricante, txtRareza, txtCantidad;
        // Referencia al componente ImageView de la fotografía o silueta del avión
        ImageView imgAvion;
        // Referencia a la tarjeta MaterialCardView que envuelve todo el ítem
        MaterialCardView cardView;
        // Referencia a la casilla CheckBox de selección múltiple
        CheckBox chkSeleccion;

        public AvionViewHolder(@NonNull View v) {
            // Llama al constructor de RecyclerView.ViewHolder
            super(v);
            // Enlaza cada componente Java con su correspondiente ID en item_avion.xml
            txtModelo = v.findViewById(R.id.txtModelo);
            txtFabricante = v.findViewById(R.id.txtFabricante);
            txtRareza = v.findViewById(R.id.txtRareza);
            txtCantidad = v.findViewById(R.id.txtCantidad);
            imgAvion = v.findViewById(R.id.imgAvion);
            cardView = v.findViewById(R.id.cardContainer);
            chkSeleccion = v.findViewById(R.id.chkSeleccion);
        }
    }
}