// Definición del paquete de la clase actual
package es.medac.skycollectorapp.activities;

// Importación para la gestión de permisos del sistema
import android.Manifest;
// Importación para la comunicación entre componentes de Android
import android.content.Intent;
// Importación para el almacenamiento persistente de datos simples
import android.content.SharedPreferences;
// Importación para verificar estados de los permisos instalados
import android.content.pm.PackageManager;
// Importación para la gestión de identificadores de recursos universales
import android.net.Uri;
// Importación para el manejo del estado de la instancia de la actividad
import android.os.Bundle;
// Importación para acceder al entorno de almacenamiento externo
import android.os.Environment;
// Importación para la manipulación de las propiedades de las vistas
import android.view.View;
// Importación para el componente de botón de la interfaz
import android.widget.Button;
// Importación para el campo de entrada de texto editable
import android.widget.EditText;
// Importación para el componente que visualiza imágenes
import android.widget.ImageView;
// Importación para el componente que visualiza texto estático
import android.widget.TextView;
// Importación para mostrar notificaciones flotantes breves
import android.widget.Toast;

// Importación para registrar lanzadores de resultados de actividades
import androidx.activity.result.ActivityResultLauncher;
// Importación para contratos estándar de resultados de actividades
import androidx.activity.result.contract.ActivityResultContracts;
// Importación base para actividades con soporte de compatibilidad
import androidx.appcompat.app.AppCompatActivity;
// Importación para el contenedor visual con diseño de tarjeta
import androidx.cardview.widget.CardView;
// Importación para comprobaciones de compatibilidad de contextos
import androidx.core.content.ContextCompat;
// Importación para compartir archivos de forma segura entre apps
import androidx.core.content.FileProvider;

// Importación principal de la librería de carga de imágenes
import com.bumptech.glide.Glide;
// Importación para configurar la estrategia de caché de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// Importación para la conversión de objetos Java a formato JSON
import com.google.gson.Gson;
// Importación para preservar información de tipos genéricos en tiempo de ejecución
import com.google.gson.reflect.TypeToken;

// Importación para la representación de archivos en el sistema
import java.io.File;
// Importación para la gestión de errores de entrada y salida
import java.io.IOException;
// Importación para el manejo de tipos de datos mediante reflexión
import java.lang.reflect.Type;
// Importación para el formateado de fechas y horas
import java.text.SimpleDateFormat;
// Importación para la estructura de lista dinámica
import java.util.ArrayList;
// Importación para la representación de instantes temporales
import java.util.Date;
// Importación para la configuración regional de datos
import java.util.Locale;

// Importación de la clase de recursos del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de datos del objeto Avión
import es.medac.skycollectorapp.models.Avion;

// Clase que gestiona la pantalla de visualización y edición de detalles de un avión
public class DetalleAvionActivity extends AppCompatActivity {

    // Referencia para la imagen principal del avión
    private ImageView imgDetalleGrande;
    // Referencia para la imagen personalizada cargada por el usuario
    private ImageView imgFotoUsuario;
    // Referencia para el campo de edición del apodo del avión
    private EditText etNombreAvion;

    // Referencias para las etiquetas de información técnica del avión
    private TextView txtDetalleFabricante, txtDetallePais, txtDetalleVelocidad,
            txtDetalleCapacidad, txtDetallePeso, txtDetalleDimensiones;

    // Referencias para los botones de origen de la fotografía
    private Button btnCamara, btnGaleria;
    // Referencias para los botones de control de la pantalla
    private Button btnGuardar, btnCancelar;

    // Referencia para el contenedor que agrupa la foto del usuario
    private CardView layoutFotoUsuario;

    // Objeto que almacena los datos del avión seleccionado
    private Avion avion;
    // Referencia temporal para la ruta de la imagen de la cámara
    private Uri uriFotoTemporal;

    // Variable para identificar el avión de forma única
    private String avionId;

    // Definición del lanzador para capturar fotografías con la cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                // Si la captura es exitosa y hay una ruta válida
                if (exito && uriFotoTemporal != null) {
                    // Muestra la imagen capturada en la interfaz
                    mostrarFotoUsuario(uriFotoTemporal);
                }
            }
    );

    // Definición del lanzador para solicitar permisos de hardware al sistema
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // Si el permiso es concedido, procede a abrir la cámara
                if (isGranted) abrirCamara();
                    // Si es denegado, informa al usuario mediante un aviso
                else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
    );

    // Definición del lanzador para seleccionar archivos de la galería
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Si se selecciona una ruta válida
                if (uri != null) {
                    try {
                        // Solicita permiso persistente para leer la ruta de la imagen
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        // Registra cualquier error en la obtención de permisos
                        e.printStackTrace();
                    }
                    // Muestra la imagen seleccionada en la interfaz
                    mostrarFotoUsuario(uri);
                }
            }
    );

    // Método principal del ciclo de vida al crear la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de inicialización superior
        super.onCreate(savedInstanceState);
        // Asignación de la interfaz visual desde el recurso XML
        setContentView(R.layout.activity_detalle_avion);

        // Invocación del método que conecta los componentes visuales
        initViews();

        // Recuperación del identificador enviado desde la pantalla anterior
        avionId = getIntent().getStringExtra("avion_id");

        // Verificación de integridad del identificador recibido
        if (avionId == null) {
            // Notifica la falta de datos y cierra la pantalla
            Toast.makeText(this, "Error: no se recibió el ID del avión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Búsqueda del objeto avión correspondiente en el almacenamiento
        avion = buscarAvionPorId(avionId);

        // Verificación de que el avión existe en la colección
        if (avion == null) {
            // Notifica la ausencia del objeto y cierra la pantalla
            Toast.makeText(this, "Error: avión no encontrado en la lista", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Carga la información del objeto en los componentes visuales
        cargarDatos();
        // Configura los eventos de interacción de los botones
        setupListeners();
    }

    // Método para inicializar y vincular los elementos de la interfaz
    private void initViews() {
        // Vinculación de la imagen de cabecera
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande);
        // Vinculación del campo de texto para el apodo
        etNombreAvion = findViewById(R.id.etNombreAvion);

        // Vinculación de los campos de texto informativos
        txtDetalleFabricante = findViewById(R.id.txtDetalleFabricante);
        txtDetallePais = findViewById(R.id.txtDetallePais);
        txtDetalleVelocidad = findViewById(R.id.txtDetalleVelocidad);
        txtDetalleCapacidad = findViewById(R.id.txtDetalleCapacidad);
        txtDetallePeso = findViewById(R.id.txtDetallePeso);
        txtDetalleDimensiones = findViewById(R.id.txtDetalleDimensiones);

        // Vinculación de los botones de acción multimedia
        btnCamara = findViewById(R.id.btnCamara);
        btnGaleria = findViewById(R.id.btnGaleria);

        // Vinculación de los botones de navegación y guardado
        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Vinculación del contenedor y marco de la foto de usuario
        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario);
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario);
    }

    // Método para definir el comportamiento de los clics
    private void setupListeners() {

        // Cierra la actividad actual sin guardar cambios al pulsar cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Procesa el guardado del apodo y los cambios al pulsar guardar
        btnGuardar.setOnClickListener(v -> {
            // Actualiza el apodo en el objeto con el texto del campo
            avion.setApodo(etNombreAvion.getText().toString().trim());

            // Persiste el objeto actualizado en las preferencias locales
            guardarCambiosEnPreferencias(avion);

            // Informa del éxito y cierra la pantalla
            Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Gestiona la lógica de apertura de la cámara con verificación de permisos
        btnCamara.setOnClickListener(v -> {
            // Comprueba si el permiso ya ha sido otorgado
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                // Inicia el procedimiento de cámara
                abrirCamara();
            } else {
                // Solicita el permiso formalmente al sistema
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Inicia el selector de imágenes de la galería del dispositivo
        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));
    }

    // Método para poblar la interfaz con la información del objeto
    private void cargarDatos() {
        // Establece el apodo actual en el campo de texto
        etNombreAvion.setText(avion.getApodo());

        // Asigna el texto formateado a cada etiqueta de detalle técnico
        txtDetalleFabricante.setText("Fabricante: " + avion.getFabricante());
        txtDetallePais.setText("País: " + avion.getPais());
        txtDetalleVelocidad.setText("Velocidad: " + avion.getVelocidad());
        txtDetalleCapacidad.setText("Capacidad: " + avion.getPasajeros());
        txtDetallePeso.setText("Peso: " + avion.getPeso());
        txtDetalleDimensiones.setText("Dimensiones: " + avion.getDimensiones());

        // Carga la imagen predefinida del avión usando la librería Glide
        Glide.with(this)
                .load(avion.getImagenResId())
                .fitCenter()
                .into(imgDetalleGrande);

        // Verifica si el usuario ya tiene una foto personalizada asignada
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            // Hace visible el marco de la fotografía del usuario
            layoutFotoUsuario.setVisibility(View.VISIBLE);

            // Carga la imagen desde la ruta almacenada
            Glide.with(this)
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .centerCrop()
                    .into(imgFotoUsuario);
        } else {
            // Oculta el marco si no existe una fotografía personalizada
            layoutFotoUsuario.setVisibility(View.GONE);
        }
    }

    // Método para actualizar y visualizar la nueva foto del usuario
    private void mostrarFotoUsuario(Uri uri) {
        // Registra la nueva ruta de imagen en el objeto avión
        avion.setUriFotoUsuario(uri.toString());

        // Asegura que el marco sea visible en la pantalla
        layoutFotoUsuario.setVisibility(View.VISIBLE);

        // Carga la nueva imagen omitiendo la caché para reflejar cambios inmediatos
        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(imgFotoUsuario);
    }

    // Método para preparar el entorno y disparar la cámara
    private void abrirCamara() {
        try {
            // Crea físicamente un archivo para albergar la fotografía
            File archivo = crearArchivoImagen();

            // Obtiene la dirección segura del archivo para aplicaciones externas
            uriFotoTemporal = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    archivo
            );

            // Lanza el contrato de captura de imagen pasando la ruta de destino
            launcherCamara.launch(uriFotoTemporal);

        } catch (Exception e) {
            // Notifica fallos durante la preparación del archivo de imagen
            Toast.makeText(this,
                    "Error cámara: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Método para generar un archivo temporal único en el almacenamiento
    private File crearArchivoImagen() throws IOException {
        // Genera un nombre basado en la fecha y hora actual
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        // Crea y devuelve el archivo temporal en el directorio de imágenes privado
        return File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        );
    }

    // Método para recuperar un avión específico desde el almacenamiento local
    private Avion buscarAvionPorId(String id) {
        // Acceso al fichero de preferencias de la aplicación
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        // Obtención de la cadena JSON que representa la lista completa
        String json = prefs.getString("lista_aviones", null);
        // Retorna nulo si no hay datos guardados previamente
        if (json == null) return null;

        // Inicializa el motor de conversión JSON
        Gson gson = new Gson();
        // Define el tipo de dato de la colección para la deserialización
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        // Convierte el texto JSON de nuevo a una lista de objetos Java
        ArrayList<Avion> lista = gson.fromJson(json, type);
        // Retorna nulo si la conversión falla o la lista está vacía
        if (lista == null) return null;

        // Recorre la colección buscando la coincidencia por identificador
        for (Avion a : lista) {
            // Si el identificador coincide con el buscado, devuelve el objeto
            if (a.getId() != null && a.getId().equals(id)) {
                return a;
            }
        }
        // Retorna nulo si no se encuentra ningún elemento con ese identificador
        return null;
    }

    // Método para sincronizar y guardar la versión actualizada del avión
    private void guardarCambiosEnPreferencias(Avion avionActualizado) {
        // Acceso al fichero de almacenamiento de preferencias
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        // Inicialización del motor de conversión JSON
        Gson gson = new Gson();

        // Recuperación de la lista existente
        String json = prefs.getString("lista_aviones", null);
        // Detiene el proceso si no hay datos que actualizar
        if (json == null) return;

        // Definición del tipo para la conversión de la lista
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        // Transformación del texto almacenado a lista dinámica
        ArrayList<Avion> lista = gson.fromJson(json, type);
        // Detiene el proceso si la lista no pudo ser recreada
        if (lista == null) return;

        // Localiza el objeto antiguo y lo reemplaza por el nuevo en la lista
        for (int i = 0; i < lista.size(); i++) {
            // Compara los identificadores para encontrar la posición correcta
            if (lista.get(i).getId().equals(avionActualizado.getId())) {
                // Actualiza el elemento en la posición encontrada
                lista.set(i, avionActualizado);
                break;
            }
        }

        // Convierte la lista completa de nuevo a JSON y la guarda permanentemente
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }
}