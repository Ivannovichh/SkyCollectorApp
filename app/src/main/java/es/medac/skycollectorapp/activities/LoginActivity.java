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
                        Toast.makeText(this, getString(R.string.toast_error_google, e.getStatusCode()), Toast.LENGTH_LONG).show();
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
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_telefono_no_valido), Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_cuota_sms), Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_error_recaptcha), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_error_sms, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) { // Cuando el SMS se ha enviado con éxito
                    verificationIdActual = verificationId; // Guardar el ID necesario para comprobar el código después
                    resendToken = token; // Guardar el token de reenvío por si el usuario solicita otro SMS
                    Toast.makeText(LoginActivity.this, getString(R.string.toast_sms_enviado), Toast.LENGTH_SHORT).show();
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
        if (savedInstanceState != null) {
            verificationIdActual = savedInstanceState.getString(KEY_VERIFICATION_ID);
            telefonoEnCurso = savedInstanceState.getString(KEY_TELEFONO_ACTUAL);
        }

        // Configurar las opciones de Google Sign-In solicitando el ID de cliente web y el email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Enlazar cada variable Java con su componente visual del archivo XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistro = findViewById(R.id.btnRegistro);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnSmsLogin = findViewById(R.id.btnSmsLogin);

        // Asignar las acciones a ejecutar cuando el usuario pulse cada botón
        btnLogin.setOnClickListener(v -> iniciarSesionCorreo());
        btnRegistro.setOnClickListener(v -> registrarUsuarioCorreo());
        btnGoogleLogin.setOnClickListener(v -> iniciarSesionGoogle());
        btnSmsLogin.setOnClickListener(v -> mostrarDialogoTelefono());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_VERIFICATION_ID, verificationIdActual);
        outState.putString(KEY_TELEFONO_ACTUAL, telefonoEnCurso);
    }

    // ------------------------------
    // AUTENTICACIÓN POR CORREO Y GOOGLE
    // ------------------------------

    // Valida los campos e inicia sesión en Firebase con correo electrónico y contraseña
    private void iniciarSesionCorreo() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (!validarCampos(email, password)) return;

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (mAuth.getCurrentUser() != null) {
                        guardarDatosContactoIniciales(mAuth.getCurrentUser());
                    }
                    irAMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.toast_error_login, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }

    // Crea un nuevo usuario en Firebase utilizando correo electrónico y contraseña
    private void registrarUsuarioCorreo() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (!validarCampos(email, password)) return;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (mAuth.getCurrentUser() != null) {
                        guardarDatosContactoIniciales(mAuth.getCurrentUser());
                    }
                    Toast.makeText(this, getString(R.string.toast_cuenta_creada), Toast.LENGTH_SHORT).show();
                    irAMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.toast_error_registro, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }

    // Cierra cualquier selección previa en Google Sign-In para permitir elegir cuenta y abre el selector
    private void iniciarSesionGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task ->
                launcherGoogle.launch(googleSignInClient.getSignInIntent())
        );
    }

    // ------------------------------
    // AUTENTICACIÓN POR TELÉFONO / SMS
    // ------------------------------

    // Despliega un cuadro de diálogo para pedir el número de teléfono con prefijo internacional
    private void mostrarDialogoTelefono() {
        EditText inputTelefono = new EditText(this);
        inputTelefono.setInputType(InputType.TYPE_CLASS_PHONE);
        inputTelefono.setHint("+34 600 000 000");
        inputTelefono.setText("+34");
        inputTelefono.setSelection(inputTelefono.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_sms_titulo))
                .setMessage(getString(R.string.dialogo_sms_mensaje))
                .setView(inputTelefono)
                .setPositiveButton(getString(R.string.dialogo_sms_enviar), (dialog, which) -> {
                    String telefono = inputTelefono.getText().toString().replaceAll("\\s+", "");
                    if (telefono.length() < 9 || !telefono.startsWith("+")) {
                        Toast.makeText(this, getString(R.string.toast_prefijo_internacional), Toast.LENGTH_LONG).show();
                        return;
                    }
                    telefonoEnCurso = telefono;
                    enviarCodigoSms(telefono, null);
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    // Configura los parámetros y solicita a Firebase el envío del SMS al teléfono indicado
    private void enviarCodigoSms(String telefono, PhoneAuthProvider.ForceResendingToken tokenReenvio) {
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(telefono)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacksSms);

        if (tokenReenvio != null) {
            builder.setForceResendingToken(tokenReenvio);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    // Muestra un cuadro de diálogo para introducir el código de 6 dígitos recibido por SMS
    private void mostrarDialogoCodigoSms() {
        EditText inputCodigo = new EditText(this);
        inputCodigo.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputCodigo.setHint("123456");

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_codigo_titulo))
                .setMessage(getString(R.string.dialogo_codigo_mensaje, telefonoEnCurso))
                .setCancelable(false)
                .setView(inputCodigo)
                .setPositiveButton(getString(R.string.dialogo_codigo_verificar), (dialog, which) -> {
                    String codigo = inputCodigo.getText().toString().trim();
                    if (codigo.length() < 6 || verificationIdActual == null) {
                        Toast.makeText(this, getString(R.string.toast_codigo_incompleto), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationIdActual, codigo);
                    autenticarConFirebase(credential);
                })
                .setNeutralButton(getString(R.string.dialogo_codigo_reenviar), (dialog, which) -> {
                    if (telefonoEnCurso != null && resendToken != null) {
                        enviarCodigoSms(telefonoEnCurso, resendToken);
                    }
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    // ------------------------------
    // CREDENCIALES, PERFIL Y NAVEGACIÓN
    // ------------------------------

    // Inicia sesión en Firebase utilizando una credencial externa (Google o SMS)
    private void autenticarConFirebase(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        guardarDatosContactoIniciales(user);
                    }
                    irAMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.toast_error_autenticacion, e.getMessage()), Toast.LENGTH_LONG).show()
                );
    }

    // Comprueba que el correo tenga formato válido y que la contraseña tenga al menos 6 caracteres
    private boolean validarCampos(String email, String password) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_correo_valido));
            etEmail.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_corta));
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    // Almacena por separado el correo, el teléfono y la foto inicial en SharedPreferences
    private void guardarDatosContactoIniciales(FirebaseUser user) {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        String uid = user.getUid();
        SharedPreferences.Editor editor = prefs.edit();

        if (user.getEmail() != null && !user.getEmail().isEmpty() && !prefs.contains("correo_" + uid)) {
            editor.putString("correo_" + uid, user.getEmail());
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() && !prefs.contains("telefono_" + uid)) {
            editor.putString("telefono_" + uid, user.getPhoneNumber());
        }

        if (!prefs.contains("foto_" + uid) && user.getPhotoUrl() != null) {
            editor.putString("foto_" + uid, user.getPhotoUrl().toString());
        }

        editor.apply();
    }

    // Navega hacia MainActivity y vacía el historial de pantallas
    private void irAMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}