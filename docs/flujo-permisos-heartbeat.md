# Flujo de permisos y heartbeat

## Problema original

Los permisos de cámara (`CAMERA`) y ubicación (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) solo se solicitaban en `LiveScannerScreen`. El usuario debía navegar manualmente a la vista de cámara para que apareciera el diálogo de permisos.

El `PresenceViewModel.startHeartbeatEngine()` iniciaba inmediatamente al entrar a `main_app`, pero sin permiso de ubicación, `LocationHelper.getLocation()` fallaba silenciosamente (`@SuppressLint("MissingPermission")`) y el heartbeat nunca enviaba datos.

## Solución implementada

### 1. Solicitud de permisos al entrar a `main_app`

**Archivo:** `app/src/main/java/com/sifa/sifa_go/ui/navigation/AppNavigation.kt`

En el composable `main_app` (ruta raíz que se muestra después del login/biometría) se agregó:

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

- Los permisos se solicitan justo después de la autenticación (biométrica o login), antes de que el usuario interactúe con cualquier vista.
- Usa `@file:OptIn(ExperimentalPermissionsApi::class)` para la API experimental de Accompanist.

### 2. Heartbeat inmediato al conceder permisos

```kotlin
LaunchedEffect(permissionsState.allPermissionsGranted) {
    if (permissionsState.allPermissionsGranted) {
        presenceViewModel.sendManualHeartbeat(context, sessionManager)
    }
}
```

apenas se otorgan todos los permisos, se dispara un heartbeat manual. Esto asegura que el servidor reciba la ubicación lo antes posible, sin esperar al siguiente ciclo de 3 minutos.

### 3. `LiveScannerScreen` — solo respaldo visual

**Archivo:** `app/src/main/java/com/sifa/sifa_go/ui/views/LiveScannerScreen.kt`

Se eliminó el `LaunchedEffect(Unit)` que solicitaba permisos automáticamente:

```kotlin
// Eliminado:
LaunchedEffect(Unit) {
    permissionState.launchMultiplePermissionRequest()
}
```

Se mantiene el `rememberMultiplePermissionsState` y la tarjeta de permisos como mecanismo de respaldo. El botón ahora maneja dos casos:

```kotlin
Button(onClick = {
    if (permissionState.shouldShowRationale) {
        // Usuario NO marcó "No volver a preguntar" → solicitar de nuevo
        permissionState.launchMultiplePermissionRequest()
    } else {
        // Permiso denegado permanentemente → abrir Configuración
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}) {
    Text(if (permissionState.shouldShowRationale) "Conceder permiso" else "Ir a configuración")
}
```

- Si `shouldShowRationale` es `true` → el sistema aún puede mostrar el diálogo → se solicita normalmente.
- Si `shouldShowRationale` es `false` y los permisos no están concedidos → al menos un permiso fue denegado con "No volver a preguntar" → se abre la pantalla de configuración de la app para que el usuario lo active manualmente.
- Al regresar de configuración, el `rememberMultiplePermissionsState` se actualiza con el nuevo estado y la UI reacciona automáticamente.

### 4. Sin cambios en `PresenceViewModel`

El ViewModel se mantiene limpio sin conocer el estado de permisos de Android. La desición de disparar el heartbeat está en la capa de UI (Composable), respetando MVVM.

## Flujo resultante

```
Login/Biometría exitoso
        │
        ▼
  main_app composable
        │
        ├─ 1. ¿Permisos ya concedidos?
        │      ├─ No → Lanzar diálogo de permisos
        │      └─ Sí  → Saltar diálogo
        │
        ├─ 2. StartHeartbeatEngine (ciclo cada 3 min)
        │
        └─ 3. ¿Permisos concedidos ahora?
               ├─ Sí → SendManualHeartbeat (inmediato)
               └─ No → Esperar al siguiente ciclo
```

Si el usuario deniega en el diálogo inicial, el heartbeat fallará hasta que:
- Otorgue permisos desde la tarjeta en `LiveScannerScreen` y el ciclo de 3 minutos lo envíe.
- Reinicie la app y conceda en el diálogo inicial.
- Si el permiso fue denegado permanentemente ("No volver a preguntar"), deberá ir a Configuración desde el botón de la tarjeta.

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `AppNavigation.kt` | Se agregó `rememberMultiplePermissionsState` + `LaunchedEffect` para solicitar permisos y trigger heartbeat al concederse |
| `LiveScannerScreen.kt` | Se eliminó el `LaunchedEffect` que auto-solicitaba permisos; se mejoró el botón de la tarjeta con lógica `shouldShowRationale` para abrir Configuración si el permiso fue denegado permanentemente |
