package com.example.huellitasya.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.model.Pet
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

data class HomeUiState(
    val pets: List<Pet> = emptyList(),
    /** Hasta la primera lectura no se muestra la lista (ni la tarjeta "+"), para no parpadear. */
    val isLoading: Boolean = true,
)

sealed interface HomeEvent {
    data class ShowMessage(val message: UiText) : HomeEvent
}

/** Home (spec 005): las mascotas del usuario en vivo; sin red, desde la caché. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observePets: ObservePetsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePets().collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { HomeUiState(pets = result.data, isLoading = false) }
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(HomeEvent.ShowMessage(result.error.toUiText()))
                    }
                }
            }
        }
    }
}
