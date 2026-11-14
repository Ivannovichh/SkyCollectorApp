// Declaración del paquete donde se ubica la actividad de perfil de usuario
package es.medac.skycollectorapp.activities;

// Importaciones del sistema Android
import android.content.Intent; // Objeto para navegación entre pantallas
import android.content.SharedPreferences; // Almacenamiento local persistente
import android.net.Uri; // Manejo de rutas URI de imágenes
import android.os.Bundle; // Contenedor de estado en creación de actividad
import android.util.Patterns; // Validación de formato de correo electrónico
import android.widget.Button; // Componente visual de botón
import android.widget.EditText; // Campo de entrada de texto
import android.widget.ImageButton; // Botón con icono
import android.widget.ImageView; // Componente para mostrar imágenes
import android.widget.Toast; // Mensajes emergentes breves

// Importaciones de AndroidX y librerías externas
import androidx.activity.result.ActivityResultLauncher; // Lanzador para obtener resultados de galería
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar de selección de contenido
import androidx.appcompat.app.AppCompatActivity; // Clase base de actividad compatible
import androidx.cardview.widget.CardView; // Tarjeta con bordes redondeados

// Importaciones de Glide y Firebase
import com.bumptech.glide.Glide; // Carga y recorte circular de imágenes
import com.google.firebase.auth.FirebaseAuth; // Gestor de autenticación de Firebase
import com.google.firebase.auth.FirebaseUser; // Objeto del usuario autenticado
import com.google.firebase.firestore.FirebaseFirestore; // Cliente de base de datos Cloud Firestore
import com.google.firebase.firestore.SetOptions; // Opciones de combinación de datos en Firestore

// Importaciones estándar de Java y recursos del proyecto
import java.util.HashMap; // Estructura clave-valor para documentos Firestore
import java.util.Map; // Interfaz genérica de mapas
import es.medac.skycollectorapp.R; // Identificadores de recursos XML

// Actividad que permite editar el nombre público y los datos privados de contacto del piloto
public class PerfilActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ
    // ------------------------------
    private ImageView imgFotoPerfil; // Vista de imagen del avatar del usuario
    private EditText etNombrePerfil; // Campo de texto para el nombre de usuario público
    private EditText etCorreoPerfil; // Campo de texto para el correo electrónico privado
    private EditText etTelefonoPerfil; // Campo de texto para el teléfono privado

    // ------------------------------
    // SELECTOR DE GALERÍA
    // ------------------------------

    // Lanzador que abre la galería del dispositivo y guarda la foto elegida
    private final ActivityResultLauncher<String> selectorImagen = registerForActivityResult(
            new ActivityResultContracts.GetContent(), // Contrato para seleccionar un archivo multimedia
            uri -> { // Callback ejecutado al volver de la galería
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener usuario actual
                if (uri != null && user != null) { // Verificar que se eligió una imagen y hay sesión
                    try { // Intentar solicitar permiso persistente de lectura sobre la imagen
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); // Conceder permiso
                    } catch (SecurityException ignored) { // Ignorar si el proveedor no admite permisos persistentes
                    }

                    SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias
                    prefs.edit().putString("foto_" + user.getUid(), uri.toString()).apply(); // Guardar URI asociada al UID

                    Glide.with(this) // Cargar la nueva imagen con Glide
                            .load(uri) // Origen URI seleccionada
                            .circleCrop() // Aplicar recorte circular
                            .into(imgFotoPerfil); // Mostrar en el ImageView
                }
            }
    );

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Inicialización de la pantalla de perfil
        super.onCreate(savedInstanceState); // Llamada al método padre
        setContentView(R.layout.activity_perfil); // Cargar layout XML de perfil

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener sesión activa
        if (user == null) { // Si no hay usuario autenticado
            finish(); // Cerrar pantalla
            return; // Detener ejecución
        }

        // Vincular componentes visuales del layout con variables Java
        ImageButton btnVolver = findViewById(R.id.btnVolverPerfil); // Botón de regreso
        CardView cardFoto = findViewById(R.id.cardFotoPerfil); // Tarjeta de foto
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil); // Imagen de avatar
        etNombrePerfil = findViewById(R.id.etNombrePerfil); // Campo nombre público
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil); // Campo correo privado
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil); // Campo teléfono privado
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil); // Botón guardar cambios
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion); // Botón cerrar sesión

        cargarDatosPerfil(user); // Rellenar campos con los datos actuales del usuario

        // Asignar eventos de clic a los controles interactivos
        btnVolver.setOnClickListener(v -> finish()); // Volver a MainActivity
        cardFoto.setOnClickListener(v -> selectorImagen.launch("image/*")); // Abrir selector de imágenes
        btnGuardar.setOnClickListener(v -> guardarCambiosPerfil(user)); // Guardar datos actualizados
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion()); // Cerrar sesión en Firebase
    }

    // ------------------------------
    // CARGA Y GUARDADO DE PERFIL
    // ------------------------------

    // Rellena los campos de nombre, correo, teléfono y foto desde SharedPreferences y FirebaseAuth
    private void cargarDatosPerfil(FirebaseUser user) {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias de perfil
        String uid = user.getUid(); // Obtener UID único del usuario

        // Recuperar datos locales usando como respaldo los datos del proveedor de autenticación
        String nombre = prefs.getString("nombre_" + uid, ""); // Nombre público elegido por el usuario
        String correo = prefs.getString("correo_" + uid, user.getEmail() != null ? user.getEmail() : ""); // Correo
        String telefono = prefs.getString("telefono_" + uid, user.getPhoneNumber() != null ? user.getPhoneNumber() : ""); // Teléfono
        String foto = prefs.getString("foto_" + uid, null); // URI de foto de perfil

        etNombrePerfil.setText(nombre); // Mostrar nombre de usuario
        etCorreoPerfil.setText(correo); // Mostrar correo electrónico
        etTelefonoPerfil.setText(telefono); // Mostrar teléfono de contacto

        if (foto != null) { // Si existe fotografía guardada
            Glide.with(this) // Inicializar Glide
                    .load(Uri.parse(foto)) // Cargar URI
                    .placeholder(android.R.drawable.ic_menu_myplaces) // Icono mientras carga
                    .error(android.R.drawable.ic_menu_myplaces) // Icono si falla
                    .circleCrop() // Recorte circular
                    .into(imgFotoPerfil); // Asignar al ImageView
        }
    }

    // Valida y guarda los datos en almacenamiento local, documento privado de usuario y colección pública de ranking
    private void guardarCambiosPerfil(FirebaseUser user) {
        String nombre = etNombrePerfil.getText().toString().trim(); // Leer nombre de usuario
        String correo = etCorreoPerfil.getText().toString().trim(); // Leer correo de contacto
        String telefono = etTelefonoPerfil.getText().toString().trim(); // Leer teléfono de contacto

        if (nombre.length() < 3) { // Validar longitud mínima del indicativo público
            etNombrePerfil.setError("El nombre de usuario debe tener al menos 3 caracteres"); // Mostrar aviso
            etNombrePerfil.requestFocus(); // Enfocar campo
            return; // Detener guardado
        }

        if (!correo.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) { // Validar formato si se escribió correo
            etCorreoPerfil.setError("Formato de correo no válido"); // Mostrar aviso
            etCorreoPerfil.requestFocus(); // Enfocar campo
            return; // Detener guardado
        }

        String uid = user.getUid(); // Obtener UID único
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias
        prefs.edit()
                .putString("nombre_" + uid, nombre) // Guardar nombre público en local
                .putString("correo_" + uid, correo) // Guardar correo privado en local
                .putString("telefono_" + uid, telefono) // Guardar teléfono privado en local
                .apply(); // Confirmar escritura local

        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Obtener instancia de Firestore

        // 1. Guardar todos los datos en el documento privado del usuario
        Map<String, Object> datosUsuario = new HashMap<>(); // Mapa de datos completos del perfil
        datosUsuario.put("nombreUsuario", nombre); // Guardar nombre
        datosUsuario.put("correoContacto", correo); // Guardar correo privado
        datosUsuario.put("telefonoContacto", telefono); // Guardar teléfono privado

        db.collection("usuarios") // Colección privada de usuarios
                .document(uid) // Documento del usuario
                .set(datosUsuario, SetOptions.merge()); // Fusionar cambios

        // 2. Actualizar únicamente el nombre público en la colección 'ranking' para proteger privacidad
        Map<String, Object> datosRanking = new HashMap<>(); // Mapa exclusivo de datos públicos
        datosRanking.put("nombreUsuario", nombre); // Actualizar indicativo visible en el ranking

        db.collection("ranking") // Colección pública del ranking
                .document(uid) // Documento del usuario
                .set(datosRanking, SetOptions.merge()) // Actualizar nombre público
                .addOnSuccessListener(aVoid -> { // Al completarse la sincronización
                    Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show(); // Notificar éxito
                    finish(); // Cerrar pantalla de perfil y volver a MainActivity
                })
                .addOnFailureListener(e -> // Si no hubo conexión a internet
                        Toast.makeText(this, "Guardado localmente, error al sincronizar nube", Toast.LENGTH_SHORT).show() // Avisar
                );
    }

    // Cierra la sesión actual de Firebase y redirige a la pantalla de inicio de sesión
    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut(); // Desconectar usuario en Firebase Auth
        Intent intent = new Intent(this, LoginActivity.class); // Preparar navegación al login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpiar historial de pantallas
        startActivity(intent); // Abrir LoginActivity
        finish(); // Finalizar PerfilActivity
    }
}