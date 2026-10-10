# 002 — Autenticación

- **Estado:** Implemented (pendiente de QA en dispositivo con el proyecto Firebase real)
- **Paquetes:** `feature.auth`, `core.domain`, `core.data`, `core.model`, `core.ui`, `navigation`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-)
  - Presentación: `PRESENTACION` (19:80)
  - Bienvenido (elegir acceso): `BIENVENIDO` (43:3)
  - Inicio de sesión: `INICIO SESION CORREO` (158:1303); con datos `INICIO SESION CORREO 2` (84:5); errores `Android Compact - 14/15/36`
  - Registro: `REGISTRO` (158:1329); errores `Android Compact - 6/7/35`
  - Recuperar contraseña: `OLVIDO CONTRASEÑA` (141:1069); errores `OC ERROR 1-4`

## Objetivo

Permitir que el dueño de una mascota cree una cuenta e inicie sesión (correo/contraseña o Google), recupere su contraseña y mantenga la sesión abierta entre aperturas de la app. Cada cuenta tiene un perfil en Firestore.

## Historias de usuario

- Como usuario nuevo, quiero registrarme con nombre, correo y contraseña para guardar la información de mis mascotas.
- Como usuario, quiero iniciar sesión con mi correo o con Google para entrar rápido.
- Como usuario que olvidó su contraseña, quiero recibir un correo para restablecerla.
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

## Errores remotos (`AuthError` → presentación)

| Excepción Firebase | `AuthError` | Dónde | Mensaje |
|--------------------|-------------|-------|---------|
| `FirebaseAuthInvalidCredentialsException` / `FirebaseAuthInvalidUserException` | `InvalidCredentials` | Bajo la contraseña (login) | "Correo o contraseña incorrectos" |
| `FirebaseAuthUserCollisionException` | `EmailAlreadyInUse` | Bajo el correo (registro) | "Este correo ya está registrado" |
| `FirebaseAuthWeakPasswordException` | `WeakPassword` | Bajo la contraseña (registro) | "La contraseña es demasiado débil" |
| `FirebaseNetworkException` | `Network` | Snackbar | "Sin conexión. Revisa tu internet" |
| `FirebaseTooManyRequestsException` | `TooManyRequests` | Snackbar | "Demasiados intentos. Intenta más tarde" |
| Google: usuario cancela | `Cancelled` | — | (sin mensaje) |
| Google: sin cuentas / credencial inválida | `GoogleSignInFailed` | Snackbar | "No se pudo iniciar sesión con Google" |
| Otro | `Unknown` | Snackbar | "Ocurrió un error inesperado" |

Diferencias con Figma, intencionales:
- Figma separa "Este correo no se encuentra registrado" de "La contraseña no coincide". Firebase, con la protección contra enumeración de correos que viene activa por defecto, devuelve el mismo error en ambos casos. Por eso se muestra un único mensaje bajo la contraseña.
- Recuperar contraseña muestra siempre la misma confirmación ("Si el correo está registrado, te enviamos un enlace…"), para no revelar qué correos existen. Por eso no se implementa el estado `OC ERROR 1` ("Este correo no se encuentra registrado").

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

### Domain (`core.domain`)

- `AuthRepository`: `currentUser: Flow<User?>`, `signInWithEmail`, `registerWithEmail`, `signInWithGoogle(idToken)`, `sendPasswordReset`, `signOut`.
- `UserRepository`: `createIfMissing(user)`.
- Use cases: `ObserveSessionUseCase`, `SignInWithEmailUseCase`, `RegisterUseCase`, `SignInWithGoogleUseCase`, `SendPasswordResetUseCase`, `SignOutUseCase`, `ValidateNameUseCase`, `ValidateEmailUseCase`, `ValidatePasswordUseCase`.

### Google Sign-In

- `GoogleIdTokenRequester` (en `feature.auth.google`) abre el selector de cuentas con Credential Manager (`GetSignInWithGoogleOption`) y devuelve el ID token. Vive en la capa de UI porque necesita el `Context` de un Activity.
- El `serverClientId` es `R.string.default_web_client_id`. Lo genera el plugin de Google Services a partir de `google-services.json` y `MainActivity` lo pasa a `authGraph(...)`.

### Rutas (`feature.auth.navigation`)

`AuthGraph` (grafo anidado) → `WelcomeDestination`, `AuthChooserDestination`, `LoginDestination`, `RegisterDestination`, `ForgotPasswordDestination`.

## Criterios de aceptación

> Lógica cubierta por unit tests (`LoginViewModelTest`, `RegisterViewModelTest`, `ForgotPasswordViewModelTest`, `AuthChooserViewModelTest`, `AuthUseCasesTest`, `MainViewModelTest`). Marcar cada casilla al verificarla en dispositivo.

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
- [ ] Recuperar contraseña con correo válido muestra la confirmación genérica y envía el correo.
- [ ] Con sesión activa, al reabrir la app se entra a Home sin ver Presentación (el splash se mantiene mientras se resuelve la sesión).
- [ ] "Cerrar sesión" en Configuración regresa a Presentación con el historial limpio.
- [ ] Mientras una petición está en curso, el botón principal no se puede pulsar dos veces.

## Fuera de alcance

- **Restablecer contraseña con código de 4 dígitos** (Figma `VERIFICACION CORREO` y `CREAR CONTRASEÑA`). Firebase Auth restablece contraseñas con un enlace por correo, no con códigos. Implementar ese flujo requiere un backend propio (Cloud Functions) o manejar el `oobCode` del enlace con un deep link. Queda para una spec futura.
- Contenido de Términos y Condiciones: los enlaces se muestran, pero aún no abren nada.
- Verificación de correo obligatoria.
- Login con Facebook, Apple o teléfono.
- Edición de perfil y cambio de contraseña (Configuración → Seguridad).
- Eliminación de cuenta.
