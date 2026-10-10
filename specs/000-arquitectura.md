# 000 — Arquitectura

- **Estado:** Approved
- **Módulos:** todos

## Objetivo

Definir una arquitectura MVVM multi-módulo que permita desarrollar features en paralelo, reutilizar componentes de UI y mantener la lógica de negocio testeable sin Android ni Firebase.

## Mapa de módulos

```
:app                  Punto de entrada: MainActivity, Application (Hilt), NavHost raíz,
                      barra inferior y gate de sesión.
:core:model           Modelos de dominio puros (Kotlin/JVM, sin Android).
:core:common          Result/AppError y utilidades transversales (Kotlin/JVM).
:core:domain          Interfaces de repositorio + use cases + validadores (Kotlin/JVM).
:core:data            Implementaciones (Firebase Auth, Firestore, Credential Manager) y
                      módulos Hilt que las enlazan con las interfaces de :core:domain.
:core:designsystem    Tema, tokens (color, tipografía, espaciado) y componentes Compose
                      sin lógica de negocio.
:core:ui              Helpers de UI compartidos por features (UiText, ObserveAsEvents).
:feature:<nombre>     Pantallas + ViewModels de una feature (auth, home, calendar,
                      profile, settings).
build-logic/          Convention plugins de Gradle (configuración común de módulos).
```

### Reglas de dependencia

```
:app ──► :feature:* ──► :core:domain ──► :core:model, :core:common
  │           │
  │           ├──► :core:designsystem
  │           └──► :core:ui
  └──► :core:data ──► :core:domain
```

- Una feature **nunca** depende de otra feature ni de `:core:data`. Solo conoce interfaces (`:core:domain`); Hilt inyecta las implementaciones que `:app` incluye.
- `:core:model`, `:core:common` y `:core:domain` son módulos JVM puros: nada de `android.*` ni Firebase.
- `:core:designsystem` no conoce modelos de dominio ni ViewModels.

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
- Los textos visibles van en `strings.xml` del módulo. Los errores llegan a la UI como `UiText` (de `:core:ui`).
- Sub-composables de una pantalla son `private` en el mismo archivo o en `components/` del módulo; si se usan en ≥ 2 features, suben a `:core:designsystem`.

## Navegación

- Navigation Compose type-safe: cada feature declara sus rutas `@Serializable` en `navigation/XNavigation.kt` y expone:
  - `fun NavGraphBuilder.xScreen(...)` (o `xGraph(...)` si es un grafo anidado).
  - `fun NavController.navigateToX(navOptions)`.
- `:app` ensambla los grafos en `HuellitasNavHost`.
- Grafos raíz: `AuthGraph` (Welcome, Login, Register, ForgotPassword) y las pestañas principales (Home, Calendar, Profile, Settings) con barra inferior.

## Inyección de dependencias

- Hilt. `@HiltAndroidApp` en `HuellitasApplication`, `@AndroidEntryPoint` en `MainActivity`.
- Use cases con `@Inject constructor` (sin anotaciones de Android: `javax.inject` funciona en módulos JVM).
- Bindings `@Binds` / `@Provides` viven en `:core:data/di`.

## Convenciones de nombres

- Paquetes: `com.example.huellitasya.core.<modulo>` y `com.example.huellitasya.feature.<nombre>`.
- Use cases: verbo + sustantivo + `UseCase` (`SignInWithEmailUseCase`), con `operator fun invoke`.
- Repositorios: interfaz `XRepository` en domain, implementación `FirebaseXRepository` / `FirestoreXRepository` en data.

## Criterios de aceptación

- [ ] El proyecto compila con los módulos listados y las reglas de dependencia se cumplen (ninguna feature declara `:core:data` ni otra feature).
- [ ] Cada pantalla con lógica tiene ViewModel + Route + Screen stateless con `@Preview`.
- [ ] Los módulos `:core:model`, `:core:common`, `:core:domain` compilan como JVM puro.

## Fuera de alcance

- Persistencia offline propia (Room). Firestore ya trae caché offline.
- Modularización `api`/`impl` por feature.
