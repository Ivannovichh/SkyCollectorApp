package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.models.FlightResponse;
import es.medac.skycollectorapp.network.FlightRadarService;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapaActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // -------------------------------
    // MODOS
    // -------------------------------
    private enum ModoMapa { TODOS, AVISTADOS }
    private ModoMapa modoActual = ModoMapa.TODOS;

    // -------------------------------
    // MAPA / API
    // -------------------------------
    private GoogleMap mMap;
    private FlightRadarService service;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // -------------------------------
    // UI
    // -------------------------------
    private CardView panel;
    private TextView txtModelo, txtDatos;
    private Button btnAvistar, btnTodos, btnAvistados;

    // -------------------------------
    // DATOS
    // -------------------------------
    private final List<Avion> miColeccion = new ArrayList<>();
    private final Map<String, Marker> marcadores = new HashMap<>();
    private String icaoSeleccionado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        panel = findViewById(R.id.card_info_vuelo);
        txtModelo = findViewById(R.id.txt_modelo_panel);
        txtDatos = findViewById(R.id.txt_datos_panel);
        btnAvistar = findViewById(R.id.btn_avistar);

        // 🔴 BOTONES DE MODO (deben existir en el XML)
        btnTodos = findViewById(R.id.btn_todos);
        btnAvistados = findViewById(R.id.btn_avistados);

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

        // -------------------------------
        // BOTONES
        // -------------------------------

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

        btnAvistar.setOnClickListener(v -> {
            if (icaoSeleccionado == null) {
                Toast.makeText(this,
                        "Selecciona un avión primero",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs =
                    getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);

            prefs.edit()
                    .putString("icao_seleccionado", icaoSeleccionado)
                    .apply();

            Toast.makeText(this,
                    "Avión seleccionado. Pulsa + para añadirlo",
                    Toast.LENGTH_LONG).show();
        });
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

    private void refrescarMapa() {
        for (Marker m : marcadores.values()) m.remove();
        marcadores.clear();
        panel.setVisibility(View.GONE);
        descargar();
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

    private Avion buscarPorIcao(String icao) {
        for (Avion a : miColeccion) {
            if (icao.equalsIgnoreCase(a.getIcao24())) return a;
        }
        return null;
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

            if (api.latitude == null || api.longitude == null) continue;

            Avion avistado = buscarPorIcao(api.icao24);

            // 🔴 FILTRO POR MODO
            if (modoActual == ModoMapa.AVISTADOS && avistado == null) continue;

            visibles.add(api.icao24);
            LatLng pos = new LatLng(api.latitude, api.longitude);

            Marker m = marcadores.get(api.icao24);
            if (m == null) {
                BitmapDescriptor icono =
                        avistado != null
                                ? iconoSegunRareza(avistado.getRareza())
                                : iconoGenerico();

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
        }

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
    // CLICK
    // -------------------------------

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object[] data = (Object[]) marker.getTag();
        FlightResponse.OpenSkyAvion api =
                (FlightResponse.OpenSkyAvion) data[0];
        Avion a = (Avion) data[1];

        icaoSeleccionado = api.icao24;

        int vel = api.velocity != null
                ? (int) (api.velocity * 3.6)
                : 0;

        if (a != null) {
            txtModelo.setText(a.getModelo() + " | " + a.getFabricante());
        } else {
            txtModelo.setText("Vuelo " + api.callsign);
        }

        txtDatos.setText(
                "ICAO: " + api.icao24 + "\n" +
                        "País: " + api.originCountry + "\n" +
                        "Velocidad: " + vel + " km/h\n" +
                        "Altitud: " + api.altitude + " m"
        );

        panel.setVisibility(View.VISIBLE);
        btnAvistar.setVisibility(
                a == null && modoActual == ModoMapa.TODOS
                        ? View.VISIBLE
                        : View.GONE
        );

        return true;
    }

    // -------------------------------
    // ICONOS
    // -------------------------------

    private BitmapDescriptor iconoGenerico() {
        return iconoDesdeDrawable(R.drawable.avioncomun);
    }

    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int res;
        switch (rareza.toUpperCase()) {
            case "RARE": res = R.drawable.avionraro; break;
            case "EPIC": res = R.drawable.avionepico; break;
            case "LEGENDARY": res = R.drawable.avionlegendario; break;
            default: res = R.drawable.avioncomun;
        }
        return iconoDesdeDrawable(res);
    }

    private BitmapDescriptor iconoDesdeDrawable(int res) {
        Drawable d = ContextCompat.getDrawable(this, res);
        int size = 64;

        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, size, size);
        d.draw(c);

        return BitmapDescriptorFactory.fromBitmap(b);
    }
}
