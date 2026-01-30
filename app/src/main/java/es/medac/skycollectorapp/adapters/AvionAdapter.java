package es.medac.skycollectorapp.adapters;

import android.graphics.Color;
import android.net.Uri;
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

    public interface OnItemClickListener {
        void onItemClick(Avion avion, int position);
    }

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

        // 1. DATOS DE TEXTO
        holder.txtModelo.setText(avion.getApodo()); // O avion.getModelo()
        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());

        // 2. COLORES (Borde y etiqueta)
        int color = avion.getColorRareza();
        if (color == 0) color = Color.GRAY; // Protección por si viene 0

        holder.cardView.setStrokeColor(color);
        holder.cardView.setStrokeWidth(4);
        holder.txtRareza.setBackgroundColor(color);

        // 3. IMAGEN (Lógica restaurada y segura)
        Object imagenCarga;

        // ¿Tiene foto de usuario (cámara/galería)?
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            imagenCarga = Uri.parse(avion.getUriFotoUsuario());
        }
        // ¿Tiene foto oficial (base de datos)?
        else if (avion.getImagenResId() != 0) {
            imagenCarga = avion.getImagenResId();
        }
        // Si no tiene nada, icono por defecto
        else {
            imagenCarga = android.R.drawable.ic_menu_gallery;
        }
        // Cargar imagen
        Glide.with(holder.itemView.getContext())
                .load(imagenCarga)
                .fitCenter()
                .override(600, 400)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_delete)
                .into(holder.imgAvion);

        // 4. CHECKBOX (Sin bugs de scroll)
        holder.chkSeleccion.setOnCheckedChangeListener(null);
        holder.chkSeleccion.setChecked(avion.isSeleccionado());

        holder.chkSeleccion.setOnClickListener(v -> {
            boolean isChecked = holder.chkSeleccion.isChecked();
            avion.setSeleccionado(isChecked);
            selectionListener.onSelectionChanged();
        });

        holder.itemView.setOnClickListener(v -> listener.onItemClick(avion, position));
    }

    @Override
    public int getItemCount() {
        return listaAviones.size();
    }

    // --- MÉTODO PARA BORRAR (Necesario para el Main) ---
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