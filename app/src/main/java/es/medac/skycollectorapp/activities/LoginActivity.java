// Declaración del paquete donde se ubica la actividad de autenticación
package es.medac.skycollectorapp.activities;

// Importaciones de Android necesarias
import android.content.Intent; // Navegación entre actividades
import android.os.Bundle; // Manejo del estado de la actividad
import android.util.Patterns; // Utilidad para validación de patrones de expresiones regulares (emails, URLs)
import android.widget.Button; // Componente de interfaz tipo botón
import android.widget.EditText; // Componente de campo de texto editable
import android.widget.Toast; // Mensajes emergentes cortos
import androidx.appcompat.app.AppCompatActivity; // Clase base para compatibilidad

// Importaciones de servicios de Firebase
import com.google.firebase.auth.FirebaseAuth; // Autenticación en la nube de Firebase
import com.google.firebase.auth.FirebaseUser; // Objeto que representa al usuario actual

// Importaciones del proyecto
import es.medac.skycollectorapp.R;

// Clase principal que gestiona el acceso y registro de nuevos usuarios
public class LoginActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ Y AUTH
    // ------------------------------
    private EditText etEmail, etPassword; // Campos de entrada para correo y clave
    private FirebaseAuth mAuth; // Instancia principal del servicio de autenticación

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método de creación de la actividad
        super.onCreate(savedInstanceState); // Llamada al método de la clase padre
        setContentView(R.layout.activity_login); // Asignar el diseño XML de login

        // Inicializar la instancia de autenticación de Firebase
        mAuth = FirebaseAuth.getInstance();

        // Localizar las referencias de los componentes en el layout XML
        etEmail = findViewById(R.id.etEmail); // Campo del correo
        etPassword = findViewById(R.id.etPassword); // Campo de la contraseña
        Button btnLogin = findViewById(R.id.btnLogin); // Botón para iniciar sesión
        Button btnRegistro = findViewById(R.id.btnRegistro); // Botón para registrarse

        // Configuración del escuchador de clic para el botón de inicio de sesión
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Limpiar espacios extras del correo
            String pass = etPassword.getText().toString().trim(); // Limpiar espacios extras de la clave

            if (validarCampos(email, pass)) { // Validar los datos antes de hacer la petición
                loginUsuario(email, pass); // Proceder con la autenticación
            }
        });

        // Configuración del escuchador de clic para el botón de registro
        btnRegistro.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim(); // Limpiar espacios del correo
            String pass = etPassword.getText().toString().trim(); // Limpiar espacios de la clave

            if (validarCampos(email, pass)) { // Validar requisitos mínimos
                crearUsuario(email, pass); // Proceder con la creación del nuevo perfil
            }
        });
    }

    @Override
    public void onStart() { // Método llamado al poner la actividad en primer plano
        super.onStart();
        // Comprobar si ya existe una sesión abierta previamente
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            irAMain(); // Redirigir directamente a la pantalla principal sin pedir credenciales
        }
    }

    // ------------------------------
    // LÓGICA DE AUTENTICACIÓN
    // ------------------------------

    // Intenta iniciar sesión con correo y contraseña en Firebase
    private void loginUsuario(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) { // Acceso correcto
                        Toast.makeText(LoginActivity.this, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show();
                        irAMain(); // Navegar a la pantalla principal
                    } else { // Acceso denegado
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Registra un nuevo usuario en la plataforma Firebase Auth
    private void crearUsuario(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) { // Registro completado
                        Toast.makeText(LoginActivity.this, "¡Cuenta creada! Entrando...", Toast.LENGTH_SHORT).show();
                        irAMain(); // Entrar directamente a la app
                    } else { // Fallo durante el registro
                        Toast.makeText(LoginActivity.this, "Fallo al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Comprueba que los datos introducidos no estén vacíos y cumplan con el formato formal de correo y seguridad
    private boolean validarCampos(String email, String pass) {
        // Verificar que no haya campos vacíos
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena correo y contraseña", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar que la cadena tenga una estructura de correo electrónico válida (ejemplo@dominio.com)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, introduce un correo electrónico válido", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Comprobar longitud mínima requerida por Firebase para la clave
        if (pass.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true; // Todos los filtros superados
    }

    // Realiza la transición a la actividad principal
    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class); // Definir la intención de cambio de pantalla
        startActivity(intent); // Iniciar MainActivity
        finish(); // Finalizar LoginActivity para sacarla de la pila de navegación
    }
}