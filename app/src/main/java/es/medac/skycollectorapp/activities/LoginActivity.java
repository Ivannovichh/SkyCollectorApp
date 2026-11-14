// Declaración del paquete donde se aloja la actividad de inicio de sesión
package es.medac.skycollectorapp.activities;

// Importaciones básicas del sistema Android
import android.content.Context; // Contexto de la aplicación para acceder a preferencias
import android.content.Intent; // Objeto para lanzar pantallas y recibir resultados
import android.content.SharedPreferences; // Almacenamiento local persistente ligero
import android.os.Bundle; // Contenedor de estado durante la creación o restauración de la actividad
import android.text.InputType; // Constantes para definir el tipo de teclado en los diálogos
import android.util.Patterns; // Expresiones regulares estándar para validar correos
import android.widget.Button; // Componente visual de botón estándar
import android.widget.EditText; // Componente visual de campo de texto editable
import android.widget.Toast; // Notificaciones emergentes breves en pantalla

// Importaciones de AndroidX para gestionar resultados de actividades y compatibilidad
import androidx.activity.result.ActivityResultLauncher; // Lanzador moderno para recibir resultados de otra pantalla
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar para lanzar Intents
import androidx.annotation.NonNull; // Anotación que indica que un parámetro no puede ser nulo
import androidx.appcompat.app.AlertDialog; // Cuadros de diálogo emergentes nativos
import androidx.appcompat.app.AppCompatActivity; // Clase base de actividad compatible

// Importaciones de Google Sign-In y Material Design
import com.google.android.gms.auth.api.signin.GoogleSignIn; // Punto de acceso principal al SDK de Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignInAccount; // Objeto con los datos de la cuenta de Google elegida
import com.google.android.gms.auth.api.signin.GoogleSignInClient; // Cliente configurado para gestionar el acceso con Google
import com.google.android.gms.auth.api.signin.GoogleSignInOptions; // Opciones de configuración para solicitar token y correo
import com.google.android.gms.common.api.ApiException; // Excepción lanzada si falla el proceso de Google Sign-In
import com.google.android.gms.tasks.Task; // Tarea asíncrona devuelta por los servicios de Google Play
import com.google.android.material.button.MaterialButton; // Botón estilizado de Material Design

// Importaciones de Firebase Authentication (Correo, Google y Teléfono/SMS)
import com.google.firebase.FirebaseException; // Excepción base para errores de servicios de Firebase
import com.google.firebase.FirebaseTooManyRequestsException; // Excepción lanzada al superar el límite de peticiones SMS
import com.google.firebase.auth.AuthCredential; // Credencial genérica válida para iniciar sesión en Firebase
import com.google.firebase.auth.FirebaseAuth; // Gestor central de autenticación de Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException; // Excepción para números o códigos mal formateados
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException; // Excepción si falta actividad para reCAPTCHA
import com.google.firebase.auth.FirebaseUser; // Representación del usuario autenticado actualmente
import com.google.firebase.auth.GoogleAuthProvider; // Proveedor que convierte el token de Google en credencial Firebase
import com.google.firebase.auth.PhoneAuthCredential; // Credencial generada tras verificar un número de teléfono
import com.google.firebase.auth.PhoneAuthOptions; // Configuración de parámetros para el envío del SMS
import com.google.firebase.auth.PhoneAuthProvider; // Proveedor encargado de enviar y verificar códigos SMS

// Importaciones estándar de Java y recursos del proyecto
import java.util.concurrent.TimeUnit; // Unidades de tiempo para configurar el timeout del SMS
import es.medac.skycollectorapp.R; // Clase generada con los identificadores de recursos XML

// Clase principal que controla el acceso mediante correo/contraseña, Google y código SMS
public class LoginActivity extends AppCompatActivity {

    // ------------------------------
    // CONSTANTES Y VARIABLES DE ESTADO
    // ------------------------------
    private static final String KEY_VERIFICATION_ID = "key_verification_id"; // Clave para guardar el ID de verificación en Bundle
    private static final String KEY_TELEFONO_ACTUAL = "key_telefono_actual"; // Clave para guardar el teléfono en curso en Bundle

    private EditText etEmail; // Campo de texto para escribir el correo electrónico
    private EditText etPassword; // Campo de texto para escribir la contraseña
    private Button btnLogin; // Botón para iniciar sesión con correo y contraseña
    private Button btnRegistro; // Botón para registrar una cuenta nueva con correo
    private MaterialButton btnGoogleLogin; // Botón para iniciar sesión con cuenta de Google
    private MaterialButton btnSmsLogin; // Botón para iniciar sesión mediante código SMS

    private FirebaseAuth mAuth; // Instancia principal de Firebase Authentication
    private GoogleSignInClient googleSignInClient; // Cliente de inicio de sesión de Google

    private String verificationIdActual; // Identificador de verificación recibido al enviar el SMS
    private String telefonoEnCurso; // Número de teléfono que está siendo verificado actualmente
    private PhoneAuthProvider.ForceResendingToken resendToken; // Token necesario para forzar el reenvío del SMS

    // ------------------------------
    // LANZADOR Y CALLBACKS EXTERNOS
    // ------------------------------

    // Lanzador que recoge el resultado de la ventana de selección de cuenta de Google
    private final ActivityResultLauncher<Intent> launcherGoogle = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), // Contrato estándar para esperar resultado
            result -> { // Expresión lambda ejecutada al cerrar el selector de Google
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData()); // Extraer tarea
                try { // Bloque protegido frente a errores de conexión o cancelación
                    GoogleSignInAccount cuenta = task.getResult(ApiException.class); // Obtener cuenta seleccionada
                    if (cuenta != null && cuenta.getIdToken() != null) { // Comprobar que la cuenta trae token válido
                        autenticarConFirebase(GoogleAuthProvider.getCredential(cuenta.getIdToken(), null)); // Entrar en Firebase
                    }
                } catch (ApiException e) { // Capturar error devuelto por Google Play Services
                    if (e.getStatusCode() != 12501) { // Ignorar código 12501 (el usuario cerró la ventana manualmente)
                        Toast.makeText(this, "Error Google Sign-In (código " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show(); // Avisar error
                    }
                }
            }
    );

    // Callbacks que escuchan el estado del envío y verificación del SMS en Firebase
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacksSms =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) { // Si Google verifica el teléfono automáticamente
                    autenticarConFirebase(credential); // Iniciar sesión directamente sin pedir el código manual
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) { // Si falla el envío del SMS o la validación
                    if (e instanceof FirebaseAuthInvalidCredentialsException) { // Comprobar si el formato del teléfono es inválido
                        Toast.makeText(LoginActivity.this, "Formato de teléfono no válido", Toast.LENGTH_LONG).show(); // Avisar formato incorrecto
                    } else if (e instanceof FirebaseTooManyRequestsException) { // Comprobar si se superó la cuota de envíos
                        Toast.makeText(LoginActivity.this, "Cuota de SMS excedida temporalmente", Toast.LENGTH_LONG).show(); // Avisar exceso de intentos
                    } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) { // Comprobar si falló la actividad para reCAPTCHA
                        Toast.makeText(LoginActivity.this, "Falta actividad válida para verificar reCAPTCHA", Toast.LENGTH_LONG).show(); // Avisar error reCAPTCHA
                    } else { // Cualquier otro error genérico de Firebase
                        Toast.makeText(LoginActivity.this, "Error SMS: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Mostrar mensaje detallado
                    }
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) { // Cuando el SMS se ha enviado con éxito
                    verificationIdActual = verificationId; // Guardar el ID necesario para comprobar el código después
                    resendToken = token; // Guardar el token de reenvío por si el usuario solicita otro SMS
                    Toast.makeText(LoginActivity.this, "Código SMS enviado", Toast.LENGTH_SHORT).show(); // Notificar envío
                    mostrarDialogoCodigoSms(); // Abrir ventana para que el usuario escriba los 6 dígitos
                }
            };

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método ejecutado al crearse la pantalla de login
        super.onCreate(savedInstanceState); // Llamada obligatoria al constructor de la superclase
        setContentView(R.layout.activity_login); // Cargar el diseño XML de la pantalla

        mAuth = FirebaseAuth.getInstance(); // Obtener la instancia activa de Firebase Auth
        mAuth.useAppLanguage(); // Configurar el idioma del dispositivo para las plantillas de SMS de Firebase

        if (mAuth.getCurrentUser() != null) { // Comprobar si el usuario ya tenía la sesión iniciada
            irAMain(); // Saltar directamente a la pantalla principal
            return; // Detener ejecución para no inicializar vistas innecesarias
        }

        // Restaurar datos si Android recreó la actividad tras volver del navegador de reCAPTCHA
        if (savedInstanceState != null) { // Verificar si existe estado guardado previamente
            verificationIdActual = savedInstanceState.getString(KEY_VERIFICATION_ID); // Recuperar ID de verificación
            telefonoEnCurso = savedInstanceState.getString(KEY_TELEFONO_ACTUAL); // Recuperar teléfono en curso
        }

        // Configurar las opciones de Google Sign-In solicitando el ID de cliente web y el email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Vincular con el cliente web de Firebase
                .requestEmail() // Solicitar acceso al correo electrónico de la cuenta
                .build(); // Construir el objeto de configuración
        googleSignInClient = GoogleSignIn.getClient(this, gso); // Inicializar el cliente de Google

        // Enlazar cada variable Java con su componente visual del archivo XML
        etEmail = findViewById(R.id.etEmail); // Campo de correo electrónico
        etPassword = findViewById(R.id.etPassword); // Campo de contraseña
        btnLogin = findViewById(R.id.btnLogin); // Botón de acceso tradicional
        btnRegistro = findViewById(R.id.btnRegistro); // Botón de registro de nueva cuenta
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin); // Botón de acceso con Google
        btnSmsLogin = findViewById(R.id.btnSmsLogin); // Botón de acceso por SMS

        // Asignar las acciones a ejecutar cuando el usuario pulse cada botón
        btnLogin.setOnClickListener(v -> iniciarSesionCorreo()); // Iniciar sesión con correo y contraseña
        btnRegistro.setOnClickListener(v -> registrarUsuarioCorreo()); // Crear cuenta nueva con correo
        btnGoogleLogin.setOnClickListener(v -> iniciarSesionGoogle()); // Abrir selector de cuentas de Google
        btnSmsLogin.setOnClickListener(v -> mostrarDialogoTelefono()); // Abrir cuadro para introducir el teléfono
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) { // Guarda el estado antes de destruir o recrear la actividad
        super.onSaveInstanceState(outState); // Llamada obligatoria al método padre
        outState.putString(KEY_VERIFICATION_ID, verificationIdActual); // Guardar ID de verificación SMS actual
        outState.putString(KEY_TELEFONO_ACTUAL, telefonoEnCurso); // Guardar número de teléfono en curso
    }

    // ------------------------------
    // AUTENTICACIÓN POR CORREO Y GOOGLE
    // ------------------------------

    // Valida los campos e inicia sesión en Firebase con correo electrónico y contraseña
    private void iniciarSesionCorreo() {
        String email = etEmail.getText().toString().trim(); // Obtener texto del correo eliminando espacios
        String password = etPassword.getText().toString().trim(); // Obtener texto de la contraseña
        if (!validarCampos(email, password)) return; // Salir si el correo o la contraseña no son válidos

        mAuth.signInWithEmailAndPassword(email, password) // Enviar credenciales a Firebase Auth
                .addOnSuccessListener(authResult -> { // Si la autenticación es correcta
                    if (mAuth.getCurrentUser() != null) { // Verificar usuario activo
                        guardarDatosContactoIniciales(mAuth.getCurrentUser()); // Guardar su correo en preferencias privadas
                    }
                    irAMain(); // Entrar a MainActivity
                })
                .addOnFailureListener(e -> // Si las credenciales son incorrectas o hay error de red
                        Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show() // Avisar error
                );
    }

    // Crea un nuevo usuario en Firebase utilizando correo electrónico y contraseña
    private void registrarUsuarioCorreo() {
        String email = etEmail.getText().toString().trim(); // Obtener correo escrito
        String password = etPassword.getText().toString().trim(); // Obtener contraseña escrita
        if (!validarCampos(email, password)) return; // Detener si los datos no cumplen los requisitos

        mAuth.createUserWithEmailAndPassword(email, password) // Solicitar creación de cuenta en Firebase
                .addOnSuccessListener(authResult -> { // Al crearse la cuenta correctamente
                    if (mAuth.getCurrentUser() != null) { // Verificar usuario creado
                        guardarDatosContactoIniciales(mAuth.getCurrentUser()); // Guardar correo en preferencias privadas
                    }
                    Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show(); // Mostrar confirmación
                    irAMain(); // Acceder directamente a la aplicación
                })
                .addOnFailureListener(e -> // Si el correo ya existe o hay error
                        Toast.makeText(this, "Error al registrar: " + e.getMessage(), Toast.LENGTH_LONG).show() // Mostrar error
                );
    }

    // Cierra cualquier selección previa en Google Sign-In para permitir elegir cuenta y abre el selector
    private void iniciarSesionGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> // Limpiar cuenta en caché del cliente
                launcherGoogle.launch(googleSignInClient.getSignInIntent()) // Lanzar ventana de selección de cuenta
        );
    }

    // ------------------------------
    // AUTENTICACIÓN POR TELÉFONO / SMS
    // ------------------------------

    // Despliega un cuadro de diálogo para pedir el número de teléfono con prefijo internacional
    private void mostrarDialogoTelefono() {
        EditText inputTelefono = new EditText(this); // Crear campo de texto dinámico para el diálogo
        inputTelefono.setInputType(InputType.TYPE_CLASS_PHONE); // Mostrar teclado numérico telefónico
        inputTelefono.setHint("+34 600 000 000"); // Texto de ejemplo con prefijo
        inputTelefono.setText("+34"); // Pre-rellenar prefijo de España por comodidad
        inputTelefono.setSelection(inputTelefono.getText().length()); // Colocar cursor al final del prefijo

        new AlertDialog.Builder(this) // Construir cuadro de diálogo emergente
                .setTitle("Acceso por SMS") // Título de la ventana
                .setMessage("Introduce tu número de teléfono con el prefijo internacional:") // Instrucciones
                .setView(inputTelefono) // Insertar el campo de texto dentro del diálogo
                .setPositiveButton("Enviar código", (dialog, which) -> { // Acción al pulsar enviar
                    String telefono = inputTelefono.getText().toString().replaceAll("\\s+", ""); // Limpiar espacios
                    if (telefono.length() < 9 || !telefono.startsWith("+")) { // Validar longitud mínima y símbolo '+'
                        Toast.makeText(this, "Incluye el prefijo internacional (ej. +34600112233)", Toast.LENGTH_LONG).show(); // Avisar formato
                        return; // No enviar si el formato es incorrecto
                    }
                    telefonoEnCurso = telefono; // Guardar el teléfono introducido para mostrarlo o reenviarlo
                    enviarCodigoSms(telefono, null); // Iniciar solicitud de SMS a Firebase sin token de reenvío
                })
                .setNegativeButton("Cancelar", null) // Botón para cerrar sin hacer nada
                .show(); // Mostrar el diálogo en pantalla
    }

    // Configura los parámetros y solicita a Firebase el envío del SMS al teléfono indicado
    private void enviarCodigoSms(String telefono, PhoneAuthProvider.ForceResendingToken tokenReenvio) {
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth) // Crear constructor de opciones SMS
                .setPhoneNumber(telefono) // Establecer número destino en formato E.164
                .setTimeout(60L, TimeUnit.SECONDS) // Tiempo máximo de espera antes de permitir reenvío
                .setActivity(this) // Vincular a la actividad actual para verificación automática
                .setCallbacks(callbacksSms); // Asociar los callbacks que recibirán el resultado

        if (tokenReenvio != null) { // Comprobar si se trata de una solicitud de reenvío
            builder.setForceResendingToken(tokenReenvio); // Adjuntar token para forzar el reenvío del SMS
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build()); // Ejecutar petición de envío de SMS
    }

    // Muestra un cuadro de diálogo para introducir el código de 6 dígitos recibido por SMS
    private void mostrarDialogoCodigoSms() {
        EditText inputCodigo = new EditText(this); // Crear campo de texto para el código
        inputCodigo.setInputType(InputType.TYPE_CLASS_NUMBER); // Mostrar teclado numérico
        inputCodigo.setHint("123456"); // Ejemplo de código de 6 dígitos

        new AlertDialog.Builder(this) // Construir diálogo de verificación
                .setTitle("Verificar código") // Título del cuadro
                .setMessage("Introduce el código de 6 dígitos enviado a " + telefonoEnCurso + ":") // Mensaje explicativo
                .setCancelable(false) // Impedir cierre accidental al tocar fuera
                .setView(inputCodigo) // Añadir campo numérico al diálogo
                .setPositiveButton("Verificar", (dialog, which) -> { // Al pulsar el botón Verificar
                    String codigo = inputCodigo.getText().toString().trim(); // Leer código escrito
                    if (codigo.length() < 6 || verificationIdActual == null) { // Comprobar que tiene 6 dígitos
                        Toast.makeText(this, "Introduce el código completo de 6 dígitos", Toast.LENGTH_SHORT).show(); // Avisar
                        return; // Salir sin verificar
                    }
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationIdActual, codigo); // Crear credencial
                    autenticarConFirebase(credential); // Iniciar sesión en Firebase con la credencial SMS
                })
                .setNeutralButton("Reenviar SMS", (dialog, which) -> { // Botón para volver a solicitar el código SMS
                    if (telefonoEnCurso != null && resendToken != null) { // Verificar que existen teléfono y token válidos
                        enviarCodigoSms(telefonoEnCurso, resendToken); // Reenviar SMS usando el token guardado
                    }
                })
                .setNegativeButton("Cancelar", null) // Botón para cancelar verificación
                .show(); // Mostrar diálogo
    }

    // ------------------------------
    // CREDENCIALES, PERFIL Y NAVEGACIÓN
    // ------------------------------

    // Inicia sesión en Firebase utilizando una credencial externa (Google o SMS)
    private void autenticarConFirebase(AuthCredential credential) {
        mAuth.signInWithCredential(credential) // Autenticar con la credencial proporcionada
                .addOnSuccessListener(authResult -> { // Si la autenticación tiene éxito
                    FirebaseUser user = mAuth.getCurrentUser(); // Obtener datos del usuario autenticado
                    if (user != null) { // Verificar que el objeto usuario no sea nulo
                        guardarDatosContactoIniciales(user); // Guardar correo/teléfono y foto por defecto si no tiene
                    }
                    irAMain(); // Entrar a la pantalla principal
                })
                .addOnFailureListener(e -> // Si el código SMS es incorrecto o la credencial expiró
                        Toast.makeText(this, "Error de autenticación: " + e.getMessage(), Toast.LENGTH_LONG).show() // Avisar error
                );
    }

    // Comprueba que el correo tenga formato válido y que la contraseña tenga al menos 6 caracteres
    private boolean validarCampos(String email, String password) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { // Validar patrón de correo
            etEmail.setError("Introduce un correo válido"); // Marcar error en el campo email
            etEmail.requestFocus(); // Enfocar el cursor en el campo email
            return false; // Retornar validación fallida
        }
        if (password.length() < 6) { // Validar longitud mínima exigida por Firebase
            etPassword.setError("La contraseña debe tener al menos 6 caracteres"); // Marcar error en contraseña
            etPassword.requestFocus(); // Enfocar cursor en contraseña
            return false; // Retornar validación fallida
        }
        return true; // Ambos campos son válidos
    }

    // Almacena por separado el correo, el teléfono y la foto inicial en SharedPreferences sin exponer datos privados como nombre
    private void guardarDatosContactoIniciales(FirebaseUser user) {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE); // Abrir preferencias del perfil
        String uid = user.getUid(); // Obtener identificador único del usuario
        SharedPreferences.Editor editor = prefs.edit(); // Iniciar editor de preferencias

        // Si el usuario tiene correo (acceso por email o Google) y aún no estaba guardado, almacenarlo en su clave privada
        if (user.getEmail() != null && !user.getEmail().isEmpty() && !prefs.contains("correo_" + uid)) {
            editor.putString("correo_" + uid, user.getEmail()); // Guardar correo de contacto
        }

        // Si el usuario entró por teléfono SMS y aún no estaba guardado, almacenarlo en su clave privada
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() && !prefs.contains("telefono_" + uid)) {
            editor.putString("telefono_" + uid, user.getPhoneNumber()); // Guardar teléfono de contacto
        }

        // Si aún no tiene foto de perfil local y Google proporciona avatar, guardarlo
        if (!prefs.contains("foto_" + uid) && user.getPhotoUrl() != null) {
            editor.putString("foto_" + uid, user.getPhotoUrl().toString()); // Guardar URI de la foto
        }

        editor.apply(); // Confirmar y guardar cambios en almacenamiento local
    }

    // Navega hacia MainActivity y vacía el historial de pantallas para impedir volver atrás al login
    private void irAMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Crear intención hacia MainActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpiar pila de actividades
        startActivity(intent); // Iniciar pantalla principal
        finish(); // Cerrar LoginActivity
    }
}