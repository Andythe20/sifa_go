# SIFA GO - Aplicación Móvil de Fiscalización

**SIFA GO** es una aplicación móvil desarrollada en Android (Kotlin/Jetpack Compose) diseñada para optimizar y digitalizar el proceso de fiscalización vehicular en terreno. Su objetivo principal es permitir a los fiscalizadores capturar patentes de vehículos, procesarlas mediante Inteligencia Artificial (OCR) y agilizar la emisión de multas.

> 🚧 **Estado del Proyecto:** En Desarrollo (Work In Progress)

## 📱 Funcionalidades Actuales
* **Captura de Evidencia:** Integración nativa con la cámara del dispositivo usando CameraX para capturar fotografías de las placas patentes.
* **Interfaz de Previsualización:** Flujo interactivo que permite al fiscalizador revisar la fotografía tomada, con opciones para reintentar la captura o enviarla a procesar.
* **Conexión con IA (Backend):** Consumo de una API REST (Python/FastAPI) mediante Retrofit para enviar la fotografía y recibir los datos extraídos de la patente en formato JSON.

## 🚀 Próximos Pasos (Roadmap)
* **Paso 4:** Conectar el texto de la patente reconocida con el backend principal (Java/Spring Boot) para consultar los datos del vehículo y del propietario.
* **Paso 5:** Interfaz de validación para que el fiscalizador decida si procede o no la infracción en base a los datos recibidos.
* **Paso 6:** Generación automática del formulario de la multa, pre-llenado con la información del vehículo y adjuntando la fotografía original como evidencia en almacenamiento persistente.
* **Modo Offline:** Implementación de base de datos local (SQLite/Room) para permitir la captura de evidencia en zonas sin cobertura de red y sincronización diferida.

---

## 🛠️ Requisitos Previos para Desarrollo

Para compilar y ejecutar este proyecto en tu entorno local, necesitarás:
* [Android Studio](https://developer.android.com/studio) (Versión Iguana o superior recomendada).
* Un dispositivo físico Android (Celular o Tablet).
* Cable USB para conectar el dispositivo al PC.

## ⚙️ Instrucciones de Instalación y Pruebas

### 1. Preparar el Dispositivo Físico (Móvil)
Para poder instalar la aplicación en desarrollo directamente desde Android Studio, debes habilitar opciones especiales en tu celular:
1. Ve a **Ajustes > Acerca del teléfono**.
2. Toca 7 veces seguidas sobre el **"Número de compilación"** (o "Versión de MIUI" / "Versión de software") hasta que aparezca el mensaje *"¡Ya eres un desarrollador!"*.
3. Vuelve al menú principal de Ajustes y busca **"Opciones de desarrollador"** (suele estar dentro de "Sistema" o "Ajustes adicionales").
4. Activa la opción **Depuración por USB**.

### 2. Ejecutar la Aplicación
1. Clona este repositorio y ábrelo en **Android Studio**.
2. Conecta tu dispositivo móvil al PC mediante el cable USB. (Acepta el mensaje de "Permitir depuración USB" que aparecerá en la pantalla del celular).
3. En la barra superior de Android Studio, selecciona tu dispositivo en el menú desplegable.
4. Haz clic en el botón verde **"Run"** (Shift + F10) para compilar e instalar la app en tu celular.

### 3. Probar la conexión con el Backend (IA)
Esta aplicación móvil está diseñada para comunicarse con el motor de IA (YOLO/PaddleOCR) que extrae el texto de las patentes. Para probar esto en tu entorno local, **tanto tu PC (donde corre el backend) como tu celular (donde corre la app) deben estar conectados a la misma red Wi-Fi.**

Dado que la IP local cambia dependiendo de la red, debes actualizarla en el código de Android antes de compilar:

1. En tu PC, obtén tu dirección IP local (ej. usando `ipconfig` en Windows o `ip a` en Linux/Mac).
2. En el proyecto de Android Studio, abre el archivo de configuración de red:
   `app/src/main/java/com/example/sifa_go/core/network/PlateDetectorApi.kt`
3. Modifica la variable `BASE_URL` con tu IP actual y el puerto donde se expone el contenedor de Docker (generalmente el 8001):
   ```kotlin
   // Ejemplo de configuración:
   // Modificar ip dependiendo a qué red te conectes
   private const val BASE_URL = "http://TU_IP_LOCAL:8001/"
    ```
4. Asegúrate de que tu backend de Python esté ejecutándose (vía `docker-compose up`).
5. Compila nuevamente la aplicación y prueba tomar una fotografía.