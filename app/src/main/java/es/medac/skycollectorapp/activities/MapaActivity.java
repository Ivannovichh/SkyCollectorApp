package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

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

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FlightRadarService service;
    private Handler handler = new Handler(Looper.getMainLooper());

    private CardView panel;
    private TextView txtModelo, txtDatos;

    // GOD MODE
    private List<Avion> misAviones = new ArrayList<>();
    private Map<String, Marker> marcadores = new HashMap<>();

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
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    private void cargarColeccion() {
        String json = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE)
                .getString("lista_aviones", null);
        if (json == null) return;

        List<Avion> lista = new Gson().fromJson(json, new TypeToken<ArrayList<Avion>>(){}.getType());
        if (lista != null) misAviones.addAll(lista);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(40.4, -3.7), 5));

        cargarColeccion();
        descargar(); // Pinta tus aviones
    }

    private void iniciarLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                descargar();
                handler.postDelayed(this, 10000);
            }
        }, 1000);
    }

    private void descargar() {
        // TODO: todo el mundo
        service.getVuelosEnZona(-90, -180, 90, 180) // cubre todo el planeta
                .enqueue(new Callback<FlightResponse>() {
            @Override
            public void onResponse(Call<FlightResponse> call, Response<FlightResponse> response) {
                if (!response.isSuccessful() || response.body()==null) return;
                procesar(response.body().getStates());
            }

            @Override
            public void onFailure(Call<FlightResponse> call, Throwable t) {}
        });
    }

    private void procesar(List<List<Object>> raw) {
        if (raw == null || mMap == null) return;

        Set<String> vistos = new HashSet<>();

        for (List<Object> s : raw) {
            FlightResponse.OpenSkyAvion apiAvion = new FlightResponse.OpenSkyAvion(s);

            if (apiAvion.latitude == null || apiAvion.longitude == null) continue;

            // Buscar si el vuelo actual coincide con un avión de tu colección
            Avion miAvion = null;
            for (Avion a : misAviones) {
                if (a.getModelo().equalsIgnoreCase("Boeing 737-800") && apiAvion.callsign != null) {
                    miAvion = a;
                    break;
                }
                if (a.getModelo().equalsIgnoreCase("Airbus A320") && apiAvion.callsign != null) {
                    miAvion = a;
                    break;
                }
            }

            if (miAvion == null) continue; // si no es de tu colección, saltar

            // Posición y marcador
            LatLng pos = new LatLng(apiAvion.latitude, apiAvion.longitude);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .icon(iconoSegunRareza(miAvion.getRareza()))
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .rotation(apiAvion.trueTrack)
            );
            marker.setTag(miAvion);
        }


        // Limpiar marcadores que ya no están
        Iterator<Map.Entry<String, Marker>> it = marcadores.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> e = it.next();
            if (!vistos.contains(e.getKey())) {
                e.getValue().remove();
                it.remove();
            }
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        FlightResponse.OpenSkyAvion api = (FlightResponse.OpenSkyAvion) marker.getTag();
        if (api == null) return false;

        Avion a = null;
        for (Avion av : misAviones) {
            if (api.callsign != null && api.callsign.toUpperCase().contains(av.getModelo().split(" ")[0].toUpperCase())) {
                a = av;
                break;
            }
        }
        if (a == null) return false;

        int vel = api.velocity != null ? (int)(api.velocity*3.6) : 0;

        txtModelo.setText(a.getModelo()+" | "+a.getFabricante());
        txtDatos.setText(
                "ICAO: "+api.icao24+"\n"+
                        "Vuelo: "+api.callsign+"\n"+
                        "País: "+api.originCountry+"\n"+
                        "Vel: "+vel+" km/h\n"+
                        "Alt: "+api.altitude+" m"
        );

        panel.setVisibility(View.VISIBLE);
        return true;
    }

    private BitmapDescriptor iconoSegunRareza(String rareza) {
        int resId;
        switch (rareza.toUpperCase()) {
            case "COMMON": resId = R.drawable.avioncomun; break;
            case "RARE": resId = R.drawable.avionraro; break;
            case "EPIC": resId = R.drawable.avionepico; break;
            case "LEGENDARY": resId = R.drawable.avionlegendario; break;
            default: resId = R.drawable.avioncomun;
        }
        Drawable d = ContextCompat.getDrawable(this, resId);
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.draw(c);
        return BitmapDescriptorFactory.fromBitmap(b);
    }


}
