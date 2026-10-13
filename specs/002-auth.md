# 002 — Autenticación

- **Estado:** Implemented (pendiente de QA en dispositivo con el proyecto Firebase real)
- **Paquetes:** `feature.auth`, `core.domain`, `core.data`, `core.model`, `core.ui`, `navigation`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-)
  - Presentación: `PRESENTACION` (19:80)
  - Bienvenido (elegir acceso): `BIENVENIDO` (43:3)
  - Inicio de sesión: `INICIO SESION CORREO` (158:1303); con datos `INICIO SESION CORREO 2` (84:5); errores `Android Compact - 14/15/36`
  - Registro: `REGISTRO` (158:1329); errores `Android Compact - 6/7/35`
  - Recuperar contraseña: `OLVIDO CONTRASEÑA` (141:1069); errores `OC ERROR 1-4`
  - Verificar código: `VERIFICACION CORREO` (139:822); errores `VC ERROR 1-4`
  - Crear nueva contraseña: `CREAR CONTRASEÑA` (141:996); estados `CC ERROR 1-4`

## Objetivo

Permitir que el dueño de una mascota cree una cuenta e inicie sesión (correo/contraseña o Google), recupere su contraseña y mantenga la sesión abierta entre aperturas de la app. Cada cuenta tiene un perfil en Firestore.

## Historias de usuario

- Como usuario nuevo, quiero registrarme con nombre, correo y contraseña para guardar la información de mis mascotas.
- Como usuario, quiero iniciar sesión con mi correo o con Google para entrar rápido.
- Como usuario que olvidó su contraseña, quiero recibir un código por correo para crear una nueva.
- Como usuario con sesión abierta, quiero entrar directo al inicio al abrir la app.
- Como usuario, quiero cerrar sesión desde Configuración.

## Flujo de navegación

```
[App abre] ──(sesión activa)──► Home (pestañas)
     │
     └─(sin sesión)──► Presentación ──"Comenzar"──► Bienvenido
                                                     ├─ "Continuar con correo" ─► Inicio de sesión ─ "¿Se te olvidó la contraseña?" ─► Recuperar
                                                     ├─ "Continuar con Google" ─► selector de cuentas ─► Home
                                                     └─ "Regístrate" ─────────► Registro ─► Home
Configuración ──"Cerrar sesión"──► Presentación (historial limpio)
```

- La navegación hacia Home **no** la hace la pantalla: la app observa `SessionState` y reemplaza el grafo de auth por el principal (y viceversa al cerrar sesión). Así el historial anterior desaparece siempre.
- Los formularios tienen el botón "<" arriba a la izquierda para volver.

## Reglas de validación (en cliente, `core.domain`)

| Campo | Regla | Mensaje |
|-------|-------|---------|
| Cualquiera | Vacío | Campo resaltado en naranja + "Rellene todos los datos para continuar" (un solo aviso para el formulario) |
| Nombre | ≥ 2 caracteres (trim) | "El nombre debe tener al menos 2 caracteres" |
| Correo | Formato `x@y.z` | "Ingresa un correo válido" |
| Contraseña (registro) | ≥ 8 caracteres | "Mínimo 8 caracteres" |
| | Al menos una mayúscula | "Debe incluir una letra mayúscula" |
| | Al menos un número o carácter especial | "Debe incluir un número o carácter especial" |
| Contraseña (login) | No vacía | — |
| Confirmar contraseña | Igual a contraseña | "Las contraseñas no coinciden" (rojo) |
| Términos y Condiciones | Casilla marcada | "Acepta los Términos y Condiciones para continuar" |

Presentación (Figma "ERRORES LAYOUTS"):
- **Advertencia** (dato faltante o mal formado): borde naranja + ícono de alerta.
- **Error** (no coincide o lo rechazó el servidor): borde rojo + ícono X.
- El aviso de un campo se limpia al editarlo.
- El botón principal se ve gris (`ButtonIncomplete`) mientras falten datos y verde (`GreenAction`) cuando el formulario está completo. Sigue siendo pulsable en gris para mostrar los avisos.
- Mientras hay una petición en curso, el botón muestra un indicador y no acepta otro clic.

## Adaptación a pantalla y teclado

Login, Registro y Recuperar contraseña comparten `AuthFormScaffold` y se ajustan sin scroll vertical:

- **Cabecera flexible.** La foto y la insignia ocupan el espacio que sobra del alto disponible:
  - en pantallas altas crecen hasta el tamaño de Figma;
  - en pantallas bajas se encogen hasta un mínimo de 96 dp;
  - la ola blanca inferior siempre queda visible, y la insignia se oculta si no cabe.
- **Teclado.** El alto disponible excluye el teclado y sigue su animación. Al abrirse el teclado:
  - la foto, la insignia y el subtítulo se ocultan con animación;
  - la flecha volver pasa a negro;
  - el botón principal queda justo encima del teclado.

  Al cerrarlo, todo vuelve a aparecer.
- **Modo compacto.** Se activa con el teclado abierto o con un alto disponible < 640 dp:
  - campos de 44 dp con etiqueta de 14 sp;
  - título de 20 sp (24 sp en Recuperar);
  - espacios reducidos;
  - margen inferior del botón de 12 dp.
- **Barra de navegación del sistema.** Sin teclado, el botón queda por encima de ella (la app dibuja edge-to-edge).
- **Respaldo.** Si el contenido no cabe ni así (p. ej. un teléfono de 640 dp de alto con el teclado abierto, o una fuente del sistema muy grande), el formulario se puede desplazar. Nunca se corta contenido.

Verificado en un Vivo V2436 (384×857 dp, Android 16):
- sin scroll en las tres pantallas, con y sin teclado;
- con la pantalla simulada a 360×640 dp, sin scroll sin teclado.
- En 360×640 dp **con** teclado, Login usa el respaldo.

## Recuperar contraseña con código

```
Inicio de sesión ─ "¿Se te olvidó la contraseña?" ─► ¿Olvidaste tu contraseña? ─ "Enviar código" ─►
Verifica tu correo (4 dígitos) ─ "Verificar código" ─► Crear nueva contraseña ─ "Restablecer contraseña" ─►
Inicio de sesión + "Tu contraseña se actualizó. Inicia sesión."
```

- **Backend:** Firebase Auth no tiene códigos de recuperación, así que el flujo lo resuelven 3 Cloud Functions en `southamerica-east1` (`functions/src/index.ts`). Cambiar la contraseña de un usuario sin sesión solo es posible con el Admin SDK.
  - `requestPasswordResetCode({email})` genera el código y lo envía.
  - `verifyPasswordResetCode({email, code})` valida el código y devuelve un `resetToken` de un solo uso.
  - `resetPasswordWithToken({email, resetToken, newPassword})` cambia la contraseña y cierra las sesiones abiertas (`revokeRefreshTokens`).
- **Correo:** sale de un Gmail dedicado *noreply* (`"Huellitas YA" <SMTP_USER>`, ver spec 001), en español. Lleva el código grande, la vigencia, "Si no lo pediste, ignora este correo" y "No respondas a este mensaje".
- **Código:** 4 dígitos (`crypto.randomInt`).
  - Vence a los **10 min** y admite **5 intentos**; después hay que pedir uno nuevo.
  - Se guarda hasheado (HMAC con el secreto `RESET_CODE_PEPPER`) en `passwordResetCodes/{sha256(correo)}`. Las reglas de Firestore niegan esa colección a los clientes.
- **Reenvío:** "Reenviar código" se habilita a los **60 s**, con una cuenta regresiva en el botón, y admite máximo **5 envíos por hora**.
- **Privacidad:** si el correo no tiene cuenta, el backend responde igual y no envía nada. La app siempre avanza a "Verifica tu correo".
- **Token:** vale 10 min y un solo uso. Al pasar a "Crear nueva contraseña" se saca "Verifica tu correo" del historial: atrás vuelve a "¿Olvidaste tu contraseña?".

| Situación | Presentación |
|-----------|--------------|
| Código incompleto | Casillas en naranja + "Ingresa un número válido" |
| Código incorrecto | Casillas en rojo + "El código ingresado no es correcto" |
| Código vencido | Rojo + "El código venció. Pide uno nuevo" |
| 5 intentos fallidos | Rojo + "Demasiados intentos. Pide un código nuevo" |
| Reenvío antes de tiempo | La cuenta regresiva usa el tiempo que indica el servidor |
| Contraseña nueva | Checklist en vivo: ✗ naranja / ✓ verde por cada regla (8 caracteres, mayúscula, número o símbolo, coinciden). El servidor valida la misma política. |
| Token vencido al restablecer | Snackbar "El código venció. Pide uno nuevo" |
| Sin red | Overlay "Sin conexión" |

## Sin conexión

- **Figma:** `ERROR INTERNET Overlay` (159:647).
- **Dónde:** cubre todas las pantallas del flujo de autenticación: Presentación, Bienvenido, Inicio de sesión, Registro y Recuperar contraseña.
- **Cuándo aparece:**
  - cuando el dispositivo no tiene una red con internet **validado** (`NetworkMonitor`);
  - cuando una operación de Firebase falla con `AuthError.Network`, aunque el sistema crea que hay red (p. ej. un wifi sin salida a internet).
- **Estados:**
  - **Sin conexión:** velo blanco al 60 %, con la pantalla de fondo desenfocada en Android 12+. Tarjeta con el cachorro, "¡Oh No! Sin Conexión", un mensaje y el botón **Reintentar**. Bloquea los toques sobre la pantalla de abajo.
  - **Conectando…:** al pulsar Reintentar se muestra el spinner de Figma, que da 3 vueltas lineales en 2.4 s, durante al menos 1.2 s. Si hay internet, el overlay se cierra; si no, vuelve a "Sin conexión".
- **Cierre automático:** cuando el sistema recupera la red, el overlay se cierra solo.
- **Reintentar** no repite la acción que falló: el usuario vuelve a pulsar el botón del formulario.
- **Estado compartido:** lo lleva `ConnectivityViewModel`, que vive en la entrada del grafo `AuthGraph`, así el overlay no se reinicia al navegar entre pantallas.

## Errores remotos (`AuthError` → presentación)

| Excepción Firebase | `AuthError` | Dónde | Mensaje |
|--------------------|-------------|-------|---------|
| `FirebaseAuthInvalidCredentialsException` / `FirebaseAuthInvalidUserException` | `InvalidCredentials` | Bajo la contraseña (login) | "Correo o contraseña incorrectos" |
| `FirebaseAuthUserCollisionException` | `EmailAlreadyInUse` | Bajo el correo (registro) | "Este correo ya está registrado" |
| `FirebaseAuthWeakPasswordException` | `WeakPassword` | Bajo la contraseña (registro) | "La contraseña es demasiado débil" |
| `FirebaseNetworkException` | `Network` | Overlay "Sin conexión" | "¡Oh No! Sin Conexión" |
| `FirebaseTooManyRequestsException` | `TooManyRequests` | Snackbar | "Demasiados intentos. Intenta más tarde" |
| Google: usuario cancela | `Cancelled` | — | (sin mensaje) |
| Google: sin cuentas / credencial inválida | `GoogleSignInFailed` | Snackbar | "No se pudo iniciar sesión con Google" |
| Otro | `Unknown` | Snackbar | "Ocurrió un error inesperado" |

Diferencias con Figma, intencionales:
- Figma separa "Este correo no se encuentra registrado" de "La contraseña no coincide". Firebase, con la protección contra enumeración de correos que viene activa por defecto, devuelve el mismo error en ambos casos. Por eso se muestra un único mensaje bajo la contraseña.
- Recuperar contraseña avanza siempre a "Verifica tu correo", exista o no la cuenta, para no revelar qué correos existen. Por eso no se implementa el estado `OC ERROR 1` ("Este correo no se encuentra registrado").

## Contratos

### Modelos (`core.model`)

```kotlin
data class User(val uid: String, val name: String, val email: String, val photoUrl: String?, val provider: AuthProvider)
enum class AuthProvider { EMAIL, GOOGLE }
sealed interface SessionState { Loading; SignedOut; SignedIn(user: User) }
```

### Firestore: `users/{uid}`

| Campo | Tipo | Notas |
|-------|------|-------|
| `uid` | string | = id del documento |
| `name` | string | Del formulario o del perfil de Google |
| `email` | string | |
| `photoUrl` | string? | Solo Google |
| `provider` | string | `"email"` \| `"google"` |
| `createdAt` | timestamp | `FieldValue.serverTimestamp()` |

- El documento se crea **solo si no existe** (`createIfMissing`) en registro, login con correo y login con Google. No se pisa `createdAt`, y si una creación falló, el siguiente login la reintenta.
- Esa escritura corre en `@ApplicationScope`, porque la pantalla que la inicia se destruye en cuanto la sesión cambia.
- Los campos del perfil (`phone`, `address`, `birthDate`, `updatedAt`) y las reglas de edición los define la spec 004.

### Domain (`core.domain`)

- `AuthRepository`: `currentUser: Flow<User?>`, `signInWithEmail`, `registerWithEmail`, `signInWithGoogle(idToken)`, `signOut`. La spec 004 agrega la reautenticación, el cambio de correo y `updateDisplayName`.
- `PasswordResetRepository` (implementado sobre Cloud Functions): `requestCode`, `verifyCode` → `resetToken`, `resetPassword`. Errores en `PasswordResetError`.
- `UserRepository`: `createIfMissing(user)`. Después del login también se llama a `syncEmail` (spec 004). Lectura y edición del perfil: spec 004.
- Use cases: `ObserveSessionUseCase`, `SignInWithEmailUseCase`, `RegisterUseCase`, `SignInWithGoogleUseCase`, `RequestPasswordResetCodeUseCase`, `VerifyPasswordResetCodeUseCase`, `ResetPasswordUseCase`, `SignOutUseCase`, `ValidateNameUseCase`, `ValidateEmailUseCase`, `ValidatePasswordUseCase`.

### Google Sign-In

- `GoogleIdTokenRequester` (en `core.ui.google`, porque Configuración también lo usa para reautenticar) abre el selector de cuentas con Credential Manager (`GetSignInWithGoogleOption`) y devuelve el ID token. Vive en la capa de UI porque necesita el `Context` de un Activity.
- El `serverClientId` es `R.string.default_web_client_id`. Lo genera el plugin de Google Services a partir de `google-services.json` y `MainActivity` lo busca por nombre y lo pasa a `authGraph(...)`. Si no existe (Google sin configurar), el botón de Google responde "No se pudo iniciar sesión con Google".

### Rutas (`feature.auth.navigation`)

`AuthGraph` (grafo anidado) → `WelcomeDestination`, `AuthChooserDestination`, `LoginDestination`, `RegisterDestination`, `ForgotPasswordDestination`, `VerifyResetCodeDestination(email)`, `NewPasswordDestination(email, resetToken)`.

## Criterios de aceptación

> Lógica cubierta por unit tests (`LoginViewModelTest`, `RegisterViewModelTest`, `ForgotPasswordViewModelTest`, `VerifyResetCodeViewModelTest`, `NewPasswordViewModelTest`, `AuthChooserViewModelTest`, `AuthUseCasesTest`, `MainViewModelTest`). Marcar cada casilla al verificarla en dispositivo.

- [ ] Sin sesión, la app abre en Presentación; "Comenzar" lleva a Bienvenido.
- [ ] "Continuar con correo" abre Inicio de sesión; "Regístrate" abre Registro.
- [ ] Login con correo y contraseña correctos lleva a Home; atrás desde Home no vuelve a Login.
- [ ] Login con credenciales incorrectas muestra "Correo o contraseña incorrectos" bajo la contraseña.
- [ ] Pulsar el botón con campos vacíos resalta los campos y muestra "Rellene todos los datos para continuar" sin llamar a Firebase.
- [ ] Registro válido crea el usuario en Firebase Auth y el documento `users/{uid}` con `provider = "email"`, y lleva a Home.
- [ ] Registro con correo existente muestra "Este correo ya está registrado" bajo el correo.
- [ ] Registro sin aceptar Términos y Condiciones no se envía.
- [ ] "Continuar con Google" abre el selector de cuentas; al elegir una se entra a Home y se crea `users/{uid}` con `provider = "google"` solo la primera vez.
- [ ] Cancelar el selector de Google no muestra error.
- [ ] Recuperar contraseña con un correo registrado envía un correo en español desde el Gmail *noreply*, con un código de 4 dígitos.
- [ ] Un código incorrecto se marca en rojo; tras 5 intentos o 10 minutos hay que pedir otro.
- [ ] "Reenviar código" solo se activa después de 60 s.
- [ ] Con el código correcto se crea la nueva contraseña (checklist en verde), se vuelve a Login con el mensaje de éxito, y la contraseña anterior deja de funcionar.
- [ ] Con sesión activa, al reabrir la app se entra a Home sin ver Presentación (el splash se mantiene mientras se resuelve la sesión).
- [ ] "Cerrar sesión" en Configuración regresa a Presentación con el historial limpio.
- [ ] Mientras una petición está en curso, el botón principal no se puede pulsar dos veces.
- [x] Login, Registro y Recuperar no necesitan scroll, con o sin teclado, en pantallas de ≥ 800 dp de alto; sin teclado, tampoco desde 640 dp.
- [x] Al abrir el teclado se ocultan con animación la foto, la insignia y el subtítulo, y el botón queda encima del teclado; al cerrarlo, reaparecen.
- [x] Sin internet aparece el overlay "Sin conexión" en cualquier pantalla de auth; "Reintentar" muestra "Conectando…" y el overlay se cierra al volver la red. Verificado en el Vivo V2436 con modo avión, sobre Presentación; las demás pantallas usan el mismo envoltorio.

## Fuera de alcance

- App Check en las Cloud Functions (recomendado antes de publicar la app).
- Contenido de Términos y Condiciones: los enlaces se muestran, pero aún no abren nada.
- Verificación de correo obligatoria.
- Login con Facebook, Apple o teléfono.
- Edición de perfil y eliminación de cuenta: ver la [spec 004](004-perfil-usuario.md).
- Cambio de contraseña (Configuración → Seguridad): spec futura.
