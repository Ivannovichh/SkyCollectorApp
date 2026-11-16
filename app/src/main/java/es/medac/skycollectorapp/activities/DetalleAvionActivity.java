// Declaración del paquete donde se encuentra ubicada la actividad
package es.medac.skycollectorapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad que gestiona la visualización, edición de apodos y fotos personalizadas de un avión en la colección,
 * aplicando estilo dorado y foto de operaciones si se trata de un Easter Egg.
 */
public class DetalleAvionActivity extends AppCompatActivity {

    private ImageView imgDetalleGrande;
    private ImageView imgFotoUsuario;
    private EditText etNombreAvion;

    private TextView txtDetalleFabricante, txtDetallePais, txtDetalleVelocidad,
            txtDetalleCapacidad, txtDetallePeso, txtDetalleDimensiones;

    private Button btnCamara, btnGaleria;
    private Button btnGuardar, btnCancelar;
    private CardView layoutFotoUsuario;

    private Avion avion;
    private Uri uriFotoTemporal;
    private String avionId;

    private Object fuenteImagenCatalogoActual;
    private Object fuenteFotoUsuarioActual;
    private TextView txtAvisoSinFoto;

    private final ActivityResultLauncher<Uri> launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            exito -> {
                if (exito && uriFotoTemporal != null) {
                    mostrarFotoUsuario(uriFotoTemporal, true);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) abrirCamara();
                else Toast.makeText(this, getString(R.string.toast_permiso_denegado), Toast.LENGTH_SHORT).show();
            }
    );

    private final ActivityResultLauncher<String> launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception e) {
                        Log.w("URI_PERM", "El proveedor no soporta persistencia de URI", e);
                    }
                    mostrarFotoUsuario(uri, false);
                }
            }
    );

    /**
     * Inicializa la actividad, recupera la aeronave desde la colección local y configura los eventos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_avion);

        initViews();

        avionId = getIntent().getStringExtra("avion_id");

        if (avionId == null) {
            Toast.makeText(this, getString(R.string.toast_error_sin_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        avion = buscarAvionPorId(avionId);

        if (avion == null) {
            Toast.makeText(this, getString(R.string.toast_error_no_encontrado), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarDatos();
        setupListeners();
    }

    /**
     * Genera la clave de almacenamiento local asociada al UID del usuario autenticado.
     */
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    /**
     * Vincula las referencias de los componentes del archivo XML y ajusta los márgenes de la imagen principal.
     */
    private void initViews() {
        imgDetalleGrande = findViewById(R.id.imgDetalleGrande);

        ViewGroup.MarginLayoutParams imgParams = (ViewGroup.MarginLayoutParams) imgDetalleGrande.getLayoutParams();
        if (imgParams != null) {
            imgParams.setMargins(0, imgParams.topMargin, 0, imgParams.bottomMargin);
            imgDetalleGrande.setLayoutParams(imgParams);
        }
        imgDetalleGrande.setPadding(0, 0, 0, 0);
        imgDetalleGrande.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (imgDetalleGrande.getParent() instanceof CardView) {
            CardView cardPadre = (CardView) imgDetalleGrande.getParent();
            cardPadre.setContentPadding(0, 0, 0, 0);
        }

        etNombreAvion = findViewById(R.id.etNombreAvion);

        txtDetalleFabricante = findViewById(R.id.txtDetalleFabricante);
        txtDetallePais = findViewById(R.id.txtDetallePais);
        txtDetalleVelocidad = findViewById(R.id.txtDetalleVelocidad);
        txtDetalleCapacidad = findViewById(R.id.txtDetalleCapacidad);
        txtDetallePeso = findViewById(R.id.txtDetallePeso);
        txtDetalleDimensiones = findViewById(R.id.txtDetalleDimensiones);

        btnCamara = findViewById(R.id.btnCamara);
        btnGaleria = findViewById(R.id.btnGaleria);

        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);

        layoutFotoUsuario = findViewById(R.id.layoutFotoUsuario);
        imgFotoUsuario = findViewById(R.id.imgFotoUsuario);
    }

    /**
     * Configura las acciones asociadas a los clics de los botones y vistas de imagen.
     */
    private void setupListeners() {
        btnCancelar.setOnClickListener(v -> finish());

        btnGuardar.setOnClickListener(v -> {
            String nuevoApodo = etNombreAvion.getText().toString().trim();
            if (!nuevoApodo.isEmpty()) {
                avion.setApodo(nuevoApodo);
            }

            guardarCambiosEnPreferencias(avion);
            guardarCambiosEnFirestore(avion);

            Toast.makeText(this, getString(R.string.toast_cambios_guardados), Toast.LENGTH_SHORT).show();
            finish();
        });

        btnCamara.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGaleria.setOnClickListener(v -> launcherGaleria.launch("image/*"));

        imgDetalleGrande.setOnClickListener(v -> {
            if (fuenteImagenCatalogoActual != null) {
                abrirImagenAmpliada(fuenteImagenCatalogoActual);
            }
        });

        imgFotoUsuario.setOnClickListener(v -> {
            if (fuenteFotoUsuarioActual != null) {
                abrirImagenAmpliada(fuenteFotoUsuarioActual);
            }
        });
    }

    /**
     * Aplica un acabado visual dorado exclusivo cuando el elemento avistado es un Easter Egg.
     */
    private void aplicarEstiloDoradoSiEsEasterEgg() {
        if (avion == null) return;
        boolean esEasterEgg = "EASTER_EGG".equalsIgnoreCase(avion.getRareza())
                || AvionGenerator.esEasterEggTerrestre(avion.getTipoIcao(), null, avion.getMatricula(), avion.getModelo());

        if (!esEasterEgg) return;

        View root = findViewById(android.R.id.content);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) {
            View pantallaPrincipal = ((ViewGroup) root).getChildAt(0);
            GradientDrawable fondoDorado = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF3B2800, 0xFF6B4C00, 0xFF1F1400}
            );
            pantallaPrincipal.setBackground(fondoDorado);
        }

        if (etNombreAvion != null) {
            etNombreAvion.setTextColor(0xFFFFD700);
        }
        if (btnGuardar != null) {
            btnGuardar.setBackgroundColor(0xFFFFD700);
            btnGuardar.setTextColor(0xFF1F1400);
        }
    }

    /**
     * Rellena la interfaz gráfica con los datos técnicos y fotografías del objeto Avion cargado.
     */
    private void cargarDatos() {
        aplicarEstiloDoradoSiEsEasterEgg();

        boolean esEasterEgg = "EASTER_EGG".equalsIgnoreCase(avion.getRareza())
                || AvionGenerator.esEasterEggTerrestre(avion.getTipoIcao(), null, avion.getMatricula(), avion.getModelo());

        if (esEasterEgg && (avion.getImagenUrl() == null || avion.getImagenUrl().isEmpty())) {
            avion.setImagenUrl(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO);
            avion.setUrlImagenRemota(AvionGenerator.URL_FOTO_EASTER_EGG_VEHICULO);
            guardarCambiosEnPreferencias(avion);
            guardarCambiosEnFirestore(avion);
        }

        etNombreAvion.setText(avion.getApodo());

        txtDetalleFabricante.setText(getString(R.string.detalle_fabricante_formato, avion.getFabricante()));
        txtDetallePais.setText(getString(R.string.detalle_pais_formato, avion.getPais()));
        txtDetalleVelocidad.setText(getString(R.string.detalle_velocidad_formato, avion.getVelocidad()));
        txtDetalleCapacidad.setText(getString(R.string.detalle_capacidad_formato, avion.getPasajeros()));
        txtDetallePeso.setText(getString(R.string.detalle_peso_formato, avion.getPeso()));
        txtDetalleDimensiones.setText(getString(R.string.detalle_dimensiones_formato, avion.getDimensiones()));

        String nombreModeloCrudo = avion.getModelo() != null ? avion.getModelo() : getString(R.string.aeronave_desconocida);

        if (avion.getImagenUrl() != null && !avion.getImagenUrl().isEmpty()) {
            fuenteImagenCatalogoActual = avion.getImagenUrl();
            gestionarAvisoSinFoto(imgDetalleGrande, false);
            Glide.with(this)
                    .load(avion.getImagenUrl())
                    .centerCrop()
                    .into(imgDetalleGrande);
        } else {
            String modeloLimpioParaWiki = limpiarNombreParaBusqueda(nombreModeloCrudo);
            cargarImagenAutomaticaDesdeInternet(modeloLimpioParaWiki, imgDetalleGrande, avion.getImagenResId());
        }

        if (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty()) {
            layoutFotoUsuario.setVisibility(View.VISIBLE);
            fuenteFotoUsuarioActual = avion.getUriFotoUsuario();

            Glide.with(this)
                    .load(Uri.parse(avion.getUriFotoUsuario()))
                    .centerCrop()
                    .into(imgFotoUsuario);
        } else {
            layoutFotoUsuario.setVisibility(View.GONE);
            fuenteFotoUsuarioActual = null;
        }
    }

    /**
     * Refresca la vista, asigna la nueva foto al objeto Avion y actualiza su estado de verificación.
     */
    private void mostrarFotoUsuario(Uri uri, boolean esDesdeCamara) {
        avion.setUriFotoUsuario(uri.toString());
        avion.setEsFotoVerificada(esDesdeCamara);
        layoutFotoUsuario.setVisibility(View.VISIBLE);
        fuenteFotoUsuarioActual = uri.toString();

        Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .into(imgFotoUsuario);
    }

    /**
     * Prepara el archivo privado y lanza la aplicación de cámara.
     */
    private void abrirCamara() {
        try {
            File archivo = crearArchivoImagen();
            uriFotoTemporal = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            launcherCamara.launch(uriFotoTemporal);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_error_camara), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Genera un archivo JPG temporal en el directorio persistente privado.
     */
    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir != null && !dir.exists()) dir.mkdirs();

        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", dir);
    }

    /**
     * Busca una aeronave dentro del almacenamiento JSON local del usuario por su identificador ID.
     */
    private Avion buscarAvionPorId(String id) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        String json = prefs.getString(getClaveListaUsuario(), null);
        if (json == null) return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return null;

        for (Avion a : lista) {
            if (a.getId() != null && a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Actualiza la versión modificada del avión en la lista de SharedPreferences local del usuario.
     */
    private void guardarCambiosEnPreferencias(Avion avionActualizado) {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", MODE_PRIVATE);
        Gson gson = new Gson();
        String clave = getClaveListaUsuario();

        String json = prefs.getString(clave, null);
        if (json == null) return;

        Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
        ArrayList<Avion> lista = gson.fromJson(json, type);
        if (lista == null) return;

        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId().equals(avionActualizado.getId())) {
                lista.set(i, avionActualizado);
                break;
            }
        }

        prefs.edit().putString(clave, gson.toJson(lista)).apply();
    }

    /**
     * Sincroniza las modificaciones del avión en la base de datos Firestore en la nube.
     */
    private void guardarCambiosEnFirestore(Avion avionActualizado) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avionActualizado.getId())
                .set(avionActualizado)
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Cambios del avión actualizados en Firestore: " + avionActualizado.getApodo()))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al actualizar cambios en Firestore", e));
    }

    /**
     * Limpia prefijos genéricos y simplifica familias de modelos para optimizar la búsqueda en Wikipedia.
     */
    private String limpiarNombreParaBusqueda(String modelo) {
        if (modelo == null || modelo.isEmpty()) return "";

        String limpio = AvionGenerator.limpiarModeloParaWiki(this, modelo);
        String mayus = limpio.toUpperCase(Locale.ROOT);

        if (mayus.contains("737")) return "Boeing 737";
        if (mayus.contains("A320")) return "Airbus A320";
        if (mayus.contains("A321")) return "Airbus A321";
        if (mayus.contains("A319")) return "Airbus A319";
        if (mayus.contains("A330")) return "Airbus A330";

        return limpio.replaceAll("(?i)(-\\d+|\\b\\d+[A-Z]{2,}\\b|\\/.*)", "").trim();
    }

    /**
     * Muestra u oculta un aviso visual superpuesto cuando no hay fotografía real disponible.
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
     * Obtiene automáticamente la fotografía desde Wikipedia en segundo plano y la persiste si tiene éxito.
     */
    private void cargarImagenAutomaticaDesdeInternet(String terminoBusqueda, ImageView imageView, int recursoFallback) {
        fuenteImagenCatalogoActual = recursoFallback;

        if (terminoBusqueda == null || terminoBusqueda.isEmpty()
                || AvionGenerator.esModeloDesconocido(this, terminoBusqueda)) {
            Glide.with(this).load(recursoFallback).centerCrop().into(imageView);
            gestionarAvisoSinFoto(imageView, true);
            return;
        }

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            String langPrincipal = AvionGenerator.obtenerCodigoIdiomaWiki();
            String urlImagenRemota = obtenerUrlDesdeWikipedia(terminoBusqueda, langPrincipal);
            if ((urlImagenRemota == null || urlImagenRemota.isEmpty()) && !"en".equals(langPrincipal)) {
                urlImagenRemota = obtenerUrlDesdeWikipedia(terminoBusqueda, "en");
            }

            final String urlFinal = urlImagenRemota;
            mainHandler.post(() -> {
                if (urlFinal != null && !urlFinal.isEmpty()) {
                    fuenteImagenCatalogoActual = urlFinal;

                    avion.setImagenUrl(urlFinal);
                    guardarCambiosEnPreferencias(avion);
                    guardarCambiosEnFirestore(avion);

                    gestionarAvisoSinFoto(imageView, false);
                    Glide.with(this)
                            .load(urlFinal)
                            .centerCrop()
                            .into(imageView);
                } else {
                    fuenteImagenCatalogoActual = recursoFallback;
                    gestionarAvisoSinFoto(imageView, true);
                    Glide.with(this)
                            .load(recursoFallback)
                            .centerCrop()
                            .into(imageView);
                }
            });
        });
    }

    /**
     * Consulta la API de búsqueda e imágenes de Wikipedia filtrando resultados no válidos.
     */
    private String obtenerUrlDesdeWikipedia(String terminoBusqueda, String lang) {
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
            android.util.Log.e("GestorAviones", "Error al buscar en Wikipedia: " + e.getMessage());
        }
        return null;
    }

    /**
     * Realiza una petición HTTP GET segura con User-Agent y cierre garantizado de conexión.
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
            }
        }

        Glide.with(this)
                .load(cargaFinal)
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model,
                                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                                   com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                   com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        imgZoom.post(centrarImagenInicial);
                        return false;
                    }
                })
                .into(imgZoom);

        dialog.show();
    }
}