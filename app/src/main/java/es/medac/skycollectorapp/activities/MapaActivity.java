package es.medac.skycollectorapp.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.List;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.FlightResponse;
import es.medac.skycollectorapp.network.FlightRadarService;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FlightRadarService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean yaPintado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header(
                                    "Authorization",
                                    Credentials.basic(
                                            "jotamynds",
                                            "aPIPUTERA1"
                                    )
                            )
                            .build();
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://opensky-network.org/api/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(FlightRadarService.class);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(48.0, 10.0), 5));

        iniciarLoop();
    }

    private void iniciarLoop() {
        handler.postDelayed(() -> descargar(), 1000);
    }

    private void descargar() {
        service.getVuelosEnZona(35.0, -10.0, 60.0, 30.0)
                .enqueue(new Callback<FlightResponse>() {
                    @Override
                    public void onResponse(Call<FlightResponse> call,
                                           Response<FlightResponse> response) {

                        if (response.body() == null ||
                                response.body().getStates() == null) return;

                        if (yaPintado) return;

                        pintar(response.body().getStates());
                        yaPintado = true;
                    }

                    @Override
                    public void onFailure(Call<FlightResponse> call, Throwable t) {
                        Log.e("OPENSKY", "ERROR", t);
                    }
                });
    }

    private void pintar(List<List<Object>> raw) {

        int max = Math.min(raw.size(), 100);

        for (int i = 0; i < max; i++) {
            List<Object> r = raw.get(i);

            Double lon = r.get(5) instanceof Number
                    ? ((Number) r.get(5)).doubleValue()
                    : null;
            Double lat = r.get(6) instanceof Number
                    ? ((Number) r.get(6)).doubleValue()
                    : null;

            if (lat == null || lon == null) continue;

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .icon(iconoAvion()));
        }
    }

    private BitmapDescriptor iconoAvion() {
        Drawable d = ContextCompat.getDrawable(this, R.drawable.avioncomun);
        Bitmap b = Bitmap.createBitmap(
                d.getIntrinsicWidth(),
                d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.draw(c);
        return BitmapDescriptorFactory.fromBitmap(b);
    }
}
