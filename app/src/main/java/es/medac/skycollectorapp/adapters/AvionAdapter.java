package es.medac.skycollectorapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;

public class AvionAdapter extends RecyclerView.Adapter<AvionAdapter.AvionViewHolder> {

    private List<Avion> listaAviones;
    private final OnItemClickListener listener;
    private final OnSelectionChangedListener selectionListener;

    // Interfaz para clics normales
    public interface OnItemClickListener {
        void onItemClick(Avion avion, int position);
    }

    // Interfaz para cambios en el CheckBox
    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    public AvionAdapter(List<Avion> listaAviones, OnItemClickListener listener, OnSelectionChangedListener selectionListener) {
        this.listaAviones = listaAviones;
        this.listener = listener;
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        return new AvionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        Avion avion = listaAviones.get(position);

        // 1) TEXTOS
        holder.txtModelo.setText(avion.getApodo());
        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());

        // 2) COLORES (Borde y etiqueta)
        int color = avion.getColorRareza();
        if (color == 0) color = Color.GRAY;

        holder.cardView.setStrokeColor(color);
        holder.cardView.setStrokeWidth(4);

        // Si quieres que el fondo de rareza no se vea feo, puedes dejarlo así:
        holder.txtRareza.setBackgroundColor(color);

        // 3) IMAGEN
        // 🔥 CAMBIO CLAVE:
        // En la LISTA PRINCIPAL SIEMPRE se muestra la imagen oficial (imagenResId)
        Object imagenCarga;

        if (avion.getImagenResId() != 0) {
            imagenCarga = avion.getImagenResId();
        } else {
            // fallback si no hay imagen oficial
            imagenCarga = android.R.drawable.ic_menu_gallery;
        }

        Glide.with(holder.itemView.getContext())
                .load(imagenCarga)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // puedes cambiar a AUTOMATIC si quieres caché
                .skipMemoryCache(true)
                .fitCenter()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.imgAvion);

        // 4) CHECKBOX
        holder.chkSeleccion.setOnCheckedChangeListener(null);
        holder.chkSeleccion.setChecked(avion.isSeleccionado());

        holder.chkSeleccion.setOnClickListener(v -> {
            boolean isChecked = holder.chkSeleccion.isChecked();
            avion.setSeleccionado(isChecked);
            if (selectionListener != null) selectionListener.onSelectionChanged();
        });

        // 5) CLICK ITEM
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(avion, position);
        });
    }

    @Override
    public int getItemCount() {
        return listaAviones != null ? listaAviones.size() : 0;
    }

    // --- MÉTODO PARA BORRAR (Usado en MainActivity) ---
    public void borrarSeleccionados() {
        if (listaAviones == null) return;

        List<Avion> aConservar = new ArrayList<>();
        for (Avion a : listaAviones) {
            if (!a.isSeleccionado()) {
                aConservar.add(a);
            }
        }

        listaAviones.clear();
        listaAviones.addAll(aConservar);
        notifyDataSetChanged();
    }

    // --- CLASE VIEWHOLDER ---
    public static class AvionViewHolder extends RecyclerView.ViewHolder {

        TextView txtModelo, txtFabricante, txtRareza;
        ImageView imgAvion;
        MaterialCardView cardView;
        CheckBox chkSeleccion;

        public AvionViewHolder(@NonNull View v) {
            super(v);
            txtModelo = v.findViewById(R.id.txtModelo);
            txtFabricante = v.findViewById(R.id.txtFabricante);
            txtRareza = v.findViewById(R.id.txtRareza);
            imgAvion = v.findViewById(R.id.imgAvion);
            cardView = v.findViewById(R.id.cardContainer);
            chkSeleccion = v.findViewById(R.id.chkSeleccion);
        }
    }
}
