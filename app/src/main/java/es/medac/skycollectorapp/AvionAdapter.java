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
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Avion avion, int position);
    }

    public AvionAdapter(List<Avion> listaAviones, OnItemClickListener listener) {
        this.listaAviones = listaAviones;
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        Avion avion = listaAviones.get(position);

        holder.txtModelo.setText(avion.getApodo());
        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());

        // COLORES Y BORDES
        int color = avion.getColorRareza();
        holder.cardView.setStrokeColor(color);
        holder.cardView.setStrokeWidth(5);
        holder.txtRareza.setBackgroundColor(color);

        // IMAGEN (Prioridad a la cámara del usuario, si no hay, usa el recurso)
        Glide.with(holder.itemView.getContext())
                .load(avion.getImagenResId())
                .placeholder(android.R.drawable.ic_menu_camera)
                .into(holder.imgAvion);

        if (avion.getUriFotoUsuario() != null) {
            holder.imgAvion.setImageURI(android.net.Uri.parse(avion.getUriFotoUsuario()));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(avion, position));
    }

    @NonNull @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        return new AvionViewHolder(view);
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
            cardView = v.findViewById(R.id.cardContainer);
        }
    }
}