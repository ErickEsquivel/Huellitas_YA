# 000 — Arquitectura

- **Estado:** Implemented
- **Módulo:** `:app` (único módulo Gradle)

## Objetivo

Definir una arquitectura MVVM por capas que permita desarrollar features en paralelo, reutilizar componentes de UI y mantener la lógica de negocio testeable sin Android ni Firebase.

## Estructura de paquetes

Todo vive en `:app`, bajo `app/src/main/kotlin/com/example/huellitasya/`:

```
MainActivity, HuellitasApplication, MainViewModel   Punto de entrada, Hilt y gate de sesión
navigation/          HuellitasApp (NavHost raíz), barra inferior, TopLevelDestination
core/model/          Modelos de dominio puros (User, SessionState)
core/common/         AppResult/AppError, @ApplicationScope
core/domain/         Interfaces de repositorio + use cases + validadores
core/data/           Implementaciones (Firebase Auth, Firestore) y módulos Hilt (di/)
core/designsystem/   Tema, tokens (color, tipografía, espaciado) y componentes Compose sin lógica
core/ui/             Helpers de UI compartidos (UiText, FieldFeedback, ObserveAsEvents)
feature/<nombre>/    Pantallas + ViewModels de una feature (auth, home, calendar, profile, settings)
```

Los tests están en `app/src/test/kotlin/...` con la misma estructura. `core/testing/` contiene los fakes de repositorios y `MainDispatcherRule`.

### Reglas de dependencia entre paquetes

```
feature.* ──► core.domain ──► core.model, core.common
    │
    ├──► core.designsystem
    └──► core.ui
core.data ──► core.domain
```

Al ser un solo módulo, el compilador no impone estas reglas. Se revisan en code review:
- Un paquete `feature.*` **nunca** importa `core.data` ni otra feature. Solo conoce interfaces de `core.domain`, y Hilt inyecta las implementaciones.
- `core.model`, `core.common` y `core.domain` son Kotlin puro: nada de `android.*`, `androidx.*` ni Firebase. Así se pueden mover a módulos JVM si el proyecto crece.
- `core.designsystem` no conoce modelos de dominio ni ViewModels.
- Firebase solo se importa desde `core.data`. Credential Manager solo desde `feature.auth.google`.

Comprobación rápida (debe salir vacío):

```bash
grep -rln "huellitasya.core.data" app/src/main/kotlin/com/example/huellitasya/feature
grep -rln "import android\|import androidx\|google" app/src/main/kotlin/com/example/huellitasya/core/{model,common,domain}
```

## Patrón MVVM por pantalla

Cada pantalla se compone de:

| Pieza | Responsabilidad |
|-------|-----------------|
| `XUiState` (`data class`) | Todo lo que la UI pinta. Inmutable. |
| `XAction` (`sealed interface`) | Intenciones del usuario (escribir, pulsar). |
| `XEvent` (`sealed interface`) | Efectos de una sola vez (navegar, mostrar error). |
| `XViewModel` (`@HiltViewModel`) | Expone `StateFlow<XUiState>` y `Flow<XEvent>` (Channel). Recibe `onAction(XAction)`. Solo depende de use cases. |
| `XRoute` (`@Composable`) | Stateful: obtiene el ViewModel con `hiltViewModel()`, recoge el estado con `collectAsStateWithLifecycle()`, observa eventos con `ObserveAsEvents` y recibe callbacks de navegación. |
| `XScreen` (`@Composable`) | Stateless: `XScreen(state, onAction)`. Tiene `@Preview`. Es la única pieza que se reescribe al aplicar un diseño de Figma. |

Reglas:
- Las pantallas no reciben `NavController`; exponen lambdas (`onNavigateToHome`) que el grafo resuelve.
- Los textos visibles van en `res/values/strings_<área>.xml` (p. ej. `strings_auth.xml`, `strings_errors.xml`). Los errores llegan a la UI como `UiText` (de `core.ui`).
- Los sub-composables de una pantalla son `private` en el mismo archivo o van en `components/` de la feature. Si se usan en ≥ 2 features, suben a `core.designsystem`.
- Recursos: un único `R` (`com.example.huellitasya.R`). Para evitar choques, los nombres llevan prefijo de área (`img_auth_*`, `ic_settings_*`).

## Navegación

- Navigation Compose type-safe. Cada feature declara sus destinos `@Serializable` (`XDestination`) en `navigation/XNavigation.kt` y expone `fun NavGraphBuilder.xScreen(...)`, o `xGraph(...)` si es un grafo anidado.
- El sufijo `Destination` se usa para los objetos de ruta, porque `XRoute` es el nombre del composable stateful.
- `navigation/HuellitasApp.kt` decide el NavHost según `SessionState`:
  - `SignedOut` → NavHost de auth (`authGraph`).
  - `SignedIn` → NavHost principal con barra inferior (`TopLevelDestination`).
- Iniciar o cerrar sesión **no** navega: reemplaza el NavHost completo, y con él el historial.

## Inyección de dependencias

- Hilt. `@HiltAndroidApp` en `HuellitasApplication`, `@AndroidEntryPoint` en `MainActivity`.
- Use cases con `@Inject constructor` (solo `javax.inject`, sin anotaciones de Android).
- Bindings `@Binds` / `@Provides` en `core/data/di`.

## Tests

- Unit tests en la JVM para validadores, use cases y ViewModels, con los fakes de `core.testing` (`FakeAuthRepository`, `FakeUserRepository`), `MainDispatcherRule` y Turbine para flows.
- En tests de ViewModel, el `@ApplicationScope` de los use cases es el propio `TestScope` (`appScope = this`), para que `advanceUntilIdle()` ejecute ese trabajo.

## Convenciones de nombres

- Paquetes: `com.example.huellitasya.core.<capa>` y `com.example.huellitasya.feature.<nombre>`.
- Use cases: verbo + sustantivo + `UseCase` (`SignInWithEmailUseCase`), con `operator fun invoke`.
- Repositorios: interfaz `XRepository` en domain, implementación `FirebaseXRepository` / `FirestoreXRepository` en data.

## Criterios de aceptación

- [x] El proyecto compila como un solo módulo y las reglas de dependencia entre paquetes se cumplen (la comprobación rápida sale vacía).
- [x] Cada pantalla con lógica tiene ViewModel + Route + Screen stateless con `@Preview`.
- [x] `core.model`, `core.common` y `core.domain` no importan Android ni Firebase.

## Fuera de alcance

- Persistencia offline propia (Room). Firestore ya trae caché offline.
- Volver a multi-módulo Gradle. Si el equipo o los tiempos de build lo justifican, cada paquete `core.*` / `feature.*` puede pasar a su propio módulo sin cambiar de paquete.
