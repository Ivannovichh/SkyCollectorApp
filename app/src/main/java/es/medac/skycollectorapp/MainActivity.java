package es.medac.skycollectorapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;
    private TextView txtVacio;

    private int posicionEditando = -1;

    // --> NUEVO: Referencias a Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;


    // 1. LANZADOR PARA RECIBIR CAMBIOS DEL DETALLE (ACTUALIZADO PARA FIRESTORE)
    private final ActivityResultLauncher<Intent> lanzadorDetalle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion avionModificado = (Avion) result.getData().getSerializableExtra("avion_modificado");
                    String documentId = result.getData().getStringExtra("avion_document_id"); // --> NUEVO: Recuperar el ID

                    if (posicionEditando != -1 && avionModificado != null && documentId != null && !documentId.isEmpty()) {
                        // --> NUEVO: Actualizar en Firestore
                        db.collection("usuarios").document(currentUser.getUid()).collection("aviones")
                                .document(documentId)
                                .set(avionModificado) // set() sobrescribe el objeto completo
                                .addOnSuccessListener(aVoid -> {
                                    // Éxito en DB, ahora actualiza la UI
                                    listaAviones.set(posicionEditando, avionModificado);
                                    adapter.notifyItemChanged(posicionEditando);
                                    Toast.makeText(this, "Avión actualizado", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al actualizar el avión.", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }
    );

    // 2. LANZADOR PARA AÑADIR NUEVOS (ACTUALIZADO PARA FIRESTORE)
    private final ActivityResultLauncher<Intent> lanzadorAddAvion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Avion nuevoAvion = (Avion) result.getData().getSerializableExtra("nuevo_avion");
                    if (nuevoAvion != null) {
                        // --> NUEVO: Guardar en Firestore
                        db.collection("usuarios").document(currentUser.getUid()).collection("aviones")
                                .add(nuevoAvion) // add() crea un ID automático
                                .addOnSuccessListener(documentReference -> {
                                    // Éxito: El avión se guardó. Ahora lo añadimos a la lista local y actualizamos la UI.
                                    nuevoAvion.setDocumentId(documentReference.getId()); // Guardamos el ID en el objeto
                                    listaAviones.add(0, nuevoAvion);
                                    adapter.notifyItemInserted(0);
                                    recyclerView.scrollToPosition(0);
                                    actualizarVistaVacia();
                                    Toast.makeText(this, "Avión añadido a tu colección", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al guardar el avión.", Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --> NUEVO: Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // --> NUEVO: Comprobación de seguridad
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado. Vuelva a iniciar sesión.", Toast.LENGTH_LONG).show();
            // Aquí podrías redirigir a la pantalla de Login
            // Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // startActivity(intent);
            finish(); // Cierra esta actividad si no hay usuario
            return;
        }

        recyclerView = findViewById(R.id.recyclerView);
        txtVacio = findViewById(R.id.txtVacio);
        listaAviones = new ArrayList<>();

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new AvionAdapter(listaAviones, (avion, position) -> {
            posicionEditando = position;

            Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
            intent.putExtra("avion_extra", avion);
            // --> NUEVO: Pasamos el ID del documento para poder editarlo/borrarlo
            intent.putExtra("avion_document_id", avion.getDocumentId());
            lanzadorDetalle.launch(intent);
        });

        recyclerView.setAdapter(adapter);

        // --> NUEVO: Cargar datos desde Firestore en lugar de empezar con la lista vacía
        cargarAvionesDesdeFirestore();

        Button btnEscanear = findViewById(R.id.btnEscanear);
        btnEscanear.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddAvionActivity.class);
            lanzadorAddAvion.launch(intent);
        });
    }

    // --> NUEVO: Método para cargar los datos desde Firestore
    private void cargarAvionesDesdeFirestore() {
        // La ruta será /usuarios/{ID_DEL_USUARIO}/aviones/{ID_DEL_AVION}
        db.collection("usuarios").document(currentUser.getUid()).collection("aviones")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaAviones.clear(); // Limpiamos la lista local antes de llenarla
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Avion avion = document.toObject(Avion.class);
                            avion.setDocumentId(document.getId()); // Guardamos el ID del documento en el objeto
                            listaAviones.add(avion);
                        }
                        adapter.notifyDataSetChanged(); // Notificamos al adaptador que los datos han cambiado
                        actualizarVistaVacia(); // Comprobamos si la lista está vacía
                    } else {
                        Log.w("Firestore", "Error al cargar documentos.", task.getException());
                        Toast.makeText(MainActivity.this, "Error al cargar los aviones.", Toast.LENGTH_SHORT).show();
                    }
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
