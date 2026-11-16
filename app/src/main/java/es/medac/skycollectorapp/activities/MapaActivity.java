// Declaración del paquete al que pertenece esta actividad
package es.medac.skycollectorapp.activities;

// Importaciones de permisos y contexto del sistema Android
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

// Importaciones de librerías de soporte AndroidX
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Importaciones del SDK de Google Maps para Android
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

// Importaciones de Firebase y Gson
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

// Importaciones de utilidades estándar de Java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Importaciones del proyecto
import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.AdsbResponse;
import es.medac.skycollectorapp.models.AdsbdbResponse;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.models.Vuelo;
import es.medac.skycollectorapp.network.AdsbService;
import es.medac.skycollectorapp.network.AdsbdbService;
import es.medac.skycollectorapp.utils.AvionGenerator;

// Importaciones de Retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Actividad que gestiona el radar aéreo en tiempo real sobre Google Maps,
 * mostrando vuelos activos, rutas validadas, trayectorias, el radio visual de 40 km
 * y carga inmediata al desplazar la cámara del mapa.
 */
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private enum ModoMapa { TODOS, AVISTADOS }
    private ModoMapa modoActual = ModoMapa.TODOS;

    private static final String URL_API_VUELOS = "https://api.adsb.lol/";
    private static final int RADIO_MAXIMO_NM = 250;
    public static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 40f;
    private static final String URL_API_DETALLES = "https://api.adsbdb.com/";

    // Tiempos optimizados para carga rápida al mover el mapa y refresco ágil
    private static final long INTERVALO_ACTUALIZACION_MS = 8000;
    private static final long DEBOUNCE_MOVIMIENTO_CAMARA_MS = 450;
    private static final long COOLDOWN_DESCARGA_MOVIMIENTO_MS = 1200;
    private static final long INTERVALO_ANIMACION_MS = 300;
    private static final float ZOOM_MINIMO_ANIMACION = 7f;
    private static final double RADIO_TIERRA = 6371000.0;
    private static final long INTERVALO_PUNTO_TRAYECTORIA_MS = 5000;

    private GoogleMap mMap;
    private AdsbService service;
    private AdsbdbService serviceDetalles;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean activa = false;
    private boolean descargando = false;
    private boolean descargaPendienteTrasMovimiento = false;
    private boolean avisoLimiteMostrado = false;
    private long ultimaDescargaManual = 0;
    private long ultimaDescargaPorMovimiento = 0;

    private final Runnable tareaActualizar = new Runnable() {
        @Override
        public void run() {
            descargar(false);
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS);
        }
    };

    private final Runnable tareaDescargaPorCamara = () -> {
        if (!activa || mMap == null) return;
        long ahora = SystemClock.elapsedRealtime();
        if (ahora - ultimaDescargaPorMovimiento >= COOLDOWN_DESCARGA_MOVIMIENTO_MS) {
            ultimaDescargaPorMovimiento = ahora;
            descargar(false);
        }
    };

    private final Runnable tareaAnimar = new Runnable() {
        @Override
        public void run() {
            animarMarcadores();
            handler.postDelayed(this, INTERVALO_ANIMACION_MS);
        }
    };

    private CardView panel;
    private TextView txtModelo, txtDatos;
    private Button btnAvistar, btnTodos, btnAvistados;
    private ImageButton btnVolverMapa;

    private final List<Avion> miColeccion = new ArrayList<>();
    private final Map<String, Avion> coleccionPorIcao = new HashMap<>();
    private final Map<String, Marker> marcadores = new HashMap<>();
    private final Map<Integer, BitmapDescriptor> cacheIconos = new HashMap<>();
    private final Map<String, EstadoVuelo> estados = new HashMap<>();
    private final List<LatLng> puntosTrayectoria = new ArrayList<>();

    private Circle circuloRadioAvistamiento;
    private Polyline lineaHistorial;
    private Polyline lineaActual;
    private long ultimoPuntoTrayectoria = 0;

    private String icaoSeleccionado = null;
    private String tipoSeleccionado = null;
    private String matriculaSeleccionada = null;
    private Location ultimaUbicacion = null;
    private Vuelo vueloSeleccionado = null;
    private Avion avistadoSeleccionado = null;

    private final Map<String, AdsbdbResponse.Aeronave> cacheAeronaves = new HashMap<>();
    private final Map<String, AdsbdbResponse.RutaVuelo> cacheRutas = new HashMap<>();
    private final Set<String> aeronavesNoDisponibles = new HashSet<>();
    private final Set<String> rutasNoDisponibles = new HashSet<>();

    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean concedido =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (concedido) {
                    activarUbicacion();
                } else {
                    Toast.makeText(this, getString(R.string.toast_sin_permiso_ubicacion), Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Almacena la última posición conocida, velocidad y rumbo de un vuelo para interpolar su movimiento.
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

        LatLng posicionEn(long ahora) {
            double segundos = Math.min((ahora - tiempoBase) / 1000.0, 60);
            double distancia = velocidad * segundos;
            double rumboRad = Math.toRadians(rumbo);

            double dLat = (distancia * Math.cos(rumboRad)) / RADIO_TIERRA;
            double dLon = (distancia * Math.sin(rumboRad)) / (RADIO_TIERRA * Math.cos(Math.toRadians(lat)));

            return new LatLng(lat + Math.toDegrees(dLat), lon + Math.toDegrees(dLon));
        }
    }

    /**
     * Inicializa la interfaz del mapa, los clientes REST (ADSB y ADSBDB) y los controles de filtrado.
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
     * Reanuda las actualizaciones periódicas del radar y actualiza el marcador si el avión fue recién avistado.
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
        handler.postDelayed(tareaActualizar, 300);
        handler.removeCallbacks(tareaAnimar);
        handler.postDelayed(tareaAnimar, INTERVALO_ANIMACION_MS);
    }

    /**
     * Detiene las tareas periódicas de descarga, movimiento y animación al pausar la actividad.
     */
    @Override
    protected void onPause() {
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
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

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

        mMap.getUiSettings().setZoomControlsEnabled(true);
        pedirOActivarUbicacion();

        if (activa) {
            handler.removeCallbacks(tareaActualizar);
            handler.postDelayed(tareaActualizar, 300);
        }
    }

    /**
     * Comprueba permisos de localización y lanza la solicitud al usuario si aún no están concedidos.
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
     * Activa la capa de ubicación del usuario en el mapa, dibuja el círculo de 40 km
     * y suscribe listeners de actualización GPS/Red.
     */
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void activarUbicacion() {
        if (mMap == null) return;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMyLocationChangeListener(location -> {
            ultimaUbicacion = location;
            actualizarCirculoAvistamiento(location);
        });

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            ultimaUbicacion = gps != null ? gps : net;
            if (ultimaUbicacion != null) {
                actualizarCirculoAvistamiento(ultimaUbicacion);
            }

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

        centrarEnUbicacionActual();
    }

    /**
     * Dibuja o actualiza sobre el mapa el perímetro visual de 40 km dentro del cual se pueden avistar aeronaves.
     */
    private void actualizarCirculoAvistamiento(Location location) {
        if (mMap == null || location == null) return;
        LatLng centro = new LatLng(location.getLatitude(), location.getLongitude());
        double radioMetros = DISTANCIA_MAXIMA_AVISTAMIENTO_KM * 1000.0;

        if (circuloRadioAvistamiento == null) {
            circuloRadioAvistamiento = mMap.addCircle(new CircleOptions()
                    .center(centro)
                    .radius(radioMetros)
                    .strokeColor(Color.parseColor("#661E88E5"))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor("#141E88E5"))
                    .clickable(false));
        } else {
            circuloRadioAvistamiento.setCenter(centro);
            circuloRadioAvistamiento.setRadius(radioMetros);
        }
    }

    /**
     * Centra la cámara del mapa en la última ubicación conocida del dispositivo.
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
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 10f));
            actualizarCirculoAvistamiento(ultimaUbicacion);
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
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miPosicion, 10f));
            actualizarCirculoAvistamiento(ubicacion);
        } else {
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
     * Valida la distancia al avión seleccionado, detecta si es un Easter Egg terrestre
     * y lanza la actividad para registrarlo en el hangar.
     */
    private void avistarSeleccionado() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) {
            Toast.makeText(this, getString(R.string.toast_selecciona_avion_primero), Toast.LENGTH_SHORT).show();
            return;
        }

        Float distancia = distanciaAlAvionKm();
        if (distancia == null) {
            Toast.makeText(this, getString(R.string.toast_no_comprobar_ubicacion), Toast.LENGTH_LONG).show();
            return;
        }

        if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(
                    this,
                    getString(R.string.toast_acercarse_rango, Math.round(distancia), Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM)),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Marker m = marcadores.get(icaoSeleccionado);
        LatLng posAvion = m != null ? m.getPosition() : new LatLng(vueloSeleccionado.latitude, vueloSeleccionado.longitude);

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
            nombre = getString(R.string.easter_egg_vehiculo_modelo);
            urlFotoGuardar = AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO;
            if (tipoGuardar == null || tipoGuardar.trim().isEmpty()) {
                tipoGuardar = (vueloSeleccionado.callsign != null && !vueloSeleccionado.callsign.trim().isEmpty())
                        ? vueloSeleccionado.callsign.trim().toUpperCase(Locale.ROOT)
                        : "OPS";
            }
        } else {
            nombre = modeloCompleto(cacheAeronaves.get(icaoSeleccionado));
            if (nombre == null && !AvionGenerator.esModeloDesconocido(this, vueloSeleccionado.descripcion)) {
                nombre = vueloSeleccionado.descripcion;
            }
        }

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

        Intent intent = new Intent(MapaActivity.this, AddAvionActivity.class);
        startActivity(intent);
    }

    /**
     * Abre la pantalla de detalle completo del vuelo pasando telemetría, callsign,
     * indicadores de Easter Egg y ruta validada.
     */
    private void abrirDetalleVueloCompleto() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) return;

        boolean esEasterEgg = AvionGenerator.esEasterEggTerrestre(
                tipoSeleccionado,
                vueloSeleccionado.callsign,
                matriculaSeleccionada,
                vueloSeleccionado.descripcion
        );

        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(icaoSeleccionado);
        String callsignKey = vueloSeleccionado.callsign != null
                ? vueloSeleccionado.callsign.trim().toUpperCase(Locale.ROOT)
                : null;
        AdsbdbResponse.RutaVuelo ruta = callsignKey != null ? cacheRutas.get(callsignKey) : null;

        String tipoEfectivo = tipoSeleccionado;
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && aeronave != null && aeronave.icaoType != null) {
            tipoEfectivo = aeronave.icaoType.trim().toUpperCase(Locale.ROOT);
        }
        if ((tipoEfectivo == null || tipoEfectivo.trim().isEmpty()) && esEasterEgg) {
            tipoEfectivo = (callsignKey != null && !callsignKey.isEmpty()) ? callsignKey : "OPS";
        }

        String modeloOficial = esEasterEgg ? getString(R.string.easter_egg_vehiculo_modelo) : modeloCompleto(aeronave);
        if (modeloOficial == null && vueloSeleccionado.descripcion != null
                && !AvionGenerator.esModeloDesconocido(this, vueloSeleccionado.descripcion)) {
            modeloOficial = vueloSeleccionado.descripcion;
        }

        String apodoUsuario = null;
        if (avistadoSeleccionado != null && avistadoSeleccionado.getModelo() != null
                && !avistadoSeleccionado.getModelo().trim().isEmpty()) {
            apodoUsuario = avistadoSeleccionado.getModelo().trim();
        }

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

        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null) ? origen + " → " + destino : null;

        int velKmh = vueloSeleccionado.velocity != null ? (int) (vueloSeleccionado.velocity * 3.6) : 0;
        String altitud = vueloSeleccionado.enTierra
                ? getString(R.string.altitud_en_tierra)
                : (vueloSeleccionado.altitude != null ? Math.round(vueloSeleccionado.altitude) + " m" : "—");

        Float distancia = distanciaAlAvionKm();
        Marker m = marcadores.get(icaoSeleccionado);
        double lat = m != null ? m.getPosition().latitude : vueloSeleccionado.latitude;
        double lon = m != null ? m.getPosition().longitude : vueloSeleccionado.longitude;

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
        for (Marker m : marcadores.values()) m.remove();
        marcadores.clear();
        estados.clear();
        borrarTrayectoria();
        panel.setVisibility(View.GONE);
        ajustarPaddingMapa(false);
        descargar(true);
    }

    /**
     * Ajusta el margen inferior del mapa cuando el panel de información de vuelo está desplegado.
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

    /**
     * Devuelve la clave de almacenamiento local en SharedPreferences asociada al usuario actual.
     */
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Carga en memoria los aviones avistados por el usuario indexados por código ICAO24.
     */
    private void cargarColeccion() {
        miColeccion.clear();
        coleccionPorIcao.clear();

        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString(getClaveListaUsuario(), null);
        if (json == null) return;

        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
        if (lista != null) {
            miColeccion.addAll(sanearFechasAvistamientos(lista));
        }

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
        if (icao == null) return null;
        return coleccionPorIcao.get(icao.toLowerCase());
    }

    /**
     * Descarga los vuelos activos dentro de la región visible actual del mapa.
     * Si ya hay una petición en curso, encola una actualización inmediata para la nueva zona.
     */
    private void descargar(boolean avisar) {
        if (mMap == null) return;
        if (descargando) {
            descargaPendienteTrasMovimiento = true;
            return;
        }

        if (avisar) {
            long ahora = SystemClock.elapsedRealtime();
            if (ahora - ultimaDescargaManual < 3000) {
                Toast.makeText(this, getString(R.string.toast_espera_refrescar), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.toast_acerca_mapa), Toast.LENGTH_SHORT).show();
            }
            return;
        }

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

                if (response.code() == 429) {
                    if (!avisoLimiteMostrado) {
                        avisoLimiteMostrado = true;
                        Toast.makeText(MapaActivity.this, getString(R.string.toast_limite_api), Toast.LENGTH_LONG).show();
                    }
                    handler.removeCallbacks(tareaActualizar);
                    handler.postDelayed(tareaActualizar, 45000);
                    return;
                }

                if (response.body() != null && response.body().getAviones() != null) {
                    List<Vuelo> vuelos = new ArrayList<>();
                    for (AdsbResponse.AdsbAircraft a : response.body().getAviones()) {
                        vuelos.add(a.toVuelo());
                    }
                    procesar(vuelos);
                }

                if (descargaPendienteTrasMovimiento && activa) {
                    descargaPendienteTrasMovimiento = false;
                    handler.postDelayed(tareaDescargaPorCamara, 250);
                }
            }

            @Override
            public void onFailure(Call<AdsbResponse> call, Throwable t) {
                descargando = false;
            }
        });
    }

    /**
     * Comprueba que el código ICAO24 sea un identificador hexadecimal oficial y no un blanco temporal TIS-B ('~').
     */
    private boolean esIcaoValido(String icao24) {
        if (icao24 == null) return false;
        String limpio = icao24.trim();
        return !limpio.isEmpty() && !limpio.startsWith("~");
    }

    /**
     * Actualiza posiciones, iconos y estados de los marcadores de vuelos recibidos desde la API.
     */
    private void procesar(List<Vuelo> vuelos) {
        Set<String> visibles = new HashSet<>();
        long ahora = SystemClock.elapsedRealtime();

        for (Vuelo api : vuelos) {
            if (!esBlancoRadarValido(api)) continue;

            Avion avistado = buscarPorIcao(api.icao24);
            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue;

            visibles.add(api.icao24);
            LatLng pos = new LatLng(api.latitude, api.longitude);

            if ((api.tipo == null || api.tipo.trim().isEmpty()) && api.icao24.equals(icaoSeleccionado) && tipoSeleccionado != null) {
                api.tipo = tipoSeleccionado;
            }

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
     * Interpola suavemente las coordenadas de los marcadores visibles entre actualizaciones de red.
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

    /**
     * Inicia la polilínea de trayectoria para la aeronave seleccionada.
     */
    private void mostrarTrayectoria(String icao) {
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
        if (lineaHistorial == null) lineaHistorial = mMap.addPolyline(estiloLinea());
        lineaHistorial.setPoints(new ArrayList<>(puntosTrayectoria));

        if (lineaActual == null) lineaActual = mMap.addPolyline(estiloLinea());

        Marker m = icaoSeleccionado != null ? marcadores.get(icaoSeleccionado) : null;
        if (m != null) actualizarTramoActual(m.getPosition());
    }

    /**
     * Actualiza el último segmento dinámico de la trayectoria hasta la posición interpolada actual.
     */
    private void actualizarTramoActual(LatLng posActual) {
        if (lineaActual == null || puntosTrayectoria.isEmpty()) return;
        LatLng ultimo = puntosTrayectoria.get(puntosTrayectoria.size() - 1);
        lineaActual.setPoints(Arrays.asList(ultimo, posActual));
    }

    /**
     * Elimina las líneas de trayectoria activas del mapa.
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
     * Define el color y grosor de la línea de trayectoria de vuelo.
     */
    private PolylineOptions estiloLinea() {
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
     * Lanza las consultas asíncronas utilizando el generador global y valida la ruta comercial del vuelo.
     */
    private void pedirDetalles(Vuelo api) {
        if (!esIcaoValido(api.icao24)) return;

        pedirAeronave(api.icao24);

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

        if (esCallsignComercialValido(api.callsign, api.matricula)) {
            pedirRuta(api);
        }
    }

    /**
     * Consulta la ficha de la aeronave en ADSBDB por su código ICAO24 y actualiza el tipo ICAO si faltaba.
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
        String indicativo = vuelo.callsign.trim().toUpperCase(Locale.ROOT);
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

                AdsbdbResponse.RutaVuelo ruta = body.getRespuesta().flightroute;

                if (!esRutaCoherenteConVuelo(ruta, vuelo)) {
                    rutasNoDisponibles.add(indicativo);
                    return;
                }

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
        if (callsign == null) return false;
        String cs = callsign.trim().toUpperCase(Locale.ROOT);
        if (cs.length() < 4 || cs.length() > 8) return false;

        if (matricula != null && cs.replace("-", "").equalsIgnoreCase(matricula.trim().replace("-", ""))) {
            return false;
        }

        return cs.matches("^[A-Z]{3}[0-9][A-Z0-9]{0,4}$");
    }

    /**
     * Comprueba mediante geometría esférica si la posición, el corredor lateral y el rumbo
     * actuales del avión encajan físicamente con el origen y destino devueltos por la API.
     */
    private boolean esRutaCoherenteConVuelo(AdsbdbResponse.RutaVuelo ruta, Vuelo vuelo) {
        if (ruta == null || ruta.origin == null || ruta.destination == null) return false;
        if (vuelo == null || vuelo.latitude == null || vuelo.longitude == null) return true;

        Double latOrig = ruta.origin.latitude;
        Double lonOrig = ruta.origin.longitude;
        Double latDest = ruta.destination.latitude;
        Double lonDest = ruta.destination.longitude;

        if (latOrig == null || lonOrig == null || latDest == null || lonDest == null) return true;

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
        double dist13Rad = (calcularDistanciaKm(lat1, lon1, latP, lonP) * 1000.0) / RADIO_TIERRA;
        double rumbo13Rad = Math.toRadians(calcularRumboGrados(lat1, lon1, latP, lonP));
        double rumbo12Rad = Math.toRadians(calcularRumboGrados(lat1, lon1, lat2, lon2));

        double dXtRad = Math.asin(Math.sin(dist13Rad) * Math.sin(rumbo13Rad - rumbo12Rad));
        return Math.abs(dXtRad * RADIO_TIERRA) / 1000.0;
    }

    /**
     * Calcula la distancia en kilómetros entre dos coordenadas geográficas.
     */
    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        float[] res = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, res);
        return res[0] / 1000.0;
    }

    /**
     * Calcula el rumbo inicial (azimut en grados 0-360) desde un punto A hacia un punto B.
     */
    private double calcularRumboGrados(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLambda = Math.toRadians(lon2 - lon1);

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
        Vuelo api = vueloSeleccionado;
        Avion a = avistadoSeleccionado;
        if (api == null) return;

        boolean esEasterEgg = AvionGenerator.esEasterEggTerrestre(
                api.tipo != null ? api.tipo : tipoSeleccionado,
                api.callsign,
                api.matricula,
                api.descripcion
        );

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

        String callsignKey = api.callsign != null ? api.callsign.trim().toUpperCase(Locale.ROOT) : null;
        AdsbdbResponse.RutaVuelo ruta = callsignKey != null ? cacheRutas.get(callsignKey) : null;
        Avion modeloCatalogo = a == null ? AvionGenerator.getAvionPorTipoIcao(tipoEfectivo) : null;

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

        String nuevoModelo = (empresa != null && !empresa.isEmpty()) ? modelo + " | " + empresa : modelo;

        int vel = api.velocity != null ? (int) (api.velocity * 3.6) : 0;
        String rareza = esEasterEgg
                ? "EASTER EGG"
                : (a != null ? a.getRareza() : (modeloCatalogo != null ? modeloCatalogo.getRareza() : null));
        String altitud = api.enTierra
                ? getString(R.string.altitud_en_tierra)
                : (api.altitude != null ? Math.round(api.altitude) + " m" : "—");

        Float distancia = distanciaAlAvionKm();
        String lineaDistancia;
        if (distancia == null) {
            lineaDistancia = "";
        } else if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            lineaDistancia = getString(R.string.panel_distancia_lejos, Math.round(distancia));
        } else {
            lineaDistancia = getString(R.string.panel_distancia_ok, Math.round(distancia));
        }

        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null)
                ? getString(R.string.panel_ruta_formato, origen + " → " + destino)
                : "";

        String nuevosDatos = lineaRuta +
                "ICAO: " + api.icao24 + "\n" +
                getString(R.string.telemetria_matricula, api.matricula != null ? api.matricula : "—") +
                getString(R.string.panel_tipo_formato, (tipoEfectivo != null && !tipoEfectivo.isEmpty()) ? tipoEfectivo : "—") +
                (rareza != null ? getString(R.string.panel_rareza_formato, rareza) : "") +
                lineaDistancia +
                getString(R.string.panel_velocidad_formato, vel) +
                getString(R.string.panel_altitud_formato, altitud);

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
        if (aeronave == null || aeronave.type == null || aeronave.type.trim().isEmpty()) return null;
        String tipo = aeronave.type.trim().replaceFirst("^(A\\d{3})\\s+(\\d{3}[A-Z]?)$", "$1-$2");
        String fabricante = formatearNombre(aeronave.manufacturer);
        if (fabricante == null || fabricante.isEmpty() || tipo.toLowerCase().contains(fabricante.toLowerCase())) {
            return tipo;
        }
        return fabricante + " " + tipo;
    }

    /**
     * Convierte cadenas completamente en mayúsculas a formato título legible.
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
     * Formatea la ciudad y el código IATA de un aeropuerto de origen o destino.
     */
    private String describirAeropuerto(AdsbdbResponse.Aeropuerto ap) {
        if (ap == null) return null;
        String ciudad = (ap.municipality != null && !ap.municipality.isEmpty()) ? ap.municipality : ap.name;
        if (ciudad == null) return ap.iataCode;
        return (ap.iataCode != null && !ap.iataCode.isEmpty()) ? ciudad + " (" + ap.iataCode + ")" : ciudad;
    }

    /**
     * Devuelve el icono de marcador estándar para vuelos aún no avistados.
     */
    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun);
    }

    /**
     * Devuelve el icono coloreado según la rareza para vuelos ya capturados por el usuario.
     */
    private BitmapDescriptor iconoSegunRareza(String rareza) {
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

    /**
     * Corrige marcas de tiempo corruptas en avistamientos históricos guardados localmente.
     */
    private List<Avion> sanearFechasAvistamientos(List<Avion> listaOriginal) {
        List<Avion> listaLimpia = new ArrayList<>();
        long timestampMinimoValido = 1577836800000L;
        long timestampActual = System.currentTimeMillis();

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
        if (!esIcaoValido(api.icao24) || api.latitude == null || api.longitude == null) {
            return false;
        }

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

        return true;
    }
}