package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

import es.medac.skycollectorapp.R;

public class PerfilActivity extends AppCompatActivity {

    private ImageView imgPerfil;
    private EditText etNombre;
    private Uri uriFotoSeleccionada;
    private Uri uriFotoCamaraTemporal;
    private String userId;

    // 1. LANZADOR PARA GALERÍA
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uriFotoSeleccionada = uri;
                    Glide.with(this).load(uri).circleCrop().into(imgPerfil);
                }
            }
    );

    // 2. LANZADOR PARA CÁMARA
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito) {
                    uriFotoSeleccionada = uriFotoCamaraTemporal;
                    Glide.with(this).load(uriFotoSeleccionada).circleCrop().into(imgPerfil);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            cerrarSesion();
            return;
        }
        userId = user.getUid();
        String email = user.getEmail();

        // Vincular vistas
        imgPerfil = findViewById(R.id.imgPerfilGrande);
        etNombre = findViewById(R.id.etNombreUsuario);
        TextView txtEmail = findViewById(R.id.txtEmailFijo);
        Button btnFoto = findViewById(R.id.btnCambiarFoto);
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);
        Button btnLogout = findViewById(R.id.btnLogout);

        txtEmail.setText(email);
        cargarDatos();

        // --- CORRECCIÓN AQUÍ ---
        // Antes tenías launcherGaleria.launch(), ahora llamamos al diálogo
        btnFoto.setOnClickListener(v -> mostrarDialogoSeleccion());

        btnGuardar.setOnClickListener(v -> guardarDatos());
        btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    // --- MENÚ DE SELECCIÓN ---
    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Hacer foto con Cámara", "Elegir de Galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar foto de perfil");
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
        launcherGaleria.launch("image/*");
    }

    private void abrirCamara() {
        uriFotoCamaraTemporal = crearUriTemporal();
        if (uriFotoCamaraTemporal != null) {
            launcherCamara.launch(uriFotoCamaraTemporal);
        } else {
            Toast.makeText(this, "Error: No se pudo crear el archivo de foto", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri crearUriTemporal() {
        try {
            File cachePath = new File(getExternalCacheDir(), "mis_fotos");
            if (!cachePath.exists()) cachePath.mkdirs();

            File nuevoArchivo = new File(cachePath, "foto_" + System.currentTimeMillis() + ".jpg");

            // IMPORTANTE: El 'authority' debe coincidir con lo que pusiste en el Manifest
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", nuevoArchivo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- GUARDAR Y CARGAR ---
    private void guardarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("nombre_" + userId, etNombre.getText().toString());

        if (uriFotoSeleccionada != null) {
            editor.putString("foto_" + userId, uriFotoSeleccionada.toString());
        }

        editor.apply();
        Toast.makeText(this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void cargarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        String nombreGuardado = prefs.getString("nombre_" + userId, "");
        etNombre.setText(nombreGuardado);

        String fotoGuardada = prefs.getString("foto_" + userId, null);
        if (fotoGuardada != null) {
            Glide.with(this).load(fotoGuardada).circleCrop().into(imgPerfil);
        }
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}