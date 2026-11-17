// Define el paquete donde reside esta actividad dentro de la estructura del proyecto
package es.medac.skycollectorapp.activities;

// Importación para gestionar los permisos de acceso al hardware (cámara) en Android
import android.Manifest;
// Importación de Context para acceder a los recursos y preferencias del sistema
import android.content.Context;
// Importación de SharedPreferences para leer y guardar datos persistentes localmente en formato clave-valor
import android.content.SharedPreferences;
// Importación de PackageManager para verificar el estado de concesión de permisos
import android.content.pm.PackageManager;
// Importación de GradientDrawable para generar fondos degradados programáticamente (tema Easter Egg)
import android.graphics.drawable.GradientDrawable;
// Importación de Uri para gestionar identificadores de rutas de archivos multimedia
import android.net.Uri;
// Importación de Bundle para recibir y guardar el estado de la instancia de la actividad
import android.os.Bundle;
// Importación de Environment para acceder a directorios estándar del almacenamiento externo
import android.os.Environment;
// Importación de Log para imprimir mensajes de depuración y seguimiento en Logcat
import android.util.Log;
// Importación de View como clase base de todos los componentes visuales
import android.view.View;
// Importación de ViewGroup para manipular contenedores de vistas raíz
import android.view.ViewGroup;
// Importación de ArrayAdapter para vincular listas de texto con componentes desplegables
import android.widget.ArrayAdapter;
// Importación de Button para interactuar con los botones de la interfaz
import android.widget.Button;
// Importación de ImageView para mostrar la vista previa de la fotografía del avión
import android.widget.ImageView;
// Importación de Spinner para mostrar el desplegable con el modelo de avión identificado
import android.widget.Spinner;
// Importación de Toast para mostrar notificaciones emergentes breves al usuario
import android.widget.Toast;

// Importación de OnBackPressedCallback para interceptar y personalizar el botón o gesto de retroceso
import androidx.activity.OnBackPressedCallback;
// Importación de ActivityResultLauncher para gestionar de forma moderna el retorno de otras actividades o permisos
import androidx.activity.result.ActivityResultLauncher;
// Importación de ActivityResultContracts para usar contratos estándar como solicitud de permisos y captura de fotos
import androidx.activity.result.contract.ActivityResultContracts;
// Importación de AlertDialog para construir cuadros de diálogo de confirmación modales
import androidx.appcompat.app.AlertDialog;
// Importación de AppCompatActivity como clase base compatible para actividades modernas en Android
import androidx.appcompat.app.AppCompatActivity;
// Importación de ContextCompat para consultar permisos manteniendo retrocompatibilidad
import androidx.core.content.ContextCompat;
// Importación de FileProvider para compartir rutas de archivos de forma segura entre aplicaciones (cámara)
import androidx.core.content.FileProvider;

// Importación de Glide para cargar, escalar y renderizar imágenes eficientemente
import com.bumptech.glide.Glide;
// Importación de DiskCacheStrategy para controlar el comportamiento del almacenamiento en caché en disco
import com.bumptech.glide.load.engine.DiskCacheStrategy;
// Importación de FirebaseAuth para gestionar la sesión del usuario autenticado
import com.google.firebase.auth.FirebaseAuth;
// Importación de FirebaseUser para obtener los datos y el identificador único (UID) del usuario actual
import com.google.firebase.auth.FirebaseUser;
// Importación de FirebaseFirestore para conectar y escribir datos en la base de datos en la nube
import com.google.firebase.firestore.FirebaseFirestore;
// Importación de Gson para convertir objetos Java en cadenas JSON y viceversa
import com.google.gson.Gson;
// Importación de TypeToken para capturar tipos genéricos (como List<Avion>) durante la deserialización JSON
import com.google.gson.reflect.TypeToken;

// Importación de File para representar y manipular archivos en el sistema de ficheros
import java.io.File;
// Importación de IOException para capturar excepciones de entrada/salida al crear archivos
import java.io.IOException;
// Importación de Type para manejar metadatos de tipos en tiempo de ejecución
import java.lang.reflect.Type;
// Importación de SimpleDateFormat para formatear marcas de tiempo en nombres de archivo
import java.text.SimpleDateFormat;
// Importación de ArrayList como implementación dinámica de listas
import java.util.ArrayList;
// Importación de Date para capturar la fecha y hora exacta del sistema
import java.util.Date;
// Importación de List como interfaz estándar de colecciones ordenadas
import java.util.List;
// Importación de Locale para configurar formatos dependientes del idioma o región
import java.util.Locale;

// Importación de la clase R generada para acceder a los recursos XML (layouts, strings, IDs)
import es.medac.skycollectorapp.R;
// Importación de la clase modelo Avion que representa una aeronave o vehículo avistado
import es.medac.skycollectorapp.models.Avion;
// Importación de la clase de utilidad AvionGenerator encargada de identificar y resolver aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator;
// Importación de la utilidad ImagenBase64Utils para comprimir fotografías a texto Base64
import es.medac.skycollectorapp.utils.ImagenBase64Utils;
// Importación de RankingManager para actualizar la puntuación global tras un nuevo avistamiento
import es.medac.skycollectorapp.utils.RankingManager;

/**
 * Actividad encargada de gestionar la captura fotográfica en directo y el registro definitivo
 * de un avión o vehículo terrestre especial (Easter Egg) seleccionado previamente desde el mapa radar.
 */
public class AddAvionActivity extends AppCompatActivity {

    // Desplegable que muestra el modelo y la rareza de la aeronave resuelta
    private Spinner spinnerAviones;
    // Vista de imagen donde se previsualiza la foto capturada por el usuario
    private ImageView imgPreviewAvion;
    // Botones de acción para abrir la cámara, guardar el registro o cancelar la operación
    private Button btnSubirFoto, btnGuardar, btnCancelar;

    // URI definitiva de la imagen confirmada y tomada por el usuario
    private Uri uriImagenFinal;
    // URI temporal asignada al archivo vacío antes de lanzar la cámara externa
    private Uri uriFotoCamaraTemporal;
    // Lista interna que almacena el avión resuelto para alimentar al adaptador del Spinner
    private List<Avion> listaAvionesBase;
    // Identificador hexadecimal único (transpondedor ICAO24) del blanco seleccionado en el mapa
    private String icaoSeleccionado;
    // Objeto Avion completo una vez consultadas las fuentes de datos (local + Wikipedia + Easter Eggs)
    private Avion avionDetectadoResuelto;
    // Bandera booleana que indica si el blanco seleccionado es un vehículo terrestre Easter Egg
    private boolean esEasterEgg;

    // Lanzador registrado para solicitar el permiso de cámara y actuar según la respuesta del usuario
    private final ActivityResultLauncher<String> launcherPermisos = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Si el usuario concede el permiso, abre directamente la aplicación de cámara
                if (isGranted) abrirCamara();
                    // Si lo deniega, muestra un aviso informativo en pantalla
                else Toast.makeText(this, getString(R.string.toast_permiso_camara), Toast.LENGTH_SHORT).show();
            });

    // Lanzador registrado para recibir el resultado de la captura fotográfica desde la app de cámara
    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                // Si la foto se tomó con éxito y la URI temporal existe, carga la imagen en la vista previa
                if (ok && uriFotoCamaraTemporal != null) cargarImagen(uriFotoCamaraTemporal);
            });

    /**
     * Método del ciclo de vida que inicializa la interfaz, recupera los metadatos del blanco
     * seleccionados en el radar, aplica la estética especial si es Easter Egg e inicia la identificación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la implementación de la superclase para inicializar la actividad
        super.onCreate(savedInstanceState);
        // Vincula el archivo de diseño XML activity_add_avion con esta actividad
        setContentView(R.layout.activity_add_avion);

        // Enlaza las variables Java con los componentes visuales definidos en el XML
        spinnerAviones = findViewById(R.id.spinnerAviones);
        imgPreviewAvion = findViewById(R.id.imgPreviewAvion);
        btnSubirFoto = findViewById(R.id.btnSubirFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Accede al archivo de preferencias privadas donde el mapa guardó los datos del avión pulsado
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        // Recupera el código ICAO24 del blanco seleccionado
        icaoSeleccionado = prefs.getString("icao_seleccionado", null);
        // Recupera el indicativo de llamada (callsign) del vuelo
        String callsignSeleccionado = prefs.getString("callsign_seleccionado", null);
        // Recupera la matrícula de la aeronave si está disponible
        String matriculaSeleccionada = prefs.getString("matricula_seleccionada", null);
        // Recupera el código de tipo de aeronave ICAO (ej. B738, A320)
        String tipoSeleccionado = prefs.getString("tipo_seleccionado", null);
        // Recupera la descripción preliminar obtenida en el radar
        String descripcionSeleccionada = prefs.getString("descripcion_seleccionada", null);

        // Si no hay ningún código ICAO guardado, significa que se abrió la pantalla indebidamente
        if (icaoSeleccionado == null) {
            // Avisa al usuario de que primero debe pulsar un avión en el mapa
            Toast.makeText(this, getString(R.string.toast_selecciona_mapa_primero), Toast.LENGTH_LONG).show();
            // Cierra esta actividad inmediatamente
            finish();
            // Detiene la ejecución del método onCreate
            return;
        }

        // Comprueba mediante bandera guardada o análisis de metadatos si se trata de un Easter Egg terrestre
        esEasterEgg = prefs.getBoolean("es_easter_egg_seleccionado", false)
                || AvionGenerator.esEasterEggTerrestre(tipoSeleccionado, callsignSeleccionado, matriculaSeleccionada, descripcionSeleccionada);

        // Si se confirma que es un Easter Egg, transforma los colores de la pantalla a tema dorado
        if (esEasterEgg) {
            aplicarTemaDoradoEasterEgg();
        }

        // Inicializa la lista vacía que contendrá el avión identificado
        listaAvionesBase = new ArrayList<>();
        // Lanza el proceso asíncrono para identificar el modelo exacto, fabricante y rareza
        resolverYFijarAeronave(
                icaoSeleccionado,
                callsignSeleccionado,
                matriculaSeleccionada,
                tipoSeleccionado,
                descripcionSeleccionada,
                prefs.getString("categoria_seleccionada", null)
        );

        // Asigna el evento de clic al botón de tomar foto para mostrar el aviso previo a la cámara
        btnSubirFoto.setOnClickListener(v -> mostrarDialogoSeleccion());
        // Asigna el evento de clic al botón de guardar para registrar el avistamiento definitivo
        btnGuardar.setOnClickListener(v -> guardarAvionCatalogo());

        // Asigna el evento de clic al botón cancelar para limpiar datos temporales y cerrar
        btnCancelar.setOnClickListener(v -> cancelarYVolverAlMapa());
        // Intercepta el botón o gesto de retroceso del sistema para ejecutar la misma limpieza segura
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelarYVolverAlMapa();
            }
        });
    }

    /**
     * Modifica dinámicamente el fondo de la pantalla y los botones principales con tonos dorados
     * cuando el usuario está a punto de capturar un vehículo Easter Egg especial.
     */
    private void aplicarTemaDoradoEasterEgg() {
        // Obtiene el contenedor raíz del contenido de la ventana actual
        View root = findViewById(android.R.id.content);
        // Verifica que sea un ViewGroup válido con al menos un hijo (el layout principal)
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            // Obtiene la vista principal de la pantalla
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            // Crea un degradado lineal vertical con tres tonos de oro oscuro a bronce
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            // Aplica el degradado dorado como fondo de la pantalla
            pantallaPrincipal.setBackground(fondoDorado);
        }

        // Si el botón de guardar existe, le aplica fondo oro brillante y texto oscuro
        if (btnGuardar != null) {
            btnGuardar.setBackgroundColor(0xFFFFD700);
            btnGuardar.setTextColor(0xFF1F1400);
        }
        // Si el botón de subir foto existe, le aplica fondo ámbar oscuro y texto dorado
        if (btnSubirFoto != null) {
            btnSubirFoto.setBackgroundColor(0xFFB45309);
            btnSubirFoto.setTextColor(0xFFFFD700);
        }
    }

    /**
     * Limpia los datos del avión seleccionado en SharedPreferences y cierra la actividad
     * para regresar al radar sin guardar cambios.
     */
    private void cancelarYVolverAlMapa() {
        // Borra las claves temporales almacenadas
        limpiarAvistamientoPendiente();
        // Finaliza la actividad actual
        finish();
    }

    /**
     * Genera la clave de almacenamiento local específica para el usuario autenticado,
     * evitando que distintas cuentas en el mismo dispositivo mezclen sus hangares.
     */
    private String getClaveListaUsuario() {
        // Obtiene la instancia del usuario actualmente conectado en Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Devuelve la clave concatenada con su UID, o una clave genérica si no hay sesión
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Consulta el motor híbrido de identificación (catálogo interno + API Wikipedia + Easter Eggs)
     * en segundo plano y actualiza el Spinner y la interfaz con los datos finales.
     */
    private void resolverYFijarAeronave(String icao24, String callsign, String matricula,
                                        String tipoIcao, String descripcionFallback, String categoriaRadar) {
        // Deshabilita el desplegable para evitar interacciones mientras se consulta la información
        spinnerAviones.setEnabled(false);
        // Deshabilita el botón de guardado hasta que la aeronave esté completamente resuelta
        btnGuardar.setEnabled(false);

        // Prepara una lista temporal con el texto de carga
        List<String> cargando = new ArrayList<>();
        // Añade el mensaje localizado "Identificando aeronave..."
        cargando.add(getString(R.string.identificando_aeronave));
        // Muestra el mensaje de espera dentro del Spinner
        spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cargando));

        // Invoca la resolución asíncrona global en AvionGenerator
        AvionGenerator.resolverAvionGlobalAsync(
                this,
                icao24,
                callsign,
                matricula,
                tipoIcao,
                descripcionFallback,
                categoriaRadar,
                resuelto -> {
                    // Si la actividad ya se está cerrando o destruyendo, aborta la actualización visual
                    if (isFinishing() || isDestroyed()) return;
                    // Almacena el objeto Avion devuelto por el motor
                    avionDetectadoResuelto = resuelto;
                    // Limpia la lista base y añade el avión resuelto
                    listaAvionesBase.clear();
                    listaAvionesBase.add(resuelto);

                    // Comprueba si el resultado final tiene rareza EASTER_EGG
                    if ("EASTER_EGG".equalsIgnoreCase(resuelto.getRareza()) || esEasterEgg) {
                        // Marca la bandera como verdadera y asegura el tema dorado
                        esEasterEgg = true;
                        aplicarTemaDoradoEasterEgg();
                        // Si el usuario aún no ha tomado foto propia, muestra una imagen ilustrativa por defecto
                        if (uriImagenFinal == null && imgPreviewAvion != null) {
                            Glide.with(this)
                                    .load(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO)
                                    .centerCrop()
                                    .into(imgPreviewAvion);
                        }
                    }

                    // Prepara la lista de texto final para mostrar en el Spinner
                    List<String> etiqueta = new ArrayList<>();
                    // Formatea la etiqueta de rareza con estrellas si es Easter Egg, o la traduce al idioma activo
                    String etiquetaRareza = "EASTER_EGG".equalsIgnoreCase(resuelto.getRareza())
                            ? "★ EASTER EGG ★"
                            : AvionGenerator.traducirRareza(this, resuelto.getRareza());
                    // Combina el modelo de avión y su rareza entre paréntesis
                    etiqueta.add(resuelto.getModelo() + " (" + etiquetaRareza + ")");
                    // Asigna el adaptador definitivo al Spinner y selecciona el primer elemento
                    spinnerAviones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, etiqueta));
                    spinnerAviones.setSelection(0);
                    // Reactiva el botón de guardar ahora que los datos están listos
                    btnGuardar.setEnabled(true);
                }
        );
    }

    /**
     * Valida los datos, construye el objeto Avion definitivo con su foto comprimida si existe,
     * lo guarda localmente y en Firestore, actualiza el ranking y cierra la actividad.
     */
    private void guardarAvionCatalogo() {
        // Verifica que tanto el código ICAO como el objeto resuelto existan antes de guardar
        if (icaoSeleccionado == null || avionDetectadoResuelto == null) {
            Toast.makeText(this, getString(R.string.toast_espera_identificacion), Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprueba si este avión específico (por ICAO24) ya había sido capturado antes por el usuario
        if (yaAvistado(icaoSeleccionado)) {
            Toast.makeText(this, getString(R.string.toast_ya_avistado_antes), Toast.LENGTH_SHORT).show();
            limpiarAvistamientoPendiente();
            finish();
            return;
        }

        // Crea la instancia final del Avion copiando todas sus especificaciones técnicas resueltas
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

        // Copia la descripción detallada de la aeronave
        nuevo.setDescripcion(avionDetectadoResuelto.getDescripcion());

        // Recupera los metadatos geográficos y temporales guardados durante el toque en el mapa
        SharedPreferences prefsAv = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        nuevo.setMatricula(prefsAv.getString("matricula_seleccionada", null));

        // Obtiene el código de tipo ICAO del radar, o usa el resuelto como respaldo si venía vacío
        String tipoGuardar = prefsAv.getString("tipo_seleccionado", null);
        if ((tipoGuardar == null || tipoGuardar.trim().isEmpty()) && avionDetectadoResuelto.getTipoIcao() != null) {
            tipoGuardar = avionDetectadoResuelto.getTipoIcao();
        }
        nuevo.setTipoIcao(tipoGuardar);

        // Asigna la marca de tiempo, distancia desde el usuario, coordenadas exactas y origen del avistamiento
        nuevo.setFechaAvistamiento(prefsAv.getLong("fecha_seleccionada", System.currentTimeMillis()));
        nuevo.setDistanciaKm(prefsAv.getFloat("distancia_seleccionada", 0f));
        nuevo.setLatAvion(prefsAv.getFloat("lat_avion_seleccionado", 0f));
        nuevo.setLonAvion(prefsAv.getFloat("lon_avion_seleccionado", 0f));
        nuevo.setOrigen("MAPA");

        // Recupera la URL de la fotografía de referencia obtenida de Wikipedia si existe
        String urlRemotaWiki = prefsAv.getString("url_imagen_remota_seleccionada", null);
        if ((urlRemotaWiki == null || urlRemotaWiki.isEmpty()) && avionDetectadoResuelto.getUrlImagenRemota() != null) {
            urlRemotaWiki = avionDetectadoResuelto.getUrlImagenRemota();
        }
        // Si hay URL remota válida, la asigna al modelo
        if (urlRemotaWiki != null && !urlRemotaWiki.isEmpty()) {
            nuevo.setImagenUrl(urlRemotaWiki);
            nuevo.setUrlImagenRemota(urlRemotaWiki);
        }

        // Comprueba si el usuario tomó una fotografía real con la cámara
        if (uriImagenFinal != null) {
            // Comprime y redimensiona la imagen a Base64 (máx 600px, calidad 75%) para poder subirla a Firestore
            String fotoComprimida = ImagenBase64Utils.uriABase64Comprimida(this, uriImagenFinal, 600, 75);
            // Guarda la cadena Base64 (o la URI local si fallara la compresión)
            nuevo.setUriFotoUsuario(fotoComprimida != null ? fotoComprimida : uriImagenFinal.toString());
            // Marca el avistamiento como verificado con foto real (otorga puntos extra en el ranking)
            nuevo.setEsFotoVerificada(true);
        } else {
            // Si no tomó foto, se guarda como avistamiento estándar no verificado
            nuevo.setEsFotoVerificada(false);
        }

        // Guarda el nuevo avión en la lista JSON local y obtiene la colección actualizada
        List<Avion> listaActualizada = guardarEnPreferencias(nuevo);
        // Sube el documento del avión a la subcolección en la nube de Firebase Firestore
        guardarEnFirestore(nuevo);
        // Recalcula y sincroniza la puntuación total del usuario en la tabla de clasificación global
        RankingManager.sincronizarRankingUsuario(listaActualizada, null, null, null);

        // Limpia las variables temporales de SharedPreferences
        limpiarAvistamientoPendiente();

        // Selecciona el mensaje Toast adecuado según si el registro incluye foto verificada o no
        String mensaje = nuevo.isEsFotoVerificada()
                ? getString(R.string.toast_guardado_verificado)
                : getString(R.string.toast_guardado_sin_foto);
        // Muestra la confirmación al usuario y cierra la pantalla
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Deserializa la lista actual de aviones desde SharedPreferences, añade el nuevo registro
     * y vuelve a guardar la lista serializada en formato JSON.
     */
    private List<Avion> guardarEnPreferencias(Avion nuevo) {
        // Abre las preferencias privadas de la aplicación
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        // Instancia el motor de serialización Gson
        Gson gson = new Gson();
        // Obtiene la clave única asociada al usuario actual
        String clave = getClaveListaUsuario();
        // Lee la cadena JSON almacenada previamente
        String json = prefs.getString(clave, null);
        // Define el tipo genérico ArrayList<Avion> para Gson
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        // Convierte el JSON a lista Java, o crea una nueva lista vacía si es el primer avión
        List<Avion> lista = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        if (lista == null) lista = new ArrayList<>();
        // Añade el nuevo avistamiento a la lista
        lista.add(nuevo);
        // Serializa la lista actualizada a JSON y la persiste de forma asíncrona
        prefs.edit().putString(clave, gson.toJson(lista)).apply();
        // Devuelve la lista completa actualizada
        return lista;
    }

    /**
     * Almacena el objeto Avion en Cloud Firestore bajo la ruta usuarios/{uid}/aviones/{idAvion}.
     */
    private void guardarEnFirestore(Avion avion) {
        // Obtiene la instancia de autenticación de Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        // Si no hay un usuario autenticado, no realiza operaciones en la nube
        if (auth.getCurrentUser() == null) return;
        // Escribe el documento del avión en Firestore y registra el resultado en Logcat
        FirebaseFirestore.getInstance().collection("usuarios").document(auth.getCurrentUser().getUid())
                .collection("aviones").document(avion.getId()).set(avion)
                .addOnSuccessListener(v -> Log.d("FIRESTORE", "Sincronizado"))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error", e));
    }

    /**
     * Muestra un cuadro de diálogo informativo explicando que solo se permiten fotos
     * tomadas en tiempo real con la cámara (sin galería) para garantizar la autenticidad del ranking.
     */
    private void mostrarDialogoSeleccion() {
        // Construye y muestra el AlertDialog con opciones de continuar a la cámara o cancelar
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_foto_titulo))
                .setMessage(getString(R.string.dialogo_foto_mensaje))
                .setPositiveButton(getString(R.string.dialogo_foto_hacer), (d, i) -> {
                    // Comprueba si el permiso de cámara ya fue concedido anteriormente
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        // Abre directamente la cámara
                        abrirCamara();
                    } else {
                        // Solicita el permiso de cámara al sistema operativo
                        launcherPermisos.launch(Manifest.permission.CAMERA);
                    }
                })
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    /**
     * Crea un archivo de imagen temporal, genera una URI segura mediante FileProvider
     * y lanza la actividad nativa de la cámara del dispositivo.
     */
    private void abrirCamara() {
        try {
            // Crea el fichero físico vacío donde la cámara volcará la foto
            File f = crearArchivoImagen();
            // Convierte el archivo en una URI segura accesible por apps externas mediante el FileProvider configurado
            uriFotoCamaraTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            // Lanza la cámara pasándole la URI de destino
            launcherCamara.launch(uriFotoCamaraTemporal);
        } catch (Exception e) {
            // Si ocurre algún error al preparar el archivo o abrir la cámara, notifica al usuario
            Toast.makeText(this, getString(R.string.toast_error_camara), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Genera un archivo temporal con extensión .jpg y nombre basado en la fecha y hora exacta
     * dentro del directorio privado de imágenes de la aplicación.
     */
    private File crearArchivoImagen() throws IOException {
        // Formatea la fecha y hora actual (ej. 20261117_030800) para evitar colisiones de nombres
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        // Obtiene el directorio de imágenes externo privado de la app
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Si la carpeta no existe todavía, la crea
        if (dir != null && !dir.exists()) dir.mkdirs();
        // Crea y devuelve el archivo temporal en disco
        return File.createTempFile("IMG_" + ts + "_", ".jpg", dir);
    }

    /**
     * Carga la fotografía recién tomada en el ImageView de vista previa, forzando a Glide
     * a ignorar cachés previas para asegurar que se muestre siempre la última captura.
     */
    private void cargarImagen(Uri uri) {
        // Guarda la URI como imagen final confirmada
        uriImagenFinal = uri;
        // Carga y recorta al centro la imagen en el ImageView desactivando caché en disco y memoria
        Glide.with(this).load(uri).centerCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(imgPreviewAvion);
    }

    /**
     * Verifica si el código transpondedor ICAO24 recibido ya se encuentra registrado
     * en la colección local del usuario para evitar avistamientos duplicados.
     */
    private boolean yaAvistado(String icao) {
        // Si el código es nulo, devuelve falso
        if (icao == null) return false;
        // Lee la lista JSON de aviones del usuario
        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString(getClaveListaUsuario(), null);
        if (json == null) return false;
        // Deserializa la lista de objetos Avion
        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista == null) return false;
        // Recorre todos los aviones guardados buscando coincidencia exacta sin distinguir mayúsculas
        for (Avion a : lista) {
            if (icao.equalsIgnoreCase(a.getIcao24())) return true;
        }
        // Si termina el bucle sin coincidencias, el avión aún no ha sido avistado
        return false;
    }

    /**
     * Elimina de SharedPreferences todas las claves temporales almacenadas al seleccionar
     * un avión en el radar para dejar limpio el estado de la aplicación.
     */
    private void limpiarAvistamientoPendiente() {
        // Inicia una edición en las preferencias y elimina cada clave temporal del radar
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