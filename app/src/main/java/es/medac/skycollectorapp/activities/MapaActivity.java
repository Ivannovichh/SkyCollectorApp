package es.medac.skycollectorapp.activities; // Paquete en el que se encuentra la clase

// Importaciones de Android necesarias para la actividad
import android.content.Context; // Para acceder a SharedPreferences y otras funciones de contexto
import android.content.SharedPreferences; // Para almacenar y recuperar datos persistentes
import android.graphics.Bitmap; // Para crear imágenes en memoria
import android.graphics.Canvas; // Para dibujar sobre un Bitmap
import android.graphics.drawable.Drawable; // Para manejar imágenes drawable de recursos
import android.os.Bundle; // Para recibir parámetros al crear la actividad
import android.os.Handler; // Para programar tareas en el hilo principal
import android.os.Looper; // Para asociar Handler al hilo principal
import android.view.View; // Para manejar vistas en la interfaz
import android.widget.Button; // Elemento de interfaz tipo botón
import android.widget.TextView; // Elemento de interfaz para mostrar texto
import android.widget.Toast; // Para mostrar mensajes emergentes al usuario

// Importaciones de AndroidX
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
import es.medac.skycollectorapp.models.FlightResponse; // Modelo de respuesta de la API de vuelos
import es.medac.skycollectorapp.network.FlightRadarService; // Interfaz para llamadas HTTP a la API de vuelos
import retrofit2.*; // Clases Retrofit para llamadas HTTP
import retrofit2.converter.gson.GsonConverterFactory; // Conversor de JSON para Retrofit

// Clase principal de la actividad que muestra un mapa con aviones
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener { // Implementa callbacks del mapa y clicks en marcadores

    // Enum para definir los modos del mapa
    private enum ModoMapa { TODOS, AVISTADOS } // Modo TODOS muestra todos los vuelos, AVISTADOS solo los avistados
    private ModoMapa modoActual = ModoMapa.TODOS; // Modo inicial del mapa

    // Objeto GoogleMap para mostrar el mapa
    private GoogleMap mMap;

    // Servicio Retrofit para obtener datos de vuelos
    private FlightRadarService service;

    // Handler para ejecutar tareas en el hilo principal
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Elementos de la interfaz (UI)
    private CardView panel; // Panel que muestra información del avión seleccionado
    private TextView txtModelo, txtDatos; // TextViews para mostrar modelo/fabricante y otros datos
    private Button btnAvistar, btnTodos, btnAvistados; // Botones para acciones y cambio de modo

    // Datos de la aplicación
    private final List<Avion> miColeccion = new ArrayList<>(); // Lista de aviones avistados por el usuario
    private final Map<String, Marker> marcadores = new HashMap<>(); // Mapa que relaciona ICAO con Marker en el mapa
    private String icaoSeleccionado = null; // ICAO del avión seleccionado actualmente

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
                .baseUrl("https://opensky-network.org/api/") // URL base de la API
                .addConverterFactory(GsonConverterFactory.create()) // Convertir JSON automáticamente
                .build();

        service = retrofit.create(FlightRadarService.class); // Crear instancia del servicio

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

        btnAvistar.setOnClickListener(v -> {
            if (icaoSeleccionado == null) { // Validar que un avión esté seleccionado
                Toast.makeText(this,
                        "Selecciona un avión primero",
                        Toast.LENGTH_SHORT).show(); // Mostrar mensaje corto
                return; // Salir si no hay selección
            }

            // Guardar el ICAO seleccionado en SharedPreferences
            SharedPreferences prefs =
                    getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);

            prefs.edit()
                    .putString("icao_seleccionado", icaoSeleccionado)
                    .apply();

            // Notificar al usuario que el avión está seleccionado
            Toast.makeText(this,
                    "Avión seleccionado. Pulsa + para añadirlo",
                    Toast.LENGTH_LONG).show(); // Toast largo
        });
    }

    // Callback cuando el mapa está listo
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // Guardar referencia del mapa
        mMap.setOnMarkerClickListener(this); // Asignar listener de click en marcadores

        // Mover la cámara a una ubicación inicial con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(48.0, 10.0), 5));

        handler.postDelayed(this::descargar, 1000); // Ejecutar método descargar después de 1 segundo
    }

    // Método para refrescar el mapa eliminando marcadores antiguos y descargando nuevos
    private void refrescarMapa() {
        for (Marker m : marcadores.values()) m.remove(); // Eliminar cada marcador existente del mapa
        marcadores.clear(); // Limpiar mapa de marcadores
        panel.setVisibility(View.GONE); // Ocultar panel de información
        descargar(); // Descargar nuevos vuelos
    }

    // Cargar la colección de aviones avistados desde SharedPreferences
    private void cargarColeccion() {
        miColeccion.clear(); // Limpiar lista existente

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
    }

    // Buscar un avión en la colección local por su ICAO
    private Avion buscarPorIcao(String icao) {
        for (Avion a : miColeccion) { // Recorrer cada avión
            if (icao.equalsIgnoreCase(a.getIcao24())) return a; // Retornar avión si coincide
        }
        return null; // Retornar null si no se encuentra
    }

    // Descargar vuelos desde la API usando Retrofit
    private void descargar() {
        service.getVuelosEnZona(35.0, -10.0, 60.0, 30.0) // Coordenadas del área
                .enqueue(new Callback<FlightResponse>() { // Llamada asíncrona
                    @Override
                    public void onResponse(Call<FlightResponse> call,
                                           Response<FlightResponse> response) {
                        if (response.body() == null ||
                                response.body().getStates() == null) return; // Validar datos
                        procesar(response.body().getStates()); // Procesar los estados de vuelos
                    }

                    @Override
                    public void onFailure(Call<FlightResponse> call, Throwable t) {
                        // No se hace nada en caso de fallo
                    }
                });
    }

    // Procesar lista cruda de vuelos recibidos de la API
    private void procesar(List<List<Object>> raw) {
        Set<String> visibles = new HashSet<>(); // Conjunto de ICAO visibles en el mapa

        for (List<Object> r : raw) { // Recorrer cada vuelo
            FlightResponse.OpenSkyAvion api =
                    new FlightResponse.OpenSkyAvion(r); // Crear objeto OpenSkyAvion

            if (api.latitude == null || api.longitude == null) continue; // Ignorar vuelos sin coordenadas

            Avion avistado = buscarPorIcao(api.icao24); // Buscar en colección local

            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue; // Filtrar según modo

            visibles.add(api.icao24); // Agregar ICAO a visibles
            LatLng pos = new LatLng(api.latitude, api.longitude); // Crear LatLng para el marcador

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
        }

        // Eliminar marcadores que ya no están visibles
        Iterator<Map.Entry<String, Marker>> it =
                marcadores.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Marker> e = it.next();
            if (!visibles.contains(e.getKey())) {
                e.getValue().remove(); // Remover del mapa
                it.remove(); // Remover del mapa de marcadores
            }
        }
    }

    // Listener para clicks en marcadores
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object[] data = (Object[]) marker.getTag(); // Obtener datos guardados en tag
        FlightResponse.OpenSkyAvion api =
                (FlightResponse.OpenSkyAvion) data[0]; // Datos del API
        Avion a = (Avion) data[1]; // Avión de colección local (si existe)

        icaoSeleccionado = api.icao24; // Guardar ICAO seleccionado

        int vel = api.velocity != null
                ? (int) (api.velocity * 3.6) // Convertir de m/s a km/h
                : 0; // Velocidad cero si no disponible

        if (a != null) {
            txtModelo.setText(a.getModelo() + " | " + a.getFabricante()); // Mostrar modelo y fabricante
        } else {
            txtModelo.setText("Vuelo " + api.callsign); // Mostrar callsign si no hay avión local
        }

        txtDatos.setText(
                "ICAO: " + api.icao24 + "\n" +
                        "País: " + api.originCountry + "\n" +
                        "Velocidad: " + vel + " km/h\n" +
                        "Altitud: " + api.altitude + " m"
        ); // Mostrar otros datos

        panel.setVisibility(View.VISIBLE); // Mostrar panel de información
        btnAvistar.setVisibility(
                a == null && modoActual == ModoMapa.TODOS
                        ? View.VISIBLE // Mostrar botón solo si no está avistado y modo TODOS
                        : View.GONE
        );

        return true; // Consumir evento click
    }

    // Método para obtener icono genérico de avión
    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun); // Icono de avión común
    }

    // Método para obtener icono según rareza del avión
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res; // Recurso drawable
        switch (rareza.toUpperCase()) { // Comparar rareza ignorando mayúsculas/minúsculas
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun; // Por defecto común
        }
        return iconoDesdeDrawable(res); // Devolver icono como BitmapDescriptor
    }

    // Convertir drawable a BitmapDescriptor para usar como icono en Google Maps
    private BitmapDescriptor iconoDesdeDrawable(int res) {
        Drawable d = ContextCompat.getDrawable(this, res); // Obtener drawable desde recurso
        int size = 64; // Tamaño en píxeles del icono

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); // Crear bitmap en memoria
        Canvas c = new Canvas(b); // Canvas para dibujar sobre el bitmap
        d.setBounds(0, 0, size, size); // Establecer bounds del drawable
        d.draw(c); // Dibujar drawable sobre canvas

        return BitmapDescriptorFactory.fromBitmap(b); // Convertir bitmap a descriptor de icono
    }
}
