// Declaración del paquete donde se ubica la actividad de añadir aviones
package es.medac.skycollectorapp.activities;

// Importaciones de sistema y utilidades de Android
import android.Manifest; // Permisos de cámara
import android.content.Context; // Contexto de la app
import android.content.SharedPreferences; // Persistencia local
import android.content.pm.PackageManager; // Gestor de permisos
import android.net.Uri; // Direcciones de archivos
import android.os.Bundle; // Pases de datos entre estados
import android.os.Environment; // Directorios del sistema
import android.util.Log; // Logs de depuración
import android.widget.ArrayAdapter; // Adaptador de Spinner
import android.widget.Button; // Botones
import android.widget.ImageView; // Imágenes
import android.widget.Spinner; // Lista desplegable
import android.widget.Toast; // Mensajes flotantes

// Importaciones de AndroidX y librerías externas
import androidx.activity.result.ActivityResultLauncher; // Lanzadores de resultados
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar
import androidx.appcompat.app.AlertDialog; // Diálogos emergentes
import androidx.appcompat.app.AppCompatActivity; // Base de actividad
import androidx.core.content.ContextCompat; // Compatibilidad de contexto
import androidx.core.content.FileProvider; // Proveedor de archivos seguro
import com.bumptech.glide.Glide; // Carga de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Caché de imágenes
import com.google.firebase.auth.FirebaseAuth; // Auth de Firebase
import com.google.firebase.firestore.FirebaseFirestore; // DB Firestore
import com.google.gson.Gson; // JSON
import com.google.gson.reflect.TypeToken; // Tipos para Gson

// Importaciones estándar de Java
import java.io.File; // Archivos
import java.io.IOException; // Errores de entrada/salida
import java.lang.reflect.Type; // Reflexión de tipos
import java.text.SimpleDateFormat; // Formato de fecha
import java.util.ArrayList; // Listas dinámicas
import java.util.Date; // Fechas
import java.util.List; // Interfaz lista
import java.util.Locale; // Configuración regional

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos
import es.medac.skycollectorapp.models.Avion; // Modelo Avión
import es.medac.skycollectorapp.utils.AvionGenerator; // Catálogo

// Clase que gestiona el registro de avistamientos verificados con foto de cámara
public class AddAvionActivity extends AppCompatActivity {

    // Componentes de interfaz
    private Spinner spinnerAviones; // Selector de modelos
    private ImageView imgPreviewAvion; // Previsualización de la foto
    private Button btnSubirFoto, btnGuardar, btnCancelar; // Acciones

    // Variables de estado y multimedia
    private Uri uriImagenFinal; // URI de la foto para guardar
    private Uri uriFotoCamaraTemporal; // URI temporal para la captura
    private List<Avion> listaAvionesBase; // Catálogo de aviones
    private String icaoSeleccionado; // ICAO detectado en el radar

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Al iniciar la pantalla
        super.onCreate(savedInstanceState); // Llamada base
        setContentView(R.layout.activity_add_avion); // Cargar diseño

        // Inicializar componentes visuales
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Recuperar datos del radar
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        icaoSeleccionado = prefs.getString("icao_seleccionado", null);

        if (icaoSeleccionado == null) { // Si el usuario entró aquí sin cazar nada en el mapa
            Toast.makeText(this, "No has seleccionado ningún avión en el mapa", Toast.LENGTH_LONG).show();
        }

        cargarDatosGenerator(); // Llenar el Spinner con los modelos

        // Bloquear el Spinner en el modelo que el radar ha detectado automáticamente
        fijarModeloDetectado(prefs.getString("tipo_seleccionado", null), prefs.getString("descripcion_seleccionada", null));

        // Configurar botones
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion()); // Diálogo de cámara segura
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo()); // Guardar con verificación
        btnCancelar.setOnClickListener(v -> finish()); // Cerrar
    }

    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones(); // Leer catálogo
        actualizarSpinner(); // Pintar en el selector
    }

    private void actualizarSpinner() {
        List<String> nombres = new ArrayList<>(); // Lista de etiquetas
        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")"); // Formatear nombre + rareza
        }
        spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres)); // Asignar
    }

    // Procesa el guardado del avión marcándolo como verificado si se tomó una foto real
    private void guardarAvionCatalogo() {
        if (icaoSeleccionado == null) { // Validar selección previa
            Toast.makeText(this, "Primero selecciona un avión en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (yaAvistado(icaoSeleccionado)) { // Evitar duplicar la misma matrícula
            Toast.makeText(this, "Ya has avistado este avión anteriormente", Toast.LENGTH_SHORT).show();
            limpiarAvistamientoPendiente();
            return;
        }

        int pos = spinnerAviones.getSelectedItemPosition(); // Obtener posición elegida
        if (pos < 0) return;

        Avion base = listaAvionesBase.get(pos); // Obtener datos técnicos base
        Avion nuevo = new Avion(base.getModelo(), base.getFabricante(), base.getRareza(), base.getImagenResId(),
                base.getVelocidad(), base.getPasajeros(), base.getDimensiones(), base.getPais(), base.getPeso(), icaoSeleccionado);

        // Volcar telemetría del radar al objeto definitivo
        SharedPreferences prefsAv = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        nuevo.setMatricula(prefsAv.getString("matricula_seleccionada", null));
        nuevo.setTipoIcao(prefsAv.getString("tipo_seleccionado", null));
        nuevo.setFechaAvistamiento(prefsAv.getLong("fecha_seleccionada", System.currentTimeMillis()));
        nuevo.setDistanciaKm(prefsAv.getFloat("distancia_seleccionada", 0f));
        nuevo.setLatAvion(prefsAv.getFloat("lat_avion_seleccionado", 0f));
        nuevo.setLonAvion(prefsAv.getFloat("lon_avion_seleccionado", 0f));
        nuevo.setOrigen("MAPA");

        // GESTIÓN DE RANKING: Si hay foto, marcarla como verificada (anti-trampas)
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString()); // Guardar ruta
            nuevo.setEsFotoVerificada(true); // Activar bandera de autenticidad para el ranking
        }

        guardarEnPreferencias(nuevo); // Guardar JSON local
        guardarEnFirestore(nuevo); // Sincronizar nube
        limpiarAvistamientoPendiente(); // Borrar datos temporales

        Toast.makeText(this, "Avistamiento verificado guardado", Toast.LENGTH_SHORT).show();
        finish(); // Volver
    }

    private void guardarEnPreferencias(Avion nuevo) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("lista_aviones", null);
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        List<Avion> lista = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        if (lista == null) lista = new ArrayList<>();
        lista.add(nuevo);
        prefs.edit().putString("lista_aviones", gson.toJson(lista)).apply();
    }

    private void guardarEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(auth.getCurrentUser().getUid())
                .collection("aviones").document(avion.getId()).set(avion)
                .addOnSuccessListener(v -> Log.d("FIRESTORE", "Sincronizado"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error", e));
    }

    // Muestra un diálogo que fuerza el uso de la cámara (Eliminada opción de Galería para el Ranking)
    private void mostrarDialogoSeleccion() {
        new AlertDialog.Builder(this)
                .setTitle("Añadir foto del avistamiento") // Título
                .setMessage("Para el ranking oficial, solo se admiten fotos tomadas en el momento.") // Explicación de seguridad
                .setPositiveButton("Hacer foto", (d, i) -> { // Opción única segura
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        abrirCamara(); // Abrir si hay permiso
                    } else {
                        launcherPermisos.launch(Manifest.permission.CAMERA); // Pedir permiso
                    }
                })
                .setNegativeButton("Cancelar", null) // Salir
                .show(); // Mostrar
    }

    private final ActivityResultLauncher<String> launcherPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) abrirCamara(); // Abrir si se acepta el permiso
                else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && uriFotoCamaraTemporal != null) cargarImagen(uriFotoCamaraTemporal); // Cargar si salió bien
            });

    private void abrirCamara() {
        try {
            File f = crearArchivoImagen(); // Crear destino
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f); // URI segura
            launcherCamara.launch(uriFotoCamaraTemporal); // Disparar cámara
        } catch (Exception e) { e.printStackTrace(); }
    }

    private File crearArchivoImagen() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // Directorio persistente
        if (dir != null && !dir.exists()) dir.mkdirs();
        return File.createTempFile("IMG_" + ts + "_", ".jpg", dir); // Crear temporal real
    }

    private void cargarImagen(Uri uri) {
        uriImagenFinal = uri; // Guardar URI definitiva
        Glide.with(this).load(uri).centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imgPreviewAvion); // Cargar en UI
    }

    private void fijarModeloDetectado(String tipoIcao, String descripcion) {
        if (icaoSeleccionado == null) return;
        Avion detectado = AvionGenerator.getAvionPorTipoIcao(tipoIcao);
        if (detectado != null) {
            for (int i = 0; i < listaAvionesBase.size(); i++) {
                if (listaAvionesBase.get(i).getModelo().equals(detectado.getModelo())) {
                    spinnerAviones.setSelection(i);
                    break;
                }
            }
        } else {
            String nombre = (descripcion != null && !descripcion.isEmpty()) ? descripcion : "Aeronave " + tipoIcao;
            Avion generico = new Avion(nombre, "Sin catalogar", "COMMON", R.drawable.sil_jet, "—", "—", "—", "—", "—");
            listaAvionesBase.add(0, generico);
            actualizarSpinner();
            spinnerAviones.setSelection(0);
        }
        spinnerAviones.setEnabled(false); // Impedir cambio manual para evitar trampas en el catálogo
    }

    private boolean yaAvistado(String icao) {
        if (icao == null) return false;
        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString("lista_aviones", null);
        if (json == null) return false;
        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista == null) return false;
        for (Avion a : lista) { if (icao.equalsIgnoreCase(a.getIcao24())) return true; }
        return false;
    }

    private void limpiarAvistamientoPendiente() {
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE).edit()
                .remove("icao_seleccionado").remove("tipo_seleccionado").remove("matricula_seleccionada")
                .remove("descripcion_seleccionada").remove("fecha_seleccionada").remove("distancia_seleccionada")
                .remove("lat_avion_seleccionado").remove("lon_avion_seleccionado").apply();
    }
}