package es.medac.skycollectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;
    private TextView txtVacio;
    private ImageButton btnPapelera;
    private int posicionEditando = -1;

    // --- LANZADORES (Igual que antes) ---
    private final ActivityResultLauncher<Intent> lanzadorDetalle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion avionModificado = (Avion) result.getData().getSerializableExtra("avion_modificado");
                    if (posicionEditando != -1 && avionModificado != null) {
                        listaAviones.set(posicionEditando, avionModificado);
                        adapter.notifyItemChanged(posicionEditando);
                        guardarDatosEnMovil();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> lanzadorAddAvion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion nuevoAvion = (Avion) result.getData().getSerializableExtra("nuevo_avion");

                    if (nuevoAvion != null) {
                        if (listaAviones.contains(nuevoAvion)) {
                            Toast.makeText(this, "⚠️ ¡Ese avión ya existe!", Toast.LENGTH_LONG).show();
                        } else {
                            listaAviones.add(0, nuevoAvion);
                            adapter.notifyItemInserted(0);
                            recyclerView.scrollToPosition(0);
                            actualizarVistaVacia();
                            guardarDatosEnMovil();
                            comprobarSeleccion(); // Por si acaso
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        txtVacio = findViewById(R.id.txtVacio);
        btnPapelera = findViewById(R.id.btnPapelera);

        listaAviones = cargarDatosDelMovil();
        for(Avion a : listaAviones) a.setSeleccionado(false);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new AvionAdapter(listaAviones,
                (avion, position) -> {
                    posicionEditando = position;
                    Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
                    intent.putExtra("avion_extra", avion);
                    lanzadorDetalle.launch(intent);
                },
                this::comprobarSeleccion
        );

        recyclerView.setAdapter(adapter);
        actualizarVistaVacia();
        comprobarSeleccion();

        // --- NUEVA LÓGICA DE BOTONES INFERIORES ---

        // 1. CHATBOT (Izquierda)
            findViewById(R.id.btnChat).setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
                startActivity(intent);

        });

        // 2. AÑADIR AVIÓN (Centro - El botón +)
        findViewById(R.id.btnAddAvion).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAvionActivity.class);
            lanzadorAddAvion.launch(intent);
        });

        // 3. MAPA FLIGHTRADAR (Derecha)
        findViewById(R.id.btnMap).setOnClickListener(v -> {
            Toast.makeText(this, "🗺️ Mapa Flightradar próximamente", Toast.LENGTH_SHORT).show();
            // Aquí abriremos la actividad del Mapa en el futuro
        });

        // PERFIL (Arriba)
        findViewById(R.id.cardPerfil).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PerfilActivity.class));
        });

        // PAPELERA (Borrar)
        btnPapelera.setOnClickListener(v -> {
            adapter.borrarSeleccionados();
            guardarDatosEnMovil();
            actualizarVistaVacia();
            comprobarSeleccion();
            Toast.makeText(this, "Eliminados correctamente", Toast.LENGTH_SHORT).show();
        });
    }

    // --- LÓGICA DE LA PAPELERA INTELIGENTE ---
    private void comprobarSeleccion() {
        boolean hayAlgoSeleccionado = false;

        // Recorremos la lista para ver si hay al menos uno marcado
        for (Avion a : listaAviones) {
            if (a.isSeleccionado()) {
                hayAlgoSeleccionado = true;
                break; // Ya encontramos uno, no hace falta seguir buscando
            }
        }

        // Si hay seleccionados, mostramos papelera. Si no, la ocultamos.
        if (hayAlgoSeleccionado) {
            btnPapelera.setVisibility(View.VISIBLE);
        } else {
            btnPapelera.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
            String fotoUri = prefs.getString("foto_" + user.getUid(), null);
            if (fotoUri != null) {
                ImageView imgMini = findViewById(R.id.imgPerfilMini);
                com.bumptech.glide.Glide.with(this).load(fotoUri).circleCrop().into(imgMini);
            }
        }
    }

    private void guardarDatosEnMovil() {
        SharedPreferences sharedPreferences = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(listaAviones);
        editor.putString("lista_aviones", json);
        editor.apply();
    }

    private List<Avion> cargarDatosDelMovil() {
        SharedPreferences sharedPreferences = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("lista_aviones", null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void actualizarVistaVacia() {
        if (listaAviones.isEmpty()) {
            txtVacio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtVacio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}