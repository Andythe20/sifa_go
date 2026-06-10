# Feature: Cambio de Contraseña desde App Móvil

## Resumen
Implementación del flujo completo de cambio de contraseña desde la pantalla de perfil de la app móvil SIFA GO. Incluye un nuevo endpoint en el microservicio `auth` y toda la lógica frontend (UI, ViewModel, validaciones, navegación).

---

## Cambios Realizados

### 1. Microservicio `auth`

#### Nuevos archivos:
| Archivo | Descripción |
|---|---|
| `src/main/java/com/evecta/auth/dto/auth/ChangePasswordRequestDTO.java` | DTO con `oldPassword` (requerida) y `newPassword` (requiere min 8 chars, 1 mayúscula, 1 minúscula, 1 número) |

#### Archivos modificados:
| Archivo | Cambio |
|---|---|
| `controller/AuthController.java` | Nuevo endpoint `POST /auth/api/v1/change-password` que recibe `Authentication` (del token JWT) + `ChangePasswordRequestDTO`. Import agregado: `Authentication`. |
| `service/AuthService.java` | Nuevo método `changePassword(email, request)`: busca usuario por email, verifica contraseña actual con BCrypt, valida que la nueva sea diferente, guarda nueva contraseña hasheada, revoca todos los tokens del usuario y registra auditoría. |
| `config/SecurityConfig.java` | Sin cambios. El endpoint cae bajo `.anyRequest().authenticated()`, accesible para cualquier usuario autenticado. |

#### Endpoint:
```
POST /auth/api/v1/change-password
Content-Type: application/json
Authorization: Bearer <token>

{
  "oldPassword": "Actual123",
  "newPassword": "NuevaClave456"
}

Response 200: { "message": "Contraseña cambiada con éxito." }
Response 400: { "error": "La contraseña actual no es correcta" }
```

---

### 2. App Móvil `sifa_go`

#### Nuevos archivos:
| Archivo | Descripción |
|---|---|
| `app/src/main/java/com/sifa/sifa_go/core/utils/PasswordValidator.kt` | Objeto utilitario reutilizable con método `validate(password)` que retorna `List<Requirement>` y `isFullyValid(password)`. Cada `Requirement` tiene `label` e `isValid`. |
| `app/src/main/java/com/sifa/sifa_go/ui/components/PasswordRequirementsIndicator.kt` | Composable reutilizable que muestra los requisitos de contraseña con colores interactivos (verde si cumple, gris si no) y animaciones suaves. Usa `PasswordValidator`. |
| `app/src/main/java/com/sifa/sifa_go/viewmodel/ChangePasswordViewModel.kt` | ViewModel con estado para los 3 campos (old, new, confirm), validaciones en tiempo real, llamada a API, manejo de errores y `sessionManager.logout()` post-éxito. |
| `app/src/main/java/com/sifa/sifa_go/ui/views/ChangePasswordScreen.kt` | Pantalla completa con: campos de contraseña con toggle visibilidad, indicador de requisitos interactivo, checkbox de aceptación de cierre de sesión, manejo de `BackHandler` (bloquea botón atrás), diálogo de éxito y navegación a login. |

#### Archivos modificados:
| Archivo | Cambio |
|---|---|
| `data/model/AuthModels.kt` | Se agregaron `ChangePasswordRequest(oldPassword, newPassword)` y `ChangePasswordResponse(message)`. |
| `core/network/AuthApi.kt` | Nuevo método `changePassword(request)` en `AuthApiService`. |
| `ui/views/ProfileScreen.kt` | Nuevo parámetro `onChangePassword`. Nuevo botón "Cambiar Contraseña" (OutlinedButton con ícono de candado) entre la info del perfil y el botón de cerrar sesión. |
| `ui/navigation/AppNavigation.kt` | Nueva ruta `"change_password"` en el `NavHost` de tabs (dentro de `MainLayout`), heredando top bar y estructura visual. `onChangePassword` se maneja internamente con `tabsNavController`. Al éxito: `onLogout` → heartbeat stop + `sessionManager.logout()` + navegar a `"login"` con `popUpTo(0)`. |

---

## Flujo de Usuario

1. Usuario va a **Perfil** (tab navigation)
2. Presiona **"Cambiar Contraseña"**
3. Se navega dentro del mismo `MainLayout` (con top bar y bottom nav, back vuelve al perfil)
4. Usuario ingresa:
   - Contraseña actual
   - Nueva contraseña (con indicador interactivo de requisitos: verde/gris)
   - Repetir nueva contraseña
   - Checkbox "Entiendo que se cerrará mi sesión"
5. Al presionar "Cambiar contraseña":
   - Se valida localmente (contraseña actual no vacía, nueva cumple requisitos, nuevas coinciden, diferentes)
   - Se llama a `POST /auth/api/v1/change-password`
   - Si éxito: se limpia sesión local, se muestra diálogo de éxito
   - Al cerrar diálogo: se redirige a Login (stack completamente limpio)
   - Si error: se muestra mensaje de error en tarjeta con animación

## Arquitectura y Patrones

- **MVVM**: `ChangePasswordScreen` (View) → `ChangePasswordViewModel` → `AuthApiService` (Model)
- **DRY/SRP**: `PasswordValidator` y `PasswordRequirementsIndicator` son reutilizables
- **Inmutabilidad defensiva**: propiedades del ViewModel con `private set`
- **Validación en dos capas**: cliente (Kotlin) + servidor (Java DTO con Bean Validation)
- **Navegación limpia**: `popUpTo` evita back navigation no deseada

## Requisitos de Contraseña (del DTO `UserCreateDTO.java`)
- Mínimo **8 caracteres**
- Al menos **1 mayúscula** (A-Z)
- Al menos **1 minúscula** (a-z)
- Al menos **1 número** (0-9)
