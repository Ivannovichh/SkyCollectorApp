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
        void onItemClick(GrupoAvion grupo, int position);
    }

    // Interfaz para notificar cambios en el estado de las casillas de verificación
    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    // Constructor que inicializa los datos y los escuchadores del adaptador
    public AvionAdapter(List<GrupoAvion> listaGrupos, OnItemClickListener listener, OnSelectionChangedListener selectionListener) {
        this.listaGrupos = listaGrupos;
        this.listener = listener;
        this.selectionListener = selectionListener;
    }

    // ------------------------------
    // MÉTODOS SOBREESCRITOS DE RECYCLERVIEW
    // ------------------------------

    @NonNull
    @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar el diseño XML del elemento individual de la lista
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        return new AvionViewHolder(view); // Devolver nueva instancia delViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        GrupoAvion grupo = listaGrupos.get(position); // Recuperar el grupo en la posición actual
        Avion avion = grupo.getRepresentante(); // Obtener el avión representante del grupo

        // Mostrar apodo si hay un único avistamiento o el nombre del modelo si hay varios
        holder.txtModelo.setText(grupo.getCantidad() == 1 ? avion.getApodo() : avion.getModelo());

        // Configurar la etiqueta con el contador de avistamientos acumulados
        if (grupo.getCantidad() > 1) {
            holder.txtCantidad.setText("×" + grupo.getCantidad());
            holder.txtCantidad.setVisibility(View.VISIBLE);
        } else {
            holder.txtCantidad.setVisibility(View.GONE);
        }

        holder.txtFabricante.setText(avion.getFabricante()); // Asignar fabricante
        holder.txtRareza.setText(avion.getRareza()); // Asignar categoría de rareza

        // Aplicar el color distintivo según la rareza del avión
        int color = avion.getColorRareza();
        if (color == 0) color = Color.GRAY; // Asignar gris por defecto si es cero

        holder.cardView.setStrokeColor(color); // Ajustar color del borde de la tarjeta
        holder.cardView.setStrokeWidth(4); // Ajustar grosor del borde
        holder.txtRareza.setBackgroundColor(color); // Ajustar fondo de la etiqueta de rareza

        // ----------------------------------------------------
        // SELECCIÓN INTELIGENTE DE LA FUENTE DE IMAGEN (COLECCIÓN)
        // ----------------------------------------------------
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            // 1. Prioridad: Foto propia tomada o cargada por el usuario
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);

        } else if (avion.getUrlImagenRemota() != null && !avion.getUrlImagenRemota().isEmpty()) {
            // 2. Segunda prioridad: Foto real descargada de internet (Wikipedia) ya guardada previamente
            Glide.with(holder.itemView.getContext())
                    .load(avion.getUrlImagenRemota())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);

        } else {
            // 3. Última opción: Recurrir al recurso local del catálogo o silueta por defecto
            Object imagenCarga = (avion.getImagenResId() != 0) ? avion.getImagenResId() : R.drawable.sil_jet;

            Glide.with(holder.itemView.getContext())
                    .load(imagenCarga)
                    .fitCenter()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(holder.imgAvion);
        }

        // Evitar activaciones accidentales del listener durante la configuración
        holder.chkSeleccion.setOnCheckedChangeListener(null);
        holder.chkSeleccion.setChecked(grupo.isSeleccionado()); // Marcar o desmarcar casilla

        // Listener de interacción con la casilla de verificación
        holder.chkSeleccion.setOnClickListener(v -> {
            boolean isChecked = holder.chkSeleccion.isChecked();
            grupo.setSeleccionado(isChecked); // Actualizar estado en el modelo
            if (selectionListener != null) selectionListener.onSelectionChanged(); // Notificar cambio
        });

        // Listener de interacción al hacer clic en cualquier parte de la tarjeta
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(grupo, position); // Notificar clic
        });
    }

    @Override
    public int getItemCount() {
        return listaGrupos != null ? listaGrupos.size() : 0; // Devolver tamaño de la lista
    }

    // ------------------------------
    // VIEWHOLDER INTERNO
    // ------------------------------

    // Clase interna que mantiene las referencias visuales de los elementos del layout
    public static class AvionViewHolder extends RecyclerView.ViewHolder {

        TextView txtModelo, txtFabricante, txtRareza, txtCantidad; // Elementos de texto
        ImageView imgAvion; // Imagen del avión
        MaterialCardView cardView; // Contenedor de tarjeta
        CheckBox chkSeleccion; // Casilla de marcado

        public AvionViewHolder(@NonNull View v) {
            super(v);
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