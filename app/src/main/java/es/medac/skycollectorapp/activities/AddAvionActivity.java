package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.utils.AvionGenerator;
import retrofit2.http.POST;

public class AddAvionActivity extends AppCompatActivity {

    private Spinner spinnerAviones;
    private ImageView imgPreviewAvion;
    private Button btnSubirFoto, btnGuardar, btnCancelar;

    // Variables para la foto
    private Uri uriImagenFinal;
    private Uri uriFotoCamaraTemporal;

    // Base de datos de aviones (Catálogo)
    private List<Avion> listaAvionesBase;

    // --- LANZADORES ---
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cargarImagen(uri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito && uriFotoCamaraTemporal != null) {
                    cargarImagen(uriFotoCamaraTemporal);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) abrirCamara();
                else Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_avion);

        // 1) Vincular vistas
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar); // 👈 IMPORTANTE

        // 2) Cargar catálogo
        cargarDatosGenerator();

        // 3) Listeners
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        btnGuardar.setOnClickListener(v -> guardarAvionAutomatico());

        // CANCELAR -> volver a pantalla inicial (MainActivity)
        btnCancelar.setOnClickListener(v -> {
            Intent i = new Intent(AddAvionActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
    }

    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        List<String> nombres = new ArrayList<>();

        for (Avion a : listaAvionesBase) {
            nombres.add(a.getApodo() + " (" + a.getRareza() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nombres
        );

        spinnerAviones.setAdapter(adapter);
    }

    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Hacer Foto (Cámara)", "Elegir de Galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir foto del avistamiento");
        builder.setItems(opciones, (d, which) -> {
            if (which == 0) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            } else {
                launcherGaleria.launch("image/*");
            }
        });
        builder.show();
    }

    private void abrirCamara() {
        try {
            File archivo = crearArchivoImagen();
            uriFotoCamaraTemporal = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            );
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            Toast.makeText(this, "Error cámara: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void cargarImagen(Uri uri) {
        if (uri != null) {
            uriImagenFinal = uri;

            Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imgPreviewAvion);
        }
    }

    private void guardarAvionAutomatico() {
        int pos = spinnerAviones.getSelectedItemPosition();
        if (pos < 0 || listaAvionesBase == null || listaAvionesBase.isEmpty()) return;

        Avion base = listaAvionesBase.get(pos);

        Avion nuevo = new Avion(
                base.getApodo(),
                base.getFabricante(),
                base.getRareza(),
                base.getImagenResId(),
                base.getVelocidad(),
                base.getPasajeros(),
                base.getDimensiones(),
                base.getPais(),
                base.getPeso(),
                base.getIcao24()
        );

        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
        }

        // 💾 LOCAL
        guardarEnPreferencias(nuevo);

        // ☁️ FIRESTORE
        guardarAvionEnFirestore(nuevo);

        Toast.makeText(this, "¡Avistamiento guardado!", Toast.LENGTH_SHORT).show();

        Intent i = new Intent(AddAvionActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }


    private void guardarEnPreferencias(Avion nuevoAvion) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String json = prefs.getString("lista_aviones", null);
        List<Avion> lista;

        if (json == null) {
            lista = new ArrayList<>();
        } else {
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            lista = gson.fromJson(json, type);
            if (lista == null) lista = new ArrayList<>();
        }

        lista.add(nuevoAvion);
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }

    public void guardarAvionEnFirestore(Avion avion) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avion.getId()) // 🔑 ID estable
                .set(avion)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIRESTORE", "Avión guardado: " + avion.getApodo());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al guardar avión", e);
                });
    }
}
