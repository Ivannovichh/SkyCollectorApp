// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

// Importación para lanzar otras pantallas mediante Intents y enviar extras
import android.content.Intent;
// Importación para gestionar el ciclo de vida y creación de la actividad
import android.os.Bundle;
// Importación para inflar layouts XML en vistas dinámicas
import android.view.LayoutInflater;
// Importación de la clase base de componentes visuales
import android.view.View;
// Importación para manipular contenedores de vistas
import android.view.ViewGroup;
// Importación para casillas de verificación (ocultas en este modo)
import android.widget.CheckBox;
// Importación para botones gráficos como el de volver atrás
import android.widget.ImageButton;
// Importación para mostrar imágenes como el avatar y las fotos de aviones
import android.widget.ImageView;
// Importación para la barra de carga circular
import android.widget.ProgressBar;
// Importación para etiquetas de texto
import android.widget.TextView;
// Importación para mostrar avisos emergentes breves
import android.widget.Toast;

// Importación para indicar parámetros que no admiten valores nulos
import androidx.annotation.NonNull;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación del gestor de diseño en cuadrícula para el RecyclerView
import androidx.recyclerview.widget.GridLayoutManager;
// Importación del componente de lista reciclable
import androidx.recyclerview.widget.RecyclerView;

// Importación de Glide para cargar y recortar imágenes desde URL o Base64
import com.bumptech.glide.Glide;
// Importación de las tarjetas de Material Design
import com.google.android.material.card.MaterialCardView;
// Importación de la base de datos Cloud Firestore
import com.google.firebase.firestore.FirebaseFirestore;

// Importación de listas dinámicas
import java.util.ArrayList;
// Importación de la interfaz List
import java.util.List;
// Importación de la interfaz Map para leer los objetos guardados en Firestore
import java.util.Map;

// Importación de los recursos XML de la aplicación
import es.medac.skycollectorapp.R;
// Importación del modelo de datos Avion
import es.medac.skycollectorapp.models.Avion;
// Importación de utilidades para resolver siluetas y traducir rarezas
import es.medac.skycollectorapp.utils.AvionGenerator;

// Actividad que muestra el perfil público de un piloto y sus avistamientos verificados en modo solo lectura
public class PerfilPublicoActivity extends AppCompatActivity {

    // Imagen circular del avatar del piloto consultado
    private ImageView imgAvatar;
    // Etiqueta con el nombre público del piloto
    private TextView txtNombre;
    // Etiqueta con el puesto, puntos totales y cantidad de aviones
    private TextView txtEstadisticas;
    // Cuadrícula donde se muestran las tarjetas de aviones públicos del piloto
    private RecyclerView recyclerAviones;
    // Indicador de carga mientras se descarga el documento desde Firestore
    private ProgressBar progressBar;
    // Mensaje que se muestra si el piloto aún no tiene aviones públicos
    private TextView txtVacio;

    // Lista en memoria con los aviones reconstruidos desde el perfil público
    private final List<Avion> avionesPiloto = new ArrayList<>();
    // Adaptador encargado de pintar cada tarjeta de avión en la cuadrícula
    private AvionesPublicosAdapter adapter;
    // Posición que ocupa el piloto en la tabla del ranking (-1 si no se recibió)
    private int puestoPiloto = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Establece el diseño XML de la pantalla de perfil público
        setContentView(R.layout.activity_perfil_publico);

        // Enlaza el botón de retroceso y todas las vistas del layout XML
        ImageButton btnVolver = findViewById(R.id.btnVolverPerfilPublico);
        imgAvatar = findViewById(R.id.imgAvatarPerfilPublico);
        txtNombre = findViewById(R.id.txtNombrePerfilPublico);
        txtEstadisticas = findViewById(R.id.txtEstadisticasPerfilPublico);
        recyclerAviones = findViewById(R.id.recyclerAvionesPublicos);
        progressBar = findViewById(R.id.progressPerfilPublico);
        txtVacio = findViewById(R.id.txtHangarPublicoVacio);

        // Cierra la pantalla al pulsar la flecha de volver
        btnVolver.setOnClickListener(v -> finish());

        // Recupera el UID del piloto y su puesto desde el Intent que abrió esta pantalla
        String uidPiloto = getIntent().getStringExtra("uid_piloto");
        puestoPiloto = getIntent().getIntExtra("puesto_piloto", -1);

        // Si no se recibió un UID válido, cierra la actividad inmediatamente
        if (uidPiloto == null || uidPiloto.isEmpty()) {
            finish();
            return;
        }

        // Instancia el adaptador, configura una cuadrícula de 2 columnas y lo vincula al RecyclerView
        adapter = new AvionesPublicosAdapter();
        recyclerAviones.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerAviones.setAdapter(adapter);

        // Inicia la descarga de los datos públicos del piloto desde Firestore
        cargarPerfilPublico(uidPiloto);
    }

    // Descarga el documento público ranking/{uid} con los datos del piloto y su lista de aviones
    @SuppressWarnings("unchecked")
    private void cargarPerfilPublico(String uidPiloto) {
        // Muestra la barra de progreso mientras se realiza la consulta en la nube
        progressBar.setVisibility(View.VISIBLE);

        // Consulta el documento del piloto dentro de la colección pública "ranking"
        FirebaseFirestore.getInstance()
                .collection("ranking")
                .document(uidPiloto)
                .get()
                .addOnSuccessListener(doc -> {
                    // Aborta si la actividad ya se está cerrando o ha sido destruida
                    if (isFinishing() || isDestroyed()) return;
                    // Oculta la barra de progreso al recibir respuesta
                    progressBar.setVisibility(View.GONE);

                    // Si el documento no existe en Firestore, muestra el aviso de hangar vacío y termina
                    if (!doc.exists()) {
                        txtVacio.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Obtiene el nombre público del piloto o asigna un nombre provisional por defecto
                    String nombre = doc.getString("nombreUsuario");
                    txtNombre.setText(nombre != null && !nombre.isEmpty()
                            ? nombre
                            : getString(R.string.piloto_provisional));

                    // Si el piloto tiene foto de perfil pública, la carga recortada en círculo con Glide
                    String fotoUrl = doc.getString("fotoPerfilUrl");
                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(fotoUrl)
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .error(android.R.drawable.ic_menu_myplaces)
                                .circleCrop()
                                .into(imgAvatar);
                    }

                    // Extrae la puntuación y el número total de avistamientos del documento
                    long puntos = doc.getLong("puntos") != null ? doc.getLong("puntos") : 0L;
                    long totalAviones = doc.getLong("totalAviones") != null ? doc.getLong("totalAviones") : 0L;

                    // Formatea el texto de estadísticas incluyendo el puesto si se recibió desde el ranking
                    if (puestoPiloto > 0) {
                        txtEstadisticas.setText(getString(R.string.perfil_publico_stats_con_puesto, puestoPiloto, puntos, totalAviones));
                    } else {
                        txtEstadisticas.setText(getString(R.string.perfil_publico_stats, puntos, totalAviones));
                    }

                    // Limpia la lista previa y reconstruye cada avión desde el array "avionesPublicos"
                    avionesPiloto.clear();
                    Object rawLista = doc.get("avionesPublicos");
                    if (rawLista instanceof List) {
                        for (Object elem : (List<Object>) rawLista) {
                            if (elem instanceof Map) {
                                avionesPiloto.add(mapearAvionDesdeFirestore((Map<String, Object>) elem));
                            }
                        }
                    }

                    // Muestra el mensaje de lista vacía o notifica al adaptador para pintar las tarjetas
                    if (avionesPiloto.isEmpty()) {
                        txtVacio.setVisibility(View.VISIBLE);
                    } else {
                        txtVacio.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    // Si la pantalla sigue activa ante un fallo de red, oculta el spinner y avisa con un Toast
                    if (isFinishing() || isDestroyed()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_error_cargar_perfil_publico), Toast.LENGTH_SHORT).show();
                });
    }

    // Reconstruye un objeto Avion a partir de cada mapa guardado en avionesPublicos
    private Avion mapearAvionDesdeFirestore(Map<String, Object> map) {
        // Crea una nueva instancia vacía de Avion y rellena sus campos de texto de forma segura
        Avion a = new Avion();
        a.setId(obtenerString(map, "id"));
        a.setModelo(obtenerString(map, "modelo"));
        a.setApodo(obtenerString(map, "apodo"));
        a.setFabricante(obtenerString(map, "fabricante"));
        a.setRareza(obtenerString(map, "rareza"));
        a.setTipoIcao(obtenerString(map, "tipoIcao"));
        a.setIcao24(obtenerString(map, "icao24"));
        a.setMatricula(obtenerString(map, "matricula"));
        a.setDescripcion(obtenerString(map, "descripcion"));
        a.setUrlImagenRemota(obtenerString(map, "urlImagenRemota"));
        a.setUriFotoUsuario(obtenerString(map, "uriFotoUsuario"));

        // Convierte el valor numérico de distancia a float si está presente en el mapa
        Object distObj = map.get("distanciaKm");
        if (distObj instanceof Number) {
            a.setDistanciaKm(((Number) distObj).floatValue());
        }

        // Convierte la marca de tiempo de avistamiento a long si está presente en el mapa
        Object fechaObj = map.get("fechaAvistamiento");
        if (fechaObj instanceof Number) {
            a.setFechaAvistamiento(((Number) fechaObj).longValue());
        }

        // Resuelve la silueta por tipo ICAO o nombre del modelo (con sil_jet como respaldo seguro)
        a.setImagenResId(AvionGenerator.resolverImagenPorModelo(a.getModelo(), a.getTipoIcao()));

        // Devuelve el objeto Avion completamente reconstruido
        return a;
    }

    // Extrae un valor de texto de forma segura desde el mapa de Firestore
    private String obtenerString(Map<String, Object> map, String clave) {
        // Obtiene el objeto asociado a la clave y lo convierte a String si no es nulo
        Object val = map.get(clave);
        return val != null ? String.valueOf(val) : null;
    }

    // Abre la ficha grande del avión en modo solo lectura (sin el botón inferior de avistar)
    private void abrirFichaSoloLectura(Avion avion) {
        // Prepara el Intent hacia DetalleVueloRadarActivity enviando todos los datos del ejemplar
        Intent intent = new Intent(this, DetalleVueloRadarActivity.class);
        // Activa la bandera de solo lectura para ocultar acciones de captura
        intent.putExtra("modo_solo_lectura", true);
        intent.putExtra("icao", avion.getIcao24());
        intent.putExtra("tipo", avion.getTipoIcao());
        intent.putExtra("matricula", avion.getMatricula());
        intent.putExtra("modelo", avion.getModelo());
        intent.putExtra("empresa", avion.getFabricante());
        intent.putExtra("distancia_km", avion.getDistanciaKm());
        intent.putExtra("fecha_avistamiento", avion.getFechaAvistamiento());
        // Prioriza la foto subida por el usuario y usa la remota como respaldo
        intent.putExtra("foto_avistamiento_url", avion.getUriFotoUsuario() != null ? avion.getUriFotoUsuario() : avion.getUrlImagenRemota());
        // Lanza la pantalla de detalle
        startActivity(intent);
    }

    // Adaptador en cuadrícula reutilizando item_avion.xml sin casilla de borrado
    private class AvionesPublicosAdapter extends RecyclerView.Adapter<AvionesPublicosAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Infla el diseño XML de la tarjeta de avión para cada celda de la cuadrícula
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avion, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Obtiene el avión correspondiente a la posición actual
            Avion avion = avionesPiloto.get(position);

            // Oculta la casilla de selección y el contador de grupo porque se muestran ejemplares individuales
            holder.chkSeleccion.setVisibility(View.GONE);
            holder.txtCantidad.setVisibility(View.GONE);

            // Rellena modelo, fabricante, rareza traducida y color de borde según la rareza
            holder.txtModelo.setText(avion.getModelo());
            holder.txtFabricante.setText(avion.getFabricante());
            holder.txtRareza.setText(AvionGenerator.traducirRareza(PerfilPublicoActivity.this, avion.getRareza()));
            holder.card.setStrokeColor(avion.getColorRareza());

            // Determina la silueta de respaldo y la fuente de imagen disponible (foto usuario o remota)
            int silueta = avion.getImagenResId() != 0 ? avion.getImagenResId() : R.drawable.sil_jet;
            String fuenteFoto = (avion.getUriFotoUsuario() != null && !avion.getUriFotoUsuario().isEmpty())
                    ? avion.getUriFotoUsuario()
                    : avion.getUrlImagenRemota();

            // Si existe URL o Base64 de fotografía la carga con Glide; si no, pinta la silueta vectorial
            if (fuenteFoto != null && !fuenteFoto.isEmpty()) {
                Glide.with(PerfilPublicoActivity.this)
                        .load(fuenteFoto)
                        .placeholder(silueta)
                        .error(silueta)
                        .centerCrop()
                        .into(holder.imgAvion);
            } else {
                holder.imgAvion.setImageResource(silueta);
            }

            // Abre la ficha detallada en modo solo lectura al pulsar sobre la tarjeta
            holder.itemView.setOnClickListener(v -> abrirFichaSoloLectura(avion));
        }

        @Override
        public int getItemCount() {
            // Devuelve el número total de aviones públicos en la lista
            return avionesPiloto.size();
        }

        // Contenedor de vistas para cada tarjeta de aeronave en el perfil público
        class ViewHolder extends RecyclerView.ViewHolder {
            // Referencias a los componentes visuales de item_avion.xml
            final MaterialCardView card;
            final ImageView imgAvion;
            final TextView txtRareza;
            final CheckBox chkSeleccion;
            final TextView txtCantidad;
            final TextView txtModelo;
            final TextView txtFabricante;

            ViewHolder(@NonNull View itemView) {
                // Llama al constructor base y enlaza cada vista por su ID
                super(itemView);
                card = itemView.findViewById(R.id.cardContainer);
                imgAvion = itemView.findViewById(R.id.imgAvion);
                txtRareza = itemView.findViewById(R.id.txtRareza);
                chkSeleccion = itemView.findViewById(R.id.chkSeleccion);
                txtCantidad = itemView.findViewById(R.id.txtCantidad);
                txtModelo = itemView.findViewById(R.id.txtModelo);
                txtFabricante = itemView.findViewById(R.id.txtFabricante);
            }
        }
    }
}