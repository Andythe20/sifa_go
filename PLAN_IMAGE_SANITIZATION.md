# Plan de Implementación: Image Sanitization Pipeline

## Problema

Las fotos capturadas con CameraX incluyen metadatos EXIF (GPS, modelo de cámara,
fecha, etc.) que se envían al backend, exponiendo información sensible.

## Solución

Pipeline de procesamiento de imágenes que elimina metadatos antes de enviar
al backend, siguiendo principios SOLID y patrones de diseño.

## Arquitectura

```
core/image/
├── ImageProcessor.kt              # Interfaz funcional (SRP + DIP)
├── ExifMetadataStripper.kt        # Strategy: elimina metadatos EXIF
├── OrientationCorrector.kt        # Strategy: corrige rotación
├── ImageCompressor.kt             # Strategy: comprime JPEG
├── ImageProcessingPipeline.kt     # Pipeline: encadena procesadores
├── ImageSanitizer.kt              # Facade: pipeline preconfigurado
└── FileExtensions.kt              # Extension functions para Retrofit
```

## Principios SOLID

| Principio | Aplicación |
|-----------|-----------|
| **S**ingle Responsibility | Cada clase hace UNA cosa |
| **O**pen/Closed | Nuevos procesadores se agregan sin modificar existentes |
| **L**iskov Substitution | Todos implementan `ImageProcessor`, son intercambiables |
| **I**nterface Segregation | Interfaz con un solo método: `process(input: File): File` |
| **D**ependency Inversion | Consumidores dependen de `ImageProcessor`, no de concretos |

## Patrones de Diseño

| Patrón | Dónde | Propósito |
|--------|-------|-----------|
| **Strategy** | `ImageProcessor` | Algoritmos encapsulados e intercambiables |
| **Pipeline** | `ImageProcessingPipeline` | Componer procesadores en secuencia |
| **Facade** | `ImageSanitizer` | Interfaz simple que oculta la complejidad |
| **Extension Functions** | `FileExtensions.kt` | Integración limpia con Retrofit |

## Flujo de datos

```
File original
    → ExifMetadataStripper  (decodifica a Bitmap, re-codifica sin EXIF)
    → OrientationCorrector   (corrige rotación si es necesario)
    → ImageCompressor        (comprime JPEG calidad 80)
    → File limpio
```

## Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `core/utils/ImageUtils.kt` | Deprecado, delega internamente al pipeline |
| `viewmodel/SifaViewModel.kt` | Usa `File.toCleanMultipartPart()` |
| `viewmodel/CoreViewModel.kt` | Usa `File.toCleanMultipartPart()` |
| `ui/views/LiveScannerScreen.kt` | Usa `ImageSanitizer.sanitize()` |
| `ui/navigation/AppNavigation.kt` | Usa `ImageSanitizer.sanitize()` |

## Uso

```kotlin
// Sanitizar un archivo
val cleanFile = ImageSanitizer.sanitize(photoFile)

// Crear MultipartBody.Part limpio para Retrofit
val part = file.toCleanMultipartPart("fotos")

// Crear múltiples parts desde una lista
val parts = files.toCleanMultipartParts("fotos")

// Pipeline personalizado
val customPipeline = ImageProcessingPipeline(
    ExifMetadataStripper(),
    ImageCompressor(quality = 60)
)
val result = customPipeline.process(file)
```
