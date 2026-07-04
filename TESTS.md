# Tests

Este documento describe los tests del proyecto, qué cubren y cómo ejecutarlos.

## Stack de Testing

- **Framework:** JUnit 4
- **Mocking:** MockK 1.13.16
- **Tests de UI Compose:** `createComposeRule` + `ComposeTestRule`
- **Corrutinas:** `kotlinx-coroutines-test`

---

## Tests Unitarios

### `PasswordValidatorTest`

**Archivo:** `app/src/test/java/com/sifa/sifa_go/core/utils/PasswordValidatorTest.kt`

**Cobertura:** `PasswordValidator` (`core/utils/PasswordValidator.kt`)

Valida todas las reglas de contraseña implementadas en `PasswordValidator`:

| Test | Qué verifica |
|---|---|
| `validate returns 4 requirements` | Que la lista tenga exactamente 4 requirements |
| `validate detects short password` | Que detecte menos de 8 caracteres |
| `validate accepts password with exactly 8 characters` | Frontera: 8 caracteres es válido |
| `validate detects missing uppercase` | Que detecte ausencia de mayúscula |
| `validate accepts password with uppercase` | Que acepte mayúscula presente |
| `validate detects missing lowercase` | Que detecte ausencia de minúscula |
| `validate accepts password with lowercase` | Que acepte minúscula presente |
| `validate detects missing digit` | Que detecte ausencia de dígito |
| `validate accepts password with digit` | Que acepte dígito presente |
| `validate fails for empty password` | Que vacío falle en todas las reglas |
| `validate fails for password with only numbers` | Solo números: length y digit OK, mayúscula y minúscula fail |
| `validate accepts valid password` | Contraseña que cumple todos los requisitos |
| `isFullyValid returns true for valid password` | `isFullyValid` con contraseña válida |
| `isFullyValid returns false for short password` | `isFullyValid` con contraseña corta |
| `isFullyValid returns false for missing uppercase` | `isFullyValid` sin mayúscula |
| `isFullyValid returns false for missing digit` | `isFullyValid` sin número |
| `isFullyValid returns false for empty password` | `isFullyValid` con vacío |
| `isFullyValid returns false for missing lowercase` | `isFullyValid` sin minúscula |
| `password with special characters passes` | Caracteres especiales no interfieren con reglas |
| `password with only whitespace fails` | Espacios en blanco: length OK, resto fail |

**Ejecución:**
```bash
./gradlew test --tests "*PasswordValidatorTest"
```

---

### `DataModelTest`

**Archivo:** `app/src/test/java/com/sifa/sifa_go/data/model/DataModelTest.kt`

**Cobertura:** Modelos de datos en `data/model/AuthModels.kt`

Verifica construcción y comportamiento básico de los modelos de datos:

| Test | Qué verifica |
|---|---|
| `UserResponse with all fields` | Construcción con todos los campos poblados |
| `UserResponse with nullable fields null` | Construcción con campos null |
| `UserResponse copy and modify` | `copy()` con modificación parcial |
| `LoginRequest construction` | Construcción de `LoginRequest` |
| `LoginResponse all fields` | Construcción de `LoginResponse` |
| `LoginResponse toString does not expose sensitive data` | Que `toString` muestre los campos (data class) |
| `ChangePasswordRequest construction` | Construcción de `ChangePasswordRequest` |
| `ChangePasswordResponse construction` | Construcción de `ChangePasswordResponse` |
| `LoginResult Success construction` | Construcción de `LoginResult.Success` |
| `LoginResult Error construction` | Construcción de `LoginResult.Error` |

**Ejecución:**
```bash
./gradlew test --tests "*DataModelTest"
```

---

## Cómo Ejecutar Todos los Tests

### Tests unitarios (JVM, no requieren dispositivo/emulador)
```bash
./gradlew test
```

### Tests instrumentados (requieren emulador o dispositivo físico)
```bash
./gradlew connectedAndroidTest
```

### Tests unitarios específicos por clase
```bash
./gradlew test --tests "com.sifa.sifa_go.core.utils.PasswordValidatorTest"
```

---

## Coverage Actual

| Clase | Tipo | Cobertura | Estado |
|---|---|---|---|
| `PasswordValidator` | Unitaria | Completa (20 tests) | ✅ |
| `AuthModels` (data models) | Unitaria | Completa (10 tests) | ✅ |
| `ProfileViewModel` | Unitaria | Pendiente (ver REFACTOR.md) | ⏳ |
| `ProfileScreen` | UI Compose | Pendiente (ver REFACTOR.md) | ⏳ |
| `SessionManager` | Unitaria | Pendiente (ver REFACTOR.md) | ⏳ |

> **Nota:** `ProfileViewModel`, `ProfileScreen` y `SessionManager` no tienen tests por su alto acoplamiento. Consulta `REFACTOR.md` para ver los cambios necesarios.
