// Declaración del paquete donde se ubica la actividad del chatbot
package es.medac.skycollectorapp.activities;

// Importaciones de Android necesarias
import android.os.Bundle; // Pases de estado de la actividad
import android.os.Handler; // Programación de tareas con retardo en el hilo principal
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
import org.json.JSONArray;
import org.json.JSONObject;

// Importaciones estándar de Java
import java.text.Normalizer; // Utilidad para normalización de tildes y diacríticos
import java.util.ArrayList; // Lista dinámica
import java.util.Iterator; // Iterador de claves JSON
import java.util.List; // Interfaz para listas

// Importaciones del proyecto
import es.medac.skycollectorapp.adapters.ChatAdapter; // Adaptador del chat
import es.medac.skycollectorapp.models.Mensaje; // Modelo del mensaje
import es.medac.skycollectorapp.R; // Recursos del proyecto

// Clase principal que gestiona el asistente virtual e integrado con la Wikipedia
public class ChatbotActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ Y DATOS
    // ------------------------------
    private RecyclerView recyclerChat; // Lista visual para la conversación
    private EditText etMensaje; // Caja de texto para escribir la consulta
    private ImageButton btnEnviar; // Botón para mandar el mensaje
    private ChatAdapter adapter; // Adaptador que conecta los mensajes con la vista
    private List<Mensaje> mensajes; // Colección de mensajes en memoria
    private RequestQueue requestQueue; // Cola de procesamiento de peticiones de red Volley

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

        // Inicializar la lista dinámica y el adaptador
        mensajes = new ArrayList<>();
        adapter = new ChatAdapter(mensajes);

        // Configurar el RecyclerView con orientación vertical
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        // Inicializar la cola de peticiones HTTP Volley
        requestQueue = Volley.newRequestQueue(this);

        // Enviar un mensaje de bienvenida automático al cargar la pantalla
        recibirMensaje("Sistema online. Pregúntame por un modelo (Ej: F18, Boeing 737) y buscaré su ficha.");

        // Acción al pulsar el botón de envío
        btnEnviar.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim(); // Capturar texto ingresado
            if (!texto.isEmpty()) {
                enviarMensaje(texto); // Mostrar mensaje del usuario
                etMensaje.setText(""); // Limpiar la caja de texto
                new Handler().postDelayed(() -> cerebroDelBot(texto), 500); // Procesar consulta con 0.5s de retardo
            }
        });
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
        mensajes.add(new Mensaje(texto, false)); // Crear mensaje del bot (esMio = false)
        adapter.notifyItemInserted(mensajes.size() - 1); // Notificar al adaptador
        recyclerChat.scrollToPosition(mensajes.size() - 1); // Desplazar lista automáticamente al final
    }

    // ------------------------------
    // LÓGICA DE PROCESAMIENTO Y RED
    // ------------------------------

    // Procesa la pregunta del usuario y lanza la búsqueda externa
    private void cerebroDelBot(String pregunta) {
        String modeloLimpio = limpiarConsulta(pregunta); // Filtrar tildes, signos y palabras basura

        if (modeloLimpio.length() < 2) { // Verificar longitud mínima
            recibirMensaje("Por favor, escribe el modelo del avión.");
            return;
        }

        recibirMensaje("Localizando ficha oficial de '" + modeloLimpio + "'..."); // Notificar inicio de búsqueda
        buscarTituloOficial(modeloLimpio); // Iniciar primera llamada a la API
    }

    // Limpia el texto de la pregunta eliminando tildes, signos de puntuación y palabras comunes
    private String limpiarConsulta(String textoBruto) {
        if (textoBruto == null) return "";

        // 1. Descomponer caracteres y eliminar marcas diacríticas (tildes)
        String sinTildes = Normalizer.normalize(textoBruto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();

        // 2. Sustituir caracteres especiales y signos de puntuación por espacios
        String limpio = sinTildes.replaceAll("[^a-z0-9\\s]", " ");

        // 3. Colección de expresiones irrelevantes para aislar el término de búsqueda
        String[] basura = {
                "dame informacion sobre", "dame informacion de", "dame informacion",
                "informacion del", "informacion de", "informacion",
                "que es un", "que es el", "que es", "que son",
                "quiero saber del", "quiero saber sobre", "dame datos del", "dame datos de",
                "busca el", "busca la", "busca", "sobre el", "sobre la", "sobre",
                "caza", "bombardero", "avion", "jet",
                " el ", " la ", " los ", " las ", " un ", " una ", " unos ", " unas "
        };

        // Recorrer el arreglo de cadenas irrelevantes y reemplazarlas por espacios
        for (String b : basura) {
            limpio = limpio.replace(b, " ");
        }

        // Colapsar múltiples espacios seguidos en uno solo y eliminar espacios en los extremos
        return limpio.replaceAll("\\s+", " ").trim();
    }

    // Consulta el título oficial del artículo en Wikipedia utilizando la API OpenSearch
    private void buscarTituloOficial(String termino) {
        String terminoUrl = termino.replace(" ", "%20"); // Codificar espacios para la URL
        String url = "https://es.wikipedia.org/w/api.php?action=opensearch&search=" + terminoUrl + "&limit=1&format=json";

        // Crear la petición de array JSON con Volley
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray titulos = response.getJSONArray(1); // Extraer lista de títulos encontrados

                        if (titulos.length() > 0) {
                            String tituloOficial = titulos.getString(0); // Tomar la primera coincidencia
                            descargarContenido(tituloOficial); // Descargar el resumen del artículo
                        } else {
                            recibirMensaje("No encuentro ningún avión llamado '" + termino + "'. Prueba con el nombre completo.");
                        }
                    } catch (Exception e) {
                        recibirMensaje("Error al identificar el avión.");
                    }
                },
                error -> recibirMensaje("Error de conexión a internet.")
        );
        requestQueue.add(request); // Añadir petición a la cola de Volley
    }

    // Descarga el extracto introductorio del artículo localizado en Wikipedia
    private void descargarContenido(String tituloOficial) {
        String tituloUrl = tituloOficial.replace(" ", "%20"); // Codificar título para la URL
        String url = "https://es.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" + tituloUrl + "&format=json&redirects=1";

        // Crear la petición de objeto JSON con Volley
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject query = response.getJSONObject("query"); // Obtener bloque query
                        JSONObject pages = query.getJSONObject("pages"); // Obtener bloque pages
                        Iterator<String> keys = pages.keys(); // Iterar sobre las claves dinámicas de las páginas
                        String pageId = keys.next(); // Obtener la primera clave de página

                        if (!pageId.equals("-1")) { // Verificar que la página exista
                            JSONObject page = pages.getJSONObject(pageId);
                            String extracto = page.getString("extract"); // Extraer el texto introductorio
                            String tituloReal = page.getString("title"); // Extraer el título formateado

                            if (extracto != null && !extracto.isEmpty()) {
                                recibirMensaje("INFORME: " + tituloReal.toUpperCase() + "\n\n" + extracto);
                            } else {
                                recibirMensaje("He encontrado el artículo '" + tituloReal + "' pero parece estar vacío.");
                            }
                        } else {
                            recibirMensaje("Error al recuperar los datos.");
                        }
                    } catch (Exception e) {
                        recibirMensaje("Error al leer el informe.");
                    }
                },
                error -> recibirMensaje("Error de red.")
        );
        requestQueue.add(request); // Registrar la petición final en la cola de red
    }
}