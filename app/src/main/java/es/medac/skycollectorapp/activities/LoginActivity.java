// Definición del paquete de la clase
package es.medac.skycollectorapp.activities;

// Importación para la gestión de navegación entre actividades
import android.content.Intent;
// Importación para el manejo de estados y datos de la actividad
import android.os.Bundle;
// Importación para el componente de botón de la interfaz
import android.widget.Button;
// Importación para el componente de campo de texto editable
import android.widget.EditText;
// Importación para la visualización de avisos rápidos en pantalla
import android.widget.Toast;
// Importación de la clase base para actividades con soporte de compatibilidad
import androidx.appcompat.app.AppCompatActivity;
// Importación para la gestión de servicios de identidad de Firebase
import com.google.firebase.auth.FirebaseAuth;
// Importación para la representación del perfil de usuario autenticado
import com.google.firebase.auth.FirebaseUser;

// Importación de los recursos generados del proyecto
import es.medac.skycollectorapp.R;

// Declaración de la clase que gestiona el acceso y registro de usuarios
public class LoginActivity extends AppCompatActivity {

    // Variable para la entrada de texto del correo electrónico
    private EditText etEmail, etPassword;
    // Variable para el motor de autenticación de la plataforma en la nube
    private FirebaseAuth mAuth;

    // Método que inicializa la actividad y configura la vista
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de creación de la clase padre
        super.onCreate(savedInstanceState);
        // Vinculación del archivo de diseño visual correspondiente
        setContentView(R.layout.activity_login);

        // Obtención de la instancia activa del servicio de seguridad
        mAuth = FirebaseAuth.getInstance();

        // Localización del campo de correo en la interfaz
        etEmail = findViewById(R.id.etEmail);
        // Localización del campo de contraseña en la interfaz
        etPassword = findViewById(R.id.etPassword);
        // Localización del botón de inicio de sesión
        Button btnLogin = findViewById(R.id.btnLogin);
        // Localización del botón de registro de nuevos usuarios
        Button btnRegistro = findViewById(R.id.btnRegistro);

        // Configuración de la respuesta al pulsar el botón de entrada
        btnLogin.setOnClickListener(v -> {
            // Captura y limpieza de espacios en blanco del correo
            String email = etEmail.getText().toString().trim();
            // Captura y limpieza de espacios en blanco de la clave
            String pass = etPassword.getText().toString().trim();

            // Verificación de que los datos introducidos sean correctos formalmente
            if (validarCampos(email, pass)) {
                // Invocación del proceso de validación en el servidor
                loginUsuario(email, pass);
            }
        });

        // Configuración de la respuesta al pulsar el botón de creación de cuenta
        btnRegistro.setOnClickListener(v -> {
            // Extracción del correo del componente de texto
            String email = etEmail.getText().toString().trim();
            // Extracción de la clave del componente de texto
            String pass = etPassword.getText().toString().trim();

            // Verificación de que los datos cumplen los requisitos mínimos
            if (validarCampos(email, pass)) {
                // Invocación del proceso de creación de usuario en el servidor
                crearUsuario(email, pass);
            }
        });
    }

    // Método que verifica el estado de la sesión al volver a la actividad
    @Override
    public void onStart() {
        // Ejecución del procedimiento de inicio de la clase padre
        super.onStart();
        // Consulta del usuario que se encuentra actualmente identificado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Comprobación de si existe una sesión activa persistente
        if(currentUser != null){
            // Salto automático a la pantalla principal si el usuario ya entró
            irAMain();
        }
    }

    // Método que gestiona el acceso con credenciales existentes
    private void loginUsuario(String email, String password) {
        // Intento de conexión mediante correo y clave en el servicio remoto
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Evaluación del resultado de la petición de acceso
                    if (task.isSuccessful()) {
                        // Notificación de éxito al usuario
                        Toast.makeText(LoginActivity.this, "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show();
                        // Redirección a la pantalla de inicio de la aplicación
                        irAMain();
                    } else {
                        // Notificación en caso de error en la identificación
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método que gestiona el alta de nuevos usuarios en el sistema
    private void crearUsuario(String email, String password) {
        // Solicitud de creación de nuevo perfil en el servidor de seguridad
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Evaluación del éxito de la creación de la cuenta
                    if (task.isSuccessful()) {
                        // Notificación de registro completado correctamente
                        Toast.makeText(LoginActivity.this, "¡Cuenta creada! Entrando...", Toast.LENGTH_SHORT).show();
                        // Redirección inmediata a la actividad principal
                        irAMain();
                    } else {
                        // Notificación detallada sobre el fallo en el registro
                        Toast.makeText(LoginActivity.this, "Fallo al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Método para validar que los datos de entrada no estén vacíos o sean cortos
    private boolean validarCampos(String email, String pass) {
        // Comprobación de que ninguno de los campos esté en blanco
        if (email.isEmpty() || pass.isEmpty()) {
            // Aviso de obligatoriedad de los datos
            Toast.makeText(this, "Por favor, rellena correo y contraseña", Toast.LENGTH_SHORT).show();
            // Retorno negativo de la validación
            return false;
        }
        // Comprobación de que la clave tenga una longitud de seguridad mínima
        if (pass.length() < 6) {
            // Aviso sobre el requisito de longitud de la contraseña
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            // Retorno negativo de la validación
            return false;
        }
        // Retorno positivo si se superan todas las comprobaciones
        return true;
    }

    // Método para realizar la transición a la pantalla principal
    private void irAMain() {
        // Definición de la intención de cambio entre Login y Main
        Intent intent = new Intent(this, MainActivity.class);
        // Inicio de la nueva actividad definida
        startActivity(intent);
        // Finalización de la actividad actual para evitar que quede en segundo plano
        finish();
    }
}