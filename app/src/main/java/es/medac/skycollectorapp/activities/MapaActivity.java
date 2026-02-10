// Declaración del paquete donde se encuentra la clase
package es.medac.skycollectorapp.activities;

// Importación para acceder al contexto de la aplicación
import android.content.Context;
// Importación para la gestión de mapas de bits
import android.graphics.Bitmap;
// Importación para realizar dibujos sobre un lienzo
import android.graphics.Canvas;
// Importación para la gestión de recursos gráficos dibujables
import android.graphics.drawable.Drawable;
// Importación para el manejo del estado de la actividad
import android.os.Bundle;
// Importación para la ejecución de tareas programadas
import android.os.Handler;
// Importación para la gestión de la cola de mensajes del hilo principal
import android.os.Looper;

// Importación para marcar parámetros que no deben ser nulos
import androidx.annotation.NonNull;
// Importación base para actividades con soporte de compatibilidad
import androidx.appcompat.app.AppCompatActivity;
// Importación para el contenedor visual con diseño de tarjeta
import androidx.cardview.widget.CardView;
// Importación para obtener recursos de forma compatible
import androidx.core.content.ContextCompat;

// Importación de las utilidades de servicios de mapas de Google
import com.google.android.gms.maps.*;
// Importación de modelos y opciones para elementos del mapa
import com.google.android.gms.maps.model.*;
// Importación de la librería para conversión de datos JSON
import com.google.gson.Gson;
// Importación para manejar tipos de datos genéricos en colecciones
import com.google.gson.reflect.TypeToken;

// Importación para la gestión de propiedades de las vistas
import android.view.View;
// Importación para el componente visual de texto
import android.widget.TextView;

// Importación de utilidades generales de colecciones de Java
import java.util.*;

// Importación de los recursos de la aplicación
import es.medac.skycollectorapp.R;
// Importación del modelo de datos para aviones
import es.medac.skycollectorapp.models.Avion;
// Importación del modelo de respuesta para vuelos
import es.medac.skycollectorapp.models.FlightResponse;
// Importación de la interfaz para el servicio de red
import es.medac.skycollectorapp.network.FlightRadarService;
// Importación de la librería Retrofit para peticiones HTTP
import retrofit2.*;
// Importación del conversor Gson para Retrofit
import retrofit2.converter.gson.GsonConverterFactory;

// Definición de la clase para la actividad del mapa con interfaces de mapas
public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Variable para controlar la instancia del mapa de Google
    private GoogleMap mMap;
    // Variable para el acceso a la interfaz de servicios de red
    private FlightRadarService service;
    // Variable para gestionar hilos y retrasos en el hilo principal
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Variable para el panel de información visual
    private CardView panel;
    // Variables para los campos de texto del panel
    private TextView txtModelo, txtDatos;

    // Lista dinámica que almacena la colección de aviones del usuario
    private final List<Avion> miColeccion = new ArrayList<>();

    // Diccionario para rastrear los marcadores activos por su identificador
    private final Map<String, Marker> marcadores = new HashMap<>();

    // Método que inicializa la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de creación de la clase superior
        super.onCreate(savedInstanceState);
        // Establecimiento del diseño visual de la actividad
        setContentView(R.layout.activity_mapa);

        // Vinculación del panel de información desde el diseño
        panel = findViewById(R.id.card_info_vuelo);
        // Vinculación del texto del modelo desde el diseño
        txtModelo = findViewById(R.id.txt_modelo_panel);
        // Vinculación del texto de datos desde el diseño
        txtDatos = findViewById(R.id.txt_datos_panel);

        // Llamada al método para cargar los datos del usuario
        cargarColeccion();

        // Configuración del cliente de red Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opensky-network.org/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Creación de la implementación del servicio de red
        service = retrofit.create(FlightRadarService.class);

        // Obtención del fragmento del mapa desde el gestor de fragmentos
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        // Solicitud asíncrona para inicializar el mapa
        mapFragment.getMapAsync(this);
    }

    // Método que se activa cuando el mapa está listo para usarse
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Asignación de la instancia del mapa recibida
        mMap = googleMap;
        // Registro del escucha para eventos de clic en marcadores
        mMap.setOnMarkerClickListener(this);

        // Posicionamiento inicial de la cámara sobre coordenadas específicas
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(48.0, 10.0), 5));

        // Programación de la primera descarga con un pequeño retraso
        handler.postDelayed(this::descargar, 1000);
    }

    // Método para recuperar la lista de aviones desde el almacenamiento
    private void cargarColeccion() {
        // Limpieza de la lista actual en memoria
        miColeccion.clear();

        // Recuperación de la cadena JSON de las preferencias compartidas
        String json = getSharedPreferences(
                "SkyCollectorDatos",
                Context.MODE_PRIVATE
        ).getString("lista_aviones", null);

        // Finalización si no existen datos guardados
        if (json == null) return;

        // Conversión del formato JSON a una lista de objetos de tipo Avion
        List<Avion> lista = new Gson().fromJson(
                json,
                new TypeToken<ArrayList<Avion>>() {}.getType()
        );

        // Adición de todos los elementos recuperados a la colección
        if (lista != null) miColeccion.addAll(lista);
    }

    // Método para solicitar los datos de vuelos al servidor externo
    private void descargar() {
        // Ejecución de la petición filtrando por coordenadas geográficas
        service.getVuelosEnZona(35.0, -10.0, 60.0, 30.0)
                .enqueue(new Callback<FlightResponse>() {
                    // Gestión de la respuesta exitosa del servidor
                    @Override
                    public void onResponse(Call<FlightResponse> call,
                                           Response<FlightResponse> response) {

                        // Validación de que el cuerpo de la respuesta contenga datos
                        if (response.body() == null ||
                                response.body().getStates() == null) return;

                        // Procesamiento de los estados de vuelo recibidos
                        procesar(response.body().getStates());
                    }

                    // Gestión del fallo en la comunicación de red
                    @Override
                    public void onFailure(Call<FlightResponse> call, Throwable t) {}
                });
    }

    // Método para transformar los datos crudos en elementos visuales del mapa
    private void procesar(List<List<Object>> raw) {
        // Conjunto para rastrear qué aviones permanecen visibles en esta actualización
        Set<String> visibles = new HashSet<>();

        // Recorrido de cada registro de avión recibido de la API
        for (List<Object> r : raw) {
            // Transformación de la lista de objetos en un modelo estructurado
            FlightResponse.OpenSkyAvion api =
                    new FlightResponse.OpenSkyAvion(r);

            // Filtrado de registros que carecen de datos esenciales
            if (api.latitude == null || api.longitude == null ||
                    api.callsign == null) continue;

            // Verificación de si el avión de la API pertenece a la colección del usuario
            Avion match = buscarEnColeccion(api.callsign);
            // Salto al siguiente si el avión no es de interés
            if (match == null) continue;

            // Registro del identificador como avión presente
            visibles.add(api.icao24);

            // Creación de la posición geográfica para el marcador
            LatLng pos = new LatLng(api.latitude, api.longitude);

            // Búsqueda de un marcador existente para este avión
            Marker m = marcadores.get(api.icao24);
            // Lógica para crear un marcador nuevo si no existía
            if (m == null) {
                // Configuración y adición del nuevo marcador al mapa
                m = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .icon(iconoSegunRareza(match.getRareza()))
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                        .rotation(api.trueTrack)
                );
                // Almacenamiento del marcador en el mapa de control
                marcadores.put(api.icao24, m);
            } else {
                // Actualización de la posición del marcador existente
                m.setPosition(pos);
                // Actualización de la rotación según el rumbo real
                m.setRotation(api.trueTrack);
            }

            // Almacenamiento de metadatos en el marcador para su recuperación posterior
            m.setTag(new Object[]{match, api});
        }

        // Obtención de un iterador para limpiar marcadores obsoletos
        Iterator<Map.Entry<String, Marker>> it =
                marcadores.entrySet().iterator();

        // Recorrido de los marcadores registrados actualmente
        while (it.hasNext()) {
            // Obtención de la entrada del marcador
            Map.Entry<String, Marker> e = it.next();
            // Verificación de si el marcador ya no está en los datos nuevos
            if (!visibles.contains(e.getKey())) {
                // Eliminación física del marcador del mapa
                e.getValue().remove();
                // Eliminación del marcador del diccionario de control
                it.remove();
            }
        }
    }

    // Método para comparar códigos de vuelo con los modelos de la colección
    private Avion buscarEnColeccion(String callsign) {
        // Normalización del código de vuelo a mayúsculas
        String cs = callsign.toUpperCase();

        // Bucle de búsqueda a través de la colección cargada
        for (Avion a : miColeccion) {
            // Normalización del nombre del modelo para la comparación
            String modelo = a.getModelo().toUpperCase();

            // Lógica de coincidencia basada en prefijos de aerolíneas comunes
            if (modelo.contains("737") && cs.startsWith("RYR")) return a;
            if (modelo.contains("A320") && cs.startsWith("VLG")) return a;
            if (modelo.contains("A320") && cs.startsWith("IBE")) return a;
            if (modelo.contains("A320") && cs.startsWith("EZY")) return a;
        }
        // Retorno de nulo si no hay coincidencias encontradas
        return null;
    }

    // Método que gestiona la interacción al tocar un avión en el mapa
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // Recuperación de los datos asociados al marcador tocado
        Object[] data = (Object[]) marker.getTag();
        // Extracción del objeto Avion de los datos
        Avion a = (Avion) data[0];
        // Extracción de los datos en tiempo real de la API
        FlightResponse.OpenSkyAvion api =
                (FlightResponse.OpenSkyAvion) data[1];

        // Cálculo de la velocidad convirtiendo de metros por segundo a km/h
        int vel = api.velocity != null
                ? (int) (api.velocity * 3.6)
                : 0;

        // Actualización del título del panel con el modelo y fabricante
        txtModelo.setText(a.getModelo() + " | " + a.getFabricante());

        // Formateo y actualización de la descripción detallada en el panel
        txtDatos.setText(
                "ICAO: " + api.icao24 + "\n" +
                        "Vuelo: " + api.callsign + "\n" +
                        "País: " + api.originCountry + "\n" +
                        "Velocidad: " + vel + " km/h\n" +
                        "Altitud: " + api.altitude + " m\n" +
                        "Rareza: " + a.getRareza()
        );

        // Activación de la visibilidad del panel informativo
        panel.setVisibility(View.VISIBLE);
        // Indicación de que el evento ha sido gestionado
        return true;
    }

    // Método para generar el gráfico del marcador basado en la rareza del avión
    private BitmapDescriptor iconoSegunRareza(String rareza) {
        // Variable para almacenar el identificador del recurso gráfico
        int res;
        // Selección del recurso según la categoría de rareza
        switch (rareza.toUpperCase()) {
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun;
        }

        // Obtención del objeto gráfico desde los recursos
        Drawable d = ContextCompat.getDrawable(this, res);
        // Definición del tamaño del icono en píxeles
        int size = 64;

        // Creación de una imagen vacía en memoria con el tamaño deseado
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        // Creación del lienzo para dibujar sobre la imagen
        Canvas c = new Canvas(b);
        // Ajuste de los límites del dibujo gráfico
        d.setBounds(0, 0, size, size);
        // Ejecución del dibujo sobre el lienzo
        d.draw(c);

        // Conversión del mapa de bits resultante en un descriptor para el mapa
        return BitmapDescriptorFactory.fromBitmap(b);
    }
}