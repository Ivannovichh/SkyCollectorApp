// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

// Importación para suprimir advertencias de accesibilidad en listeners táctiles personalizados
import android.annotation.SuppressLint;
// Importación para crear cuadros de diálogo modales a pantalla completa (visor de zoom)
import android.app.Dialog;
// Importación para leer datos transmitidos entre actividades mediante Intents
import android.content.Intent;
// Importación para gestionar constantes y códigos hexadecimales de color
import android.graphics.Color;
// Importación para representar objetos dibujables en listeners de carga de imágenes
import android.graphics.drawable.Drawable;
// Importación para generar degradados de color programáticamente (tema Easter Egg)
import android.graphics.drawable.GradientDrawable;
// Importación para manejar el ciclo de vida de la actividad
import android.os.Bundle;
// Importación para publicar tareas de actualización de interfaz en el hilo principal
import android.os.Handler;
// Importación para vincular el Handler al bucle de mensajes principal
import android.os.Looper;
// Importación para configurar la alineación y gravedad de vistas dinámicas
import android.view.Gravity;
// Importación para procesar eventos táctiles de gestos y desplazamiento
import android.view.MotionEvent;
// Importación para detectar gestos de pellizco (pinch-to-zoom)
import android.view.ScaleGestureDetector;
// Importación de la clase base de todos los elementos visuales
import android.view.View;
// Importación para manipular contenedores y parámetros de diseño de vistas
import android.view.ViewGroup;
// Importación para el botón de acción de avistamiento
import android.widget.Button;
// Importación para crear contenedores superpuestos dinámicamente
import android.widget.FrameLayout;
// Importación para los botones gráficos de retroceso y cierre del visor
import android.widget.ImageButton;
// Importación para mostrar la fotografía o silueta de la aeronave
import android.widget.ImageView;
// Importación para mostrar los distintos bloques de texto y telemetría
import android.widget.TextView;
// Importación para mostrar avisos emergentes breves al usuario
import android.widget.Toast;

// Importación para indicar parámetros que pueden aceptar valores nulos
import androidx.annotation.Nullable;
// Importación de la clase base compatible para actividades modernas
import androidx.appcompat.app.AppCompatActivity;

// Importación de Glide para descarga, caché y renderizado de imágenes
import com.bumptech.glide.Glide;
// Importación para identificar el origen de datos de una imagen cargada
import com.bumptech.glide.load.DataSource;
// Importación para capturar excepciones durante la descarga de imágenes
import com.bumptech.glide.load.engine.GlideException;
// Importación para construir URLs personalizadas con cabeceras HTTP en Glide
import com.bumptech.glide.load.model.GlideUrl;
// Importación para añadir cabeceras personalizadas (User-Agent) a peticiones Glide
import com.bumptech.glide.load.model.LazyHeaders;
// Importación para escuchar eventos de éxito o fallo al cargar imágenes con Glide
import com.bumptech.glide.request.RequestListener;
// Importación para referenciar el destino de renderizado en callbacks de Glide
import com.bumptech.glide.request.target.Target;

// Importación para formatear fechas y horas de avistamientos previos
import java.text.SimpleDateFormat;
// Importación para instanciar marcas de tiempo como objetos Date
import java.util.Date;
// Importación para configurar formatos dependientes de idioma o región
import java.util.Locale;
// Importación para gestionar hilos de ejecución en segundo plano
import java.util.concurrent.ExecutorService;
// Importación para instanciar ejecutores de hilos
import java.util.concurrent.Executors;

// Importación de recursos XML del proyecto
import es.medac.skycollectorapp.R;
// Importación del motor global de identificación y utilidades de aeronaves
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad encargada de mostrar la ficha previa detallada de un vuelo detectado en el radar
 * o consultado desde un perfil público, aplicando estilo dorado exclusivo si es un Easter Egg.
 */
public class DetalleVueloRadarActivity extends AppCompatActivity {

    // Distancia máxima permitida en kilómetros desde el usuario para poder capturar un vuelo
    private static final float DISTANCIA_MAXIMA_AVISTAMIENTO_KM = MapaActivity.DISTANCIA_MAXIMA_AVISTAMIENTO_KM;

    // Código hexadecimal único del transpondedor ICAO24 de la aeronave
    private String icao;
    // Indicativo de llamada (callsign) emitido por el vuelo
    private String callsign;
    // Código de diseño de tipo de aeronave ICAO (ej. B738, A320)
    private String tipoIcao;
    // Matrícula oficial de registro de la aeronave
    private String matricula;
    // Nombre comercial del modelo de avión o vehículo
    private String nombreModelo;
    // Distancia calculada en kilómetros entre el usuario y el blanco
    private float distanciaKm;
    // Coordenada de latitud actual de la aeronave
    private double latAvion;
    // Coordenada de longitud actual de la aeronave
    private double lonAvion;
    // Indica si el usuario ya tiene registrado este avión en su hangar
    private boolean yaAvistado;
    // Indica si la pantalla se abrió desde un perfil ajeno/ranking sin opción de captura
    private boolean modoSoloLectura;
    // Indica si la entidad seleccionada corresponde a un Easter Egg terrestre
    private boolean esEasterEgg;

    // Referencia a la imagen actual mostrada (URL o recurso drawable) para ampliarla al pulsar
    private Object fuenteImagenActual;
    // URL de la fotografía obtenida de Wikipedia para reenviarla a AddAvionActivity si se captura
    private String urlRemotaWikipedia = null;
    // Etiqueta superpuesta que informa cuando no hay fotografía disponible en red
    private TextView txtAvisoSinFoto;

    /**
     * Inicializa la actividad y valida los parámetros recibidos en el Intent.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Carga el diseño XML correspondiente a la ficha de vuelo del radar
        setContentView(R.layout.activity_detalle_vuelo_radar);

        // Verifica que el código ICAO sea válido; si falta, cierra la pantalla con un aviso
        if (!validarDatosEntrada()) {
            // Muestra notificación de error de carga
            Toast.makeText(this, getString(R.string.error_cargar_vuelo), Toast.LENGTH_SHORT).show();
            // Finaliza la actividad actual
            finish();
            return;
        }

        // Vincula vistas, muestra los datos iniciales y lanza la resolución completa del modelo
        inicializarVistas();
    }

    /**
     * Comprueba que el código ICAO recibido en el Intent sea válido antes de cargar la interfaz.
     */
    private boolean validarDatosEntrada() {
        // Recupera el Intent con el que se abrió esta actividad
        Intent intent = getIntent();
        // Si el Intent es nulo, devuelve falso
        if (intent == null) return false;

        // Extrae el identificador ICAO24 y comprueba que no sea nulo ni esté en blanco
        icao = intent.getStringExtra("icao");
        return icao != null && !icao.trim().isEmpty();
    }

    /**
     * Enlaza las vistas del layout, muestra la telemetría inicial y lanza la resolución asíncrona del avión.
     */
    private void inicializarVistas() {
        // Vincula el botón superior de retroceso
        ImageButton btnVolver = findViewById(R.id.btn_volver_detalle_radar);
        // Vincula la imagen principal del vuelo
        ImageView imgAvion = findViewById(R.id.img_avion_detalle_radar);
        // Vincula la etiqueta insignia de rareza
        TextView txtRareza = findViewById(R.id.txt_rareza_detalle);
        // Vincula la insignia del código ICAO24
        TextView txtIcaoBadge = findViewById(R.id.txt_icao_badge_detalle);
        // Vincula el título del modelo o apodo
        TextView txtModelo = findViewById(R.id.txt_modelo_detalle_radar);
        // Vincula el subtítulo de aerolínea o fabricante
        TextView txtEmpresa = findViewById(R.id.txt_empresa_detalle_radar);
        // Vincula el bloque de datos de telemetría en tiempo real
        TextView txtTelemetria = findViewById(R.id.txt_telemetria_detalle_radar);
        // Vincula el bloque de especificaciones técnicas
        TextView txtSpecs = findViewById(R.id.txt_specs_detalle_radar);
        // Vincula el texto de estado de distancia GPS
        TextView txtEstadoDistancia = findViewById(R.id.txt_estado_distancia_detalle);
        // Vincula el botón principal para iniciar el avistamiento
        Button btnAvistar = findViewById(R.id.btn_avistar_desde_detalle);
        // Vincula el contenedor inferior del pie de página
        View footerAvistar = findViewById(R.id.footer_avistar_radar);

        // Configura el botón superior para volver al mapa o al perfil anterior
        btnVolver.setOnClickListener(v -> finish());

        // Permite abrir la fotografía en el visor de zoom a pantalla completa al tocarla
        imgAvion.setOnClickListener(v -> {
            if (fuenteImagenActual != null) {
                abrirImagenAmpliada(fuenteImagenActual);
            }
        });

        // Recupera el Intent y extrae los parámetros básicos de identificación
        Intent intent = getIntent();
        modoSoloLectura = intent.getBooleanExtra("modo_solo_lectura", false);
        callsign = intent.getStringExtra("callsign");
        tipoIcao = intent.getStringExtra("tipo");
        matricula = intent.getStringExtra("matricula");

        // Extrae y limpia prefijos genéricos del modelo preliminar recibido si existen
        String modeloCrudo = intent.getStringExtra("modelo");
        nombreModelo = (modeloCrudo != null)
                ? modeloCrudo.replaceAll("(?i)^(aeronave|avión|aircraft)\\s+", "").trim()
                : null;

        // Determina si la entidad seleccionada corresponde a un Easter Egg terrestre
        esEasterEgg = intent.getBooleanExtra("es_easter_egg", false)
                || AvionGenerator.esEasterEggTerrestre(tipoIcao, callsign, matricula, nombreModelo);

        // Recupera el resto de metadatos opcionales del vuelo o avistamiento público
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

        // Formatea y muestra la insignia con el código hexadecimal ICAO24 en mayúsculas
        String icaoFormateado = (icao != null && !icao.isEmpty()) ? icao.toUpperCase(Locale.ROOT) : "---";
        txtIcaoBadge.setText(getString(R.string.icao_badge_formato, icaoFormateado));

        // Construye el bloque de texto de telemetría según si es consulta pública o vuelo en directo
        StringBuilder telemetria = new StringBuilder();
        if (modoSoloLectura) {
            // Añade matrícula y tipo ICAO para la vista de solo lectura
            telemetria.append(getString(R.string.telemetria_matricula, matricula != null && !matricula.isEmpty() ? matricula : "—"))
                    .append(getString(R.string.telemetria_tipo_icao, tipoIcao != null && !tipoIcao.isEmpty() ? tipoIcao : "—"));
            // Si hay distancia de captura válida, la añade al resumen
            if (distanciaKm >= 0) {
                telemetria.append(getString(R.string.telemetria_distancia_captura, Math.round(distanciaKm)));
            }
            // Si hay marca temporal de captura, la formatea y la añade
            if (fechaAvistamiento > 0) {
                String fechaStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(fechaAvistamiento));
                telemetria.append(getString(R.string.telemetria_fecha_captura, fechaStr));
            }
        } else {
            // Si existe ruta estimada en radar, la incluye al inicio
            if (rutaTexto != null && !rutaTexto.isEmpty()) {
                telemetria.append(getString(R.string.telemetria_ruta_estimada, rutaTexto));
            }
            // Añade matrícula, tipo ICAO, velocidad en km/h y altitud actual
            telemetria.append(getString(R.string.telemetria_matricula, matricula != null ? matricula : "—"))
                    .append(getString(R.string.telemetria_tipo_icao, tipoIcao != null && !tipoIcao.isEmpty() ? tipoIcao : "—"))
                    .append(getString(R.string.telemetria_velocidad, velocidadKmh))
                    .append(getString(R.string.telemetria_altitud, altitudTexto != null ? altitudTexto : "—"));
        }
        // Vuelca el texto de telemetría construido en el TextView
        txtTelemetria.setText(telemetria.toString().trim());

        // Si es modo solo lectura (desde ranking/perfil ajeno), oculta la barra inferior de captura
        if (modoSoloLectura) {
            if (footerAvistar != null) footerAvistar.setVisibility(View.GONE);
        } else {
            // Configura el estado y disponibilidad del botón de avistamiento según distancia
            configurarBotonAvistar(btnAvistar, txtEstadoDistancia);
        }

        // Muestra textos provisionales mientras el motor híbrido resuelve todos los datos técnicos
        txtModelo.setText(nombreModelo != null && !nombreModelo.isEmpty()
                ? nombreModelo
                : getString(R.string.identificando_aeronave));
        txtEmpresa.setText(empresa != null && !empresa.isEmpty()
                ? empresa
                : getString(R.string.consultando_base_datos));
        txtSpecs.setText(getString(R.string.obteniendo_ficha_tecnica));
        // Aplica el estilo visual inicial de rareza
        aplicarEstiloRareza(txtRareza, esEasterEgg ? "EASTER_EGG" : "COMMON");

        // Si ya se detectó como Easter Egg, aplica inmediatamente la estética dorada
        if (esEasterEgg) {
            aplicarTemaDoradoEasterEgg();
        }

        // Prepara ejecutor en segundo plano y manejador del hilo principal para actualizar la UI
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final String empresaInicial = empresa;

        // Consulta el motor global de identificación (Catálogo local + APIs externas + Wikipedia)
        AvionGenerator.resolverAvionGlobalAsync(
                this,
                icao,
                callsign,
                matricula,
                tipoIcao,
                nombreModelo,
                categoriaRadar,
                avionResuelto -> {
                    // Aborta si la pantalla ya se cerró durante la consulta asíncrona
                    if (isFinishing() || isDestroyed()) return;

                    // Verifica si la resolución definitiva confirma que es un Easter Egg
                    if ("EASTER_EGG".equalsIgnoreCase(avionResuelto.getRareza()) || esEasterEgg) {
                        esEasterEgg = true;
                        aplicarTemaDoradoEasterEgg();
                    }

                    // Actualiza el nombre del modelo con el resultado oficial
                    nombreModelo = avionResuelto.getModelo();

                    // Si el radar no traía el código de tipo ICAO pero el motor lo dedujo, actualiza la telemetría
                    if (avionResuelto.getTipoIcao() != null && !avionResuelto.getTipoIcao().isEmpty()
                            && (tipoIcao == null || tipoIcao.trim().isEmpty())) {
                        tipoIcao = avionResuelto.getTipoIcao();
                        String textoActual = txtTelemetria.getText().toString();
                        String lineaVacia = getString(R.string.telemetria_tipo_icao, "—").trim();
                        String lineaNueva = getString(R.string.telemetria_tipo_icao, tipoIcao).trim();
                        txtTelemetria.setText(textoActual.replace(lineaVacia, lineaNueva));
                    }

                    // Determina el nombre del operador o fabricante a mostrar bajo el título
                    String operadorFinal = (empresaInicial != null && !empresaInicial.isEmpty())
                            ? empresaInicial
                            : avionResuelto.getFabricante();

                    // Si el usuario le puso un apodo personalizado distinto al modelo, lo destaca entre comillas
                    if (apodo != null && !apodo.equalsIgnoreCase(nombreModelo)) {
                        txtModelo.setText("«" + apodo + "»");
                        txtEmpresa.setText(nombreModelo + " • " + operadorFinal);
                    } else {
                        txtModelo.setText(nombreModelo);
                        txtEmpresa.setText(operadorFinal);
                    }

                    // Actualiza la insignia visual de rareza con el resultado definitivo
                    aplicarEstiloRareza(txtRareza, avionResuelto.getRareza());

                    // Formatea y muestra la ficha técnica completa según el modo de visualización
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

                    // Selecciona la silueta local de respaldo adecuada para este tipo de aeronave
                    int fallback = (avionResuelto.getImagenResId() != 0)
                            ? avionResuelto.getImagenResId()
                            : AvionGenerator.resolverImagenPorModelo(avionResuelto.getModelo(), avionResuelto.getTipoIcao());

                    // Comprueba si el modelo sigue sin poder identificarse
                    boolean esDesconocido = AvionGenerator.esModeloDesconocido(this, avionResuelto.getModelo());

                    // Si la aeronave no pudo identificarse, muestra directamente la silueta con el aviso sin foto
                    if (esDesconocido) {
                        urlRemotaWikipedia = null;
                        fuenteImagenActual = fallback;
                        gestionarAvisoSinFoto(imgAvion, true);
                        Glide.with(this).load(fallback).fitCenter().into(imgAvion);
                        return;
                    }

                    // Prioriza la foto pública del avistamiento o la URL directa resuelta por el motor
                    String urlDirecta = (fotoAvistamientoPublica != null && !fotoAvistamientoPublica.isEmpty())
                            ? fotoAvistamientoPublica
                            : avionResuelto.getUrlImagenRemota();

                    // Carga la imagen remota si existe, o lanza una búsqueda de respaldo en Wikipedia
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
        // Si la URL es nula, devuelve null
        if (url == null) return null;
        // Si pertenece a Wikimedia o Wikipedia, envuelve la URL con la cabecera User-Agent exigida
        if (url.contains("wikimedia.org") || url.contains("wikipedia.org")) {
            return new GlideUrl(
                    url,
                    new LazyHeaders.Builder()
                            .addHeader("User-Agent", AvionGenerator.USER_AGENT)
                            .build()
            );
        }
        // Para cualquier otro dominio, devuelve la URL plana
        return url;
    }

    /**
     * Descarga una URL remota en el ImageView con User-Agent compatible y, si falla,
     * intenta rescatarla mediante Wikipedia o muestra la silueta de respaldo con su aviso.
     */
    private void cargarImagenRemotaConRespaldo(ImageView imgAvion, String urlDirecta, int fallback,
                                               ExecutorService executor, Handler mainHandler) {
        // Almacena la URL remota como fuente activa y oculta el aviso de sin fotografía
        urlRemotaWikipedia = urlDirecta;
        fuenteImagenActual = urlDirecta;
        gestionarAvisoSinFoto(imgAvion, false);

        // Inicia la descarga con Glide escuchando posibles errores de carga
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
                        // Publica en el hilo principal una búsqueda alternativa en Wikipedia
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
                        // Permite que Glide renderice normalmente la imagen descargada
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
        // Prepara el término de búsqueda conservando el texto si es Easter Egg o limpiándolo si es avión
        String modeloWiki = esEasterEgg
                ? terminoRaw
                : AvionGenerator.limpiarModeloParaWiki(this, terminoRaw);

        // Si tras limpiar el nombre no queda término útil, muestra directamente la silueta
        if (modeloWiki == null || modeloWiki.isEmpty()) {
            urlRemotaWikipedia = null;
            fuenteImagenActual = fallback;
            gestionarAvisoSinFoto(imgAvion, true);
            Glide.with(this).load(fallback).fitCenter().into(imgAvion);
            return;
        }

        // Ejecuta la consulta HTTP fuera del hilo principal
        executor.execute(() -> {
            // Consulta Wikipedia libre para Easter Eggs o con filtro aeronáutico para aviones
            String urlFotoWiki = esEasterEgg
                    ? consultarImagenWikiLibre(modeloWiki)
                    : obtenerUrlDesdeWikipedia(modeloWiki);

            // Regresa al hilo principal para actualizar la imagen en pantalla
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (urlFotoWiki != null && !urlFotoWiki.isEmpty()) {
                    // Si obtuvo URL válida, la guarda como fuente actual y la renderiza con Glide
                    urlRemotaWikipedia = urlFotoWiki;
                    fuenteImagenActual = urlFotoWiki;
                    gestionarAvisoSinFoto(imgAvion, false);
                    Glide.with(this)
                            .load(construirModeloGlide(urlFotoWiki))
                            .error(fallback)
                            .fitCenter()
                            .into(imgAvion);
                } else {
                    // Si no obtuvo foto, muestra la silueta de respaldo junto con el aviso superpuesto
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
        // Define una lista ordenada de términos de búsqueda de mayor a menor especificidad
        String[] consultas = new String[]{termino, "Follow-me car", "Airport apron"};
        for (String consulta : consultas) {
            if (consulta == null || consulta.trim().isEmpty()) continue;
            try {
                // Consulta la API de búsqueda de Wikipedia en inglés para hallar el título exacto
                String searchEndpoint = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                        + java.net.URLEncoder.encode(consulta, "UTF-8") + "&format=json&srlimit=1";
                String searchResp = leerUrlHttp(searchEndpoint);
                if (searchResp == null) continue;

                // Extrae el título del primer resultado encontrado
                org.json.JSONObject searchJson = new org.json.JSONObject(searchResp);
                org.json.JSONObject queryObj = searchJson.optJSONObject("query");
                if (queryObj == null) continue;

                org.json.JSONArray searchArray = queryObj.optJSONArray("search");
                if (searchArray == null || searchArray.length() == 0) continue;

                String titulo = searchArray.getJSONObject(0).optString("title", "");
                if (titulo.isEmpty()) continue;

                // Consulta el endpoint REST summary del artículo para extraer su miniatura o imagen original
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
        // Devuelve null si ninguna consulta produjo imagen válida
        return null;
    }

    /**
     * Aplica colores y estilos personalizados a la insignia de rareza de la aeronave.
     */
    private void aplicarEstiloRareza(TextView badge, String rareza) {
        // Normaliza el texto de rareza a mayúsculas y asigna su traducción
        String r = rareza != null ? rareza.toUpperCase(Locale.ROOT) : "COMMON";
        badge.setText(AvionGenerator.traducirRareza(this, r));

        // Selecciona la combinación de fondo y texto según el nivel de rareza
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
        // Obtiene el contenedor raíz del contenido de la ventana
        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            // Aplica un degradado vertical dorado al layout principal
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            pantallaPrincipal.setBackground(fondoDorado);
        }

        // Localiza los textos y botones principales para aplicarles paleta dorada
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
        // Si el avión ya está en el hangar del usuario, desactiva el botón de captura
        if (yaAvistado) {
            btnAvistar.setEnabled(false);
            btnAvistar.setText(getString(R.string.btn_ya_avistado));
            txtEstadoDistancia.setText(getString(R.string.msg_ya_en_hangar));
            txtEstadoDistancia.setTextColor(Color.parseColor("#B0BEC5"));
            return;
        }

        // Muestra el estado de alcance según la distancia GPS del usuario al blanco
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

        // Registra el evento de clic para iniciar el proceso de avistamiento
        btnAvistar.setOnClickListener(v -> iniciarAvistamiento());
    }

    /**
     * Valida que el usuario esté dentro del rango de captura permitido y
     * almacena temporalmente los datos del avión antes de abrir la pantalla de registro.
     */
    private void iniciarAvistamiento() {
        // Comprueba que haya una lectura de distancia GPS válida
        if (distanciaKm < 0) {
            Toast.makeText(this, getString(R.string.toast_activar_gps), Toast.LENGTH_LONG).show();
            return;
        }

        // Verifica que el blanco esté dentro del radio máximo permitido
        if (distanciaKm > DISTANCIA_MAXIMA_AVISTAMIENTO_KM) {
            Toast.makeText(
                    this,
                    getString(R.string.toast_acercarse_rango, Math.round(distanciaKm), Math.round(DISTANCIA_MAXIMA_AVISTAMIENTO_KM)),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Guarda los metadatos del blanco en SharedPreferences para que AddAvionActivity los recoja
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

        // Abre la actividad de captura fotográfica y cierra esta pantalla previa
        Intent intent = new Intent(this, AddAvionActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra u oculta dinámicamente un aviso visual superpuesto cuando la aeronave
     * no dispone de una fotografía real en los repositorios externos.
     */
    private void gestionarAvisoSinFoto(ImageView imageView, boolean mostrar) {
        // Obtiene el contenedor padre del ImageView
        ViewGroup parent = (ViewGroup) imageView.getParent();
        if (parent == null) return;

        if (mostrar) {
            // Crea dinámicamente la etiqueta de aviso si aún no ha sido instanciada
            if (txtAvisoSinFoto == null) {
                txtAvisoSinFoto = new TextView(this);
                txtAvisoSinFoto.setText(getString(R.string.sin_fotografia_disponible));
                txtAvisoSinFoto.setTextColor(Color.WHITE);
                txtAvisoSinFoto.setTextSize(14f);
                txtAvisoSinFoto.setTypeface(null, android.graphics.Typeface.BOLD);
                txtAvisoSinFoto.setPadding(24, 12, 24, 12);
                txtAvisoSinFoto.setBackgroundColor(Color.parseColor("#CC1E293B"));
                txtAvisoSinFoto.setGravity(Gravity.CENTER);

                // Centra el aviso sobre el ImageView
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER;
                txtAvisoSinFoto.setLayoutParams(params);
                parent.addView(txtAvisoSinFoto);
            } else {
                // Vuelve visible la etiqueta si ya estaba creada
                txtAvisoSinFoto.setVisibility(View.VISIBLE);
            }
        } else {
            // Oculta la etiqueta de aviso si hay fotografía disponible
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
        // Evita consultas innecesarias si el modelo es desconocido
        if (AvionGenerator.esModeloDesconocido(this, terminoBusqueda)) return null;
        // Consulta primero en el idioma configurado en la aplicación
        String langPrincipal = AvionGenerator.obtenerCodigoIdiomaWiki();
        String url = consultarImagenWikiPorIdioma(terminoBusqueda, langPrincipal);
        // Si no se encuentra y el idioma principal no era inglés, reintenta en inglés
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
            // Construye la URL de búsqueda añadiendo el sufijo aeronáutico localizado
            String sufijo = getString(R.string.wiki_sufijo_busqueda);
            String searchEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + java.net.URLEncoder.encode(terminoBusqueda + sufijo, "UTF-8") + "&format=json&srlimit=1";
            String searchRespuesta = leerUrlHttp(searchEndpoint);
            if (searchRespuesta == null) return null;

            // Extrae el título del primer artículo coincidente
            org.json.JSONObject searchJson = new org.json.JSONObject(searchRespuesta);
            org.json.JSONObject queryObj = searchJson.optJSONObject("query");
            if (queryObj == null) return null;

            org.json.JSONArray searchArray = queryObj.optJSONArray("search");
            if (searchArray == null || searchArray.length() == 0) return null;

            String tituloEncontrado = searchArray.getJSONObject(0).optString("title");
            if (tituloEncontrado.isEmpty()) return null;

            // Descarta artículos correspondientes a escudos, banderas, divisiones territoriales o designadores
            String tituloLower = tituloEncontrado.toLowerCase(Locale.ROOT);
            if (tituloLower.contains("escudo") || tituloLower.contains("coat of arms") ||
                    tituloLower.contains("bandera") || tituloLower.contains("flag") ||
                    tituloLower.contains("municipality") || tituloLower.contains("province") ||
                    tituloLower.contains("forza aerea") || tituloLower.contains("fuerza aérea") ||
                    tituloLower.contains("type designator") || tituloLower.contains("designador")) {
                return null;
            }

            // Solicita la miniatura principal del artículo encontrado
            String imageEndpoint = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=500&titles="
                    + java.net.URLEncoder.encode(tituloEncontrado, "UTF-8");
            String imageRespuesta = leerUrlHttp(imageEndpoint);
            if (imageRespuesta == null) return null;

            // Navega por la respuesta JSON hasta obtener el objeto thumbnail
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
                // Filtra imágenes de accidentes o siniestros aéreos
                String urlLower = sourceUrl.toLowerCase(Locale.ROOT);
                if (urlLower.contains("accident") || urlLower.contains("crash") ||
                        urlLower.contains("incident") || urlLower.contains("damage") ||
                        urlLower.contains("destroy") || urlLower.contains("fire") ||
                        urlLower.contains("overrun") || urlLower.contains("wrecks")) {
                    return null;
                }
                // Devuelve la URL limpia de la miniatura
                return sourceUrl;
            }
        } catch (Exception e) {
            // Registra el error ocurrido durante la consulta a Wikipedia
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
            // Abre la conexión HTTP configurando método GET, User-Agent y tiempos de espera
            java.net.URL url = new java.net.URL(endpointUrl);
            conexion = (java.net.HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setRequestProperty("User-Agent", AvionGenerator.USER_AGENT);
            conexion.setConnectTimeout(3000);
            conexion.setReadTimeout(3000);

            // Comprueba que la respuesta del servidor sea HTTP 200 (OK)
            if (conexion.getResponseCode() != 200) return null;

            // Lee el flujo de respuesta en UTF-8 y lo acumula en un StringBuilder
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
            // Asegura el cierre de la conexión HTTP en cualquier escenario
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
        // Crea el contenedor raíz negro a pantalla completa
        FrameLayout contenedor = new FrameLayout(this);
        contenedor.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        contenedor.setBackgroundColor(Color.BLACK);

        // Crea el ImageView en modo MATRIX para soportar transformaciones de escala y traslación
        ImageView imgZoom = new ImageView(this);
        imgZoom.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imgZoom.setScaleType(ImageView.ScaleType.MATRIX);
        contenedor.addView(imgZoom);

        // Crea y posiciona el botón de cerrar en la esquina superior derecha
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

        // Crea el diálogo modal a pantalla completa y vincula el cierre
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(contenedor);
        dialog.setCancelable(true);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        // Variables de estado para transformaciones matriciales de escala y traslación
        final android.graphics.Matrix matriz = new android.graphics.Matrix();
        final float[] valoresMatriz = new float[9];
        final float[] escalaBase = {1.0f};
        final float[] escalaActual = {1.0f};
        final float[] ultimoX = {0f};
        final float[] ultimoY = {0f};
        final int[] punteroActivoId = {MotionEvent.INVALID_POINTER_ID};
        final boolean[] inicializado = {false};

        // Calcula la escala inicial para ajustar la imagen centrada dentro de la pantalla
        Runnable centrarImagenInicial = () -> {
            if (imgZoom.getDrawable() == null || imgZoom.getWidth() == 0 || imgZoom.getHeight() == 0) return;

            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();
            float imgW = imgZoom.getDrawable().getIntrinsicWidth();
            float imgH = imgZoom.getDrawable().getIntrinsicHeight();
            if (imgW <= 0 || imgH <= 0) return;

            // Calcula la escala mínima para encajar la imagen y el centrado horizontal y vertical
            float escala = Math.min(viewW / imgW, viewH / imgH);
            float dx = (viewW - imgW * escala) * 0.5f;
            float dy = (viewH - imgH * escala) * 0.5f;

            // Reinicia la matriz y aplica la transformación inicial
            matriz.reset();
            matriz.postScale(escala, escala);
            matriz.postTranslate(dx, dy);
            imgZoom.setImageMatrix(matriz);

            escalaBase[0] = escala;
            escalaActual[0] = 1.0f;
            inicializado[0] = true;
        };

        // Restringe los desplazamientos para evitar que la imagen salga de los bordes visibles
        Runnable ajustarLimitesBordes = () -> {
            if (imgZoom.getDrawable() == null) return;
            // Extrae los valores actuales de traslación y escala de la matriz
            matriz.getValues(valoresMatriz);
            float transX = valoresMatriz[android.graphics.Matrix.MTRANS_X];
            float transY = valoresMatriz[android.graphics.Matrix.MTRANS_Y];
            float escalaReal = valoresMatriz[android.graphics.Matrix.MSCALE_X];

            // Calcula el tamaño escalado actual de la imagen en pantalla
            float anchoContenido = imgZoom.getDrawable().getIntrinsicWidth() * escalaReal;
            float altoContenido = imgZoom.getDrawable().getIntrinsicHeight() * escalaReal;
            float viewW = imgZoom.getWidth();
            float viewH = imgZoom.getHeight();

            float correccionX = 0f;
            float correccionY = 0f;

            // Calcula la corrección horizontal para mantener la imagen centrada o dentro de los bordes
            if (anchoContenido <= viewW) {
                correccionX = (viewW - anchoContenido) * 0.5f - transX;
            } else if (transX > 0) {
                correccionX = -transX;
            } else if (transX + anchoContenido < viewW) {
                correccionX = viewW - (transX + anchoContenido);
            }

            // Calcula la corrección vertical para mantener la imagen centrada o dentro de los bordes
            if (altoContenido <= viewH) {
                correccionY = (viewH - altoContenido) * 0.5f - transY;
            } else if (transY > 0) {
                correccionY = -transY;
            } else if (transY + altoContenido < viewH) {
                correccionY = viewH - (transY + altoContenido);
            }

            // Aplica la corrección de traslación a la matriz
            matriz.postTranslate(correccionX, correccionY);
            imgZoom.setImageMatrix(matriz);
        };

        // Detector de gesto de pellizco para ampliar o reducir entre 1x y 6x
        ScaleGestureDetector detectorEscala = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!inicializado[0]) return false;
                // Calcula y acota el factor de escala dentro del rango permitido [1.0, 6.0]
                float factor = detector.getScaleFactor();
                float nuevaEscala = escalaActual[0] * factor;
                nuevaEscala = Math.max(1.0f, Math.min(nuevaEscala, 6.0f));
                float factorAplicado = nuevaEscala / escalaActual[0];
                escalaActual[0] = nuevaEscala;

                // Escala tomando como pivote el punto central entre los dedos
                matriz.postScale(factorAplicado, factorAplicado, detector.getFocusX(), detector.getFocusY());
                ajustarLimitesBordes.run();
                return true;
            }
        });

        // Detector de doble toque para alternar rápidamente entre zoom 2.5x y vista normal
        android.view.GestureDetector detectorDobleToque = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!inicializado[0]) return true;
                if (escalaActual[0] > 1.05f) {
                    // Restaura el centrado inicial si ya estaba ampliada
                    centrarImagenInicial.run();
                } else {
                    // Amplía a 2.5x sobre el punto pulsado
                    float factorObjetivo = 2.5f / escalaActual[0];
                    escalaActual[0] = 2.5f;
                    matriz.postScale(factorObjetivo, factorObjetivo, e.getX(), e.getY());
                    ajustarLimitesBordes.run();
                }
                return true;
            }
        });

        // Gestiona el movimiento táctil con un dedo y el cambio fluido entre punteros multitáctiles
        imgZoom.setOnTouchListener((v, event) -> {
            if (!inicializado[0]) centrarImagenInicial.run();
            detectorDobleToque.onTouchEvent(event);
            detectorEscala.onTouchEvent(event);

            final int accion = event.getActionMasked();
            switch (accion) {
                case MotionEvent.ACTION_DOWN: {
                    // Registra coordenadas iniciales y puntero activo al apoyar el primer dedo
                    int index = event.getActionIndex();
                    ultimoX[0] = event.getX(index);
                    ultimoY[0] = event.getY(index);
                    punteroActivoId[0] = event.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // Desplaza la imagen siguiendo al puntero activo cuando hay zoom aplicado
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
                    // Libera el puntero activo al finalizar el toque
                    punteroActivoId[0] = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    // Transfiere el control al segundo dedo sin saltos bruscos si se levanta el principal
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

        // Prepara la fuente final convirtiendo rutas locales en Uri o aplicando cabeceras GlideUrl
        Object cargaFinal = fuenteImagen;
        if (fuenteImagen instanceof String) {
            String str = (String) fuenteImagen;
            if (str.startsWith("content://") || str.startsWith("file://") || str.startsWith("/")) {
                cargaFinal = android.net.Uri.parse(str);
            } else {
                cargaFinal = construirModeloGlide(str);
            }
        }

        // Carga la imagen con Glide y ejecuta el centrado inicial en cuanto el recurso está listo
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
                        // Ejecuta el centrado tras el renderizado del ImageView
                        imgZoom.post(centrarImagenInicial);
                        return false;
                    }
                })
                .into(imgZoom);

        // Muestra el diálogo modal a pantalla completa
        dialog.show();
    }
}