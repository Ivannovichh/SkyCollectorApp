package es.medac.skycollectorapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;
    private TextView txtVacio;

    // Variable para recordar qué posición estamos editando
    private int posicionEditando = -1;

    // 1. LANZADOR PARA RECIBIR CAMBIOS DEL DETALLE
    private final ActivityResultLauncher<Intent> lanzadorDetalle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Recuperamos el avión modificado
                    Avion avionModificado = (Avion) result.getData().getSerializableExtra("avion_modificado");

                    // Si todo es correcto, ACTUALIZAMOS LA LISTA
                    if (posicionEditando != -1 && avionModificado != null) {
                        listaAviones.set(posicionEditando, avionModificado); // Machacamos el viejo
                        adapter.notifyItemChanged(posicionEditando); // Avisamos visualmente
                    }
                }
            }
    );

    // 2. LANZADOR PARA AÑADIR NUEVOS
    private final ActivityResultLauncher<Intent> lanzadorAddAvion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion nuevo = (Avion) result.getData().getSerializableExtra("nuevo_avion");
                    // Evitar duplicados por modelo base (puedes quitar esto si quieres permitir repetidos con distinto nombre)
                    if (!listaAviones.contains(nuevo)) {
                        listaAviones.add(0, nuevo);
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                        actualizarVistaVacia();
                    } else {
                        Toast.makeText(this, "¡Ya tienes este modelo!", Toast.LENGTH_SHORT).show();
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
        listaAviones = new ArrayList<>();

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // CONECTAMOS EL ADAPTADOR CON LA LÓGICA DE CLIC
        adapter = new AvionAdapter(listaAviones, (avion, position) -> {
            posicionEditando = position; // Guardamos la posición (Importante para saber cuál actualizar luego)

            Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
            intent.putExtra("avion_extra", avion);

            // ¡USAMOS EL LANZADOR, NO STARTACTIVITY!
            lanzadorDetalle.launch(intent);
        });

        recyclerView.setAdapter(adapter);
        actualizarVistaVacia();

        Button btnEscanear = findViewById(R.id.btnEscanear);
        btnEscanear.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAvionActivity.class);
            lanzadorAddAvion.launch(intent);
        });
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