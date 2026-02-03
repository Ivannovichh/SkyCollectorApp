package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.FlightResponse;
import es.medac.skycollectorapp.network.FlightRadarService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Handler handler;
    private Runnable runnableCode;

    private CardView cardInfoPanel;
    private TextView txtModeloPanel, txtDatosPanel;

    private Polyline trayectoriaActualPolyline;
    private String idAvionSeleccionado = null;
    private FlightRadarService service;
    private Map<String, BitmapDescriptor> cacheIconos = new HashMap<>();
    private boolean isActualizando = false;
    private static final int MAX_AVIONES_EN_MAPA = 200; // Masiva+
    private Map<String, Marker> marcadoresPorAvion = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        cardInfoPanel = findViewById(R.id.card_info_vuelo);
        txtModeloPanel = findViewById(R.id.txt_modelo_panel);
        txtDatosPanel = findViewById(R.id.txt_datos_panel);

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://opensky-network.org/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(FlightRadarService.class);
        } catch (Exception e) {}

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) return;

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setOnMarkerClickListener(this);

        mMap.setOnMapClickListener(latLng -> {
            if (cardInfoPanel != null) cardInfoPanel.setVisibility(android.view.View.GONE);
            if (trayectoriaActualPolyline != null) {
                trayectoriaActualPolyline.remove();
                trayectoriaActualPolyline = null;
            }
            idAvionSeleccionado = null;
        });

        preCargarIconos();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4167, -3.7037), 5));

        iniciarCicloDeActualizacion();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        FlightResponse.OpenSkyAvion vuelo = (FlightResponse.OpenSkyAvion) marker.getTag();
        if (vuelo == null) return false;

        idAvionSeleccionado = vuelo.icao24;

        txtModeloPanel.setText(vuelo.callsign != null ? vuelo.callsign : "Desconocido");

        int velocidadKmh = (vuelo.velocity != null) ? (int) (vuelo.velocity * 3.6) : 0;
        txtDatosPanel.setText(
                "ID ICAO: " + vuelo.icao24 + "\n" +
                        "Vuelo: " + vuelo.callsign + "\n" +
                        "País: " + vuelo.originCountry + "\n" +
                        "Alt: " + vuelo.altitude + "m | Vel: " + velocidadKmh + " km/h"
        );

        cardInfoPanel.setVisibility(android.view.View.VISIBLE);
        return true;
    }

    private void iniciarCicloDeActualizacion() {
        runnableCode = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    descargarVuelos();
                    handler.postDelayed(this, 10000);
                }
            }
        };
        handler.post(runnableCode);
    }

    private void descargarVuelos() {
        if (isActualizando) return;
        isActualizando = true;

        service.getVuelosEnZona(35.0, -15.0, 58.0, 30.0).enqueue(new Callback<FlightResponse>() {
            @Override
            public void onResponse(Call<FlightResponse> call, Response<FlightResponse> response) {
                isActualizando = false;
                if (response.isSuccessful() && response.body() != null) {
                    procesarVuelos(response.body().getStates());
                }
            }

            @Override
            public void onFailure(Call<FlightResponse> call, Throwable t) {
                isActualizando = false;
            }
        });
    }

    private void procesarVuelos(List<List<Object>> rawStates) {
        if (rawStates == null) return;

        int cont = 0;
        Set<String> icosActuales = new HashSet<>();

        for (List<Object> state : rawStates) {
            if (cont >= MAX_AVIONES_EN_MAPA) break;

            FlightResponse.OpenSkyAvion avionAPI = new FlightResponse.OpenSkyAvion(state);
            if (avionAPI.latitude == null || avionAPI.longitude == null || avionAPI.icao24 == null) continue;

            icosActuales.add(avionAPI.icao24);
            LatLng nuevaPos = new LatLng(avionAPI.latitude, avionAPI.longitude);

            Marker marcadorExistente = marcadoresPorAvion.get(avionAPI.icao24);
            BitmapDescriptor icono = cacheIconos.getOrDefault("comun", BitmapDescriptorFactory.defaultMarker());

            if (marcadorExistente != null) {
                marcadorExistente.setPosition(nuevaPos);
                marcadorExistente.setRotation(avionAPI.trueTrack);
                marcadorExistente.setTag(avionAPI);
            } else {
                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(nuevaPos)
                        .icon(icono)
                        .rotation(avionAPI.trueTrack)
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                );
                m.setTag(avionAPI);
                marcadoresPorAvion.put(avionAPI.icao24, m);
            }

            cont++;
        }

        // Eliminar marcadores desaparecidos
        Iterator<Map.Entry<String, Marker>> it = marcadoresPorAvion.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> entry = it.next();
            if (!icosActuales.contains(entry.getKey())) {
                entry.getValue().remove();
                it.remove();
            }
        }
    }

    private void preCargarIconos() {
        if (!cacheIconos.isEmpty()) return;
        cacheIconos.put("comun", bitmapDescriptorFromVector(this, R.drawable.avioncomun));
        cacheIconos.put("raro", bitmapDescriptorFromVector(this, R.drawable.avionraro));
        cacheIconos.put("epico", bitmapDescriptorFromVector(this, R.drawable.avionepico));
        cacheIconos.put("legendario", bitmapDescriptorFromVector(this, R.drawable.avionlegendario));
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth()/2, vectorDrawable.getIntrinsicHeight()/2);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getBounds().width(), vectorDrawable.getBounds().height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
