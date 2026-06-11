# Refactorización para Testeabilidad

Este documento identifica componentes con alto acoplamiento que necesitan refactorización antes de poder escribir pruebas unitarias efectivas.

---

## Estado de la Refactorización

- ✅ **Resuelto**: el cambio ya fue implementado.
- ⬜ **Pendiente**:仍需 refactorización.

---

## 1. ProfileViewModel

**Archivo:** `app/src/main/java/com/sifa/sifa_go/viewmodel/ProfileViewModel.kt`

**Problema:** Creaba sus propias dependencias internamente:

```kotlin
private val sessionManager = SessionManager(application)

init {
    NetworkModule.init(application)
    loadUserProfile()
}
```

**Solución aplicada:** Inyección de dependencias por constructor:

```kotlin
class ProfileViewModel(
    application: Application,
    private val sessionRepository: SessionRepository = SessionManager(application),
    private val apiService: AuthApiService = AuthRetrofitClient.apiService
) : AndroidViewModel(application)
```

- ✅ `NetworkModule.init(application)` eliminado del `init` (ya se llama desde `SifaApplication.onCreate()`).
- ✅ `sessionManager` reemplazado por `sessionRepository: SessionRepository`.
- ✅ `AuthRetrofitClient.apiService` reemplazado por `apiService: AuthApiService` inyectado.
- ✅ Los tests pueden pasar mocks de `SessionRepository` y `AuthApiService`.

---

## 2. SessionManager

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/utils/SessionManager.kt`

**Problema:** Sin interfaz, imposible de mockear sin MockK.

**Solución aplicada:** Extracción de interfaz `SessionRepository`:

```kotlin
interface SessionRepository {
    fun saveSession(...)
    fun getToken(): String?
    fun getRefreshToken(): String?
    fun getTokenExpiry(): Long
    fun getTokenIat(): Long
    fun getUsername(): String?
    fun getRoles(): List<String>
    fun hasUserAppRole(): Boolean
    fun hasValidSession(): Boolean
    fun logout()
}
```

- ✅ Interfaz `SessionRepository` creada en `domain/repository/SessionRepository.kt`.
- ✅ `SessionManager` ahora implementa `SessionRepository`.
- ✅ Los ViewModel que usen `SessionRepository` en lugar de `SessionManager` pueden recibir mocks.
- ⬜ **Pendiente:** Refactorizar los demás ViewModel (`AuthViewModel`, `ChangePasswordViewModel`, `SifaViewModel`, `CoreViewModel`) para que usen la interfaz en lugar de la clase concreta.

---

## 3. NetworkModule

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/network/NetworkModule.kt`

**Problema:** Objeto singleton con inicialización perezosa. No se puede reemplazar en tests.

**Solución propuesta:** Convertir en clase con interfaz o permitir sobreescribir la instancia de Retrofit desde tests.

- ⬜ **Pendiente:** Evaluar si es necesario refactorizar. Con la inyección de `apiService` en los ViewModel, `NetworkModule` queda desacoplado de los tests.

---

## 4. AuthInterceptor

**Archivo:** `app/src/main/java/com/sifa/sifa_go/core/network/AuthInterceptor.kt`

**Problema:** Alto acoplamiento con `SessionManager`, `OkHttpClient`, `Retrofit` y corutinas con `runBlocking`.

**Solución propuesta:** Extraer la lógica de refresh a un servicio independiente inyectable.

- ⬜ **Pendiente.** Baja prioridad.

---

## 5. Retrofit Clients

**Archivos:** `core/network/AuthApi.kt`, `CoreApi.kt`, `DeviceApi.kt`, `PlateDetectorApi.kt`

**Problema:** Objetos `object` que crean el service de Retrofit con `lazy`. No se pueden reemplazar en tests.

**Solución aplicada:** Los 4 Retrofit clients ahora permiten sobreescribir el `apiService` desde tests:

```kotlin
object AuthRetrofitClient {
    private var _apiService: AuthApiService? = null

    val apiService: AuthApiService
        get() = _apiService ?: NetworkModule.retrofit.create(AuthApiService::class.java)

    fun setApiService(service: AuthApiService) { _apiService = service }
    fun resetApiService() { _apiService = null }
}
```

- ✅ `AuthRetrofitClient.apiService` ahora es sobreescribible con `setApiService()`.
- ✅ `CoreRetrofitClient.apiService` sobreescribible con `setApiService()`.
- ✅ `DeviceRetrofitClient.apiService` sobreescribible con `setApiService()`.
- ✅ `RetrofitClient.apiService` (Plate Detector) sobreescribible con `setApiService()`.

---

## Prioridad de Refactorización

| Componente | Impacto en Tests | Esfuerzo | Prioridad | Estado |
|---|---|---|---|---|
| ProfileViewModel | Alto | Bajo | Alta | ✅ |
| SessionManager | Alto | Medio | Alta | ✅ (interfaz) |
| NetworkModule | Alto | Medio | Media | ⬜ |
| AuthInterceptor | Medio | Alto | Baja | ⬜ |
| Retrofit Clients | Medio | Bajo | Alta | ✅ |
