// Declaración del paquete donde se ubica la actividad de perfil de usuario
package es.medac.skycollectorapp.activities;

// Importación para lanzar actividades y gestionar permisos persistentes de URI
import android.content.Intent;
// Importación para leer y guardar datos del perfil en almacenamiento local
import android.content.SharedPreferences;
// Importación para manipular rutas URI de imágenes seleccionadas de la galería
import android.net.Uri;
// Importación para gestionar el estado y creación de la actividad
import android.os.Bundle;
// Importación para validar direcciones de correo electrónico mediante expresiones regulares
import android.util.Patterns;
// Importación para botones estándar de la interfaz
import android.widget.Button;
// Importación para campos de texto editables del formulario de perfil
import android.widget.EditText;
// Importación para el botón gráfico de retroceso
import android.widget.ImageButton;
// Importación para mostrar la fotografía circular del piloto
import android.widget.ImageView;
// Importación para notificaciones emergentes breves
import android.widget.Toast;

// Importación del lanzador moderno para obtener resultados del selector de galería
import androidx.activity.result.ActivityResultLauncher;
// Importación de los contratos estándar de resultados de AndroidX
import androidx.activity.result.contract.ActivityResultContracts;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación del contenedor tarjeta que envuelve la foto de perfil
import androidx.cardview.widget.CardView;

// Importación de Glide para cargar y recortar en círculo el avatar del usuario
import com.bumptech.glide.Glide;
// Importación del gestor de sesión de Firebase Authentication
import com.google.firebase.auth.FirebaseAuth;
// Importación del objeto que representa al usuario autenticado
import com.google.firebase.auth.FirebaseUser;
// Importación de la instancia de base de datos Cloud Firestore
import com.google.firebase.firestore.FirebaseFirestore;
// Importación para actualizar documentos en Firestore fusionando campos (merge)
import com.google.firebase.firestore.SetOptions;

// Importación de mapas clave-valor para construir los datos a subir a Firestore
import java.util.HashMap;
// Importación de la interfaz Map
import java.util.Map;

// Importación de los recursos XML del proyecto
import es.medac.skycollectorapp.R;
// Importación de utilidades para comprimir y codificar imágenes a Base64
import es.medac.skycollectorapp.utils.ImagenBase64Utils;
// Importación del gestor de sincronización de identidad en el ranking público
import es.medac.skycollectorapp.utils.RankingManager;

// Actividad que permite editar el nombre público, avatar público y datos privados de contacto del piloto
public class PerfilActivity extends AppCompatActivity {

    // Componente visual donde se muestra el avatar circular del usuario
    private ImageView imgFotoPerfil;
    // Campo editable para el apodo público del piloto
    private EditText etNombrePerfil;
    // Campo editable para el correo electrónico de contacto privado
    private EditText etCorreoPerfil;
    // Campo editable para el número de teléfono de contacto privado
    private EditText etTelefonoPerfil;
    // Cadena Base64 comprimida del avatar lista para publicarse en el ranking
    private String fotoBase64Pendiente = null;

    // Lanzador que abre el selector de imágenes del sistema y procesa el avatar elegido
    private final ActivityResultLauncher<String> selectorImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Obtiene el usuario actual de Firebase
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                // Comprueba que se haya elegido una imagen válida y exista sesión activa
                if (uri != null && user != null) {
                    try {
                        // Solicita permiso persistente de lectura para poder cargar la URI en futuros reinicios
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                        // Ignora si el proveedor de documentos no soporta permisos persistentes
                    }

                    // Guarda la URI local de la foto en SharedPreferences asociada al UID
                    SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
                    prefs.edit().putString("foto_" + user.getUid(), uri.toString()).apply();

                    // Comprime el avatar a 240px para publicarlo en el ranking de Firestore
                    fotoBase64Pendiente = ImagenBase64Utils.uriABase64Comprimida(this, uri, 240, 75);
                    if (fotoBase64Pendiente != null) {
                        // Persiste la versión Base64 en local y actualiza inmediatamente el avatar público en ranking
                        prefs.edit().putString("foto_publica_" + user.getUid(), fotoBase64Pendiente).apply();
                        RankingManager.actualizarIdentidadPublica(null, fotoBase64Pendiente);
                    }

                    // Renderiza la nueva fotografía recortada en círculo en la pantalla de perfil
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(imgFotoPerfil);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Carga el diseño XML de la pantalla de perfil
        setContentView(R.layout.activity_perfil);

        // Verifica que exista un usuario autenticado; si no, cierra la actividad
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        // Enlaza todos los controles visuales del archivo XML con sus variables Java
        ImageButton btnVolver = findViewById(R.id.btnVolverPerfil);
        CardView cardFoto = findViewById(R.id.cardFotoPerfil);
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil);
        etNombrePerfil = findViewById(R.id.etNombrePerfil);
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil);
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil);
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Rellena los campos del formulario con los datos guardados del usuario
        cargarDatosPerfil(user);

        // Configura el botón superior izquierdo para volver atrás
        btnVolver.setOnClickListener(v -> finish());
        // Abre la galería de imágenes al pulsar sobre la tarjeta de fotografía
        cardFoto.setOnClickListener(v -> selectorImagen.launch("image/*"));
        // Ejecuta la validación y guardado de cambios al pulsar Guardar
        btnGuardar.setOnClickListener(v -> guardarCambiosPerfil(user));
        // Cierra la sesión actual al pulsar Cerrar Sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void cargarDatosPerfil(FirebaseUser user) {
        // Abre las preferencias privadas del perfil y obtiene el UID del usuario
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String uid = user.getUid();

        // Recupera nombre, correo, teléfono y foto local usando los datos de FirebaseAuth como respaldo
        String nombre = prefs.getString("nombre_" + uid, "");
        String correo = prefs.getString("correo_" + uid, user.getEmail() != null ? user.getEmail() : "");
        String telefono = prefs.getString("telefono_" + uid, user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        String foto = prefs.getString("foto_" + uid, null);
        // Recupera la cadena Base64 pública previamente almacenada si existe
        fotoBase64Pendiente = prefs.getString("foto_publica_" + uid, null);

        // Rellena los campos de texto de la interfaz
        etNombrePerfil.setText(nombre);
        etCorreoPerfil.setText(correo);
        etTelefonoPerfil.setText(telefono);

        // Si el usuario tiene una fotografía guardada, la carga recortada en círculo con Glide
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
        // Lee y limpia espacios laterales de los valores escritos por el usuario
        String nombre = etNombrePerfil.getText().toString().trim();
        String correo = etCorreoPerfil.getText().toString().trim();
        String telefono = etTelefonoPerfil.getText().toString().trim();

        // Valida que el nombre de piloto tenga al menos 3 caracteres
        if (nombre.length() < 3) {
            etNombrePerfil.setError(getString(R.string.error_nombre_corto));
            etNombrePerfil.requestFocus();
            return;
        }

        // Si se introdujo un correo, verifica que cumpla el formato estándar de email
        if (!correo.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoPerfil.setError(getString(R.string.error_correo_formato));
            etCorreoPerfil.requestFocus();
            return;
        }

        // Guarda los datos actualizados en SharedPreferences bajo el UID del usuario
        String uid = user.getUid();
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        prefs.edit()
                .putString("nombre_" + uid, nombre)
                .putString("correo_" + uid, correo)
                .putString("telefono_" + uid, telefono)
                .apply();

        // Sincroniza nombre y avatar público en la colección "ranking"
        RankingManager.actualizarIdentidadPublica(nombre, fotoBase64Pendiente);

        // Prepara el mapa con los datos privados del usuario para guardarlos en Firestore
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombreUsuario", nombre);
        datosUsuario.put("correoContacto", correo);
        datosUsuario.put("telefonoContacto", telefono);

        // Actualiza el documento del usuario en la nube sin sobrescribir otros campos existentes
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .set(datosUsuario, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Notifica éxito al usuario y cierra la pantalla de perfil
                    Toast.makeText(this, getString(R.string.toast_perfil_actualizado), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        // Informa si hubo un error al sincronizar con Firestore
                        Toast.makeText(this, getString(R.string.toast_perfil_error_nube), Toast.LENGTH_SHORT).show()
                );
    }

    private void cerrarSesion() {
        // Cierra la sesión activa en Firebase Authentication
        FirebaseAuth.getInstance().signOut();
        // Prepara el Intent hacia LoginActivity limpiando todo el historial de pantallas
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Lanza la pantalla de inicio de sesión y finaliza PerfilActivity
        startActivity(intent);
        finish();
    }
}