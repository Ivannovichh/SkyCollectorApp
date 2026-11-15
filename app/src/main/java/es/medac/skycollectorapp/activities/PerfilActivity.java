// Declaración del paquete donde se ubica la actividad de perfil de usuario
package es.medac.skycollectorapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.utils.ImagenBase64Utils;
import es.medac.skycollectorapp.utils.RankingManager;

// Actividad que permite editar el nombre público, avatar público y datos privados de contacto del piloto
public class PerfilActivity extends AppCompatActivity {

    private ImageView imgFotoPerfil;
    private EditText etNombrePerfil;
    private EditText etCorreoPerfil;
    private EditText etTelefonoPerfil;
    private String fotoBase64Pendiente = null;

    private final ActivityResultLauncher<String> selectorImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (uri != null && user != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                    }

                    SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
                    prefs.edit().putString("foto_" + user.getUid(), uri.toString()).apply();

                    // Comprime el avatar a 240px para publicarlo en el ranking de Firestore
                    fotoBase64Pendiente = ImagenBase64Utils.uriABase64Comprimida(this, uri, 240, 75);
                    if (fotoBase64Pendiente != null) {
                        prefs.edit().putString("foto_publica_" + user.getUid(), fotoBase64Pendiente).apply();
                        RankingManager.actualizarIdentidadPublica(null, fotoBase64Pendiente);
                    }

                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(imgFotoPerfil);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        ImageButton btnVolver = findViewById(R.id.btnVolverPerfil);
        CardView cardFoto = findViewById(R.id.cardFotoPerfil);
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil);
        etNombrePerfil = findViewById(R.id.etNombrePerfil);
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil);
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil);
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        cargarDatosPerfil(user);

        btnVolver.setOnClickListener(v -> finish());
        cardFoto.setOnClickListener(v -> selectorImagen.launch("image/*"));
        btnGuardar.setOnClickListener(v -> guardarCambiosPerfil(user));
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void cargarDatosPerfil(FirebaseUser user) {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String uid = user.getUid();

        String nombre = prefs.getString("nombre_" + uid, "");
        String correo = prefs.getString("correo_" + uid, user.getEmail() != null ? user.getEmail() : "");
        String telefono = prefs.getString("telefono_" + uid, user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        String foto = prefs.getString("foto_" + uid, null);
        fotoBase64Pendiente = prefs.getString("foto_publica_" + uid, null);

        etNombrePerfil.setText(nombre);
        etCorreoPerfil.setText(correo);
        etTelefonoPerfil.setText(telefono);

        if (foto != null) {
            Glide.with(this)
                    .load(Uri.parse(foto))
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(imgFotoPerfil);
        }
    }

    private void guardarCambiosPerfil(FirebaseUser user) {
        String nombre = etNombrePerfil.getText().toString().trim();
        String correo = etCorreoPerfil.getText().toString().trim();
        String telefono = etTelefonoPerfil.getText().toString().trim();

        if (nombre.length() < 3) {
            etNombrePerfil.setError(getString(R.string.error_nombre_corto));
            etNombrePerfil.requestFocus();
            return;
        }

        if (!correo.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoPerfil.setError(getString(R.string.error_correo_formato));
            etCorreoPerfil.requestFocus();
            return;
        }

        String uid = user.getUid();
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        prefs.edit()
                .putString("nombre_" + uid, nombre)
                .putString("correo_" + uid, correo)
                .putString("telefono_" + uid, telefono)
                .apply();

        // Sincroniza nombre y avatar público en la colección "ranking"
        RankingManager.actualizarIdentidadPublica(nombre, fotoBase64Pendiente);

        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombreUsuario", nombre);
        datosUsuario.put("correoContacto", correo);
        datosUsuario.put("telefonoContacto", telefono);

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .set(datosUsuario, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.toast_perfil_actualizado), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.toast_perfil_error_nube), Toast.LENGTH_SHORT).show()
                );
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}