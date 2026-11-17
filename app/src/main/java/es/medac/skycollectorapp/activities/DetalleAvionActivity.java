// Declaración del paquete donde se encuentra ubicada la actividad
package es.medac.skycollectorapp.activities;

// Importación para solicitar permisos del sistema en tiempo de ejecución (cámara)
import android.Manifest;
// Importación para suprimir advertencias de accesibilidad en listeners táctiles personalizados
import android.annotation.SuppressLint;
// Importación para crear diálogos modales a pantalla completa (visor de zoom)
import android.app.Dialog;
// Importación para gestionar intenciones y banderas de permisos de lectura de URIs
import android.content.Intent;
// Importación para leer y guardar datos locales en SharedPreferences
import android.content.SharedPreferences;
// Importación para verificar el estado de concesión de permisos del sistema
import android.content.pm.PackageManager;
// Importación para utilizar constantes de color
import android.graphics.Color;
// Importación para crear fondos con degradado de color en modo Easter Egg
import android.graphics.drawable.GradientDrawable;
// Importación para manipular rutas de archivos multimedia e imágenes
import android.net.Uri;
// Importación para gestionar el estado y ciclo de vida de la actividad
import android.os.Bundle;
// Importación para acceder a los directorios estándar de almacenamiento de imágenes
import android.os.Environment;
// Importación para registrar eventos y errores en la consola Logcat
import android.util.Log;
// Importación para configurar la alineación y gravedad de vistas dinámicas
import android.view.Gravity;
// Importación para capturar eventos táctiles de gestos y desplazamiento
import android.view.MotionEvent;
// Importación para detectar gestos de pellizco (pinch-to-zoom)
import android.view.ScaleGestureDetector;
// Importación de la clase base de componentes visuales
import android.view.View;
// Importación para manipular contenedores y parámetros de diseño de vistas
import android.view.ViewGroup;
// Importación para los botones de acción de la interfaz
import android.widget.Button;
// Importación para el campo editable donde el usuario cambia el apodo del avión
import android.widget.EditText;
// Importación para crear contenedores superpuestos dinámicamente
import android.widget.FrameLayout;
// Importación para el botón gráfico de cerrar en el visor de imagen ampliada
import android.widget.ImageButton;
// Importación para mostrar la imagen principal y la foto personalizada del usuario
import android.widget.ImageView;
// Importación para mostrar los datos técnicos del avión en pantalla
import android.widget.TextView;
// Importación para mostrar notificaciones emergentes breves
import android.widget.Toast;

// Importación para gestionar resultados modernos de cámara, galería y permisos
import androidx.activity.result.ActivityResultLauncher;
// Importación de los contratos estándar de resultados de AndroidX
import androidx.activity.result.contract.ActivityResultContracts;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación de tarjetas con elevación y bordes redondeados
import androidx.cardview.widget.CardView;
// Importación para comprobar permisos manteniendo retrocompatibilidad
import androidx.core.content.ContextCompat;
// Importación para compartir URIs seguras con la aplicación de cámara externa
import androidx.core.content.FileProvider;

// Importación de Glide para carga, recorte y caché de imágenes
import com.bumptech.glide.Glide;
// Importación para configurar la estrategia de caché en disco de Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// Importación para obtener la sesión del usuario autenticado en Firebase
import com.google.firebase.auth.FirebaseAuth;
// Importación para consultar el identificador UID del usuario actual
import com.google.firebase.auth.FirebaseUser;
// Importación para actualizar los cambios del avión en Cloud Firestore
import com.google.firebase.firestore.FirebaseFirestore;
// Importación de Gson para serializar y deserializar objetos JSON
import com.google.gson.Gson;
// Importación de TypeToken para manejar tipos genéricos con Gson
import com.google.gson.reflect.TypeToken;

// Importación para crear y manipular ficheros en disco
import java.io.File;
// Importación para capturar errores de entrada/salida al crear archivos
import java.io.IOException;
// Importación para metadatos de tipos en tiempo de ejecución
import java.lang.reflect.Type;
// Importación para formatear marcas de tiempo en nombres de archivo
import java.text.SimpleDateFormat;
// Importación de listas dinámicas
import java.util.ArrayList;
// Importación para obtener la fecha y hora actual
import java.util.Date;
// Importación para configurar formatos regionales
import java.util.Locale;

// Importación de recursos XML del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de datos Avion
import es.medac.skycollectorapp.models.Avion;
// Importación del motor global de utilidades y detección de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad que gestiona la visualización, edición de apodos y fotos personalizadas de un avión en la colección,
 * aplicando estilo dorado y foto de operaciones si se trata de un Easter Egg.
 */
public class DetalleAvionActivity extends AppCompatActivity {

    // Vista de imagen principal para mostrar la fotografía del catálogo o Wikipedia
    private ImageView imgDetalleGrande;
    // Vista de imagen secundaria para mostrar la fotografía tomada o elegida por el usuario
    private ImageView imgFotoUsuario;
    // Campo de texto editable para personalizar el apodo de la aeronave
    private EditText etNombreAvion;

    // Etiquetas de texto para mostrar las especificaciones técnicas del avión
    private TextView txtDetalleFabricante, txtDetallePais, txtDetalleVelocidad,
            txtDetalleCapacidad, txtDetallePeso, txtDetalleDimensiones;

    // Botones para adjuntar foto desde la cámara o desde la galería del dispositivo
    private Button btnCamara, btnGaleria;
    // Botones para guardar las modificaciones o cancelar y cerrar la vista
    private Button btnGuardar, btnCancelar;
    // Contenedor tarjeta que agrupa y muestra la fotografía personalizada del usuario
    private CardView layoutFotoUsuario;

    // Instancia del objeto Avion recuperado de la colección del usuario
    private Avion avion;
    // URI temporal asignada al fichero creado antes de abrir la cámara
    private Uri uriFotoTemporal;
    // Identificador único del avión recibido mediante el Intent
    private String avionId;

    // Almacena la fuente actual de la imagen del catálogo para abrirla en el visor de zoom
    private Object fuenteImagenCatalogoActual;
    // Almacena la fuente actual de la foto del usuario para abrirla en el visor de zoom
    private Object fuenteFotoUsuarioActual;
    // Etiqueta de texto dinámica superpuesta cuando no existe fotografía disponible
    private TextView txtAvisoSinFoto;

    // Lanzador registrado para recibir el resultado de la captura de foto desde la cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                // Comprueba si la foto se capturó correctamente y la URI temporal es válida
                if (exito && uriFotoTemporal != null) {
                    // Muestra la foto capturada y la marca como verificada desde cámara
                    mostrarFotoUsuario(uriFotoTemporal, true);
                }
            }
    );

    // Lanzador registrado para solicitar el permiso de cámara en tiempo de ejecución
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // Si el usuario concede el permiso, abre la aplicación de cámara
                if (isGranted) abrirCamara();
                    // Si deniega el permiso, muestra un mensaje informativo
                else Toast.makeText(this, getString(R.string.toast_permiso_denegado), Toast.LENGTH_SHORT).show();
            }
    );

    // Lanzador registrado para seleccionar una imagen desde la galería del dispositivo
    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Comprueba que el usuario haya seleccionado una imagen válida
                if (uri != null) {
                    try {
                        // Solicita permiso persistente de lectura sobre la URI de la galería
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        // Registra advertencia si el proveedor de contenidos no soporta persistencia
                        Log.w("URI_PERM", "El proveedor no soporta persistencia de URI", e);
                    }
                    // Muestra la foto elegida y la marca como no verificada por cámara directa
                    mostrarFotoUsuario(uri, false);
                }
            }
    );

    /**
     * Inicializa la actividad, recupera la aeronave desde la colección local y configura los eventos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama al método onCreate de la superclase AppCompatActivity
        super.onCreate(savedInstanceState);
        // Establece el diseño XML correspondiente a esta pantalla
        setContentView(R.layout.activity_detalle_avion);

        // Enlaza los componentes visuales del XML con las variables Java
        initViews();

        // Obtiene el identificador del avión enviado desde la actividad anterior
        avionId = getIntent().getStringExtra("avion_id");

        // Valida que el identificador recibido no sea nulo
        if (avionId == null) {
            // Muestra error si falta el ID y cierra la actividad
            Toast.makeText(this, getString(R.string.toast_error_sin_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Busca y recupera el objeto Avion correspondiente en el almacenamiento local
        avion = buscarAvionPorId(avionId);

        // Verifica que el avión exista en la colección del usuario
        if (avion == null) {
            // Muestra error si no se encuentra el registro y cierra la actividad
            Toast.makeText(this, getString(R.string.toast_error_no_encontrado), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Rellena los campos de la interfaz con los datos del avión
        cargarDatos();
        // Configura los escuchadores de clic para botones e imágenes
        setupListeners();
    }

    /**
     * Genera la clave de almacenamiento local asociada al UID del usuario autenticado.
     */
    private String getClaveListaUsuario() {
        // Obtiene el usuario autenticado actualmente en Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Devuelve la clave vinculada al UID del usuario o una clave por defecto si es anónimo
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Vincula las referencias de los componentes del archivo XML y ajusta los márgenes de la imagen principal.
     */
    private void initViews() {
        // Enlaza la vista de imagen principal
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande);

        // Obtiene los parámetros de margen de la imagen para eliminar espacios laterales
        ViewGroup.MarginLayoutParams imgParams = (ViewGroup.MarginLayoutParams) imgDetalleGrande.getLayoutParams();
        if (imgParams != null) {
            // Ajusta los márgenes izquierdo y derecho a cero manteniendo superior e inferior
            imgParams.setMargins(0, imgParams.topMargin, 0, imgParams.bottomMargin);
            imgDetalleGrande.setLayoutParams(imgParams);
        }
        // Elimina cualquier relleno interno de la vista de imagen
        imgDetalleGrande.setPadding(0, 0, 0, 0);
        // Configura el escalado para recortar centrando la imagen en el contenedor
        imgDetalleGrande.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Si el padre es un CardView, elimina también su relleno de contenido
        if (imgDetalleGrande.getParent() instanceof CardView) {
            CardView cardPadre = (CardView) imgDetalleGrande.getParent();
            cardPadre.setContentPadding(0, 0, 0, 0);
        }

        // Enlaza el campo de edición del apodo del avión
        etNombreAvion = findViewById(R.id.etNombreAvion);

        // Enlaza todas las etiquetas de texto de la ficha técnica
        txtDetalleFabricante = findViewById(R.id.txtDetalleFabricante);
        txtDetallePais = findViewById(R.id.txtDetallePais);
        txtDetalleVelocidad = findViewById(R.id.txtDetalleVelocidad);
        txtDetalleCapacidad = findViewById(R.id.txtDetalleCapacidad);
        txtDetallePeso = findViewById(R.id.txtDetallePeso);
        txtDetalleDimensiones = findViewById(R.id.txtDetalleDimensiones);

        // Enlaza los botones de cámara y galería
        btnCamara = findViewById(R.id.btnCamara);
        btnGaleria = findViewById(R.id.btnGaleria);

        // Enlaza los botones de cancelar y guardar cambios
        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Enlaza la tarjeta y la vista de imagen de la fotografía personalizada del usuario
        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario);
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario);
    }

    /**
     * Configura las acciones asociadas a los clics de los botones y vistas de imagen.
     */
    private void setupListeners() {
        // Cierra la actividad sin guardar cambios al pulsar Cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Guarda el nuevo apodo y sincroniza los cambios al pulsar Guardar
        btnGuardar.setOnClickListener(v -> {
            // Lee el texto introducido en el campo de apodo eliminando espacios extremos
            String nuevoApodo = etNombreAvion.getText().toString().trim();
            // Si el apodo no está vacío, lo actualiza en el objeto Avion
            if (!nuevoApodo.isEmpty()) {
                avion.setApodo(nuevoApodo);
            }

            // Persiste los cambios en SharedPreferences local
            guardarCambiosEnPreferencias(avion);
            // Sincroniza los cambios en la nube con Firebase Firestore
            guardarCambiosEnFirestore(avion);

            // Notifica al usuario que los cambios se han guardado y cierra la pantalla
            Toast.makeText(this, getString(R.string.toast_cambios_guardados), Toast.LENGTH_SHORT).show();
            finish();
        });

        // Configura el botón de cámara comprobando primero si el permiso está concedido
        btnCamara.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Abre la cámara directamente si ya tiene permiso
                abrirCamara();
            } else {
                // Solicita el permiso de cámara al sistema
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Configura el botón de galería para abrir el selector de imágenes del sistema
        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));

        // Abre la imagen principal en pantalla completa con zoom al pulsar sobre ella
        imgDetalleGrande.setOnClickListener(v -> {
            if (fuenteImagenCatalogoActual != null) {
                abrirImagenAmpliada(fuenteImagenCatalogoActual);
            }
        });

        // Abre la fotografía del usuario en pantalla completa con zoom al pulsar sobre ella
        imgFotoUsuario.setOnClickListener(v -> {
            if (fuenteFotoUsuarioActual != null) {
                abrirImagenAmpliada(fuenteFotoUsuarioActual);
            }
        });
    }

    /**
     * Aplica un acabado visual dorado exclusivo cuando el elemento avistado es un Easter Egg.
     */
    private void aplicarEstiloDoradoSiEsEasterEgg() {
        // Si el objeto avión es nulo, sale del método sin hacer nada
        if (avion == null) return;
        // Comprueba por rareza o metadatos si el registro corresponde a un Easter Egg terrestre
        boolean esEasterEgg = "EASTER_EGG".equalsIgnoreCase(avion.getRareza())
                || AvionGenerator.esEasterEggTerrestre(avion.getTipoIcao(), null, avion.getMatricula(), avion.getModelo());

        // Si no es un Easter Egg, mantiene el estilo estándar
        if (!esEasterEgg) return;

        // Obtiene el contenedor raíz del contenido de la ventana
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            // Recupera el layout principal y le aplica un degradado vertical en tonos dorados
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            pantallaPrincipal.setBackground(fondoDorado);
        }

        // Cambia el color de texto del campo de apodo a oro brillante
        if (etNombreAvion != null) {
            etNombreAvion.setTextColor(0xFFFFD700);
        }
        // Cambia el fondo y texto del botón Guardar a estética dorada
        if (btnGuardar != null) {
            btnGuardar.setBackgroundColor(0xFFFFD700);
            btnGuardar.setTextColor(0xFF1F1400);
        }
    }

    /**
     * Rellena la interfaz gráfica con los datos técnicos y fotografías del objeto Avion cargado.
     */
    private void cargarDatos() {
        // Aplica el tema visual dorado si corresponde a un Easter Egg
        aplicarEstiloDoradoSiEsEasterEgg();

        // Verifica si el avión es un Easter Egg para asegurar su fotografía oficial
        boolean esEasterEgg = "EASTER_EGG".equalsIgnoreCase(avion.getRareza())
                || AvionGenerator.esEasterEggTerrestre(avion.getTipoIcao(), null, avion.getMatricula(), avion.getModelo());

        // Si es Easter Egg y carece de URL de imagen, le asigna la foto oficial y la guarda
        if (esEasterEgg && (avion.getImagenUrl() == null || avion.getImagenUrl().isEmpty())) {
            avion.setImagenUrl(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO);
            avion.setUrlImagenRemota(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO);
            guardarCambiosEnPreferencias(avion);
            guardarCambiosEnFirestore(avion);
        }

        // Establece el apodo actual del avión en el campo editable
        etNombreAvion.setText(avion.getApodo());

        // Asigna las especificaciones técnicas formateadas a cada TextView correspondiente
        txtDetalleFabricante.setText(getString(R.string.detalle_fabricante_formato, avion.getFabricante()));
        txtDetallePais.setText(getString(R.string.detalle_pais_formato, avion.getPais()));
        txtDetalleVelocidad.setText(getString(R.string.detalle_velocidad_formato, avion.getVelocidad()));
        txtDetalleCapacidad.setText(getString(R.string.detalle_capacidad_formato, avion.getPasajeros()));
        txtDetallePeso.setText(getString(R.string.detalle_peso_formato, avion.getPeso()));
        txtDetalleDimensiones.setText(getString(R.string.detalle_dimensiones_formato, avion.getDimensiones()));

        // Obtiene el nombre del modelo o un texto por defecto si es nulo
        String nombreModeloCrudo = avion.getModelo() != null ? avion.getModelo() : getString(R.string.aeronave_desconocida);

        // Si ya dispone de una URL de imagen guardada, la carga directamente con Glide
        if (avion.getImagenUrl() != null && !avion.getImagenUrl().isEmpty()) {
            fuenteImagenCatalogoActual = avion.getImagenUrl();
            gestionarAvisoSinFoto(imgDetalleGrande, false);
            Glide.with(this)
                    .load(avion.getImagenUrl())
                    .centerCrop()
                    .into(imgDetalleGrande);
        } else {
            // Si no tiene URL guardada, limpia el nombre del modelo y la busca en Wikipedia
            String modeloLimpioParaWiki = limpiarNombreParaBusqueda(nombreModeloCrudo);
            cargarImagenAutomaticaDesdeInternet(modeloLimpioParaWiki, imgDetalleGrande, avion.getImagenResId());
        }

        // Comprueba si el usuario tiene una fotografía personalizada asociada a este avión
        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            // Hace visible la tarjeta de foto de usuario y carga la imagen
            layoutFotoUsuario.setVisibility(View.VISIBLE);
            fuenteFotoUsuarioActual = avion.getUriFotoUsuario();

            Glide.with(this)
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .centerCrop()
                    .into(imgFotoUsuario);
        } else {
            // Oculta la tarjeta si no hay foto del usuario
            layoutFotoUsuario.setVisibility(View.GONE);
            fuenteFotoUsuarioActual = null;
        }
    }

    /**
     * Refresca la vista, asigna la nueva foto al objeto Avion y actualiza su estado de verificación.
     */
    private void mostrarFotoUsuario(Uri uri, boolean esDesdeCamara) {
        // Guarda la URI en formato texto dentro del modelo Avion
        avion.setUriFotoUsuario(uri.toString());
        // Marca si la foto fue tomada en directo con la cámara (verificada) o desde galería
        avion.setEsFotoVerificada(esDesdeCamara);
        // Hace visible el contenedor de la foto del usuario
        layoutFotoUsuario.setVisibility(View.VISIBLE);
        // Actualiza la fuente de imagen para el visor de zoom
        fuenteFotoUsuarioActual = uri.toString();

        // Renderiza la nueva fotografía en el ImageView evitando cachés antiguas
        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(imgFotoUsuario);
    }

    /**
     * Prepara el archivo privado y lanza la aplicación de cámara.
     */
    private void abrirCamara() {
        try {
            // Crea el fichero físico temporal en el almacenamiento privado de la app
            File archivo = crearArchivoImagen();
            // Obtiene una URI segura mediante FileProvider para compartirla con la cámara
            uriFotoTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            // Lanza el contrato de captura fotográfica
            launcherCamara.launch(uriFotoTemporal);
        } catch (Exception e) {
            // Muestra mensaje de error y registra la traza si falla la apertura de cámara
            Toast.makeText(this, getString(R.string.toast_error_camara), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Genera un archivo JPG temporal en el directorio persistente privado.
     */
    private File crearArchivoImagen() throws IOException {
        // Genera una marca de tiempo única con formato año-mes-día y hora-minuto-segundo
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        // Obtiene el directorio externo privado de imágenes de la aplicación
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Crea el directorio si aún no existe
        if (dir != null && !dir.exists()) dir.mkdirs();

        // Crea y devuelve el archivo temporal JPEG
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", dir);
    }

    /**
     * Busca una aeronave dentro del almacenamiento JSON local del usuario por su identificador ID.
     */
    private Avion buscarAvionPorId(String id) {
        // Abre el archivo de preferencias compartidas de la aplicación
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        // Recupera la cadena JSON con la lista de aviones del usuario actual
        String json = prefs.getString(getClaveListaUsuario(), null);
        if (json == null) return null;

        // Deserializa el JSON en una lista de objetos Avion usando Gson
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return null;

        // Recorre la lista buscando el avión cuyo ID coincida con el solicitado
        for (Avion a : lista) {
            if (a.getId() != null && a.getId().equals(id)) {
                return a;
            }
        }
        // Devuelve null si no se encontró ningún avión con ese ID
        return null;
    }

    /**
     * Actualiza la versión modificada del avión en la lista de SharedPreferences local del usuario.
     */
    private void guardarCambiosEnPreferencias(Avion avionActualizado) {
        // Accede a SharedPreferences y prepara Gson
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        Gson gson = new Gson();
        String clave = getClaveListaUsuario();

        // Lee el JSON actual de la colección del usuario
        String json = prefs.getString(clave, null);
        if (json == null) return;

        // Convierte el JSON a ArrayList<Avion>
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return;

        // Localiza la posición del avión modificado y lo reemplaza por la versión actualizada
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(avionActualizado.getId())) {
                lista.set(i, avionActualizado);
                break;
            }
        }

        // Serializa nuevamente la lista completa y la guarda en SharedPreferences
        prefs.edit().putString(clave, gson.toJson(lista)).apply();
    }

    /**
     * Sincroniza las modificaciones del avión en la base de datos Firestore en la nube.
     */
    private void guardarCambiosEnFirestore(Avion avionActualizado) {
        // Comprueba que haya un usuario autenticado en FirebaseAuth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        // Obtiene el UID del usuario actual
        String uid = auth.getCurrentUser().getUid();

        // Sobrescribe el documento del avión dentro de la subcolección del usuario en Firestore
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avionActualizado.getId())
                .set(avionActualizado)
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Cambios del avión actualizados en Firestore: " + avionActualizado.getApodo()))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al actualizar cambios en Firestore", e));
    }

    /**
     * Limpia prefijos genéricos y simplifica familias de modelos para optimizar la búsqueda en Wikipedia.
     */
    private String limpiarNombreParaBusqueda(String modelo) {
        // Si el modelo es nulo o está vacío, devuelve cadena vacía
        if (modelo == null || modelo.isEmpty()) return "";

        // Limpia el texto usando la utilidad global y lo pasa a mayúsculas para detectar familias
        String limpio = AvionGenerator.limpiarModeloParaWiki(this, modelo);
        String mayus = limpio.toUpperCase(Locale.ROOT);

        // Agrupa variantes comerciales comunes bajo su denominación estándar principal
        if (mayus.contains("737")) return "Boeing 737";
        if (mayus.contains("A320")) return "Airbus A320";
        if (mayus.contains("A321")) return "Airbus A321";
        if (mayus.contains("A319")) return "Airbus A319";
        if (mayus.contains("A330")) return "Airbus A330";

        // Elimina subvariantes numéricas o sufijos técnicos mediante expresión regular
        return limpio.replaceAll("(?i)(-\\d+|\\b\\d+[A-Z]{2,}\\b|\\/.*)", "").trim();
    }

    /**
     * Muestra u oculta un aviso visual superpuesto cuando no hay fotografía real disponible.
     */
    private void gestionarAvisoSinFoto(ImageView imageView, boolean mostrar) {
        // Obtiene el contenedor padre del ImageView
        ViewGroup parent = (ViewGroup) imageView.getParent();
        if (parent == null) return;

        if (mostrar) {
            // Si la etiqueta de aviso aún no existe, la crea y configura dinámicamente
            if (txtAvisoSinFoto == null) {
                txtAvisoSinFoto = new TextView(this);
                txtAvisoSinFoto.setText(getString(R.string.sin_fotografia_disponible));
                txtAvisoSinFoto.setTextColor(Color.WHITE);
                txtAvisoSinFoto.setTextSize(14f);
                txtAvisoSinFoto.setTypeface(null, android.graphics.Typeface.BOLD);
                txtAvisoSinFoto.setPadding(24, 12, 24, 12);
                txtAvisoSinFoto.setBackgroundColor(Color.parseColor("#CC1E293B"));
                txtAvisoSinFoto.setGravity(Gravity.CENTER);

                // Configura los parámetros para centrar el aviso sobre la silueta
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER;
                txtAvisoSinFoto.setLayoutParams(params);
                // Añade la etiqueta al contenedor padre
                parent.addView(txtAvisoSinFoto);
            } else {
                // Si ya estaba creada, simplemente la vuelve visible
                txtAvisoSinFoto.setVisibility(View.VISIBLE);
            }
        } else {
            // Si debe ocultarse y existe, cambia su visibilidad a GONE
            if (txtAvisoSinFoto != null) {
                txtAvisoSinFoto.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Obtiene automáticamente la fotografía desde Wikipedia en segundo plano y la persiste si tiene éxito.
     */
    private void cargarImagenAutomaticaDesdeInternet(String terminoBusqueda, ImageView imageView, int recursoFallback) {
        // Asigna inicialmente el recurso local de respaldo como fuente para el visor
        fuenteImagenCatalogoActual = recursoFallback;

        // Si el término está vacío o corresponde a una aeronave desconocida, carga la silueta directamente
        if (terminoBusqueda == null || terminoBusqueda.isEmpty()
                || AvionGenerator.esModeloDesconocido(this, terminoBusqueda)) {
            Glide.with(this).load(recursoFallback).centerCrop().into(imageView);
            gestionarAvisoSinFoto(imageView, true);
            return;
        }

        // Crea un ejecutor de un solo hilo y un Handler en el hilo principal para la respuesta
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // Ejecuta la consulta HTTP fuera del hilo de interfaz de usuario
        executor.execute(() -> {
            // Obtiene el idioma configurado en la app y consulta Wikipedia
            String langPrincipal = AvionGenerator.obtenerCodigoIdiomaWiki();
            String urlImagenRemota = obtenerUrlDesdeWikipedia(terminoBusqueda, langPrincipal);
            // Si no hay resultados en el idioma principal y no era inglés, reintenta en inglés
            if ((urlImagenRemota == null || urlImagenRemota.isEmpty()) && !"en".equals(langPrincipal)) {
                urlImagenRemota = obtenerUrlDesdeWikipedia(terminoBusqueda, "en");
            }

            final String urlFinal = urlImagenRemota;
            // Vuelve al hilo principal para actualizar la interfaz
            mainHandler.post(() -> {
                if (urlFinal != null && !urlFinal.isEmpty()) {
                    // Actualiza la fuente, guarda la URL obtenida en local/Firestore y carga la foto
                    fuenteImagenCatalogoActual = urlFinal;

                    avion.setImagenUrl(urlFinal);
                    guardarCambiosEnPreferencias(avion);
                    guardarCambiosEnFirestore(avion);

                    gestionarAvisoSinFoto(imageView, false);
                    Glide.with(this)
                            .load(urlFinal)
                            .centerCrop()
                            .into(imageView);
                } else {
                    // Si no se halló foto válida, mantiene la silueta de respaldo y muestra el aviso
                    fuenteImagenCatalogoActual = recursoFallback;
                    gestionarAvisoSinFoto(imageView, true);
                    Glide.with(this)
                            .load(recursoFallback)
                            .centerCrop()
                            .into(imageView);
                }
            });
        });
    }

    /**
     * Consulta la API de búsqueda e imágenes de Wikipedia filtrando resultados no válidos.
     */
    private String obtenerUrlDesdeWikipedia(String terminoBusqueda, String lang) {
        try {
            // Construye la URL de búsqueda añadiendo el sufijo aeronáutico localizado
            String sufijo = getString(R.string.wiki_sufijo_busqueda);
            String searchEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + java.net.URLEncoder.encode(terminoBusqueda + sufijo, "UTF-8") + "&format=json&srlimit=1";
            // Ejecuta la petición HTTP de búsqueda
            String searchRespuesta = leerUrlHttp(searchEndpoint);
            if (searchRespuesta == null) return null;

            // Parsea el JSON de respuesta para extraer el título del primer artículo encontrado
            org.json.JSONObject searchJson = new org.json.JSONObject(searchRespuesta);
            org.json.JSONObject queryObj = searchJson.optJSONObject("query");
            if (queryObj == null) return null;

            org.json.JSONArray searchArray = queryObj.optJSONArray("search");
            if (searchArray == null || searchArray.length() == 0) return null;

            String tituloEncontrado = searchArray.getJSONObject(0).optString("title");
            if (tituloEncontrado.isEmpty()) return null;

            // Descarta artículos que correspondan a escudos, banderas, divisiones administrativas o designadores
            String tituloLower = tituloEncontrado.toLowerCase(Locale.ROOT);
            if (tituloLower.contains("escudo") || tituloLower.contains("coat of arms") ||
                    tituloLower.contains("bandera") || tituloLower.contains("flag") ||
                    tituloLower.contains("municipality") || tituloLower.contains("province") ||
                    tituloLower.contains("forza aerea") || tituloLower.contains("fuerza aérea") ||
                    tituloLower.contains("type designator") || tituloLower.contains("designador")) {
                return null;
            }

            // Construye la URL para obtener la miniatura principal (pageimages) del artículo hallado
            String imageEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=500&titles="
                    + java.net.URLEncoder.encode(tituloEncontrado, "UTF-8");
            String imageRespuesta = leerUrlHttp(imageEndpoint);
            if (imageRespuesta == null) return null;

            // Navega por la estructura JSON hasta obtener el objeto thumbnail
            org.json.JSONObject jsonObject = new org.json.JSONObject(imageRespuesta);
            org.json.JSONObject query = jsonObject.optJSONObject("query");
            if (query == null) return null;

            org.json.JSONObject pages = query.optJSONObject("pages");
            if (pages == null || !pages.keys().hasNext()) return null;

            String primeraClave = pages.keys().next();
            org.json.JSONObject pagina = pages.optJSONObject(primeraClave);
            if (pagina == null) return null;

            org.json.JSONObject thumbnail = pagina.optJSONObject("thumbnail");
            if (thumbnail != null) {
                String sourceUrl = thumbnail.optString("source");
                // Filtra imágenes relacionadas con accidentes o siniestros aéreos
                String urlLower = sourceUrl.toLowerCase(Locale.ROOT);
                if (urlLower.contains("accident") || urlLower.contains("crash") ||
                        urlLower.contains("incident") || urlLower.contains("damage") ||
                        urlLower.contains("destroy") || urlLower.contains("fire") ||
                        urlLower.contains("overrun") || urlLower.contains("wrecks")) {
                    return null;
                }
                // Devuelve la URL limpia de la miniatura
                return sourceUrl;
            }
        } catch (Exception e) {
            // Registra cualquier error ocurrido durante la consulta o parseo JSON
            android.util.Log.e("GestorAviones", "Error al buscar en Wikipedia: " + e.getMessage());
        }
        return null;
    }

    /**
     * Realiza una petición HTTP GET segura con User-Agent y cierre garantizado de conexión.
     */
    private String leerUrlHttp(String endpointUrl) {
        java.net.HttpURLConnection conexion = null;
        try {
            // Abre la conexión HTTP configurando método GET, User-Agent y tiempos de espera
            java.net.URL url = new java.net.URL(endpointUrl);
            conexion = (java.net.HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setRequestProperty("User-Agent", AvionGenerator.USER_AGENT);
            conexion.setConnectTimeout(3000);
            conexion.setReadTimeout(3000);

            // Verifica que el código de respuesta HTTP sea 200 (OK)
            if (conexion.getResponseCode() != 200) return null;

            // Lee el flujo de entrada línea por línea en UTF-8
            java.io.BufferedReader lector = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conexion.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                respuesta.append(linea);
            }
            // Cierra el lector y devuelve el contenido completo
            lector.close();
            return respuesta.toString();
        } catch (Exception e) {
            return null;
        } finally {
            // Garantiza la desconexión del socket HTTP en cualquier escenario
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    /**
     * Despliega un visor de imagen a pantalla completa con zoom fluido hacia el punto focal,
     * doble toque, arrastre sin saltos entre dedos y límites de borde para no perder la foto.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void abrirImagenAmpliada(Object fuenteImagen) {
        // Crea el contenedor principal negro a pantalla completa
        FrameLayout contenedor = new FrameLayout(this);
        contenedor.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        contenedor.setBackgroundColor(Color.BLACK);

        // Crea el ImageView configurado en modo MATRIX para soportar transformaciones de zoom y pan
        ImageView imgZoom = new ImageView(this);
        imgZoom.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imgZoom.setScaleType(ImageView.ScaleType.MATRIX);
        contenedor.addView(imgZoom);

        // Crea y posiciona el botón de cierre en la esquina superior derecha
        ImageButton btnCerrar = new ImageButton(this);
        FrameLayout.LayoutParams paramsBtn = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        paramsBtn.gravity = Gravity.TOP | Gravity.END;
        paramsBtn.setMargins(40, 40, 40, 40);
        btnCerrar.setLayoutParams(paramsBtn);
        btnCerrar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnCerrar.setBackgroundColor(Color.parseColor("#99000000"));
        btnCerrar.setColorFilter(Color.WHITE);
        contenedor.addView(btnCerrar);

        // Crea el diálogo a pantalla completa sin barra de título y vincula el botón cerrar
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(contenedor);
        dialog.setCancelable(true);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        // Variables de estado para transformaciones matriciales de escala y traslación
        final android.graphics.Matrix matriz = new android.graphics.Matrix();
        final float[] valoresMatriz = new float[9];
        final float[] escalaBase = {1.0f};
        final float[] escalaActual = {1.0f};
        final float[] ultimoX = {0f};
        final float[] ultimoY = {0f};
        final int[] punteroActivoId = {MotionEvent.INVALID_POINTER_ID};
        final boolean[] inicializado = {false};

        // Calcula la escala inicial para ajustar la imagen centrada dentro de la pantalla
        Runnable centrarImagenInicial = () -> {
            // Comprueba que el drawable y las dimensiones de la vista ya estén medidos
            if (imgZoom.getDrawable() == null || imgZoom.getWidth() == 0 || imgZoom.getHeight() == 0) return;

            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();
            float imgW = imgZoom.getDrawable().getIntrinsicWidth();
            float imgH = imgZoom.getDrawable().getIntrinsicHeight();
            if (imgW <= 0 || imgH <= 0) return;

            // Calcula el factor de escala para encajar la imagen completa y sus desplazamientos de centrado
            float escala = Math.min(viewW / imgW, viewH / imgH);
            float dx = (viewW - imgW * escala) * 0.5f;
            float dy = (viewH - imgH * escala) * 0.5f;

            // Reinicia la matriz y aplica escala y centrado inicial
            matriz.reset();
            matriz.postScale(escala, escala);
            matriz.postTranslate(dx, dy);
            imgZoom.setImageMatrix(matriz);

            // Guarda los valores base de referencia
            escalaBase[0] = escala;
            escalaActual[0] = 1.0f;
            inicializado[0] = true;
        };

        // Restringe los desplazamientos para evitar que la imagen salga de los bordes visibles
        Runnable ajustarLimitesBordes = () -> {
            if (imgZoom.getDrawable() == null) return;
            // Extrae los valores actuales de traslación y escala de la matriz
            matriz.getValues(valoresMatriz);
            float transX = valoresMatriz[android.graphics.Matrix.MTRANS_X];
            float transY = valoresMatriz[android.graphics.Matrix.MTRANS_Y];
            float escalaReal = valoresMatriz[android.graphics.Matrix.MSCALE_X];

            // Calcula las dimensiones reales escaladas de la imagen en pantalla
            float anchoContenido = imgZoom.getDrawable().getIntrinsicWidth() * escalaReal;
            float altoContenido = imgZoom.getDrawable().getIntrinsicHeight() * escalaReal;
            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();

            float correccionX = 0f;
            float correccionY = 0f;

            // Calcula la corrección horizontal para centrar si es menor que la pantalla o fijar bordes
            if (anchoContenido <= viewW) {
                correccionX = (viewW - anchoContenido) * 0.5f - transX;
            } else if (transX > 0) {
                correccionX = -transX;
            } else if (transX + anchoContenido < viewW) {
                correccionX = viewW - (transX + anchoContenido);
            }

            // Calcula la corrección vertical para centrar si es menor que la pantalla o fijar bordes
            if (altoContenido <= viewH) {
                correccionY = (viewH - altoContenido) * 0.5f - transY;
            } else if (transY > 0) {
                correccionY = -transY;
            } else if (transY + altoContenido < viewH) {
                correccionY = viewH - (transY + altoContenido);
            }

            // Aplica la traslación correctiva y actualiza la matriz de la imagen
            matriz.postTranslate(correccionX, correccionY);
            imgZoom.setImageMatrix(matriz);
        };

        // Detector de gesto de pellizco para ampliar o reducir entre 1x y 6x
        ScaleGestureDetector detectorEscala = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!inicializado[0]) return false;
                // Calcula y acota el nuevo factor de escala dentro del rango permitido [1.0, 6.0]
                float factor = detector.getScaleFactor();
                float nuevaEscala = escalaActual[0] * factor;
                nuevaEscala = Math.max(1.0f, Math.min(nuevaEscala, 6.0f));
                float factorAplicado = nuevaEscala / escalaActual[0];
                escalaActual[0] = nuevaEscala;

                // Aplica el escalado tomando como pivote el punto focal entre los dedos
                matriz.postScale(factorAplicado, factorAplicado, detector.getFocusX(), detector.getFocusY());
                ajustarLimitesBordes.run();
                return true;
            }
        });

        // Detector de doble toque para alternar rápidamente entre zoom 2.5x y vista normal
        android.view.GestureDetector detectorDobleToque = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!inicializado[0]) return true;
                if (escalaActual[0] > 1.05f) {
                    // Si ya estaba ampliada, restaura el encuadre inicial centrado
                    centrarImagenInicial.run();
                } else {
                    // Si estaba en vista normal, amplía a 2.5x hacia el punto pulsado
                    float factorObjetivo = 2.5f / escalaActual[0];
                    escalaActual[0] = 2.5f;
                    matriz.postScale(factorObjetivo, factorObjetivo, e.getX(), e.getY());
                    ajustarLimitesBordes.run();
                }
                return true;
            }
        });

        // Gestiona el movimiento táctil con un dedo y el cambio fluido entre punteros multitáctiles
        imgZoom.setOnTouchListener((v, event) -> {
            // Asegura la inicialización en el primer contacto táctil
            if (!inicializado[0]) centrarImagenInicial.run();
            // Delega el evento a los detectores de doble toque y pellizco
            detectorDobleToque.onTouchEvent(event);
            detectorEscala.onTouchEvent(event);

            final int accion = event.getActionMasked();
            switch (accion) {
                case MotionEvent.ACTION_DOWN: {
                    // Registra las coordenadas iniciales y el ID del primer dedo que toca la pantalla
                    int index = event.getActionIndex();
                    ultimoX[0] = event.getX(index);
                    ultimoY[0] = event.getY(index);
                    punteroActivoId[0] = event.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // Si no se está pellizcando y hay zoom activo, desplaza la imagen siguiendo al dedo
                    if (!detectorEscala.isInProgress() && escalaActual[0] > 1.0f) {
                        int pointerIndex = event.findPointerIndex(punteroActivoId[0]);
                        if (pointerIndex >= 0) {
                            float x = event.getX(pointerIndex);
                            float y = event.getY(pointerIndex);
                            matriz.postTranslate(x - ultimoX[0], y - ultimoY[0]);
                            ajustarLimitesBordes.run();
                            ultimoX[0] = x;
                            ultimoY[0] = y;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    // Libera el identificador del puntero activo al levantar todos los dedos
                    punteroActivoId[0] = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    // Si se levanta el dedo principal mientras queda otro apoyado, transfiere el control sin saltos
                    int pointerIndex = event.getActionIndex();
                    int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == punteroActivoId[0]) {
                        int nuevoIndice = (pointerIndex == 0) ? 1 : 0;
                        ultimoX[0] = event.getX(nuevoIndice);
                        ultimoY[0] = event.getY(nuevoIndice);
                        punteroActivoId[0] = event.getPointerId(nuevoIndice);
                    }
                    break;
                }
            }
            return true;
        });

        // Prepara la fuente final convirtiendo rutas de archivo locales en Uri si es necesario
        Object cargaFinal = fuenteImagen;
        if (fuenteImagen instanceof String) {
            String str = (String) fuenteImagen;
            if (str.startsWith("content://") || str.startsWith("file://") || str.startsWith("/")) {
                cargaFinal = android.net.Uri.parse(str);
            }
        }

        // Carga la imagen con Glide y ejecuta el centrado inicial en cuanto el recurso está listo
        Glide.with(this)
                .load(cargaFinal)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model,
                                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                                   com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                   com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        // Ejecuta el centrado tras el renderizado del ImageView
                        imgZoom.post(centrarImagenInicial);
                        return false;
                    }
                })
                .into(imgZoom);

        // Muestra el diálogo modal a pantalla completa
        dialog.show();
    }
}