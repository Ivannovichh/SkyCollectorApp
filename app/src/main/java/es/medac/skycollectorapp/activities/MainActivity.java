package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager; // <--- IMPORTANTE: Importar esto

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.adapters.AvionAdapter;
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
import es.medac.skycollectorapp.models.Avion;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. INICIALIZAR LISTA Y ADAPTADOR
        listaAviones = new ArrayList<>();

        adapter = new AvionAdapter(listaAviones,
                // A) Clic Normal: Ver detalles
                (avion, position) -> {
                    Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
                    intent.putExtra("avion_extra", avion);
                    startActivity(intent);
                },
                // B) Selección cambiada: Mostrar/Ocultar papelera
                () -> {
                    actualizarPapelera();
                }
        );

        // --- CAMBIO CLAVE AQUÍ ---
        // Usamos GridLayoutManager con '2' columnas en lugar de LinearLayoutManager
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        // 2. CONFIGURACIÓN DE BOTONES
        binding.btnAddAvion.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddAvionActivity.class));
        });

        binding.btnPapelera.setOnClickListener(v -> borrarSeleccionados());

        binding.cardPerfil.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PerfilActivity.class));
        });

        binding.btnChat.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChatbotActivity.class));
        });

        binding.btnMap.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MapaActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarListaDeAviones();
    }

    // --- MÉTODOS DE CARGA Y BORRADO ---
    private void cargarListaDeAviones() {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        String json = prefs.getString("lista_aviones", null);

        listaAviones.clear();

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            List<Avion> avionesGuardados = gson.fromJson(json, type);
            if (avionesGuardados != null) {
                listaAviones.addAll(avionesGuardados);
            }
        }

        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.txtVacio.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
        actualizarPapelera();
    }

    private void borrarSeleccionados() {
        adapter.borrarSeleccionados();

        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        String json = gson.toJson(listaAviones);
        editor.putString("lista_aviones", json);
        editor.apply();

        actualizarPapelera();

        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
        }

        Toast.makeText(this, "Aviones eliminados", Toast.LENGTH_SHORT).show();
    }

    private void actualizarPapelera() {
        boolean haySeleccionados = false;
        for (Avion a : listaAviones) {
            if (a.isSeleccionado()) {
                haySeleccionados = true;
                break;
            }
        }

        if (haySeleccionados) {
            binding.btnPapelera.setVisibility(View.VISIBLE);
        } else {
            binding.btnPapelera.setVisibility(View.GONE);
        }
    }
}