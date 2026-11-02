package com.example.huellitasya.feature.calendar.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveUpcomingRemindersUseCase
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** Un recordatorio próximo con el índice de color de su mascota (`petColors`). */
data class NotificationItem(val reminder: Reminder, val colorIndex: Int)

data class NotificationsUiState(
    val items: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface NotificationsEvent {
    data class ShowMessage(val message: UiText) : NotificationsEvent
}

/** Próximos recordatorios de todas las mascotas, por fecha (spec 007, Figma 3263:914). */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    observePets: ObservePetsUseCase,
    observeUpcomingReminders: ObserveUpcomingRemindersUseCase,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificationsEvent>(Channel.BUFFERED)
    val events: Flow<NotificationsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(observePets(), observeUpcomingReminders(now = { LocalDateTime.now(clock) }), ::Pair)
                .collect { (petsResult, remindersResult) ->
                    if (petsResult is AppResult.Failure || remindersResult is AppResult.Failure) {
                        val error = (petsResult as? AppResult.Failure)?.error ?: (remindersResult as AppResult.Failure).error
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(NotificationsEvent.ShowMessage(error.toUiText()))
                        return@collect
                    }
                    // El color de cada mascota es su posición, en orden de registro, como en el Calendario.
                    val colorByPet = (petsResult as AppResult.Success).data.withIndex().associate { (index, pet) -> pet.id to index }
                    val items = (remindersResult as AppResult.Success).data.mapNotNull { reminder ->
                        colorByPet[reminder.petId]?.let { NotificationItem(reminder, it) }
                    }
                    _uiState.update { NotificationsUiState(items = items, isLoading = false) }
                }
        }
    }
}
