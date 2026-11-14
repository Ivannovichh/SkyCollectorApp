package es.medac.skycollectorapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Pantalla completa de previsualización de una aeronave detectada en el radar.
 * Muestra la carta con su foto de catálogo o silueta, rareza, especificaciones y telemetría en vivo,
 * respetando el apodo si el usuario ya la capturó y permitiendo iniciar el avistamiento si está en rango.
 */
public class DetalleVueloRadarActivity extends AppCompatActivity {

    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = 30f;

    private String icao;
    private String tipoIcao;
    private String matricula;
    private String nombreModelo;
    private float distanciaKm;
    private double latAvion;
    private double lonAvion;
    private boolean yaAvistado;

    /**
     * Inicializa las vistas, recupera los datos oficiales y el posible apodo del vuelo,
     * carga su fotografía de catálogo y configura el botón inferior para avistar.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_vuelo_radar);

        ImageButton btnVolver = findViewById(R.id.btn_volver_detalle_radar);
        ImageView imgAvion = findViewById(R.id.img_avion_detalle_radar);
        TextView txtRareza = findViewById(R.id.txt_rareza_detalle);
        TextView txtIcaoBadge = findViewById(R.id.txt_icao_badge_detalle);
        TextView txtModelo = findViewById(R.id.txt_modelo_detalle_radar);
        TextView txtEmpresa = findViewById(R.id.txt_empresa_detalle_radar);
        TextView txtTelemetria = findViewById(R.id.txt_telemetria_detalle_radar);
        TextView txtSpecs = findViewById(R.id.txt_specs_detalle_radar);
        TextView txtEstadoDistancia = findViewById(R.id.txt_estado_distancia_detalle);
        Button btnAvistar = findViewById(R.id.btn_avistar_desde_detalle);

        btnVolver.setOnClickListener(v -> finish());

        // Recuperar datos enviados por MapaActivity separando modelo oficial del apodo del usuario
        Intent intent = getIntent();
        icao = intent.getStringExtra("icao");
        tipoIcao = intent.getStringExtra("tipo");
        matricula = intent.getStringExtra("matricula");
        nombreModelo = intent.getStringExtra("modelo");
        String apodo = intent.getStringExtra("apodo");
        String empresa = intent.getStringExtra("empresa");
        String rutaTexto = intent.getStringExtra("ruta");
        int velocidadKmh = intent.getIntExtra("velocidad_kmh", 0);
        String altitudTexto = intent.getStringExtra("altitud");
        distanciaKm = intent.getFloatExtra("distancia_km", -1f);
        latAvion = intent.getDoubleExtra("lat_avion", 0.0);
        lonAvion = intent.getDoubleExtra("lon_avion", 0.0);
        yaAvistado = intent.getBooleanExtra("ya_avistado", false);

        // Buscar ficha técnica y foto usando solo datos oficiales (nunca el apodo del usuario)
        Avion fichaCatalogo = AvionGenerator.getAvionPorTipoIcao(tipoIcao);
        if (fichaCatalogo == null) {
            fichaCatalogo = AvionGenerator.getAvionPorNombreModelo(nombreModelo);
        }

        if (fichaCatalogo != null && fichaCatalogo.getImagenResId() != 0) {
            imgAvion.setImageResource(fichaCatalogo.getImagenResId());
            if (nombreModelo == null || nombreModelo.isEmpty()) {
                nombreModelo = fichaCatalogo.getModelo();
            }
            if (empresa == null || empresa.isEmpty()) {
                empresa = fichaCatalogo.getFabricante();
            }
            aplicarEstiloRareza(txtRareza, fichaCatalogo.getRareza());

            String specs = "Fabricante: " + fichaCatalogo.getFabricante() + "\n" +
                    "Velocidad crucero: " + fichaCatalogo.getVelocidad() + "\n" +
                    "Capacidad: " + fichaCatalogo.getPasajeros() + "\n" +
                    "Dimensiones: " + fichaCatalogo.getDimensiones() + "\n" +
                    "País de origen: " + fichaCatalogo.getPais() + "\n" +
                    "Peso máximo: " + fichaCatalogo.getPeso();
            txtSpecs.setText(specs);
        } else {
            imgAvion.setImageResource(R.drawable.sil_jet);
            aplicarEstiloRareza(txtRareza, "COMMON");
            txtSpecs.setText("Aeronave no registrada en el catálogo oficial.\nTipo ICAO: " + (tipoIcao != null ? tipoIcao : "Desconocido"));
        }

        txtIcaoBadge.setText("ICAO: " + (icao != null ? icao.toUpperCase() : "---"));

        // Destacar el apodo personalizado arriba si el avión ya fue capturado y renombrado
        if (apodo != null && !apodo.equalsIgnoreCase(nombreModelo)) {
            txtModelo.setText("«" + apodo + "»");
            String subtitulo = (nombreModelo != null ? nombreModelo : "") +
                    (empresa != null && !empresa.isEmpty() ? " • " + empresa : "");
            txtEmpresa.setText(subtitulo);
        } else {
            txtModelo.setText(nombreModelo != null ? nombreModelo : "Aeronave desconocida");
            txtEmpresa.setText(empresa != null && !empresa.isEmpty() ? empresa : "Operador no disponible");
        }

        StringBuilder telemetria = new StringBuilder();
        if (rutaTexto != null && !rutaTexto.isEmpty()) {
            telemetria.append("Ruta: ").append(rutaTexto).append("\n");
        }
        telemetria.append("Matrícula: ").append(matricula != null ? matricula : "—").append("\n")
                .append("Tipo ICAO: ").append(tipoIcao != null ? tipoIcao : "—").append("\n")
                .append("Velocidad actual: ").append(velocidadKmh).append(" km/h\n")
                .append("Altitud actual: ").append(altitudTexto != null ? altitudTexto : "—");
        txtTelemetria.setText(telemetria.toString());

        configurarBotonAvistar(btnAvistar, txtEstadoDistancia);
    }

    /**
     * Ajusta los colores de la etiqueta de rareza sobre el fondo oscuro de la carta.
     */
    private void aplicarEstiloRareza(TextView badge, String rareza) {
        String r = rareza != null ? rareza.toUpperCase() : "COMMON";
        badge.setText(r);

        switch (r) {
            case "RARE":
                badge.setBackgroundColor(Color.parseColor("#1E3A8A"));
                badge.setTextColor(Color.parseColor("#93C5FD"));
                break;
            case "EPIC":
                badge.setBackgroundColor(Color.parseColor("#4C1D95"));
                badge.setTextColor(Color.parseColor("#D8B4FE"));
                break;
            case "LEGENDARY":
            case "MYTHIC":
                badge.setBackgroundColor(Color.parseColor("#78350F"));
                badge.setTextColor(Color.parseColor("#FDE68A"));
                break;
            default:
                badge.setBackgroundColor(Color.parseColor("#3A506B"));
                badge.setTextColor(Color.parseColor("#FFFFFF"));
                break;
        }
    }

    /**
     * Configura el estado visual y la acción del botón inferior de avistamiento
     * comprobando si ya está en la colección o si cumple el límite de 30 km.
     */
    private void configurarBotonAvistar(Button btnAvistar, TextView txtEstadoDistancia) {
        if (yaAvistado) {
            btnAvistar.setEnabled(false);
            btnAvistar.setText("Ya avistado en tu colección");
            txtEstadoDistancia.setText("Esta aeronave ya forma parte de tu hangar.");
            txtEstadoDistancia.setTextColor(Color.parseColor("#B0BEC5"));
            return;
        }

        if (distanciaKm < 0) {
            txtEstadoDistancia.setText("Ubicación GPS no disponible para comprobar distancia");
            txtEstadoDistancia.setTextColor(Color.parseColor("#B0BEC5"));
        } else if (distanciaKm > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            txtEstadoDistancia.setText("Distancia actual: " + Math.round(distanciaKm) + " km (acércate a menos de 30 km)");
            txtEstadoDistancia.setTextColor(Color.parseColor("#FF5252"));
        } else {
            txtEstadoDistancia.setText("¡En rango de avistamiento! (" + Math.round(distanciaKm) + " km)");
            txtEstadoDistancia.setTextColor(Color.parseColor("#00E676"));
        }

        btnAvistar.setOnClickListener(v -> iniciarAvistamiento());
    }

    /**
     * Valida la distancia máxima de 30 km, guarda los datos temporales en SharedPreferences
     * y abre AddAvionActivity para capturar la fotografía del avistamiento.
     */
    private void iniciarAvistamiento() {
        if (distanciaKm < 0) {
            Toast.makeText(this, "Activa tu ubicación GPS para comprobar la distancia al avión", Toast.LENGTH_LONG).show();
            return;
        }

        if (distanciaKm > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(this, "Estás a " + Math.round(distanciaKm) + " km. Acércate a menos de " + Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM) + " km para avistarlo", Toast.LENGTH_LONG).show();
            return;
        }

        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icao)
                .putString("tipo_seleccionado", tipoIcao)
                .putString("matricula_seleccionada", matricula)
                .putString("descripcion_seleccionada", nombreModelo)
                .putLong("fecha_seleccionada", System.currentTimeMillis())
                .putFloat("distancia_seleccionada", distanciaKm)
                .putFloat("lat_avion_seleccionado", (float) latAvion)
                .putFloat("lon_avion_seleccionado", (float) lonAvion)
                .apply();

        Intent intent = new Intent(this, AddAvionActivity.class);
        startActivity(intent);
        finish();
    }
}