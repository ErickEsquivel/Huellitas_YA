package com.example.huellitasya.feature.calendar.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.DeleteReminderUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveReminderUseCase
import com.example.huellitasya.core.domain.usecase.UpdateReminderUseCase
import com.example.huellitasya.core.domain.validation.ValidateReminderUseCase
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class ReminderDetailUiState(
    val reminder: Reminder? = null,
    val pet: Pet? = null,
    /** Fecha y hora en edición: arrancan con las guardadas. */
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val dateTimeFeedback: FieldFeedback? = null,
    val showDeleteDialog: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val isBusy: Boolean get() = isUpdating || isDeleting

    /** "Actualizar información" se activa solo con cambios (como Editar mascota). */
    val hasChanges: Boolean
        get() = reminder != null && date != null && time != null && LocalDateTime.of(date, time) != reminder.dateTime
}

sealed interface ReminderDetailAction {
    data class DateChanged(val date: LocalDate) : ReminderDetailAction
    data class TimeChanged(val time: LocalTime) : ReminderDetailAction
    data object Update : ReminderDetailAction
    data object DeleteClick : ReminderDetailAction
    data object DismissDelete : ReminderDetailAction
    data object ConfirmDelete : ReminderDetailAction
}

sealed interface ReminderDetailEvent {
    data class ShowMessage(val message: UiText) : ReminderDetailEvent

    /** Se guardó, se eliminó o ya no existe: la Route vuelve atrás (una sola vez). */
    data object Close : ReminderDetailEvent
}

/** Detalle de un recordatorio (spec 007, Figma 3376:1050): cambiar la fecha y la hora, o eliminarlo. */
@HiltViewModel
class ReminderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeReminder: ObserveReminderUseCase,
    observePets: ObservePetsUseCase,
    private val updateReminder: UpdateReminderUseCase,
    private val deleteReminder: DeleteReminderUseCase,
    private val validateReminder: ValidateReminderUseCase,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) : ViewModel() {

    /** Argumento de `ReminderDetailDestination`. */
    private val reminderId: String = checkNotNull(savedStateHandle[REMINDER_ID_ARG]) { "Falta el recordatorio" }

    private val _uiState = MutableStateFlow(ReminderDetailUiState())
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReminderDetailEvent>(Channel.BUFFERED)
    val events: Flow<ReminderDetailEvent> = _events.receiveAsFlow()

    /** Se sale una sola vez, aunque lleguen a la vez el fin del borrado y el `NotFound` del listener. */
    private var isLeaving = false

    private var pets: List<Pet> = emptyList()

    init {
        viewModelScope.launch {
            observePets().collect { result ->
                if (result is AppResult.Success) {
                    pets = result.data
                    _uiState.update { state -> state.copy(pet = state.reminder?.let { r -> pets.firstOrNull { it.id == r.petId } }) }
                }
            }
        }
        viewModelScope.launch {
            observeReminder(reminderId).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { state ->
                        val reminder = result.data
                        // Si el usuario no estaba editando, los campos siguen a lo guardado.
                        val keepEdits = state.hasChanges
                        state.copy(
                            reminder = reminder,
                            pet = pets.firstOrNull { it.id == reminder.petId },
                            date = if (keepEdits) state.date else reminder.dateTime.toLocalDate(),
                            time = if (keepEdits) state.time else reminder.dateTime.toLocalTime(),
                        )
                    }
                    is AppResult.Failure -> when {
                        // Al eliminarlo aquí, el listener avisa antes de que termine el borrado.
                        result.error == DataError.NotFound -> if (!_uiState.value.isDeleting) leave()
                        else -> _events.send(ReminderDetailEvent.ShowMessage(result.error.toUiText()))
                    }
                }
            }
        }
    }

    fun onAction(action: ReminderDetailAction) {
        when (action) {
            is ReminderDetailAction.DateChanged -> _uiState.update { it.copy(date = action.date, dateTimeFeedback = null) }
            is ReminderDetailAction.TimeChanged -> _uiState.update { it.copy(time = action.time, dateTimeFeedback = null) }
            ReminderDetailAction.Update -> update()
            ReminderDetailAction.DeleteClick -> _uiState.update {
                if (it.isBusy || it.reminder == null) it else it.copy(showDeleteDialog = true)
            }
            ReminderDetailAction.DismissDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            ReminderDetailAction.ConfirmDelete -> delete()
        }
    }

    private fun update() {
        val state = _uiState.value
        if (state.isBusy || !state.hasChanges) return
        val dateTime = LocalDateTime.of(state.date ?: return, state.time ?: return)
        validateReminder.dateTime(dateTime, LocalDateTime.now(clock))?.let { error ->
            _uiState.update { it.copy(dateTimeFeedback = error.toFieldFeedback()) }
            return
        }
        _uiState.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            if (!isOnline()) {
                _uiState.update { it.copy(isUpdating = false) }
                return@launch
            }
            when (val result = updateReminder(reminderId, dateTime)) {
                is AppResult.Success -> leave()
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.send(ReminderDetailEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    private fun delete() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            if (!isOnline()) {
                _uiState.update { it.copy(isDeleting = false) }
                return@launch
            }
            when (val result = deleteReminder(reminderId)) {
                is AppResult.Success -> leave()
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(ReminderDetailEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    private suspend fun leave() {
        if (isLeaving) return
        isLeaving = true
        _events.send(ReminderDetailEvent.Close)
    }

    /** Sin red, Firestore encola la escritura y no termina: se avisa y no se intenta. */
    private suspend fun isOnline(): Boolean {
        val online = networkMonitor.isOnline.first()
        if (!online) _events.send(ReminderDetailEvent.ShowMessage(DataError.Network.toUiText()))
        return online
    }

    companion object {
        const val REMINDER_ID_ARG = "reminderId"
    }
}
