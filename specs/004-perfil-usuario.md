# 004 — Perfil de usuario

- **Estado:** Implemented (pendiente de QA en dispositivo y de desplegar reglas, Storage y `deleteAccount`)
  - Revisada contra `master` después del PR #2 (auth con código OTP, overlay "Sin conexión" y Cloud Functions).
  - Las [desviaciones de Figma](#desviaciones-de-figma) quedaron decididas con el equipo.
  - Antes de probar en dispositivo, el Owner debe crear el bucket de Storage y desplegar: `firebase deploy --only firestore:rules,storage,functions` (spec 001, pasos 8 y 9).
- **Paquetes:** `feature.profile`, `feature.settings`, `core.domain`, `core.model`, `core.data`, `core.ui`, `core.designsystem`, `navigation`, y el backend en `functions/`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-)
  - Perfil (ver): `PERFIL` (99:723)
  - Datos Personales (editar): `CONFIGURACION` (3254:1023). Tarjeta "Datos Dueño" con lápiz y botón "Actualizar información".
  - Configuración (punto de entrada): `CONFIGURACION` (3452:1018) y la versión completa (144:621), sección "Mi cuenta".
  - Diálogo de confirmación para eliminar: se reutiliza el de mascotas, `EDIT PERRO 2` (3050:1515), "¿Seguro deseas eliminarlo?".
  - Solo como referencia (fuera de alcance): `Seguridad`, cambio de contraseña (3263:1086).

## Objetivo

Que el dueño de mascotas pueda ver su perfil, completar y editar sus datos personales (nombre, correo, teléfono, dirección y fecha de nacimiento), cambiar su foto y eliminar su cuenta. Los datos se guardan en Firestore (`users/{uid}`) y la foto en Firebase Storage.

Hoy `feature/profile/ProfileScreen.kt` es una maqueta con datos fijos, sin ViewModel, y `users/{uid}` solo se crea (`createIfMissing`, spec 002).

## Historias de usuario

- Como usuario, quiero ver mis datos en la pestaña Perfil.
- Como usuario, quiero editar mis datos desde "Editar información" (Perfil) o desde Configuración → Datos personales.
- Como usuario, quiero cambiar mi correo de forma segura, confirmándolo desde el correo nuevo.
- Como usuario, quiero cambiar mi foto de perfil.
- Como usuario, quiero eliminar mi cuenta y todos mis datos desde Configuración.

## Flujo de navegación

```
Perfil (pestaña) ──"Editar información"──► Datos Personales ──"<"──► atrás
Configuración ──"Datos personales"──────► Datos Personales
Configuración ──"Eliminar cuenta"──► Diálogo de confirmación ──► Reautenticación ──► (sesión cerrada) Presentación
Perfil ──tocar la foto──► Photo Picker del sistema ──► subida ──► foto actualizada
```

- Datos Personales vive en `feature.profile`, que expone `NavGraphBuilder.personalDataScreen(onBack)`.
- Configuración no importa `feature.profile`: recibe la lambda `onNavigateToPersonalData`, y `MainNavHost` (`navigation/HuellitasApp.kt`) la resuelve.
- Eliminar la cuenta **no navega**. Al terminar, la app cierra la sesión local, `SessionState` pasa a `SignedOut` y se reemplaza el NavHost, como en el logout de la spec 002.

## Operaciones (CRUD)

| Operación | Dónde | Comportamiento |
|-----------|-------|----------------|
| **Crear** | Login y registro (ya existe) | `createIfMissing` no cambia. Los campos nuevos nacen ausentes. |
| **Leer** | Perfil y Datos Personales | Se escucha `users/{uid}` en tiempo real (snapshot listener → `Flow`). Funciona offline con la caché de Firestore. Los campos opcionales vacíos muestran "Sin registrar" en `LightGrayText`. Si el documento todavía no existe (el login lo está creando), se muestran el nombre, el correo y la foto de la sesión. |
| **Actualizar datos** | Datos Personales | Ver [Datos Personales](#datos-personales). |
| **Actualizar foto** | Perfil, al tocar el avatar | Ver [Foto de perfil](#foto-de-perfil). |
| **Actualizar correo** | Datos Personales | Ver [Cambio de correo](#cambio-de-correo). |
| **Eliminar** | Configuración → "Eliminar cuenta" | Ver [Eliminar cuenta](#eliminar-cuenta). La hace una Cloud Function. |

### Datos Personales

- Por defecto se muestra en modo lectura. El lápiz de la tarjeta "Datos Dueño" activa la edición; si se pulsa otra vez durante la edición, descarta los cambios.
- "Actualizar información" se ve gris (`ButtonIncomplete`) mientras no hay cambios y verde (`GreenAction`) cuando los hay. Igual que en la spec 002, sigue siendo pulsable en gris y, con una petición en curso, muestra un indicador y no acepta otro clic.
- Pulsar "Actualizar información" en modo lectura abre la edición, igual que el lápiz.
- Escribir de nuevo el valor guardado (p. ej. el mismo teléfono con espacios) no cuenta como cambio.
- Si el perfil cambia en otro dispositivo durante la edición, no se pisan los campos que se están editando.
- Al pulsarlo se valida y se guardan **solo los campos modificados**, más `updatedAt`.
- Si se guarda bien, la pantalla sale del modo edición y muestra el Snackbar "Datos actualizados".
- Al salir con "<" en modo edición, los cambios sin guardar se descartan.

### Foto de perfil

- Al tocar el avatar en Perfil se abre `PickVisualMedia` (solo imágenes; no pide permisos). Si el usuario cancela, no pasa nada.
- La imagen se comprime a JPEG de 1024 px como máximo por lado, se sube a Storage en `users/{uid}/profile.jpg` y su URL de descarga se guarda en `photoUrl`.
- Mientras sube, el avatar muestra un indicador de progreso.
- La foto se carga con Coil. Si no hay foto o falla la carga, se muestra `img_profile_placeholder`.

### Cambio de correo

- Solo cuentas de correo. En cuentas Google, el correo es de solo lectura (pertenece a la cuenta de Google).
- Si el correo cambió al guardar, se llama a `verifyBeforeUpdateEmail(nuevo)` y se muestra "Te enviamos un enlace a <correo> para confirmar el cambio". El resto de cambios se guarda normalmente.
- El correo de verificación sale en español: la app ya llama a `setLanguageCode("es")` y las plantillas de Auth están en español (spec 001, paso 3).
- Si Firebase pide un inicio de sesión reciente, primero se reautentica (ver abajo) y se reintenta.
- Mientras el usuario no abra el enlace, el correo mostrado sigue siendo el anterior. Firestore no cambia hasta que Auth confirme el correo nuevo.
- Cambiar el correo es un cambio sensible: Firebase invalida las sesiones abiertas, así que la app vuelve a Presentación cuando se le vence el token. Al iniciar sesión con el correo nuevo, la copia de Firestore se sincroniza (ver [Contratos](#contratos)).
- `verifyBeforeUpdateEmail` funciona con la protección contra enumeración de correos activa; `updateEmail` no.

### Eliminar cuenta

1. Configuración → "Eliminar cuenta" abre el diálogo con el estilo de 3050:1515: "¿Seguro deseas eliminarlo?", "No podrás recuperar los datos luego de confirmar la eliminación." y el botón "Eliminar cuenta".
2. Siempre se reautentica antes de borrar.
3. La app llama a la Cloud Function callable `deleteAccount`, en `southamerica-east1`. Con el Admin SDK, la función:
   1. exige un inicio de sesión reciente (`auth_time` de hace ≤ 5 min). Si no lo es, responde `failed-precondition` con `reason: "requires_recent_login"`;
   2. borra todo lo que esté bajo `users/{uid}/` en Storage;
   3. borra `users/{uid}` con `recursiveDelete`, que incluye las subcolecciones futuras (mascotas);
   4. borra el usuario de Auth.

   Es idempotente: si falla a la mitad, al reintentar se salta lo que ya no existe.
4. Si la función responde bien, la app llama a `signOut()`. El borrado en el servidor no cierra la sesión local de inmediato.
5. La llamada corre en `@ApplicationScope`, porque la pantalla se destruye en cuanto cambia la sesión (igual que el login, spec 002).

Se hace en el backend porque el cliente no puede borrar de forma confiable el documento, la foto y el usuario de Auth juntos. Además, así las reglas pueden seguir negando el `delete` a los clientes.

### Reautenticación

Se usa al cambiar el correo (solo si Firebase la pide) y siempre al eliminar la cuenta.

- **Cuenta de correo:** diálogo "Confirma tu contraseña" con `HuellitasPasswordField`. Si la contraseña es incorrecta, se muestra "Correo o contraseña incorrectos" bajo el campo.
- **Cuenta de Google:** se abre el selector de cuentas con `GoogleIdTokenRequester`.
- Si el usuario cancela, la operación se aborta sin mensaje.

### Sin conexión

- El overlay "Sin conexión" de la spec 002 solo cubre el grafo de auth (`feature.auth.connectivity`). En el grafo principal se usa el Snackbar "Sin conexión. Revisa tu internet" (`error_network`).
- Firestore no falla al escribir sin red: encola la escritura y su `await()` no termina hasta que vuelve la conexión. Por eso, antes de guardar datos, subir la foto, cambiar el correo o eliminar la cuenta, el ViewModel consulta `NetworkMonitor.isOnline`. Si no hay red, muestra el Snackbar y no lo intenta.
- Leer sí funciona sin red, desde la caché.

## Desviaciones de Figma

Decididas con el equipo (Figma no las cubre):

- **Fila "Eliminar cuenta"** en "Mi cuenta" de Configuración. Usa la misma tarjeta que "Datos personales" y "Seguridad", con el título y un ícono de papelera en `AlertRed`, para que se distinga como acción destructiva.
- **Insignia de cámara** sobre el avatar de Perfil: círculo verde abajo a la derecha, para indicar que la foto se puede tocar.
- **Subtítulo de Perfil:** "Gestiona tu información personal". El de Figma ("Gestiona los eventos de tu mascota") estaba copiado de otra pantalla.
- **Campos subrayados en Datos Personales**, no la caja de `HuellitasTextField`. Se crea `PersonalDataField` en `feature/profile/components`. Si la edición de mascotas ("EDIT PERRO") usa el mismo estilo, sube a `core.designsystem` (regla de ≥ 2 pantallas, spec 003).

## Reglas de validación (en cliente, `core.domain`)

| Campo | Regla | Mensaje |
|-------|-------|---------|
| Nombre / Correo | Vacío | Campo resaltado en naranja + "Rellene todos los datos para continuar" (un solo aviso, como en la spec 002) |
| Nombre | ≥ 2 caracteres (trim), con `ValidateNameUseCase` | "El nombre debe tener al menos 2 caracteres" |
| | ≤ 50 caracteres | "Máximo 50 caracteres" |
| Correo | `ValidateEmailUseCase` | "Ingresa un correo válido" |
| Teléfono (opcional) | `+` opcional y de 7 a 15 dígitos; se permiten espacios y guiones | "Ingresa un teléfono válido" |
| Dirección (opcional) | ≤ 100 caracteres | "Máximo 100 caracteres" |
| Fecha de nacimiento (opcional) | Se elige con el `DatePickerDialog` de Material 3. No puede ser futura ni anterior a 1900. Se muestra `dd/MM/yyyy`. | "Fecha no válida" |

Los avisos usan `FieldFeedback` (`Warning`/`Error`) y se limpian al editar el campo, igual que en la spec 002.

## Errores

| Origen | Error | Dónde | Mensaje |
|--------|-------|-------|---------|
| `FirebaseAuthRecentLoginRequiredException`, o `deleteAccount` con `reason: "requires_recent_login"` | `AuthError.RequiresRecentLogin` (nuevo) | — | (sin mensaje: abre la reautenticación) |
| Contraseña incorrecta al reautenticar | `AuthError.InvalidCredentials` | Bajo la contraseña | "Correo o contraseña incorrectos" |
| Correo nuevo ya en uso | `AuthError.EmailAlreadyInUse` | Bajo el correo | "Este correo ya está registrado" |
| Imagen > 5 MB tras comprimir | `DataError.FileTooLarge` (nuevo) | Snackbar | "La imagen es demasiado grande" |
| `users/{uid}` no existe al guardar | `DataError.NotFound` (nuevo) | Snackbar | "No encontramos tu perfil" |
| Sin red (`NetworkMonitor` o `Network`) | `AuthError.Network` / `DataError.Network` | Snackbar | "Sin conexión. Revisa tu internet" |
| Otro | `Unknown` | Snackbar | "Ocurrió un error inesperado" |

## Contratos

### Modelos (`core.model`)

```kotlin
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String?,
    val address: String?,
    val birthDate: LocalDate?, // java.time
    val photoUrl: String?,
    val provider: AuthProvider,
)

/** Solo los campos modificados; `null` = no cambia. Vaciar un campo opcional se representa con `Field.Clear`. */
data class ProfileChanges(
    val name: String? = null,
    val phone: Field<String>? = null,
    val address: Field<String>? = null,
    val birthDate: Field<LocalDate>? = null,
)

sealed interface Field<out T> {
    data class Set<T>(val value: T) : Field<T>
    data object Clear : Field<Nothing>
}
```

- `java.time` exige API 26 y el proyecto tiene `minSdk 24`. Hay que habilitar `isCoreLibraryDesugaringEnabled` y agregar `desugar_jdk_libs`.
- Usar `java.time` en `core.domain` no rompe la regla de capas de la spec 000, porque no es `android.*`.

### Firestore: `users/{uid}`

Se agregan estos campos a la tabla de la spec 002:

| Campo | Tipo | Notas |
|-------|------|-------|
| `phone` | string? | Normalizado sin espacios ni guiones |
| `address` | string? | |
| `birthDate` | string? | `yyyy-MM-dd`, sin zona horaria |
| `photoUrl` | string? | URL de descarga de Storage, o la de Google hasta que el usuario la cambie |
| `updatedAt` | timestamp | `FieldValue.serverTimestamp()` en cada actualización |

- Vaciar un campo opcional lo borra del documento (`FieldValue.delete()`).
- `email` es una copia del correo de Auth. Solo cambia mediante `syncEmail`.

### Reglas (`firebase/firestore.rules`)

- `update`, además de lo que ya exige:
  - `provider` no cambia;
  - `email` solo puede cambiar a `request.auth.token.email`;
  - solo se aceptan los campos del esquema (`keys().hasOnly([...])`), con límites de tamaño (`name` ≤ 50, `address` ≤ 100, `phone` ≤ 20).
- `delete` sigue en `false`: solo borra la Cloud Function, con el Admin SDK.

### Storage

- **Bucket:** el predeterminado del proyecto `huellitasya-3fd78`, en `southamerica-east1` (la misma región que Firestore y Functions). El proyecto ya está en plan Blaze, que Storage exige.
- **Reglas (`firebase/storage.rules`, nuevo):**
  - `users/{uid}/profile.jpg`: solo su dueño puede leer y escribir.
  - `request.resource.contentType` debe ser `image/.*` y `request.resource.size` < 5 MB.
  - Los clientes no pueden borrar; todo lo demás también queda denegado.
- **`firebase.json`:** agregar `"storage": { "rules": "firebase/storage.rules" }`. Se publica con `firebase deploy --only storage`.

### Backend (`functions/`)

- La callable `deleteAccount()` se agrega a `functions/src/index.ts` y usa las `setGlobalOptions` existentes (región `southamerica-east1`).
  - **Sin sesión:** `unauthenticated`.
  - **Sesión vieja:** `failed-precondition`, `details.reason = "requires_recent_login"`.
  - **Éxito:** `{ deleted: true }`.
- La lógica pura (p. ej. `isRecentLogin(authTime, now)`) va en `functions/src/account.ts`, con tests de `node --test`, igual que `resetCodes.ts`.
- Como con la recuperación de contraseña (CLAUDE.md), el contrato con la app son los códigos `HttpsError` y `details.reason`: si cambias uno, cambia el otro.

### Domain (`core.domain`)

- `UserRepository`, se agregan:
  - `observeProfile(uid): Flow<AppResult<UserProfile, DataError>>`
  - `updateProfile(uid, changes: ProfileChanges): AppResult<Unit, DataError>`
  - `updatePhotoUrl(uid, url): AppResult<Unit, DataError>`
  - `syncEmail(uid, email): AppResult<Unit, DataError>`
- `ProfilePhotoRepository` (nuevo; implementación `FirebaseStorageProfilePhotoRepository` en `core.data`):
  - `upload(uid, localUri: String): AppResult<String, DataError>`. Devuelve la URL de descarga. `core.data` resuelve el `Uri` con el `ContentResolver` y comprime, así el dominio no conoce `android.net.Uri`.
- `AccountRepository` (nuevo; implementación `FunctionsAccountRepository` sobre la callable, como `FunctionsPasswordResetRepository`):
  - `deleteAccount(): AppResult<Unit, AuthError>`
- `AuthRepository`, se agregan:
  - `reauthenticateWithPassword(password): AppResult<Unit, AuthError>`
  - `reauthenticateWithGoogle(idToken): AppResult<Unit, AuthError>`
  - `requestEmailChange(newEmail): AppResult<Unit, AuthError>` (`verifyBeforeUpdateEmail`)
  - `updateDisplayName(name): AppResult<Unit, AuthError>`. Se hace sin bloquear el guardado y mantiene al día el `User.name` de la sesión.
- Use cases:
  - `ObserveUserProfileUseCase`: si `users/{uid}` aún no existe, emite un perfil con los datos de la sesión.
  - `UpdateUserProfileUseCase`: guarda los cambios ya validados y, si cambió el nombre, llama a `updateDisplayName`. La validación por campo y el cambio de correo los coordina `PersonalDataViewModel`, para mostrar cada aviso bajo su campo.
  - `RequestEmailChangeUseCase`
  - `ChangeProfilePhotoUseCase`: sube la foto y actualiza `photoUrl`.
  - `ReauthenticateUseCase`
  - `DeleteAccountUseCase` (`@ApplicationScope`): llama a `AccountRepository.deleteAccount()` y, si sale bien, a `AuthRepository.signOut()`.
  - `ValidatePhoneUseCase`, `ValidateAddressUseCase`, `ValidateBirthDateUseCase`
- Se reutiliza `NetworkMonitor` (spec 002) para no intentar escrituras sin red.
- Sincronización del correo: después de `createIfMissing`, los use cases de inicio de sesión llaman a `syncEmail` si el correo de Auth es distinto al de Firestore. También corre en `@ApplicationScope`.

### UI (patrón MVVM, spec 000)

- `feature/profile`: `ProfileUiState`, `ProfileAction`, `ProfileEvent`, `ProfileViewModel`, `ProfileRoute` y `ProfileScreen`. Reemplaza la `ProfileScreen` estática actual.
- `feature/profile/personaldata`: `PersonalData*` con el mismo patrón. El estado incluye `isEditing`, `hasChanges` y un `FieldFeedback` por campo.
- `feature/settings`: `SettingsViewModel` pasa al patrón `UiState`/`Action`/`Event` y agrega eliminar la cuenta (confirmación, reautenticación y `LaunchGoogleReauth`). Las filas de "Mi cuenta" se rehacen según 3452:1018; el diálogo es `components/DeleteAccountDialog.kt`, con la tarjeta de Figma como imagen (`img_settings_delete_dialog`).
- `core.designsystem`: `HuellitasBackButton` (antes `AuthBackButton`) y `PasswordConfirmDialog`, compartidos por Auth, Perfil y Configuración. Su estado vive en `core.ui.PasswordPromptState`.
- `GoogleIdTokenRequester` sale de `feature.auth.google` y pasa a `core.ui.google`, porque Configuración también lo necesita para reautenticar y una feature no importa otra.
- Strings en `res/values/strings_profile.xml` y `strings_settings.xml`; los errores nuevos, en `strings_errors.xml`. Recursos con prefijo `img_profile_*` / `ic_profile_*` e `ic_settings_*`.

### Rutas

- `feature.profile.navigation`: `ProfileDestination` (ya existe) y `PersonalDataDestination`.
- `profileScreen(onNavigateToPersonalData)` y `personalDataScreen(onBack)`.
- `settingsScreen(onNavigateToPersonalData)`.

### Dependencias nuevas (`gradle/libs.versions.toml`)

- `firebase-storage` (por el BoM). `firebase-functions` ya está.
- `coil-compose` y `coil-network-okhttp` (Coil 3)
- `desugar_jdk_libs`

## Cambios a otras specs

Hechos en el PR de implementación:

- **000 — Arquitectura:** Credential Manager solo desde `core.ui.google`.
- **001 — Firebase setup:** paso 9 "Storage", `firebase-storage` en Contratos y criterio de las reglas de Storage.
- **002 — Autenticación:** enlaces a esta spec en `users/{uid}`, `AuthRepository`, `UserRepository` y Google Sign-In.
- **003 — Design system:** `HuellitasBackButton` y `PasswordConfirmDialog` en el catálogo.
- **CLAUDE.md:** sección "Perfil (spec 004)", nueva ubicación de `GoogleIdTokenRequester` y `firebase deploy --only storage`.

## Riesgos

- **Correo de verificación desde Firebase.** El cambio de correo usa el remitente de Firebase Auth, no el Gmail *noreply* de la recuperación con código. Si se quiere el mismo remitente, hace falta otra Cloud Function; queda fuera de alcance.
- **App Check.** Igual que las funciones de la spec 002, `deleteAccount` queda sin App Check hasta que se active para todo el backend.

## Criterios de aceptación

> Marcar cada casilla al verificarla en dispositivo.

- [ ] Perfil muestra los datos reales de `users/{uid}`, sin textos fijos, y se actualiza en vivo al guardar cambios.
- [ ] Los campos opcionales vacíos muestran "Sin registrar".
- [ ] "Editar información" (Perfil) y Configuración → "Datos personales" abren la misma pantalla.
- [ ] En Datos Personales no se puede escribir hasta pulsar el lápiz.
- [ ] Sin cambios, "Actualizar información" se ve gris; con cambios, verde.
- [ ] Un cambio válido se guarda, muestra "Datos actualizados" y la pantalla sale del modo edición.
- [ ] Datos inválidos muestran su aviso bajo el campo y no se escribe nada en Firestore.
- [ ] Vaciar el teléfono, la dirección o la fecha los borra del documento.
- [ ] Cambiar el correo pide la contraseña si Firebase lo exige, envía el enlace de verificación (en español) y el correo mostrado no cambia hasta confirmarlo.
- [ ] Tras confirmar el correo nuevo y volver a iniciar sesión, `users/{uid}.email` tiene el correo nuevo.
- [ ] En una cuenta Google, el correo no se puede editar.
- [ ] Elegir una foto la sube a Storage y el avatar la muestra; cancelar el selector no hace nada.
- [ ] "Eliminar cuenta" pide confirmación y reautenticación; luego desaparecen la foto, el documento y el usuario de Auth, y la app vuelve a Presentación con el historial limpio.
- [ ] Llamar a `deleteAccount` con un inicio de sesión de hace más de 5 min responde `requires_recent_login` y no borra nada.
- [ ] Después de eliminar la cuenta, el mismo correo se puede volver a registrar.
- [ ] Las reglas impiden leer o editar el perfil o la foto de otro usuario, borrar cualquiera de los dos desde el cliente, y cambiar `uid`, `provider` o `createdAt`.
- [ ] Sin conexión, Perfil muestra los datos en caché, y guardar, subir la foto o eliminar la cuenta muestran "Sin conexión. Revisa tu internet" sin quedarse cargando.

## Plan de tests

Unit tests en la JVM, con los fakes de `core/testing`:

- Validadores: `ValidatePhoneUseCaseTest`, `ValidateAddressUseCaseTest`, `ValidateBirthDateUseCaseTest`.
- Use cases: `ProfileUseCasesTest` (actualizar, cambio de correo, foto, eliminar + `signOut`).
- ViewModels: `ProfileViewModelTest`, `PersonalDataViewModelTest`, `SettingsViewModelTest`. Eliminar la cuenta se prueba con `appScope = this`, y el caso sin red con `FakeNetworkMonitor(initiallyOnline = false)`.
- Fakes: ampliar `FakeUserRepository` y `FakeAuthRepository` (`FakeRepositories.kt`), y agregar `FakeProfilePhotoRepository` y `FakeAccountRepository`.
- Las reglas de Firestore y Storage no tienen tests automáticos (no se usa el Emulator Suite, spec 001): se verifican con los criterios de aceptación.
- Backend: `functions/src/account.test.ts` (`npm --prefix functions test`).

## Fuera de alcance

- Cambio de contraseña (Configuración → Seguridad, 3263:1086): spec futura.
- Preferencias de la app, Ayuda y soporte, Términos y condiciones, Acerca de.
- Overlay "Sin conexión" en el grafo principal. Vive en `feature.auth.connectivity`; para reutilizarlo, habría que moverlo a `core.ui`.
- Recortar o editar la imagen antes de subirla.
- Correo de cambio de dirección desde el Gmail *noreply*.
- Confirmación al salir de Datos Personales con cambios sin guardar.
