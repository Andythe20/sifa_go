# SIFA GO - Aplicacion Movil de Fiscalizacion

SIFA GO es una aplicacion movil desarrollada en Android con Kotlin y Jetpack Compose, disenada para digitalizar el proceso de fiscalizacion vehicular en terreno. Permite a los fiscalizadores capturar patentes, procesarlas mediante OCR (Inteligencia Artificial) y agilizar la emision de multas.

**Estado del Proyecto:** En Desarrollo

---

## Funcionalidades

- Captura de evidencia fotografica con CameraX
- Previsualizacion y confirmacion de captura antes de enviar
- Consumo de API REST (Python/FastAPI) para OCR de patentes via Retrofit
- Autenticacion biométrica (huella digital, rostro) + credencial de dispositivo
- Navegacion entre pantallas con Navigation Compose
- Visualizacion de resultados de consulta de patentes
- Notificaciones push con Firebase Cloud Messaging
- Localizacion GPS y deteccion de cambios en proveedores de ubicacion
- Soporte offline preparado para implementacion con Room
- Gestion de sesion de usuario
- Deteccion de conectividad de red

---

## Especificaciones Tecnicas

| Parametro | Valor |
|-----------|-------|
| Min SDK | 24 (Android 7.0 Nougat) |
| Target SDK | 36 (Android 16) |
| Compile SDK | 36 |
| Kotlin | 2.0.21 |
| AGP | 8.13.2 |
| Gradle | 8.13 |
| Compose BOM | 2024.09.00 |

### Compatibilidad con API 24

La aplicacion es 100% compatible con API 24 gracias a:

- **Desugaring de Java 8+:** `coreLibraryDesugaringEnabled = true` con `desugar_jdk_libs:2.1.4` para habilitar `java.time` y otras APIs modernas en versiones antiguas de Android.
- **Guardias de API:** Todo codigo que utiliza APIs introducidas despues de API 24 esta protegido con `Build.VERSION.SDK_INT`.
- **Librerias retrocompatibles:** Todas las dependencias (Compose, CameraX, Coil, Retrofit, Biometric, Firebase, Play Services) soportan API 21+.


### Dispositivos Verificados

La aplicacion ha sido probada y funciona correctamente en los siguientes niveles de API:

| API | Version Android | Estado |
|-----|-----------------|--------|
| 24 | Android 7.0 Nougat | Funcional |
| 25 | Android 7.1.1 Nougat | Funcional |
| 27 | Android 8.1 Oreo | Funcional |
| 30 | Android 11 | Funcional |
| 33 | Android 13 | Funcional |

---

## Requisitos Previos

- Android Studio (Iguana o superior)
- Dispositivo Android fisico (API 24 o superior)
- Cable USB para depuracion

## Instalacion y Ejecucion

### 1. Preparar el Dispositivo

1. Ve a Ajustes > Acerca del telefono.
2. Toca 7 veces sobre "Numero de compilacion" hasta activar modo desarrollador.
3. En Ajustes > Opciones de desarrollador, activa "Depuracion por USB".

### 2. Ejecutar la Aplicacion

1. Clona el repositorio y abrelo en Android Studio.
2. Conecta el dispositivo por USB y acepta el permiso de depuracion.
3. Selecciona el dispositivo en el menu desplegable de Android Studio.
4. Haz clic en "Run" (Shift + F10).

### 3. Probar Conexion con Backend (IA)

La aplicacion se comunica con un motor de IA (YOLO/PaddleOCR) para extraer texto de patentes. Para pruebas locales, PC y dispositivo deben estar en la misma red Wi-Fi.

1. Obtén tu IP local (`ipconfig` en Windows, `ip a` en Linux/Mac).
2. Abre `app/src/main/java/com/sifa/sifa_go/core/network/ServerConfig.kt`.
3. Modifica `BASE_URL` con tu IP y puerto (generalmente 8001):
   ```kotlin
   const val BASE_URL = "http://TU_IP_LOCAL:8001/"
   ```
4. Asegurate de que el backend de Python este corriendo (via `docker-compose up`).
5. Compila y prueba la captura de fotografia.

---

## Documentacion Adicional

- `gradle/libs.versions.toml` - Catalogo de versiones de dependencias.
