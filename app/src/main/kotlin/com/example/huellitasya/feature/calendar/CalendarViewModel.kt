package com.example.huellitasya.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRemindersUseCase
import com.example.huellitasya.core.model.Pet
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

/** Recordatorios próximos de una mascota, con el índice de su color (`petColors`). */
data class UpcomingGroup(val pet: Pet, val colorIndex: Int, val reminders: List<Reminder>)

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    /** Día del mes mostrado → índice de color de la mascota de su primer recordatorio. */
    val dayMarks: Map<Int, Int> = emptyMap(),
    val upcoming: List<UpcomingGroup> = emptyList(),
    /** Hasta la primera lectura no se muestra la tarjeta "+", para no parpadear. */
    val isLoading: Boolean = true,
)

sealed interface CalendarAction {
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
}

sealed interface CalendarEvent {
    data class ShowMessage(val message: UiText) : CalendarEvent
}

/** Calendario del mes y "Próximos eventos" de todas las mascotas (spec 007, Figma 3241:1074). */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    observePets: ObservePetsUseCase,
    observeReminders: ObserveRemindersUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LocalDate.now(clock).let { today -> CalendarUiState(month = YearMonth.from(today), today = today) },
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _events = Channel<CalendarEvent>(Channel.BUFFERED)
    val events: Flow<CalendarEvent> = _events.receiveAsFlow()

    /** Última lectura completa; se guarda para recalcular las marcas al cambiar de mes. */
    private var pets: List<Pet> = emptyList()
    private var reminders: List<Reminder> = emptyList()

    init {
        viewModelScope.launch {
            combine(observePets(), observeReminders(), ::Pair).collect { (petsResult, remindersResult) ->
                val error = (petsResult as? AppResult.Failure)?.error ?: (remindersResult as? AppResult.Failure)?.error
                if (error != null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(CalendarEvent.ShowMessage(error.toUiText()))
                    return@collect
                }
                pets = (petsResult as AppResult.Success).data
                reminders = (remindersResult as AppResult.Success).data
                refresh()
            }
        }
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.PreviousMonth -> _uiState.update { it.copy(month = it.month.minusMonths(1)) }
            CalendarAction.NextMonth -> _uiState.update { it.copy(month = it.month.plusMonths(1)) }
        }
        refresh()
    }

    private fun refresh() {
        // El color de cada mascota es su posición en la lista, que viene en orden de registro.
        val colorByPet = pets.withIndex().associate { (index, pet) -> pet.id to index }
        // Un recordatorio de una mascota ya eliminada no se muestra.
        val visible = reminders.filter { it.petId in colorByPet }
        val now = LocalDateTime.now(clock)
        _uiState.update { state ->
            state.copy(
                today = now.toLocalDate(),
                dayMarks = visible
                    .filter { YearMonth.from(it.dateTime) == state.month }
                    .groupBy { it.dateTime.dayOfMonth }
                    .mapValues { (_, dayReminders) -> colorByPet.getValue(dayReminders.first().petId) },
                upcoming = pets.mapIndexedNotNull { index, pet ->
                    visible.filter { it.petId == pet.id && !it.dateTime.isBefore(now) }
                        .takeIf { it.isNotEmpty() }
                        ?.let { UpcomingGroup(pet, index, it) }
                },
                isLoading = false,
            )
        }
    }
}
