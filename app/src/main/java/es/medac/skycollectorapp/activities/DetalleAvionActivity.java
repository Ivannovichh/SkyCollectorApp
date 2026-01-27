package es.medac.skycollectorapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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
    private String nuevaRutaFoto = null; // Variable temporal para guardar la nueva foto

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

        // --- 1. CONFIGURAR EL BOTÓN DE LA CÁMARA (GALERÍA) ---
        ActivityResultLauncher<String> selectorFotos = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // IMPORTANTE: Copiamos la imagen a la memoria interna
                        nuevaRutaFoto = guardarImagenEnInterno(uri);

                        if (nuevaRutaFoto != null) {
                            // Actualizamos la vista previa inmediatamente
                            Glide.with(this).load(nuevaRutaFoto).centerCrop().into(binding.imgFotoUsuario);
                            binding.layoutFotoUsuario.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Listener: Al pulsar el icono de cámara -> Abrir galería
        binding.btnEditarFoto.setOnClickListener(v -> selectorFotos.launch("image/*"));


        // --- 2. CONFIGURAR EL ZOOM (AL TOCAR LA FOTO) ---
        binding.imgFotoUsuario.setOnClickListener(v -> mostrarDialogoZoom());


        // --- 3. CONFIGURAR EL BOTÓN DE GUARDAR Y SALIR ---
        binding.btnVolver.setOnClickListener(v -> {
            guardarCambiosYSalir();
        });
    }

    private void cargarDatosEnPantalla() {
        // Ponemos el APODO en el campo editable (si no tiene, pone el modelo)
        binding.etNombreAvion.setText(avionActual.getApodo());

        // Textos técnicos (Fijos)
        binding.txtDetalleFabricante.setText("Fabricante: " + avionActual.getFabricante());
        binding.txtDetallePais.setText("País: " + avionActual.getPaisOrigen());
        binding.txtDetalleVelocidad.setText("Velocidad: " + avionActual.getVelocidadMax());
        binding.txtDetalleCapacidad.setText("Capacidad: " + avionActual.getCapacidad());
        binding.txtDetallePeso.setText("Peso: " + avionActual.getPesoMax());
        binding.txtDetalleDimensiones.setText("Dimensiones: " + avionActual.getDimensiones());

        // Cargar Foto Oficial (Arriba)
        Glide.with(this).load(avionActual.getImagenResId()).fitCenter().into(binding.imgDetalleGrande);

        // Cargar Foto Usuario (Abajo)
        if (avionActual.getUriFotoUsuario() != null) {
            binding.layoutFotoUsuario.setVisibility(View.VISIBLE);
            Glide.with(this).load(avionActual.getUriFotoUsuario()).centerCrop().into(binding.imgFotoUsuario);
        } else {
            // Si no hay foto, dejamos el hueco visible pero vacío para que se vea el botón de cámara
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

        // 3. Preparar la maleta de vuelta
        Intent resultIntent = new Intent();
        // IMPORTANTE: La clave "avion_modificado" debe coincidir con la de MainActivity
        resultIntent.putExtra("avion_modificado", avionActual);

        resultIntent.putExtra("avion_document_id", getIntent().getStringExtra("avion_document_id"));

        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    // --- DIÁLOGO FLOTANTE PARA EL ZOOM ---
    private void mostrarDialogoZoom() {
        // Si no hay foto cargada, no hacemos nada
        if (avionActual.getUriFotoUsuario() == null && nuevaRutaFoto == null) return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        com.github.chrisbanes.photoview.PhotoView visor = new com.github.chrisbanes.photoview.PhotoView(this);

        // Decidimos qué foto mostrar (la nueva o la antigua)
        String ruta = (nuevaRutaFoto != null) ? nuevaRutaFoto : avionActual.getUriFotoUsuario();
        Glide.with(this).load(ruta).into(visor);

        dialog.setContentView(visor);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

    // --- FUNCIÓN CLAVE: Copia la imagen a la carpeta privada de la app ---
    private String guardarImagenEnInterno(Uri uriOrigen) {
        try {
            InputStream in = getContentResolver().openInputStream(uriOrigen);
            if (in == null) return null;

            // Creamos un nombre único
            String nombre = "img_" + System.currentTimeMillis() + ".jpg";
            File archivo = new File(getFilesDir(), nombre);

            OutputStream out = new FileOutputStream(archivo);
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, leidos);
            }
            out.close();
            in.close();

            return archivo.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}