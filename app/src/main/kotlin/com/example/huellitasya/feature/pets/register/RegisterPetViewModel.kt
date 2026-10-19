package com.example.huellitasya.feature.pets.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.RegisterPetUseCase
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormState
import com.example.huellitasya.feature.pets.form.PetFormValidator
import com.example.huellitasya.feature.pets.form.reduce
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
import javax.inject.Inject

data class RegisterPetUiState(
    val form: PetFormState = PetFormState(),
    /** Overlay "Guardando..." (Figma 3234:1022). */
    val isSaving: Boolean = false,
)

sealed interface RegisterPetAction {
    data class Form(val action: PetFormAction) : RegisterPetAction
    data object Save : RegisterPetAction
}

sealed interface RegisterPetEvent {
    data class ShowMessage(val message: UiText) : RegisterPetEvent

    /** La mascota quedó guardada: la Route vuelve a Home. */
    data object PetSaved : RegisterPetEvent
}

/** "¡Conozcamos a tu mascota!" (spec 005): valida, sube la foto (si hay) y crea la mascota. */
@HiltViewModel
class RegisterPetViewModel @Inject constructor(
    private val registerPet: RegisterPetUseCase,
    private val validator: PetFormValidator,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterPetUiState())
    val uiState: StateFlow<RegisterPetUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterPetEvent>(Channel.BUFFERED)
    val events: Flow<RegisterPetEvent> = _events.receiveAsFlow()

    fun onAction(action: RegisterPetAction) {
        when (action) {
            is RegisterPetAction.Form -> _uiState.update {
                if (it.isSaving) it else it.copy(form = it.form.reduce(action.action))
            }
            RegisterPetAction.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val draft = when (val result = validator.validate(state.form)) {
            is PetFormValidator.Result.Invalid -> {
                _uiState.update { it.copy(form = result.form) }
                return
            }
            is PetFormValidator.Result.Valid -> result.draft
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Sin red, Firestore encola la escritura y no termina: mejor avisar y no intentarlo.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(RegisterPetEvent.ShowMessage(DataError.Network.toUiText()))
                return@launch
            }
            when (val result = registerPet(draft, state.form.newPhotoUri)) {
                is AppResult.Success -> _events.send(RegisterPetEvent.PetSaved)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(RegisterPetEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }
}
