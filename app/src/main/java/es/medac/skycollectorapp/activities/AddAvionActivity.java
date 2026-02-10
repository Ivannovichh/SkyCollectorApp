package es.medac.skycollectorapp.activities; // Definición del paquete donde se encuentra la actividad

// Importaciones de clases de Android necesarias para la funcionalidad
import android.Manifest; // Permisos del sistema, aquí usado para la cámara
import android.content.Context; // Contexto de la aplicación
import android.content.SharedPreferences; // Para almacenar y recuperar datos persistentes
import android.content.pm.PackageManager; // Comprobación de permisos de la aplicación
import android.net.Uri; // Representación de URIs de archivos o recursos
import android.os.Bundle; // Contenedor para pasar datos al crear actividades
import android.os.Environment; // Para acceder al almacenamiento externo
import android.util.Log; // Para imprimir logs de depuración
import android.widget.ArrayAdapter; // Adaptador para mostrar listas en Spinner
import android.widget.Button; // Elemento de interfaz de tipo botón
import android.widget.ImageView; // Elemento de interfaz para mostrar imágenes
import android.widget.Spinner; // Elemento de interfaz tipo lista desplegable
import android.widget.Toast; // Para mostrar mensajes emergentes cortos

// Importaciones de librerías de AndroidX
import androidx.activity.result.ActivityResultLauncher; // Para lanzar actividades y recibir resultados
import androidx.activity.result.contract.ActivityResultContracts; // Contratos para obtener contenido o tomar fotos
import androidx.appcompat.app.AlertDialog; // Para crear cuadros de diálogo
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades con compatibilidad de ActionBar
import androidx.core.content.ContextCompat; // Para acceder a recursos y permisos de manera segura
import androidx.core.content.FileProvider; // Para compartir archivos entre la app y la cámara

// Importaciones de librerías externas
import com.bumptech.glide.Glide; // Biblioteca para cargar imágenes de manera eficiente
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Estrategia de cache de Glide
import com.google.firebase.auth.FirebaseAuth; // Para autenticación de usuario con Firebase
import com.google.firebase.firestore.FirebaseFirestore; // Para guardar y recuperar datos en Firestore
import com.google.gson.Gson; // Biblioteca para convertir objetos a JSON y viceversa
import com.google.gson.reflect.TypeToken; // Para obtener el tipo de listas genéricas en Gson

// Importaciones de Java estándar
import java.io.File; // Para trabajar con archivos
import java.io.IOException; // Excepción lanzada por operaciones de archivo
import java.lang.reflect.Type; // Representación de tipos genéricos para Gson
import java.text.SimpleDateFormat; // Para dar formato a fechas
import java.util.ArrayList; // Lista dinámica de elementos
import java.util.Date; // Representación de fecha y hora
import java.util.List; // Interfaz para listas
import java.util.Locale; // Para especificar configuraciones regionales

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos del proyecto (layouts, drawables, strings)
import es.medac.skycollectorapp.models.Avion; // Clase modelo de Avión
import es.medac.skycollectorapp.utils.AvionGenerator; // Clase que genera datos de aviones de ejemplo

// Clase principal de la actividad para añadir aviones al catálogo
public class AddAvionActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ
    // ------------------------------
    private Spinner spinnerAviones; // Spinner que muestra los modelos de aviones disponibles
    private ImageView imgPreviewAvion; // ImageView para mostrar la foto del avión
    private Button btnSubirFoto, btnGuardar, btnCancelar; // Botones de interacción con el usuario

    // ------------------------------
    // VARIABLES DE IMAGEN
    // ------------------------------
    private Uri uriImagenFinal; // URI final de la imagen seleccionada o tomada
    private Uri uriFotoCamaraTemporal; // URI temporal usado para la foto de cámara

    // ------------------------------
    // LISTAS Y DATOS
    // ------------------------------
    private List<Avion> listaAvionesBase; // Lista de aviones base generados por AvionGenerator

    private String icaoSeleccionado; // ICAO del avión seleccionado previamente en el mapa

    // ------------------------------
    // MÉTODOS PRINCIPALES
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método llamado al crear la actividad
        super.onCreate(savedInstanceState); // Llamada al método padre
        setContentView(R.layout.activity_add_avion); // Cargar el layout asociado a esta actividad

        // Inicializar referencias a los elementos de la interfaz
        spinnerAviones = findViewById(R.id.spinnerAviones); // Spinner de aviones
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion); // ImageView de previsualización
        btnSubirFoto = findViewById(R.id.btnSubirFoto); // Botón para subir foto
        btnGuardar = findViewById(R.id.btnGuardar); // Botón para guardar avión
        btnCancelar = findViewById(R.id.btnCancelar); // Botón para cancelar acción

        // Recuperar ICAO seleccionado previamente usando SharedPreferences
        SharedPreferences prefs =
                getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        icaoSeleccionado = prefs.getString("icao_seleccionado", null); // Leer ICAO si existe

        // Mostrar mensaje si no hay avión seleccionado
        if (icaoSeleccionado == null) {
            Toast.makeText(this,
                    "No has seleccionado ningún avión en el mapa",
                    Toast.LENGTH_LONG).show(); // Mostrar Toast largo
        }

        cargarDatosGenerator(); // Llenar el spinner con los aviones disponibles

        // Asignar listeners a los botones
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion()); // Mostrar opciones de foto
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo()); // Guardar avión seleccionado
        btnCancelar.setOnClickListener(v -> finish()); // Cerrar la actividad sin guardar
    }

    // Método para cargar los datos de aviones en el spinner
    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones(); // Obtener lista completa de aviones
        List<String> nombres = new ArrayList<>(); // Crear lista de nombres para mostrar en el spinner

        // Recorrer cada avión y agregar su modelo y rareza a la lista de nombres
        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")");
        }

        // Configurar adaptador del spinner con la lista de nombres
        spinnerAviones.setAdapter(
                new ArrayAdapter<>(
                        this, // Contexto actual
                        android.R.layout.simple_spinner_dropdown_item, // Layout predeterminado del spinner
                        nombres // Lista de elementos a mostrar
                )
        );
    }

    // Método para guardar el avión en el catálogo
    private void guardarAvionCatalogo() {

        // Validar que se haya seleccionado un avión en el mapa
        if (icaoSeleccionado == null) {
            Toast.makeText(this,
                    "Primero selecciona un avión en el mapa",
                    Toast.LENGTH_SHORT).show();
            return; // Salir del método si no hay ICAO seleccionado
        }

        int pos = spinnerAviones.getSelectedItemPosition(); // Obtener posición seleccionada en spinner
        if (pos < 0) return; // Salir si no hay selección

        Avion base = listaAvionesBase.get(pos); // Obtener avión base correspondiente

        // Crear nuevo objeto Avion combinando datos base y ICAO seleccionado
        Avion nuevo = new Avion(
                base.getModelo(), // Modelo del avión
                base.getFabricante(), // Fabricante
                base.getRareza(), // Rareza
                base.getImagenResId(), // ID de imagen del recurso
                base.getVelocidad(), // Velocidad máxima
                base.getPasajeros(), // Capacidad de pasajeros
                base.getDimensiones(), // Dimensiones físicas
                base.getPais(), // País de origen
                base.getPeso(), // Peso del avión
                icaoSeleccionado // ICAO real seleccionado
        );

        // Asignar foto del usuario si existe
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString()); // Convertir URI a String
        }

        // Guardar avión en preferencias locales y Firestore
        guardarEnPreferencias(nuevo);
        guardarEnFirestore(nuevo);

        // Limpiar ICAO usado en SharedPreferences
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .remove("icao_seleccionado")
                .apply();

        // Notificar al usuario que el avión se guardó correctamente
        Toast.makeText(this,
                "Avistamiento guardado correctamente",
                Toast.LENGTH_SHORT).show();

        finish(); // Cerrar actividad
    }

    // Guardar avión en SharedPreferences
    private void guardarEnPreferencias(Avion nuevo) {
        SharedPreferences prefs =
                getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE); // Obtener SharedPreferences
        Gson gson = new Gson(); // Crear objeto Gson para conversión JSON

        // Recuperar lista existente de aviones en formato JSON
        String json = prefs.getString("lista_aviones", null);
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType(); // Tipo para conversión genérica

        // Convertir JSON a lista de objetos Avion o crear nueva lista vacía
        List<Avion> lista = json != null
                ? gson.fromJson(json, type)
                : new ArrayList<>();

        if (lista == null) lista = new ArrayList<>(); // Asegurar que no sea null

        lista.add(nuevo); // Agregar nuevo avión
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply(); // Guardar lista como JSON
    }

    // Guardar avión en Firestore
    private void guardarEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance(); // Obtener instancia de autenticación
        if (auth.getCurrentUser() == null) return; // Salir si no hay usuario logueado

        // Guardar avión en colección del usuario
        FirebaseFirestore.getInstance()
                .collection("usuarios") // Colección de usuarios
                .document(auth.getCurrentUser().getUid()) // Documento del usuario actual
                .collection("aviones") // Subcolección de aviones
                .document(avion.getId()) // Documento con ID del avión
                .set(avion) // Guardar objeto Avion
                .addOnSuccessListener(v ->
                        Log.d("FIRESTORE", "Avión guardado")) // Log en caso de éxito
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE", "Error", e)); // Log en caso de fallo
    }

    // Mostrar diálogo para seleccionar fuente de imagen
    private void mostrarDialogoSeleccion() {
        String[] opciones = {"Cámara", "Galería"}; // Opciones disponibles

        new AlertDialog.Builder(this)
                .setTitle("Añadir foto") // Título del diálogo
                .setItems(opciones, (d, i) -> { // Acción al seleccionar opción
                    if (i == 0) abrirCamara(); // Abrir cámara
                    else launcherGaleria.launch("image/*"); // Abrir galería
                })
                .show(); // Mostrar diálogo
    }

    // Lanzador para obtener imagen desde galería
    private final ActivityResultLauncher<String> launcherGaleria =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(), // Contrato para seleccionar contenido
                    uri -> {
                        if (uri != null) cargarImagen(uri); // Cargar imagen si no es null
                    });

    // Lanzador para tomar foto con cámara
    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(), // Contrato para tomar foto
                    ok -> {
                        if (ok && uriFotoCamaraTemporal != null) {
                            cargarImagen(uriFotoCamaraTemporal); // Cargar foto tomada
                        }
                    });

    // Abrir cámara y preparar archivo temporal
    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) return; // Comprobar permiso

        try {
            File f = crearArchivoImagen(); // Crear archivo temporal para foto
            uriFotoCamaraTemporal = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", f); // Obtener URI para cámara
            launcherCamara.launch(uriFotoCamaraTemporal); // Lanzar cámara
        } catch (Exception e) {
            e.printStackTrace(); // Imprimir error si ocurre
        }
    }

    // Crear archivo temporal para la foto de cámara
    private File crearArchivoImagen() throws IOException {
        String ts = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()); // Crear timestamp único

        File dir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES); // Directorio para almacenar imagen

        return File.createTempFile("IMG_" + ts, ".jpg", dir); // Crear archivo temporal
    }

    // Cargar imagen en ImageView usando Glide
    private void cargarImagen(Uri uri) {
        uriImagenFinal = uri; // Guardar URI final
        Glide.with(this)
                .load(uri) // Cargar imagen desde URI
                .centerCrop() // Ajustar recorte
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Evitar cache en disco
                .skipMemoryCache(true) // Evitar cache en memoria
                .into(imgPreviewAvion); // Mostrar en ImageView
    }
}
