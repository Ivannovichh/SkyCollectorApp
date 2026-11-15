// Declaración del paquete al que pertenece la actividad principal
package es.medac.skycollectorapp.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.adapters.AvionAdapter;
import es.medac.skycollectorapp.databinding.ActivityMainBinding;
import es.medac.skycollectorapp.models.Avion;
import es.medac.skycollectorapp.models.GrupoAvion;
import es.medac.skycollectorapp.utils.RankingManager;

// Clase principal que representa la pantalla de inicio, catálogo personal, ranking público y borrado granular
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AvionAdapter adapter;
    private List<Avion> listaAviones;
    private List<GrupoAvion> grupos;
    private AlertDialog dialogoNombreActivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        listaAviones = new ArrayList<>();
        grupos = new ArrayList<>();

        adapter = new AvionAdapter(grupos,
                (grupo, position) -> {
                    if (grupo.getCantidad() == 1) abrirDetalle(grupo.getRepresentante());
                    else mostrarAvistamientos(grupo);
                },
                this::actualizarPapelera
        );

        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.btnAddAvion.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddAvionActivity.class))
        );

        binding.btnPapelera.setOnClickListener(v -> iniciarFlujoBorrado());

        binding.cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PerfilActivity.class))
        );

        binding.btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatbotActivity.class))
        );

        binding.btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapaActivity.class))
        );

        binding.btnRanking.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RankingActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        cargarListaDeAviones();
        sincronizarDesdeFirestore();
        cargarFotoPerfilMini();
        verificarYCargarNombreUsuario();
    }

    // ------------------------------
    // GESTIÓN DE NOMBRE DE USUARIO Y RANKING PÚBLICO
    // ------------------------------

    // Comprueba si el usuario dispone de un apodo configurado en local o en Firestore; si no, abre ventana emergente
    private void verificarYCargarNombreUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String uid = user.getUid();
        String nombreLocal = prefs.getString("nombre_" + uid, null);

        if (nombreLocal != null && !nombreLocal.trim().isEmpty()) {
            binding.txtNombreUsuario.setText(nombreLocal);
            RankingManager.sincronizarRankingUsuario(listaAviones, nombreLocal, null, null);
            return;
        }

        binding.txtNombreUsuario.setText(getString(R.string.piloto_provisional));

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isDestroyed() || isFinishing()) return;

                    String nombreNube = doc.getString("nombreUsuario");
                    if (nombreNube != null && !nombreNube.trim().isEmpty()) {
                        prefs.edit().putString("nombre_" + uid, nombreNube).apply();
                        binding.txtNombreUsuario.setText(nombreNube);
                        RankingManager.sincronizarRankingUsuario(listaAviones, nombreNube, null, null);
                    } else {
                        mostrarDialogoElegirNombre(user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed() && !isFinishing()) {
                        mostrarDialogoElegirNombre(user);
                    }
                });
    }

    // Despliega una ventana emergente con la estética de la app para que el usuario escoja su nombre público
    private void mostrarDialogoElegirNombre(FirebaseUser user) {
        if (dialogoNombreActivo != null && dialogoNombreActivo.isShowing()) return;

        View vista = getLayoutInflater().inflate(R.layout.dialog_elegir_nombre, null);
        EditText etNombre = vista.findViewById(R.id.etNuevoNombreUsuario);
        Button btnConfirmar = vista.findViewById(R.id.btnGuardarNombreInicial);

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            etNombre.setText(user.getDisplayName());
            etNombre.setSelection(etNombre.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(vista)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirmar.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            if (nuevoNombre.length() < 3) {
                etNombre.setError(getString(R.string.error_nombre_corto));
                etNombre.requestFocus();
                return;
            }

            String uid = user.getUid();
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
            prefs.edit().putString("nombre_" + uid, nuevoNombre).apply();

            Map<String, Object> datosUsuario = new HashMap<>();
            datosUsuario.put("nombreUsuario", nuevoNombre);
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .set(datosUsuario, SetOptions.merge());

            RankingManager.sincronizarRankingUsuario(listaAviones, nuevoNombre, null, null);

            binding.txtNombreUsuario.setText(nuevoNombre);
            dialog.dismiss();
            Toast.makeText(this, getString(R.string.toast_bienvenido, nuevoNombre), Toast.LENGTH_SHORT).show();
        });

        dialogoNombreActivo = dialog;
        dialog.show();
    }

    // ------------------------------
    // CARGA Y SINCRONIZACIÓN DE DATOS
    // ------------------------------

    private String getClaveListaUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? "lista_aviones_" + user.getUid() : "lista_aviones_anonimo";
    }

    private void cargarFotoPerfilMini() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
        String fotoGuardada = prefs.getString("foto_" + user.getUid(), null);

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
        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        String json = prefs.getString(getClaveListaUsuario(), null);

        listaAviones.clear();

        if (json != null) {
            Type type = new TypeToken<ArrayList<Avion>>() {}.getType();
            List<Avion> avionesGuardados = new Gson().fromJson(json, type);
            if (avionesGuardados != null) listaAviones.addAll(avionesGuardados);
        }

        refrescarInterfazColeccion();
    }

    private void sincronizarDesdeFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(user.getUid())
                .collection("aviones")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isDestroyed() || isFinishing()) return;

                    List<Avion> listaNube = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Avion avion = doc.toObject(Avion.class);
                        if (avion != null) listaNube.add(avion);
                    }

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
        construirGrupos();

        if (listaAviones.isEmpty()) {
            binding.txtVacio.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.txtVacio.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
        actualizarPapelera();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("PerfilUsuario", MODE_PRIVATE);
            String nombre = prefs.getString("nombre_" + user.getUid(), null);
            RankingManager.sincronizarRankingUsuario(listaAviones, nombre, null, null);
        }
    }

    private void construirGrupos() {
        grupos.clear();
        Map<String, GrupoAvion> porModelo = new LinkedHashMap<>();

        for (Avion a : listaAviones) {
            String clave = a.getModelo() != null ? a.getModelo() : "";
            GrupoAvion g = porModelo.get(clave);
            if (g == null) {
                g = new GrupoAvion(clave);
                porModelo.put(clave, g);
            }
            g.agregar(a);
        }

        grupos.addAll(porModelo.values());
    }

    // ------------------------------
    // FLUJO DE SELECCIÓN Y BORRADO GRANULAR
    // ------------------------------

    private void iniciarFlujoBorrado() {
        List<Avion> avionesParaBorrar = new ArrayList<>();
        List<GrupoAvion> gruposMultiples = new ArrayList<>();

        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                if (g.getCantidad() == 1) {
                    avionesParaBorrar.add(g.getRepresentante());
                } else {
                    gruposMultiples.add(g);
                }
            }
        }

        if (avionesParaBorrar.isEmpty() && gruposMultiples.isEmpty()) return;

        procesarSiguienteGrupoMultiple(0, gruposMultiples, avionesParaBorrar);
    }

    private void procesarSiguienteGrupoMultiple(int indice, List<GrupoAvion> gruposMultiples, List<Avion> avionesParaBorrar) {
        if (indice >= gruposMultiples.size()) {
            confirmarBorradoFinal(avionesParaBorrar);
            return;
        }

        GrupoAvion grupoActual = gruposMultiples.get(indice);
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(vista);
        dialog.setCancelable(false);

        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo);
        CheckBox cbTodos = vista.findViewById(R.id.cbSeleccionarTodos);
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos);
        LinearLayout layoutBotones = vista.findViewById(R.id.layoutBotonesBorradoGrupo);
        Button btnCancelar = vista.findViewById(R.id.btnCancelarSeleccionGrupo);
        Button btnConfirmar = vista.findViewById(R.id.btnConfirmarSeleccionGrupo);

        String progreso = gruposMultiples.size() > 1 ? " (" + (indice + 1) + "/" + gruposMultiples.size() + ")" : "";
        txtTitulo.setText(grupoActual.getModelo() + progreso);

        cbTodos.setVisibility(View.VISIBLE);
        layoutBotones.setVisibility(View.VISIBLE);
        btnConfirmar.setText(indice + 1 < gruposMultiples.size() ? getString(R.string.btn_siguiente) : getString(R.string.btn_aceptar));

        Set<Avion> seleccionadosEnGrupo = new HashSet<>();
        final boolean[] actualizandoMasivo = {false};

        recycler.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapterSeleccion = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Avion a = grupoActual.getAvistamientos().get(grupoActual.getCantidad() - 1 - position);

                CheckBox cbItem = holder.itemView.findViewById(R.id.cbSeleccionarAvistamiento);
                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula);
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha);
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia);
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto);

                cbItem.setVisibility(View.VISIBLE);
                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24());
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento())));
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm()));
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE);

                cbItem.setOnCheckedChangeListener(null);
                cbItem.setChecked(seleccionadosEnGrupo.contains(a));

                cbItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) seleccionadosEnGrupo.add(a);
                    else seleccionadosEnGrupo.remove(a);

                    actualizandoMasivo[0] = true;
                    cbTodos.setChecked(seleccionadosEnGrupo.size() == grupoActual.getCantidad());
                    actualizandoMasivo[0] = false;
                });

                holder.itemView.setOnClickListener(v -> cbItem.setChecked(!cbItem.isChecked()));
            }

            @Override
            public int getItemCount() {
                return grupoActual.getCantidad();
            }
        };

        recycler.setAdapter(adapterSeleccion);

        cbTodos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoMasivo[0]) return;
            seleccionadosEnGrupo.clear();
            if (isChecked) {
                seleccionadosEnGrupo.addAll(grupoActual.getAvistamientos());
            }
            adapterSeleccion.notifyDataSetChanged();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnConfirmar.setOnClickListener(v -> {
            avionesParaBorrar.addAll(seleccionadosEnGrupo);
            dialog.dismiss();
            procesarSiguienteGrupoMultiple(indice + 1, gruposMultiples, avionesParaBorrar);
        });

        dialog.show();
    }

    private void confirmarBorradoFinal(List<Avion> avionesParaBorrar) {
        if (avionesParaBorrar.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_ningun_avion_seleccionado), Toast.LENGTH_SHORT).show();
            return;
        }

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

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialogo_borrar_titulo))
                .setMessage(mensaje)
                .setPositiveButton(getString(R.string.btn_eliminar), (d, w) -> ejecutarBorradoAviones(avionesParaBorrar))
                .setNegativeButton(getString(R.string.dialogo_foto_cancelar), null)
                .show();
    }

    private void ejecutarBorradoAviones(List<Avion> avionesParaBorrar) {
        for (Avion avion : avionesParaBorrar) {
            borrarAvionEnFirestore(avion);
            listaAviones.remove(avion);
        }

        SharedPreferences prefs = getSharedPreferences("SkyCollectorDatos", Context.MODE_PRIVATE);
        prefs.edit().putString(getClaveListaUsuario(), new Gson().toJson(listaAviones)).apply();

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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || avion.getId() == null) return;

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
        Intent intent = new Intent(MainActivity.this, DetalleAvionActivity.class);
        intent.putExtra("avion_id", avion.getId());
        startActivity(intent);
    }

    private void mostrarAvistamientos(GrupoAvion grupo) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_grupo_avistamientos, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(vista);

        TextView txtTitulo = vista.findViewById(R.id.txtTituloGrupo);
        RecyclerView recycler = vista.findViewById(R.id.recyclerAvistamientos);

        txtTitulo.setText(grupo.getModelo() + " (" + grupo.getCantidad() + ")");
        recycler.setLayoutManager(new LinearLayoutManager(this));

        recycler.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avistamiento_grupo, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Avion a = grupo.getAvistamientos().get(grupo.getCantidad() - 1 - position);

                TextView txtMat = holder.itemView.findViewById(R.id.txtMatricula);
                TextView txtFec = holder.itemView.findViewById(R.id.txtFecha);
                TextView txtDist = holder.itemView.findViewById(R.id.txtDistancia);
                ImageView imgFoto = holder.itemView.findViewById(R.id.imgTieneFoto);

                txtMat.setText(a.getMatricula() != null && !a.getMatricula().isEmpty() ? a.getMatricula() : a.getIcao24());
                txtFec.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(a.getFechaAvistamiento())));
                txtDist.setText(String.format(Locale.getDefault(), "%.1f km", a.getDistanciaKm()));
                imgFoto.setVisibility(a.getUriFotoUsuario() != null ? View.VISIBLE : View.GONE);

                holder.itemView.setOnClickListener(v -> {
                    abrirDetalle(a);
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return grupo.getCantidad();
            }
        });

        dialog.show();
    }

    private void actualizarPapelera() {
        boolean haySeleccionados = false;

        for (GrupoAvion g : grupos) {
            if (g.isSeleccionado()) {
                haySeleccionados = true;
                break;
            }
        }

        binding.btnPapelera.setVisibility(haySeleccionados ? View.VISIBLE : View.GONE);
    }
}