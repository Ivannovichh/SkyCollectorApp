// Declaración del paquete donde se encuentra ubicada la actividad
package es.medac.skycollectorapp.activities;

// Importaciones de permisos y utilidades del sistema Android
import android.Manifest; // Definición de permisos de sistema (Cámara)
import android.content.Intent; // Objeto para intenciones y paso entre actividades
import android.content.SharedPreferences; // Almacenamiento persistente clave-valor local
import android.content.pm.PackageManager; // Gestor para comprobación de permisos de la app
import android.net.Uri; // Representación de direcciones URI para archivos e imágenes
import android.os.Bundle; // Pases de datos durante el ciclo de vida de la actividad
import android.os.Environment; // Directorios estándar del sistema de archivos
import android.util.Log; // Registro de mensajes de depuración en Logcat
import android.view.View; // Componente base de las vistas de interfaz
import android.widget.Button; // Componente de la interfaz tipo botón
import android.widget.EditText; // Componente de texto editable para apodos
import android.widget.ImageView; // Componente para mostrar imágenes
import android.widget.TextView; // Componente para mostrar textos estáticos
import android.widget.Toast; // Notificaciones emergentes breves en pantalla

// Importaciones de librerías de soporte AndroidX
import androidx.activity.result.ActivityResultLauncher; // Lanzador para recibir resultados de actividades
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar para fotos y permisos
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades con soporte ActionBar
import androidx.cardview.widget.CardView; // Tarjeta contenedora con elevación y bordes redondeados
import androidx.core.content.ContextCompat; // Métodos de compatibilidad para contextos y permisos
import androidx.core.content.FileProvider; // Proveedor seguro para compartir archivos con la cámara

// Importaciones de librerías externas
import com.bumptech.glide.Glide; // Carga y procesamiento eficiente de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Estrategias de caché en disco para Glide
import com.google.firebase.auth.FirebaseAuth; // Gestor de autenticación de usuarios de Firebase
import com.google.firebase.firestore.FirebaseFirestore; // Gestor de la base de datos Firestore en la nube
import com.google.gson.Gson; // Motor de conversión de objetos Java a formato JSON
import com.google.gson.reflect.TypeToken; // Preservación de tipos genéricos en Gson

// Importaciones estándar de Java
import java.io.File; // Manipulación de archivos en almacenamiento
import java.io.IOException; // Manejo de excepciones de entrada/salida
import java.lang.reflect.Type; // Tipo genérico para la deserialización con Gson
import java.text.SimpleDateFormat; // Formateador de fechas a cadenas de texto
import java.util.ArrayList; // Colección de lista dinámicamente redimensionable
import java.util.Date; // Representación de fecha y hora del sistema
import java.util.Locale; // Configuración regional de idioma

// Importaciones de recursos y modelos del proyecto
import es.medac.skycollectorapp.R; // Recursos del proyecto
import es.medac.skycollectorapp.models.Avion; // Modelo de datos del objeto Avión

// Clase principal que gestiona la visualización, edición de apodos y fotos personalizadas de un avión
public class DetalleAvionActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ
    // ------------------------------
    private ImageView imgDetalleGrande; // Imagen principal con la silueta o ilustración oficial del avión
    private ImageView imgFotoUsuario; // Imagen personalizada tomada o cargada por el usuario
    private EditText etNombreAvion; // Campo editable para cambiar el apodo personalizado de la aeronave

    private TextView txtDetalleFabricante, txtDetallePais, txtDetalleVelocidad,
            txtDetalleCapacidad, txtDetallePeso, txtDetalleDimensiones; // Etiquetas de la ficha técnica

    private Button btnCamara, btnGaleria; // Botones para capturar o elegir una fotografía
    private Button btnGuardar, btnCancelar; // Botones de confirmación o cancelación de edición

    private CardView layoutFotoUsuario; // Tarjeta contenedora de la fotografía del usuario

    // ------------------------------
    // VARIABLES DE DATOS
    // ------------------------------
    private Avion avion; // Objeto de tipo Avion cargado para edición
    private Uri uriFotoTemporal; // URI provisional generada para la toma con la cámara
    private String avionId; // Identificador único ID del avión seleccionado

    // ------------------------------
    // LANZADORES DE ACTIVIDADES
    // ------------------------------

    // Lanzador para capturar fotografías con la aplicación de cámara del dispositivo
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), // Contrato para tomar foto
            exito -> {
                if (exito && uriFotoTemporal != null) { // Si la captura fue correcta
                    mostrarFotoUsuario(uriFotoTemporal); // Cargar y visualizar la foto tomada
                }
            }
    );

    // Lanzador para solicitar permisos de sistema en tiempo de ejecución
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), // Contrato de solicitud de permiso
            isGranted -> {
                if (isGranted) abrirCamara(); // Si el permiso fue otorgado, abrir cámara
                else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show(); // Avisar si fue denegado
            }
    );

    // Lanzador para seleccionar archivos multimedia desde la galería
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(), // Contrato para seleccionar contenido
            uri -> {
                if (uri != null) { // Si el usuario eligió una imagen
                    try {
                        // Solicitar permiso de lectura persistente sobre la URI de la galería
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        e.printStackTrace(); // Registrar excepciones si el proveedor no soporta persistencia
                    }
                    mostrarFotoUsuario(uri); // Cargar y mostrar la imagen en la vista
                }
            }
    );

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Inicialización de la actividad
        super.onCreate(savedInstanceState); // Llamada al método padre
        setContentView(R.layout.activity_detalle_avion); // Cargar el diseño XML de detalle

        initViews(); // Vincular las vistas del layout con las variables locales

        avionId = getIntent().getStringExtra("avion_id"); // Recuperar el ID enviado en la intención

        if (avionId == null) { // Validar la existencia del parámetro
            Toast.makeText(this, "Error: no se recibió el ID del avión", Toast.LENGTH_SHORT).show(); // Avisar de error
            finish(); // Cerrar la pantalla
            return;
        }

        avion = buscarAvionPorId(avionId); // Búsqueda del objeto dentro de la colección local

        if (avion == null) { // Validar si el objeto existe
            Toast.makeText(this, "Error: avión no encontrado en la colección", Toast.LENGTH_SHORT).show(); // Avisar de error
            finish(); // Cerrar la pantalla
            return;
        }

        cargarDatos(); // Poblar las etiquetas con la información técnica del avión
        setupListeners(); // Configurar los eventos de clic en los botones
    }

    // Vincula las referencias de los componentes del archivo XML con las variables locales
    private void initViews() {
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande); // Imagen oficial grande
        etNombreAvion = findViewById(R.id.etNombreAvion); // Campo editable del apodo

        txtDetalleFabricante = findViewById(R.id.txtDetalleFabricante); // Etiqueta de fabricante
        txtDetallePais = findViewById(R.id.txtDetallePais); // Etiqueta de país de origen
        txtDetalleVelocidad = findViewById(R.id.txtDetalleVelocidad); // Etiqueta de velocidad
        txtDetalleCapacidad = findViewById(R.id.txtDetalleCapacidad); // Etiqueta de pasajeros
        txtDetallePeso = findViewById(R.id.txtDetallePeso); // Etiqueta de peso
        txtDetalleDimensiones = findViewById(R.id.txtDetalleDimensiones); // Etiqueta de dimensiones

        btnCamara = findViewById(R.id.btnCamara); // Botón para foto de cámara
        btnGaleria = findViewById(R.id.btnGaleria); // Botón para foto de galería

        btnCancelar = findViewById(R.id.btnCancelar); // Botón para cancelar
        btnGuardar = findViewById(R.id.btnGuardar); // Botón para guardar

        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario); // Contenedor de la foto del usuario
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario); // Vista de la foto del usuario
    }

    // Configura las acciones asociadas a los clics de los botones
    private void setupListeners() {

        btnCancelar.setOnClickListener(v -> finish()); // Cerrar la actividad sin guardar cambios

        btnGuardar.setOnClickListener(v -> {
            avion.setApodo(etNombreAvion.getText().toString().trim()); // Actualizar apodo en el objeto

            guardarCambiosEnPreferencias(avion); // Persistir cambios localmente en SharedPreferences
            guardarCambiosEnFirestore(avion); // Sincronizar los cambios actualizados en la nube Firestore

            Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show(); // Notificar al usuario
            finish(); // Cerrar la pantalla
        });

        btnCamara.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara(); // Abrir cámara si se cuenta con el permiso concedido
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA); // Solicitar permiso de cámara
            }
        });

        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*")); // Lanzar selector de la galería
    }

    // Rellena la interfaz gráfica con los datos del objeto Avion cargado
    private void cargarDatos() {
        etNombreAvion.setText(avion.getApodo()); // Mostrar el apodo o modelo actual

        // Asignar los textos formateados a las etiquetas técnicas
        txtDetalleFabricante.setText("Fabricante: " + avion.getFabricante());
        txtDetallePais.setText("País: " + avion.getPais());
        txtDetalleVelocidad.setText("Velocidad: " + avion.getVelocidad());
        txtDetalleCapacidad.setText("Capacidad: " + avion.getPasajeros());
        txtDetallePeso.setText("Peso: " + avion.getPeso());
        txtDetalleDimensiones.setText("Dimensiones: " + avion.getDimensiones());

        // Cargar la imagen o silueta del catálogo base con Glide
        Glide.with(this)
                .load(avion.getImagenResId()) // Cargar id del recurso
                .fitCenter() // Ajustar centrado
                .into(imgDetalleGrande); // Asignar al ImageView principal

        // Mostrar la foto del usuario si existe previamente guardada
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            layoutFotoUsuario.setVisibility(View.VISIBLE); // Hacer visible el contenedor

            Glide.with(this)
                    .load(Uri.parse(avion.getUriFotoUsuario())) // Cargar foto desde la URI guardada
                    .centerCrop() // Recortar centrado
                    .into(imgFotoUsuario); // Renderizar en la vista de foto de usuario
        } else {
            layoutFotoUsuario.setVisibility(View.GONE); // Ocultar si no hay foto personalizada
        }
    }

    // Refresca la vista y asigna la nueva foto seleccionada al objeto Avion
    private void mostrarFotoUsuario(Uri uri) {
        avion.setUriFotoUsuario(uri.toString()); // Asignar la nueva cadena de URI al objeto
        layoutFotoUsuario.setVisibility(View.VISIBLE); // Asegurar visibilidad del contenedor

        Glide.with(this)
                .load(uri) // Cargar la nueva URI
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Desactivar caché en disco para refresco inmediato
                .skipMemoryCache(true) // Omitir caché en memoria
                .centerCrop() // Recorte centrado
                .into(imgFotoUsuario); // Cargar en la vista
    }

    // Prepara el archivo privado y lanza la aplicación de cámara
    private void abrirCamara() {
        try {
            File archivo = crearArchivoImagen(); // Crear archivo temporal seguro en disco
            uriFotoTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo); // Generar la URI
            launcherCamara.launch(uriFotoTemporal); // Disparar la captura de la cámara
        } catch (Exception e) {
            Toast.makeText(this, "Error cámara: " + e.getMessage(), Toast.LENGTH_LONG).show(); // Informar de error
            e.printStackTrace(); // Imprimir traza de la excepción
        }
    }

    // Genera un archivo JPG temporal en el directorio persistente privado
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()); // Timestamp único
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // Directorio persistente privado
        if (dir != null && !dir.exists()) dir.mkdirs(); // Crear directorios si no existen

        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", dir); // Devolver el nuevo archivo temporal
    }

    // ------------------------------
    // PERSISTENCIA Y SINCRONIZACIÓN
    // ------------------------------

    // Busca una aeronave dentro del almacenamiento JSON local por su identificador ID
    private Avion buscarAvionPorId(String id) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE); // Leer preferencias
        String json = prefs.getString("lista_aviones", null); // Leer la cadena JSON
        if (json == null) return null; // Salir si está vacía

        Gson gson = new Gson(); // Instanciar conversor Gson
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType(); // Definir tipo para la deserialización
        ArrayList<Avion> lista = gson.fromJson(json, type); // Deserializar la lista
        if (lista == null) return null; // Salir si la lista es nula

        for (Avion a : lista) { // Recorrer los aviones guardados
            if (a.getId() != null && a.getId().equals(id)) {
                return a; // Devolver el objeto coincidente
            }
        }
        return null; // Retornar nulo si no se encuentra
    }

    // Actualiza la versión modificada del avión en la lista de SharedPreferences local
    private void guardarCambiosEnPreferencias(Avion avionActualizado) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE); // Leer preferencias
        Gson gson = new Gson(); // Instanciar conversor

        String json = prefs.getString("lista_aviones", null); // Obtener JSON existente
        if (json == null) return;

        Type type = new TypeToken<ArrayList<Avion>>() {}.getType(); // Definir el tipo
        ArrayList<Avion> lista = gson.fromJson(json, type); // Deserializar la lista
        if (lista == null) return;

        for (int i = 0; i < lista.size(); i++) { // Recorrer la lista para sustituir el elemento modificado
            if (lista.get(i).getId().equals(avionActualizado.getId())) {
                lista.set(i, avionActualizado); // Reemplazar el objeto antiguo por el actualizado
                break;
            }
        }

        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply(); // Guardar la lista actualizada en JSON
    }

    // Sincroniza las modificaciones del avión en la base de datos Firestore en la nube
    private void guardarCambiosEnFirestore(Avion avionActualizado) {
        FirebaseAuth auth = FirebaseAuth.getInstance(); // Obtener la instancia de autenticación
        if (auth.getCurrentUser() == null) return; // Salir si no hay un usuario autenticado

        String uid = auth.getCurrentUser().getUid(); // Obtener el UID del usuario activo

        // Actualizar el documento en la ruta: usuarios / {uid} / aviones / {id}
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avionActualizado.getId())
                .set(avionActualizado) // Sobrescribir el documento actualizado en Firestore
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Cambios del avión actualizados en Firestore: " + avionActualizado.getApodo())) // Log de éxito
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al actualizar cambios en Firestore", e)); // Log de error
    }
}