# Refactorización para Testeabilidad

Este documento identifica componentes con alto acoplamiento que necesitan refactorización antes de poder escribir pruebas unitarias efectivas.

## 1. ProfileViewModel

**Archivo:** `app/src/main/java/com/sifa/sifa_go/viewmodel/ProfileViewModel.kt`

**Problema:** Crea sus propias dependencias internamente:

```kotlin
private val sessionManager = SessionManager(application)

init {
    NetworkModule.init(application)
    loadUserProfile()
}
```

Esto imposibilita inyectar mocks/fakes de `SessionManager`, `AuthApiService` y `NetworkModule`.

**Solución propuesta:** Inyección de dependencias por constructor:

```kotlin
class ProfileViewModel(
    application: Application,
    private val sessionManager: SessionManager = SessionManager(application),
    private val apiService: AuthApiService = AuthRetrofitClient.apiService
) : AndroidViewModel(application) {
    // ...
}
```

Esto permite pasar dependencias mockeadas desde el test sin modificar el comportamiento por defecto.

---

## 2. SessionManager

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/utils/SessionManager.kt`

**Problema:** Depende directamente de `SharedPreferences` de Android, creada con `context.getSharedPreferences(...)`.

**Solución propuesta:** Extraer una interfaz `SessionRepository` y crear una implementación con `SharedPreferences`. La interfaz se puede mockear fácilmente.

---

## 3. NetworkModule

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/network/NetworkModule.kt`

**Problema:** Objeto singleton con inicialización perezosa que depende de `Context` y de `SessionManager`. No se puede reemplazar en tests.

**Solución propuesta:** Convertir en clase con interfaz o permitir sobreescribir la instancia de Retrofit desde tests.

---

## 4. AuthInterceptor

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/network/AuthInterceptor.kt`

**Problema:** Alto acoplamiento con `SessionManager`, `OkHttpClient`, `Retrofit` y corutinas con `runBlocking`. Difícil de probar en aislamiento.

**Solución propuesta:** Extraer la lógica de refresh a un servicio independiente inyectable.

---

## 5. AuthRetrofitClient / CoreRetrofitClient / DeviceRetrofitClient

**Archivos:** `core/network/AuthApi.kt`, `CoreApi.kt`, `DeviceApi.kt`

**Problema:** Objetos `object` que crean el service de Retrofit con `lazy`. No se pueden reemplazar en tests.

**Solución propuesta:** Usar una interfaz `ApiServiceProvider` que pueda tener una implementación fake para tests.

---

## Prioridad de Refactorización

| Componente | Impacto en Tests | Esfuerzo | Prioridad |
|---|---|---|---|
| ProfileViewModel | Alto | Bajo | Alta |
| SessionManager | Alto | Medio | Alta |
| NetworkModule | Alto | Medio | Media |
| AuthInterceptor | Medio | Alto | Baja |
| Retrofit Clients | Medio | Bajo | Alta |
