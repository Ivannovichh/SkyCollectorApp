package es.medac.skycollectorapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import es.medac.skycollectorapp.models.Mensaje;
import es.medac.skycollectorapp.R;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Mensaje> listaMensajes;

    public ChatAdapter(List<Mensaje> listaMensajes) { this.listaMensajes = listaMensajes; }

    @NonNull @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new ChatViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder h, int pos) {
        Mensaje m = listaMensajes.get(pos);
        if (m.isEsMio()) {
            h.txtUsuario.setText(m.getTexto());
            h.txtUsuario.setVisibility(View.VISIBLE);
            h.txtRobot.setVisibility(View.GONE);
        } else {
            h.txtRobot.setText(m.getTexto());
            h.txtRobot.setVisibility(View.VISIBLE);
            h.txtUsuario.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return listaMensajes.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView txtRobot, txtUsuario;
        public ChatViewHolder(@NonNull View v) {
            super(v);
            txtRobot = v.findViewById(R.id.txtRobot);
            txtUsuario = v.findViewById(R.id.txtUsuario);
        }
    }
}