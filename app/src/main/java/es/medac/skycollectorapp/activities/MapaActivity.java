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
import android.widget.Toast;

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

    private static final String TAG = "EcoCityMap";
    private GoogleMap mMap;
    private Handler handler;
    private Runnable runnableCode;

    // --- ELEMENTOS DE LA INTERFAZ (NUEVO PANEL) ---
    private CardView cardInfoPanel;
    private TextView txtModeloPanel, txtDatosPanel;

    // --- VARIABLES DE DATOS ---
    private List<String> misNombresDeModelos = new ArrayList<>();
    private Map<String, String> mapaRarezas = new HashMap<>();

    // Variables de estado
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

        // 1. Enlazar los elementos del nuevo panel inferior
        cardInfoPanel = findViewById(R.id.card_info_vuelo);
        txtModeloPanel = findViewById(R.id.txt_modelo_panel);
        txtDatosPanel = findViewById(R.id.txt_datos_panel);

        cargarDatosDeUsuario();

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://opensky-network.org/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(FlightRadarService.class);
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando Retrofit: " + e.getMessage());
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        handler = new Handler(Looper.getMainLooper());
    }

    private void cargarDatosDeUsuario() {
        try {
            SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
            String json = prefs.getString("lista_aviones", null);
            misNombresDeModelos.clear();
            mapaRarezas.clear();

            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
                List<Avion> lista = gson.fromJson(json, type);
                if (lista != null) {
                    for (Avion a : lista) {
                        if (a.getModelo() != null) {
                            misNombresDeModelos.add(a.getModelo());
                            String rareza = (a.getRareza() != null) ? a.getRareza() : "Comun";
                            mapaRarezas.put(a.getModelo().toLowerCase(), rareza);
                        }
                    }
                }
            }
        } catch (Exception e) { Log.e(TAG, "Error datos: " + e.getMessage()); }
    }

    private void preCargarIconos() {
        if (!cacheIconos.isEmpty()) return;
        try {
            cacheIconos.put("comun", bitmapDescriptorFromVector(this, R.drawable.avioncomun));
            cacheIconos.put("raro", bitmapDescriptorFromVector(this, R.drawable.avionraro));
            cacheIconos.put("epico", bitmapDescriptorFromVector(this, R.drawable.avionepico));
            cacheIconos.put("legendario", bitmapDescriptorFromVector(this, R.drawable.avionlegendario));

            cacheIconos.put("rare", cacheIconos.get("raro"));
            cacheIconos.put("epic", cacheIconos.get("epico"));
            cacheIconos.put("legendary", cacheIconos.get("legendario"));
            cacheIconos.put("common", cacheIconos.get("comun"));
        } catch (Exception e) {}
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) return;

        try {
            mMap.getUiSettings().setZoomControlsEnabled(false); // Quitamos botones zoom para limpiar pantalla
            mMap.getUiSettings().setRotateGesturesEnabled(false);

            // YA NO USAMOS CustomInfoWindowAdapter PORQUE TAPABA EL AVIÓN
            // mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

            mMap.setOnMarkerClickListener(this);

            // NUEVO: Si tocas el mapa (no un avión), se cierra el panel
            mMap.setOnMapClickListener(latLng -> {
                if (cardInfoPanel != null) {
                    cardInfoPanel.setVisibility(View.GONE);
                }
                if (trayectoriaActualPolyline != null) {
                    trayectoriaActualPolyline.remove();
                    trayectoriaActualPolyline = null;
                }
                idAvionSeleccionado = null;
            });

            preCargarIconos();

            LatLng centro = new LatLng(40.4167, -3.7037);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 5));

            iniciarCicloDeActualizacion();
        } catch (Exception e) { Log.e(TAG, "Error mapa: " + e.getMessage()); }
    }

    private String determinarModeloSimulado(String icao24) {
        if (icao24 == null) return "Desconocido";
        if (misNombresDeModelos.isEmpty()) return "Sin Colección";
        int hash = Math.abs(icao24.hashCode());
        int indice = hash % misNombresDeModelos.size();
        return misNombresDeModelos.get(indice);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        try {
            FlightResponse.OpenSkyAvion vuelo = (FlightResponse.OpenSkyAvion) marker.getTag();
            if (vuelo == null) return false;

            idAvionSeleccionado = vuelo.icao24;

            // 1. ACTUALIZAR PANEL INFERIOR
            if (cardInfoPanel != null) {
                String modelo = determinarModeloSimulado(vuelo.icao24);
                int kph = (vuelo.velocity != null) ? (int)(vuelo.velocity * 3.6) : 0;

                txtModeloPanel.setText(modelo);
                txtDatosPanel.setText(
                        "Matrícula: " + vuelo.icao24 + "\n" +
                                "País: " + vuelo.originCountry + "\n" +
                                "Altitud: " + vuelo.altitude + " m  |  Velocidad: " + kph + " km/h"
                );

                // Hacemos visible la tarjeta
                cardInfoPanel.setVisibility(View.VISIBLE);
            }

            // 2. DIBUJAR TRAYECTORIA
            if (trayectoriaActualPolyline != null) trayectoriaActualPolyline.remove();

            // Llamada API Trayectoria
            service.getTrayectoria(vuelo.icao24, 0).enqueue(new Callback<es.medac.skycollectorapp.TrackResponse>() {
                @Override
                public void onResponse(Call<es.medac.skycollectorapp.TrackResponse> call, Response<es.medac.skycollectorapp.TrackResponse> response) {
                    if (isDestroyed() || isFinishing()) return;

                    if (response.isSuccessful() && response.body() != null && response.body().path != null) {
                        List<LatLng> rutaAPI = new ArrayList<>();
                        for (List<Object> punto : response.body().path) {
                            try {
                                double lat = ((Number) punto.get(1)).doubleValue();
                                double lon = ((Number) punto.get(2)).doubleValue();
                                rutaAPI.add(new LatLng(lat, lon));
                            } catch (Exception e) {}
                        }

                        runOnUiThread(() -> {
                            try {
                                if (mMap != null && idAvionSeleccionado != null && idAvionSeleccionado.equals(vuelo.icao24)) {
                                    PolylineOptions opt = new PolylineOptions().addAll(rutaAPI).color(Color.RED).width(8f);
                                    trayectoriaActualPolyline = mMap.addPolyline(opt);
                                }
                            } catch (Exception e) {}
                        });
                    } else {
                        usarHistorialLocal(vuelo.icao24);
                    }
                }
                @Override public void onFailure(Call<es.medac.skycollectorapp.TrackResponse> call, Throwable t) {
                    if (!isDestroyed() && !isFinishing()) usarHistorialLocal(vuelo.icao24);
                }
            });

            // NO llamamos a showInfoWindow() para que no salga la burbuja
            return true; // Devolvemos true para indicar que "ya hemos manejado el clic" (no centrar ni abrir burbuja)

        } catch (Exception e) { Log.e(TAG, "Error click: " + e.getMessage()); }
        return false;
    }

    private void usarHistorialLocal(String icao24) {
        if (mMap == null) return;
        List<LatLng> puntos = historialLocal.get(icao24);
        if (puntos != null && puntos.size() > 1) {
            try {
                PolylineOptions opt = new PolylineOptions().addAll(puntos).color(Color.BLUE).width(8f);
                trayectoriaActualPolyline = mMap.addPolyline(opt);
            } catch (Exception e) {}
        }
    }

    private void iniciarCicloDeActualizacion() {
        runnableCode = new Runnable() {
            @Override public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    descargarVuelosYFiltrar();
                    handler.postDelayed(this, 10000);
                }
            }
        };
        handler.post(runnableCode);
    }

    private void descargarVuelosYFiltrar() {
        if (isActualizando) return;
        isActualizando = true;

        service.getVuelosEnZona(35.0, -15.0, 58.0, 30.0).enqueue(new Callback<FlightResponse>() {
            @Override
            public void onResponse(Call<FlightResponse> call, Response<FlightResponse> response) {
                isActualizando = false;
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getStates() != null) {
                    List<List<Object>> rawStates = response.body().getStates();
                    new Thread(() -> {
                        List<FlightResponse.OpenSkyAvion> vuelosProcesados = new ArrayList<>();
                        int encontrados = 0;
                        if (misNombresDeModelos.isEmpty()) return;

                        for (List<Object> state : rawStates) {
                            if (encontrados >= MAX_AVIONES_EN_MAPA) break;
                            try {
                                FlightResponse.OpenSkyAvion vuelo = new FlightResponse.OpenSkyAvion(state);
                                if (vuelo.latitude != 0.0 && vuelo.longitude != 0.0) {
                                    vuelosProcesados.add(vuelo);
                                    encontrados++;
                                }
                            } catch (Exception e) {}
                        }

                        runOnUiThread(() -> {
                            if (mMap == null || isFinishing() || isDestroyed()) return;
                            try {
                                for (FlightResponse.OpenSkyAvion v : vuelosProcesados) {
                                    LatLng pos = new LatLng(v.latitude, v.longitude);
                                    if (!historialLocal.containsKey(v.icao24)) historialLocal.put(v.icao24, new ArrayList<>());
                                    historialLocal.get(v.icao24).add(pos);
                                }

                                mMap.clear();
                                if (idAvionSeleccionado != null) trayectoriaActualPolyline = null;

                                for (FlightResponse.OpenSkyAvion v : vuelosProcesados) {
                                    String modelo = determinarModeloSimulado(v.icao24);
                                    String rareza = mapaRarezas.get(modelo.toLowerCase());
                                    if (rareza == null) rareza = "comun";

                                    BitmapDescriptor icono = cacheIconos.get(rareza.toLowerCase());
                                    if (icono == null) icono = BitmapDescriptorFactory.defaultMarker();

                                    Marker m = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(v.latitude, v.longitude))
                                            .icon(icono)
                                            .rotation(v.trueTrack)
                                            .anchor(0.5f, 0.5f)
                                            .flat(true));
                                    m.setTag(v);

                                    // Si hay un avión seleccionado, actualizamos su info en el panel si sigue existiendo
                                    if (v.icao24.equals(idAvionSeleccionado) && cardInfoPanel.getVisibility() == View.VISIBLE) {
                                        int kph = (v.velocity != null) ? (int)(v.velocity * 3.6) : 0;
                                        txtDatosPanel.setText("Matrícula: " + v.icao24 + "\nPaís: " + v.originCountry + "\nAltitud: " + v.altitude + " m  |  Velocidad: " + kph + " km/h");
                                    }
                                }
                            } catch (Exception e) {}
                        });
                    }).start();
                }
            }
            @Override public void onFailure(Call<FlightResponse> call, Throwable t) { isActualizando = false; }
        });
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
            if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();
            int w = vectorDrawable.getIntrinsicWidth();
            int h = vectorDrawable.getIntrinsicHeight();
            if (w > 100) { w = w/2; h = h/2; } // Reducción de tamaño
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) { return BitmapDescriptorFactory.defaultMarker(); }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnableCode != null) handler.removeCallbacks(runnableCode);
        mMap = null;
    }
}