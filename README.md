# ✈️ SkyCollectorApp

**SkyCollectorApp** es una aplicación Android desarrollada en **Java** como proyecto de 2º DAM.
Permite gestionar una colección de aviones, visualizar vuelos reales en un mapa y consultar información aeronáutica mediante una API externa.

Proyecto académico orientado a demostrar desarrollo Android completo: interfaz, consumo de API, arquitectura modular y visualización de datos en tiempo real.

---

## 📱 Descripción

SkyCollectorApp combina:

* Colección personal de aviones
* Visualización de vuelos reales en mapa
* Consulta de datos aeronáuticos
* Chat interno
* Perfil de usuario

La aplicación integra datos externos de aviación con una interfaz móvil Android estructurada por paquetes.

---

## 🌍 API utilizada

Para la funcionalidad del mapa se utiliza la **API de OpenSky Network**, que proporciona datos reales de tráfico aéreo:

* Posición de aviones en tiempo real
* Identificación de vuelos
* Coordenadas geográficas
* Información de seguimiento

Estos datos se consumen desde la app y se representan en el mapa dentro de **MapaActivity**, permitiendo visualizar aeronaves activas.

---

## 🧠 Tecnologías usadas

* Java
* Android Studio
* Android SDK
* XML layouts
* RecyclerView
* API REST (OpenSky)
* Gradle
* Git / GitHub

---

## 🏗️ Estructura del proyecto

```
app/
│
├── manifests/
│   └── AndroidManifest.xml
│
├── java/es.medac.skycollectorapp/
│   │
│   ├── activities/
│   │   ├── AddAvionActivity.java
│   │   ├── ChatbotActivity.java
│   │   ├── DetalleAvionActivity.java
│   │   ├── LoginActivity.java
│   │   ├── MainActivity.java
│   │   ├── MapaActivity.java
│   │   ├── PerfilActivity.java
│   │   └── TrackResponse.java
│   │
│   ├── adapters/
│   │   ├── AvionAdapter.java
│   │   └── ChatAdapter.java
│   │
│   ├── models/
│   │   ├── Avion.java
│   │   ├── FlightResponse.java
│   │   └── Mensaje.java
│   │
│   ├── network/
│   │   └── FlightRadarService.java
│   │
│   └── utils/
│       └── AvionGenerator.java
│
├── res/
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
│   ├── drawable/
│   ├── menu/
│   ├── mipmap/
│   ├── values/
│   └── xml/
```

---

## 🧩 Actividades principales

**MainActivity** → Pantalla principal
**LoginActivity** → Acceso de usuario
**MapaActivity** → Mapa con datos de OpenSky
**AddAvionActivity** → Añadir aviones
**DetalleAvionActivity** → Información detallada
**ChatbotActivity** → Chat interno
**PerfilActivity** → Perfil usuario

---

## 🌐 Consumo de API

La clase:

**FlightRadarService.java**

se encarga de:

* Conectar con la API OpenSky
* Obtener datos de vuelos
* Procesar respuestas
* Enviar datos al mapa

Los modelos **FlightResponse** y **TrackResponse** representan las respuestas de la API.

---

## ▶️ Ejecución

```bash
git clone https://github.com/Ivannovichh/SkyCollectorApp.git
```

Abrir en Android Studio → Sync Gradle → Ejecutar.

---

## 🚀 Funcionalidades

* Colección de aviones
* Visualización en mapa en tiempo real
* Datos reales de OpenSky
* Chat interno
* Perfil de usuario
* RecyclerViews personalizados

---

## 👨‍💻 Autores

**Iván Sánchez**
**Ángel Japón**

2º DAM — Desarrollo de Aplicaciones Multiplataforma

---

## 🎓 Proyecto académico

Aplicación desarrollada como práctica de Android integrando:

* API externa real
* Visualización en mapa
* Arquitectura por paquetes
* Navegación entre activities
* GitHub

---

## ✈️ SkyCollectorApp

App Android de colección y visualización de tráfico aéreo en tiempo real mediante OpenSky API.
