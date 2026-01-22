package es.medac.skycollectorapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AvionAdapter extends RecyclerView.Adapter<AvionAdapter.AvionViewHolder> {

    private List<Avion> listaAviones;
    private final OnItemClickListener listener; // El "micrófono" para avisar a MainActivity

    // Interfaz para comunicarse
    public interface OnItemClickListener {
        void onItemClick(Avion avion, int position);
    }

    // Constructor que OBLIGA a pasar el listener
    public AvionAdapter(List<Avion> listaAviones, OnItemClickListener listener) {
        this.listaAviones = listaAviones;
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        Avion avion = listaAviones.get(position);

        // IMPORTANTE: Mostramos el APODO, no el modelo
        // Si no lo has editado, saldrá el modelo igual.
        holder.txtModelo.setText(avion.getApodo());

        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());
        holder.cardView.setStrokeColor(avion.getColorRareza());
        holder.txtRareza.setBackgroundColor(avion.getColorRareza());

        Glide.with(holder.itemView.getContext())
                .load(avion.getImagenResId())
                .fitCenter()
                .into(holder.imgAvion);

        // AQUÍ ESTÁ LA CLAVE:
        // Al hacer clic, NO abrimos la actividad.
        // Llamamos al listener. MainActivity decidirá qué hacer.
        holder.itemView.setOnClickListener(v -> listener.onItemClick(avion, position));
    }

    // ... Resto del código igual ...
    @NonNull @Override public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new AvionViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_avion, p, false));
    }
    @Override public int getItemCount() { return listaAviones.size(); }

    public static class AvionViewHolder extends RecyclerView.ViewHolder {
        TextView txtModelo, txtFabricante, txtRareza;
        ImageView imgAvion;
        MaterialCardView cardView;
        public AvionViewHolder(@NonNull View v) {
            super(v);
            txtModelo = v.findViewById(R.id.txtModelo);
            txtFabricante = v.findViewById(R.id.txtFabricante);
            txtRareza = v.findViewById(R.id.txtRareza);
            imgAvion = v.findViewById(R.id.imgAvion);
            cardView = (MaterialCardView) v;
        }
    }
}