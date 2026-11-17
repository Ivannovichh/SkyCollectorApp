// Declaración del paquete donde se ubica la actividad del chatbot
package es.medac.skycollectorapp.activities;

// Importaciones de Android necesarias
import android.os.Bundle; // Pases de estado de la actividad
import android.os.Handler; // Programación de tareas con retardo en el hilo principal
import android.os.Looper; // Bucle de mensajes asociado al hilo principal de interfaz
import android.widget.EditText; // Entrada de texto editable
import android.widget.ImageButton; // Botón con imagen
import androidx.appcompat.app.AppCompatActivity; // Clase base para actividades
import androidx.recyclerview.widget.LinearLayoutManager; // Layout lineal para listas
import androidx.recyclerview.widget.RecyclerView; // Lista eficiente para los mensajes

// Importaciones de la librería de red Volley
import com.android.volley.Request; // Clase base para métodos de petición HTTP
import com.android.volley.RequestQueue; // Gestor de la cola de peticiones HTTP
import com.android.volley.toolbox.JsonArrayRequest; // Peticiones con respuesta JSON Array
import com.android.volley.toolbox.JsonObjectRequest; // Peticiones con respuesta JSON Object
import com.android.volley.toolbox.Volley; // Clase fábrica de la librería Volley

// Importaciones para procesamiento de JSON
import org.json.JSONArray; // Arreglo de elementos en formato JSON
import org.json.JSONObject; // Objeto clave-valor en formato JSON

// Importaciones estándar de Java
import java.net.URLEncoder; // Codificador seguro de parámetros para URLs HTTP
import java.nio.charset.StandardCharsets; // Definición estándar de codificación UTF-8
import java.text.Normalizer; // Utilidad para normalización de tildes y diacríticos
import java.util.ArrayList; // Lista dinámica
import java.util.Iterator; // Iterador de claves JSON
import java.util.List; // Interfaz para listas

// Importaciones del proyecto
import es.medac.skycollectorapp.adapters.ChatAdapter; // Adaptador del chat
import es.medac.skycollectorapp.models.Mensaje; // Modelo del mensaje
import es.medac.skycollectorapp.R; // Recursos del proyecto
import es.medac.skycollectorapp.utils.AvionGenerator; // Utilidad para obtener el idioma activo de la app

// Clase principal que gestiona el asistente virtual e integrado con la Wikipedia bilingüe
public class ChatbotActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ Y DATOS
    // ------------------------------
    private RecyclerView recyclerChat; // Lista visual para la conversación
    private EditText etMensaje; // Caja de texto para escribir la consulta
    private ImageButton btnEnviar; // Botón para mandar el mensaje
    private ImageButton btnVolverChat; // Botón superior para regresar a la pantalla anterior
    private ChatAdapter adapter; // Adaptador que conecta los mensajes con la vista
    private List<Mensaje> mensajes; // Colección de mensajes en memoria
    private RequestQueue requestQueue; // Cola de procesamiento de peticiones de red Volley
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // Manejador vinculado al hilo principal

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Inicialización de la pantalla
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot); // Cargar layout del chatbot

        // Vincular componentes del XML con las variables locales
        recyclerChat = findViewById(R.id.recyclerChat);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnVolverChat = findViewById(R.id.btnVolverChat); // Enlazar botón de retroceso superior

        // Configurar acción del botón de retroceso para cerrar la pantalla y volver atrás
        btnVolverChat.setOnClickListener(v -> finish());

        // Inicializar la lista dinámica y el adaptador
        mensajes = new ArrayList<>();
        adapter = new ChatAdapter(mensajes);

        // Configurar el RecyclerView con orientación vertical
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        // Inicializar la cola de peticiones HTTP Volley
        requestQueue = Volley.newRequestQueue(this);

        // Enviar un mensaje de bienvenida traducido automáticamente al cargar la pantalla
        recibirMensaje(getString(R.string.chat_bienvenida));

        // Acción al pulsar el botón de envío
        btnEnviar.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim(); // Capturar texto ingresado
            if (!texto.isEmpty()) {
                enviarMensaje(texto); // Mostrar mensaje del usuario
                etMensaje.setText(""); // Limpiar la caja de texto
                mainHandler.postDelayed(() -> cerebroDelBot(texto), 500); // Procesar consulta con 0.5s de retardo
            }
        });
    }

    @Override
    protected void onDestroy() { // Limpieza de recursos al destruir la pantalla
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null); // Cancelar tareas pendientes en el hilo principal
        if (requestQueue != null) {
            requestQueue.cancelAll(req -> true); // Cancelar todas las peticiones de red activas
        }
    }

    // ------------------------------
    // LÓGICA DE INTERFAZ DEL CHAT
    // ------------------------------

    // Añade un mensaje enviado por el usuario a la lista visual
    private void enviarMensaje(String texto) {
        mensajes.add(new Mensaje(texto, true)); // Crear mensaje del usuario (esMio = true)
        adapter.notifyItemInserted(mensajes.size() - 1); // Notificar inserción al adaptador
        recyclerChat.scrollToPosition(mensajes.size() - 1); // Desplazar lista automáticamente al final
    }

    // Añade un mensaje recibido del bot a la lista visual
    private void recibirMensaje(String texto) {
        if (isDestroyed() || isFinishing()) return; // Evitar actualizar vistas si la actividad ya cerró
        mensajes.add(new Mensaje(texto, false)); // Crear mensaje del bot (esMio = false)
        adapter.notifyItemInserted(mensajes.size() - 1); // Notificar al adaptador
        recyclerChat.scrollToPosition(mensajes.size() - 1); // Desplazar lista automáticamente al final
    }

    // ------------------------------
    // LÓGICA DE PROCESAMIENTO Y RED
    // ------------------------------

    // Procesa la pregunta del usuario y lanza la búsqueda externa en el idioma activo
    private void cerebroDelBot(String pregunta) {
        String modeloLimpio = limpiarConsulta(pregunta); // Filtrar tildes, signos y palabras basura en ES/EN

        if (modeloLimpio.length() < 2) { // Verificar longitud mínima
            recibirMensaje(getString(R.string.chat_pedir_modelo));
            return;
        }

        recibirMensaje(getString(R.string.chat_buscando, modeloLimpio)); // Notificar inicio de búsqueda
        buscarTituloOficial(modeloLimpio, AvionGenerator.obtenerCodigoIdiomaWiki()); // Iniciar llamada en el idioma activo
    }

    // Limpia el texto de la pregunta eliminando tildes, signos de puntuación y palabras comunes tanto en español como en inglés
    private String limpiarConsulta(String textoBruto) {
        if (textoBruto == null) return "";

        // 1. Descomponer caracteres y eliminar marcas diacríticas (tildes)
        String sinTildes = Normalizer.normalize(textoBruto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();

        // 2. Sustituir caracteres especiales y signos de puntuación por espacios manteniendo guiones
        String limpio = sinTildes.replaceAll("[^a-z0-9\\s-]", " ");

        // 3. Colección de expresiones irrelevantes en español e inglés para aislar el término de búsqueda
        String[] basura = {
                "dame informacion sobre", "dame informacion de", "dame informacion",
                "informacion del", "informacion de", "informacion",
                "que es un", "que es el", "que es", "que son",
                "quiero saber del", "quiero saber sobre", "dame datos del", "dame datos de",
                "busca el", "busca la", "busca", "sobre el", "sobre la", "sobre",
                "give me information about", "information about", "tell me about",
                "what is an", "what is a", "what is the", "what is", "search for", "search",
                " el ", " la ", " los ", " las ", " un ", " una ", " unos ", " unas ",
                " the ", " an ", " a ", " about "
        };

        // Recorrer el arreglo de cadenas irrelevantes y reemplazarlas por espacios
        for (String b : basura) {
            limpio = limpio.replace(b, " ");
        }

        // Colapsar múltiples espacios seguidos en uno solo y eliminar espacios en los extremos
        return limpio.replaceAll("\\s+", " ").trim();
    }

    // Consulta el título oficial del artículo en Wikipedia en el idioma activo (con respaldo en inglés si no existe en español)
    private void buscarTituloOficial(String termino, String lang) {
        try {
            String terminoUrl = URLEncoder.encode(termino, StandardCharsets.UTF_8.name()); // Codificar parámetros de URL
            String url = "https://" + lang + ".wikipedia.org/w/api.php?action=opensearch&search=" + terminoUrl + "&limit=1&format=json";

            // Crear la petición de array JSON con Volley
            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray titulos = response.getJSONArray(1); // Extraer lista de títulos encontrados

                            if (titulos.length() > 0) {
                                String tituloOficial = titulos.getString(0); // Tomar la primera coincidencia
                                descargarContenido(tituloOficial, lang); // Descargar el resumen del artículo en ese idioma
                            } else if (!"en".equals(lang)) {
                                // Si no hubo resultados en español, reintentar automáticamente en inglés
                                buscarTituloOficial(termino, "en");
                            } else {
                                recibirMensaje(getString(R.string.chat_no_encontrado, termino));
                            }
                        } catch (Exception e) {
                            recibirMensaje(getString(R.string.chat_error_identificar));
                        }
                    },
                    error -> recibirMensaje(getString(R.string.chat_error_conexion))
            );
            requestQueue.add(request); // Añadir petición a la cola de Volley
        } catch (Exception e) {
            recibirMensaje(getString(R.string.chat_error_codificar));
        }
    }

    // Descarga el extracto introductorio del artículo localizado en Wikipedia
    private void descargarContenido(String tituloOficial, String lang) {
        try {
            String tituloUrl = URLEncoder.encode(tituloOficial, StandardCharsets.UTF_8.name()); // Codificar título para la URL
            String url = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" + tituloUrl + "&format=json&redirects=1";

            // Crear la petición de objeto JSON con Volley
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONObject query = response.getJSONObject("query"); // Obtener bloque query
                            JSONObject pages = query.getJSONObject("pages"); // Obtener bloque pages
                            Iterator<String> keys = pages.keys(); // Iterar sobre las claves dinámicas de las páginas

                            if (!keys.hasNext()) { // Evitar excepción si el JSON viene sin páginas
                                recibirMensaje(getString(R.string.chat_sin_detalles));
                                return;
                            }

                            String pageId = keys.next(); // Obtener la primera clave de página

                            if (!pageId.equals("-1")) { // Verificar que la página exista
                                JSONObject page = pages.getJSONObject(pageId);
                                String extracto = page.optString("extract", ""); // Extraer el texto introductorio de forma segura
                                String tituloReal = page.optString("title", tituloOficial); // Extraer el título formateado

                                if (!extracto.isEmpty()) {
                                    recibirMensaje(getString(R.string.chat_informe_formato, tituloReal.toUpperCase(), extracto));
                                } else {
                                    recibirMensaje(getString(R.string.chat_articulo_vacio, tituloReal));
                                }
                            } else {
                                recibirMensaje(getString(R.string.chat_error_datos));
                            }
                        } catch (Exception e) {
                            recibirMensaje(getString(R.string.chat_error_leer));
                        }
                    },
                    error -> recibirMensaje(getString(R.string.chat_error_red))
            );
            requestQueue.add(request); // Registrar la petición final en la cola de red
        } catch (Exception e) {
            recibirMensaje(getString(R.string.chat_error_solicitar));
        }
    }
}