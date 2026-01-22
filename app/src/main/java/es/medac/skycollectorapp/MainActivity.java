package es.medac.skycollectorapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private int posicionEditando = -1;

    // Lanza la pantalla de detalle para editar
    private final ActivityResultLauncher<Intent> lanzadorDetalle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion avionModificado = (Avion) result.getData().getSerializableExtra("avion_modificado");
                    if (posicionEditando != -1 && avionModificado != null) {
                        listaAviones.set(posicionEditando, avionModificado);
                        adapter.notifyItemChanged(posicionEditando);
                        guardarDatosEnMovil(); // <--- GUARDA AUTOMÁTICAMENTE
                    }
                }
            }
    );

    // Lanza la pantalla para añadir uno nuevo
    private final ActivityResultLauncher<Intent> lanzadorAddAvion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion nuevoAvion = (Avion) result.getData().getSerializableExtra("nuevo_avion");
                    if (nuevoAvion != null) {
                        listaAviones.add(0, nuevoAvion);
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                        actualizarVistaVacia();
                        guardarDatosEnMovil(); // <--- GUARDA AUTOMÁTICAMENTE
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

        // 1. CARGAMOS LOS DATOS GUARDADOS
        listaAviones = cargarDatosDelMovil();

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AvionAdapter(listaAviones, (avion, position) -> {
            posicionEditando = position;
            Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
            intent.putExtra("avion_extra", avion);
            lanzadorDetalle.launch(intent);
        });
        recyclerView.setAdapter(adapter);
        actualizarVistaVacia();

        // Botón Escanear/Añadir
        findViewById(R.id.btnEscanear).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAvionActivity.class);
            lanzadorAddAvion.launch(intent);
        });

        // =======================================================
        // BOTÓN ROJO DE EMERGENCIA (PARA CARGAR DATOS LA 1ª VEZ)
        // =======================================================
        Button btnReset = new Button(this);
        btnReset.setText("REINICIAR COLECCIÓN (CARGA INICIAL)");
        btnReset.setBackgroundColor(android.graphics.Color.RED);
        btnReset.setTextColor(android.graphics.Color.WHITE);
        addContentView(btnReset, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnReset.setOnClickListener(v -> cargarDatosInicialesPorDefecto());
    }

    // --- MÉTODOS DE GUARDADO LOCAL (SIN FIREBASE) ---

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
            return new ArrayList<>(); // Si no hay nada, lista vacía
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

    private void cargarDatosInicialesPorDefecto() {
        // Borramos lo que haya y ponemos los 3 básicos
        listaAviones.clear();
        listaAviones.add(new Avion("Boeing 737", "Boeing", "COMMON", android.R.drawable.ic_menu_camera, "842 km/h", "189 pax", "39m", "EE.UU.", "79t"));
        listaAviones.add(new Avion("Airbus A320", "Airbus", "COMMON", android.R.drawable.ic_menu_camera, "828 km/h", "180 pax", "34m", "Europa", "78t"));
        listaAviones.add(new Avion("F-22 Raptor", "Lockheed", "LEGENDARY", android.R.drawable.ic_menu_camera, "2414 km/h", "1 piloto", "13m", "EE.UU.", "38t"));

        adapter.notifyDataSetChanged();
        guardarDatosEnMovil(); // Guardar cambios
        actualizarVistaVacia();
        Toast.makeText(this, "¡Colección Reiniciada!", Toast.LENGTH_SHORT).show();
    }
}