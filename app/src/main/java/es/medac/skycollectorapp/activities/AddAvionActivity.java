// Declaración del paquete donde se encuentra la clase
package es.medac.skycollectorapp.activities;

// Importación de dependencias necesarias para el funcionamiento de la actividad
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.utils.AvionGenerator;

// Definición de la clase principal que hereda de AppCompatActivity
public class AddAvionActivity extends AppCompatActivity {

    // Declaración de variables para los componentes de la interfaz de usuario
    private Spinner spinnerAviones;
    private ImageView imgPreviewAvion;
    private Button btnSubirFoto, btnGuardar, btnCancelar;

    // Declaración de variables para gestionar las rutas de las imágenes (URIs)
    private Uri uriImagenFinal;
    private Uri uriFotoCamaraTemporal;

    // Lista que almacenará los datos base de los aviones
    private List<Avion> listaAvionesBase;

    // Método que se ejecuta al crear la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la implementación de la clase padre
        super.onCreate(savedInstanceState);
        // Establece el diseño XML asociado a esta actividad
        setContentView(R.layout.activity_add_avion);

        // Vinculación de las variables con sus respectivos elementos en el XML
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Llama al método para cargar los datos en el selector
        cargarDatosGenerator();

        // Configura la acción al pulsar el botón de subir foto
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        // Configura la acción al pulsar el botón de guardar
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo());
        // Configura la acción al pulsar el botón cancelar para cerrar la pantalla
        btnCancelar.setOnClickListener(v -> finish());
    }

    // Método para rellenar el Spinner con datos de una clase generadora
    private void cargarDatosGenerator() {
        // Obtiene la lista completa de aviones desde la utilidad AvionGenerator
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        // Crea una lista de strings para mostrar los nombres en la interfaz
        List<String> nombres = new ArrayList<>();

        // Recorre la lista de aviones para dar formato al nombre y rareza
        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")");
        }

        // Crea un adaptador para vincular los nombres con el componente Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres);
        // Asigna el adaptador configurado al Spinner
        spinnerAviones.setAdapter(adapter);
    }

    // Método principal para procesar y guardar el avión seleccionado
    private void guardarAvionCatalogo() {
        // Obtiene la posición del elemento seleccionado en el Spinner
        int pos = spinnerAviones.getSelectedItemPosition();
        // Si no hay nada seleccionado, detiene la ejecución
        if (pos < 0) return;

        // Recupera el objeto avión base según la posición seleccionada
        Avion base = listaAvionesBase.get(pos);

        // Crea una nueva instancia de Avion copiando los datos del objeto base
        Avion nuevo = new Avion(
                base.getModelo(),
                base.getFabricante(),
                base.getRareza(),
                base.getImagenResId(),
                base.getVelocidad(),
                base.getPasajeros(),
                base.getDimensiones(),
                base.getPais(),
                base.getPeso()
        );

        // Si el usuario seleccionó una imagen propia, se asigna al nuevo objeto
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
        }

        // Guarda el avión en el almacenamiento local del dispositivo
        guardarEnPreferencias(nuevo);
        // Sincroniza el avión con la base de datos en la nube (Firestore)
        guardarEnFirestore(nuevo);

        // Muestra un mensaje de confirmación al usuario
        Toast.makeText(this, "Avión añadido a tu colección", Toast.LENGTH_SHORT).show();
        // Cierra la actividad actual
        finish();
    }

    // Método para persistir datos localmente usando SharedPreferences
    private void guardarEnPreferencias(Avion nuevo) {
        // Accede al archivo de preferencias compartidas de la aplicación
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        // Inicializa la librería Gson para convertir objetos a formato JSON
        Gson gson = new Gson();

        // Recupera la lista actual de aviones guardada como String JSON
        String json = prefs.getString("lista_aviones", null);
        // Define el tipo de dato para la conversión de la lista
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();

        // Convierte el JSON a una lista de objetos o crea una nueva si no existe
        List<Avion> lista = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        // Doble comprobación de seguridad para asegurar que la lista no sea nula
        if (lista == null) lista = new ArrayList<>();

        // Añade el nuevo avión a la lista recuperada
        lista.add(nuevo);
        // Guarda la lista actualizada convirtiéndola de nuevo a JSON
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }

    // Método para guardar el objeto en la base de datos de Firebase
    private void guardarEnFirestore(Avion avion) {
        // Obtiene la instancia actual de autenticación de Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        // Si no hay un usuario logueado, sale del método
        if (auth.getCurrentUser() == null) return;

        // Accede a la colección "usuarios", luego al documento del usuario y a su subcolección "aviones"
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(auth.getCurrentUser().getUid())
                .collection("aviones")
                .document(avion.getId())
                .set(avion) // Inserta o actualiza el documento con el objeto avión
                .addOnSuccessListener(v ->
                        Log.d("FIRESTORE", "Avión guardado")) // Log de éxito
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE", "Error", e)); // Log de error
    }

    // Método para mostrar un cuadro de diálogo y elegir origen de la foto
    private void mostrarDialogoSeleccion() {
        // Define las opciones disponibles en el diálogo
        String[] opciones = {"Cámara", "Galería"};

        // Construye y muestra un AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Añadir foto") // Título del diálogo
                .setItems(opciones, (d, i) -> {
                    // Si elige la primera opción (índice 0), abre la cámara
                    if (i == 0) abrirCamara();
                        // Si elige la segunda, lanza el selector de archivos de imagen
                    else launcherGaleria.launch("image/*");
                })
                .show(); // Hace visible el diálogo
    }

    // Lanzador para manejar el resultado de seleccionar una imagen de la galería
    private final ActivityResultLauncher<String> launcherGaleria =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                // Si se seleccionó una imagen, se procede a cargarla en la vista
                if (uri != null) cargarImagen(uri);
            });

    // Lanzador para manejar el resultado de capturar una foto con la cámara
    private final ActivityResultLauncher<Uri> launcherCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                // Si la foto se tomó correctamente, se carga en la vista
                if (ok && uriFotoCamaraTemporal != null) {
                    cargarImagen(uriFotoCamaraTemporal);
                }
            });

    // Método para iniciar el proceso de captura de foto
    private void abrirCamara() {
        // Comprueba si la aplicación tiene permisos para usar la cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) return;

        try {
            // Crea un archivo físico donde se guardará la foto
            File f = crearArchivoImagen();
            // Genera una URI segura para el archivo usando FileProvider
            uriFotoCamaraTemporal = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", f);
            // Lanza el contrato de la cámara pasando la URI de destino
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            // Imprime errores en la consola en caso de fallo al crear el archivo
            e.printStackTrace();
        }
    }

    // Método para generar un archivo temporal de imagen en el almacenamiento
    private File crearArchivoImagen() throws IOException {
        // Genera una marca de tiempo para que el nombre del archivo sea único
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        // Obtiene el directorio de imágenes privado de la aplicación
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Crea y devuelve el archivo temporal con prefijo IMG y extensión jpg
        return File.createTempFile("IMG_" + ts, ".jpg", dir);
    }

    // Método para visualizar la imagen seleccionada utilizando la librería Glide
    private void cargarImagen(Uri uri) {
        // Guarda la referencia de la URI para su uso posterior al guardar el avión
        uriImagenFinal = uri;
        // Utiliza Glide para cargar la imagen de forma eficiente
        Glide.with(this)
                .load(uri) // Carga la fuente desde la URI
                .centerCrop() // Recorta la imagen para que llene el contenedor
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Desactiva caché en disco para ver cambios inmediatos
                .skipMemoryCache(true) // Desactiva caché en memoria
                .into(imgPreviewAvion); // Inserta el resultado en el ImageView
    }
}