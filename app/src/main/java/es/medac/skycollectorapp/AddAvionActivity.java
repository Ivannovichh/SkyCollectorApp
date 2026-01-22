package es.medac.skycollectorapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
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
import java.util.ArrayList;
import java.util.List;

import es.medac.skycollectorapp.databinding.ActivityAddAvionBinding;

public class AddAvionActivity extends AppCompatActivity {

    private ActivityAddAvionBinding binding;
    private List<Avion> listaCompleta;
    // Esta variable ahora guardará la RUTA FINAL del archivo copiado
    private String rutaImagenFinal = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddAvionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. CARGAR SPINNER
        listaCompleta = AvionGenerator.getTodosLosAviones();
        List<String> nombres = new ArrayList<>();
        for (Avion a : listaCompleta) {
            nombres.add(a.getModelo());
        }
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres);
        binding.spinnerAviones.setAdapter(adapterSpinner);

        // 2. CONFIGURAR SELECTOR DE GALERÍA
        ActivityResultLauncher<String> selectorFotos = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        // --- CAMBIO CLAVE AQUÍ ---
                        // No guardamos la URI temporal.
                        // Inmediatamente COPIAMOS el archivo a la memoria interna de la app.
                        rutaImagenFinal = guardarImagenEnInterno(uri);

                        if (rutaImagenFinal != null) {
                            // Mostramos la previsualización usando el archivo copiado
                            Glide.with(this).load(rutaImagenFinal).fitCenter().into(binding.imgPreview);
                        } else {
                            Toast.makeText(this, "Error al copiar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        binding.btnCargarFoto.setOnClickListener(v -> selectorFotos.launch("image/*"));

        // 3. GUARDAR Y VOLVER
        binding.btnGuardar.setOnClickListener(v -> {
            int pos = binding.spinnerAviones.getSelectedItemPosition();
            Avion avionElegido = listaCompleta.get(pos);

            // Si se copió una imagen correctamente, guardamos su ruta
            if (rutaImagenFinal != null) {
                avionElegido.setUriFotoUsuario(rutaImagenFinal);
            }

            Intent returnIntent = new Intent();
            returnIntent.putExtra("nuevo_avion", avionElegido);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });
    }

    /**
     * FUNCIÓN AYUDANTE: Coge una URI de la galería y copia el archivo
     * físicamente a la carpeta privada de la aplicación.
     * @return La ruta absoluta del nuevo archivo copiado.
     */
    private String guardarImagenEnInterno(Uri uriOrigen) {
        try {
            // 1. Abrir el grifo para leer el archivo de origen
            InputStream inputStream = getContentResolver().openInputStream(uriOrigen);
            if (inputStream == null) return null;

            // 2. Crear un archivo vacío en la carpeta privada de la app
            // Le damos un nombre único usando el tiempo actual para que no se sobrescriban
            String nombreArchivo = "avistamiento_" + System.currentTimeMillis() + ".jpg";
            File archivoDestino = new File(getFilesDir(), nombreArchivo);

            // 3. Abrir el grifo para escribir en el archivo destino
            OutputStream outputStream = new FileOutputStream(archivoDestino);

            // 4. Copiar los datos (el "agua") de un cubo a otro
            byte[] buffer = new byte[4096]; // Un cubo para llevar datos
            int bytesLeidos;
            while ((bytesLeidos = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesLeidos);
            }

            // 5. Cerrar los grifos
            outputStream.close();
            inputStream.close();

            // 6. Devolver la dirección de la nueva casa del archivo
            return archivoDestino.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null; // Si falla algo
        }
    }
}