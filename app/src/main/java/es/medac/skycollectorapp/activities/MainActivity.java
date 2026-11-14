// Declaración del paquete al que pertenece la actividad principal
package es.medac.skycollectorapp.activities;

// Importaciones de contexto y utilidades de navegación de Android
import android.content.Context; // Contexto de la aplicación
import android.content.Intent; // Objeto para la navegación entre actividades
import android.content.SharedPreferences; // Almacenamiento persistente ligero
import android.net.Uri; // Representación de direcciones URI para archivos o imágenes
import android.os.Bundle; // Pases de datos durante el ciclo de vida de la actividad
import android.util.Log; // Registro de logs en la consola Logcat
import android.view.LayoutInflater; // Conversor de archivos XML a objetos de vista en Java
import android.view.View; // Componente base de vistas de interfaz
import android.view.ViewGroup; // Contenedor padre de vistas
import android.widget.ImageView; // Componente para mostrar imágenes
import android.widget.TextView; // Componente para mostrar texto
import android.widget.Toast; // Notificaciones emergentes cortas

// Importaciones de AndroidX
import androidx.annotation.NonNull; // Marcador para parámetros que no pueden ser nulos
import androidx.appcompat.app.AlertDialog; // Cuadros de diálogo emergentes de confirmación
import androidx.appcompat.app.AppCompatActivity; // Clase base con compatibilidad para ActionBar
import androidx.recyclerview.widget.GridLayoutManager; // Gestor de diseño en cuadrícula para listas RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager; // Gestor de diseño lineal para el diálogo
import androidx.recyclerview.widget.RecyclerView; // Componente eficiente para listas de elementos

// Importaciones de librerías de Material Design y externas
import com.bumptech.glide.Glide; // Carga y procesado eficiente de imágenes
import com.google.android.material.bottomsheet.BottomSheetDialog; // Diálogo deslizante desde la parte inferior
import com.google.firebase.auth.FirebaseAuth; // Servicio de autenticación de usuario con Firebase
import com.google.firebase.auth.FirebaseUser; // Objeto de usuario autenticado en Firebase
import com.google.firebase.firestore.FirebaseFirestore; // Servicio de la base de datos Firestore en la nube
import com.google.gson.Gson; // Motor de conversión de objetos Java a formato JSON
import com.google.gson.reflect.TypeToken; // Preservación de tipos de colecciones genéricas para Gson

// Importaciones estándar de Java
import java.lang.reflect.Type; // Manejo de tipos de datos mediante reflexión
import java.text.SimpleDateFormat; // Formateador de fechas a cadenas de texto
import java.util.ArrayList; // Colección de lista dinámica
import java.util.Date; // Objeto para instantes temporales
import java.util.LinkedHashMap; // Mapa que conserva el orden de inserción de elementos
import java.util.List; // Interfaz para listas
import java.util.Locale; // Configuración regional de lenguaje
import java.util.Map; // Interfaz para mapas

// Importaciones de modelos, adaptadores y recursos del proyecto
import es.medac.skycollectorapp.R; // Recursos del proyecto
import es.medac.skycollectorapp.adapters.AvionAdapter; // Adaptador para la lista de tarjetas de aviones
import es.medac.skycollectorapp.databinding.ActivityMainBinding; // Clase de vinculación de vistas ViewBinding generada
import es.medac.skycollectorapp.models.Avion; // Modelo de datos del objeto Avión
import es.medac.skycollectorapp.models.GrupoAvion; // Modelo de agrupación por modelo de avión

// Clase principal que representa la pantalla de inicio y catálogo personal del usuario
public class MainActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES Y ADAPTADORES
    // ------------------------------
    private ActivityMainBinding binding; // Referencia para el acceso directo a los elementos del XML mediante ViewBinding
    private AvionAdapter adapter; // Adaptador para gestionar la visualización de los grupos de aviones
    private List<Avion> listaAviones; // Lista completa en memoria de todos los avistamientos del usuario
    private List<GrupoAvion> grupos; // Lista de avistamientos agrupados por modelo para la interfaz

    // ------------------------------
    // MÉTODOS DEL CICLO DE VIDA
    // ------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) { // Inicialización de la actividad
        super.onCreate(savedInstanceState); // Llamada al método padre
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // Inflar la vista mediante ViewBinding
        setContentView(binding.getRoot()); // Establecer la vista raíz del layout en pantalla

        listaAviones = new ArrayList<>(); // Inicializar la lista dinámica de avistamientos individuales
        grupos = new ArrayList<>(); // Inicializar la lista dinámica de grupos de avistamientos

        // Inicializar el adaptador pasando los grupos y configurando los eventos de clic
        adapter = new AvionAdapter(grupos,
                (grupo, position) -> {
                    // Si el grupo contiene un único avistamiento, abrir directamente la pantalla de detalle
                    if (grupo.getCantidad() == 1) abrirDetalle(grupo.getRepresentante());
                        // Si existen varios avistamientos del mismo modelo, desplegar menú deslizante para elegir cuál ver
                    else mostrarAvistamientos(grupo);
                },
                this::actualizarPapelera // Pasar la referencia del método para gestionar el botón de eliminación
        );

        // Configurar el RecyclerView con una disposición en cuadrícula de dos columnas
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter); // Conectar el adaptador al RecyclerView

        // Asignar los escuchadores de clic a los botones principales de la interfaz
        binding.btnAddAvion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddAvionActivity.class)) // Abrir la pantalla de añadir
        );

        binding.btnPapelera.setOnClickListener(v -> borrarSeleccionados()); // Ejecutar el borrado de elementos marcados

        binding.cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class)) // Abrir la pantalla de perfil
        );

        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class)) // Abrir el asistente virtual
        );

        binding.btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapaActivity.class)) // Abrir el mapa interactivo
        );
    }

    @Override
    protected void onResume() { // Método invocado cada vez que la pantalla vuelve a primer plano
        super.onResume(); // Llamada al método base
        cargarListaDeAviones(); // Recargar la colección local desde SharedPreferences
        cargarFotoPerfilMini(); // Cargar la foto de perfil en miniatura
        cargarNombreUsuario(); // Cargar el nombre personalizado del usuario
    }

    // ------------------------------
    // CARGA Y SINCRONIZACIÓN DE DATOS
    // ------------------------------

    // Carga la miniatura de la foto de perfil guardada para el usuario activo
    private void cargarFotoPerfilMini() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario autenticado
        if (user == null) return; // Salir si no hay una sesión activa

        String userId = user.getUid(); // Obtener el UID del usuario
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Leer preferencias
        String fotoGuardada = prefs.getString("foto_" + userId, null); // Obtener la URI de la foto guardada

        if (fotoGuardada != null) {
            Glide.with(this)
                    .load(Uri.parse(fotoGuardada)) // Cargar la imagen desde la URI
                    .circleCrop() // Aplicar corte circular
                    .into(binding.imgPerfilMini); // Insertar en la vista previa del perfil
        }
    }

    // Carga el nombre del usuario desde las preferencias locales o usa uno por defecto
    private void cargarNombreUsuario() {
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Leer preferencias
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario activo
        if (user == null) return; // Salir si no hay sesión iniciada

        String userId = user.getUid(); // Obtener UID
        String nombreUsuario = prefs.getString("nombre_" + userId, "usuario"); // Recuperar nombre o valor por defecto
        binding.txtNombreUsuario.setText(nombreUsuario); // Asignar el nombre en el TextView
    }

    // Lee los avistamientos guardados en SharedPreferences y reconstruye los grupos
    private void cargarListaDeAviones() {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE); // Obtener preferencias
        String json = prefs.getString("lista_aviones", null); // Leer la cadena JSON de avistamientos

        listaAviones.clear(); // Limpiar la lista de avistamientos en memoria

        if (json != null) {
            Gson gson = new Gson(); // Instanciar conversor Gson
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType(); // Definir el tipo para deserializar
            List<Avion> avionesGuardados = gson.fromJson(json, type); // Deserializar la lista desde JSON
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados); // Agregar los elementos recuperados
        }

        construirGrupos(); // Agrupar los avistamientos por modelo técnico

        // Controlar la visibilidad de la interfaz según si existen datos o no
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE); // Mostrar el mensaje de colección vacía
            binding.recyclerView.setVisibility(View.GONE); // Ocultar el RecyclerView
        } else {
            binding.txtVacio.setVisibility(View.GONE); // Ocultar el mensaje de colección vacía
            binding.recyclerView.setVisibility(View.VISIBLE); // Mostrar el RecyclerView
        }

        adapter.notifyDataSetChanged(); // Notificar al adaptador para refrescar la vista
        actualizarPapelera(); // Actualizar la visibilidad del botón de borrado
    }

    // Organiza la lista de avistamientos individuales en grupos ordenados por modelo
    private void construirGrupos() {
        grupos.clear(); // Limpiar los grupos anteriores
        Map<String, GrupoAvion> porModelo = new LinkedHashMap<>(); // Mapa ordenado por el nombre del modelo

        for (Avion a : listaAviones) { // Iterar sobre cada avistamiento guardado
            String clave = a.getModelo() != null ? a.getModelo() : ""; // Obtener la clave del modelo
            GrupoAvion g = porModelo.get(clave); // Buscar si ya existe un grupo creado para este modelo
            if (g == null) {
                g = new GrupoAvion(clave); // Crear un nuevo grupo si es el primer avistamiento del modelo
                porModelo.put(clave, g);
            }
            g.agregar(a); // Agregar el avistamiento al grupo correspondiente
        }

        grupos.addAll(porModelo.values()); // Trasvasar los grupos construidos a la lista principal
    }

    // ------------------------------
    // LÓGICA DE BORRADO Y ACCIONES
    // ------------------------------

    // Muestra una confirmación y elimina los grupos marcados por el usuario
    private void borrarSeleccionados() {
        List<GrupoAvion> gruposABorrar = new ArrayList<>(); // Lista de grupos seleccionados para eliminar
        int totalAvistamientos = 0; // Contador de avistamientos totales contenidos

        // Identificar cuáles grupos están marcados con el CheckBox de selección
        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                gruposABorrar.add(g); // Añadir a la lista de borrado
                totalAvistamientos += g.getCantidad(); // Sumar la cantidad de avistamientos
            }
        }

        if (gruposABorrar.isEmpty()) return; // Salir si no hay nada seleccionado

        // Si se van a eliminar múltiples avistamientos acumulados, solicitar confirmación
        if (totalAvistamientos > gruposABorrar.size()) {
            final int total = totalAvistamientos;
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar avistamientos") // Título del diálogo de confirmación
                    .setMessage("Se eliminarán " + total + " avistamientos de la colección. ¿Deseas continuar?") // Mensaje
                    .setPositiveButton("Eliminar", (d, w) -> eliminarGrupos(gruposABorrar)) // Confirmar eliminación
                    .setNegativeButton("Cancelar", null) // Cancelar borrado
                    .show();
        } else {
            eliminarGrupos(gruposABorrar); // Eliminar directamente si es un solo avistamiento por grupo
        }
    }

    // Procesa la eliminación física local y en Firestore de los grupos indicados
    private void eliminarGrupos(List<GrupoAvion> gruposABorrar) {
        for (GrupoAvion g : gruposABorrar) {
            for (Avion a : g.getAvistamientos()) {
                borrarAvionEnFirestore(a); // Borrar el registro correspondiente en la base de datos de la nube
                listaAviones.remove(a); // Borrar el avistamiento de la lista en memoria local
            }
        }

        // Guardar la nueva lista de avistamientos en SharedPreferences local
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        prefs.edit().putString("lista_aviones", new Gson().toJson(listaAviones)).apply(); // Serializar y aplicar cambios

        construirGrupos(); // Reconstruir los grupos con los datos actualizados
        adapter.notifyDataSetChanged(); // Notificar al adaptador para refrescar la pantalla
        actualizarPapelera(); // Actualizar la visibilidad del botón de papelera

        // Mostrar el mensaje descriptivo si la colección ha quedado vacía
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        }

        Toast.makeText(this, "Aviones eliminados de la colección", Toast.LENGTH_SHORT).show(); // Notificar al usuario
    }

    // Borra un documento de la subcolección de aviones en Firestore por su ID
    public void borrarAvionEnFirestore(Avion avion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Obtener la instancia de Firestore
        FirebaseAuth auth = FirebaseAuth.getInstance(); // Obtener la instancia de autenticación

        if (auth.getCurrentUser() == null) return; // Salir si no hay una sesión activa
        String uid = auth.getCurrentUser().getUid(); // Obtener el UID del usuario activo

        // Borrar el documento en la ruta: usuarios / {uid} / aviones / {id}
        db.collection("usuarios")
                .document(uid)
                .collection("aviones")
                .document(avion.getId())
                .delete() // Ejecutar orden de borrado en la nube
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Avión eliminado en Firestore: " + avion.getApodo())) // Log de éxito
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al borrar avión en Firestore", e)); // Log de error
    }

    // Abre la actividad de detalle de un avistamiento enviando su ID como argumento
    private void abrirDetalle(Avion avion) {
        Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class); // Definir la intención
        intent.putExtra("avion_id", avion.getId()); // Pasar el ID único del avión como extra
        startActivity(intent); // Iniciar la actividad
    }

    // Muestra un panel deslizante (BottomSheet) con la lista detallada de avistamientos individuales de un modelo
    private void mostrarAvistamientos(GrupoAvion grupo) {
        // Inflar el diseño XML del panel de diálogo personalizado desde los recursos
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null);

        // Crear instancia del BottomSheetDialog para mostrar el contenido de forma elegante desde abajo
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(vista); // Establecer la vista inflada dentro del diálogo

        // Localizar los componentes de la interfaz del diálogo
        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo); // Título que indica el modelo y cantidad
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos); // Lista para mostrar los avistamientos

        // Establecer el título del diálogo combinando el modelo y el total de capturas
        txtTitulo.setText(grupo.getModelo() + " (" + grupo.getCantidad() + ")");

        // Configurar el gestor de diseño para la lista interna del diálogo
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Implementar un adaptador anónimo para poblar la lista de avistamientos
        recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Inflar el diseño de cada fila individual de la lista
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // Obtener el avión específico basándose en la posición invertida (del más nuevo al más antiguo)
                Avion a = grupo.getAvistamientos().get(grupo.getCantidad() - 1 - position);

                // Vincular las vistas del ítem con sus referencias
                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula); // Campo para matrícula o ICAO
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha); // Campo para la fecha del avistamiento
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia); // Campo para la distancia recorrida
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto); // Indicador visual de foto adjunta

                // Asignar los valores a cada componente visual
                txtMat.setText((a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24()));
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento())));
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm()));

                // Mostrar un icono de cámara si el usuario añadió una foto personalizada a este avistamiento
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE);

                // Configurar el evento al pulsar sobre un avistamiento concreto
                holder.itemView.setOnClickListener(v -> {
                    abrirDetalle(a); // Abrir la actividad de detalle de este avión
                    dialog.dismiss(); // Cerrar el diálogo deslizante tras la selección
                });
            }

            @Override
            public int getItemCount() {
                return grupo.getCantidad(); // Retornar el total de avistamientos contenidos en el grupo
            }
        });

        dialog.show(); // Mostrar el diálogo deslizante en pantalla
    }

    // Muestra u oculta el botón flotante de eliminación masiva según el estado de las selecciones
    private void actualizarPapelera() {
        boolean haySeleccionados = false; // Variable de control de estado

        // Verificar si al menos uno de los grupos tiene activada la marca de selección
        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                haySeleccionados = true; // Activar bandera
                break; // Salir del bucle al encontrar el primero
            }
        }

        // Ajustar la visibilidad del botón en la interfaz según corresponda
        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE);
    }
}