// Definición del paquete donde se encuentra esta actividad
package es.medac.skycollectorapp.activities;

// Importación para el acceso al contexto de la aplicación
import android.content.Context;
// Importación para gestionar la navegación entre pantallas
import android.content.Intent;
// Importación para el almacenamiento persistente de datos del perfil
import android.content.SharedPreferences;
// Importación para la gestión de rutas de archivos y recursos
import android.net.Uri;
// Importación para el manejo del estado de la actividad
import android.os.Bundle;
// Importación para el componente visual de botón
import android.widget.Button;
// Importación para el campo de entrada de texto editable
import android.widget.EditText;
// Importación para el componente que visualiza imágenes
import android.widget.ImageView;
// Importación para el componente que visualiza texto estático
import android.widget.TextView;
// Importación para mostrar avisos rápidos al usuario
import android.widget.Toast;

// Importación para registrar el manejador de resultados de actividades
import androidx.activity.result.ActivityResultLauncher;
// Importación para usar contratos estándar de selección de contenido
import androidx.activity.result.contract.ActivityResultContracts;
// Importación para la creación de cuadros de diálogo emergentes
import androidx.appcompat.app.AlertDialog;
// Importación base para actividades con soporte de compatibilidad
import androidx.appcompat.app.AppCompatActivity;
// Importación para compartir archivos de forma segura mediante un proveedor
import androidx.core.content.FileProvider;

// Importación de la librería para la gestión eficiente de imágenes
import com.bumptech.glide.Glide;
// Importación del motor de autenticación de la plataforma en la nube
import com.google.firebase.auth.FirebaseAuth;
// Importación para representar la sesión del usuario activo
import com.google.firebase.auth.FirebaseUser;

// Importación para la representación de archivos en el sistema de ficheros
import java.io.File;

// Importación de los recursos del proyecto
import es.medac.skycollectorapp.R;

// Clase que gestiona la edición del perfil de usuario y la foto personalizada
public class PerfilActivity extends AppCompatActivity {

    // Variable para la vista de la foto de perfil
    private ImageView imgPerfil;
    // Variable para el campo de edición del nombre
    private EditText etNombre;
    // Variable para almacenar la dirección final de la foto elegida
    private Uri uriFotoSeleccionada;
    // Variable para almacenar la dirección provisional de la cámara
    private Uri uriFotoCamaraTemporal;
    // Variable para identificar al usuario actual
    private String userId;

    // Inicialización del lanzador para obtener imágenes desde la galería
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Comprobación de que se ha seleccionado un archivo
                if (uri != null) {
                    // Asignación de la dirección a la variable global
                    uriFotoSeleccionada = uri;
                    // Carga y recorte circular de la imagen en la vista
                    Glide.with(this).load(uri).circleCrop().into(imgPerfil);
                }
            }
    );

    // Inicialización del lanzador para capturar fotografías con la cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                // Comprobación de que la captura ha sido correcta
                if (exito) {
                    // Sincronización de la foto provisional con la seleccionada
                    uriFotoSeleccionada = uriFotoCamaraTemporal;
                    // Visualización de la foto capturada con formato circular
                    Glide.with(this).load(uriFotoSeleccionada).circleCrop().into(imgPerfil);
                }
            }
    );

    // Método principal que se ejecuta al crear la pantalla
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de creación superior
        super.onCreate(savedInstanceState);
        // Carga del diseño visual desde el fichero XML
        setContentView(R.layout.activity_perfil);

        // Obtención de la instancia del usuario identificado en Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Verificación de seguridad por si no hay sesión iniciada
        if (user == null) {
            // Expulsión del usuario a la pantalla de acceso
            cerrarSesion();
            return;
        }
        // Recuperación del código identificador del usuario
        userId = user.getUid();
        // Recuperación del correo electrónico de la cuenta
        String email = user.getEmail();

        // Localización de los componentes visuales en el diseño
        imgPerfil = findViewById(R.id.imgPerfilGrande);
        etNombre = findViewById(R.id.etNombreUsuario);
        TextView txtEmail = findViewById(R.id.txtEmailFijo);
        Button btnFoto = findViewById(R.id.btnCambiarFoto);
        Button btnGuardar = findViewById(R.id.btnGuardarPerfil);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Escritura del correo electrónico en su etiqueta correspondiente
        txtEmail.setText(email);
        // Recuperación de la información guardada previamente
        cargarDatos();

        // Configuración de la acción para el botón de cambio de imagen
        btnFoto.setOnClickListener(v -> mostrarDialogoSeleccion());

        // Configuración de la acción para el botón de confirmación de cambios
        btnGuardar.setOnClickListener(v -> guardarDatos());
        // Configuración de la acción para el botón de salida de sesión
        btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    // Método para desplegar las opciones de origen de la fotografía
    private void mostrarDialogoSeleccion() {
        // Definición de las etiquetas de las opciones disponibles
        String[] opciones = {"Hacer foto con Cámara", "Elegir de Galería"};

        // Creación del constructor para el cuadro de diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Asignación del encabezado del diálogo
        builder.setTitle("Cambiar foto de perfil");
        // Definición del comportamiento según la opción pulsada
        builder.setItems(opciones, (dialog, which) -> {
            // Selección de la cámara si se pulsa el primer índice
            if (which == 0) {
                abrirCamara();
            } else {
                // Selección de la galería en caso contrario
                abrirGaleria();
            }
        });
        // Activación visual del diálogo en pantalla
        builder.show();
    }

    // Método para disparar el selector de archivos del dispositivo
    private void abrirGaleria() {
        // Invocación del lanzador para tipos de archivos de imagen
        launcherGaleria.launch("image/*");
    }

    // Método para preparar el entorno y activar la cámara fotográfica
    private void abrirCamara() {
        // Generación de un enlace temporal para guardar la foto
        uriFotoCamaraTemporal = crearUriTemporal();
        // Verificación de que el enlace se ha creado correctamente
        if (uriFotoCamaraTemporal != null) {
            // Disparo del proceso de captura de imagen
            launcherCamara.launch(uriFotoCamaraTemporal);
        } else {
            // Aviso al usuario si falla la creación del fichero temporal
            Toast.makeText(this, "Error: No se pudo crear el archivo de foto", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para fabricar una dirección segura para el nuevo fichero de imagen
    private Uri crearUriTemporal() {
        try {
            // Definición de la ruta de almacenamiento en la caché externa
            File cachePath = new File(getExternalCacheDir(), "mis_fotos");
            // Creación de las carpetas necesarias si no existen
            if (!cachePath.exists()) cachePath.mkdirs();

            // Creación de la estructura del archivo con nombre basado en el tiempo actual
            File nuevoArchivo = new File(cachePath, "foto_" + System.currentTimeMillis() + ".jpg");

            // Generación de la URI segura mediante el proveedor de archivos declarado
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", nuevoArchivo);
        } catch (Exception e) {
            // Registro del error en caso de fallo técnico
            e.printStackTrace();
            return null;
        }
    }

    // Método para persistir la información del perfil en el almacenamiento local
    private void guardarDatos() {
        // Acceso al fichero de preferencias privadas de la aplicación
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        // Apertura del modo de edición de datos
        SharedPreferences.Editor editor = prefs.edit();

        // Almacenamiento del nombre introducido asociado al ID del usuario
        editor.putString("nombre_" + userId, etNombre.getText().toString());

        // Almacenamiento de la ruta de la foto si el usuario ha elegido una nueva
        if (uriFotoSeleccionada != null) {
            editor.putString("foto_" + userId, uriFotoSeleccionada.toString());
        }

        // Aplicación efectiva y permanente de los cambios
        editor.apply();
        // Notificación de confirmación al usuario
        Toast.makeText(this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show();
        // Cierre de la actividad actual para volver atrás
        finish();
    }

    // Método para recuperar y visualizar la información guardada del perfil
    private void cargarDatos() {
        // Acceso al fichero de preferencias locales
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", Context.MODE_PRIVATE);
        // Obtención del nombre guardado o una cadena vacía si no existe
        String nombreGuardado = prefs.getString("nombre_" + userId, "");
        // Asignación del nombre recuperado al campo de texto
        etNombre.setText(nombreGuardado);

        // Recuperación de la ruta de la foto guardada
        String fotoGuardada = prefs.getString("foto_" + userId, null);
        // Si existe una foto previa, se procede a su visualización
        if (fotoGuardada != null) {
            // Carga circular de la imagen mediante la librería Glide
            Glide.with(this).load(fotoGuardada).circleCrop().into(imgPerfil);
        }
    }

    // Método para desvincular la cuenta y reiniciar la navegación
    private void cerrarSesion() {
        // Petición de salida al sistema de autenticación de Firebase
        FirebaseAuth.getInstance().signOut();
        // Preparación del salto a la pantalla de acceso
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        // Configuración de banderas para limpiar el historial de navegación
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Inicio de la actividad de acceso
        startActivity(intent);
        // Cierre definitivo de la pantalla de perfil
        finish();
    }
}