// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad encargada de mostrar la ficha previa detallada de un vuelo detectado en el radar
 * o consultado desde un perfil público, aplicando estilo dorado exclusivo si es un Easter Egg.
 */
public class DetalleVueloRadarActivity extends AppCompatActivity {

    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = MapaActivity.DISTANCIA_MAXIMA_AVISTAMIENTO_KM;

    private String icao;
    private String callsign;
    private String tipoIcao;
    private String matricula;
    private String nombreModelo;
    private float distanciaKm;
    private double latAvion;
    private double lonAvion;
    private boolean yaAvistado;
    private boolean modoSoloLectura;
    private boolean esEasterEgg;

    private Object fuenteImagenActual;
    private String urlRemotaWikipedia = null;
    private TextView txtAvisoSinFoto;

    /**
     * Inicializa la actividad y valida los parámetros recibidos en el Intent.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_vuelo_radar);

        if (!validarDatosEntrada()) {
            Toast.makeText(this, getString(R.string.error_cargar_vuelo), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
    }

    /**
     * Comprueba que el código ICAO recibido en el Intent sea válido antes de cargar la interfaz.
     */
    private boolean validarDatosEntrada() {
        Intent intent = getIntent();
        if (intent == null) return false;

        icao = intent.getStringExtra("icao");
        return icao != null && !icao.trim().isEmpty();
    }

    /**
     * Enlaza las vistas del layout, muestra la telemetría inicial y lanza la resolución asíncrona del avión.
     */
    private void inicializarVistas() {
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
        View footerAvistar = findViewById(R.id.footer_avistar_radar);

        btnVolver.setOnClickListener(v -> finish());

        imgAvion.setOnClickListener(v -> {
            if (fuenteImagenActual != null) {
                abrirImagenAmpliada(fuenteImagenActual);
            }
        });

        Intent intent = getIntent();
        modoSoloLectura = intent.getBooleanExtra("modo_solo_lectura", false);
        callsign = intent.getStringExtra("callsign");
        tipoIcao = intent.getStringExtra("tipo");
        matricula = intent.getStringExtra("matricula");

        String modeloCrudo = intent.getStringExtra("modelo");
        nombreModelo = (modeloCrudo != null)
                ? modeloCrudo.replaceAll("(?i)^(aeronave|avión|aircraft)\\s+", "").trim()
                : null;

        esEasterEgg = intent.getBooleanExtra("es_easter_egg", false)
                || AvionGenerator.esEasterEggTerrestre(tipoIcao, callsign, matricula, nombreModelo);

        String apodo = intent.getStringExtra("apodo");
        String empresa = intent.getStringExtra("empresa");
        String rutaTexto = intent.getStringExtra("ruta");
        String categoriaRadar = getIntent().getStringExtra("categoria");
        int velocidadKmh = intent.getIntExtra("velocidad_kmh", 0);
        String altitudTexto = intent.getStringExtra("altitud");
        distanciaKm = intent.getFloatExtra("distancia_km", -1f);
        latAvion = intent.getDoubleExtra("lat_avion", 0.0);
        lonAvion = intent.getDoubleExtra("lon_avion", 0.0);
        yaAvistado = intent.getBooleanExtra("ya_avistado", false);
        long fechaAvistamiento = intent.getLongExtra("fecha_avistamiento", 0L);
        String fotoAvistamientoPublica = intent.getStringExtra("foto_avistamiento_url");

        String icaoFormateado = (icao != null && !icao.isEmpty()) ? icao.toUpperCase(Locale.ROOT) : "---";
        txtIcaoBadge.setText(getString(R.string.icao_badge_formato, icaoFormateado));

        StringBuilder telemetria = new StringBuilder();
        if (modoSoloLectura) {
            telemetria.append(getString(R.string.telemetria_matricula, matricula != null && !matricula.isEmpty() ? matricula : "—"))
                    .append(getString(R.string.telemetria_tipo_icao, tipoIcao != null && !tipoIcao.isEmpty() ? tipoIcao : "—"));
            if (distanciaKm >= 0) {
                telemetria.append(getString(R.string.telemetria_distancia_captura, Math.round(distanciaKm)));
            }
            if (fechaAvistamiento > 0) {
                String fechaStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(fechaAvistamiento));
                telemetria.append(getString(R.string.telemetria_fecha_captura, fechaStr));
            }
        } else {
            if (rutaTexto != null && !rutaTexto.isEmpty()) {
                telemetria.append(getString(R.string.telemetria_ruta_estimada, rutaTexto));
            }
            telemetria.append(getString(R.string.telemetria_matricula, matricula != null ? matricula : "—"))
                    .append(getString(R.string.telemetria_tipo_icao, tipoIcao != null && !tipoIcao.isEmpty() ? tipoIcao : "—"))
                    .append(getString(R.string.telemetria_velocidad, velocidadKmh))
                    .append(getString(R.string.telemetria_altitud, altitudTexto != null ? altitudTexto : "—"));
        }
        txtTelemetria.setText(telemetria.toString().trim());

        if (modoSoloLectura) {
            if (footerAvistar != null) footerAvistar.setVisibility(View.GONE);
        } else {
            configurarBotonAvistar(btnAvistar, txtEstadoDistancia);
        }

        txtModelo.setText(nombreModelo != null && !nombreModelo.isEmpty()
                ? nombreModelo
                : getString(R.string.identificando_aeronave));
        txtEmpresa.setText(empresa != null && !empresa.isEmpty()
                ? empresa
                : getString(R.string.consultando_base_datos));
        txtSpecs.setText(getString(R.string.obteniendo_ficha_tecnica));
        aplicarEstiloRareza(txtRareza, esEasterEgg ? "EASTER_EGG" : "COMMON");

        if (esEasterEgg) {
            aplicarTemaDoradoEasterEgg();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final String empresaInicial = empresa;

        AvionGenerator.resolverAvionGlobalAsync(
                this,
                icao,
                callsign,
                matricula,
                tipoIcao,
                nombreModelo,
                categoriaRadar,
                avionResuelto -> {
                    if (isFinishing() || isDestroyed()) return;

                    if ("EASTER_EGG".equalsIgnoreCase(avionResuelto.getRareza()) || esEasterEgg) {
                        esEasterEgg = true;
                        aplicarTemaDoradoEasterEgg();
                    }

                    nombreModelo = avionResuelto.getModelo();

                    if (avionResuelto.getTipoIcao() != null && !avionResuelto.getTipoIcao().isEmpty()
                            && (tipoIcao == null || tipoIcao.trim().isEmpty())) {
                        tipoIcao = avionResuelto.getTipoIcao();
                        String textoActual = txtTelemetria.getText().toString();
                        String lineaVacia = getString(R.string.telemetria_tipo_icao, "—").trim();
                        String lineaNueva = getString(R.string.telemetria_tipo_icao, tipoIcao).trim();
                        txtTelemetria.setText(textoActual.replace(lineaVacia, lineaNueva));
                    }

                    String operadorFinal = (empresaInicial != null && !empresaInicial.isEmpty())
                            ? empresaInicial
                            : avionResuelto.getFabricante();

                    if (apodo != null && !apodo.equalsIgnoreCase(nombreModelo)) {
                        txtModelo.setText("«" + apodo + "»");
                        txtEmpresa.setText(nombreModelo + " • " + operadorFinal);
                    } else {
                        txtModelo.setText(nombreModelo);
                        txtEmpresa.setText(operadorFinal);
                    }

                    aplicarEstiloRareza(txtRareza, avionResuelto.getRareza());

                    String specs = modoSoloLectura
                            ? getString(
                            R.string.specs_detalle_solo_lectura_formato,
                            avionResuelto.getFabricante(),
                            AvionGenerator.traducirRareza(this, avionResuelto.getRareza()),
                            avionResuelto.getDescripcion()
                    )
                            : getString(
                            R.string.specs_detalle_formato,
                            avionResuelto.getFabricante(),
                            AvionGenerator.traducirRareza(this, avionResuelto.getRareza()),
                            velocidadKmh,
                            avionResuelto.getDescripcion()
                    );
                    txtSpecs.setText(specs);

                    int fallback = (avionResuelto.getImagenResId() != 0)
                            ? avionResuelto.getImagenResId()
                            : AvionGenerator.resolverImagenPorModelo(avionResuelto.getModelo(), avionResuelto.getTipoIcao());

                    boolean esDesconocido = AvionGenerator.esModeloDesconocido(this, avionResuelto.getModelo());

                    if (esDesconocido) {
                        urlRemotaWikipedia = null;
                        fuenteImagenActual = fallback;
                        gestionarAvisoSinFoto(imgAvion, true);
                        Glide.with(this).load(fallback).fitCenter().into(imgAvion);
                        return;
                    }

                    String urlDirecta = (fotoAvistamientoPublica != null && !fotoAvistamientoPublica.isEmpty())
                            ? fotoAvistamientoPublica
                            : avionResuelto.getUrlImagenRemota();

                    if (urlDirecta != null && !urlDirecta.isEmpty()) {
                        cargarImagenRemotaConRespaldo(imgAvion, urlDirecta, fallback, executor, mainHandler);
                    } else {
                        buscarYMostrarFotoWikipedia(imgAvion, avionResuelto.getModelo(), fallback, executor, mainHandler);
                    }
                }
        );
    }

    /**
     * Construye el modelo de carga para Glide añadiendo la cabecera User-Agent obligatoria
     * cuando la URL pertenece a Wikimedia o Wikipedia para evitar bloqueos HTTP 403.
     */
    private Object construirModeloGlide(String url) {
        if (url == null) return null;
        if (url.contains("wikimedia.org") || url.contains("wikipedia.org")) {
            return new GlideUrl(
                    url,
                    new LazyHeaders.Builder()
                            .addHeader("User-Agent", AvionGenerator.USER_AGENT)
                            .build()
            );
        }
        return url;
    }

    /**
     * Descarga una URL remota en el ImageView con User-Agent compatible y, si falla,
     * intenta rescatarla mediante Wikipedia o muestra la silueta de respaldo con su aviso.
     */
    private void cargarImagenRemotaConRespaldo(ImageView imgAvion, String urlDirecta, int fallback,
                                               ExecutorService executor, Handler mainHandler) {
        urlRemotaWikipedia = urlDirecta;
        fuenteImagenActual = urlDirecta;
        gestionarAvisoSinFoto(imgAvion, false);

        Glide.with(this)
                .load(construirModeloGlide(urlDirecta))
                .fitCenter()
                .listener(new RequestListener<Drawable>() {
                    /**
                     * Gestiona el fallo de carga de la imagen principal recurriendo a una búsqueda de respaldo en Wikipedia.
                     */
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        mainHandler.post(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            boolean esTorre = AvionGenerator.esEasterEggTorre(tipoIcao, callsign, matricula, nombreModelo);
                            String terminoRespaldo = esTorre
                                    ? "Air traffic control tower"
                                    : (esEasterEgg ? "Airport ground support equipment" : nombreModelo);
                            buscarYMostrarFotoWikipedia(imgAvion, terminoRespaldo, fallback, executor, mainHandler);
                        });
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imgAvion);
    }

    /**
     * Busca la imagen del modelo en Wikipedia en segundo plano y aplica la silueta si no hay resultados.
     */
    private void buscarYMostrarFotoWikipedia(ImageView imgAvion, String terminoRaw, int fallback,
                                             ExecutorService executor, Handler mainHandler) {
        String modeloWiki = esEasterEgg
                ? terminoRaw
                : AvionGenerator.limpiarModeloParaWiki(this, terminoRaw);

        if (modeloWiki == null || modeloWiki.isEmpty()) {
            urlRemotaWikipedia = null;
            fuenteImagenActual = fallback;
            gestionarAvisoSinFoto(imgAvion, true);
            Glide.with(this).load(fallback).fitCenter().into(imgAvion);
            return;
        }

        executor.execute(() -> {
            String urlFotoWiki = esEasterEgg
                    ? consultarImagenWikiLibre(modeloWiki)
                    : obtenerUrlDesdeWikipedia(modeloWiki);

            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (urlFotoWiki != null && !urlFotoWiki.isEmpty()) {
                    urlRemotaWikipedia = urlFotoWiki;
                    fuenteImagenActual = urlFotoWiki;
                    gestionarAvisoSinFoto(imgAvion, false);
                    Glide.with(this)
                            .load(construirModeloGlide(urlFotoWiki))
                            .error(fallback)
                            .fitCenter()
                            .into(imgAvion);
                } else {
                    urlRemotaWikipedia = null;
                    fuenteImagenActual = fallback;
                    gestionarAvisoSinFoto(imgAvion, true);
                    Glide.with(this).load(fallback).fitCenter().into(imgAvion);
                }
            });
        });
    }

    /**
     * Busca directamente una fotografía en Wikipedia usando la API REST de resúmenes
     * sin añadir sufijos aeronáuticos (respaldo para Easter Eggs terrestres).
     */
    private String consultarImagenWikiLibre(String termino) {
        String[] consultas = new String[]{termino, "Follow-me car", "Airport apron"};
        for (String consulta : consultas) {
            if (consulta == null || consulta.trim().isEmpty()) continue;
            try {
                String searchEndpoint = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                        + java.net.URLEncoder.encode(consulta, "UTF-8") + "&format=json&srlimit=1";
                String searchResp = leerUrlHttp(searchEndpoint);
                if (searchResp == null) continue;

                org.json.JSONObject searchJson = new org.json.JSONObject(searchResp);
                org.json.JSONObject queryObj = searchJson.optJSONObject("query");
                if (queryObj == null) continue;

                org.json.JSONArray searchArray = queryObj.optJSONArray("search");
                if (searchArray == null || searchArray.length() == 0) continue;

                String titulo = searchArray.getJSONObject(0).optString("title", "");
                if (titulo.isEmpty()) continue;

                String summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                        + java.net.URLEncoder.encode(titulo.replace(" ", "_"), "UTF-8");
                String summaryResp = leerUrlHttp(summaryUrl);
                if (summaryResp == null) continue;

                org.json.JSONObject summaryJson = new org.json.JSONObject(summaryResp);
                org.json.JSONObject thumb = summaryJson.optJSONObject("thumbnail");
                if (thumb != null) {
                    String src = thumb.optString("source", null);
                    if (src != null && !src.isEmpty()) return src;
                }
                org.json.JSONObject orig = summaryJson.optJSONObject("originalimage");
                if (orig != null) {
                    String src = orig.optString("source", null);
                    if (src != null && !src.isEmpty()) return src;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Aplica colores y estilos personalizados a la insignia de rareza de la aeronave.
     */
    private void aplicarEstiloRareza(TextView badge, String rareza) {
        String r = rareza != null ? rareza.toUpperCase(Locale.ROOT) : "COMMON";
        badge.setText(AvionGenerator.traducirRareza(this, r));

        switch (r) {
            case "EASTER_EGG":
                badge.setBackgroundColor(Color.parseColor("#B45309"));
                badge.setTextColor(Color.parseColor("#FFD700"));
                badge.setText("★ EASTER EGG ★");
                break;
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
     * Transforma la pantalla de detalle en una edición dorada exclusiva cuando la entidad es un Easter Egg.
     */
    private void aplicarTemaDoradoEasterEgg() {
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            pantallaPrincipal.setBackground(fondoDorado);
        }

        TextView txtModelo = findViewById(R.id.txt_modelo_detalle_radar);
        TextView txtEmpresa = findViewById(R.id.txt_empresa_detalle_radar);
        TextView txtIcaoBadge = findViewById(R.id.txt_icao_badge_detalle);
        Button btnAvistar = findViewById(R.id.btn_avistar_desde_detalle);

        if (txtModelo != null) txtModelo.setTextColor(0xFFFFD700);
        if (txtEmpresa != null) txtEmpresa.setTextColor(0xFFFDE68A);
        if (txtIcaoBadge != null) {
            txtIcaoBadge.setBackgroundColor(0xFF78350F);
            txtIcaoBadge.setTextColor(0xFFFFD700);
        }
        if (btnAvistar != null && btnAvistar.isEnabled()) {
            btnAvistar.setBackgroundColor(0xFFFFD700);
            btnAvistar.setTextColor(0xFF1F1400);
        }
    }

    /**
     * Configura el comportamiento, texto y colores del botón de avistamiento
     * en función de la distancia GPS calculada con el usuario (rango de 40 km).
     */
    private void configurarBotonAvistar(Button btnAvistar, TextView txtEstadoDistancia) {
        if (yaAvistado) {
            btnAvistar.setEnabled(false);
            btnAvistar.setText(getString(R.string.btn_ya_avistado));
            txtEstadoDistancia.setText(getString(R.string.msg_ya_en_hangar));
            txtEstadoDistancia.setTextColor(Color.parseColor("#B0BEC5"));
            return;
        }

        if (distanciaKm < 0) {
            txtEstadoDistancia.setText(getString(R.string.msg_gps_no_disponible));
            txtEstadoDistancia.setTextColor(Color.parseColor("#B0BEC5"));
        } else if (distanciaKm > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            txtEstadoDistancia.setText(getString(R.string.msg_fuera_rango, Math.round(distanciaKm)));
            txtEstadoDistancia.setTextColor(Color.parseColor("#FF5252"));
        } else {
            txtEstadoDistancia.setText(getString(R.string.msg_en_rango, Math.round(distanciaKm)));
            txtEstadoDistancia.setTextColor(Color.parseColor("#00E676"));
        }

        btnAvistar.setOnClickListener(v -> iniciarAvistamiento());
    }

    /**
     * Valida que el usuario esté dentro del rango de captura permitido y
     * almacena temporalmente los datos del avión antes de abrir la pantalla de registro.
     */
    private void iniciarAvistamiento() {
        if (distanciaKm < 0) {
            Toast.makeText(this, getString(R.string.toast_activar_gps), Toast.LENGTH_LONG).show();
            return;
        }

        if (distanciaKm > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(
                    this,
                    getString(R.string.toast_acercarse_rango, Math.round(distanciaKm), Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM)),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE)
                .edit()
                .putString("icao_seleccionado", icao)
                .putString("callsign_seleccionado", callsign)
                .putString("tipo_seleccionado", tipoIcao)
                .putString("matricula_seleccionada", matricula)
                .putString("descripcion_seleccionada", nombreModelo)
                .putString("url_imagen_remota_seleccionada", urlRemotaWikipedia)
                .putBoolean("es_easter_egg_seleccionado", esEasterEgg)
                .putLong("fecha_seleccionada", System.currentTimeMillis())
                .putFloat("distancia_seleccionada", distanciaKm)
                .putFloat("lat_avion_seleccionado", (float) latAvion)
                .putFloat("lon_avion_seleccionado", (float) lonAvion)
                .apply();

        Intent intent = new Intent(this, AddAvionActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra u oculta dinámicamente un aviso visual superpuesto cuando la aeronave
     * no dispone de una fotografía real en los repositorios externos.
     */
    private void gestionarAvisoSinFoto(ImageView imageView, boolean mostrar) {
        ViewGroup parent = (ViewGroup) imageView.getParent();
        if (parent == null) return;

        if (mostrar) {
            if (txtAvisoSinFoto == null) {
                txtAvisoSinFoto = new TextView(this);
                txtAvisoSinFoto.setText(getString(R.string.sin_fotografia_disponible));
                txtAvisoSinFoto.setTextColor(Color.WHITE);
                txtAvisoSinFoto.setTextSize(14f);
                txtAvisoSinFoto.setTypeface(null, android.graphics.Typeface.BOLD);
                txtAvisoSinFoto.setPadding(24, 12, 24, 12);
                txtAvisoSinFoto.setBackgroundColor(Color.parseColor("#CC1E293B"));
                txtAvisoSinFoto.setGravity(Gravity.CENTER);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER;
                txtAvisoSinFoto.setLayoutParams(params);
                parent.addView(txtAvisoSinFoto);
            } else {
                txtAvisoSinFoto.setVisibility(View.VISIBLE);
            }
        } else {
            if (txtAvisoSinFoto != null) {
                txtAvisoSinFoto.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Orquesta la consulta de la imagen del modelo en Wikipedia utilizando el idioma activo
     * de la aplicación y aplicando un respaldo automático al inglés si fuera necesario.
     */
    private String obtenerUrlDesdeWikipedia(String terminoBusqueda) {
        if (AvionGenerator.esModeloDesconocido(this, terminoBusqueda)) return null;
        String langPrincipal = AvionGenerator.obtenerCodigoIdiomaWiki();
        String url = consultarImagenWikiPorIdioma(terminoBusqueda, langPrincipal);
        if ((url == null || url.isEmpty()) && !"en".equals(langPrincipal)) {
            url = consultarImagenWikiPorIdioma(terminoBusqueda, "en");
        }
        return url;
    }

    /**
     * Realiza peticiones a la API de MediaWiki para buscar el artículo coincidente y
     * extraer la URL de su miniatura principal, filtrando resultados no deseados.
     */
    private String consultarImagenWikiPorIdioma(String terminoBusqueda, String lang) {
        try {
            String sufijo = getString(R.string.wiki_sufijo_busqueda);
            String searchEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + java.net.URLEncoder.encode(terminoBusqueda + sufijo, "UTF-8") + "&format=json&srlimit=1";
            String searchRespuesta = leerUrlHttp(searchEndpoint);
            if (searchRespuesta == null) return null;

            org.json.JSONObject searchJson = new org.json.JSONObject(searchRespuesta);
            org.json.JSONObject queryObj = searchJson.optJSONObject("query");
            if (queryObj == null) return null;

            org.json.JSONArray searchArray = queryObj.optJSONArray("search");
            if (searchArray == null || searchArray.length() == 0) return null;

            String tituloEncontrado = searchArray.getJSONObject(0).optString("title");
            if (tituloEncontrado.isEmpty()) return null;

            String tituloLower = tituloEncontrado.toLowerCase(Locale.ROOT);
            if (tituloLower.contains("escudo") || tituloLower.contains("coat of arms") ||
                    tituloLower.contains("bandera") || tituloLower.contains("flag") ||
                    tituloLower.contains("municipality") || tituloLower.contains("province") ||
                    tituloLower.contains("forza aerea") || tituloLower.contains("fuerza aérea") ||
                    tituloLower.contains("type designator") || tituloLower.contains("designador")) {
                return null;
            }

            String imageEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=500&titles="
                    + java.net.URLEncoder.encode(tituloEncontrado, "UTF-8");
            String imageRespuesta = leerUrlHttp(imageEndpoint);
            if (imageRespuesta == null) return null;

            org.json.JSONObject jsonObject = new org.json.JSONObject(imageRespuesta);
            org.json.JSONObject query = jsonObject.optJSONObject("query");
            if (query == null) return null;

            org.json.JSONObject pages = query.optJSONObject("pages");
            if (pages == null || !pages.keys().hasNext()) return null;

            String primeraClave = pages.keys().next();
            org.json.JSONObject pagina = pages.optJSONObject(primeraClave);
            if (pagina == null) return null;

            org.json.JSONObject thumbnail = pagina.optJSONObject("thumbnail");
            if (thumbnail != null) {
                String sourceUrl = thumbnail.optString("source");
                String urlLower = sourceUrl.toLowerCase(Locale.ROOT);
                if (urlLower.contains("accident") || urlLower.contains("crash") ||
                        urlLower.contains("incident") || urlLower.contains("damage") ||
                        urlLower.contains("destroy") || urlLower.contains("fire") ||
                        urlLower.contains("overrun") || urlLower.contains("wrecks")) {
                    return null;
                }
                return sourceUrl;
            }
        } catch (Exception e) {
            android.util.Log.e("GestorAviones", "Error al buscar en Wikipedia (" + lang + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Ejecuta una petición HTTP GET síncrona añadiendo el User-Agent obligatorio exigido por las políticas de Wikipedia.
     */
    private String leerUrlHttp(String endpointUrl) {
        java.net.HttpURLConnection conexion = null;
        try {
            java.net.URL url = new java.net.URL(endpointUrl);
            conexion = (java.net.HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setRequestProperty("User-Agent", AvionGenerator.USER_AGENT);
            conexion.setConnectTimeout(3000);
            conexion.setReadTimeout(3000);

            if (conexion.getResponseCode() != 200) return null;

            java.io.BufferedReader lector = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conexion.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder respuesta = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                respuesta.append(linea);
            }
            lector.close();
            return respuesta.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    /**
     * Despliega un visor de imagen a pantalla completa con zoom fluido hacia el punto focal,
     * doble toque, arrastre sin saltos entre dedos y límites de borde para no perder la foto.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void abrirImagenAmpliada(Object fuenteImagen) {
        FrameLayout contenedor = new FrameLayout(this);
        contenedor.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        contenedor.setBackgroundColor(Color.BLACK);

        ImageView imgZoom = new ImageView(this);
        imgZoom.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imgZoom.setScaleType(ImageView.ScaleType.MATRIX);
        contenedor.addView(imgZoom);

        ImageButton btnCerrar = new ImageButton(this);
        FrameLayout.LayoutParams paramsBtn = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        paramsBtn.gravity = Gravity.TOP | Gravity.END;
        paramsBtn.setMargins(40, 40, 40, 40);
        btnCerrar.setLayoutParams(paramsBtn);
        btnCerrar.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnCerrar.setBackgroundColor(Color.parseColor("#99000000"));
        btnCerrar.setColorFilter(Color.WHITE);
        contenedor.addView(btnCerrar);

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(contenedor);
        dialog.setCancelable(true);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        final android.graphics.Matrix matriz = new android.graphics.Matrix();
        final float[] valoresMatriz = new float[9];
        final float[] escalaBase = {1.0f};
        final float[] escalaActual = {1.0f};
        final float[] ultimoX = {0f};
        final float[] ultimoY = {0f};
        final int[] punteroActivoId = {MotionEvent.INVALID_POINTER_ID};
        final boolean[] inicializado = {false};

        Runnable centrarImagenInicial = () -> {
            if (imgZoom.getDrawable() == null || imgZoom.getWidth() == 0 || imgZoom.getHeight() == 0) return;

            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();
            float imgW = imgZoom.getDrawable().getIntrinsicWidth();
            float imgH = imgZoom.getDrawable().getIntrinsicHeight();
            if (imgW <= 0 || imgH <= 0) return;

            float escala = Math.min(viewW / imgW, viewH / imgH);
            float dx = (viewW - imgW * escala) * 0.5f;
            float dy = (viewH - imgH * escala) * 0.5f;

            matriz.reset();
            matriz.postScale(escala, escala);
            matriz.postTranslate(dx, dy);
            imgZoom.setImageMatrix(matriz);

            escalaBase[0] = escala;
            escalaActual[0] = 1.0f;
            inicializado[0] = true;
        };

        Runnable ajustarLimitesBordes = () -> {
            if (imgZoom.getDrawable() == null) return;
            matriz.getValues(valoresMatriz);
            float transX = valoresMatriz[android.graphics.Matrix.MTRANS_X];
            float transY = valoresMatriz[android.graphics.Matrix.MTRANS_Y];
            float escalaReal = valoresMatriz[android.graphics.Matrix.MSCALE_X];

            float anchoContenido = imgZoom.getDrawable().getIntrinsicWidth() * escalaReal;
            float altoContenido = imgZoom.getDrawable().getIntrinsicHeight() * escalaReal;
            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();

            float correccionX = 0f;
            float correccionY = 0f;

            if (anchoContenido <= viewW) {
                correccionX = (viewW - anchoContenido) * 0.5f - transX;
            } else if (transX > 0) {
                correccionX = -transX;
            } else if (transX + anchoContenido < viewW) {
                correccionX = viewW - (transX + anchoContenido);
            }

            if (altoContenido <= viewH) {
                correccionY = (viewH - altoContenido) * 0.5f - transY;
            } else if (transY > 0) {
                correccionY = -transY;
            } else if (transY + altoContenido < viewH) {
                correccionY = viewH - (transY + altoContenido);
            }

            matriz.postTranslate(correccionX, correccionY);
            imgZoom.setImageMatrix(matriz);
        };

        ScaleGestureDetector detectorEscala = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!inicializado[0]) return false;
                float factor = detector.getScaleFactor();
                float nuevaEscala = escalaActual[0] * factor;
                nuevaEscala = Math.max(1.0f, Math.min(nuevaEscala, 6.0f));
                float factorAplicado = nuevaEscala / escalaActual[0];
                escalaActual[0] = nuevaEscala;

                matriz.postScale(factorAplicado, factorAplicado, detector.getFocusX(), detector.getFocusY());
                ajustarLimitesBordes.run();
                return true;
            }
        });

        android.view.GestureDetector detectorDobleToque = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!inicializado[0]) return true;
                if (escalaActual[0] > 1.05f) {
                    centrarImagenInicial.run();
                } else {
                    float factorObjetivo = 2.5f / escalaActual[0];
                    escalaActual[0] = 2.5f;
                    matriz.postScale(factorObjetivo, factorObjetivo, e.getX(), e.getY());
                    ajustarLimitesBordes.run();
                }
                return true;
            }
        });

        imgZoom.setOnTouchListener((v, event) -> {
            if (!inicializado[0]) centrarImagenInicial.run();
            detectorDobleToque.onTouchEvent(event);
            detectorEscala.onTouchEvent(event);

            final int accion = event.getActionMasked();
            switch (accion) {
                case MotionEvent.ACTION_DOWN: {
                    int index = event.getActionIndex();
                    ultimoX[0] = event.getX(index);
                    ultimoY[0] = event.getY(index);
                    punteroActivoId[0] = event.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (!detectorEscala.isInProgress() && escalaActual[0] > 1.0f) {
                        int pointerIndex = event.findPointerIndex(punteroActivoId[0]);
                        if (pointerIndex >= 0) {
                            float x = event.getX(pointerIndex);
                            float y = event.getY(pointerIndex);
                            matriz.postTranslate(x - ultimoX[0], y - ultimoY[0]);
                            ajustarLimitesBordes.run();
                            ultimoX[0] = x;
                            ultimoY[0] = y;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    punteroActivoId[0] = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    int pointerIndex = event.getActionIndex();
                    int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == punteroActivoId[0]) {
                        int nuevoIndice = (pointerIndex == 0) ? 1 : 0;
                        ultimoX[0] = event.getX(nuevoIndice);
                        ultimoY[0] = event.getY(nuevoIndice);
                        punteroActivoId[0] = event.getPointerId(nuevoIndice);
                    }
                    break;
                }
            }
            return true;
        });

        Object cargaFinal = fuenteImagen;
        if (fuenteImagen instanceof String) {
            String str = (String) fuenteImagen;
            if (str.startsWith("content://") || str.startsWith("file://") || str.startsWith("/")) {
                cargaFinal = android.net.Uri.parse(str);
            } else {
                cargaFinal = construirModeloGlide(str);
            }
        }

        Glide.with(this)
                .load(cargaFinal)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        imgZoom.post(centrarImagenInicial);
                        return false;
                    }
                })
                .into(imgZoom);

        dialog.show();
    }
}