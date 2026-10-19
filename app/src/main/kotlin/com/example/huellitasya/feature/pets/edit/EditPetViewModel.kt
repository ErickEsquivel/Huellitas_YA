package com.example.huellitasya.feature.pets.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.DeletePetUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.UpdatePetUseCase
import com.example.huellitasya.core.domain.validation.ValidatePetWeightUseCase
import com.example.huellitasya.core.model.Pet
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

data class EditPetUiState(
    /** Mascota tal como está guardada; `null` mientras carga. */
    val saved: Pet? = null,
    val form: PetFormState = PetFormState(),
    /** Figma pinta "Actualizar información" en gris sin cambios y en verde con cambios. */
    val hasChanges: Boolean = false,
    val showDeleteDialog: Boolean = false,
    /** Overlay "Actualizando…" (Figma 3242:2283). */
    val isUpdating: Boolean = false,
    /** Overlay "Eliminando…" (Figma 3242:2009). */
    val isDeleting: Boolean = false,
) {
    val isBusy: Boolean get() = isUpdating || isDeleting
}

sealed interface EditPetAction {
    data class Form(val action: PetFormAction) : EditPetAction
    data object Update : EditPetAction
    data object DeleteClick : EditPetAction
    data object DismissDelete : EditPetAction
    data object ConfirmDelete : EditPetAction
}

sealed interface EditPetEvent {
    data class ShowMessage(val message: UiText) : EditPetEvent

    /** Se guardaron los cambios: la Route vuelve al perfil, que ya los muestra en vivo. */
    data object Updated : EditPetEvent

    /** La mascota se eliminó (o ya no existe): la Route vuelve a Home. */
    data object Deleted : EditPetEvent
}

/** "Actualiza la información de tu mascota" (spec 005, Figma 56:192): editar y eliminar. */
@HiltViewModel
class EditPetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    private val updatePet: UpdatePetUseCase,
    private val deletePet: DeletePetUseCase,
    private val validator: PetFormValidator,
    private val validateWeight: ValidatePetWeightUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Argumento de `EditPetDestination`. */
    private val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }

    private val _uiState = MutableStateFlow(EditPetUiState())
    val uiState: StateFlow<EditPetUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditPetEvent>(Channel.BUFFERED)
    val events: Flow<EditPetEvent> = _events.receiveAsFlow()

    init {
        // Se lee una vez: mientras se edita no se pisan los campos con cambios remotos.
        viewModelScope.launch {
            when (val result = observePet(petId).first()) {
                is AppResult.Success -> _uiState.update { it.copy(saved = result.data, form = result.data.toForm()) }
                is AppResult.Failure -> _events.send(
                    if (result.error == DataError.NotFound) EditPetEvent.Deleted else EditPetEvent.ShowMessage(result.error.toUiText()),
                )
            }
        }
    }

    fun onAction(action: EditPetAction) {
        when (action) {
            is EditPetAction.Form -> updateState { if (it.isBusy) it else it.copy(form = it.form.reduce(action.action)) }
            EditPetAction.Update -> update()
            EditPetAction.DeleteClick -> _uiState.update { if (it.isBusy || it.saved == null) it else it.copy(showDeleteDialog = true) }
            EditPetAction.DismissDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            EditPetAction.ConfirmDelete -> delete()
        }
    }

    private fun update() {
        val state = _uiState.value
        if (state.isBusy || state.saved == null || !state.hasChanges) return

        val draft = when (val result = validator.validate(state.form)) {
            is PetFormValidator.Result.Invalid -> {
                _uiState.update { it.copy(form = result.form) }
                return
            }
            is PetFormValidator.Result.Valid -> result.draft
        }

        _uiState.update { it.copy(isUpdating = true) }
        viewModelScope.launch {
            if (!isOnline()) {
                _uiState.update { it.copy(isUpdating = false) }
                return@launch
            }
            when (val result = updatePet(petId, draft, state.form.newPhotoUri)) {
                is AppResult.Success -> _events.send(EditPetEvent.Updated)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.send(EditPetEvent.ShowMessage(result.error.toUiText()))
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
            when (val result = deletePet(petId)) {
                is AppResult.Success -> _events.send(EditPetEvent.Deleted)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(EditPetEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    /** Sin red, Firestore encola la escritura y no termina: se avisa y no se intenta. */
    private suspend fun isOnline(): Boolean {
        val online = networkMonitor.isOnline.first()
        if (!online) _events.send(EditPetEvent.ShowMessage(DataError.Network.toUiText()))
        return online
    }

    /** Aplica [transform] y recalcula `hasChanges` contra lo guardado. */
    private fun updateState(transform: (EditPetUiState) -> EditPetUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(hasChanges = next.saved?.let { next.form.differsFrom(it) } ?: false)
        }
    }

    private fun PetFormState.differsFrom(pet: Pet): Boolean =
        newPhotoUri != null ||
            name.trim() != pet.name ||
            species != pet.species ||
            breed.trim() != pet.breed ||
            birthDate != pet.birthDate ||
            sex != pet.sex ||
            validateWeight.parse(weight) != pet.weightKg

    private fun Pet.toForm() = PetFormState(
        name = name,
        species = species,
        breed = breed,
        birthDate = birthDate,
        sex = sex,
        weight = weightKg.toFormText(),
        savedPhotoUrl = photoUrl,
    )

    /** `5.0` → "5", `4.5` → "4,5": como lo escribiría el usuario. */
    private fun Double.toFormText(): String =
        if (this % 1.0 == 0.0) toLong().toString() else toString().replace('.', ',')

    companion object {
        const val PET_ID_ARG = "petId"
    }
}
