// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad encargada de mostrar la ficha previa detallada de un vuelo detectado en el radar
 * o consultado desde un perfil público, permitiendo su avistamiento y registro en el hangar.
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
    private boolean modoSoloLectura;

    private Object fuenteImagenActual;
    private String urlRemotaWikipedia = null;
    private TextView txtAvisoSinFoto;

    /**
     * Inicializa la vista de detalle del vuelo, recupera los extras del Intent,
     * configura la telemetría inicial y lanza la resolución asíncrona de la aeronave.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_vuelo_radar);

        // Vinculación de componentes de la interfaz de usuario
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

        // Configuración del botón de retroceso
        btnVolver.setOnClickListener(v -> finish());

        // Configuración del evento táctil para ampliar la fotografía de la aeronave
        imgAvion.setOnClickListener(v -> {
            if (fuenteImagenActual != null) {
                abrirImagenAmpliada(fuenteImagenActual);
            }
        });

        // Extracción de parámetros enviados a través del Intent
        Intent intent = getIntent();
        modoSoloLectura = intent.getBooleanExtra("modo_solo_lectura", false);
        icao = intent.getStringExtra("icao");
        tipoIcao = intent.getStringExtra("tipo");
        matricula = intent.getStringExtra("matricula");

        String modeloCrudo = intent.getStringExtra("modelo");
        nombreModelo = (modeloCrudo != null)
                ? modeloCrudo.replaceAll("(?i)^(aeronave|avión|aircraft)\\s+", "").trim()
                : null;

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

        // Mostrar código ICAO formateado en la insignia superior
        String icaoFormateado = (icao != null && !icao.isEmpty()) ? icao.toUpperCase(Locale.ROOT) : "---";
        txtIcaoBadge.setText(getString(R.string.icao_badge_formato, icaoFormateado));

        // Construcción y pintado de la telemetría del vuelo en pantalla
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

        // Gestión de visibilidad del pie de página de avistamiento
        if (modoSoloLectura) {
            if (footerAvistar != null) footerAvistar.setVisibility(View.GONE);
        } else {
            configurarBotonAvistar(btnAvistar, txtEstadoDistancia);
        }

        // Textos provisionales mientras se resuelve la aeronave en segundo plano
        txtModelo.setText(nombreModelo != null && !nombreModelo.isEmpty()
                ? nombreModelo
                : getString(R.string.identificando_aeronave));
        txtEmpresa.setText(empresa != null && !empresa.isEmpty()
                ? empresa
                : getString(R.string.consultando_base_datos));
        txtSpecs.setText(getString(R.string.obteniendo_ficha_tecnica));
        aplicarEstiloRareza(txtRareza, "COMMON");

        // Ejecutor para peticiones de red secundarias en hilos de fondo
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final String empresaInicial = empresa;

        // Llamada al motor híbrido global pasando ICAO, matrícula, tipo y modelo de respaldo
        AvionGenerator.resolverAvionGlobalAsync(
                this,
                icao,
                matricula,
                tipoIcao,
                nombreModelo,
                categoriaRadar,
                avionResuelto -> {
                    if (isFinishing() || isDestroyed()) return;

                    nombreModelo = avionResuelto.getModelo();

                    // Actualización dinámica de la telemetría si el tipo ICAO se ha recuperado remotamente
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

                    // Si la aeronave es desconocida, se muestra la silueta por defecto sin buscar en Wikipedia
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
                        urlRemotaWikipedia = urlDirecta;
                        fuenteImagenActual = urlDirecta;
                        gestionarAvisoSinFoto(imgAvion, false);
                        Glide.with(this).load(urlDirecta).fitCenter().into(imgAvion);
                    } else {
                        String modeloWiki = AvionGenerator.limpiarModeloParaWiki(this, avionResuelto.getModelo());
                        if (modeloWiki.isEmpty()) {
                            urlRemotaWikipedia = null;
                            fuenteImagenActual = fallback;
                            gestionarAvisoSinFoto(imgAvion, true);
                            Glide.with(this).load(fallback).fitCenter().into(imgAvion);
                            return;
                        }

                        // Consulta asíncrona a Wikipedia para obtener una fotografía real del modelo
                        executor.execute(() -> {
                            String urlFotoWiki = obtenerUrlDesdeWikipedia(modeloWiki);
                            mainHandler.post(() -> {
                                if (isFinishing() || isDestroyed()) return;

                                if (urlFotoWiki != null && !urlFotoWiki.isEmpty()) {
                                    urlRemotaWikipedia = urlFotoWiki;
                                    fuenteImagenActual = urlFotoWiki;
                                    gestionarAvisoSinFoto(imgAvion, false);
                                    Glide.with(this).load(urlFotoWiki).fitCenter().into(imgAvion);
                                } else {
                                    urlRemotaWikipedia = null;
                                    fuenteImagenActual = fallback;
                                    gestionarAvisoSinFoto(imgAvion, true);
                                    Glide.with(this).load(fallback).fitCenter().into(imgAvion);
                                }
                            });
                        });
                    }
                }
        );
    }

    /**
     * Aplica colores y estilos personalizados a la insignia de rareza de la aeronave.
     */
    private void aplicarEstiloRareza(TextView badge, String rareza) {
        String r = rareza != null ? rareza.toUpperCase(Locale.ROOT) : "COMMON";
        badge.setText(AvionGenerator.traducirRareza(this, r));

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
     * Configura el comportamiento, texto y colores del botón de avistamiento
     * en función de la distancia GPS calculada con el usuario.
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
                .putString("tipo_seleccionado", tipoIcao)
                .putString("matricula_seleccionada", matricula)
                .putString("descripcion_seleccionada", nombreModelo)
                .putString("url_imagen_remota_seleccionada", urlRemotaWikipedia)
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
     * Despliega un diálogo flotante a pantalla completa para visualizar la imagen ampliada,
     * incorporando detectores de gestos táctiles para zoom por pellizco y desplazamiento libre.
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
        FrameLayout.LayoutParams paramsImg = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        paramsImg.gravity = Gravity.CENTER;
        imgZoom.setLayoutParams(paramsImg);
        imgZoom.setScaleType(ImageView.ScaleType.FIT_CENTER);
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

        com.bumptech.glide.RequestManager glideReq = Glide.with(this);
        if (fuenteImagen instanceof String) {
            glideReq.load((String) fuenteImagen).fitCenter().into(imgZoom);
        } else if (fuenteImagen instanceof Integer) {
            glideReq.load((Integer) fuenteImagen).fitCenter().into(imgZoom);
        } else {
            glideReq.load(fuenteImagen).fitCenter().into(imgZoom);
        }

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        final float[] escalaActual = {1.0f};
        final boolean[] estaArrastrando = {false};
        final float[] ultimaX = {0f};
        final float[] ultimaY = {0f};

        ScaleGestureDetector detectorEscala = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                escalaActual[0] *= detector.getScaleFactor();
                escalaActual[0] = Math.max(1.0f, Math.min(escalaActual[0], 5.0f));
                imgZoom.setScaleX(escalaActual[0]);
                imgZoom.setScaleY(escalaActual[0]);
                return true;
            }
        });

        imgZoom.setOnTouchListener((v, event) -> {
            detectorEscala.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ultimaX[0] = event.getRawX();
                    ultimaY[0] = event.getRawY();
                    estaArrastrando[0] = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (estaArrastrando[0] && escalaActual[0] > 1.0f) {
                        float deltaX = event.getRawX() - ultimaX[0];
                        float deltaY = event.getRawY() - ultimaY[0];
                        imgZoom.setTranslationX(imgZoom.getTranslationX() + deltaX);
                        imgZoom.setTranslationY(imgZoom.getTranslationY() + deltaY);
                        ultimaX[0] = event.getRawX();
                        ultimaY[0] = event.getRawY();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    estaArrastrando[0] = false;
                    if (escalaActual[0] <= 1.0f) {
                        imgZoom.setTranslationX(0f);
                        imgZoom.setTranslationY(0f);
                    }
                    break;
            }
            return true;
        });

        dialog.show();
    }
}