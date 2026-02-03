package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.adapters.AvionAdapter;
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.activities.AddAvionActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        listaAviones = new ArrayList<>();

        adapter = new AvionAdapter(listaAviones,
                (avion, position) -> {
                    Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
                    intent.putExtra("avion_id", avion.getId());
                    startActivity(intent);
                },
                this::actualizarPapelera
        );

        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.btnAddAvion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddAvionActivity.class))
        );

        binding.btnPapelera.setOnClickListener(v -> borrarSeleccionados());

        binding.cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class))
        );

        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class))
        );

        binding.btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapaActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarListaDeAviones();
        cargarFotoPerfilMini();
        cargarNombreUsuario();
    }

    private void cargarFotoPerfilMini() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String fotoGuardada = prefs.getString("foto_" + userId, null);

        if (fotoGuardada != null) {
            Glide.with(this)
                    .load(Uri.parse(fotoGuardada))
                    .circleCrop()
                    .into(binding.imgPerfilMini);
        }
    }

    private void cargarListaDeAviones() {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        String json = prefs.getString("lista_aviones", null);

        listaAviones.clear();

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            List<Avion> avionesGuardados = gson.fromJson(json, type);
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados);
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
        if (listaAviones.isEmpty()) return;

        // 1️⃣ Guardar los aviones seleccionados antes de borrarlos
        List<Avion> avionesABorrar = new ArrayList<>();
        for (Avion a : listaAviones) {
            if (a.isSeleccionado()) {
                avionesABorrar.add(a);
            }
        }

        if (avionesABorrar.isEmpty()) return; // No hay aviones seleccionados

        // 2️⃣ Borrar los aviones de Firestore en segundo plano
        for (Avion a : avionesABorrar) {
            borrarAvionEnFirestore(a);
        }

        // 3️⃣ Borrar los aviones de la lista local y notificar al adaptador (animación)
        for (int i = listaAviones.size() - 1; i >= 0; i--) { // recorrer al revés para evitar problemas de índices
            Avion a = listaAviones.get(i);
            if (a.isSeleccionado()) {
                listaAviones.remove(i);
                adapter.notifyItemRemoved(i); // animación de borrado
            }
        }

        // 4️⃣ Actualizar SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        editor.putString("lista_aviones", gson.toJson(listaAviones));
        editor.apply();

        // 5️⃣ Actualizar botón de papelera
        actualizarPapelera();

        // 6️⃣ Mostrar mensaje si la lista quedó vacía
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
        }

        // 7️⃣ Feedback al usuario
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

        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE);
    }
    public void borrarAvionEnFirestore(Avion avion) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avion.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIRESTORE", "Avión eliminado: " + avion.getApodo());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al borrar avión", e);
                });
    }
    private void cargarNombreUsuario() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        // Aquí usamos la misma clave que PerfilActivity
        String nombreUsuario = prefs.getString("nombre_" + userId, "usuario");
        binding.txtNombreUsuario.setText(nombreUsuario);
    }


}
