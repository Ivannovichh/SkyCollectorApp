// Paquete que agrupa las clases de utilidad del proyecto
package es.medac.skycollectorapp.utils;

// Importaciones de colecciones de Java y componentes propios de la aplicación
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.medac.skycollectorapp.R;
import es.medac.skycollectorapp.models.Avion;

/**
 * Clase de utilidad encargada de centralizar el catálogo técnico de aeronaves
 * y mapear sus códigos de tipo ICAO para su identificación en tiempo real en el mapa.
 */
public class AvionGenerator {

    // ------------------------------
    // CATÁLOGO PRINCIPAL CON ARTE PROPIO
    // ------------------------------

    /**
     * Arreglo con los modelos principales que disponen de ilustración dedicada.
     * Formato: Modelo, Fabricante, Rareza, Drawable, Velocidad, Capacidad, Envergadura, País, Peso máximo.
     */
    private static final AvionData[] DATOS_AVIONES = {

            // Aeronaves de categoría común (COMMON)
            new AvionData("Boeing 737-800", "Boeing", "COMMON", R.drawable.boeing_737, "842 km/h", "189 pax", "36m", "EE.UU.", "79t"),
            new AvionData("Airbus A320", "Airbus", "COMMON", R.drawable.airbus_a320, "828 km/h", "180 pax", "36m", "Europa", "78t"),
            new AvionData("Cessna 172 Skyhawk", "Cessna", "COMMON", R.drawable.cessna_172, "226 km/h", "4 pax", "11m", "EE.UU.", "1.1t"),
            new AvionData("Piper PA-28", "Piper", "COMMON", R.drawable.piper_pa28, "217 km/h", "4 pax", "10m", "EE.UU.", "1.1t"),
            new AvionData("ATR 72", "ATR", "COMMON", R.drawable.atr_72, "510 km/h", "78 pax", "27m", "Francia/Italia", "23t"),
            new AvionData("Bombardier CRJ-900", "Bombardier", "COMMON", R.drawable.crj_900, "871 km/h", "90 pax", "25m", "Canadá", "38t"),
            new AvionData("Robinson R44", "Robinson", "COMMON", R.drawable.robinson_r44, "240 km/h", "4 pax", "10m", "EE.UU.", "1.1t"),
            new AvionData("Embraer E190", "Embraer", "COMMON", R.drawable.embraer_e190, "871 km/h", "114 pax", "29m", "Brasil", "51t"),
            new AvionData("Beechcraft Baron", "Beechcraft", "COMMON", R.drawable.beechcraft_baron, "370 km/h", "6 pax", "11.5m", "EE.UU.", "2.3t"),
            new AvionData("Diamond DA42", "Diamond", "COMMON", R.drawable.diamond_da42, "365 km/h", "4 pax", "13.5m", "Austria", "1.7t"),
            new AvionData("Bell 206 JetRanger", "Bell", "COMMON", R.drawable.bell_206, "224 km/h", "5 pax", "10m", "EE.UU.", "1.4t"),
            new AvionData("Cessna 152", "Cessna", "COMMON", R.drawable.cessna_152, "198 km/h", "2 pax", "10m", "EE.UU.", "757kg"),

            // Aeronaves de categoría rara (RARE)
            new AvionData("Boeing 787 Dreamliner", "Boeing", "RARE", R.drawable.boeing_787, "903 km/h", "242 pax", "60m", "EE.UU.", "227t"),
            new AvionData("Airbus A350 XWB", "Airbus", "RARE", R.drawable.airbus_a350, "903 km/h", "325 pax", "65m", "Europa", "280t"),
            new AvionData("Eurofighter Typhoon", "Eurofighter", "RARE", R.drawable.eurofighter, "2495 km/h", "1 piloto", "11m", "Europa", "23t"),
            new AvionData("F-16 Fighting Falcon", "General Dynamics", "RARE", R.drawable.f16, "2120 km/h", "1 piloto", "10m", "EE.UU.", "19t"),
            new AvionData("C-130 Hercules", "Lockheed", "RARE", R.drawable.c130_hercules, "671 km/h", "Carga", "40m", "EE.UU.", "70t"),
            new AvionData("Learjet 60", "Bombardier", "RARE", R.drawable.learjet_60, "839 km/h", "8 pax", "13.5m", "EE.UU.", "10t"),
            new AvionData("Boeing 777-300ER", "Boeing", "RARE", R.drawable.boeing_777, "905 km/h", "396 pax", "65m", "EE.UU.", "351t"),
            new AvionData("Airbus A330", "Airbus", "RARE", R.drawable.airbus_a330, "871 km/h", "277 pax", "60m", "Europa", "242t"),
            new AvionData("Gulfstream G650", "Gulfstream", "RARE", R.drawable.gulfstream_g650, "956 km/h", "19 pax", "30m", "EE.UU.", "45t"),
            new AvionData("Dassault Falcon 7X", "Dassault", "RARE", R.drawable.falcon_7x, "900 km/h", "12 pax", "26m", "Francia", "31t"),
            new AvionData("Mirage 2000", "Dassault", "RARE", R.drawable.mirage_2000, "2336 km/h", "1 piloto", "9m", "Francia", "17t"),
            new AvionData("Saab 39 Gripen", "Saab", "RARE", R.drawable.saab_gripen, "2204 km/h", "1 piloto", "8.4m", "Suecia", "14t"),
            new AvionData("CH-47 Chinook", "Boeing", "RARE", R.drawable.ch47_chinook, "315 km/h", "55 tropas", "18m", "EE.UU.", "22t"),

            // Aeronaves de categoría épica (EPIC)
            new AvionData("Airbus A380", "Airbus", "EPIC", R.drawable.airbus_a380, "1020 km/h", "853 pax", "80m", "Europa", "575t"),
            new AvionData("Boeing 747-400", "Boeing", "EPIC", R.drawable.boeing_747, "988 km/h", "416 pax", "64m", "EE.UU.", "396t"),
            new AvionData("F-35 Lightning II", "Lockheed Martin", "EPIC", R.drawable.f35, "1960 km/h", "1 piloto", "11m", "EE.UU.", "31t"),
            new AvionData("AH-64 Apache", "Boeing", "EPIC", R.drawable.apache_ah64, "293 km/h", "2 pilotos", "15m", "EE.UU.", "10t"),
            new AvionData("V-22 Osprey", "Bell Boeing", "EPIC", R.drawable.v22_osprey, "509 km/h", "24 tropas", "26m", "EE.UU.", "27t"),
            new AvionData("F/A-18 Super Hornet", "Boeing", "EPIC", R.drawable.fa18_hornet, "1915 km/h", "1 piloto", "14m", "EE.UU.", "29t"),
            new AvionData("Antonov An-124", "Antonov", "EPIC", R.drawable.a124, "865 km/h", "Carga", "73m", "URSS", "405t"),
            new AvionData("Airbus Beluga XL", "Airbus", "EPIC", R.drawable.beluga_xl, "737 km/h", "Carga Esp.", "60m", "Europa", "227t"),
            new AvionData("Boeing Dreamlifter", "Boeing", "EPIC", R.drawable.dreamlifter, "878 km/h", "Carga Esp.", "64m", "EE.UU.", "364t"),
            new AvionData("Mi-24 Hind", "Mil", "EPIC", R.drawable.mi24_hind, "335 km/h", "8 tropas", "17m", "URSS", "12t"),

            // Aeronaves de categoría legendaria (LEGENDARY)
            new AvionData("F-22 Raptor", "Lockheed Martin", "LEGENDARY", R.drawable.f22, "2414 km/h", "1 piloto", "14m", "EE.UU.", "38t"),
            new AvionData("Spitfire", "Supermarine", "LEGENDARY", R.drawable.spitfire, "584 km/h", "1 piloto", "11m", "UK", "3t"),
            new AvionData("P-51 Mustang", "North American", "LEGENDARY", R.drawable.p51_mustang, "703 km/h", "1 piloto", "11m", "EE.UU.", "5t"),
            new AvionData("Me 262", "Messerschmitt", "LEGENDARY", R.drawable.me_262, "900 km/h", "1 piloto", "12.5m", "Alemania", "7t"),
            new AvionData("Harrier Jump Jet", "Hawker Siddeley", "LEGENDARY", R.drawable.harrier, "1176 km/h", "1 piloto", "9.2m", "UK", "11t")
    };

    // ------------------------------
    // ESTRUCTURA INTERNA Y SILUETAS
    // ------------------------------

    /**
     * Estructura interna inmutable que agrupa los metadatos técnicos de cada aeronave del catálogo.
     */
    private static class AvionData {
        final String modelo, fabricante, rareza, vel, cap, dim, pais, peso;
        final int imgId;

        /**
         * Construye una ficha técnica completa con todos sus parámetros de rendimiento.
         */
        AvionData(String m, String f, String r, int i, String v, String c, String d, String p, String pe) {
            modelo = m;
            fabricante = f;
            rareza = r;
            imgId = i;
            vel = v;
            cap = c;
            dim = d;
            pais = p;
            peso = pe;
        }
    }

    // Identificadores de siluetas genéricas para los modelos sin ilustración dedicada
    private static final int SIL_JET = R.drawable.sil_jet;
    private static final int SIL_TURBOHELICE = R.drawable.sil_turbohelice;
    private static final int SIL_LIGERO = R.drawable.sil_ligero;
    private static final int SIL_HELICOPTERO = R.drawable.sil_helicoptero;
    private static final int SIL_CAZA = R.drawable.sil_caza;
    private static final int SIL_CARGA = R.drawable.sil_carga;
    private static final int SIL_HISTORICO = R.drawable.sil_historico;

    // Almacén dinámico para modelos adicionales detectables en el radar ADS-B
    private static final List<AvionData> DATOS_NUEVOS = new ArrayList<>();

    // Mapa inverso que vincula cada código de tipo ICAO con el nombre exacto del modelo en el catálogo
    private static final Map<String, String> TIPOS_ICAO = new HashMap<>();

    // ------------------------------
    // INICIALIZACIÓN DE CÓDIGOS ICAO
    // ------------------------------

    // Bloque estático que registra los códigos ICAO de todos los modelos activos en vuelo
    static {
        // Mapeo ICAO del catálogo principal
        registrarTipos("Boeing 737-800", "B738");
        registrarTipos("Airbus A320", "A320");
        registrarTipos("Cessna 172 Skyhawk", "C172", "C72R");
        registrarTipos("Piper PA-28", "P28A", "P28B", "P28R", "P28S", "P28T", "P28U");
        registrarTipos("ATR 72", "AT72", "AT73", "AT75", "AT76");
        registrarTipos("Bombardier CRJ-900", "CRJ7", "CRJ9", "CRJX");
        registrarTipos("Robinson R44", "R44");
        registrarTipos("Embraer E190", "E190", "E195");
        registrarTipos("Beechcraft Baron", "BE55", "BE58", "B58T");
        registrarTipos("Diamond DA42", "DA42", "D42P");
        registrarTipos("Bell 206 JetRanger", "B06", "B06T");
        registrarTipos("Cessna 152", "C152");
        registrarTipos("Boeing 787 Dreamliner", "B788", "B789", "B78X");
        registrarTipos("Airbus A350 XWB", "A359", "A35K");
        registrarTipos("Eurofighter Typhoon", "EUFI");
        registrarTipos("F-16 Fighting Falcon", "F16");
        registrarTipos("C-130 Hercules", "C130", "C30J", "L100");
        registrarTipos("Learjet 60", "LJ60");
        registrarTipos("Boeing 777-300ER", "B77W");
        registrarTipos("Airbus A330", "A332", "A333");
        registrarTipos("Gulfstream G650", "GLF6");
        registrarTipos("Dassault Falcon 7X", "FA7X");
        registrarTipos("Mirage 2000", "MIR2");
        registrarTipos("Saab 39 Gripen", "SB39");
        registrarTipos("CH-47 Chinook", "H47");
        registrarTipos("Airbus A380", "A388");
        registrarTipos("Boeing 747-400", "B744");
        registrarTipos("F-35 Lightning II", "F35");
        registrarTipos("AH-64 Apache", "AH64");
        registrarTipos("V-22 Osprey", "V22");
        registrarTipos("F/A-18 Super Hornet", "F18S", "F18H");
        registrarTipos("Antonov An-124", "A124");
        registrarTipos("Airbus Beluga XL", "A337");
        registrarTipos("Boeing Dreamlifter", "BLCF");
        registrarTipos("Mi-24 Hind", "MI24");
        registrarTipos("F-22 Raptor", "F22");
        registrarTipos("Spitfire", "SPIT");
        registrarTipos("P-51 Mustang", "P51");
        registrarTipos("Me 262", "ME62");
        registrarTipos("Harrier Jump Jet", "HAR");

        // Reactores comerciales de Airbus
        anadir("Airbus A220-100", "Airbus", "RARE", SIL_JET, "828 km/h", "130 pax", "35m", "Canadá/Europa", "63t", "BCS1");
        anadir("Airbus A220-300", "Airbus", "RARE", SIL_JET, "828 km/h", "145 pax", "35m", "Canadá/Europa", "71t", "BCS3");
        anadir("Airbus A318", "Airbus", "RARE", SIL_JET, "828 km/h", "117 pax", "34m", "Europa", "68t", "A318");
        anadir("Airbus A319", "Airbus", "COMMON", SIL_JET, "828 km/h", "134 pax", "34m", "Europa", "75t", "A319");
        anadir("Airbus A321", "Airbus", "COMMON", SIL_JET, "828 km/h", "185 pax", "34m", "Europa", "93t", "A321");
        anadir("Airbus A319neo", "Airbus", "RARE", SIL_JET, "828 km/h", "140 pax", "36m", "Europa", "75t", "A19N");
        anadir("Airbus A320neo", "Airbus", "COMMON", SIL_JET, "828 km/h", "165 pax", "36m", "Europa", "79t", "A20N");
        anadir("Airbus A321neo", "Airbus", "COMMON", SIL_JET, "828 km/h", "206 pax", "36m", "Europa", "97t", "A21N");
        anadir("Airbus A300-600", "Airbus", "RARE", SIL_JET, "833 km/h", "266 pax", "45m", "Europa", "171t", "A306", "A30B");
        anadir("Airbus A310", "Airbus", "EPIC", SIL_JET, "850 km/h", "220 pax", "44m", "Europa", "150t", "A310");
        anadir("Airbus A330neo", "Airbus", "RARE", SIL_JET, "880 km/h", "287 pax", "64m", "Europa", "251t", "A338", "A339");
        anadir("Airbus A340-300", "Airbus", "EPIC", SIL_JET, "880 km/h", "295 pax", "60m", "Europa", "276t", "A342", "A343");
        anadir("Airbus A340-500", "Airbus", "EPIC", SIL_JET, "900 km/h", "313 pax", "63m", "Europa", "380t", "A345");
        anadir("Airbus A340-600", "Airbus", "EPIC", SIL_JET, "890 km/h", "380 pax", "64m", "Europa", "380t", "A346");

        // Reactores comerciales de Boeing
        anadir("Boeing 707", "Boeing", "LEGENDARY", SIL_JET, "965 km/h", "189 pax", "44m", "EE.UU.", "151t", "B703");
        anadir("Boeing 717-200", "Boeing", "RARE", SIL_JET, "811 km/h", "106 pax", "28m", "EE.UU.", "55t", "B712");
        anadir("Boeing 727-200", "Boeing", "EPIC", SIL_JET, "960 km/h", "189 pax", "33m", "EE.UU.", "95t", "B721", "B722");
        anadir("Boeing 737-200", "Boeing", "EPIC", SIL_JET, "780 km/h", "130 pax", "28m", "EE.UU.", "58t", "B732");
        anadir("Boeing 737-300", "Boeing", "RARE", SIL_JET, "800 km/h", "149 pax", "29m", "EE.UU.", "63t", "B733");
        anadir("Boeing 737-400", "Boeing", "RARE", SIL_JET, "800 km/h", "168 pax", "29m", "EE.UU.", "68t", "B734");
        anadir("Boeing 737-500", "Boeing", "EPIC", SIL_JET, "800 km/h", "132 pax", "29m", "EE.UU.", "60t", "B735");
        anadir("Boeing 737-600", "Boeing", "EPIC", SIL_JET, "828 km/h", "110 pax", "34m", "EE.UU.", "66t", "B736");
        anadir("Boeing 737-700", "Boeing", "COMMON", SIL_JET, "828 km/h", "140 pax", "36m", "EE.UU.", "78t", "B737");
        anadir("Boeing 737-900", "Boeing", "RARE", SIL_JET, "828 km/h", "180 pax", "36m", "EE.UU.", "85t", "B739");
        anadir("Boeing 737 MAX 7", "Boeing", "RARE", SIL_JET, "839 km/h", "153 pax", "36m", "EE.UU.", "80t", "B37M");
        anadir("Boeing 737 MAX 8", "Boeing", "COMMON", SIL_JET, "839 km/h", "178 pax", "36m", "EE.UU.", "82t", "B38M");
        anadir("Boeing 737 MAX 9", "Boeing", "RARE", SIL_JET, "839 km/h", "204 pax", "36m", "EE.UU.", "88t", "B39M");
        anadir("Boeing 737 MAX 10", "Boeing", "RARE", SIL_JET, "839 km/h", "230 pax", "36m", "EE.UU.", "90t", "B3XM");
        anadir("Boeing 747-200", "Boeing", "LEGENDARY", SIL_JET, "894 km/h", "366 pax", "60m", "EE.UU.", "377t", "B741", "B742");
        anadir("Boeing 747-300", "Boeing", "LEGENDARY", SIL_JET, "907 km/h", "400 pax", "60m", "EE.UU.", "378t", "B743");
        anadir("Boeing 747SP", "Boeing", "LEGENDARY", SIL_JET, "920 km/h", "276 pax", "60m", "EE.UU.", "318t", "B74S");
        anadir("Boeing 747-8", "Boeing", "EPIC", SIL_JET, "920 km/h", "467 pax", "68m", "EE.UU.", "448t", "B748");
        anadir("Boeing 757-200", "Boeing", "RARE", SIL_JET, "850 km/h", "200 pax", "38m", "EE.UU.", "115t", "B752");
        anadir("Boeing 757-300", "Boeing", "EPIC", SIL_JET, "850 km/h", "243 pax", "38m", "EE.UU.", "123t", "B753");
        anadir("Boeing 767-200", "Boeing", "EPIC", SIL_JET, "851 km/h", "216 pax", "48m", "EE.UU.", "143t", "B762");
        anadir("Boeing 767-300", "Boeing", "RARE", SIL_JET, "851 km/h", "261 pax", "48m", "EE.UU.", "187t", "B763");
        anadir("Boeing 767-400ER", "Boeing", "EPIC", SIL_JET, "851 km/h", "245 pax", "52m", "EE.UU.", "204t", "B764");
        anadir("Boeing 777-200", "Boeing", "RARE", SIL_JET, "892 km/h", "305 pax", "61m", "EE.UU.", "298t", "B772");
        anadir("Boeing 777-300", "Boeing", "RARE", SIL_JET, "892 km/h", "368 pax", "61m", "EE.UU.", "299t", "B773");
        anadir("Boeing 777 Freighter", "Boeing", "RARE", SIL_JET, "896 km/h", "Carga", "65m", "EE.UU.", "347t", "B77L");
        anadir("Boeing 777-8", "Boeing", "EPIC", SIL_JET, "905 km/h", "395 pax", "72m", "EE.UU.", "351t", "B778");
        anadir("Boeing 777-9", "Boeing", "EPIC", SIL_JET, "905 km/h", "426 pax", "72m", "EE.UU.", "351t", "B779");

        // Reactores de Douglas, McDonnell Douglas y Lockheed
        anadir("Douglas DC-8", "Douglas", "LEGENDARY", SIL_JET, "875 km/h", "Carga/189 pax", "45m", "EE.UU.", "159t", "DC85", "DC86", "DC87");
        anadir("McDonnell Douglas MD-80", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "155 pax", "33m", "EE.UU.", "68t", "MD81", "MD82", "MD83", "MD87", "MD88");
        anadir("McDonnell Douglas MD-90", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "160 pax", "33m", "EE.UU.", "76t", "MD90");
        anadir("McDonnell Douglas MD-11", "McDonnell Douglas", "EPIC", SIL_JET, "876 km/h", "293 pax", "52m", "EE.UU.", "286t", "MD11");
        anadir("McDonnell Douglas DC-9", "McDonnell Douglas", "LEGENDARY", SIL_JET, "804 km/h", "115 pax", "28m", "EE.UU.", "49t", "DC91", "DC92", "DC93", "DC94", "DC95");
        anadir("McDonnell Douglas DC-10", "McDonnell Douglas", "EPIC", SIL_JET, "875 km/h", "270 pax", "50m", "EE.UU.", "260t", "DC10");
        anadir("Lockheed L-1011 TriStar", "Lockheed", "LEGENDARY", SIL_JET, "960 km/h", "256 pax", "47m", "EE.UU.", "195t", "L101");

        // Reactores regionales
        anadir("Embraer E170", "Embraer", "RARE", SIL_JET, "828 km/h", "72 pax", "26m", "Brasil", "37t", "E170");
        anadir("Embraer E175", "Embraer", "COMMON", SIL_JET, "828 km/h", "78 pax", "26m", "Brasil", "40t", "E75L", "E75S");
        anadir("Embraer E190-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "106 pax", "34m", "Brasil", "57t", "E290");
        anadir("Embraer E195-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "132 pax", "35m", "Brasil", "61t", "E295");
        anadir("Embraer ERJ-145", "Embraer", "COMMON", SIL_JET, "828 km/h", "50 pax", "20m", "Brasil", "22t", "E135", "E140", "E145");
        anadir("Bombardier CRJ-200", "Bombardier", "COMMON", SIL_JET, "830 km/h", "50 pax", "21m", "Canadá", "24t", "CRJ1", "CRJ2");
        anadir("Fokker F28 Fellowship", "Fokker", "LEGENDARY", SIL_JET, "800 km/h", "85 pax", "25m", "Países Bajos", "33t", "F28");
        anadir("Fokker 70", "Fokker", "EPIC", SIL_JET, "845 km/h", "79 pax", "28m", "Países Bajos", "40t", "F70");
        anadir("Fokker 100", "Fokker", "EPIC", SIL_JET, "845 km/h", "109 pax", "28m", "Países Bajos", "46t", "F100");
        anadir("BAe 146", "British Aerospace", "EPIC", SIL_JET, "789 km/h", "100 pax", "26m", "UK", "44t", "B461", "B462", "B463", "RJ70", "RJ85", "RJ1H");
        anadir("Sukhoi Superjet 100", "Sukhoi", "EPIC", SIL_JET, "828 km/h", "98 pax", "28m", "Rusia", "49t", "SU95");
        anadir("Comac C919", "Comac", "EPIC", SIL_JET, "828 km/h", "158 pax", "36m", "China", "78t", "C919");
        anadir("Comac ARJ21", "Comac", "EPIC", SIL_JET, "828 km/h", "90 pax", "27m", "China", "41t", "AJ27");

        // Reactores soviéticos y rusos
        anadir("Tupolev Tu-154", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "180 pax", "38m", "URSS", "100t", "T154");
        anadir("Tupolev Tu-134", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "76 pax", "29m", "URSS", "47t", "T134");
        anadir("Tupolev Tu-204", "Tupolev", "EPIC", SIL_JET, "810 km/h", "210 pax", "42m", "Rusia", "110t", "T204");
        anadir("Ilyushin Il-62", "Ilyushin", "LEGENDARY", SIL_JET, "900 km/h", "186 pax", "43m", "URSS", "165t", "IL62");
        anadir("Ilyushin Il-96", "Ilyushin", "EPIC", SIL_JET, "850 km/h", "300 pax", "60m", "Rusia", "250t", "IL96");
        anadir("Antonov An-72", "Antonov", "EPIC", SIL_JET, "705 km/h", "68 pax", "32m", "URSS", "35t", "AN72", "AN74");
        anadir("Antonov An-148", "Antonov", "EPIC", SIL_JET, "835 km/h", "75 pax", "29m", "Ucrania", "43t", "A148");
        anadir("Yakovlev Yak-40", "Yakovlev", "LEGENDARY", SIL_JET, "550 km/h", "32 pax", "25m", "URSS", "16t", "YK40");
        anadir("Yakovlev Yak-42", "Yakovlev", "EPIC", SIL_JET, "810 km/h", "120 pax", "35m", "URSS", "57t", "YK42");

        // Reactores privados y ejecutivos
        anadir("Bombardier Challenger 350", "Bombardier", "RARE", SIL_JET, "870 km/h", "10 pax", "21m", "Canadá", "18t", "CL35");
        anadir("Bombardier Challenger 604", "Bombardier", "RARE", SIL_JET, "870 km/h", "12 pax", "20m", "Canadá", "22t", "CL60");
        anadir("Bombardier Global Express", "Bombardier", "RARE", SIL_JET, "902 km/h", "17 pax", "29m", "Canadá", "45t", "GLEX", "GL5T", "GL6T", "GL7T");
        anadir("Gulfstream G280", "Gulfstream", "RARE", SIL_JET, "850 km/h", "10 pax", "20m", "EE.UU.", "18t", "G280");
        anadir("Gulfstream G450", "Gulfstream", "RARE", SIL_JET, "904 km/h", "16 pax", "24m", "EE.UU.", "34t", "GLF4");
        anadir("Gulfstream G550", "Gulfstream", "RARE", SIL_JET, "904 km/h", "19 pax", "28.5m", "EE.UU.", "41t", "GLF5");
        anadir("Gulfstream G700", "Gulfstream", "EPIC", SIL_JET, "904 km/h", "19 pax", "31m", "EE.UU.", "48t", "GLF7");
        anadir("Dassault Falcon 900", "Dassault", "RARE", SIL_JET, "870 km/h", "14 pax", "19m", "Francia", "21t", "F900");
        anadir("Dassault Falcon 2000", "Dassault", "RARE", SIL_JET, "870 km/h", "10 pax", "21m", "Francia", "19t", "F2TH");
        anadir("Dassault Falcon 8X", "Dassault", "RARE", SIL_JET, "880 km/h", "16 pax", "26m", "Francia", "33t", "FA8X");
        anadir("Cessna Citation Mustang", "Cessna", "RARE", SIL_JET, "630 km/h", "4 pax", "13m", "EE.UU.", "4t", "C510");
        anadir("Cessna Citation CJ", "Cessna", "RARE", SIL_JET, "770 km/h", "8 pax", "15m", "EE.UU.", "6t", "C525", "C25A", "C25B", "C25C");
        anadir("Cessna Citation XLS", "Cessna", "RARE", SIL_JET, "815 km/h", "9 pax", "17m", "EE.UU.", "9t", "C56X");
        anadir("Cessna Citation X", "Cessna", "EPIC", SIL_JET, "900 km/h", "9 pax", "19m", "EE.UU.", "16t", "C750");
        anadir("Embraer Phenom 100", "Embraer", "RARE", SIL_JET, "720 km/h", "4 pax", "12m", "Brasil", "5t", "E50P");
        anadir("Embraer Phenom 300", "Embraer", "RARE", SIL_JET, "839 km/h", "9 pax", "16m", "Brasil", "8t", "E55P");
        anadir("HondaJet", "Honda", "RARE", SIL_JET, "782 km/h", "5 pax", "12m", "Japón/EE.UU.", "5t", "HDJT");
        anadir("Pilatus PC-24", "Pilatus", "RARE", SIL_JET, "815 km/h", "8 pax", "17m", "Suiza", "8t", "PC24");
        anadir("Learjet 45", "Learjet", "RARE", SIL_JET, "860 km/h", "9 pax", "14.5m", "EE.UU.", "9t", "LJ45");
        anadir("Learjet 35", "Learjet", "EPIC", SIL_JET, "830 km/h", "8 pax", "12m", "EE.UU.", "8t", "LJ35");
        anadir("Hawker 800", "Hawker", "RARE", SIL_JET, "830 km/h", "8 pax", "16m", "UK/EE.UU.", "12t", "H25B");
        anadir("Eclipse 500", "Eclipse", "RARE", SIL_JET, "685 km/h", "5 pax", "11.5m", "EE.UU.", "3t", "EA50");
        anadir("Cirrus Vision Jet", "Cirrus", "RARE", SIL_JET, "556 km/h", "5 pax", "12m", "EE.UU.", "3t", "SF50");

        // Turbohélices comerciales, regionales y utilitarios
        anadir("ATR 42", "ATR", "RARE", SIL_TURBOHELICE, "556 km/h", "48 pax", "25m", "Francia/Italia", "19t", "AT43", "AT44", "AT45", "AT46");
        anadir("De Havilland Dash 8-100/200", "De Havilland", "RARE", SIL_TURBOHELICE, "500 km/h", "37 pax", "26m", "Canadá", "16t", "DH8A", "DH8B");
        anadir("De Havilland Dash 8-300", "De Havilland", "RARE", SIL_TURBOHELICE, "528 km/h", "50 pax", "27m", "Canadá", "19t", "DH8C");
        anadir("Bombardier Dash 8 Q400", "Bombardier", "RARE", SIL_TURBOHELICE, "667 km/h", "78 pax", "28m", "Canadá", "29t", "DH8D");
        anadir("De Havilland Dash 7", "De Havilland", "EPIC", SIL_TURBOHELICE, "428 km/h", "50 pax", "28m", "Canadá", "20t", "DHC7");
        anadir("De Havilland Twin Otter", "De Havilland", "RARE", SIL_TURBOHELICE, "337 km/h", "19 pax", "20m", "Canadá", "6t", "DHC6");
        anadir("De Havilland Otter", "De Havilland", "RARE", SIL_TURBOHELICE, "260 km/h", "11 pax", "18m", "Canadá", "3.6t", "DHC3", "DH3T");
        anadir("Saab 340", "Saab", "RARE", SIL_TURBOHELICE, "467 km/h", "34 pax", "21m", "Suecia", "13t", "SF34");
        anadir("Saab 2000", "Saab", "EPIC", SIL_TURBOHELICE, "665 km/h", "50 pax", "25m", "Suecia", "23t", "SB20");
        anadir("Fokker F27 Friendship", "Fokker", "EPIC", SIL_TURBOHELICE, "480 km/h", "44 pax", "29m", "Países Bajos", "20t", "F27");
        anadir("Fokker 50", "Fokker", "EPIC", SIL_TURBOHELICE, "500 km/h", "56 pax", "29m", "Países Bajos", "21t", "F50");
        anadir("Embraer EMB-110 Bandeirante", "Embraer", "RARE", SIL_TURBOHELICE, "413 km/h", "19 pax", "15m", "Brasil", "6t", "E110");
        anadir("Embraer EMB-120 Brasilia", "Embraer", "RARE", SIL_TURBOHELICE, "552 km/h", "30 pax", "20m", "Brasil", "12t", "E120");
        anadir("BAe ATP", "British Aerospace", "EPIC", SIL_TURBOHELICE, "496 km/h", "64 pax", "31m", "UK", "23t", "ATP");
        anadir("Hawker Siddeley HS 748", "Hawker Siddeley", "EPIC", SIL_TURBOHELICE, "452 km/h", "48 pax", "30m", "UK", "21t", "A748");
        anadir("Short 330", "Short", "RARE", SIL_TURBOHELICE, "350 km/h", "30 pax", "23m", "UK", "10t", "SH33");
        anadir("Short 360", "Short", "RARE", SIL_TURBOHELICE, "400 km/h", "36 pax", "23m", "UK", "12t", "SH36");
        anadir("Short Skyvan", "Short", "RARE", SIL_TURBOHELICE, "324 km/h", "19 pax", "20m", "UK", "6t", "SC7");
        anadir("Dornier 228", "Dornier", "RARE", SIL_TURBOHELICE, "428 km/h", "19 pax", "17m", "Alemania", "6t", "D228");
        anadir("Dornier 328", "Dornier", "EPIC", SIL_TURBOHELICE, "620 km/h", "32 pax", "21m", "Alemania", "14t", "D328");
        anadir("BAe Jetstream 31", "British Aerospace", "RARE", SIL_TURBOHELICE, "430 km/h", "19 pax", "16m", "UK", "7t", "JS31", "JS32");
        anadir("BAe Jetstream 41", "British Aerospace", "RARE", SIL_TURBOHELICE, "482 km/h", "29 pax", "18m", "UK", "10t", "JS41");
        anadir("Fairchild Metroliner", "Fairchild", "RARE", SIL_TURBOHELICE, "576 km/h", "19 pax", "17m", "EE.UU.", "7t", "SW3", "SW4");
        anadir("Beechcraft 99", "Beechcraft", "RARE", SIL_TURBOHELICE, "450 km/h", "15 pax", "14m", "EE.UU.", "5t", "BE99");
        anadir("Beechcraft 1900D", "Beechcraft", "RARE", SIL_TURBOHELICE, "518 km/h", "19 pax", "18m", "EE.UU.", "8t", "B190");
        anadir("Beechcraft King Air 350", "Beechcraft", "RARE", SIL_TURBOHELICE, "578 km/h", "11 pax", "18m", "EE.UU.", "7t", "B350");
        anadir("Beechcraft King Air 200", "Beechcraft", "RARE", SIL_TURBOHELICE, "536 km/h", "9 pax", "17m", "EE.UU.", "6t", "BE20");
        anadir("Pilatus PC-12", "Pilatus", "RARE", SIL_TURBOHELICE, "528 km/h", "9 pax", "16m", "Suiza", "5t", "PC12", "P12E");
        anadir("Cessna 208 Caravan", "Cessna", "COMMON", SIL_TURBOHELICE, "344 km/h", "9 pax", "16m", "EE.UU.", "4t", "C208");
        anadir("Daher TBM 900", "Daher", "RARE", SIL_TURBOHELICE, "600 km/h", "6 pax", "12m", "Francia", "3.4t", "TBM7", "TBM8", "TBM9");
        anadir("Convair 580", "Convair", "EPIC", SIL_TURBOHELICE, "480 km/h", "50 pax", "32m", "EE.UU.", "25t", "CVLT");
        anadir("CASA C-295", "CASA", "RARE", SIL_TURBOHELICE, "480 km/h", "71 pax", "26m", "España", "23t", "C295");
        anadir("CASA CN-235", "CASA", "RARE", SIL_TURBOHELICE, "460 km/h", "44 pax", "26m", "España/Indonesia", "16t", "CN35");
        anadir("Pilatus PC-21", "Pilatus", "RARE", SIL_TURBOHELICE, "685 km/h", "2 pilotos", "9m", "Suiza", "3t", "PC21");
        anadir("Let L-410 Turbolet", "Let", "RARE", SIL_TURBOHELICE, "405 km/h", "19 pax", "20m", "Rep. Checa", "6.6t", "L410");
        anadir("Harbin Y-12", "Harbin", "RARE", SIL_TURBOHELICE, "328 km/h", "17 pax", "17m", "China", "5.3t", "Y12");
        anadir("Xian MA60", "Xian", "RARE", SIL_TURBOHELICE, "514 km/h", "60 pax", "29m", "China", "22t", "MA60");
        anadir("Antonov An-24", "Antonov", "EPIC", SIL_TURBOHELICE, "450 km/h", "50 pax", "29m", "URSS", "21t", "AN24");
        anadir("Antonov An-26", "Antonov", "EPIC", SIL_TURBOHELICE, "440 km/h", "Carga", "29m", "URSS", "24t", "AN26");
        anadir("Antonov An-32", "Antonov", "EPIC", SIL_TURBOHELICE, "530 km/h", "Carga", "29m", "URSS", "27t", "AN32");
        anadir("Ilyushin Il-18", "Ilyushin", "LEGENDARY", SIL_TURBOHELICE, "625 km/h", "110 pax", "37m", "URSS", "64t", "IL18");

        // Aviación ligera y commuters de pistón
        anadir("Cessna 150", "Cessna", "COMMON", SIL_LIGERO, "200 km/h", "2 pax", "10m", "EE.UU.", "0.7t", "C150");
        anadir("Cessna 182 Skylane", "Cessna", "COMMON", SIL_LIGERO, "280 km/h", "4 pax", "11m", "EE.UU.", "1.4t", "C182", "C82S", "C82T", "C82R");
        anadir("Cessna 206 Stationair", "Cessna", "RARE", SIL_LIGERO, "280 km/h", "6 pax", "11m", "EE.UU.", "1.6t", "C206", "T206");
        anadir("Cessna 210 Centurion", "Cessna", "RARE", SIL_LIGERO, "320 km/h", "6 pax", "11m", "EE.UU.", "1.7t", "C210");
        anadir("Cirrus SR20", "Cirrus", "COMMON", SIL_LIGERO, "290 km/h", "4 pax", "11.5m", "EE.UU.", "1.4t", "SR20");
        anadir("Cirrus SR22", "Cirrus", "COMMON", SIL_LIGERO, "340 km/h", "4 pax", "11.5m", "EE.UU.", "1.6t", "SR22");
        anadir("Diamond DA20", "Diamond", "COMMON", SIL_LIGERO, "250 km/h", "2 pax", "11m", "Canadá", "0.8t", "DA20");
        anadir("Diamond DA40", "Diamond", "COMMON", SIL_LIGERO, "280 km/h", "4 pax", "12m", "Austria", "1.2t", "DA40", "D40D");
        anadir("Piper PA-34 Seneca", "Piper", "COMMON", SIL_LIGERO, "315 km/h", "6 pax", "12m", "EE.UU.", "2t", "PA34");
        anadir("Piper PA-44 Seminole", "Piper", "COMMON", SIL_LIGERO, "320 km/h", "4 pax", "12m", "EE.UU.", "1.7t", "PA44");
        anadir("Piper PA-32 Cherokee Six", "Piper", "RARE", SIL_LIGERO, "270 km/h", "6 pax", "11m", "EE.UU.", "1.7t", "PA32");
        anadir("Piper PA-18 Super Cub", "Piper", "RARE", SIL_LIGERO, "185 km/h", "2 pax", "11m", "EE.UU.", "0.8t", "PA18");
        anadir("Piper PA-46 Malibu", "Piper", "RARE", SIL_LIGERO, "440 km/h", "6 pax", "13m", "EE.UU.", "1.4t", "PA46");
        anadir("Beechcraft Bonanza", "Beechcraft", "RARE", SIL_LIGERO, "330 km/h", "6 pax", "10m", "EE.UU.", "1.5t", "BE33", "BE35", "BE36");
        anadir("Mooney M20", "Mooney", "RARE", SIL_LIGERO, "280 km/h", "4 pax", "11m", "EE.UU.", "1.2t", "M20P", "M20J", "M20T");
        anadir("Robin DR400", "Robin", "COMMON", SIL_LIGERO, "250 km/h", "4 pax", "9m", "Francia", "1.1t", "DR40");
        anadir("Tecnam P2006T", "Tecnam", "RARE", SIL_LIGERO, "270 km/h", "4 pax", "11.5m", "Italia", "1.2t", "P06T");
        anadir("Extra 300", "Extra", "RARE", SIL_LIGERO, "340 km/h", "2 pax", "8m", "Alemania", "0.95t", "E300");
        anadir("Pitts Special", "Pitts", "RARE", SIL_LIGERO, "330 km/h", "2 pax", "6m", "EE.UU.", "0.7t", "PTS2");
        anadir("Britten-Norman Islander", "Britten-Norman", "RARE", SIL_LIGERO, "257 km/h", "9 pax", "15m", "UK", "3t", "BN2P");
        anadir("Britten-Norman Trislander", "Britten-Norman", "EPIC", SIL_LIGERO, "267 km/h", "16 pax", "16m", "UK", "4.5t", "TRIS");

        // Helicópteros
        anadir("Robinson R22", "Robinson", "COMMON", SIL_HELICOPTERO, "180 km/h", "2 pax", "7.7m", "EE.UU.", "0.6t", "R22");
        anadir("Robinson R66", "Robinson", "RARE", SIL_HELICOPTERO, "200 km/h", "4 pax", "10m", "EE.UU.", "1.3t", "R66");
        anadir("Airbus H125", "Airbus Helicopters", "COMMON", SIL_HELICOPTERO, "250 km/h", "6 pax", "11m", "Francia", "2.2t", "AS50");
        anadir("Airbus H135", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "254 km/h", "8 pax", "10m", "Alemania", "3t", "EC35");
        anadir("Airbus H145", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "246 km/h", "9 pax", "11m", "Alemania", "4t", "EC45");
        anadir("Airbus EC120 Colibri", "Airbus Helicopters", "RARE", SIL_HELICOPTERO, "223 km/h", "4 pax", "10m", "Francia", "1.7t", "EC20");
        anadir("Airbus H175", "Airbus Helicopters", "EPIC", SIL_HELICOPTERO, "300 km/h", "18 pax", "15m", "Francia", "8t", "EC75");
        anadir("Airbus H225 Super Puma", "Airbus Helicopters", "EPIC", SIL_HELICOPTERO, "260 km/h", "24 pax", "16m", "Francia", "11t", "EC25");
        anadir("Bell 407", "Bell", "RARE", SIL_HELICOPTERO, "260 km/h", "6 pax", "11m", "EE.UU.", "2.3t", "B407");
        anadir("Bell 412", "Bell", "RARE", SIL_HELICOPTERO, "260 km/h", "13 pax", "14m", "EE.UU.", "5.4t", "B412");
        anadir("Bell 429", "Bell", "RARE", SIL_HELICOPTERO, "278 km/h", "7 pax", "11m", "EE.UU.", "3.2t", "B429");
        anadir("Leonardo AW139", "Leonardo", "RARE", SIL_HELICOPTERO, "306 km/h", "15 pax", "14m", "Italia", "6.4t", "A139");
        anadir("Agusta A109", "Agusta", "RARE", SIL_HELICOPTERO, "285 km/h", "7 pax", "11m", "Italia", "3t", "A109");
        anadir("Sikorsky S-92", "Sikorsky", "EPIC", SIL_HELICOPTERO, "300 km/h", "19 pax", "17m", "EE.UU.", "12t", "S92");
        anadir("Sikorsky UH-60 Black Hawk", "Sikorsky", "EPIC", SIL_HELICOPTERO, "295 km/h", "11 tropas", "16m", "EE.UU.", "10t", "H60");
        anadir("Sikorsky CH-53E Super Stallion", "Sikorsky", "EPIC", SIL_HELICOPTERO, "315 km/h", "55 tropas", "24m", "EE.UU.", "33t", "H53");
        anadir("NHIndustries NH90", "NHIndustries", "EPIC", SIL_HELICOPTERO, "300 km/h", "20 tropas", "16m", "Europa", "11t", "NH90");
        anadir("Mil Mi-8", "Mil", "EPIC", SIL_HELICOPTERO, "260 km/h", "24 pax", "21m", "URSS", "12t", "MI8");
        anadir("Mil Mi-26", "Mil", "EPIC", SIL_HELICOPTERO, "295 km/h", "90 tropas", "32m", "URSS", "56t", "MI26");
        anadir("Bell UH-1 Huey", "Bell", "LEGENDARY", SIL_HELICOPTERO, "204 km/h", "12 tropas", "15m", "EE.UU.", "4.3t", "UH1");

        // Transporte, bombarderos y apoyo militar
        anadir("Airbus A400M Atlas", "Airbus", "EPIC", SIL_CARGA, "780 km/h", "116 tropas", "42m", "Europa", "141t", "A400");
        anadir("Airbus Beluga ST", "Airbus", "EPIC", SIL_CARGA, "750 km/h", "Carga Esp.", "45m", "Europa", "155t", "A3ST");
        anadir("Ilyushin Il-76", "Ilyushin", "EPIC", SIL_CARGA, "850 km/h", "Carga", "50m", "URSS", "195t", "IL76");
        anadir("Antonov An-12", "Antonov", "LEGENDARY", SIL_CARGA, "670 km/h", "Carga", "38m", "URSS", "61t", "AN12");
        anadir("Lockheed C-5 Galaxy", "Lockheed", "EPIC", SIL_CARGA, "833 km/h", "Carga", "68m", "EE.UU.", "381t", "C5M", "C5");
        anadir("Boeing C-17 Globemaster III", "Boeing", "EPIC", SIL_CARGA, "830 km/h", "Carga", "52m", "EE.UU.", "265t", "C17");
        anadir("Embraer KC-390", "Embraer", "EPIC", SIL_CARGA, "870 km/h", "Carga", "35m", "Brasil", "86t", "C390");
        anadir("Transall C-160", "Transall", "EPIC", SIL_CARGA, "513 km/h", "93 tropas", "40m", "Europa", "51t", "C160");
        anadir("Lockheed L-188 Electra", "Lockheed", "EPIC", SIL_CARGA, "600 km/h", "98 pax", "30m", "EE.UU.", "52t", "L188");
        anadir("Lockheed P-3 Orion", "Lockheed", "EPIC", SIL_CARGA, "761 km/h", "11 tripulantes", "30m", "EE.UU.", "61t", "P3");
        anadir("Boeing P-8 Poseidon", "Boeing", "EPIC", SIL_JET, "907 km/h", "9 tripulantes", "38m", "EE.UU.", "86t", "P8");
        anadir("Boeing E-3 Sentry", "Boeing", "EPIC", SIL_CARGA, "855 km/h", "20 tripulantes", "44m", "EE.UU.", "148t", "E3TF", "E3CF");
        anadir("Boeing KC-135 Stratotanker", "Boeing", "EPIC", SIL_CARGA, "933 km/h", "Cisterna", "40m", "EE.UU.", "146t", "K35R");
        anadir("Boeing B-52 Stratofortress", "Boeing", "EPIC", SIL_CARGA, "1047 km/h", "5 tripulantes", "56m", "EE.UU.", "220t", "B52");
        anadir("Rockwell B-1B Lancer", "Rockwell", "EPIC", SIL_CAZA, "1335 km/h", "4 tripulantes", "42m", "EE.UU.", "216t", "B1");

        // Cazas y entrenadores militares
        anadir("F-15 Eagle", "McDonnell Douglas", "EPIC", SIL_CAZA, "2655 km/h", "1 piloto", "13m", "EE.UU.", "31t", "F15");
        anadir("F-4 Phantom II", "McDonnell Douglas", "LEGENDARY", SIL_CAZA, "2370 km/h", "2 pilotos", "12m", "EE.UU.", "28t", "F4");
        anadir("A-10 Thunderbolt II", "Fairchild Republic", "EPIC", SIL_CAZA, "706 km/h", "1 piloto", "18m", "EE.UU.", "23t", "A10");
        anadir("Dassault Rafale", "Dassault", "EPIC", SIL_CAZA, "1912 km/h", "1 piloto", "11m", "Francia", "25t", "RFAL");
        anadir("Panavia Tornado", "Panavia", "EPIC", SIL_CAZA, "2400 km/h", "2 pilotos", "14m", "Europa", "28t", "TOR");
        anadir("Mikoyan MiG-29", "Mikoyan", "EPIC", SIL_CAZA, "2400 km/h", "1 piloto", "11m", "URSS", "18t", "MG29");
        anadir("Northrop F-5 Tiger II", "Northrop", "EPIC", SIL_CAZA, "1700 km/h", "1 piloto", "8m", "EE.UU.", "11t", "F5");
        anadir("Lockheed U-2 Dragon Lady", "Lockheed", "EPIC", SIL_CAZA, "800 km/h", "1 piloto", "31m", "EE.UU.", "18t", "U2");
        anadir("BAE Hawk", "BAE Systems", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "10m", "UK", "9t", "HAWK");
        anadir("Dassault/Dornier Alpha Jet", "Dassault/Dornier", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "9m", "Francia/Alemania", "8t", "ALPH");
        anadir("Aermacchi M-346", "Aermacchi", "RARE", SIL_CAZA, "1090 km/h", "2 pilotos", "10m", "Italia", "10t", "M346");
        anadir("Northrop T-38 Talon", "Northrop", "RARE", SIL_CAZA, "1380 km/h", "2 pilotos", "8m", "EE.UU.", "5t", "T38");
        anadir("Mikoyan MiG-21", "Mikoyan", "LEGENDARY", SIL_CAZA, "2175 km/h", "1 piloto", "7m", "URSS", "9t", "MG21");
        anadir("Mikoyan MiG-15", "Mikoyan", "LEGENDARY", SIL_CAZA, "1075 km/h", "1 piloto", "10m", "URSS", "5t", "MG15");
        anadir("F-86 Sabre", "North American", "LEGENDARY", SIL_CAZA, "1100 km/h", "1 piloto", "12m", "EE.UU.", "7t", "F86");

        // Históricos todavía activos en festivales y vuelos privados
        anadir("Douglas DC-3", "Douglas", "LEGENDARY", SIL_HISTORICO, "333 km/h", "32 pax", "29m", "EE.UU.", "11t", "DC3");
        anadir("Douglas DC-4", "Douglas", "LEGENDARY", SIL_HISTORICO, "365 km/h", "44 pax", "36m", "EE.UU.", "33t", "DC4");
        anadir("Douglas DC-6", "Douglas", "LEGENDARY", SIL_HISTORICO, "507 km/h", "66 pax", "36m", "EE.UU.", "48t", "DC6");
        anadir("Lockheed Constellation", "Lockheed", "LEGENDARY", SIL_HISTORICO, "555 km/h", "62 pax", "38m", "EE.UU.", "62t", "CONI");
        anadir("Curtiss C-46 Commando", "Curtiss", "LEGENDARY", SIL_HISTORICO, "435 km/h", "40 tropas", "33m", "EE.UU.", "22t", "C46");
        anadir("Junkers Ju 52", "Junkers", "LEGENDARY", SIL_HISTORICO, "265 km/h", "17 pax", "29m", "Alemania", "11t", "JU52");
        anadir("Boeing B-17 Flying Fortress", "Boeing", "LEGENDARY", SIL_HISTORICO, "462 km/h", "10 tripulantes", "32m", "EE.UU.", "29t", "B17");
        anadir("Boeing B-29 Superfortress", "Boeing", "LEGENDARY", SIL_HISTORICO, "574 km/h", "11 tripulantes", "43m", "EE.UU.", "62t", "B29");
        anadir("Avro Lancaster", "Avro", "LEGENDARY", SIL_HISTORICO, "454 km/h", "7 tripulantes", "31m", "UK", "30t", "LANC");
        anadir("Consolidated PBY Catalina", "Consolidated", "LEGENDARY", SIL_HISTORICO, "314 km/h", "9 tripulantes", "32m", "EE.UU.", "16t", "CATA");
        anadir("De Havilland Mosquito", "De Havilland", "LEGENDARY", SIL_HISTORICO, "668 km/h", "2 pilotos", "17m", "UK", "10t", "MOSQ");
        anadir("Hawker Hurricane", "Hawker", "LEGENDARY", SIL_HISTORICO, "547 km/h", "1 piloto", "12m", "UK", "3t", "HURR");
        anadir("Messerschmitt Bf 109", "Messerschmitt", "LEGENDARY", SIL_HISTORICO, "640 km/h", "1 piloto", "10m", "Alemania", "3.4t", "ME09");
        anadir("Focke-Wulf Fw 190", "Focke-Wulf", "LEGENDARY", SIL_HISTORICO, "653 km/h", "1 piloto", "10m", "Alemania", "4.4t", "FW19");
        anadir("Mitsubishi A6M Zero", "Mitsubishi", "LEGENDARY", SIL_HISTORICO, "533 km/h", "1 piloto", "12m", "Japón", "2.8t", "ZERO");
        anadir("Vought F4U Corsair", "Vought", "LEGENDARY", SIL_HISTORICO, "718 km/h", "1 piloto", "13m", "EE.UU.", "6t", "F4U");
        anadir("Grumman F6F Hellcat", "Grumman", "LEGENDARY", SIL_HISTORICO, "612 km/h", "1 piloto", "13m", "EE.UU.", "7t", "F6F");
        anadir("Lockheed P-38 Lightning", "Lockheed", "LEGENDARY", SIL_HISTORICO, "667 km/h", "1 piloto", "16m", "EE.UU.", "8t", "P38");
        anadir("Republic P-47 Thunderbolt", "Republic", "LEGENDARY", SIL_HISTORICO, "697 km/h", "1 piloto", "12m", "EE.UU.", "8t", "P47");
        anadir("North American T-6 Texan", "North American", "LEGENDARY", SIL_HISTORICO, "340 km/h", "2 pilotos", "13m", "EE.UU.", "2.4t", "T6");
        anadir("Boeing Stearman", "Boeing", "LEGENDARY", SIL_HISTORICO, "200 km/h", "2 pax", "10m", "EE.UU.", "1.2t", "ST75");
        anadir("De Havilland Tiger Moth", "De Havilland", "LEGENDARY", SIL_HISTORICO, "175 km/h", "2 pax", "9m", "UK", "0.8t", "DH82");
        anadir("Piper J-3 Cub", "Piper", "LEGENDARY", SIL_HISTORICO, "120 km/h", "2 pax", "11m", "EE.UU.", "0.5t", "J3");
        anadir("Antonov An-2", "Antonov", "LEGENDARY", SIL_HISTORICO, "190 km/h", "12 pax", "18m", "URSS", "5.5t", "AN2");
        anadir("De Havilland Beaver", "De Havilland", "LEGENDARY", SIL_HISTORICO, "255 km/h", "7 pax", "14m", "Canadá", "2.3t", "DHC2");
    }

    // ------------------------------
    // MÉTODOS AUXILIARES Y PÚBLICOS
    // ------------------------------

    /**
     * Registra uno o varios códigos de tipo ICAO asociados a un mismo nombre de modelo.
     */
    private static void registrarTipos(String modelo, String... codigos) {
        for (String c : codigos) {
            TIPOS_ICAO.put(c, modelo);
        }
    }

    /**
     * Añade un modelo nuevo al catálogo ampliado y vincula sus códigos ICAO para el radar.
     */
    private static void anadir(String modelo, String fabricante, String rareza, int imgId,
                               String vel, String cap, String dim, String pais, String peso,
                               String... codigos) {
        DATOS_NUEVOS.add(new AvionData(modelo, fabricante, rareza, imgId, vel, cap, dim, pais, peso));
        registrarTipos(modelo, codigos);
    }

    /**
     * Convierte una ficha interna {@link AvionData} en una instancia del modelo {@link Avion}.
     */
    private static Avion crearAvion(AvionData d) {
        return new Avion(d.modelo, d.fabricante, d.rareza, d.imgId, d.vel, d.cap, d.dim, d.pais, d.peso);
    }

    /**
     * Busca y devuelve la ficha del catálogo correspondiente al código ICAO detectado en el mapa.
     *
     * @param tipoIcao Código ICAO de 4 caracteres recibido desde la API de vuelos (ej. "B738", "A320").
     * @return Instancia de {@link Avion} con sus datos técnicos, o {@code null} si no está catalogado.
     */
    public static Avion getAvionPorTipoIcao(String tipoIcao) {
        if (tipoIcao == null) return null;

        String modelo = TIPOS_ICAO.get(tipoIcao.toUpperCase(Locale.ROOT));
        if (modelo == null) return null;

        for (AvionData d : DATOS_AVIONES) {
            if (d.modelo.equals(modelo)) return crearAvion(d);
        }

        for (AvionData d : DATOS_NUEVOS) {
            if (d.modelo.equals(modelo)) return crearAvion(d);
        }

        return null;
    }

    /**
     * Busca una ficha en el catálogo comparando el nombre oficial del modelo cuando no coincide el código ICAO.
     * Prioriza siempre las aeronaves con ilustración dedicada frente a las que tienen silueta genérica.
     */
    public static Avion getAvionPorNombreModelo(String nombreModeloOficial) {
        if (nombreModeloOficial == null || nombreModeloOficial.trim().isEmpty()) return null;
        String buscado = nombreModeloOficial.trim().toLowerCase(Locale.ROOT);

        for (AvionData d : DATOS_AVIONES) {
            String cat = d.modelo.toLowerCase(Locale.ROOT);
            if (cat.equals(buscado) || buscado.contains(cat) || cat.contains(buscado)) {
                return crearAvion(d);
            }
        }

        for (AvionData d : DATOS_NUEVOS) {
            String cat = d.modelo.toLowerCase(Locale.ROOT);
            if (cat.equals(buscado) || buscado.contains(cat)) {
                return crearAvion(d);
            }
        }

        return null;
    }

    /**
     * Construye y retorna la lista completa de todas las aeronaves detectables en el mapa.
     */
    public static List<Avion> getTodosLosAviones() {
        List<Avion> lista = new ArrayList<>();

        for (AvionData d : DATOS_AVIONES) {
            lista.add(crearAvion(d));
        }

        for (AvionData d : DATOS_NUEVOS) {
            lista.add(crearAvion(d));
        }

        return lista;
    }
}