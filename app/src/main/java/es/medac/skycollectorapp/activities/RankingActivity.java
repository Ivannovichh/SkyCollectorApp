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
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.R;

/**
 * Actividad que muestra la clasificación global de pilotos con puntuación positiva (> 0)
 * e incluye una barra fija inferior con la posición exacta del usuario autenticado.
 */
public class RankingActivity extends AppCompatActivity {

    /**
     * Estructura interna que representa cada fila de la clasificación.
     */
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

    // Vistas de la tarjeta inferior fija del usuario actual
    private MaterialCardView cardMiPosicion;
    private TextView txtMiPosicion;
    private ImageView imgMiAvatar;
    private TextView txtMiNombre;
    private TextView txtMisAvistados;
    private TextView txtMisPuntos;

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

        cardMiPosicion = findViewById(R.id.cardMiPosicionRanking);
        txtMiPosicion = findViewById(R.id.txtMiPosicionRanking);
        imgMiAvatar = findViewById(R.id.imgMiAvatarRanking);
        txtMiNombre = findViewById(R.id.txtMiNombreRanking);
        txtMisAvistados = findViewById(R.id.txtMisAvistadosRanking);
        txtMisPuntos = findViewById(R.id.txtMisPuntosRanking);

        btnVolver.setOnClickListener(v -> finish());

        adapter = new RankingAdapter();
        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        recyclerRanking.setAdapter(adapter);

        cargarClasificacion();
    }

    /**
     * Consulta en Firestore los 50 mejores pilotos con puntuación mayor que 0
     * y resuelve la posición exacta del usuario actual en la barra inferior.
     */
    private void cargarClasificacion() {
        progressRanking.setVisibility(View.VISIBLE);
        txtRankingVacio.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ranking")
                .whereGreaterThan("puntos", 0L)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressRanking.setVisibility(View.GONE);
                    listaRanking.clear();

                    int puestoEncontradoEnTop = -1;
                    EntradaRanking entradaPropiaEnTop = null;

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

                        // Filtro defensivo extra para asegurar que no se muestren usuarios con 0 puntos
                        if (puntos <= 0L) continue;

                        EntradaRanking entrada = new EntradaRanking(uid, nombre, foto, puntos, total);
                        listaRanking.add(entrada);

                        if (!uidActual.isEmpty() && uid.equals(uidActual)) {
                            puestoEncontradoEnTop = listaRanking.size();
                            entradaPropiaEnTop = entrada;
                        }
                    }

                    if (listaRanking.isEmpty()) {
                        txtRankingVacio.setVisibility(View.VISIBLE);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    if (!uidActual.isEmpty()) {
                        if (entradaPropiaEnTop != null) {
                            mostrarTarjetaMiPosicion(puestoEncontradoEnTop, entradaPropiaEnTop);
                        } else {
                            cargarMiPosicionFueraDelTop(db);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressRanking.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_error_cargar_ranking), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Consulta los datos del usuario actual cuando no figura en el Top 50 y calcula su puesto global
     * contando cuántos pilotos tienen estrictamente más puntos que él en Firestore.
     */
    private void cargarMiPosicionFueraDelTop(FirebaseFirestore db) {
        db.collection("ranking").document(uidActual).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed() || !doc.exists()) return;

                    String nombre = doc.getString("nombreUsuario");
                    if (nombre == null || nombre.trim().isEmpty()) {
                        nombre = getString(R.string.piloto_provisional);
                    }
                    String foto = doc.getString("fotoPerfilUrl");
                    Long puntosObj = doc.getLong("puntos");
                    Long totalObj = doc.getLong("totalAviones");

                    long misPuntos = puntosObj != null ? puntosObj : 0L;
                    long misAviones = totalObj != null ? totalObj : 0L;
                    EntradaRanking miEntrada = new EntradaRanking(uidActual, nombre, foto, misPuntos, misAviones);

                    if (misPuntos <= 0L) {
                        mostrarTarjetaMiPosicion(-1, miEntrada);
                        return;
                    }

                    db.collection("ranking")
                            .whereGreaterThan("puntos", misPuntos)
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(aggregate -> {
                                if (isFinishing() || isDestroyed()) return;
                                int puestoGlobal = (int) aggregate.getCount() + 1;
                                mostrarTarjetaMiPosicion(puestoGlobal, miEntrada);
                            })
                            .addOnFailureListener(e -> {
                                if (isFinishing() || isDestroyed()) return;
                                mostrarTarjetaMiPosicion(-1, miEntrada);
                            });
                });
    }

    /**
     * Rellena y muestra la barra inferior fija con el puesto, avatar y puntos del usuario actual.
     */
    private void mostrarTarjetaMiPosicion(int puesto, EntradaRanking miEntrada) {
        cardMiPosicion.setVisibility(View.VISIBLE);

        if (puesto > 0) {
            txtMiPosicion.setText("#" + puesto);
            if (puesto == 1) txtMiPosicion.setTextColor(Color.parseColor("#FFD700"));
            else if (puesto == 2) txtMiPosicion.setTextColor(Color.parseColor("#E0E0E0"));
            else if (puesto == 3) txtMiPosicion.setTextColor(Color.parseColor("#CD7F32"));
            else txtMiPosicion.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            txtMiPosicion.setText("#—");
            txtMiPosicion.setTextColor(Color.parseColor("#90A4AE"));
        }

        txtMiNombre.setText(getString(R.string.ranking_tu_usuario_formato, miEntrada.nombreUsuario));
        txtMisPuntos.setText(getString(R.string.ranking_puntos_formato, miEntrada.puntos));

        if (miEntrada.puntos <= 0L) {
            txtMisAvistados.setText(getString(R.string.ranking_sin_puntos_aviso));
        } else {
            txtMisAvistados.setText(getString(R.string.ranking_avistados_formato, miEntrada.totalAviones));
        }

        if (miEntrada.fotoPerfilUrl != null && !miEntrada.fotoPerfilUrl.isEmpty()) {
            Glide.with(this)
                    .load(miEntrada.fotoPerfilUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(imgMiAvatar);
        } else {
            imgMiAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        cardMiPosicion.setOnClickListener(v -> {
            Intent intent = new Intent(RankingActivity.this, PerfilPublicoActivity.class);
            intent.putExtra("uid_piloto", miEntrada.uid);
            intent.putExtra("puesto_piloto", puesto > 0 ? puesto : 0);
            startActivity(intent);
        });
    }

    /**
     * Adaptador del RecyclerView para pintar cada puesto y abrir el perfil público al pulsar.
     */
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