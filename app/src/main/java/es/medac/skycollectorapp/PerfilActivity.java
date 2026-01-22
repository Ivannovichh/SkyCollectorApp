package es.medac.skycollectorapp;

import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilActivity extends AppCompatActivity {

    private ImageView imgPerfil;
    private EditText etNombre;
    private Uri uriFotoSeleccionada;
    private String userId;

    // ESTO ABRE LA GALERÍA DEL MÓVIL
    private final ActivityResultLauncher<String> selectorFotos = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Cuando eliges una foto, la guardamos temporalmente en la variable y la mostramos
                    uriFotoSeleccionada = uri;
                    Glide.with(this).load(uri).circleCrop().into(imgPerfil);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // 1. Obtener usuario actual (para usar su ID como clave de guardado)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish(); // Si no hay usuario, cerramos
            return;
        }
        userId = user.getUid();
        String email = user.getEmail();

        // 2. Vincular vistas con el XML
        imgPerfil = findViewById(R.id.imgPerfilGrande);
        etNombre = findViewById(R.id.etNombreUsuario);
        TextView txtEmail = findViewById(R.id.txtEmailFijo);
        Button btnFoto = findViewById(R.id.btnCambiarFoto);
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);

        // 3. Poner el email (fijo)
        txtEmail.setText(email);

        // 4. Cargar datos guardados anteriormente
        cargarDatos();

        // 5. Botón para cambiar foto
        btnFoto.setOnClickListener(v -> selectorFotos.launch("image/*"));

        // 6. Botón para guardar cambios
        btnGuardar.setOnClickListener(v -> guardarDatos());
    }

    private void guardarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Guardamos el nombre
        editor.putString("nombre_" + userId, etNombre.getText().toString());

        // Si ha elegido foto nueva, guardamos la ruta
        if (uriFotoSeleccionada != null) {
            editor.putString("foto_" + userId, uriFotoSeleccionada.toString());
        }

        editor.apply(); // Confirmar guardado

        Toast.makeText(this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show();
        finish(); // Volver al menú principal
    }

    private void cargarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);

        // Recuperar nombre
        String nombreGuardado = prefs.getString("nombre_" + userId, "");
        etNombre.setText(nombreGuardado);

        // Recuperar foto
        String fotoGuardada = prefs.getString("foto_" + userId, null);
        if (fotoGuardada != null) {
            // Usamos Glide para que se vea redonda
            Glide.with(this).load(fotoGuardada).circleCrop().into(imgPerfil);
        }
    }
}