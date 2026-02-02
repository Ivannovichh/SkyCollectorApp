package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

public class AddAvionActivity extends AppCompatActivity {

    private Spinner spinnerAviones;
    private ImageView imgPreviewAvion;
    private Button btnSubirFoto, btnGuardar;

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
                    // Dar permiso persistente para leer la foto de la galería siempre
                    try {
                        getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) { e.printStackTrace(); }
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

    // Lanzador para pedir permiso de cámara si no lo tenemos
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

        // 1. Vincular vistas
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // 2. Cargar datos del generador (Lista desplegable)
        cargarDatosGenerator();

        // 3. Listeners
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        btnGuardar.setOnClickListener(v -> guardarAvionAutomatico());
    }

    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        List<String> nombres = new ArrayList<>();

        for (Avion a : listaAvionesBase) {
            // Mostramos "Modelo (Rareza)" en el desplegable
            nombres.add(a.getApodo() + " (" + a.getRareza() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres);
        spinnerAviones.setAdapter(adapter);
    }

    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Hacer Foto (Cámara)", "Elegir de Galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir foto del avistamiento");
        builder.setItems(opciones, (d, which) -> {
            if (which == 0) {
                // Comprobamos permiso antes de abrir cámara
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
            // IMPORTANTE: El authority debe coincidir con tu AndroidManifest
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (IOException e) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
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
        if (pos < 0 || listaAvionesBase.isEmpty()) return;

        Avion base = listaAvionesBase.get(pos);

        // CREAMOS EL NUEVO AVIÓN copiando los datos del catálogo.
        // Usamos el constructor que definimos en Avion.java
        Avion nuevo = new Avion(
                base.getApodo(),       // Modelo
                base.getFabricante(),
                base.getRareza(),
                base.getImagenResId(), // Foto oficial (icono)
                base.getVelocidad(),
                base.getPasajeros(),
                base.getDimensiones(),
                base.getPais(),
                base.getPeso()
        );

        // Añadimos la foto del usuario si la hizo
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
        }

        // GUARDAR EN SHAREDPREFERENCES
        guardarEnPreferencias(nuevo);

        Toast.makeText(this, "¡Avistamiento guardado!", Toast.LENGTH_SHORT).show();
        finish(); // Volver atrás
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
        }

        lista.add(nuevoAvion);
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }
}