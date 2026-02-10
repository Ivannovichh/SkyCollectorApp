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
// Importación para el uso de listas de tamaño dinámico
import java.util.ArrayList;
// Importación para la definición de interfaces de listas
import java.util.List;

// Importación del adaptador para la visualización de aviones
import es.medac.skycollectorapp.adapters.AvionAdapter;
// Importación de la clase generada para la vinculación de vistas
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
// Importación del modelo de datos de Avión
import es.medac.skycollectorapp.models.Avion;
// Importación de la actividad para añadir nuevos aviones
import es.medac.skycollectorapp.activities.AddAvionActivity;

// Clase principal que gestiona la pantalla de inicio y el listado de la colección
public class MainActivity extends AppCompatActivity {

    // Variable para acceder a los componentes del diseño sin usar findViewById
    private ActivityMainBinding binding;
    // Variable para gestionar el puente entre los datos y la lista visual
    private AvionAdapter adapter;
    // Lista en memoria que contiene los objetos de la colección
    private List<Avion> listaAviones;

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

        // Inicialización del adaptador con la lista y los eventos de clic
        adapter = new AvionAdapter(listaAviones,
                (avion, position) -> {
                    // Creación de una intención para abrir la pantalla de detalles
                    Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
                    // Inserción del identificador del avión en la intención
                    intent.putExtra("avion_id", avion.getId());
                    // Inicio de la actividad de destino
                    startActivity(intent);
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

    // Método para eliminar los elementos marcados por el usuario
    private void borrarSeleccionados() {
        // Interrupción si no hay elementos en la colección
        if (listaAviones.isEmpty()) return;

        // Creación de una lista temporal para identificar los elementos a eliminar
        List<Avion> avionesABorrar = new ArrayList<>();
        // Recorrido de la lista buscando elementos con la marca de selección
        for (Avion a : listaAviones) {
            // Verificación del estado de selección del objeto
            if (a.isSeleccionado()) {
                // Adición del objeto a la lista temporal de borrado
                avionesABorrar.add(a);
            }
        }

        // Interrupción si no se ha marcado ningún elemento para borrar
        if (avionesABorrar.isEmpty()) return;

        // Recorrido de la lista temporal para eliminar registros remotos
        for (Avion a : avionesABorrar) {
            // Invocación del borrado en la base de datos de la nube
            borrarAvionEnFirestore(a);
        }

        // Eliminación de los elementos en la lista local de forma segura
        for (int i = listaAviones.size() - 1; i >= 0; i--) {
            // Obtención del objeto en la posición actual
            Avion a = listaAviones.get(i);
            // Verificación si el objeto debe ser eliminado
            if (a.isSeleccionado()) {
                // Remoción del objeto de la colección en memoria
                listaAviones.remove(i);
                // Notificación al adaptador para ejecutar la animación de borrado
                adapter.notifyItemRemoved(i);
            }
        }

        // Apertura del editor para actualizar el almacenamiento local
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Conversión de la lista resultante a formato de texto JSON
        Gson gson = new Gson();
        // Almacenamiento de la nueva cadena de datos
        editor.putString("lista_aviones", gson.toJson(listaAviones));
        // Aplicación persistente de los cambios
        editor.apply();

        // Refresco de la visibilidad del botón de papelera
        actualizarPapelera();

        // Muestra el aviso visual si ya no quedan elementos en la colección
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
        }

        // Notificación final al usuario sobre el éxito del proceso
        Toast.makeText(this, "Aviones eliminados", Toast.LENGTH_SHORT).show();
    }

    // Método para mostrar u ocultar el botón de borrado según la selección
    private void actualizarPapelera() {
        // Variable de control para detectar si hay algún elemento marcado
        boolean haySeleccionados = false;
        // Recorrido de la colección en busca de cualquier selección activa
        for (Avion a : listaAviones) {
            // Comprobación de la marca de selección
            if (a.isSeleccionado()) {
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