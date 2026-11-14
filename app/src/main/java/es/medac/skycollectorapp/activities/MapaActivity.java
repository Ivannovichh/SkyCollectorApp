// Declaración del paquete al que pertenece esta actividad
package es.medac.skycollectorapp.activities;

// Importaciones de permisos y contexto del sistema Android
import android.Manifest; // Permisos del sistema para ubicación GPS
import android.annotation.SuppressLint; // Silencia avisos de permisos ya verificados manualmente
import android.content.Context; // Contexto global de la aplicación
import android.content.Intent; // Navegación entre pantallas (Mapa -> Añadir Avión)
import android.content.pm.PackageManager; // Comprobación de permisos concedidos
import android.graphics.Bitmap; // Imagen en memoria para construir iconos
import android.graphics.Canvas; // Lienzo para dibujar recursos sobre un Bitmap
import android.graphics.Color; // Gestión de colores hexadecimales
import android.graphics.drawable.Drawable; // Recursos gráficos vectoriales o PNG
import android.location.Location; // Coordenadas y datos de ubicación GPS
import android.location.LocationListener; // Escuchador de cambios de posición
import android.location.LocationManager; // Gestor de servicios de localización del sistema
import android.os.Bundle; // Contenedor de datos del ciclo de vida
import android.os.Handler; // Ejecución de tareas programadas en el hilo principal
import android.os.Looper; // Bucle de mensajes del hilo principal de interfaz
import android.os.SystemClock; // Reloj del sistema para medir intervalos de tiempo
import android.util.Log; // Registros de depuración en Logcat
import android.view.View; // Clase base de los componentes visuales
import android.widget.Button; // Botón de interacción
import android.widget.ImageButton; // Botón con icono para volver atrás
import android.widget.TextView; // Etiqueta de texto
import android.widget.Toast; // Mensajes emergentes breves

// Importaciones de librerías de soporte AndroidX
import androidx.activity.result.ActivityResultLauncher; // Lanzador para resultados de permisos
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar de solicitud de permisos
import androidx.annotation.NonNull; // Indica parámetros que no admiten valores nulos
import androidx.appcompat.app.AppCompatActivity; // Actividad base con soporte moderno
import androidx.cardview.widget.CardView; // Tarjeta con bordes redondeados y sombra
import androidx.core.app.ActivityCompat; // Utilidad de compatibilidad para permisos
import androidx.core.content.ContextCompat; // Acceso compatible a recursos y permisos

// Importaciones del SDK de Google Maps para Android
import com.google.android.gms.maps.CameraUpdateFactory; // Control de movimiento y zoom de cámara
import com.google.android.gms.maps.GoogleMap; // Objeto principal del mapa interactivo
import com.google.android.gms.maps.OnMapReadyCallback; // Notificación cuando el mapa termina de cargar
import com.google.android.gms.maps.SupportMapFragment; // Fragmento contenedor del mapa
import com.google.android.gms.maps.model.BitmapDescriptor; // Icono gráfico para marcadores
import com.google.android.gms.maps.model.BitmapDescriptorFactory; // Generador de iconos BitmapDescriptor
import com.google.android.gms.maps.model.LatLng; // Par de coordenadas (Latitud, Longitud)
import com.google.android.gms.maps.model.LatLngBounds; // Límites rectangulares de la zona visible
import com.google.android.gms.maps.model.Marker; // Marcador visual de cada avión en el mapa
import com.google.android.gms.maps.model.MarkerOptions; // Configuración de posición, icono y rotación del marcador
import com.google.android.gms.maps.model.Polyline; // Línea poligonal para dibujar trayectorias
import com.google.android.gms.maps.model.PolylineOptions; // Estilo visual de las líneas de trayectoria

// Importaciones de Firebase y Gson
import com.google.firebase.auth.FirebaseAuth; // Gestión de sesión de Firebase
import com.google.firebase.auth.FirebaseUser; // Usuario autenticado actual
import com.google.gson.Gson; // Conversor entre objetos Java y JSON
import com.google.gson.reflect.TypeToken; // Conservación de tipos genéricos en listas JSON

// Importaciones de utilidades estándar de Java
import java.util.ArrayList; // Lista dinámica
import java.util.Arrays; // Utilidades para listas fijas
import java.util.HashMap; // Mapa clave-valor en memoria
import java.util.HashSet; // Conjunto sin elementos duplicados
import java.util.List; // Interfaz de listas
import java.util.Locale; // Configuración regional para formatear coordenadas
import java.util.Map; // Interfaz de mapas
import java.util.Set; // Interfaz de conjuntos

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos de la aplicación
import es.medac.skycollectorapp.models.AdsbResponse; // Respuesta JSON del radar en vivo
import es.medac.skycollectorapp.models.AdsbdbResponse; // Respuesta JSON de fichas técnicas y rutas
import es.medac.skycollectorapp.models.Avion; // Modelo de aeronave capturada
import es.medac.skycollectorapp.models.Vuelo; // Modelo de vuelo activo en pantalla
import es.medac.skycollectorapp.network.AdsbService; // Cliente API de tráfico aéreo
import es.medac.skycollectorapp.network.AdsbdbService; // Cliente API de detalles de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator; // Catálogo estático de modelos

// Importaciones de Retrofit
import retrofit2.Call; // Petición HTTP asíncrona
import retrofit2.Callback; // Respuesta asíncrona de red
import retrofit2.Response; // Contenedor de respuesta HTTP
import retrofit2.Retrofit; // Constructor de clientes REST
import retrofit2.converter.gson.GsonConverterFactory; // Conversor JSON automático para Retrofit

/**
 * Actividad que gestiona el radar aéreo en tiempo real sobre Google Maps.
 */
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // ------------------------------
    // ENUMS Y CONSTANTES DE CONFIGURACIÓN
    // ------------------------------
    private enum ModoMapa { TODOS, AVISTADOS } // Filtros disponibles en el mapa
    private ModoMapa modoActual = ModoMapa.TODOS; // Filtro activo por defecto

    private static final String URL_API_VUELOS = "https://api.adsb.lol/"; // API de tráfico aéreo en vivo
    private static final int RADIO_MAXIMO_NM = 250; // Radio máximo de consulta en millas náuticas
    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 30f; // Distancia máxima para permitir avistar
    private static final String URL_API_DETALLES = "https://api.adsbdb.com/"; // API de fichas técnicas y rutas

    private static final long INTERVALO_ACTUALIZACION_MS = 15000; // Refresco automático del radar cada 15s
    private static final long INTERVALO_ANIMACION_MS = 300; // Actualización de movimiento suave cada 300ms
    private static final float ZOOM_MINIMO_ANIMACION = 7f; // Zoom mínimo para animar aviones en pantalla
    private static final double RADIO_TIERRA = 6371000.0; // Radio terrestre en metros para cálculos geodésicos
    private static final long INTERVALO_PUNTO_TRAYECTORIA_MS = 5000; // Frecuencia de puntos en la ruta dibujada (5s)

    // ------------------------------
    // COMPONENTES PRINCIPALES Y SERVICIOS
    // ------------------------------
    private GoogleMap mMap; // Instancia del mapa de Google
    private AdsbService service; // Servicio de consulta de vuelos
    private AdsbdbService serviceDetalles; // Servicio de consulta de detalles técnicos
    private final Handler handler = new Handler(Looper.getMainLooper()); // Programador de tareas en hilo UI

    private boolean activa = false; // Controla si la actividad está visible
    private boolean descargando = false; // Evita solapar peticiones de red simultáneas
    private boolean avisoLimiteMostrado = false; // Evita repetir alertas de límite de API (HTTP 429)
    private long ultimaDescargaManual = 0; // Control de tiempo mínimo entre refrescos manuales

    // ------------------------------
    // TAREAS PROGRAMADAS (RUNNABLES)
    // ------------------------------

    // Descarga periódicamente el tráfico aéreo de la zona visible
    private final Runnable tareaActualizar = new Runnable() {
        @Override
        public void run() {
            descargar(false);
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS);
        }
    };

    // Recalcula periódicamente la posición estimada de los aviones para moverlos con fluidez
    private final Runnable tareaAnimar = new Runnable() {
        @Override
        public void run() {
            animarMarcadores();
            handler.postDelayed(this, INTERVALO_ANIMACION_MS);
        }
    };

    // ------------------------------
    // COMPONENTES DE INTERFAZ Y COLECCIONES
    // ------------------------------
    private CardView panel; // Tarjeta inferior con información del vuelo seleccionado
    private TextView txtModelo, txtDatos; // Textos de título y telemetría del panel
    private Button btnAvistar, btnTodos, btnAvistados; // Botones de acción y filtrado
    private ImageButton btnVolverMapa; // Botón flotante para regresar a la pantalla principal

    private final List<Avion> miColeccion = new ArrayList<>(); // Lista de aviones capturados por el usuario
    private final Map<String, Avion> coleccionPorIcao = new HashMap<>(); // Índice rápido por código ICAO
    private final Map<String, Marker> marcadores = new HashMap<>(); // Marcadores activos en el mapa por ICAO
    private final Map<Integer, BitmapDescriptor> cacheIconos = new HashMap<>(); // Caché de iconos generados
    private final Map<String, EstadoVuelo> estados = new HashMap<>(); // Telemetría base de cada vuelo para interpolación
    private final List<LatLng> puntosTrayectoria = new ArrayList<>(); // Coordenadas de la estela del avión seleccionado

    private Polyline lineaHistorial; // Línea azul del recorrido histórico
    private Polyline lineaActual; // Tramo final entre el último punto fijo y el icono en movimiento
    private long ultimoPuntoTrayectoria = 0; // Timestamp del último punto añadido a la ruta

    private String icaoSeleccionado = null; // ICAO del avión seleccionado
    private String tipoSeleccionado = null; // Código de tipo ICAO del avión seleccionado
    private String matriculaSeleccionada = null; // Matrícula del avión seleccionado
    private Location ultimaUbicacion = null; // Última posición GPS conocida del dispositivo
    private Vuelo vueloSeleccionado = null; // Datos de vuelo de la aeronave seleccionada
    private Avion avistadoSeleccionado = null; // Datos de colección si el avión ya fue avistado

    private final Map<String, AdsbdbResponse.Aeronave> cacheAeronaves = new HashMap<>(); // Caché de fichas técnicas
    private final Map<String, AdsbdbResponse.RutaVuelo> cacheRutas = new HashMap<>(); // Caché de rutas comerciales
    private final Set<String> aeronavesNoDisponibles = new HashSet<>(); // ICAOs sin ficha para evitar reintentos
    private final Set<String> rutasNoDisponibles = new HashSet<>(); // Indicativos sin ruta para evitar reintentos

    // Gestiona la respuesta del usuario al solicitar permisos de ubicación GPS
    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean concedido =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (concedido) {
                    activarUbicacion();
                } else {
                    Toast.makeText(this, "Sin permiso no se puede mostrar tu ubicación", Toast.LENGTH_SHORT).show();
                }
            });

    // ------------------------------
    // CLASE INTERNA DE ESTADO DE VUELO
    // ------------------------------

    /**
     * Guarda la última lectura real de un vuelo para proyectar su avance entre actualizaciones.
     */
    private static class EstadoVuelo {
        final double lat;
        final double lon;
        final double velocidad;
        final double rumbo;
        final long tiempoBase;

        EstadoVuelo(double lat, double lon, double velocidad, double rumbo, long tiempoBase) {
            this.lat = lat;
            this.lon = lon;
            this.velocidad = velocidad;
            this.rumbo = rumbo;
            this.tiempoBase = tiempoBase;
        }

        /**
         * Calcula la coordenada proyectada del avión en el instante indicado.
         */
        LatLng posicionEn(long ahora) {
            double segundos = Math.min((ahora - tiempoBase) / 1000.0, 60);
            double distancia = velocidad * segundos;
            double rumboRad = Math.toRadians(rumbo);

            double dLat = (distancia * Math.cos(rumboRad)) / RADIO_TIERRA;
            double dLon = (distancia * Math.sin(rumboRad)) / (RADIO_TIERRA * Math.cos(Math.toRadians(lat)));

            return new LatLng(lat + Math.toDegrees(dLat), lon + Math.toDegrees(dLon));
        }
    }

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    /**
     * Inicializa la vista, los clientes Retrofit y los eventos de los botones.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        panel = findViewById(R.id.card_info_vuelo);
        txtModelo = findViewById(R.id.txt_modelo_panel);
        txtDatos = findViewById(R.id.txt_datos_panel);
        btnAvistar = findViewById(R.id.btn_avistar);
        btnTodos = findViewById(R.id.btn_todos);
        btnAvistados = findViewById(R.id.btn_avistados);
        btnVolverMapa = findViewById(R.id.btn_volver_mapa);

        btnVolverMapa.setOnClickListener(v -> finish());

        cargarColeccion();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL_API_VUELOS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(AdsbService.class);

        Retrofit retrofitDetalles = new Retrofit.Builder()
                .baseUrl(URL_API_DETALLES)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        serviceDetalles = retrofitDetalles.create(AdsbdbService.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnTodos.setOnClickListener(v -> {
            modoActual = ModoMapa.TODOS;
            refrescarMapa();
            btnAvistar.setVisibility(View.VISIBLE);
        });

        btnAvistados.setOnClickListener(v -> {
            modoActual = ModoMapa.AVISTADOS;
            refrescarMapa();
            btnAvistar.setVisibility(View.GONE);
        });

        btnAvistar.setOnClickListener(v -> avistarSeleccionado());

        panel.setOnClickListener(v -> abrirDetalleVueloCompleto());
    }

    /**
     * Reactiva el radar, las animaciones y actualiza el estado si se acaba de capturar un avión.
     */
    @Override
    protected void onResume() {
        super.onResume();
        activa = true;
        cargarColeccion();

        if (icaoSeleccionado != null) {
            Avion recienAvistado = buscarPorIcao(icaoSeleccionado);
            if (recienAvistado != null) {
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

        handler.removeCallbacks(tareaActualizar);
        handler.postDelayed(tareaActualizar, 1000);
        handler.removeCallbacks(tareaAnimar);
        handler.postDelayed(tareaAnimar, INTERVALO_ANIMACION_MS);
    }

    /**
     * Pausa las tareas de red y animación cuando la pantalla pasa a segundo plano.
     */
    @Override
    protected void onPause() {
        super.onPause();
        activa = false;
        handler.removeCallbacks(tareaActualizar);
        handler.removeCallbacks(tareaAnimar);
    }

    // ------------------------------
    // CONFIGURACIÓN Y CALLBACKS DEL MAPA
    // ------------------------------

    /**
     * Configura los escuchadores y controles una vez que el mapa está listo.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        mMap.setOnMapClickListener(latLng -> {
            panel.setVisibility(View.GONE);
            ajustarPaddingMapa(false);
            borrarTrayectoria();
        });

        mMap.getUiSettings().setZoomControlsEnabled(true);
        pedirOActivarUbicacion();

        if (activa) {
            handler.removeCallbacks(tareaActualizar);
            handler.postDelayed(tareaActualizar, 1000);
        }
    }

    /**
     * Comprueba los permisos de ubicación o lanza la solicitud si aún no se han concedido.
     */
    private void pedirOActivarUbicacion() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            activarUbicacion();
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            permisoUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Activa el punto azul de posición, escucha cambios GPS y centra la cámara en el usuario.
     */
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void activarUbicacion() {
        if (mMap == null) return;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMyLocationChangeListener(location -> ultimaUbicacion = location);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            ultimaUbicacion = gps != null ? gps : net;

            LocationListener locListener = location -> ultimaUbicacion = location;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, locListener);
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10f, locListener);
            }
        }

        centrarEnUbicacionActual();
    }

    /**
     * Centra la cámara del mapa en la última ubicación conocida o solicita una lectura puntual.
     */
    @SuppressLint("MissingPermission")
    private void centrarEnUbicacionActual() {
        if (mMap == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            return;
        }

        if (ultimaUbicacion != null) {
            LatLng miPosicion = new LatLng(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 11f));
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        Location ubicacion = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (ubicacion == null) {
            ubicacion = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (ubicacion != null) {
            ultimaUbicacion = ubicacion;
            LatLng miPosicion = new LatLng(ubicacion.getLatitude(), ubicacion.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 11f));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4168, -3.7038), 6f));
            String proveedor = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;

            locationManager.requestSingleUpdate(proveedor, location -> {
                ultimaUbicacion = location;
                LatLng miPosicion = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 11f));
            }, null);
        }
    }

    // ------------------------------
    // LÓGICA DE AVISTAMIENTO Y DISTANCIAS
    // ------------------------------

    /**
     * Calcula la distancia en kilómetros entre el usuario y el avión seleccionado.
     */
    private Float distanciaAlAvionKm() {
        if (ultimaUbicacion == null || icaoSeleccionado == null) return null;
        Marker m = marcadores.get(icaoSeleccionado);
        if (m == null) return null;

        float[] resultado = new float[1];
        Location.distanceBetween(
                ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(),
                m.getPosition().latitude, m.getPosition().longitude, resultado);
        return resultado[0] / 1000f;
    }

    /**
     * Valida la distancia al avión, guarda sus datos temporales y abre AddAvionActivity directamente.
     */
    private void avistarSeleccionado() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) {
            Toast.makeText(this, "Selecciona un avión primero", Toast.LENGTH_SHORT).show();
            return;
        }

        Float distancia = distanciaAlAvionKm();
        if (distancia == null) {
            Toast.makeText(this, "No se puede comprobar tu ubicación. Activa la ubicación y concede el permiso para avistar aviones", Toast.LENGTH_LONG).show();
            return;
        }

        if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(this, "Estás a " + Math.round(distancia) + " km del avión. Acércate a menos de " + Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM) + " km para avistarlo", Toast.LENGTH_LONG).show();
            return;
        }

        Marker m = marcadores.get(icaoSeleccionado);
        LatLng posAvion = m != null ? m.getPosition() : new LatLng(vueloSeleccionado.latitude, vueloSeleccionado.longitude);

        String nombre = modeloCompleto(cacheAeronaves.get(icaoSeleccionado));
        if (nombre == null) nombre = vueloSeleccionado.descripcion;

        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icaoSeleccionado)
                .putString("tipo_seleccionado", tipoSeleccionado)
                .putString("matricula_seleccionada", matriculaSeleccionada)
                .putString("descripcion_seleccionada", nombre)
                .putLong("fecha_seleccionada", System.currentTimeMillis())
                .putFloat("distancia_seleccionada", distancia)
                .putFloat("lat_avion_seleccionado", (float) posAvion.latitude)
                .putFloat("lon_avion_seleccionado", (float) posAvion.longitude)
                .apply();

        Intent intent = new Intent(MapaActivity.this, AddAvionActivity.class);
        startActivity(intent);
    }

    /**
     * Abre la pantalla completa con la carta, especificaciones y foto de catálogo
     * del avión seleccionado actualmente en el radar, respetando su apodo si ya fue capturado.
     */
    private void abrirDetalleVueloCompleto() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) return;

        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(icaoSeleccionado);
        AdsbdbResponse.RutaVuelo ruta = vueloSeleccionado.callsign != null ? cacheRutas.get(vueloSeleccionado.callsign) : null;

        // Obtener siempre el nombre oficial del modelo para localizar su ilustración en el catálogo
        String modeloOficial = modeloCompleto(aeronave);
        if (modeloOficial == null && vueloSeleccionado.descripcion != null) {
            modeloOficial = vueloSeleccionado.descripcion;
        }

        // Comprobar si el usuario ya avistó este avión y le asignó un apodo personalizado
        String apodoUsuario = null;
        if (avistadoSeleccionado != null && avistadoSeleccionado.getModelo() != null
                && !avistadoSeleccionado.getModelo().trim().isEmpty()) {
            apodoUsuario = avistadoSeleccionado.getModelo().trim();
        }

        String empresa = null;
        if (ruta != null && ruta.airline != null && ruta.airline.name != null) {
            empresa = ruta.airline.name;
        } else if (aeronave != null && aeronave.registeredOwner != null) {
            empresa = formatearNombre(aeronave.registeredOwner);
        } else if (avistadoSeleccionado != null) {
            empresa = avistadoSeleccionado.getFabricante();
        }

        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null) ? origen + " → " + destino : null;

        int velKmh = vueloSeleccionado.velocity != null ? (int) (vueloSeleccionado.velocity * 3.6) : 0;
        String altitud = vueloSeleccionado.enTierra ? "En tierra"
                : (vueloSeleccionado.altitude != null ? Math.round(vueloSeleccionado.altitude) + " m" : "—");

        Float distancia = distanciaAlAvionKm();
        Marker m = marcadores.get(icaoSeleccionado);
        double lat = m != null ? m.getPosition().latitude : vueloSeleccionado.latitude;
        double lon = m != null ? m.getPosition().longitude : vueloSeleccionado.longitude;

        Intent intent = new Intent(this, DetalleVueloRadarActivity.class);
        intent.putExtra("icao", icaoSeleccionado);
        intent.putExtra("tipo", tipoSeleccionado);
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
        startActivity(intent);
    }

    /**
     * Limpia los marcadores y trayectorias actuales y fuerza una nueva descarga.
     */
    private void refrescarMapa() {
        for (Marker m : marcadores.values()) m.remove();
        marcadores.clear();
        estados.clear();
        borrarTrayectoria();
        panel.setVisibility(View.GONE);
        ajustarPaddingMapa(false);
        descargar(true);
    }

    /**
     * Ajusta el margen inferior del mapa para que el panel no tape los controles de Google.
     */
    private void ajustarPaddingMapa(boolean panelVisible) {
        if (mMap == null) return;
        if (panelVisible) {
            panel.post(() -> {
                int margen = (int) (16 * getResources().getDisplayMetrics().density);
                mMap.setPadding(0, 0, 0, panel.getHeight() + margen * 2);
            });
        } else {
            mMap.setPadding(0, 0, 0, 0);
        }
    }

    // ------------------------------
    // COLECCIÓN LOCAL Y DESCARGA API
    // ------------------------------

    /**
     * Devuelve la clave de SharedPreferences correspondiente a la lista del usuario activo.
     */
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Carga en memoria la colección de aviones capturados por el usuario actual.
     */
    private void cargarColeccion() {
        miColeccion.clear();
        coleccionPorIcao.clear();

        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString(getClaveListaUsuario(), null);
        if (json == null) return;

        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista != null) miColeccion.addAll(lista);

        for (Avion a : miColeccion) {
            if (a.getIcao24() != null) {
                coleccionPorIcao.put(a.getIcao24().toLowerCase(), a);
            }
        }
    }

    /**
     * Comprueba si un código ICAO pertenece a un avión ya avistado por el usuario.
     */
    private Avion buscarPorIcao(String icao) {
        if (icao == null) return null;
        return coleccionPorIcao.get(icao.toLowerCase());
    }

    /**
     * Descarga de la API los vuelos activos dentro de la región visible en pantalla.
     */
    private void descargar(boolean avisar) {
        if (mMap == null || descargando) return;

        if (avisar) {
            long ahora = SystemClock.elapsedRealtime();
            if (ahora - ultimaDescargaManual < 10000) {
                Toast.makeText(this, "Espera unos segundos para refrescar", Toast.LENGTH_SHORT).show();
                return;
            }
            ultimaDescargaManual = ahora;
        }

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng centro = zona.getCenter();

        float[] distancia = new float[1];
        Location.distanceBetween(centro.latitude, centro.longitude, zona.northeast.latitude, zona.northeast.longitude, distancia);
        int radioNm = (int) Math.ceil(distancia[0] / 1852.0);

        if (radioNm > RADIO_MAXIMO_NM) {
            if (avisar) {
                Toast.makeText(this, "Acerca el mapa para ver los aviones", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        descargando = true;

        service.getVuelosEnZona(
                String.format(Locale.US, "%.4f", centro.latitude),
                String.format(Locale.US, "%.4f", centro.longitude),
                radioNm
        ).enqueue(new Callback<AdsbResponse>() {
            @Override
            public void onResponse(Call<AdsbResponse> call, Response<AdsbResponse> response) {
                descargando = false;
                if (isDestroyed() || isFinishing()) return;

                if (response.code() == 429 && !avisoLimiteMostrado) {
                    avisoLimiteMostrado = true;
                    Toast.makeText(MapaActivity.this, "Límite de peticiones de la API alcanzado", Toast.LENGTH_LONG).show();
                }

                if (response.body() == null || response.body().getAviones() == null) return;

                List<Vuelo> vuelos = new ArrayList<>();
                for (AdsbResponse.AdsbAircraft a : response.body().getAviones()) {
                    vuelos.add(a.toVuelo());
                }
                procesar(vuelos);
            }

            @Override
            public void onFailure(Call<AdsbResponse> call, Throwable t) {
                descargando = false;
            }
        });
    }

    /**
     * Actualiza, crea o elimina marcadores en el mapa según los datos recibidos del radar.
     */
    private void procesar(List<Vuelo> vuelos) {
        Set<String> visibles = new HashSet<>();
        long ahora = SystemClock.elapsedRealtime();

        for (Vuelo api : vuelos) {
            if (api.icao24 == null || api.latitude == null || api.longitude == null) continue;

            Avion avistado = buscarPorIcao(api.icao24);
            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue;

            visibles.add(api.icao24);
            LatLng pos = new LatLng(api.latitude, api.longitude);

            estados.put(api.icao24, new EstadoVuelo(
                    api.latitude, api.longitude,
                    api.velocity != null ? api.velocity : 0,
                    api.trueTrack != null ? api.trueTrack : 0,
                    ahora));

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

            m.setTag(new Object[]{api, avistado});

            if (api.icao24.equals(icaoSeleccionado)) {
                vueloSeleccionado = api;
                avistadoSeleccionado = avistado;
                if (panel.getVisibility() == View.VISIBLE) pintarPanel();
            }

            if (api.icao24.equals(icaoSeleccionado) && lineaHistorial != null) {
                puntosTrayectoria.add(pos);
                dibujarTrayectoria();
            }
        }

        for (String icao : new ArrayList<>(marcadores.keySet())) {
            if (!visibles.contains(icao)) {
                Marker marcador = marcadores.remove(icao);
                if (marcador != null) marcador.remove();
                estados.remove(icao);
            }
        }
    }

    /**
     * Mueve suavemente los marcadores visibles en función de su velocidad y rumbo.
     */
    private void animarMarcadores() {
        if (mMap == null || marcadores.isEmpty()) return;

        long ahora = SystemClock.elapsedRealtime();

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

        if (mMap.getCameraPosition().zoom < ZOOM_MINIMO_ANIMACION) return;

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds;

        for (Map.Entry<String, Marker> e : marcadores.entrySet()) {
            if (e.getKey().equals(icaoSeleccionado)) continue;

            EstadoVuelo estado = estados.get(e.getKey());
            if (estado == null || estado.velocidad < 1) continue;

            LatLng pos = estado.posicionEn(ahora);
            if (!zona.contains(pos)) continue;

            e.getValue().setPosition(pos);
        }
    }

    // ------------------------------
    // LÓGICA DE DIBUJO DE TRAYECTORIAS
    // ------------------------------

    /**
     * Inicia el trazado de trayectoria para un avión seleccionado.
     */
    private void mostrarTrayectoria(String icao) {
        borrarTrayectoria();
        Marker m = marcadores.get(icao);
        if (m != null) puntosTrayectoria.add(m.getPosition());
        ultimoPuntoTrayectoria = SystemClock.elapsedRealtime();
        dibujarTrayectoria();
    }

    /**
     * Dibuja el historial de ruta y el tramo dinámico hasta la posición actual del avión.
     */
    private void dibujarTrayectoria() {
        if (lineaHistorial == null) lineaHistorial = mMap.addPolyline(estiloLinea());
        lineaHistorial.setPoints(new ArrayList<>(puntosTrayectoria));

        if (lineaActual == null) lineaActual = mMap.addPolyline(estiloLinea());

        Marker m = icaoSeleccionado != null ? marcadores.get(icaoSeleccionado) : null;
        if (m != null) actualizarTramoActual(m.getPosition());
    }

    /**
     * Conecta el último punto registrado de la estela con la posición interpolada actual.
     */
    private void actualizarTramoActual(LatLng posActual) {
        if (lineaActual == null || puntosTrayectoria.isEmpty()) return;
        LatLng ultimo = puntosTrayectoria.get(puntosTrayectoria.size() - 1);
        lineaActual.setPoints(Arrays.asList(ultimo, posActual));
    }

    /**
     * Elimina las polilíneas de trayectoria del mapa y limpia sus puntos.
     */
    private void borrarTrayectoria() {
        if (lineaHistorial != null) {
            lineaHistorial.remove();
            lineaHistorial = null;
        }
        if (lineaActual != null) {
            lineaActual.remove();
            lineaActual = null;
        }
        puntosTrayectoria.clear();
    }

    /**
     * Define el color y grosor de las líneas de trayectoria.
     */
    private PolylineOptions estiloLinea() {
        return new PolylineOptions()
                .color(Color.parseColor("#1E88E5"))
                .width(8f)
                .geodesic(false);
    }

    // ------------------------------
    // INTERACCIÓN Y PANEL DE INFORMACIÓN
    // ------------------------------

    /**
     * Muestra la tarjeta inferior con datos del vuelo al pulsar sobre su marcador.
     */
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (!(marker.getTag() instanceof Object[])) return false;
        Object[] data = (Object[]) marker.getTag();
        if (data.length < 2 || !(data[0] instanceof Vuelo)) return false;

        Vuelo api = (Vuelo) data[0];
        Avion a = data[1] instanceof Avion ? (Avion) data[1] : null;

        boolean cambioDeAvion = !api.icao24.equals(icaoSeleccionado) || lineaHistorial == null;
        icaoSeleccionado = api.icao24;
        tipoSeleccionado = api.tipo;
        matriculaSeleccionada = api.matricula;
        vueloSeleccionado = api;
        avistadoSeleccionado = a;

        if (cambioDeAvion) mostrarTrayectoria(api.icao24);

        pintarPanel();
        pedirDetalles(api);

        panel.setVisibility(View.VISIBLE);
        ajustarPaddingMapa(true);
        btnAvistar.setVisibility(a == null && modoActual == ModoMapa.TODOS ? View.VISIBLE : View.GONE);

        return true;
    }

    /**
     * Lanza las peticiones secundarias de ficha de aeronave y ruta comercial.
     */
    private void pedirDetalles(Vuelo api) {
        pedirAeronave(api.icao24);
        if (api.callsign != null && !api.callsign.isEmpty()) pedirRuta(api.callsign);
    }

    /**
     * Consulta en adsbdb el modelo exacto y propietario de la aeronave por ICAO.
     */
    private void pedirAeronave(String icao) {
        if (icao == null || cacheAeronaves.containsKey(icao) || aeronavesNoDisponibles.contains(icao)) return;

        serviceDetalles.getAeronave(icao).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                AdsbdbResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().aircraft == null) {
                    aeronavesNoDisponibles.add(icao);
                    return;
                }

                cacheAeronaves.put(icao, body.getRespuesta().aircraft);
                if (icao.equals(icaoSeleccionado)) pintarPanel();
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb aeronave fallo: " + t.getMessage());
            }
        });
    }

    /**
     * Consulta en adsbdb la aerolínea y aeropuertos de origen/destino por indicativo.
     */
    private void pedirRuta(String indicativo) {
        if (cacheRutas.containsKey(indicativo) || rutasNoDisponibles.contains(indicativo)) return;

        serviceDetalles.getRuta(indicativo).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return;

                AdsbdbResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().flightroute == null) {
                    rutasNoDisponibles.add(indicativo);
                    return;
                }

                cacheRutas.put(indicativo, body.getRespuesta().flightroute);
                if (vueloSeleccionado != null && indicativo.equals(vueloSeleccionado.callsign)) pintarPanel();
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb ruta fallo: " + t.getMessage());
            }
        });
    }

    /**
     * Formatea y actualiza los textos del panel inferior solo si han cambiado.
     */
    private void pintarPanel() {
        Vuelo api = vueloSeleccionado;
        Avion a = avistadoSeleccionado;
        if (api == null) return;

        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(api.icao24);
        AdsbdbResponse.RutaVuelo ruta = api.callsign != null ? cacheRutas.get(api.callsign) : null;
        Avion modeloCatalogo = a == null ? AvionGenerator.getAvionPorTipoIcao(api.tipo) : null;

        String modelo = modeloCompleto(aeronave);
        if (modelo == null) {
            if (a != null) {
                modelo = a.getModelo();
            } else if (modeloCatalogo != null) {
                modelo = modeloCatalogo.getModelo();
            } else if (api.descripcion != null && !api.descripcion.isEmpty()) {
                modelo = api.descripcion;
            } else {
                modelo = "Vuelo " + (api.callsign != null && !api.callsign.isEmpty() ? api.callsign : api.icao24);
            }
        }

        String empresa = null;
        if (ruta != null && ruta.airline != null && ruta.airline.name != null && !ruta.airline.name.isEmpty()) {
            empresa = ruta.airline.name;
        } else if (aeronave != null && aeronave.registeredOwner != null && !aeronave.registeredOwner.isEmpty()) {
            empresa = formatearNombre(aeronave.registeredOwner);
        } else if (a != null) {
            empresa = a.getFabricante();
        } else if (modeloCatalogo != null) {
            empresa = modeloCatalogo.getFabricante();
        }

        String nuevoModelo = (empresa != null && !empresa.isEmpty()) ? modelo + " | " + empresa : modelo;

        int vel = api.velocity != null ? (int) (api.velocity * 3.6) : 0;
        String rareza = a != null ? a.getRareza() : (modeloCatalogo != null ? modeloCatalogo.getRareza() : null);
        String altitud = api.enTierra ? "En tierra" : (api.altitude != null ? Math.round(api.altitude) + " m" : "—");

        Float distancia = distanciaAlAvionKm();
        String lineaDistancia = distancia == null ? "" : "Distancia: " + Math.round(distancia) + " km" + (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM ? " (demasiado lejos para avistarlo)" : "") + "\n";

        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null) ? "Ruta: " + origen + " → " + destino + "\n" : "";

        String nuevosDatos = lineaRuta +
                "ICAO: " + api.icao24 + "\n" +
                "Matrícula: " + (api.matricula != null ? api.matricula : "—") + "\n" +
                "Tipo: " + (api.tipo != null ? api.tipo : "—") + "\n" +
                (rareza != null ? "Rareza: " + rareza + "\n" : "") +
                lineaDistancia +
                "Velocidad: " + vel + " km/h\n" +
                "Altitud: " + altitud;

        if (!nuevoModelo.equals(txtModelo.getText().toString())) {
            txtModelo.setText(nuevoModelo);
        }
        if (!nuevosDatos.equals(txtDatos.getText().toString())) {
            txtDatos.setText(nuevosDatos);
        }
    }

    /**
     * Combina fabricante y modelo normalizado (Ej. Airbus A320-214).
     */
    private String modeloCompleto(AdsbdbResponse.Aeronave aeronave) {
        if (aeronave == null || aeronave.type == null || aeronave.type.trim().isEmpty()) return null;
        String tipo = aeronave.type.trim().replaceFirst("^(A\\d{3})\\s+(\\d{3}[A-Z]?)$", "$1-$2");
        String fabricante = formatearNombre(aeronave.manufacturer);
        if (fabricante == null || fabricante.isEmpty() || tipo.toLowerCase().contains(fabricante.toLowerCase())) {
            return tipo;
        }
        return fabricante + " " + tipo;
    }

    /**
     * Formatea cadenas escritas íntegramente en mayúsculas a estilo Título.
     */
    private String formatearNombre(String texto) {
        if (texto == null) return null;
        texto = texto.trim();
        if (texto.isEmpty() || !texto.equals(texto.toUpperCase())) return texto;

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
     * Devuelve el nombre o ciudad de un aeropuerto junto con su código IATA.
     */
    private String describirAeropuerto(AdsbdbResponse.Aeropuerto ap) {
        if (ap == null) return null;
        String ciudad = (ap.municipality != null && !ap.municipality.isEmpty()) ? ap.municipality : ap.name;
        if (ciudad == null) return ap.iataCode;
        return (ap.iataCode != null && !ap.iataCode.isEmpty()) ? ciudad + " (" + ap.iataCode + ")" : ciudad;
    }

    // ------------------------------
    // GENERACIÓN Y CACHÉ DE ICONOS
    // ------------------------------

    /**
     * Devuelve el icono estándar para aviones aún no avistados.
     */
    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun);
    }

    /**
     * Selecciona el icono de marcador correspondiente a la rareza del avión capturado.
     */
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res;
        switch (rareza != null ? rareza.toUpperCase() : "") {
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY":
            case "MYTHIC": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun;
        }
        return iconoDesdeDrawable(res);
    }

    /**
     * Convierte un Drawable a BitmapDescriptor de 64x64px y lo guarda en caché.
     */
    private BitmapDescriptor iconoDesdeDrawable(int res) {
        BitmapDescriptor cacheado = cacheIconos.get(res);
        if (cacheado != null) return cacheado;

        Drawable d = ContextCompat.getDrawable(this, res);
        int size = 64;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (d != null) {
            d.setBounds(0, 0, size, size);
            d.draw(c);
        }

        BitmapDescriptor icono = BitmapDescriptorFactory.fromBitmap(b);
        cacheIconos.put(res, icono);
        return icono;
    }
}