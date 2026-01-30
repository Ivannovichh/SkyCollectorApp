package es.medac.skycollectorapp.adapters;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Mensaje;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Mensaje> listaMensajes;

    public ChatAdapter(List<Mensaje> listaMensajes) {
        this.listaMensajes = listaMensajes;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Mensaje msg = listaMensajes.get(position);
        holder.txtMensaje.setText(msg.getContenido());

        // Lógica visual: Usuario a la derecha (Verde), Bot a la izquierda (Gris)
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.txtMensaje.getLayoutParams();

        if (msg.isEsMio()) {
            params.gravity = Gravity.END;
            holder.txtMensaje.setBackgroundColor(Color.parseColor("#00C853")); // Verde
            holder.txtMensaje.setTextColor(Color.WHITE);
        } else {
            params.gravity = Gravity.START;
            holder.txtMensaje.setBackgroundColor(Color.parseColor("#424242")); // Gris oscuro
            holder.txtMensaje.setTextColor(Color.WHITE);
        }
        holder.txtMensaje.setLayoutParams(params);
    }

    @Override
    public int getItemCount() { return listaMensajes.size(); }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView txtMensaje;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMensaje = itemView.findViewById(R.id.txtMensaje);
        }
    }
}