# 004 — Perfil de usuario

- **Estado:** Implemented (criterios verificados en dispositivo; reglas, Storage y Cloud Functions desplegados)
  - Revisada contra `master` después del PR #2 (auth con código OTP, overlay "Sin conexión" y Cloud Functions).
  - Las [desviaciones de Figma](#desviaciones-de-figma) quedaron decididas con el equipo.
  - Para probar en otro proyecto de Firebase, el Owner crea el bucket de Storage y despliega: `firebase deploy --only firestore:rules,storage,functions` (spec 001, pasos 8 y 9).
- **Paquetes:** `feature.profile`, `feature.settings`, `core.domain`, `core.model`, `core.data`, `core.ui`, `core.designsystem`, `navigation`, y el backend en `functions/`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-)
  - Perfil (ver): `PERFIL` (99:723)
  - Datos Personales (editar): `CONFIGURACION` (3254:1023). Tarjeta "Datos Dueño" con lápiz y botón "Actualizar información".
  - Configuración (punto de entrada): `CONFIGURACION` (3452:1018) y la versión completa (144:621), sección "Mi cuenta".
  - Diálogo de confirmación para eliminar: se reutiliza el de mascotas, `EDIT PERRO 2` (3050:1515), "¿Seguro deseas eliminarlo?".
  - Seguridad, cambio de contraseña: `CONFIGURACION` (3263:1086); con la contraseña actual incorrecta, 3278:1170.
  - "Verifica tu correo" del cambio de correo: mismos textos y casillas que `VERIFICACION CORREO` (139:822), sin la foto de cabecera.

## Objetivo

Que el dueño de mascotas pueda ver su perfil, completar y editar sus datos personales (nombre, correo, teléfono, dirección y fecha de nacimiento), cambiar su foto, cambiar su contraseña y eliminar su cuenta. Los datos se guardan en Firestore (`users/{uid}`) y la foto en Firebase Storage. Los correos del cambio de correo y de contraseña salen del Gmail *noreply* con la plantilla de Huellitas (Cloud Functions), como la recuperación de contraseña de la spec 002.

Hoy `feature/profile/ProfileScreen.kt` es una maqueta con datos fijos, sin ViewModel, y `users/{uid}` solo se crea (`createIfMissing`, spec 002).

## Historias de usuario

- Como usuario, quiero ver mis datos en la pestaña Perfil.
- Como usuario, quiero editar mis datos desde "Editar información" (Perfil) o desde Configuración → Datos personales.
- Como usuario, quiero cambiar mi correo de forma segura, confirmándolo con un código que llega al correo nuevo.
- Como usuario, quiero cambiar mi contraseña desde Configuración → Seguridad.
- Como usuario, quiero recibir un aviso por correo si cambian el correo o la contraseña de mi cuenta.
- Como usuario, quiero cambiar mi foto de perfil.
- Como usuario, quiero eliminar mi cuenta y todos mis datos desde Configuración.

## Flujo de navegación

```
Perfil (pestaña) ──"Editar información"──► Datos Personales ──"<"──► atrás
Configuración ──"Datos personales"──────► Datos Personales ──correo nuevo──► Verifica tu correo ──código──► Datos Personales
Configuración ──"Seguridad"─────────────► Seguridad (cuentas Google: aviso, no abre)
Configuración ──"Eliminar cuenta"──► Diálogo de confirmación ──► Reautenticación ──► (sesión cerrada) Presentación
Perfil ──tocar la foto──► Photo Picker del sistema ──► subida ──► foto actualizada
```

- **Temporal:** al iniciar sesión, la app abre en Perfil (`startDestination` de `MainNavHost`). Home y Calendario se ven atenuados en la barra inferior y no se pueden abrir (`TopLevelDestination.isEnabled = false`) hasta que tengan su spec; entonces Home vuelve a ser la pestaña inicial.
- Datos Personales y "Verifica tu correo" viven en `feature.profile`; Seguridad, en `feature.settings`.
- Configuración no importa `feature.profile`: recibe la lambda `onNavigateToPersonalData`, y `MainNavHost` (`navigation/HuellitasApp.kt`) la resuelve.
- Eliminar la cuenta **no navega**. Al terminar, la app cierra la sesión local, `SessionState` pasa a `SignedOut` y se reemplaza el NavHost, como en el logout de la spec 002.

## Operaciones (CRUD)

| Operación | Dónde | Comportamiento |
|-----------|-------|----------------|
| **Crear** | Login y registro (ya existe) | `createIfMissing` no cambia. Los campos nuevos nacen ausentes. |
| **Leer** | Perfil y Datos Personales | Se escucha `users/{uid}` en tiempo real (snapshot listener → `Flow`). Funciona offline con la caché de Firestore. Los campos opcionales vacíos muestran "Sin registrar" en `LightGrayText`. Si el documento todavía no existe (el login lo está creando), se muestran el nombre, el correo y la foto de la sesión. |
| **Actualizar datos** | Datos Personales | Ver [Datos Personales](#datos-personales). |
| **Actualizar foto** | Perfil, al tocar el avatar | Ver [Foto de perfil](#foto-de-perfil). |
| **Actualizar correo** | Datos Personales → Verifica tu correo | Ver [Cambio de correo](#cambio-de-correo). |
| **Cambiar contraseña** | Configuración → Seguridad | Ver [Seguridad](#seguridad). |
| **Eliminar** | Configuración → "Eliminar cuenta" | Ver [Eliminar cuenta](#eliminar-cuenta). La hace una Cloud Function. |

### Datos Personales

- Por defecto se muestra en modo lectura. El lápiz de la tarjeta "Datos Dueño" activa la edición; si se pulsa otra vez durante la edición, descarta los cambios.
- "Actualizar información" se ve gris (`ButtonIncomplete`) mientras no hay cambios y verde (`GreenAction`) cuando los hay. Igual que en la spec 002, sigue siendo pulsable en gris y, con una petición en curso, muestra un indicador y no acepta otro clic.
- Solo el lápiz activa la edición: en modo lectura, "Actualizar información" no hace nada.
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

Con un código de 4 dígitos, igual que la recuperación de contraseña (spec 002). Así el correo sale de Huellitas y no de Firebase, y no hace falta volver a iniciar sesión.

- Solo cuentas de correo. En cuentas Google, el correo es de solo lectura (pertenece a la cuenta de Google).
- Al guardar con el correo cambiado, primero se guardan los demás cambios y después se llama a `requestEmailChangeCode(nuevo)`:
  - el backend exige un inicio de sesión de hace ≤ 5 min; si no, la app pide la contraseña ([Reautenticación](#reautenticación)) y reintenta;
  - si el correo ya es de otra cuenta, o es igual al actual, se marca en rojo bajo el campo;
  - si sale bien, Datos Personales vuelve a modo lectura y se abre "Verifica tu correo".
- **Verifica tu correo:** "Enviamos un código de 4 dígitos a <correo>", las cuatro casillas (`OtpCodeField`) y "Verificar código". Igual que en la spec 002:
  - el código vence a los 10 min y admite 5 intentos;
  - "Reenviar código" se habilita a los 60 s, con cuenta regresiva, y como máximo 5 envíos por hora;
  - si reenviar pasa de los 5 min de sesión, se pide volver a guardar el correo desde Datos Personales.
- Con el código correcto, `confirmEmailChange` cambia el correo en Auth (`emailVerified = true`) y en `users/{uid}` a la vez. La app vuelve a Datos Personales y muestra "Tu correo ahora es <correo>". La sesión sigue abierta:
  - cambiar el correo con el Admin SDK revoca las sesiones del usuario, así que el backend devuelve también un custom token;
  - la app inicia sesión con él (`signInWithCustomToken`) en `@ApplicationScope`, para que el cambio de sesión no interrumpa el use case.
- Después, el backend avisa al correo anterior: "El correo de tu cuenta ahora es n****@…", con el contacto de soporte por si no fue el usuario.

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

### Seguridad

Configuración → Seguridad (Figma 3263:1086). Solo cuentas de correo: en una cuenta Google, la fila muestra "Tu contraseña la gestiona tu cuenta de Google" y no abre la pantalla.

- Campos: "Contraseña actual", "Nueva contraseña" y "Confirmar contraseña" (`HuellitasPasswordField`), y la checklist en vivo de la spec 002 (`PasswordRequirementRow`): 8 caracteres, una mayúscula, un número o símbolo, y que coincidan.
- "Actualizar información" se ve verde cuando hay contraseña actual y la checklist está completa.
- Validación en cliente:
  - campos vacíos: aviso agrupado "Rellene todos los datos para continuar";
  - checklist incompleta: se resaltan los campos;
  - nueva igual a la actual: "La nueva contraseña debe ser distinta a la actual".
- Al enviar: sin red, Snackbar. Si no, se reautentica con la contraseña actual y se llama a `updatePassword`. La sesión sigue abierta.
  - Contraseña actual incorrecta: "La contraseña ingresada no es correcta" bajo el campo, en rojo (Figma 3278:1170).
- Si sale bien, se limpia el formulario, se muestra "Contraseña actualizada. Te enviamos un aviso a tu correo" y la callable `notifyPasswordChanged` envía el aviso "Tu contraseña cambió". Si el aviso no sale, el cambio sigue siendo válido.
- El mismo aviso se envía al restablecer la contraseña con el código de la spec 002.

### Reautenticación

Se usa al cambiar el correo (si el backend la pide) y siempre al eliminar la cuenta. Seguridad reautentica directamente con la contraseña actual.

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
- **Ícono de regla no cumplida** en la checklist de contraseña (`ic_password_rule_unmet`): una X naranja dentro del círculo, con la misma geometría que el visto verde (`ic_password_rule_met`). El glifo de Figma (un visto cruzado por una X) se leía como cumplido.
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
| Correo nuevo ya en uso | `EmailChangeError.EmailAlreadyInUse` | Bajo el correo | "Este correo ya está registrado" |
| Correo nuevo igual al actual o inválido | `EmailChangeError.InvalidEmail` | Bajo el correo | "Ingresa un correo distinto al actual" |
| `requestEmailChangeCode` con sesión vieja | `EmailChangeError.RequiresRecentLogin` | — | (sin mensaje: abre la reautenticación) |
| Código incorrecto / vencido / 5 intentos | `EmailChangeError.InvalidCode` / `ExpiredCode` / `TooManyAttempts` | Bajo las casillas, en rojo | Mismos textos que la spec 002 |
| Reenvío antes de tiempo | `EmailChangeError.TooManyRequests` | Snackbar + cuenta regresiva | "Espera N s para pedir otro código" |
| Contraseña actual incorrecta (Seguridad) | `AuthError.InvalidCredentials` | Bajo "Contraseña actual" | "La contraseña ingresada no es correcta" |
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

Callables en `functions/src/index.ts`, con las `setGlobalOptions` existentes (región `southamerica-east1`). Como con la recuperación de contraseña (CLAUDE.md), el contrato con la app son los códigos `HttpsError` y `details.reason`: si cambias uno, cambia el otro.

- **`deleteAccount()`**
  - Sin sesión: `unauthenticated`.
  - Sesión vieja (> 5 min): `failed-precondition`, `reason: "requires_recent_login"`.
  - Éxito: `{ deleted: true }`.
- **`requestEmailChangeCode({ newEmail })`** (secretos `SMTP_USER`, `SMTP_PASSWORD`, `RESET_CODE_PEPPER`)
  - Sesión vieja: `failed-precondition`, `requires_recent_login`.
  - Correo inválido o igual al actual: `invalid-argument`, `invalid_email`.
  - Correo de otra cuenta: `already-exists`, `email_in_use`.
  - Reenvío antes de tiempo: `resource-exhausted`, `resend_cooldown` + `retryAfterSeconds`.
  - El correo no salió: `unavailable`, `email_failed` (el envío no cuenta para la espera).
  - Guarda el código hasheado en `emailChangeCodes/{uid}` (`newEmail`, `codeHash`, `expiresAt`, `attempts`, `sends`). Las reglas de Firestore niegan esa colección a los clientes.
- **`confirmEmailChange({ code })`**
  - Mismos errores del código que `verifyPasswordResetCode`: `invalid_code`, `too_many_attempts`, `expired`. También `email_in_use`, si otra cuenta tomó el correo mientras tanto.
  - Éxito: `{ email, customToken }`. Cambia Auth y `users/{uid}`, borra el registro y avisa al correo anterior (best-effort).
  - El cambio revoca las sesiones, por eso devuelve un custom token. Firmarlo requiere `iam.serviceAccounts.signBlob`: la función corre con la cuenta del Admin SDK (`firebase-adminsdk-fbsvc@<proyecto>.iam.gserviceaccount.com`), que tiene "Service Account Token Creator"; la cuenta de Compute por defecto no lo tiene.
- **`notifyPasswordChanged()`**
  - Exige sesión reciente; como máximo un aviso por minuto (`passwordChangeNotices/{uid}`).
  - Éxito: `{ sent }`.
- `resetPasswordWithToken` (spec 002) también envía el aviso "Tu contraseña cambió".
- Lógica pura con tests de `node --test`: `account.ts` (`isRecentLogin`, `maskEmail`, `EmailChangeRecord`), `resetCodes.ts` (códigos, reutilizados con `CodeRecord`) y `email.ts` (plantillas con la marca, texto escapado).

### Domain (`core.domain`)

- `UserRepository`, se agregan:
  - `observeProfile(uid): Flow<AppResult<UserProfile, DataError>>`
  - `updateProfile(uid, changes: ProfileChanges): AppResult<Unit, DataError>`
  - `updatePhotoUrl(uid, url): AppResult<Unit, DataError>`
  - `syncEmail(uid, email): AppResult<Unit, DataError>`
- `ProfilePhotoRepository` (nuevo; implementación `FirebaseStorageProfilePhotoRepository` en `core.data`):
  - `upload(uid, localUri: String): AppResult<String, DataError>`. Devuelve la URL de descarga. `core.data` resuelve el `Uri` con el `ContentResolver` y comprime, así el dominio no conoce `android.net.Uri`.
- `AccountRepository` (nuevo; implementación `FunctionsAccountRepository` sobre las callables, como `FunctionsPasswordResetRepository`):
  - `deleteAccount(): AppResult<Unit, AuthError>`
  - `notifyPasswordChanged(): AppResult<Unit, AuthError>`
- `EmailChangeRepository` (nuevo; implementación `FunctionsEmailChangeRepository`), con errores en `EmailChangeError`:
  - `requestCode(newEmail): AppResult<Unit, EmailChangeError>`
  - `confirm(code): AppResult<ConfirmedEmailChange, EmailChangeError>`: devuelve el correo nuevo y el custom token.
- `AuthRepository`, se agregan:
  - `reauthenticateWithPassword(password): AppResult<Unit, AuthError>`
  - `reauthenticateWithGoogle(idToken): AppResult<Unit, AuthError>`
  - `updatePassword(newPassword): AppResult<Unit, AuthError>`
  - `signInWithCustomToken(token): AppResult<User, AuthError>`: vuelve a entrar tras cambiar el correo, porque el cambio revoca la sesión.
  - `updateDisplayName(name): AppResult<Unit, AuthError>`. Se hace sin bloquear el guardado y mantiene al día el `User.name` de la sesión.
- Use cases:
  - `ObserveUserProfileUseCase`: si `users/{uid}` aún no existe, emite un perfil con los datos de la sesión.
  - `UpdateUserProfileUseCase`: guarda los cambios ya validados y, si cambió el nombre, llama a `updateDisplayName`. La validación por campo y el cambio de correo los coordina `PersonalDataViewModel`, para mostrar cada aviso bajo su campo.
  - `RequestEmailChangeCodeUseCase` y `ConfirmEmailChangeUseCase` (`@ApplicationScope`; al confirmar, entra con el custom token)
  - `ChangePasswordUseCase`: reautentica con la actual, cambia la contraseña y pide el aviso (best-effort)
  - `ChangeProfilePhotoUseCase`: sube la foto y actualiza `photoUrl`.
  - `ReauthenticateUseCase`
  - `DeleteAccountUseCase` (`@ApplicationScope`): llama a `AccountRepository.deleteAccount()` y, si sale bien, a `AuthRepository.signOut()`.
  - `ValidatePhoneUseCase`, `ValidateAddressUseCase`, `ValidateBirthDateUseCase`
- Se reutiliza `NetworkMonitor` (spec 002) para no intentar escrituras sin red.
- Sincronización del correo: después de `createIfMissing`, los use cases de inicio de sesión llaman a `syncEmail` si el correo de Auth es distinto al de Firestore. También corre en `@ApplicationScope`. Con el cambio por código ya no es necesaria para ese flujo (el backend actualiza ambos), pero se mantiene por si el correo cambia por otra vía.

### UI (patrón MVVM, spec 000)

- `feature/profile`: `ProfileUiState`, `ProfileAction`, `ProfileEvent`, `ProfileViewModel`, `ProfileRoute` y `ProfileScreen`. Reemplaza la `ProfileScreen` estática actual.
- `feature/profile/personaldata`: `PersonalData*` con el mismo patrón. El estado incluye `isEditing`, `hasChanges` y un `FieldFeedback` por campo.
- `feature/profile/verifyemail`: `VerifyEmailChange*` ("Verifica tu correo"), con la cuenta regresiva para reenviar.
- `feature/settings/security`: `Security*` (cambio de contraseña).
- `feature/settings`: `SettingsViewModel` pasa al patrón `UiState`/`Action`/`Event` y agrega eliminar la cuenta (confirmación, reautenticación y `LaunchGoogleReauth`). Las filas de "Mi cuenta" se rehacen según 3452:1018; el diálogo es `components/DeleteAccountDialog.kt`, con la tarjeta de Figma como imagen (`img_settings_delete_dialog`).
- `core.designsystem`: `HuellitasBackButton` (antes `AuthBackButton`), `PasswordConfirmDialog` y `PasswordRequirementRow` (antes en `feature.auth.components`), compartidos por Auth, Perfil y Configuración. El estado del diálogo vive en `core.ui.PasswordPromptState`.
- `GoogleIdTokenRequester` sale de `feature.auth.google` y pasa a `core.ui.google`, porque Configuración también lo necesita para reautenticar y una feature no importa otra.
- Strings en `res/values/strings_profile.xml` y `strings_settings.xml`; los errores nuevos, en `strings_errors.xml`. Recursos con prefijo `img_profile_*` / `ic_profile_*` e `ic_settings_*`.

### Rutas

- `feature.profile.navigation`: `ProfileDestination`, `PersonalDataDestination` y `VerifyEmailChangeDestination(newEmail)`.
  - `profileScreen(onNavigateToPersonalData)`, `personalDataScreen(onBack, onNavigateToVerifyEmail)` y `verifyEmailChangeScreen(onBack, onEmailChanged)`.
  - `NavController.popBackToPersonalData(email)` devuelve el correo nuevo a Datos Personales por el `SavedStateHandle` de su entrada.
- `feature.settings.navigation`: `SettingsDestination` y `SecurityDestination`; `settingsScreen(googleServerClientId, onNavigateToPersonalData, onNavigateToSecurity)` y `securityScreen(onBack)`.

### Dependencias nuevas (`gradle/libs.versions.toml`)

- `firebase-storage` (por el BoM). `firebase-functions` ya está.
- `coil-compose` y `coil-network-okhttp` (Coil 3)
- `desugar_jdk_libs`

## Cambios a otras specs

Hechos en el PR de implementación:

- **000 — Arquitectura:** Credential Manager solo desde `core.ui.google`.
- **001 — Firebase setup:** paso 9 "Storage", `firebase-storage` en Contratos y criterio de las reglas de Storage.
- **002 — Autenticación:** enlaces a esta spec en `users/{uid}`, `AuthRepository`, `UserRepository` y Google Sign-In.
- **002 — Autenticación:** `PasswordRequirementRow` pasa al design system y restablecer la contraseña envía el aviso "Tu contraseña cambió".
- **003 — Design system:** `HuellitasBackButton`, `PasswordConfirmDialog` y `PasswordRequirementRow` en el catálogo.
- **CLAUDE.md:** sección "Perfil (spec 004)", nueva ubicación de `GoogleIdTokenRequester` y `firebase deploy --only storage`.

## Riesgos

- **Gmail *noreply*.** Todos los correos salen de la misma cuenta, con un límite de ~500 por día (spec 001). Si la app crece, se cambia a un servicio con dominio propio sin tocar la app.
- **App Check.** Igual que las funciones de la spec 002, las callables de esta spec quedan sin App Check hasta que se active para todo el backend.

## Criterios de aceptación

> Marcar cada casilla al verificarla en dispositivo.

- [x] Perfil muestra los datos reales de `users/{uid}`, sin textos fijos, y se actualiza en vivo al guardar cambios.
- [x] Los campos opcionales vacíos muestran "Sin registrar".
- [x] "Editar información" (Perfil) y Configuración → "Datos personales" abren la misma pantalla.
- [x] En Datos Personales no se puede escribir hasta pulsar el lápiz.
- [x] Sin cambios, "Actualizar información" se ve gris; con cambios, verde.
- [x] Un cambio válido se guarda, muestra "Datos actualizados" y la pantalla sale del modo edición.
- [x] Datos inválidos muestran su aviso bajo el campo y no se escribe nada en Firestore.
- [x] Vaciar el teléfono, la dirección o la fecha los borra del documento.
- [x] Cambiar el correo pide la contraseña si el backend lo exige y abre "Verifica tu correo"; llega un código de 4 dígitos al correo nuevo, desde el Gmail *noreply*.
- [x] Con el código correcto, Datos Personales muestra el correo nuevo sin cerrar la sesión, y `users/{uid}.email` y Auth tienen el correo nuevo.
- [x] Un código incorrecto se marca en rojo; tras 5 intentos o 10 minutos hay que pedir otro; "Reenviar código" solo se activa después de 60 s. (En dispositivo se verificaron el rojo y los 60 s; los 5 intentos y los 10 minutos, con los tests de `functions`.) (Verificados en dispositivo: el rojo y los 60 s. Los 5 intentos y los 10 minutos, con los tests de `functions`.)
- [x] Al correo anterior llega el aviso "Tu correo cambió" con el correo nuevo enmascarado.
- [x] Un correo que ya es de otra cuenta se marca en rojo bajo el campo y no se envía código.
- [x] En una cuenta Google, el correo no se puede editar.
- [x] Seguridad cambia la contraseña con la actual correcta, limpia el formulario y llega el aviso "Tu contraseña cambió"; la sesión sigue abierta y la contraseña anterior deja de funcionar.
- [x] Con la contraseña actual incorrecta, Seguridad muestra "La contraseña ingresada no es correcta" en rojo y no cambia nada.
- [x] En una cuenta Google, la fila Seguridad solo muestra el aviso.
- [x] Restablecer la contraseña con el código (spec 002) también envía el aviso "Tu contraseña cambió".
- [x] Elegir una foto la sube a Storage y el avatar la muestra; cancelar el selector no hace nada.
- [x] "Eliminar cuenta" pide confirmación y reautenticación; luego desaparecen la foto, el documento y el usuario de Auth, y la app vuelve a Presentación con el historial limpio.
- [x] Llamar a `deleteAccount` con un inicio de sesión de hace más de 5 min responde `requires_recent_login` y no borra nada.
- [x] Después de eliminar la cuenta, el mismo correo se puede volver a registrar.
- [x] Las reglas impiden leer o editar el perfil o la foto de otro usuario, borrar cualquiera de los dos desde el cliente, y cambiar `uid`, `provider` o `createdAt`.
- [x] Sin conexión, Perfil muestra los datos en caché, y guardar, subir la foto o eliminar la cuenta muestran "Sin conexión. Revisa tu internet" sin quedarse cargando.

## Plan de tests

Unit tests en la JVM, con los fakes de `core/testing`:

- Validadores: `ValidatePhoneUseCaseTest`, `ValidateAddressUseCaseTest`, `ValidateBirthDateUseCaseTest`.
- Use cases: `ProfileUseCasesTest` (actualizar, cambio de correo con código, cambio de contraseña con aviso, foto, eliminar + `signOut`).
- ViewModels: `ProfileViewModelTest`, `PersonalDataViewModelTest`, `VerifyEmailChangeViewModelTest`, `SecurityViewModelTest`, `SettingsViewModelTest`. Eliminar la cuenta se prueba con `appScope = this`, y el caso sin red con `FakeNetworkMonitor(initiallyOnline = false)`.
- Fakes: ampliar `FakeUserRepository` y `FakeAuthRepository` (`FakeRepositories.kt`), y agregar `FakeProfilePhotoRepository`, `FakeAccountRepository` y `FakeEmailChangeRepository`.
- Las reglas de Firestore y Storage no tienen tests automáticos (no se usa el Emulator Suite, spec 001): se verifican con los criterios de aceptación.
- Backend: `functions/src/account.test.ts` y `email.test.ts` (`npm --prefix functions test`).

## Fuera de alcance

- Preferencias de la app, Ayuda y soporte, Términos y condiciones, Acerca de.
- Overlay "Sin conexión" en el grafo principal. Vive en `feature.auth.connectivity`; para reutilizarlo, habría que moverlo a `core.ui`.
- Recortar o editar la imagen antes de subirla.
- Confirmación al salir de Datos Personales con cambios sin guardar.
