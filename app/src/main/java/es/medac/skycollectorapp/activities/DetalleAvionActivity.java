package es.medac.skycollectorapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.databinding.ActivityDetalleAvionBinding;

public class DetalleAvionActivity extends AppCompatActivity {

    private ActivityDetalleAvionBinding binding;
    private Avion avionActual;
    private String nuevaRutaFoto = null; // Ruta final del archivo guardado en memoria interna
    private Uri uriFotoCamaraTemporal;   // URI temporal solo para hacer la foto

    // --- 1. LANZADOR GALERÍA ---
    private final ActivityResultLauncher<String> selectorGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    procesarYGuardarFoto(uri);
                }
            }
    );

    // --- 2. LANZADOR CÁMARA ---
    private final ActivityResultLauncher<Uri> selectorCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito && uriFotoCamaraTemporal != null) {
                    // Si la foto se hizo bien, la procesamos igual que la de galería
                    // para guardarla en la carpeta privada de la app
                    procesarYGuardarFoto(uriFotoCamaraTemporal);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetalleAvionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Recibimos el avión
        avionActual = (Avion) getIntent().getSerializableExtra("avion_extra");

        if (avionActual != null) {
            cargarDatosEnPantalla();
        }

        // --- BOTÓN EDITAR FOTO: AHORA ABRE EL MENÚ ---
        binding.btnEditarFoto.setOnClickListener(v -> mostrarDialogoSeleccion());


        // --- ZOOM (AL TOCAR LA FOTO) ---
        binding.imgFotoUsuario.setOnClickListener(v -> mostrarDialogoZoom());


        // --- BOTÓN GUARDAR Y SALIR ---
        binding.btnVolver.setOnClickListener(v -> guardarCambiosYSalir());
    }

    // --- MENÚ DE SELECCIÓN ---
    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Hacer foto con Cámara", "Elegir de Galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar foto");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                abrirCamara();
            } else {
                abrirGaleria();
            }
        });
        builder.show();
    }

    private void abrirGaleria() {
        selectorGaleria.launch("image/*");
    }

    private void abrirCamara() {
        uriFotoCamaraTemporal = crearUriTemporal();
        if (uriFotoCamaraTemporal != null) {
            selectorCamara.launch(uriFotoCamaraTemporal);
        } else {
            Toast.makeText(this, "No se pudo iniciar la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    // Método común para procesar la foto (venga de cámara o galería) y mostrarla
    private void procesarYGuardarFoto(Uri uriOrigen) {
        // Usamos tu método para copiar la imagen a la memoria interna de la app
        nuevaRutaFoto = guardarImagenEnInterno(uriOrigen);

        if (nuevaRutaFoto != null) {
            // Actualizamos la vista previa inmediatamente
            Glide.with(this).load(nuevaRutaFoto).centerCrop().into(binding.imgFotoUsuario);
            binding.layoutFotoUsuario.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarDatosEnPantalla() {
        binding.etNombreAvion.setText(avionActual.getApodo());

        binding.txtDetalleFabricante.setText("Fabricante: " + avionActual.getFabricante());
        binding.txtDetallePais.setText("País: " + avionActual.getPaisOrigen());
        binding.txtDetalleVelocidad.setText("Velocidad: " + avionActual.getVelocidadMax());
        binding.txtDetalleCapacidad.setText("Capacidad: " + avionActual.getCapacidad());
        binding.txtDetallePeso.setText("Peso: " + avionActual.getPesoMax());
        binding.txtDetalleDimensiones.setText("Dimensiones: " + avionActual.getDimensiones());

        // Foto Oficial
        Glide.with(this)
                .load(avionActual.getImagenResId())
                .fitCenter()
                .into(binding.imgDetalleGrande);

        // Foto Usuario
        if (avionActual.getUriFotoUsuario() != null) {
            binding.layoutFotoUsuario.setVisibility(View.VISIBLE);
            Glide.with(this).load(avionActual.getUriFotoUsuario()).centerCrop().into(binding.imgFotoUsuario);
        } else {
            binding.layoutFotoUsuario.setVisibility(View.VISIBLE);
            binding.imgFotoUsuario.setImageDrawable(null);
        }
    }

    private void guardarCambiosYSalir() {
        // 1. Guardar apodo
        avionActual.setApodo(binding.etNombreAvion.getText().toString());

        // 2. Guardar foto (si cambió)
        if (nuevaRutaFoto != null) {
            avionActual.setUriFotoUsuario(nuevaRutaFoto);
        }

        // 3. Devolver resultado
        Intent resultIntent = new Intent();
        resultIntent.putExtra("avion_modificado", avionActual);
        resultIntent.putExtra("avion_document_id", getIntent().getStringExtra("avion_document_id"));

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void mostrarDialogoZoom() {
        if (avionActual.getUriFotoUsuario() == null && nuevaRutaFoto == null) return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        com.github.chrisbanes.photoview.PhotoView visor = new com.github.chrisbanes.photoview.PhotoView(this);

        String ruta = (nuevaRutaFoto != null) ? nuevaRutaFoto : avionActual.getUriFotoUsuario();
        Glide.with(this).load(ruta).into(visor);

        dialog.setContentView(visor);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

    // --- UTILIDADES ---

    // Crea un archivo vacío en caché para que la cámara escriba ahí
    private Uri crearUriTemporal() {
        try {
            File cachePath = new File(getExternalCacheDir(), "mis_fotos_aviones");
            if (!cachePath.exists()) cachePath.mkdirs();

            // Nombre temporal
            File nuevoArchivo = new File(cachePath, "temp_cam_" + System.currentTimeMillis() + ".jpg");

            // IMPORTANTE: Debe coincidir con el provider en AndroidManifest
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", nuevoArchivo);
        } catch (Exception e) {
            return null;
        }
    }

    // Tu función original: Copia cualquier URI (Cámara o Galería) a un archivo privado definitivo
    private String guardarImagenEnInterno(Uri uriOrigen) {
        try {
            InputStream in = getContentResolver().openInputStream(uriOrigen);
            if (in == null) return null;

            String nombre = "img_final_" + System.currentTimeMillis() + ".jpg";
            File archivo = new File(getFilesDir(), nombre);

            OutputStream out = new FileOutputStream(archivo);
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, leidos);
            }
            out.close();
            in.close();

            return archivo.getAbsolutePath(); // Devolvemos la ruta del archivo final
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}