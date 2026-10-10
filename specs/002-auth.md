# 002 — Autenticación

- **Estado:** Approved
- **Módulos:** :feature:auth, :core:domain, :core:data, :core:model, :app
- **Figma:** _pendiente (Welcome, Login, Registro, Recuperar contraseña)_

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
     └─(sin sesión)──► Welcome ──"Comenzar"──► Login ──"Crear cuenta"──► Register
                                                │                          │
                                                ├─"¿Olvidaste tu contraseña?"► ForgotPassword
                                                │                          │
                                                └──(éxito)──► Home ◄───────┘ (éxito)
Settings ──"Cerrar sesión"──► Welcome (historial limpio)
```

- Al autenticarse se elimina el grafo de auth del back stack (atrás desde Home cierra la app).
- Al cerrar sesión, la app vuelve a Welcome reaccionando al cambio de estado de sesión (no por navegación manual de la pantalla).

## Reglas de validación (en cliente, `:core:domain`)

| Campo | Regla | Error |
|-------|-------|-------|
| Nombre | No vacío (trim), ≥ 2 caracteres | "Ingresa tu nombre" |
| Correo | No vacío y formato `x@y.z` | "Ingresa tu correo" / "Correo no válido" |
| Contraseña (registro) | ≥ 8 caracteres, al menos una letra y un número | "Mínimo 8 caracteres" / "Debe incluir letras y números" |
| Contraseña (login) | No vacía | "Ingresa tu contraseña" |
| Confirmar contraseña | Igual a contraseña | "Las contraseñas no coinciden" |

- Los errores se muestran bajo cada campo al pulsar el botón principal; se limpian al editar ese campo.
- El botón principal se deshabilita y muestra carga mientras hay una petición en curso.

## Errores remotos (`AuthError` → mensaje)

| Excepción Firebase | `AuthError` | Mensaje |
|--------------------|-------------|---------|
| `FirebaseAuthInvalidCredentialsException` / `FirebaseAuthInvalidUserException` | `InvalidCredentials` | "Correo o contraseña incorrectos" |
| `FirebaseAuthUserCollisionException` | `EmailAlreadyInUse` | "Ya existe una cuenta con este correo" |
| `FirebaseAuthWeakPasswordException` | `WeakPassword` | "La contraseña es demasiado débil" |
| `FirebaseNetworkException` | `Network` | "Sin conexión. Revisa tu internet" |
| `FirebaseTooManyRequestsException` | `TooManyRequests` | "Demasiados intentos. Intenta más tarde" |
| Google: usuario cancela | `Cancelled` | (sin mensaje) |
| Google: sin cuentas / credencial inválida | `GoogleSignInFailed` | "No se pudo iniciar sesión con Google" |
| Otro | `Unknown` | "Ocurrió un error inesperado" |

- Recuperar contraseña muestra siempre el mismo mensaje de éxito ("Si el correo está registrado, te enviamos un enlace") para no revelar qué correos existen.

## Contratos

### Modelos (`:core:model`)

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

- Registro con correo: se crea el usuario en Auth, se actualiza su `displayName` y se crea el documento.
- Google: el documento se crea **solo si no existe** (no se sobreescribe `createdAt` en logins posteriores).

### Domain (`:core:domain`)

- `AuthRepository`: `sessionUser: Flow<User?>`, `signInWithEmail`, `registerWithEmail`, `signInWithGoogle(idToken)`, `sendPasswordReset`, `signOut`.
- `UserRepository`: `createIfMissing(user)`.
- `GoogleIdTokenProvider`: `getIdToken(context)` — abstrae Credential Manager.
- Use cases: `ObserveSessionUseCase`, `SignInWithEmailUseCase`, `RegisterUseCase`, `SignInWithGoogleUseCase`, `SendPasswordResetUseCase`, `SignOutUseCase`, `ValidateEmailUseCase`, `ValidatePasswordUseCase`, `ValidateNameUseCase`.

### Rutas (`:feature:auth`)

`AuthGraph` (grafo anidado) → `WelcomeRoute`, `LoginRoute`, `RegisterRoute`, `ForgotPasswordRoute`.

## Criterios de aceptación

- [ ] Sin sesión, la app abre en Welcome; "Comenzar" lleva a Login.
- [ ] Login con correo y contraseña correctos lleva a Home; atrás desde Home no vuelve a Login.
- [ ] Login con credenciales incorrectas muestra "Correo o contraseña incorrectos".
- [ ] Registro válido crea el usuario en Firebase Auth y el documento `users/{uid}` con `provider = "email"`, y lleva a Home.
- [ ] Registro con correo existente muestra "Ya existe una cuenta con este correo".
- [ ] Las validaciones de la tabla se muestran bajo cada campo sin llamar a Firebase.
- [ ] "Continuar con Google" abre el selector de cuentas; al elegir una se entra a Home y se crea `users/{uid}` con `provider = "google"` solo la primera vez.
- [ ] Cancelar el selector de Google no muestra error.
- [ ] Recuperar contraseña con correo válido muestra el mensaje genérico de éxito y llega el correo.
- [ ] Con sesión activa, al reabrir la app se entra a Home sin ver Welcome (el splash se mantiene mientras se resuelve la sesión).
- [ ] "Cerrar sesión" en Configuración regresa a Welcome con el historial limpio.
- [ ] Mientras una petición está en curso, el botón principal no se puede pulsar dos veces.

## Fuera de alcance

- Verificación de correo obligatoria.
- Login con Facebook/Apple/teléfono.
- Edición de perfil y cambio de contraseña (Configuración → Seguridad) — spec futura.
- Eliminación de cuenta.
