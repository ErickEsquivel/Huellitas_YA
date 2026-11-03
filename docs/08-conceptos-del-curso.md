# 08 — Conceptos del curso en Huellitas YA

El curso [Android Basics with Compose](https://developer.android.com/courses/android-basics-compose/course?hl=es-419) enseña las bases de Android moderno. Esta tabla muestra **dónde aparece cada tema en este proyecto**, para pasar de la teoría al código real.

> Los enlaces llevan al archivo donde se ve el concepto. Ábrelo y busca el nombre indicado.

## Unidad 1 — Tu primera app

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Función `@Composable` | Cada pantalla, por ejemplo `LoginScreen` en [`LoginScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginScreen.kt) |
| `@Preview` | Al final de cada `XScreen.kt`: se ve la pantalla en Android Studio sin ejecutar la app |
| `val` y `var` | Los `UiState` usan solo `val`: el estado no se cambia, se copia con `copy(...)` |
| Recursos de texto (`strings.xml`) | [`res/values/`](../app/src/main/res/values): un archivo por área (`strings_auth.xml`, `strings_calendar.xml`…) |
| Imágenes (`painterResource`) | [`res/drawable-nodpi/`](../app/src/main/res/drawable-nodpi): fotos en WebP |

## Unidad 2 — Crear la interfaz

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Estado en Compose (`remember`, `mutableStateOf`) | Estado pequeño de la pantalla: mostrar u ocultar la contraseña en [`HuellitasTextField.kt`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem/components/HuellitasTextField.kt) (`rememberSaveable`) |
| Elevar el estado (*state hoisting*) | Las `XScreen` no guardan estado: lo reciben y avisan con `onAction`. Ver [02 — Capas y conceptos](02-capas-y-conceptos.md) |
| `if` / `when` | `when (result)` sobre `AppResult` en todos los ViewModels; `when (uiState)` en [`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt) |
| Seguridad frente a `null` (`?.`, `?:`) | `val localUri = state.localUri ?: return` en [`UploadRecordViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordViewModel.kt) |
| Clases `sealed` y `enum` | `AppResult` en [`AppResult.kt`](../app/src/main/kotlin/com/example/huellitasya/core/common/AppResult.kt); `XAction` y `XEvent` de cada pantalla; `ReminderKind` en [`Reminder.kt`](../app/src/main/kotlin/com/example/huellitasya/core/model/Reminder.kt) |
| Lambdas y funciones de orden superior | `onAction: (XAction) -> Unit`; el `onProgress` de `UploadRecordUseCase` |
| Pruebas unitarias | [`app/src/test/`](../app/src/test/kotlin/com/example/huellitasya), por ejemplo [`LoginViewModelTest.kt`](../app/src/test/kotlin/com/example/huellitasya/feature/auth/login/LoginViewModelTest.kt) con *fakes* de [`core/testing/`](../app/src/test/kotlin/com/example/huellitasya/core/testing) |

## Unidad 3 — Listas y Material Design

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Listas de Kotlin (`map`, `filter`, `firstOrNull`, `sortedBy`) | `mapNotNull { it.toReminder() }.sortedBy { it.dateTime }` en [`FirestoreReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreReminderRepository.kt) |
| `LazyColumn` / `LazyRow` | Mascotas en [`HomeScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/home/HomeScreen.kt); registros en [`RecordListScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/list/RecordListScreen.kt) |
| Tarjetas (`Card`) | [`PetCard.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/home/components/PetCard.kt), [`RecordCard.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/components/RecordCard.kt) |
| Tema de Material 3 (colores, tipografía, formas) | [`core/designsystem/`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem): [`Theme.kt`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem/Theme.kt), [`Color.kt`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem/Color.kt), [`Type.kt`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem/Type.kt) (fuente Nunito) |
| Componentes reutilizables | `HuellitasButton`, `HuellitasTextField`, `ScreenHeader` en [`core/designsystem/components/`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem/components) |
| Animaciones | `animateFloatAsState` en la barra de subida de [`UploadRecordScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/records/upload/UploadRecordScreen.kt) |

## Unidad 4 — Navegación y arquitectura

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| `ViewModel` | Uno por pantalla, por ejemplo [`LoginViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginViewModel.kt) |
| `StateFlow` + `UiState` | `private val _uiState = MutableStateFlow(...)` y `val uiState = _uiState.asStateFlow()` en cada ViewModel |
| `collectAsStateWithLifecycle` | En cada `XRoute`: la pantalla se redibuja cuando cambia el estado |
| Capa de UI y capa de datos | Ver [02 — Capas y conceptos](02-capas-y-conceptos.md). Este proyecto suma una capa de **dominio** en el medio |
| Navigation Compose | [`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt): `NavHost`, `navigate(...)`, `popBackStack()` |
| Pasar argumentos | Destinos `@Serializable` con datos (`UploadRecordDestination(petId, category)`), leídos con `SavedStateHandle` en el ViewModel |
| Barra inferior | [`HuellitasBottomBar.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasBottomBar.kt) y [`TopLevelDestination.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/TopLevelDestination.kt) |
| Pantallas adaptables | `AuthAdaptiveLayout`: la cabecera se encoge cuando se abre el teclado |

## Unidad 5 — Conectarse a internet

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Corrutinas, `suspend`, `launch` | `viewModelScope.launch { … }` en los ViewModels; los repositorios tienen funciones `suspend` |
| `async` / `await` | `appScope.async { … }.await()` en los casos de uso de [`RecordUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/RecordUseCases.kt) |
| Ámbitos (*scopes*) | `viewModelScope` muere con la pantalla; `@ApplicationScope` ([`ApplicationScope.kt`](../app/src/main/kotlin/com/example/huellitasya/core/common/ApplicationScope.kt)) vive mientras viva la app |
| Patrón repositorio | Interfaces en [`core/domain/repository/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository) e implementaciones en [`core/data/repository/`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository) |
| Inyección de dependencias | El curso la hace a mano con un `AppContainer`; aquí la hace **Hilt**: [`DataModule.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/di/DataModule.kt) con `@Binds` y `@Provides` |
| Cargar imágenes de internet (Coil) | `AsyncImage` en [`PetCard.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/home/components/PetCard.kt); miniatura de PDF en [`PdfFirstPageDecoder.kt`](../app/src/main/kotlin/com/example/huellitasya/core/ui/pdf/PdfFirstPageDecoder.kt) |
| Manejo de errores | En vez de `try/catch` en la pantalla, los repositorios devuelven `AppResult.Failure(error)` y la UI lo traduce con [`ErrorMessages.kt`](../app/src/main/kotlin/com/example/huellitasya/core/ui/ErrorMessages.kt) |
| Sin conexión | [`ConnectivityNetworkMonitor.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/network/ConnectivityNetworkMonitor.kt) |

En el curso los datos vienen de una API REST con Retrofit. Aquí vienen de **Firebase**, que trae su propio cliente; la idea del repositorio es la misma.

## Unidad 6 — Persistencia de datos

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Base de datos (Room) | No se usa Room: **Firestore** guarda los datos en la nube y mantiene una **caché local**, así que la app también muestra datos sin internet. Ver [03 — Firebase y datos](03-firebase-y-datos.md) |
| `Flow` para leer datos que cambian | `observeReminders`, `observePets`, `observeRecords`: `callbackFlow` + `addSnapshotListener` |
| Consultas | Rutas de colección y documento (`users/{uid}/pets`) en los repositorios `Firestore*` |
| Preferencias (DataStore) | No se usa: lo que hay que recordar está en Firestore o en el estado de la sesión |

## Unidad 7 — WorkManager

| Concepto del curso | Dónde está en Huellitas YA |
|---|---|
| Trabajo en segundo plano | Avisos de recordatorios: [`WorkManagerReminderNotifier.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/WorkManagerReminderNotifier.kt) programa un trabajo por recordatorio |
| `Worker` | [`ReminderWorker.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/ReminderWorker.kt) muestra la notificación a la hora indicada |
| Trabajo único con nombre | `enqueueUniqueWork`: si el recordatorio cambia, su trabajo se reemplaza |
| Descargas | [`DownloadManagerRecordDownloader.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/download/DownloadManagerRecordDownloader.kt) usa el `DownloadManager` del sistema |

## Unidad 8 — Views y Compose

No aplica: la app es **100 % Compose** y no tiene pantallas con XML.

## Más allá del curso

| Tema | Dónde |
|---|---|
| Firebase Auth, Firestore, Storage y Cloud Functions | [03 — Firebase y datos](03-firebase-y-datos.md) |
| Hilt | [02 — Capas y conceptos](02-capas-y-conceptos.md) |
| Casos de uso (capa de dominio) | [`core/domain/usecase/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase) |
| Eventos de una sola vez (`Channel` + `ObserveAsEvents`) | [`ObserveAsEvents.kt`](../app/src/main/kotlin/com/example/huellitasya/core/ui/ObserveAsEvents.kt) |
| Reglas de seguridad en el servidor | [`firebase/firestore.rules`](../firebase/firestore.rules), [`firebase/storage.rules`](../firebase/storage.rules) |

## Glosario breve

| Término | En una línea |
|---|---|
| **Composable** | Función que dibuja una parte de la pantalla. |
| **Estado** | Los datos que la pantalla muestra en este momento. Si cambia, la pantalla se redibuja. |
| **UiState** | Una `data class` con todo el estado de una pantalla. |
| **Action** | Lo que el usuario hizo (tocar, escribir). La pantalla se la envía al ViewModel. |
| **Event** | Algo que pasa una sola vez: un mensaje, navegar. |
| **ViewModel** | Guarda el estado y decide qué hacer con cada acción. Sobrevive al girar el teléfono. |
| **Caso de uso** | Una tarea concreta de la app ("crear recordatorio") con sus reglas. |
| **Repositorio** | Quien sabe guardar y leer datos. La interfaz está en el dominio; la versión con Firebase, en `data`. |
| **Modelo** | Una `data class` con los datos de la app (`Pet`, `Reminder`), sin nada de Android ni de Firebase. |
| **`AppResult`** | El resultado de una operación: `Success` con el dato o `Failure` con un error con nombre. |
| **`Flow`** | Una corriente de valores en el tiempo. Se usa para datos que cambian. |
| **`suspend`** | Función que puede esperar (a la red, por ejemplo) sin congelar la pantalla. |
| **Hilt** | Crea los objetos y se los entrega a quien los pide (`@Inject`). |
| **Documento / colección** | Cómo guarda los datos Firestore: documentos con campos, agrupados en colecciones. |
| **Regla de seguridad** | Condición que el servidor revisa antes de dejar leer o escribir. |
