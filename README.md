# ✈️ SkyCollectorApp

**SkyCollectorApp** es una aplicación Android desarrollada en **Java** como proyecto de 2º DAM.
La app permite gestionar una colección de aviones, consultar información de vuelos mediante API externa, visualizar aviones en mapa y usar un sistema de chat interno.

Proyecto académico completo orientado a demostrar desarrollo Android real: interfaz, consumo de API, modelos de datos y arquitectura modular.

---

## 📱 Descripción

SkyCollectorApp combina tres ideas principales:

* Colección personal de aviones
* Consulta de datos de vuelos en tiempo real
* Visualización en mapa
* Chat interno dentro de la app
* Gestión básica de usuario

El usuario puede añadir aviones, ver sus detalles, consultar información externa de vuelos y navegar por distintas pantallas dentro de una app Android estructurada por capas.

---

## 🧠 Tecnologías usadas

* Java
* Android Studio
* Android SDK
* XML (layouts)
* RecyclerView
* Consumo de API REST
* Gradle
* Git / GitHub

---

## 🏗️ Estructura real del proyecto

```
app/
│
├── manifests/
│   └── AndroidManifest.xml
│
├── java/
│   └── es.medac.skycollectorapp/
│       │
│       ├── activities/
│       │   ├── AddAvionActivity.java
│       │   ├── ChatbotActivity.java
│       │   ├── DetalleAvionActivity.java
│       │   ├── LoginActivity.java
│       │   ├── MainActivity.java
│       │   ├── MapaActivity.java
│       │   ├── PerfilActivity.java
│       │   └── TrackResponse.java
│       │
│       ├── adapters/
│       │   ├── AvionAdapter.java
│       │   └── ChatAdapter.java
│       │
│       ├── models/
│       │   ├── Avion.java
│       │   ├── FlightResponse.java
│       │   └── Mensaje.java
│       │
│       ├── network/
│       │   └── FlightRadarService.java
│       │
│       └── utils/
│           └── AvionGenerator.java
│
├── res/
│   ├── drawable/
│   ├── layout/
│   │   ├── activity_add_avion.xml
│   │   ├── activity_chatbot.xml
│   │   ├── activity_detalle_avion.xml
│   │   ├── activity_login.xml
│   │   ├── activity_main.xml
│   │   ├── activity_mapa.xml
│   │   ├── activity_perfil.xml
│   │   ├── item_avion.xml
│   │   ├── item_chat.xml
│   │   └── ventana_info_avion.xml
│   │
│   ├── menu/
│   ├── mipmap/
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   ├── style.xml
│   │   └── themes/
│   │
│   └── xml/
│
└── Gradle Scripts
```

---

## 🧩 Actividades principales

**MainActivity**
Pantalla principal de la aplicación y punto de entrada tras login.

**LoginActivity**
Gestión de acceso del usuario.

**AddAvionActivity**
Permite añadir aviones a la colección.

**DetalleAvionActivity**
Muestra información detallada de un avión.

**MapaActivity**
Visualiza aviones en un mapa interactivo.

**ChatbotActivity**
Sistema de chat dentro de la aplicación.

**PerfilActivity**
Gestión de datos del usuario.

**TrackResponse**
Clase usada para manejar respuestas relacionadas con seguimiento de vuelos.

---

## 🧱 Modelos

**Avion.java**
Representa un avión dentro de la colección.

**FlightResponse.java**
Modelo de respuesta de la API de vuelos.

**Mensaje.java**
Modelo de mensajes del chat.

---

## 🔌 Adaptadores

**AvionAdapter.java**
Adapter para mostrar aviones en RecyclerView.

**ChatAdapter.java**
Adapter del sistema de chat.

---

## 🌐 Red / API

**FlightRadarService.java**
Servicio encargado de conectarse a la API externa de vuelos y obtener información en tiempo real.

---

## 🛠️ Utils

**AvionGenerator.java**
Generador de datos de aviones para pruebas o carga inicial.

---

## 🎨 Layouts principales

| Layout                     | Función                   |
| -------------------------- | ------------------------- |
| activity_main.xml          | Pantalla principal        |
| activity_login.xml         | Login                     |
| activity_add_avion.xml     | Añadir avión              |
| activity_detalle_avion.xml | Detalle avión             |
| activity_mapa.xml          | Mapa                      |
| activity_chatbot.xml       | Chat                      |
| activity_perfil.xml        | Perfil                    |
| item_avion.xml             | Item RecyclerView aviones |
| item_chat.xml              | Mensajes chat             |
| ventana_info_avion.xml     | Info en mapa              |

---

## ▶️ Ejecución

```bash
git clone https://github.com/Ivannovichh/SkyCollectorApp.git
```

Abrir en Android Studio → Sync Gradle → Ejecutar en emulador o dispositivo.

---

## 🚀 Funcionalidades

* Gestión de colección de aviones
* Visualización en mapa
* Consulta de vuelos mediante API
* Chat integrado
* Perfil de usuario
* RecyclerViews personalizados
* Arquitectura por paquetes

---

## 👨‍💻 Autores

**Iván Sánchez**
**Ángel Japón**

2º DAM — Desarrollo de Aplicaciones Multiplataforma

---

## 🎓 Proyecto académico

Aplicación desarrollada como práctica completa de Android integrando:

* Navegación entre activities
* Consumo de API
* Modelos de datos
* Adaptadores RecyclerView
* Organización por paquetes
* Uso de GitHub

---

## ✈️ SkyCollectorApp

Proyecto Android de colección y seguimiento de aviones.
