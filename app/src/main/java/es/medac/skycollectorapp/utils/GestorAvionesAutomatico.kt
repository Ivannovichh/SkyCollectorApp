import android.util.Log
import android.widget.ImageView
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Gestor encargado de buscar y resolver de forma automática la imagen
 * de cualquier modelo de avión consultando directamente fuentes públicas en internet.
 */
object GestorAvionesAutomatico {

    /**
     * Método genérico principal que toma el nombre del modelo de avión,
     * consulta la API pública de Wikipedia en segundo plano y pinta la imagen resultante en la interfaz.
     */
    suspend fun cargarImagenDesdeInternet(nombreModelo: String, imageView: ImageView) {
        // Normaliza el texto para asegurar una búsqueda exitosa en la enciclopedia
        val consultaLimpia = nombreModelo.trim().replace(" ", "_")
        val urlImagenRemota = obtenerUrlDesdeWikipedia(consultaLimpia)

        // Cambia al hilo principal para actualizar la interfaz gráfica con Coil
        withContext(Dispatchers.Main) {
            if (!urlImagenRemota.isNullOrEmpty()) {
                imageView.load(urlImagenRemota) {
                    crossfade(true) // Transición visual elegante al cargar
                }
            } else {
                Log.w("GestorAviones", "No se encontró una imagen automática para: $nombreModelo")
                // Opcional: Cargar una imagen por defecto local si la web no devuelve nada
            }
        }
    }

    /**
     * Método interno encargado de realizar la petición HTTP a la API de MediaWiki/Wikipedia
     * para extraer el enlace directo de la fotografía oficial del modelo.
     */
    private suspend fun obtenerUrlDesdeWikipedia(terminoBusqueda: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Endpoint oficial de la API de Wikipedia para obtener la miniatura o imagen principal del artículo
                val endpoint = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=500&titles=$terminoBusqueda"
                val respuestaJson = URL(endpoint).readText()

                val jsonObject = JSONObject(respuestaJson)
                val query = jsonObject.optJSONObject("query") ?: return@withContext null
                val pages = query.optJSONObject("pages") ?: return@withContext null

                // Extrae la primera clave de página devuelta por la API
                val primeraClave = pages.keys().takeIf { it.hasNext() }?.next() ?: return@withContext null
                val pagina = pages.optJSONObject(primeraClave) ?: return@withContext null
                val thumbnail = pagina.optJSONObject("thumbnail")

                // Devuelve la URL de la imagen si existe
                thumbnail?.optString("source")
            } catch (e: Exception) {
                Log.e("GestorAviones", "Error al conectar con la API de Wikipedia: ${e.message}")
                null
            }
        }
    }
}