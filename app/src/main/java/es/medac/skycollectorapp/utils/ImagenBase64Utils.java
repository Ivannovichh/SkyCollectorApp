// Paquete de utilidades del proyecto
package es.medac.skycollectorapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

// Utilidad para convertir imágenes locales (Uri) a Data-URI Base64 ligera compatible con Firestore y Glide
public class ImagenBase64Utils {

    // Redimensiona y comprime una imagen desde su Uri devolviendo un String "data:image/jpeg;base64,..."
    public static String uriABase64Comprimida(Context context, Uri uri, int maxLadoPx, int calidadJpeg) {
        if (context == null || uri == null) return null;
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();
            if (original == null) return null;

            int ancho = original.getWidth();
            int alto = original.getHeight();
            float escala = Math.min((float) maxLadoPx / ancho, (float) maxLadoPx / alto);

            Bitmap escalado = original;
            if (escala < 1.0f) {
                int nuevoAncho = Math.round(ancho * escala);
                int nuevoAlto = Math.round(alto * escala);
                escalado = Bitmap.createScaledBitmap(original, nuevoAncho, nuevoAlto, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            escalado.compress(Bitmap.CompressFormat.JPEG, calidadJpeg, baos);
            byte[] bytes = baos.toByteArray();

            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}