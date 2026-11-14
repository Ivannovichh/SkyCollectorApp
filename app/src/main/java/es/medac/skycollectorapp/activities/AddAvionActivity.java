// Declaración del paquete donde se ubica la actividad de añadir aviones
package es.medac.skycollectorapp.activities;

// Importaciones de sistema y utilidades de Android
import android.Manifest; // Permisos de cámara
import android.content.Context; // Contexto de la aplicación
import android.content.SharedPreferences; // Almacenamiento local clave-valor
import android.content.pm.PackageManager; // Comprobación de permisos
import android.net.Uri; // Rutas de archivos de imagen
import android.os.Bundle; // Datos del ciclo de vida
import android.os.Environment; // Directorios de almacenamiento
import android.util.Log; // Registros en Logcat
import android.widget.ArrayAdapter; // Adaptador para el Spinner
import android.widget.Button; // Botones de acción
import android.widget.ImageView; // Vista previa de la fotografía
import android.widget.Spinner; // Selector desplegable de modelos
import android.widget.Toast; // Mensajes emergentes breves

// Importaciones de AndroidX y librerías externas
import androidx.activity.OnBackPressedCallback; // Control del botón físico/gestual atrás
import androidx.activity.result.ActivityResultLauncher; // Lanzador de cámara y permisos
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar de resultados
import androidx.appcompat.app.AlertDialog; // Diálogo de confirmación para cámara
import androidx.appcompat.app.AppCompatActivity; // Actividad base
import androidx.core.content.ContextCompat; // Utilidad de compatibilidad de permisos
import androidx.core.content.FileProvider; // Proveedor seguro de URIs de archivo
import com.bumptech.glide.Glide; // Carga y escalado de imágenes
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Control de caché de imágenes
import com.google.firebase.auth.FirebaseAuth; // Autenticación de Firebase
import com.google.firebase.auth.FirebaseUser; // Usuario actual de Firebase
import com.google.firebase.firestore.FirebaseFirestore; // Base de datos en la nube Firestore
import com.google.gson.Gson; // Serialización JSON
import com.google.gson.reflect.TypeToken; // Conservación de tipos genéricos

// Importaciones estándar de Java
import java.io.File; // Gestión de archivos
import java.io.IOException; // Excepciones de entrada/salida
import java.lang.reflect.Type; // Tipo genérico para Gson
import java.text.SimpleDateFormat; // Formateo de marcas de tiempo
import java.util.ArrayList; // Lista dinámica
import java.util.Date; // Fecha actual
import java.util.List; // Interfaz de listas
import java.util.Locale; // Configuración regional

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos de la app
import es.medac.skycollectorapp.models.Avion; // Modelo Avión
import es.medac.skycollectorapp.utils.AvionGenerator; // Catálogo de aeronaves

// Actividad para fotografiar en directo y registrar un avión detectado en el radar
public class AddAvionActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ Y ESTADO
    // ------------------------------
    private Spinner spinnerAviones; // Desplegable bloqueado con el modelo detectado
    private ImageView imgPreviewAvion; // Previsualización de la foto tomada
    private Button btnSubirFoto, btnGuardar, btnCancelar; // Botones de cámara, guardado y cancelación

    private Uri uriImagenFinal; // URI definitiva de la fotografía capturada
    private Uri uriFotoCamaraTemporal; // URI temporal creada antes de abrir la cámara
    private List<Avion> listaAvionesBase; // Lista completa del catálogo
    private String icaoSeleccionado; // Código ICAO recibido desde MapaActivity

    // ------------------------------
    // LANZADORES DE ACTIVIDADES
    // ------------------------------

    // Solicita permiso de cámara en tiempo de ejecución
    private final ActivityResultLauncher<String> launcherPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) abrirCamara(); // Abrir cámara si concede el permiso
                else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
            });

    // Recibe la fotografía tomada por la cámara del sistema
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && uriFotoCamaraTemporal != null) cargarImagen(uriFotoCamaraTemporal); // Mostrar foto si fue exitosa
            });

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    // Inicializa la pantalla, recupera los datos del radar y configura los botones
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_avion); // Cargar diseño XML

        // Vincular componentes visuales
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Recuperar el código ICAO enviado desde el mapa
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        icaoSeleccionado = prefs.getString("icao_seleccionado", null);

        if (icaoSeleccionado == null) {
            Toast.makeText(this, "Primero selecciona y avista un avión en el mapa", Toast.LENGTH_LONG).show();
            finish(); // Cerrar si no hay selección previa válida
            return;
        }

        cargarDatosGenerator(); // Cargar catálogo de aviones en el Spinner
        fijarModeloDetectado(prefs.getString("tipo_seleccionado", null), prefs.getString("descripcion_seleccionada", null)); // Bloquear modelo detectado

        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion()); // Abrir cámara
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo()); // Guardar avistamiento

        // Al pulsar Cancelar o retroceder, limpiar datos pendientes y volver al mapa
        btnCancelar.setOnClickListener(v -> cancelarYVolverAlMapa());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelarYVolverAlMapa();
            }
        });
    }

    // Limpia el avistamiento pendiente y cierra la pantalla para regresar al mapa
    private void cancelarYVolverAlMapa() {
        limpiarAvistamientoPendiente();
        finish();
    }

    // ------------------------------
    // LÓGICA DE DATOS Y GUARDADO
    // ------------------------------

    // Genera la clave de almacenamiento local vinculada al UID del usuario actual
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    // Carga todos los modelos del catálogo estático
    private void cargarDatosGenerator() {
        listaAvionesBase = AvionGenerator.getTodosLosAviones();
        actualizarSpinner();
    }

    // Rellena el Spinner con el nombre y rareza de cada modelo
    private void actualizarSpinner() {
        List<String> nombres = new ArrayList<>();
        for (Avion a : listaAvionesBase) {
            nombres.add(a.getModelo() + " (" + a.getRareza() + ")");
        }
        spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));
    }

    // Construye el objeto Avion con la telemetría del radar y lo guarda localmente y en Firestore
    private void guardarAvionCatalogo() {
        if (icaoSeleccionado == null) {
            Toast.makeText(this, "Primero selecciona un avión en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (yaAvistado(icaoSeleccionado)) {
            Toast.makeText(this, "Ya has avistado este avión anteriormente", Toast.LENGTH_SHORT).show();
            limpiarAvistamientoPendiente();
            finish();
            return;
        }

        int pos = spinnerAviones.getSelectedItemPosition();
        if (pos < 0) return;

        Avion base = listaAvionesBase.get(pos);
        Avion nuevo = new Avion(base.getModelo(), base.getFabricante(), base.getRareza(), base.getImagenResId(),
                base.getVelocidad(), base.getPasajeros(), base.getDimensiones(), base.getPais(), base.getPeso(), icaoSeleccionado);

        SharedPreferences prefsAv = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        nuevo.setMatricula(prefsAv.getString("matricula_seleccionada", null));
        nuevo.setTipoIcao(prefsAv.getString("tipo_seleccionado", null));
        nuevo.setFechaAvistamiento(prefsAv.getLong("fecha_seleccionada", System.currentTimeMillis()));
        nuevo.setDistanciaKm(prefsAv.getFloat("distancia_seleccionada", 0f));
        nuevo.setLatAvion(prefsAv.getFloat("lat_avion_seleccionado", 0f));
        nuevo.setLonAvion(prefsAv.getFloat("lon_avion_seleccionado", 0f));
        nuevo.setOrigen("MAPA");

        // Solo puntúa en el ranking si la foto se toma directamente al avistar desde el mapa
        if (uriImagenFinal != null) {
            nuevo.setUriFotoUsuario(uriImagenFinal.toString());
            nuevo.setEsFotoVerificada(true);
        } else {
            nuevo.setEsFotoVerificada(false);
        }

        guardarEnPreferencias(nuevo);
        guardarEnFirestore(nuevo);
        limpiarAvistamientoPendiente();

        String mensaje = nuevo.isEsFotoVerificada()
                ? "Avistamiento verificado guardado (puntúa en ranking)"
                : "Avistamiento guardado sin foto (no puntúa en ranking)";
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        finish();
    }

    // Añade el avión capturado a la lista JSON local del usuario activo
    private void guardarEnPreferencias(Avion nuevo) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String clave = getClaveListaUsuario();
        String json = prefs.getString(clave, null);
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        List<Avion> lista = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        if (lista == null) lista = new ArrayList<>();
        lista.add(nuevo);
        prefs.edit().putString(clave, gson.toJson(lista)).apply();
    }

    // Guarda el avistamiento en la colección del usuario dentro de Firebase Firestore
    private void guardarEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(auth.getCurrentUser().getUid())
                .collection("aviones").document(avion.getId()).set(avion)
                .addOnSuccessListener(v -> Log.d("FIRESTORE", "Sincronizado"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error", e));
    }

    // ------------------------------
    // LÓGICA DE CÁMARA Y CATALOGACIÓN
    // ------------------------------

    // Muestra el aviso de captura directa por cámara (sin galería para evitar trampas)
    private void mostrarDialogoSeleccion() {
        new AlertDialog.Builder(this)
                .setTitle("Añadir foto del avistamiento")
                .setMessage("Para el ranking oficial, solo se admiten fotos tomadas en el momento.")
                .setPositiveButton("Hacer foto", (d, i) -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        abrirCamara(); // Abrir cámara si ya tiene permiso
                    } else {
                        launcherPermisos.launch(Manifest.permission.CAMERA); // Solicitar permiso de cámara
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Crea el archivo temporal y abre la aplicación de cámara del dispositivo
    private void abrirCamara() {
        try {
            File f = crearArchivoImagen();
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    // Crea un fichero JPEG único en el almacenamiento privado de imágenes
    private File crearArchivoImagen() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir != null && !dir.exists()) dir.mkdirs();
        return File.createTempFile("IMG_" + ts + "_", ".jpg", dir);
    }

    // Carga la imagen capturada en el ImageView sin usar caché antigua
    private void cargarImagen(Uri uri) {
        uriImagenFinal = uri;
        Glide.with(this).load(uri).centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imgPreviewAvion);
    }

    // Selecciona automáticamente el modelo detectado por el radar y bloquea el Spinner
    private void fijarModeloDetectado(String tipoIcao, String descripcion) {
        if (icaoSeleccionado == null) return;
        Avion detectado = AvionGenerator.getAvionPorTipoIcao(tipoIcao);
        if (detectado != null) {
            for (int i = 0; i < listaAvionesBase.size(); i++) {
                if (listaAvionesBase.get(i).getModelo().equals(detectado.getModelo())) {
                    spinnerAviones.setSelection(i); // Seleccionar modelo coincidente
                    break;
                }
            }
        } else {
            String nombre = (descripcion != null && !descripcion.isEmpty()) ? descripcion : "Aeronave " + tipoIcao;
            Avion generico = new Avion(nombre, "Sin catalogar", "COMMON", R.drawable.sil_jet, "—", "—", "—", "—", "—");
            listaAvionesBase.add(0, generico); // Añadir ficha genérica si no está en catálogo
            actualizarSpinner();
            spinnerAviones.setSelection(0);
        }
        spinnerAviones.setEnabled(false); // Bloquear selección manual
    }

    // Comprueba si el avión con ese código ICAO ya fue registrado previamente por el usuario
    private boolean yaAvistado(String icao) {
        if (icao == null) return false;
        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString(getClaveListaUsuario(), null);
        if (json == null) return false;
        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista == null) return false;
        for (Avion a : lista) {
            if (icao.equalsIgnoreCase(a.getIcao24())) return true;
        }
        return false;
    }

    // Borra las claves temporales del avistamiento pendiente en SharedPreferences
    private void limpiarAvistamientoPendiente() {
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE).edit()
                .remove("icao_seleccionado").remove("tipo_seleccionado").remove("matricula_seleccionada")
                .remove("descripcion_seleccionada").remove("fecha_seleccionada").remove("distancia_seleccionada")
                .remove("lat_avion_seleccionado").remove("lon_avion_seleccionado").apply();
    }
}