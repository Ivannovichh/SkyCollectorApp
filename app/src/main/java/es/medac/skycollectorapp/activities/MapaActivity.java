package es.medac.skycollectorapp.activities; // Paquete en el que se encuentra la clase

// Importaciones de Android necesarias para la actividad
import android.Manifest; // Para referenciar los permisos de Android
import android.annotation.SuppressLint; // Para silenciar el aviso de permiso comprobado manualmente
import android.content.Context; // Para acceder a SharedPreferences y otras funciones de contexto
import android.content.SharedPreferences; // Para almacenar y recuperar datos persistentes
import android.content.pm.PackageManager; // Para comprobar si un permiso está concedido
import android.location.Location; // Para calcular distancias entre coordenadas
import android.graphics.Bitmap; // Para crear imágenes en memoria
import android.graphics.Canvas; // Para dibujar sobre un Bitmap
import android.graphics.Color; // Para definir el color de la línea de trayectoria
import android.graphics.drawable.Drawable; // Para manejar imágenes drawable de recursos
import android.os.Bundle; // Para recibir parámetros al crear la actividad
import android.os.Handler; // Para programar tareas en el hilo principal
import android.os.Looper; // Para asociar Handler al hilo principal
import android.os.SystemClock; // Para medir el tiempo transcurrido entre actualizaciones
import android.util.Log; // Para escribir mensajes de diagnóstico en el Logcat
import android.view.View; // Para manejar vistas en la interfaz
import android.widget.Button; // Elemento de interfaz tipo botón
import android.widget.TextView; // Elemento de interfaz para mostrar texto
import android.widget.Toast; // Para mostrar mensajes emergentes al usuario

// Importaciones de AndroidX
import androidx.activity.result.ActivityResultLauncher; // Para lanzar la petición de permisos
import androidx.activity.result.contract.ActivityResultContracts; // Contratos predefinidos para pedir permisos
import androidx.annotation.NonNull; // Para marcar parámetros que no pueden ser nulos
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades con compatibilidad de ActionBar
import androidx.cardview.widget.CardView; // Componente tipo tarjeta para mostrar información
import androidx.core.content.ContextCompat; // Para acceder a recursos de manera segura

// Importaciones de Google Maps
import com.google.android.gms.maps.*; // Clases principales de Google Maps
import com.google.android.gms.maps.model.*; // Clases para LatLng, Marker, BitmapDescriptor, etc.

// Importaciones de Gson para manejar JSON
import com.google.gson.Gson; // Para convertir objetos a JSON y viceversa
import com.google.gson.reflect.TypeToken; // Para manejar conversiones de listas genéricas

// Importaciones de Java estándar
import java.util.*; // Para List, Map, Set, ArrayList, HashMap, HashSet, Iterator

// Importaciones del proyecto
import es.medac.skycollectorapp.R; // Recursos de layouts, drawables, strings, etc.
import es.medac.skycollectorapp.models.Avion; // Modelo de datos de Avión
import es.medac.skycollectorapp.models.AdsbResponse; // Modelo de respuesta de la API de vuelos
import es.medac.skycollectorapp.models.AdsbdbResponse; // Modelo de respuesta de la API de detalles de aeronaves
import es.medac.skycollectorapp.models.Vuelo; // Modelo de un vuelo con las unidades que usa la app
import es.medac.skycollectorapp.network.AdsbService; // Interfaz para llamadas HTTP a la API de vuelos
import es.medac.skycollectorapp.network.AdsbdbService; // Interfaz para llamadas HTTP a la API de detalles de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator; // Catálogo de aviones y tabla de tipos ICAO
import retrofit2.*; // Clases Retrofit para llamadas HTTP
import retrofit2.converter.gson.GsonConverterFactory; // Conversor de JSON para Retrofit

// Clase principal de la actividad que muestra un mapa con aviones
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener { // Implementa callbacks del mapa y clicks en marcadores

    // Enum para definir los modos del mapa
    private enum ModoMapa { TODOS, AVISTADOS } // Modo TODOS muestra todos los vuelos, AVISTADOS solo los avistados
    private ModoMapa modoActual = ModoMapa.TODOS; // Modo inicial del mapa

    // Configuración de la API de vuelos
    private static final String URL_API_VUELOS = "https://api.adsb.lol/"; // URL base de la API (con airplanes.live sería https://api.airplanes.live/)
    private static final int RADIO_MAXIMO_NM = 250; // Radio máximo que permite la API, en millas náuticas
    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 30f; // Distancia máxima entre el usuario y el avión para poder avistarlo
    private static final String URL_API_DETALLES = "https://api.adsbdb.com/"; // URL base de la API con el modelo completo, la aerolínea y la ruta

    // Tiempo entre cada actualización automática de los vuelos
    private static final long INTERVALO_ACTUALIZACION_MS = 15000; // 15 segundos entre cada descarga de vuelos

    // Configuración del movimiento suave de los aviones entre actualizaciones
    private static final long INTERVALO_ANIMACION_MS = 300; // Cada cuánto se recalcula la posición de los aviones
    private static final float ZOOM_MINIMO_ANIMACION = 7f; // Por debajo de este zoom el movimiento no se aprecia y no se anima
    private static final double RADIO_TIERRA = 6371000.0; // Radio de la Tierra en metros
    private static final long INTERVALO_PUNTO_TRAYECTORIA_MS = 5000; // Cada cuánto se añade un punto al camino del avión seleccionado

    // Objeto GoogleMap para mostrar el mapa
    private GoogleMap mMap;

    // Servicio Retrofit para obtener datos de vuelos
    private AdsbService service;

    // Servicio Retrofit para obtener el modelo completo, la aerolínea y la ruta de un vuelo
    private AdsbdbService serviceDetalles;

    // Handler para ejecutar tareas en el hilo principal
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Control de las actualizaciones automáticas
    private boolean activa = false; // Indica si la pantalla está en primer plano
    private boolean descargando = false; // Indica si hay una petición en curso para no lanzar dos a la vez
    private boolean avisoLimiteMostrado = false; // Evita repetir el aviso de límite de la API

    // Tarea que descarga los vuelos y se vuelve a programar sola
    private final Runnable tareaActualizar = new Runnable() {
        @Override
        public void run() {
            descargar(false); // Descargar vuelos sin mostrar avisos de zoom
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS); // Programar la siguiente actualización
        }
    };

    // Tarea que recalcula la posición de los aviones para que se muevan con suavidad
    private final Runnable tareaAnimar = new Runnable() {
        @Override
        public void run() {
            animarMarcadores(); // Mover los aviones a su posición estimada
            handler.postDelayed(this, INTERVALO_ANIMACION_MS); // Programar el siguiente movimiento
        }
    };

    // Elementos de la interfaz (UI)
    private CardView panel; // Panel que muestra información del avión seleccionado
    private TextView txtModelo, txtDatos; // TextViews para mostrar modelo/fabricante y otros datos
    private Button btnAvistar, btnTodos, btnAvistados; // Botones para acciones y cambio de modo

    // Datos de la aplicación
    private final List<Avion> miColeccion = new ArrayList<>(); // Lista de aviones avistados por el usuario
    private final Map<String, Avion> coleccionPorIcao = new HashMap<>(); // Mapa que relaciona ICAO (en minúsculas) con el avión de la colección
    private final Map<String, Marker> marcadores = new HashMap<>(); // Mapa que relaciona ICAO con Marker en el mapa
    private final Map<Integer, BitmapDescriptor> cacheIconos = new HashMap<>(); // Iconos ya creados, reutilizados por todos los marcadores
    private final Map<String, EstadoVuelo> estados = new HashMap<>(); // Mapa que relaciona ICAO con el último dato recibido del vuelo
    private final List<LatLng> puntosTrayectoria = new ArrayList<>(); // Puntos del camino recorrido por el avión seleccionado
    private Polyline lineaHistorial; // Línea con el camino ya recorrido por el avión seleccionado
    private Polyline lineaActual; // Tramo que une el último punto del camino con la posición actual del avión
    private long ultimoPuntoTrayectoria = 0; // Instante en el que se añadió el último punto al camino
    private String icaoSeleccionado = null; // ICAO del avión seleccionado actualmente
    private String tipoSeleccionado = null; // Código de tipo (por ejemplo A320) del avión seleccionado actualmente
    private String matriculaSeleccionada = null; // Matrícula del avión seleccionado actualmente
    private Location ultimaUbicacion = null; // Última ubicación conocida del usuario
    private Vuelo vueloSeleccionado = null; // Último dato recibido del vuelo seleccionado
    private Avion avistadoSeleccionado = null; // Avión de la colección que corresponde al vuelo seleccionado (si existe)
    private final Map<String, AdsbdbResponse.Aeronave> cacheAeronaves = new HashMap<>(); // Datos de aeronaves ya descargados (por ICAO)
    private final Map<String, AdsbdbResponse.RutaVuelo> cacheRutas = new HashMap<>(); // Rutas ya descargadas (por indicativo)
    private final Set<String> aeronavesNoDisponibles = new HashSet<>(); // Aeronaves que la API no conoce, para no repetir la consulta
    private final Set<String> rutasNoDisponibles = new HashSet<>(); // Indicativos que la API no conoce, para no repetir la consulta

    // Lanzador para pedir el permiso de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean concedido =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION)); // Basta con uno de los dos permisos
                if (concedido) {
                    activarUbicacion(); // Mostrar el punto azul si el usuario acepta
                } else {
                    Toast.makeText(this,
                            "Sin permiso no se puede mostrar tu ubicación",
                            Toast.LENGTH_SHORT).show(); // Avisar si el usuario rechaza
                }
            });

    // Clase que guarda el último dato recibido de un vuelo para estimar su posición entre actualizaciones
    private static class EstadoVuelo {
        final double lat; // Última latitud recibida
        final double lon; // Última longitud recibida
        final double velocidad; // Velocidad en metros por segundo
        final double rumbo; // Rumbo en grados respecto al norte
        final long tiempoBase; // Instante en el que se recibió el dato

        EstadoVuelo(double lat, double lon, double velocidad, double rumbo, long tiempoBase) {
            this.lat = lat;
            this.lon = lon;
            this.velocidad = velocidad;
            this.rumbo = rumbo;
            this.tiempoBase = tiempoBase;
        }

        // Calcular la posición estimada del avión en un instante dado
        LatLng posicionEn(long ahora) {
            double segundos = Math.min((ahora - tiempoBase) / 1000.0, 60); // Limitar el tiempo estimado por si fallan las descargas
            double distancia = velocidad * segundos; // Metros recorridos desde el último dato
            double rumboRad = Math.toRadians(rumbo); // Rumbo en radianes
            double dLat = (distancia * Math.cos(rumboRad)) / RADIO_TIERRA; // Desplazamiento en latitud (radianes)
            double dLon = (distancia * Math.sin(rumboRad))
                    / (RADIO_TIERRA * Math.cos(Math.toRadians(lat))); // Desplazamiento en longitud (radianes)
            return new LatLng(lat + Math.toDegrees(dLat), lon + Math.toDegrees(dLon)); // Nueva posición estimada
        }
    }

    // Método llamado al crear la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Llamada al método padre
        setContentView(R.layout.activity_mapa); // Cargar layout de la actividad

        // Inicializar elementos de la UI
        panel = findViewById(R.id.card_info_vuelo); // CardView para información del vuelo
        txtModelo = findViewById(R.id.txt_modelo_panel); // TextView para mostrar modelo/fabricante
        txtDatos = findViewById(R.id.txt_datos_panel); // TextView para mostrar otros datos del vuelo
        btnAvistar = findViewById(R.id.btn_avistar); // Botón para marcar un avión como avistado

        // Inicializar botones de cambio de modo
        btnTodos = findViewById(R.id.btn_todos); // Botón para mostrar todos los aviones
        btnAvistados = findViewById(R.id.btn_avistados); // Botón para mostrar solo avistados

        cargarColeccion(); // Cargar lista de aviones avistados desde SharedPreferences

        // Configurar Retrofit para consumir la API de vuelos
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL_API_VUELOS) // URL base de la API
                .addConverterFactory(GsonConverterFactory.create()) // Convertir JSON automáticamente
                .build();

        service = retrofit.create(AdsbService.class); // Crear instancia del servicio

        // Configurar Retrofit para consumir la API de detalles de aeronaves
        Retrofit retrofitDetalles = new Retrofit.Builder()
                .baseUrl(URL_API_DETALLES) // URL base de la API
                .addConverterFactory(GsonConverterFactory.create()) // Convertir JSON automáticamente
                .build();

        serviceDetalles = retrofitDetalles.create(AdsbdbService.class); // Crear instancia del servicio

        // Obtener el fragmento del mapa y configurar el callback
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this); // Ejecutar onMapReady cuando el mapa esté disponible

        // Configurar acciones de los botones
        btnTodos.setOnClickListener(v -> {
            modoActual = ModoMapa.TODOS; // Cambiar modo a TODOS
            refrescarMapa(); // Refrescar marcadores del mapa
            btnAvistar.setVisibility(View.VISIBLE); // Mostrar botón Avistar
        });

        btnAvistados.setOnClickListener(v -> {
            modoActual = ModoMapa.AVISTADOS; // Cambiar modo a AVISTADOS
            refrescarMapa(); // Refrescar marcadores del mapa
            btnAvistar.setVisibility(View.GONE); // Ocultar botón Avistar
        });

        btnAvistar.setOnClickListener(v -> avistarSeleccionado()); // Comprobar la distancia y guardar el avistamiento pendiente
    }

    // Método llamado cuando la pantalla vuelve a primer plano
    @Override
    protected void onResume() {
        super.onResume(); // Llamada al método padre
        activa = true; // Marcar la pantalla como activa
        handler.removeCallbacks(tareaActualizar); // Evitar duplicar la tarea
        handler.postDelayed(tareaActualizar, 1000); // Empezar las actualizaciones automáticas tras 1 segundo
        handler.removeCallbacks(tareaAnimar); // Evitar duplicar la animación
        handler.postDelayed(tareaAnimar, INTERVALO_ANIMACION_MS); // Empezar a mover los aviones con suavidad
    }

    // Método llamado cuando la pantalla deja de estar en primer plano
    @Override
    protected void onPause() {
        super.onPause(); // Llamada al método padre
        activa = false; // Marcar la pantalla como inactiva
        handler.removeCallbacks(tareaActualizar); // Detener las actualizaciones para no gastar peticiones ni batería
        handler.removeCallbacks(tareaAnimar); // Detener la animación de los aviones
    }

    // Callback cuando el mapa está listo
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // Guardar referencia del mapa
        mMap.setOnMarkerClickListener(this); // Asignar listener de click en marcadores

        // Al tocar una zona vacía del mapa se cierra el panel y se borra el camino del avión
        mMap.setOnMapClickListener(latLng -> {
            panel.setVisibility(View.GONE); // Ocultar panel de información
            ajustarPaddingMapa(false); // Devolver los controles del mapa a la esquina
            borrarTrayectoria(); // Quitar la línea del camino recorrido
        });

        // Mostrar los botones de zoom (+ / -) en el mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);

        pedirOActivarUbicacion(); // Pedir permiso de ubicación o activar el punto azul si ya está concedido

        // Mover la cámara a una ubicación inicial con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(48.0, 10.0), 7));

        if (activa) { // Solo si la pantalla está en primer plano
            handler.removeCallbacks(tareaActualizar); // Reiniciar el ciclo de actualizaciones
            handler.postDelayed(tareaActualizar, 1000); // Primera descarga después de 1 segundo
        }
    }

    // Comprobar el permiso de ubicación y pedirlo si todavía no está concedido
    private void pedirOActivarUbicacion() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; // Ubicación precisa
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; // Ubicación aproximada

        if (fine || coarse) {
            activarUbicacion(); // Permiso ya concedido, activar el punto azul
        } else {
            permisoUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}); // Mostrar el diálogo de permisos
        }
    }

    // Activar el punto azul de ubicación y el botón para centrar el mapa en el usuario
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void activarUbicacion() {
        if (mMap == null) return; // Salir si el mapa aún no está listo
        mMap.setMyLocationEnabled(true); // Mostrar el punto azul con la ubicación del usuario
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Mostrar el botón que centra el mapa en el usuario
        mMap.setOnMyLocationChangeListener(location -> ultimaUbicacion = location); // Guardar cada nueva ubicación del usuario
    }

    // Calcular la distancia en kilómetros entre el usuario y el avión seleccionado (null si no se puede calcular)
    private Float distanciaAlAvionKm() {
        if (ultimaUbicacion == null || icaoSeleccionado == null) return null; // Sin ubicación o sin avión seleccionado
        Marker m = marcadores.get(icaoSeleccionado); // Marcador del avión seleccionado
        if (m == null) return null; // El avión ya no está en el mapa

        float[] resultado = new float[1]; // Resultado del cálculo en metros
        Location.distanceBetween(
                ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(),
                m.getPosition().latitude, m.getPosition().longitude, resultado); // Distancia entre el usuario y el avión
        return resultado[0] / 1000f; // Convertir de metros a kilómetros
    }

    // Comprobar que el usuario está cerca del avión seleccionado y guardar el avistamiento pendiente de añadir
    private void avistarSeleccionado() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) { // Validar que un avión esté seleccionado
            Toast.makeText(this,
                    "Selecciona un avión primero",
                    Toast.LENGTH_SHORT).show(); // Mostrar mensaje corto
            return; // Salir si no hay selección
        }

        Float distancia = distanciaAlAvionKm(); // Distancia entre el usuario y el avión
        if (distancia == null) { // No se conoce la ubicación del usuario
            Toast.makeText(this,
                    "No se puede comprobar tu ubicación. Activa la ubicación y concede el permiso para avistar aviones",
                    Toast.LENGTH_LONG).show();
            return; // Salir sin guardar el avistamiento
        }

        if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) { // El usuario está demasiado lejos
            Toast.makeText(this,
                    "Estás a " + Math.round(distancia) + " km del avión. Acércate a menos de "
                            + Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM) + " km para avistarlo",
                    Toast.LENGTH_LONG).show();
            return; // Salir sin guardar el avistamiento
        }

        Marker m = marcadores.get(icaoSeleccionado); // Marcador del avión seleccionado
        LatLng posAvion = m != null
                ? m.getPosition() // Posición actual del avión en el mapa
                : new LatLng(vueloSeleccionado.latitude, vueloSeleccionado.longitude); // Última posición recibida

        // Nombre del modelo que da la API, por si el tipo no está en el catálogo
        String nombre = modeloCompleto(cacheAeronaves.get(icaoSeleccionado));
        if (nombre == null) nombre = vueloSeleccionado.descripcion;

        // Guardar en SharedPreferences el avistamiento verificado, pendiente de añadir
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icaoSeleccionado) // Identificador del avión
                .putString("tipo_seleccionado", tipoSeleccionado) // Tipo para elegir el modelo automáticamente
                .putString("matricula_seleccionada", matriculaSeleccionada) // Matrícula del avión
                .putString("descripcion_seleccionada", nombre) // Nombre del modelo según la API
                .putLong("fecha_seleccionada", System.currentTimeMillis()) // Momento del avistamiento
                .putFloat("distancia_seleccionada", distancia) // Distancia a la que estaba el usuario
                .putFloat("lat_avion_seleccionado", (float) posAvion.latitude) // Latitud del avión
                .putFloat("lon_avion_seleccionado", (float) posAvion.longitude) // Longitud del avión
                .apply();

        // Notificar al usuario que el avión está avistado
        Toast.makeText(this,
                "Avión avistado a " + Math.round(distancia) + " km. Pulsa + para añadirlo",
                Toast.LENGTH_LONG).show(); // Toast largo
    }

    // Método para refrescar el mapa eliminando marcadores antiguos y descargando nuevos
    private void refrescarMapa() {
        for (Marker m : marcadores.values()) m.remove(); // Eliminar cada marcador existente del mapa
        marcadores.clear(); // Limpiar mapa de marcadores
        estados.clear(); // Limpiar los datos de movimiento de los vuelos
        borrarTrayectoria(); // Quitar la línea del camino recorrido
        panel.setVisibility(View.GONE); // Ocultar panel de información
        ajustarPaddingMapa(false); // Devolver los controles del mapa a la esquina
        descargar(true); // Descargar nuevos vuelos mostrando avisos al usuario
    }

    // Sube o baja los controles del mapa según esté visible el panel del avión
    private void ajustarPaddingMapa(boolean panelVisible) {
        if (mMap == null) return; // Salir si el mapa aún no está listo

        if (panelVisible) {
            // Esperar a que el panel se dibuje para conocer su altura real
            panel.post(() -> {
                int margen = (int) (16 * getResources().getDisplayMetrics().density); // Margen del panel convertido a píxeles
                mMap.setPadding(0, 0, 0, panel.getHeight() + margen * 2); // Subir los controles por encima del panel
            });
        } else {
            // Sin panel: los controles vuelven a la esquina
            mMap.setPadding(0, 0, 0, 0);
        }
    }

    // Cargar la colección de aviones avistados desde SharedPreferences
    private void cargarColeccion() {
        miColeccion.clear(); // Limpiar lista existente
        coleccionPorIcao.clear(); // Limpiar mapa de búsqueda por ICAO

        String json = getSharedPreferences(
                "SkyCollectorDatos",
                Context.MODE_PRIVATE
        ).getString("lista_aviones", null); // Leer JSON desde SharedPreferences

        if (json == null) return; // Salir si no hay datos

        // Convertir JSON a lista de objetos Avion
        List<Avion> lista = new Gson().fromJson(
                json,
                new TypeToken<ArrayList<Avion>>() {}.getType()
        );

        if (lista != null) miColeccion.addAll(lista); // Agregar aviones a la colección local

        // Rellenar el mapa de búsqueda para encontrar cada avión por su ICAO al instante
        for (Avion a : miColeccion) {
            if (a.getIcao24() != null) {
                coleccionPorIcao.put(a.getIcao24().toLowerCase(), a); // Guardar con el ICAO en minúsculas
            }
        }
    }

    // Buscar un avión en la colección local por su ICAO
    private Avion buscarPorIcao(String icao) {
        if (icao == null) return null; // Retornar null si no hay ICAO
        return coleccionPorIcao.get(icao.toLowerCase()); // Buscar directamente en el mapa, sin recorrer la lista
    }

    // Descargar vuelos desde la API usando Retrofit
    private void descargar(boolean avisar) {
        if (mMap == null) return; // Salir si el mapa aún no está listo
        if (descargando) return; // Salir si ya hay una petición en curso

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds; // Zona que se ve actualmente en pantalla
        LatLng centro = zona.getCenter(); // Centro de la zona visible

        float[] distancia = new float[1]; // Resultado del cálculo de distancia
        Location.distanceBetween(centro.latitude, centro.longitude,
                zona.northeast.latitude, zona.northeast.longitude, distancia); // Distancia del centro a una esquina
        int radioNm = (int) Math.ceil(distancia[0] / 1852.0); // Convertir de metros a millas náuticas

        if (radioNm > RADIO_MAXIMO_NM) { // Comprobar que la zona visible no es demasiado grande
            if (avisar) {
                Toast.makeText(this,
                        "Acerca el mapa para ver los aviones",
                        Toast.LENGTH_SHORT).show(); // Avisar al usuario solo si la descarga fue manual
            }
            return; // Salir sin descargar
        }

        descargando = true; // Marcar que hay una petición en curso

        service.getVuelosEnZona(
                        String.format(Locale.US, "%.4f", centro.latitude), // Latitud del centro con punto decimal
                        String.format(Locale.US, "%.4f", centro.longitude), // Longitud del centro con punto decimal
                        radioNm) // Radio de la zona visible
                .enqueue(new Callback<AdsbResponse>() { // Llamada asíncrona
                    @Override
                    public void onResponse(Call<AdsbResponse> call,
                                           Response<AdsbResponse> response) {
                        descargando = false; // La petición ha terminado

                        if (isDestroyed()) return; // Salir si la pantalla ya se cerró

                        if (response.code() == 429 && !avisoLimiteMostrado) { // Límite de peticiones de la API alcanzado
                            avisoLimiteMostrado = true; // Mostrar el aviso solo una vez
                            Toast.makeText(MapaActivity.this,
                                    "Límite de peticiones de la API alcanzado",
                                    Toast.LENGTH_LONG).show();
                        }

                        if (response.body() == null ||
                                response.body().getAviones() == null) return; // Validar datos

                        List<Vuelo> vuelos = new ArrayList<>(); // Lista de vuelos ya convertidos
                        for (AdsbResponse.AdsbAircraft a : response.body().getAviones()) {
                            vuelos.add(a.toVuelo()); // Convertir cada avión a las unidades de la app
                        }
                        procesar(vuelos); // Procesar los vuelos recibidos
                    }

                    @Override
                    public void onFailure(Call<AdsbResponse> call, Throwable t) {
                        descargando = false; // La petición ha terminado, se reintentará en la siguiente actualización
                    }
                });
    }

    // Procesar la lista de vuelos recibidos de la API
    private void procesar(List<Vuelo> vuelos) {
        Set<String> visibles = new HashSet<>(); // Conjunto de ICAO visibles en el mapa
        long ahora = SystemClock.elapsedRealtime(); // Instante en el que se recibieron los datos

        for (Vuelo api : vuelos) { // Recorrer cada vuelo
            if (api.icao24 == null || api.latitude == null || api.longitude == null) continue; // Ignorar vuelos sin identificador o sin coordenadas

            Avion avistado = buscarPorIcao(api.icao24); // Buscar en colección local

            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue; // Filtrar según modo

            visibles.add(api.icao24); // Agregar ICAO a visibles
            LatLng pos = new LatLng(api.latitude, api.longitude); // Crear LatLng para el marcador

            estados.put(api.icao24, new EstadoVuelo(
                    api.latitude, api.longitude,
                    api.velocity != null ? api.velocity : 0,
                    api.trueTrack != null ? api.trueTrack : 0,
                    ahora)); // Guardar el último dato para estimar el movimiento entre actualizaciones

            Marker m = marcadores.get(api.icao24); // Buscar marcador existente
            if (m == null) { // Si no existe marcador
                BitmapDescriptor icono =
                        avistado != null
                                ? iconoSegunRareza(avistado.getRareza()) // Icono según rareza
                                : iconoGenerico(); // Icono genérico

                m = mMap.addMarker(new MarkerOptions()
                        .position(pos) // Posición del marcador
                        .icon(icono) // Icono del marcador
                        .rotation(api.trueTrack) // Rotación según rumbo
                        .flat(true) // Marcador plano
                        .anchor(0.5f, 0.5f)); // Anclaje al centro

                marcadores.put(api.icao24, m); // Guardar marcador
            } else {
                m.setPosition(pos); // Actualizar posición
                m.setRotation(api.trueTrack); // Actualizar rotación
            }

            m.setTag(new Object[]{api, avistado}); // Guardar datos en el tag del marcador

            if (api.icao24.equals(icaoSeleccionado)) { // Si es el avión seleccionado
                vueloSeleccionado = api; // Guardar el dato más reciente del vuelo
                avistadoSeleccionado = avistado; // Guardar el avión de la colección que le corresponde
                if (panel.getVisibility() == View.VISIBLE) pintarPanel(); // Refrescar velocidad y altitud en el panel
            }

            if (api.icao24.equals(icaoSeleccionado) && lineaHistorial != null) { // Si es el avión seleccionado y su camino está dibujado
                puntosTrayectoria.add(pos); // Añadir la nueva posición real al camino recorrido
                dibujarTrayectoria(); // Redibujar el camino
            }
        }

        // Eliminar marcadores que ya no están visibles
        Iterator<Map.Entry<String, Marker>> it =
                marcadores.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Marker> e = it.next();
            if (!visibles.contains(e.getKey())) {
                e.getValue().remove(); // Remover del mapa
                estados.remove(e.getKey()); // Remover sus datos de movimiento
                it.remove(); // Remover del mapa de marcadores
            }
        }
    }

    // Mover cada avión visible a la posición estimada según su velocidad y rumbo
    private void animarMarcadores() {
        if (mMap == null || marcadores.isEmpty()) return; // Salir si no hay nada que mover

        long ahora = SystemClock.elapsedRealtime(); // Instante actual

        // El avión seleccionado siempre se mueve y deja su camino, aunque haya poco zoom
        if (icaoSeleccionado != null && lineaHistorial != null) {
            Marker seleccionado = marcadores.get(icaoSeleccionado); // Marcador del avión seleccionado
            EstadoVuelo estadoSel = estados.get(icaoSeleccionado); // Último dato recibido de ese vuelo

            if (seleccionado != null && estadoSel != null && estadoSel.velocidad >= 1) {
                LatLng posSel = estadoSel.posicionEn(ahora); // Posición estimada ahora mismo
                seleccionado.setPosition(posSel); // Mover el marcador

                if (ahora - ultimoPuntoTrayectoria >= INTERVALO_PUNTO_TRAYECTORIA_MS) {
                    ultimoPuntoTrayectoria = ahora; // Guardar el instante del último punto
                    puntosTrayectoria.add(posSel); // Añadir la posición al camino recorrido
                    dibujarTrayectoria(); // Redibujar el camino
                } else {
                    actualizarTramoActual(posSel); // El camino sigue al avión seleccionado
                }
            }
        }

        if (mMap.getCameraPosition().zoom < ZOOM_MINIMO_ANIMACION) return; // A poco zoom el movimiento no se aprecia, se evita gastar recursos

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds; // Zona que se ve actualmente en pantalla

        for (Map.Entry<String, Marker> e : marcadores.entrySet()) { // Recorrer cada marcador
            if (e.getKey().equals(icaoSeleccionado)) continue; // El avión seleccionado ya se ha movido

            EstadoVuelo estado = estados.get(e.getKey()); // Último dato recibido del vuelo
            if (estado == null || estado.velocidad < 1) continue; // Ignorar vuelos sin datos o parados

            LatLng pos = estado.posicionEn(ahora); // Posición estimada ahora mismo
            if (!zona.contains(pos)) continue; // No mover los aviones que están fuera de la pantalla

            e.getValue().setPosition(pos); // Mover el marcador
        }
    }

    // Empezar a dibujar el camino recorrido por un avión
    private void mostrarTrayectoria(String icao) {
        borrarTrayectoria(); // Quitar el camino anterior

        Marker m = marcadores.get(icao); // Marcador del avión seleccionado
        if (m != null) puntosTrayectoria.add(m.getPosition()); // El camino empieza desde la posición actual
        ultimoPuntoTrayectoria = SystemClock.elapsedRealtime(); // Reiniciar el contador de puntos del camino

        dibujarTrayectoria(); // Dibujar las líneas
    }

    // Dibujar en el mapa el camino recorrido por el avión seleccionado
    private void dibujarTrayectoria() {
        if (lineaHistorial == null) lineaHistorial = mMap.addPolyline(estiloLinea()); // Crear la línea del camino recorrido
        lineaHistorial.setPoints(new ArrayList<>(puntosTrayectoria)); // Actualizar sus puntos

        if (lineaActual == null) lineaActual = mMap.addPolyline(estiloLinea()); // Crear el tramo hasta la posición actual

        Marker m = icaoSeleccionado != null ? marcadores.get(icaoSeleccionado) : null; // Marcador del avión seleccionado
        if (m != null) actualizarTramoActual(m.getPosition()); // Unir el camino con la posición actual
    }

    // Actualizar el tramo que une el último punto del camino con la posición actual del avión
    private void actualizarTramoActual(LatLng posActual) {
        if (lineaActual == null || puntosTrayectoria.isEmpty()) return; // Salir si no hay camino dibujado
        LatLng ultimo = puntosTrayectoria.get(puntosTrayectoria.size() - 1); // Último punto del camino
        lineaActual.setPoints(Arrays.asList(ultimo, posActual)); // Unir ese punto con el avión
    }

    // Quitar del mapa el camino del avión seleccionado
    private void borrarTrayectoria() {
        if (lineaHistorial != null) {
            lineaHistorial.remove(); // Eliminar la línea del camino recorrido
            lineaHistorial = null;
        }
        if (lineaActual != null) {
            lineaActual.remove(); // Eliminar el tramo hasta la posición actual
            lineaActual = null;
        }
        puntosTrayectoria.clear(); // Vaciar la lista de puntos
    }

    // Estilo de las líneas del camino del avión
    private PolylineOptions estiloLinea() {
        return new PolylineOptions()
                .color(Color.parseColor("#1E88E5")) // Color azul de la línea
                .width(8f) // Grosor de la línea
                .geodesic(false); // Línea recta entre puntos
    }

    // Listener para clicks en marcadores
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object[] data = (Object[]) marker.getTag(); // Obtener datos guardados en tag
        Vuelo api = (Vuelo) data[0]; // Datos del vuelo
        Avion a = (Avion) data[1]; // Avión de colección local (si existe)

        boolean cambioDeAvion = !api.icao24.equals(icaoSeleccionado) || lineaHistorial == null; // Comprobar si hay que dibujar un camino nuevo
        icaoSeleccionado = api.icao24; // Guardar ICAO seleccionado
        tipoSeleccionado = api.tipo; // Guardar el tipo de avión seleccionado
        matriculaSeleccionada = api.matricula; // Guardar la matrícula del avión seleccionado
        vueloSeleccionado = api; // Guardar el vuelo seleccionado para poder repintar el panel
        avistadoSeleccionado = a; // Guardar el avión de la colección que le corresponde
        if (cambioDeAvion) mostrarTrayectoria(api.icao24); // Dibujar el camino recorrido por el avión seleccionado

        pintarPanel(); // Mostrar en el panel los datos que ya se conocen
        pedirDetalles(api); // Pedir el modelo completo, la aerolínea y la ruta

        panel.setVisibility(View.VISIBLE); // Mostrar panel de información
        ajustarPaddingMapa(true); // Subir los controles del mapa por encima del panel
        btnAvistar.setVisibility(
                a == null && modoActual == ModoMapa.TODOS
                        ? View.VISIBLE // Mostrar botón solo si no está avistado y modo TODOS
                        : View.GONE
        );

        return true; // Consumir evento click
    }

    // Pedir a la API los datos de la aeronave y de la ruta del vuelo seleccionado, en dos consultas independientes
    private void pedirDetalles(Vuelo api) {
        pedirAeronave(api.icao24); // Modelo completo y propietario del avión
        if (api.callsign != null && !api.callsign.isEmpty()) pedirRuta(api.callsign); // Aerolínea y ruta del vuelo
    }

    // Pedir a la API los datos de una aeronave (modelo completo y propietario)
    private void pedirAeronave(String icao) {
        if (icao == null || cacheAeronaves.containsKey(icao) || aeronavesNoDisponibles.contains(icao)) return; // Salir si ya se consultó

        serviceDetalles.getAeronave(icao).enqueue(new Callback<AdsbdbResponse>() { // Llamada asíncrona
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed()) return; // Salir si la pantalla ya se cerró

                AdsbdbResponse body = response.body(); // Contenido de la respuesta
                if (!response.isSuccessful() || body == null
                        || body.getRespuesta() == null || body.getRespuesta().aircraft == null) {
                    aeronavesNoDisponibles.add(icao); // Recordar que la API no conoce esta aeronave
                    Log.d("SkyCollector", "adsbdb aeronave " + icao + " sin datos (HTTP " + response.code() + ")");
                    return;
                }

                cacheAeronaves.put(icao, body.getRespuesta().aircraft); // Guardar los datos en memoria
                if (icao.equals(icaoSeleccionado)) pintarPanel(); // Repintar si sigue seleccionado
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb aeronave " + icao + " fallo: " + t.getMessage()); // Se reintentará al tocar de nuevo
            }
        });
    }

    // Pedir a la API la aerolínea y la ruta de un vuelo a partir de su indicativo
    private void pedirRuta(String indicativo) {
        if (cacheRutas.containsKey(indicativo) || rutasNoDisponibles.contains(indicativo)) return; // Salir si ya se consultó

        serviceDetalles.getRuta(indicativo).enqueue(new Callback<AdsbdbResponse>() { // Llamada asíncrona
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed()) return; // Salir si la pantalla ya se cerró

                AdsbdbResponse body = response.body(); // Contenido de la respuesta
                if (!response.isSuccessful() || body == null
                        || body.getRespuesta() == null || body.getRespuesta().flightroute == null) {
                    rutasNoDisponibles.add(indicativo); // Recordar que la API no conoce este indicativo
                    Log.d("SkyCollector", "adsbdb ruta " + indicativo + " sin datos (HTTP " + response.code() + ")");
                    return;
                }

                cacheRutas.put(indicativo, body.getRespuesta().flightroute); // Guardar la ruta en memoria
                if (vueloSeleccionado != null && indicativo.equals(vueloSeleccionado.callsign)) pintarPanel(); // Repintar si sigue seleccionado
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb ruta " + indicativo + " fallo: " + t.getMessage()); // Se reintentará al tocar de nuevo
            }
        });
    }

    // Escribir en el panel los datos del vuelo seleccionado con la información disponible
    private void pintarPanel() {
        Vuelo api = vueloSeleccionado; // Vuelo seleccionado
        Avion a = avistadoSeleccionado; // Avión de la colección (si existe)
        if (api == null) return; // Salir si no hay ningún vuelo seleccionado

        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(api.icao24); // Datos de la aeronave (si los hay)
        AdsbdbResponse.RutaVuelo ruta = api.callsign != null ? cacheRutas.get(api.callsign) : null; // Datos de la ruta (si los hay)

        Avion modeloCatalogo = a == null
                ? AvionGenerator.getAvionPorTipoIcao(api.tipo) // Buscar el tipo en el catálogo si el avión aún no está avistado
                : null;

        // Parte izquierda del título: modelo completo, o el del catálogo si no hay datos
        String modelo = modeloCompleto(aeronave);
        if (modelo == null) {
            if (a != null) {
                modelo = a.getModelo(); // Modelo del avión ya avistado
            } else if (modeloCatalogo != null) {
                modelo = modeloCatalogo.getModelo(); // Modelo del catálogo que corresponde al tipo
            } else if (api.descripcion != null && !api.descripcion.isEmpty()) {
                modelo = api.descripcion; // Descripción que da la API de vuelos
            } else {
                modelo = "Vuelo " + (api.callsign != null && !api.callsign.isEmpty()
                        ? api.callsign : api.icao24); // Callsign (o ICAO si no hay)
            }
        }

        // Parte derecha del título: aerolínea, o propietario, o fabricante del catálogo
        String empresa = null;
        if (ruta != null && ruta.airline != null && ruta.airline.name != null && !ruta.airline.name.isEmpty()) {
            empresa = ruta.airline.name; // Aerolínea que opera el vuelo
        } else if (aeronave != null && aeronave.registeredOwner != null && !aeronave.registeredOwner.isEmpty()) {
            empresa = formatearNombre(aeronave.registeredOwner); // Propietario registrado de la aeronave
        } else if (a != null) {
            empresa = a.getFabricante(); // Fabricante del avión ya avistado
        } else if (modeloCatalogo != null) {
            empresa = modeloCatalogo.getFabricante(); // Fabricante del modelo del catálogo
        }

        txtModelo.setText(empresa != null && !empresa.isEmpty() ? modelo + " | " + empresa : modelo); // Mostrar el título

        int vel = api.velocity != null
                ? (int) (api.velocity * 3.6) // Convertir de m/s a km/h
                : 0; // Velocidad cero si no disponible

        String rareza = a != null
                ? a.getRareza() // Rareza del avión ya avistado
                : (modeloCatalogo != null ? modeloCatalogo.getRareza() : null); // Rareza del modelo del catálogo si se conoce

        String altitud = api.enTierra
                ? "En tierra" // Avión parado en el suelo
                : (api.altitude != null ? Math.round(api.altitude) + " m" : "—"); // Altitud redondeada o guion si no hay dato

        // Línea con la distancia al avión, indicando si es demasiado lejos para avistarlo
        Float distancia = distanciaAlAvionKm();
        String lineaDistancia = distancia == null
                ? ""
                : "Distancia: " + Math.round(distancia) + " km"
                + (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM ? " (demasiado lejos para avistarlo)" : "") + "\n";

        // Línea con el origen y el destino si la API los conoce
        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null;
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null;
        String lineaRuta = (origen != null && destino != null) ? "Ruta: " + origen + " → " + destino + "\n" : "";

        txtDatos.setText(
                lineaRuta +
                        "ICAO: " + api.icao24 + "\n" +
                        "Matrícula: " + (api.matricula != null ? api.matricula : "—") + "\n" +
                        "Tipo: " + (api.tipo != null ? api.tipo : "—") + "\n" +
                        (rareza != null ? "Rareza: " + rareza + "\n" : "") +
                        lineaDistancia +
                        "Velocidad: " + vel + " km/h\n" +
                        "Altitud: " + altitud
        ); // Mostrar otros datos
    }

    // Construir el modelo completo de la aeronave a partir de los datos de la API (por ejemplo Airbus A320-252N)
    private String modeloCompleto(AdsbdbResponse.Aeronave aeronave) {
        if (aeronave == null || aeronave.type == null || aeronave.type.trim().isEmpty()) return null; // Sin datos
        String tipo = aeronave.type.trim().replaceFirst("^(A\\d{3})\\s+(\\d{3}[A-Z]?)$", "$1-$2"); // Los Airbus pasan de A320 214 a A320-214
        String fabricante = formatearNombre(aeronave.manufacturer); // Fabricante con formato legible
        if (fabricante == null || fabricante.isEmpty()
                || tipo.toLowerCase().contains(fabricante.toLowerCase())) {
            return tipo; // No se repite el fabricante si ya está incluido
        }
        return fabricante + " " + tipo; // Fabricante seguido del modelo
    }

    // Convertir un nombre escrito todo en mayúsculas a un formato legible (AIRBUS pasa a Airbus)
    private String formatearNombre(String texto) {
        if (texto == null) return null; // Salir si no hay texto
        texto = texto.trim(); // Quitar espacios sobrantes
        if (texto.isEmpty() || !texto.equals(texto.toUpperCase())) return texto; // Solo se corrige si está todo en mayúsculas

        StringBuilder sb = new StringBuilder(); // Resultado
        for (String palabra : texto.split(" ")) { // Recorrer cada palabra
            if (palabra.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (palabra.length() <= 3) {
                sb.append(palabra); // Las palabras cortas (ATR, BAE, DE) se dejan como siglas
            } else {
                sb.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1).toLowerCase()); // Primera letra en mayúscula y el resto en minúscula
            }
        }
        return sb.toString();
    }

    // Describir un aeropuerto con su ciudad y su código IATA (por ejemplo Seville (SVQ))
    private String describirAeropuerto(AdsbdbResponse.Aeropuerto ap) {
        if (ap == null) return null; // Sin datos del aeropuerto
        String ciudad = (ap.municipality != null && !ap.municipality.isEmpty()) ? ap.municipality : ap.name; // Ciudad o nombre
        if (ciudad == null) return ap.iataCode; // Solo el código si no hay nombre
        return (ap.iataCode != null && !ap.iataCode.isEmpty()) ? ciudad + " (" + ap.iataCode + ")" : ciudad;
    }

    // Método para obtener icono genérico de avión
    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun); // Icono de avión común
    }

    // Método para obtener icono según rareza del avión
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res; // Recurso drawable
        switch (rareza != null ? rareza.toUpperCase() : "") { // Comparar rareza ignorando mayúsculas/minúsculas
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY": res = R.drawable.avionlegendario; break;
            case "MYTHIC": res = R.drawable.avionlegendario; break; // Los míticos usan el icono legendario de momento
            default: res = R.drawable.avioncomun; // Por defecto común
        }
        return iconoDesdeDrawable(res); // Devolver icono como BitmapDescriptor
    }

    // Convertir drawable a BitmapDescriptor para usar como icono en Google Maps
    private BitmapDescriptor iconoDesdeDrawable(int res) {
        BitmapDescriptor cacheado = cacheIconos.get(res); // Buscar si el icono ya fue creado antes
        if (cacheado != null) return cacheado; // Reutilizar el icono existente

        Drawable d = ContextCompat.getDrawable(this, res); // Obtener drawable desde recurso
        int size = 64; // Tamaño en píxeles del icono

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); // Crear bitmap en memoria
        Canvas c = new Canvas(b); // Canvas para dibujar sobre el bitmap
        d.setBounds(0, 0, size, size); // Establecer bounds del drawable
        d.draw(c); // Dibujar drawable sobre canvas

        BitmapDescriptor icono = BitmapDescriptorFactory.fromBitmap(b); // Convertir bitmap a descriptor de icono
        cacheIconos.put(res, icono); // Guardar el icono para reutilizarlo en el resto de marcadores
        return icono; // Devolver icono creado
    }
}