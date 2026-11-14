// Declaración del paquete al que pertenece esta actividad
package es.medac.skycollectorapp.activities;

// Importaciones de permisos y contexto del sistema Android
import android.Manifest; // Permisos del sistema Android para cámara y ubicación
import android.annotation.SuppressLint; // Anotación para silenciar advertencias de permisos comprobados manualmente
import android.content.Context; // Contexto global de la aplicación
import android.content.SharedPreferences; // Sistema de almacenamiento ligero clave-valor
import android.content.pm.PackageManager; // Gestor para verificar permisos de la aplicación
import android.graphics.Bitmap; // Objeto de imagen gráfica en memoria
import android.graphics.Canvas; // Lienzo para realizar dibujos sobre objetos Bitmap
import android.graphics.Color; // Utilidad para la gestión de colores RGB y Hexadecimales
import android.graphics.drawable.Drawable; // Recursos de imagen dibujables en la interfaz
import android.location.Location; // Objeto que contiene coordenadas y datos de posicionamiento GPS
import android.os.Bundle; // Contenedor de datos para pasar información entre estados del ciclo de vida
import android.os.Handler; // Manejador para enviar y procesar mensajes/tareas en el hilo principal
import android.os.Looper; // Bucle de procesamiento de eventos para el hilo de interfaz
import android.os.SystemClock; // Reloj de sistema para medir intervalos de tiempo transcurridos
import android.util.Log; // Utilidad para imprimir registros de depuración en la consola Logcat
import android.view.View; // Componente base de todos los elementos visuales de la interfaz
import android.widget.Button; // Componente de la interfaz tipo botón de interacción
import android.widget.TextView; // Componente de la interfaz para mostrar etiquetas de texto
import android.widget.Toast; // Notificaciones flotantes de texto breve en pantalla

// Importaciones de librerías de soporte AndroidX
import androidx.activity.result.ActivityResultLauncher; // Lanzador para recibir resultados de actividades
import androidx.activity.result.contract.ActivityResultContracts; // Contratos estándar para solicitar permisos del sistema
import androidx.annotation.NonNull; // Anotación para indicar parámetros que no deben ser nulos
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades con soporte de ActionBar
import androidx.cardview.widget.CardView; // Tarjeta contenedora con elevación y bordes redondeados
import androidx.core.content.ContextCompat; // Métodos de compatibilidad para recursos y permisos

// Importaciones del SDK de Google Maps para Android
import com.google.android.gms.maps.CameraUpdateFactory; // Utilidad para aplicar cambios y animaciones a la cámara del mapa
import com.google.android.gms.maps.GoogleMap; // Clase principal para controlar el mapa de Google
import com.google.android.gms.maps.OnMapReadyCallback; // Interfaz que notifica cuando el mapa está listo
import com.google.android.gms.maps.SupportMapFragment; // Fragmento de interfaz que alberga el componente de mapa
import com.google.android.gms.maps.model.BitmapDescriptor; // Descriptor gráfico utilizado para definir el icono de un marcador
import com.google.android.gms.maps.model.BitmapDescriptorFactory; // Fábrica para crear objetos BitmapDescriptor desde distintas fuentes
import com.google.android.gms.maps.model.LatLng; // Representación de una coordenada geográfica (Latitud, Longitud)
import com.google.android.gms.maps.model.LatLngBounds; // Rectángulo geográfico definido por coordenadas de esquinas
import com.google.android.gms.maps.model.Marker; // Representación de un marcador visual posicionado en el mapa
import com.google.android.gms.maps.model.MarkerOptions; // Conjunto de opciones para personalizar un objeto Marker
import com.google.android.gms.maps.model.Polyline; // Objeto de línea poligonal dibujada sobre el mapa
import com.google.android.gms.maps.model.PolylineOptions; // Conjunto de opciones para personalizar una línea Polyline

// Importaciones de la librería Gson para serialización y deserialización de JSON
import com.google.gson.Gson; // Motor principal para convertir objetos a JSON y viceversa
import com.google.gson.reflect.TypeToken; // Preservación de tipos de datos genéricos en colecciones para Gson

// Importaciones de utilidades estándar de Java
import java.text.SimpleDateFormat; // Formateador de fechas para registros
import java.util.ArrayList; // Estructura de lista dinámicamente redimensionable
import java.util.Arrays; // Utilidades para la conversión y manipulación de arreglos
import java.util.Date; // Objeto para gestión de fechas
import java.util.HashMap; // Colección de mapa basado en pares clave-valor
import java.util.HashSet; // Colección de conjunto que garantiza elementos únicos sin duplicados
import java.util.Iterator; // Interfaz para recorrer elementos de una colección
import java.util.List; // Interfaz para la definición de listas
import java.util.Locale; // Configuración de región geográfica e idioma para formateo de cadenas
import java.util.Map; // Interfaz para la definición de mapas
import java.util.Set; // Interfaz para la definición de conjuntos

// Importaciones del paquete propio del proyecto
import es.medac.skycollectorapp.R; // Recursos generados del proyecto (layouts, drawables, cadenas)
import es.medac.skycollectorapp.models.AdsbResponse; // Modelo que representa la respuesta de la API de radar en vivo
import es.medac.skycollectorapp.models.AdsbdbResponse; // Modelo que representa la respuesta de la API de detalles de aeronaves
import es.medac.skycollectorapp.models.Avion; // Modelo de datos del objeto Avión para la colección
import es.medac.skycollectorapp.models.Vuelo; // Modelo procesado para almacenar datos de un vuelo en pantalla
import es.medac.skycollectorapp.network.AdsbService; // Interfaz Retrofit para consultas de tráfico aéreo en vivo
import es.medac.skycollectorapp.network.AdsbdbService; // Interfaz Retrofit para consultas de fichas técnicas de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator; // Clase utilitaria con el catálogo estático de aeronaves

// Importaciones de la librería cliente HTTP Retrofit
import retrofit2.Call; // Representación de una petición HTTP asíncrona de Retrofit
import retrofit2.Callback; // Interfaz para recibir respuestas o errores de llamadas HTTP
import retrofit2.Response; // Objeto envoltorio que contiene la respuesta HTTP devuelta por la API
import retrofit2.Retrofit; // Cliente principal para la creación de servicios de red HTTP
import retrofit2.converter.gson.GsonConverterFactory; // Conversor de datos JSON a objetos Java para Retrofit

// Clase principal que gestiona la pantalla del mapa interactivo con radares de tráfico aéreo en tiempo real
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener { // Implementa los escuchadores de mapa listo y clic en marcadores

    // ------------------------------
    // ENUMS Y CONSTANTES DE CONFIGURACIÓN
    // ------------------------------
    private enum ModoMapa { TODOS, AVISTADOS } // Enumeración para gestionar los modos de filtro del mapa
    private ModoMapa modoActual = ModoMapa.TODOS; // Modo de visualización seleccionado inicialmente

    private static final String URL_API_VUELOS = "https://api.adsb.lol/"; // Dirección URL base de la API de tráfico aéreo
    private static final int RADIO_MAXIMO_NM = 250; // Radio máximo de consulta permitido por la API en millas náuticas
    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 30f; // Distancia límite en kilómetros para permitir avistar un avión
    private static final String URL_API_DETALLES = "https://api.adsbdb.com/"; // Dirección URL base de la API de fichas de aeronaves

    private static final long INTERVALO_ACTUALIZACION_MS = 15000; // Tiempo entre descargas automáticas de tráfico (15 segundos)
    private static final long INTERVALO_ANIMACION_MS = 300; // Frecuencia de recalculación de movimiento suave (300 milisegundos)
    private static final float ZOOM_MINIMO_ANIMACION = 7f; // Zoom mínimo requerido para ejecutar animaciones de movimiento
    private static final double RADIO_TIERRA = 6371000.0; // Radio medio constante del planeta Tierra expresado en metros
    private static final long INTERVALO_PUNTO_TRAYECTORIA_MS = 5000; // Tiempo entre capturas de puntos para el camino del avión (5s)

    // ------------------------------
    // COMPONENTES PRINCIPALES Y SERVICIOS
    // ------------------------------
    private GoogleMap mMap; // Referencia al objeto de control del mapa de Google
    private AdsbService service; // Instancia del servicio Retrofit para consultar datos de tráfico aéreo
    private AdsbdbService serviceDetalles; // Instancia del servicio Retrofit para consultar datos técnicos detallados
    private final Handler handler = new Handler(Looper.getMainLooper()); // Manejador asociado al hilo principal de UI

    private boolean activa = false; // Bandera de estado para controlar si la actividad está en primer plano
    private boolean descargando = false; // Bandera para evitar peticiones de red simultáneas o duplicadas
    private boolean avisoLimiteMostrado = false; // Bandera para evitar repetir la alerta por exceso de peticiones a la API
    private long ultimaDescargaManual = 0; // Marca de tiempo para controlar el cooldown entre peticiones manuales y evitar baneos

    // ------------------------------
    // TAREAS PROGRAMADAS (RUNNABLES)
    // ------------------------------

    // Tarea periódica para solicitar la descarga de datos de vuelos y reprogramar su ejecución
    private final Runnable tareaActualizar = new Runnable() {
        @Override
        public void run() {
            descargar(false); // Descargar datos de vuelos de forma silenciosa sin avisos de zoom
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS); // Programar la siguiente descarga en 15 segundos
        }
    };

    // Tarea periódica para recalcular la ubicación proyectada de los aviones y moverlos suavemente
    private final Runnable tareaAnimar = new Runnable() {
        @Override
        public void run() {
            animarMarcadores(); // Mover marcadores a la coordenada interpolada en base a su velocidad
            handler.postDelayed(this, INTERVALO_ANIMACION_MS); // Programar la siguiente recalculación en 300ms
        }
    };

    // ------------------------------
    // COMPONENTES DE INTERFAZ Y COLECCIONES
    // ------------------------------
    private CardView panel; // Contenedor tipo tarjeta que muestra la información del avión seleccionado
    private TextView txtModelo, txtDatos; // Campos de texto para mostrar nombre, fabricante y telemetría del avión
    private Button btnAvistar, btnTodos, btnAvistados; // Botones para realizar el avistamiento y cambiar filtros

    private final List<Avion> miColeccion = new ArrayList<>(); // Lista local con los aviones capturados por el usuario
    private final Map<String, Avion> coleccionPorIcao = new HashMap<>(); // Mapa de acceso rápido para buscar aviones capturados por su ICAO
    private final Map<String, Marker> marcadores = new HashMap<>(); // Mapa para asociar cada código ICAO con su Marker en el mapa
    private final Map<Integer, BitmapDescriptor> cacheIconos = new HashMap<>(); // Caché para almacenar y reutilizar los iconos generados
    private final Map<String, EstadoVuelo> estados = new HashMap<>(); // Mapa para guardar el último estado de telemetría de cada vuelo
    private final List<LatLng> puntosTrayectoria = new ArrayList<>(); // Lista de puntos de coordenadas que forman la ruta del avión activo

    private Polyline lineaHistorial; // Objeto Polyline para dibujar el tramo histórico recorrido por el avión
    private Polyline lineaActual; // Objeto Polyline para conectar la posición anterior del historial con la posición viva
    private long ultimoPuntoTrayectoria = 0; // Registro temporal del instante en que se añadió el último punto a la trayectoria

    private String icaoSeleccionado = null; // Código ICAO del avión seleccionado en el mapa
    private String tipoSeleccionado = null; // Código ICAO de tipo de aeronave del avión seleccionado
    private String matriculaSeleccionada = null; // Matrícula de la aeronave seleccionada
    private Location ultimaUbicacion = null; // Última posición GPS del usuario obtenida en el sistema
    private Vuelo vueloSeleccionado = null; // Objeto Vuelo asociado al avión seleccionado actualmente
    private Avion avistadoSeleccionado = null; // Objeto Avion de la colección local coincidente si ya fue avistado

    private final Map<String, AdsbdbResponse.Aeronave> cacheAeronaves = new HashMap<>(); // Caché en memoria para almacenar fichas de aeronaves
    private final Map<String, AdsbdbResponse.RutaVuelo> cacheRutas = new HashMap<>(); // Caché en memoria para almacenar rutas de vuelos
    private final Set<String> aeronavesNoDisponibles = new HashSet<>(); // Conjunto de códigos ICAO no encontrados en la API para omitir reintentos
    private final Set<String> rutasNoDisponibles = new HashSet<>(); // Conjunto de indicativos no encontrados en la API para omitir reintentos

    // Lanzador para solicitar los permisos de ubicación en tiempo de ejecución
    private final ActivityResultLauncher<String[]> permisoUbicacionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean concedido =
                        Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION)); // Validar si se aceptó al menos un permiso
                if (concedido) {
                    activarUbicacion(); // Activar capa de posición si se concede el permiso
                } else {
                    Toast.makeText(this, "Sin permiso no se puede mostrar tu ubicación", Toast.LENGTH_SHORT).show(); // Informar en caso de rechazo
                }
            });

    // ------------------------------
    // CLASE INTERNA DE ESTADO DE VUELO
    // ------------------------------

    // Estructura interna para almacenar la velocidad, rumbo y tiempo de lectura de un avión y proyectar su movimiento
    private static class EstadoVuelo {
        final double lat; // Última latitud real reportada por la API
        final double lon; // Última longitud real reportada por la API
        final double velocidad; // Velocidad del avión medida en metros por segundo
        final double rumbo; // Rumbo del avión medido en grados sexagesimales
        final long tiempoBase; // Marca de tiempo en milisegundos de la recepción del dato

        EstadoVuelo(double lat, double lon, double velocidad, double rumbo, long tiempoBase) {
            this.lat = lat;
            this.lon = lon;
            this.velocidad = velocidad;
            this.rumbo = rumbo;
            this.tiempoBase = tiempoBase;
        }

        // Estima y devuelve la posición geográfica esperada del avión en función del tiempo transcurrido
        LatLng posicionEn(long ahora) {
            double segundos = Math.min((ahora - tiempoBase) / 1000.0, 60); // Limitar la estimación a un máximo de 60 segundos
            double distancia = velocidad * segundos; // Calcular la distancia estimada en metros
            double rumboRad = Math.toRadians(rumbo); // Convertir el rumbo sexagesimal a radianes

            double dLat = (distancia * Math.cos(rumboRad)) / RADIO_TIERRA; // Desplazamiento estimado en latitud (radianes)
            double dLon = (distancia * Math.sin(rumboRad)) / (RADIO_TIERRA * Math.cos(Math.toRadians(lat))); // Desplazamiento estimado en longitud (radianes)

            return new LatLng(lat + Math.toDegrees(dLat), lon + Math.toDegrees(dLon)); // Devolver la coordenada geográfica interpolada
        }
    }

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Método invocado al crear la actividad
        super.onCreate(savedInstanceState); // Ejecutar código base del padre
        setContentView(R.layout.activity_mapa); // Cargar el diseño XML de la pantalla del mapa

        // Asociar las variables de la clase con los componentes del XML
        panel = findViewById(R.id.card_info_vuelo); // Tarjeta desplegable del vuelo
        txtModelo = findViewById(R.id.txt_modelo_panel); // Texto de modelo del panel
        txtDatos = findViewById(R.id.txt_datos_panel); // Texto de datos técnicos del panel
        btnAvistar = findViewById(R.id.btn_avistar); // Botón para registrar el avistamiento
        btnTodos = findViewById(R.id.btn_todos); // Botón de filtro para ver todos los vuelos
        btnAvistados = findViewById(R.id.btn_avistados); // Botón de filtro para ver solo avistados

        cargarColeccion(); // Leer la colección local guardada en SharedPreferences

        // Configurar el cliente Retrofit para las consultas a la API de tráfico aéreo
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL_API_VUELOS) // Asignar la URL base de vuelos
                .addConverterFactory(GsonConverterFactory.create()) // Añadir conversor JSON
                .build();
        service = retrofit.create(AdsbService.class); // Crear instancia de la interfaz

        // Configurar el cliente Retrofit para las consultas a la API de fichas de aeronaves
        Retrofit retrofitDetalles = new Retrofit.Builder()
                .baseUrl(URL_API_DETALLES) // Asignar la URL base de fichas
                .addConverterFactory(GsonConverterFactory.create()) // Añadir conversor JSON
                .build();
        serviceDetalles = retrofitDetalles.create(AdsbdbService.class); // Crear instancia de la interfaz

        // Obtener la referencia al fragmento del mapa y suscribir el callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // Iniciar la carga del mapa asíncronamente
        }

        // Configurar acción al pulsar el botón de ver todos los vuelos
        btnTodos.setOnClickListener(v -> {
            modoActual = ModoMapa.TODOS; // Cambiar el filtro a modo TODOS
            refrescarMapa(); // Limpiar y volver a pedir datos
            btnAvistar.setVisibility(View.VISIBLE); // Hacer visible el botón de avistar
        });

        // Configurar acción al pulsar el botón de ver solo aviones avistados
        btnAvistados.setOnClickListener(v -> {
            modoActual = ModoMapa.AVISTADOS; // Cambiar el filtro a modo AVISTADOS
            refrescarMapa(); // Limpiar y volver a pedir datos
            btnAvistar.setVisibility(View.GONE); // Ocultar el botón de avistar
        });

        // Configurar acción al pulsar el botón de avistar
        btnAvistar.setOnClickListener(v -> avistarSeleccionado()); // Procesar la verificación del avistamiento
    }

    @Override
    protected void onResume() { // Método invocado cuando la actividad vuelve a estar visible
        super.onResume(); // Llamada al método base
        activa = true; // Marcar el estado como activo
        handler.removeCallbacks(tareaActualizar); // Limpiar ejecuciones pendientes de descargas
        handler.postDelayed(tareaActualizar, 1000); // Programar la primera descarga tras 1 segundo
        handler.removeCallbacks(tareaAnimar); // Limpiar ejecuciones pendientes de animación
        handler.postDelayed(tareaAnimar, INTERVALO_ANIMACION_MS); // Programar el inicio de las animaciones suaves
    }

    @Override
    protected void onPause() { // Método invocado cuando la actividad pasa a segundo plano
        super.onPause(); // Llamada al método base
        activa = false; // Marcar el estado como inactivo
        handler.removeCallbacks(tareaActualizar); // Cancelar las descargas en segundo plano para ahorrar datos
        handler.removeCallbacks(tareaAnimar); // Cancelar la animación de los marcadores para ahorrar procesador
    }

    // ------------------------------
    // CONFIGURACIÓN Y CALLBACKS DEL MAPA
    // ------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) { // Callback invocado cuando el mapa está completamente cargado
        mMap = googleMap; // Almacenar la referencia al objeto GoogleMap
        mMap.setOnMarkerClickListener(this); // Asignar la propia clase como escuchadora de clics en marcadores

        // Configurar el evento de toque en una zona vacía del mapa
        mMap.setOnMapClickListener(latLng -> {
            panel.setVisibility(View.GONE); // Ocultar el panel inferior de información
            ajustarPaddingMapa(false); // Restablecer la posición original de los botones del mapa
            borrarTrayectoria(); // Eliminar las líneas de ruta dibujadas
        });

        mMap.getUiSettings().setZoomControlsEnabled(true); // Habilitar los controles de Zoom visuales (+ / -)
        pedirOActivarUbicacion(); // Verificar permisos de GPS y activar la capa de posición si procede

        // Centrar la vista inicial del mapa con un nivel de zoom adecuado
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.0, 10.0), 7));

        if (activa) { // Si la pantalla está activa al completar la carga
            handler.removeCallbacks(tareaActualizar); // Limpiar tareas pendientes
            handler.postDelayed(tareaActualizar, 1000); // Disparar actualización de vuelos tras 1s
        }
    }

    // Comprueba el estado de los permisos de GPS e inicia la solicitud o la activación según corresponda
    private void pedirOActivarUbicacion() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; // Comprobar GPS preciso
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; // Comprobar GPS de red

        if (fine || coarse) {
            activarUbicacion(); // Si se cuenta con algún permiso, activar la capa de usuario
        } else {
            permisoUbicacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }); // Solicitar permisos mediante el lanzador estándar
        }
    }

    // Activa la capa del punto azul del mapa que señala la ubicación GPS del usuario
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private void activarUbicacion() {
        if (mMap == null) return; // Salir si la instancia del mapa no existe
        mMap.setMyLocationEnabled(true); // Habilitar el punto azul de ubicación
        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Habilitar el botón para centrar la cámara en el usuario
        mMap.setOnMyLocationChangeListener(location -> ultimaUbicacion = location); // Registrar los cambios de posición del usuario
    }

    // ------------------------------
    // LÓGICA DE AVISTAMIENTO Y DISTANCIAS
    // ------------------------------

    // Calcula la distancia en kilómetros entre el usuario y el avión seleccionado actualmente
    private Float distanciaAlAvionKm() {
        if (ultimaUbicacion == null || icaoSeleccionado == null) return null; // Retornar nulo si no hay GPS o avión elegido
        Marker m = marcadores.get(icaoSeleccionado); // Obtener el marcador activo del avión
        if (m == null) return null; // Retornar nulo si el avión ya no existe en el mapa

        float[] resultado = new float[1]; // Arreglo para almacenar la distancia calculada en metros
        Location.distanceBetween(
                ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude(),
                m.getPosition().latitude, m.getPosition().longitude, resultado); // Calcular distancia geodésica
        return resultado[0] / 1000f; // Convertir y devolver el resultado en kilómetros
    }

    // Valida la proximidad física del usuario y registra un avistamiento verificado pendiente de guardado
    private void avistarSeleccionado() {
        if (icaoSeleccionado == null || vueloSeleccionado == null) { // Comprobar selección activa
            Toast.makeText(this, "Selecciona un avión primero", Toast.LENGTH_SHORT).show(); // Avisar al usuario
            return; // Interrumpir ejecución
        }

        Float distancia = distanciaAlAvionKm(); // Obtener la distancia actual en kilómetros
        if (distancia == null) { // Comprobar disponibilidad de ubicación GPS
            Toast.makeText(this, "No se puede comprobar tu ubicación. Activa la ubicación y concede el permiso para avistar aviones", Toast.LENGTH_LONG).show();
            return; // Interrumpir ejecución
        }

        if (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) { // Comprobar si el usuario supera el límite de distancia
            Toast.makeText(this, "Estás a " + Math.round(distancia) + " km del avión. Acércate a menos de " + Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM) + " km para avistarlo", Toast.LENGTH_LONG).show();
            return; // Interrumpir ejecución
        }

        Marker m = marcadores.get(icaoSeleccionado); // Obtener marcador actual
        LatLng posAvion = m != null ? m.getPosition() : new LatLng(vueloSeleccionado.latitude, vueloSeleccionado.longitude); // Obtener coordenadas vivas

        String nombre = modeloCompleto(cacheAeronaves.get(icaoSeleccionado)); // Obtener nombre desde la caché técnica
        if (nombre == null) nombre = vueloSeleccionado.descripcion; // Usar descripción básica si no hay ficha técnica

        // Escribir los datos del avistamiento validado en SharedPreferences como pendiente
        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icaoSeleccionado) // Guardar identificador ICAO
                .putString("tipo_seleccionado", tipoSeleccionado) // Guardar tipo de avión
                .putString("matricula_seleccionada", matriculaSeleccionada) // Guardar matrícula de la aeronave
                .putString("descripcion_seleccionada", nombre) // Guardar descripción o modelo
                .putLong("fecha_seleccionada", System.currentTimeMillis()) // Guardar fecha y hora actual
                .putFloat("distancia_seleccionada", distancia) // Guardar distancia registrada
                .putFloat("lat_avion_seleccionado", (float) posAvion.latitude) // Guardar latitud del avión
                .putFloat("lon_avion_seleccionado", (float) posAvion.longitude) // Guardar longitud del avión
                .apply(); // Confirmar escritura asíncrona

        Toast.makeText(this, "Avión avistado a " + Math.round(distancia) + " km. Pulsa + para añadirlo", Toast.LENGTH_LONG).show(); // Informar de éxito
    }

    // Limpia los componentes del mapa y reanuda la descarga de nuevos datos
    private void refrescarMapa() {
        for (Marker m : marcadores.values()) m.remove(); // Eliminar individualmente todos los marcadores del mapa
        marcadores.clear(); // Limpiar el mapa de marcadores de memoria
        estados.clear(); // Limpiar las estructuras de telemetría de movimiento
        borrarTrayectoria(); // Eliminar las líneas de ruta trazadas
        panel.setVisibility(View.GONE); // Ocultar la tarjeta de datos
        ajustarPaddingMapa(false); // Restablecer margen inferior del mapa
        descargar(true); // Forzar la descarga con alertas visibles
    }

    // Modifica los márgenes del mapa para que los controles de Google no queden tapados por el panel
    private void ajustarPaddingMapa(boolean panelVisible) {
        if (mMap == null) return; // Salir si el mapa no ha sido creado
        if (panelVisible) {
            panel.post(() -> { // Esperar al cálculo de dimensiones de la tarjeta
                int margen = (int) (16 * getResources().getDisplayMetrics().density); // Convertir 16dp a píxeles
                mMap.setPadding(0, 0, 0, panel.getHeight() + margen * 2); // Desplazar controles por encima del panel
            });
        } else {
            mMap.setPadding(0, 0, 0, 0); // Restablecer margen nulo
        }
    }

    // ------------------------------
    // COLECCIÓN LOCAL Y DESCARGA API
    // ------------------------------

    // Carga la colección de aviones del usuario almacenada previamente en SharedPreferences
    private void cargarColeccion() {
        miColeccion.clear(); // Vaciar la lista en memoria
        coleccionPorIcao.clear(); // Vaciar el índice por código ICAO

        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE).getString("lista_aviones", null); // Leer JSON
        if (json == null) return; // Salir si no hay colección registrada

        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType()); // Deserializar lista
        if (lista != null) miColeccion.addAll(lista); // Añadir a la colección principal

        for (Avion a : miColeccion) { // Indexar cada avión por su ICAO en minúsculas
            if (a.getIcao24() != null) {
                coleccionPorIcao.put(a.getIcao24().toLowerCase(), a);
            }
        }
    }

    // Busca si un código ICAO específico ya pertenece a la colección de avistados
    private Avion buscarPorIcao(String icao) {
        if (icao == null) return null; // Devolver nulo si el código es inválido
        return coleccionPorIcao.get(icao.toLowerCase()); // Consultar en el índice O(1) de memoria
    }

    // Solicita a la API de tráfico aéreo los vuelos presentes dentro de la ventana visible del mapa
    private void descargar(boolean avisar) {
        if (mMap == null || descargando) return; // Salir si el mapa no está listo o ya hay una petición en curso

        // PROTECCIÓN ANTI-BANEO API: Si es petición manual, exigir intervalo mínimo de 10 segundos
        if (avisar) {
            long ahora = SystemClock.elapsedRealtime(); // Marca de tiempo actual
            if (ahora - ultimaDescargaManual < 10000) { // Si han transcurrido menos de 10 segundos
                Toast.makeText(this, "Espera unos segundos para refrescar", Toast.LENGTH_SHORT).show(); // Notificar cooldown
                return; // Cancelar llamada a la API
            }
            ultimaDescargaManual = ahora; // Actualizar registro del último refresco manual
        }

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds; // Obtener límites de la pantalla activa
        LatLng centro = zona.getCenter(); // Calcular coordenada central

        float[] distancia = new float[1]; // Arreglo para almacenar la distancia en metros
        Location.distanceBetween(centro.latitude, centro.longitude, zona.northeast.latitude, zona.northeast.longitude, distancia); // Calcular radio visible
        int radioNm = (int) Math.ceil(distancia[0] / 1852.0); // Convertir metros a millas náuticas

        if (radioNm > RADIO_MAXIMO_NM) { // Validar que el radio no supere el límite permitido por la API
            if (avisar) {
                Toast.makeText(this, "Acerca el mapa para ver los aviones", Toast.LENGTH_SHORT).show(); // Avisar al usuario
            }
            return; // Cancelar descarga por radio excesivo
        }

        descargando = true; // Marcar inicio del proceso de descarga de red

        // Realizar la llamada HTTP a través de Retrofit
        service.getVuelosEnZona(
                String.format(Locale.US, "%.4f", centro.latitude),
                String.format(Locale.US, "%.4f", centro.longitude),
                radioNm
        ).enqueue(new Callback<AdsbResponse>() {
            @Override
            public void onResponse(Call<AdsbResponse> call, Response<AdsbResponse> response) {
                descargando = false; // Marcar fin del proceso de descarga
                if (isDestroyed() || isFinishing()) return; // Cancelar procesamiento si la actividad se está cerrando

                if (response.code() == 429 && !avisoLimiteMostrado) { // Capturar error por exceso de peticiones (Rate Limit)
                    avisoLimiteMostrado = true; // Marcar bandera para no saturar con mensajes
                    Toast.makeText(MapaActivity.this, "Límite de peticiones de la API alcanzado", Toast.LENGTH_LONG).show();
                }

                if (response.body() == null || response.body().getAviones() == null) return; // Validar cuerpo devuelto

                List<Vuelo> vuelos = new ArrayList<>(); // Lista de vuelos convertidos
                for (AdsbResponse.AdsbAircraft a : response.body().getAviones()) {
                    vuelos.add(a.toVuelo()); // Mapear aeronave devuelta al modelo Vuelo de la app
                }
                procesar(vuelos); // Procesar los vuelos obtenidos para colocarlos en el mapa
            }

            @Override
            public void onFailure(Call<AdsbResponse> call, Throwable t) {
                descargando = false; // Restablecer la bandera de descarga en caso de fallo de red
            }
        });
    }

    // Procesa y actualiza en el mapa la lista de vuelos leídos desde la red
    private void procesar(List<Vuelo> vuelos) {
        Set<String> visibles = new HashSet<>(); // Conjunto de códigos ICAO que deben permanecer en pantalla
        long ahora = SystemClock.elapsedRealtime(); // Registrar marca de tiempo actual

        for (Vuelo api : vuelos) { // Iterar sobre cada vuelo recibido
            if (api.icao24 == null || api.latitude == null || api.longitude == null) continue; // Ignorar registros incompletos

            Avion avistado = buscarPorIcao(api.icao24); // Verificar si la aeronave ya fue avistada
            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue; // Filtrar según el modo activo

            visibles.add(api.icao24); // Marcar la aeronave como visible
            LatLng pos = new LatLng(api.latitude, api.longitude); // Crear coordenada con los nuevos datos

            // Registrar el nuevo estado de telemetría para interpolaciones de movimiento
            estados.put(api.icao24, new EstadoVuelo(
                    api.latitude, api.longitude,
                    api.velocity != null ? api.velocity : 0,
                    api.trueTrack != null ? api.trueTrack : 0,
                    ahora));

            Marker m = marcadores.get(api.icao24); // Buscar el marcador existente en el mapa
            if (m == null) { // Si el marcador no existía previamente
                BitmapDescriptor icono = avistado != null ? iconoSegunRareza(avistado.getRareza()) : iconoGenerico(); // Asignar icono según rareza
                m = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .icon(icono)
                        .rotation(api.trueTrack)
                        .flat(true)
                        .anchor(0.5f, 0.5f)); // Añadir nuevo marcador plano y centrado
                marcadores.put(api.icao24, m); // Registrar el marcador en el mapa de memoria
            } else {
                m.setPosition(pos); // Actualizar la posición del marcador existente
                m.setRotation(api.trueTrack); // Actualizar la rotación del icono según el rumbo
            }

            m.setTag(new Object[]{api, avistado}); // Vincular los datos del vuelo al marcador mediante la propiedad Tag

            if (api.icao24.equals(icaoSeleccionado)) { // Si la aeronave coincide con la seleccionada por el usuario
                vueloSeleccionado = api; // Actualizar datos de vuelo activo
                avistadoSeleccionado = avistado; // Actualizar datos de avión avistado
                if (panel.getVisibility() == View.VISIBLE) pintarPanel(); // Refrescar los textos del panel
            }

            if (api.icao24.equals(icaoSeleccionado) && lineaHistorial != null) { // Si es la aeronave activa y su ruta está dibujada
                puntosTrayectoria.add(pos); // Insertar la posición real en el historial de ruta
                dibujarTrayectoria(); // Redibujar la línea en el mapa
            }
        }

        // Eliminar del mapa los marcadores de aeronaves que salieron del campo de visión o del filtro
        Iterator<Map.Entry<String, Marker>> it = marcadores.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> e = it.next();
            if (!visibles.contains(e.getKey())) {
                e.getValue().remove(); // Remover el marcador visualmente del mapa
                estados.remove(e.getKey()); // Remover la telemetría asociada
                it.remove(); // Remover la clave de la lista en memoria
            }
        }
    }

    // Interpola suavemente la posición de los aviones marcados en pantalla en función del tiempo transcurrido
    private void animarMarcadores() {
        if (mMap == null || marcadores.isEmpty()) return; // Salir si el mapa no está disponible o no hay marcadores

        long ahora = SystemClock.elapsedRealtime(); // Obtener la marca temporal exacta de la ejecución

        // Mover prioritariamente la aeronave seleccionada independientemente del nivel de Zoom
        if (icaoSeleccionado != null && lineaHistorial != null) {
            Marker seleccionado = marcadores.get(icaoSeleccionado); // Obtener marcador del seleccionado
            EstadoVuelo estadoSel = estados.get(icaoSeleccionado); // Obtener telemetría del seleccionado

            if (seleccionado != null && estadoSel != null && estadoSel.velocidad >= 1) {
                LatLng posSel = estadoSel.posicionEn(ahora); // Calcular posición estimada
                seleccionado.setPosition(posSel); // Actualizar coordenada del marcador

                if (ahora - ultimoPuntoTrayectoria >= INTERVALO_PUNTO_TRAYECTORIA_MS) {
                    ultimoPuntoTrayectoria = ahora; // Actualizar timestamp del último punto
                    puntosTrayectoria.add(posSel); // Añadir el nuevo punto a la ruta
                    dibujarTrayectoria(); // Redibujar Polyline
                } else {
                    actualizarTramoActual(posSel); // Ajustar el tramo dinámico con el avión
                }
            }
        }

        if (mMap.getCameraPosition().zoom < ZOOM_MINIMO_ANIMACION) return; // Omitir la animación de aviones lejanos a Zoom bajo

        LatLngBounds zona = mMap.getProjection().getVisibleRegion().latLngBounds; // Obtener marco de visión activa

        for (Map.Entry<String, Marker> e : marcadores.entrySet()) { // Iterar sobre los marcadores
            if (e.getKey().equals(icaoSeleccionado)) continue; // Omitir el ya procesado

            EstadoVuelo estado = estados.get(e.getKey()); // Obtener telemetría del vuelo
            if (estado == null || estado.velocidad < 1) continue; // Ignorar aviones sin velocidad o parados

            LatLng pos = estado.posicionEn(ahora); // Calcular coordenada interpolada
            if (!zona.contains(pos)) continue; // Ignorar aeronaves situadas fuera de la pantalla

            e.getValue().setPosition(pos); // Actualizar la posición del marcador en pantalla
        }
    }

    // ------------------------------
    // LÓGICA DE DIBUJO DE TRAYECTORIAS
    // ------------------------------

    // Inicializa la captura y representación visual de la ruta histórica de un avión seleccionado
    private void mostrarTrayectoria(String icao) {
        borrarTrayectoria(); // Borrar cualquier trazado anterior
        Marker m = marcadores.get(icao); // Obtener el marcador del avión
        if (m != null) puntosTrayectoria.add(m.getPosition()); // Iniciar la lista de puntos con la coordenada actual
        ultimoPuntoTrayectoria = SystemClock.elapsedRealtime(); // Guardar timestamp inicial
        dibujarTrayectoria(); // Dibujar sobre el mapa
    }

    // Actualiza o genera los objetos Polyline que componen la ruta en el mapa
    private void dibujarTrayectoria() {
        if (lineaHistorial == null) lineaHistorial = mMap.addPolyline(estiloLinea()); // Generar línea si no existe
        lineaHistorial.setPoints(new ArrayList<>(puntosTrayectoria)); // Actualizar puntos del historial

        if (lineaActual == null) lineaActual = mMap.addPolyline(estiloLinea()); // Generar tramo dinámico si no existe

        Marker m = icaoSeleccionado != null ? marcadores.get(icaoSeleccionado) : null;
        if (m != null) actualizarTramoActual(m.getPosition()); // Conectar la línea con el icono en movimiento
    }

    // Actualiza el tramo dinámico entre el último punto fijo del historial y el icono en movimiento
    private void actualizarTramoActual(LatLng posActual) {
        if (lineaActual == null || puntosTrayectoria.isEmpty()) return; // Validar la existencia de datos
        LatLng ultimo = puntosTrayectoria.get(puntosTrayectoria.size() - 1); // Extraer el último punto registrado
        lineaActual.setPoints(Arrays.asList(ultimo, posActual)); // Dibujar línea entre el último punto y la posición viva
    }

    // Elimina completamente del mapa las líneas de ruta trazadas
    private void borrarTrayectoria() {
        if (lineaHistorial != null) {
            lineaHistorial.remove(); // Quitar línea del historial del mapa
            lineaHistorial = null; // Liberar referencia
        }
        if (lineaActual != null) {
            lineaActual.remove(); // Quitar tramo dinámico del mapa
            lineaActual = null; // Liberar referencia
        }
        puntosTrayectoria.clear(); // Limpiar la lista de puntos registrados
    }

    // Define la configuración visual (estilo, ancho y color) para las líneas de trayectoria
    private PolylineOptions estiloLinea() {
        return new PolylineOptions()
                .color(Color.parseColor("#1E88E5")) // Asignar color azul para la línea
                .width(8f) // Definir el grosor en 8 píxeles
                .geodesic(false); // Dibujar como líneas planas simples
    }

    // ------------------------------
    // INTERACCIÓN Y PANEL DE INFORMACIÓN
    // ------------------------------

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) { // Evento al pulsar sobre un marcador del mapa
        Object[] data = (Object[]) marker.getTag(); // Extraer el objeto adjunto en el Tag
        if (data == null) return false; // Salir si el marcador no incluye datos

        Vuelo api = (Vuelo) data[0]; // Extraer el objeto de vuelo
        Avion a = (Avion) data[1]; // Extraer el objeto de avión de la colección local

        boolean cambioDeAvion = !api.icao24.equals(icaoSeleccionado) || lineaHistorial == null; // Comprobar si se seleccionó una aeronave nueva
        icaoSeleccionado = api.icao24; // Actualizar código ICAO activo
        tipoSeleccionado = api.tipo; // Actualizar tipo ICAO activo
        matriculaSeleccionada = api.matricula; // Actualizar matrícula activa
        vueloSeleccionado = api; // Actualizar referencia de vuelo activo
        avistadoSeleccionado = a; // Actualizar referencia de avión avistado

        if (cambioDeAvion) mostrarTrayectoria(api.icao24); // Inicializar la ruta si es una nueva selección

        pintarPanel(); // Actualizar componentes visuales del panel
        pedirDetalles(api); // Disparar la búsqueda de detalles técnicos extendidos

        panel.setVisibility(View.VISIBLE); // Mostrar la tarjeta desplegable
        ajustarPaddingMapa(true); // Levantar botones de Google Maps por encima del panel
        btnAvistar.setVisibility(a == null && modoActual == ModoMapa.TODOS ? View.VISIBLE : View.GONE); // Determinar la visibilidad del botón de avistar

        return true; // Devolver verdadero para indicar que el evento fue procesado
    }

    // Solicita de forma independiente la información técnica y la ruta de un vuelo a las APIs secundarias
    private void pedirDetalles(Vuelo api) {
        pedirAeronave(api.icao24); // Solicitar modelo y propietario por el código ICAO
        if (api.callsign != null && !api.callsign.isEmpty()) pedirRuta(api.callsign); // Solicitar origen/destino por el indicativo (Callsign)
    }

    // Realiza la llamada de red a la API de fichas de aeronaves por su ICAO (Mode S)
    private void pedirAeronave(String icao) {
        if (icao == null || cacheAeronaves.containsKey(icao) || aeronavesNoDisponibles.contains(icao)) return; // Omitir si ya existe en caché o se sabe indisponible

        serviceDetalles.getAeronave(icao).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return; // Cancelar si la actividad se está cerrando

                AdsbdbResponse body = response.body(); // Extraer el cuerpo devuelto
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().aircraft == null) {
                    aeronavesNoDisponibles.add(icao); // Marcar como indisponible para omitir futuras consultas
                    return;
                }

                cacheAeronaves.put(icao, body.getRespuesta().aircraft); // Guardar respuesta en la caché de aeronaves
                if (icao.equals(icaoSeleccionado)) pintarPanel(); // Refrescar el panel si la aeronave sigue activa en pantalla
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb aeronave fallo: " + t.getMessage()); // Registrar en consola en caso de fallo
            }
        });
    }

    // Realiza la llamada de red a la API de fichas para obtener la aerolínea y origen/destino de un vuelo
    private void pedirRuta(String indicativo) {
        if (cacheRutas.containsKey(indicativo) || rutasNoDisponibles.contains(indicativo)) return; // Omitir si ya fue procesado

        serviceDetalles.getRuta(indicativo).enqueue(new Callback<AdsbdbResponse>() {
            @Override
            public void onResponse(Call<AdsbdbResponse> call, Response<AdsbdbResponse> response) {
                if (isDestroyed() || isFinishing()) return; // Cancelar si la actividad se está cerrando

                AdsbdbResponse body = response.body(); // Extraer cuerpo devuelto
                if (!response.isSuccessful() || body == null || body.getRespuesta() == null || body.getRespuesta().flightroute == null) {
                    rutasNoDisponibles.add(indicativo); // Marcar como indisponible para omitir futuras consultas
                    return;
                }

                cacheRutas.put(indicativo, body.getRespuesta().flightroute); // Guardar respuesta en la caché de rutas
                if (vueloSeleccionado != null && indicativo.equals(vueloSeleccionado.callsign)) pintarPanel(); // Refrescar el panel si sigue activo
            }

            @Override
            public void onFailure(Call<AdsbdbResponse> call, Throwable t) {
                Log.d("SkyCollector", "adsbdb ruta fallo: " + t.getMessage()); // Registrar error en consola
            }
        });
    }

    // Construye las cadenas de información y refresca el panel sólo si los datos han cambiado para ahorrar CPU
    private void pintarPanel() {
        Vuelo api = vueloSeleccionado; // Obtener vuelo activo
        Avion a = avistadoSeleccionado; // Obtener avión de colección coincidente
        if (api == null) return; // Salir si no hay vuelo activo

        AdsbdbResponse.Aeronave aeronave = cacheAeronaves.get(api.icao24); // Consultar caché de aeronave
        AdsbdbResponse.RutaVuelo ruta = api.callsign != null ? cacheRutas.get(api.callsign) : null; // Consultar caché de ruta

        Avion modeloCatalogo = a == null ? AvionGenerator.getAvionPorTipoIcao(api.tipo) : null; // Consultar catálogo estático por tipo ICAO

        String modelo = modeloCompleto(aeronave); // Determinar la denominación de modelo más precisa disponible
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

        String empresa = null; // Determinar la operadora o fabricante del vuelo
        if (ruta != null && ruta.airline != null && ruta.airline.name != null && !ruta.airline.name.isEmpty()) {
            empresa = ruta.airline.name; // Usar nombre de aerolínea comercial
        } else if (aeronave != null && aeronave.registeredOwner != null && !aeronave.registeredOwner.isEmpty()) {
            empresa = formatearNombre(aeronave.registeredOwner); // Usar propietario registrado
        } else if (a != null) {
            empresa = a.getFabricante(); // Usar fabricante de la colección
        } else if (modeloCatalogo != null) {
            empresa = modeloCatalogo.getFabricante(); // Usar fabricante del catálogo estático
        }

        String nuevoModelo = (empresa != null && !empresa.isEmpty()) ? modelo + " | " + empresa : modelo; // Construir el título combinado del panel

        int vel = api.velocity != null ? (int) (api.velocity * 3.6) : 0; // Convertir velocidad de m/s a km/h
        String rareza = a != null ? a.getRareza() : (modeloCatalogo != null ? modeloCatalogo.getRareza() : null); // Extraer categoría de rareza
        String altitud = api.enTierra ? "En tierra" : (api.altitude != null ? Math.round(api.altitude) + " m" : "—"); // Formatear altitud

        Float distancia = distanciaAlAvionKm(); // Calcular la distancia GPS al usuario
        String lineaDistancia = distancia == null ? "" : "Distancia: " + Math.round(distancia) + " km" + (distancia > DISTANCIA_MAXIMA_AVISTAMIENTO_KM ? " (demasiado lejos para avistarlo)" : "") + "\n";

        String origen = ruta != null ? describirAeropuerto(ruta.origin) : null; // Obtener origen formateado
        String destino = ruta != null ? describirAeropuerto(ruta.destination) : null; // Obtener destino formateado
        String lineaRuta = (origen != null && destino != null) ? "Ruta: " + origen + " → " + destino + "\n" : ""; // Construir línea de ruta completa

        // Construir el bloque completo de detalles técnicos para el panel
        String nuevosDatos = lineaRuta +
                "ICAO: " + api.icao24 + "\n" +
                "Matrícula: " + (api.matricula != null ? api.matricula : "—") + "\n" +
                "Tipo: " + (api.tipo != null ? api.tipo : "—") + "\n" +
                (rareza != null ? "Rareza: " + rareza + "\n" : "") +
                lineaDistancia +
                "Velocidad: " + vel + " km/h\n" +
                "Altitud: " + altitud;

        // Optimización de interfaz: Aplicar el texto al componente sólo si ha variado con respecto al mostrado actualmente
        if (!nuevoModelo.equals(txtModelo.getText().toString())) {
            txtModelo.setText(nuevoModelo);
        }
        if (!nuevosDatos.equals(txtDatos.getText().toString())) {
            txtDatos.setText(nuevosDatos);
        }
    }

    // Normaliza y genera el nombre formal completo del modelo de una aeronave (Ej. Airbus A320-214)
    private String modeloCompleto(AdsbdbResponse.Aeronave aeronave) {
        if (aeronave == null || aeronave.type == null || aeronave.type.trim().isEmpty()) return null; // Validar campos vacíos
        String tipo = aeronave.type.trim().replaceFirst("^(A\\d{3})\\s+(\\d{3}[A-Z]?)$", "$1-$2"); // Formatear designadores Airbus con guiones
        String fabricante = formatearNombre(aeronave.manufacturer); // Limpiar y formatear fabricante
        if (fabricante == null || fabricante.isEmpty() || tipo.toLowerCase().contains(fabricante.toLowerCase())) {
            return tipo; // Evitar duplicar la marca si ya está en el tipo
        }
        return fabricante + " " + tipo; // Devolver fabricante y tipo concatenados
    }

    // Convierte cadenas de texto en MAYÚSCULAS fijas a formato de Capitalización de Título estándar
    private String formatearNombre(String texto) {
        if (texto == null) return null; // Salir si el texto es nulo
        texto = texto.trim(); // Limpiar espacios laterales
        if (texto.isEmpty() || !texto.equals(texto.toUpperCase())) return texto; // Omitir si no viene completamente en mayúsculas

        StringBuilder sb = new StringBuilder(); // Objeto para construir la cadena limpia
        for (String palabra : texto.split(" ")) { // Separar la cadena por palabras
            if (palabra.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (palabra.length() <= 3) {
                sb.append(palabra); // Mantener siglas cortas en mayúsculas (ATR, BAE, etc.)
            } else {
                sb.append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1).toLowerCase()); // Primera mayúscula y resto minúscula
            }
        }
        return sb.toString(); // Devolver la cadena reformateada
    }

    // Devuelve la representación legible de un objeto Aeropuerto agregando su código IATA si existe
    private String describirAeropuerto(AdsbdbResponse.Aeropuerto ap) {
        if (ap == null) return null; // Salir si el objeto es nulo
        String ciudad = (ap.municipality != null && !ap.municipality.isEmpty()) ? ap.municipality : ap.name; // Usar ciudad o nombre
        if (ciudad == null) return ap.iataCode; // Usar únicamente IATA si no hay datos de nombre
        return (ap.iataCode != null && !ap.iataCode.isEmpty()) ? ciudad + " (" + ap.iataCode + ")" : ciudad; // Devolver formato Ciudad (IATA)
    }

    // ------------------------------
    // GENERACIÓN Y CACHÉ DE ICONOS
    // ------------------------------

    // Genera el descriptor del icono por defecto para aviones sin rareza especificada
    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun); // Cargar icono de tipo común
    }

    // Determina y genera el icono de marcador apropiado según la categoría de rareza
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res; // Variable para almacenar el ID del recurso drawable
        switch (rareza != null ? rareza.toUpperCase() : "") { // Evaluar rareza ignorando mayúsculas
            case "RARE": res = R.drawable.avionraro; break; // Icono para raros
            case "EPIC": res = R.drawable.avionepico; break; // Icono para épicos
            case "LEGENDARY":
            case "MYTHIC": res = R.drawable.avionlegendario; break; // Icono para legendarios o míticos
            default: res = R.drawable.avioncomun; // Icono por defecto para comunes
        }
        return iconoDesdeDrawable(res); // Generar y devolver el BitmapDescriptor
    }

    // Convierte un recurso Drawable a un icono BitmapDescriptor para Google Maps reutilizando el resultado en memoria
    private BitmapDescriptor iconoDesdeDrawable(int res) {
        BitmapDescriptor cacheado = cacheIconos.get(res); // Buscar el icono en la caché de memoria
        if (cacheado != null) return cacheado; // Devolver el icono existente sin volver a crearlo

        Drawable d = ContextCompat.getDrawable(this, res); // Obtener el drawable desde los recursos del sistema
        int size = 64; // Definir el tamaño en píxeles del icono en el mapa (64x64)

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); // Crear la imagen Bitmap vacía en memoria
        Canvas c = new Canvas(b); // Inicializar un lienzo asociado al Bitmap
        if (d != null) {
            d.setBounds(0, 0, size, size); // Asignar el tamaño al recurso dibujable
            d.draw(c); // Dibujar el recurso gráfico dentro del lienzo del Bitmap
        }

        BitmapDescriptor icono = BitmapDescriptorFactory.fromBitmap(b); // Convertir el Bitmap en un objeto BitmapDescriptor
        cacheIconos.put(res, icono); // Guardar en la caché para reutilizaciones posteriores
        return icono; // Devolver la referencia al nuevo icono generado
    }
}