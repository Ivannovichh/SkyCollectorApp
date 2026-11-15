// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.R;

// Actividad que muestra la clasificación global de pilotos ordenada por puntuación
public class RankingActivity extends AppCompatActivity {

    // Estructura interna que representa cada fila de la clasificación
    private static class EntradaRanking {
        final String uid;
        final String nombreUsuario;
        final String fotoPerfilUrl;
        final long puntos;
        final long totalAviones;

        EntradaRanking(String uid, String nombreUsuario, String fotoPerfilUrl, long puntos, long totalAviones) {
            this.uid = uid;
            this.nombreUsuario = nombreUsuario;
            this.fotoPerfilUrl = fotoPerfilUrl;
            this.puntos = puntos;
            this.totalAviones = totalAviones;
        }
    }

    private RecyclerView recyclerRanking;
    private ProgressBar progressRanking;
    private TextView txtRankingVacio;
    private final List<EntradaRanking> listaRanking = new ArrayList<>();
    private RankingAdapter adapter;
    private String uidActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        uidActual = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        ImageButton btnVolver = findViewById(R.id.btnVolverRanking);
        recyclerRanking = findViewById(R.id.recyclerRanking);
        progressRanking = findViewById(R.id.progressRanking);
        txtRankingVacio = findViewById(R.id.txtRankingVacio);

        btnVolver.setOnClickListener(v -> finish());

        adapter = new RankingAdapter();
        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        recyclerRanking.setAdapter(adapter);

        cargarClasificacion();
    }

    // Consulta en Firestore los pilotos ordenados de mayor a menor puntuación
    private void cargarClasificacion() {
        progressRanking.setVisibility(View.VISIBLE);
        txtRankingVacio.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("ranking")
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressRanking.setVisibility(View.GONE);
                    listaRanking.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String uid = doc.getId();
                        String nombre = doc.getString("nombreUsuario");
                        if (nombre == null || nombre.trim().isEmpty()) {
                            nombre = getString(R.string.piloto_provisional);
                        }
                        String foto = doc.getString("fotoPerfilUrl");
                        Long puntosObj = doc.getLong("puntos");
                        Long totalObj = doc.getLong("totalAviones");

                        long puntos = puntosObj != null ? puntosObj : 0L;
                        long total = totalObj != null ? totalObj : 0L;

                        listaRanking.add(new EntradaRanking(uid, nombre, foto, puntos, total));
                    }

                    if (listaRanking.isEmpty()) {
                        txtRankingVacio.setVisibility(View.VISIBLE);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressRanking.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_error_cargar_ranking), Toast.LENGTH_SHORT).show();
                });
    }

    // Adaptador del RecyclerView para pintar cada puesto y abrir el perfil público al pulsar
    private class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EntradaRanking item = listaRanking.get(position);
            int puesto = position + 1;

            holder.txtPosicion.setText("#" + puesto);
            holder.txtNombre.setText(item.nombreUsuario);
            holder.txtAvistados.setText(getString(R.string.ranking_avistados_formato, item.totalAviones));
            holder.txtPuntos.setText(getString(R.string.ranking_puntos_formato, item.puntos));

            // Colores especiales para el podio y resaltado si es el propio usuario
            if (puesto == 1) {
                holder.txtPosicion.setTextColor(Color.parseColor("#FFD700")); // Oro
            } else if (puesto == 2) {
                holder.txtPosicion.setTextColor(Color.parseColor("#E0E0E0")); // Plata
            } else if (puesto == 3) {
                holder.txtPosicion.setTextColor(Color.parseColor("#CD7F32")); // Bronce
            } else {
                holder.txtPosicion.setTextColor(Color.parseColor("#00E5FF"));
            }

            if (item.uid.equals(uidActual)) {
                holder.card.setStrokeColor(Color.parseColor("#00E5FF"));
                holder.card.setStrokeWidth(4);
            } else {
                holder.card.setStrokeColor(Color.parseColor("#26324B"));
                holder.card.setStrokeWidth(2);
            }

            if (item.fotoPerfilUrl != null && !item.fotoPerfilUrl.isEmpty()) {
                Glide.with(RankingActivity.this)
                        .load(item.fotoPerfilUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .error(android.R.drawable.ic_menu_myplaces)
                        .circleCrop()
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

            // Al tocar cualquier fila se abre el perfil público de ese piloto
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(RankingActivity.this, PerfilPublicoActivity.class);
                intent.putExtra("uid_piloto", item.uid);
                intent.putExtra("puesto_piloto", puesto);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return listaRanking.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final TextView txtPosicion;
            final ImageView imgAvatar;
            final TextView txtNombre;
            final TextView txtAvistados;
            final TextView txtPuntos;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardItemRanking);
                txtPosicion = itemView.findViewById(R.id.txtPosicionRanking);
                imgAvatar = itemView.findViewById(R.id.imgAvatarRanking);
                txtNombre = itemView.findViewById(R.id.txtNombreRanking);
                txtAvistados = itemView.findViewById(R.id.txtAvistadosRanking);
                txtPuntos = itemView.findViewById(R.id.txtPuntosRanking);
            }
        }
    }
}