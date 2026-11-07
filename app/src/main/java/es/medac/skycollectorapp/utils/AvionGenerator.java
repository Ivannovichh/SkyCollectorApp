// Declaración del paquete que contiene las clases de utilidad del proyecto
package es.medac.skycollectorapp.utils;

// Importación de los recursos de la aplicación para gestionar los identificadores de imágenes
import es.medac.skycollectorapp.R;
// Importación del modelo de datos de tipo Avión para la construcción de objetos
import es.medac.skycollectorapp.models.Avion;

// Definición de la clase encargada de centralizar y generar la base de datos de aviones
public class AvionGenerator {

    // Declaración de un arreglo constante que almacena la información técnica de todos los aviones
    private static final AvionData[] DATOS_AVIONES = {

            // Definición de un avión común con sus atributos: nombre, fabricante, rareza, imagen y datos técnicos
            new AvionData("Boeing 737-800", "Boeing", "COMMON", R.drawable.boeing_737, "842 km/h", "189 pax", "39m", "EE.UU.", "79t"),
            // Definición de datos para el modelo Airbus A320
            new AvionData("Airbus A320", "Airbus", "COMMON", R.drawable.airbus_a320, "828 km/h", "180 pax", "34m", "Europa", "78t"),
            // Definición de datos para la aeronave ligera Cessna 172
            new AvionData("Cessna 172 Skyhawk", "Cessna", "COMMON", R.drawable.cessna_172, "226 km/h", "4 pax", "11m", "EE.UU.", "1.1t"),
            // Definición de datos para el modelo Piper PA-28
            new AvionData("Piper PA-28", "Piper", "COMMON", R.drawable.piper_pa28, "217 km/h", "4 pax", "10m", "EE.UU.", "1.1t"),
            // Definición de datos para el avión regional turbohélice ATR 72
            new AvionData("ATR 72", "ATR", "COMMON", R.drawable.atr_72, "510 km/h", "78 pax", "27m", "Francia/Italia", "23t"),
            // Definición de datos para el jet regional Bombardier CRJ-900
            new AvionData("Bombardier CRJ-900", "Bombardier", "COMMON", R.drawable.crj_900, "871 km/h", "90 pax", "36m", "Canadá", "38t"),
            // Definición de datos para el helicóptero ligero Robinson R44
            new AvionData("Robinson R44", "Robinson", "COMMON", R.drawable.robinson_r44, "240 km/h", "4 pax", "11m", "EE.UU.", "1.1t"),
            // Definición de datos para el modelo brasileño Embraer E190
            new AvionData("Embraer E190", "Embraer", "COMMON", R.drawable.embraer_e190, "871 km/h", "114 pax", "36m", "Brasil", "51t"),
            // Definición de datos para el bimotor Beechcraft Baron
            new AvionData("Beechcraft Baron", "Beechcraft", "COMMON", R.drawable.beechcraft_baron, "370 km/h", "6 pax", "11m", "EE.UU.", "2.3t"),
            // Definición de datos para el Diamond DA42 austríaco
            new AvionData("Diamond DA42", "Diamond", "COMMON", R.drawable.diamond_da42, "365 km/h", "4 pax", "13m", "Austria", "1.7t"),
            // Definición de datos para el helicóptero Bell 206
            new AvionData("Bell 206 JetRanger", "Bell", "COMMON", R.drawable.bell_206, "224 km/h", "5 pax", "12m", "EE.UU.", "1.4t"),
            // Definición de datos para el modelo de instrucción Cessna 152
            new AvionData("Cessna 152", "Cessna", "COMMON", R.drawable.cessna_152, "198 km/h", "2 pax", "10m", "EE.UU.", "757kg"),

            // Registro de un modelo de categoría rara con sus especificaciones
            new AvionData("Boeing 787 Dreamliner", "Boeing", "RARE", R.drawable.boeing_787, "903 km/h", "242 pax", "60m", "EE.UU.", "227t"),
            // Definición de datos para el Airbus A350 avanzado
            new AvionData("Airbus A350 XWB", "Airbus", "RARE", R.drawable.airbus_a350, "903 km/h", "325 pax", "64m", "Europa", "280t"),
            // Definición de datos para el caza de combate Eurofighter
            new AvionData("Eurofighter Typhoon", "Eurofighter", "RARE", R.drawable.eurofighter, "2495 km/h", "1 piloto", "11m", "Europa", "23t"),
            // Definición de datos para el caza estadounidense F-16
            new AvionData("F-16 Fighting Falcon", "General Dynamics", "RARE", R.drawable.f16, "2120 km/h", "1 piloto", "10m", "EE.UU.", "19t"),
            // Definición de datos para el avión de transporte C-130 Hercules
            new AvionData("C-130 Hercules", "Lockheed", "RARE", R.drawable.c130_hercules, "671 km/h", "Carga", "40m", "EE.UU.", "70t"),
            // Definición de datos para el jet privado Learjet 60
            new AvionData("Learjet 60", "Bombardier", "RARE", R.drawable.learjet_60, "839 km/h", "8 pax", "17m", "EE.UU.", "10t"),
            // Definición de datos para el Boeing 777 de largo alcance
            new AvionData("Boeing 777-300ER", "Boeing", "RARE", R.drawable.boeing_777, "905 km/h", "396 pax", "73m", "EE.UU.", "351t"),
            // Definición de datos para el modelo Airbus A330
            new AvionData("Airbus A330", "Airbus", "RARE", R.drawable.airbus_a330, "871 km/h", "277 pax", "60m", "Europa", "242t"),
            // Definición de datos para el jet de negocios Gulfstream G650
            new AvionData("Gulfstream G650", "Gulfstream", "RARE", R.drawable.gulfstream_g650, "956 km/h", "19 pax", "30m", "EE.UU.", "45t"),
            // Definición de datos para el Falcon 7X francés
            new AvionData("Dassault Falcon 7X", "Dassault", "RARE", R.drawable.falcon_7x, "900 km/h", "12 pax", "23m", "Francia", "31t"),
            // Definición de datos para el caza interceptor Mirage 2000
            new AvionData("Mirage 2000", "Dassault", "RARE", R.drawable.mirage_2000, "2336 km/h", "1 piloto", "14m", "Francia", "17t"),
            // Definición de datos para el caza sueco Saab 39 Gripen
            new AvionData("Saab 39 Gripen", "Saab", "RARE", R.drawable.saab_gripen, "2204 km/h", "1 piloto", "14m", "Suecia", "14t"),
            // Definición de datos para el helicóptero de carga pesada Chinook
            new AvionData("CH-47 Chinook", "Boeing", "RARE", R.drawable.ch47_chinook, "315 km/h", "55 tropas", "30m", "EE.UU.", "22t"),

            // Registro de un avión de categoría épica como el Airbus A380
            new AvionData("Airbus A380", "Airbus", "EPIC", R.drawable.airbus_a380, "1020 km/h", "853 pax", "79m", "Europa", "575t"),
            // Definición de datos para el clásico Boeing 747
            new AvionData("Boeing 747-400", "Boeing", "EPIC", R.drawable.boeing_747, "988 km/h", "416 pax", "64m", "EE.UU.", "396t"),
            // Definición de datos para el caza furtivo F-35
            new AvionData("F-35 Lightning II", "Lockheed Martin", "EPIC", R.drawable.f35, "1960 km/h", "1 piloto", "10m", "EE.UU.", "31t"),
            // Definición de datos para el helicóptero de ataque Apache
            new AvionData("AH-64 Apache", "Boeing", "EPIC", R.drawable.apache_ah64, "293 km/h", "2 pilotos", "14m", "EE.UU.", "10t"),
            // Definición de datos para la aeronave de rotores basculantes Osprey
            new AvionData("V-22 Osprey", "Bell Boeing", "EPIC", R.drawable.v22_osprey, "509 km/h", "24 tropas", "17m", "EE.UU.", "27t"),
            // Definición de datos para el caza naval Super Hornet
            new AvionData("F/A-18 Super Hornet", "Boeing", "EPIC", R.drawable.fa18_hornet, "1915 km/h", "1 piloto", "18m", "EE.UU.", "29t"),
            // Definición de datos para el carguero soviético An-124
            new AvionData("Antonov An-124", "Antonov", "EPIC", R.drawable.a124, "865 km/h", "Carga", "69m", "URSS", "405t"),
            // Definición de datos para el avión de transporte especial Beluga XL
            new AvionData("Airbus Beluga XL", "Airbus", "EPIC", R.drawable.beluga_xl, "737 km/h", "Carga Esp.", "63m", "Europa", "227t"),
            // Definición de datos para el transporte de piezas Boeing Dreamlifter
            new AvionData("Boeing Dreamlifter", "Boeing", "EPIC", R.drawable.dreamlifter, "878 km/h", "Carga Esp.", "71m", "EE.UU.", "364t"),
            // Definición de datos para el helicóptero de combate Mi-24 Hind
            new AvionData("Mi-24 Hind", "Mil", "EPIC", R.drawable.mi24_hind, "335 km/h", "8 tropas", "17m", "URSS", "12t"),

            // Definición de datos para el caza de superioridad aérea F-22 Raptor
            new AvionData("F-22 Raptor", "Lockheed Martin", "LEGENDARY", R.drawable.f22, "2414 km/h", "1 piloto", "13m", "EE.UU.", "38t"),
            // Definición de datos para el caza histórico Spitfire
            new AvionData("Spitfire", "Supermarine", "LEGENDARY", R.drawable.spitfire, "584 km/h", "1 piloto", "11m", "UK", "3t"),
            // Definición de datos para el P-51 Mustang de la Segunda Guerra Mundial
            new AvionData("P-51 Mustang", "North American", "LEGENDARY", R.drawable.p51_mustang, "703 km/h", "1 piloto", "11m", "EE.UU.", "5t"),
            // Definición de datos para el primer caza a reacción operativo Me 262
            new AvionData("Me 262", "Messerschmitt", "LEGENDARY", R.drawable.me_262, "900 km/h", "1 piloto", "10m", "Alemania", "7t"),
            // Definición de datos para el avión de despegue vertical Harrier
            new AvionData("Harrier Jump Jet", "Hawker Siddeley", "LEGENDARY", R.drawable.harrier, "1176 km/h", "1 piloto", "14m", "UK", "11t"),
    };

    // Estructura interna estática para organizar las cadenas de texto y recursos de cada avión
    private static class AvionData {
        // Declaración de variables para el modelo, fabricante, rareza y datos de rendimiento
        String modelo, fabricante, rareza, vel, cap, dim, pais, peso;
        // Declaración de la variable para el identificador numérico de la imagen
        int imgId;
        // Constructor para asignar de forma masiva todos los valores a la estructura interna
        AvionData(String m, String f, String r, int i, String v, String c, String d, String p, String pe) {
            // Asignación de cada parámetro a su variable correspondiente en la clase
            modelo=m; fabricante=f; rareza=r; imgId=i; vel=v; cap=c; dim=d; pais=p; peso=pe;
        }
    }

    // Identificadores de las siluetas genéricas usadas como imagen provisional en los modelos sin foto propia
    private static final int SIL_JET = R.drawable.sil_jet;
    private static final int SIL_TURBOHELICE = R.drawable.sil_turbohelice;
    private static final int SIL_LIGERO = R.drawable.sil_ligero;
    private static final int SIL_HELICOPTERO = R.drawable.sil_helicoptero;
    private static final int SIL_CAZA = R.drawable.sil_caza;
    private static final int SIL_CARGA = R.drawable.sil_carga;
    private static final int SIL_HISTORICO = R.drawable.sil_historico;

    // Lista con los modelos añadidos al catálogo, que de momento usan una silueta genérica como imagen
    private static final java.util.List<AvionData> DATOS_NUEVOS = new java.util.ArrayList<>();

    // Lista con los modelos que no pueden aparecer en el mapa (museos, retirados, sigilosos o que no emiten su posición)
    // No se ofrecen al añadir un avión: quedan guardados para un futuro apartado de avistamientos especiales con foto
    private static final java.util.List<AvionData> DATOS_ESPECIALES = new java.util.ArrayList<>();

    // Tabla que relaciona los códigos ICAO de tipo de aeronave con el modelo del catálogo
    private static final java.util.Map<String, String> TIPOS_ICAO = new java.util.HashMap<>();

    // Bloque que rellena el catálogo ampliado y la tabla de tipos al cargar la clase
    static {
        // Modelos originales que ya no pueden aparecer en el mapa
        // Registro de un avión legendario de reconocimiento SR-71
        DATOS_ESPECIALES.add(new AvionData("SR-71 Blackbird", "Lockheed", "LEGENDARY", R.drawable.sr71, "3529 km/h", "2 trip.", "17m", "EE.UU.", "78t"));
        // Definición de datos para el bombardero invisible B-2 Spirit
        DATOS_ESPECIALES.add(new AvionData("B-2 Spirit", "Northrop", "LEGENDARY", R.drawable.b2_spirit, "1010 km/h", "2 pilotos", "52m", "EE.UU.", "170t"));
        // Definición de datos para la primera aeronave de los Hermanos Wright
        DATOS_ESPECIALES.add(new AvionData("Wright Flyer", "Hermanos Wright", "LEGENDARY", R.drawable.wright_flyer, "48 km/h", "1 piloto", "6m", "EE.UU.", "274kg"));
        // Definición de datos para el avión más grande jamás construido An-225
        DATOS_ESPECIALES.add(new AvionData("Antonov An-225", "Antonov", "LEGENDARY", R.drawable.an225, "850 km/h", "Carga Max.", "84m", "URSS/Ucrania", "640t"));
        // Definición de datos para el avión cohete experimental X-15
        DATOS_ESPECIALES.add(new AvionData("X-15", "North American", "LEGENDARY", R.drawable.x15, "7274 km/h", "1 piloto", "15m", "EE.UU.", "15t"));
        // Definición de datos para el triplano Fokker del Barón Rojo
        DATOS_ESPECIALES.add(new AvionData("Fokker Dr.I (Barón Rojo)", "Fokker", "LEGENDARY", R.drawable.fokker_dr1, "185 km/h", "1 piloto", "5.7m", "Alemania", "586kg"));
        // Definición de datos para el Rutan Voyager que dio la vuelta al mundo
        DATOS_ESPECIALES.add(new AvionData("Rutan Voyager", "Rutan", "LEGENDARY", R.drawable.voyager, "196 km/h", "2 pilotos", "8.9m", "EE.UU.", "4t"));
        // Definición de datos para el Spirit of St. Louis de Lindbergh
        DATOS_ESPECIALES.add(new AvionData("Spirit of St. Louis", "Ryan", "LEGENDARY", R.drawable.spirit_of_st_louis, "214 km/h", "1 piloto", "8m", "EE.UU.", "2t"));
        // Definición de datos para el avión supersónico de pasajeros Concorde
        DATOS_ESPECIALES.add(new AvionData("Concorde", "BAC", "LEGENDARY", R.drawable.concorde, "2179 km/h", "100 pax", "25m", "UK/Francia", "185t"));
        // Definición de datos para el caza de quinta generación ruso Su-57
        DATOS_ESPECIALES.add(new AvionData("Su-57 Felon", "Sukhoi", "LEGENDARY", R.drawable.su57, "2130 km/h", "1 piloto", "19m", "Rusia", "35t"));

        // Códigos de tipo de los modelos que están en el catálogo del mapa
        registrarTipos("Boeing 737-800", "B738");
        registrarTipos("Airbus A320", "A320");
        registrarTipos("Cessna 172 Skyhawk", "C172");
        registrarTipos("Piper PA-28", "P28A", "P28B", "P28R", "P28T", "P28U");
        registrarTipos("ATR 72", "AT72", "AT73", "AT75", "AT76");
        registrarTipos("Bombardier CRJ-900", "CRJ7", "CRJ9", "CRJX");
        registrarTipos("Robinson R44", "R44");
        registrarTipos("Embraer E190", "E190", "E195");
        registrarTipos("Beechcraft Baron", "BE55", "BE58");
        registrarTipos("Diamond DA42", "DA42");
        registrarTipos("Bell 206 JetRanger", "B06", "B06T");
        registrarTipos("Cessna 152", "C152");
        registrarTipos("Boeing 787 Dreamliner", "B788", "B789", "B78X");
        registrarTipos("Airbus A350 XWB", "A359", "A35K");
        registrarTipos("Eurofighter Typhoon", "EUFI");
        registrarTipos("F-16 Fighting Falcon", "F16");
        registrarTipos("C-130 Hercules", "C130", "C30J");
        registrarTipos("Learjet 60", "LJ60");
        registrarTipos("Boeing 777-300ER", "B77W");
        registrarTipos("Airbus A330", "A332", "A333", "A338", "A339");
        registrarTipos("Gulfstream G650", "GLF6");
        registrarTipos("Dassault Falcon 7X", "FA7X");
        registrarTipos("CH-47 Chinook", "H47");
        registrarTipos("Airbus A380", "A388");
        registrarTipos("Boeing 747-400", "B743", "B744");
        registrarTipos("F-35 Lightning II", "F35");
        registrarTipos("AH-64 Apache", "AH64");
        registrarTipos("V-22 Osprey", "V22");
        registrarTipos("F/A-18 Super Hornet", "F18S");
        registrarTipos("Antonov An-124", "A124");
        registrarTipos("Airbus Beluga XL", "A337");
        registrarTipos("F-22 Raptor", "F22");
        registrarTipos("Spitfire", "SPIT");
        registrarTipos("P-51 Mustang", "P51");
        registrarTipos("Harrier Jump Jet", "HAR");

        // Datos aproximados de los modelos nuevos: nombre, fabricante, rareza, imagen, velocidad, capacidad, envergadura, país, peso máximo y códigos de tipo

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
        anadir("Airbus A340-300", "Airbus", "EPIC", SIL_JET, "880 km/h", "295 pax", "60m", "Europa", "276t", "A342", "A343");
        anadir("Airbus A340-500", "Airbus", "EPIC", SIL_JET, "900 km/h", "313 pax", "63m", "Europa", "380t", "A345");
        anadir("Airbus A340-600", "Airbus", "EPIC", SIL_JET, "890 km/h", "380 pax", "64m", "Europa", "380t", "A346");

        // Reactores comerciales de Boeing
        anadir("Boeing 717-200", "Boeing", "RARE", SIL_JET, "811 km/h", "106 pax", "28m", "EE.UU.", "55t", "B712");
        anadir("Boeing 727-200", "Boeing", "EPIC", SIL_JET, "960 km/h", "189 pax", "33m", "EE.UU.", "95t", "B721", "B722");
        anadir("Boeing 737-200", "Boeing", "EPIC", SIL_JET, "780 km/h", "130 pax", "28m", "EE.UU.", "58t", "B732");
        anadir("Boeing 737-300", "Boeing", "RARE", SIL_JET, "800 km/h", "149 pax", "29m", "EE.UU.", "63t", "B733");
        anadir("Boeing 737-400", "Boeing", "RARE", SIL_JET, "800 km/h", "168 pax", "29m", "EE.UU.", "68t", "B734");
        anadir("Boeing 737-500", "Boeing", "EPIC", SIL_JET, "800 km/h", "132 pax", "29m", "EE.UU.", "60t", "B735");
        anadir("Boeing 737-700", "Boeing", "COMMON", SIL_JET, "828 km/h", "140 pax", "36m", "EE.UU.", "78t", "B737");
        anadir("Boeing 737-900", "Boeing", "RARE", SIL_JET, "828 km/h", "180 pax", "36m", "EE.UU.", "85t", "B739");
        anadir("Boeing 737 MAX 8", "Boeing", "COMMON", SIL_JET, "839 km/h", "178 pax", "36m", "EE.UU.", "82t", "B38M");
        anadir("Boeing 737 MAX 9", "Boeing", "RARE", SIL_JET, "839 km/h", "204 pax", "36m", "EE.UU.", "88t", "B39M");
        anadir("Boeing 747-8", "Boeing", "EPIC", SIL_JET, "920 km/h", "467 pax", "68m", "EE.UU.", "448t", "B748");
        anadir("Boeing 747-200", "Boeing", "LEGENDARY", SIL_JET, "894 km/h", "366 pax", "60m", "EE.UU.", "377t", "B741", "B742");
        anadir("Boeing 757-200", "Boeing", "RARE", SIL_JET, "850 km/h", "200 pax", "38m", "EE.UU.", "115t", "B752");
        anadir("Boeing 757-300", "Boeing", "EPIC", SIL_JET, "850 km/h", "243 pax", "38m", "EE.UU.", "123t", "B753");
        anadir("Boeing 767-200", "Boeing", "EPIC", SIL_JET, "851 km/h", "216 pax", "48m", "EE.UU.", "143t", "B762");
        anadir("Boeing 767-300", "Boeing", "RARE", SIL_JET, "851 km/h", "261 pax", "48m", "EE.UU.", "187t", "B763");
        anadir("Boeing 767-400ER", "Boeing", "EPIC", SIL_JET, "851 km/h", "245 pax", "52m", "EE.UU.", "204t", "B764");
        anadir("Boeing 777-200", "Boeing", "RARE", SIL_JET, "892 km/h", "305 pax", "61m", "EE.UU.", "298t", "B772");
        anadir("Boeing 777-300", "Boeing", "RARE", SIL_JET, "892 km/h", "368 pax", "61m", "EE.UU.", "299t", "B773");
        anadir("Boeing 777 Freighter", "Boeing", "RARE", SIL_JET, "896 km/h", "Carga", "65m", "EE.UU.", "347t", "B77L");
        anadir("Boeing 777-9", "Boeing", "EPIC", SIL_JET, "905 km/h", "426 pax", "72m", "EE.UU.", "351t", "B779");

        // Reactores de McDonnell Douglas y Lockheed
        anadir("McDonnell Douglas MD-80", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "155 pax", "33m", "EE.UU.", "68t", "MD81", "MD82", "MD83", "MD87", "MD88");
        anadir("McDonnell Douglas MD-90", "McDonnell Douglas", "EPIC", SIL_JET, "811 km/h", "160 pax", "33m", "EE.UU.", "76t", "MD90");
        anadir("McDonnell Douglas MD-11", "McDonnell Douglas", "EPIC", SIL_JET, "876 km/h", "293 pax", "52m", "EE.UU.", "286t", "MD11");
        anadir("McDonnell Douglas DC-9", "McDonnell Douglas", "LEGENDARY", SIL_JET, "804 km/h", "115 pax", "28m", "EE.UU.", "49t", "DC91", "DC92", "DC93", "DC94", "DC95");
        anadir("McDonnell Douglas DC-10", "McDonnell Douglas", "EPIC", SIL_JET, "875 km/h", "270 pax", "50m", "EE.UU.", "260t", "DC10");
        anadir("Lockheed L-1011 TriStar", "Lockheed", "LEGENDARY", SIL_JET, "960 km/h", "256 pax", "47m", "EE.UU.", "195t", "L101");

        // Reactores regionales
        anadir("Embraer E170", "Embraer", "RARE", SIL_JET, "828 km/h", "72 pax", "26m", "Brasil", "37t", "E170");
        anadir("Embraer E175", "Embraer", "COMMON", SIL_JET, "828 km/h", "78 pax", "26m", "Brasil", "40t", "E75L", "E75S");
        anadir("Embraer E190-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "106 pax", "33m", "Brasil", "57t", "E290");
        anadir("Embraer E195-E2", "Embraer", "RARE", SIL_JET, "833 km/h", "132 pax", "35m", "Brasil", "61t", "E295");
        anadir("Embraer ERJ-145", "Embraer", "COMMON", SIL_JET, "828 km/h", "50 pax", "20m", "Brasil", "22t", "E135", "E140", "E145");
        anadir("Bombardier CRJ-200", "Bombardier", "COMMON", SIL_JET, "830 km/h", "50 pax", "21m", "Canadá", "24t", "CRJ1", "CRJ2");
        anadir("Fokker 100", "Fokker", "EPIC", SIL_JET, "845 km/h", "109 pax", "28m", "Países Bajos", "46t", "F100");
        anadir("Fokker 70", "Fokker", "EPIC", SIL_JET, "845 km/h", "79 pax", "28m", "Países Bajos", "40t", "F70");
        anadir("BAe 146", "British Aerospace", "EPIC", SIL_JET, "789 km/h", "100 pax", "27m", "UK", "44t", "B461", "B462", "B463", "RJ70", "RJ85", "RJ1H");
        anadir("Sukhoi Superjet 100", "Sukhoi", "EPIC", SIL_JET, "828 km/h", "98 pax", "28m", "Rusia", "49t", "SU95");
        anadir("Comac C919", "Comac", "EPIC", SIL_JET, "828 km/h", "158 pax", "36m", "China", "78t", "C919");
        anadir("Comac ARJ21", "Comac", "EPIC", SIL_JET, "828 km/h", "90 pax", "28m", "China", "41t", "AJ27");

        // Reactores soviéticos y rusos
        anadir("Tupolev Tu-154", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "180 pax", "38m", "URSS", "100t", "T154");
        anadir("Tupolev Tu-134", "Tupolev", "LEGENDARY", SIL_JET, "850 km/h", "76 pax", "29m", "URSS", "47t", "T134");
        anadir("Tupolev Tu-204", "Tupolev", "EPIC", SIL_JET, "810 km/h", "210 pax", "42m", "Rusia", "110t", "T204");
        anadir("Ilyushin Il-62", "Ilyushin", "LEGENDARY", SIL_JET, "900 km/h", "186 pax", "43m", "URSS", "165t", "IL62");
        anadir("Ilyushin Il-96", "Ilyushin", "EPIC", SIL_JET, "850 km/h", "300 pax", "60m", "Rusia", "250t", "IL96");
        anadir("Antonov An-148", "Antonov", "EPIC", SIL_JET, "835 km/h", "75 pax", "29m", "Ucrania", "43t", "A148");
        anadir("Yakovlev Yak-40", "Yakovlev", "LEGENDARY", SIL_JET, "550 km/h", "32 pax", "25m", "URSS", "16t", "YK40");
        anadir("Yakovlev Yak-42", "Yakovlev", "EPIC", SIL_JET, "810 km/h", "120 pax", "35m", "URSS", "57t", "YK42");

        // Reactores privados
        anadir("Bombardier Challenger 350", "Bombardier", "RARE", SIL_JET, "870 km/h", "10 pax", "21m", "Canadá", "18t", "CL35");
        anadir("Bombardier Challenger 604", "Bombardier", "RARE", SIL_JET, "870 km/h", "12 pax", "20m", "Canadá", "22t", "CL60");
        anadir("Bombardier Global Express", "Bombardier", "RARE", SIL_JET, "902 km/h", "17 pax", "29m", "Canadá", "45t", "GLEX", "GL5T", "GL6T", "GL7T");
        anadir("Gulfstream G280", "Gulfstream", "RARE", SIL_JET, "850 km/h", "10 pax", "20m", "EE.UU.", "18t", "G280");
        anadir("Gulfstream G450", "Gulfstream", "RARE", SIL_JET, "904 km/h", "16 pax", "28m", "EE.UU.", "34t", "GLF4");
        anadir("Gulfstream G550", "Gulfstream", "RARE", SIL_JET, "904 km/h", "19 pax", "29m", "EE.UU.", "41t", "GLF5");
        anadir("Gulfstream G700", "Gulfstream", "EPIC", SIL_JET, "904 km/h", "19 pax", "31m", "EE.UU.", "48t", "GLF7");
        anadir("Dassault Falcon 900", "Dassault", "RARE", SIL_JET, "870 km/h", "14 pax", "20m", "Francia", "21t", "F900");
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
        anadir("Learjet 45", "Learjet", "RARE", SIL_JET, "860 km/h", "9 pax", "15m", "EE.UU.", "9t", "LJ45");
        anadir("Learjet 35", "Learjet", "EPIC", SIL_JET, "830 km/h", "8 pax", "12m", "EE.UU.", "8t", "LJ35");
        anadir("Hawker 800", "Hawker", "RARE", SIL_JET, "830 km/h", "8 pax", "16m", "UK/EE.UU.", "12t", "H25B");
        anadir("Eclipse 500", "Eclipse", "RARE", SIL_JET, "685 km/h", "5 pax", "11m", "EE.UU.", "3t", "EA50");
        anadir("Cirrus Vision Jet", "Cirrus", "RARE", SIL_JET, "556 km/h", "5 pax", "12m", "EE.UU.", "3t", "SF50");

        // Turbohélices regionales y utilitarios
        anadir("ATR 42", "ATR", "RARE", SIL_TURBOHELICE, "556 km/h", "48 pax", "25m", "Francia/Italia", "19t", "AT43", "AT44", "AT45", "AT46");
        anadir("Bombardier Dash 8 Q400", "Bombardier", "RARE", SIL_TURBOHELICE, "667 km/h", "78 pax", "28m", "Canadá", "29t", "DH8D");
        anadir("De Havilland Dash 8-300", "De Havilland", "RARE", SIL_TURBOHELICE, "528 km/h", "50 pax", "27m", "Canadá", "19t", "DH8A", "DH8B", "DH8C");
        anadir("Saab 340", "Saab", "RARE", SIL_TURBOHELICE, "467 km/h", "34 pax", "21m", "Suecia", "13t", "SF34");
        anadir("Saab 2000", "Saab", "EPIC", SIL_TURBOHELICE, "665 km/h", "50 pax", "25m", "Suecia", "23t", "SB20");
        anadir("Fokker 50", "Fokker", "EPIC", SIL_TURBOHELICE, "500 km/h", "56 pax", "29m", "Países Bajos", "21t", "F50");
        anadir("Embraer EMB-120 Brasilia", "Embraer", "RARE", SIL_TURBOHELICE, "552 km/h", "30 pax", "20m", "Brasil", "12t", "E120");
        anadir("De Havilland Twin Otter", "De Havilland", "RARE", SIL_TURBOHELICE, "337 km/h", "19 pax", "20m", "Canadá", "6t", "DHC6");
        anadir("Short 360", "Short", "RARE", SIL_TURBOHELICE, "400 km/h", "36 pax", "23m", "UK", "12t", "SH36");
        anadir("Dornier 228", "Dornier", "RARE", SIL_TURBOHELICE, "428 km/h", "19 pax", "17m", "Alemania", "6t", "D228");
        anadir("Dornier 328", "Dornier", "EPIC", SIL_TURBOHELICE, "620 km/h", "32 pax", "21m", "Alemania", "14t", "D328");
        anadir("BAe Jetstream 31", "British Aerospace", "RARE", SIL_TURBOHELICE, "430 km/h", "19 pax", "16m", "UK", "7t", "JS31", "JS32");
        anadir("BAe Jetstream 41", "British Aerospace", "RARE", SIL_TURBOHELICE, "482 km/h", "29 pax", "18m", "UK", "10t", "JS41");
        anadir("Beechcraft 1900D", "Beechcraft", "RARE", SIL_TURBOHELICE, "518 km/h", "19 pax", "18m", "EE.UU.", "8t", "B190");
        anadir("Beechcraft King Air 350", "Beechcraft", "RARE", SIL_TURBOHELICE, "578 km/h", "11 pax", "18m", "EE.UU.", "7t", "B350");
        anadir("Beechcraft King Air 200", "Beechcraft", "RARE", SIL_TURBOHELICE, "536 km/h", "9 pax", "17m", "EE.UU.", "6t", "BE20");
        anadir("Pilatus PC-12", "Pilatus", "RARE", SIL_TURBOHELICE, "528 km/h", "9 pax", "16m", "Suiza", "5t", "PC12");
        anadir("Cessna 208 Caravan", "Cessna", "COMMON", SIL_TURBOHELICE, "344 km/h", "9 pax", "16m", "EE.UU.", "4t", "C208");
        anadir("Daher TBM 900", "Daher", "RARE", SIL_TURBOHELICE, "600 km/h", "6 pax", "12m", "Francia", "3.4t", "TBM7", "TBM8", "TBM9");
        anadir("Convair 580", "Convair", "EPIC", SIL_TURBOHELICE, "480 km/h", "50 pax", "32m", "EE.UU.", "25t", "CVLT");
        anadir("CASA C-295", "CASA", "RARE", SIL_TURBOHELICE, "480 km/h", "71 pax", "26m", "España", "23t", "C295");
        anadir("CASA CN-235", "CASA", "RARE", SIL_TURBOHELICE, "460 km/h", "44 pax", "26m", "España/Indonesia", "16t", "CN35");
        anadir("Pilatus PC-21", "Pilatus", "RARE", SIL_TURBOHELICE, "685 km/h", "2 pilotos", "9m", "Suiza", "3t", "PC21");

        // Aviación ligera
        anadir("Cessna 150", "Cessna", "COMMON", SIL_LIGERO, "200 km/h", "2 pax", "10m", "EE.UU.", "0.7t", "C150");
        anadir("Cessna 182 Skylane", "Cessna", "COMMON", SIL_LIGERO, "280 km/h", "4 pax", "11m", "EE.UU.", "1.4t", "C182");
        anadir("Cessna 206 Stationair", "Cessna", "RARE", SIL_LIGERO, "280 km/h", "6 pax", "11m", "EE.UU.", "1.6t", "C206");
        anadir("Cessna 210 Centurion", "Cessna", "RARE", SIL_LIGERO, "320 km/h", "6 pax", "12m", "EE.UU.", "1.7t", "C210");
        anadir("Cirrus SR20", "Cirrus", "COMMON", SIL_LIGERO, "290 km/h", "4 pax", "11m", "EE.UU.", "1.4t", "SR20");
        anadir("Cirrus SR22", "Cirrus", "COMMON", SIL_LIGERO, "340 km/h", "4 pax", "12m", "EE.UU.", "1.6t", "SR22");
        anadir("Diamond DA20", "Diamond", "COMMON", SIL_LIGERO, "250 km/h", "2 pax", "11m", "Canadá", "0.8t", "DA20");
        anadir("Diamond DA40", "Diamond", "COMMON", SIL_LIGERO, "280 km/h", "4 pax", "12m", "Austria", "1.2t", "DA40");
        anadir("Piper PA-34 Seneca", "Piper", "COMMON", SIL_LIGERO, "315 km/h", "6 pax", "12m", "EE.UU.", "2t", "PA34");
        anadir("Piper PA-44 Seminole", "Piper", "COMMON", SIL_LIGERO, "320 km/h", "4 pax", "12m", "EE.UU.", "1.7t", "PA44");
        anadir("Piper PA-32 Cherokee Six", "Piper", "RARE", SIL_LIGERO, "270 km/h", "6 pax", "11m", "EE.UU.", "1.7t", "PA32");
        anadir("Piper PA-18 Super Cub", "Piper", "RARE", SIL_LIGERO, "185 km/h", "2 pax", "11m", "EE.UU.", "0.8t", "PA18");
        anadir("Piper PA-46 Malibu", "Piper", "RARE", SIL_LIGERO, "440 km/h", "6 pax", "13m", "EE.UU.", "1.4t", "PA46");
        anadir("Beechcraft Bonanza", "Beechcraft", "RARE", SIL_LIGERO, "330 km/h", "6 pax", "10m", "EE.UU.", "1.5t", "BE33", "BE35", "BE36");
        anadir("Mooney M20", "Mooney", "RARE", SIL_LIGERO, "280 km/h", "4 pax", "11m", "EE.UU.", "1.2t", "M20P", "M20J", "M20T");
        anadir("Robin DR400", "Robin", "COMMON", SIL_LIGERO, "250 km/h", "4 pax", "9m", "Francia", "1.1t", "DR40");
        anadir("Tecnam P2006T", "Tecnam", "RARE", SIL_LIGERO, "270 km/h", "4 pax", "12m", "Italia", "1.2t", "P06T");
        anadir("Extra 300", "Extra", "RARE", SIL_LIGERO, "340 km/h", "2 pax", "8m", "Alemania", "0.95t", "E300");
        anadir("Pitts Special", "Pitts", "RARE", SIL_LIGERO, "330 km/h", "2 pax", "6m", "EE.UU.", "0.7t", "PTS2");
        anadir("Britten-Norman Islander", "Britten-Norman", "RARE", SIL_LIGERO, "257 km/h", "9 pax", "15m", "UK", "3t", "BN2P");

        // Helicópteros
        anadir("Robinson R22", "Robinson", "COMMON", SIL_HELICOPTERO, "180 km/h", "2 pax", "8m", "EE.UU.", "0.6t", "R22");
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
        anadir("Airbus Beluga ST", "Airbus", "EPIC", SIL_CARGA, "750 km/h", "Carga Esp.", "44m", "Europa", "155t", "A3ST");
        anadir("Ilyushin Il-76", "Ilyushin", "EPIC", SIL_CARGA, "850 km/h", "Carga", "50m", "URSS", "195t", "IL76");
        anadir("Antonov An-12", "Antonov", "LEGENDARY", SIL_CARGA, "670 km/h", "Carga", "38m", "URSS", "61t", "AN12");
        anadir("Lockheed C-5 Galaxy", "Lockheed", "EPIC", SIL_CARGA, "833 km/h", "Carga", "68m", "EE.UU.", "381t", "C5M", "C5");
        anadir("Boeing C-17 Globemaster III", "Boeing", "EPIC", SIL_CARGA, "830 km/h", "Carga", "52m", "EE.UU.", "265t", "C17");
        anadir("Embraer KC-390", "Embraer", "EPIC", SIL_CARGA, "870 km/h", "Carga", "35m", "Brasil", "86t");
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
        anadir("Lockheed U-2 Dragon Lady", "Lockheed", "EPIC", SIL_CAZA, "800 km/h", "1 piloto", "32m", "EE.UU.", "18t", "U2");
        anadir("BAE Hawk", "BAE Systems", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "10m", "UK", "9t", "HAWK");
        anadir("Dassault/Dornier Alpha Jet", "Dassault/Dornier", "RARE", SIL_CAZA, "1000 km/h", "2 pilotos", "9m", "Francia/Alemania", "8t", "ALPH");
        anadir("Aermacchi M-346", "Aermacchi", "RARE", SIL_CAZA, "1090 km/h", "2 pilotos", "10m", "Italia", "10t", "M346");
        anadir("Northrop T-38 Talon", "Northrop", "RARE", SIL_CAZA, "1380 km/h", "2 pilotos", "8m", "EE.UU.", "5t", "T38");
        anadir("Mikoyan MiG-21", "Mikoyan", "LEGENDARY", SIL_CAZA, "2175 km/h", "1 piloto", "7m", "URSS", "9t", "MG21");
        anadir("Mikoyan MiG-15", "Mikoyan", "LEGENDARY", SIL_CAZA, "1075 km/h", "1 piloto", "10m", "URSS", "5t", "MG15");
        anadir("F-86 Sabre", "North American", "LEGENDARY", SIL_CAZA, "1100 km/h", "1 piloto", "12m", "EE.UU.", "7t", "F86");

        // Históricos todavía en vuelo
        anadir("Douglas DC-3", "Douglas", "LEGENDARY", SIL_HISTORICO, "333 km/h", "32 pax", "29m", "EE.UU.", "11t", "DC3");
        anadir("Douglas DC-4", "Douglas", "LEGENDARY", SIL_HISTORICO, "365 km/h", "44 pax", "36m", "EE.UU.", "33t", "DC4");
        anadir("Douglas DC-6", "Douglas", "LEGENDARY", SIL_HISTORICO, "507 km/h", "66 pax", "36m", "EE.UU.", "48t", "DC6");
        anadir("Lockheed Constellation", "Lockheed", "LEGENDARY", SIL_HISTORICO, "555 km/h", "62 pax", "38m", "EE.UU.", "62t", "CONI");
        anadir("Vickers Viscount", "Vickers", "LEGENDARY", SIL_HISTORICO, "563 km/h", "65 pax", "29m", "UK", "33t", "VISC");
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

        // Modelos nuevos que no pueden aparecer en el mapa: no emiten su posición (militares rusos e iraníes)
        anadirEspecial("Tupolev Tu-95 Bear", "Tupolev", "EPIC", SIL_CARGA, "830 km/h", "7 tripulantes", "51m", "URSS", "188t");
        anadirEspecial("Tupolev Tu-160", "Tupolev", "EPIC", SIL_CAZA, "2220 km/h", "4 tripulantes", "56m", "URSS", "275t");
        anadirEspecial("F-14 Tomcat", "Grumman", "LEGENDARY", SIL_CAZA, "2485 km/h", "2 pilotos", "19m", "EE.UU.", "33t");
        anadirEspecial("Sukhoi Su-27", "Sukhoi", "EPIC", SIL_CAZA, "2500 km/h", "1 piloto", "15m", "URSS", "33t");
        anadirEspecial("Sukhoi Su-35", "Sukhoi", "EPIC", SIL_CAZA, "2500 km/h", "1 piloto", "15m", "Rusia", "34t");

        // Modelos míticos: prototipos, retirados o solo visibles en museos
        anadirEspecial("Hughes H-4 Hercules (Spruce Goose)", "Hughes", "MYTHIC", SIL_HISTORICO, "351 km/h", "750 tropas", "97m", "EE.UU.", "180t");
        anadirEspecial("North American XB-70 Valkyrie", "North American", "MYTHIC", SIL_CAZA, "3309 km/h", "2 pilotos", "32m", "EE.UU.", "250t");
        anadirEspecial("Bell X-1", "Bell", "MYTHIC", SIL_CAZA, "1541 km/h", "1 piloto", "9m", "EE.UU.", "6t");
        anadirEspecial("Messerschmitt Me 163 Komet", "Messerschmitt", "MYTHIC", SIL_CAZA, "960 km/h", "1 piloto", "9m", "Alemania", "4.3t");
        anadirEspecial("Tupolev Tu-144", "Tupolev", "MYTHIC", SIL_JET, "2300 km/h", "140 pax", "29m", "URSS", "180t");
        anadirEspecial("De Havilland Comet 4", "De Havilland", "MYTHIC", SIL_JET, "809 km/h", "81 pax", "35m", "UK", "74t");
        anadirEspecial("Vickers VC10", "Vickers", "MYTHIC", SIL_JET, "930 km/h", "150 pax", "44m", "UK", "152t");
        anadirEspecial("Boeing 314 Clipper", "Boeing", "MYTHIC", SIL_HISTORICO, "340 km/h", "74 pax", "46m", "EE.UU.", "38t");
        anadirEspecial("Convair B-36 Peacemaker", "Convair", "MYTHIC", SIL_CARGA, "672 km/h", "15 tripulantes", "70m", "EE.UU.", "185t");
        anadirEspecial("Northrop YB-49", "Northrop", "MYTHIC", SIL_CAZA, "793 km/h", "7 tripulantes", "52m", "EE.UU.", "88t");
        anadirEspecial("Lockheed YF-12", "Lockheed", "MYTHIC", SIL_CAZA, "3330 km/h", "2 pilotos", "17m", "EE.UU.", "54t");
        anadirEspecial("Lockheed F-117 Nighthawk", "Lockheed", "MYTHIC", SIL_CAZA, "993 km/h", "1 piloto", "13m", "EE.UU.", "24t");
        anadirEspecial("Avro Vulcan", "Avro", "MYTHIC", SIL_CAZA, "1038 km/h", "5 tripulantes", "30m", "UK", "77t");
        anadirEspecial("Dornier Do X", "Dornier", "MYTHIC", SIL_HISTORICO, "175 km/h", "66 pax", "48m", "Alemania", "52t");
    }

    // Método auxiliar que asocia varios códigos de tipo ICAO a un mismo modelo del catálogo
    private static void registrarTipos(String modelo, String... codigos) {
        // Recorrido de cada código para guardarlo en la tabla junto a su modelo
        for (String c : codigos) TIPOS_ICAO.put(c, modelo);
    }

    // Método auxiliar que añade un modelo nuevo al catálogo del mapa y registra sus códigos de tipo
    private static void anadir(String modelo, String fabricante, String rareza, int imgId,
                               String vel, String cap, String dim, String pais, String peso,
                               String... codigos) {
        // Adición de los datos del modelo a la lista de modelos nuevos
        DATOS_NUEVOS.add(new AvionData(modelo, fabricante, rareza, imgId, vel, cap, dim, pais, peso));
        // Registro de los códigos de tipo que corresponden a este modelo
        registrarTipos(modelo, codigos);
    }

    // Método auxiliar que añade un modelo especial, que no puede aparecer en el mapa
    private static void anadirEspecial(String modelo, String fabricante, String rareza, int imgId,
                                       String vel, String cap, String dim, String pais, String peso) {
        // Adición de los datos del modelo a la lista de modelos especiales
        DATOS_ESPECIALES.add(new AvionData(modelo, fabricante, rareza, imgId, vel, cap, dim, pais, peso));
    }

    // Método auxiliar que convierte la estructura AvionData en un objeto Avion real
    private static Avion crearAvion(AvionData d) {
        // Construcción del avión con todos los datos de la estructura
        return new Avion(d.modelo, d.fabricante, d.rareza, d.imgId, d.vel, d.cap, d.dim, d.pais, d.peso);
    }

    // Método público que devuelve los modelos especiales, que no pueden aparecer en el mapa
    public static java.util.List<Avion> getAvionesEspeciales() {
        // Creación de la lista de resultado
        java.util.List<Avion> lista = new java.util.ArrayList<>();
        // Conversión de cada modelo especial a un objeto Avion
        for (AvionData d : DATOS_ESPECIALES) lista.add(crearAvion(d));
        // Devolución de la lista completa
        return lista;
    }

    // Método público que busca el avión del catálogo que corresponde a un código de tipo ICAO
    public static Avion getAvionPorTipoIcao(String tipoIcao) {
        // Retorno de null si la API no ha dado ningún tipo
        if (tipoIcao == null) return null;
        // Búsqueda del modelo asociado al código en la tabla
        String modelo = TIPOS_ICAO.get(tipoIcao.toUpperCase());
        // Retorno de null si el tipo no está en el catálogo
        if (modelo == null) return null;
        // Búsqueda del modelo entre los datos originales
        for (AvionData d : DATOS_AVIONES) {
            if (d.modelo.equals(modelo)) return crearAvion(d);
        }
        // Búsqueda del modelo entre los datos nuevos
        for (AvionData d : DATOS_NUEVOS) {
            if (d.modelo.equals(modelo)) return crearAvion(d);
        }
        // Retorno de null si no se encuentra el modelo
        return null;
    }

    // Método público que transforma los datos estáticos en una lista de objetos Avion usables por la app
    public static java.util.List<Avion> getTodosLosAviones() {
        // Inicialización de una lista dinámica de tipo Avion
        java.util.List<Avion> lista = new java.util.ArrayList<>();
        // Recorrido de cada elemento almacenado en el arreglo constante de datos
        for (AvionData d : DATOS_AVIONES) {
            // Conversión de la estructura AvionData a un objeto Avion real y adición a la lista
            lista.add(crearAvion(d));
        }
        // Recorrido de los modelos nuevos que usan siluetas genéricas
        for (AvionData d : DATOS_NUEVOS) {
            // Conversión de la estructura AvionData a un objeto Avion real y adición a la lista
            lista.add(crearAvion(d));
        }
        // Retorno de la colección completa de aviones generados
        return lista;
    }
}