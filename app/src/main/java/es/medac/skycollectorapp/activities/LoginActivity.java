package es.medac.skycollectorapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.medac.skycollectorapp.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth; // La llave maestra de Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();

        // 2. Vincular controles
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegistro = findViewById(R.id.btnRegistro);

        // 3. Lógica del Botón INICIAR SESIÓN (El Cyan)
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // .trim() quita espacios accidentales
            String pass = etPassword.getText().toString().trim();

            if (validarCampos(email, pass)) {
                loginUsuario(email, pass);
            }
        });

        // 4. Lógica del Botón REGISTRARSE (El Azul)
        btnRegistro.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (validarCampos(email, pass)) {
                crearUsuario(email, pass);
            }
        });
    }

    // --- COMPROBACIÓN AUTOMÁTICA AL ABRIR APP ---
    @Override
    public void onStart() {
        super.onStart();
        // Si ya estabas logueado de antes, entra directo sin pedir contraseña
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            irAMain();
        }
    }

    // --- FUNCIÓN PARA ENTRAR ---
    private void loginUsuario(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show();
                        irAMain();
                    } else {
                        // Si falla (contraseña mal, etc.)
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- FUNCIÓN PARA REGISTRAR ---
    private void crearUsuario(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "¡Cuenta creada! Entrando...", Toast.LENGTH_SHORT).show();
                        irAMain();
                    } else {
                        // Si falla (correo ya existe, contraseña corta, etc.)
                        Toast.makeText(LoginActivity.this, "Fallo al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // --- AYUDANTES ---
    private boolean validarCampos(String email, String pass) {
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena correo y contraseña", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Cerramos Login para que al dar 'Atrás' no vuelva aquí
    }
}