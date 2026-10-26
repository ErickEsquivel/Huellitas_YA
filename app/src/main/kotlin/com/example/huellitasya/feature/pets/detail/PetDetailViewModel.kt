package com.example.huellitasya.feature.pets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRecordCountsUseCase
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

data class PetDetailUiState(
    /** `null` mientras carga. */
    val pet: Pet? = null,
    /** Archivos de cada categoría de "Registros Medicos" (spec 006). */
    val recordCounts: Map<RecordCategory, Int> = emptyMap(),
)

sealed interface PetDetailEvent {
    data class ShowMessage(val message: UiText) : PetDetailEvent

    /** La mascota ya no existe (se eliminó, quizá desde otro dispositivo): la Route vuelve a Home. */
    data object PetGone : PetDetailEvent
}

/** Perfil de la mascota (spec 005, Figma 54:30): escucha su documento en vivo. */
@HiltViewModel
class PetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    observeRecordCounts: ObserveRecordCountsUseCase,
) : ViewModel() {

    /** Argumento de `PetDetailDestination`. */
    val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }

    private val _uiState = MutableStateFlow(PetDetailUiState())
    val uiState: StateFlow<PetDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PetDetailEvent>(Channel.BUFFERED)
    val events: Flow<PetDetailEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePet(petId).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(pet = result.data) }
                    is AppResult.Failure -> _events.send(
                        if (result.error == DataError.NotFound) {
                            PetDetailEvent.PetGone
                        } else {
                            PetDetailEvent.ShowMessage(result.error.toUiText())
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            // Un error al contar no interrumpe el perfil: las tarjetas siguen en 0.
            observeRecordCounts(petId).collect { result ->
                if (result is AppResult.Success) _uiState.update { it.copy(recordCounts = result.data) }
            }
        }
    }

    companion object {
        const val PET_ID_ARG = "petId"
    }
}
