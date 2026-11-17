// Declaración del paquete donde se ubica la actividad del asistente virtual
package es.medac.skycollectorapp.activities;

// Importación de Bundle para recibir y restaurar el estado guardado de la actividad
import android.os.Bundle;
// Importación de Handler para programar ejecuciones diferidas (ej. simular tiempo de respuesta del bot)
import android.os.Handler;
// Importación de Looper para vincular el Handler directamente al hilo principal de interfaz de usuario
import android.os.Looper;
// Importación de EditText para capturar el texto escrito por el usuario en la barra inferior
import android.widget.EditText;
// Importación de ImageButton para los botones gráficos de enviar consulta y regresar atrás
import android.widget.ImageButton;
// Importación de AppCompatActivity como clase base compatible para pantallas modernas de Android
import androidx.appcompat.app.AppCompatActivity;
// Importación de LinearLayoutManager para organizar los mensajes del chat en una lista vertical secuencial
import androidx.recyclerview.widget.LinearLayoutManager;
// Importación de RecyclerView para renderizar eficientemente el historial dinámico de mensajes
import androidx.recyclerview.widget.RecyclerView;

// Importación de Request para especificar los métodos HTTP estándar (como GET)
import com.android.volley.Request;
// Importación de RequestQueue para administrar la cola asíncrona de peticiones de red
import com.android.volley.RequestQueue;
// Importación de JsonArrayRequest para procesar respuestas de API que devuelven un arreglo JSON raíz
import com.android.volley.toolbox.JsonArrayRequest;
// Importación de JsonObjectRequest para procesar respuestas de API que devuelven un objeto JSON raíz
import com.android.volley.toolbox.JsonObjectRequest;
// Importación de Volley como clase fábrica encargada de instanciar colas de red
import com.android.volley.toolbox.Volley;

// Importación de JSONArray para navegar por listas indexadas dentro de respuestas JSON
import org.json.JSONArray;
// Importación de JSONObject para extraer propiedades clave-valor de respuestas JSON
import org.json.JSONObject;

// Importación de URLEncoder para escapar caracteres especiales y espacios antes de insertarlos en una URL
import java.net.URLEncoder;
// Importación de StandardCharsets para garantizar el uso estándar de codificación UTF-8
import java.nio.charset.StandardCharsets;
// Importación de Normalizer para descomponer caracteres Unicode y eliminar tildes o acentos
import java.text.Normalizer;
// Importación de ArrayList como implementación dinámica de lista para almacenar el historial del chat
import java.util.ArrayList;
// Importación de Iterator para recorrer claves dinámicas desconocidas dentro de un JSONObject
import java.util.Iterator;
// Importación de List como interfaz estándar de colecciones ordenadas
import java.util.List;

// Importación de ChatAdapter que transforma cada objeto Mensaje en una burbuja visual (izquierda o derecha)
import es.medac.skycollectorapp.adapters.ChatAdapter;
// Importación del modelo de datos Mensaje que encapsula el texto y el emisor (usuario o bot)
import es.medac.skycollectorapp.models.Mensaje;
// Importación de la clase R generada para acceder a los identificadores de vistas y cadenas localizadas
import es.medac.skycollectorapp.R;
// Importación de AvionGenerator para consultar el idioma activo configurado en la aplicación (es/en)
import es.medac.skycollectorapp.utils.AvionGenerator;

/**
 * Actividad que implementa el asistente virtual integrado en la aplicación.
 * Limpia las consultas en lenguaje natural del usuario (en español o inglés) y consulta
 * en tiempo real la API pública de Wikipedia para ofrecer fichas técnicas e históricas de aeronaves.
 */
public class ChatbotActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES DE INTERFAZ Y DATOS
    // ------------------------------
    // Componente visual encargado de mostrar la lista desplazable de burbujas de conversación
    private RecyclerView recyclerChat;
    // Campo de entrada de texto donde el usuario redacta su pregunta sobre un avión
    private EditText etMensaje;
    // Botón gráfico encargado de disparar el envío de la consulta escrita
    private ImageButton btnEnviar;
    // Botón gráfico situado en la cabecera para cerrar el chat y volver a la pantalla anterior
    private ImageButton btnVolverChat;
    // Adaptador encargado de vincular la lista de mensajes en memoria con las vistas del RecyclerView
    private ChatAdapter adapter;
    // Lista dinámica en memoria que almacena todos los mensajes intercambiados durante la sesión
    private List<Mensaje> mensajes;
    // Cola de peticiones HTTP de Volley encargada de gestionar las consultas a Wikipedia en segundo plano
    private RequestQueue requestQueue;
    // Manejador asociado al hilo principal para ejecutar tareas visuales con retardo controlado
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    /**
     * Inicializa la interfaz del chatbot, configura la lista de mensajes, prepara la cola
     * de red Volley, muestra el saludo inicial y registra los eventos de interacción.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama al constructor base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Carga el archivo de diseño XML activity_chatbot en pantalla
        setContentView(R.layout.activity_chatbot);

        // Vincula cada variable Java con su correspondiente componente definido en el XML
        recyclerChat = findViewById(R.id.recyclerChat);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnVolverChat = findViewById(R.id.btnVolverChat);

        // Configura el evento de clic en la flecha superior para finalizar la actividad actual
        btnVolverChat.setOnClickListener(v -> finish());

        // Instancia la lista vacía de mensajes y la entrega al adaptador visual
        mensajes = new ArrayList<>();
        adapter = new ChatAdapter(mensajes);

        // Establece un gestor de diseño vertical lineal para apilar los mensajes de arriba a abajo
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        // Conecta el adaptador con el RecyclerView
        recyclerChat.setAdapter(adapter);

        // Inicializa la cola de peticiones de red de Volley vinculada al contexto de esta actividad
        requestQueue = Volley.newRequestQueue(this);

        // Inserta el mensaje inicial de bienvenida del asistente traducido según el idioma del dispositivo
        recibirMensaje(getString(R.string.chat_bienvenida));

        // Registra el escuchador de clic en el botón de enviar mensaje
        btnEnviar.setOnClickListener(v -> {
            // Extrae el texto introducido por el usuario eliminando espacios sobrantes al inicio y al final
            String texto = etMensaje.getText().toString().trim();
            // Comprueba que el usuario haya escrito al menos un carácter válido
            if (!texto.isEmpty()) {
                // Muestra inmediatamente el mensaje del usuario en la conversación
                enviarMensaje(texto);
                // Vacía la caja de texto para dejarla lista para la siguiente pregunta
                etMensaje.setText("");
                // Programa la ejecución del motor de respuesta con un retardo de 500 ms para dar naturalidad
                mainHandler.postDelayed(() -> cerebroDelBot(texto), 500);
            }
        });
    }

    /**
     * Libera recursos, cancela retardos pendientes y detiene cualquier petición HTTP en curso
     * cuando la actividad se destruye para evitar fugas de memoria o fallos en segundo plano.
     */
    @Override
    protected void onDestroy() {
        // Llama a la destrucción estándar de la superclase
        super.onDestroy();
        // Elimina cualquier tarea programada pendiente en el Handler del hilo principal
        mainHandler.removeCallbacksAndMessages(null);
        // Si la cola de red existe, cancela todas las peticiones HTTP activas de esta pantalla
        if (requestQueue != null) {
            requestQueue.cancelAll(req -> true);
        }
    }

    // ------------------------------
    // LÓGICA DE INTERFAZ DEL CHAT
    // ------------------------------

    /**
     * Añade un mensaje emitido por el usuario a la lista, notifica al adaptador y desplaza
     * el scroll automáticamente hasta la última burbuja.
     */
    private void enviarMensaje(String texto) {
        // Crea un nuevo objeto Mensaje marcado como propio (true -> alineado a la derecha)
        mensajes.add(new Mensaje(texto, true));
        // Notifica al adaptador que se ha insertado un nuevo elemento en la última posición
        adapter.notifyItemInserted(mensajes.size() - 1);
        // Desplaza suavemente la vista de lista hasta el final para mostrar el nuevo mensaje
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    /**
     * Añade una respuesta generada por el asistente virtual a la conversación, verificando
     * previamente que la pantalla siga activa para evitar excepciones de interfaz.
     */
    private void recibirMensaje(String texto) {
        // Si la actividad ya está cerrándose o destruida, aborta la actualización visual
        if (isDestroyed() || isFinishing()) return;
        // Crea un nuevo objeto Mensaje marcado como ajeno (false -> alineado a la izquierda)
        mensajes.add(new Mensaje(texto, false));
        // Notifica al adaptador la inserción de la respuesta del bot
        adapter.notifyItemInserted(mensajes.size() - 1);
        // Desplaza la vista hasta el último mensaje recibido
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    // ------------------------------
    // LÓGICA DE PROCESAMIENTO Y RED
    // ------------------------------

    /**
     * Analiza la pregunta formulada por el usuario, extrae el nombre limpio de la aeronave
     * y lanza la búsqueda de información en el idioma activo de la aplicación.
     */
    private void cerebroDelBot(String pregunta) {
        // Filtra palabras vacías, signos y tildes para aislar el término clave de búsqueda
        String modeloLimpio = limpiarConsulta(pregunta);

        // Si tras limpiar la frase queda menos de 2 caracteres, pide al usuario que concrete el modelo
        if (modeloLimpio.length() < 2) {
            recibirMensaje(getString(R.string.chat_pedir_modelo));
            return;
        }

        // Muestra un mensaje de estado indicando al usuario qué término se está consultando
        recibirMensaje(getString(R.string.chat_buscando, modeloLimpio));
        // Inicia la consulta en Wikipedia usando el código de idioma activo ("es" o "en")
        buscarTituloOficial(modeloLimpio, AvionGenerator.obtenerCodigoIdiomaWiki());
    }

    /**
     * Normaliza y depura la consulta en lenguaje natural eliminando acentos, signos de puntuación
     * y frases introductorias comunes tanto en español como en inglés.
     */
    private String limpiarConsulta(String textoBruto) {
        // Si la cadena recibida es nula, devuelve una cadena vacía por seguridad
        if (textoBruto == null) return "";

        // Descompone los caracteres acentuados (NFD), elimina las marcas diacríticas y pasa todo a minúsculas
        String sinTildes = Normalizer.normalize(textoBruto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();

        // Reemplaza cualquier símbolo o signo de puntuación por espacios, conservando letras, números y guiones
        String limpio = sinTildes.replaceAll("[^a-z0-9\\s-]", " ");

        // Diccionario de frases conversacionales y artículos irrelevantes en español e inglés a descartar
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

        // Recorre todas las expresiones irrelevantes y las sustituye por espacios en blanco
        for (String b : basura) {
            limpio = limpio.replace(b, " ");
        }

        // Reduce múltiples espacios consecutivos a un único espacio y recorta los extremos
        return limpio.replaceAll("\\s+", " ").trim();
    }

    /**
     * Paso 1 de consulta a Wikipedia: utiliza el endpoint OpenSearch para localizar el título
     * exacto del artículo correspondiente al término limpio. Si falla en español, reintenta en inglés.
     */
    private void buscarTituloOficial(String termino, String lang) {
        try {
            // Codifica el término de búsqueda en formato seguro para URL (UTF-8)
            String terminoUrl = URLEncoder.encode(termino, StandardCharsets.UTF_8.name());
            // Construye la URL de búsqueda rápida de Wikipedia limitando el resultado a 1 coincidencia
            String url = "https://" + lang + ".wikipedia.org/w/api.php?action=opensearch&search=" + terminoUrl + "&limit=1&format=json";

            // Crea la petición HTTP GET esperando un arreglo JSON como respuesta
            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            // Extrae el segundo elemento del arreglo OpenSearch, que contiene los títulos encontrados
                            JSONArray titulos = response.getJSONArray(1);

                            // Si se encontró al menos un artículo coincidente
                            if (titulos.length() > 0) {
                                // Obtiene el título oficial del primer resultado
                                String tituloOficial = titulos.getString(0);
                                // Lanza el Paso 2 para descargar el texto introductorio de ese artículo
                                descargarContenido(tituloOficial, lang);
                            } else if (!"en".equals(lang)) {
                                // Si no hubo resultados en el idioma actual (ej. español), reintenta en Wikipedia en inglés
                                buscarTituloOficial(termino, "en");
                            } else {
                                // Si tampoco existe en inglés, informa al usuario de que no se halló el modelo
                                recibirMensaje(getString(R.string.chat_no_encontrado, termino));
                            }
                        } catch (Exception e) {
                            // Informa si hubo un fallo al interpretar la estructura JSON de búsqueda
                            recibirMensaje(getString(R.string.chat_error_identificar));
                        }
                    },
                    // Callback ejecutado si falla la conexión de red o el servidor devuelve error HTTP
                    error -> recibirMensaje(getString(R.string.chat_error_conexion))
            );
            // Encola la petición en Volley para su ejecución asíncrona
            requestQueue.add(request);
        } catch (Exception e) {
            // Captura posibles errores al codificar la cadena en UTF-8
            recibirMensaje(getString(R.string.chat_error_codificar));
        }
    }

    /**
     * Paso 2 de consulta a Wikipedia: solicita el extracto introductorio en texto plano
     * del artículo cuyo título oficial fue resuelto en el paso anterior.
     */
    private void descargarContenido(String tituloOficial, String lang) {
        try {
            // Codifica el título oficial para insertarlo de forma segura en la URL de consulta
            String tituloUrl = URLEncoder.encode(tituloOficial, StandardCharsets.UTF_8.name());
            // Construye la URL de la API Query de Wikipedia pidiendo solo la introducción en texto plano con redirecciones
            String url = "https://" + lang + ".wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" + tituloUrl + "&format=json&redirects=1";

            // Crea la petición HTTP GET esperando un objeto JSON con los detalles de la página
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            // Accede al nodo raíz "query" de la respuesta
                            JSONObject query = response.getJSONObject("query");
                            // Accede al diccionario "pages" indexado por el ID numérico interno de Wikipedia
                            JSONObject pages = query.getJSONObject("pages");
                            // Obtiene un iterador sobre las claves dinámicas de las páginas devueltas
                            Iterator<String> keys = pages.keys();

                            // Si el objeto de páginas viene vacío, informa que no hay detalles disponibles
                            if (!keys.hasNext()) {
                                recibirMensaje(getString(R.string.chat_sin_detalles));
                                return;
                            }

                            // Extrae el identificador numérico de la primera página encontrada
                            String pageId = keys.next();

                            // Un ID distinto de "-1" confirma que el artículo existe válidamente en Wikipedia
                            if (!pageId.equals("-1")) {
                                // Obtiene el objeto JSON correspondiente a esa página
                                JSONObject page = pages.getJSONObject(pageId);
                                // Extrae el texto resumido de introducción (o cadena vacía si no tiene)
                                String extracto = page.optString("extract", "");
                                // Obtiene el título definitivo normalizado tras posibles redirecciones
                                String tituloReal = page.optString("title", tituloOficial);

                                // Si el extracto contiene texto útil, lo formatea y lo muestra en el chat
                                if (!extracto.isEmpty()) {
                                    recibirMensaje(getString(R.string.chat_informe_formato, tituloReal.toUpperCase(), extracto));
                                } else {
                                    // Informa si el artículo existe pero carece de texto introductorio
                                    recibirMensaje(getString(R.string.chat_articulo_vacio, tituloReal));
                                }
                            } else {
                                // Informa si Wikipedia devolvió ID "-1" (página inexistente o eliminada)
                                recibirMensaje(getString(R.string.chat_error_datos));
                            }
                        } catch (Exception e) {
                            // Informa si ocurrió un error al leer los campos del objeto JSON recibido
                            recibirMensaje(getString(R.string.chat_error_leer));
                        }
                    },
                    // Callback ejecutado si ocurre un error de red durante la descarga del resumen
                    error -> recibirMensaje(getString(R.string.chat_error_red))
            );
            // Añade la petición a la cola de ejecución de Volley
            requestQueue.add(request);
        } catch (Exception e) {
            // Informa si falló la preparación o codificación de la solicitud
            recibirMensaje(getString(R.string.chat_error_solicitar));
        }
    }
}