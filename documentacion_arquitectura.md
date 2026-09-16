# Documentación de Arquitectura — SIFA_GO

> Aplicación Android para fiscalizadores de tránsito. Escanea patentes, emite infracciones y gestiona la presencia en terreno.
> Desarrollada con **Kotlin**, **Jetpack Compose** y **arquitectura limpia** (data / domain / infrastructure / ui / viewmodel / core).

---

## Índice

1. [Ubicación y precisión GPS](#1-ubicación-y-precisión-gps)
5. [Notificaciones push (Firebase Cloud Messaging)](#5-notificaciones-push-firebase-cloud-messaging)
2. [Permisos (notificaciones, ubicación, cámara)](#2-permisos)
3. [Almacenamiento del JWT y gestión de sesión](#3-jwt-y-sesión)
4. [Pipeline de captura y almacenamiento de imágenes](#4-pipeline-de-imágenes)

---

## 1. Ubicación y precisión GPS

### Dependencias utilizadas

| Librería | Versión | ¿Qué es? |
|----------|---------|----------|
| `com.google.android.gms:play-services-location` | (vía catálogo de versiones) | **Google Play Services Location** — API oficial de Google para acceder al GPS del dispositivo. Proporciona `FusedLocationProviderClient`, que combina señales de GPS, redes WiFi y torres de telefonía para dar una ubicación más precisa y con menor consumo de batería que el `LocationManager` de Android nativo. |
| `androidx.exifinterface:exifinterface` | 1.3.7 | Se usa en geocodificación inversa para extraer direcciones legibles — no está relacionada directamente con el GPS, pero se invoca desde `LocationHelper`. |

### Implementación

#### LocationHelper (`core/utils/LocationHelper.kt`)

Clase helper que encapsula toda la lógica de ubicación. Recibe un `Context` y crea una instancia de `FusedLocationProviderClient`:

```kotlin
private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)
```

Ofrece dos mecanismos:

##### a) Obtención única (`getLocation()`)
Método `suspend` que usa `getCurrentLocation()` con `Priority.PRIORITY_HIGH_ACCURACY`. Esto le pide al sistema que active el GPS si es necesario y devuelva la mejor ubicación disponible en ese momento. Está envuelto en `suspendCancellableCoroutine` para integrarse con corrutinas de Kotlin.

```kotlin
suspend fun getLocation(): Location? = suspendCancellableCoroutine { continuation ->
    val task = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
    task.addOnSuccessListener { location -> continuation.resume(location) }
    task.addOnFailureListener { continuation.resume(null) }
}
```

##### b) Calibración de precisión (`startPrecisionCalibration()`)
Solicita **5 actualizaciones rápidas** de ubicación con un intervalo de 1 segundo y un mínimo de 500ms entre cada una. Cada vez que llega una nueva ubicación, ejecuta el callback `onLocationReceived`.

```kotlin
val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
    .setMinUpdateIntervalMillis(500L)
    .setMaxUpdates(5)
    .build()
```

##### c) Geocodificación inversa (`getAddressFromLocation()`)
Convierte coordenadas (lat, lon) en una dirección legible usando `Geocoder`. Maneja la API moderna (callback asíncrono en API 33+) y la deprecated (síncrona en hilo aparte para APIs anteriores).

#### Lógica de calibración en SifaViewModel (`viewmodel/SifaViewModel.kt`)

`startGpsCalibration()` inicia la calibración y `processCalibrationStep()` se ejecuta para cada una de las 5 muestras:

```kotlin
fun processCalibrationStep(location: Location) {
    gpsAttemptCount++
    val currentAccuracy = location.accuracy
    if (gpsAccuracy == null || currentAccuracy < (gpsAccuracy ?: Float.MAX_VALUE)) {
        latitude = location.latitude
        longitude = location.longitude
        gpsAccuracy = currentAccuracy
    }
}
```

**Estrategia:** de las 5 muestras, se conserva la de **menor `accuracy`** (valor en metros, más bajo = más preciso). Un temporizador de 5 segundos (`gpsTimerJob`) detiene la calibración automáticamente.

#### Observador de estado del GPS (`core/network/LocationObserver.kt`)

Un `BroadcastReceiver` envuelto en un `callbackFlow` de Kotlin que reacciona a `LocationManager.PROVIDERS_CHANGED_ACTION` y emite `GpsStatus.Available` o `GpsStatus.Unavailable`. Incluye un `@Composable` helper `rememberGpsStatus()` para uso directo desde la UI.

#### Heartbeat periódico (PresenceViewModel)

Cada 3 minutos (`ServerConfig.HEARTBEAT_INTERVAL_MS = 180_000L`) el `PresenceViewModel` obtiene la ubicación con `locationHelper.getLocation()` y la envía al backend junto con el `deviceId`, la marca y el modelo del dispositivo.

---

## 2. Permisos

### Dependencias utilizadas

| Librería | Versión | ¿Qué es? |
|----------|---------|----------|
| `com.google.accompanist:accompanist-permissions` | 0.28.0 | **Accompanist Permissions** — Librería de Google que ofrece APIs declarativas para manejar permisos runtime en Jetpack Compose. Proporciona `rememberMultiplePermissionsState()` que gestiona el estado de los permisos, `launchMultiplePermissionRequest()` para solicitarlos, y `shouldShowRationale` para mostrar explicaciones al usuario. |
| `androidx.activity:activity-compose` | (vía BOM) | Proporciona `registerForActivityResult()` en `MainActivity` para solicitar permisos individuales con el contrato `ActivityResultContracts.RequestPermission`. |

### Permisos declarados en AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

La cámara se declara como no requerida (`android:required="false"`) para que la app se pueda instalar en dispositivos sin cámara.

### Implementación

#### Notificaciones — en MainActivity

Se solicita al arrancar la app, solo en Android 13+ (TIRAMISU):

```kotlin
private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

Usa `ActivityResultContracts.RequestPermission()` con un callback que ignora el resultado (se solicita una sola vez y no bloquea la funcionalidad).

#### Cámara + Ubicación — en AppNavigation (navegación principal)

Al entrar al flujo principal de la app se activa un `LaunchedEffect` que dispara el diálogo multi-permiso de Accompanist:

```kotlin
val permissionsState = rememberMultiplePermissionsState(
    permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
)
LaunchedEffect(Unit) {
    if (!permissionsState.allPermissionsGranted) {
        permissionsState.launchMultiplePermissionRequest()
    }
}
```

#### Cámara + Ubicación — en LiveScannerScreen (pantalla de scanner)

Se vuelve a verificar con el mismo patrón. Si los permisos **no** están concedidos, se muestra una tarjeta con:
- Explicación de por qué se necesitan los permisos.
- Botón "Conceder permiso" si `shouldShowRationale` es `true`.
- Botón "Ir a configuración" si el usuario ya denegó permanentemente (abre los ajustes del sistema con `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).

Cuando se conceden, se dispara automáticamente `onStartGpsCalibration()` para comenzar la calibración GPS.

---

## 3. JWT y sesión

### Dependencias utilizadas

| Librería | Versión | ¿Qué es? |
|----------|---------|----------|
| `com.squareup.retrofit2:retrofit` | 2.9.0 | **Retrofit** — Cliente HTTP tipado para Android. Convierte APIs REST en interfaces Kotlin. Se usa para todas las llamadas al backend. |
| `com.squareup.retrofit2:converter-gson` | 2.9.0 | **Converter Gson** — Plugin de Retrofit que serializa/deserializa automáticamente JSON a objetos Kotlin usando Gson. |
| `com.squareup.okhttp3:okhttp` | (transitiva) | **OkHttp** — Cliente HTTP subyacente de Retrofit. Permite agregar interceptores como `AuthInterceptor`. |
| `androidx.security:security-crypto` | **NO se usa** | Deprecada por Google (2025-2026). Se usa **Android Keystore directo** (`core/security/KeystoreAesCipher.kt`) en su lugar. |

### Almacenamiento — SessionManager (`core/utils/SessionManager.kt`)

Implementa la interfaz `SessionRepository` (`domain/repository/SessionRepository.kt`). Usa **`SharedPreferences`** con el archivo **`sifa_session_enc`** en modo privado (`Context.MODE_PRIVATE`), pero **todos los valores se cifran** con **AES/GCM** respaldado por una clave maestro del **Android Keystore** (`core/security/KeystoreAesCipher.kt`). En disco solo existe ciphertext Base64; la clave maestro nunca sale del hardware (TEE/StrongBox).

**Datos almacenados (cifrados):**

| Clave | Contenido |
|-------|-----------|
| `TOKEN` | JWT access token |
| `REFRESH_TOKEN` | Refresh token (para renovar el access token) |
| `TOKEN_EXPIRY` | Timestamp `exp` del JWT (segundos desde epoch) |
| `TOKEN_IAT` | Timestamp `iat` del JWT |
| `USERNAME` | `sub` del JWT (email del usuario) |
| `ROLES` | Roles serializados como texto (separados por `\n`), cifrados |

**Formato del ciphertext:** `Base64(IV[12 bytes] || ciphertextGCM)`. El tag GCM (128 bits) de autenticación va incluido al final del ciphertext.

**Cifrado de sesión — Android Keystore (no EncryptedSharedPreferences):**

- `EncryptedSharedPreferences` fue **deprecada** por Google (librería `androidx.security:security-crypto` congelada). La recomendación oficial es el uso directo del **Android Keystore** (`KeyGenerator` + `KeyGenParameterSpec`) o Google Tink.
- `KeystoreAesCipher` genera una clave AES-256 en el Keystore bajo el alias `sifa_keystore_master_key`, con `AES/GCM/NoPadding` y IV aleatorio por operación (GCM es autenticado: detecta manipulación/corrupción).
- La clave **no** se vincula a autenticación biométrica: así sobrevive el cambio de huellas/rostro (la biometría es un feature aparte de la app).
- Manejo de fallos: dato manipulado (`AEADBadTagException`) → devuelve `null` **sin** borrar la clave; clave invalidada/eliminada del Keystore (`KeyPermanentlyInvalidatedException`) → se invalida localmente y se devuelve `null`, lo que fuerza un logout limpio (re-login).

**Migración one-time:** al arrancar, `SessionManager(context)` y `SharedPreferencesPushTokenRepository(context)` ejecutan `StorageMigration` (`core/security/StorageMigration.kt`): leen el archivo legacy en texto plano (`sifa_session` / `sifa_push`), re-cifran los valores y **borran el archivo legacy** (`deleteSharedPreferences`). La operación es idempotente (flag `MIGRATED_V1`, escrita al final con `commit()`).

**Exclusión de backup:** los archivos `sifa_session_enc.xml` y `sifa_push_enc.xml` (y los legacy) están excluidos del backup en nube y de la transferencia dispositivo-a-dispositivo (`res/xml/backup_rules.xml` para API ≤ 30 y `res/xml/data_extraction_rules.xml` para API ≥ 31). El resto de datos (Room, estado, etc.) sí se respalda.

**Evento de sesión expirada:** `SessionManager` expone un `SharedFlow<Unit>` llamado `sessionExpiredEvent` que es emitido desde `AuthInterceptor` cuando el refresh token también expira. En `AppNavigation` se recolecta este evento para redirigir al login.

### Flujo de login — AuthViewModel

1. El usuario ingresa email y contraseña.
2. `AuthViewModel.login()` envía un POST a `/auth/api/v1/login`.
3. Si la respuesta es exitosa y el rol `USER_APP` está presente, se guarda la sesión llamando a `sessionManager.saveSession(...)`.
4. Se registra el dispositivo con FCM token (`registerDevice()`).

### AuthInterceptor — Inyección automática del token (`core/network/AuthInterceptor.kt`)

Interceptor de OkHttp que se ejecuta en **cada** petición HTTP:

1. **Omite** las rutas `/auth/api/v1/login` y `/auth/api/v1/refresh`.
2. **Adjunta** el header `Authorization: Bearer <token>` a todas las demás peticiones.
3. **Refresh proactivo:** si el token expira en menos de 30 segundos (`currentTime >= (expiry * 1000) - 30s`), intenta renovarlo automáticamente antes de enviar la petición original.
4. **Manejo de 401/403:** si el servidor responde con no autorizado, el interceptor:
   - Usa `synchronized(this)` para evitar condiciones de carrera entre hilos.
   - Si otro hilo ya refrescó el token, re-intenta la petición original.
   - Si no, intenta refrescar con el `refreshRetrofit` (cliente OkHttp separado y sin el propio interceptor para evitar loops infinitos).
   - **Resultados posibles:** `Success` (re-intenta), `Expired` (emite `sessionExpiredEvent` y cierra sesión), `NetworkError` (retorna 401 sintético).

### Verificación de sesión al inicio — AppNavigation

Al abrir la app, si `sessionManager.hasValidSession()` es `true`:
1. Se intenta un **refresh proactivo** del token.
2. Luego se muestra la autenticación biométrica (`BiometricHelper`).
3. Si la biometría es exitosa, se navega a la pantalla principal.

---

## 4. Pipeline de imágenes

### Dependencias utilizadas

| Librería | Versión | ¿Qué es? |
|----------|---------|----------|
| `androidx.camera:camera-camera2` | 1.6.0 | **CameraX Camera2** — Implementación de CameraX basada en la API Camera2 de Android. Proporciona una capa de abstracción consistente entre dispositivos. |
| `androidx.camera:camera-lifecycle` | 1.6.0 | **CameraX Lifecycle** — Permite vincular el ciclo de vida de la cámara al de un `LifecycleOwner` (Activity o Fragment). |
| `androidx.camera:camera-view` | 1.6.0 | **CameraX View** — Proporciona `PreviewView` (vista para mostrar el preview de la cámara en pantalla) y `LifecycleCameraController` (controlador unificado que maneja preview, captura de fotos y análisis de imagen). |
| `com.google.guava:guava` | 33.4.0-android | **Guava** — Librería de utilidades de Google. CameraX la usa internamente para `ListenableFuture` y otras operaciones asíncronas. |
| `androidx.exifinterface:exifinterface` | 1.3.7 | **ExifInterface** — Permite leer y escribir metadatos EXIF en imágenes JPEG (rotación, GPS, fabricante, modelo, fecha). Se usa para corregir orientación y para eliminar metadatos sensibles. |
| `io.coil-kt:coil-compose` | 2.7.0 | **Coil** — Cargador de imágenes liviano para Kotlin y Compose. Se usa para mostrar fotos de evidencia y previews en la UI. |

### Pipeline completo

```
┌─────────────────────────────────────────────────────────────────┐
│                    CAPTURA (CameraX)                            │
│  LifecycleCameraController.takePictureWithFlash()               │
│  → setImageCaptureFlashMode(ON/OFF)                             │
│  → takePicture(outputOptions, executor, callback)               │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              ARCHIVO TEMPORAL                                    │
│  context.cacheDir / sifa_photo_<timestamp>.jpg                   │
│  Creado por ImageCapture.OutputFileOptions.Builder(photoFile)    │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              IMAGE SANITIZER (3 pasos)                           │
│  ImageSanitizer.sanitize(photoFile)                              │
│                                                                  │
│  Paso 1: OrientationCorrector                                    │
│  ─────────────────────────────────────                           │
│  Lee EXIF TAG_ORIENTATION. Si la foto está rotada (90°/180°/    │
│  270°), decodifica el bitmap, aplica Matrix.postRotate() y lo   │
│  re-guarda. Si ya está normal, devuelve el archivo original.    │
│                                                                  │
│  Paso 2: ExifMetadataStripper                                    │
│  ─────────────────────────────────────                           │
│  Verifica si la imagen tiene metadatos EXIF (latLong, TAG_MAKE,  │
│  TAG_MODEL, TAG_DATETIME). Si tiene, decodifica el bitmap y lo  │
│  re-guarda como JPEG (esto elimina TODO el EXIF automáticamente) │
│  → PRIVACIDAD: se eliminan coordenadas GPS, marca, modelo y      │
│    fecha del dispositivo de la foto.                             │
│                                                                  │
│  Paso 3: ImageCompressor (80%)                                   │
│  ─────────────────────────────                                   │
│  Decodifica el bitmap y lo re-comprime con JPEG quality 80       │
│  para reducir el tamaño del archivo antes de subirlo.            │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
└───────────────────────┬─────────────────────────────────────────┘
│          ARCHIVO PERMANENTE                                       │
│  Se devuelve el absolutePath del archivo procesado (ej:           │
│  compressed_stripped_rotated_sifa_photo_1234567890.jpg)          │
│                                                                   │
│  Se elimina el archivo temporal original (photoFile.delete())    │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              ALMACENAMIENTO EN VIEWMODEL                         │
│  SifaViewModel.currentPhotoPath = permanentPath                 │
│  o → SifaViewModel.evidencePhotoPaths.add(permanentPath)         │
│                                                                  │
│  Si se elimina evidencia:                                        │
│  ImageUtils.deleteImageFile(path) + remove de la lista          │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│              SUBIDA AL BACKEND                                    │
│                                                                  │
│  Opción A: Detección de patente (SifaViewModel)                  │
│  ──────────────────────────────────────────────                  │
│  File(filePath).toCleanMultipartPart("file")                     │
│  → sanitiza NUEVAMENTE la imagen                                  │
│  → crea MultipartBody.Part                                       │
│  → POST /plate/api/v1/detect                                     │
│  → parsea DetectionRootResponse, extrae plateResult              │
│                                                                  │
│  Opción B: Envío de multa (CoreViewModel)                        │
│  ─────────────────────────────────────                           │
│  imagePaths.map { File(it).toCleanMultipartPart("fotos") }       │
│  → sanitiza CADA imagen otra vez                                  │
│  → crea lista de MultipartBody.Part                              │
│  → POST /core/api/v1/infracciones                                │
│    con @Part request (JSON) y @Part fotos (multipart)            │
└─────────────────────────────────────────────────────────────────┘
```

### Detalles de implementación por archivo

| Archivo | Rol |
|---------|-----|
| `core/utils/CameraUtils.kt` | Extensión `takePictureWithFlash()` que configura el flash en el `LifecycleCameraController` antes de capturar. |
| `ui/views/LiveScannerScreen.kt` | Función `takePicture()` (privada): crea el archivo temporal en `cacheDir`, llama a `takePictureWithFlash()`, y en `onImageSaved` pasa el archivo por `ImageSanitizer.sanitize()` y elimina el temporal. |
| `core/image/ImageSanitizer.kt` | Orquesta los 3 pasos del pipeline. Expone `sanitize()` (pipeline completo) y `sanitizeLight()` (solo corrección de orientación + stripping EXIF, sin compresión). |
| `core/image/ImageProcessingPipeline.kt` | Implementa el patrón **Chain of Responsibility**: ejecuta cada `ImageProcessor` en secuencia, pasando el archivo resultante de uno al siguiente. Los archivos intermedios se marcan con `deleteOnExit()`. |
| `core/image/ImageProcessor.kt` | Interfaz funcional: `fun process(input: File): File`. |
| `core/image/OrientationCorrector.kt` | Lee `ExifInterface.TAG_ORIENTATION`, rota el bitmap si es necesario, guarda como `rotated_<original>`. |
| `core/image/ExifMetadataStripper.kt` | Verifica existencia de metadatos EXIF (GPS, make, model, datetime). Si existen, re-codifica el bitmap sin metadatos como `stripped_<original>`. |
| `core/image/ImageCompressor.kt` | Re-comprime a JPEG calidad 80% (configurable) y guarda como `compressed_<original>`. |
| `core/image/FileExtensions.kt` | Extensiones `File.toCleanMultipartPart()` y `List<File>.toCleanMultipartParts()` que sanitizan la imagen (por defecto con `ImageSanitizer::sanitize`) y crean `MultipartBody.Part` para subir por Retrofit. |
| `core/utils/ImageUtils.kt` | Utilidad `deleteImageFile()` para eliminar archivos físicos. |
| `viewmodel/SifaViewModel.kt` | Gestiona `currentPhotoPath`, `evidencePhotoPaths`, llama a `uploadImageToBackend()` que usa `toCleanMultipartPart()`. |
| `viewmodel/CoreViewModel.kt` | `submitInfraccion()` sanitiza cada foto de evidencia y las envía como multipart al crear la infracción. |

---

## 5. Notificaciones push (Firebase Cloud Messaging)

### Dependencias utilizadas

| Librería | Versión | ¿Qué es? |
|----------|---------|----------|
| `com.google.firebase:firebase-messaging` | 24.1.0 | **Firebase Cloud Messaging (FCM)** — Servicio de mensajería push de Google. Permite enviar notificaciones desde un servidor a dispositivos Android (y otras plataformas) de forma gratuita y escalable. Cada dispositivo se identifica con un token único que debe registrarse en el backend. |
| `com.google.gms:google-services` | 4.4.2 | **Google Services Gradle Plugin** — Plugin de Gradle que procesa el archivo `google-services.json` durante la compilación y genera los recursos de Android necesarios para que Firebase funcione (ID del proyecto, clave API, etc.). Se aplica en `build.gradle.kts` raíz y de módulo. |
| `com.google.firebase:firebase-messaging` | 24.1.0 | Dependencia única de Firebase — no se usan otros productos (ni Firestore, ni Analytics, ni Authentication). |

### Archivo de configuración Firebase

- `app/google-services.json` — Archivo generado desde la consola de Firebase que contiene la configuración del proyecto (project_id, api_key, client_id, etc.). El plugin `google-services` lo lee automáticamente en tiempo de compilación.
- `google-services.json` (raíz del proyecto) — Copia adicional, probablemente no utilizada.

### Pipeline detallado

A continuación se explica el recorrido completo que sigue una notificación push, desde que la app se inicia por primera vez hasta que el dispositivo recibe y muestra una notificación enviada desde el servidor. El flujo se divide en 4 etapas fundamentales.

---

#### Etapa 1: Inicialización y obtención del token FCM

**¿Qué es el token FCM?** Es un identificador único que Firebase asigna a cada instalación de la app en un dispositivo específico. Funciona como una "dirección" que el backend necesita conocer para enviarle notificaciones a ese dispositivo en particular. Si se desinstala y reinstala la app, se genera un token nuevo.

**¿Cuándo ocurre esta etapa?** Apenas el usuario abre la app, en `SifaApplication.onCreate()`.

**¿Qué pasa paso a paso?**

1. **`FirebaseApp.initializeApp(this)`** — Inicializa el SDK de Firebase. Lee la configuración del archivo `google-services.json` (que contiene el project_id, api_key, etc., generado desde la consola de Firebase) y la usa para conectar la app con el proyecto de Firebase correspondiente. Sin esta llamada, ninguna funcionalidad de Firebase funciona.

2. **`createNotificationChannel()`** — Crea el canal de notificaciones `"sifa_push"` con prioridad `IMPORTANCE_HIGH`. En Android 8+ (API 26), las notificaciones deben pertenecer a un canal para poder mostrarse. El usuario puede silenciar o personalizar cada canal desde Configuración. Si no se crea el canal antes de enviar la primera notificación, esta no se muestra.

3. **`FirebaseMessaging.getInstance().token`** — Solicita a los servidores de Firebase el token FCM actual. Esto es una operación de red asíncrona que devuelve un `Task<String>`. Firebase internamente se comunica con sus servidores, y si es la primera vez, genera un token nuevo; si ya existía uno (por ejemplo, si la app se actualizó sin desinstalarse), devuelve el mismo.

4. **`addOnSuccessListener`** — Cuando el `Task` se completa exitosamente, se ejecuta este callback. El token recibido (un string como `"fP1zA2b3cD4e..."`) se guarda localmente llamando a `pushTokenRepository.saveToken(PushToken(token))`, que lo persiste en `SharedPreferences` bajo el archivo `sifa_push` con clave `FCM_TOKEN`.

5. **`addOnFailureListener`** — Si la solicitud del token falla (por ejemplo, no hay internet, o los servidores de Firebase no responden), se registra un error en Logcat. Firebase reintentará automáticamente más tarde y eventualmente llamará a `onNewToken()` del servicio FCM cuando el token esté disponible.

**Código relevante:** `SifaApplication.kt` — método `retrieveFcmToken()`.

---

#### Etapa 2: Refresco del token FCM en runtime

**¿Por qué cambiaría el token?** FCM puede rotar el token en varios escenarios:
- La app se reinstala.
- El usuario borra los datos de la app desde Configuración.
- La app se restaura en un dispositivo nuevo desde un backup.
- Firebase detecta un problema de seguridad o expiración del token anterior.

**¿Cómo se entera la app?** Firebase llama automáticamente al método `onNewToken(token: String)` del `SifaFirebaseMessagingService` cada vez que se asigna un token nuevo.

**¿Qué hace la app con el token nuevo?** Solo lo guarda localmente sobrescribiendo el anterior en `SharedPreferences`:

```kotlin
override fun onNewToken(token: String) {
    scope.launch { tokenRepository.saveToken(PushToken(token)) }
}
```

**Limitación importante:** La app **no** re-registra automáticamente el token nuevo en el backend. Si el token cambia mientras el usuario está con la sesión activa, el backend seguirá intentando enviar notificaciones al token anterior (ahora inválido) y el dispositivo dejará de recibirlas. Para que el servidor se entere del nuevo token, el usuario debe: (a) cerrar sesión y volver a iniciarla, (b) o esperar a que se dispare `registerDevice()` desde la UI (al tocar la tarjeta de ubicación en OverviewScreen).

**Código relevante:** `SifaFirebaseMessagingService.kt` — método `onNewToken()`.

---

#### Etapa 3: Registro del dispositivo en el backend

**¿Para qué sirve?** Para que el servidor sepa que este dispositivo (identificado por su token FCM) pertenece a un fiscalizador específico y pueda enviarle notificaciones dirigidas. Es un paso obligatorio: si el backend no tiene el token, no puede enviar pushes.

**¿Cuándo se dispara el registro?** Hay 3 momentos distintos:

1. **Después de un login exitoso con credenciales** — en `AuthViewModel.login()`, justo después de validar el rol `USER_APP` y guardar la sesión JWT. Es el punto principal de registro.

2. **Después de autenticación biométrica al iniciar la app** — en `AppNavigation.kt`, cuando el usuario ya tenía una sesión válida y se autentica con huella/rostro. Se asume que el token pudo haber cambiado desde el último login.

3. **Al refrescar la ubicación desde OverviewScreen** — cuando el usuario toca la tarjeta de GPS para recalibrar, también se llama a `registerDevice()`.

**¿Qué información se envía?** El `RegisterDeviceUseCase` arma el siguiente payload:

```kotlin
DeviceRegisterRequest(
    token = pushToken.value,       // Token FCM leído de SharedPreferences
    platform = "ANDROID",          // Fijo, identifica la plataforma
    appVersion = "1.0",           // BuildConfig.VERSION_NAME
    deviceId = "abc123...",        // ANDROID_ID (Settings.Secure.ANDROID_ID)
    deviceModel = "SM-G998B",      // Build.MODEL
    manufacturer = "samsung"       // Build.MANUFACTURER
)
```

**¿Cómo viaja esta información?** El `RegisterDeviceUseCase` llama a `deviceApi.registerDevice(request)` que hace un POST a `/core/api/v1/devices/register`. La petición se hace a través del `NetworkModule.retrofit`, que tiene configurado el `AuthInterceptor`. Esto significa que el request lleva automáticamente el header `Authorization: Bearer <JWT>`, autenticando al fiscalizador que está registrando el dispositivo.

**¿Qué pasa si el token FCM aún no está disponible?** El `RegisterDeviceUseCase` verifica si `tokenRepository.getToken()` devuelve null. Si es así, retorna `Result.failure` con el mensaje `"FCM token not available yet"`. Esto puede ocurrir si se intenta registrar el dispositivo antes de que `retrieveFcmToken()` haya terminado (especialmente en la primera ejecución con conexión lenta).

**¿Qué hace el backend con esto?** Almacena la asociación entre el usuario (identificado por el JWT), el token FCM y los metadatos del dispositivo. Cuando alguien desde el panel de administración quiera enviar una notificación a un fiscalizador específico, el backend usará este token para direccionar el mensaje FCM.

**Código relevante:**
- `AuthViewModel.kt` — método `registerDevice()`
- `RegisterDeviceUseCase.kt` — método `invoke()`
- `DeviceApi.kt` — interfaz Retrofit
- `DeviceRegisterRequest.kt` — DTO de la petición
- `AppInfoUtils.kt` — función `getDeviceInfo()` que obtiene ANDROID_ID, modelo y fabricante

---

#### Etapa 4: Recepción y visualización de la notificación

**¿Qué pasa cuando el backend envía un push?** El servidor de Firebase recibe el mensaje del backend y lo reenvía al dispositivo usando el token FCM como dirección. El sistema operativo Android despierta (si es necesario) el `SifaFirebaseMessagingService` y llama a su método `onMessageReceived()`.

**Extracción del título y cuerpo:**

El método intenta obtener el título y el cuerpo de la notificación de dos fuentes posibles, en orden de prioridad:

1. **`message.notification?.title` y `message.notification?.body`** — Esta información viene en el payload estructurado de FCM cuando el backend envía una "notification message" (mensaje con formato predefinido de FCM). Firebase se encarga de mostrar este tipo automáticamente si la app está en background, pero si la app está en foreground, delega en `onMessageReceived()`.

2. **`message.data["title"]` y `message.data["body"]`** — Como fallback, si el backend envió un "data message" (payload completamente personalizado, sin formato de notificación), se buscan estas claves dentro del mapa de datos.

Si alguna de las dos (title o body) es null, la notificación **no se muestra** y solo se registra en Logcat. Esto significa que los mensajes de datos puros (silenciosos) no gatillan ninguna acción visible.

**Construcción de la notificación Android:**

Una vez extraídos title y body, `NotificationHelper.showNotification()` construye la notificación:

- **Permiso Android 13+:** Si el dispositivo está en API 33 o superior y el usuario denegó el permiso `POST_NOTIFICATIONS`, la notificación se descarta silenciosamente. No hay crash, pero el usuario nunca se entera de que llegó.

- **PendingIntent:** Se crea un `Intent` que abre `MainActivity` con flags `NEW_TASK | CLEAR_TOP`. Cuando el usuario toca la notificación, la app se abre (o se trae al frente si ya estaba abierta). El pendingIntent se marca como `FLAG_IMMUTABLE` por seguridad (requerido en Android 12+).

- **Canal de notificación:** Usa el canal `"sifa_push"` (creado en la Etapa 1). Con prioridad `IMPORTANCE_HIGH`, la notificación aparece con sonido, en formato heads-up (ventana flotante breve) y en la parte superior de la pantalla de bloqueo.

- **AutoCancel:** La notificación se elimina automáticamente de la barra de estado cuando el usuario la toca.

- **ID de notificación:** Siempre es `1001`. Esto significa que si llegan 3 notificaciones push, solo se verá la última; las anteriores se sobrescriben porque tienen el mismo ID.

**¿Qué pasa con los datos adicionales del payload FCM?** Si el backend adjunta datos extra (por ejemplo, `tipo: "nueva_infraccion"`, `id_infraccion: "123"`), estos se ignoran completamente. La app solo extrae `title` y `body`; el resto del mapa `message.data` queda sin procesar.

**Código relevante:**
- `SifaFirebaseMessagingService.kt` — método `onMessageReceived()`
- `NotificationHelper.kt` — métodos `createNotificationChannel()` y `showNotification()`

---

### Diagrama del pipeline completo

```
┌─────────────────────────────────────────────────────────────────────┐
│                  1. INICIALIZACIÓN (App Startup)                     │
│                                                                      │
│  SifaApplication.onCreate()                                          │
│  ├── FirebaseApp.initializeApp(this)                                 │
│  │   └── Lee google-services.json, conecta con proyecto Firebase    │
│  ├── createNotificationChannel()                                     │
│  │   └── Crea canal "sifa_push" (IMPORTANCE_HIGH)                  │
│  └── retrieveFcmToken()                                              │
│      └── FirebaseMessaging.getInstance().token (Task<String>)       │
│          ├── ✔ Éxito → SharedPreferences (sifa_push / FCM_TOKEN)    │
│          └── ✗ Falla → Log de error, FCM reintenta automáticamente  │
│                                                                      │
│  Estado final: Token disponible en SharedPreferences                │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│              2. REFRESCO DE TOKEN (Runtime, si FCM rota)             │
│                                                                      │
│  SifaFirebaseMessagingService.onNewToken(nuevoToken)                 │
│  └── tokenRepository.saveToken(PushToken(nuevoToken))               │
│      └── SharedPreferences: se sobrescribe el token anterior        │
│                                                                      │
│  ⚠ El token nuevo queda guardado localmente, pero NO se envía       │
│    automáticamente al backend. El servidor sigue usando el token     │
│    antiguo (inválido) hasta que ocurra un nuevo registerDevice().   │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│              3. REGISTRO EN BACKEND (trigger externo)                │
│                                                                      │
│  ¿Qué lo dispara?                                                    │
│  ┌─ Login exitoso (AuthViewModel.login()) ──────────────────────┐   │
│  │ ✔ Se ejecuta justo después de validar rol USER_APP          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│  ┌─ Biometría al iniciar (AppNavigation, line 163) ────────────┐   │
│  │ ✔ Cuando el usuario se autentica con huella/rostro          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│  ┌─ Refrescar ubicación (OverviewScreen) ──────────────────────┐   │
│  │ ✔ Cada vez que el usuario toca la tarjeta de GPS           │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  AuthViewModel.registerDevice()                                      │
│  ├── Recolecta metadatos: appVersion, deviceId, model, manufacturer │
│  └── RegisterDeviceUseCase.invoke()                                 │
│      ├── 1. Lee token de SharedPreferences                          │
│      ├── 2. ¿Token nulo? → Falla con "FCM token not available yet" │
│      └── 3. deviceApi.registerDevice(DeviceRegisterRequest)        │
│            POST /core/api/v1/devices/register                       │
│            Body: { token, platform: "ANDROID", appVersion,         │
│                    deviceId, deviceModel, manufacturer }             │
│            Header: Authorization: Bearer <JWT>                      │
│            ┌─ Respuesta ────────────────────────────────┐           │
│            │ ✔ 2xx → "Device registered successfully"   │           │
│            │ ✗ Error → Log de error                     │           │
│            └─────────────────────────────────────────────┘           │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│             4. RECEPCIÓN Y VISUALIZACIÓN DE NOTIFICACIÓN             │
│                                                                      │
│  Llega un mensaje FCM desde el servidor                              │
│                                                                      │
│  SifaFirebaseMessagingService.onMessageReceived(message)             │
│  ├── ¿message.notification?.title existe?                           │
│  │   ├── ✔ → title = message.notification.title                    │
│  │   └── ✗ → title = message.data["title"] (fallback a datos)     │
│  ├── ¿message.notification?.body existe?                            │
│  │   ├── ✔ → body = message.notification.body                     │
│  │   └── ✗ → body = message.data["body"] (fallback a datos)       │
│  └── ¿title != null AND body != null?                                │
│      ├── ✔ → NotificationHelper.showNotification(context, t, b)   │
│      └── ✗ → Solo Log.d (datos silenciosos ignorados)             │
│                                                                      │
│  NotificationHelper.showNotification()                               │
│  ├── ¿API 33+ AND permiso POST_NOTIFICATIONS denegado?             │
│  │   └── ✔ → Descarta, no muestra nada (return)                   │
│  ├── Crea PendingIntent → MainActivity (NEW_TASK | CLEAR_TOP)     │
│  ├── Construye notificación:                                        │
│  │   - Canal: "sifa_push"                                          │
│  │   - Icono: ic_launcher_foreground                                │
│  │   - Prioridad: HIGH                                              │
│  │   - AutoCancel: true                                             │
│  └── NotificationManagerCompat.notify(1001, notificación)          │
│      ⚠ ID fijo 1001 → múltiples notificaciones se solapan         │
└─────────────────────────────────────────────────────────────────────┘
```

### Organización por capas (Clean Architecture)

| Capa | Archivos | Responsabilidad |
|------|----------|----------------|
| **Domain** | `PushToken.kt`, `PushTokenRepository.kt`, `RegisterDeviceUseCase.kt` | Value object que envuelve el token, interfaz del repositorio (contrato), y caso de uso que contiene la lógica de negocio para registrar el dispositivo. |
| **Data** | `DeviceRegisterRequest.kt` | DTO (Data Transfer Object) que modela el cuerpo de la petición HTTP para el registro de dispositivo. |
| **Infrastructure** | `SharedPreferencesPushTokenRepository.kt`, `SifaFirebaseMessagingService.kt`, `NotificationHelper.kt` | Implementaciones concretas: persistencia del token en SharedPreferences, servicio FCM que recibe mensajes y eventos de token, y helper que construye y muestra notificaciones Android. |
| **Core/Network** | `DeviceApi.kt`, `NetworkModule.kt` | Interfaz Retrofit que define el endpoint de registro, y configuración del cliente HTTP compartido con el `AuthInterceptor`. |
| **Presentation** | `AuthViewModel.kt`, `OverviewScreen.kt`, `AppNavigation.kt` | Puntos de entrada de la interfaz de usuario que disparan `registerDevice()` según las acciones del usuario (login, biometría, refresco de ubicación). |

### Observaciones importantes

1. **ID de notificación fijo (1001):** Múltiples notificaciones push entrantes se **sobrescriben** entre sí. Una implementación más robusta usaría un contador o `System.currentTimeMillis()` para IDs únicos.

2. **Sin re-registro automático en onNewToken:** Cuando FCM rota el token (por desinstalación/reinstalación, borrado de datos, etc.), `onNewToken()` guarda el token nuevo localmente pero **no lo re-registra en el backend**. El usuario tendría que hacer login nuevamente o disparar `registerDevice()` manualmente desde la UI para que el servidor reciba el token actualizado.

3. **Registro redundante en OverviewScreen:** Cada vez que el usuario toca la tarjeta de ubicación para refrescar coordenadas, se llama a `registerDevice()` innecesariamente.

4. **Notificaciones de solo datos ignoradas:** Si el payload FCM no contiene `title` ni `body` (mensajes silenciosos de datos), el servicio solo los registra en log pero no ejecuta ninguna acción.

5. **Múltiples instancias de PushTokenRepository:** Se crean instancias separadas en `SifaApplication`, `SifaFirebaseMessagingService` y `AuthViewModel`. Como todas apuntan al mismo archivo de `SharedPreferences`, los datos son consistentes, pero rompe el principio de singleton.

6. **Sin Hilt/Dagger/Koin:** No hay inyección de dependencias. Los objetos se instancian manualmente con `new()` en cada lugar donde se necesitan.
