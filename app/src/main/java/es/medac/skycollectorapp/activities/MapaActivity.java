package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.view.View;
import android.widget.TextView;

import java.util.*;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.models.FlightResponse;
import es.medac.skycollectorapp.network.FlightRadarService;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FlightRadarService service;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Panel inferior
    private CardView panel;
    private TextView txtModelo, txtDatos;

    // Colección del usuario
    private final List<Avion> miColeccion = new ArrayList<>();

    // Marcadores activos (icao24 → marker)
    private final Map<String, Marker> marcadores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        panel = findViewById(R.id.card_info_vuelo);
        txtModelo = findViewById(R.id.txt_modelo_panel);
        txtDatos = findViewById(R.id.txt_datos_panel);

        cargarColeccion();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opensky-network.org/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(FlightRadarService.class);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    // -------------------------------
    // MAPA
    // -------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(48.0, 10.0), 5));

        handler.postDelayed(this::descargar, 1000);
    }

    // -------------------------------
    // COLECCIÓN
    // -------------------------------

    private void cargarColeccion() {
        miColeccion.clear();

        String json = getSharedPreferences(
                "SkyCollectorDatos",
                Context.MODE_PRIVATE
        ).getString("lista_aviones", null);

        if (json == null) return;

        List<Avion> lista = new Gson().fromJson(
                json,
                new TypeToken<ArrayList<Avion>>() {}.getType()
        );

        if (lista != null) miColeccion.addAll(lista);
    }

    // -------------------------------
    // OPENSKY
    // -------------------------------

    private void descargar() {
        service.getVuelosEnZona(35.0, -10.0, 60.0, 30.0)
                .enqueue(new Callback<FlightResponse>() {
                    @Override
                    public void onResponse(Call<FlightResponse> call,
                                           Response<FlightResponse> response) {

                        if (response.body() == null ||
                                response.body().getStates() == null) return;

                        procesar(response.body().getStates());
                    }

                    @Override
                    public void onFailure(Call<FlightResponse> call, Throwable t) {}
                });
    }

    private void procesar(List<List<Object>> raw) {
        Set<String> visibles = new HashSet<>();

        for (List<Object> r : raw) {
            FlightResponse.OpenSkyAvion api =
                    new FlightResponse.OpenSkyAvion(r);

            if (api.latitude == null || api.longitude == null ||
                    api.callsign == null) continue;

            Avion match = buscarEnColeccion(api.callsign);
            if (match == null) continue; // ⛔ no es de tu colección

            visibles.add(api.icao24);

            LatLng pos = new LatLng(api.latitude, api.longitude);

            Marker m = marcadores.get(api.icao24);
            if (m == null) {
                m = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .icon(iconoSegunRareza(match.getRareza()))
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                        .rotation(api.trueTrack)
                );
                marcadores.put(api.icao24, m);
            } else {
                m.setPosition(pos);
                m.setRotation(api.trueTrack);
            }

            m.setTag(new Object[]{match, api});
        }

        // Eliminar los que ya no están
        Iterator<Map.Entry<String, Marker>> it =
                marcadores.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Marker> e = it.next();
            if (!visibles.contains(e.getKey())) {
                e.getValue().remove();
                it.remove();
            }
        }
    }

    // -------------------------------
    // FILTRO
    // -------------------------------

    private Avion buscarEnColeccion(String callsign) {
        String cs = callsign.toUpperCase();

        for (Avion a : miColeccion) {
            String modelo = a.getModelo().toUpperCase();

            // Coincidencia simple y realista
            if (modelo.contains("737") && cs.startsWith("RYR")) return a;
            if (modelo.contains("A320") && cs.startsWith("VLG")) return a;
            if (modelo.contains("A320") && cs.startsWith("IBE")) return a;
            if (modelo.contains("A320") && cs.startsWith("EZY")) return a;
        }
        return null;
    }

    // -------------------------------
    // CLICK EN AVIÓN
    // -------------------------------

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object[] data = (Object[]) marker.getTag();
        Avion a = (Avion) data[0];
        FlightResponse.OpenSkyAvion api =
                (FlightResponse.OpenSkyAvion) data[1];

        int vel = api.velocity != null
                ? (int) (api.velocity * 3.6)
                : 0;

        txtModelo.setText(a.getModelo() + " | " + a.getFabricante());

        txtDatos.setText(
                "ICAO: " + api.icao24 + "\n" +
                        "Vuelo: " + api.callsign + "\n" +
                        "País: " + api.originCountry + "\n" +
                        "Velocidad: " + vel + " km/h\n" +
                        "Altitud: " + api.altitude + " m\n" +
                        "Rareza: " + a.getRareza()
        );

        panel.setVisibility(View.VISIBLE);
        return true;
    }

    // -------------------------------
    // ICONOS
    // -------------------------------

    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res;
        switch (rareza.toUpperCase()) {
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun;
        }

        Drawable d = ContextCompat.getDrawable(this, res);
        int size = 64;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, size, size);
        d.draw(c);

        return BitmapDescriptorFactory.fromBitmap(b);
    }
}
