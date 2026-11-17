# Política de Privacidad — SkyCollectorApp

**Última actualización:** Noviembre de 2026

Esta política explica qué datos recopila **SkyCollectorApp** (versión beta cerrada), cómo se utilizan y cuáles son tus derechos como usuario de prueba.

---

## 1. Datos que recopilamos

Para el funcionamiento de la aplicación y la sincronización de tu progreso en la nube mediante **Google Firebase**, almacenamos los siguientes datos:

* **Datos de autenticación:** Dirección de correo electrónico, número de teléfono (si accedes mediante SMS) e identificador único de usuario (UID).
* **Datos de perfil público:** Nombre de usuario elegido y fotografía de perfil. Estos dos datos son **visibles para otros usuarios** en la tabla de clasificación (*Ranking*).
* **Actividad dentro de la app:** Historial de aeronaves avistadas, fecha/hora del avistamiento, coordenadas aproximadas de captura y puntuación acumulada.
* **Ubicación en tiempo real:** Se utiliza exclusivamente en primer plano mientras usas el radar para calcular la distancia respecto a las aeronaves cercanas (regla de radio de 40 km). **No almacenamos tu historial de ubicación continua en nuestros servidores.**

---

## 2. Servicios de terceros utilizados

La aplicación se conecta a los siguientes servicios externos para ofrecer sus funcionalidades:

* **Google Firebase (Authentication y Cloud Firestore):** Gestión de cuentas y almacenamiento de datos de usuarios y avistamientos.
* **Google Maps SDK for Android:** Visualización del mapa interactivo en el radar.
* **APIs públicas de tráfico aéreo (adsb.lol, ADSBDB, HexDB, OpenSky Network, Planespotters):** Consulta en tiempo real de posiciones y datos técnicos de aeronaves.
* **Wikipedia / Wikimedia Commons:** Obtención de resúmenes enciclopédicos e imágenes ilustrativas de los modelos de aeronaves.

---

## 3. Conservación y eliminación de datos

* Puedes eliminar tus avistamientos registrados directamente desde las opciones de la aplicación.
* Si deseas **eliminar por completo tu cuenta y todos tus datos asociados** (perfil, correo, teléfono y registros en Firestore), puedes solicitarlo en cualquier momento escribiendo a **jotamyndsesp@gmail.com** o abriendo una solicitud en el repositorio oficial: [GitHub Issues](https://github.com/Ivannovichh/SkyCollectorApp/issues). Procesaremos el borrado completo en un plazo máximo de 72 horas.

---

## 4. Seguridad y naturaleza del proyecto

* **SkyCollectorApp** es un proyecto educativo y experimental en fase de **beta cerrada**.
* No vendemos, alquilamos ni compartimos tus datos personales con terceros con fines comerciales o publicitarios.
* Aunque aplicamos reglas de seguridad en Cloud Firestore para proteger los registros de cada usuario, recomendamos no utilizar contraseñas reutilizadas de otros servicios.

---

## 5. Contacto

Para cualquier duda sobre privacidad o gestión de tus datos durante la fase de pruebas, contacta con el desarrollador en **jotamyndsesp@gmail.com**.
