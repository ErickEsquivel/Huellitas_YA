# Huellitas YA! — Arquitectura y Firebase CLI

## 1. La idea general

La app Android no usa un servidor propio: habla directamente con Firebase. La app y Firebase se configuran por dos vías distintas:

| Pieza | Para qué sirve | Cómo se conecta |
|---|---|---|
| **App Android** (`app/`) | Pantallas, lógica y lectura/escritura de datos | SDK de Firebase + `app/google-services.json` (no está en el repo) |
| **Firebase CLI** (`firebase.json`, `.firebaserc`) | Publicar en la nube las reglas y las Cloud Functions | Comando `firebase` en la terminal del desarrollador |

Al ejecutarse, la app nunca usa la CLI. La CLI solo sube a Firebase lo que la app necesita que exista allá: las reglas de seguridad y las funciones del backend.

```text
 Desarrollador ──firebase deploy──►  Firebase (proyecto huellitasya-3fd78)
                                      ├── Firestore + firestore.rules
                                      ├── Storage   + storage.rules
                                      ├── Cloud Functions (functions/)
                                      └── Authentication
                                              ▲
 Teléfono ──SDK (google-services.json)────────┘
```

## 2. Arquitectura de la app (MVVM por capas)

Todo está en un solo módulo Gradle (`:app`), dentro de `app/src/main/kotlin/com/example/huellitasya/`, separado en capas:

```text
feature/      Pantallas: Screen (Compose) + ViewModel   ── solo conoce el dominio
   │
   ▼
core/domain/  Casos de uso, interfaces de repositorio, validaciones, errores
   ▲            (Kotlin puro: sin Android ni Firebase)
   │ implementa
core/data/    Repositorios con Firebase (Auth, Firestore, Storage, Functions)
              + módulos de Hilt (di/) que conectan interfaz ↔ implementación
```

Además:

- `core/model/`: datos puros (`Pet`, `MedicalRecord`, `User`…).
- `core/common/`: `AppResult` y `@ApplicationScope`.
- `core/designsystem/`: tema y componentes visuales.
- `core/ui/`: utilidades de UI (`UiText`, mensajes de error, Google Sign-In, decoder del PDF).

**Reglas de capas** (spec 000; el compilador no las impone porque es un solo módulo):

1. `feature.*` nunca importa `core.data` ni otra feature.
2. `core.model`, `core.common` y `core.domain` no importan Android ni Firebase.
3. Firebase solo aparece en `core.data`.

### 2.1 El flujo de una acción, paso a paso

Ejemplo: el usuario toca **"Subir Archivo"** en Registros médicos.

```text
UploadRecordScreen  ──onAction(Upload)──►  UploadRecordViewModel
 (feature/records/upload)                    │ valida el formulario
                                             │ consulta NetworkMonitor (¿hay red?)
                                             ▼
                                     UploadRecordUseCase  (core/domain/usecase)
                                             │ 1. upload del archivo (avance → barra)
                                             │ 2. downloadUrl
                                             │ 3. createRecord en Firestore
                                             ▼
                RecordFileRepository / RecordRepository   (interfaces de dominio)
                                             │ Hilt inyecta la implementación
                                             ▼
   FirebaseStorageRecordFileRepository / FirestoreRecordRepository  (core/data)
                                             │
                                             ▼
                                    Firebase Storage + Firestore
                                    (las reglas validan cada escritura)
```

La respuesta vuelve por el mismo camino:

- El repositorio devuelve `AppResult.Success` o `AppResult.Failure(DataError)`. Nunca lanza excepciones.
- El ViewModel actualiza su `UiState` (un `StateFlow`) o emite un `Event` de una sola vez por un `Channel` (por ejemplo "Sin conexión" o volver a la lista).
- La Screen redibuja al cambiar el estado y reacciona a los eventos con `ObserveAsEvents`.

### 2.2 Las piezas de cada pantalla

| Pieza | Qué hace | Ejemplo |
|---|---|---|
| `XUiState` | Data class con todo lo que se dibuja | `RecordDetailUiState(pet, record, isDeleting…)` |
| `XAction` | Lo que el usuario puede hacer | `RecordDetailAction.Download` |
| `XEvent` | Efectos de una vez (Snackbar, navegar) | `RecordDetailEvent.Deleted` |
| `XViewModel` | `@HiltViewModel`; depende solo de casos de uso | `RecordDetailViewModel` |
| `XRoute` | Composable con estado: `hiltViewModel()` + `collectAsStateWithLifecycle` | `RecordDetailRoute` |
| `XScreen` | Composable sin estado, con `@Preview` | `RecordDetailScreen` |

### 2.3 Datos en vivo

Los repositorios de lectura usan `callbackFlow` sobre los listeners de Firestore. Si un dato cambia en la nube, la pantalla se actualiza sola. Por ejemplo, el conteo "2 Archivos" del perfil de la mascota baja al eliminar uno. Sin red, Firestore entrega lo que tiene en caché.

### 2.4 Sesión y navegación

`MainViewModel` escucha la sesión de Firebase Auth y `navigation/HuellitasApp.kt` elige el grafo:

```kotlin
when (uiState) {
    MainUiState.Loading -> Unit                       // splash
    MainUiState.SignedOut -> AuthNavHost(...)         // Bienvenido, login, registro…
    is MainUiState.SignedIn -> MainNavHost(...)       // Home, mascotas, perfil…
}
```

Iniciar o cerrar sesión **no navega**: Firebase cambia la sesión y se reemplaza el NavHost completo. Por eso los casos de uso que deben terminar aunque la pantalla se destruya, como crear el perfil al entrar o eliminar un registro, corren en `@ApplicationScope`:

```kotlin
suspend operator fun invoke(...) = appScope.async { /* trabajo */ }.await()
```

### 2.5 Inyección de dependencias (Hilt)

`core/data/di/DataModule.kt` une cada interfaz del dominio con su implementación de Firebase:

```kotlin
@Binds @Singleton
abstract fun bindRecordRepository(impl: FirestoreRecordRepository): RecordRepository
```

En las pruebas, en lugar de Firebase se usan repositorios falsos (`app/src/test/.../core/testing/`). Los ViewModels se prueban sin teléfono ni red.

## 3. Cómo funciona Firebase CLI en este proyecto

### 3.1 Archivos de configuración (raíz del repo)

`.firebaserc` dice a qué proyecto apunta la CLI:

```json
{ "projects": { "default": "huellitasya-3fd78" } }
```

`firebase.json` dice qué se publica y desde dónde:

```json
{
  "firestore": { "rules": "firebase/firestore.rules" },
  "storage":   { "rules": "firebase/storage.rules" },
  "functions": {
    "source": "functions",
    "runtime": "nodejs22",
    "predeploy": ["npm --prefix \"$RESOURCE_DIR\" run build"]
  }
}
```

| Sección | Archivo fuente | Qué controla |
|---|---|---|
| `firestore` | `firebase/firestore.rules` | Quién lee y escribe cada documento, y con qué forma (por ejemplo, un registro médico solo con campos válidos y < 10 MB) |
| `storage` | `firebase/storage.rules` | Quién sube, descarga y borra fotos y archivos, y de qué tipo y tamaño |
| `functions` | `functions/src/*.ts` | Backend en TypeScript (Node 22). Antes de desplegar se compila con `tsc` (`predeploy`) |

### 3.2 Las Cloud Functions

Viven en `functions/src/index.ts`, en la región `southamerica-east1`. Son *callables*: la app las llama desde `core/data` con el SDK de Functions.

| Función | Módulo | Qué hace |
|---|---|---|
| `requestPasswordResetCode` | Autenticación | Genera un código de 4 dígitos y lo envía por correo |
| `verifyPasswordResetCode` | Autenticación | Valida el código y devuelve un token de un solo uso |
| `resetPasswordWithToken` | Autenticación | Cambia la contraseña con ese token |
| `requestEmailChangeCode` | Perfil | Envía un código al correo nuevo |
| `confirmEmailChange` | Perfil | Cambia el correo y devuelve un custom token para volver a entrar |
| `notifyPasswordChanged` | Perfil | Avisa por correo que la contraseña cambió |
| `deleteAccount` | Perfil | Borra archivos, documentos (con subcolecciones) y el usuario |

La lógica pura está separada para poder probarla sin la nube:

- `resetCodes.ts`: códigos, intentos y tiempos de espera.
- `account.ts`: eliminación de la cuenta.
- `email.ts`: plantillas de correo.

Sus pruebas son los archivos `*.test.ts`.

**Secretos:** las funciones usan `SMTP_USER`, `SMTP_PASSWORD` (Gmail noreply) y `RESET_CODE_PEPPER`. No están en el código: se guardan en Firebase con `firebase functions:secrets:set <NOMBRE>`. Las Cloud Functions requieren el plan Blaze.

### 3.3 Comandos

```bash
firebase login                              # una vez por computador (cuenta con acceso al proyecto)
firebase projects:list                      # comprobar que se ve huellitasya-3fd78

firebase deploy --only firestore:rules      # publicar firebase/firestore.rules
firebase deploy --only storage              # publicar firebase/storage.rules
firebase deploy --only functions            # compilar y desplegar functions/

npm --prefix functions test                 # compilar y probar el backend (sin desplegar)
```

### 3.4 Flujo de trabajo con la CLI

1. **Se cambia la spec** (`specs/00X-*.md`) con los campos y permisos nuevos.
2. **Se editan las reglas** en `firebase/` o las funciones en `functions/src/`.
3. **Se prueba localmente:** `npm --prefix functions test` para el backend y `./gradlew testDebugUnitTest` para la app.
4. **Se despliega** solo lo que cambió, con `--only`, y únicamente con el permiso del equipo, porque afecta a todos los usuarios.
5. **Se verifica en la nube:**
   - las reglas, con llamadas REST desde dos cuentas (dueño y extraño), como las 19 pruebas del módulo de Registros;
   - las funciones, desde la app en el teléfono.

> Si el contrato con la app cambia (los códigos `HttpsError` y su `details.reason`), hay que cambiar a la vez `functions/src/index.ts` y el repositorio de `core/data` que lo interpreta.

### 3.5 Cosas a tener en cuenta

- `app/google-services.json` conecta la app con el proyecto. **Nunca se sube al repo** (está en `.gitignore`).
- La cuenta con la que se hace `firebase login` debe tener acceso al proyecto. Si un deploy responde 403, hay que volver a iniciar sesión con una cuenta autorizada (`firebase login --reauth`).
- Las reglas no tienen pruebas automáticas, porque el proyecto no usa el Emulator Suite. Se verifican con los criterios de aceptación de cada spec.
