package com.example.huellitasya.feature.records.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRecordsUseCase
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordListUiState(
    val pet: Pet? = null,
    val records: List<MedicalRecord> = emptyList(),
    /** Hasta la primera lectura no se muestra la lista (ni la tarjeta "+"), para no parpadear. */
    val isLoading: Boolean = true,
)

sealed interface RecordListEvent {
    data class ShowMessage(val message: UiText) : RecordListEvent

    /** La mascota ya no existe: la Route vuelve a Home. */
    data object PetGone : RecordListEvent
}

/** Lista de una categoría de registros médicos (spec 006, Figma 3193:3626 y 3193:3339), en vivo. */
@HiltViewModel
class RecordListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    observeRecords: ObserveRecordsUseCase,
) : ViewModel() {

    /** Argumentos de `RecordListDestination`. */
    val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }
    val category: RecordCategory = RecordCategory.valueOf(checkNotNull(savedStateHandle[CATEGORY_ARG]) { "Falta la categoría" })

    private val _uiState = MutableStateFlow(RecordListUiState())
    val uiState: StateFlow<RecordListUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecordListEvent>(Channel.BUFFERED)
    val events: Flow<RecordListEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePet(petId).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(pet = result.data) }
                    is AppResult.Failure -> if (result.error == DataError.NotFound) _events.send(RecordListEvent.PetGone)
                }
            }
        }
        viewModelScope.launch {
            observeRecords(petId, category).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(records = result.data, isLoading = false) }
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(RecordListEvent.ShowMessage(result.error.toUiText()))
                    }
                }
            }
        }
    }

    companion object {
        const val PET_ID_ARG = "petId"
        const val CATEGORY_ARG = "category"
    }
}
