// Definición del paquete donde se ubica la clase
package es.medac.skycollectorapp.activities;

// Importación para el acceso al contexto de la aplicación
import android.content.Context;
// Importación para la gestión de navegación entre pantallas
import android.content.Intent;
// Importación para el manejo de persistencia de datos ligera
import android.content.SharedPreferences;
// Importación para el manejo de rutas de recursos y archivos
import android.net.Uri;
// Importación para la gestión del estado de la actividad
import android.os.Bundle;
// Importación para el registro de mensajes en la consola de depuración
import android.util.Log;
// Importación para la gestión de propiedades de las vistas
import android.view.View;
// Importación para mostrar mensajes emergentes rápidos
import android.widget.Toast;

// Importación de la clase base para actividades de compatibilidad
import androidx.appcompat.app.AppCompatActivity;
// Importación para mostrar cuadros de diálogo
import androidx.appcompat.app.AlertDialog;
// Importación para la disposición de elementos en cuadrícula dentro de una lista
import androidx.recyclerview.widget.GridLayoutManager;

// Importación de la librería para la gestión y carga de imágenes
import com.bumptech.glide.Glide;
// Importación de la herramienta de autenticación de Firebase
import com.google.firebase.auth.FirebaseAuth;
// Importación del modelo de usuario autenticado en Firebase
import com.google.firebase.auth.FirebaseUser;
// Importación del motor de base de datos en la nube de Firestore
import com.google.firebase.firestore.FirebaseFirestore;
// Importación de la librería para conversión de objetos a formato JSON
import com.google.gson.Gson;
// Importación para la gestión de tipos de datos en colecciones genéricas
import com.google.gson.reflect.TypeToken;

// Importación para el uso de reflexión en tipos de datos
import java.lang.reflect.Type;
// Importación para dar formato a las fechas
import java.text.SimpleDateFormat;
// Importación para el uso de listas de tamaño dinámico
import java.util.ArrayList;
// Importación para guardar el orden de aparición de los modelos
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
// Importación para la definición de interfaces de listas
import java.util.List;

// Importación del adaptador para la visualización de aviones
import es.medac.skycollectorapp.adapters.AvionAdapter;
// Importación de la clase generada para la vinculación de vistas
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
// Importación del modelo de datos de Avión
import es.medac.skycollectorapp.models.Avion;
// Importación del modelo que agrupa los avistamientos de un mismo modelo
import es.medac.skycollectorapp.models.GrupoAvion;
// Importación de la actividad para añadir nuevos aviones
import es.medac.skycollectorapp.activities.AddAvionActivity;

// Clase principal que gestiona la pantalla de inicio y el listado de la colección
public class MainActivity extends AppCompatActivity {

    // Variable para acceder a los componentes del diseño sin usar findViewById
    private ActivityMainBinding binding;
    // Variable para gestionar el puente entre los datos y la lista visual
    private AvionAdapter adapter;
    // Lista en memoria que contiene todos los avistamientos de la colección
    private List<Avion> listaAviones;
    // Lista en memoria con los avistamientos agrupados por modelo para mostrarlos
    private List<GrupoAvion> grupos;

    // Método que se ejecuta al iniciar la creación de la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ejecución de la lógica de creación de la clase superior
        super.onCreate(savedInstanceState);
        // Inicialización de la vinculación de vistas mediante el inflador
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // Establecimiento del contenido visual de la actividad
        setContentView(binding.getRoot());

        // Instanciación de la lista dinámica de aviones
        listaAviones = new ArrayList<>();

        // Instanciación de la lista de grupos de aviones
        grupos = new ArrayList<>();

        // Inicialización del adaptador con la lista de grupos y los eventos de clic
        adapter = new AvionAdapter(grupos,
                (grupo, position) -> {
                    // Apertura directa del detalle si el modelo tiene un solo avistamiento
                    if (grupo.getCantidad() == 1) abrirDetalle(grupo.getRepresentante());
                        // Si hay varios avistamientos se deja elegir cuál abrir
                    else mostrarAvistamientos(grupo);
                },
                // Referencia al método que gestiona la visibilidad del botón de borrado
                this::actualizarPapelera
        );

        // Configuración de la lista para mostrarse en dos columnas
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        // Vinculación del adaptador con el componente de lista visual
        binding.recyclerView.setAdapter(adapter);

        // Definición de la acción al pulsar el botón de añadir avión
        binding.btnAddAvion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddAvionActivity.class))
        );

        // Definición de la acción al pulsar el botón de eliminación masiva
        binding.btnPapelera.setOnClickListener(v -> borrarSeleccionados());

        // Definición de la acción al pulsar sobre el área del perfil
        binding.cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class))
        );

        // Definición de la acción al pulsar el botón del asistente virtual
        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class))
        );

        // Definición de la acción al pulsar el botón de visualización del mapa
        binding.btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapaActivity.class))
        );
    }

    // Método que se ejecuta cada vez que la pantalla vuelve a primer plano
    @Override
    protected void onResume() {
        // Ejecución de la lógica de reanudación superior
        super.onResume();
        // Recarga de la colección de aviones desde el almacenamiento
        cargarListaDeAviones();
        // Actualización de la imagen de perfil en la interfaz
        cargarFotoPerfilMini();
        // Actualización del nombre de usuario en la interfaz
        cargarNombreUsuario();
    }

    // Método para recuperar y mostrar la imagen de perfil reducida
    private void cargarFotoPerfilMini() {
        // Obtención del usuario autenticado actualmente
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Interrupción si no hay una sesión de usuario activa
        if (user == null) return;

        // Recuperación del identificador único del usuario
        String userId = user.getUid();
        // Acceso al fichero de preferencias del perfil de usuario
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        // Recuperación de la ruta de la foto asociada al identificador
        String fotoGuardada = prefs.getString("foto_" + userId, null);

        // Verificación de existencia de una ruta de foto válida
        if (fotoGuardada != null) {
            // Carga de la imagen con recorte circular en el componente visual
            Glide.with(this)
                    .load(Uri.parse(fotoGuardada))
                    .circleCrop()
                    .into(binding.imgPerfilMini);
        }
    }

    // Método para sincronizar la lista visual con los datos guardados
    private void cargarListaDeAviones() {
        // Acceso al almacenamiento de datos de la colección
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        // Recuperación de la cadena JSON con la información de los aviones
        String json = prefs.getString("lista_aviones", null);

        // Limpieza de la lista actual en memoria
        listaAviones.clear();

        // Procesamiento de los datos si el archivo JSON no está vacío
        if (json != null) {
            // Inicialización del motor de conversión de datos
            Gson gson = new Gson();
            // Definición del tipo de colección para la conversión
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            // Transformación del texto JSON en una lista de objetos
            List<Avion> avionesGuardados = gson.fromJson(json, type);
            // Inserción de los objetos recuperados en la lista de trabajo
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados);
        }

        // Agrupación de los avistamientos por modelo para no mostrar tarjetas repetidas
        construirGrupos();

        // Gestión de la visibilidad de los componentes según si hay datos
        if (listaAviones.isEmpty()) {
            // Muestra el mensaje informativo de lista vacía
            binding.txtVacio.setVisibility(View.VISIBLE);
            // Oculta el componente de la lista
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            // Oculta el mensaje de lista vacía
            binding.txtVacio.setVisibility(View.GONE);
            // Muestra el componente de la lista
            binding.recyclerView.setVisibility(View.VISIBLE);
        }

        // Notificación al adaptador para refrescar toda la interfaz de la lista
        adapter.notifyDataSetChanged();
        // Actualización del estado visual del botón de borrado
        actualizarPapelera();
    }

    // Método para eliminar los grupos marcados por el usuario
    private void borrarSeleccionados() {
        // Creación de una lista temporal para identificar los grupos a eliminar
        List<GrupoAvion> gruposABorrar = new ArrayList<>();
        // Contador del total de avistamientos que se eliminarían
        int totalAvistamientos = 0;

        // Recorrido de los grupos buscando los que tienen la marca de selección
        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                gruposABorrar.add(g); // Adición del grupo a la lista de borrado
                totalAvistamientos += g.getCantidad(); // Suma de sus avistamientos
            }
        }

        // Interrupción si no se ha marcado ningún grupo para borrar
        if (gruposABorrar.isEmpty()) return;

        // Si se van a borrar varios avistamientos de un mismo modelo se pide confirmación
        if (totalAvistamientos > gruposABorrar.size()) {
            final int total = totalAvistamientos;
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar avistamientos")
                    .setMessage("Se eliminarán " + total + " avistamientos. ¿Continuar?")
                    .setPositiveButton("Eliminar", (d, w) -> eliminarGrupos(gruposABorrar))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            // Borrado directo si cada grupo tiene un solo avistamiento
            eliminarGrupos(gruposABorrar);
        }
    }

    // Método que elimina los grupos indicados junto con todos sus avistamientos
    private void eliminarGrupos(List<GrupoAvion> gruposABorrar) {
        // Recorrido de cada grupo y de cada uno de sus avistamientos
        for (GrupoAvion g : gruposABorrar) {
            for (Avion a : g.getAvistamientos()) {
                // Invocación del borrado en la base de datos de la nube
                borrarAvionEnFirestore(a);
                // Eliminación del avistamiento de la lista local
                listaAviones.remove(a);
            }
        }

        // Apertura del editor para actualizar el almacenamiento local
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Almacenamiento de la nueva lista de avistamientos en formato JSON
        editor.putString("lista_aviones", new Gson().toJson(listaAviones));
        // Aplicación persistente de los cambios
        editor.apply();

        // Reconstrucción de los grupos y refresco de la lista visual
        construirGrupos();
        adapter.notifyDataSetChanged();

        // Refresco de la visibilidad del botón de papelera
        actualizarPapelera();

        // Muestra el aviso visual si ya no quedan elementos en la colección
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        }

        // Notificación final al usuario sobre el éxito del proceso
        Toast.makeText(this, "Aviones eliminados", Toast.LENGTH_SHORT).show();
    }

    // Método para agrupar los avistamientos por modelo conservando el orden de aparición
    private void construirGrupos() {
        // Limpieza de los grupos actuales
        grupos.clear();

        // Mapa que relaciona cada modelo con su grupo, respetando el orden de inserción
        Map<String, GrupoAvion> porModelo = new LinkedHashMap<>();

        // Recorrido de todos los avistamientos para repartirlos por modelo
        for (Avion a : listaAviones) {
            String clave = a.getModelo() != null ? a.getModelo() : "";
            GrupoAvion g = porModelo.get(clave);
            if (g == null) {
                g = new GrupoAvion(clave); // Creación del grupo si es el primer avistamiento del modelo
                porModelo.put(clave, g);
            }
            g.agregar(a); // Adición del avistamiento al grupo
        }

        // Paso de los grupos creados a la lista que usa el adaptador
        grupos.addAll(porModelo.values());
    }

    // Método para abrir la pantalla de detalles de un avistamiento
    private void abrirDetalle(Avion avion) {
        // Creación de una intención para abrir la pantalla de detalles
        Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
        // Inserción del identificador del avión en la intención
        intent.putExtra("avion_id", avion.getId());
        // Inicio de la actividad de destino
        startActivity(intent);
    }

    // Método para mostrar la lista de avistamientos de un modelo y elegir cuál abrir
    private void mostrarAvistamientos(GrupoAvion grupo) {
        List<Avion> lista = grupo.getAvistamientos();
        String[] etiquetas = new String[lista.size()];
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Construcción de una etiqueta por avistamiento, del más reciente al más antiguo
        for (int i = 0; i < lista.size(); i++) {
            Avion a = lista.get(lista.size() - 1 - i);

            // Matrícula si existe, si no el ICAO y si no un texto genérico
            String matricula = (a.getMatricula() != null && !a.getMatricula().isEmpty())
                    ? a.getMatricula()
                    : (a.getIcao24() != null ? a.getIcao24() : "Sin matrícula");

            // Fecha del avistamiento si se conoce
            String fecha = a.getFechaAvistamiento() > 0
                    ? formato.format(new Date(a.getFechaAvistamiento()))
                    : "fecha desconocida";

            // Indicación de si el avistamiento tiene foto del usuario
            String foto = a.getUriFotoUsuario() != null ? " · con foto" : "";

            etiquetas[i] = matricula + " · " + fecha + foto;
        }

        // Diálogo con la lista de avistamientos del modelo
        new AlertDialog.Builder(this)
                .setTitle(grupo.getModelo() + " (" + lista.size() + " avistamientos)")
                .setItems(etiquetas, (d, which) -> abrirDetalle(lista.get(lista.size() - 1 - which)))
                .show();
    }

    // Método para mostrar u ocultar el botón de borrado según la selección
    private void actualizarPapelera() {
        // Variable de control para detectar si hay algún elemento marcado
        boolean haySeleccionados = false;
        // Recorrido de los grupos en busca de cualquier selección activa
        for (GrupoAvion g : grupos) {
            // Comprobación de la marca de selección
            if (g.isSeleccionado()) {
                // Activación del estado de detección
                haySeleccionados = true;
                // Salida prematura del bucle al encontrar el primero
                break;
            }
        }

        // Modificación de la visibilidad del botón según el estado de selección detectado
        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE);
    }

    // Método para gestionar la eliminación de un registro en el servidor
    public void borrarAvionEnFirestore(Avion avion) {
        // Inicialización de la instancia de la base de datos remota
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Obtención de la instancia del gestor de usuarios
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Interrupción si no existe una sesión de usuario válida
        if (auth.getCurrentUser() == null) return;

        // Recuperación del identificador único del usuario
        String uid = auth.getCurrentUser().getUid();

        // Acceso a la ruta del documento específico para su eliminación
        db.collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avion.getId())
                .delete() // Ejecución de la orden de borrado
                .addOnSuccessListener(aVoid -> {
                    // Registro del éxito de la operación en la consola
                    Log.d("FIRESTORE", "Avión eliminado: " + avion.getApodo());
                })
                .addOnFailureListener(e -> {
                    // Registro del fallo y la causa del error en la consola
                    Log.e("FIRESTORE", "Error al borrar avión", e);
                });
    }

    // Método para sincronizar el nombre del perfil con la interfaz
    private void cargarNombreUsuario() {
        // Acceso al fichero de configuración del perfil de usuario
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        // Obtención del usuario autenticado en la plataforma
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Interrupción si no se detecta usuario logueado
        if (user == null) return;

        // Recuperación del identificador único del usuario activo
        String userId = user.getUid();
        // Obtención del nombre almacenado o un valor por defecto si no existe
        String nombreUsuario = prefs.getString("nombre_" + userId, "usuario");
        // Actualización del componente visual de texto con el nombre obtenido
        binding.txtNombreUsuario.setText(nombreUsuario);
    }
}