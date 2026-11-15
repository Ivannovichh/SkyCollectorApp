// Paquete que agrupa las clases de utilidad del proyecto
package es.medac.skycollectorapp.utils;

// Importación de Log para registrar eventos y errores de sincronización en Logcat
import android.util.Log;

// Importación de FirebaseAuth para obtener el usuario autenticado actual
import com.google.firebase.auth.FirebaseAuth;
// Importación de FirebaseUser para leer UID, nombre y foto por defecto
import com.google.firebase.auth.FirebaseUser;
// Importación de FirebaseFirestore para escribir en la colección pública "ranking"
import com.google.firebase.firestore.FirebaseFirestore;
// Importación de SetOptions para actualizar campos sin borrar los existentes
import com.google.firebase.firestore.SetOptions;

// Importación de ArrayList para construir la lista pública de aviones verificados
import java.util.ArrayList;
// Importación de HashMap para estructurar los documentos y mapas de Firestore
import java.util.HashMap;
// Importación de List para recibir la colección de aviones del usuario
import java.util.List;
// Importación de Locale para normalizar cadenas de rareza a mayúsculas
import java.util.Locale;
// Importación de Map como interfaz de pares clave-valor
import java.util.Map;

// Importación del modelo de datos Avion
import es.medac.skycollectorapp.models.Avion;

// Clase de utilidad encargada de calcular puntuaciones por rareza y sincronizar el perfil público en Firestore
public class RankingManager {

    // Etiqueta identificativa para los mensajes de depuración en Logcat
    private static final String TAG = "RankingManager";
    // Nombre de la colección pública de clasificación en Firestore
    private static final String COLECCION_RANKING = "ranking";

    // Puntos otorgados según la calidad/rareza de cada avión verificado
    public static final int PUNTOS_COMMON = 10;
    public static final int PUNTOS_RARE = 25;
    public static final int PUNTOS_EPIC = 50;
    public static final int PUNTOS_LEGENDARY = 100;
    public static final int PUNTOS_MYTHIC = 200;

    // Interfaz opcional para notificar cuando la sincronización con Firestore finaliza
    public interface OnRankingSyncListener {
        void onSincronizado(int puntosTotales, int avionesVerificados);
        void onError(Exception e);
    }

    // Devuelve los puntos correspondientes a una categoría de rareza concreta
    public static int obtenerPuntosPorRareza(String rareza) {
        if (rareza == null) return PUNTOS_COMMON;
        switch (rareza.trim().toUpperCase(Locale.ROOT)) {
            case "RARE":
            case "RARO":
                return PUNTOS_RARE;
            case "EPIC":
            case "EPICO":
                return PUNTOS_EPIC;
            case "LEGENDARY":
            case "LEGENDARIO":
                return PUNTOS_LEGENDARY;
            case "MYTHIC":
            case "MITICO":
                return PUNTOS_MYTHIC;
            case "COMMON":
            case "COMUN":
            default:
                return PUNTOS_COMMON;
        }
    }

    // Calcula la puntuación total de una lista de aviones (solo suman los verificados con foto en directo)
    public static int calcularPuntosTotales(List<Avion> listaAviones) {
        if (listaAviones == null || listaAviones.isEmpty()) return 0;
        int totalPuntos = 0;
        for (Avion avion : listaAviones) {
            if (avion != null && avion.isEsFotoVerificada()) {
                totalPuntos += obtenerPuntosPorRareza(avion.getRareza());
            }
        }
        return totalPuntos;
    }

    // Sincroniza puntos, contadores y vitrina pública de aviones del usuario actual en Firestore
    public static void sincronizarRankingUsuario(
            List<Avion> listaAviones,
            String nombrePublicoOpcional,
            String fotoPerfilUrlOpcional,
            OnRankingSyncListener listener
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onError(new IllegalStateException("Usuario no autenticado"));
            return;
        }

        String uid = user.getUid();
        int puntosTotales = 0;
        int totalVerificados = 0;
        List<Object> avionesPublicos = new ArrayList<>();

        // Recorre los aviones del usuario para sumar puntos y preparar los datos públicos
        if (listaAviones != null) {
            for (Avion avion : listaAviones) {
                if (avion == null || !avion.isEsFotoVerificada()) continue;

                int puntosAvion = obtenerPuntosPorRareza(avion.getRareza());
                puntosTotales += puntosAvion;
                totalVerificados++;

                // Construye un mapa ligero y seguro para mostrar en el perfil público
                Map<String, Object> itemPublico = new HashMap<>();
                itemPublico.put("id", avion.getId());
                itemPublico.put("modelo", avion.getModelo());
                itemPublico.put("apodo", avion.getApodo());
                itemPublico.put("fabricante", avion.getFabricante());
                itemPublico.put("rareza", avion.getRareza() != null ? avion.getRareza().toUpperCase(Locale.ROOT) : "COMMON");
                itemPublico.put("puntos", puntosAvion);
                itemPublico.put("tipoIcao", avion.getTipoIcao());
                itemPublico.put("icao24", avion.getIcao24());
                itemPublico.put("matricula", avion.getMatricula());
                itemPublico.put("fechaAvistamiento", avion.getFechaAvistamiento());
                itemPublico.put("distanciaKm", avion.getDistanciaKm());
                itemPublico.put("descripcion", avion.getDescripcion());
                itemPublico.put("urlImagenRemota", avion.getUrlImagenRemota());

                // Solo guarda la foto del usuario si es una URL remota o Base64 accesible por otros dispositivos
                String uriUsuario = avion.getUriFotoUsuario();
                if (uriUsuario != null && (uriUsuario.startsWith("http://")
                        || uriUsuario.startsWith("https://")
                        || uriUsuario.startsWith("data:image/"))) {
                    itemPublico.put("uriFotoUsuario", uriUsuario);
                }

                avionesPublicos.add(itemPublico);
            }
        }

        // Prepara el documento público del piloto en la colección "ranking"
        Map<String, Object> datosRanking = new HashMap<>();
        datosRanking.put("uid", uid);
        datosRanking.put("puntos", puntosTotales);
        datosRanking.put("totalAviones", totalVerificados);
        datosRanking.put("avionesPublicos", avionesPublicos);
        datosRanking.put("ultimaActualizacion", System.currentTimeMillis());

        // Actualiza el nombre público solo si se proporciona o si tiene displayName en Auth
        if (nombrePublicoOpcional != null && !nombrePublicoOpcional.trim().isEmpty()) {
            datosRanking.put("nombreUsuario", nombrePublicoOpcional.trim());
        } else if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            datosRanking.put("nombreUsuario", user.getDisplayName().trim());
        }

        // Actualiza la URL de foto de perfil solo si se proporciona o existe en Auth
        if (fotoPerfilUrlOpcional != null && !fotoPerfilUrlOpcional.trim().isEmpty()) {
            datosRanking.put("fotoPerfilUrl", fotoPerfilUrlOpcional.trim());
        } else if (user.getPhotoUrl() != null) {
            datosRanking.put("fotoPerfilUrl", user.getPhotoUrl().toString());
        }

        final int puntosFinales = puntosTotales;
        final int verificadosFinales = totalVerificados;

        // Guarda o fusiona los datos en Firestore bajo ranking/{uid}
        FirebaseFirestore.getInstance()
                .collection(COLECCION_RANKING)
                .document(uid)
                .set(datosRanking, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Ranking sincronizado: " + puntosFinales + " pts (" + verificadosFinales + " aviones)");
                    if (listener != null) {
                        listener.onSincronizado(puntosFinales, verificadosFinales);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al sincronizar ranking: " + e.getMessage());
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    // Actualiza únicamente los datos de identidad pública (nombre/foto) cuando el usuario edita su perfil
    public static void actualizarIdentidadPublica(String nombreUsuario, String fotoPerfilUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> identidad = new HashMap<>();
        identidad.put("uid", user.getUid());
        if (nombreUsuario != null && !nombreUsuario.trim().isEmpty()) {
            identidad.put("nombreUsuario", nombreUsuario.trim());
        }
        if (fotoPerfilUrl != null && !fotoPerfilUrl.trim().isEmpty()) {
            identidad.put("fotoPerfilUrl", fotoPerfilUrl.trim());
        }

        FirebaseFirestore.getInstance()
                .collection(COLECCION_RANKING)
                .document(user.getUid())
                .set(identidad, SetOptions.merge());
    }
}