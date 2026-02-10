package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.content.Context;
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

public class AddAvionActivity extends AppCompatActivity {

    private Spinner spinnerAviones;
    private ImageView imgPreviewAvion;
    private Button btnSubirFoto, btnGuardar, btnCancelar;

    private Uri uriImagenFinal;
    private Uri uriFotoCamaraTemporal;

    private List<Avion> listaAvionesBase;

    // 🔴 NUEVO
    private String icaoSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_avion);

        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // 🔴 LEER ICAO DESDE MAPA
        SharedPreferences prefs =
                getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        icaoSeleccionado = prefs.getString("icao_seleccionado", null);

        if (icaoSeleccionado == null) {
            Toast.makeText(this,
                    "No has seleccionado ningún avión en el mapa",
                    Toast.LENGTH_LONG).show();
        }

        cargarDatosGenerator();

        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        List<String> nombres = new ArrayList<>();

        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")");
        }

        spinnerAviones.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                )
        );
    }

    private void guardarAvionCatalogo() {

        if (icaoSeleccionado == null) {
            Toast.makeText(this,
                    "Primero selecciona un avión en el mapa",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerAviones.getSelectedItemPosition();
        if (pos < 0) return;

        Avion base = listaAvionesBase.get(pos);

        // 🔴 ASOCIAMOS EL ICAO24 REAL
        Avion nuevo = new Avion(
                base.getModelo(),
                base.getFabricante(),
                base.getRareza(),
                base.getImagenResId(),
                base.getVelocidad(),
                base.getPasajeros(),
                base.getDimensiones(),
                base.getPais(),
                base.getPeso(),
                icaoSeleccionado
        );

        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
        }

        guardarEnPreferencias(nuevo);
        guardarEnFirestore(nuevo);

        // 🔴 LIMPIAR ICAO USADO
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .remove("icao_seleccionado")
                .apply();

        Toast.makeText(this,
                "Avistamiento guardado correctamente",
                Toast.LENGTH_SHORT).show();

        finish();
    }

    private void guardarEnPreferencias(Avion nuevo) {
        SharedPreferences prefs =
                getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String json = prefs.getString("lista_aviones", null);
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();

        List<Avion> lista = json != null
                ? gson.fromJson(json, type)
                : new ArrayList<>();

        if (lista == null) lista = new ArrayList<>();

        lista.add(nuevo);
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }

    private void guardarEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(auth.getCurrentUser().getUid())
                .collection("aviones")
                .document(avion.getId())
                .set(avion)
                .addOnSuccessListener(v ->
                        Log.d("FIRESTORE", "Avión guardado"))
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE", "Error", e));
    }

    // -------------------------------
    // CÁMARA / GALERÍA (SIN CAMBIOS)
    // -------------------------------

    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Cámara", "Galería"};

        new AlertDialog.Builder(this)
                .setTitle("Añadir foto")
                .setItems(opciones, (d, i) -> {
                    if (i == 0) abrirCamara();
                    else launcherGaleria.launch("image/*");
                })
                .show();
    }

    private final ActivityResultLauncher<String> launcherGaleria =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) cargarImagen(uri);
                    });

    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    ok -> {
                        if (ok && uriFotoCamaraTemporal != null) {
                            cargarImagen(uriFotoCamaraTemporal);
                        }
                    });

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) return;

        try {
            File f = crearArchivoImagen();
            uriFotoCamaraTemporal = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", f);
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String ts = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());

        File dir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);

        return File.createTempFile("IMG_" + ts, ".jpg", dir);
    }

    private void cargarImagen(Uri uri) {
        uriImagenFinal = uri;
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imgPreviewAvion);
    }
}
