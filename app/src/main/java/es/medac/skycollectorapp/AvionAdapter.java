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

        // Datos básicos
        holder.txtModelo.setText(avion.getApodo()); // Muestra el apodo
        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());

        // --- AQUÍ ESTÁN TUS BORDES DE COLORES ---
        // Recuperamos el color según la rareza y lo aplicamos al borde y al fondo de la etiqueta
        int color = avion.getColorRareza();

        holder.cardView.setStrokeColor(color); // Borde de la tarjeta
        holder.cardView.setStrokeWidth(5);     // Grosor del borde (para que se vea bien)
        holder.txtRareza.setBackgroundColor(color); // Fondo de la etiqueta pequeña

        // Cargar imagen con Glide
        Glide.with(holder.itemView.getContext())
                .load(avion.getImagenResId()) // Carga la imagen oficial
                .placeholder(android.R.drawable.ic_menu_camera) // Si falla, pone una cámara
                .into(holder.imgAvion);

        // Si el usuario tiene foto propia, intentamos cargarla (Opcional, si quieres usarlo)
        if (avion.getUriFotoUsuario() != null) {
            holder.imgAvion.setImageURI(android.net.Uri.parse(avion.getUriFotoUsuario()));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(avion, position));
    }

    @NonNull
    @Override
    public AvionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
        return new AvionViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return listaAviones.size();
    }

    public static class AvionViewHolder extends RecyclerView.ViewHolder {
        TextView txtModelo, txtFabricante, txtRareza;
        ImageView imgAvion;
        MaterialCardView cardView; // Usamos MaterialCardView para los bordes

        public AvionViewHolder(@NonNull View v) {
            super(v);
            txtModelo = v.findViewById(R.id.txtModelo);
            txtFabricante = v.findViewById(R.id.txtFabricante);
            txtRareza = v.findViewById(R.id.txtRareza);
            imgAvion = v.findViewById(R.id.imgAvion);
            // El casting necesario para poder cambiar el color del borde
            cardView = v.findViewById(R.id.cardContainer);
        }
    }
}