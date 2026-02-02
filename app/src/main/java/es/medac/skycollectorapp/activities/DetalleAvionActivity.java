package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import java.util.Locale;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;

public class DetalleAvionActivity extends AppCompatActivity {

    private ImageView imgDetalleGrande;
    private ImageView imgFotoUsuario;
    private EditText etNombreAvion;

    private TextView txtDetalleFabricante, txtDetallePais, txtDetalleVelocidad,
            txtDetalleCapacidad, txtDetallePeso, txtDetalleDimensiones;

    private Button btnCamara, btnGaleria;
    private Button btnGuardar, btnCancelar;

    private CardView layoutFotoUsuario;

    private Avion avion;
    private Uri uriFotoTemporal;

    private String avionId;

    // ==============================
    // LANZADORES
    // ==============================
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito && uriFotoTemporal != null) {
                    mostrarFotoUsuario(uriFotoTemporal);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) abrirCamara();
                else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
    );

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
                    mostrarFotoUsuario(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_avion);

        initViews();

        avionId = getIntent().getStringExtra("avion_id");

        if (avionId == null) {
            Toast.makeText(this, "Error: no se recibió el ID del avión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        avion = buscarAvionPorId(avionId);

        if (avion == null) {
            Toast.makeText(this, "Error: avión no encontrado en la lista", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarDatos();
        setupListeners();
    }

    private void initViews() {
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande);
        etNombreAvion = findViewById(R.id.etNombreAvion);

        txtDetalleFabricante = findViewById(R.id.txtDetalleFabricante);
        txtDetallePais = findViewById(R.id.txtDetallePais);
        txtDetalleVelocidad = findViewById(R.id.txtDetalleVelocidad);
        txtDetalleCapacidad = findViewById(R.id.txtDetalleCapacidad);
        txtDetallePeso = findViewById(R.id.txtDetallePeso);
        txtDetalleDimensiones = findViewById(R.id.txtDetalleDimensiones);

        btnCamara = findViewById(R.id.btnCamara);
        btnGaleria = findViewById(R.id.btnGaleria);

        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);

        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario);
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario);
    }

    private void setupListeners() {

        // CANCELAR
        btnCancelar.setOnClickListener(v -> finish());

        // GUARDAR
        btnGuardar.setOnClickListener(v -> {
            avion.setApodo(etNombreAvion.getText().toString().trim());

            guardarCambiosEnPreferencias(avion);

            Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
            finish();
        });

        // CAMARA
        btnCamara.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // GALERIA
        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));
    }

    private void cargarDatos() {
        etNombreAvion.setText(avion.getApodo());

        txtDetalleFabricante.setText("Fabricante: " + avion.getFabricante());
        txtDetallePais.setText("País: " + avion.getPais());
        txtDetalleVelocidad.setText("Velocidad: " + avion.getVelocidad());
        txtDetalleCapacidad.setText("Capacidad: " + avion.getPasajeros());
        txtDetallePeso.setText("Peso: " + avion.getPeso());
        txtDetalleDimensiones.setText("Dimensiones: " + avion.getDimensiones());

        Glide.with(this)
                .load(avion.getImagenResId())
                .fitCenter()
                .into(imgDetalleGrande);

        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            layoutFotoUsuario.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .centerCrop()
                    .into(imgFotoUsuario);
        } else {
            layoutFotoUsuario.setVisibility(View.GONE);
        }
    }

    private void mostrarFotoUsuario(Uri uri) {
        avion.setUriFotoUsuario(uri.toString());

        layoutFotoUsuario.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(imgFotoUsuario);
    }

    private void abrirCamara() {
        try {
            File archivo = crearArchivoImagen();

            uriFotoTemporal = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            );

            launcherCamara.launch(uriFotoTemporal);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Error cámara: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        );
    }

    private Avion buscarAvionPorId(String id) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        String json = prefs.getString("lista_aviones", null);
        if (json == null) return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return null;

        for (Avion a : lista) {
            if (a.getId() != null && a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    private void guardarCambiosEnPreferencias(Avion avionActualizado) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        Gson gson = new Gson();

        String json = prefs.getString("lista_aviones", null);
        if (json == null) return;

        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return;

        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(avionActualizado.getId())) {
                lista.set(i, avionActualizado);
                break;
            }
        }

        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }
}
