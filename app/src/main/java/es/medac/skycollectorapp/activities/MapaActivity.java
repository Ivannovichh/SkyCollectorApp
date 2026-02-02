package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.models.FlightResponse;
import es.medac.skycollectorapp.network.FlightRadarService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapaActivity";
    private GoogleMap mMap;
    private Handler handler;
    private Runnable runnableCode;

    // Elementos del panel
    private CardView cardInfoPanel;
    private TextView txtModeloPanel, txtDatosPanel;

    // Datos
    private List<String> misNombresDeModelos = new ArrayList<>();
    private Map<String, String> mapaRarezas = new HashMap<>();
    private Map<String, List<LatLng>> historialLocal = new HashMap<>();
    private Polyline trayectoriaActualPolyline;
    private String idAvionSeleccionado = null;
    private FlightRadarService service;
    private Map<String, BitmapDescriptor> cacheIconos = new HashMap<>();
    private boolean isActualizando = false;
    private static final int MAX_AVIONES_EN_MAPA = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        cardInfoPanel = findViewById(R.id.card_info_vuelo);
        txtModeloPanel = findViewById(R.id.txt_modelo_panel);
        txtDatosPanel = findViewById(R.id.txt_datos_panel);

        cargarDatosDeUsuario();

        // Configurar Retrofit para la API
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://opensky-network.org/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(FlightRadarService.class);
        } catch (Exception e) { Log.e(TAG, "Error Retrofit: " + e.getMessage()); }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        handler = new Handler(Looper.getMainLooper());
    }

    // Carga tus aviones para saber qué modelos pintar (simulación visual)
    private void cargarDatosDeUsuario() {
        try {
            SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
            String json = prefs.getString("lista_aviones", null);
            if (json != null) {
                List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>() {}.getType());
                if (lista != null) {
                    for (Avion a : lista) {
                        if (a.getModelo() != null) {
                            misNombresDeModelos.add(a.getModelo());
                            mapaRarezas.put(a.getModelo().toLowerCase(), (a.getRareza() != null ? a.getRareza() : "Comun"));
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) return;

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setOnMarkerClickListener(this);

        // Cerrar panel al tocar el mapa vacío
        mMap.setOnMapClickListener(latLng -> {
            if (cardInfoPanel != null) cardInfoPanel.setVisibility(View.GONE);
            if (trayectoriaActualPolyline != null) {
                trayectoriaActualPolyline.remove();
                trayectoriaActualPolyline = null;
            }
            idAvionSeleccionado = null;
        });

        preCargarIconos();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4167, -3.7037), 5)); // Madrid

        iniciarCicloDeActualizacion();
    }

    // --- AQUÍ ES DONDE MOSTRAMOS LOS DATOS REALES DE LA API ---
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        FlightResponse.OpenSkyAvion vuelo = (FlightResponse.OpenSkyAvion) marker.getTag();
        if (vuelo == null) return false;

        idAvionSeleccionado = vuelo.icao24;

        // 1. Texto del Panel
        // Usamos el ICAO24 real de la API y el Callsign real de la API.
        // NO inventamos matrículas.
        String modeloSimulado = determinarModeloSimulado(vuelo.icao24); // Solo el modelo se simula porque la API gratis no lo da.

        txtModeloPanel.setText(modeloSimulado);

        // DATOS REALES DIRECTOS DE LA API
        int velocidadKmh = (vuelo.velocity != null) ? (int)(vuelo.velocity * 3.6) : 0;

        txtDatosPanel.setText(
                "ID ICAO: " + vuelo.icao24 + "\n" +    // Matrícula técnica real
                        "Vuelo: " + vuelo.callsign + "\n" +    // Código de vuelo real
                        "País: " + vuelo.originCountry + "\n" +
                        "Alt: " + vuelo.altitude + "m | Vel: " + velocidadKmh + " km/h"
        );

        cardInfoPanel.setVisibility(View.VISIBLE);

        // 2. Dibujar Trayectoria Real (API)
        dibujarTrayectoria(vuelo.icao24);

        return true;
    }

    private void dibujarTrayectoria(String icao24) {
        if (trayectoriaActualPolyline != null) trayectoriaActualPolyline.remove();

        service.getTrayectoria(icao24, 0).enqueue(new Callback<es.medac.skycollectorapp.TrackResponse>() {
            @Override
            public void onResponse(Call<es.medac.skycollectorapp.TrackResponse> call, Response<es.medac.skycollectorapp.TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().path != null) {
                    List<LatLng> ruta = new ArrayList<>();
                    for (List<Object> p : response.body().path) {
                        ruta.add(new LatLng(((Number)p.get(1)).doubleValue(), ((Number)p.get(2)).doubleValue()));
                    }
                    if (mMap != null) {
                        PolylineOptions opt = new PolylineOptions().addAll(ruta).color(Color.RED).width(8f);
                        trayectoriaActualPolyline = mMap.addPolyline(opt);
                    }
                }
            }
            @Override public void onFailure(Call<es.medac.skycollectorapp.TrackResponse> call, Throwable t) {}
        });
    }

    // Simulación visual del modelo (necesaria porque OpenSky free no dice si es un Boeing o Airbus)
    private String determinarModeloSimulado(String icao24) {
        if (icao24 == null || misNombresDeModelos.isEmpty()) return "Avión Desconocido";
        int index = Math.abs(icao24.hashCode()) % misNombresDeModelos.size();
        return misNombresDeModelos.get(index);
    }

    private void iniciarCicloDeActualizacion() {
        runnableCode = new Runnable() {
            @Override public void run() {
                if (!isFinishing()) {
                    descargarVuelos();
                    handler.postDelayed(this, 10000); // Cada 10s
                }
            }
        };
        handler.post(runnableCode);
    }

    private void descargarVuelos() {
        if (isActualizando) return;
        isActualizando = true;

        // Zona geográfica (Europa/España aprox)
        service.getVuelosEnZona(35.0, -15.0, 58.0, 30.0).enqueue(new Callback<FlightResponse>() {
            @Override
            public void onResponse(Call<FlightResponse> call, Response<FlightResponse> response) {
                isActualizando = false;
                if (response.isSuccessful() && response.body() != null) {
                    procesarVuelos(response.body().getStates());
                }
            }
            @Override public void onFailure(Call<FlightResponse> call, Throwable t) { isActualizando = false; }
        });
    }

    private void procesarVuelos(List<List<Object>> rawStates) {
        if (rawStates == null) return;
        mMap.clear(); // Limpiamos mapa para repintar

        int cont = 0;
        for (List<Object> state : rawStates) {
            if (cont >= MAX_AVIONES_EN_MAPA) break;
            try {
                FlightResponse.OpenSkyAvion avionAPI = new FlightResponse.OpenSkyAvion(state);

                if (avionAPI.latitude != 0.0 && avionAPI.longitude != 0.0) {
                    // Elegir icono según el modelo simulado
                    String modelo = determinarModeloSimulado(avionAPI.icao24);
                    String rareza = mapaRarezas.get(modelo.toLowerCase());
                    BitmapDescriptor icono = cacheIconos.get(rareza != null ? rareza.toLowerCase() : "comun");

                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(avionAPI.latitude, avionAPI.longitude))
                            .icon(icono != null ? icono : BitmapDescriptorFactory.defaultMarker())
                            .rotation(avionAPI.trueTrack)
                            .anchor(0.5f, 0.5f)
                            .flat(true));
                    m.setTag(avionAPI); // Guardamos el objeto real dentro del marcador
                    cont++;
                }
            } catch (Exception e) {}
        }

        // Si teníamos un avión seleccionado, intentamos redibujar su línea si sigue en el mapa
        // (Lógica simplificada para no complicar el código)
    }

    private void preCargarIconos() {
        if (!cacheIconos.isEmpty()) return;
        cacheIconos.put("comun", bitmapDescriptorFromVector(this, R.drawable.avioncomun));
        cacheIconos.put("raro", bitmapDescriptorFromVector(this, R.drawable.avionraro));
        cacheIconos.put("epico", bitmapDescriptorFromVector(this, R.drawable.avionepico));
        cacheIconos.put("legendario", bitmapDescriptorFromVector(this, R.drawable.avionlegendario));
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
            if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth()/2, vectorDrawable.getIntrinsicHeight()/2);
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getBounds().width(), vectorDrawable.getBounds().height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) { return BitmapDescriptorFactory.defaultMarker(); }
    }
}