package es.medac.skycollectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
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
    private int posicionEditando = -1;

    // --- 1. LANZADOR PARA EDITAR UN AVIÓN ---
    private final ActivityResultLauncher<Intent> lanzadorDetalle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion avionModificado = (Avion) result.getData().getSerializableExtra("avion_modificado");
                    if (posicionEditando != -1 && avionModificado != null) {
                        listaAviones.set(posicionEditando, avionModificado);
                        adapter.notifyItemChanged(posicionEditando);
                        guardarDatosEnMovil(); // Guardar cambios
                    }
                }
            }
    );

    // --- 2. LANZADOR PARA AÑADIR UNO NUEVO ---
    private final ActivityResultLauncher<Intent> lanzadorAddAvion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion nuevoAvion = (Avion) result.getData().getSerializableExtra("nuevo_avion");
                    if (nuevoAvion != null) {
                        listaAviones.add(0, nuevoAvion); // Añadir al principio
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                        actualizarVistaVacia();
                        guardarDatosEnMovil(); // Guardar cambios
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

        // A. CARGAMOS LOS DATOS GUARDADOS (Si no hay nada, lista vacía)
        listaAviones = cargarDatosDelMovil();

        // B. CONFIGURAMOS EL RECYCLERVIEW
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AvionAdapter(listaAviones, (avion, position) -> {
            posicionEditando = position;
            Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
            intent.putExtra("avion_extra", avion);
            lanzadorDetalle.launch(intent);
        });
        recyclerView.setAdapter(adapter);

        // C. COMPROBAR SI ESTÁ VACÍA (Para mostrar el texto de fondo)
        actualizarVistaVacia();

        // D. BOTÓN DE ESCANEAR / AÑADIR
        findViewById(R.id.btnEscanear).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAvionActivity.class);
            lanzadorAddAvion.launch(intent);
        });
    }

    // --- MÉTODOS DE GUARDADO LOCAL (SharedPreferences) ---

    private void guardarDatosEnMovil() {
        SharedPreferences sharedPreferences = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(listaAviones); // Convertimos la lista a texto
        editor.putString("lista_aviones", json);
        editor.apply();
    }

    private List<Avion> cargarDatosDelMovil() {
        SharedPreferences sharedPreferences = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("lista_aviones", null);

        if (json == null) {
            return new ArrayList<>(); // ¡IMPORTANTE! Si no hay datos, devolvemos lista vacía (0 aviones)
        }

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