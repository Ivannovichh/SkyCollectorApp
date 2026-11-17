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
                    if (e instanceof FirebaseAuthInvalidCredentialsException) { // Comprueba si el formato del teléfono es incorrecto
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_telefono_no_valido), Toast.LENGTH_LONG).show(); // Avisa de número inválido
                    } else if (e instanceof FirebaseTooManyRequestsException) { // Comprueba si se superó el límite de peticiones SMS
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_cuota_sms), Toast.LENGTH_LONG).show(); // Avisa de cuota agotada
                    } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) { // Comprueba si faltó la actividad para reCAPTCHA
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_error_recaptcha), Toast.LENGTH_LONG).show(); // Avisa de fallo de verificación humana
                    } else { // Cualquier otro error devuelto por Firebase
                        Toast.makeText(LoginActivity.this, getString(R.string.toast_error_sms, e.getMessage()), Toast.LENGTH_LONG).show(); // Muestra el mensaje técnico del error
                    }
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) { // Cuando el SMS se ha enviado con éxito
                    verificationIdActual = verificationId; // Guardar el ID necesario para comprobar el código después
                    resendToken = token; // Guardar el token de reenvío por si el usuario solicita otro SMS
                    Toast.makeText(LoginActivity.this, getString(R.string.toast_sms_enviado), Toast.LENGTH_SHORT).show(); // Confirmar envío al usuario
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
            verificationIdActual = savedInstanceState.getString(KEY_VERIFICATION_ID); // Recuperar ID de verificación previo
            telefonoEnCurso = savedInstanceState.getString(KEY_TELEFONO_ACTUAL); // Recuperar número de teléfono en curso
        }

        // Configurar las opciones de Google Sign-In solicitando el ID de cliente web y el email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // Construir opciones de inicio estándar
                .requestIdToken(getString(R.string.default_web_client_id)) // Solicitar token de ID vinculado a Firebase
                .requestEmail() // Solicitar dirección de correo de la cuenta Google
                .build(); // Finalizar construcción de opciones
        googleSignInClient = GoogleSignIn.getClient(this, gso); // Instanciar el cliente de Google Sign-In

        // Enlazar cada variable Java con su componente visual del archivo XML
        etEmail = findViewById(R.id.etEmail); // Vincular campo de correo
        etPassword = findViewById(R.id.etPassword); // Vincular campo de contraseña
        btnLogin = findViewById(R.id.btnLogin); // Vincular botón de iniciar sesión
        btnRegistro = findViewById(R.id.btnRegistro); // Vincular botón de registro
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin); // Vincular botón de acceso con Google
        //btnSmsLogin = findViewById(R.id.btnSmsLogin);  //Vincular botón de acceso por SMS

        // Asignar las acciones a ejecutar cuando el usuario pulse cada botón
        btnLogin.setOnClickListener(v -> iniciarSesionCorreo()); // Ejecutar login por correo al pulsar
        btnRegistro.setOnClickListener(v -> registrarUsuarioCorreo()); // Ejecutar registro por correo al pulsar
        btnGoogleLogin.setOnClickListener(v -> iniciarSesionGoogle()); // Abrir selector de Google al pulsar
        // btnSmsLogin.setOnClickListener(v -> mostrarDialogoTelefono()); // Abrir diálogo de teléfono al pulsar
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); // Llamar al guardado de estado de la superclase
        outState.putString(KEY_VERIFICATION_ID, verificationIdActual); // Guardar ID de verificación SMS antes de destrucción
        outState.putString(KEY_TELEFONO_ACTUAL, telefonoEnCurso); // Guardar teléfono en verificación antes de destrucción
    }

    // ------------------------------
    // AUTENTICACIÓN POR CORREO Y GOOGLE
    // ------------------------------

    // Valida los campos e inicia sesión en Firebase con correo electrónico y contraseña
    private void iniciarSesionCorreo() {
        String email = etEmail.getText().toString().trim(); // Obtener correo escrito sin espacios laterales
        String password = etPassword.getText().toString().trim(); // Obtener contraseña escrita sin espacios laterales
        if (!validarCampos(email, password)) return; // Detener si algún campo no cumple el formato válido

        mAuth.signInWithEmailAndPassword(email, password) // Solicitar inicio de sesión en Firebase
                .addOnSuccessListener(authResult -> { // Callback si las credenciales son correctas
                    if (mAuth.getCurrentUser() != null) { // Verificar que el usuario autenticado no sea nulo
                        guardarDatosContactoIniciales(mAuth.getCurrentUser()); // Persistir datos iniciales en SharedPreferences
                    }
                    irAMain(); // Redirigir a la pantalla principal
                })
                .addOnFailureListener(e -> // Callback si ocurre algún error de autenticación
                        Toast.makeText(this, getString(R.string.toast_error_login, e.getMessage()), Toast.LENGTH_LONG).show() // Mostrar error
                );
    }

    // Crea un nuevo usuario en Firebase utilizando correo electrónico y contraseña
    private void registrarUsuarioCorreo() {
        String email = etEmail.getText().toString().trim(); // Obtener correo escrito sin espacios laterales
        String password = etPassword.getText().toString().trim(); // Obtener contraseña escrita sin espacios laterales
        if (!validarCampos(email, password)) return; // Detener si los datos no pasan la validación

        mAuth.createUserWithEmailAndPassword(email, password) // Solicitar creación de nueva cuenta en Firebase
                .addOnSuccessListener(authResult -> { // Callback si la cuenta se crea correctamente
                    if (mAuth.getCurrentUser() != null) { // Verificar que la sesión quedó iniciada
                        guardarDatosContactoIniciales(mAuth.getCurrentUser()); // Guardar correo inicial del perfil
                    }
                    Toast.makeText(this, getString(R.string.toast_cuenta_creada), Toast.LENGTH_SHORT).show(); // Notificar registro exitoso
                    irAMain(); // Redirigir a la pantalla principal
                })
                .addOnFailureListener(e -> // Callback si falla el registro (ej. correo ya existente)
                        Toast.makeText(this, getString(R.string.toast_error_registro, e.getMessage()), Toast.LENGTH_LONG).show() // Mostrar error
                );
    }

    // Cierra cualquier selección previa en Google Sign-In para permitir elegir cuenta y abre el selector
    private void iniciarSesionGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> // Cerrar sesión previa en el cliente de Google
                launcherGoogle.launch(googleSignInClient.getSignInIntent()) // Lanzar el selector de cuentas de Google
        );
    }

    // ------------------------------
    // AUTENTICACIÓN POR TELÉFONO / SMS
    // ------------------------------

    // Despliega un cuadro de diálogo para pedir el número de teléfono con prefijo internacional
    private void mostrarDialogoTelefono() {
        EditText inputTelefono = new EditText(this); // Crear campo de entrada de texto dinámicamente
        inputTelefono.setInputType(InputType.TYPE_CLASS_PHONE); // Configurar teclado numérico telefónico
        inputTelefono.setHint("+34 600 000 000"); // Establecer texto de ayuda con ejemplo internacional
        inputTelefono.setText("+34"); // Prellenar con el prefijo internacional de España
        inputTelefono.setSelection(inputTelefono.getText().length()); // Colocar el cursor al final del prefijo

        new AlertDialog.Builder(this) // Construir cuadro de diálogo modal
                .setTitle(getString(R.string.dialogo_sms_titulo)) // Asignar título del diálogo
                .setMessage(getString(R.string.dialogo_sms_mensaje)) // Asignar instrucciones para el usuario
                .setView(inputTelefono) // Insertar el campo de teléfono dentro del diálogo
                .setPositiveButton(getString(R.string.dialogo_sms_enviar), (dialog, which) -> { // Acción al pulsar Enviar
                    String telefono = inputTelefono.getText().toString().replaceAll("\\s+", ""); // Limpiar espacios en blanco del número
                    if (telefono.length() < 9 || !telefono.startsWith("+")) { // Validar longitud mínima y presencia del signo '+'
                        Toast.makeText(this, getString(R.string.toast_prefijo_internacional), Toast.LENGTH_LONG).show(); // Avisar formato incorrecto
                        return; // Abortar envío
                    }
                    telefonoEnCurso = telefono; // Guardar el número validado como teléfono en curso
                    enviarCodigoSms(telefono, null); // Iniciar el envío del SMS mediante Firebase
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null) // Botón para cancelar y cerrar el diálogo
                .show(); // Mostrar el diálogo en pantalla
    }

    // Configura los parámetros y solicita a Firebase el envío del SMS al teléfono indicado
    private void enviarCodigoSms(String telefono, PhoneAuthProvider.ForceResendingToken tokenReenvio) {
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth) // Crear constructor de opciones de verificación telefónica
                .setPhoneNumber(telefono) // Indicar el número de destino con prefijo internacional
                .setTimeout(60L, TimeUnit.SECONDS) // Establecer tiempo de espera de 60 segundos
                .setActivity(this) // Vincular la actividad actual para flujos de reCAPTCHA
                .setCallbacks(callbacksSms); // Asociar los callbacks de respuesta del SMS

        if (tokenReenvio != null) { // Comprobar si se trata de una solicitud de reenvío
            builder.setForceResendingToken(tokenReenvio); // Adjuntar el token de reenvío forzado
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build()); // Ejecutar la verificación telefónica en Firebase
    }

    // Muestra un cuadro de diálogo para introducir el código de 6 dígitos recibido por SMS
    private void mostrarDialogoCodigoSms() {
        EditText inputCodigo = new EditText(this); // Crear campo de entrada para el código numérico
        inputCodigo.setInputType(InputType.TYPE_CLASS_NUMBER); // Configurar teclado exclusivamente numérico
        inputCodigo.setHint("123456"); // Establecer ejemplo de código de 6 dígitos

        new AlertDialog.Builder(this) // Construir cuadro de diálogo de verificación
                .setTitle(getString(R.string.dialogo_codigo_titulo)) // Asignar título del diálogo
                .setMessage(getString(R.string.dialogo_codigo_mensaje, telefonoEnCurso)) // Mostrar número al que se envió el SMS
                .setCancelable(false) // Evitar cierre accidental tocando fuera del cuadro
                .setView(inputCodigo) // Insertar el campo de código en el diálogo
                .setPositiveButton(getString(R.string.dialogo_codigo_verificar), (dialog, which) -> { // Acción al pulsar Verificar
                    String codigo = inputCodigo.getText().toString().trim(); // Leer el código introducido
                    if (codigo.length() < 6 || verificationIdActual == null) { // Comprobar que tenga 6 dígitos y exista ID de verificación
                        Toast.makeText(this, getString(R.string.toast_codigo_incompleto), Toast.LENGTH_SHORT).show(); // Avisar si está incompleto
                        return; // Abortar verificación
                    }
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationIdActual, codigo); // Generar credencial SMS
                    autenticarConFirebase(credential); // Iniciar sesión en Firebase con la credencial obtenida
                })
                .setNeutralButton(getString(R.string.dialogo_codigo_reenviar), (dialog, which) -> { // Botón neutro para reenviar el SMS
                    if (telefonoEnCurso != null && resendToken != null) { // Verificar que existan teléfono y token de reenvío
                        enviarCodigoSms(telefonoEnCurso, resendToken); // Solicitar un nuevo envío de código SMS
                    }
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null) // Botón para cancelar el proceso
                .show(); // Mostrar el diálogo en pantalla
    }

    // ------------------------------
    // CREDENCIALES, PERFIL Y NAVEGACIÓN
    // ------------------------------

    // Inicia sesión en Firebase utilizando una credencial externa (Google o SMS)
    private void autenticarConFirebase(AuthCredential credential) {
        mAuth.signInWithCredential(credential) // Autenticar al usuario con la credencial proporcionada
                .addOnSuccessListener(authResult -> { // Callback si la autenticación es exitosa
                    FirebaseUser user = mAuth.getCurrentUser(); // Obtener el objeto del usuario autenticado
                    if (user != null) { // Comprobar que el usuario no sea nulo
                        guardarDatosContactoIniciales(user); // Guardar datos iniciales (correo, teléfono, foto) en local
                    }
                    irAMain(); // Redirigir a la pantalla principal
                })
                .addOnFailureListener(e -> // Callback si falla la validación de la credencial
                        Toast.makeText(this, getString(R.string.toast_error_autenticacion, e.getMessage()), Toast.LENGTH_LONG).show() // Mostrar error
                );
    }

    // Comprueba que el correo tenga formato válido y que la contraseña tenga al menos 6 caracteres
    private boolean validarCampos(String email, String password) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { // Verificar que el correo no esté vacío y cumpla el patrón estándar
            etEmail.setError(getString(R.string.error_correo_valido)); // Mostrar mensaje de error en el campo de correo
            etEmail.requestFocus(); // Enfocar el cursor en el campo de correo
            return false; // Indicar validación fallida
        }
        if (password.length() < 6) { // Verificar longitud mínima de 6 caracteres exigida por Firebase
            etPassword.setError(getString(R.string.error_password_corta)); // Mostrar mensaje de error en el campo de contraseña
            etPassword.requestFocus(); // Enfocar el cursor en el campo de contraseña
            return false; // Indicar validación fallida
        }
        return true; // Indicar que ambos campos son válidos
    }

    // Almacena por separado el correo, el teléfono y la foto inicial en SharedPreferences
    private void guardarDatosContactoIniciales(FirebaseUser user) {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE); // Abrir preferencias privadas del perfil
        String uid = user.getUid(); // Obtener identificador único del usuario
        SharedPreferences.Editor editor = prefs.edit(); // Iniciar editor de preferencias

        if (user.getEmail() != null && !user.getEmail().isEmpty() && !prefs.contains("correo_" + uid)) { // Si tiene correo y aún no está guardado
            editor.putString("correo_" + uid, user.getEmail()); // Guardar el correo asociado a su UID
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() && !prefs.contains("telefono_" + uid)) { // Si tiene teléfono y aún no está guardado
            editor.putString("telefono_" + uid, user.getPhoneNumber()); // Guardar el teléfono asociado a su UID
        }

        if (!prefs.contains("foto_" + uid) && user.getPhotoUrl() != null) { // Si tiene foto de perfil (ej. Google) y aún no está guardada
            editor.putString("foto_" + uid, user.getPhotoUrl().toString()); // Guardar la URL de la foto asociada a su UID
        }

        editor.apply(); // Aplicar todos los cambios de forma asíncrona
    }

    // Navega hacia MainActivity y vacía el historial de pantallas
    private void irAMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Preparar intención hacia la pantalla principal
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpiar pila de actividades anteriores para evitar volver al login
        startActivity(intent); // Iniciar MainActivity
        finish(); // Cerrar LoginActivity
    }
}