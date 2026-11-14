// Declaración del paquete al que pertenece la actividad principal
package es.medac.skycollectorapp.activities;

// Importaciones de contexto, gráficos y utilidades de Android
import android.content.Context; // Contexto de la aplicación
import android.content.Intent; // Objeto para la navegación entre actividades
import android.content.SharedPreferences; // Almacenamiento persistente ligero
import android.graphics.Color; // Constantes de color para el fondo transparente del diálogo
import android.graphics.drawable.ColorDrawable; // Fondo dibujable para ventanas emergentes
import android.net.Uri; // Representación de direcciones URI para archivos o imágenes
import android.os.Bundle; // Pases de datos durante el ciclo de vida de la actividad
import android.util.Log; // Registro de logs en la consola Logcat
import android.view.LayoutInflater; // Conversor de archivos XML a objetos de vista en Java
import android.view.View; // Componente base de vistas de interfaz
import android.view.ViewGroup; // Contenedor padre de vistas
import android.widget.Button; // Componente de interfaz tipo botón
import android.widget.CheckBox; // Componente de casilla de verificación
import android.widget.EditText; // Componente de entrada de texto
import android.widget.ImageView; // Componente para mostrar imágenes
import android.widget.LinearLayout; // Contenedor lineal de vistas
import android.widget.TextView; // Componente para mostrar texto
import android.widget.Toast; // Notificaciones emergentes cortas

// Importaciones de AndroidX
import androidx.annotation.NonNull; // Marcador para parámetros que no pueden ser nulos
import androidx.appcompat.app.AlertDialog; // Cuadros de diálogo emergentes de confirmación
import androidx.appcompat.app.AppCompatActivity; // Clase base con compatibilidad para ActionBar
import androidx.recyclerview.widget.GridLayoutManager; // Gestor de diseño en cuadrícula para listas RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager; // Gestor de diseño lineal para el diálogo
import androidx.recyclerview.widget.RecyclerView; // Componente eficiente para listas de elementos

// Importaciones de librerías de Material Design y Firebase
import com.bumptech.glide.Glide; // Carga y procesado eficiente de imágenes
import com.google.android.material.bottomsheet.BottomSheetDialog; // Diálogo deslizante desde la parte inferior
import com.google.firebase.auth.FirebaseAuth; // Servicio de autenticación de usuario con Firebase
import com.google.firebase.auth.FirebaseUser; // Objeto de usuario autenticado en Firebase
import com.google.firebase.firestore.DocumentSnapshot; // Documento individual recuperado desde Firestore
import com.google.firebase.firestore.FirebaseFirestore; // Servicio de la base de datos Firestore en la nube
import com.google.firebase.firestore.SetOptions; // Opciones de fusión al escribir en documentos de Firestore
import com.google.gson.Gson; // Motor de conversión de objetos Java a formato JSON
import com.google.gson.reflect.TypeToken; // Preservación de tipos de colecciones genéricas para Gson

// Importaciones estándar de Java
import java.lang.reflect.Type; // Manejo de tipos de datos mediante reflexión
import java.text.SimpleDateFormat; // Formateador de fechas a cadenas de texto
import java.util.ArrayList; // Colección de lista dinámica
import java.util.Date; // Objeto para instantes temporales
import java.util.HashMap; // Mapa clave-valor para documentos de Firestore
import java.util.HashSet; // Conjunto sin duplicados para selecciones activas
import java.util.LinkedHashMap; // Mapa que conserva el orden de inserción de elementos
import java.util.List; // Interfaz para listas
import java.util.Locale; // Configuración regional de lenguaje
import java.util.Map; // Interfaz para mapas
import java.util.Set; // Interfaz para conjuntos

// Importaciones de modelos, adaptadores y recursos del proyecto
import es.medac.skycollectorapp.R; // Recursos del proyecto
import es.medac.skycollectorapp.adapters.AvionAdapter; // Adaptador para la lista de tarjetas de aviones
import es.medac.skycollectorapp.databinding.ActivityMainBinding; // Clase de vinculación de vistas ViewBinding generada
import es.medac.skycollectorapp.models.Avion; // Modelo de datos del objeto Avión
import es.medac.skycollectorapp.models.GrupoAvion; // Modelo de agrupación por modelo de avión

// Clase principal que representa la pantalla de inicio, catálogo personal, ranking público y borrado granular
public class MainActivity extends AppCompatActivity {

    // ------------------------------
    // VARIABLES Y ADAPTADORES
    // ------------------------------
    private ActivityMainBinding binding; // Referencia para el acceso directo a los elementos del XML mediante ViewBinding
    private AvionAdapter adapter; // Adaptador para gestionar la visualización de los grupos de aviones
    private List<Avion> listaAviones; // Lista completa en memoria de todos los avistamientos del usuario
    private List<GrupoAvion> grupos; // Lista de avistamientos agrupados por modelo para la interfaz
    private AlertDialog dialogoNombreActivo; // Referencia al diálogo de elección de nombre para evitar duplicados

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

        binding.btnPapelera.setOnClickListener(v -> iniciarFlujoBorrado()); // Iniciar asistente de selección y borrado

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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { // Verificar que exista una sesión activa
            startActivity(new Intent(this, LoginActivity.class)); // Redirigir al login si no hay sesión
            finish(); // Cerrar la actividad actual
            return; // Detener ejecución
        }
        cargarListaDeAviones(); // Recargar la colección local del usuario activo desde SharedPreferences
        sincronizarDesdeFirestore(); // Descargar los avistamientos guardados en la nube para este usuario
        cargarFotoPerfilMini(); // Cargar la foto de perfil en miniatura
        verificarYCargarNombreUsuario(); // Comprobar si el usuario ya eligió nombre o mostrar ventana emergente
    }

    // ------------------------------
    // GESTIÓN DE NOMBRE DE USUARIO Y RANKING PÚBLICO
    // ------------------------------

    // Comprueba si el usuario dispone de un apodo configurado en local o en Firestore; si no, abre ventana emergente
    private void verificarYCargarNombreUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario autenticado
        if (user == null) return; // Salir si no hay sesión

        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias de perfil
        String uid = user.getUid(); // Obtener UID único del usuario
        String nombreLocal = prefs.getString("nombre_" + uid, null); // Buscar nombre guardado localmente

        if (nombreLocal != null && !nombreLocal.trim().isEmpty()) { // Si ya existe un nombre configurado
            binding.txtNombreUsuario.setText(nombreLocal); // Mostrarlo en la cabecera
            actualizarRankingEnFirestore(nombreLocal); // Mantener sincronizado su puesto en el ranking público
            return; // Terminar comprobación
        }

        binding.txtNombreUsuario.setText("Piloto"); // Texto provisional mientras se consulta Firestore

        // Consultar en Firestore por si el usuario ya creó su apodo en otra sesión o dispositivo
        FirebaseFirestore.getInstance()
                .collection("usuarios") // Colección de usuarios
                .document(uid) // Documento del usuario activo
                .get() // Leer documento
                .addOnSuccessListener(doc -> { // Al recibir respuesta de Firestore
                    if (isDestroyed() || isFinishing()) return; // Ignorar si la actividad se cerró

                    String nombreNube = doc.getString("nombreUsuario"); // Obtener campo nombreUsuario
                    if (nombreNube != null && !nombreNube.trim().isEmpty()) { // Si ya tenía apodo guardado en la nube
                        prefs.edit().putString("nombre_" + uid, nombreNube).apply(); // Guardarlo en caché local
                        binding.txtNombreUsuario.setText(nombreNube); // Mostrarlo en pantalla
                        actualizarRankingEnFirestore(nombreNube); // Sincronizar colección pública de ranking
                    } else { // Si es un usuario nuevo que aún no ha elegido apodo
                        mostrarDialogoElegirNombre(user); // Abrir ventana emergente para que escoja su nombre
                    }
                })
                .addOnFailureListener(e -> { // Si falla la consulta de red
                    if (!isDestroyed() && !isFinishing()) { // Si la pantalla sigue activa
                        mostrarDialogoElegirNombre(user); // Pedir el nombre igualmente para no bloquear al usuario
                    }
                });
    }

    // Despliega una ventana emergente con la estética de la app para que el usuario escoja su nombre público
    private void mostrarDialogoElegirNombre(FirebaseUser user) {
        if (dialogoNombreActivo != null && dialogoNombreActivo.isShowing()) return; // Evitar abrir dos diálogos a la vez

        View vista = getLayoutInflater().inflate(R.layout.dialog_elegir_nombre, null); // Inflar diseño personalizado
        EditText etNombre = vista.findViewById(R.id.etNuevoNombreUsuario); // Campo de entrada del nombre
        Button btnConfirmar = vista.findViewById(R.id.btnGuardarNombreInicial); // Botón de confirmación

        // Si entró con Google y su cuenta tiene nombre público, sugerirlo en el cuadro para facilitar el paso
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            etNombre.setText(user.getDisplayName()); // Rellenar sugerencia
            etNombre.setSelection(etNombre.getText().length()); // Colocar cursor al final
        }

        AlertDialog dialog = new AlertDialog.Builder(this) // Crear constructor del diálogo
                .setView(vista) // Asignar la tarjeta personalizada
                .setCancelable(false) // Obligar a elegir un nombre antes de continuar
                .create(); // Instanciar diálogo

        if (dialog.getWindow() != null) { // Si la ventana del diálogo es accesible
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Quitar marco blanco por defecto
        }

        // Configurar acción al pulsar el botón Confirmar nombre
        btnConfirmar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim(); // Leer texto escrito sin espacios sobrantes
            if (nuevoNombre.length() < 3) { // Exigir mínimo 3 caracteres para el indicativo
                etNombre.setError("El nombre debe tener al menos 3 caracteres"); // Mostrar aviso
                etNombre.requestFocus(); // Enfocar el campo
                return; // Detener guardado
            }

            String uid = user.getUid(); // Obtener UID del usuario
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias
            prefs.edit().putString("nombre_" + uid, nuevoNombre).apply(); // Guardar nombre en almacenamiento local

            // Guardar el nombre de usuario en el documento principal del usuario en Firestore
            Map<String, Object> datosUsuario = new HashMap<>(); // Mapa de campos
            datosUsuario.put("nombreUsuario", nuevoNombre); // Añadir nombre elegido
            FirebaseFirestore.getInstance()
                    .collection("usuarios") // Colección de usuarios
                    .document(uid) // Documento del usuario
                    .set(datosUsuario, SetOptions.merge()); // Guardar sin sobrescribir otros campos

            actualizarRankingEnFirestore(nuevoNombre); // Registrar inmediatamente al usuario en la colección pública de ranking

            binding.txtNombreUsuario.setText(nuevoNombre); // Actualizar nombre en la cabecera
            dialog.dismiss(); // Cerrar ventana emergente
            Toast.makeText(this, "Bienvenido, " + nuevoNombre, Toast.LENGTH_SHORT).show(); // Saludar al piloto
        });

        dialogoNombreActivo = dialog; // Guardar referencia del diálogo abierto
        dialog.show(); // Mostrar ventana emergente en pantalla
    }

    // Cuenta únicamente los avistamientos de la colección que disponen de fotografía adjunta
    private int contarAvionesConFoto() {
        int totalVerificados = 0;
        for (Avion avion : listaAviones) {
            boolean tieneFoto = avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().trim().isEmpty();
            if (tieneFoto && avion.isEsFotoVerificada()) {
                totalVerificados++;
            }
        }
        return totalVerificados;
    }

    // Actualiza la colección pública 'ranking' en Firestore con el nombre público y total de aviones con foto
    private void actualizarRankingEnFirestore(String nombreUsuario) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener usuario autenticado
        if (user == null || nombreUsuario == null || nombreUsuario.trim().isEmpty()) return; // Salir si faltan datos

        Map<String, Object> datosRanking = new HashMap<>(); // Mapa con los únicos datos públicos necesarios para el ranking
        datosRanking.put("nombreUsuario", nombreUsuario); // Indicativo público elegido por el usuario
        datosRanking.put("totalAviones", contarAvionesConFoto()); // Cantidad de aviones cazados que tienen foto

        FirebaseFirestore.getInstance()
                .collection("ranking") // Colección pública dedicada exclusivamente al ranking global
                .document(user.getUid()) // Documento identificado por el UID del usuario
                .set(datosRanking, SetOptions.merge()) // Crear o actualizar puntuación y nombre
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al actualizar ranking público", e)); // Registrar posible fallo
    }

    // ------------------------------
    // CARGA Y SINCRONIZACIÓN DE DATOS
    // ------------------------------

    // Genera la clave de almacenamiento local asociada al UID del usuario autenticado
    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario actual
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo"; // Devolver clave única por cuenta
    }

    // Carga la miniatura de la foto de perfil guardada para el usuario activo
    private void cargarFotoPerfilMini() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario autenticado
        if (user == null) return; // Salir si no hay una sesión activa

        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Leer preferencias
        String fotoGuardada = prefs.getString("foto_" + user.getUid(), null); // Obtener la URI de la foto guardada

        if (fotoGuardada != null) { // Verificar si existe una fotografía guardada
            Glide.with(this) // Inicializar Glide en este contexto
                    .load(Uri.parse(fotoGuardada)) // Cargar la imagen desde la URI
                    .placeholder(android.R.drawable.ic_menu_myplaces) // Icono provisional mientras carga
                    .error(android.R.drawable.ic_menu_myplaces) // Icono de respaldo si falla la lectura
                    .circleCrop() // Aplicar corte circular
                    .into(binding.imgPerfilMini); // Insertar en la vista previa del perfil
        }
    }

    // Lee los avistamientos guardados en SharedPreferences para el usuario activo y reconstruye los grupos
    private void cargarListaDeAviones() {
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE); // Obtener preferencias
        String json = prefs.getString(getClaveListaUsuario(), null); // Leer la cadena JSON de avistamientos del usuario

        listaAviones.clear(); // Limpiar la lista de avistamientos en memoria

        if (json != null) { // Verificar si existen datos serializados
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType(); // Definir el tipo para deserializar
            List<Avion> avionesGuardados = new Gson().fromJson(json, type); // Deserializar la lista desde JSON
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados); // Agregar los elementos recuperados
        }

        refrescarInterfazColeccion(); // Actualizar grupos y estado visual de la lista
    }

    // Descarga los avistamientos almacenados en Firestore para el usuario activo y actualiza la caché local
    private void sincronizarDesdeFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener el usuario autenticado
        if (user == null) return; // Salir si no hay sesión iniciada

        FirebaseFirestore.getInstance() // Obtener instancia de Firestore
                .collection("usuarios") // Acceder a la colección de usuarios
                .document(user.getUid()) // Acceder al documento del usuario activo
                .collection("aviones") // Acceder a su subcolección de aviones
                .get() // Consultar todos los documentos almacenados
                .addOnSuccessListener(querySnapshot -> { // Al recibir respuesta exitosa
                    if (isDestroyed() || isFinishing()) return; // Evitar actualizar UI si la pantalla ya se cerró

                    List<Avion> listaNube = new ArrayList<>(); // Lista temporal para los aviones descargados
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) { // Recorrer cada documento recibido
                        Avion avion = doc.toObject(Avion.class); // Convertir el documento JSON a objeto Avion
                        if (avion != null) listaNube.add(avion); // Añadir a la lista descargada
                    }

                    // Si existen datos en la nube o la colección local estaba vacía, sincronizar estado
                    if (!listaNube.isEmpty() || listaAviones.isEmpty()) {
                        listaAviones.clear(); // Limpiar lista local en memoria
                        listaAviones.addAll(listaNube); // Volcar los datos recuperados de Firestore
                        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE); // Abrir preferencias
                        prefs.edit().putString(getClaveListaUsuario(), new Gson().toJson(listaAviones)).apply(); // Guardar respaldo local
                        refrescarInterfazColeccion(); // Reconstruir grupos y refrescar pantalla
                    }
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al sincronizar colección", e)); // Registrar error
    }

    // Reconstruye los grupos por modelo, alterna la visibilidad de la lista y actualiza el contador del ranking
    private void refrescarInterfazColeccion() {
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

        // Actualizar puntuación total en el ranking público si el usuario ya tiene nombre asignado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); // Obtener usuario actual
        if (user != null) { // Verificar sesión
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE); // Abrir preferencias de perfil
            String nombre = prefs.getString("nombre_" + user.getUid(), null); // Leer apodo guardado
            if (nombre != null) actualizarRankingEnFirestore(nombre); // Actualizar contador en Firestore
        }
    }

    // Organiza la lista de avistamientos individuales en grupos ordenados por modelo
    private void construirGrupos() {
        grupos.clear(); // Limpiar los grupos anteriores
        Map<String, GrupoAvion> porModelo = new LinkedHashMap<>(); // Mapa ordenado por el nombre del modelo

        for (Avion a : listaAviones) { // Iterar sobre cada avistamiento guardado
            String clave = a.getModelo() != null ? a.getModelo() : ""; // Obtener la clave del modelo
            GrupoAvion g = porModelo.get(clave); // Buscar si ya existe un grupo creado para este modelo
            if (g == null) { // Si no existe todavía
                g = new GrupoAvion(clave); // Crear un nuevo grupo para este modelo
                porModelo.put(clave, g); // Registrar el grupo en el mapa
            }
            g.agregar(a); // Agregar el avistamiento al grupo correspondiente
        }

        grupos.addAll(porModelo.values()); // Trasvasar los grupos construidos a la lista principal
    }

    // ------------------------------
    // FLUJO DE SELECCIÓN Y BORRADO GRANULAR
    // ------------------------------

    // Clasifica los grupos marcados entre unitarios (x1) y múltiples (x2+) para iniciar el asistente secuencial
    private void iniciarFlujoBorrado() {
        List<Avion> avionesParaBorrar = new ArrayList<>(); // Lista acumulativa de ejemplares concretos a eliminar
        List<GrupoAvion> gruposMultiples = new ArrayList<>(); // Cola de grupos con 2 o más ejemplares que requieren selección

        for (GrupoAvion g : grupos) { // Recorrer todos los grupos actuales
            if (g.isSeleccionado()) { // Verificar si el usuario marcó la tarjeta
                if (g.getCantidad() == 1) { // Si el grupo tiene un único avión
                    avionesParaBorrar.add(g.getRepresentante()); // Añadir directamente a la lista de borrado
                } else { // Si el grupo contiene múltiples ejemplares (x2 o más)
                    gruposMultiples.add(g); // Encolar para abrir su selector individual
                }
            }
        }

        if (avionesParaBorrar.isEmpty() && gruposMultiples.isEmpty()) return; // Salir si no hay elementos marcados

        procesarSiguienteGrupoMultiple(0, gruposMultiples, avionesParaBorrar); // Arrancar el asistente desde el primer grupo múltiple
    }

    // Despliega secuencialmente un BottomSheet con checkboxes para cada grupo con más de un avión
    private void procesarSiguienteGrupoMultiple(int indice, List<GrupoAvion> gruposMultiples, List<Avion> avionesParaBorrar) {
        if (indice >= gruposMultiples.size()) { // Cuando ya se han recorrido todos los grupos múltiples seleccionados
            confirmarBorradoFinal(avionesParaBorrar); // Mostrar diálogo final de confirmación antes de eliminar
            return; // Finalizar recursión
        }

        GrupoAvion grupoActual = gruposMultiples.get(indice); // Obtener el grupo correspondiente al paso actual
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null); // Inflar el layout del selector
        BottomSheetDialog dialog = new BottomSheetDialog(this); // Instanciar el panel inferior
        dialog.setContentView(vista); // Asignar la vista inflada al diálogo
        dialog.setCancelable(false); // Evitar cierre accidental pulsando fuera durante el asistente

        // Vincular las vistas de cabecera, lista y botonera del selector
        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo); // Título del modelo actual
        CheckBox cbTodos = vista.findViewById(R.id.cbSeleccionarTodos); // Casilla superior "Todos"
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos); // Lista de ejemplares del modelo
        LinearLayout layoutBotones = vista.findViewById(R.id.layoutBotonesBorradoGrupo); // Contenedor de botones inferiores
        Button btnCancelar = vista.findViewById(R.id.btnCancelarSeleccionGrupo); // Botón para cancelar
        Button btnConfirmar = vista.findViewById(R.id.btnConfirmarSeleccionGrupo); // Botón para confirmar y continuar

        // Indicar progreso (ej. "1/2") si el usuario marcó varias colecciones con múltiples aviones
        String progreso = gruposMultiples.size() > 1 ? " (" + (indice + 1) + "/" + gruposMultiples.size() + ")" : "";
        txtTitulo.setText(grupoActual.getModelo() + progreso); // Mostrar modelo y paso actual

        cbTodos.setVisibility(View.VISIBLE); // Hacer visible el checkbox superior "Todos"
        layoutBotones.setVisibility(View.VISIBLE); // Hacer visible la botonera inferior
        btnConfirmar.setText(indice + 1 < gruposMultiples.size() ? "Siguiente" : "Aceptar"); // Ajustar etiqueta del botón

        Set<Avion> seleccionadosEnGrupo = new HashSet<>(); // Conjunto de ejemplares marcados dentro de este grupo
        final boolean[] actualizandoMasivo = {false}; // Bandera para evitar bucles entre cbTodos y los ítems

        recycler.setLayoutManager(new LinearLayoutManager(this)); // Configurar lista vertical
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapterSeleccion = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false); // Inflar fila
                return new RecyclerView.ViewHolder(v) {}; // Devolver ViewHolder
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // Obtener el ejemplar ordenado del más reciente al más antiguo
                Avion a = grupoActual.getAvistamientos().get(grupoActual.getCantidad() - 1 - position);

                // Vincular componentes visuales de la fila
                CheckBox cbItem = holder.itemView.findViewById(R.id.cbSeleccionarAvistamiento); // Casilla izquierda
                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula); // Matrícula o ICAO
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha); // Fecha de captura
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia); // Distancia
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto); // Icono de foto

                cbItem.setVisibility(View.VISIBLE); // Mostrar la casilla de selección a la izquierda
                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24()); // Mostrar identificador
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento()))); // Formatear fecha
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm())); // Formatear distancia
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE); // Mostrar icono si tiene foto

                cbItem.setOnCheckedChangeListener(null); // Limpiar listener anterior antes de reciclar la vista
                cbItem.setChecked(seleccionadosEnGrupo.contains(a)); // Sincronizar estado visual del checkbox

                // Escuchar cambios individuales en la casilla de cada avión
                cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) seleccionadosEnGrupo.add(a); // Añadir ejemplar al conjunto si se marca
                    else seleccionadosEnGrupo.remove(a); // Retirar ejemplar del conjunto si se desmarca

                    actualizandoMasivo[0] = true; // Activar bandera de actualización silenciosa
                    cbTodos.setChecked(seleccionadosEnGrupo.size() == grupoActual.getCantidad()); // Marcar "Todos" solo si están todos
                    actualizandoMasivo[0] = false; // Desactivar bandera
                });

                // Permitir pulsar en cualquier parte de la fila para alternar su checkbox
                holder.itemView.setOnClickListener(v -> cbItem.setChecked(!cbItem.isChecked()));
            }

            @Override
            public int getItemCount() {
                return grupoActual.getCantidad(); // Retornar total de ejemplares del grupo
            }
        };

        recycler.setAdapter(adapterSeleccion); // Conectar adaptador de selección al RecyclerView

        // Configurar el evento del checkbox superior "Todos"
        cbTodos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoMasivo[0]) return; // Ignorar si el cambio viene provocado por un ítem individual
            seleccionadosEnGrupo.clear(); // Limpiar selección actual del grupo
            if (isChecked) { // Si se marcó "Todos"
                seleccionadosEnGrupo.addAll(grupoActual.getAvistamientos()); // Añadir todos los ejemplares del modelo
            }
            adapterSeleccion.notifyDataSetChanged(); // Refrescar todas las casillas de la lista
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss()); // Cerrar el diálogo y cancelar todo el borrado

        // Al pulsar Siguiente/Aceptar, acumular los elegidos y pasar al siguiente grupo de la cola
        btnConfirmar.setOnClickListener(v -> {
            avionesParaBorrar.addAll(seleccionadosEnGrupo); // Sumar los ejemplares elegidos de este modelo
            dialog.dismiss(); // Cerrar diálogo actual
            procesarSiguienteGrupoMultiple(indice + 1, gruposMultiples, avionesParaBorrar); // Avanzar al siguiente grupo
        });

        dialog.show(); // Mostrar el panel selector en pantalla
    }

    // Muestra siempre un aviso de confirmación antes de ejecutar el borrado (tanto para 1 avión como para varios)
    private void confirmarBorradoFinal(List<Avion> avionesParaBorrar) {
        if (avionesParaBorrar.isEmpty()) { // Si el usuario no marcó ningún ejemplar en los selectores
            Toast.makeText(this, "No se seleccionó ningún avión para eliminar", Toast.LENGTH_SHORT).show(); // Informar
            return; // Salir sin borrar nada
        }

        String mensaje; // Texto descriptivo del diálogo de confirmación
        if (avionesParaBorrar.size() == 1) { // Mensaje personalizado cuando se va a eliminar 1 solo avión
            Avion unico = avionesParaBorrar.get(0); // Obtener el avión único
            String identificador = (unico.getMatricula() != null && !unico.getMatricula().isEmpty())
                    ? unico.getMatricula() // Usar matrícula si está disponible
                    : unico.getIcao24(); // Usar código ICAO como respaldo
            mensaje = "¿Seguro que quieres eliminar el " + unico.getModelo()
                    + (identificador != null ? " (" + identificador + ")" : "") + " de tu colección?";
        } else { // Mensaje resumen cuando se van a eliminar múltiples ejemplares
            mensaje = "Se eliminarán " + avionesParaBorrar.size() + " avistamientos seleccionados de tu colección. ¿Deseas continuar?";
        }

        // Construir y mostrar el cuadro de diálogo de confirmación obligatorio
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación") // Título del aviso
                .setMessage(mensaje) // Detalle de lo que se va a borrar
                .setPositiveButton("Eliminar", (d, w) -> ejecutarBorradoAviones(avionesParaBorrar)) // Confirmar y borrar
                .setNegativeButton("Cancelar", null) // Cancelar operación
                .show(); // Mostrar diálogo
    }

    // Procesa la eliminación física en Firestore y en SharedPreferences de los ejemplares confirmados
    private void ejecutarBorradoAviones(List<Avion> avionesParaBorrar) {
        for (Avion avion : avionesParaBorrar) { // Recorrer cada avión confirmado
            borrarAvionEnFirestore(avion); // Eliminar documento en Firebase Firestore
            listaAviones.remove(avion); // Eliminar objeto de la lista local en memoria
        }

        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE); // Abrir preferencias
        prefs.edit().putString(getClaveListaUsuario(), new Gson().toJson(listaAviones)).apply(); // Guardar lista actualizada

        refrescarInterfazColeccion(); // Reconstruir grupos, actualizar pantalla y recalcular puntuación en el ranking
        Toast.makeText(this,
                avionesParaBorrar.size() == 1 ? "Avión eliminado de la colección" : "Aviones eliminados de la colección",
                Toast.LENGTH_SHORT).show(); // Notificar resultado al usuario
    }

    // Borra un documento de la subcolección de aviones en Firestore por su ID
    public void borrarAvionEnFirestore(Avion avion) {
        FirebaseAuth auth = FirebaseAuth.getInstance(); // Obtener instancia de autenticación
        if (auth.getCurrentUser() == null || avion.getId() == null) return; // Salir si no hay sesión o ID válido

        FirebaseFirestore.getInstance() // Obtener instancia de Firestore
                .collection("usuarios") // Colección raíz de usuarios
                .document(auth.getCurrentUser().getUid()) // Documento del usuario activo
                .collection("aviones") // Subcolección de aviones
                .document(avion.getId()) // Documento concreto del avistamiento
                .delete() // Ejecutar borrado en la nube
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Avión eliminado: " + avion.getApodo())) // Log de éxito
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al borrar avión en Firestore", e)); // Log de error
    }

    // ------------------------------
    // NAVEGACIÓN Y VISTA DE DETALLE
    // ------------------------------

    // Abre la actividad de detalle de un avistamiento enviando su ID como argumento
    private void abrirDetalle(Avion avion) {
        Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class); // Definir intención
        intent.putExtra("avion_id", avion.getId()); // Adjuntar ID único del avión
        startActivity(intent); // Iniciar pantalla de detalle
    }

    // Muestra un panel deslizante (BottomSheet) en modo consulta para abrir un avistamiento concreto
    private void mostrarAvistamientos(GrupoAvion grupo) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null); // Inflar layout del diálogo
        BottomSheetDialog dialog = new BottomSheetDialog(this); // Crear instancia del panel inferior
        dialog.setContentView(vista); // Asignar vista al diálogo

        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo); // Título del grupo
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos); // Lista interna del grupo

        txtTitulo.setText(grupo.getModelo() + " (" + grupo.getCantidad() + ")"); // Asignar título y cantidad
        recycler.setLayoutManager(new LinearLayoutManager(this)); // Configurar disposición vertical

        // Implementar adaptador para mostrar cada avistamiento en modo lectura
        recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false); // Inflar fila
                return new RecyclerView.ViewHolder(v) {}; // Devolver ViewHolder
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // Obtener el ejemplar ordenado del más nuevo al más antiguo
                Avion a = grupo.getAvistamientos().get(grupo.getCantidad() - 1 - position);

                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula); // Campo matrícula/ICAO
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha); // Campo fecha
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia); // Campo distancia
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto); // Icono de foto

                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24()); // Mostrar matrícula
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento()))); // Mostrar fecha
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm())); // Mostrar distancia
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE); // Mostrar icono de cámara si hay foto

                // Al pulsar sobre un ejemplar, abrir su ficha técnica y cerrar el panel
                holder.itemView.setOnClickListener(v -> {
                    abrirDetalle(a); // Navegar a DetalleAvionActivity
                    dialog.dismiss(); // Cerrar el diálogo
                });
            }

            @Override
            public int getItemCount() {
                return grupo.getCantidad(); // Retornar total de ejemplares del grupo
            }
        });

        dialog.show(); // Mostrar el panel deslizante
    }

    // Muestra u oculta el botón superior de papelera según si existe algún grupo marcado
    private void actualizarPapelera() {
        boolean haySeleccionados = false; // Variable de control

        for (GrupoAvion g : grupos) { // Recorrer todos los grupos
            if (g.isSeleccionado()) { // Verificar si alguno está seleccionado
                haySeleccionados = true; // Activar bandera
                break; // Detener búsqueda
            }
        }

        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE); // Actualizar visibilidad de la papelera
    }
}