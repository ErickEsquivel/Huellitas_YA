# 02 — Capas y conceptos

Cada sección explica un concepto en tres partes: qué es, dónde está en el proyecto y un ejemplo real del código.

## El patrón de cada pantalla (MVVM)

Toda pantalla tiene las mismas seis piezas. Tomemos **Iniciar sesión** como ejemplo ([`LoginViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginViewModel.kt), [`LoginScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginScreen.kt)).

| Pieza | Qué es | En Login |
|---|---|---|
| `XUiState` | Todo lo que la pantalla dibuja, en una sola `data class`. | `LoginUiState` |
| `XAction` | Todo lo que el usuario puede hacer. | `LoginAction` |
| `XEvent` | Cosas que pasan **una sola vez**: un mensaje, navegar. | `LoginEvent` |
| `XViewModel` | Guarda el estado, recibe las acciones y llama a los casos de uso. | `LoginViewModel` |
| `XRoute` | Composable **con** estado: conecta el ViewModel con la pantalla. | `LoginRoute` |
| `XScreen` | Composable **sin** estado: solo dibuja lo que recibe. Tiene `@Preview`. | `LoginScreen` |

### UiState: el estado

```kotlin
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailFeedback: FieldFeedback? = null,
    val passwordFeedback: FieldFeedback? = null,
    /** Aviso del formulario completo ("Rellene todos los datos para continuar"). */
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    /** Figma pinta el botón en gris mientras falten datos y en verde cuando está listo. */
    val isComplete: Boolean get() = email.isNotBlank() && password.isNotEmpty()
}
```

- Es inmutable (`val`): para cambiarlo se crea una copia con `copy(...)`.
- `isComplete` es un valor **derivado**: se calcula a partir de los otros campos y no se guarda aparte.

### Action y Event

```kotlin
sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEvent {
    data class ShowMessage(val message: UiText) : LoginEvent
    data object NetworkError : LoginEvent
}
```

- `sealed interface` significa que la lista de opciones está cerrada. Así el `when` obliga a manejar todas.
- **Estado o evento:** el estado se queda (el texto escrito); el evento ocurre una vez (un Snackbar). Si un mensaje fuera estado, se volvería a mostrar al girar el teléfono.

### ViewModel

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()
```

- `@HiltViewModel` + `@Inject constructor`: Hilt crea el ViewModel y le entrega los casos de uso. Nadie hace `new`.
- `MutableStateFlow` es privado: **solo el ViewModel** cambia el estado. Hacia afuera se expone como `StateFlow`, de solo lectura.
- `Channel` lleva los eventos de una sola vez.
- El ViewModel **sobrevive a la rotación** de pantalla: el texto escrito no se pierde.
- Depende solo de **casos de uso**, nunca de Firebase.

Las acciones entran por una sola puerta:

```kotlin
fun onAction(action: LoginAction) {
    when (action) {
        is LoginAction.EmailChanged -> _uiState.update {
            it.copy(email = action.value, emailFeedback = null, formFeedback = null)
        }
        is LoginAction.PasswordChanged -> _uiState.update {
            it.copy(password = action.value, passwordFeedback = null, formFeedback = null)
        }
        LoginAction.Submit -> submit()
    }
}
```

`update { it.copy(...) }` cambia el estado de forma segura: toma el actual y devuelve uno nuevo.

### Route y Screen

```kotlin
internal fun LoginRoute(
    ...
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ...
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            LoginEvent.NetworkError -> onNetworkError()
        }
    }

    LoginScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        ...
    )
}
```

- `hiltViewModel()` pide a Hilt el ViewModel de esta pantalla.
- `collectAsStateWithLifecycle()` convierte el `StateFlow` en estado de Compose: cuando el estado cambia, la pantalla se vuelve a dibujar (*recomposición*). Deja de escuchar si la app pasa a segundo plano.
- [`ObserveAsEvents`](../app/src/main/kotlin/com/example/huellitasya/core/ui/ObserveAsEvents.kt) escucha los eventos solo mientras la pantalla está visible.
- `LoginScreen` recibe el estado y una función `onAction`. Así se puede ver en `@Preview` sin Firebase. A esto se le llama **elevación de estado** (*state hoisting*).

## UiText: textos sin `Context`

El ViewModel no debe conocer Android, pero los textos viven en `strings.xml`. [`UiText`](../app/src/main/kotlin/com/example/huellitasya/core/ui/UiText.kt) guarda "qué texto mostrar" y la pantalla lo convierte:

```kotlin
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    class Resource(@param:StringRes val id: Int, vararg val args: Any) : UiText {
```

El ViewModel envía `UiText.Resource(R.string.error_network)` y la Route lo convierte en texto con `asString(context)`.

## Modelos: `core/model/`

Son las clases de datos de la app, en Kotlin puro. No saben nada de pantallas ni de Firebase.

```kotlin
/** Mascota guardada en `users/{uid}/pets/{petId}` (spec 005). */
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val breed: String,
    val birthDate: LocalDate,
    val sex: PetSex,
    val weightKg: Double,
    val photoUrl: String?,
)
```

[`Pet.kt`](../app/src/main/kotlin/com/example/huellitasya/core/model/Pet.kt). Hay dos tipos de modelo:
- **El guardado** (`Pet`, `Reminder`): tiene `id` porque ya existe.
- **El borrador** (`PetDraft`, `ReminderDraft`): son los datos ya validados de un formulario, sin `id`, listos para guardar.

`String?` significa que el valor puede faltar (`null`); Kotlin obliga a revisarlo antes de usarlo.

## AppResult: éxito o error sin excepciones

Las operaciones que pueden fallar devuelven un [`AppResult`](../app/src/main/kotlin/com/example/huellitasya/core/common/AppResult.kt):

```kotlin
sealed interface AppResult<out D, out E : AppError> {
    data class Success<out D>(val data: D) : AppResult<D, Nothing>
    data class Failure<out E : AppError>(val error: E) : AppResult<Nothing, E>
}
```

- `Success(data)` trae el resultado; `Failure(error)` trae un error **con nombre**: `DataError.Network`, `AuthError.InvalidCredentials`…
- Quien lo recibe hace `when (result)` y está obligado a manejar los dos casos. Un error no puede "escaparse" y cerrar la app.
- Los errores están en [`core/domain/error/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/error), y [`ErrorMessages.kt`](../app/src/main/kotlin/com/example/huellitasya/core/ui/ErrorMessages.kt) los convierte en mensajes con `toUiText()`.

## Dominio: `core/domain/`

Es el corazón de la app: **qué** se puede hacer, sin decir **cómo**. No importa Android ni Firebase.

### Interfaces de repositorio

```kotlin
/** Recordatorios del usuario en `users/{uid}/reminders`, de todas sus mascotas (spec 007). */
interface ReminderRepository {
    /** Todos los recordatorios, del más próximo al más lejano; sin red, desde la caché. */
    fun observeReminders(uid: String): Flow<AppResult<List<Reminder>, DataError>>

    /** Id para un recordatorio nuevo. Se genera antes de guardarlo para programar su aviso con el mismo id. */
    fun newReminderId(uid: String): String

    suspend fun createReminder(uid: String, reminderId: String, draft: ReminderDraft): AppResult<Unit, DataError>
    ...
}
```

[`ReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository/ReminderRepository.kt).

- **`Flow`** es un valor que **cambia en el tiempo**: cada vez que Firestore cambia, llega una lista nueva. Se usa para leer en vivo.
- **`suspend`** es una función que puede esperar (la red) sin congelar la pantalla. Se usa para escribir una vez.
- Es una **interfaz**: el dominio no sabe que existe Firestore. En las pruebas se reemplaza por un `FakeReminderRepository` en memoria.

### Casos de uso

Cada uno hace **una** acción de negocio, como [`CreateReminderUseCase`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/ReminderUseCases.kt):

```kotlin
class CreateReminderUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(draft: ReminderDraft): AppResult<String, DataError> = appScope.async {
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        val id = reminderRepository.newReminderId(uid)
        val normalized = draft.copy(customTitle = draft.customTitle?.trim()?.takeIf { it.isNotEmpty() })
        reminderRepository.createReminder(uid, id, normalized).map { id }
    }.await()
}
```

- `operator fun invoke` permite llamarlo como una función: `createReminder(draft)`.
- Busca el usuario con sesión, prepara los datos y llama al repositorio. Combina varios repositorios cuando hace falta.
- Están agrupados por tema en [`core/domain/usecase/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase): `PetUseCases.kt`, `RecordUseCases.kt`, `ReminderUseCases.kt`, `SignInUseCases.kt`…

### Validaciones

[`Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt) tiene una clase por regla. Devuelven `null` si el dato está bien y un `ValidationError` si no:

```kotlin
class ValidateReminderUseCase @Inject constructor() {
    fun title(title: String): ValidationError? = when {
        title.isBlank() -> ValidationError.EmptyReminderTitle
        title.trim().length > MAX_REMINDER_TITLE_LENGTH -> ValidationError.ReminderTitleTooLong
        else -> null
    }
```

## `@ApplicationScope`: trabajo que no se debe cortar

Las corrutinas de un ViewModel (`viewModelScope`) se cancelan cuando su pantalla se cierra. Hay trabajos que deben terminar aunque el usuario salga: guardar un recordatorio, crear el perfil al iniciar sesión. Para ellos existe un *scope* que vive lo mismo que la app ([`ApplicationScope.kt`](../app/src/main/kotlin/com/example/huellitasya/core/common/ApplicationScope.kt)), creado en [`DataModule.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/di/DataModule.kt):

```kotlin
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

El caso de uso hace `appScope.async { ... }.await()`: el trabajo corre allí y el ViewModel solo espera el resultado.

## Datos: `core/data/`

Aquí está la **implementación** de cada interfaz, con Firebase. Es el único lugar que importa Firebase.

```kotlin
/** `users/{uid}/reminders/{reminderId}` (spec 007). */
internal class FirestoreReminderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ReminderRepository {
```

[`FirestoreReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreReminderRepository.kt). Cómo se leen y escriben los datos se explica en [03 — Firebase y datos](03-firebase-y-datos.md).

## Hilt: inyección de dependencias

Ninguna clase crea sus dependencias. Las pide en su constructor (`@Inject constructor`) y **Hilt** se las entrega. Hilt sabe qué entregar gracias a los módulos de [`core/data/di/`](../app/src/main/kotlin/com/example/huellitasya/core/data/di).

**`@Binds`** dice qué implementación corresponde a cada interfaz ([`DataModule.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/di/DataModule.kt)):

```kotlin
    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: FirestoreReminderRepository): ReminderRepository
```

**`@Provides`** crea objetos que no son nuestros, como Firebase:

```kotlin
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
```

- `@Singleton`: hay una sola instancia en toda la app.
- **Ventaja:** en las pruebas el ViewModel se construye a mano con repositorios falsos, sin cambiar el código.
- Así también se entrega el reloj (`Clock`, en [`TimeModule.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/di/TimeModule.kt)): las pruebas fijan "hoy" en una fecha concreta.

## Navegación tipada

Cada destino es una clase `@Serializable`. Sus propiedades son los argumentos ([`CalendarNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/navigation/CalendarNavigation.kt)):

```kotlin
@Serializable
data class NewReminderDestination(val kind: String) {
    constructor(kind: ReminderKind) : this(kind.name)
}
```

- **Navegar:** `navController.navigate(NewReminderDestination(ReminderKind.VACCINE))`.
- **Leer el argumento:** el ViewModel lo toma del `SavedStateHandle` con el mismo nombre de la propiedad ([`NewReminderViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderViewModel.kt)):

```kotlin
    private val kind: ReminderKind = ReminderKind.valueOf(checkNotNull(savedStateHandle[KIND_ARG]) { "Falta el tipo" })
```

- **Volver atrás:** `navController.popBackStack()`.
- Todas las conexiones entre pantallas están en [`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt).

## Reglas de capas

El compilador no las impone porque todo está en un solo módulo, así que se respetan a mano (spec 000):

1. `feature.*` nunca importa `core.data` ni otra feature: solo usa casos de uso.
2. `core.model`, `core.common` y `core.domain` no importan Android ni Firebase.
3. Firebase solo aparece en `core.data`.

**Por qué:** si mañana se cambia Firebase por otra base de datos, solo cambia `core/data`. Las pantallas y las reglas de negocio no se tocan.
