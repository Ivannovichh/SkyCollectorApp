// Declaración del paquete de la aplicación
package es.medac.skycollectorapp.activities;

// Importación para el manejo del estado y paquetes de datos de la actividad
import android.os.Bundle;
// Importación para gestionar la ejecución de código con retardo
import android.os.Handler;
// Importación para el componente de entrada de texto del usuario
import android.widget.EditText;
// Importación para el componente de botón que contiene una imagen
import android.widget.ImageButton;
// Importación de la clase base para actividades con soporte de librerías modernas
import androidx.appcompat.app.AppCompatActivity;
// Importación para organizar los elementos de una lista de forma lineal
import androidx.recyclerview.widget.LinearLayoutManager;
// Importación para el contenedor de listas eficiente y desplazable
import androidx.recyclerview.widget.RecyclerView;

// Importación para la definición base de peticiones de red
import com.android.volley.Request;
// Importación para el gestor de la cola de mensajes de red
import com.android.volley.RequestQueue;
// Importación para realizar peticiones que esperan un array JSON
import com.android.volley.toolbox.JsonArrayRequest;
// Importación para realizar peticiones que esperan un objeto JSON
import com.android.volley.toolbox.JsonObjectRequest;
// Importación para la inicialización de la librería de comunicación Volley
import com.android.volley.toolbox.Volley;

// Importación para la manipulación de estructuras de datos tipo array en JSON
import org.json.JSONArray;
// Importación para la manipulación de estructuras de datos tipo objeto en JSON
import org.json.JSONObject;

// Importación para el uso de listas de tamaño dinámico
import java.util.ArrayList;
// Importación para recorrer colecciones de datos de forma secuencial
import java.util.Iterator;
// Importación para la interfaz que define el comportamiento de las listas
import java.util.List;

// Importación del adaptador personalizado para los mensajes del chat
import es.medac.skycollectorapp.adapters.ChatAdapter;
// Importación del modelo de datos que representa un mensaje
import es.medac.skycollectorapp.models.Mensaje;
// Importación de la clase generada para el acceso a recursos del proyecto
import es.medac.skycollectorapp.R;

// Clase principal que gestiona la lógica de la pantalla del chatbot
public class ChatbotActivity extends AppCompatActivity {

    // Variable para el control visual de la lista de mensajes
    private RecyclerView recyclerChat;
    // Variable para capturar lo que el usuario escribe
    private EditText etMensaje;
    // Variable para el botón que activa el envío del mensaje
    private ImageButton btnEnviar;
    // Variable para el controlador que vincula datos con la lista visual
    private ChatAdapter adapter;
    // Variable para almacenar la colección de mensajes en memoria
    private List<Mensaje> mensajes;

    // Variable para gestionar las peticiones de red hacia servicios externos
    private RequestQueue requestQueue;

    // Punto de entrada principal cuando se crea la pantalla
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de inicialización del sistema
        super.onCreate(savedInstanceState);
        // Carga el diseño visual definido en el archivo XML
        setContentView(R.layout.activity_chatbot);

        // Asocia el componente de la lista del diseño con la variable
        recyclerChat = findViewById(R.id.recyclerChat);
        // Asocia el campo de texto del diseño con la variable
        etMensaje = findViewById(R.id.etMensaje);
        // Asocia el botón de envío del diseño con la variable
        btnEnviar = findViewById(R.id.btnEnviar);

        // Crea una nueva instancia para la lista de mensajes
        mensajes = new ArrayList<>();
        // Crea el adaptador pasándole la lista vacía recién creada
        adapter = new ChatAdapter(mensajes);

        // Establece que la lista se muestre en una sola columna vertical
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        // Conecta el adaptador a la lista para mostrar los datos
        recyclerChat.setAdapter(adapter);

        // Configura la cola para procesar peticiones HTTP
        requestQueue = Volley.newRequestQueue(this);

        // Envía un mensaje automático de bienvenida al iniciar
        recibirMensaje("Sistema online. Preguntame por un modelo (Ej: F18, Boeing 737) y buscare su ficha.");

        // Define qué sucede cuando se presiona el botón de enviar
        btnEnviar.setOnClickListener(v -> {
            // Captura el texto escrito y quita espacios innecesarios
            String texto = etMensaje.getText().toString().trim();
            // Comprueba que el usuario realmente haya escrito algo
            if (!texto.isEmpty()) {
                // Muestra el mensaje del usuario en la pantalla
                enviarMensaje(texto);
                // Borra el texto de la caja de entrada
                etMensaje.setText("");
                // Espera medio segundo antes de procesar la respuesta del bot
                new Handler().postDelayed(() -> cerebroDelBot(texto), 500);
            }
        });
    }

    // Procesa la inclusión de un mensaje del usuario en la interfaz
    private void enviarMensaje(String texto) {
        // Añade el texto a la lista indicando que es un mensaje propio
        mensajes.add(new Mensaje(texto, true));
        // Indica al adaptador que hay un nuevo dato para refrescar la vista
        adapter.notifyItemInserted(mensajes.size() - 1);
        // Hace que la pantalla baje automáticamente hasta el nuevo mensaje
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    // Procesa la inclusión de un mensaje de respuesta en la interfaz
    private void recibirMensaje(String texto) {
        // Añade el texto a la lista indicando que es un mensaje recibido
        mensajes.add(new Mensaje(texto, false));
        // Indica al adaptador que debe mostrar el mensaje entrante
        adapter.notifyItemInserted(mensajes.size() - 1);
        // Asegura que el último mensaje recibido sea visible
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    // Determina cómo debe responder el sistema según la entrada del usuario
    private void cerebroDelBot(String pregunta) {
        // Limpia la pregunta para quedarse solo con el nombre del avión
        String modeloLimpio = limpiarConsulta(pregunta);

        // Verifica si el término buscado es lo suficientemente largo
        if (modeloLimpio.length() < 2) {
            // Solicita más información si la búsqueda es demasiado corta
            recibirMensaje("Por favor, escribe el modelo del avion.");
            return;
        }

        // Informa al usuario que la búsqueda ha comenzado
        recibirMensaje("Localizando ficha oficial de '" + modeloLimpio + "'...");
        // Inicia el primer paso de la consulta externa
        buscarTituloOficial(modeloLimpio);
    }

    // Filtra palabras comunes para extraer el modelo del avión
    private String limpiarConsulta(String textoBruto) {
        // Convierte todo a minúsculas para igualar los datos
        String limpio = textoBruto.toLowerCase();

        // Lista de frases y artículos que no sirven para la búsqueda
        String[] basura = {
                "dame informacion sobre", "dame informacion de",
                "informacion del", "informacion de",
                "que es un", "que es el", "que es", "que son",
                "quiero saber del", "dame datos del",
                "busca el", "busca", "sobre el",
                "caza", "bombardero", "avion", "jet",
                " el ", " la ", " los ", " un ", " una "
        };

        // Recorre la lista de palabras irrelevantes para borrarlas
        for (String b : basura) {
            // Sustituye el texto irrelevante por un espacio en blanco
            limpio = limpio.replace(b, " ");
        }
        // Devuelve el texto limpio y sin espacios a los lados
        return limpio.trim();
    }

    // Consulta en el servidor externo el título exacto del artículo
    private void buscarTituloOficial(String termino) {
        // Codifica el término para que sea apto para una dirección web
        String terminoUrl = termino.replace(" ", "%20");
        // Construye la dirección URL para la búsqueda de títulos
        String url = "https://es.wikipedia.org/w/api.php?action=opensearch&search=" + terminoUrl + "&limit=1&format=json";

        // Crea la petición para obtener una respuesta en formato array
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Extrae la lista de títulos de la respuesta recibida
                        JSONArray titulos = response.getJSONArray(1);

                        // Si hay al menos un resultado, procede al siguiente paso
                        if (titulos.length() > 0) {
                            // Obtiene la cadena de texto con el título oficial
                            String tituloOficial = titulos.getString(0);
                            // Lanza la petición para descargar el contenido del artículo
                            descargarContenido(tituloOficial);
                        } else {
                            // Avisa al usuario si no se encontró nada con ese nombre
                            recibirMensaje("No encuentro ningun avion llamado '" + termino + "'. Prueba con el nombre completo.");
                        }
                    } catch (Exception e) {
                        // Maneja cualquier fallo en el procesamiento de la respuesta
                        recibirMensaje("Error al identificar el avion.");
                    }
                },
                // Maneja fallos de comunicación o falta de internet
                error -> recibirMensaje("Error de conexion a internet.")
        );
        // Envía la petición a la cola de procesamiento
        requestQueue.add(request);
    }

    // Descarga y muestra el texto informativo del avión
    private void descargarContenido(String tituloOficial) {
        // Prepara el título oficial para la petición final
        String tituloUrl = tituloOficial.replace(" ", "%20");

        // Construye la URL para extraer el resumen del artículo y seguir redirecciones
        String url = "https://es.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" + tituloUrl + "&format=json&redirects=1";

        // Crea la petición para obtener el contenido detallado en formato objeto
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Accede al bloque de datos de consulta de la respuesta
                        JSONObject query = response.getJSONObject("query");
                        // Accede al bloque de páginas de la consulta
                        JSONObject pages = query.getJSONObject("pages");
                        // Obtiene un buscador para las claves de las páginas
                        Iterator<String> keys = pages.keys();
                        // Selecciona la clave de la primera página encontrada
                        String pageId = keys.next();

                        // Verifica que la página sea válida y exista
                        if (!pageId.equals("-1")) {
                            // Accede a la información específica de esa página
                            JSONObject page = pages.getJSONObject(pageId);
                            // Extrae el texto descriptivo del avión
                            String extracto = page.getString("extract");
                            // Recupera el nombre real del artículo
                            String tituloReal = page.getString("title");

                            // Si el texto descriptivo no está vacío, lo muestra
                            if (extracto != null && !extracto.isEmpty()) {
                                // Envía el informe final formateado al chat
                                recibirMensaje("INFORME: " + tituloReal.toUpperCase() + "\n\n" + extracto);
                            } else {
                                // Informa si el artículo no tiene texto disponible
                                recibirMensaje("He encontrado el articulo '" + tituloReal + "' pero parece estar vacio.");
                            }
                        } else {
                            // Indica que hubo un problema al localizar los datos
                            recibirMensaje("Error al recuperar los datos.");
                        }
                    } catch (Exception e) {
                        // Maneja errores durante la lectura del objeto JSON
                        recibirMensaje("Error al leer el informe.");
                    }
                },
                // Indica problemas de red durante la descarga del informe
                error -> recibirMensaje("Error de red.")
        );
        // Registra la petición final en el gestor de red
        requestQueue.add(request);
    }
}