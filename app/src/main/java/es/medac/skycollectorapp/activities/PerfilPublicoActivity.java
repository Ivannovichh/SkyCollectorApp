// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.utils.AvionGenerator;

// Actividad que muestra el perfil público de un piloto y sus avistamientos verificados en modo solo lectura
public class PerfilPublicoActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private TextView txtNombre;
    private TextView txtEstadisticas;
    private RecyclerView recyclerAviones;
    private ProgressBar progressBar;
    private TextView txtVacio;

    private final List<Avion> avionesPiloto = new ArrayList<>();
    private AvionesPublicosAdapter adapter;
    private int puestoPiloto = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_publico);

        ImageButton btnVolver = findViewById(R.id.btnVolverPerfilPublico);
        imgAvatar = findViewById(R.id.imgAvatarPerfilPublico);
        txtNombre = findViewById(R.id.txtNombrePerfilPublico);
        txtEstadisticas = findViewById(R.id.txtEstadisticasPerfilPublico);
        recyclerAviones = findViewById(R.id.recyclerAvionesPublicos);
        progressBar = findViewById(R.id.progressPerfilPublico);
        txtVacio = findViewById(R.id.txtHangarPublicoVacio);

        btnVolver.setOnClickListener(v -> finish());

        String uidPiloto = getIntent().getStringExtra("uid_piloto");
        puestoPiloto = getIntent().getIntExtra("puesto_piloto", -1);

        if (uidPiloto == null || uidPiloto.isEmpty()) {
            finish();
            return;
        }

        adapter = new AvionesPublicosAdapter();
        recyclerAviones.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerAviones.setAdapter(adapter);

        cargarPerfilPublico(uidPiloto);
    }

    // Descarga el documento público ranking/{uid} con los datos del piloto y su lista de aviones
    @SuppressWarnings("unchecked")
    private void cargarPerfilPublico(String uidPiloto) {
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("ranking")
                .document(uidPiloto)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);

                    if (!doc.exists()) {
                        txtVacio.setVisibility(View.VISIBLE);
                        return;
                    }

                    String nombre = doc.getString("nombreUsuario");
                    txtNombre.setText(nombre != null && !nombre.isEmpty()
                            ? nombre
                            : getString(R.string.piloto_provisional));

                    String fotoUrl = doc.getString("fotoPerfilUrl");
                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(fotoUrl)
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .error(android.R.drawable.ic_menu_myplaces)
                                .circleCrop()
                                .into(imgAvatar);
                    }

                    long puntos = doc.getLong("puntos") != null ? doc.getLong("puntos") : 0L;
                    long totalAviones = doc.getLong("totalAviones") != null ? doc.getLong("totalAviones") : 0L;

                    if (puestoPiloto > 0) {
                        txtEstadisticas.setText(getString(R.string.perfil_publico_stats_con_puesto, puestoPiloto, puntos, totalAviones));
                    } else {
                        txtEstadisticas.setText(getString(R.string.perfil_publico_stats, puntos, totalAviones));
                    }

                    avionesPiloto.clear();
                    Object rawLista = doc.get("avionesPublicos");
                    if (rawLista instanceof List) {
                        for (Object elem : (List<Object>) rawLista) {
                            if (elem instanceof Map) {
                                avionesPiloto.add(mapearAvionDesdeFirestore((Map<String, Object>) elem));
                            }
                        }
                    }

                    if (avionesPiloto.isEmpty()) {
                        txtVacio.setVisibility(View.VISIBLE);
                    } else {
                        txtVacio.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_error_cargar_perfil_publico), Toast.LENGTH_SHORT).show();
                });
    }

    // Reconstruye un objeto Avion a partir de cada mapa guardado en avionesPublicos
    private Avion mapearAvionDesdeFirestore(Map<String, Object> map) {
        Avion a = new Avion();
        a.setId(obtenerString(map, "id"));
        a.setModelo(obtenerString(map, "modelo"));
        a.setApodo(obtenerString(map, "apodo"));
        a.setFabricante(obtenerString(map, "fabricante"));
        a.setRareza(obtenerString(map, "rareza"));
        a.setTipoIcao(obtenerString(map, "tipoIcao"));
        a.setIcao24(obtenerString(map, "icao24"));
        a.setMatricula(obtenerString(map, "matricula"));
        a.setDescripcion(obtenerString(map, "descripcion"));
        a.setUrlImagenRemota(obtenerString(map, "urlImagenRemota"));
        a.setUriFotoUsuario(obtenerString(map, "uriFotoUsuario"));

        Object distObj = map.get("distanciaKm");
        if (distObj instanceof Number) {
            a.setDistanciaKm(((Number) distObj).floatValue());
        }

        Object fechaObj = map.get("fechaAvistamiento");
        if (fechaObj instanceof Number) {
            a.setFechaAvistamiento(((Number) fechaObj).longValue());
        }

        // Resuelve la silueta por tipo ICAO o nombre del modelo (con sil_jet como respaldo seguro)
        a.setImagenResId(AvionGenerator.resolverImagenPorModelo(a.getModelo(), a.getTipoIcao()));

        return a;
    }

    // Extrae un valor de texto de forma segura desde el mapa de Firestore
    private String obtenerString(Map<String, Object> map, String clave) {
        Object val = map.get(clave);
        return val != null ? String.valueOf(val) : null;
    }

    // Abre la ficha grande del avión en modo solo lectura (sin el botón inferior de avistar)
    private void abrirFichaSoloLectura(Avion avion) {
        Intent intent = new Intent(this, DetalleVueloRadarActivity.class);
        intent.putExtra("modo_solo_lectura", true);
        intent.putExtra("icao", avion.getIcao24());
        intent.putExtra("tipo", avion.getTipoIcao());
        intent.putExtra("matricula", avion.getMatricula());
        intent.putExtra("modelo", avion.getModelo());
        intent.putExtra("empresa", avion.getFabricante());
        intent.putExtra("distancia_km", avion.getDistanciaKm());
        intent.putExtra("fecha_avistamiento", avion.getFechaAvistamiento());
        intent.putExtra("foto_avistamiento_url", avion.getUriFotoUsuario() != null ? avion.getUriFotoUsuario() : avion.getUrlImagenRemota());
        startActivity(intent);
    }

    // Adaptador en cuadrícula reutilizando item_avion.xml sin casilla de borrado
    private class AvionesPublicosAdapter extends RecyclerView.Adapter<AvionesPublicosAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Avion avion = avionesPiloto.get(position);

            holder.chkSeleccion.setVisibility(View.GONE);
            holder.txtCantidad.setVisibility(View.GONE);

            holder.txtModelo.setText(avion.getModelo());
            holder.txtFabricante.setText(avion.getFabricante());
            holder.txtRareza.setText(AvionGenerator.traducirRareza(PerfilPublicoActivity.this, avion.getRareza()));
            holder.card.setStrokeColor(avion.getColorRareza());

            int silueta = avion.getImagenResId() != 0 ? avion.getImagenResId() : R.drawable.sil_jet;
            String fuenteFoto = (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty())
                    ? avion.getUriFotoUsuario()
                    : avion.getUrlImagenRemota();

            if (fuenteFoto != null && !fuenteFoto.isEmpty()) {
                Glide.with(PerfilPublicoActivity.this)
                        .load(fuenteFoto)
                        .placeholder(silueta)
                        .error(silueta)
                        .centerCrop()
                        .into(holder.imgAvion);
            } else {
                holder.imgAvion.setImageResource(silueta);
            }

            holder.itemView.setOnClickListener(v -> abrirFichaSoloLectura(avion));
        }

        @Override
        public int getItemCount() {
            return avionesPiloto.size();
        }

        // Contenedor de vistas para cada tarjeta de aeronave en el perfil público
        class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ImageView imgAvion;
            final TextView txtRareza;
            final CheckBox chkSeleccion;
            final TextView txtCantidad;
            final TextView txtModelo;
            final TextView txtFabricante;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.cardContainer);
                imgAvion = itemView.findViewById(R.id.imgAvion);
                txtRareza = itemView.findViewById(R.id.txtRareza);
                chkSeleccion = itemView.findViewById(R.id.chkSeleccion);
                txtCantidad = itemView.findViewById(R.id.txtCantidad);
                txtModelo = itemView.findViewById(R.id.txtModelo);
                txtFabricante = itemView.findViewById(R.id.txtFabricante);
            }
        }
    }
}