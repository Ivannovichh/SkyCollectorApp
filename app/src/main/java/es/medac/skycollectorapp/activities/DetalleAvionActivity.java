package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.content.Intent;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;

public class DetalleAvionActivity extends AppCompatActivity {

    private ImageView imgDetalleGrande;
    private EditText etNombreAvion;
    private TextView txtFabricante, txtPais, txtVelocidad, txtCapacidad, txtPeso, txtDimensiones;
    private Button btnCamara, btnGaleria, btnGuardar;
    private CardView layoutFotoUsuario;
    private ImageView imgFotoUsuario;

    private Avion avion;
    private Uri uriFotoTemporal;

    // --- LANZADORES ---
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> { if (exito && uriFotoTemporal != null) mostrarFotoUsuario(uriFotoTemporal); }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> { if (isGranted) abrirCamara(); else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show(); }
    );

    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                    catch (Exception e) { e.printStackTrace(); }
                    mostrarFotoUsuario(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_avion);

        try {
            // 1. Inicializar Vistas
            initViews();

            // 2. Cargar Avión
            avion = (Avion) getIntent().getSerializableExtra("objeto_avion");
            if (avion != null) {
                cargarDatos();
            } else {
                Toast.makeText(this, "Error: No se recibió el avión", Toast.LENGTH_SHORT).show();
            }

            // 3. Configurar Listeners
            setupListeners();

        } catch (Exception e) {
            // Si algo falla, te mostrará el error en pantalla en vez de cerrarse
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initViews() {
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande);
        etNombreAvion = findViewById(R.id.etNombreAvion);

        txtFabricante = findViewById(R.id.txtDetalleFabricante);
        txtPais = findViewById(R.id.txtDetallePais);
        txtVelocidad = findViewById(R.id.txtDetalleVelocidad);
        txtCapacidad = findViewById(R.id.txtDetalleCapacidad);
        txtPeso = findViewById(R.id.txtDetallePeso);
        txtDimensiones = findViewById(R.id.txtDetalleDimensiones);

        btnCamara = findViewById(R.id.btnCamara);
        btnGaleria = findViewById(R.id.btnGaleria);
        btnGuardar = findViewById(R.id.btnVolver); // En tu XML se llama btnVolver

        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario);
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario);
    }

    private void setupListeners() {
        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> {
                if (avion != null) {
                    avion.setApodo(etNombreAvion.getText().toString());
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("avion_modificado", avion);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            });
        }

        if (btnCamara != null) {
            btnCamara.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }

        if (btnGaleria != null) {
            btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));
        }
    }

    private void cargarDatos() {
        etNombreAvion.setText(avion.getApodo());
        txtFabricante.setText("Fabricante: " + avion.getFabricante());
        txtPais.setText("País: " + avion.getPais());
        txtVelocidad.setText("Velocidad: " + avion.getVelocidad());
        txtCapacidad.setText("Capacidad: " + avion.getPasajeros());
        txtPeso.setText("Peso: " + avion.getPeso());
        txtDimensiones.setText("Dimensiones: " + avion.getDimensiones());

        // Foto Oficial
        Glide.with(this).load(avion.getImagenResId()).fitCenter().into(imgDetalleGrande);

        // Foto Usuario (Si existe)
        if (avion.getUriFotoUsuario() != null) {
            layoutFotoUsuario.setVisibility(View.VISIBLE);
            Glide.with(this).load(Uri.parse(avion.getUriFotoUsuario())).centerCrop().into(imgFotoUsuario);
        } else {
            layoutFotoUsuario.setVisibility(View.GONE);
        }
    }

    private void mostrarFotoUsuario(Uri uri) {
        avion.setUriFotoUsuario(uri.toString());
        layoutFotoUsuario.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).centerCrop().into(imgFotoUsuario);
    }

    private void abrirCamara() {
        try {
            File archivo = crearArchivoImagen();
            uriFotoTemporal = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", archivo);
            launcherCamara.launch(uriFotoTemporal);
        } catch (IOException e) {
            Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
    }
}