// Paquete que agrupa las clases de utilidad del proyecto
package es.medac.skycollectorapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;

/**
 * Clase encargada de gestionar el catálogo técnico local de aeronaves usando siluetas
 * categorizadas y enriquecer de forma asíncrona descripciones e imágenes mediante HexDB,
 * ADSBDB y Wikipedia, bloqueando cualquier búsqueda visual si la aeronave es desconocida.
 */
public class AvionGenerator {

    private static final String TAG = "AvionGenerator";
    private static final String PREFS_WIKI_CACHE = "skycollector_wiki_cache_v5";
    private static final String PREFS_HEX_CACHE = "skycollector_hex_cache_v3";
    private static final String SIN_RESULTADO_CACHE = "NONE";
    private static final String SEPARADOR_CACHE = "|||IMG|||";
    public static final String USER_AGENT = "SkyCollectorApp/1.0 (https://github.com/medac/skycollectorapp; contacto@skycollector.app)";

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ConcurrentHashMap<String, String> CACHE_MEMORIA_WIKI = new ConcurrentHashMap<>();
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
        final String modelo, fabricante, rareza, vel, cap, dim, pais, peso;
        final int siluetaId;

        AvionData(String m, String f, String r, int s, String v, String c, String d, String p, String pe) {
            modelo = m;
            fabricante = f;
            rareza = r;
            siluetaId = s;
            vel = v;
            cap = c;
            dim = d;
            pais = p;
            peso = pe;
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

    // Estructuras de almacenamiento rápido en memoria
    private static final List<AvionData> DATOS_AVIONES = new ArrayList<>();
    private static final Map<String, AvionData> MAPA_POR_ICAO = new HashMap<>();
    private static final Map<String, AvionData> MAPA_POR_NOMBRE_NORMALIZADO = new HashMap<>();

    // Inicialización estática del catálogo maestro local
    static {
        // --- EASTER EGG ---
        registrar("Airport Operations Vehicle", "Airport", "EPIC", SIL_CARGA, "60 km/h", "Operations", "5m", "Global", "2t", "SERV", "GND");
        registrar("Tower | Torre de control", "Airport", "EPIC", SIL_CARGA, "0 km/h", "Operations", "5m", "Global", "2t", "TWR");

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
        registrar("Eurofighter Typhoon", "Eurofighter", "RARE", SIL_CAZA, "2495 km/h", "1 piloto", "11m", "Europa", "23t", "EUFI");
        registrar("F-16 Fighting Falcon", "General Dynamics", "RARE", SIL_CAZA, "2120 km/h", "1 piloto", "10m", "EE.UU.", "19t", "F16");
        registrar("C-130 Hercules", "Lockheed", "RARE", SIL_CARGA, "671 km/h", "Carga", "40m", "EE.UU.", "70t", "C130", "C30J", "L100");
        registrar("Learjet 60", "Bombardier", "RARE", SIL_JET, "839 km/h", "8 pax", "13.5m", "EE.UU.", "10t", "LJ60");
        registrar("Boeing 777-300ER", "Boeing", "RARE", SIL_JET, "905 km/h", "396 pax", "65m", "EE.UU.", "351t", "B77W");
        registrar("Airbus A330", "Airbus", "RARE", SIL_JET, "871 km/h", "277 pax", "60m", "Europa", "242t", "A332", "A333");
        registrar("Gulfstream G650", "Gulfstream", "RARE", SIL_JET, "956 km/h", "19 pax", "30m", "EE.UU.", "45t", "GLF6");
        registrar("Dassault Falcon 7X", "Dassault", "RARE", SIL_JET, "900 km/h", "12 pax", "26m", "Francia", "31t", "FA7X");
        registrar("Mirage 2000", "Dassault", "RARE", SIL_CAZA, "2336 km/h", "1 piloto", "9m", "Francia", "17t", "MIR2");
        registrar("Saab 39 Gripen", "Saab", "RARE", SIL_CAZA, "2204 km/h", "1 piloto", "8.4m", "Suecia", "14t", "SB39");
        registrar("CH-47 Chinook", "Boeing", "RARE", SIL_HELICOPTERO, "315 km/h", "55 tropas", "18m", "EE.UU.", "22t", "H47");

        // --- AERONAVES PRINCIPALES (EPIC) ---
        registrar("Airbus A380", "Airbus", "EPIC", SIL_JET, "1020 km/h", "853 pax", "80m", "Europa", "575t", "A388");
        registrar("Boeing 747-400", "Boeing", "EPIC", SIL_JET, "988 km/h", "416 pax", "64m", "EE.UU.", "396t", "B744");
        registrar("F-35 Lightning II", "Lockheed Martin", "EPIC", SIL_CAZA, "1960 km/h", "1 piloto", "11m", "EE.UU.", "31t", "F35");
        registrar("AH-64 Apache", "Boeing", "EPIC", SIL_HELICOPTERO, "293 km/h", "2 pilotos", "15m", "EE.UU.", "10t", "AH64");
        registrar("V-22 Osprey", "Bell Boeing", "EPIC", SIL_CARGA, "509 km/h", "24 tropas", "26m", "EE.UU.", "27t", "V22");
        registrar("F/A-18 Super Hornet", "Boeing", "EPIC", SIL_CAZA, "1915 km/h", "1 piloto", "14m", "EE.UU.", "29t", "F18S", "F18H");
        registrar("Antonov An-124", "Antonov", "EPIC", SIL_CARGA, "865 km/h", "Carga", "73m", "URSS", "405t", "A124");
        registrar("Airbus Beluga XL", "Airbus", "EPIC", SIL_CARGA, "737 km/h", "Carga Esp.", "60m", "Europa", "227t", "A337");
        registrar("Boeing Dreamlifter", "Boeing", "EPIC", SIL_CARGA, "878 km/h", "Carga Esp.", "64m", "EE.UU.", "364t", "BLCF");
        registrar("Mi-24 Hind", "Mil", "EPIC", SIL_HELICOPTERO, "335 km/h", "8 tropas", "17m", "URSS", "12t", "MI24");

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

        registrar("F-15 Eagle", "McDonnell Douglas", "EPIC", SIL_CAZA, "2655 km/h", "1 piloto", "13m", "EE.UU.", "31t", "F15");
        registrar("F-4 Phantom II", "McDonnell Douglas", "LEGENDARY", SIL_CAZA, "2370 km/h", "2 pilotos", "12m", "EE.UU.", "28t", "F4");
        registrar("A-10 Thunderbolt II", "Fairchild Republic", "EPIC", SIL_CAZA, "706 km/h", "1 piloto", "18m", "EE.UU.", "23t", "A10");
        registrar("Dassault Rafale", "Dassault", "EPIC", SIL_CAZA, "1912 km/h", "1 piloto", "11m", "Francia", "25t", "RFAL");
        registrar("Panavia Tornado", "Panavia", "EPIC", SIL_CAZA, "2400 km/h", "2 pilotos", "14m", "Europa", "28t", "TOR");
        registrar("Mikoyan MiG-29", "Mikoyan", "EPIC", SIL_CAZA, "2400 km/h", "1 piloto", "11m", "URSS", "18t", "MG29");
        registrar("Northrop F-5 Tiger II", "Northrop", "EPIC", SIL_CAZA, "1700 km/h", "1 piloto", "8m", "EE.UU.", "11t", "F5");
        registrar("Lockheed U-2 Dragon Lady", "Lockheed", "EPIC", SIL_CAZA, "800 km/h", "1 piloto", "31m", "EE.UU.", "18t", "U2");
        registrar("BAE Hawk", "BAE Systems", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "10m", "UK", "9t", "HAWK");
        registrar("Dassault/Dornier Alpha Jet", "Dassault/Dornier", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "9m", "Francia/Alemania", "8t", "ALPH");
        registrar("Aermacchi M-346", "Aermacchi", "RARE", SIL_CAZA, "1090 km/h", "2 pilotos", "10m", "Italia", "10t", "M346");
        registrar("Northrop T-38 Talon", "Northrop", "RARE", SIL_CAZA, "1380 km/h", "2 pilotos", "8m", "EE.UU.", "5t", "T38");
        registrar("Mikoyan MiG-21", "Mikoyan", "LEGENDARY", SIL_CAZA, "2175 km/h", "1 piloto", "7m", "URSS", "9t", "MG21");
        registrar("Mikoyan MiG-15", "Mikoyan", "LEGENDARY", SIL_CAZA, "1075 km/h", "1 piloto", "10m", "URSS", "5t", "MG15");
        registrar("F-86 Sabre", "North American", "LEGENDARY", SIL_CAZA, "1100 km/h", "1 piloto", "12m", "EE.UU.", "7t", "F86");

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
    }

    /**
     * Registra una aeronave en el catálogo maestro e indexa tanto sus códigos ICAO
     * como su nombre normalizado para búsquedas exactas.
     */
    private static void registrar(String modelo, String fabricante, String rareza, int siluetaId,
                                  String vel, String cap, String dim, String pais, String peso,
                                  String... codigos) {
        AvionData data = new AvionData(modelo, fabricante, rareza, siluetaId, vel, cap, dim, pais, peso);
        DATOS_AVIONES.add(data);
        MAPA_POR_NOMBRE_NORMALIZADO.put(normalizarNombre(modelo), data);
        for (String c : codigos) {
            MAPA_POR_ICAO.put(c.trim().toUpperCase(Locale.ROOT), data);
        }
    }

    /**
     * Normaliza un nombre eliminando espacios y guiones en minúsculas para coincidencias exactas.
     */
    private static String normalizarNombre(String texto) {
        if (texto == null) return "";
        return texto.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-_./]", "");
    }

    /**
     * Construye un objeto Avion a partir de una ficha local verificada.
     */
    private static Avion crearAvionDesdeData(AvionData d) {
        return new Avion(d.modelo, d.fabricante, d.rareza, d.siluetaId, d.vel, d.cap, d.dim, d.pais, d.peso);
    }

    /**
     * Crea la instancia localizada del Easter Egg para vehículos terrestres aeroportuarios.
     */
    private static Avion crearEasterEggVehiculo(Context appContext, String icao24, String tipoLimpio) {
        Avion easterEgg = new Avion(
                appContext.getString(R.string.easter_egg_vehiculo_modelo),
                appContext.getString(R.string.easter_egg_vehiculo_fabricante),
                "EPIC",
                SIL_CARGA,
                "60 km/h",
                appContext.getString(R.string.easter_egg_vehiculo_capacidad),
                "5m",
                appContext.getString(R.string.easter_egg_vehiculo_pais),
                "2t",
                icao24
        );
        easterEgg.setTipoIcao(tipoLimpio);
        easterEgg.setDescripcion(appContext.getString(R.string.easter_egg_vehiculo_descripcion));
        return easterEgg;
    }

    /**
     * Devuelve la silueta correspondiente a partir del nombre del modelo o tipo ICAO.
     */
    public static int resolverImagenPorModelo(String modelo, String tipoIcao) {
        if (tipoIcao != null && !tipoIcao.trim().isEmpty()) {
            AvionData data = MAPA_POR_ICAO.get(tipoIcao.trim().toUpperCase(Locale.ROOT));
            if (data != null) return data.siluetaId;
        }
        if (modelo != null && !modelo.trim().isEmpty()) {
            AvionData data = MAPA_POR_NOMBRE_NORMALIZADO.get(normalizarNombre(modelo));
            if (data != null) return data.siluetaId;
        }
        return SIL_JET;
    }

    /**
     * Comprueba si un nombre de modelo corresponde a un avión desconocido en cualquier idioma.
     */
    public static boolean esModeloDesconocido(Context context, String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) return true;
        String m = modelo.trim();
        String desconocidoLocalizado = context.getString(R.string.aeronave_desconocida);
        return m.equalsIgnoreCase(desconocidoLocalizado)
                || m.equalsIgnoreCase("Unknown Aircraft")
                || m.equalsIgnoreCase("Aeronave desconocida");
    }

    /**
     * Limpia prefijos genéricos del modelo para mejorar la precisión al buscar imágenes en Wikipedia.
     */
    public static String limpiarModeloParaWiki(Context context, String modelo) {
        if (esModeloDesconocido(context, modelo)) return "";
        String formatoGenerico = context.getString(R.string.aeronave_generica, "");
        return modelo.replace(formatoGenerico, "")
                .replaceAll("(?i)^(aeronave|avión|aircraft)\\s+(icao\\s+)?", "")
                .trim();
    }

    /**
     * Determina la silueta adecuada cuando una aeronave genérica no está en el catálogo local.
     */
    private static int determinarSiluetaPorCategoriaRadar(String categoriaRadar) {
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
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String lang = (!appLocales.isEmpty() && appLocales.get(0) != null)
                ? appLocales.get(0).getLanguage()
                : Locale.getDefault().getLanguage();
        return "es".equalsIgnoreCase(lang) ? "es" : "en";
    }

    /**
     * Resuelve de forma asíncrona el código de tipo ICAO y su nombre de modelo a partir del ICAO24
     * para actualizar la tarjeta del radar en el mapa cuando el feed omite el tipo.
     */
    public static void resolverTipoIcaoAsync(Context context, String icao24, OnTipoIcaoResueltoListener listener) {
        if (listener == null || icao24 == null || icao24.trim().isEmpty()) return;
        final Context appContext = context.getApplicationContext();

        NETWORK_EXECUTOR.execute(() -> {
            String recuperado = obtenerTipoIcaoDesdeHexDbCacheado(appContext, icao24);
            if (recuperado != null && !recuperado.isEmpty()) {
                String upper = recuperado.trim().toUpperCase(Locale.ROOT);
                AvionData data = MAPA_POR_ICAO.get(upper);
                if (data == null) {
                    data = MAPA_POR_NOMBRE_NORMALIZADO.get(normalizarNombre(recuperado));
                }

                String tipoFinal = (upper.length() <= 4 && upper.matches("[A-Z0-9]{2,4}")) ? upper : "";
                String modeloFinal = (data != null)
                        ? data.modelo
                        : (!tipoFinal.isEmpty() ? appContext.getString(R.string.aeronave_generica, tipoFinal) : recuperado.trim());

                MAIN_HANDLER.post(() -> listener.onTipoIcaoResuelto(tipoFinal, modeloFinal));
            }
        });
    }

    /**
     * Resuelve de forma asíncrona una aeronave cruzando la capa local, respaldos remotos por Hex/Matrícula y Wikipedia.
     */
    public static void resolverAvionGlobalAsync(Context context, String icao24, String matricula, String tipoIcao,
                                                String modeloRadarFallback, String categoriaRadar,
                                                OnAvionResueltoListener listener) {
        if (listener == null) return;
        final Context appContext = context.getApplicationContext();
        final String tipoLimpio = (tipoIcao != null) ? tipoIcao.trim().toUpperCase(Locale.ROOT) : "";
        final String matriculaLimpia = (matricula != null) ? matricula.trim() : "";
        final String modeloFallbackUpper = (modeloRadarFallback != null) ? modeloRadarFallback.trim().toUpperCase(Locale.ROOT) : "";

        // Detección automática de Torres de Control (TWR) y vehículos de tierra (GRND, SERV, GND)
        if ("TWR".equals(tipoLimpio) || "SERV".equals(tipoLimpio) || "GND".equals(tipoLimpio) || tipoLimpio.contains("GRND") || modeloFallbackUpper.contains("TWR")) {
            String tipoEntidad = "TWR".equals(tipoLimpio) ? "TWR" : (!tipoLimpio.isEmpty() ? tipoLimpio : "GRND");
            Avion easterEgg = crearEasterEggVehiculo(appContext, icao24, tipoEntidad);
            MAIN_HANDLER.post(() -> listener.onAvionResuelto(easterEgg));
            return;
        }

        // 1. Capa local maestra
        AvionData dataLocal = null;
        if (!tipoLimpio.isEmpty()) {
            dataLocal = MAPA_POR_ICAO.get(tipoLimpio);
        }
        if (dataLocal == null && modeloRadarFallback != null && !esModeloDesconocido(appContext, modeloRadarFallback)) {
            dataLocal = MAPA_POR_NOMBRE_NORMALIZADO.get(normalizarNombre(modeloRadarFallback));
        }

        final Avion avionBase;
        final String terminoBusquedaWiki;

        if (dataLocal != null) {
            avionBase = crearAvionDesdeData(dataLocal);
            terminoBusquedaWiki = dataLocal.modelo;
        } else {
            String nombreAgrupado = !tipoLimpio.isEmpty()
                    ? appContext.getString(R.string.aeronave_generica, tipoLimpio)
                    : (!esModeloDesconocido(appContext, modeloRadarFallback)
                    ? modeloRadarFallback.trim()
                    : appContext.getString(R.string.aeronave_desconocida));

            String fabricante = deducirFabricante(modeloRadarFallback);
            int silueta = determinarSiluetaPorCategoriaRadar(categoriaRadar);

            avionBase = new Avion(nombreAgrupado, fabricante, "COMMON", silueta, "—", "—", "—", "—", "—");
            terminoBusquedaWiki = !esModeloDesconocido(appContext, modeloRadarFallback)
                    ? modeloRadarFallback.trim()
                    : tipoLimpio;
        }

        if (!tipoLimpio.isEmpty()) avionBase.setTipoIcao(tipoLimpio);
        if (icao24 != null) avionBase.setIcao24(icao24);

        // 2. Capa remota: consulta pasando tanto ICAO24 como la matrícula
        NETWORK_EXECUTOR.execute(() -> {
            String tipoEfectivo = tipoLimpio;
            String terminoEfectivo = terminoBusquedaWiki;

            if ((tipoEfectivo.isEmpty() || esModeloDesconocido(appContext, avionBase.getModelo()))
                    && (icao24 != null || !matriculaLimpia.isEmpty())) {

                String recuperado = obtenerTipoIcaoDesdeHexYMatriculaCacheado(appContext, icao24, matriculaLimpia);
                if (recuperado != null) {
                    String recuperadoUpper = recuperado.trim().toUpperCase(Locale.ROOT);
                    if ("SERV".equals(recuperadoUpper) || "GND".equals(recuperadoUpper)) {
                        Avion easterEgg = crearEasterEggVehiculo(appContext, icao24, recuperadoUpper);
                        MAIN_HANDLER.post(() -> listener.onAvionResuelto(easterEgg));
                        return;
                    }

                    AvionData dataRecuperada = MAPA_POR_ICAO.get(recuperadoUpper);
                    if (dataRecuperada == null) {
                        dataRecuperada = MAPA_POR_NOMBRE_NORMALIZADO.get(normalizarNombre(recuperado));
                    }

                    if (dataRecuperada != null) {
                        tipoEfectivo = (recuperadoUpper.length() <= 4) ? recuperadoUpper : tipoEfectivo;
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
                        tipoEfectivo = recuperadoUpper;
                        avionBase.setTipoIcao(tipoEfectivo);
                        avionBase.setModelo(appContext.getString(R.string.aeronave_generica, tipoEfectivo));
                        terminoEfectivo = tipoEfectivo;
                    } else {
                        avionBase.setModelo(recuperado.trim());
                        terminoEfectivo = recuperado.trim();
                    }
                }
            }

            if (esModeloDesconocido(appContext, avionBase.getModelo()) || terminoEfectivo == null || terminoEfectivo.trim().isEmpty()) {
                avionBase.setDescripcion(appContext.getString(
                        R.string.descripcion_fallback,
                        avionBase.getFabricante(),
                        avionBase.getModelo()
                ));
                avionBase.setUrlImagenRemota(null);
                avionBase.setImagenUrl(null);
                MAIN_HANDLER.post(() -> listener.onAvionResuelto(avionBase));
                return;
            }

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

            MAIN_HANDLER.post(() -> listener.onAvionResuelto(avionBase));
        });
    }

    /**
     * Recupera el tipo de aeronave probando primero por ICAO24 y, si falla, consultando
     * directamente por la matrícula oficial (por ejemplo, endpoint N-Number de ADSBDB para N47470).
     */
    private static String obtenerTipoIcaoDesdeHexYMatriculaCacheado(Context appContext, String icao24, String matricula) {
        String claveCache = (icao24 != null && !icao24.trim().isEmpty())
                ? icao24.trim().toLowerCase(Locale.ROOT)
                : (matricula != null ? "reg_" + matricula.trim().toLowerCase(Locale.ROOT) : "");

        if (claveCache.isEmpty()) return null;

        // 1. Comprobar en memoria RAM
        String cacheadoMemoria = CACHE_MEMORIA_HEX.get(claveCache);
        if (cacheadoMemoria != null) {
            return SIN_RESULTADO_CACHE.equals(cacheadoMemoria) ? null : cacheadoMemoria;
        }

        // 2. Comprobar en disco (SharedPreferences)
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_HEX_CACHE, Context.MODE_PRIVATE);
        String cacheadoDisco = prefs.getString(claveCache, null);
        if (cacheadoDisco != null && !SIN_RESULTADO_CACHE.equals(cacheadoDisco)) {
            CACHE_MEMORIA_HEX.put(claveCache, cacheadoDisco);
            return cacheadoDisco;
        }

        // 3. Consultar a la red (HexDB -> ADSBDB por Hex -> OpenSky -> ADSBDB por Matrícula)
        String tipoRemoto = null;
        if (icao24 != null && !icao24.trim().isEmpty()) {
            tipoRemoto = consultarTipoIcaoRemoto(icao24);
        }

        // Si por ICAO no dio resultado pero tenemos matrícula, consultamos por N-Number en ADSBDB
        if (tipoRemoto == null && matricula != null && !matricula.trim().isEmpty()) {
            tipoRemoto = consultarTipoIcaoPorMatriculaRemoto(matricula);
        }

        if (tipoRemoto != null) {
            CACHE_MEMORIA_HEX.put(claveCache, tipoRemoto);
            prefs.edit().putString(claveCache, tipoRemoto).apply();
            return tipoRemoto;
        } else {
            CACHE_MEMORIA_HEX.put(claveCache, SIN_RESULTADO_CACHE);
            return null;
        }
    }

    /**
     * Consulta el tipo o modelo de la aeronave en ADSBDB utilizando directamente su matrícula (N-Number).
     */
    private static String consultarTipoIcaoPorMatriculaRemoto(String matricula) {
        try {
            String urlAdsbReg = "https://api.adsbdb.com/v0/n-number/" + URLEncoder.encode(matricula.trim(), "UTF-8");
            String respAdsb = peticionHttpGet(urlAdsbReg);
            if (respAdsb != null) {
                JSONObject root = new JSONObject(respAdsb);
                JSONObject response = root.optJSONObject("response");
                if (response != null) {
                    JSONObject aircraft = response.optJSONObject("aircraft");
                    if (aircraft != null) {
                        String icaoType = aircraft.optString("icao_type", "").trim().toUpperCase(Locale.ROOT);
                        if (!icaoType.isEmpty() && !"NULL".equals(icaoType)) {
                            return icaoType;
                        }
                        String tipoTexto = aircraft.optString("type", "").trim();
                        if (!tipoTexto.isEmpty() && !"null".equalsIgnoreCase(tipoTexto)) {
                            return tipoTexto;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Recupera el código de tipo ICAO o modelo desde caché o consultando secuencialmente
     * HexDB, ADSBDB y OpenSky. Los aciertos se guardan en disco, los fallos solo en RAM.
     */
    private static String obtenerTipoIcaoDesdeHexDbCacheado(Context appContext, String icao24) {
        String hexLimpio = icao24.trim().toLowerCase(Locale.ROOT);
        if (hexLimpio.isEmpty()) return null;

        // 1. Comprobar primero en memoria RAM (sesión actual)
        String cacheadoMemoria = CACHE_MEMORIA_HEX.get(hexLimpio);
        if (cacheadoMemoria != null) {
            return SIN_RESULTADO_CACHE.equals(cacheadoMemoria) ? null : cacheadoMemoria;
        }

        // 2. Comprobar en SharedPreferences (disco) SOLO si es un resultado válido
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_HEX_CACHE, Context.MODE_PRIVATE);
        String cacheadoDisco = prefs.getString(hexLimpio, null);
        if (cacheadoDisco != null && !SIN_RESULTADO_CACHE.equals(cacheadoDisco)) {
            CACHE_MEMORIA_HEX.put(hexLimpio, cacheadoDisco);
            return cacheadoDisco;
        }

        // 3. Si no está en caché válida, consultamos a la red (HexDB -> ADSBDB -> OpenSky)
        String tipoRemoto = consultarTipoIcaoRemoto(hexLimpio);

        if (tipoRemoto != null) {
            // Acierto: Guardar permanentemente en disco y RAM
            CACHE_MEMORIA_HEX.put(hexLimpio, tipoRemoto);
            prefs.edit().putString(hexLimpio, tipoRemoto).apply();
            return tipoRemoto;
        } else {
            // Fallo: Guardar "NONE" SOLO en memoria RAM (volátil).
            // Así en el siguiente reinicio de la app volverá a intentarlo por si la API ya lo conoce.
            CACHE_MEMORIA_HEX.put(hexLimpio, SIN_RESULTADO_CACHE);
            return null;
        }
    }

    /**
     * Consulta HexDB y ADSBDB por ICAO24 (y por matrícula si está disponible) para obtener
     * el código de tipo ICAO o el nombre del modelo de la aeronave.
     */
    private static String consultarTipoIcaoRemoto(String hexLimpio) {
        // 1. Intento principal: HexDB por ICAO24
        try {
            String urlHex = "https://hexdb.io/api/v1/aircraft/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respHex = peticionHttpGet(urlHex);
            if (respHex != null) {
                JSONObject json = new JSONObject(respHex);
                String tipo = json.optString("ICAOTypeCode", "").trim().toUpperCase(Locale.ROOT);
                if (!tipo.isEmpty() && !"NULL".equals(tipo)) {
                    return tipo;
                }
                // Si HexDB devuelve Type pero no ICAOTypeCode
                String typeName = json.optString("Type", "").trim();
                if (!typeName.isEmpty() && !"null".equalsIgnoreCase(typeName)) {
                    return typeName;
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Intento de respaldo: ADSBDB por ICAO24
        try {
            String urlAdsb = "https://api.adsbdb.com/v0/aircraft/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respAdsb = peticionHttpGet(urlAdsb);
            if (respAdsb != null) {
                JSONObject root = new JSONObject(respAdsb);
                JSONObject response = root.optJSONObject("response");
                if (response != null) {
                    JSONObject aircraft = response.optJSONObject("aircraft");
                    if (aircraft != null) {
                        String icaoType = aircraft.optString("icao_type", "").trim().toUpperCase(Locale.ROOT);
                        if (!icaoType.isEmpty() && !"NULL".equals(icaoType)) {
                            return icaoType;
                        }
                        String tipoTexto = aircraft.optString("type", "").trim();
                        if (!tipoTexto.isEmpty() && !"null".equalsIgnoreCase(tipoTexto)) {
                            return tipoTexto;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Tercer intento: OpenSky Metadata API (muy actualizada con matriculaciones FAA 2026 como N47470)
        try {
            String urlOpenSky = "https://opensky-network.org/api/metadata/aircraft/icao/" + URLEncoder.encode(hexLimpio, "UTF-8");
            String respOs = peticionHttpGet(urlOpenSky);
            if (respOs != null) {
                JSONObject json = new JSONObject(respOs);
                String typecode = json.optString("typecode", "").trim().toUpperCase(Locale.ROOT);
                if (!typecode.isEmpty() && !"NULL".equals(typecode)) {
                    return typecode;
                }
                String model = json.optString("model", "").trim();
                if (!model.isEmpty() && !"null".equalsIgnoreCase(model)) {
                    return model;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Consulta la descripción e imagen de la aeronave en caché o en Wikipedia de forma inteligente.
     * Los aciertos se almacenan permanentemente en disco y RAM, mientras que los fallos o ausencias
     * de artículo se guardan únicamente en la RAM de la sesión actual para permitir reintentos futuros.
     */
    private static String[] obtenerInfoCacheadaORemota(Context appContext, String claveCache,
                                                       String terminoBusqueda, String fabricante, String modelo) {
        // 1. Comprobar primero en el mapa de memoria RAM de la sesión actual
        String empaquetado = CACHE_MEMORIA_WIKI.get(claveCache);
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_WIKI_CACHE, Context.MODE_PRIVATE);

        // 2. Si no está en RAM, consultar el disco (SharedPreferences) ignorando los resultados negativos "NONE"
        if (empaquetado == null) {
            String empaquetadoDisco = prefs.getString(claveCache, null);
            if (empaquetadoDisco != null && !SIN_RESULTADO_CACHE.equals(empaquetadoDisco)) {
                empaquetado = empaquetadoDisco;
                CACHE_MEMORIA_WIKI.put(claveCache, empaquetado);
            } else if (SIN_RESULTADO_CACHE.equals(empaquetadoDisco)) {
                CACHE_MEMORIA_WIKI.put(claveCache, SIN_RESULTADO_CACHE);
            }
        }

        // Si encontramos un resultado válido en caché, lo devolvemos directamente
        if (empaquetado != null && !SIN_RESULTADO_CACHE.equals(empaquetado)) {
            return desempaquetarWiki(empaquetado);
        }

        // Si ya sabemos que falló en esta misma sesión, evitamos reintentar en bucle y devolvemos el fallback
        if (SIN_RESULTADO_CACHE.equals(CACHE_MEMORIA_WIKI.get(claveCache))) {
            return new String[]{appContext.getString(R.string.descripcion_fallback, fabricante, modelo), null};
        }

        // 3. Si no hay caché válida, consultamos directamente a Wikipedia a través de la red
        String[] wikiRemoto = consultarWikipediaValidada(appContext, terminoBusqueda);

        if (wikiRemoto != null) {
            // Acierto: Empaquetar y guardar permanentemente tanto en disco como en RAM
            String valorAGuardar = empaquetarWiki(wikiRemoto[0], wikiRemoto[1]);
            CACHE_MEMORIA_WIKI.put(claveCache, valorAGuardar);
            prefs.edit().putString(claveCache, valorAGuardar).apply();
            return wikiRemoto;
        } else {
            // Fallo o artículo inexistente: Guardar "NONE" exclusivamente en la memoria RAM volátil
            CACHE_MEMORIA_WIKI.put(claveCache, SIN_RESULTADO_CACHE);
            return new String[]{appContext.getString(R.string.descripcion_fallback, fabricante, modelo), null};
        }
    }

    /**
     * Empaqueta descripción e imagen en una sola cadena para guardarlas juntas en caché.
     */
    private static String empaquetarWiki(String descripcion, String urlImagen) {
        return descripcion + SEPARADOR_CACHE + (urlImagen != null ? urlImagen : "");
    }

    /**
     * Separa la descripción y la URL de imagen recuperadas desde la caché.
     */
    private static String[] desempaquetarWiki(String empaquetado) {
        int idx = empaquetado.indexOf(SEPARADOR_CACHE);
        if (idx < 0) return new String[]{empaquetado, null};
        String desc = empaquetado.substring(0, idx);
        String img = empaquetado.substring(idx + SEPARADOR_CACHE.length()).trim();
        return new String[]{desc, img.isEmpty() ? null : img};
    }

    /**
     * Realiza la búsqueda en Wikipedia con User-Agent reglamentario, valida que el artículo sea
     * una aeronave real y extrae tanto el texto con atribución como la URL de su foto principal.
     */
    private static String[] consultarWikipediaValidada(Context appContext, String terminoBusqueda) {
        if (terminoBusqueda == null || terminoBusqueda.trim().isEmpty()) return null;
        String lang = obtenerCodigoIdiomaWiki();

        String[] res = consultarWikipediaPorIdioma(appContext, terminoBusqueda, lang);
        if (res == null && !"en".equals(lang)) {
            res = consultarWikipediaPorIdioma(appContext, terminoBusqueda, "en");
        }
        return res;
    }

    /**
     * Ejecuta la búsqueda y extracción de resumen e imagen en un idioma específico de Wikipedia.
     */
    private static String[] consultarWikipediaPorIdioma(Context appContext, String terminoBusqueda, String lang) {
        try {
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

            String summaryUrl = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/"
                    + URLEncoder.encode(titulo.replace(" ", "_"), "UTF-8");
            String summaryResp = peticionHttpGet(summaryUrl);
            if (summaryResp == null) return null;

            JSONObject summaryJson = new JSONObject(summaryResp);
            String descripcionCorta = summaryJson.optString("description", "").toLowerCase(Locale.ROOT);
            String extracto = summaryJson.optString("extract", "").trim();

            if (!esArticuloAeronauticoValido(descripcionCorta, extracto)) {
                return null;
            }

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

            if (extracto.length() > 260) {
                int punto = extracto.indexOf('.', 140);
                if (punto > 0) extracto = extracto.substring(0, punto + 1);
            }

            String urlArticulo = "https://" + lang + ".wikipedia.org/wiki/" + titulo.replace(" ", "_");
            String atribucion = appContext.getString(R.string.wiki_atribucion_formato, urlArticulo);

            return new String[]{extracto + atribucion, urlImagen};
        } catch (Exception e) {
            Log.w(TAG, "Fallo controlado al consultar Wikipedia: " + e.getMessage());
            return null;
        }
    }

    /**
     * Filtra imágenes de accidentes, banderas o diagramas no representativos.
     */
    private static boolean esImagenNoDeseada(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("accident") || u.contains("crash") || u.contains("incident")
                || u.contains("damage") || u.contains("destroy") || u.contains("fire")
                || u.contains("wrecks") || u.contains("flag_of") || u.contains("coat_of_arms");
    }

    /**
     * Comprueba que el artículo de Wikipedia sea una aeronave concreta y no un artículo de códigos OACI.
     */
    private static boolean esArticuloAeronauticoValido(String descripcionCorta, String extracto) {
        String combinado = (descripcionCorta + " " + extracto).toLowerCase(Locale.ROOT);
        if (combinado.contains("type designator") || combinado.contains("designador")
                || combinado.contains("international civil aviation") || combinado.contains("aviación civil internacional")
                || combinado.contains("list of ") || combinado.contains("lista de ")) {
            return false;
        }
        return combinado.contains("avión") || combinado.contains("avion") || combinado.contains("aeronave")
                || combinado.contains("helicóptero") || combinado.contains("helicoptero") || combinado.contains("caza")
                || combinado.contains("aircraft") || combinado.contains("airliner") || combinado.contains("helicopter")
                || combinado.contains("fighter") || combinado.contains("jet") || combinado.contains("monoplane")
                || combinado.contains("biplane") || combinado.contains("boeing") || combinado.contains("airbus")
                || combinado.contains("cessna") || combinado.contains("piper");
    }

    /**
     * Deduce el fabricante únicamente cuando el nombre del radar lo menciona de forma explícita.
     */
    private static String deducirFabricante(String modelo) {
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
     * Ejecuta una petición HTTP GET segura con User-Agent, codificación UTF-8 y cierre garantizado.
     */
    private static String peticionHttpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Consulta síncrona directa al catálogo maestro local por código ICAO.
     */
    public static Avion getAvionPorTipoIcao(String tipoIcao) {
        if (tipoIcao == null) return null;
        AvionData data = MAPA_POR_ICAO.get(tipoIcao.trim().toUpperCase(Locale.ROOT));
        return (data != null) ? crearAvionDesdeData(data) : null;
    }

    /**
     * Devuelve una copia de todas las aeronaves registradas en el catálogo maestro local.
     */
    public static List<Avion> getTodosLosAviones() {
        List<Avion> lista = new ArrayList<>();
        for (AvionData d : DATOS_AVIONES) {
            lista.add(crearAvionDesdeData(d));
        }
        return lista;
    }

    /**
     * Traduce la clave interna de rareza al idioma configurado en la aplicación.
     */
    public static String traducirRareza(Context context, String rareza) {
        if (rareza == null) return context.getString(R.string.rareza_common);
        switch (rareza.toUpperCase(Locale.ROOT)) {
            case "RARE": return context.getString(R.string.rareza_rare);
            case "EPIC": return context.getString(R.string.rareza_epic);
            case "LEGENDARY": return context.getString(R.string.rareza_legendary);
            case "MYTHIC": return context.getString(R.string.rareza_mythic);
            default: return context.getString(R.string.rareza_common);
        }
    }
}