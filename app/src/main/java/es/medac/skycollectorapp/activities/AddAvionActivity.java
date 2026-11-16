// Paquete que agrupa las actividades de la aplicación
package es.medac.skycollectorapp.activities;

// Importación de permisos del sistema Android
import android.Manifest;
// Importación de Context para acceder a SharedPreferences
import android.content.Context;
// Importación de SharedPreferences para leer y guardar datos locales
import android.content.SharedPreferences;
// Importación de PackageManager para verificar si el permiso de cámara está concedido
import android.content.pm.PackageManager;
// Importación de GradientDrawable para aplicar el degradado dorado en Easter Eggs
import android.graphics.drawable.GradientDrawable;
// Importación de Uri para manejar las rutas de las fotografías capturadas
import android.net.Uri;
// Importación de Bundle para el ciclo de vida de la actividad
import android.os.Bundle;
// Importación de Environment para acceder al directorio de imágenes de la app
import android.os.Environment;
// Importación de Log para registrar eventos de sincronización
import android.util.Log;
// Importación de View y ViewGroup para estilizar la vista raíz en modo Easter Egg
import android.view.View;
import android.view.ViewGroup;
// Importación de ArrayAdapter para poblar el Spinner
import android.widget.ArrayAdapter;
// Importación de Button para los botones de acción
import android.widget.Button;
// Importación de ImageView para la vista previa de la foto tomada
import android.widget.ImageView;
// Importación de Spinner para mostrar el modelo identificado
import android.widget.Spinner;
// Importación de Toast para notificaciones breves al usuario
import android.widget.Toast;

// Importación para interceptar el botón físico/gestual de retroceso
import androidx.activity.OnBackPressedCallback;
// Importación de ActivityResultLauncher para gestionar resultados de cámara y permisos
import androidx.activity.result.ActivityResultLauncher;
// Importación de contratos estándar de resultados de AndroidX
import androidx.activity.result.contract.ActivityResultContracts;
// Importación de AlertDialog para mostrar el aviso de verificación de foto
import androidx.appcompat.app.AlertDialog;
// Importación de AppCompatActivity como clase base
import androidx.appcompat.app.AppCompatActivity;
// Importación de ContextCompat para comprobar permisos de forma compatible
import androidx.core.content.ContextCompat;
// Importación de FileProvider para compartir URIs seguras con la app de cámara
import androidx.core.content.FileProvider;

// Importación de Glide para cargar y recortar la foto capturada
import com.bumptech.glide.Glide;
// Importación de DiskCacheStrategy para evitar caché antigua en fotos nuevas
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// Importación de FirebaseAuth para identificar al usuario actual
import com.google.firebase.auth.FirebaseAuth;
// Importación de FirebaseUser para obtener el UID del usuario
import com.google.firebase.auth.FirebaseUser;
// Importación de FirebaseFirestore para guardar el avistamiento en la nube
import com.google.firebase.firestore.FirebaseFirestore;
// Importación de Gson para serializar la lista de aviones a JSON
import com.google.gson.Gson;
// Importación de TypeToken para deserializar listas genéricas con Gson
import com.google.gson.reflect.TypeToken;

// Importación de File para crear el archivo temporal de la cámara
import java.io.File;
// Importación de IOException para manejar errores de escritura en disco
import java.io.IOException;
// Importación de Type para tipos genéricos
import java.lang.reflect.Type;
// Importación de SimpleDateFormat para nombrar archivos con fecha y hora
import java.text.SimpleDateFormat;
// Importación de ArrayList para listas dinámicas
import java.util.ArrayList;
// Importación de Date para obtener la marca de tiempo actual
import java.util.Date;
// Importación de List como interfaz de colección
import java.util.List;
// Importación de Locale para formato regional
import java.util.Locale;

// Importación de recursos de la aplicación
import es.medac.skycollectorapp.R;
// Importación del modelo Avion
import es.medac.skycollectorapp.models.Avion;
// Importación del motor global de identificación de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator;
// Importación de la utilidad de compresión Base64 para compartir fotos en Firestore
import es.medac.skycollectorapp.utils.ImagenBase64Utils;
// Importación del gestor de puntuación y clasificación pública
import es.medac.skycollectorapp.utils.RankingManager;

/**
 * Actividad para fotografiar en directo y registrar un avión o vehículo Easter Egg detectado en el radar.
 */
public class AddAvionActivity extends AppCompatActivity {

    // Componentes visuales del formulario de avistamiento
    private Spinner spinnerAviones;
    private ImageView imgPreviewAvion;
    private Button btnSubirFoto, btnGuardar, btnCancelar;

    // URIs para controlar la foto capturada por el usuario
    private Uri uriImagenFinal;
    private Uri uriFotoCamaraTemporal;
    // Lista interna para el adaptador del Spinner
    private List<Avion> listaAvionesBase;
    // Código ICAO24 hexadecimal del avión seleccionado en el mapa
    private String icaoSeleccionado;
    // Instancia del avión una vez resuelto por el motor global (Catálogo local + Wikipedia + Easter Eggs)
    private Avion avionDetectadoResuelto;
    // Indica si el blanco seleccionado corresponde a un Easter Egg terrestre
    private boolean esEasterEgg;

    // Lanzador que solicita permiso de cámara en tiempo de ejecución
    private final ActivityResultLauncher<String> launcherPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) abrirCamara();
                else Toast.makeText(this, getString(R.string.toast_permiso_camara), Toast.LENGTH_SHORT).show();
            });

    // Lanzador que recibe la confirmación de foto tomada por la cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && uriFotoCamaraTemporal != null) cargarImagen(uriFotoCamaraTemporal);
            });

    /**
     * Inicializa la pantalla, recupera los datos temporales del radar, aplica tema dorado si es Easter Egg
     * y resuelve la aeronave o vehículo terrestre.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_avion);

        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        icaoSeleccionado = prefs.getString("icao_seleccionado", null);
        String callsignSeleccionado = prefs.getString("callsign_seleccionado", null);
        String matriculaSeleccionada = prefs.getString("matricula_seleccionada", null);
        String tipoSeleccionado = prefs.getString("tipo_seleccionado", null);
        String descripcionSeleccionada = prefs.getString("descripcion_seleccionada", null);

        if (icaoSeleccionado == null) {
            Toast.makeText(this, getString(R.string.toast_selecciona_mapa_primero), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        esEasterEgg = prefs.getBoolean("es_easter_egg_seleccionado", false)
                || AvionGenerator.esEasterEggTerrestre(tipoSeleccionado, callsignSeleccionado, matriculaSeleccionada, descripcionSeleccionada);

        if (esEasterEgg) {
            aplicarTemaDoradoEasterEgg();
        }

        listaAvionesBase = new ArrayList<>();
        resolverYFijarAeronave(
                icaoSeleccionado,
                callsignSeleccionado,
                matriculaSeleccionada,
                tipoSeleccionado,
                descripcionSeleccionada,
                prefs.getString("categoria_seleccionada", null)
        );

        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo());

        btnCancelar.setOnClickListener(v -> cancelarYVolverAlMapa());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelarYVolverAlMapa();
            }
        });
    }

    /**
     * Aplica el degradado dorado exclusivo y resalta los botones cuando se va a capturar un Easter Egg.
     */
    private void aplicarTemaDoradoEasterEgg() {
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            pantallaPrincipal.setBackground(fondoDorado);
        }

        if (btnGuardar != null) {
            btnGuardar.setBackgroundColor(0xFFFFD700);
            btnGuardar.setTextColor(0xFF1F1400);
        }
        if (btnSubirFoto != null) {
            btnSubirFoto.setBackgroundColor(0xFFB45309);
            btnSubirFoto.setTextColor(0xFFFFD700);
        }
    }

    /**
     * Borra las claves temporales del avistamiento y vuelve al radar.
     */
    private void cancelarYVolverAlMapa() {
        limpiarAvistamientoPendiente();
        finish();
    }

    /**
     * Construye la clave única de almacenamiento local vinculada al UID de Firebase del usuario.
     */
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Resuelve el modelo, fabricante y rareza mediante el motor híbrido asíncrono y fija el Spinner.
     */
    private void resolverYFijarAeronave(String icao24, String callsign, String matricula,
                                        String tipoIcao, String descripcionFallback, String categoriaRadar) {
        spinnerAviones.setEnabled(false);
        btnGuardar.setEnabled(false);

        List<String> cargando = new ArrayList<>();
        cargando.add(getString(R.string.identificando_aeronave));
        spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cargando));

        AvionGenerator.resolverAvionGlobalAsync(
                this,
                icao24,
                callsign,
                matricula,
                tipoIcao,
                descripcionFallback,
                categoriaRadar,
                resuelto -> {
                    if (isFinishing() || isDestroyed()) return;
                    avionDetectadoResuelto = resuelto;
                    listaAvionesBase.clear();
                    listaAvionesBase.add(resuelto);

                    if ("EASTER_EGG".equalsIgnoreCase(resuelto.getRareza()) || esEasterEgg) {
                        esEasterEgg = true;
                        aplicarTemaDoradoEasterEgg();
                        if (uriImagenFinal == null && imgPreviewAvion != null) {
                            Glide.with(this)
                                    .load(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO)
                                    .centerCrop()
                                    .into(imgPreviewAvion);
                        }
                    }

                    List<String> etiqueta = new ArrayList<>();
                    String etiquetaRareza = "EASTER_EGG".equalsIgnoreCase(resuelto.getRareza())
                            ? "★ EASTER EGG ★"
                            : AvionGenerator.traducirRareza(this, resuelto.getRareza());
                    etiqueta.add(resuelto.getModelo() + " (" + etiquetaRareza + ")");
                    spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, etiqueta));
                    spinnerAviones.setSelection(0);
                    btnGuardar.setEnabled(true);
                }
        );
    }

    /**
     * Crea el registro definitivo del avistamiento y lo persiste tanto en local como en Firestore.
     */
    private void guardarAvionCatalogo() {
        if (icaoSeleccionado == null || avionDetectadoResuelto == null) {
            Toast.makeText(this, getString(R.string.toast_espera_identificacion), Toast.LENGTH_SHORT).show();
            return;
        }

        if (yaAvistado(icaoSeleccionado)) {
            Toast.makeText(this, getString(R.string.toast_ya_avistado_antes), Toast.LENGTH_SHORT).show();
            limpiarAvistamientoPendiente();
            finish();
            return;
        }

        Avion nuevo = new Avion(
                avionDetectadoResuelto.getModelo(),
                avionDetectadoResuelto.getFabricante(),
                avionDetectadoResuelto.getRareza(),
                avionDetectadoResuelto.getImagenResId(),
                avionDetectadoResuelto.getVelocidad(),
                avionDetectadoResuelto.getPasajeros(),
                avionDetectadoResuelto.getDimensiones(),
                avionDetectadoResuelto.getPais(),
                avionDetectadoResuelto.getPeso(),
                icaoSeleccionado
        );

        nuevo.setDescripcion(avionDetectadoResuelto.getDescripcion());

        SharedPreferences prefsAv = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        nuevo.setMatricula(prefsAv.getString("matricula_seleccionada", null));

        String tipoGuardar = prefsAv.getString("tipo_seleccionado", null);
        if ((tipoGuardar == null || tipoGuardar.trim().isEmpty()) && avionDetectadoResuelto.getTipoIcao() != null) {
            tipoGuardar = avionDetectadoResuelto.getTipoIcao();
        }
        nuevo.setTipoIcao(tipoGuardar);

        nuevo.setFechaAvistamiento(prefsAv.getLong("fecha_seleccionada", System.currentTimeMillis()));
        nuevo.setDistanciaKm(prefsAv.getFloat("distancia_seleccionada", 0f));
        nuevo.setLatAvion(prefsAv.getFloat("lat_avion_seleccionado", 0f));
        nuevo.setLonAvion(prefsAv.getFloat("lon_avion_seleccionado", 0f));
        nuevo.setOrigen("MAPA");

        String urlRemotaWiki = prefsAv.getString("url_imagen_remota_seleccionada", null);
        if ((urlRemotaWiki == null || urlRemotaWiki.isEmpty()) && avionDetectadoResuelto.getUrlImagenRemota() != null) {
            urlRemotaWiki = avionDetectadoResuelto.getUrlImagenRemota();
        }
        if (urlRemotaWiki != null && !urlRemotaWiki.isEmpty()) {
            nuevo.setImagenUrl(urlRemotaWiki);
            nuevo.setUrlImagenRemota(urlRemotaWiki);
        }

        // Si tomó foto en directo, la comprime a Base64 ligera para compartirla en Firestore y el ranking
        if (uriImagenFinal != null) {
            String fotoComprimida = ImagenBase64Utils.uriABase64Comprimida(this, uriImagenFinal, 600, 75);
            nuevo.setUriFotoUsuario(fotoComprimida != null ? fotoComprimida : uriImagenFinal.toString());
            nuevo.setEsFotoVerificada(true);
        } else {
            nuevo.setEsFotoVerificada(false);
        }

        // Guarda en local, sube el avistamiento a Firestore y recalcula el ranking global
        List<Avion> listaActualizada = guardarEnPreferencias(nuevo);
        guardarEnFirestore(nuevo);
        RankingManager.sincronizarRankingUsuario(listaActualizada, null, null, null);

        limpiarAvistamientoPendiente();

        String mensaje = nuevo.isEsFotoVerificada()
                ? getString(R.string.toast_guardado_verificado)
                : getString(R.string.toast_guardado_sin_foto);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Añade el avión a la lista JSON guardada en SharedPreferences y devuelve la lista actualizada.
     */
    private List<Avion> guardarEnPreferencias(Avion nuevo) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String clave = getClaveListaUsuario();
        String json = prefs.getString(clave, null);
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        List<Avion> lista = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        if (lista == null) lista = new ArrayList<>();
        lista.add(nuevo);
        prefs.edit().putString(clave, gson.toJson(lista)).apply();
        return lista;
    }

    /**
     * Sube el registro del avión a la subcolección del usuario en Firebase Firestore.
     */
    private void guardarEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("usuarios").document(auth.getCurrentUser().getUid())
                .collection("aviones").document(avion.getId()).set(avion)
                .addOnSuccessListener(v -> Log.d("FIRESTORE", "Sincronizado"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error", e));
    }

    /**
     * Muestra un diálogo explicando que solo se aceptan fotos tomadas en el momento para el ranking.
     */
    private void mostrarDialogoSeleccion() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_foto_titulo))
                .setMessage(getString(R.string.dialogo_foto_mensaje))
                .setPositiveButton(getString(R.string.dialogo_foto_hacer), (d, i) -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        abrirCamara();
                    } else {
                        launcherPermisos.launch(Manifest.permission.CAMERA);
                    }
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    /**
     * Prepara el archivo temporal y lanza la aplicación de cámara del teléfono.
     */
    private void abrirCamara() {
        try {
            File f = crearArchivoImagen();
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_error_camara), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Crea un fichero JPEG único con marca de tiempo en el directorio privado de imágenes.
     */
    private File crearArchivoImagen() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir != null && !dir.exists()) dir.mkdirs();
        return File.createTempFile("IMG_" + ts + "_", ".jpg", dir);
    }

    /**
     * Muestra la fotografía tomada en el ImageView desactivando la caché de disco y memoria.
     */
    private void cargarImagen(Uri uri) {
        uriImagenFinal = uri;
        Glide.with(this).load(uri).centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imgPreviewAvion);
    }

    /**
     * Comprueba si el código ICAO24 ya está registrado en la colección local del usuario.
     */
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

    /**
     * Elimina de SharedPreferences todas las claves temporales del avistamiento en curso.
     */
    private void limpiarAvistamientoPendiente() {
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE).edit()
                .remove("icao_seleccionado")
                .remove("callsign_seleccionado")
                .remove("tipo_seleccionado")
                .remove("matricula_seleccionada")
                .remove("descripcion_seleccionada")
                .remove("fecha_seleccionada")
                .remove("distancia_seleccionada")
                .remove("lat_avion_seleccionado")
                .remove("lon_avion_seleccionado")
                .remove("url_imagen_remota_seleccionada")
                .remove("categoria_seleccionada")
                .remove("es_easter_egg_seleccionado")
                .apply();
    }
}