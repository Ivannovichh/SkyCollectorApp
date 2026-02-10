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

            // Registro de un avión legendario de reconocimiento SR-71
            new AvionData("SR-71 Blackbird", "Lockheed", "LEGENDARY", R.drawable.sr71, "3529 km/h", "2 trip.", "17m", "EE.UU.", "78t"),
            // Definición de datos para el caza de superioridad aérea F-22 Raptor
            new AvionData("F-22 Raptor", "Lockheed Martin", "LEGENDARY", R.drawable.f22, "2414 km/h", "1 piloto", "13m", "EE.UU.", "38t"),
            // Definición de datos para el bombardero invisible B-2 Spirit
            new AvionData("B-2 Spirit", "Northrop", "LEGENDARY", R.drawable.b2_spirit, "1010 km/h", "2 pilotos", "52m", "EE.UU.", "170t"),
            // Definición de datos para el caza histórico Spitfire
            new AvionData("Spitfire", "Supermarine", "LEGENDARY", R.drawable.spitfire, "584 km/h", "1 piloto", "11m", "UK", "3t"),
            // Definición de datos para el P-51 Mustang de la Segunda Guerra Mundial
            new AvionData("P-51 Mustang", "North American", "LEGENDARY", R.drawable.p51_mustang, "703 km/h", "1 piloto", "11m", "EE.UU.", "5t"),
            // Definición de datos para la primera aeronave de los Hermanos Wright
            new AvionData("Wright Flyer", "Hermanos Wright", "LEGENDARY", R.drawable.wright_flyer, "48 km/h", "1 piloto", "6m", "EE.UU.", "274kg"),
            // Definición de datos para el avión más grande jamás construido An-225
            new AvionData("Antonov An-225", "Antonov", "LEGENDARY", R.drawable.an225, "850 km/h", "Carga Max.", "84m", "URSS/Ucrania", "640t"),
            // Definición de datos para el avión cohete experimental X-15
            new AvionData("X-15", "North American", "LEGENDARY", R.drawable.x15, "7274 km/h", "1 piloto", "15m", "EE.UU.", "15t"),
            // Definición de datos para el triplano Fokker del Barón Rojo
            new AvionData("Fokker Dr.I (Barón Rojo)", "Fokker", "LEGENDARY", R.drawable.fokker_dr1, "185 km/h", "1 piloto", "5.7m", "Alemania", "586kg"),
            // Definición de datos para el primer caza a reacción operativo Me 262
            new AvionData("Me 262", "Messerschmitt", "LEGENDARY", R.drawable.me_262, "900 km/h", "1 piloto", "10m", "Alemania", "7t"),
            // Definición de datos para el Rutan Voyager que dio la vuelta al mundo
            new AvionData("Rutan Voyager", "Rutan", "LEGENDARY", R.drawable.voyager, "196 km/h", "2 pilotos", "8.9m", "EE.UU.", "4t"),
            // Definición de datos para el Spirit of St. Louis de Lindbergh
            new AvionData("Spirit of St. Louis", "Ryan", "LEGENDARY", R.drawable.spirit_of_st_louis, "214 km/h", "1 piloto", "8m", "EE.UU.", "2t"),
            // Definición de datos para el avión de despegue vertical Harrier
            new AvionData("Harrier Jump Jet", "Hawker Siddeley", "LEGENDARY", R.drawable.harrier, "1176 km/h", "1 piloto", "14m", "UK", "11t"),
            // Definición de datos para el avión supersónico de pasajeros Concorde
            new AvionData("Concorde", "BAC", "LEGENDARY", R.drawable.concorde, "2179 km/h", "100 pax", "25m", "UK/Francia", "185t"),
            // Definición de datos para el caza de quinta generación ruso Su-57
            new AvionData("Su-57 Felon", "Sukhoi", "LEGENDARY", R.drawable.su57, "2130 km/h", "1 piloto", "19m", "Rusia", "35t"),
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

    // Método público que transforma los datos estáticos en una lista de objetos Avion usables por la app
    public static java.util.List<Avion> getTodosLosAviones() {
        // Inicialización de una lista dinámica de tipo Avion
        java.util.List<Avion> lista = new java.util.ArrayList<>();
        // Recorrido de cada elemento almacenado en el arreglo constante de datos
        for (AvionData d : DATOS_AVIONES) {
            // Conversión de la estructura AvionData a un objeto Avion real y adición a la lista
            lista.add(new Avion(d.modelo, d.fabricante, d.rareza, d.imgId, d.vel, d.cap, d.dim, d.pais, d.peso));
        }
        // Retorno de la colección completa de aviones generados
        return lista;
    }
}