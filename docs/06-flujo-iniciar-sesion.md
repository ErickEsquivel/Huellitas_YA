# 06 — Flujo: iniciar sesión

**Qué pasa:** el usuario escribe su correo y contraseña y toca "Iniciar sesión". La app valida los datos y Firebase Auth comprueba la cuenta. La app asegura que exista su perfil en Firestore y cambia de las pantallas de inicio a la app principal. Lo particular es que **la pantalla de login no navega**: la app escucha la sesión y reemplaza todo el conjunto de pantallas por sí sola.

> En los bloques de código, los comentarios que empiezan con `// ←` son explicaciones de esta guía. El resto es el código tal cual está en el proyecto.

## La cadena de llamadas

```text
LoginScreen ─onAction(Submit)─► LoginViewModel.submit()
   │   ├─ ValidateEmailUseCase / ValidatePasswordUseCase
   │   └─ SignInWithEmailUseCase      (@ApplicationScope)
   │         ├─ AuthRepository.signInWithEmail ─► FirebaseAuthRepository ─► Firebase Auth
   │         └─ UserRepository.createIfMissing ─► FirestoreUserRepository ─► Firestore: users/{uid}
   │
   ├─ si falla: AppResult.Failure ─► aviso bajo el campo / Snackbar / "Sin conexión"
   │
   └─ si funciona: Firebase Auth cambia de usuario
         └─► FirebaseAuthRepository.currentUser (Flow) ─► ObserveSessionUseCase
               └─► MainViewModel.uiState = SignedIn ─► HuellitasApp cambia AuthNavHost por MainNavHost
```

## Paso 1. La pantalla envía la acción

[`LoginScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginScreen.kt#L118)

```kotlin
            HuellitasButton(
                text = stringResource(R.string.auth_login_button),
                onClick = {
                    focusManager.clearFocus()                        // ← cierra el teclado
                    onAction(LoginAction.Submit)                     // ← avisa al ViewModel; la pantalla no decide nada
                },
                isLoading = state.isLoading,                         // ← muestra el círculo de carga en el botón
                containerColor = if (state.isComplete) GreenAction else ButtonIncomplete, // ← verde si hay correo y contraseña
```

Cada letra escrita también es una acción:

```kotlin
        HuellitasTextField(
            value = state.email,                                     // ← el campo muestra lo que dice el estado
            onValueChange = { onAction(LoginAction.EmailChanged(it)) }, // ← cada cambio vuelve al ViewModel
            label = stringResource(R.string.auth_field_email),
            status = state.emailFeedback?.status ?: FieldStatus.Default, // ← borde rojo o naranja si hay aviso
            message = state.emailFeedback?.message?.asString(),      // ← el texto del aviso
```

## Paso 2. El ViewModel valida y llama al caso de uso

[`LoginViewModel.kt` (`submit`)](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginViewModel.kt#L81)

```kotlin
    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return                                  // ← ignora toques repetidos

        val emailError = validateEmail(state.email)                  // ← ¿vacío? ¿tiene forma de correo?
        val passwordError = validatePassword.forLogin(state.password) // ← ¿vacía?
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailFeedback = emailError?.toFieldFeedback(),   // ← aviso bajo el correo
                    passwordFeedback = passwordError?.toFieldFeedback(),
                    formFeedback = formFeedbackFor(emailError, passwordError), // ← "Rellene todos los datos para continuar"
                )
            }
            return                                                   // ← no llama a Firebase
        }

        _uiState.update { it.copy(isLoading = true) }                // ← el botón muestra carga
        viewModelScope.launch {
            signInWithEmail(state.email, state.password).onFailure(::showError) // ← si falla, showError; si funciona, no hace nada (ver paso 6)
        }
    }
```

Los errores se muestran según su tipo:

```kotlin
    private fun showError(error: AuthError) {
        _uiState.update { it.copy(isLoading = false) }               // ← quita la carga
        when (error) {
            // Figma muestra el rechazo bajo el campo de contraseña.
            AuthError.InvalidCredentials -> _uiState.update { it.copy(passwordFeedback = error.toFieldFeedback()) } // ← correo o contraseña incorrectos
            AuthError.Network -> viewModelScope.launch { _events.send(LoginEvent.NetworkError) } // ← sin red: muestra el aviso "Sin conexión"
            else -> error.toUiText()?.let { message ->
                viewModelScope.launch { _events.send(LoginEvent.ShowMessage(message)) } // ← otros errores: Snackbar
            }
        }
    }
```

La validación del correo está en el dominio ([`Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt#L149)):

```kotlin
class ValidateEmailUseCase @Inject constructor() {
    operator fun invoke(email: String): ValidationError? {
        val trimmed = email.trim()                                   // ← ignora espacios al inicio y al final
        return when {
            trimmed.isEmpty() -> ValidationError.EmptyEmail
            !EMAIL_REGEX.matches(trimmed) -> ValidationError.InvalidEmail // ← no tiene forma de correo
            else -> null
```

## Paso 3. El caso de uso: autenticar y asegurar el perfil

[`SignInUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/SignInUseCases.kt#L21)

```kotlin
class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,    // ← clave: ver la nota de abajo
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User, AuthError> =
        appScope.async {
            authRepository.signInWithEmail(email.trim(), password)   // ← 1. Firebase Auth comprueba la cuenta
                .onSuccess { userRepository.ensureProfile(it) }      // ← 2. si entró, asegura que exista users/{uid}
        }.await()
}
```

```kotlin
/** Crea `users/{uid}` si falta y copia el correo de Auth si cambió (spec 004, cambio de correo). */
private suspend fun UserRepository.ensureProfile(user: User) {
    createIfMissing(user)                                            // ← crea el perfil la primera vez
    if (user.email.isNotBlank()) syncEmail(user.uid, user.email)     // ← mantiene el correo al día
}
```

**Por qué `@ApplicationScope`:** en cuanto Firebase acepta la cuenta, la app cambia de pantallas (paso 6) y la pantalla de login **se destruye junto con su ViewModel**. Si el trabajo corriera en `viewModelScope`, se cancelaría a mitad y el perfil podría no crearse. En el scope de la aplicación termina igual.

## Paso 4. El repositorio habla con Firebase Auth

[`FirebaseAuthRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseAuthRepository.kt#L30)

```kotlin
    override suspend fun signInWithEmail(email: String, password: String): AppResult<User, AuthError> =
        runAuth {                                                    // ← atrapa errores y los vuelve AuthError
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await() // ← llamada real a Firebase Auth; espera la respuesta
            checkNotNull(result.user).toUser()                       // ← FirebaseUser → nuestro modelo User
        }
```

```kotlin
    private inline fun <T> runAuth(block: () -> T): AppResult<T, AuthError> =
        try {
            AppResult.Success(block())                               // ← todo bien
        } catch (e: CancellationException) {
            throw e                                                  // ← una cancelación no es un error: se deja pasar
        } catch (e: Exception) {
            AppResult.Failure(e.toAuthError())                       // ← p. ej. FirebaseAuthInvalidCredentialsException → AuthError.InvalidCredentials
        }
```

## Paso 5. El perfil en Firestore

[`FirestoreUserRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreUserRepository.kt#L28)

```kotlin
    override suspend fun createIfMissing(user: User): AppResult<Unit, DataError> = runData {
        val document = userDocument(user.uid)                        // ← users/{uid}
        if (!document.get().await().exists()) {                      // ← lee una vez: ¿ya existe?
            document.set(user.toFirestoreMap()).await()              // ← si no, lo crea
        }
    }
```

```kotlin
    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        NAME to name,
        EMAIL to email,
        PHOTO_URL to photoUrl,
        "provider" to provider.name.lowercase(Locale.ROOT),          // ← "email" o "google"
        "createdAt" to FieldValue.serverTimestamp(),                 // ← la hora del servidor
    )
```

## Paso 6. La sesión cambia y la app cambia de pantallas

Nadie llamó a `navigate`. Firebase Auth avisa que hay un usuario nuevo y esa señal recorre la app.

**a) El repositorio escucha la sesión** ([`FirebaseAuthRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseAuthRepository.kt#L24)):

```kotlin
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.toUser()) } // ← Firebase avisa al entrar o salir; null = sin sesión
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
```

**b) El caso de uso lo traduce a un estado de sesión** ([`SessionUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/SessionUseCases.kt)):

```kotlin
    operator fun invoke(): Flow<SessionState> =
        authRepository.currentUser
            .map { user -> if (user == null) SessionState.SignedOut else SessionState.SignedIn(user) } // ← usuario → estado de sesión
            .distinctUntilChanged()                                  // ← solo avisa cuando cambia de verdad
```

**c) `MainViewModel` decide qué mostrar** ([`MainViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/MainViewModel.kt#L47)):

```kotlin
    val uiState: StateFlow<MainUiState> = observeSession()
        // Solo cuenta quién inició sesión: si cambian sus datos (nombre, correo), el grafo no se recrea.
        .distinctUntilChanged { old, new -> old.uid == new.uid }
        .transformLatest { session ->
            when (session) {
                SessionState.Loading -> emit(MainUiState.Loading)    // ← todavía no se sabe: se ve el splash
                SessionState.SignedOut -> emit(MainUiState.SignedOut) // ← sin sesión: pantallas de login
                is SessionState.SignedIn -> {
                    emit(MainUiState.Loading)
                    // Se decide una vez por sesión: registrar la primera mascota no recrea el grafo.
                    emit(MainUiState.SignedIn(startWithIntro = shouldShowPetsIntro())) // ← con sesión: ¿empieza en Presentación (sin mascotas) o en Home?
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading) // ← lo convierte en StateFlow que empieza en Loading
```

**d) `HuellitasApp` reemplaza el NavHost completo** ([`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt#L63)):

```kotlin
    when (uiState) {
        // Al abrir la app, el splash del sistema cubre este estado (ver MainActivity).
        MainUiState.Loading -> Unit
        MainUiState.SignedOut -> AuthNavHost(googleServerClientId)   // ← Bienvenido, Login, Registro…
        is MainUiState.SignedIn -> MainNavHost(googleServerClientId, startWithIntro = uiState.startWithIntro, openCalendarRequest = openCalendarRequest) // ← la app principal con su barra inferior
    }
```

**Por qué así:** al cambiar el `NavHost` entero desaparece el historial del login. "Atrás" desde Home sale de la app en vez de volver a Login. Cerrar sesión funciona igual, en sentido contrario.

## El aviso "Sin conexión"

Todas las pantallas de autenticación están envueltas en `ConnectivityAware` ([`AuthNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/navigation/AuthNavigation.kt#L77)):

```kotlin
        composable<LoginDestination> { entry ->
            val connectivity = authConnectivityViewModel(navController, entry) // ← un ViewModel compartido por todo el grafo de login
            ...
            ConnectivityAware(connectivity) {                        // ← dibuja la pantalla y, encima, el aviso si no hay red
                LoginRoute(
                    ...
                    onNetworkError = connectivity::onNetworkError,   // ← cuando Login recibe AuthError.Network, enciende el aviso
```

[`ConnectivityViewModel`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/connectivity/ConnectivityViewModel.kt) muestra el aviso si `NetworkMonitor` dice que no hay internet o si una pantalla reportó un error de red, y lo quita cuando vuelve la conexión.

## Qué pasa si falla

| Situación | Dónde se detecta | Qué ve el usuario |
|---|---|---|
| Correo o contraseña vacíos | `ValidateEmailUseCase`, `forLogin` | Aviso bajo el campo y "Rellene todos los datos para continuar" |
| Correo sin forma de correo | `ValidateEmailUseCase` | Aviso bajo el correo |
| Correo o contraseña incorrectos | Firebase → `AuthError.InvalidCredentials` | "Correo o contraseña incorrectos" bajo la contraseña |
| Sin internet | Firebase → `AuthError.Network` | Pantalla "Sin conexión" con "Reintentar" |
| Demasiados intentos | `AuthError.TooManyRequests` | Snackbar |

## Archivos involucrados

- [`feature/auth/login/LoginScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginScreen.kt)
- [`feature/auth/login/LoginViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/login/LoginViewModel.kt)
- [`feature/auth/navigation/AuthNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/navigation/AuthNavigation.kt)
- [`feature/auth/connectivity/ConnectivityViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/auth/connectivity/ConnectivityViewModel.kt)
- [`core/domain/validation/Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt)
- [`core/domain/usecase/SignInUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/SignInUseCases.kt)
- [`core/domain/usecase/SessionUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/SessionUseCases.kt)
- [`core/data/repository/FirebaseAuthRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirebaseAuthRepository.kt)
- [`core/data/repository/FirestoreUserRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreUserRepository.kt)
- [`core/data/mapper/FirebaseMappers.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/mapper/FirebaseMappers.kt)
- [`MainViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/MainViewModel.kt)
- [`navigation/HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt)
