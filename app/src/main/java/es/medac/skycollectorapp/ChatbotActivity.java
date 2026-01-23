package es.medac.skycollectorapp;

import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView recyclerChat;
    private EditText etMensaje;
    private ImageButton btnEnviar;
    private ChatAdapter adapter;
    private List<Mensaje> mensajes;

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        recyclerChat = findViewById(R.id.recyclerChat);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);

        mensajes = new ArrayList<>();
        adapter = new ChatAdapter(mensajes);

        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        recibirMensaje("Sistema online. Preguntame por un modelo (Ej: F18, Boeing 737) y buscare su ficha.");

        btnEnviar.setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim();
            if (!texto.isEmpty()) {
                enviarMensaje(texto);
                etMensaje.setText("");
                new Handler().postDelayed(() -> cerebroDelBot(texto), 500);
            }
        });
    }

    private void enviarMensaje(String texto) {
        mensajes.add(new Mensaje(texto, true));
        adapter.notifyItemInserted(mensajes.size() - 1);
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    private void recibirMensaje(String texto) {
        mensajes.add(new Mensaje(texto, false));
        adapter.notifyItemInserted(mensajes.size() - 1);
        recyclerChat.scrollToPosition(mensajes.size() - 1);
    }

    // --- CEREBRO ---
    private void cerebroDelBot(String pregunta) {
        String modeloLimpio = limpiarConsulta(pregunta);

        if (modeloLimpio.length() < 2) {
            recibirMensaje("Por favor, escribe el modelo del avion.");
            return;
        }

        recibirMensaje("Localizando ficha oficial de '" + modeloLimpio + "'...");
        buscarTituloOficial(modeloLimpio);
    }

    private String limpiarConsulta(String textoBruto) {
        String limpio = textoBruto.toLowerCase();

        String[] basura = {
                "dame informacion sobre", "dame informacion de",
                "informacion del", "informacion de",
                "que es un", "que es el", "que es", "que son",
                "quiero saber del", "dame datos del",
                "busca el", "busca", "sobre el",
                "caza", "bombardero", "avion", "jet",
                " el ", " la ", " los ", " un ", " una "
        };

        for (String b : basura) {
            limpio = limpio.replace(b, " ");
        }
        return limpio.trim();
    }

    // --- PASO 1: ENCONTRAR EL ARTICULO EXACTO (OpenSearch) ---
    private void buscarTituloOficial(String termino) {
        String terminoUrl = termino.replace(" ", "%20");
        String url = "https://es.wikipedia.org/w/api.php?action=opensearch&search=" + terminoUrl + "&limit=1&format=json";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray titulos = response.getJSONArray(1);

                        if (titulos.length() > 0) {
                            String tituloOficial = titulos.getString(0);
                            descargarContenido(tituloOficial);
                        } else {
                            recibirMensaje("No encuentro ningun avion llamado '" + termino + "'. Prueba con el nombre completo.");
                        }
                    } catch (Exception e) {
                        recibirMensaje("Error al identificar el avion.");
                    }
                },
                error -> recibirMensaje("Error de conexion a internet.")
        );
        requestQueue.add(request);
    }

    // --- PASO 2: DESCARGAR EL TEXTO (AHORA CON REDIRECTS=1) ---
    private void descargarContenido(String tituloOficial) {
        String tituloUrl = tituloOficial.replace(" ", "%20");

        // CORRECCIÓN CRUCIAL: Añadido '&redirects=1' al final de la URL
        String url = "https://es.wikipedia.org/w/api.php?action=query&prop=extracts&exintro&explaintext&titles=" + tituloUrl + "&format=json&redirects=1";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject query = response.getJSONObject("query");
                        JSONObject pages = query.getJSONObject("pages");
                        Iterator<String> keys = pages.keys();
                        String pageId = keys.next();

                        if (!pageId.equals("-1")) {
                            JSONObject page = pages.getJSONObject(pageId);
                            String extracto = page.getString("extract");
                            // A veces, al seguir el redirect, el título cambia al real (ej: F18 -> F/A-18 Hornet)
                            String tituloReal = page.getString("title");

                            if (extracto != null && !extracto.isEmpty()) {
                                recibirMensaje("INFORME: " + tituloReal.toUpperCase() + "\n\n" + extracto);
                            } else {
                                recibirMensaje("He encontrado el articulo '" + tituloReal + "' pero parece estar vacio.");
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
        requestQueue.add(request);
    }
}