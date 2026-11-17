// Paquete que contiene las actividades de la aplicación
package es.medac.skycollectorapp.activities;

// Importación para abrir el perfil público de un piloto mediante Intents
import android.content.Intent;
// Importación para asignar colores hexadecimales al podio (oro, plata, bronce) y resaltados
import android.graphics.Color;
// Importación para gestionar el ciclo de vida y creación de la actividad
import android.os.Bundle;
// Importación para inflar los layouts XML de cada fila de la clasificación
import android.view.LayoutInflater;
// Importación de la clase base de componentes visuales
import android.view.View;
// Importación para manipular contenedores de vistas
import android.view.ViewGroup;
// Importación para botones gráficos como el de volver atrás
import android.widget.ImageButton;
// Importación para mostrar los avatares circulares de los pilotos
import android.widget.ImageView;
// Importación para el indicador de carga circular
import android.widget.ProgressBar;
// Importación para etiquetas de texto
import android.widget.TextView;
// Importación para mostrar notificaciones emergentes breves
import android.widget.Toast;

// Importación para indicar parámetros que no admiten valores nulos
import androidx.annotation.NonNull;
// Importación de la clase base compatible para actividades
import androidx.appcompat.app.AppCompatActivity;
// Importación del gestor de diseño lineal vertical para la lista de clasificación
import androidx.recyclerview.widget.LinearLayoutManager;
// Importación del componente de lista reciclable
import androidx.recyclerview.widget.RecyclerView;

// Importación de Glide para cargar y recortar en círculo los avatares desde URL o Base64
import com.bumptech.glide.Glide;
// Importación de las tarjetas de Material Design
import com.google.android.material.card.MaterialCardView;
// Importación para obtener el UID del usuario actualmente autenticado
import com.google.firebase.auth.FirebaseAuth;
// Importación para especificar consultas de conteo agregadas en servidor
import com.google.firebase.firestore.AggregateSource;
// Importación para leer cada documento recuperado de Firestore
import com.google.firebase.firestore.DocumentSnapshot;
// Importación de la base de datos Cloud Firestore
import com.google.firebase.firestore.FirebaseFirestore;
// Importación para ordenar los resultados de forma descendente por puntuación
import com.google.firebase.firestore.Query;

// Importación de listas dinámicas
import java.util.ArrayList;
// Importación de la interfaz List
import java.util.List;

// Importación de los recursos XML del proyecto
import es.medac.skycollectorapp.R;

/**
 * Actividad que muestra la clasificación global de pilotos con puntuación positiva (> 0)
 * e incluye una barra fija inferior con la posición exacta del usuario autenticado.
 */
public class RankingActivity extends AppCompatActivity {

    /**
     * Estructura interna que representa cada fila de la clasificación.
     */
    private static class EntradaRanking {
        // Identificador único del piloto en Firebase
        final String uid;
        // Nombre público elegido por el piloto
        final String nombreUsuario;
        // URL o cadena Base64 de su avatar público
        final String fotoPerfilUrl;
        // Puntuación total acumulada en el ranking
        final long puntos;
        // Número total de aeronaves avistadas por el piloto
        final long totalAviones;

        EntradaRanking(String uid, String nombreUsuario, String fotoPerfilUrl, long puntos, long totalAviones) {
            // Asigna todos los campos inmutables de la entrada del ranking
            this.uid = uid;
            this.nombreUsuario = nombreUsuario;
            this.fotoPerfilUrl = fotoPerfilUrl;
            this.puntos = puntos;
            this.totalAviones = totalAviones;
        }
    }

    // Lista reciclable principal donde se muestra el Top 50 de pilotos
    private RecyclerView recyclerRanking;
    // Indicador de progreso mientras se consulta Firestore
    private ProgressBar progressRanking;
    // Mensaje de aviso si aún no hay pilotos con puntos en la clasificación
    private TextView txtRankingVacio;

    // Vistas de la tarjeta inferior fija del usuario actual
    private MaterialCardView cardMiPosicion;
    // Etiqueta con el puesto global del usuario actual (#1, #52, etc.)
    private TextView txtMiPosicion;
    // Imagen circular del avatar del usuario actual
    private ImageView imgMiAvatar;
    // Etiqueta con el nombre del usuario actual
    private TextView txtMiNombre;
    // Etiqueta con el total de aviones avistados por el usuario actual
    private TextView txtMisAvistados;
    // Etiqueta con los puntos totales del usuario actual
    private TextView txtMisPuntos;

    // Lista en memoria con las entradas descargadas del Top 50
    private final List<EntradaRanking> listaRanking = new ArrayList<>();
    // Adaptador encargado de renderizar cada fila del ranking
    private RankingAdapter adapter;
    // UID del usuario autenticado en la sesión actual
    private String uidActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Llama a la inicialización base de AppCompatActivity
        super.onCreate(savedInstanceState);
        // Establece el diseño XML de la pantalla de ranking
        setContentView(R.layout.activity_ranking);

        // Obtiene el UID del usuario actual o cadena vacía si no hubiera sesión
        uidActual = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        // Enlaza el botón de volver y las vistas de la lista principal
        ImageButton btnVolver = findViewById(R.id.btnVolverRanking);
        recyclerRanking = findViewById(R.id.recyclerRanking);
        progressRanking = findViewById(R.id.progressRanking);
        txtRankingVacio = findViewById(R.id.txtRankingVacio);

        // Enlaza los componentes de la tarjeta fija inferior del propio usuario
        cardMiPosicion = findViewById(R.id.cardMiPosicionRanking);
        txtMiPosicion = findViewById(R.id.txtMiPosicionRanking);
        imgMiAvatar = findViewById(R.id.imgMiAvatarRanking);
        txtMiNombre = findViewById(R.id.txtMiNombreRanking);
        txtMisAvistados = findViewById(R.id.txtMisAvistadosRanking);
        txtMisPuntos = findViewById(R.id.txtMisPuntosRanking);

        // Cierra la actividad al pulsar el botón de retroceso
        btnVolver.setOnClickListener(v -> finish());

        // Inicializa el adaptador, asigna el LayoutManager vertical y lo vincula al RecyclerView
        adapter = new RankingAdapter();
        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        recyclerRanking.setAdapter(adapter);

        // Inicia la descarga de la clasificación desde Cloud Firestore
        cargarClasificacion();
    }

    /**
     * Consulta en Firestore los 50 mejores pilotos con puntuación mayor que 0
     * y resuelve la posición exacta del usuario actual en la barra inferior.
     */
    private void cargarClasificacion() {
        // Muestra la barra de carga y oculta el aviso de lista vacía
        progressRanking.setVisibility(View.VISIBLE);
        txtRankingVacio.setVisibility(View.GONE);

        // Consulta los primeros 50 documentos de "ranking" con puntos > 0 ordenados de mayor a menor
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ranking")
                .whereGreaterThan("puntos", 0L)
                .orderBy("puntos", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    // Aborta si la actividad ya se está cerrando o destruyendo
                    if (isFinishing() || isDestroyed()) return;
                    // Oculta el indicador de carga y limpia los resultados anteriores
                    progressRanking.setVisibility(View.GONE);
                    listaRanking.clear();

                    // Variables para detectar si el propio usuario figura dentro del Top 50
                    int puestoEncontradoEnTop = -1;
                    EntradaRanking entradaPropiaEnTop = null;

                    // Recorre cada documento devuelto por la consulta
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String uid = doc.getId();
                        // Recupera el apodo público o asigna un texto provisional si está vacío
                        String nombre = doc.getString("nombreUsuario");
                        if (nombre == null || nombre.trim().isEmpty()) {
                            nombre = getString(R.string.piloto_provisional);
                        }
                        String foto = doc.getString("fotoPerfilUrl");
                        Long puntosObj = doc.getLong("puntos");
                        Long totalObj = doc.getLong("totalAviones");

                        long puntos = puntosObj != null ? puntosObj : 0L;
                        long total = totalObj != null ? totalObj : 0L;

                        // Filtro defensivo extra para asegurar que no se muestren usuarios con 0 puntos
                        if (puntos <= 0L) continue;

                        // Añade la entrada a la lista ordenada
                        EntradaRanking entrada = new EntradaRanking(uid, nombre, foto, puntos, total);
                        listaRanking.add(entrada);

                        // Si el documento corresponde al usuario actual, guarda su puesto y datos
                        if (!uidActual.isEmpty() && uid.equals(uidActual)) {
                            puestoEncontradoEnTop = listaRanking.size();
                            entradaPropiaEnTop = entrada;
                        }
                    }

                    // Muestra el mensaje de ranking vacío o refresca la lista en pantalla
                    if (listaRanking.isEmpty()) {
                        txtRankingVacio.setVisibility(View.VISIBLE);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    // Actualiza la tarjeta inferior con la posición del usuario (dentro o fuera del Top 50)
                    if (!uidActual.isEmpty()) {
                        if (entradaPropiaEnTop != null) {
                            mostrarTarjetaMiPosicion(puestoEncontradoEnTop, entradaPropiaEnTop);
                        } else {
                            cargarMiPosicionFueraDelTop(db);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Si ocurre un error de red, oculta el spinner y muestra un aviso Toast
                    if (isFinishing() || isDestroyed()) return;
                    progressRanking.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.toast_error_cargar_ranking), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Consulta los datos del usuario actual cuando no figura en el Top 50 y calcula su puesto global
     * contando cuántos pilotos tienen estrictamente más puntos que él en Firestore.
     */
    private void cargarMiPosicionFueraDelTop(FirebaseFirestore db) {
        // Lee directamente el documento del usuario actual en la colección "ranking"
        db.collection("ranking").document(uidActual).get()
                .addOnSuccessListener(doc -> {
                    // Aborta si la pantalla cerró o el documento aún no existe
                    if (isFinishing() || isDestroyed() || !doc.exists()) return;

                    // Extrae nombre, avatar, puntos y total de aviones del documento del usuario
                    String nombre = doc.getString("nombreUsuario");
                    if (nombre == null || nombre.trim().isEmpty()) {
                        nombre = getString(R.string.piloto_provisional);
                    }
                    String foto = doc.getString("fotoPerfilUrl");
                    Long puntosObj = doc.getLong("puntos");
                    Long totalObj = doc.getLong("totalAviones");

                    long misPuntos = puntosObj != null ? puntosObj : 0L;
                    long misAviones = totalObj != null ? totalObj : 0L;
                    EntradaRanking miEntrada = new EntradaRanking(uidActual, nombre, foto, misPuntos, misAviones);

                    // Si el usuario tiene 0 puntos, muestra la tarjeta sin número de puesto (#—)
                    if (misPuntos <= 0L) {
                        mostrarTarjetaMiPosicion(-1, miEntrada);
                        return;
                    }

                    // Ejecuta una consulta agregada count() en el servidor para contar cuántos jugadores le superan
                    db.collection("ranking")
                            .whereGreaterThan("puntos", misPuntos)
                            .count()
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(aggregate -> {
                                // Calcula el puesto exacto sumando 1 al número de pilotos por encima
                                if (isFinishing() || isDestroyed()) return;
                                int puestoGlobal = (int) aggregate.getCount() + 1;
                                mostrarTarjetaMiPosicion(puestoGlobal, miEntrada);
                            })
                            .addOnFailureListener(e -> {
                                // Si falla el conteo agregado, muestra los datos sin número de puesto
                                if (isFinishing() || isDestroyed()) return;
                                mostrarTarjetaMiPosicion(-1, miEntrada);
                            });
                });
    }

    /**
     * Rellena y muestra la barra inferior fija con el puesto, avatar y puntos del usuario actual.
     */
    private void mostrarTarjetaMiPosicion(int puesto, EntradaRanking miEntrada) {
        // Hace visible la tarjeta fija inferior
        cardMiPosicion.setVisibility(View.VISIBLE);

        // Formatea el número de puesto y aplica el color del podio o cian estándar
        if (puesto > 0) {
            txtMiPosicion.setText("#" + puesto);
            if (puesto == 1) txtMiPosicion.setTextColor(Color.parseColor("#FFD700"));
            else if (puesto == 2) txtMiPosicion.setTextColor(Color.parseColor("#E0E0E0"));
            else if (puesto == 3) txtMiPosicion.setTextColor(Color.parseColor("#CD7F32"));
            else txtMiPosicion.setTextColor(Color.parseColor("#00E5FF"));
        } else {
            txtMiPosicion.setText("#—");
            txtMiPosicion.setTextColor(Color.parseColor("#90A4AE"));
        }

        // Muestra el nombre formateado como "(Tú)" y la puntuación acumulada
        txtMiNombre.setText(getString(R.string.ranking_tu_usuario_formato, miEntrada.nombreUsuario));
        txtMisPuntos.setText(getString(R.string.ranking_puntos_formato, miEntrada.puntos));

        // Muestra el aviso para puntuar si tiene 0 puntos o el contador de aviones avistados
        if (miEntrada.puntos <= 0L) {
            txtMisAvistados.setText(getString(R.string.ranking_sin_puntos_aviso));
        } else {
            txtMisAvistados.setText(getString(R.string.ranking_avistados_formato, miEntrada.totalAviones));
        }

        // Carga el avatar del usuario recortado en círculo o un icono por defecto si no tiene
        if (miEntrada.fotoPerfilUrl != null && !miEntrada.fotoPerfilUrl.isEmpty()) {
            Glide.with(this)
                    .load(miEntrada.fotoPerfilUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(imgMiAvatar);
        } else {
            imgMiAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // Permite abrir el propio perfil público al pulsar sobre la barra inferior
        cardMiPosicion.setOnClickListener(v -> {
            Intent intent = new Intent(RankingActivity.this, PerfilPublicoActivity.class);
            intent.putExtra("uid_piloto", miEntrada.uid);
            intent.putExtra("puesto_piloto", puesto > 0 ? puesto : 0);
            startActivity(intent);
        });
    }

    /**
     * Adaptador del RecyclerView para pintar cada puesto y abrir el perfil público al pulsar.
     */
    private class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Infla el diseño XML de cada tarjeta de fila en la tabla de clasificación
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ranking, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // Obtiene la entrada correspondiente y calcula su número de puesto (base 1)
            EntradaRanking item = listaRanking.get(position);
            int puesto = position + 1;

            // Rellena posición, nombre, cantidad de avistamientos y puntos totales
            holder.txtPosicion.setText("#" + puesto);
            holder.txtNombre.setText(item.nombreUsuario);
            holder.txtAvistados.setText(getString(R.string.ranking_avistados_formato, item.totalAviones));
            holder.txtPuntos.setText(getString(R.string.ranking_puntos_formato, item.puntos));

            // Colores especiales para el podio y resaltado si es el propio usuario
            if (puesto == 1) {
                holder.txtPosicion.setTextColor(Color.parseColor("#FFD700")); // Oro
            } else if (puesto == 2) {
                holder.txtPosicion.setTextColor(Color.parseColor("#E0E0E0")); // Plata
            } else if (puesto == 3) {
                holder.txtPosicion.setTextColor(Color.parseColor("#CD7F32")); // Bronce
            } else {
                holder.txtPosicion.setTextColor(Color.parseColor("#00E5FF"));
            }

            // Destaca el borde de la tarjeta con azul cian más grueso si la fila pertenece al usuario actual
            if (item.uid.equals(uidActual)) {
                holder.card.setStrokeColor(Color.parseColor("#00E5FF"));
                holder.card.setStrokeWidth(4);
            } else {
                holder.card.setStrokeColor(Color.parseColor("#26324B"));
                holder.card.setStrokeWidth(2);
            }

            // Carga el avatar del piloto recortado en círculo o muestra el icono genérico
            if (item.fotoPerfilUrl != null && !item.fotoPerfilUrl.isEmpty()) {
                Glide.with(RankingActivity.this)
                        .load(item.fotoPerfilUrl)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .error(android.R.drawable.ic_menu_myplaces)
                        .circleCrop()
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
            }

            // Al tocar cualquier fila se abre el perfil público de ese piloto
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(RankingActivity.this, PerfilPublicoActivity.class);
                intent.putExtra("uid_piloto", item.uid);
                intent.putExtra("puesto_piloto", puesto);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            // Devuelve el número total de pilotos cargados en la lista
            return listaRanking.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            // Referencias a los componentes visuales de item_ranking.xml
            final MaterialCardView card;
            final TextView txtPosicion;
            final ImageView imgAvatar;
            final TextView txtNombre;
            final TextView txtAvistados;
            final TextView txtPuntos;

            ViewHolder(@NonNull View itemView) {
                // Llama al constructor padre y enlaza las vistas de la fila por su ID
                super(itemView);
                card = itemView.findViewById(R.id.cardItemRanking);
                txtPosicion = itemView.findViewById(R.id.txtPosicionRanking);
                imgAvatar = itemView.findViewById(R.id.imgAvatarRanking);
                txtNombre = itemView.findViewById(R.id.txtNombreRanking);
                txtAvistados = itemView.findViewById(R.id.txtAvistadosRanking);
                txtPuntos = itemView.findViewById(R.id.txtPuntosRanking);
            }
        }
    }
}