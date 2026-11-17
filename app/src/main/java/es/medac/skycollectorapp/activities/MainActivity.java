// Declaración del paquete al que pertenece la actividad principal
package es.medac.skycollectorapp.activities;

// Importación para acceder al contexto y modos de almacenamiento de la aplicación
import android.content.Context;
// Importación para navegar entre distintas pantallas mediante Intents
import android.content.Intent;
// Importación para leer y guardar datos persistentes ligeros en SharedPreferences
import android.content.SharedPreferences;
// Importación para utilizar constantes de color
import android.graphics.Color;
// Importación para establecer fondos transparentes en diálogos personalizados
import android.graphics.drawable.ColorDrawable;
// Importación para manipular rutas URI de imágenes de perfil
import android.net.Uri;
// Importación para gestionar el ciclo de vida y estado de la actividad
import android.os.Bundle;
// Importación para registrar mensajes y errores en la consola Logcat
import android.util.Log;
// Importación para inflar diseños XML dinámicamente
import android.view.LayoutInflater;
// Importación de la clase base de componentes visuales
import android.view.View;
// Importación para manipular contenedores de vistas
import android.view.ViewGroup;
// Importación para botones estándar de la interfaz
import android.widget.Button;
// Importación para casillas de verificación en el borrado múltiple
import android.widget.CheckBox;
// Importación para campos de texto editables
import android.widget.EditText;
// Importación para mostrar imágenes en los ítems y perfil
import android.widget.ImageView;
// Importación para contenedores lineales
import android.widget.LinearLayout;
// Importación para etiquetas de texto
import android.widget.TextView;
// Importación para mostrar notificaciones emergentes breves
import android.widget.Toast;

// Importación para indicar parámetros que no aceptan valores nulos
import androidx.annotation.NonNull;
// Importación para mostrar cuadros de diálogo modales
import androidx.appcompat.app.AlertDialog;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación para organizar la cuadrícula de cartas en 2 columnas
import androidx.recyclerview.widget.GridLayoutManager;
// Importación para organizar listas verticales en los desplegables inferiores
import androidx.recyclerview.widget.LinearLayoutManager;
// Importación del componente de lista reciclable
import androidx.recyclerview.widget.RecyclerView;

// Importación de Glide para carga y recorte circular de imágenes
import com.bumptech.glide.Glide;
// Importación del panel modal deslizable inferior de Material Design
import com.google.android.material.bottomsheet.BottomSheetDialog;
// Importación para consultar el estado de autenticación en Firebase
import com.google.firebase.auth.FirebaseAuth;
// Importación para obtener los datos del usuario autenticado
import com.google.firebase.auth.FirebaseUser;
// Importación para leer documentos obtenidos desde Firestore
import com.google.firebase.firestore.DocumentSnapshot;
// Importación para conectar con la base de datos Cloud Firestore
import com.google.firebase.firestore.FirebaseFirestore;
// Importación para realizar escrituras parciales combinadas (merge) en Firestore
import com.google.firebase.firestore.SetOptions;
// Importación de Gson para serializar y deserializar listas en JSON
import com.google.gson.Gson;
// Importación de TypeToken para especificar tipos genéricos en Gson
import com.google.gson.reflect.TypeToken;

// Importación para reflejar tipos en tiempo de ejecución
import java.lang.reflect.Type;
// Importación para formatear marcas de tiempo de avistamientos
import java.text.SimpleDateFormat;
// Importación de listas dinámicas
import java.util.ArrayList;
// Importación para instanciar fechas a partir de milisegundos
import java.util.Date;
// Importación de mapas clave-valor estándar
import java.util.HashMap;
// Importación de conjuntos sin duplicados para selección múltiple
import java.util.HashSet;
// Importación de mapas que conservan el orden de inserción
import java.util.LinkedHashMap;
// Importación de la interfaz List
import java.util.List;
// Importación para configurar formatos regionales
import java.util.Locale;
// Importación de la interfaz Map
import java.util.Map;
// Importación de la interfaz Set
import java.util.Set;

// Importación de recursos XML del proyecto
import es.medac.skycollectorapp.R;
// Importación del adaptador de grupos de aviones
import es.medac.skycollectorapp.adapters.AvionAdapter;
// Importación del enlace generado para activity_main.xml
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
// Importación del modelo de datos Avion
import es.medac.skycollectorapp.models.Avion;
// Importación del modelo de agrupación por modelo GrupoAvion
import es.medac.skycollectorapp.models.GrupoAvion;
// Importación del gestor de puntuación y sincronización de ranking público
import es.medac.skycollectorapp.utils.RankingManager;

// Clase principal que representa la pantalla de inicio, catálogo personal, ranking público y borrado granular
public class MainActivity extends AppCompatActivity {

    // Enlace ViewBinding para acceder directamente a las vistas de activity_main.xml
    private ActivityMainBinding binding;
    // Adaptador que pinta las tarjetas agrupadas en el RecyclerView principal
    private AvionAdapter adapter;
    // Lista completa con todos los aviones avistados por el usuario
    private List<Avion> listaAviones;
    // Lista de aviones agrupados por modelo para evitar tarjetas duplicadas
    private List<GrupoAvion> grupos;
    // Referencia al diálogo activo de elección de apodo para evitar duplicarlo
    private AlertDialog dialogoNombreActivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Infla el diseño mediante ViewBinding y lo establece como vista raíz
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializa las listas vacías de aviones individuales y grupos
        listaAviones = new ArrayList<>();
        grupos = new ArrayList<>();

        // Configura el adaptador definiendo el clic sobre tarjetas y el cambio de selección
        adapter = new AvionAdapter(grupos,
                (grupo, position) -> {
                    // Si el grupo solo tiene 1 avión, abre directamente su ficha de detalle
                    if (grupo.getCantidad() == 1) abrirDetalle(grupo.getRepresentante());
                        // Si tiene varios avistamientos del mismo modelo, abre el desplegable de ejemplares
                    else mostrarAvistamientos(grupo);
                },
                // Actualiza la visibilidad del botón de papelera al marcar o desmarcar grupos
                this::actualizarPapelera
        );

        // Configura el RecyclerView en formato cuadrícula de 2 columnas y le asigna el adaptador
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        // Configura el botón flotante para abrir la pantalla de añadir avión manualmente
        binding.btnAddAvion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddAvionActivity.class))
        );

        // Configura el botón de papelera para iniciar el borrado de los elementos seleccionados
        binding.btnPapelera.setOnClickListener(v -> iniciarFlujoBorrado());

        // Configura la tarjeta de perfil superior para abrir PerfilActivity
        binding.cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class))
        );

        // Configura el botón de acceso al asistente virtual ChatbotActivity
        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class))
        );

        // Configura el botón de acceso al radar aéreo en tiempo real MapaActivity
        binding.btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapaActivity.class))
        );

        // Configura el botón de acceso a la clasificación global RankingActivity
        binding.btnRanking.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RankingActivity.class))
        );
    }

    @Override
    protected void onResume() {
        // Llama al método onResume de la superclase
        super.onResume();
        // Si no hay sesión activa en Firebase, redirige a LoginActivity y cierra esta pantalla
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        // Carga la colección local desde SharedPreferences
        cargarListaDeAviones();
        // Descarga y fusiona cambios desde Cloud Firestore
        sincronizarDesdeFirestore();
        // Actualiza la miniatura de la foto de perfil en la barra superior
        cargarFotoPerfilMini();
        // Verifica y muestra el apodo público del piloto
        verificarYCargarNombreUsuario();
    }

    // ------------------------------
    // GESTIÓN DE NOMBRE DE USUARIO Y RANKING PÚBLICO
    // ------------------------------

    // Comprueba si el usuario dispone de un apodo configurado en local o en Firestore; si no, abre ventana emergente
    private void verificarYCargarNombreUsuario() {
        // Obtiene el usuario autenticado actual
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Consulta si ya existe un nombre guardado en SharedPreferences para este UID
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String uid = user.getUid();
        String nombreLocal = prefs.getString("nombre_" + uid, null);

        // Si existe nombre en local, lo muestra directamente y sincroniza el ranking
        if (nombreLocal != null && !nombreLocal.trim().isEmpty()) {
            binding.txtNombreUsuario.setText(nombreLocal);
            RankingManager.sincronizarRankingUsuario(listaAviones, nombreLocal, null, null);
            return;
        }

        // Muestra un texto provisional mientras consulta el perfil en la nube
        binding.txtNombreUsuario.setText(getString(R.string.piloto_provisional));

        // Consulta el documento del usuario en Firestore por si ya eligió nombre en otro dispositivo
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    // Aborta si la actividad ya fue destruida
                    if (isDestroyed() || isFinishing()) return;

                    // Extrae el campo nombreUsuario del documento
                    String nombreNube = doc.getString("nombreUsuario");
                    if (nombreNube != null && !nombreNube.trim().isEmpty()) {
                        // Guarda el nombre recuperado en local, lo muestra y actualiza ranking
                        prefs.edit().putString("nombre_" + uid, nombreNube).apply();
                        binding.txtNombreUsuario.setText(nombreNube);
                        RankingManager.sincronizarRankingUsuario(listaAviones, nombreNube, null, null);
                    } else {
                        // Si tampoco tiene nombre en la nube, pide al usuario que elija uno
                        mostrarDialogoElegirNombre(user);
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de fallo de red, solicita elegir nombre si la actividad sigue activa
                    if (!isDestroyed() && !isFinishing()) {
                        mostrarDialogoElegirNombre(user);
                    }
                });
    }

    // Despliega una ventana emergente con la estética de la app para que el usuario escoja su nombre público
    private void mostrarDialogoElegirNombre(FirebaseUser user) {
        // Evita abrir más de un diálogo simultáneamente
        if (dialogoNombreActivo != null && dialogoNombreActivo.isShowing()) return;

        // Infla el diseño personalizado del diálogo y obtiene sus controles
        View vista = getLayoutInflater().inflate(R.layout.dialog_elegir_nombre, null);
        EditText etNombre = vista.findViewById(R.id.etNuevoNombreUsuario);
        Button btnConfirmar = vista.findViewById(R.id.btnGuardarNombreInicial);

        // Si el usuario tiene nombre visible en su cuenta (ej. Google), lo sugiere en el campo
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            etNombre.setText(user.getDisplayName());
            etNombre.setSelection(etNombre.getText().length());
        }

        // Construye el AlertDialog no cancelable
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(vista)
                .setCancelable(false)
                .create();

        // Hace transparente el fondo de ventana para respetar los bordes redondeados del layout XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Configura el botón de confirmación del apodo inicial
        btnConfirmar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            // Valida que el nombre tenga al menos 3 caracteres
            if (nuevoNombre.length() < 3) {
                etNombre.setError(getString(R.string.error_nombre_corto));
                etNombre.requestFocus();
                return;
            }

            // Guarda el nuevo apodo en SharedPreferences local
            String uid = user.getUid();
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
            prefs.edit().putString("nombre_" + uid, nuevoNombre).apply();

            // Guarda el nombre de usuario en su documento privado de Firestore
            Map<String, Object> datosUsuario = new HashMap<>();
            datosUsuario.put("nombreUsuario", nuevoNombre);
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .set(datosUsuario, SetOptions.merge());

            // Sincroniza inmediatamente la puntuación y nombre en el ranking público
            RankingManager.sincronizarRankingUsuario(listaAviones, nuevoNombre, null, null);

            // Actualiza el texto en pantalla, cierra el diálogo y da la bienvenida
            binding.txtNombreUsuario.setText(nuevoNombre);
            dialog.dismiss();
            Toast.makeText(this, getString(R.string.toast_bienvenido, nuevoNombre), Toast.LENGTH_SHORT).show();
        });

        // Almacena la referencia y muestra el cuadro de diálogo
        dialogoNombreActivo = dialog;
        dialog.show();
    }

    // ------------------------------
    // CARGA Y SINCRONIZACIÓN DE DATOS
    // ------------------------------

    private String getClaveListaUsuario() {
        // Obtiene el usuario actual de FirebaseAuth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Devuelve la clave única vinculada al UID o una clave anónima de respaldo
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    private void cargarFotoPerfilMini() {
        // Comprueba que haya sesión iniciada
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Lee la URI o URL de la foto de perfil almacenada en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String fotoGuardada = prefs.getString("foto_" + user.getUid(), null);

        // Si hay foto guardada, la renderiza recortada en círculo con Glide
        if (fotoGuardada != null) {
            Glide.with(this)
                    .load(Uri.parse(fotoGuardada))
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(binding.imgPerfilMini);
        }
    }

    private void cargarListaDeAviones() {
        // Abre el almacenamiento local de datos de la colección y lee el JSON del usuario
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        String json = prefs.getString(getClaveListaUsuario(), null);

        // Vacía la lista en memoria antes de recargarla
        listaAviones.clear();

        // Si existen datos guardados, los deserializa con Gson y los añade a la lista
        if (json != null) {
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            List<Avion> avionesGuardados = new Gson().fromJson(json, type);
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados);
        }

        // Actualiza la interfaz gráfica y las agrupaciones
        refrescarInterfazColeccion();
    }

    private void sincronizarDesdeFirestore() {
        // Verifica que exista un usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Descarga todos los documentos de la subcolección "aviones" del usuario en Firestore
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(user.getUid())
                .collection("aviones")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Aborta si la actividad ya no está activa
                    if (isDestroyed() || isFinishing()) return;

                    // Convierte cada documento Firestore en un objeto Avion
                    List<Avion> listaNube = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Avion avion = doc.toObject(Avion.class);
                        if (avion != null) listaNube.add(avion);
                    }

                    // Si la nube contiene datos o la lista local estaba vacía, actualiza local y refresca UI
                    if (!listaNube.isEmpty() || listaAviones.isEmpty()) {
                        listaAviones.clear();
                        listaAviones.addAll(listaNube);
                        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
                        prefs.edit().putString(getClaveListaUsuario(), new Gson().toJson(listaAviones)).apply();
                        refrescarInterfazColeccion();
                    }
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al sincronizar colección", e));
    }

    // Reconstruye los grupos por modelo, alterna la visibilidad de la lista y actualiza puntos y vitrina en ranking
    private void refrescarInterfazColeccion() {
        // Agrupa los aviones individuales por su modelo comercial
        construirGrupos();

        // Muestra el mensaje de colección vacía o la cuadrícula según haya registros
        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.txtVacio.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }

        // Notifica al adaptador que los datos han cambiado y actualiza el icono de papelera
        adapter.notifyDataSetChanged();
        actualizarPapelera();

        // Sincroniza el estado actualizado de la colección con el ranking público
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
            String nombre = prefs.getString("nombre_" + user.getUid(), null);
            RankingManager.sincronizarRankingUsuario(listaAviones, nombre, null, null);
        }
    }

    private void construirGrupos() {
        // Limpia los grupos anteriores y crea un mapa ordenado por inserción
        grupos.clear();
        Map<String, GrupoAvion> porModelo = new LinkedHashMap<>();

        // Recorre todos los aviones y los agrupa bajo la clave de su modelo
        for (Avion a : listaAviones) {
            String clave = a.getModelo() != null ? a.getModelo() : "";
            GrupoAvion g = porModelo.get(clave);
            if (g == null) {
                g = new GrupoAvion(clave);
                porModelo.put(clave, g);
            }
            g.agregar(a);
        }

        // Vuelca todos los grupos generados en la lista que alimenta el RecyclerView
        grupos.addAll(porModelo.values());
    }

    // ------------------------------
    // FLUJO DE SELECCIÓN Y BORRADO GRANULAR
    // ------------------------------

    private void iniciarFlujoBorrado() {
        // Prepara listas separadas para ejemplares únicos y grupos con múltiples avistamientos
        List<Avion> avionesParaBorrar = new ArrayList<>();
        List<GrupoAvion> gruposMultiples = new ArrayList<>();

        // Clasifica los grupos seleccionados por el usuario
        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                if (g.getCantidad() == 1) {
                    // Si solo hay un ejemplar, pasa directamente a la lista de borrado
                    avionesParaBorrar.add(g.getRepresentante());
                } else {
                    // Si hay varios ejemplares, requiere selección granular
                    gruposMultiples.add(g);
                }
            }
        }

        // Si no había nada seleccionado, detiene el flujo
        if (avionesParaBorrar.isEmpty() && gruposMultiples.isEmpty()) return;

        // Inicia el recorrido secuencial por cada grupo múltiple seleccionado
        procesarSiguienteGrupoMultiple(0, gruposMultiples, avionesParaBorrar);
    }

    private void procesarSiguienteGrupoMultiple(int indice, List<GrupoAvion> gruposMultiples, List<Avion> avionesParaBorrar) {
        // Cuando ya se procesaron todos los grupos múltiples, pide confirmación final de borrado
        if (indice >= gruposMultiples.size()) {
            confirmarBorradoFinal(avionesParaBorrar);
            return;
        }

        // Obtiene el grupo actual e infla el panel modal inferior (BottomSheetDialog)
        GrupoAvion grupoActual = gruposMultiples.get(indice);
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(vista);
        dialog.setCancelable(false);

        // Vincula los componentes internos del diálogo de selección granular
        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo);
        CheckBox cbTodos = vista.findViewById(R.id.cbSeleccionarTodos);
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos);
        LinearLayout layoutBotones = vista.findViewById(R.id.layoutBotonesBorradoGrupo);
        Button btnCancelar = vista.findViewById(R.id.btnCancelarSeleccionGrupo);
        Button btnConfirmar = vista.findViewById(R.id.btnConfirmarSeleccionGrupo);

        // Muestra el nombre del modelo y el contador de progreso si hay varios grupos
        String progreso = gruposMultiples.size() > 1 ? " (" + (indice + 1) + "/" + gruposMultiples.size() + ")" : "";
        txtTitulo.setText(grupoActual.getModelo() + progreso);

        // Hace visibles la casilla de "Seleccionar todos" y la botonera inferior
        cbTodos.setVisibility(View.VISIBLE);
        layoutBotones.setVisibility(View.VISIBLE);
        btnConfirmar.setText(indice + 1 < gruposMultiples.size() ? getString(R.string.btn_siguiente) : getString(R.string.btn_aceptar));

        // Conjunto para almacenar los ejemplares individuales marcados en este grupo
        Set<Avion> seleccionadosEnGrupo = new HashSet<>();
        final boolean[] actualizandoMasivo = {false};

        // Configura el RecyclerView interno con la lista de avistamientos del grupo
        recycler.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapterSeleccion = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Infla cada fila individual de avistamiento
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // Obtiene el avión correspondiente en orden inverso (más reciente primero)
                Avion a = grupoActual.getAvistamientos().get(grupoActual.getCantidad() - 1 - position);

                // Enlaza las vistas de la fila
                CheckBox cbItem = holder.itemView.findViewById(R.id.cbSeleccionarAvistamiento);
                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula);
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha);
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia);
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto);

                // Muestra la casilla de selección y rellena matrícula, fecha, distancia e icono de foto
                cbItem.setVisibility(View.VISIBLE);
                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24());
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento())));
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm()));
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE);

                // Limpia el listener previo antes de asignar el estado actual para evitar disparos falsos al reciclar
                cbItem.setOnCheckedChangeListener(null);
                cbItem.setChecked(seleccionadosEnGrupo.contains(a));

                // Actualiza el conjunto de seleccionados y sincroniza la casilla "Seleccionar todos"
                cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) seleccionadosEnGrupo.add(a);
                    else seleccionadosEnGrupo.remove(a);

                    actualizandoMasivo[0] = true;
                    cbTodos.setChecked(seleccionadosEnGrupo.size() == grupoActual.getCantidad());
                    actualizandoMasivo[0] = false;
                });

                // Permite alternar la selección pulsando en cualquier parte de la fila
                holder.itemView.setOnClickListener(v -> cbItem.setChecked(!cbItem.isChecked()));
            }

            @Override
            public int getItemCount() {
                // Devuelve el número total de avistamientos en el grupo
                return grupoActual.getCantidad();
            }
        };

        // Asigna el adaptador al RecyclerView del diálogo
        recycler.setAdapter(adapterSeleccion);

        // Gestiona la selección o deselección masiva de todos los ejemplares del grupo
        cbTodos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoMasivo[0]) return;
            seleccionadosEnGrupo.clear();
            if (isChecked) {
                seleccionadosEnGrupo.addAll(grupoActual.getAvistamientos());
            }
            adapterSeleccion.notifyDataSetChanged();
        });

        // Cancela todo el flujo de borrado al pulsar Cancelar
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        // Añade los ejemplares elegidos, cierra el panel actual y avanza al siguiente grupo múltiple
        btnConfirmar.setOnClickListener(v -> {
            avionesParaBorrar.addAll(seleccionadosEnGrupo);
            dialog.dismiss();
            procesarSiguienteGrupoMultiple(indice + 1, gruposMultiples, avionesParaBorrar);
        });

        // Muestra el BottomSheetDialog en pantalla
        dialog.show();
    }

    private void confirmarBorradoFinal(List<Avion> avionesParaBorrar) {
        // Si al final no quedó ningún ejemplar marcado, avisa al usuario y aborta
        if (avionesParaBorrar.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_ningun_avion_seleccionado), Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepara un mensaje personalizado según si se va a borrar 1 avión o varios
        String mensaje;
        if (avionesParaBorrar.size() == 1) {
            Avion unico = avionesParaBorrar.get(0);
            String identificador = (unico.getMatricula() != null && !unico.getMatricula().isEmpty())
                    ? unico.getMatricula()
                    : unico.getIcao24();
            String etiquetaAvion = unico.getModelo() + (identificador != null ? " (" + identificador + ")" : "");
            mensaje = getString(R.string.dialogo_borrar_uno_mensaje, etiquetaAvion);
        } else {
            mensaje = getString(R.string.dialogo_borrar_varios_mensaje, avionesParaBorrar.size());
        }

        // Muestra el diálogo de confirmación definitiva antes de eliminar
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_borrar_titulo))
                .setMessage(mensaje)
                .setPositiveButton(getString(R.string.btn_eliminar), (d, w) -> ejecutarBorradoAviones(avionesParaBorrar))
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    private void ejecutarBorradoAviones(List<Avion> avionesParaBorrar) {
        // Elimina cada avión seleccionado tanto de Firestore como de la lista local en memoria
        for (Avion avion : avionesParaBorrar) {
            borrarAvionEnFirestore(avion);
            listaAviones.remove(avion);
        }

        // Guarda la lista resultante actualizada en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        prefs.edit().putString(getClaveListaUsuario(), new Gson().toJson(listaAviones)).apply();

        // Refresca la colección visual y muestra notificación de éxito
        refrescarInterfazColeccion();
        Toast.makeText(
                this,
                avionesParaBorrar.size() == 1
                        ? getString(R.string.toast_avion_eliminado)
                        : getString(R.string.toast_aviones_eliminados),
                Toast.LENGTH_SHORT
        ).show();
    }

    public void borrarAvionEnFirestore(Avion avion) {
        // Comprueba que el usuario esté autenticado y que el avión tenga un ID válido
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || avion.getId() == null) return;

        // Borra el documento correspondiente de la subcolección del usuario en Firestore
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(auth.getCurrentUser().getUid())
                .collection("aviones")
                .document(avion.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("FIRESTORE", "Avión eliminado: " + avion.getApodo()))
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al borrar avión en Firestore", e));
    }

    // ------------------------------
    // NAVEGACIÓN Y VISTA DE DETALLE
    // ------------------------------

    private void abrirDetalle(Avion avion) {
        // Crea el Intent hacia DetalleAvionActivity enviando el ID único del ejemplar
        Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
        intent.putExtra("avion_id", avion.getId());
        startActivity(intent);
    }

    private void mostrarAvistamientos(GrupoAvion grupo) {
        // Infla el panel modal inferior para listar todos los avistamientos de un mismo modelo
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(vista);

        // Vincula el título y el RecyclerView del desplegable
        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo);
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos);

        // Muestra el nombre del modelo junto con la cantidad total de ejemplares
        txtTitulo.setText(grupo.getModelo() + " (" + grupo.getCantidad() + ")");
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Configura el adaptador de consulta de ejemplares individuales
        recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Infla la vista de cada ítem de avistamiento
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // Recupera el avión correspondiente ordenado del más reciente al más antiguo
                Avion a = grupo.getAvistamientos().get(grupo.getCantidad() - 1 - position);

                // Enlaza los textos e icono de fotografía de la fila
                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula);
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha);
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia);
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto);

                // Rellena matrícula/ICAO, fecha de captura, distancia y visibilidad del indicador de foto
                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24());
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento())));
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm()));
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE);

                // Al pulsar sobre un avistamiento concreto, abre su pantalla de detalle y cierra el panel
                holder.itemView.setOnClickListener(v -> {
                    abrirDetalle(a);
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                // Devuelve el total de avistamientos agrupados
                return grupo.getCantidad();
            }
        });

        // Muestra el BottomSheetDialog en pantalla
        dialog.show();
    }

    private void actualizarPapelera() {
        // Comprueba si existe al menos un grupo seleccionado en la cuadrícula principal
        boolean haySeleccionados = false;

        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                haySeleccionados = true;
                break;
            }
        }

        // Muestra el botón de papelera si hay selección activa o lo oculta en caso contrario
        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE);
    }
}