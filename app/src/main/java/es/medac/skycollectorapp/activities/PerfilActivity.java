// Declaración del paquete donde se ubica la actividad de perfil
package es.medac.skycollectorapp.activities;

// Importaciones de Android necesarias para la gestión de la actividad y permisos
import android.Manifest; // Permisos del sistema para la cámara
import android.content.Context; // Contexto de la aplicación
import android.content.Intent; // Navegación entre actividades
import android.content.SharedPreferences; // Almacenamiento persistente clave-valor
import android.content.pm.PackageManager; // Comprobación de permisos concedidos
import android.net.Uri; // Representación de direcciones URI para recursos e imágenes
import android.os.Bundle; // Pases de datos durante la creación de la actividad
import android.os.Environment; // Acceso a directorios estándar del sistema
import android.widget.Button; // Componente de botón de la interfaz
import android.widget.EditText; // Componente de campo de texto editable
import android.widget.ImageView; // Componente para visualizar imágenes
import android.widget.TextView; // Componente para mostrar texto estático
import android.widget.Toast; // Notificaciones flotantes breves

// Importaciones de AndroidX
import androidx.activity.result.ActivityResultLauncher; // Lanzadores de resultados de actividades
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar para fotos y galería
import androidx.appcompat.app.AlertDialog; // Cuadros de diálogo emergentes
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades con soporte de ActionBar
import androidx.core.content.ContextCompat; // Comprobaciones de compatibilidad de contexto
import androidx.core.content.FileProvider; // Proveedor seguro para compartir archivos entre apps

// Importaciones de librerías externas
import com.bumptech.glide.Glide; // Carga y procesado eficiente de imágenes
import com.google.firebase.auth.FirebaseAuth; // Gestor de autenticación de Firebase
import com.google.firebase.auth.FirebaseUser; // Usuario autenticado en Firebase

// Importaciones estándar de Java
import java.io.File; // Manipulación de archivos del sistema

// Importación de recursos del proyecto
import es.medac.skycollectorapp.R;

// Clase que gestiona la edición del perfil de usuario y la foto personalizada de perfil
public class PerfilActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ
    // ------------------------------
    private ImageView imgPerfil; // Vista para mostrar la foto de perfil
    private EditText etNombre; // Campo editable para cambiar el nombre de usuario

    // ------------------------------
    // VARIABLES DE IMAGEN Y USUARIO
    // ------------------------------
    private Uri uriFotoSeleccionada; // URI final de la foto elegida (cámara o galería)
    private Uri uriFotoCamaraTemporal; // URI provisional generada para la captura de la cámara
    private String userId; // Identificador único UID del usuario en Firebase

    // ------------------------------
    // LANZADORES DE ACTIVIDADES (RESULTS)
    // ------------------------------

    // Lanzador para seleccionar una imagen desde la galería del dispositivo
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(), // Contrato para obtener contenido
            uri -> {
                if (uri != null) { // Si el usuario selecciona una foto
                    uriFotoSeleccionada = uri; // Guardar la URI seleccionada
                    Glide.with(this).load(uri).circleCrop().into(imgPerfil); // Cargarla con recorte circular
                }
            }
    );

    // Lanzador para capturar una fotografía directamente con la cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), // Contrato para tomar foto
            exito -> {
                if (exito) { // Si la fotografía se capturó correctamente
                    uriFotoSeleccionada = uriFotoCamaraTemporal; // Asignar la URI temporal como definitiva
                    Glide.with(this).load(uriFotoSeleccionada).circleCrop().into(imgPerfil); // Cargarla circular
                }
            }
    );

    // Lanzador para solicitar el permiso de cámara al usuario
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), // Contrato de petición de permisos
            isGranted -> {
                if (isGranted) abrirCamara(); // Si se concede el permiso, abrir la cámara
                else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show(); // Avisar si se rechaza
            }
    );

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método de inicialización de la actividad
        super.onCreate(savedInstanceState); // Llamada al método base
        setContentView(R.layout.activity_perfil); // Cargar el diseño XML del perfil

        // Obtener el usuario autenticado en Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { // Si no hay usuario activo, expulsar a la pantalla de login
            cerrarSesion();
            return;
        }
        userId = user.getUid(); // Recuperar el UID del usuario
        String email = user.getEmail(); // Recuperar el correo del usuario

        // Vincular los componentes visuales del XML con las variables
        imgPerfil = findViewById(R.id.imgPerfilGrande); // Imagen grande de perfil
        etNombre = findViewById(R.id.etNombreUsuario); // Campo de texto para el nombre
        TextView txtEmail = findViewById(R.id.txtEmailFijo); // Etiqueta estática para el correo
        Button btnFoto = findViewById(R.id.btnCambiarFoto); // Botón para cambiar foto
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil); // Botón para guardar cambios
        Button btnLogout = findViewById(R.id.btnLogout); // Botón para cerrar sesión

        txtEmail.setText(email); // Mostrar el correo en la interfaz
        cargarDatos(); // Cargar nombre y foto previa si existen

        // Asignar escuchadores a los botones de la interfaz
        btnFoto.setOnClickListener(v -> mostrarDialogoSeleccion()); // Desplegar opciones de foto
        btnGuardar.setOnClickListener(v -> guardarDatos()); // Guardar datos actualizados
        btnLogout.setOnClickListener(v -> cerrarSesion()); // Cerrar la sesión activa
    }

    // ------------------------------
    // LÓGICA DE FOTOGRAFÍA
    // ------------------------------

    // Muestra un cuadro de diálogo emergente para elegir entre cámara o galería
    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Hacer foto con Cámara", "Elegir de Galería"}; // Opciones del diálogo

        AlertDialog.Builder builder = new AlertDialog.Builder(this); // Construir el diálogo
        builder.setTitle("Cambiar foto de perfil"); // Título del cuadro emergente
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) { // Si se pulsa "Cámara"
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara(); // Abrir cámara si ya tenemos permiso
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA); // Solicitar permiso
                }
            } else { // Si se pulsa "Galería"
                abrirGaleria();
            }
        });
        builder.show(); // Mostrar el cuadro de diálogo
    }

    // Lanza el selector de archivos para elegir imágenes de la galería
    private void abrirGaleria() {
        launcherGaleria.launch("image/*"); // Filtrar solo imágenes
    }

    // Prepara el archivo de destino y activa la cámara
    private void abrirCamara() {
        uriFotoCamaraTemporal = crearUriTemporal(); // Crear un archivo temporal para alojar la imagen
        if (uriFotoCamaraTemporal != null) {
            launcherCamara.launch(uriFotoCamaraTemporal); // Lanzar cámara indicando la URI de destino
        } else {
            Toast.makeText(this, "Error: No se pudo crear el archivo de foto", Toast.LENGTH_SHORT).show();
        }
    }

    // Crea un archivo seguro en almacenamiento persistente para evitar que se pierda la foto
    private Uri crearUriTemporal() {
        try {
            // Utilizar el directorio de imágenes persistente de la aplicación
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir != null && !dir.exists()) dir.mkdirs(); // Crear carpetas si no existen

            // Crear un archivo temporal único con timestamp
            File nuevoArchivo = File.createTempFile(
                    "perfil_" + System.currentTimeMillis() + "_", // Prefijo único
                    ".jpg", // Extensión de imagen
                    dir // Directorio de destino
            );

            // Generar URI segura mediante FileProvider
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", nuevoArchivo);
        } catch (Exception e) {
            e.printStackTrace(); // Registrar excepciones si ocurren
            return null;
        }
    }

    // ------------------------------
    // PERSISTENCIA DE DATOS
    // ------------------------------

    // Guarda el nombre de usuario y la URI de la foto en SharedPreferences
    private void guardarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE); // Obtener SharedPreferences
        SharedPreferences.Editor editor = prefs.edit(); // Abrir el editor de datos

        // Guardar el nombre asociándolo al ID único del usuario
        editor.putString("nombre_" + userId, etNombre.getText().toString().trim());

        // Guardar la URI de la foto solo si se ha seleccionado una nueva
        if (uriFotoSeleccionada != null) {
            editor.putString("foto_" + userId, uriFotoSeleccionada.toString());
        }

        editor.apply(); // Aplicar cambios de forma asíncrona
        Toast.makeText(this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show(); // Avisar al usuario
        finish(); // Volver a la pantalla anterior
    }

    // Carga los datos almacenados del usuario en la interfaz
    private void cargarDatos() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE); // Leer preferencias
        String nombreGuardado = prefs.getString("nombre_" + userId, ""); // Recuperar nombre guardado
        etNombre.setText(nombreGuardado); // Asignar al campo de texto

        String fotoGuardada = prefs.getString("foto_" + userId, null); // Recuperar URI de foto guardada
        if (fotoGuardada != null) {
            Glide.with(this).load(fotoGuardada).circleCrop().into(imgPerfil); // Mostrar foto cargada con corte circular
        }
    }

    // Cierra la sesión activa del usuario y regresa a la pantalla de login
    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut(); // Cerrar sesión en Firebase
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class); // Intención para abrir Login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Limpiar pila de pantallas
        startActivity(intent); // Iniciar LoginActivity
        finish(); // Finalizar PerfilActivity
    }
}