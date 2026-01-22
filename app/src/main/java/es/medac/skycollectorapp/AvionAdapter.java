package es.medac.skycollectorapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class AvionAdapter extends RecyclerView.Adapter<AvionAdapter.AvionViewHolder> {

    private List<Avion> listaAviones;
    private final OnItemClickListener listener;
    private final OnSelectionChangedListener selectionListener; // Nuevo oyente

    // Interfaz para clic normal (abrir detalle)
    public interface OnItemClickListener {
        void onItemClick(Avion avion, int position);
    }

    // Interfaz para avisar al Main que hemos marcado/desmarcado algo
    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    public AvionAdapter(List<Avion> listaAviones, OnItemClickListener listener, OnSelectionChangedListener selectionListener) {
        this.listaAviones = listaAviones;
        this.listener = listener;
        this.selectionListener = selectionListener;
    }

    @Override
    public void onBindViewHolder(@NonNull AvionViewHolder holder, int position) {
        Avion avion = listaAviones.get(position);

        // Datos visuales
        holder.txtModelo.setText(avion.getApodo());
        holder.txtFabricante.setText(avion.getFabricante());
        holder.txtRareza.setText(avion.getRareza());

        int color = avion.getColorRareza();
        holder.cardView.setStrokeColor(color);
        holder.cardView.setStrokeWidth(5);
        holder.txtRareza.setBackgroundColor(color);

        Glide.with(holder.itemView.getContext())
                .load(avion.getImagenResId())
                .placeholder(android.R.drawable.ic_menu_camera)
                .into(holder.imgAvion);

        if (avion.getUriFotoUsuario() != null) {
            holder.imgAvion.setImageURI(android.net.Uri.parse(avion.getUriFotoUsuario()));
        }

        // --- LÓGICA DEL CHECKBOX (SÚPER PRECISA) ---

        // 1. Quitamos el listener anterior para evitar bugs al hacer scroll
        holder.chkSeleccion.setOnCheckedChangeListener(null);

        // 2. Ponemos el estado real del avión
        holder.chkSeleccion.setChecked(avion.isSeleccionado());

        // 3. Detectamos el clic directamente en el CheckBox
        holder.chkSeleccion.setOnClickListener(v -> {
            boolean estaMarcado = holder.chkSeleccion.isChecked();
            avion.setSeleccionado(estaMarcado);
            // Avisamos al Main para que muestre u oculte la papelera
            selectionListener.onSelectionChanged();
        });

        // --- CLIC EN LA TARJETA (ABRIR DETALLE) ---
        holder.itemView.setOnClickListener(v -> {
            listener.onItemClick(avion, position);
        });
    }

    // Método para borrar visualmente los seleccionados
    public void borrarSeleccionados() {
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