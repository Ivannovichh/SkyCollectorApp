# ✈️ SkyCollectorApp

![Prototype](https://img.shields.io/badge/Prototype-Java-brown?style=for-the-badge&logo=java)
![Prototype](https://img.shields.io/badge/Prototype-Android-green?style=for-the-badge&logo=android)
![Status](https://img.shields.io/badge/Status-Mejorando-yellowgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge)

**SkyCollectorApp** es una aplicación nativa para Android desarrollada en **Java**.
Permite gestionar una colección personal de aviones (*spotting*), visualizar vuelos reales en un mapa en tiempo real, capturar fotografías verificadas y consultar fichas técnicas mediante un motor híbrido de múltiples APIs aeronáuticas.

Proyecto orientado al desarrollo Android completo: interfaz nativa, consumo multi-API asíncrono, caché inteligente, arquitectura modular, sincronización en la nube y visualización de datos en tiempo real.

---

## 📱 Descripción

SkyCollectorApp combina:

* Colección personal de aviones (Hangar sincronizado con la nube)
* Radar de tráfico aéreo en tiempo real sobre mapa interactivo
* Motor híbrido de identificación aeronáutica y resolución de metadatos
* Registro de avistamientos con fotografía en directo verificada por rango GPS
* Detección de *Easter Eggs* en pista (Vehículos de tierra y Torres de Control)
* Ranking global de coleccionistas y perfil de usuario
* Chatbot interno de asistencia

La aplicación integra datos reales de tráfico aéreo con una arquitectura Android modular organizada por paquetes.

---

## 🌍 APIs utilizadas

Para garantizar una cobertura total y evitar aeronaves sin identificar (`Unknown Aircraft`), la aplicación utiliza un **pipeline multi-API** gestionado por `AvionGenerator`:

1. **Radar en tiempo real (ADSBExchange v2 / `adsb.lol` / `airplanes.live`):** Proporciona posición geográfica, altitud, velocidad, rumbo, matrícula y código ICAO de 24 bits en vivo.
2. **HexDB & ADSBDB API:** Consultas de respaldo por código hexadecimal ICAO (Modo S) y por matrícula oficial (*N-Number*), además de rutas comerciales y aerolíneas.
3. **OpenSky Network API:** Respaldo adicional para identificación de aeronaves recientes y telemetría de seguimiento.
4. **MediaWiki / Wikipedia API:** Obtención automática de descripciones históricas y fotografías reales de cada modelo detectado.

---

## 🧠 Tecnologías usadas

* **Java** (Desarrollo nativo Android)
* **Android Studio & Android SDK**
* **XML Layouts & Material Design**
* **Retrofit 2 & Gson** (Consumo de APIs REST y serialización JSON)
* **Firebase Authentication & Cloud Firestore** (Login, persistencia en la nube y ranking)
* **Glide** (Carga de imágenes, caché y visor con zoom táctil)
* **RecyclerView & Spinners dinámicos**
* **Gradle**
* **Git / GitHub**

---

## 🏗️ Estructura del proyecto

```text
app/
├── manifests/
│   └── AndroidManifest.xml
│
├── java/es.medac.skycollectorapp/
│   ├── activities/
│   │   ├── AddAvionActivity.java
│   │   ├── ChatbotActivity.java
│   │   ├── DetalleAvionActivity.java
│   │   ├── DetalleVueloRadarActivity.java
│   │   ├── LoginActivity.java
│   │   ├── MainActivity.java
│   │   ├── MapaActivity.java
│   │   └── PerfilActivity.java
│   │
│   ├── adapters/
│   │   ├── AvionAdapter.java
│   │   └── ChatAdapter.java
│   │
│   ├── models/
│   │   ├── AdsbdbResponse.java
│   │   ├── AdsbResponse.java
│   │   ├── Avion.java
│   │   └── Mensaje.java
│   │
│   ├── network/
│   │   ├── AdsbdbService.java
│   │   └── AdsbService.java
│   │
│   └── utils/
│       ├── AvionGenerator.java
│       ├── ImagenBase64Utils.java
│       └── RankingManager.java
│
├── res/
│   ├── layout/
│   ├── drawable/
│   ├── menu/
│   ├── mipmap/
│   ├── values/
│   └── xml/
```

---

## 🧩 Funcionalidades principales

* **Login y Perfil de usuario:** Autenticación con Firebase y gestión del hangar personal.
* **Radar en vivo (`MapaActivity`):** Visualización de tráfico aéreo cercano con cálculo de distancia GPS en tiempo real.
* **Resolución Híbrida y Caché Inteligente (`AvionGenerator`):**
  * Identificación automática por catálogo maestro local y múltiples APIs externas (ICAO Hex + Matrícula).
  * Caché de doble capa: aciertos guardados permanentemente en `SharedPreferences` y fallos temporales en RAM para reintentos automáticos sin mantenimiento manual.
  * **Easter Eggs operativos:** Reconocimiento automático de vehículos de servicio en tierra (`GRND`, `SERV`, `GND`) y Torres de Control (`TWR`) con categoría de rareza especial.
* **Ficha Técnica en Vivo (`DetalleVueloRadarActivity`):** Vista previa completa antes de capturar, con resolución dinámica de modelos, rareza, telemetría y visor de fotos ampliable por gestos.
* **Registro de Avistamientos con Cámara (`AddAvionActivity`):** Captura directa desde la cámara dentro del radio permitido (40 km), compresión Base64 y subida a Firestore.
* **Ranking Global:** Clasificación pública sincronizada según rareza y fotos verificadas.
* **Chatbot interno:** Asistente integrado en la aplicación.


---

## 👨‍💻 Autores

**Iván Sánchez**  
**Ángel Japón**

---

## 🚀 Arquitectura y Desarrollo

Aplicación desarrollada integrando:

* Consumo multi-API en tiempo real (ADSBExchange / HexDB / ADSBDB / OpenSky / Wikipedia)
* Mapa interactivo con vuelos en directo y geolocalización GPS
* Persistencia híbrida (SharedPreferences + Firebase Firestore)
* Arquitectura Android modular
* RecyclerViews y navegación fluida entre activities
* Control de versiones con Git / GitHub

---

## ✈️ SkyCollectorApp

Aplicación Android de colección, identificación y visualización de tráfico aéreo en tiempo real.
