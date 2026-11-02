package com.example.huellitasya.feature.calendar.newreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.CreateReminderUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.validation.ValidateReminderUseCase
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.ReminderDraft
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
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

data class NewReminderUiState(
    val kind: ReminderKind,
    val pets: List<Pet> = emptyList(),
    val selectedPetId: String? = null,
    val vaccineType: VaccineType? = null,
    val vetReason: VetReason? = null,
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    /** La lista de vacunas o motivos del paso 2 está abierta. */
    val isOptionsOpen: Boolean = false,
    val titleFeedback: FieldFeedback? = null,
    val dateTimeFeedback: FieldFeedback? = null,
    val isSaving: Boolean = false,
) {
    /** "Otro" y las opciones "Otros" piden escribir el nombre. */
    val needsTitle: Boolean
        get() = kind == ReminderKind.OTHER || vaccineType == VaccineType.OTHER || vetReason == VetReason.OTHER

    private val hasSubtype: Boolean
        get() = when (kind) {
            ReminderKind.VACCINE -> vaccineType != null
            ReminderKind.VET -> vetReason != null
            ReminderKind.OTHER -> true
        }

    /** "Agregar Recordatorio" queda gris hasta que todo está elegido (spec 007). */
    val isComplete: Boolean
        get() = selectedPetId != null && hasSubtype && (!needsTitle || title.isNotBlank()) && date != null && time != null
}

sealed interface NewReminderAction {
    data class PetSelected(val petId: String) : NewReminderAction
    data object ToggleOptions : NewReminderAction
    data class VaccineSelected(val type: VaccineType) : NewReminderAction
    data class VetReasonSelected(val reason: VetReason) : NewReminderAction
    data class TitleChanged(val title: String) : NewReminderAction
    data class DateChanged(val date: LocalDate) : NewReminderAction
    data class TimeChanged(val time: LocalTime) : NewReminderAction
    data object Save : NewReminderAction
}

sealed interface NewReminderEvent {
    data class ShowMessage(val message: UiText) : NewReminderEvent

    /** El recordatorio quedó guardado: la Route vuelve al Calendario. */
    data object Saved : NewReminderEvent
}

/** "Nuevo recordatorio" de vacuna, cita o "Otro" (spec 007, Figma 3362:1554, 3367:1782 y 3367:2019). */
@HiltViewModel
class NewReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePets: ObservePetsUseCase,
    private val createReminder: CreateReminderUseCase,
    private val validateReminder: ValidateReminderUseCase,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) : ViewModel() {

    /** Argumento de `NewReminderDestination`: el nombre de [ReminderKind]. */
    private val kind: ReminderKind = ReminderKind.valueOf(checkNotNull(savedStateHandle[KIND_ARG]) { "Falta el tipo" })

    private val _uiState = MutableStateFlow(NewReminderUiState(kind = kind))
    val uiState: StateFlow<NewReminderUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewReminderEvent>(Channel.BUFFERED)
    val events: Flow<NewReminderEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePets().collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { state ->
                        val pets = result.data
                        // Con una sola mascota no hay nada que elegir; si la elegida se eliminó, se descarta.
                        val selected = state.selectedPetId?.takeIf { id -> pets.any { it.id == id } }
                            ?: pets.singleOrNull()?.id
                        state.copy(pets = pets, selectedPetId = selected)
                    }
                    is AppResult.Failure -> _events.send(NewReminderEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    fun onAction(action: NewReminderAction) {
        when (action) {
            is NewReminderAction.PetSelected -> _uiState.update { it.copy(selectedPetId = action.petId) }
            NewReminderAction.ToggleOptions -> _uiState.update { it.copy(isOptionsOpen = !it.isOptionsOpen) }
            is NewReminderAction.VaccineSelected -> _uiState.update {
                it.copy(vaccineType = action.type, isOptionsOpen = false, titleFeedback = null)
            }
            is NewReminderAction.VetReasonSelected -> _uiState.update {
                it.copy(vetReason = action.reason, isOptionsOpen = false, titleFeedback = null)
            }
            is NewReminderAction.TitleChanged -> _uiState.update { it.copy(title = action.title, titleFeedback = null) }
            is NewReminderAction.DateChanged -> _uiState.update { it.copy(date = action.date, dateTimeFeedback = null) }
            is NewReminderAction.TimeChanged -> _uiState.update { it.copy(time = action.time, dateTimeFeedback = null) }
            NewReminderAction.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || !state.isComplete) return
        val petId = state.selectedPetId ?: return
        val dateTime = LocalDateTime.of(state.date ?: return, state.time ?: return)

        val titleError = if (state.needsTitle) validateReminder.title(state.title) else null
        val dateTimeError = validateReminder.dateTime(dateTime, LocalDateTime.now(clock))
        if (titleError != null || dateTimeError != null) {
            _uiState.update {
                it.copy(titleFeedback = titleError?.toFieldFeedback(), dateTimeFeedback = dateTimeError?.toFieldFeedback())
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Sin red, Firestore encola la escritura y no termina: se avisa y no se intenta.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(NewReminderEvent.ShowMessage(DataError.Network.toUiText()))
                return@launch
            }
            val draft = ReminderDraft(
                petId = petId,
                kind = kind,
                vaccineType = state.vaccineType.takeIf { kind == ReminderKind.VACCINE },
                vetReason = state.vetReason.takeIf { kind == ReminderKind.VET },
                customTitle = state.title.takeIf { state.needsTitle },
                dateTime = dateTime,
            )
            when (val result = createReminder(draft)) {
                is AppResult.Success -> _events.send(NewReminderEvent.Saved)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(NewReminderEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    companion object {
        const val KIND_ARG = "kind"
    }
}
