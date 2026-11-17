// Paquete que agrupa las clases de utilidad del proyecto
package es.medac.skycollectorapp.utils;

// Importación para acceder al contexto de aplicación, recursos y preferencias locales
import android.content.Context;
// Importación para almacenar en disco las cachés persistentes de Wikipedia y códigos ICAO
import android.content.SharedPreferences;
// Importación para programar el envío de resultados hacia el hilo principal de interfaz
import android.os.Handler;
// Importación para asociar el Handler al Looper principal de Android
import android.os.Looper;
// Importación para registrar trazas de depuración y advertencias controladas
import android.util.Log;

// Importación para consultar el idioma activo configurado dentro de la aplicación
import androidx.appcompat.app.AppCompatDelegate;
// Importación para leer de forma compatible el versionCode de la aplicación instalada
import androidx.core.content.pm.PackageInfoCompat;
// Importación para manejar la lista de locales activos en AppCompat
import androidx.core.os.LocaleListCompat;

// Importación para parsear arreglos JSON de las respuestas de APIs aeronáuticas
import org.json.JSONArray;
// Importación para parsear objetos JSON de las respuestas de APIs aeronáuticas y Wikipedia
import org.json.JSONObject;

// Importación para leer flujos de texto de respuestas HTTP de manera eficiente
import java.io.BufferedReader;
// Importación para convertir flujos de bytes de red en caracteres legibles
import java.io.InputStreamReader;
// Importación para abrir y configurar peticiones HTTP/HTTPS nativas
import java.net.HttpURLConnection;
// Importación para construir direcciones URL de consulta
import java.net.URL;
// Importación para codificar parámetros de consulta en formato URL seguro
import java.net.URLEncoder;
// Importación para especificar la codificación estándar UTF-8
import java.nio.charset.StandardCharsets;
// Importación de listas dinámicas
import java.util.ArrayList;
// Importación de mapas hash estándar
import java.util.HashMap;
// Importación de la interfaz List
import java.util.List;
// Importación para normalización de cadenas independiente de la configuración regional
import java.util.Locale;
// Importación de la interfaz Map
import java.util.Map;
// Importación de mapas concurrentes seguros para acceso multihilo en caché de memoria
import java.util.concurrent.ConcurrentHashMap;
// Importación del gestor de grupos de hilos de ejecución en segundo plano
import java.util.concurrent.ExecutorService;
// Importación de la fábrica de pools de hilos cacheados
import java.util.concurrent.Executors;

// Importación de los recursos XML (cadenas localizadas y drawables) del proyecto
import es.medac.skycollectorapp.R;
// Importación del modelo de datos principal de aeronave
import es.medac.skycollectorapp.models.Avion;

/**
 * Clase encargada de gestionar el catálogo técnico local de aeronaves usando siluetas
 * categorizadas, detectar Easter Eggs terrestres diferenciados (Torre de Control vs Vehículo de Pista)
 * y enriquecer de forma asíncrona descripciones e imágenes mediante ADSBDB, HexDB, adsb.lol,
 * OpenSky, Planespotters y Wikipedia.
 * Incluye sistema de caché autogestionado por versión de app y caducidad temporal (TTL).
 */
public class AvionGenerator {

    // Etiqueta de registro para trazas Logcat
    private static final String TAG = "AvionGenerator";
    // Nombre del archivo SharedPreferences para la caché persistente de artículos de Wikipedia
    private static final String PREFS_WIKI_CACHE = "skycollector_wiki_cache";
    // Nombre del archivo SharedPreferences para la caché persistente de resoluciones hex/matrícula
    private static final String PREFS_HEX_CACHE = "skycollector_hex_cache";
    // Clave reservada en preferencias para detectar actualizaciones de versión de la app
    private static final String KEY_APP_VERSION = "_cached_app_version_";
    // Delimitador interno para serializar descripción + URL de imagen en una sola cadena
    private static final String SEPARADOR_CACHE = "|||IMG|||";
    // Delimitador interno para anteponer la marca de tiempo de caducidad (TTL)
    private static final String SEPARADOR_TTL = "|||TS|||";
    // Tiempo máximo de vigencia en caché: 7 días expresados en milisegundos
    private static final long CACHE_TTL_MS = 7L * 24L * 60L * 60L * 1000L;

    // Cabecera identificativa enviada en todas las peticiones HTTP según políticas de Wikimedia/APIs
    public static final String USER_AGENT = "SkyCollectorApp/1.0 (https://github.com/Ivannovichh/SkyCollectorApp; contacto@skycollector.app)";

    // Fotos reales libres de Wikimedia Commons para cada Easter Egg terrestre
    public static final String URL_FOTO_EASTER_EGG_VEHICULO =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Follow-me_car_at_Frankfurt_Airport.jpg/800px-Follow-me_car_at_Frankfurt_Airport.jpg";
    public static final String URL_FOTO_EASTER_EGG_TORRE =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Air_traffic_control_tower_at_Frankfurt_Airport.jpg/800px-Air_traffic_control_tower_at_Frankfurt_Airport.jpg";

    // Pool de hilos elástico para ejecutar consultas de red sin bloquear la interfaz
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newCachedThreadPool();
    // Handler vinculado al hilo principal para entregar callbacks seguros a la UI
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    // Caché rápida en RAM para resultados ya resueltos de Wikipedia
    private static final ConcurrentHashMap<String, String> CACHE_MEMORIA_WIKI = new ConcurrentHashMap<>();
    // Caché rápida en RAM para tipos ICAO resueltos a partir de hex o matrícula
    private static final ConcurrentHashMap<String, String> CACHE_MEMORIA_HEX = new ConcurrentHashMap<>();

    /**
     * Interfaz de callback para devolver la aeronave resuelta al hilo principal sin bloquear la UI.
     */
    public interface OnAvionResueltoListener {
        void onAvionResuelto(Avion avion);
    }

    /**
     * Interfaz de callback ligero para actualizar el tipo ICAO y modelo en la tarjeta del radar.
     */
    public interface OnTipoIcaoResueltoListener {
        void onTipoIcaoResuelto(String tipoIcao, String modeloCatalogo);
    }

    /**
     * Estructura interna inmutable con los datos técnicos verificados de cada aeronave del catálogo.
     */
    private static class AvionData {
        // Campos inmutables con la ficha técnica predefinida en el catálogo maestro
        final String modelo, fabricante, rareza, vel, cap, dim, pais, peso, codigoIcaoPrincipal;
        // Identificador del recurso vectorial de silueta asociado
        final int siluetaId;

        /**
         * Construye una entrada técnica verificada para el catálogo estático.
         */
        AvionData(String m, String f, String r, int s, String v, String c, String d, String p, String pe, String icaoPrincipal) {
            // Asigna todos los valores técnicos de la aeronave
            modelo = m;
            fabricante = f;
            rareza = r;
            siluetaId = s;
            vel = v;
            cap = c;
            dim = d;
            pais = p;
            peso = pe;
            codigoIcaoPrincipal = icaoPrincipal;
        }
    }

    // Identificadores únicos de siluetas genéricas por categoría
    private static final int SIL_JET = R.drawable.sil_jet;
    private static final int SIL_TURBOHELICE = R.drawable.sil_turbohelice;
    private static final int SIL_LIGERO = R.drawable.sil_ligero;
    private static final int SIL_HELICOPTERO = R.drawable.sil_helicoptero;
    private static final int SIL_CAZA = R.drawable.sil_caza;
    private static final int SIL_CARGA = R.drawable.sil_carga;
    private static final int SIL_HISTORICO = R.drawable.sil_historico;

    // Lista ordenada con todas las aeronaves registradas en el catálogo maestro
    private static final List<AvionData> DATOS_AVIONES = new ArrayList<>();
    // Índice rápido O(1) para localizar una aeronave por cualquier código de tipo ICAO
    private static final Map<String, AvionData> MAPA_POR_ICAO = new HashMap<>();
    // Índice rápido O(1) para localizar una aeronave por su nombre comercial normalizado
    private static final Map<String, AvionData> MAPA_POR_NOMBRE_NORMALIZADO = new HashMap<>();

    // Bloque estático que puebla todo el catálogo maestro local al cargar la clase en memoria
    static {
        // --- EASTER EGGS ---
        registrar("Airport Operations Vehicle", "Airport", "EASTER_EGG", SIL_CARGA, "60 km/h", "Operations", "5m", "Global", "2t", "SERV", "GND", "OPS", "VEH");
        registrar("Air Traffic Control Tower", "Air Traffic Control", "EASTER_EGG", SIL_CARGA, "0 km/h", "ATC", "45m", "Global", "—", "TWR");

        // --- AERONAVES PRINCIPALES (COMMON) ---
        registrar("Boeing 737-800", "Boeing", "COMMON", SIL_JET, "842 km/h", "189 pax", "36m", "EE.UU.", "79t", "B738");
        registrar("Airbus A320", "Airbus", "COMMON", SIL_JET, "828 km/h", "180 pax", "36m", "Europa", "78t", "A320");
        registrar("Cessna 172 Skyhawk", "Cessna", "COMMON", SIL_LIGERO, "226 km/h", "4 pax", "11m", "EE.UU.", "1.1t", "C172", "C72R");
        registrar("Piper PA-28", "Piper", "COMMON", SIL_LIGERO, "217 km/h", "4 pax", "10m", "EE.UU.", "1.1t", "P28A", "P28B", "P28R", "P28S", "P28T", "P28U");
        registrar("ATR 72", "ATR", "COMMON", SIL_TURBOHELICE, "510 km/h", "78 pax", "27m", "Francia/Italia", "23t", "AT72", "AT73", "AT75", "AT76");
        registrar("Bombardier CRJ-900", "Bombardier", "COMMON", SIL_JET, "871 km/h", "90 pax", "25m", "Canadá", "38t", "CRJ7", "CRJ9", "CRJX");
        registrar("Robinson R44", "Robinson", "COMMON", SIL_HELICOPTERO, "240 km/h", "4 pax", "10m", "EE.UU.", "1.1t", "R44");
        registrar("Embraer E190", "Embraer", "COMMON", SIL_JET, "871 km/h", "114 pax", "29m", "Brasil", "51t", "E190", "E195");
        registrar("Beechcraft Baron", "Beechcraft", "COMMON", SIL_LIGERO, "370 km/h", "6 pax", "11.5m", "EE.UU.", "2.3t", "BE55", "BE58", "B58T");
        registrar("Diamond DA42", "Diamond", "COMMON", SIL_LIGERO, "365 km/h", "4 pax", "13.5m", "Austria", "1.7t", "DA42", "D42P");
        registrar("Bell 206 JetRanger", "Bell", "COMMON", SIL_HELICOPTERO, "224 km/h", "5 pax", "10m", "EE.UU.", "1.4t", "B06", "B06T");
        registrar("Cessna 152", "Cessna", "COMMON", SIL_LIGERO, "198 km/h", "2 pax", "10m", "EE.UU.", "757kg", "C152");

        // --- AERONAVES PRINCIPALES (RARE) ---
        registrar("Boeing 787 Dreamliner", "Boeing", "RARE", SIL_JET, "903 km/h", "242 pax", "60m", "EE.UU.", "227t", "B788", "B789", "B78X");
        registrar("Airbus A350 XWB", "Airbus", "RARE", SIL_JET, "903 km/h", "325 pax", "65m", "Europa", "280t", "A359", "A35K");
        registrar("C-130 Hercules", "Lockheed", "RARE", SIL_CARGA, "671 km/h", "Carga", "40m", "EE.UU.", "70t", "C130", "C30J", "L100");
        registrar("Learjet 60", "Bombardier", "RARE", SIL_JET, "839 km/h", "8 pax", "13.5m", "EE.UU.", "10t", "LJ60");
        registrar("Boeing 777-300ER", "Boeing", "RARE", SIL_JET, "905 km/h", "396 pax", "65m", "EE.UU.", "351t", "B77W");
        registrar("Airbus A330", "Airbus", "RARE", SIL_JET, "871 km/h", "277 pax", "60m", "Europa", "242t", "A332", "A333");
        registrar("Gulfstream G650", "Gulfstream", "RARE", SIL_JET, "956 km/h", "19 pax", "30m", "EE.UU.", "45t", "GLF6");
        registrar("Dassault Falcon 7X", "Dassault", "RARE", SIL_JET, "900 km/h", "12 pax", "26m", "Francia", "31t", "FA7X");
        registrar("CH-47 Chinook", "Boeing", "RARE", SIL_HELICOPTERO, "315 km/h", "55 tropas", "18m", "EE.UU.", "22t", "H47");

        // --- AERONAVES PRINCIPALES (EPIC) ---
        registrar("Airbus A380", "Airbus", "EPIC", SIL_JET, "1020 km/h", "853 pax", "80m", "Europa", "575t", "A388");
        registrar("Boeing 747-400", "Boeing", "EPIC", SIL_JET, "988 km/h", "416 pax", "64m", "EE.UU.", "396t", "B744");
        registrar("AH-64 Apache", "Boeing", "EPIC", SIL_HELICOPTERO, "293 km/h", "2 pilotos", "15m", "EE.UU.", "10t", "AH64");
        registrar("V-22 Osprey", "Bell Boeing", "EPIC", SIL_CARGA, "509 km/h", "24 tropas", "26m", "EE.UU.", "27t", "V22");
        registrar("Antonov An-124", "Antonov", "EPIC", SIL_CARGA, "865 km/h", "Carga", "73m", "URSS", "405t", "A124");
        registrar("Airbus Beluga XL", "Airbus", "EPIC", SIL_CARGA, "737 km/h", "Carga Esp.", "60m", "Europa", "227t", "A337");
        registrar("Boeing Dreamlifter", "Boeing", "EPIC", SIL_CARGA, "878 km/h", "Carga Esp.", "64m", "EE.UU.", "364t", "BLCF");
        registrar("Mi-24 Hind", "Mil", "EPIC", SIL_HELICOPTERO, "335 km/h", "8 tropas", "17m", "URSS", "12t", "MI24");

        // Cazas y jets militares
        registrar("Eurofighter Typhoon", "Eurofighter", "EPIC", SIL_CAZA, "2495 km/h", "1 piloto", "11m", "Europa", "23t", "EUFI");
        registrar("F-16 Fighting Falcon", "General Dynamics", "EPIC", SIL_CAZA, "2120 km/h", "1 piloto", "10m", "EE.UU.", "19t", "F16");
        registrar("Mirage 2000", "Dassault", "EPIC", SIL_CAZA, "2336 km/h", "1 piloto", "9m", "Francia", "17t", "MIR2");
        registrar("Saab 39 Gripen", "Saab", "EPIC", SIL_CAZA, "2204 km/h", "1 piloto", "8.4m", "Suecia", "14t", "SB39");
        registrar("F-35 Lightning II", "Lockheed Martin", "EPIC", SIL_CAZA, "1960 km/h", "1 piloto", "11m", "EE.UU.", "31t", "F35");
        registrar("F/A-18 Super Hornet", "Boeing", "EPIC", SIL_CAZA, "1915 km/h", "1 piloto", "14m", "EE.UU.", "29t", "F18S", "F18H");

        // --- AERONAVES PRINCIPALES (LEGENDARY) ---
        registrar("F-22 Raptor", "Lockheed Martin", "LEGENDARY", SIL_CAZA, "2414 km/h", "1 piloto", "14m", "EE.UU.", "38t", "F22");
        registrar("Spitfire", "Supermarine", "LEGENDARY", SIL_HISTORICO, "584 km/h", "1 piloto", "11m", "UK", "3t", "SPIT");
        registrar("P-51 Mustang", "North American", "LEGENDARY", SIL_HISTORICO, "703 km/h", "1 piloto", "11m", "EE.UU.", "5t", "P51");
        registrar("Me 262", "Messerschmitt", "LEGENDARY", SIL_HISTORICO, "900 km/h", "1 piloto", "12.5m", "Alemania", "7t", "ME62");
        registrar("Harrier Jump Jet", "Hawker Siddeley", "LEGENDARY", SIL_CAZA, "1176 km/h", "1 piloto", "9.2m", "UK", "11t", "HAR");

        // --- CATÁLOGO EXTENDIDO (AIRBUS) ---
        registrar("Airbus A220-100", "Airbus", "RARE", SIL_JET, "828 km/h", "130 pax", "35m", "Canadá/Europa", "63t", "BCS1");
        registrar("Airbus A220-300", "Airbus", "RARE", SIL_JET, "828 km/h", "145 pax", "35m", "Canadá/Europa", "71t", "BCS3");
        registrar("Airbus A318", "Airbus", "RARE", SIL_JET, "828 km/h", "117 pax", "34m", "Europa", "68t", "A318");
        registrar("Airbus A319", "Airbus", "COMMON", SIL_JET, "828 km/h", "134 pax", "34m", "Europa", "75t", "A319");
        registrar("Airbus A321", "Airbus", "COMMON", SIL_JET, "828 km/h", "185 pax", "34m", "Europa", "93t", "A321");
        registrar("Airbus A319neo", "Airbus", "RARE", SIL_JET, "828 km/h", "140 pax", "36m", "Europa", "75t", "A19N");
        registrar("Airbus A320neo", "Airbus", "COMMON", SIL_JET, "828 km/h", "165 pax", "36m", "Europa", "79t", "A20N");
        registrar("Airbus A321neo", "Airbus", "COMMON", SIL_JET, "828 km/h", "206 pax", "36m", "Europa", "97t", "A21N");
        registrar("Airbus A300-600", "Airbus", "RARE", SIL_JET, "833 km/h", "266 pax", "45m", "Europa", "171t", "A306", "A30B");
        registrar("Airbus A310", "Airbus", "EPIC", SIL_JET, "850 km/h", "220 pax", "44m", "Europa", "150t", "A310");
        registrar("Airbus A330neo", "Airbus", "RARE", SIL_JET, "880 km/h", "287 pax", "64m", "Europa", "251t", "A338", "A339");
        registrar("Airbus A340-300", "Airbus", "EPIC", SIL_JET, "880 km/h", "295 pax", "60m", "Europa", "276t", "A342", "A343");
        registrar("Airbus A340-500", "Airbus", "EPIC", SIL_JET, "900 km/h", "313 pax", "63m", "Europa", "380t", "A345");
        registrar("Airbus A340-600", "Airbus", "EPIC", SIL_JET, "890 km/h", "380 pax", "64m", "Europa", "380t", "A346");

        // --- CATÁLOGO EXTENDIDO (BOEING) ---
        registrar("Boeing 707", "Boeing", "LEGENDARY", SIL_JET, "965 km/h", "189 pax", "44m", "EE.UU.", "151t", "B703");
        registrar("Boeing 717-200", "Boeing", "RARE", SIL_JET, "811 km/h", "106 pax", "28m", "EE.UU.", "55t", "B712");
        registrar("Boeing 727-200", "Boeing", "EPIC", SIL_JET, "960 km/h", "189 pax", "33m", "EE.UU.", "95t", "B721", "B722");
        registrar("Boeing 737-200", "Boeing", "EPIC", SIL_JET, "780 km/h", "130 pax", "28m", "EE.UU.", "58t", "B732");
        registrar("Boeing 737-300", "Boeing", "RARE", SIL_JET, "800 km/h", "149 pax", "29m", "EE.UU.", "63t", "B733");
        registrar("Boeing 737-400", "Boeing", "RARE", SIL_JET, "800 km/h", "168 pax", "29m", "EE.UU.", "68t", "B734");
        registrar("Boeing 737-500", "Boeing", "EPIC", SIL_JET, "800 km/h", "132 pax", "29m", "EE.UU.", "60t", "B735");
        registrar("Boeing 737-600", "Boeing", "EPIC", SIL_JET, "828 km/h", "110 pax", "34m", "EE.UU.", "66t", "B736");
        registrar("Boeing 737-700", "Boeing", "COMMON", SIL_JET, "828 km/h", "140 pax", "36m", "EE.UU.", "78t", "B737");
        registrar("Boeing 737-900", "Boeing", "RARE", SIL_JET, "828 km/h", "180 pax", "36m", "EE.UU.", "85t", "B739");
        registrar("Boeing 737 MAX 7", "Boeing", "RARE", SIL_JET, "839 km/h", "153 pax", "36m", "EE.UU.", "80t", "B37M");
        registrar("Boeing 737 MAX 8", "Boeing", "COMMON", SIL_JET, "839 km/h", "178 pax", "36m", "EE.UU.", "82t", "B38M");
        registrar("Boeing 737 MAX 9", "Boeing", "RARE", SIL_JET, "839 km/h", "204 pax", "36m", "EE.UU.", "88t", "B39M");
        registrar("Boeing 737 MAX 10", "Boeing", "RARE", SIL_JET, "839 km/h", "230 pax", "36m", "EE.UU.", "90t", "B3XM");
        registrar("Boeing 747-200", "Boeing", "LEGENDARY", SIL_JET, "894 km/h", "366 pax", "60m", "EE.UU.", "377t", "B741", "B742");
        registrar("Boeing 747-300", "Boeing", "LEGENDARY", SIL_JET, "907 km/h", "400 pax", "60m", "EE.UU.", "378t", "B743");
        registrar("Boeing 747SP", "Boeing", "LEGENDARY", SIL_JET, "920 km/h", "276 pax", "60m", "EE.UU.", "318t", "B74S");
        registrar("Boeing 747-8", "Boeing", "EPIC", SIL_JET, "920 km/h", "467 pax", "68m", "EE.UU.", "448t", "B748");
        registrar("Boeing 757-200", "Boeing", "RARE", SIL_JET, "850 km/h", "200 pax", "38m", "EE.UU.", "115t", "B752");
        registrar("Boeing 757-300", "Boeing", "EPIC", SIL_JET, "850 km/h", "243 pax", "38m", "EE.UU.", "123t", "B753");
        registrar("Boeing 767-200", "Boeing", "EPIC", SIL_JET, "851 km/h", "216 pax", "48m", "EE.UU.", "143t", "B762");
        registrar("Boeing 767-300", "Boeing", "RARE", SIL_JET, "851 km/h", "261 pax", "48m", "EE.UU.", "187t", "B763");
        registrar("Boeing 767-400ER", "Boeing", "EPIC", SIL_JET, "851 km/h", "245 pax", "52m", "EE.UU.", "204t", "B764");
        registrar("Boeing 777-200", "Boeing", "RARE", SIL_JET, "892 km/h", "305 pax", "61m", "EE.UU.", "298t", "B772");
        registrar("Boeing 777-300", "Boeing", "RARE", SIL_JET, "892 km/h", "368 pax", "61m", "EE.UU.", "299t", "B773");
        registrar("Boeing 777 Freighter", "Boeing", "RARE", SIL_JET, "896 km/h", "Carga", "65m", "EE.UU.", "347t", "B77L");
        registrar("Boeing 777-8", "Boeing", "EPIC", SIL_JET, "905 km/h", "395 pax", "72m", "EE.UU.", "351t", "B778");
        registrar("Boeing 777-9", "Boeing", "EPIC", SIL_JET, "905 km/h", "426 pax", "72m", "EE.UU.", "351t", "B779");

        // --- DOUGLAS, MCDONNELL DOUGLAS Y LOCKHEED ---
        registrar("Douglas DC-8", "Douglas", "LEGENDARY", SIL_JET, "875 km/h", "Carga/189 pax", "45m", "EE.UU.", "159t", "DC85", "DC86", "DC87");
        registrar("McDonnell Douglas MD-80", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "155 pax", "33m", "EE.UU.", "68t", "MD81", "MD82", "MD83", "MD87", "MD88");
        registrar("McDonnell Douglas MD-90", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "160 pax", "33m", "EE.UU.", "76t", "MD90");
        registrar("McDonnell Douglas MD-11", "McDonnell Douglas", "EPIC", SIL_JET, "876 km/h", "293 pax", "52m", "EE.UU.", "286t", "MD11");
        registrar("McDonnell Douglas DC-9", "McDonnell Douglas", "LEGENDARY", SIL_JET, "804 km/h", "115 pax", "28m", "EE.UU.", "49t", "DC91", "DC92", "DC93", "DC94", "DC95");
        registrar("McDonnell Douglas DC-10", "McDonnell Douglas", "EPIC", SIL_JET, "875 km/h", "270 pax", "50m", "EE.UU.", "260t", "DC10");
        registrar("Lockheed L-1011 TriStar", "Lockheed", "LEGENDARY", SIL_JET, "960 km/h", "256 pax", "47m", "EE.UU.", "195t", "L101");

        // --- REACTORES REGIONALES Y SOVIÉTICOS ---
        registrar("Embraer E170", "Embraer", "RARE", SIL_JET, "828 km/h", "72 pax", "26m", "Brasil", "37t", "E170");
        registrar("Embraer E175", "Embraer", "COMMON", SIL_JET, "828 km/h", "78 pax", "26m", "Brasil", "40t", "E75L", "E75S");
        registrar("Embraer E190-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "106 pax", "34m", "Brasil", "57t", "E290");
        registrar("Embraer E195-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "132 pax", "35m", "Brasil", "61t", "E295");
        registrar("Embraer ERJ-145", "Embraer", "COMMON", SIL_JET, "828 km/h", "50 pax", "20m", "Brasil", "22t", "E135", "E140", "E145");
        registrar("Bombardier CRJ-200", "Bombardier", "COMMON", SIL_JET, "830 km/h", "50 pax", "21m", "Canadá", "24t", "CRJ1", "CRJ2");
        registrar("Fokker F28 Fellowship", "Fokker", "LEGENDARY", SIL_JET, "800 km/h", "85 pax", "25m", "Países Bajos", "33t", "F28");
        registrar("Fokker 70", "Fokker", "EPIC", SIL_JET, "845 km/h", "79 pax", "28m", "Países Bajos", "40t", "F70");
        registrar("Fokker 100", "Fokker", "EPIC", SIL_JET, "845 km/h", "109 pax", "28m", "Países Bajos", "46t", "F100");
        registrar("BAe 146", "British Aerospace", "EPIC", SIL_JET, "789 km/h", "100 pax", "26m", "UK", "44t", "B461", "B462", "B463", "RJ70", "RJ85", "RJ1H");
        registrar("Sukhoi Superjet 100", "Sukhoi", "EPIC", SIL_JET, "828 km/h", "98 pax", "28m", "Rusia", "49t", "SU95");
        registrar("Comac C919", "Comac", "EPIC", SIL_JET, "828 km/h", "158 pax", "36m", "China", "78t", "C919");
        registrar("Comac ARJ21", "Comac", "EPIC", SIL_JET, "828 km/h", "90 pax", "27m", "China", "41t", "AJ27");
        registrar("Tupolev Tu-154", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "180 pax", "38m", "URSS", "100t", "T154");
        registrar("Tupolev Tu-134", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "76 pax", "29m", "URSS", "47t", "T134");
        registrar("Tupolev Tu-204", "Tupolev", "EPIC", SIL_JET, "810 km/h", "210 pax", "42m", "Rusia", "110t", "T204");
        registrar("Ilyushin Il-62", "Ilyushin", "LEGENDARY", SIL_JET, "900 km/h", "186 pax", "43m", "URSS", "165t", "IL62");
        registrar("Ilyushin Il-96", "Ilyushin", "EPIC", SIL_JET, "850 km/h", "300 pax", "60m", "Rusia", "250t", "IL96");
        registrar("Antonov An-72", "Antonov", "EPIC", SIL_JET, "705 km/h", "68 pax", "32m", "URSS", "35t", "AN72", "AN74");
        registrar("Antonov An-148", "Antonov", "EPIC", SIL_JET, "835 km/h", "75 pax", "29m", "Ucrania", "43t", "A148");
        registrar("Yakovlev Yak-40", "Yakovlev", "LEGENDARY", SIL_JET, "550 km/h", "32 pax", "25m", "URSS", "16t", "YK40");
        registrar("Yakovlev Yak-42", "Yakovlev", "EPIC", SIL_JET, "810 km/h", "120 pax", "35m", "URSS", "57t", "YK42");

        // --- REACTORES PRIVADOS Y EJECUTIVOS ---
        registrar("Bombardier Challenger 350", "Bombardier", "RARE", SIL_JET, "870 km/h", "10 pax", "21m", "Canadá", "18t", "CL35");
        registrar("Bombardier Challenger 604", "Bombardier", "RARE", SIL_JET, "870 km/h", "12 pax", "20m", "Canadá", "22t", "CL60");
        registrar("Bombardier Global Express", "Bombardier", "RARE", SIL_JET, "902 km/h", "17 pax", "29m", "Canadá", "45t", "GLEX", "GL5T", "GL6T", "GL7T");
        registrar("Gulfstream G280", "Gulfstream", "RARE", SIL_JET, "850 km/h", "10 pax", "20m", "EE.UU.", "18t", "G280");
        registrar("Gulfstream G450", "Gulfstream", "RARE", SIL_JET, "904 km/h", "16 pax", "24m", "EE.UU.", "34t", "GLF4");
        registrar("Gulfstream G550", "Gulfstream", "RARE", SIL_JET, "904 km/h", "19 pax", "28.5m", "EE.UU.", "41t", "GLF5");
        registrar("Gulfstream G700", "Gulfstream", "EPIC", SIL_JET, "904 km/h", "19 pax", "31m", "EE.UU.", "48t", "GLF7");
        registrar("Dassault Falcon 900", "Dassault", "RARE", SIL_JET, "870 km/h", "14 pax", "19m", "Francia", "21t", "F900");
        registrar("Dassault Falcon 2000", "Dassault", "RARE", SIL_JET, "870 km/h", "10 pax", "21m", "Francia", "19t", "F2TH");
        registrar("Dassault Falcon 8X", "Dassault", "RARE", SIL_JET, "880 km/h", "16 pax", "26m", "Francia", "33t", "FA8X");
        registrar("Cessna Citation Mustang", "Cessna", "RARE", SIL_JET, "630 km/h", "4 pax", "13m", "EE.UU.", "4t", "C510");
        registrar("Cessna Citation CJ", "Cessna", "RARE", SIL_JET, "770 km/h", "8 pax", "15m", "EE.UU.", "6t", "C525", "C25A", "C25B", "C25C");
        registrar("Cessna Citation XLS", "Cessna", "RARE", SIL_JET, "815 km/h", "9 pax", "17m", "EE.UU.", "9t", "C56X");
        registrar("Cessna Citation X", "Cessna", "EPIC", SIL_JET, "900 km/h", "9 pax", "19m", "EE.UU.", "16t", "C750");
        registrar("Embraer Phenom 100", "Embraer", "RARE", SIL_JET, "720 km/h", "4 pax", "12m", "Brasil", "5t", "E50P");
        registrar("Embraer Phenom 300", "Embraer", "RARE", SIL_JET, "839 km/h", "9 pax", "16m", "Brasil", "8t", "E55P");
        registrar("HondaJet", "Honda", "RARE", SIL_JET, "782 km/h", "5 pax", "12m", "Japón/EE.UU.", "5t", "HDJT");
        registrar("Pilatus PC-24", "Pilatus", "RARE", SIL_JET, "815 km/h", "8 pax", "17m", "Suiza", "8t", "PC24");
        registrar("Learjet 45", "Learjet", "RARE", SIL_JET, "860 km/h", "9 pax", "14.5m", "EE.UU.", "9t", "LJ45");
        registrar("Learjet 35", "Learjet", "EPIC", SIL_JET, "830 km/h", "8 pax", "12m", "EE.UU.", "8t", "LJ35");
        registrar("Hawker 800", "Hawker", "RARE", SIL_JET, "830 km/h", "8 pax", "16m", "UK/EE.UU.", "12t", "H25B");
        registrar("Eclipse 500", "Eclipse", "RARE", SIL_JET, "685 km/h", "5 pax", "11.5m", "EE.UU.", "3t", "EA50");
        registrar("Cirrus Vision Jet", "Cirrus", "RARE", SIL_JET, "556 km/h", "5 pax", "12m", "EE.UU.", "3t", "SF50");
        registrar("Cessna Citation Latitude", "Cessna", "RARE", SIL_JET, "826 km/h", "9 pax", "22m", "EE.UU.", "14t", "C68A");
        registrar("Cessna Citation Longitude", "Cessna", "RARE", SIL_JET, "895 km/h", "12 pax", "26m", "EE.UU.", "18t", "C700");
        registrar("Cessna Citation Sovereign", "Cessna", "RARE", SIL_JET, "852 km/h", "12 pax", "22m", "EE.UU.", "14t", "C680");
        registrar("Embraer Legacy 500", "Embraer", "RARE", SIL_JET, "863 km/h", "12 pax", "20m", "Brasil", "17t", "E550");
        registrar("Embraer Praetor 600", "Embraer", "RARE", SIL_JET, "863 km/h", "12 pax", "21.5m", "Brasil", "19t", "E545");

        // --- TURBOHÉLICES Y AVIACIÓN LIGERA ---
        registrar("ATR 42", "ATR", "RARE", SIL_TURBOHELICE, "556 km/h", "48 pax", "25m", "Francia/Italia", "19t", "AT43", "AT44", "AT45", "AT46");
        registrar("De Havilland Dash 8-100/200", "De Havilland", "RARE", SIL_TURBOHELICE, "500 km/h", "37 pax", "26m", "Canadá", "16t", "DH8A", "DH8B");
        registrar("De Havilland Dash 8-300", "De Havilland", "RARE", SIL_TURBOHELICE, "528 km/h", "50 pax", "27m", "Canadá", "19t", "DH8C");
        registrar("Bombardier Dash 8 Q400", "Bombardier", "RARE", SIL_TURBOHELICE, "667 km/h", "78 pax", "28m", "Canadá", "29t", "DH8D");
        registrar("De Havilland Dash 7", "De Havilland", "EPIC", SIL_TURBOHELICE, "428 km/h", "50 pax", "28m", "Canadá", "20t", "DHC7");
        registrar("De Havilland Twin Otter", "De Havilland", "RARE", SIL_TURBOHELICE, "337 km/h", "19 pax", "20m", "Canadá", "6t", "DHC6");
        registrar("De Havilland Otter", "De Havilland", "RARE", SIL_TURBOHELICE, "260 km/h", "11 pax", "18m", "Canadá", "3.6t", "DHC3", "DH3T");
        registrar("Saab 340", "Saab", "RARE", SIL_TURBOHELICE, "467 km/h", "34 pax", "21m", "Suecia", "13t", "SF34");
        registrar("Saab 2000", "Saab", "EPIC", SIL_TURBOHELICE, "665 km/h", "50 pax", "25m", "Suecia", "23t", "SB20");
        registrar("Fokker F27 Friendship", "Fokker", "EPIC", SIL_TURBOHELICE, "480 km/h", "44 pax", "29m", "Países Bajos", "20t", "F27");
        registrar("Fokker 50", "Fokker", "EPIC", SIL_TURBOHELICE, "500 km/h", "56 pax", "29m", "Países Bajos", "21t", "F50");
        registrar("Embraer EMB-110 Bandeirante", "Embraer", "RARE", SIL_TURBOHELICE, "413 km/h", "19 pax", "15m", "Brasil", "6t", "E110");
        registrar("Embraer EMB-120 Brasilia", "Embraer", "RARE", SIL_TURBOHELICE, "552 km/h", "30 pax", "20m", "Brasil", "12t", "E120");
        registrar("BAe ATP", "British Aerospace", "EPIC", SIL_TURBOHELICE, "496 km/h", "64 pax", "31m", "UK", "23t", "ATP");
        registrar("Hawker Siddeley HS 748", "Hawker Siddeley", "EPIC", SIL_TURBOHELICE, "452 km/h", "48 pax", "30m", "UK", "21t", "A748");
        registrar("Short 330", "Short", "RARE", SIL_TURBOHELICE, "350 km/h", "30 pax", "23m", "UK", "10t", "SH33");
        registrar("Short 360", "Short", "RARE", SIL_TURBOHELICE, "400 km/h", "36 pax", "23m", "UK", "12t", "SH36");
        registrar("Short Skyvan", "Short", "RARE", SIL_TURBOHELICE, "324 km/h", "19 pax", "20m", "UK", "6t", "SC7");
        registrar("Dornier 228", "Dornier", "RARE", SIL_TURBOHELICE, "428 km/h", "19 pax", "17m", "Alemania", "6t", "D228");
        registrar("Dornier 328", "Dornier", "EPIC", SIL_TURBOHELICE, "620 km/h", "32 pax", "21m", "Alemania", "14t", "D328");
        registrar("BAe Jetstream 31", "British Aerospace", "RARE", SIL_TURBOHELICE, "430 km/h", "19 pax", "16m", "UK", "7t", "JS31", "JS32");
        registrar("BAe Jetstream 41", "British Aerospace", "RARE", SIL_TURBOHELICE, "482 km/h", "29 pax", "18m", "UK", "10t", "JS41");
        registrar("Fairchild Metroliner", "Fairchild", "RARE", SIL_TURBOHELICE, "576 km/h", "19 pax", "17m", "EE.UU.", "7t", "SW3", "SW4");
        registrar("Beechcraft 99", "Beechcraft", "RARE", SIL_TURBOHELICE, "450 km/h", "15 pax", "14m", "EE.UU.", "5t", "BE99");
        registrar("Beechcraft 1900D", "Beechcraft", "RARE", SIL_TURBOHELICE, "518 km/h", "19 pax", "18m", "EE.UU.", "8t", "B190");
        registrar("Beechcraft King Air 350", "Beechcraft", "RARE", SIL_TURBOHELICE, "578 km/h", "11 pax", "18m", "EE.UU.", "7t", "B350");
        registrar("Beechcraft King Air 200", "Beechcraft", "RARE", SIL_TURBOHELICE, "536 km/h", "9 pax", "17m", "EE.UU.", "6t", "BE20");
        registrar("Pilatus PC-12", "Pilatus", "RARE", SIL_TURBOHELICE, "528 km/h", "9 pax", "16m", "Suiza", "5t", "PC12", "P12E");
        registrar("Cessna 208 Caravan", "Cessna", "COMMON", SIL_TURBOHELICE, "344 km/h", "9 pax", "16m", "EE.UU.", "4t", "C208");
        registrar("Daher TBM 900", "Daher", "RARE", SIL_TURBOHELICE, "600 km/h", "6 pax", "12m", "Francia", "3.4t", "TBM7", "TBM8", "TBM9");
        registrar("Convair 580", "Convair", "EPIC", SIL_TURBOHELICE, "480 km/h", "50 pax", "32m", "EE.UU.", "25t", "CVLT");
        registrar("CASA C-295", "CASA", "RARE", SIL_TURBOHELICE, "480 km/h", "71 pax", "26m", "España", "23t", "C295");
        registrar("CASA CN-235", "CASA", "RARE", SIL_TURBOHELICE, "460 km/h", "44 pax", "26m", "España/Indonesia", "16t", "CN35");
        registrar("Pilatus PC-21", "Pilatus", "RARE", SIL_TURBOHELICE, "685 km/h", "2 pilotos", "9m", "Suiza", "3t", "PC21");
        registrar("Let L-410 Turbolet", "Let", "RARE", SIL_TURBOHELICE, "405 km/h", "19 pax", "20m", "Rep. Checa", "6.6t", "L410");
        registrar("Harbin Y-12", "Harbin", "RARE", SIL_TURBOHELICE, "328 km/h", "17 pax", "17m", "China", "5.3t", "Y12");
        registrar("Xian MA60", "Xian", "RARE", SIL_TURBOHELICE, "514 km/h", "60 pax", "29m", "China", "22t", "MA60");
        registrar("Antonov An-24", "Antonov", "EPIC", SIL_TURBOHELICE, "450 km/h", "50 pax", "29m", "URSS", "21t", "AN24");
        registrar("Antonov An-26", "Antonov", "EPIC", SIL_TURBOHELICE, "440 km/h", "Carga", "29m", "URSS", "24t", "AN26");
        registrar("Antonov An-32", "Antonov", "EPIC", SIL_TURBOHELICE, "530 km/h", "Carga", "29m", "URSS", "27t", "AN32");
        registrar("Ilyushin Il-18", "Ilyushin", "LEGENDARY", SIL_TURBOHELICE, "625 km/h", "110 pax", "37m", "URSS", "64t", "IL18");

        // --- AVIACIÓN GENERAL Y PISTÓN ---
        registrar("Cessna 150", "Cessna", "COMMON", SIL_LIGERO, "200 km/h", "2 pax", "10m", "EE.UU.", "0.7t", "C150");
        registrar("Cessna 182 Skylane", "Cessna", "COMMON", SIL_LIGERO, "278 km/h", "4 pax", "11m", "EE.UU.", "1.4t", "C182", "C82S", "C82T", "C82R");
        registrar("Cessna 206 Stationair", "Cessna", "RARE", SIL_LIGERO, "280 km/h", "6 pax", "11m", "EE.UU.", "1.6t", "C206", "T206");
        registrar("Cessna 210 Centurion", "Cessna", "RARE", SIL_LIGERO, "320 km/h", "6 pax", "11m", "EE.UU.", "1.7t", "C210");
        registrar("Cirrus SR20", "Cirrus", "COMMON", SIL_LIGERO, "290 km/h", "4 pax", "11.5m", "EE.UU.", "1.4t", "SR20");
        registrar("Cirrus SR22", "Cirrus", "COMMON", SIL_LIGERO, "340 km/h", "4 pax", "11.5m", "EE.UU.", "1.6t", "SR22", "S22T");
        registrar("Diamond DA20", "Diamond", "COMMON", SIL_LIGERO, "250 km/h", "2 pax", "11m", "Canadá", "0.8t", "DA20");
        registrar("Diamond DA40", "Diamond", "COMMON", SIL_LIGERO, "280 km/h", "4 pax", "12m", "Austria", "1.2t", "DA40", "D40D");
        registrar("Piper PA-34 Seneca", "Piper", "COMMON", SIL_LIGERO, "315 km/h", "6 pax", "12m", "EE.UU.", "2t", "PA34");
        registrar("Piper PA-44 Seminole", "Piper", "COMMON", SIL_LIGERO, "320 km/h", "4 pax", "12m", "EE.UU.", "1.7t", "PA44");
        registrar("Piper PA-32 Cherokee Six", "Piper", "RARE", SIL_LIGERO, "270 km/h", "6 pax", "11m", "EE.UU.", "1.7t", "PA32");
        registrar("Piper PA-18 Super Cub", "Piper", "RARE", SIL_LIGERO, "185 km/h", "2 pax", "11m", "EE.UU.", "0.8t", "PA18");
        registrar("Piper PA-46 Malibu", "Piper", "RARE", SIL_LIGERO, "440 km/h", "6 pax", "13m", "EE.UU.", "1.4t", "PA46");
        registrar("Beechcraft Bonanza", "Beechcraft", "RARE", SIL_LIGERO, "330 km/h", "6 pax", "10m", "EE.UU.", "1.5t", "BE33", "BE35", "BE36");
        registrar("Mooney M20", "Mooney", "RARE", SIL_LIGERO, "280 km/h", "4 pax", "11m", "EE.UU.", "1.2t", "M20P", "M20J", "M20T");
        registrar("Robin DR400", "Robin", "COMMON", SIL_LIGERO, "250 km/h", "4 pax", "9m", "Francia", "1.1t", "DR40");
        registrar("Tecnam P2006T", "Tecnam", "RARE", SIL_LIGERO, "270 km/h", "4 pax", "11.5m", "Italia", "1.2t", "P06T");
        registrar("Extra 300", "Extra", "RARE", SIL_LIGERO, "340 km/h", "2 pax", "8m", "Alemania", "0.95t", "E300");
        registrar("Pitts Special", "Pitts", "RARE", SIL_LIGERO, "330 km/h", "2 pax", "6m", "EE.UU.", "0.7t", "PTS2");
        registrar("Britten-Norman Islander", "Britten-Norman", "RARE", SIL_LIGERO, "257 km/h", "9 pax", "15m", "UK", "3t", "BN2P");
        registrar("Britten-Norman Trislander", "Britten-Norman", "EPIC", SIL_LIGERO, "267 km/h", "16 pax", "16m", "UK", "4.5t", "TRIS");

        // --- HELICÓPTEROS, MILITARES E HISTÓRICOS ---
        registrar("Robinson R22", "Robinson", "COMMON", SIL_HELICOPTERO, "180 km/h", "2 pax", "7.7m", "EE.UU.", "0.6t", "R22");
        registrar("Robinson R66", "Robinson", "RARE", SIL_HELICOPTERO, "200 km/h", "4 pax", "10m", "EE.UU.", "1.3t", "R66");
        registrar("Airbus H125", "Airbus Helicopters", "COMMON", SIL_HELICOPTERO, "250 km/h", "6 pax", "11m", "Francia", "2.2t", "AS50");
        registrar("Airbus H135", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "254 km/h", "8 pax", "10m", "Alemania", "3t", "EC35");
        registrar("Airbus H145", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "246 km/h", "9 pax", "11m", "Alemania", "4t", "EC45");
        registrar("Airbus EC120 Colibri", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "223 km/h", "4 pax", "10m", "Francia", "1.7t", "EC20");
        registrar("Airbus H175", "Airbus Helicopters", "EPIC", SIL_HELICOPTERO, "300 km/h", "18 pax", "15m", "Francia", "8t", "EC75");
        registrar("Airbus H225 Super Puma", "Airbus Helicopters", "EPIC", SIL_HELICOPTERO, "260 km/h", "24 pax", "16m", "Francia", "11t", "EC25");
        registrar("Bell 407", "Bell", "RARE", SIL_HELICOPTERO, "260 km/h", "6 pax", "11m", "EE.UU.", "2.3t", "B407");
        registrar("Bell 412", "Bell", "RARE", SIL_HELICOPTERO, "260 km/h", "13 pax", "14m", "EE.UU.", "5.4t", "B412");
        registrar("Bell 429", "Bell", "RARE", SIL_HELICOPTERO, "278 km/h", "7 pax", "11m", "EE.UU.", "3.2t", "B429");
        registrar("Leonardo AW139", "Leonardo", "RARE", SIL_HELICOPTERO, "306 km/h", "15 pax", "14m", "Italia", "6.4t", "A139");
        registrar("Agusta A109", "Agusta", "RARE", SIL_HELICOPTERO, "285 km/h", "7 pax", "11m", "Italia", "3t", "A109");
        registrar("Sikorsky S-92", "Sikorsky", "EPIC", SIL_HELICOPTERO, "300 km/h", "19 pax", "17m", "EE.UU.", "12t", "S92");
        registrar("Sikorsky UH-60 Black Hawk", "Sikorsky", "EPIC", SIL_HELICOPTERO, "295 km/h", "11 tropas", "16m", "EE.UU.", "10t", "H60");
        registrar("Sikorsky CH-53E Super Stallion", "Sikorsky", "EPIC", SIL_HELICOPTERO, "315 km/h", "55 tropas", "24m", "EE.UU.", "33t", "H53");
        registrar("NHIndustries NH90", "NHIndustries", "EPIC", SIL_HELICOPTERO, "300 km/h", "20 tropas", "16m", "Europa", "11t", "NH90");
        registrar("Mil Mi-8", "Mil", "EPIC", SIL_HELICOPTERO, "260 km/h", "24 pax", "21m", "URSS", "12t", "MI8");
        registrar("Mil Mi-26", "Mil", "EPIC", SIL_HELICOPTERO, "295 km/h", "90 tropas", "32m", "URSS", "56t", "MI26");
        registrar("Bell UH-1 Huey", "Bell", "LEGENDARY", SIL_HELICOPTERO, "204 km/h", "12 tropas", "15m", "EE.UU.", "4.3t", "UH1");

        // --- CARGUEROS Y AVIACIÓN ESPECIAL ---
        registrar("Airbus A400M Atlas", "Airbus", "EPIC", SIL_CARGA, "780 km/h", "116 tropas", "42m", "Europa", "141t", "A400");
        registrar("Airbus Beluga ST", "Airbus", "EPIC", SIL_CARGA, "750 km/h", "Carga Esp.", "45m", "Europa", "155t", "A3ST");
        registrar("Ilyushin Il-76", "Ilyushin", "EPIC", SIL_CARGA, "850 km/h", "Carga", "50m", "URSS", "195t", "IL76");
        registrar("Antonov An-12", "Antonov", "LEGENDARY", SIL_CARGA, "670 km/h", "Carga", "38m", "URSS", "61t", "AN12");
        registrar("Lockheed C-5 Galaxy", "Lockheed", "EPIC", SIL_CARGA, "833 km/h", "Carga", "68m", "EE.UU.", "381t", "C5M", "C5");
        registrar("Boeing C-17 Globemaster III", "Boeing", "EPIC", SIL_CARGA, "830 km/h", "Carga", "52m", "EE.UU.", "265t", "C17");
        registrar("Embraer KC-390", "Embraer", "EPIC", SIL_CARGA, "870 km/h", "Carga", "35m", "Brasil", "86t", "C390");
        registrar("Transall C-160", "Transall", "EPIC", SIL_CARGA, "513 km/h", "93 tropas", "40m", "Europa", "51t", "C160");
        registrar("Lockheed L-188 Electra", "Lockheed", "EPIC", SIL_CARGA, "600 km/h", "98 pax", "30m", "EE.UU.", "52t", "L188");
        registrar("Lockheed P-3 Orion", "Lockheed", "EPIC", SIL_CARGA, "761 km/h", "11 tripulantes", "30m", "EE.UU.", "61t", "P3");
        registrar("Boeing P-8 Poseidon", "Boeing", "EPIC", SIL_JET, "907 km/h", "9 tripulantes", "38m", "EE.UU.", "86t", "P8");
        registrar("Boeing E-3 Sentry", "Boeing", "EPIC", SIL_CARGA, "855 km/h", "20 tripulantes", "44m", "EE.UU.", "148t", "E3TF", "E3CF");
        registrar("Boeing KC-135 Stratotanker", "Boeing", "EPIC", SIL_CARGA, "933 km/h", "Cisterna", "40m", "EE.UU.", "146t", "K35R");
        registrar("Boeing B-52 Stratofortress", "Boeing", "EPIC", SIL_CARGA, "1047 km/h", "5 tripulantes", "56m", "EE.UU.", "220t", "B52");
        registrar("Rockwell B-1B Lancer", "Rockwell", "EPIC", SIL_CAZA, "1335 km/h", "4 tripulantes", "42m", "EE.UU.", "216t", "B1");

        // Todos los cazas restantes convertidos a EPIC o LEGENDARY
        registrar("F-15 Eagle", "McDonnell Douglas", "EPIC", SIL_CAZA, "2655 km/h", "1 piloto", "13m", "EE.UU.", "31t", "F15");
        registrar("F-4 Phantom II", "McDonnell Douglas", "LEGENDARY", SIL_CAZA, "2370 km/h", "2 pilotos", "12m", "EE.UU.", "28t", "F4");
        registrar("A-10 Thunderbolt II", "Fairchild Republic", "EPIC", SIL_CAZA, "706 km/h", "1 piloto", "18m", "EE.UU.", "23t", "A10");
        registrar("Dassault Rafale", "Dassault", "EPIC", SIL_CAZA, "1912 km/h", "1 piloto", "11m", "Francia", "25t", "RFAL");
        registrar("Panavia Tornado", "Panavia", "EPIC", SIL_CAZA, "2400 km/h", "2 pilotos", "14m", "Europa", "28t", "TOR");
        registrar("Mikoyan MiG-29", "Mikoyan", "EPIC", SIL_CAZA, "2400 km/h", "1 piloto", "11m", "URSS", "18t", "MG29");
        registrar("Northrop F-5 Tiger II", "Northrop", "EPIC", SIL_CAZA, "1700 km/h", "1 piloto", "8m", "EE.UU.", "11t", "F5");
        registrar("Lockheed U-2 Dragon Lady", "Lockheed", "EPIC", SIL_CAZA, "800 km/h", "1 piloto", "31m", "EE.UU.", "18t", "U2");
        registrar("BAE Hawk", "BAE Systems", "EPIC", SIL_CAZA, "1000 km/h", "2 pilotos", "10m", "UK", "9t", "HAWK");
        registrar("Dassault/Dornier Alpha Jet", "Dassault/Dornier", "EPIC", SIL_CAZA, "1000 km/h", "2 pilotos", "9m", "Francia/Alemania", "8t", "ALPH");
        registrar("Aermacchi M-346", "Aermacchi", "EPIC", SIL_CAZA, "1090 km/h", "2 pilotos", "10m", "Italia", "10t", "M346");
        registrar("Northrop T-38 Talon", "Northrop", "EPIC", SIL_CAZA, "1380 km/h", "2 pilotos", "8m", "EE.UU.", "5t", "T38");
        registrar("Mikoyan MiG-21", "Mikoyan", "LEGENDARY", SIL_CAZA, "2175 km/h", "1 piloto", "7m", "URSS", "9t", "MG21");
        registrar("Mikoyan MiG-15", "Mikoyan", "LEGENDARY", SIL_CAZA, "1075 km/h", "1 piloto", "10m", "URSS", "5t", "MG15");
        registrar("F-86 Sabre", "North American", "LEGENDARY", SIL_CAZA, "1100 km/h", "1 piloto", "12m", "EE.UU.", "7t", "F86");

        // --- CLÁSICOS E HISTÓRICOS (LEGENDARY) ---
        registrar("Douglas DC-3", "Douglas", "LEGENDARY", SIL_HISTORICO, "333 km/h", "32 pax", "29m", "EE.UU.", "11t", "DC3");
        registrar("Douglas DC-4", "Douglas", "LEGENDARY", SIL_HISTORICO, "365 km/h", "44 pax", "36m", "EE.UU.", "33t", "DC4");
        registrar("Douglas DC-6", "Douglas", "LEGENDARY", SIL_HISTORICO, "507 km/h", "66 pax", "36m", "EE.UU.", "48t", "DC6");
        registrar("Lockheed Constellation", "Lockheed", "LEGENDARY", SIL_HISTORICO, "555 km/h", "62 pax", "38m", "EE.UU.", "62t", "CONI");
        registrar("Curtiss C-46 Commando", "Curtiss", "LEGENDARY", SIL_HISTORICO, "435 km/h", "40 tropas", "33m", "EE.UU.", "22t", "C46");
        registrar("Junkers Ju 52", "Junkers", "LEGENDARY", SIL_HISTORICO, "265 km/h", "17 pax", "29m", "Alemania", "11t", "JU52");
        registrar("Boeing B-17 Flying Fortress", "Boeing", "LEGENDARY", SIL_HISTORICO, "462 km/h", "10 tripulantes", "32m", "EE.UU.", "29t", "B17");
        registrar("Boeing B-29 Superfortress", "Boeing", "LEGENDARY", SIL_HISTORICO, "574 km/h", "11 tripulantes", "43m", "EE.UU.", "62t", "B29");
        registrar("Avro Lancaster", "Avro", "LEGENDARY", SIL_HISTORICO, "454 km/h", "7 tripulantes", "31m", "UK", "30t", "LANC");
        registrar("Consolidated PBY Catalina", "Consolidated", "LEGENDARY", SIL_HISTORICO, "314 km/h", "9 tripulantes", "32m", "EE.UU.", "16t", "CATA");
        registrar("De Havilland Mosquito", "De Havilland", "LEGENDARY", SIL_HISTORICO, "668 km/h", "2 pilotos", "17m", "UK", "10t", "MOSQ");
        registrar("Hawker Hurricane", "Hawker", "LEGENDARY", SIL_HISTORICO, "547 km/h", "1 piloto", "12m", "UK", "3t", "HURR");
        registrar("Messerschmitt Bf 109", "Messerschmitt", "LEGENDARY", SIL_HISTORICO, "640 km/h", "1 piloto", "10m", "Alemania", "3.4t", "ME09");
        registrar("Focke-Wulf Fw 190", "Focke-Wulf", "LEGENDARY", SIL_HISTORICO, "653 km/h", "1 piloto", "10m", "Alemania", "4.4t", "FW19");
        registrar("Mitsubishi A6M Zero", "Mitsubishi", "LEGENDARY", SIL_HISTORICO, "533 km/h", "1 piloto", "12m", "Japón", "2.8t", "ZERO");
        registrar("Vought F4U Corsair", "Vought", "LEGENDARY", SIL_HISTORICO, "718 km/h", "1 piloto", "13m", "EE.UU.", "6t", "F4U");
        registrar("Grumman F6F Hellcat", "Grumman", "LEGENDARY", SIL_HISTORICO, "612 km/h", "1 piloto", "13m", "EE.UU.", "7t", "F6F");
        registrar("Lockheed P-38 Lightning", "Lockheed", "LEGENDARY", SIL_HISTORICO, "667 km/h", "1 piloto", "16m", "EE.UU.", "8t", "P38");
        registrar("Republic P-47 Thunderbolt", "Republic", "LEGENDARY", SIL_HISTORICO, "697 km/h", "1 piloto", "12m", "EE.UU.", "8t", "P47");
        registrar("North American T-6 Texan", "North American", "LEGENDARY", SIL_HISTORICO, "340 km/h", "2 pilotos", "13m", "EE.UU.", "2.4t", "T6");
        registrar("Boeing Stearman", "Boeing", "LEGENDARY", SIL_HISTORICO, "200 km/h", "2 pax", "10m", "EE.UU.", "1.2t", "ST75");
        registrar("De Havilland Tiger Moth", "De Havilland", "LEGENDARY", SIL_HISTORICO, "175 km/h", "2 pax", "9m", "UK", "0.8t", "DH82");
        registrar("Piper J-3 Cub", "Piper", "LEGENDARY", SIL_HISTORICO, "120 km/h", "2 pax", "11m", "EE.UU.", "0.5t", "J3");
        registrar("Antonov An-2", "Antonov", "LEGENDARY", SIL_HISTORICO, "190 km/h", "12 pax", "18m", "URSS", "5.5t", "AN2");
        registrar("De Havilland Beaver", "De Havilland", "LEGENDARY", SIL_HISTORICO, "255 km/h", "7 pax", "14m", "Canadá", "2.3t", "DHC2");

        // --- AVIACIÓN LIGERA Y DEPORTIVA (LSA / EXPERIMENTALES FRECUENTES EN ADS-B) ---
        registrar("Sling 4", "Sling Aircraft", "RARE", SIL_LIGERO, "240 km/h", "4 pax", "9.5m", "Sudáfrica", "0.95t", "SLG4", "SL4");
        registrar("Sling 2", "Sling Aircraft", "RARE", SIL_LIGERO, "220 km/h", "2 pax", "9m", "Sudáfrica", "0.7t", "SLG2");
        registrar("Van's RV-7", "Van's Aircraft", "COMMON", SIL_LIGERO, "320 km/h", "2 pax", "7.6m", "EE.UU.", "0.8t", "RV7", "RV6", "RV8");
        registrar("Van's RV-10", "Van's Aircraft", "RARE", SIL_LIGERO, "330 km/h", "4 pax", "9.7m", "EE.UU.", "1.2t", "RV10");
        registrar("Van's RV-12", "Van's Aircraft", "COMMON", SIL_LIGERO, "220 km/h", "2 pax", "8.1m", "EE.UU.", "0.6t", "RV12");
        registrar("Pipistrel Virus", "Pipistrel", "RARE", SIL_LIGERO, "273 km/h", "2 pax", "10.7m", "Eslovenia", "0.6t", "PIVI", "SIRA");
        registrar("Flight Design CT", "Flight Design", "COMMON", SIL_LIGERO, "230 km/h", "2 pax", "8.5m", "Alemania", "0.6t", "FDCT");
        registrar("ICON A5", "ICON Aircraft", "EPIC", SIL_LIGERO, "176 km/h", "2 pax", "10.6m", "EE.UU.", "0.68t", "A5");
        registrar("Tecnam P92 Echo", "Tecnam", "COMMON", SIL_LIGERO, "215 km/h", "2 pax", "8.7m", "Italia", "0.6t", "P92");
        registrar("Aeroprakt A-22 Foxbat", "Aeroprakt", "RARE", SIL_LIGERO, "170 km/h", "2 pax", "9.5m", "Ucrania", "0.45t", "A22");

        // --- AVIACIÓN LIGERA Y DEPORTIVA (LSA / EXPERIMENTALES / BUSH PLANES) ---
        registrar("Aviat A-1 Husky", "Aviat Aircraft", "RARE", SIL_LIGERO, "233 km/h", "2 pax", "10.8m", "EE.UU.", "1t", "HUSK");
        registrar("American Champion Citabria", "American Champion", "COMMON", SIL_LIGERO, "210 km/h", "2 pax", "10.2m", "EE.UU.", "0.8t", "CH7A", "CH7B", "BL8");
        registrar("Maule M-7", "Maule Air", "RARE", SIL_LIGERO, "240 km/h", "4 pax", "10m", "EE.UU.", "1.1t", "M7", "M5", "M6");
        registrar("Grumman AA-5 Tiger", "Grumman American", "COMMON", SIL_LIGERO, "250 km/h", "4 pax", "9.6m", "EE.UU.", "1.1t", "AA5", "AA1");
        registrar("Luscombe 8 Silvaire", "Luscombe", "RARE", SIL_HISTORICO, "185 km/h", "2 pax", "10.7m", "EE.UU.", "0.6t", "L8");
        registrar("Aeronca Champ", "Aeronca", "RARE", SIL_HISTORICO, "150 km/h", "2 pax", "10.7m", "EE.UU.", "0.55t", "CH7");
        registrar("Stinson 108", "Stinson", "RARE", SIL_HISTORICO, "200 km/h", "4 pax", "10.4m", "EE.UU.", "1.1t", "S108");
        registrar("Bellanca Viking", "Bellanca", "RARE", SIL_LIGERO, "320 km/h", "4 pax", "10.4m", "EE.UU.", "1.5t", "BL17");
    }

    /**
     * Comprueba si la app se ha actualizado respecto a la versión que generó la caché.
     * Si el versionCode cambió, purga automáticamente los datos obsoletos sin intervención manual.
     */
    private static void validarYLimpiarCacheSiAppActualizada(Context appContext, SharedPreferences prefs, ConcurrentHashMap<String, String> memCache) {
        try {
            // Obtiene el versionCode actual del paquete instalado
            long currentVersion = PackageInfoCompat.getLongVersionCode(
                    appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0)
            );
            // Recupera la versión con la que se guardó la caché anteriormente
            long savedVersion = prefs.getLong(KEY_APP_VERSION, -1L);
            // Si la versión difiere, limpia RAM y disco y guarda el nuevo versionCode
            if (savedVersion != currentVersion) {
                memCache.clear();
                prefs.edit().clear().putLong(KEY_APP_VERSION, currentVersion).apply();
            }
        } catch (Exception ignored) {
            // Ignora fallos puntuales de consulta al PackageManager
        }
    }

    /**
     * Envuelve un valor con una marca de tiempo actual para gestionar su caducidad automática (TTL).
     */
    private static String empaquetarConTtl(String valor) {
        // Concatena timestamp actual + separador + contenido real
        return System.currentTimeMillis() + SEPARADOR_TTL + valor;
    }

    /**
     * Extrae el valor almacenado si no ha superado el tiempo de vida máximo (CACHE_TTL_MS).
     * Devuelve null si el registro está caducado para forzar su actualización limpia.
     */
    private static String desempaquetarSiVigente(String registroConTtl) {
        // Verifica que exista el registro y contenga el delimitador de tiempo
        if (registroConTtl == null) return null;
        int idx = registroConTtl.indexOf(SEPARADOR_TTL);
        if (idx < 0) return null;
        try {
            // Extrae el timestamp guardado y comprueba si superó los 7 días de validez
            long timestamp = Long.parseLong(registroConTtl.substring(0, idx));
            if (System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
                return null;
            }
            // Devuelve el contenido útil sin la cabecera de tiempo
            return registroConTtl.substring(idx + SEPARADOR_TTL.length());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Vacía manualmente toda la caché en memoria y en disco de Wikipedia e identificadores ICAO.
     */
    public static void limpiarTodaLaCache(Context context) {
        // Limpia ambos mapas concurrentes en RAM
        CACHE_MEMORIA_WIKI.clear();
        CACHE_MEMORIA_HEX.clear();
        // Borra ambos archivos de preferencias persistentes en disco
        Context appContext = context.getApplicationContext();
        appContext.getSharedPreferences(PREFS_WIKI_CACHE, Context.MODE_PRIVATE).edit().clear().apply();
        appContext.getSharedPreferences(PREFS_HEX_CACHE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    /**
     * Registra una aeronave en el catálogo maestro e indexa tanto sus códigos ICAO
     * como su nombre normalizado para búsquedas exactas.
     */
    private static void registrar(String modelo, String fabricante, String rareza, int siluetaId,
                                  String vel, String cap, String dim, String pais, String peso,
                                  String... codigos) {
        // Toma el primer código ICAO de la lista como designador principal del modelo
        String icaoPrincipal = (codigos != null && codigos.length > 0) ? codigos[0].trim().toUpperCase(Locale.ROOT) : "";
        // Crea la ficha técnica inmutable y la añade a la lista maestra
        AvionData data = new AvionData(modelo, fabricante, rareza, siluetaId, vel, cap, dim, pais, peso, icaoPrincipal);
        DATOS_AVIONES.add(data);
        // Indexa por nombre normalizado y por cada código ICAO alternativo proporcionado
        MAPA_POR_NOMBRE_NORMALIZADO.put(normalizarNombre(modelo), data);
        for (String c : codigos) {
            MAPA_POR_ICAO.put(c.trim().toUpperCase(Locale.ROOT), data);
        }
    }

    /**
     * Normaliza un nombre eliminando espacios y guiones en minúsculas para coincidencias exactas.
     */
    private static String normalizarNombre(String texto) {
        // Convierte a minúsculas y elimina espacios, guiones, barras y puntos
        if (texto == null) return "";
        return texto.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-_./]", "");
    }

    /**
     * Construye un objeto Avion a partir de una ficha local verificada.
     */
    private static Avion crearAvionDesdeData(AvionData d) {
        // Instancia un nuevo Avion con los datos técnicos de la ficha y asigna su tipo ICAO principal
        Avion a = new Avion(d.modelo, d.fabricante, d.rareza, d.siluetaId, d.vel, d.cap, d.dim, d.pais, d.peso);
        if (d.codigoIcaoPrincipal != null && !d.codigoIcaoPrincipal.isEmpty()) {
            a.setTipoIcao(d.codigoIcaoPrincipal);
        }
        return a;
    }

    /**
     * Comprueba si el blanco corresponde específicamente a una Torre de Control
     * o baliza transpondedora fija de torre (ej: TWR, TOWER, PARROT).
     */
    public static boolean esEasterEggTorre(String tipoIcao, String callsign, String matricula, String descripcion) {
        // Normaliza los cuatro campos recibidos en mayúsculas
        String t = (tipoIcao != null) ? tipoIcao.trim().toUpperCase(Locale.ROOT) : "";
        String c = (callsign != null) ? callsign.trim().toUpperCase(Locale.ROOT) : "";
        String m = (matricula != null) ? matricula.trim().toUpperCase(Locale.ROOT) : "";
        String d = (descripcion != null) ? descripcion.trim().toUpperCase(Locale.ROOT) : "";

        // Evalúa patrones habituales de transpondedores fijos de torre de control
        if ("TWR".equals(t) || "TOWER".equals(t) || "TWR".equals(m) || m.startsWith("TWR")) {
            return true;
        }
        if (c.equals("TWR") || c.startsWith("TWR") || c.endsWith("TWR")
                || c.contains("TOWER") || c.contains("PARROT")) {
            return true;
        }
        return d.contains("CONTROL TOWER") || d.contains("TORRE DE CONTROL");
    }

    /**
     * Comprueba si el blanco corresponde específicamente a un vehículo terrestre de operaciones
     * en pista (Follow-Me, Ops, mantenimiento, bomberos, etc.), excluyendo torres de control.
     */
    public static boolean esEasterEggVehiculo(String tipoIcao, String callsign, String matricula, String descripcion) {
        // Excluye primero las torres de control para evitar solapamientos
        if (esEasterEggTorre(tipoIcao, callsign, matricula, descripcion)) {
            return false;
        }

        // Normaliza los campos de identificación en mayúsculas
        String t = (tipoIcao != null) ? tipoIcao.trim().toUpperCase(Locale.ROOT) : "";
        String c = (callsign != null) ? callsign.trim().toUpperCase(Locale.ROOT) : "";
        String m = (matricula != null) ? matricula.trim().toUpperCase(Locale.ROOT) : "";
        String d = (descripcion != null) ? descripcion.trim().toUpperCase(Locale.ROOT) : "";

        // Detecta códigos de tipo, indicativos o matrículas propios de vehículos de pista
        if ("SERV".equals(t) || "GND".equals(t) || t.contains("GRND") || "VEH".equals(t) || "OPS".equals(t)) {
            return true;
        }
        if (c.matches("^(OPS|GND|SERV|VEH|FOLLOW|FIRE|RFF|MARSHAL|APRON|TOW|TUG|DEICE|SWEEP|SNOW|AIRPORT|AAST|TEST|OBD|MAINT|MNT|PATROL|SAFE|SEC)\\d*.*")) {
            return true;
        }
        if (m.startsWith("OPS") || m.startsWith("GND") || m.startsWith("VEH") || m.startsWith("OBD")) {
            return true;
        }
        return d.contains("GROUND VEHICLE") || d.contains("SURFACE VEHICLE")
                || d.contains("AIRPORT OPERATIONS") || d.contains("SURFACE TRANSPONDER");
    }

    /**
     * Comprueba si el blanco corresponde a cualquier Easter Egg terrestre (Torre de Control o Vehículo de Pista).
     */
    public static boolean esEasterEggTerrestre(String tipoIcao, String callsign, String matricula, String descripcion) {
        // Combina la detección de torre de control y de vehículo terrestre
        return esEasterEggTorre(tipoIcao, callsign, matricula, descripcion)
                || esEasterEggVehiculo(tipoIcao, callsign, matricula, descripcion);
    }

    /**
     * Crea la instancia localizada del Easter Egg para vehículos terrestres aeroportuarios con su foto oficial.
     */
    public static Avion crearEasterEggVehiculo(Context appContext, String icao24, String tipoLimpio) {
        // Construye el modelo especial EASTER_EGG con textos localizados y fotografía de coche Follow-Me
        String codigoTipo = (tipoLimpio != null && !tipoLimpio.isEmpty()) ? tipoLimpio : "OPS";
        Avion easterEgg = new Avion(
                appContext.getString(R.string.easter_egg_vehiculo_modelo),
                appContext.getString(R.string.easter_egg_vehiculo_fabricante),
                "EASTER_EGG",
                SIL_CARGA,
                "60 km/h",
                appContext.getString(R.string.easter_egg_vehiculo_capacidad),
                "5m",
                appContext.getString(R.string.easter_egg_vehiculo_pais),
                "2t",
                icao24
        );
        easterEgg.setTipoIcao(codigoTipo);
        easterEgg.setDescripcion(appContext.getString(R.string.easter_egg_vehiculo_descripcion));
        easterEgg.setUrlImagenRemota(URL_FOTO_EASTER_EGG_VEHICULO);
        easterEgg.setImagenUrl(URL_FOTO_EASTER_EGG_VEHICULO);
        return easterEgg;
    }

    /**
     * Crea la instancia localizada del Easter Egg para Torres de Control / balizas fijas TWR con su foto oficial.
     */
    public static Avion crearEasterEggTorre(Context appContext, String icao24, String tipoLimpio) {
        // Construye el modelo especial EASTER_EGG con textos localizados y fotografía de torre de control
        String codigoTipo = (tipoLimpio != null && !tipoLimpio.isEmpty()) ? tipoLimpio : "TWR";
        Avion torre = new Avion(
                appContext.getString(R.string.easter_egg_torre_modelo),
                appContext.getString(R.string.easter_egg_torre_fabricante),
                "EASTER_EGG",
                SIL_CARGA,
                "0 km/h",
                "ATC",
                "45m",
                appContext.getString(R.string.easter_egg_vehiculo_pais),
                "—",
                icao24
        );
        torre.setTipoIcao(codigoTipo);
        torre.setDescripcion(appContext.getString(R.string.easter_egg_torre_desc));
        torre.setUrlImagenRemota(URL_FOTO_EASTER_EGG_TORRE);
        torre.setImagenUrl(URL_FOTO_EASTER_EGG_TORRE);
        return torre;
    }

    /**
     * Devuelve la silueta correspondiente a partir del nombre del modelo o tipo ICAO.
     */
    public static int resolverImagenPorModelo(String modelo, String tipoIcao) {
        // Busca primero coincidencia directa por código ICAO y después por nombre comercial
        if (tipoIcao != null && !tipoIcao.trim().isEmpty()) {
            AvionData data = MAPA_POR_ICAO.get(tipoIcao.trim().toUpperCase(Locale.ROOT));
            if (data != null) return data.siluetaId;
        }
        if (modelo != null && !modelo.trim().isEmpty()) {
            AvionData data = buscarEnCatalogoPorTexto(modelo);
            if (data != null) return data.siluetaId;
        }
        // Si no hay coincidencia, devuelve la silueta genérica de reactor comercial
        return SIL_JET;
    }

    /**
     * Comprueba si un nombre de modelo corresponde a un avión desconocido en cualquier idioma.
     */
    public static boolean esModeloDesconocido(Context context, String modelo) {
        // Evalúa cadenas nulas, guiones o etiquetas localizadas de aeronave desconocida
        if (modelo == null || modelo.trim().isEmpty()) return true;
        String m = modelo.trim();
        String desconocidoLocalizado = context.getString(R.string.aeronave_desconocida);
        return m.equalsIgnoreCase(desconocidoLocalizado)
                || m.equalsIgnoreCase("Unknown Aircraft")
                || m.equalsIgnoreCase("Aeronave desconocida")
                || m.equals("—");
    }

    /**
     * Limpia prefijos genéricos, paréntesis y corchetes del modelo para mejorar la búsqueda en Wikipedia.
     */
    public static String limpiarModeloParaWiki(Context context, String modelo) {
        // Elimina palabras genéricas como "Aeronave ICAO" y signos de agrupación
        if (esModeloDesconocido(context, modelo)) return "";
        String formatoGenerico = context.getString(R.string.aeronave_generica, "");
        return modelo.replace(formatoGenerico, "")
                .replaceAll("(?i)^(aeronave|avión|aircraft)\\s+(icao\\s+)?", "")
                .replaceAll("[()\\[\\]]", "")
                .trim();
    }

    /**
     * Determina la silueta adecuada cuando una aeronave genérica no está en el catálogo local.
     */
    private static int determinarSiluetaPorCategoriaRadar(String categoriaRadar) {
        // Asocia la categoría textual reportada por el radar con su icono vectorial más cercano
        if (categoriaRadar == null) return SIL_JET;
        String cat = categoriaRadar.toLowerCase(Locale.ROOT);
        if (cat.contains("liger") || cat.contains("light") || cat.contains("small")) return SIL_LIGERO;
        if (cat.contains("heli") || cat.contains("rotor")) return SIL_HELICOPTERO;
        if (cat.contains("carga") || cat.contains("heavy") || cat.contains("cargo")) return SIL_CARGA;
        if (cat.contains("turbo") || cat.contains("prop")) return SIL_TURBOHELICE;
        if (cat.contains("caza") || cat.contains("fighter") || cat.contains("mil")) return SIL_CAZA;
        return SIL_JET;
    }

    /**
     * Obtiene el idioma activo de la aplicación ("es" o "en") para las consultas de Wikipedia.
     */
    public static String obtenerCodigoIdiomaWiki() {
        // Consulta el locale configurado en AppCompatDelegate o el del sistema por defecto
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String lang = (!appLocales.isEmpty() && appLocales.get(0) != null)
                ? appLocales.get(0).getLanguage()
                : Locale.getDefault().getLanguage();
        return "es".equalsIgnoreCase(lang) ? "es" : "en";
    }

    /**
     * Resuelve de forma asíncrona el código de tipo ICAO y su nombre de modelo a partir del ICAO24.
     */
    public static void resolverTipoIcaoAsync(Context context, String icao24, OnTipoIcaoResueltoListener listener) {
        // Valida parámetros de entrada antes de lanzar tarea en segundo plano
        if (listener == null || icao24 == null || icao24.trim().isEmpty()) return;
        final Context appContext = context.getApplicationContext();

        NETWORK_EXECUTOR.execute(() -> {
            // Consulta caché o APIs remotas para obtener el designador ICAO a partir del hex
            String recuperado = obtenerTipoIcaoDesdeHexDbCacheado(appContext, icao24);
            if (recuperado != null && !recuperado.isEmpty()) {
                String upper = recuperado.trim().toUpperCase(Locale.ROOT);
                // Cruza el resultado con el catálogo local para obtener el nombre comercial completo
                AvionData data = MAPA_POR_ICAO.get(upper);
                if (data == null) {
                    data = buscarEnCatalogoPorTexto(recuperado);
                }

                String tipoFinal = (upper.length() <= 4 && upper.matches("[A-Z0-9]{2,4}"))
                        ? upper
                        : (data != null ? data.codigoIcaoPrincipal : "");
                String modeloFinal = (data != null)
                        ? data.modelo
                        : (!tipoFinal.isEmpty() ? appContext.getString(R.string.aeronave_generica, tipoFinal) : recuperado.trim());

                // Entrega el resultado al hilo principal
                MAIN_HANDLER.post(() -> listener.onTipoIcaoResuelto(tipoFinal, modeloFinal));
            }
        });
    }

    /**
     * Sobrecarga compatible que delega pasando callsign nulo.
     */
    public static void resolverAvionGlobalAsync(Context context, String icao24, String matricula, String tipoIcao,
                                                String modeloRadarFallback, String categoriaRadar,
                                                OnAvionResueltoListener listener) {
        // Invoca la versión completa pasando null como indicativo de llamada
        resolverAvionGlobalAsync(context, icao24, null, matricula, tipoIcao, modeloRadarFallback, categoriaRadar, listener);
    }

    /**
     * Resuelve de forma asíncrona una aeronave cruzando la capa local, respaldos remotos,
     * un generador dinámico universal localizado y detección diferenciada de Easter Eggs terrestres.
     */
    public static void resolverAvionGlobalAsync(Context context, String icao24, String callsign, String matricula,
                                                String tipoIcao, String modeloRadarFallback, String categoriaRadar,
                                                OnAvionResueltoListener listener) {
        // Evita trabajar si no hay receptor para el resultado
        if (listener == null) return;
        final Context appContext = context.getApplicationContext();

        final String tipoLimpio = (tipoIcao != null) ? tipoIcao.trim().toUpperCase(Locale.ROOT) : "";

        // Comprobamos primero si es una Torre de Control (TWR)
        if (esEasterEggTorre(tipoLimpio, callsign, matricula, modeloRadarFallback)) {
            Avion torre = crearEasterEggTorre(appContext, icao24, "TWR");
            MAIN_HANDLER.post(() -> listener.onAvionResuelto(torre));
            return;
        }

        // Comprobamos si es un vehículo terrestre de operaciones en pista (Follow-Me, Ops, GND, etc.)
        if (esEasterEggVehiculo(tipoLimpio, callsign, matricula, modeloRadarFallback)) {
            String tipoEntidad = !tipoLimpio.isEmpty()
                    ? tipoLimpio
                    : (callsign != null && !callsign.trim().isEmpty() ? callsign.trim().toUpperCase(Locale.ROOT) : "OPS");
            Avion easterEgg = crearEasterEggVehiculo(appContext, icao24, tipoEntidad);
            MAIN_HANDLER.post(() -> listener.onAvionResuelto(easterEgg));
            return;
        }

        final String matriculaLimpia = (matricula != null) ? matricula.trim() : "";

        // 1. Capa local maestra: Buscamos primero en el catálogo estático por tipo ICAO o texto de respaldo
        AvionData dataLocal = null;
        if (!tipoLimpio.isEmpty()) {
            dataLocal = MAPA_POR_ICAO.get(tipoLimpio);
        }
        if (dataLocal == null && modeloRadarFallback != null && !esModeloDesconocido(appContext, modeloRadarFallback)) {
            dataLocal = buscarEnCatalogoPorTexto(modeloRadarFallback);
        }

        // Respaldo adicional utilizando el indicativo de llamada (callsign) o la matrícula
        if (dataLocal == null) {
            String icaoRespaldo = deducirIcaoPorCallsignOMatricula(callsign, matriculaLimpia);
            if (icaoRespaldo != null) {
                dataLocal = MAPA_POR_ICAO.get(icaoRespaldo);
            }
        }

        final Avion avionBase;
        final String terminoBusquedaWiki;

        if (dataLocal != null) {
            // Si el avión existe en nuestro catálogo maestro, utilizamos sus datos verificados
            avionBase = crearAvionDesdeData(dataLocal);
            terminoBusquedaWiki = dataLocal.modelo;
        } else {
            // GENERADOR DINÁMICO UNIVERSAL: Si el modelo no está registrado, construimos una ficha localizada al vuelo
            String codigoGenerico = !tipoLimpio.isEmpty()
                    ? tipoLimpio
                    : (!esModeloDesconocido(appContext, modeloRadarFallback) ? modeloRadarFallback.trim() : "");

            String nombreModeloDinamico;
            int siluetaDinamica = determinarSiluetaPorCategoriaRadar(categoriaRadar);

            // Construye un nombre descriptivo según los identificadores disponibles
            if (!codigoGenerico.isEmpty()) {
                nombreModeloDinamico = appContext.getString(R.string.aeronave_generica, codigoGenerico);
            } else if (!matriculaLimpia.isEmpty()) {
                nombreModeloDinamico = appContext.getString(R.string.aeronave_matricula_generica, matriculaLimpia);
            } else if (icao24 != null && !icao24.trim().isEmpty()) {
                nombreModeloDinamico = appContext.getString(R.string.aeronave_hex_generica, icao24.trim().toUpperCase(Locale.ROOT));
            } else {
                nombreModeloDinamico = appContext.getString(R.string.aeronave_desconocida);
            }

            // Deduce país por bloque hexadecimal ICAO y fabricante si figura en el nombre
            String paisHex = deducirPaisPorHexIcao(icao24);
            String fabricanteDinamico = deducirFabricante(modeloRadarFallback);
            if ("—".equals(fabricanteDinamico)) {
                fabricanteDinamico = appContext.getString(R.string.fabricante_aviacion_general);
            }

            // Si no hay categoría de radar explícita y es un hex de aviación general (ej. FAA), usamos silueta ligera por defecto
            if ((categoriaRadar == null || categoriaRadar.trim().isEmpty()) && "EE.UU. (FAA)".equals(paisHex)) {
                siluetaDinamica = SIL_LIGERO;
            }

            // Instanciamos el objeto con valores dinámicos universales listos para enriquecer
            avionBase = new Avion(
                    nombreModeloDinamico,
                    fabricanteDinamico,
                    "COMMON",
                    siluetaDinamica,
                    "—",
                    "—",
                    "—",
                    paisHex.isEmpty() ? "—" : paisHex,
                    "—"
            );

            // Asignamos una descripción base limpia para que nunca quede un guion vacío en pantalla
            String hexMostrado = (icao24 != null && !icao24.trim().isEmpty()) ? icao24.trim().toUpperCase(Locale.ROOT) : "N/A";
            avionBase.setDescripcion(appContext.getString(R.string.descripcion_transpondedor_anonimo, hexMostrado));

            terminoBusquedaWiki = !codigoGenerico.isEmpty() ? codigoGenerico : modeloRadarFallback;
        }

        // Asignación de identificadores de seguimiento base
        if (!tipoLimpio.isEmpty()) avionBase.setTipoIcao(tipoLimpio);
        else if (dataLocal != null && dataLocal.codigoIcaoPrincipal != null) {
            avionBase.setTipoIcao(dataLocal.codigoIcaoPrincipal);
        }
        if (icao24 != null) avionBase.setIcao24(icao24);
        if (!matriculaLimpia.isEmpty()) avionBase.setMatricula(matriculaLimpia);

        // 2. Capa remota asíncrona: consulta en servidores externos y servicios de caché de red
        NETWORK_EXECUTOR.execute(() -> {
            String tipoEfectivo = avionBase.getTipoIcao() != null ? avionBase.getTipoIcao() : tipoLimpio;
            String terminoEfectivo = terminoBusquedaWiki;

            // Si aún desconocemos el tipo exacto de aeronave, interrogamos las bases de datos remotas por ICAO24/matrícula
            if ((tipoEfectivo.isEmpty() || esModeloDesconocido(appContext, avionBase.getModelo()))
                    && (icao24 != null || !matriculaLimpia.isEmpty())) {

                String recuperado = obtenerTipoIcaoDesdeHexYMatriculaCacheado(appContext, icao24, matriculaLimpia);

                // RESPALDO INTELIGENTE: Si las APIs externas no devuelven nada pero es una matrícula "N"
                // de aviación general, asignamos un tipo base común para evitar tarjetas vacías.
                if (recuperado == null && matriculaLimpia.toUpperCase(Locale.ROOT).startsWith("N")) {
                    recuperado = "C172";
                }

                if (recuperado != null) {
                    String recuperadoUpper = recuperado.trim().toUpperCase(Locale.ROOT);
                    // Reevaluamos si el dato remoto revela que se trataba de un Easter Egg terrestre
                    if (esEasterEggTorre(recuperadoUpper, callsign, matriculaLimpia, recuperado)) {
                        Avion torre = crearEasterEggTorre(appContext, icao24, "TWR");
                        MAIN_HANDLER.post(() -> listener.onAvionResuelto(torre));
                        return;
                    }
                    if (esEasterEggVehiculo(recuperadoUpper, callsign, matriculaLimpia, recuperado)) {
                        Avion easterEgg = crearEasterEggVehiculo(appContext, icao24, recuperadoUpper);
                        MAIN_HANDLER.post(() -> listener.onAvionResuelto(easterEgg));
                        return;
                    }

                    // Intentamos mapear el tipo recuperado contra nuestro catálogo técnico verificado
                    AvionData dataRecuperada = MAPA_POR_ICAO.get(recuperadoUpper);
                    if (dataRecuperada == null) {
                        dataRecuperada = buscarEnCatalogoPorTexto(recuperado);
                    }

                    if (dataRecuperada != null) {
                        // Actualiza el objeto con todos los datos técnicos verificados del catálogo
                        tipoEfectivo = (recuperadoUpper.length() <= 4 && recuperadoUpper.matches("[A-Z0-9]{2,4}"))
                                ? recuperadoUpper
                                : dataRecuperada.codigoIcaoPrincipal;
                        if (!tipoEfectivo.isEmpty()) avionBase.setTipoIcao(tipoEfectivo);
                        avionBase.setModelo(dataRecuperada.modelo);
                        avionBase.setFabricante(dataRecuperada.fabricante);
                        avionBase.setRareza(dataRecuperada.rareza);
                        avionBase.setImagenResId(dataRecuperada.siluetaId);
                        avionBase.setVelocidad(dataRecuperada.vel);
                        avionBase.setPasajeros(dataRecuperada.cap);
                        avionBase.setDimensiones(dataRecuperada.dim);
                        avionBase.setPais(dataRecuperada.pais);
                        avionBase.setPeso(dataRecuperada.peso);
                        terminoEfectivo = dataRecuperada.modelo;
                    } else if (recuperadoUpper.length() <= 4 && recuperadoUpper.matches("[A-Z0-9]{2,4}")) {
                        // Si es un código ICAO válido fuera del catálogo, actualiza el identificador genérico
                        tipoEfectivo = recuperadoUpper;
                        avionBase.setTipoIcao(tipoEfectivo);
                        avionBase.setModelo(appContext.getString(R.string.aeronave_generica, tipoEfectivo));
                        terminoEfectivo = tipoEfectivo;
                    } else {
                        // Si la API devolvió directamente el nombre comercial, lo formatea y deduce fabricante
                        String modeloLimpio = formatearTextoModelo(recuperado.trim());
                        avionBase.setModelo(modeloLimpio);
                        String fab = deducirFabricante(modeloLimpio);
                        if (!"—".equals(fab)) {
                            avionBase.setFabricante(fab);
                        }
                        terminoEfectivo = modeloLimpio;
                    }
                }
            }

            // Si se resolvió correctamente un término de búsqueda válido, consultamos Wikipedia para obtener foto y descripción
            if (terminoEfectivo != null && !terminoEfectivo.trim().isEmpty() && !esModeloDesconocido(appContext, avionBase.getModelo())) {
                final String claveCacheWiki = obtenerCodigoIdiomaWiki() + "_"
                        + (!tipoEfectivo.isEmpty() ? tipoEfectivo : normalizarNombre(terminoEfectivo));

                String[] resultadoWiki = obtenerInfoCacheadaORemota(
                        appContext,
                        claveCacheWiki,
                        terminoEfectivo,
                        avionBase.getFabricante(),
                        avionBase.getModelo()
                );

                avionBase.setDescripcion(resultadoWiki[0]);
                if (resultadoWiki[1] != null && !resultadoWiki[1].isEmpty()) {
                    avionBase.setUrlImagenRemota(resultadoWiki[1]);
                    avionBase.setImagenUrl(resultadoWiki[1]);
                }
            }

            // Entrega el objeto final enriquecido al hilo principal de interfaz
            MAIN_HANDLER.post(() -> listener.onAvionResuelto(avionBase));
        });
    }

    /**
     * Deduce el tipo ICAO predeterminado de la aeronave analizando el prefijo del
     * indicativo de llamada (callsign) o el sufijo de la matrícula.
     */
    private static String deducirIcaoPorCallsignOMatricula(String callsign, String matricula) {
        // Asocia prefijos de aerolíneas comerciales frecuentes con su modelo de flota principal
        if (callsign != null && !callsign.trim().isEmpty()) {
            String cs = callsign.trim().toUpperCase(Locale.ROOT);

            // --- ASIA Y OCEANÍA ---
            if (cs.startsWith("FM") || cs.startsWith("CSH")) return "B738";
            if (cs.startsWith("VJA") || cs.startsWith("VJC")) return "A321";
            if (cs.startsWith("CCA") || cs.startsWith("CHG")) return "A320";
            if (cs.startsWith("CES") || cs.startsWith("CKK")) return "A320";
            if (cs.startsWith("CSN") || cs.startsWith("CBJ")) return "B738";
            if (cs.startsWith("ANA") || cs.startsWith("AHX")) return "B789";
            if (cs.startsWith("JAL") || cs.startsWith("JNA")) return "B789";
            if (cs.startsWith("CPA") || cs.startsWith("THY")) return "B77W";
            if (cs.startsWith("QFA") || cs.startsWith("QTR")) return "B738";

            // --- EUROPA ---
            if (cs.startsWith("IBE") || cs.startsWith("ANE")) return "A320";
            if (cs.startsWith("VLG")) return "A320";
            if (cs.startsWith("AEA")) return "B738";
            if (cs.startsWith("RYR") || cs.startsWith("RYC")) return "B738";
            if (cs.startsWith("DLH") || cs.startsWith("GEC")) return "A320";
            if (cs.startsWith("AFR") || cs.startsWith("VRA")) return "A320";
            if (cs.startsWith("BAW") || cs.startsWith("SHT")) return "A320";
            if (cs.startsWith("EZY") || cs.startsWith("EZS")) return "A320";
            if (cs.startsWith("KLM")) return "B738";
            if (cs.startsWith("TAP") || cs.startsWith("PGA")) return "A320";

            // --- AMÉRICA DEL NORTE ---
            if (cs.startsWith("AAL")) return "B738";
            if (cs.startsWith("DAL")) return "B738";
            if (cs.startsWith("UAL")) return "B738";
            if (cs.startsWith("SWA")) return "B38M";
            if (cs.startsWith("ACA")) return "A320";
            if (cs.startsWith("JBU")) return "A320";

            // --- ORIENTE MEDIO Y ÁFRICA ---
            if (cs.startsWith("UAE") || cs.startsWith("VRD")) return "A388";
            if (cs.startsWith("QTR")) return "B77W";
            if (cs.startsWith("ETD")) return "B789";
            if (cs.startsWith("THY")) return "B738";
            if (cs.startsWith("RAM")) return "B738";
        }

        // Comprueba sufijos de matrícula característicos de flotas regionales (ej. Horizon Air "QX")
        if (matricula != null && !matricula.trim().isEmpty()) {
            String reg = matricula.trim().toUpperCase(Locale.ROOT);
            if (reg.endsWith("QX")) {
                return "E75L";
            }
        }

        return null;
    }

    /**
     * Busca una ficha en el catálogo local por coincidencia exacta o traduciendo
     * variantes comerciales devueltas por las APIs.
     */
    private static AvionData buscarEnCatalogoPorTexto(String texto) {
        // Primero intenta coincidencia directa por nombre normalizado
        if (texto == null || texto.trim().isEmpty()) return null;
        AvionData exacta = MAPA_POR_NOMBRE_NORMALIZADO.get(normalizarNombre(texto));
        if (exacta != null) return exacta;

        // Si no coincide exactamente, deduce el código ICAO estándar a partir del texto
        String icaoDeducido = deducirCodigoIcaoPorTexto(texto);
        if (icaoDeducido != null) {
            return MAPA_POR_ICAO.get(icaoDeducido);
        }
        return null;
    }

    /**
     * Traduce designaciones comerciales o de certificación FAA a su código ICAO estándar.
     */
    private static String deducirCodigoIcaoPorTexto(String texto) {
        // Normaliza espacios y mayúsculas para evaluar subvariantes de modelos comerciales
        if (texto == null) return null;
        String u = texto.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();

        // Familia Embraer E-Jets y ERJ
        if (u.contains("170-200") || u.contains("170200") || u.contains("E175") || u.contains("EMB-175") || u.contains("EMB 175") || u.contains("ERJ-175") || u.contains("ERJ 175")) {
            return "E75L";
        }
        if (u.contains("170-100") || u.contains("170100") || u.contains("E170") || u.contains("EMB-170") || u.contains("EMB 170") || u.contains("ERJ-170") || u.contains("ERJ 170")) {
            return "E170";
        }
        if (u.contains("190-200") || u.contains("190200") || u.contains("E195") || u.contains("EMB-195") || u.contains("EMB 195") || u.contains("ERJ-195") || u.contains("ERJ 195")) {
            return u.contains("E2") ? "E295" : "E195";
        }
        if (u.contains("190-100") || u.contains("190100") || u.contains("E190") || u.contains("EMB-190") || u.contains("EMB 190") || u.contains("ERJ-190") || u.contains("ERJ 190")) {
            return u.contains("E2") ? "E290" : "E190";
        }
        if (u.contains("145") && (u.contains("EMB") || u.contains("ERJ"))) return "E145";
        if (u.contains("135") && (u.contains("EMB") || u.contains("ERJ"))) return "E135";

        // Familia Boeing 737 Classic, NG y MAX
        if (u.contains("737")) {
            if (u.contains("MAX 7") || u.contains("737-7")) return "B37M";
            if (u.contains("MAX 8") || u.contains("737-8")) return "B38M";
            if (u.contains("MAX 9") || u.contains("737-9")) return "B39M";
            if (u.contains("MAX 10") || u.contains("737-10")) return "B3XM";
            if (u.matches(".*737[-\\s]?8[0-9A-Z]{2}.*") || u.contains("737-800")) return "B738";
            if (u.matches(".*737[-\\s]?7[0-9A-Z]{2}.*") || u.contains("737-700")) return "B737";
            if (u.matches(".*737[-\\s]?9[0-9A-Z]{2}.*") || u.contains("737-900")) return "B739";
            if (u.matches(".*737[-\\s]?3[0-9A-Z]{2}.*")) return "B733";
            if (u.matches(".*737[-\\s]?4[0-9A-Z]{2}.*")) return "B734";
            if (u.matches(".*737[-\\s]?5[0-9A-Z]{2}.*")) return "B735";
            return "B738";
        }

        // Familia Airbus A320ceo/neo y A220
        if (u.contains("A320") || u.contains("320-2")) return u.contains("NEO") || u.contains("251N") || u.contains("271N") ? "A20N" : "A320";
        if (u.contains("A321") || u.contains("321-2")) return u.contains("NEO") || u.contains("251N") || u.contains("271N") ? "A21N" : "A321";
        if (u.contains("A319") || u.contains("319-1")) return u.contains("NEO") ? "A19N" : "A319";
        if (u.contains("A220-300") || u.contains("BD-500-1A11") || u.contains("CS300")) return "BCS3";
        if (u.contains("A220-100") || u.contains("BD-500-1A10") || u.contains("CS100")) return "BCS1";

        // Reactores de fuselaje ancho Boeing y Airbus
        if (u.contains("787-8")) return "B788";
        if (u.contains("787-9")) return "B789";
        if (u.contains("787-10")) return "B78X";
        if (u.contains("777-3") || u.contains("777-300")) return "B77W";
        if (u.contains("777-2") || u.contains("777-200")) return "B772";
        if (u.contains("767-3")) return "B763";
        if (u.contains("757-2")) return "B752";
        if (u.contains("747-4")) return "B744";
        if (u.contains("747-8")) return "B748";
        if (u.contains("A350-9") || u.contains("A350-900")) return "A359";
        if (u.contains("A350-10") || u.contains("A350-1000")) return "A35K";
        if (u.contains("A330-2")) return "A332";
        if (u.contains("A330-3")) return "A333";
        if (u.contains("A330-9")) return "A339";
        if (u.contains("A380")) return "A388";

        // Regionales, turbohélices y aviación ligera frecuentes
        if (u.contains("CRJ-900") || u.contains("CL-600-2D24")) return "CRJ9";
        if (u.contains("CRJ-700") || u.contains("CL-600-2C10")) return "CRJ7";
        if (u.contains("CRJ-200") || u.contains("CL-600-2B19")) return "CRJ2";
        if (u.contains("Q400") || u.contains("DHC-8-402")) return "DH8D";
        if (u.contains("ATR 72") || u.contains("ATR-72")) return "AT76";
        if (u.contains("ATR 42") || u.contains("ATR-42")) return "AT45";
        if (u.contains("172")) return "C172";
        if (u.contains("SLING 4")) return "SLG4";
        if (u.contains("SLING 2")) return "SLG2";
        if (u.contains("HUSKY")) return "HUSK";

        return null;
    }

    /**
     * Completa el nombre del fabricante en códigos abreviados de Boeing o Airbus.
     */
    private static String formatearTextoModelo(String modelo) {
        // Antepone "Boeing " o "Airbus " si la cadena empieza directamente por su código numérico
        if (modelo == null) return "";
        if (modelo.matches("^7[0-8]7.*")) return "Boeing " + modelo;
        if (modelo.matches("^A3[0-8]\\d.*")) return "Airbus " + modelo;
        return modelo;
    }

    /**
     * Obtiene el tipo ICAO o modelo desde memoria, disco o APIs externas usando ICAO24 y matrícula.
     * Valida automáticamente la versión de la aplicación y la caducidad temporal (TTL).
     */
    private static String obtenerTipoIcaoDesdeHexYMatriculaCacheado(Context appContext, String icao24, String matricula) {
        // Normaliza las claves de búsqueda para RAM y SharedPreferences
        String claveHex = (icao24 != null && !icao24.trim().isEmpty())
                ? icao24.trim().toLowerCase(Locale.ROOT)
                : "";
        String claveReg = (matricula != null && !matricula.trim().isEmpty())
                ? "reg_" + matricula.trim().toLowerCase(Locale.ROOT)
                : "";

        // Abre las preferencias de caché y purga si la app fue actualizada
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_HEX_CACHE, Context.MODE_PRIVATE);
        validarYLimpiarCacheSiAppActualizada(appContext, prefs, CACHE_MEMORIA_HEX);

        // 1. Consulta en RAM y disco por código hexadecimal ICAO24
        if (!claveHex.isEmpty()) {
            String memHex = desempaquetarSiVigente(CACHE_MEMORIA_HEX.get(claveHex));
            if (memHex != null) return memHex;

            String diskHex = desempaquetarSiVigente(prefs.getString(claveHex, null));
            if (diskHex != null) {
                CACHE_MEMORIA_HEX.put(claveHex, empaquetarConTtl(diskHex));
                return diskHex;
            }
        }

        // 2. Consulta en RAM y disco por matrícula
        if (!claveReg.isEmpty()) {
            String memReg = desempaquetarSiVigente(CACHE_MEMORIA_HEX.get(claveReg));
            if (memReg != null) return memReg;

            String diskReg = desempaquetarSiVigente(prefs.getString(claveReg, null));
            if (diskReg != null) {
                CACHE_MEMORIA_HEX.put(claveReg, empaquetarConTtl(diskReg));
                return diskReg;
            }
        }

        // 3. Consulta en cascada en servidores remotos si no había copia vigente en caché
        String tipoRemoto = null;
        if (!claveHex.isEmpty()) {
            tipoRemoto = consultarTipoIcaoRemoto(claveHex);
        }

        if (tipoRemoto == null && matricula != null && !matricula.trim().isEmpty()) {
            tipoRemoto = consultarTipoIcaoPorMatriculaRemoto(matricula);
        }

        if (tipoRemoto == null) {
            tipoRemoto = consultarPlanespottersFallback(claveHex, matricula);
        }

        // 4. Guarda el resultado resuelto con TTL en RAM y en disco
        if (tipoRemoto != null) {
            String registroConTtl = empaquetarConTtl(tipoRemoto);
            SharedPreferences.Editor editor = prefs.edit();
            if (!claveHex.isEmpty()) {
                CACHE_MEMORIA_HEX.put(claveHex, registroConTtl);
                editor.putString(claveHex, registroConTtl);
            }
            if (!claveReg.isEmpty()) {
                CACHE_MEMORIA_HEX.put(claveReg, registroConTtl);
                editor.putString(claveReg, registroConTtl);
            }
            editor.apply();
            return tipoRemoto;
        }
        return null;
    }

    /**
     * Consulta el tipo de aeronave en servidores remotos utilizando la matrícula del aparato.
     */
    private static String consultarTipoIcaoPorMatriculaRemoto(String matricula) {
        // Normaliza la matrícula en mayúsculas
        String regLimpia = matricula.trim().toUpperCase(Locale.ROOT);

        // Primer intento: API pública ADSBDB por matrícula
        try {
            String urlAdsbReg = "https://api.adsbdb.com/v0/aircraft/" + URLEncoder.encode(regLimpia, "UTF-8");
            String respAdsb = peticionHttpGet(urlAdsbReg);
            String extraido = extraerTipoDeJsonAdsbdb(respAdsb);
            if (extraido != null) return extraido;
        } catch (Exception ignored) {
        }

        // Segundo intento: API pública adsb.lol por matrícula
        try {
            String urlLolReg = "https://api.adsb.lol/v2/reg/" + URLEncoder.encode(regLimpia, "UTF-8");
            String respLol = peticionHttpGet(urlLolReg);
            String extraido = extraerTipoDeJsonAdsbLol(respLol);
            if (extraido != null) return extraido;
        } catch (Exception ignored) {
        }

        // Tercer intento: Traducción matrícula -> hex en HexDB y posterior consulta de ficha
        try {
            String urlHexReg = "https://hexdb.io/api/v1/reg/icao/" + URLEncoder.encode(regLimpia, "UTF-8");
            String hexDesdeReg = peticionHttpGet(urlHexReg);
            if (hexDesdeReg != null) {
                String hexLimpio = hexDesdeReg.replace("\"", "").trim().toLowerCase(Locale.ROOT);
                if (hexLimpio.matches("[0-9a-f]{6}")) {
                    String extraido = consultarHexDbPorIcao(hexLimpio);
                    if (extraido != null) return extraido;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Recupera el tipo ICAO desde caché o red usando exclusivamente el código ICAO24.
     */
    private static String obtenerTipoIcaoDesdeHexDbCacheado(Context appContext, String icao24) {
        // Normaliza el código hexadecimal
        String hexLimpio = icao24.trim().toLowerCase(Locale.ROOT);
        if (hexLimpio.isEmpty()) return null;

        // Comprueba validez de versión de app en caché
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_HEX_CACHE, Context.MODE_PRIVATE);
        validarYLimpiarCacheSiAppActualizada(appContext, prefs, CACHE_MEMORIA_HEX);

        // Busca en caché en memoria RAM
        String cacheadoMemoria = desempaquetarSiVigente(CACHE_MEMORIA_HEX.get(hexLimpio));
        if (cacheadoMemoria != null) {
            return cacheadoMemoria;
        }

        // Busca en caché persistente en disco
        String cacheadoDisco = desempaquetarSiVigente(prefs.getString(hexLimpio, null));
        if (cacheadoDisco != null) {
            CACHE_MEMORIA_HEX.put(hexLimpio, empaquetarConTtl(cacheadoDisco));
            return cacheadoDisco;
        }

        // Consulta en red y almacena el resultado vigente en RAM y disco
        String tipoRemoto = consultarTipoIcaoRemoto(hexLimpio);

        if (tipoRemoto != null) {
            String registroConTtl = empaquetarConTtl(tipoRemoto);
            CACHE_MEMORIA_HEX.put(hexLimpio, registroConTtl);
            prefs.edit().putString(hexLimpio, registroConTtl).apply();
            return tipoRemoto;
        }
        return null;
    }

    /**
     * Consulta en cascada ADSBDB, HexDB, adsb.lol y OpenSky para identificar el modelo por hex.
     */
    private static String consultarTipoIcaoRemoto(String hexLimpio) {
        // 1. Consulta en ADSBDB
        try {
            String urlAdsb = "https://api.adsbdb.com/v0/aircraft/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respAdsb = peticionHttpGet(urlAdsb);
            String extraido = extraerTipoDeJsonAdsbdb(respAdsb);
            if (extraido != null) return extraido;
        } catch (Exception ignored) {
        }

        // 2. Consulta en HexDB
        String extraidoHexDb = consultarHexDbPorIcao(hexLimpio);
        if (extraidoHexDb != null) return extraidoHexDb;

        // 3. Consulta en adsb.lol
        try {
            String urlAdsbLol = "https://api.adsb.lol/v2/icao/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respLol = peticionHttpGet(urlAdsbLol);
            String extraido = extraerTipoDeJsonAdsbLol(respLol);
            if (extraido != null) return extraido;
        } catch (Exception ignored) {
        }

        // 4. Consulta en metadatos de OpenSky Network
        try {
            String urlOpenSky = "https://opensky-network.org/api/metadata/aircraft/icao/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respOs = peticionHttpGet(urlOpenSky);
            if (respOs != null) {
                JSONObject json = new JSONObject(respOs);
                String typecode = json.optString("typecode", "").trim().toUpperCase(Locale.ROOT);
                if (!typecode.isEmpty() && !"NULL".equals(typecode)) {
                    return typecode;
                }
                String fabricante = json.optString("manufacturerName", "").trim();
                String model = json.optString("model", "").trim();
                if (!model.isEmpty() && !"null".equalsIgnoreCase(model)) {
                    return (!fabricante.isEmpty() && !model.toLowerCase(Locale.ROOT).contains(fabricante.toLowerCase(Locale.ROOT)))
                            ? fabricante + " " + model
                            : model;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Consulta la base de datos pública HexDB por código hexadecimal ICAO24.
     */
    private static String consultarHexDbPorIcao(String hexLimpio) {
        // Realiza petición GET a HexDB y extrae ICAOTypeCode o Manufacturer + Type
        try {
            String urlHex = "https://hexdb.io/api/v1/aircraft/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respHex = peticionHttpGet(urlHex);
            if (respHex != null) {
                JSONObject json = new JSONObject(respHex);
                String tipo = json.optString("ICAOTypeCode", "").trim().toUpperCase(Locale.ROOT);
                if (!tipo.isEmpty() && !"NULL".equals(tipo)) {
                    return tipo;
                }
                String fabricante = json.optString("Manufacturer", "").trim();
                String typeName = json.optString("Type", "").trim();
                if (!typeName.isEmpty() && !"null".equalsIgnoreCase(typeName)) {
                    return (!fabricante.isEmpty() && !typeName.toLowerCase(Locale.ROOT).contains(fabricante.toLowerCase(Locale.ROOT)))
                            ? fabricante + " " + typeName
                            : typeName;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Extrae el código ICAO o nombre comercial desde la respuesta JSON de ADSBDB.
     */
    private static String extraerTipoDeJsonAdsbdb(String respJson) {
        // Navega la estructura response -> aircraft buscando icao_type o manufacturer + type
        if (respJson == null || respJson.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(respJson);
            JSONObject response = root.optJSONObject("response");
            if (response == null) return null;
            JSONObject aircraft = response.optJSONObject("aircraft");
            if (aircraft == null) return null;

            String icaoType = aircraft.optString("icao_type", "").trim().toUpperCase(Locale.ROOT);
            if (!icaoType.isEmpty() && !"NULL".equals(icaoType)) {
                return icaoType;
            }
            String fabricante = aircraft.optString("manufacturer", "").trim();
            String tipoTexto = aircraft.optString("type", "").trim();
            if (!tipoTexto.isEmpty() && !"null".equalsIgnoreCase(tipoTexto)) {
                return (!fabricante.isEmpty() && !tipoTexto.toLowerCase(Locale.ROOT).contains(fabricante.toLowerCase(Locale.ROOT)))
                        ? fabricante + " " + tipoTexto
                        : tipoTexto;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Extrae el código ICAO o descripción desde la respuesta JSON de adsb.lol.
     */
    private static String extraerTipoDeJsonAdsbLol(String respJson) {
        // Lee el primer elemento del arreglo "ac" y extrae el campo "t" o "desc"
        if (respJson == null || respJson.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(respJson);
            JSONArray ac = root.optJSONArray("ac");
            if (ac != null && ac.length() > 0) {
                JSONObject avion = ac.getJSONObject(0);
                String t = avion.optString("t", "").trim().toUpperCase(Locale.ROOT);
                if (!t.isEmpty() && !"NULL".equals(t)) {
                    return t;
                }
                String desc = avion.optString("desc", "").trim();
                if (!desc.isEmpty() && !"null".equalsIgnoreCase(desc)) {
                    return desc;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Intenta deducir el tipo de aeronave analizando los metadatos de enlaces en Planespotters.
     */
    private static String consultarPlanespottersFallback(String hexLimpio, String matricula) {
        // Prepara endpoints públicos por hex y por matrícula de Planespotters
        String[] urls = new String[]{
                !hexLimpio.isEmpty() ? "https://api.planespotters.net/pub/photos/hex/" + hexLimpio : null,
                (matricula != null && !matricula.trim().isEmpty())
                        ? "https://api.planespotters.net/pub/photos/reg/" + matricula.trim().toUpperCase(Locale.ROOT)
                        : null
        };
        // Analiza la URL de la galería fotográfica para extraer el modelo del avión
        for (String u : urls) {
            if (u == null) continue;
            try {
                String resp = peticionHttpGet(u);
                if (resp != null && resp.contains("\"photos\"")) {
                    JSONObject root = new JSONObject(resp);
                    JSONArray photos = root.optJSONArray("photos");
                    if (photos != null && photos.length() > 0) {
                        String link = photos.getJSONObject(0).optString("link", "");
                        String deducido = deducirCodigoIcaoPorTexto(link.replace("-", " "));
                        if (deducido != null) return deducido;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Recupera descripción e imagen de Wikipedia desde caché o red aplicando texto fallback si falla.
     * Valida automáticamente la versión de la aplicación y la caducidad temporal (TTL).
     */
    private static String[] obtenerInfoCacheadaORemota(Context appContext, String claveCache,
                                                       String terminoBusqueda, String fabricante, String modelo) {
        // Verifica la validez de la caché según la versión actual de la app
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_WIKI_CACHE, Context.MODE_PRIVATE);
        validarYLimpiarCacheSiAppActualizada(appContext, prefs, CACHE_MEMORIA_WIKI);

        // 1. Intenta recuperar el artículo vigente desde memoria RAM
        String empaquetado = desempaquetarSiVigente(CACHE_MEMORIA_WIKI.get(claveCache));

        // 2. Si no está en RAM, intenta recuperarlo desde SharedPreferences en disco
        if (empaquetado == null) {
            String empaquetadoDisco = desempaquetarSiVigente(prefs.getString(claveCache, null));
            if (empaquetadoDisco != null) {
                empaquetado = empaquetadoDisco;
                CACHE_MEMORIA_WIKI.put(claveCache, empaquetarConTtl(empaquetado));
            }
        }

        if (empaquetado != null) {
            return desempaquetarWiki(empaquetado);
        }

        // 3. Si no existe caché vigente, consulta Wikipedia en red y persiste el resultado
        String[] wikiRemoto = consultarWikipediaValidada(appContext, terminoBusqueda);

        if (wikiRemoto != null) {
            String valorAGuardar = empaquetarConTtl(empaquetarWiki(wikiRemoto[0], wikiRemoto[1]));
            CACHE_MEMORIA_WIKI.put(claveCache, valorAGuardar);
            prefs.edit().putString(claveCache, valorAGuardar).apply();
            return wikiRemoto;
        }
        // Si Wikipedia no devuelve artículo aeronáutico válido, devuelve una descripción genérica localizada
        return new String[]{appContext.getString(R.string.descripcion_fallback, fabricante, modelo), null};
    }

    /**
     * Serializa descripción e imagen en una sola cadena para persistencia en SharedPreferences.
     */
    private static String empaquetarWiki(String descripcion, String urlImagen) {
        // Une descripción y URL de imagen mediante el separador interno
        return descripcion + SEPARADOR_CACHE + (urlImagen != null ? urlImagen : "");
    }

    /**
     * Separa la descripción y la URL de imagen almacenadas en caché.
     */
    private static String[] desempaquetarWiki(String empaquetado) {
        // Divide la cadena almacenada recuperando el texto y la URL opcional de la imagen
        int idx = empaquetado.indexOf(SEPARADOR_CACHE);
        if (idx < 0) return new String[]{empaquetado, null};
        String desc = empaquetado.substring(0, idx);
        String img = empaquetado.substring(idx + SEPARADOR_CACHE.length()).trim();
        return new String[]{desc, img.isEmpty() ? null : img};
    }

    /**
     * Consulta Wikipedia en el idioma activo y recurre al inglés si no encuentra artículo válido.
     */
    private static String[] consultarWikipediaValidada(Context appContext, String terminoBusqueda) {
        // Busca primero en el idioma de la interfaz y usa inglés como respaldo internacional
        if (terminoBusqueda == null || terminoBusqueda.trim().isEmpty()) return null;
        String lang = obtenerCodigoIdiomaWiki();

        String[] res = consultarWikipediaPorIdioma(appContext, terminoBusqueda, lang);
        if (res == null && !"en".equals(lang)) {
            res = consultarWikipediaPorIdioma(appContext, terminoBusqueda, "en");
        }
        return res;
    }

    /**
     * Busca un artículo aeronáutico en Wikipedia en el idioma indicado y extrae resumen y miniatura.
     */
    private static String[] consultarWikipediaPorIdioma(Context appContext, String terminoBusqueda, String lang) {
        try {
            // 1. Busca el título más relevante añadiendo un sufijo aeronáutico localizado
            String sufijo = appContext.getString(R.string.wiki_sufijo_busqueda);
            String searchUrl = "https://" + lang + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + URLEncoder.encode(terminoBusqueda + sufijo, "UTF-8") + "&format=json&srlimit=1";

            String searchResp = peticionHttpGet(searchUrl);
            if (searchResp == null) return null;

            JSONObject searchJson = new JSONObject(searchResp);
            JSONObject query = searchJson.optJSONObject("query");
            if (query == null) return null;

            JSONArray resultados = query.optJSONArray("search");
            if (resultados == null || resultados.length() == 0) return null;

            String titulo = resultados.getJSONObject(0).optString("title", "");
            if (titulo.isEmpty()) return null;

            // 2. Descarga el resumen REST v1 del artículo encontrado
            String summaryUrl = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(titulo.replace(" ", "_"), "UTF-8");
            String summaryResp = peticionHttpGet(summaryUrl);
            if (summaryResp == null) return null;

            JSONObject summaryJson = new JSONObject(summaryResp);
            String descripcionCorta = summaryJson.optString("description", "").toLowerCase(Locale.ROOT);
            String extracto = summaryJson.optString("extract", "").trim();

            // 3. Verifica que el artículo trate realmente sobre una aeronave
            if (!esArticuloAeronauticoValido(descripcionCorta, extracto)) {
                return null;
            }

            // 4. Extrae la imagen miniatura u original filtrando fotos de accidentes o banderas
            String urlImagen = null;
            JSONObject thumb = summaryJson.optJSONObject("thumbnail");
            if (thumb != null) {
                urlImagen = thumb.optString("source", null);
            }
            if (urlImagen == null || urlImagen.isEmpty()) {
                JSONObject orig = summaryJson.optJSONObject("originalimage");
                if (orig != null) urlImagen = orig.optString("source", null);
            }

            if (urlImagen != null && esImagenNoDeseada(urlImagen)) {
                urlImagen = null;
            }

            // 5. Recorta el extracto en el primer punto tras 140 caracteres si excede 260
            if (extracto.length() > 260) {
                int punto = extracto.indexOf('.', 140);
                if (punto > 0) extracto = extracto.substring(0, punto + 1);
            }

            // 6. Añade la atribución y enlace directo al artículo fuente de Wikipedia
            String urlArticulo = "https://" + lang + ".wikipedia.org/wiki/" + titulo.replace(" ", "_");
            String atribucion = appContext.getString(R.string.wiki_atribucion_formato, urlArticulo);

            return new String[]{extracto + atribucion, urlImagen};
        } catch (Exception e) {
            Log.w(TAG, "Fallo controlado al consultar Wikipedia: " + e.getMessage());
            return null;
        }
    }

    /**
     * Filtra imágenes de accidentes, incendios, banderas o escudos no representativos del avión.
     */
    private static boolean esImagenNoDeseada(String url) {
        // Comprueba palabras clave negativas en el nombre de archivo de Wikimedia
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("accident") || u.contains("crash") || u.contains("incident")
                || u.contains("damage") || u.contains("destroy") || u.contains("fire")
                || u.contains("wrecks") || u.contains("flag_of") || u.contains("coat_of_arms");
    }

    /**
     * Verifica que el resumen de Wikipedia pertenezca realmente a una aeronave y no a objetos, plantas o listados.
     */
    private static boolean esArticuloAeronauticoValido(String descripcionCorta, String extracto) {
        // Normaliza descripción corta y extracto en minúsculas
        String desc = (descripcionCorta != null) ? descripcionCorta.toLowerCase(Locale.ROOT) : "";
        String ext = (extracto != null) ? extracto.toLowerCase(Locale.ROOT) : "";

        // Descarta listados, designadores ICAO o artículos botánicos/genéricos
        if (desc.contains("type designator") || desc.contains("designador")
                || desc.contains("list of") || desc.contains("lista de")
                || desc.contains("seed") || desc.contains("plant") || desc.contains("grain")
                || desc.contains("semilla") || desc.contains("planta") || desc.contains("cereal")) {
            return false;
        }

        // Comprueba términos aeronáuticos en la descripción breve de Wikidata/Wikipedia
        boolean descEsAeronave = desc.contains("aircraft") || desc.contains("airliner")
                || desc.contains("helicopter") || desc.contains("fighter") || desc.contains("monoplane")
                || desc.contains("biplane") || desc.contains("glider") || desc.contains("seaplane")
                || desc.contains("avión") || desc.contains("avion") || desc.contains("aeronave")
                || desc.contains("helicóptero") || desc.contains("helicoptero") || desc.contains("caza")
                || desc.contains("planeador") || desc.contains("hidroavión");

        if (descEsAeronave) return true;

        // Si no hay descripción corta, inspecciona los primeros 180 caracteres del extracto
        String inicioExtracto = ext.length() > 180 ? ext.substring(0, 180) : ext;
        return inicioExtracto.contains("aircraft") || inicioExtracto.contains("airliner")
                || inicioExtracto.contains("helicopter") || inicioExtracto.contains("fighter jet")
                || inicioExtracto.contains("avión") || inicioExtracto.contains("avion")
                || inicioExtracto.contains("aeronave") || inicioExtracto.contains("helicóptero");
    }

    /**
     * Deduce el fabricante principal buscando palabras clave conocidas en el texto del modelo.
     */
    private static String deducirFabricante(String modelo) {
        // Identifica constructores aeronáuticos habituales contenidos en la cadena del modelo
        if (modelo == null) return "—";
        String m = modelo.toLowerCase(Locale.ROOT);
        if (m.contains("boeing")) return "Boeing";
        if (m.contains("airbus")) return "Airbus";
        if (m.contains("cessna")) return "Cessna";
        if (m.contains("piper")) return "Piper";
        if (m.contains("embraer")) return "Embraer";
        if (m.contains("bombardier")) return "Bombardier";
        if (m.contains("gulfstream")) return "Gulfstream";
        if (m.contains("dassault")) return "Dassault";
        if (m.contains("pilatus")) return "Pilatus";
        if (m.contains("cirrus")) return "Cirrus";
        if (m.contains("diamond")) return "Diamond";
        if (m.contains("beechcraft")) return "Beechcraft";
        if (m.contains("robinson")) return "Robinson";
        return "—";
    }

    /**
     * Ejecuta una petición HTTP GET síncrona con cabecera User-Agent y tiempos de espera acotados.
     */
    private static String peticionHttpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            // Abre la conexión HTTP con timeouts de 5 segundos y cabecera User-Agent identificativa
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Comprueba que el servidor responda con código HTTP 200 OK
            int code = conn.getResponseCode();
            if (code != 200) {
                Log.d(TAG, "HTTP " + code + " en " + urlStr);
                return null;
            }

            // Lee el cuerpo completo de la respuesta en UTF-8
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error HTTP en " + urlStr + ": " + e.getMessage());
            return null;
        } finally {
            // Libera siempre los recursos de red de la conexión
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Devuelve una instancia de Avion del catálogo maestro a partir de su código de tipo ICAO.
     */
    public static Avion getAvionPorTipoIcao(String tipoIcao) {
        // Busca el código ICAO en el índice estático y construye el objeto Avion correspondiente
        if (tipoIcao == null) return null;
        AvionData data = MAPA_POR_ICAO.get(tipoIcao.trim().toUpperCase(Locale.ROOT));
        return (data != null) ? crearAvionDesdeData(data) : null;
    }

    /**
     * Devuelve una lista completa con todas las aeronaves registradas en el catálogo maestro local.
     */
    public static List<Avion> getTodosLosAviones() {
        // Convierte cada ficha interna AvionData en una instancia pública Avion
        List<Avion> lista = new ArrayList<>();
        for (AvionData d : DATOS_AVIONES) {
            lista.add(crearAvionDesdeData(d));
        }
        return lista;
    }

    /**
     * Traduce el identificador interno de rareza al texto localizado correspondiente.
     */
    public static String traducirRareza(Context context, String rareza) {
        // Mapea la clave interna en inglés a su cadena localizada en strings.xml
        if (rareza == null) return context.getString(R.string.rareza_common);
        switch (rareza.toUpperCase(Locale.ROOT)) {
            case "RARE": return context.getString(R.string.rareza_rare);
            case "EPIC": return context.getString(R.string.rareza_epic);
            case "LEGENDARY": return context.getString(R.string.rareza_legendary);
            case "MYTHIC": return context.getString(R.string.rareza_mythic);
            case "EASTER_EGG": return "EASTER EGG";
            default: return context.getString(R.string.rareza_common);
        }
    }

    /**
     * Deduce la región o país de registro a partir del bloque hexadecimal ICAO24 estándar.
     */
    private static String deducirPaisPorHexIcao(String icao24) {
        // Convierte los 6 dígitos hexadecimales a entero y comprueba los rangos asignados por la OACI
        if (icao24 == null || icao24.trim().length() != 6) return "";
        try {
            int hexVal = Integer.parseInt(icao24.trim(), 16);
            if (hexVal >= 0xA00000 && hexVal <= 0xADF7C7) return "EE.UU. (FAA)";
            if (hexVal >= 0x340000 && hexVal <= 0x37FFFF) return "España";
            if (hexVal >= 0x400000 && hexVal <= 0x43FFFF) return "Reino Unido";
            if (hexVal >= 0x3C0000 && hexVal <= 0x3FFFFF) return "Alemania";
            if (hexVal >= 0x380000 && hexVal <= 0x3BFFFF) return "Francia";
            if (hexVal >= 0x300000 && hexVal <= 0x33FFFF) return "Italia";
            if (hexVal >= 0x490000 && hexVal <= 0x497FFF) return "Portugal";
            if (hexVal >= 0xC00000 && hexVal <= 0xC3FFFF) return "Canadá";
        } catch (NumberFormatException ignored) {
        }
        return "";
    }
}