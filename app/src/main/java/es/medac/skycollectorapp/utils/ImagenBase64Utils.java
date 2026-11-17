// Paquete de utilidades del proyecto
package es.medac.skycollectorapp.utils;

// Importación para acceder al ContentResolver y leer archivos desde URIs
import android.content.Context;
// Importación para manipular imágenes en mapa de bits y escalar sus dimensiones
import android.graphics.Bitmap;
// Importación para decodificar flujos de entrada en objetos Bitmap
import android.graphics.BitmapFactory;
// Importación para representar la ruta URI de la imagen seleccionada o capturada
import android.net.Uri;
// Importación para codificar arreglos de bytes en formato texto Base64
import android.util.Base64;

// Importación del flujo de salida en memoria para almacenar la imagen comprimida
import java.io.ByteArrayOutputStream;
// Importación del flujo de lectura de bytes desde el almacenamiento del dispositivo
import java.io.InputStream;

/**
 * Utilidad encargada de convertir imágenes locales (Uri) en cadenas Data-URI Base64
 * redimensionadas y comprimidas, optimizadas para guardarse en Firestore y cargarse con Glide.
 */
public class ImagenBase64Utils {

    /**
     * Redimensiona proporcionalmente una imagen desde su Uri y la comprime en formato JPEG,
     * devolviendo una cadena lista para usar con el prefijo "data:image/jpeg;base64,...".
     */
    public static String uriABase64Comprimida(Context context, Uri uri, int maxLadoPx, int calidadJpeg) {
        // Valida que el contexto y la URI recibida no sean nulos
        if (context == null || uri == null) return null;
        try {
            // Abre un flujo de lectura sobre la URI de la imagen mediante el ContentResolver
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            // Decodifica el flujo binario en un objeto Bitmap en memoria y cierra el flujo
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();
            if (original == null) return null;

            // Obtiene las dimensiones reales y calcula el factor de escala respecto al lado máximo permitido
            int ancho = original.getWidth();
            int alto = original.getHeight();
            float escala = Math.min((float) maxLadoPx / ancho, (float) maxLadoPx / alto);

            // Solo redimensiona la imagen si supera el límite máximo (evita ampliar imágenes pequeñas)
            Bitmap escalado = original;
            if (escala < 1.0f) {
                int nuevoAncho = Math.round(ancho * escala);
                int nuevoAlto = Math.round(alto * escala);
                escalado = Bitmap.createScaledBitmap(original, nuevoAncho, nuevoAlto, true);
            }

            // Comprime el Bitmap resultante en formato JPEG dentro de un buffer en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            escalado.compress(Bitmap.CompressFormat.JPEG, calidadJpeg, baos);
            byte[] bytes = baos.toByteArray();

            // Codifica los bytes a Base64 sin saltos de línea y antepone la cabecera Data-URI
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            // Devuelve null de forma controlada ante cualquier error de lectura o memoria
            return null;
        }
    }
}