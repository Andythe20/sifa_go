# Recuperación de Contraseña — SIFA GO (App Móvil)

## 1. Resumen

Implementar flujo de recuperación de contraseña por olvido en la app móvil SIFA GO,
reutilizando la lógica existente del backend de autenticación y siguiendo el mismo
comportamiento que el web dashboard.

---

## 2. Backend (Auth — sin cambios)

El microservicio `auth` ya expone dos endpoints **públicos** (sin autenticación):

| Endpoint | Método | Cuerpo | Respuesta |
|---|---|---|---|
| `/auth/api/v1/recovery/request` | POST | `{ "email": "..." }` | `{ "message": "..." }` |
| `/auth/api/v1/recovery/reset` | POST | `{ "email", "code", "newPassword" }` | `{ "message": "..." }` |

**Reglas de negocio del backend:**
- El código es numérico de 6 dígitos, enviado por SMTP al correo del usuario.
- El código expira a los **15 minutos** de generado.
- Se permiten **máximo 3 intentos** de código incorrecto (se invalida al superarlos).
- Al restablecer la contraseña exitosamente, se revocan **todos los tokens JWT** del usuario.
- Si el usuario no existe o está inactivo, se retorna error.

---

## 3. Flujo de la App Móvil (3 pasos)

### Paso 1 — Solicitar código de recuperación
- Usuario ingresa su correo electrónico.
- Botón "Enviar código de recuperación".
- Llamada a `POST /auth/api/v1/recovery/request`.
- Éxito → avanza al Paso 2.
- Error → muestra mensaje de error.

### Paso 2 — Ingresar código y nueva contraseña
- Campos:
  - Código de verificación (6 dígitos, solo numérico).
  - Nueva contraseña (con validador visual de requisitos).
  - Confirmar contraseña.
- Validaciones cliente:
  - Código exactamente 6 dígitos.
  - Contraseña cumple `PasswordValidator.isFullyValid()` (8+ chars, mayúscula, minúscula, dígito).
  - Contraseñas coinciden.
- Botón "Restablecer contraseña".
- Llamada a `POST /auth/api/v1/recovery/reset`.
- Éxito → avanza al Paso 3.
- Error → muestra mensaje de error (incluye intentos restantes si el código es incorrecto).

### Paso 3 — Confirmación de éxito
- Mensaje: "Contraseña restablecida con éxito".
- Botón "Ir al inicio de sesión".
- Navega a la pantalla de login.

---

## 4. Componentes a Modificar / Crear

| Archivo | Acción |
|---|---|
| `data/model/AuthModels.kt` | Agregar `PasswordRecoveryRequest`, `PasswordResetRequest` |
| `core/network/AuthApi.kt` | Agregar `requestRecovery()`, `resetPassword()` |
| `viewmodel/RecoveryViewModel.kt` | **Nuevo** — estado y lógica del flujo de recuperación |
| `ui/views/RecoveryScreen.kt` | **Nuevo** — UI de los 3 pasos |
| `ui/views/LoginScreen.kt` | Agregar enlace "Olvidé mi contraseña" |
| `ui/navigation/AppNavigation.kt` | Agregar ruta `recovery` y conectar callbacks |

---

## 5. Patrones y Principios

- **ViewModel + Screen**: Separación de responsabilidades (capa de presentación vs. UI).
- **`PasswordValidator` reutilizado**: No se duplica la lógica de validación de contraseña
  (mismo objeto usado en `ChangePasswordViewModel` y `RecoveryViewModel`).
- **`NetworkErrorHandler`**: Manejo centralizado de errores HTTP y de red.
- **State management con `mutableStateOf`**: Mismo patrón que el resto de la app
  (`AuthViewModel`, `ChangePasswordViewModel`).
- **Flujo en 3 pasos**: Misma experiencia de usuario que el web dashboard.

---

## 6. Diseño UI (referencia)

- Colores institucionales: `PrimaryBlue`, `SecondaryBlue`, `ErrorRed`, etc.
- Mismos componentes Material3 que el resto de la app (`OutlinedTextField`, `Button`, `Card`).
- `PasswordRequirementsIndicator` reutilizado para mostrar requisitos de contraseña.
- Enlace "Volver atrás" en cada paso para regresar al paso anterior o al login.
- Iconos: `Email`, `Lock`, `CheckCircle`, `ArrowBack`.

---

## 7. Dependencias

No se requieren nuevas dependencias. Se usan las ya existentes:
- Retrofit + Gson (llamadas HTTP)
- Compose Material3 (UI)
- Navigation Compose (rutas)
- `PasswordValidator` (core/utils)
- `NetworkErrorHandler` (exception)
