// Declaración del paquete al que pertenece esta actividad
package es.medac.skycollectorapp.activities;

// Importación para solicitar y verificar permisos del sistema Android
import android.Manifest;
// Importación para suprimir advertencias de verificación de permisos ya comprobados
import android.annotation.SuppressLint;
// Importación para acceder a servicios del sistema como LocationManager y preferencias
import android.content.Context;
// Importación para iniciar nuevas actividades y transmitir datos mediante Intents
import android.content.Intent;
// Importación para comprobar el estado de concesión de permisos
import android.content.pm.PackageManager;
// Importación para crear y manipular mapas de bits (iconos de aviones)
import android.graphics.Bitmap;
// Importación para dibujar vectores Drawable sobre un Bitmap
import android.graphics.Canvas;
// Importación para gestionar constantes y códigos hexadecimales de color
import android.graphics.Color;
// Importación para manipular recursos gráficos Drawable
import android.graphics.drawable.Drawable;
// Importación para representar coordenadas y datos de localización GPS
import android.location.Location;
// Importación para recibir actualizaciones periódicas de ubicación
import android.location.LocationListener;
// Importación para acceder a los proveedores de localización GPS y Red
import android.location.LocationManager;
// Importación para gestionar el ciclo de vida de la actividad
import android.os.Bundle;
// Importación para programar tareas periódicas y con retardo en el hilo principal
import android.os.Handler;
// Importación para vincular el Handler al bucle principal de Android
import android.os.Looper;
// Importación para medir tiempos relativos precisos sin verse afectados por cambios de reloj
import android.os.SystemClock;
// Importación para registrar mensajes de depuración en Logcat
import android.util.Log;
// Importación de la clase base de componentes visuales
import android.view.View;
// Importación para botones estándar de la interfaz
import android.widget.Button;
// Importación para botones con icono gráfico
import android.widget.ImageButton;
// Importación para etiquetas de texto
import android.widget.TextView;
// Importación para mostrar notificaciones emergentes breves
import android.widget.Toast;

// Importación para gestionar resultados de solicitud de permisos modernos
import androidx.activity.result.ActivityResultLauncher;
// Importación de contratos estándar de resultados de AndroidX
import androidx.activity.result.contract.ActivityResultContracts;
// Importación para indicar parámetros que no aceptan valores nulos
import androidx.annotation.NonNull;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación del contenedor tarjeta para el panel inferior de información
import androidx.cardview.widget.CardView;
// Importación para comprobar permisos en tiempo de ejecución
import androidx.core.app.ActivityCompat;
// Importación para acceder a recursos y permisos con retrocompatibilidad
import androidx.core.content.ContextCompat;

// Importación para mover y animar la cámara de Google Maps
import com.google.android.gms.maps.CameraUpdateFactory;
// Importación de la instancia principal del mapa de Google
import com.google.android.gms.maps.GoogleMap;
// Importación de la interfaz callback que notifica cuando el mapa está listo
import com.google.android.gms.maps.OnMapReadyCallback;
// Importación del fragmento que aloja el mapa en la interfaz
import com.google.android.gms.maps.SupportMapFragment;
// Importación del descriptor de imagen para personalizar marcadores en el mapa
import com.google.android.gms.maps.model.BitmapDescriptor;
// Importación de la fábrica para crear BitmapDescriptor desde Bitmaps
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
// Importación de la figura geométrica circular para el radio de 40 km
import com.google.android.gms.maps.model.Circle;
// Importación de opciones de configuración de círculos en Google Maps
import com.google.android.gms.maps.model.CircleOptions;
// Importación para representar coordenadas geográficas (latitud, longitud)
import com.google.android.gms.maps.model.LatLng;
// Importación para delimitar regiones rectangulares visibles en el mapa
import com.google.android.gms.maps.model.LatLngBounds;
// Importación para representar marcadores individuales de vuelos en el mapa
import com.google.android.gms.maps.model.Marker;
// Importación de opciones de configuración de marcadores
import com.google.android.gms.maps.model.MarkerOptions;
// Importación para dibujar líneas continuas de trayectoria en el mapa
import com.google.android.gms.maps.model.Polyline;
// Importación de opciones de configuración de polilíneas
import com.google.android.gms.maps.model.PolylineOptions;

// Importación para consultar el usuario autenticado en Firebase
import com.google.firebase.auth.FirebaseAuth;
// Importación para acceder al UID del usuario actual
import com.google.firebase.auth.FirebaseUser;
// Importación de Gson para deserializar la colección local
import com.google.gson.Gson;
// Importación de TypeToken para manejar tipos genéricos con Gson
import com.google.gson.reflect.TypeToken;

// Importación de listas dinámicas
import java.util.ArrayList;
// Importación de utilidades para manejo de arrays y listas fijas
import java.util.Arrays;
// Importación de mapas clave-valor
import java.util.HashMap;
// Importación de conjuntos sin duplicados
import java.util.HashSet;
// Importación de la interfaz List
import java.util.List;
// Importación para formatos regionales y conversión de mayúsculas
import java.util.Locale;
// Importación de la interfaz Map
import java.util.Map;
// Importación de la interfaz Set
import java.util.Set;

// Importación de recursos XML del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de respuesta de la API ADSB.lol
import es.medac.skycollectorapp.models.AdsbResponse;
// Importación del modelo de respuesta de la API ADSBDB (aeronaves y rutas)
import es.medac.skycollectorapp.models.AdsbdbResponse;
// Importación del modelo de datos Avion
import es.medac.skycollectorapp.models.Avion;
// Importación del modelo de vuelo en tiempo real
import es.medac.skycollectorapp.models.Vuelo;
// Importación de la interfaz Retrofit para consultar vuelos en zona
import es.medac.skycollectorapp.network.AdsbService;
// Importación de la interfaz Retrofit para consultar detalles y rutas
import es.medac.skycollectorapp.network.AdsbdbService;
// Importación del motor global de identificación y detección de Easter Eggs
import es.medac.skycollectorapp.utils.AvionGenerator;

// Importación de llamadas asíncronas de Retrofit
import retrofit2.Call;
// Importación de callbacks de respuesta de Retrofit
import retrofit2.Callback;
// Importación de respuestas HTTP de Retrofit
import retrofit2.Response;
// Importación del cliente HTTP Retrofit
import retrofit2.Retrofit;
// Importación del conversor JSON Gson para Retrofit
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Actividad que gestiona el radar aéreo en tiempo real sobre Google Maps,
 * mostrando vuelos activos, rutas validadas, trayectorias, el radio visual de 40 km
 * y carga inmediata al desplazar la cámara del mapa.
 */
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Enumeración que define los modos de visualización del mapa (todos o solo capturados)
    private enum ModoMapa { TODOS, AVISTADOS }
    // Modo de filtrado activo actualmente en el radar
    private ModoMapa modoActual = ModoMapa.TODOS;

    // URL base del servicio REST de tráfico aéreo en tiempo real
    private static final String URL_API_VUELOS = "https://api.adsb.lol/";
    // Radio máximo en millas náuticas permitido por la API de radar
    private static final int RADIO_MAXIMO_NM = 250;
    // Distancia máxima en kilómetros desde el usuario para poder capturar un vuelo
    public static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 40f;
    // URL base del servicio REST para detalles de aeronaves y rutas comerciales
    private static final String URL_API_DETALLES = "https://api.adsbdb.com/";

    // Intervalo periódico de refresco automático de datos de radar (8 segundos)
    private static final long INTERVALO_ACTUALIZACION_MS = 8000;
    // Tiempo de espera tras detener el arrastre de cámara antes de consultar la nueva zona
    private static final long DEBOUNCE_MOVIMIENTO_CAMARA_MS = 450;
    // Tiempo mínimo entre descargas disparadas por movimiento de cámara
    private static final long COOLDOWN_DESCARGA_MOVIMIENTO_MS = 1200;
    // Intervalo de interpolación suave de movimiento de iconos (300 ms)
    private static final long INTERVALO_ANIMACION_MS = 300;
    // Nivel mínimo de zoom requerido para activar la interpolación de movimiento
    private static final float ZOOM_MINIMO_ANIMACION = 7f;
    // Radio medio de la Tierra en metros para cálculos geodésicos
    private static final double RADIO_TIERRA = 6371000.0;
    // Intervalo entre puntos fijos añadidos a la polilínea de trayectoria
    private static final long INTERVALO_PUNTO_TRAYECTORIA_MS = 5000;

    // Instancia principal del mapa de Google
    private GoogleMap mMap;
    // Servicio Retrofit para descargar vuelos activos
    private AdsbService service;
    // Servicio Retrofit para consultar matrículas, modelos y rutas
    private AdsbdbService serviceDetalles;
    // Manejador asociado al hilo principal para temporizadores y actualizaciones UI
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Bandera que indica si la actividad está visible y activa en primer plano
    private boolean activa = false;
    // Bandera que evita lanzar peticiones de radar duplicadas en paralelo
    private boolean descargando = false;
    // Bandera que marca si quedó pendiente una descarga al terminar un movimiento de cámara
    private boolean descargaPendienteTrasMovimiento = false;
    // Bandera para mostrar solo una vez el aviso de límite de peticiones HTTP 429
    private boolean avisoLimiteMostrado = false;
    // Marca temporal de la última descarga forzada manualmente
    private long ultimaDescargaManual = 0;
    // Marca temporal de la última descarga provocada por desplazamiento de cámara
    private long ultimaDescargaPorMovimiento = 0;

    // Tarea periódica que solicita vuelos actualizados cada 8 segundos
    private final Runnable tareaActualizar = new Runnable() {
        @Override
        public void run() {
            // Ejecuta descarga silenciosa y reprograma la siguiente ejecución
            descargar(false);
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS);
        }
    };

    // Tarea ejecutada tras detenerse la cámara para cargar aviones en la nueva región visible
    private final Runnable tareaDescargaPorCamara = () -> {
        // Verifica que la pantalla siga activa y el mapa esté inicializado
        if (!activa || mMap == null) return;
        // Comprueba el cooldown para evitar saturar la API con movimientos rápidos
        long ahora = SystemClock.elapsedRealtime();
        if (ahora - ultimaDescargaPorMovimiento >= COOLDOWN_DESCARGA_MOVIMIENTO_MS) {
            ultimaDescargaPorMovimiento = ahora;
            descargar(false);
        }
    };

    // Tarea periódica que interpola la posición de los marcadores entre paquetes de radar
    private final Runnable tareaAnimar = new Runnable() {
        @Override
        public void run() {
            // Actualiza posiciones estimadas y reprograma el siguiente fotograma
            animarMarcadores();
            handler.postDelayed(this, INTERVALO_ANIMACION_MS);
        }
    };

    // Panel inferior desplegable con la información resumida del avión seleccionado
    private CardView panel;
    // Etiquetas de modelo/empresa y telemetría dentro del panel inferior
    private TextView txtModelo, txtDatos;
    // Botones de avistar avión, filtro todos y filtro avistados
    private Button btnAvistar, btnTodos, btnAvistados;
    // Botón superior para regresar a la pantalla anterior
    private ImageButton btnVolverMapa;

    // Lista completa de aviones ya capturados por el usuario
    private final List<Avion> miColeccion = new ArrayList<>();
    // Mapa indexado por ICAO24 en minúsculas para comprobar rápidamente si un vuelo ya fue avistado
    private final Map<String, Avion> coleccionPorIcao = new HashMap<>();
    // Mapa de marcadores activos en el mapa indexados por código ICAO24
    private final Map<String, Marker> marcadores = new HashMap<>();
    // Caché en memoria de iconos BitmapDescriptor para no regenerarlos en cada refresco
    private final Map<Integer, BitmapDescriptor> cacheIconos = new HashMap<>();
    // Mapa de estados cinemáticos (posición, velocidad, rumbo) para animación fluida
    private final Map<String, EstadoVuelo> estados = new HashMap<>();
    // Lista ordenada de coordenadas que conforman la estela del avión seleccionado
    private final List<LatLng> puntosTrayectoria = new ArrayList<>();

    // Círculo azul translúcido que delimita los 40 km de alcance alrededor del usuario
    private Circle circuloRadioAvistamiento;
    // Polilínea fija con el historial de puntos recorridos por el vuelo seleccionado
    private Polyline lineaHistorial;
    // Polilínea dinámica que une el último punto fijo con la posición animada actual
    private Polyline lineaActual;
    // Marca temporal del último punto añadido al historial de trayectoria
    private long ultimoPuntoTrayectoria = 0;

    // Código ICAO24 del blanco seleccionado actualmente en el mapa
    private String icaoSeleccionado = null;
    // Código de diseño de tipo ICAO del blanco seleccionado
    private String tipoSeleccionado = null;
    // Matrícula del blanco seleccionado
    private String matriculaSeleccionada = null;
    // Última ubicación GPS/Red conocida del dispositivo del usuario
    private Location ultimaUbicacion = null;
    // Objeto Vuelo correspondiente al marcador seleccionado
    private Vuelo vueloSeleccionado = null;
    // Objeto Avion si el blanco seleccionado ya pertenece a la colección del usuario
    private Avion avistadoSeleccionado = null;

    // Caché de fichas técnicas de aeronaves obtenidas de ADSBDB por código ICAO24
    private final Map<String, AdsbdbResponse.Aeronave> cacheAeronaves = new HashMap<>();
    // Caché de rutas comerciales validadas obtenidas de ADSBDB por indicativo callsign
    private final Map<String, AdsbdbResponse.RutaVuelo> cacheRutas = new HashMap<>();
    // Conjunto de códigos ICAO que no devolvieron datos para evitar reintentos inútiles
    private final Set<String> aeronavesNoDisponibles = new HashSet<>();
    // Conjunto de callsigns sin ruta válida para evitar consultas repetidas
    private final Set<String> rutasNoDisponibles = new HashSet<>();

    // Lanzador que gestiona la respuesta del usuario al solicitar permisos de ubicación
    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Comprueba si se concedió ubicación precisa o aproximada
                boolean concedido =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (concedido) {
                    // Activa el seguimiento GPS en el mapa
                    activarUbicacion();
                } else {
                    // Informa al usuario de que no se dispone de permiso de localización
                    Toast.makeText(this, getString(R.string.toast_sin_permiso_ubicacion), Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Almacena la última posición conocida, velocidad y rumbo de un vuelo para interpolar su movimiento.
     */
    private static class EstadoVuelo {
        // Latitud base recibida del radar
        final double lat;
        // Longitud base recibida del radar
        final double lon;
        // Velocidad sobre el suelo en metros por segundo
        final double velocidad;
        // Rumbo verdadero en grados (0-360)
        final double rumbo;
        // Instante temporal (elapsedRealtime) en que se recibió esta posición
        final long tiempoBase;

        EstadoVuelo(double lat, double lon, double velocidad, double rumbo, long tiempoBase) {
            // Inicializa los atributos cinemáticos del vuelo
            this.lat = lat;
            this.lon = lon;
            this.velocidad = velocidad;
            this.rumbo = rumbo;
            this.tiempoBase = tiempoBase;
        }

        LatLng posicionEn(long ahora) {
            // Calcula el tiempo transcurrido acotado a un máximo de 60 segundos
            double segundos = Math.min((ahora - tiempoBase) / 1000.0, 60);
            // Calcula la distancia recorrida en metros
            double distancia = velocidad * segundos;
            // Convierte el rumbo de grados a radianes
            double rumboRad = Math.toRadians(rumbo);

            // Calcula el desplazamiento angular en latitud y longitud sobre la esfera terrestre
            double dLat = (distancia * Math.cos(rumboRad)) / RADIO_TIERRA;
            double dLon = (distancia * Math.sin(rumboRad)) / (RADIO_TIERRA * Math.cos(Math.toRadians(lat)));

            // Devuelve las nuevas coordenadas extrapoladas
            return new LatLng(lat + Math.toDegrees(dLat), lon + Math.toDegrees(dLon));
        }
    }

    /**
     * Inicializa la interfaz del mapa, los clientes REST (ADSB y ADSBDB) y los controles de filtrado.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Establece el diseño XML de la pantalla de radar
        setContentView(R.layout.activity_mapa);

        // Vincula los componentes visuales de la interfaz
        panel = findViewById(R.id.card_info_vuelo);
        txtModelo = findViewById(R.id.txt_modelo_panel);
        txtDatos = findViewById(R.id.txt_datos_panel);
        btnAvistar = findViewById(R.id.btn_avistar);
        btnTodos = findViewById(R.id.btn_todos);
        btnAvistados = findViewById(R.id.btn_avistados);
        btnVolverMapa = findViewById(R.id.btn_volver_mapa);

        // Configura el botón de volver para cerrar la pantalla del mapa
        btnVolverMapa.setOnClickListener(v -> finish());

        // Carga en memoria los aviones ya coleccionados por el usuario
        cargarColeccion();

        // Configura el cliente Retrofit para la API de posiciones en vivo ADSB.lol
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL_API_VUELOS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(AdsbService.class);

        // Configura el cliente Retrofit para la API de datos de aeronaves y rutas ADSBDB
        Retrofit retrofitDetalles = new Retrofit.Builder()
                .baseUrl(URL_API_DETALLES)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        serviceDetalles = retrofitDetalles.create(AdsbdbService.class);

        // Obtiene el fragmento del mapa y solicita su inicialización asíncrona
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configura el botón de filtro "Todos" para mostrar todo el tráfico aéreo
        btnTodos.setOnClickListener(v -> {
            modoActual = ModoMapa.TODOS;
            refrescarMapa();
            btnAvistar.setVisibility(View.VISIBLE);
        });

        // Configura el botón de filtro "Avistados" para mostrar solo aviones ya capturados
        btnAvistados.setOnClickListener(v -> {
            modoActual = ModoMapa.AVISTADOS;
            refrescarMapa();
            btnAvistar.setVisibility(View.GONE);
        });

        // Configura el botón rápido de avistar en el panel inferior
        btnAvistar.setOnClickListener(v -> avistarSeleccionado());

        // Abre la ficha detallada completa al pulsar sobre el panel inferior de vuelo
        panel.setOnClickListener(v -> abrirDetalleVueloCompleto());
    }

    /**
     * Reanuda las actualizaciones periódicas del radar y actualiza el marcador si el avión fue recién avistado.
     */
    @Override
    protected void onResume() {
        // Llama al método onResume de la superclase y activa la bandera de estado
        super.onResume();
        activa = true;
        // Recarga la colección local por si el usuario acaba de capturar un avión
        cargarColeccion();

        // Si había un blanco seleccionado, comprueba si acaba de añadirse a la colección
        if (icaoSeleccionado != null) {
            Avion recienAvistado = buscarPorIcao(icaoSeleccionado);
            if (recienAvistado != null) {
                // Actualiza el icono del marcador según su rareza y oculta el botón de avistar
                avistadoSeleccionado = recienAvistado;
                Marker m = marcadores.get(icaoSeleccionado);
                if (m != null) {
                    m.setIcon(iconoSegunRareza(recienAvistado.getRareza()));
                    m.setTag(new Object[]{vueloSeleccionado, recienAvistado});
                }
                btnAvistar.setVisibility(View.GONE);
                if (panel.getVisibility() == View.VISIBLE) pintarPanel();
            }
        }

        // Reinicia los bucles periódicos de descarga de radar y animación de iconos
        handler.removeCallbacks(tareaActualizar);
        handler.postDelayed(tareaActualizar, 300);
        handler.removeCallbacks(tareaAnimar);
        handler.postDelayed(tareaAnimar, INTERVALO_ANIMACION_MS);
    }

    /**
     * Detiene las tareas periódicas de descarga, movimiento y animación al pausar la actividad.
     */
    @Override
    protected void onPause() {
        // Llama a la superclase, desactiva la bandera y cancela todas las tareas pendientes del Handler
        super.onPause();
        activa = false;
        handler.removeCallbacks(tareaActualizar);
        handler.removeCallbacks(tareaDescargaPorCamara);
        handler.removeCallbacks(tareaAnimar);
    }

    /**
     * Configura los listeners de Google Maps, la detección inmediata de movimiento de cámara
     * y solicita los permisos de ubicación GPS al estar listo el mapa.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Guarda la referencia al mapa listo y registra el listener de clic en marcadores
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        // Oculta el panel inferior y borra la trayectoria al pulsar en una zona vacía del mapa
        mMap.setOnMapClickListener(latLng -> {
            panel.setVisibility(View.GONE);
            ajustarPaddingMapa(false);
            borrarTrayectoria();
        });

        // Dispara una descarga rápida en cuanto el usuario termina de arrastrar o hacer zoom en el mapa
        mMap.setOnCameraIdleListener(() -> {
            if (!activa) return;
            handler.removeCallbacks(tareaDescargaPorCamara);
            handler.postDelayed(tareaDescargaPorCamara, DEBOUNCE_MOVIMIENTO_CAMARA_MS);
        });

        // Cancela descargas pendientes mientras el dedo sigue arrastrando el mapa
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                handler.removeCallbacks(tareaDescargaPorCamara);
            }
        });

        // Activa los botones nativos de zoom y gestiona la ubicación del usuario
        mMap.getUiSettings().setZoomControlsEnabled(true);
        pedirOActivarUbicacion();

        // Lanza la primera descarga de vuelos si la actividad está activa
        if (activa) {
            handler.removeCallbacks(tareaActualizar);
            handler.postDelayed(tareaActualizar, 300);
        }
    }

    /**
     * Comprueba permisos de localización y lanza la solicitud al usuario si aún no están concedidos.
     */
    private void pedirOActivarUbicacion() {
        // Verifica el estado de los permisos de ubicación precisa y aproximada
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            // Si alguno está concedido, activa la ubicación en el mapa
            activarUbicacion();
        } else {
            // Centra inicialmente sobre Madrid (España) y solicita permisos al usuario
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            permisoUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Activa la capa de ubicación del usuario en el mapa, dibuja el círculo de 40 km
     * y suscribe listeners de actualización GPS/Red.
     */
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void activarUbicacion() {
        // Comprueba que el mapa esté disponible
        if (mMap == null) return;
        // Habilita el punto azul de ubicación y el botón de centrar mi ubicación
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        // Actualiza la última posición y el círculo de 40 km cada vez que Google Maps detecta movimiento
        mMap.setOnMyLocationChangeListener(location -> {
            ultimaUbicacion = location;
            actualizarCirculoAvistamiento(location);
        });

        // Accede al servicio LocationManager para obtener una posición inmediata y suscribir actualizaciones
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            ultimaUbicacion = gps != null ? gps : net;
            if (ultimaUbicacion != null) {
                actualizarCirculoAvistamiento(ultimaUbicacion);
            }

            // Define el listener para recibir coordenadas periódicas desde GPS y Red
            LocationListener locListener = location -> {
                ultimaUbicacion = location;
                actualizarCirculoAvistamiento(location);
            };
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, locListener);
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10f, locListener);
            }
        }

        // Centra la cámara del mapa sobre la posición del usuario
        centrarEnUbicacionActual();
    }

    /**
     * Dibuja o actualiza sobre el mapa el perímetro visual de 40 km dentro del cual se pueden avistar aeronaves.
     */
    private void actualizarCirculoAvistamiento(Location location) {
        // Verifica que el mapa y la ubicación sean válidos
        if (mMap == null || location == null) return;
        // Convierte la ubicación a LatLng y calcula el radio en metros
        LatLng centro = new LatLng(location.getLatitude(), location.getLongitude());
        double radioMetros = DISTANCIA_MAXIMA_AVISTAMIENTO_KM * 1000.0;

        if (circuloRadioAvistamiento == null) {
            // Crea el círculo semitransparente azul la primera vez
            circuloRadioAvistamiento = mMap.addCircle(new CircleOptions()
                    .center(centro)
                    .radius(radioMetros)
                    .strokeColor(Color.parseColor("#661E88E5"))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor("#141E88E5"))
                    .clickable(false));
        } else {
            // Actualiza el centro y radio del círculo existente conforme se mueve el usuario
            circuloRadioAvistamiento.setCenter(centro);
            circuloRadioAvistamiento.setRadius(radioMetros);
        }
    }

    /**
     * Centra la cámara del mapa en la última ubicación conocida del dispositivo.
     */
    @SuppressLint("MissingPermission")
    private void centrarEnUbicacionActual() {
        // Comprueba que el mapa esté inicializado
        if (mMap == null) return;

        // Verifica nuevamente los permisos antes de acceder a la ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            return;
        }

        // Si ya se conoce la ubicación en memoria, mueve la cámara directamente hacia ella
        if (ultimaUbicacion != null) {
            LatLng miPosicion = new LatLng(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 10f));
            actualizarCirculoAvistamiento(ultimaUbicacion);
            return;
        }

        // Consulta la última ubicación almacenada en los proveedores GPS o Red
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        Location ubicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (ubicacion == null) {
            ubicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (ubicacion != null) {
            // Centra la cámara en la última ubicación recuperada
            ultimaUbicacion = ubicacion;
            LatLng miPosicion = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 10f));
            actualizarCirculoAvistamiento(ubicacion);
        } else {
            // Si no hay ubicación cacheada, centra en Madrid y solicita una actualización única
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            String proveedor = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;

            locationManager.requestSingleUpdate(proveedor, location -> {
                ultimaUbicacion = location;
                LatLng miPosicion = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 10f));
                actualizarCirculoAvistamiento(location);
            }, null);
        }
    }

    /**
     * Calcula la distancia en kilómetros entre el usuario y la aeronave seleccionada.
     */
    private Float distanciaAlAvionKm() {
        // Verifica que exista ubicación del usuario y un avión seleccionado
        if (ultimaUbicacion == null || icaoSeleccionado == null) return null;
        Marker m = marcadores.get(icaoSeleccionado);
        if (m == null) return null;

        // Calcula la distancia geodésica en metros y la convierte a kilómetros
        float[] resultado = new float[1];
        Location.distanceBetween(
                ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(),
                m.getPosition().latitude, m.getPosition().longitude, resultado);
        return resultado[0] / 1000f;
    }

    /**
     * Valida la distancia al avión seleccionado, detecta si es un Easter Egg terrestre
     * y lanza la actividad para registrarlo en el hangar.
     */
    private void avistarSeleccionado() {
        // Comprueba que haya un blanco seleccionado en el mapa
        if (icaoSeleccionado == null || vueloSeleccionado == null) {
            Toast.makeText(this, getString(R.string.toast_selecciona_avion_primero), Toast.LENGTH_SHORT).show();
            return;
        }

        // Calcula la distancia al blanco y comprueba que se haya podido determinar la ubicación
        Float distancia = distanciaAlAvionKm();
        if (distancia == null) {
            Toast.makeText(this, getString(R.string.toast_no_comprobar_ubicacion), Toast.LENGTH_LONG).show();
            return;
        }

        // Impide la captura si el avión supera el radio máximo de 40 km
        if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(
                    this,
                    getString(R.string.toast_acercarse_rango, Math.round(distancia), Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM)),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Obtiene las coordenadas actuales del marcador o del paquete de radar
        Marker m = marcadores.get(icaoSeleccionado);
        LatLng posAvion = m != null ? m.getPosition() : new LatLng(vueloSeleccionado.latitude, vueloSeleccionado.longitude);

        // Evalúa si el registro corresponde a un Easter Egg terrestre (vehículo Follow-Me / Ops)
        boolean esEasterEgg = AvionGenerator.esEasterEggTerrestre(
                tipoSeleccionado,
                vueloSeleccionado.callsign,
                matriculaSeleccionada,
                vueloSeleccionado.descripcion
        );

        String nombre;
        String tipoGuardar = tipoSeleccionado;
        String urlFotoGuardar = null;
        if (esEasterEgg) {
            // Asigna modelo, foto oficial y tipo ICAO por defecto para el Easter Egg
            nombre = getString(R.string.easter_egg_vehiculo_modelo);
            urlFotoGuardar = AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO;
            if (tipoGuardar == null || tipoGuardar.trim().isEmpty()) {
                tipoGuardar = (vueloSeleccionado.callsign != null && !vueloSeleccionado.callsign.trim().isEmpty())
                        ? vueloSeleccionado.callsign.trim().toUpperCase(Locale.ROOT)
                        : "OPS";
            }
        } else {
            // Resuelve el nombre comercial completo desde caché ADSBDB o descripción de radar
            nombre = modeloCompleto(cacheAeronaves.get(icaoSeleccionado));
            if (nombre == null && !AvionGenerator.esModeloDesconocido(this, vueloSeleccionado.descripcion)) {
                nombre = vueloSeleccionado.descripcion;
            }
        }

        // Guarda temporalmente los datos del vuelo seleccionado en SharedPreferences para AddAvionActivity
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icaoSeleccionado)
                .putString("callsign_seleccionado", vueloSeleccionado.callsign)
                .putString("tipo_seleccionado", tipoGuardar)
                .putString("matricula_seleccionada", matriculaSeleccionada)
                .putString("descripcion_seleccionada", nombre)
                .putString("url_imagen_remota_seleccionada", urlFotoGuardar)
                .putBoolean("es_easter_egg_seleccionado", esEasterEgg)
                .putLong("fecha_seleccionada", System.currentTimeMillis())
                .putFloat("distancia_seleccionada", distancia)
                .putFloat("lat_avion_seleccionado", (float) posAvion.latitude)
                .putFloat("lon_avion_seleccionado", (float) posAvion.longitude)
                .apply();

        // Inicia AddAvionActivity para fotografiar y guardar el avistamiento
        Intent intent = new Intent(MapaActivity.this, AddAvionActivity.class);
        startActivity(intent);
    }

    /**
     * Abre la pantalla de detalle completo del vuelo pasando telemetría, callsign,
     * indicadores de Easter Egg y ruta validada.
     */
    private void abrirDetalleVueloCompleto() {
        // Verifica que exista un vuelo seleccionado
        if (icaoSeleccionado == null || vueloSeleccionado == null) return;

        // Comprueba si el blanco seleccionado es un Easter Egg terrestre
        boolean esEasterEgg = AvionGenerator.esEasterEggTerrestre(
                tipoSeleccionado,
                vueloSeleccionado.callsign,
                matriculaSeleccionada,
                vueloSeleccionado.descripcion
        );

        // Recupera la aeronave y ruta desde las cachés locales si están disponibles
        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(icaoSeleccionado);
        String callsignKey = vueloSeleccionado.callsign != null
                ? vueloSeleccionado.callsign.trim().toUpperCase(Locale.ROOT)
                : null;
        AdsbdbResponse.RutaVuelo ruta = callsignKey != null ? cacheRutas.get(callsignKey) : null;

        // Determina el tipo ICAO efectivo combinando radar, ADSBDB y fallback de Easter Egg
        String tipoEfectivo = tipoSeleccionado;
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && aeronave != null && aeronave.icaoType != null) {
            tipoEfectivo = aeronave.icaoType.trim().toUpperCase(Locale.ROOT);
        }
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && esEasterEgg) {
            tipoEfectivo = (callsignKey != null && !callsignKey.isEmpty()) ? callsignKey : "OPS";
        }

        // Resuelve el nombre del modelo oficial a mostrar en la ficha
        String modeloOficial = esEasterEgg ? getString(R.string.easter_egg_vehiculo_modelo) : modeloCompleto(aeronave);
        if (modeloOficial == null && vueloSeleccionado.descripcion != null
                && !AvionGenerator.esModeloDesconocido(this, vueloSeleccionado.descripcion)) {
            modeloOficial = vueloSeleccionado.descripcion;
        }

        // Si el usuario ya lo avistó previamente, recupera el nombre guardado en su colección
        String apodoUsuario = null;
        if (avistadoSeleccionado != null && avistadoSeleccionado.getModelo() != null
                && !avistadoSeleccionado.getModelo().trim().isEmpty()) {
            apodoUsuario = avistadoSeleccionado.getModelo().trim();
        }

        // Determina el operador, aerolínea o fabricante de la entidad
        String empresa = esEasterEgg ? getString(R.string.easter_egg_vehiculo_fabricante) : null;
        if (empresa == null) {
            if (ruta != null && ruta.airline != null && ruta.airline.name != null) {
                empresa = ruta.airline.name;
            } else if (aeronave != null && aeronave.registeredOwner != null) {
                empresa = formatearNombre(aeronave.registeredOwner);
            } else if (avistadoSeleccionado != null) {
                empresa = avistadoSeleccionado.getFabricante();
            }
        }

        // Formatea el texto de origen -> destino si la ruta fue validada
        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null) ? origen + " → " + destino : null;

        // Convierte velocidad de m/s a km/h y formatea la altitud
        int velKmh = vueloSeleccionado.velocity != null ? (int) (vueloSeleccionado.velocity * 3.6) : 0;
        String altitud = vueloSeleccionado.enTierra
                ? getString(R.string.altitud_en_tierra)
                : (vueloSeleccionado.altitude != null ? Math.round(vueloSeleccionado.altitude) + " m" : "—");

        // Obtiene distancia y coordenadas actuales del marcador
        Float distancia = distanciaAlAvionKm();
        Marker m = marcadores.get(icaoSeleccionado);
        double lat = m != null ? m.getPosition().latitude : vueloSeleccionado.latitude;
        double lon = m != null ? m.getPosition().longitude : vueloSeleccionado.longitude;

        // Empaqueta todos los datos en un Intent y abre DetalleVueloRadarActivity
        Intent intent = new Intent(this, DetalleVueloRadarActivity.class);
        intent.putExtra("icao", icaoSeleccionado);
        intent.putExtra("callsign", vueloSeleccionado.callsign);
        intent.putExtra("tipo", tipoEfectivo);
        intent.putExtra("matricula", matriculaSeleccionada);
        intent.putExtra("modelo", modeloOficial);
        intent.putExtra("apodo", apodoUsuario);
        intent.putExtra("empresa", empresa);
        intent.putExtra("ruta", lineaRuta);
        intent.putExtra("velocidad_kmh", velKmh);
        intent.putExtra("altitud", altitud);
        intent.putExtra("distancia_km", distancia != null ? distancia : -1f);
        intent.putExtra("lat_avion", lat);
        intent.putExtra("lon_avion", lon);
        intent.putExtra("ya_avistado", avistadoSeleccionado != null);
        intent.putExtra("es_easter_egg", esEasterEgg);
        startActivity(intent);
    }

    /**
     * Limpia los marcadores y trayectorias actuales del mapa y fuerza una nueva descarga de vuelos.
     */
    private void refrescarMapa() {
        // Elimina todos los marcadores del mapa y limpia las colecciones auxiliares
        for (Marker m : marcadores.values()) m.remove();
        marcadores.clear();
        estados.clear();
        // Borra la trayectoria activa, oculta el panel y lanza una descarga nueva
        borrarTrayectoria();
        panel.setVisibility(View.GONE);
        ajustarPaddingMapa(false);
        descargar(true);
    }

    /**
     * Ajusta el margen inferior del mapa cuando el panel de información de vuelo está desplegado.
     */
    private void ajustarPaddingMapa(boolean panelVisible) {
        // Verifica que el mapa esté disponible
        if (mMap == null) return;
        if (panelVisible) {
            // Espera a que el panel calcule su altura y desplaza los controles inferiores del mapa hacia arriba
            panel.post(() -> {
                int margen = (int) (16 * getResources().getDisplayMetrics().density);
                mMap.setPadding(0, 0, 0, panel.getHeight() + margen * 2);
            });
        } else {
            // Restaura el relleno inferior del mapa a cero
            mMap.setPadding(0, 0, 0, 0);
        }
    }

    /**
     * Devuelve la clave de almacenamiento local en SharedPreferences asociada al usuario actual.
     */
    private String getClaveListaUsuario() {
        // Obtiene el usuario autenticado en FirebaseAuth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Devuelve clave personalizada por UID o anónima por defecto
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Carga en memoria los aviones avistados por el usuario indexados por código ICAO24.
     */
    private void cargarColeccion() {
        // Limpia las estructuras anteriores en memoria
        miColeccion.clear();
        coleccionPorIcao.clear();

        // Lee el JSON guardado en SharedPreferences
        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString(getClaveListaUsuario(), null);
        if (json == null) return;

        // Deserializa la lista de aviones y corrige posibles fechas corruptas
        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista != null) {
            miColeccion.addAll(sanearFechasAvistamientos(lista));
        }

        // Indexa cada avión por su ICAO24 en minúsculas para búsquedas instantáneas O(1)
        for (Avion a : miColeccion) {
            if (a.getIcao24() != null) {
                coleccionPorIcao.put(a.getIcao24().toLowerCase(), a);
            }
        }
    }

    /**
     * Busca una aeronave en la colección local del usuario a partir de su código hexadecimal ICAO24.
     */
    private Avion buscarPorIcao(String icao) {
        // Devuelve null si el parámetro es nulo o el objeto Avion desde el índice
        if (icao == null) return null;
        return coleccionPorIcao.get(icao.toLowerCase());
    }

    /**
     * Descarga los vuelos activos dentro de la región visible actual del mapa.
     * Si ya hay una petición en curso, encola una actualización inmediata para la nueva zona.
     */
    private void descargar(boolean avisar) {
        // Verifica que el mapa esté inicializado
        if (mMap == null) return;
        // Si ya hay una descarga en curso, marca que debe repetirse al finalizar
        if (descargando) {
            descargaPendienteTrasMovimiento = true;
            return;
        }

        // Si fue solicitada manualmente, aplica un anti-rebote de 3 segundos
        if (avisar) {
            long ahora = SystemClock.elapsedRealtime();
            if (ahora - ultimaDescargaManual < 3000) {
                Toast.makeText(this, getString(R.string.toast_espera_refrescar), Toast.LENGTH_SHORT).show();
                return;
            }
            ultimaDescargaManual = ahora;
        }

        // Calcula el centro y el radio en millas náuticas de la región visible en pantalla
        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng centro = zona.getCenter();

        float[] distancia = new float[1];
        Location.distanceBetween(centro.latitude, centro.longitude, zona.northeast.latitude, zona.northeast.longitude, distancia);
        int radioNm = (int) Math.ceil(distancia[0] / 1852.0);

        // Si el zoom es demasiado lejano (> 250 NM), aborta la consulta
        if (radioNm > RADIO_MAXIMO_NM) {
            if (avisar) {
                Toast.makeText(this, getString(R.string.toast_acerca_mapa), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Marca la descarga como activa y lanza la petición HTTP asíncrona
        descargando = true;
        descargaPendienteTrasMovimiento = false;

        service.getVuelosEnZona(
                String.format(Locale.US, "%.4f", centro.latitude),
                String.format(Locale.US, "%.4f", centro.longitude),
                radioNm
        ).enqueue(new Callback<AdsbResponse>() {
            @Override
            public void onResponse(Call<AdsbResponse> call, Response<AdsbResponse> response) {
                descargando = false;
                if (isDestroyed() || isFinishing()) return;

                // Si la API devuelve HTTP 429 (demasiadas peticiones), pausa el refresco durante 45 segundos
                if (response.code() == 429) {
                    if (!avisoLimiteMostrado) {
                        avisoLimiteMostrado = true;
                        Toast.makeText(MapaActivity.this, getString(R.string.toast_limite_api), Toast.LENGTH_LONG).show();
                    }
                    handler.removeCallbacks(tareaActualizar);
                    handler.postDelayed(tareaActualizar, 45000);
                    return;
                }

                // Convierte los aviones recibidos a objetos Vuelo y actualiza los marcadores del mapa
                if (response.body() != null && response.body().getAviones() != null) {
                    List<Vuelo> vuelos = new ArrayList<>();
                    for (AdsbResponse.AdsbAircraft a : response.body().getAviones()) {
                        vuelos.add(a.toVuelo());
                    }
                    procesar(vuelos);
                }

                // Si el usuario movió el mapa mientras descargaba, programa una actualización inmediata
                if (descargaPendienteTrasMovimiento && activa) {
                    descargaPendienteTrasMovimiento = false;
                    handler.postDelayed(tareaDescargaPorCamara, 250);
                }
            }

            @Override
            public void onFailure(Call<AdsbResponse> call, Throwable t) {
                // Libera la bandera de descarga en caso de error de red
                descargando = false;
            }
        });
    }

    /**
     * Comprueba que el código ICAO24 sea un identificador hexadecimal oficial y no un blanco temporal TIS-B ('~').
     */
    private boolean esIcaoValido(String icao24) {
        // Verifica que no sea nulo, no esté vacío y no empiece por virgulilla (~)
        if (icao24 == null) return false;
        String limpio = icao24.trim();
        return !limpio.isEmpty() && !limpio.startsWith("~");
    }

    /**
     * Actualiza posiciones, iconos y estados de los marcadores de vuelos recibidos desde la API.
     */
    private void procesar(List<Vuelo> vuelos) {
        // Conjunto para rastrear qué códigos ICAO siguen presentes en esta actualización
        Set<String> visibles = new HashSet<>();
        long ahora = SystemClock.elapsedRealtime();

        for (Vuelo api : vuelos) {
            // Filtra blancos inválidos o balizas estáticas de torre
            if (!esBlancoRadarValido(api)) continue;

            // Comprueba si el vuelo ya está en la colección del usuario y aplica filtro de modo
            Avion avistado = buscarPorIcao(api.icao24);
            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue;

            visibles.add(api.icao24);
            LatLng pos = new LatLng(api.latitude, api.longitude);

            // Conserva el tipo ICAO previamente resuelto para el avión seleccionado si el radar lo omite
            if ((api.tipo == null || api.tipo.trim().isEmpty()) && api.icao24.equals(icaoSeleccionado) && tipoSeleccionado != null) {
                api.tipo = tipoSeleccionado;
            }

            // Registra el nuevo estado cinemático base para la interpolación de movimiento
            estados.put(api.icao24, new EstadoVuelo(
                    api.latitude, api.longitude,
                    api.velocity != null ? api.velocity : 0,
                    api.trueTrack != null ? api.trueTrack : 0,
                    ahora));

            // Crea un marcador nuevo o actualiza la posición y rumbo del existente
            Marker m = marcadores.get(api.icao24);
            if (m == null) {
                BitmapDescriptor icono = avistado != null ? iconoSegunRareza(avistado.getRareza()) : iconoGenerico();
                m = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .icon(icono)
                        .rotation(api.trueTrack)
                        .flat(true)
                        .anchor(0.5f, 0.5f));
                marcadores.put(api.icao24, m);
            } else {
                m.setPosition(pos);
                m.setRotation(api.trueTrack);
            }

            // Adjunta el par [Vuelo, Avion] como etiqueta del marcador
            m.setTag(new Object[]{api, avistado});

            // Si es el avión seleccionado actualmente, refresca la ficha inferior
            if (api.icao24.equals(icaoSeleccionado)) {
                vueloSeleccionado = api;
                avistadoSeleccionado = avistado;
                if (panel.getVisibility() == View.VISIBLE) pintarPanel();
            }

            // Si tiene trayectoria activa, añade la nueva coordenada al historial
            if (api.icao24.equals(icaoSeleccionado) && lineaHistorial != null) {
                puntosTrayectoria.add(pos);
                dibujarTrayectoria();
            }
        }

        // Elimina del mapa y de memoria los marcadores de vuelos que ya salieron de la zona
        for (String icao : new ArrayList<>(marcadores.keySet())) {
            if (!visibles.contains(icao)) {
                Marker marcador = marcadores.remove(icao);
                if (marcador != null) marcador.remove();
                estados.remove(icao);
            }
        }
    }

    /**
     * Interpola suavemente las coordenadas de los marcadores visibles entre actualizaciones de red.
     */
    private void animarMarcadores() {
        // Aborta si no hay mapa o marcadores activos
        if (mMap == null || marcadores.isEmpty()) return;

        long ahora = SystemClock.elapsedRealtime();

        // Anima en primer lugar el avión seleccionado y actualiza su estela en tiempo real
        if (icaoSeleccionado != null && lineaHistorial != null) {
            Marker seleccionado = marcadores.get(icaoSeleccionado);
            EstadoVuelo estadoSel = estados.get(icaoSeleccionado);

            if (seleccionado != null && estadoSel != null && estadoSel.velocidad >= 1) {
                LatLng posSel = estadoSel.posicionEn(ahora);
                seleccionado.setPosition(posSel);

                if (ahora - ultimoPuntoTrayectoria >= INTERVALO_PUNTO_TRAYECTORIA_MS) {
                    ultimoPuntoTrayectoria = ahora;
                    puntosTrayectoria.add(posSel);
                    dibujarTrayectoria();
                } else {
                    actualizarTramoActual(posSel);
                }
            }
        }

        // Si el zoom es muy lejano, omite animar el resto de aviones para ahorrar batería y CPU
        if (mMap.getCameraPosition().zoom < ZOOM_MINIMO_ANIMACION) return;

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds;

        // Recorre los marcadores visibles dentro de la pantalla y actualiza su posición extrapolada
        for (Map.Entry<String, Marker> e : marcadores.entrySet()) {
            if (e.getKey().equals(icaoSeleccionado)) continue;

            EstadoVuelo estado = estados.get(e.getKey());
            if (estado == null || estado.velocidad < 1) continue;

            LatLng pos = estado.posicionEn(ahora);
            if (!zona.contains(pos)) continue;

            e.getValue().setPosition(pos);
        }
    }

    /**
     * Inicia la polilínea de trayectoria para la aeronave seleccionada.
     */
    private void mostrarTrayectoria(String icao) {
        // Limpia cualquier estela anterior y toma la posición actual como punto inicial
        borrarTrayectoria();
        Marker m = marcadores.get(icao);
        if (m != null) puntosTrayectoria.add(m.getPosition());
        ultimoPuntoTrayectoria = SystemClock.elapsedRealtime();
        dibujarTrayectoria();
    }

    /**
     * Dibuja en el mapa el historial de puntos recorridos y el segmento en tiempo real.
     */
    private void dibujarTrayectoria() {
        // Crea o actualiza la polilínea fija con todos los puntos históricos registrados
        if (lineaHistorial == null) lineaHistorial = mMap.addPolyline(estiloLinea());
        lineaHistorial.setPoints(new ArrayList<>(puntosTrayectoria));

        // Crea la polilínea dinámica del tramo final si aún no existe
        if (lineaActual == null) lineaActual = mMap.addPolyline(estiloLinea());

        // Conecta el último punto histórico con la posición actual del marcador
        Marker m = icaoSeleccionado != null ? marcadores.get(icaoSeleccionado) : null;
        if (m != null) actualizarTramoActual(m.getPosition());
    }

    /**
     * Actualiza el último segmento dinámico de la trayectoria hasta la posición interpolada actual.
     */
    private void actualizarTramoActual(LatLng posActual) {
        // Comprueba que la polilínea dinámica y el historial estén disponibles
        if (lineaActual == null || puntosTrayectoria.isEmpty()) return;
        // Une el último punto guardado con la coordenada interpolada actual
        LatLng ultimo = puntosTrayectoria.get(puntosTrayectoria.size() - 1);
        lineaActual.setPoints(Arrays.asList(ultimo, posActual));
    }

    /**
     * Elimina las líneas de trayectoria activas del mapa.
     */
    private void borrarTrayectoria() {
        // Elimina del mapa la polilínea histórica si existe
        if (lineaHistorial != null) {
            lineaHistorial.remove();
            lineaHistorial = null;
        }
        // Elimina del mapa la polilínea dinámica si existe
        if (lineaActual != null) {
            lineaActual.remove();
            lineaActual = null;
        }
        // Vacía la lista de coordenadas almacenadas
        puntosTrayectoria.clear();
    }

    /**
     * Define el color y grosor de la línea de trayectoria de vuelo.
     */
    private PolylineOptions estiloLinea() {
        // Devuelve el estilo azul con grosor de 8 píxeles para la estela de vuelo
        return new PolylineOptions()
                .color(Color.parseColor("#1E88E5"))
                .width(8f)
                .geodesic(false);
    }

    /**
     * Gestiona la pulsación sobre un marcador de avión, mostrando su ficha inferior y consultando detalles.
     */
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // Extrae y valida los objetos Vuelo y Avion almacenados en el tag del marcador
        if (!(marker.getTag() instanceof Object[])) return false;
        Object[] data = (Object[]) marker.getTag();
        if (data.length < 2 || !(data[0] instanceof Vuelo)) return false;

        Vuelo api = (Vuelo) data[0];
        Avion a = data[1] instanceof Avion ? (Avion) data[1] : null;

        // Comprueba si se ha pulsado un avión distinto al anterior para reiniciar su trayectoria
        boolean cambioDeAvion = !api.icao24.equals(icaoSeleccionado) || lineaHistorial == null;
        icaoSeleccionado = api.icao24;
        tipoSeleccionado = api.tipo;
        matriculaSeleccionada = api.matricula;
        vueloSeleccionado = api;
        avistadoSeleccionado = a;

        if (cambioDeAvion) mostrarTrayectoria(api.icao24);

        // Actualiza los textos del panel inferior y solicita información extendida en segundo plano
        pintarPanel();
        pedirDetalles(api);

        // Muestra el panel inferior, ajusta el padding del mapa y gestiona visibilidad del botón Avistar
        panel.setVisibility(View.VISIBLE);
        ajustarPaddingMapa(true);
        btnAvistar.setVisibility(a == null && modoActual == ModoMapa.TODOS ? View.VISIBLE : View.GONE);

        return true;
    }

    /**
     * Lanza las consultas asíncronas utilizando el generador global y valida la ruta comercial del vuelo.
     */
    private void pedirDetalles(Vuelo api) {
        // Ignora identificadores ICAO no válidos
        if (!esIcaoValido(api.icao24)) return;

        // Consulta los datos técnicos de la aeronave en ADSBDB
        pedirAeronave(api.icao24);

        // Resuelve el modelo y tipo ICAO mediante el motor híbrido global
        AvionGenerator.resolverAvionGlobalAsync(this, api.icao24, api.callsign, api.matricula, api.tipo, api.descripcion, null, avionResuelto -> {
            if (avionResuelto != null && api.icao24.equalsIgnoreCase(icaoSeleccionado)) {
                if (avionResuelto.getTipoIcao() != null && !avionResuelto.getTipoIcao().isEmpty()) {
                    tipoSeleccionado = avionResuelto.getTipoIcao();
                    if (vueloSeleccionado != null) {
                        vueloSeleccionado.tipo = tipoSeleccionado;
                    }
                }
                if (avionResuelto.getModelo() != null && !AvionGenerator.esModeloDesconocido(this, avionResuelto.getModelo())) {
                    if (vueloSeleccionado != null) {
                        vueloSeleccionado.descripcion = avionResuelto.getModelo();
                    }
                }
                pintarPanel();
            }
        });

        // Si el indicativo tiene formato de aerolínea comercial, consulta su ruta origen-destino
        if (esCallsignComercialValido(api.callsign, api.matricula)) {
            pedirRuta(api);
        }
    }

    /**
     * Consulta la ficha de la aeronave en ADSBDB por su código ICAO24 y actualiza el tipo ICAO si faltaba.
     */
    private void pedirAeronave(String icao) {
        // Evita consultas duplicadas si ya está en caché o marcada como no disponible
        if (icao == null || cacheAeronaves.containsKey(icao) || aeronavesNoDisponibles.contains(icao)) return;

        serviceDetalles.getAeronave(icao).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                // Si no hay datos válidos en la respuesta, registra el ICAO como no disponible
                AdsbdbResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().aircraft == null) {
                    aeronavesNoDisponibles.add(icao);
                    return;
                }

                // Guarda la aeronave en caché y actualiza el panel si sigue seleccionada
                AdsbdbResponse.Aeronave aero = body.getRespuesta().aircraft;
                cacheAeronaves.put(icao, aero);
                if (icao.equals(icaoSeleccionado)) {
                    if ((tipoSeleccionado == null || tipoSeleccionado.trim().isEmpty())
                            && aero.icaoType != null && !aero.icaoType.trim().isEmpty()) {
                        tipoSeleccionado = aero.icaoType.trim().toUpperCase(Locale.ROOT);
                        if (vueloSeleccionado != null) {
                            vueloSeleccionado.tipo = tipoSeleccionado;
                        }
                    }
                    pintarPanel();
                }
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb aeronave fallo: " + t.getMessage());
            }
        });
    }

    /**
     * Consulta el origen, destino y aerolínea de un vuelo comercial y valida su coherencia geográfica.
     */
    private void pedirRuta(Vuelo vuelo) {
        // Normaliza el callsign y comprueba si ya existe en caché o en descartados
        String indicativo = vuelo.callsign.trim().toUpperCase(Locale.ROOT);
        if (cacheRutas.containsKey(indicativo) || rutasNoDisponibles.contains(indicativo)) return;

        serviceDetalles.getRuta(indicativo).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                // Descarta el callsign si la API no devuelve información de ruta
                AdsbdbResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().flightroute == null) {
                    rutasNoDisponibles.add(indicativo);
                    return;
                }

                AdsbdbResponse.RutaVuelo ruta = body.getRespuesta().flightroute;

                // Verifica mediante geometría esférica que la ruta sea coherente con la posición y rumbo del avión
                if (!esRutaCoherenteConVuelo(ruta, vuelo)) {
                    rutasNoDisponibles.add(indicativo);
                    return;
                }

                // Almacena la ruta validada en caché y refresca el panel inferior si corresponde
                cacheRutas.put(indicativo, ruta);
                if (vueloSeleccionado != null && indicativo.equalsIgnoreCase(vueloSeleccionado.callsign)) {
                    pintarPanel();
                }
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb ruta fallo: " + t.getMessage());
            }
        });
    }

    /**
     * Filtra indicativos que corresponden a matrículas privadas o vuelos no comerciales.
     */
    private boolean esCallsignComercialValido(String callsign, String matricula) {
        // Verifica longitud válida de un indicativo OACI comercial
        if (callsign == null) return false;
        String cs = callsign.trim().toUpperCase(Locale.ROOT);
        if (cs.length() < 4 || cs.length() > 8) return false;

        // Descarta si el callsign coincide directamente con la matrícula privada del avión
        if (matricula != null && cs.replace("-", "").equalsIgnoreCase(matricula.trim().replace("-", ""))) {
            return false;
        }

        // Comprueba el patrón estándar de 3 letras de aerolínea seguidas de número de vuelo
        return cs.matches("^[A-Z]{3}[0-9][A-Z0-9]{0,4}$");
    }

    /**
     * Comprueba mediante geometría esférica si la posición, el corredor lateral y el rumbo
     * actuales del avión encajan físicamente con el origen y destino devueltos por la API.
     */
    private boolean esRutaCoherenteConVuelo(AdsbdbResponse.RutaVuelo ruta, Vuelo vuelo) {
        // Valida que existan aeropuertos de origen y destino con coordenadas
        if (ruta == null || ruta.origin == null || ruta.destination == null) return false;
        if (vuelo == null || vuelo.latitude == null || vuelo.longitude == null) return true;

        Double latOrig = ruta.origin.latitude;
        Double lonOrig = ruta.origin.longitude;
        Double latDest = ruta.destination.latitude;
        Double lonDest = ruta.destination.longitude;

        if (latOrig == null || lonOrig == null || latDest == null || lonDest == null) return true;

        // Calcula distancias ortodrómicas entre origen, avión y destino
        double distRutaTotalKm = calcularDistanciaKm(latOrig, lonOrig, latDest, lonDest);
        double distDesdeOrigenKm = calcularDistanciaKm(latOrig, lonOrig, vuelo.latitude, vuelo.longitude);
        double distHastaDestinoKm = calcularDistanciaKm(vuelo.latitude, vuelo.longitude, latDest, lonDest);

        // 1. Desvío longitudinal máximo estricto (evita aceptar vuelos de aeropuertos cercanos como SVQ por GRX)
        double desvioKm = (distDesdeOrigenKm + distHastaDestinoKm) - distRutaTotalKm;
        double margenDesvio = Math.min(120.0, Math.max(45.0, distRutaTotalKm * 0.08));
        if (desvioKm > margenDesvio) {
            return false;
        }

        // 2. Distancia perpendicular (cross-track) respecto a la ortodrómica origen -> destino
        double desvioLateralKm = calcularDistanciaTransversalKm(
                latOrig, lonOrig, latDest, lonDest, vuelo.latitude, vuelo.longitude
        );
        if (desvioLateralKm > 110.0) {
            return false;
        }

        // 3. Validación estricta del rumbo en aire hacia el destino
        if (!vuelo.enTierra && vuelo.trueTrack != null && distHastaDestinoKm > 40.0) {
            double rumboHaciaDestino = calcularRumboGrados(vuelo.latitude, vuelo.longitude, latDest, lonDest);
            double diferenciaAngular = Math.abs(vuelo.trueTrack - rumboHaciaDestino);
            if (diferenciaAngular > 180.0) {
                diferenciaAngular = 360.0 - diferenciaAngular;
            }
            // Margen más amplio solo en maniobras iniciales de despegue (< 70 km del origen)
            double limiteAngular = (distDesdeOrigenKm < 70.0) ? 75.0 : 50.0;
            if (diferenciaAngular > limiteAngular) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calcula la distancia perpendicular mínima (cross-track en km) desde la posición del avión
     * hasta el arco de círculo máximo que une el aeropuerto de origen y el de destino.
     */
    private double calcularDistanciaTransversalKm(double lat1, double lon1,
                                                  double lat2, double lon2,
                                                  double latP, double lonP) {
        // Convierte distancia angular y rumbos iniciales a radianes para trigonometría esférica
        double dist13Rad = (calcularDistanciaKm(lat1, lon1, latP, lonP) * 1000.0) / RADIO_TIERRA;
        double rumbo13Rad = Math.toRadians(calcularRumboGrados(lat1, lon1, latP, lonP));
        double rumbo12Rad = Math.toRadians(calcularRumboGrados(lat1, lon1, lat2, lon2));

        // Aplica la fórmula de distancia cross-track sobre esfera y la devuelve en kilómetros
        double dXtRad = Math.asin(Math.sin(dist13Rad) * Math.sin(rumbo13Rad - rumbo12Rad));
        return Math.abs(dXtRad * RADIO_TIERRA) / 1000.0;
    }

    /**
     * Calcula la distancia en kilómetros entre dos coordenadas geográficas.
     */
    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        // Utiliza Location.distanceBetween y convierte el resultado de metros a kilómetros
        float[] res = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, res);
        return res[0] / 1000.0;
    }

    /**
     * Calcula el rumbo inicial (azimut en grados 0-360) desde un punto A hacia un punto B.
     */
    private double calcularRumboGrados(double lat1, double lon1, double lat2, double lon2) {
        // Convierte latitudes y diferencia de longitudes a radianes
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLambda = Math.toRadians(lon2 - lon1);

        // Calcula el ángulo azimutal inicial mediante atan2 y lo normaliza al rango [0, 360)
        double y = Math.sin(dLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);
        double theta = Math.atan2(y, x);
        return (Math.toDegrees(theta) + 360.0) % 360.0;
    }

    /**
     * Actualiza los textos del panel inferior con el modelo, aerolínea, tipo ICAO,
     * etiqueta de Easter Egg si corresponde y telemetría en vivo.
     */
    private void pintarPanel() {
        // Recupera el vuelo y posible avión capturado seleccionado
        Vuelo api = vueloSeleccionado;
        Avion a = avistadoSeleccionado;
        if (api == null) return;

        // Comprueba si se trata de un Easter Egg terrestre
        boolean esEasterEgg = AvionGenerator.esEasterEggTerrestre(
                api.tipo != null ? api.tipo : tipoSeleccionado,
                api.callsign,
                api.matricula,
                api.descripcion
        );

        // Determina el tipo ICAO efectivo combinando radar, ADSBDB y fallback de Easter Egg
        String tipoEfectivo = (api.tipo != null && !api.tipo.trim().isEmpty()) ? api.tipo : tipoSeleccionado;
        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(api.icao24);
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && aeronave != null && aeronave.icaoType != null) {
            tipoEfectivo = aeronave.icaoType.trim().toUpperCase(Locale.ROOT);
        }
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && esEasterEgg) {
            tipoEfectivo = (api.callsign != null && !api.callsign.trim().isEmpty())
                    ? api.callsign.trim().toUpperCase(Locale.ROOT)
                    : "OPS";
        }

        // Busca ruta en caché y ficha base en el catálogo local
        String callsignKey = api.callsign != null ? api.callsign.trim().toUpperCase(Locale.ROOT) : null;
        AdsbdbResponse.RutaVuelo ruta = callsignKey != null ? cacheRutas.get(callsignKey) : null;
        Avion modeloCatalogo = a == null ? AvionGenerator.getAvionPorTipoIcao(tipoEfectivo) : null;

        // Resuelve el nombre del modelo siguiendo la jerarquía de prioridad
        String modelo = esEasterEgg ? getString(R.string.easter_egg_vehiculo_modelo) : modeloCompleto(aeronave);
        if (modelo == null) {
            if (a != null) {
                modelo = a.getModelo();
            } else if (modeloCatalogo != null) {
                modelo = modeloCatalogo.getModelo();
            } else if (api.descripcion != null && !api.descripcion.isEmpty()
                    && !AvionGenerator.esModeloDesconocido(this, api.descripcion)) {
                modelo = api.descripcion;
            } else {
                String idVuelo = (api.callsign != null && !api.callsign.isEmpty()) ? api.callsign : api.icao24;
                modelo = getString(R.string.panel_vuelo_generico, idVuelo);
            }
        }

        // Resuelve la aerolínea, propietario o fabricante a mostrar
        String empresa = esEasterEgg ? getString(R.string.easter_egg_vehiculo_fabricante) : null;
        if (empresa == null) {
            if (ruta != null && ruta.airline != null && ruta.airline.name != null && !ruta.airline.name.isEmpty()) {
                empresa = ruta.airline.name;
            } else if (aeronave != null && aeronave.registeredOwner != null && !aeronave.registeredOwner.isEmpty()) {
                empresa = formatearNombre(aeronave.registeredOwner);
            } else if (a != null) {
                empresa = a.getFabricante();
            } else if (modeloCatalogo != null) {
                empresa = modeloCatalogo.getFabricante();
            }
        }

        // Combina modelo y empresa en el encabezado del panel
        String nuevoModelo = (empresa != null && !empresa.isEmpty()) ? modelo + " | " + empresa : modelo;

        // Calcula velocidad en km/h, rareza y estado de altitud
        int vel = api.velocity != null ? (int) (api.velocity * 3.6) : 0;
        String rareza = esEasterEgg
                ? "EASTER EGG"
                : (a != null ? a.getRareza() : (modeloCatalogo != null ? modeloCatalogo.getRareza() : null));
        String altitud = api.enTierra
                ? getString(R.string.altitud_en_tierra)
                : (api.altitude != null ? Math.round(api.altitude) + " m" : "—");

        // Formatea el indicador de distancia respecto al radio de 40 km
        Float distancia = distanciaAlAvionKm();
        String lineaDistancia;
        if (distancia == null) {
            lineaDistancia = "";
        } else if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            lineaDistancia = getString(R.string.panel_distancia_lejos, Math.round(distancia));
        } else {
            lineaDistancia = getString(R.string.panel_distancia_ok, Math.round(distancia));
        }

        // Formatea la línea de ruta origen -> destino si está disponible
        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null)
                ? getString(R.string.panel_ruta_formato, origen + " → " + destino)
                : "";

        // Compone el bloque completo de telemetría del panel
        String nuevosDatos = lineaRuta +
                "ICAO: " + api.icao24 + "\n" +
                getString(R.string.telemetria_matricula, api.matricula != null ? api.matricula : "—") +
                getString(R.string.panel_tipo_formato, (tipoEfectivo != null && !tipoEfectivo.isEmpty()) ? tipoEfectivo : "—") +
                (rareza != null ? getString(R.string.panel_rareza_formato, rareza) : "") +
                lineaDistancia +
                getString(R.string.panel_velocidad_formato, vel) +
                getString(R.string.panel_altitud_formato, altitud);

        // Actualiza los TextView solo si el contenido ha cambiado para evitar parpadeos
        if (!nuevoModelo.equals(txtModelo.getText().toString())) {
            txtModelo.setText(nuevoModelo);
        }
        if (!nuevosDatos.equals(txtDatos.getText().toString())) {
            txtDatos.setText(nuevosDatos);
        }
    }

    /**
     * Combina fabricante y modelo de la respuesta ADSBDB evitando duplicaciones de marca.
     */
    private String modeloCompleto(AdsbdbResponse.Aeronave aeronave) {
        // Verifica que existan datos de tipo en la respuesta de ADSBDB
        if (aeronave == null || aeronave.type == null || aeronave.type.trim().isEmpty()) return null;
        // Normaliza códigos Airbus (ej. "A320 214" -> "A320-214")
        String tipo = aeronave.type.trim().replaceFirst("^(A\\d{3})\\s+(\\d{3}[A-Z]?)$", "$1-$2");
        String fabricante = formatearNombre(aeronave.manufacturer);
        // Si el fabricante ya está incluido dentro del nombre del modelo, evita repetirlo
        if (fabricante == null || fabricante.isEmpty() || tipo.toLowerCase().contains(fabricante.toLowerCase())) {
            return tipo;
        }
        return fabricante + " " + tipo;
    }

    /**
     * Convierte cadenas completamente en mayúsculas a formato título legible.
     */
    private String formatearNombre(String texto) {
        // Si es nulo, vacío o ya tiene minúsculas, lo devuelve tal cual
        if (texto == null) return null;
        texto = texto.trim();
        if (texto.isEmpty() || !texto.equals(texto.toUpperCase())) return texto;

        // Capitaliza palabra por palabra respetando siglas cortas (<= 3 letras)
        StringBuilder sb = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            if (palabra.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (palabra.length() <= 3) {
                sb.append(palabra);
            } else {
                sb.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Formatea la ciudad y el código IATA de un aeropuerto de origen o destino.
     */
    private String describirAeropuerto(AdsbdbResponse.Aeropuerto ap) {
        // Si el aeropuerto es nulo, devuelve null
        if (ap == null) return null;
        // Prioriza el municipio sobre el nombre largo del aeropuerto
        String ciudad = (ap.municipality != null && !ap.municipality.isEmpty()) ? ap.municipality : ap.name;
        if (ciudad == null) return ap.iataCode;
        // Añade el código IATA entre paréntesis si está disponible
        return (ap.iataCode != null && !ap.iataCode.isEmpty()) ? ciudad + " (" + ap.iataCode + ")" : ciudad;
    }

    /**
     * Devuelve el icono de marcador estándar para vuelos aún no avistados.
     */
    private BitmapDescriptor iconoGenerico() {
        // Carga el icono base común desde la caché de iconos
        return iconoDesdeDrawable(R.drawable.avioncomun);
    }

    /**
     * Devuelve el icono coloreado según la rareza para vuelos ya capturados por el usuario.
     */
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        // Selecciona el recurso gráfico correspondiente al nivel de rareza
        int res;
        switch (rareza != null ? rareza.toUpperCase() : "") {
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY":
            case "MYTHIC":
            case "EASTER_EGG": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun;
        }
        return iconoDesdeDrawable(res);
    }

    /**
     * Convierte un recurso drawable en BitmapDescriptor y lo almacena en caché en memoria.
     */
    private BitmapDescriptor iconoDesdeDrawable(int res) {
        // Devuelve el descriptor cacheado si ya se generó anteriormente
        BitmapDescriptor cacheado = cacheIconos.get(res);
        if (cacheado != null) return cacheado;

        // Renderiza el Drawable vectorial en un Bitmap cuadrado de 64x64 píxeles
        Drawable d = ContextCompat.getDrawable(this, res);
        int size = 64;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (d != null) {
            d.setBounds(0, 0, size, size);
            d.draw(c);
        }

        // Convierte el Bitmap a BitmapDescriptor, lo guarda en caché y lo devuelve
        BitmapDescriptor icono = BitmapDescriptorFactory.fromBitmap(b);
        cacheIconos.put(res, icono);
        return icono;
    }

    /**
     * Corrige marcas de tiempo corruptas en avistamientos históricos guardados localmente.
     */
    private List<Avion> sanearFechasAvistamientos(List<Avion> listaOriginal) {
        // Define el límite inferior válido (1 de enero de 2020) y la hora actual
        List<Avion> listaLimpia = new ArrayList<>();
        long timestampMinimoValido = 1577836800000L;
        long timestampActual = System.currentTimeMillis();

        // Sustituye cualquier timestamp fuera de rango por la fecha actual
        for (Avion avion : listaOriginal) {
            long fechaAvion = avion.getFechaAvistamiento();

            if (fechaAvion < timestampMinimoValido || fechaAvion > timestampActual) {
                avion.setFechaAvistamiento(timestampActual);
            }
            listaLimpia.add(avion);
        }
        return listaLimpia;
    }

    /**
     * Filtra transpondedores fijos de torre (TWR), balizas de calibración y blancos fantasma
     * que Flightradar24 oculta, conservando aeronaves reales y vehículos terrestres válidos.
     */
    private boolean esBlancoRadarValido(Vuelo api) {
        // Comprueba que el blanco tenga código ICAO válido y coordenadas geográficas
        if (!esIcaoValido(api.icao24) || api.latitude == null || api.longitude == null) {
            return false;
        }

        // Normaliza tipo, callsign y matrícula para aplicar filtros de exclusión
        String tipo = api.tipo != null ? api.tipo.trim().toUpperCase(Locale.ROOT) : "";
        String callsign = api.callsign != null ? api.callsign.trim().toUpperCase(Locale.ROOT) : "";
        String matricula = api.matricula != null ? api.matricula.trim().toUpperCase(Locale.ROOT) : "";

        // Descartar transpondedores fijos de torre de control y balizas de prueba (ej. TWR, GND, RADAR)
        if ("TWR".equals(tipo) || "TWR".equals(matricula) || callsign.startsWith("TWR")
                || "RADAR".equals(tipo) || "TEST".equals(callsign)) {
            return false;
        }

        // Descartar objetos estáticos en tierra sin indicativo ni tipo que no sean vehículos Follow-Me/Ops
        boolean esVehiculoValido = AvionGenerator.esEasterEggTerrestre(tipo, callsign, matricula, api.descripcion);
        if (api.enTierra && !esVehiculoValido && tipo.isEmpty() && callsign.isEmpty()) {
            return false;
        }

        // Confirma que el blanco es válido para representarse en el radar
        return true;
    }
}