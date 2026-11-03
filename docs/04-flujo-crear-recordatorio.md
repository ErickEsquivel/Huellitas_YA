# 04 — Flujo: crear un recordatorio

**Qué pasa:** el usuario toca "+" en el Calendario, elige Vacunas, Veterinario u Otro, llena el formulario y toca "Agregar Recordatorio". La app valida los datos, guarda el recordatorio en Firestore y vuelve al Calendario. El Calendario se actualiza solo, y a la hora indicada llega un aviso al teléfono.

> En los bloques de código, los comentarios que empiezan con `// ←` son explicaciones de esta guía. El resto es el código tal cual está en el proyecto.

## La cadena de llamadas

```text
CalendarRoute ("+") ─► ReminderKindSheet ─► navController.navigate(NewReminderDestination)
   │
NewReminderScreen ─onAction(Save)─► NewReminderViewModel.save()
   │   ├─ ValidateReminderUseCase      (¿nombre y fecha válidos?)
   │   ├─ NetworkMonitor               (¿hay internet?)
   │   └─ CreateReminderUseCase        (@ApplicationScope)
   │         ├─ AuthRepository.currentUser   (¿quién es el usuario?)
   │         └─ ReminderRepository.createReminder
   │               └─ FirestoreReminderRepository ─► Firestore: users/{uid}/reminders/{id}.set(...)
   │
   ◄─ AppResult.Success ─ NewReminderEvent.Saved ─► (permiso de avisos) ─► popBackStack()

Vuelta en vivo:
Firestore ─addSnapshotListener─► observeReminders (Flow) ─► CalendarViewModel ─► CalendarUiState ─► CalendarScreen
                                                       └─► SyncReminderNotificationsUseCase ─► WorkManager ─► aviso
```

## Paso 1. El "+" abre la hoja de opciones

[`CalendarScreen.kt` (CalendarRoute)](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/CalendarScreen.kt#L91)

```kotlin
    var showKindSheet by rememberSaveable { mutableStateOf(false) } // ← estado local de la pantalla: ¿se ve la hoja? Sobrevive a girar el teléfono
    ...
    CalendarScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onAddClick = { showKindSheet = true },                       // ← tocar "+" solo abre la hoja; no llama al ViewModel
        onReminderClick = onReminderClick,
        onNotificationsClick = onNotificationsClick,
    )

    if (showKindSheet) {                                             // ← si es true, Compose dibuja la hoja
        ReminderKindSheet(
            onKindSelected = { kind ->                               // ← el usuario eligió Vacunas, Veterinario u Otro
                showKindSheet = false                                // ← cierra la hoja
                onNewReminderClick(kind)                             // ← avisa hacia afuera; la Route no sabe navegar
            },
            onDismiss = { showKindSheet = false },                   // ← tocar fuera cierra la hoja
        )
    }
```

## Paso 2. La navegación abre "Nuevo recordatorio"

[`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt#L163)

```kotlin
            calendarScreen(
                onNewReminderClick = { navController.navigate(NewReminderDestination(it)) }, // ← navega llevando el tipo elegido como argumento
                ...
            )
            ...
            newReminderScreen(
                onBack = { navController.popBackStack() },           // ← "<" vuelve al Calendario
                onSaved = { navController.popBackStack() },          // ← al guardar, también vuelve al Calendario
            )
```

El destino es una clase con el argumento ([`CalendarNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/navigation/CalendarNavigation.kt)):

```kotlin
@Serializable                                                        // ← Navigation Compose la convierte en la ruta de la pantalla
data class NewReminderDestination(val kind: String) {                // ← "kind" es el nombre del argumento
    constructor(kind: ReminderKind) : this(kind.name)                // ← permite pasar el enum; se guarda como texto ("VACCINE")
}
```

## Paso 3. El estado del formulario

[`NewReminderViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderViewModel.kt#L37)

```kotlin
data class NewReminderUiState(                                       // ← todo lo que la pantalla necesita para dibujarse
    val kind: ReminderKind,                                          // ← Vacunas, Veterinario u Otro (viene de la navegación)
    val pets: List<Pet> = emptyList(),                               // ← las mascotas, para las tarjetas del paso 1
    val selectedPetId: String? = null,                               // ← la mascota elegida (null = ninguna todavía)
    val vaccineType: VaccineType? = null,                            // ← la vacuna elegida, si es de tipo Vacunas
    val vetReason: VetReason? = null,                                // ← el motivo elegido, si es de tipo Veterinario
    val title: String = "",                                          // ← el nombre escrito (Otro u "Otros")
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    /** La lista de vacunas o motivos del paso 2 está abierta. */
    val isOptionsOpen: Boolean = false,
    val titleFeedback: FieldFeedback? = null,                        // ← aviso bajo el campo nombre, si hay error
    val dateTimeFeedback: FieldFeedback? = null,                     // ← aviso bajo fecha y hora, si hay error
    val isSaving: Boolean = false,                                   // ← true mientras guarda: muestra "Guardando…"
) {
    /** "Otro" y las opciones "Otros" piden escribir el nombre. */
    val needsTitle: Boolean
        get() = kind == ReminderKind.OTHER || vaccineType == VaccineType.OTHER || vetReason == VetReason.OTHER // ← se calcula, no se guarda

    ...

    /** "Agregar Recordatorio" queda gris hasta que todo está elegido (spec 007). */
    val isComplete: Boolean
        get() = selectedPetId != null && hasSubtype && (!needsTitle || title.isNotBlank()) && date != null && time != null // ← el botón usa este valor para habilitarse
}
```

El tipo llega desde la navegación por el `SavedStateHandle`:

```kotlin
    private val kind: ReminderKind = ReminderKind.valueOf(checkNotNull(savedStateHandle[KIND_ARG]) { "Falta el tipo" }) // ← lee "kind" y lo vuelve enum
```

## Paso 4. Cada toque es una acción

Cada cosa que el usuario toca llega como una acción y produce un estado nuevo ([`NewReminderViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderViewModel.kt#L123)):

```kotlin
    fun onAction(action: NewReminderAction) {
        when (action) {                                              // ← una sola puerta de entrada para todo lo que toca el usuario
            is NewReminderAction.PetSelected -> _uiState.update { it.copy(selectedPetId = action.petId) } // ← tarjeta de mascota
            NewReminderAction.ToggleOptions -> _uiState.update { it.copy(isOptionsOpen = !it.isOptionsOpen) } // ← abre o cierra la lista
            is NewReminderAction.VaccineSelected -> _uiState.update {
                it.copy(vaccineType = action.type, isOptionsOpen = false, titleFeedback = null) // ← elige y cierra la lista
            }
            ...
            is NewReminderAction.DateChanged -> _uiState.update { it.copy(date = action.date, dateTimeFeedback = null) } // ← al cambiar la fecha se borra el aviso anterior
            is NewReminderAction.TimeChanged -> _uiState.update { it.copy(time = action.time, dateTimeFeedback = null) }
            NewReminderAction.Save -> save()                         // ← "Agregar Recordatorio"
        }
    }
```

En la pantalla, el botón envía esa acción ([`NewReminderScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderScreen.kt#L218)):

```kotlin
                HuellitasButton(
                    text = stringResource(R.string.new_reminder_save),
                    onClick = {
                        focusManager.clearFocus()                    // ← cierra el teclado
                        onAction(NewReminderAction.Save)             // ← avisa al ViewModel
                    },
                    enabled = state.isComplete,                      // ← deshabilitado si falta algo
                    containerColor = if (state.isComplete) GreenPrimary else ButtonIncomplete, // ← verde o gris
```

## Paso 5. Guardar: validar, comprobar la red y llamar al caso de uso

[`NewReminderViewModel.kt` (`save`)](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderViewModel.kt#L140)

```kotlin
    private fun save() {
        val state = _uiState.value                                   // ← foto del estado actual
        if (state.isSaving || !state.isComplete) return              // ← evita guardar dos veces o con datos faltantes
        val petId = state.selectedPetId ?: return
        val dateTime = LocalDateTime.of(state.date ?: return, state.time ?: return) // ← junta fecha y hora

        val titleError = if (state.needsTitle) validateReminder.title(state.title) else null // ← ¿nombre de 1 a 50 caracteres?
        val dateTimeError = validateReminder.dateTime(dateTime, LocalDateTime.now(clock))    // ← ¿está en el futuro? (el reloj es inyectado)
        if (titleError != null || dateTimeError != null) {           // ← si algo está mal…
            _uiState.update {
                it.copy(titleFeedback = titleError?.toFieldFeedback(), dateTimeFeedback = dateTimeError?.toFieldFeedback()) // ← …muestra el aviso bajo el campo
            }
            return                                                   // ← y no guarda
        }

        _uiState.update { it.copy(isSaving = true) }                 // ← muestra "Guardando…"
        viewModelScope.launch {                                      // ← corrutina: el trabajo sigue sin congelar la pantalla
            // Sin red, Firestore encola la escritura y no termina: se avisa y no se intenta.
            if (!networkMonitor.isOnline.first()) {                  // ← lee una vez si hay internet
                _uiState.update { it.copy(isSaving = false) }
                _events.send(NewReminderEvent.ShowMessage(DataError.Network.toUiText())) // ← "Sin conexión. Revisa tu internet"
                return@launch
            }
            val draft = ReminderDraft(                               // ← los datos ya validados, listos para guardar
                petId = petId,
                kind = kind,
                vaccineType = state.vaccineType.takeIf { kind == ReminderKind.VACCINE }, // ← solo si es vacuna
                vetReason = state.vetReason.takeIf { kind == ReminderKind.VET },         // ← solo si es cita
                customTitle = state.title.takeIf { state.needsTitle },                   // ← solo si se pide nombre
                dateTime = dateTime,
            )
            when (val result = createReminder(draft)) {              // ← llama al caso de uso y espera el resultado
                is AppResult.Success -> _events.send(NewReminderEvent.Saved)            // ← éxito: evento de una vez
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }                       // ← quita "Guardando…"
                    _events.send(NewReminderEvent.ShowMessage(result.error.toUiText())) // ← muestra el error
                }
            }
        }
    }
```

La validación está en el dominio ([`Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt#L211)):

```kotlin
class ValidateReminderUseCase @Inject constructor() {
    fun title(title: String): ValidationError? = when {
        title.isBlank() -> ValidationError.EmptyReminderTitle                                   // ← vacío o solo espacios
        title.trim().length > MAX_REMINDER_TITLE_LENGTH -> ValidationError.ReminderTitleTooLong // ← más de 50 caracteres
        else -> null                                                                            // ← null = está bien
    }

    /** Debe quedar en el futuro: un recordatorio de una hora que ya pasó no avisaría nunca. */
    fun dateTime(dateTime: LocalDateTime, now: LocalDateTime): ValidationError? =
        if (dateTime.isAfter(now)) null else ValidationError.ReminderInPast
}
```

## Paso 6. El caso de uso

[`ReminderUseCases.kt` (`CreateReminderUseCase`)](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/ReminderUseCases.kt#L62)

```kotlin
class CreateReminderUseCase @Inject constructor(                     // ← Hilt lo crea y le entrega estas dependencias
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository,              // ← la interfaz, no Firestore
    @param:ApplicationScope private val appScope: CoroutineScope,    // ← scope que vive lo mismo que la app
) {
    suspend operator fun invoke(draft: ReminderDraft): AppResult<String, DataError> = appScope.async { // ← corre fuera del ViewModel: no se corta si la pantalla se cierra
        val uid = authRepository.currentUser.first()?.uid            // ← el id del usuario con sesión
            ?: return@async AppResult.Failure(DataError.PermissionDenied) // ← sin sesión no se guarda
        val id = reminderRepository.newReminderId(uid)               // ← genera el id del documento
        val normalized = draft.copy(customTitle = draft.customTitle?.trim()?.takeIf { it.isNotEmpty() }) // ← quita espacios sobrantes del nombre
        reminderRepository.createReminder(uid, id, normalized).map { id } // ← guarda y, si salió bien, devuelve el id
    }.await()                                                        // ← el ViewModel espera aquí el resultado
}
```

## Paso 7. El repositorio escribe en Firestore

[`FirestoreReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreReminderRepository.kt#L45)

```kotlin
    override suspend fun createReminder(uid: String, reminderId: String, draft: ReminderDraft): AppResult<Unit, DataError> =
        runData {                                                    // ← atrapa errores y los devuelve como AppResult.Failure
            val data = buildMap {                                    // ← el documento como mapa campo → valor
                put(PET_ID, draft.petId)
                put(KIND, draft.kind.wireValue())                    // ← enum → texto: VACCINE → "vaccine"
                subtypeValue(draft.vaccineType, draft.vetReason)?.let { put(SUBTYPE, it) } // ← "rabies", "general"…; no va en "Otro"
                draft.customTitle?.let { put(TITLE, it) }            // ← el nombre, solo si hay
                put(DATE_TIME, draft.dateTime.toTimestamp())         // ← LocalDateTime → Timestamp de Firebase
                put(CREATED_AT, FieldValue.serverTimestamp())        // ← la hora la pone el servidor
                put(UPDATED_AT, FieldValue.serverTimestamp())
            }
            remindersCollection(uid).document(reminderId).set(data).await() // ← escribe users/{uid}/reminders/{id} y espera la confirmación
        }
```

[`FirebaseMappers.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/mapper/FirebaseMappers.kt#L185):

```kotlin
internal fun LocalDateTime.toTimestamp(): Timestamp = Timestamp(Date.from(atZone(ZoneId.systemDefault()).toInstant())) // ← la fecha local se guarda como un instante en la zona del teléfono
```

Antes de aceptar la escritura, Firestore revisa la regla de `reminders` en [`firestore.rules`](../firebase/firestore.rules#L108): que el usuario sea el dueño, que los campos sean válidos, que la mascota exista y que la hora la ponga el servidor. Si algo no se cumple, `set` falla con `PERMISSION_DENIED`, que llega como `DataError.PermissionDenied`.

## Paso 8. Volver al Calendario y pedir el permiso de avisos

[`NewReminderScreen.kt` (NewReminderRoute)](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderScreen.kt#L96)

```kotlin
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onSaved() } // ← pide el permiso y, responda lo que responda, vuelve

    ObserveAsEvents(viewModel.events) { event ->                     // ← escucha los eventos de una sola vez
        when (event) {
            is NewReminderEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context)) // ← muestra el mensaje abajo
            }
            NewReminderEvent.Saved -> if (                           // ← se guardó bien
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&            // ← Android 13 o más pide permiso para notificar…
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED // ← …y todavía no lo tiene
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) // ← muestra "¿Permitir notificaciones?"
            } else {
                onSaved()                                            // ← vuelve al Calendario (popBackStack)
            }
        }
    }
```

## Paso 9. El Calendario se actualiza solo

No hace falta "recargar". El Calendario ya escuchaba los recordatorios en vivo ([`CalendarViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/CalendarViewModel.kt#L70)):

```kotlin
    init {                                                           // ← se ejecuta al crear el ViewModel
        viewModelScope.launch {
            combine(observePets(), observeReminders(), ::Pair).collect { (petsResult, remindersResult) -> // ← escucha mascotas y recordatorios a la vez; se repite con cada cambio
                val error = (petsResult as? AppResult.Failure)?.error ?: (remindersResult as? AppResult.Failure)?.error
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(CalendarEvent.ShowMessage(error.toUiText()))
                    return@collect
                }
                pets = (petsResult as AppResult.Success).data
                reminders = (remindersResult as AppResult.Success).data
                refresh()                                            // ← recalcula los anillos del mes y "Próximos eventos"
            }
        }
    }
```

Por dentro, `observeReminders()` usa el `addSnapshotListener` del repositorio (ver [03 — Firebase y datos](03-firebase-y-datos.md#leer-en-vivo-callbackflow--addsnapshotlistener)). En cuanto `set` guarda el documento, Firestore avisa al *listener*, llega una lista nueva y `refresh()` produce un `CalendarUiState` nuevo:

```kotlin
    private fun refresh() {
        // El color de cada mascota es su posición en la lista, que viene en orden de registro.
        val colorByPet = pets.withIndex().associate { (index, pet) -> pet.id to index } // ← id de mascota → número de color
        // Un recordatorio de una mascota ya eliminada no se muestra.
        val visible = reminders.filter { it.petId in colorByPet }
        val now = LocalDateTime.now(clock)
        _uiState.update { state ->
            state.copy(
                today = now.toLocalDate(),
                dayMarks = visible
                    .filter { YearMonth.from(it.dateTime) == state.month }                          // ← solo los del mes que se ve
                    .groupBy { it.dateTime.dayOfMonth }                                            // ← agrupados por día
                    .mapValues { (_, dayReminders) -> colorByPet.getValue(dayReminders.first().petId) }, // ← el color del anillo de cada día
                upcoming = pets.mapIndexedNotNull { index, pet ->                                  // ← "Próximos eventos", por mascota
                    visible.filter { it.petId == pet.id && !it.dateTime.isBefore(now) }            // ← solo los que no pasaron
                        .takeIf { it.isNotEmpty() }
                        ?.let { UpcomingGroup(pet, index, it) }
                },
                isLoading = false,
            )
        }
    }
```

`CalendarScreen` recibe el estado nuevo y Compose vuelve a dibujar solo lo que cambió.

## Paso 10. El aviso en el teléfono

[`MainViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/MainViewModel.kt#L41) lanza una vez la sincronización de avisos, que dura mientras la app esté abierta:

```kotlin
    init {
        // Avisos de los recordatorios al día mientras la app está abierta (spec 007).
        viewModelScope.launch { syncReminderNotifications() }        // ← escucha mascotas y recordatorios y llama al notificador con cada cambio
    }
```

El caso de uso arma la lista de recordatorios futuros con el nombre de su mascota y se la pasa al notificador ([`WorkManagerReminderNotifier.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/WorkManagerReminderNotifier.kt#L29)):

```kotlin
    override fun sync(reminders: List<ScheduledReminder>) {
        val workManager = WorkManager.getInstance(context)
        // WorkManager ejecuta sus operaciones en orden: primero se cancela todo y luego se programa
        // la lista actual, así no sobrevive el aviso de un recordatorio eliminado o cambiado.
        workManager.cancelAllWorkByTag(TAG)                          // ← borra los avisos programados antes
        val now = LocalDateTime.now(clock)
        reminders.forEach { (reminder, petName) ->
            val delay = Duration.between(now, reminder.dateTime)     // ← cuánto falta para la hora del recordatorio
            if (delay.isNegative) return@forEach                     // ← si ya pasó, no se programa
            val request = OneTimeWorkRequestBuilder<ReminderWorker>() // ← un trabajo que ejecutará ReminderWorker
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS) // ← que espere hasta esa hora
                .addTag(TAG)
                .setInputData(                                       // ← los datos que el aviso necesita para su texto
                    workDataOf(
                        ReminderWorker.KEY_ID to reminder.id,
                        ...
                        ReminderWorker.KEY_PET_NAME to petName,
                        ReminderWorker.KEY_TIME to reminder.dateTime.toLocalTime().toString(),
                    ),
                )
                .build()
            workManager.enqueueUniqueWork("reminder-${reminder.id}", ExistingWorkPolicy.REPLACE, request) // ← uno por recordatorio; sobrevive al reinicio del teléfono
        }
    }
```

A la hora indicada, [`ReminderWorker`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/ReminderWorker.kt) muestra "Vacuna contra el parvovirus — Maximiliano Tercero · 01:35 PM". Al tocar el aviso se abre la app en el Calendario.

## Qué pasa si falla

| Situación | Dónde se detecta | Qué ve el usuario |
|---|---|---|
| Falta un dato | `isComplete` en el estado | El botón queda gris y no se puede tocar |
| Hora pasada o nombre muy largo | `ValidateReminderUseCase` en `save()` | Aviso naranja bajo el campo ("Elige una fecha y hora futuras") |
| Sin internet | `NetworkMonitor` en `save()` | Snackbar "Sin conexión. Revisa tu internet" |
| Firebase rechaza (reglas, red) | `runData` → `AppResult.Failure` | Snackbar con el mensaje del error |
| El usuario cierra la pantalla mientras guarda | `@ApplicationScope` | El guardado termina igual |

## Archivos involucrados

- [`feature/calendar/CalendarScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/CalendarScreen.kt)
- [`feature/calendar/components/ReminderKindSheet.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/components/ReminderKindSheet.kt)
- [`feature/calendar/navigation/CalendarNavigation.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/navigation/CalendarNavigation.kt)
- [`navigation/HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt)
- [`feature/calendar/newreminder/NewReminderScreen.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderScreen.kt)
- [`feature/calendar/newreminder/NewReminderViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/newreminder/NewReminderViewModel.kt)
- [`core/domain/validation/Validators.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation/Validators.kt)
- [`core/domain/usecase/ReminderUseCases.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase/ReminderUseCases.kt)
- [`core/domain/repository/ReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository/ReminderRepository.kt)
- [`core/data/repository/FirestoreReminderRepository.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository/FirestoreReminderRepository.kt)
- [`core/data/mapper/FirebaseMappers.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/mapper/FirebaseMappers.kt)
- [`feature/calendar/CalendarViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar/CalendarViewModel.kt)
- [`MainViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/MainViewModel.kt)
- [`core/data/notifications/WorkManagerReminderNotifier.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/WorkManagerReminderNotifier.kt)
- [`core/data/notifications/ReminderWorker.kt`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications/ReminderWorker.kt)
- [`firebase/firestore.rules`](../firebase/firestore.rules)
