package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    // Base de datos de aviones
    private List<Avion> listaAvionesBase;

    // --- LANZADORES ---
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> cargarImagen(uri)
    );

    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito) cargarImagen(uriFotoCamaraTemporal);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_avion);

        // 1. Vincular
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);

        // 2. Cargar datos del generador
        cargarDatosGenerator();

        // 3. Listeners
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        btnGuardar.setOnClickListener(v -> guardarAvionAutomatico());
    }

    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        List<String> nombres = new ArrayList<>();
        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres);
        spinnerAviones.setAdapter(adapter);
    }

    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Cámara", "Galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar foto");
        builder.setItems(opciones, (d, which) -> {
            if (which == 0) abrirCamara();
            else launcherGaleria.launch("image/*");
        });
        builder.show();
    }

    private void abrirCamara() {
        try {
            File cachePath = new File(getExternalCacheDir(), "mis_fotos");
            if (!cachePath.exists()) cachePath.mkdirs();
            File archivo = new File(cachePath, "foto_" + System.currentTimeMillis() + ".jpg");

            // IMPORTANTE: Esto debe coincidir con el Manifest
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarImagen(Uri uri) {
        if (uri != null) {
            uriImagenFinal = uri;
            Glide.with(this).load(uri).centerCrop().into(imgPreviewAvion);
        }
    }

    private void guardarAvionAutomatico() {
        int pos = spinnerAviones.getSelectedItemPosition();
        Avion base = listaAvionesBase.get(pos);

        // Clonamos el avión base con todos sus datos técnicos
        Avion nuevo = new Avion(
                base.getModelo(),
                base.getFabricante(),
                base.getRareza(),
                base.getImagenResId(),
                base.getVelocidad(),
                base.getPasajeros(),
                base.getDimensiones(),
                base.getPais(),
                base.getPeso()
        );

        // Añadimos datos únicos
        nuevo.setMatricula(generarMatricula());
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
        }

        // GUARDAR EN SHAREDPREFERENCES (Esto arregla que "no se guarden")
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("lista_aviones", null);
        List<Avion> lista;

        if (json == null) lista = new ArrayList<>();
        else {
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            lista = gson.fromJson(json, type);
        }

        lista.add(nuevo);
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();

        Toast.makeText(this, "Avión guardado correctamente", Toast.LENGTH_SHORT).show();
        finish(); // Volver al mapa/main
    }

    private String generarMatricula() {
        Random r = new Random();
        return "EC-" + (char)(r.nextInt(26)+'A') + (char)(r.nextInt(26)+'A') + (char)(r.nextInt(26)+'A');
    }
}