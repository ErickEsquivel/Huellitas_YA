package com.example.huellitasya.feature.profile.personaldata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.domain.usecase.UpdateUserProfileUseCase
import com.example.huellitasya.core.domain.validation.ValidateAddressUseCase
import com.example.huellitasya.core.domain.validation.ValidateBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePhoneUseCase
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.formFeedbackFor
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
import java.time.LocalDate
import javax.inject.Inject

data class PersonalDataUiState(
    /** Último perfil guardado en Firestore; `null` mientras carga. */
    val saved: UserProfile? = null,
    val isEditing: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val birthDate: LocalDate? = null,
    val nameFeedback: FieldFeedback? = null,
    val emailFeedback: FieldFeedback? = null,
    val phoneFeedback: FieldFeedback? = null,
    val addressFeedback: FieldFeedback? = null,
    val birthDateFeedback: FieldFeedback? = null,
    /** Aviso del formulario completo ("Rellene todos los datos para continuar"). */
    val formFeedback: FieldFeedback? = null,
    /** Figma pinta "Actualizar información" en gris sin cambios y en verde con cambios. */
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface PersonalDataAction {
    /** Lápiz de "Datos Dueño": activa la edición o, si ya está activa, descarta los cambios. */
    data object ToggleEditing : PersonalDataAction
    data class NameChanged(val value: String) : PersonalDataAction
    data class PhoneChanged(val value: String) : PersonalDataAction
    data class AddressChanged(val value: String) : PersonalDataAction
    data class BirthDateChanged(val value: LocalDate?) : PersonalDataAction
    data object Save : PersonalDataAction
}

sealed interface PersonalDataEvent {
    data class ShowMessage(val message: UiText) : PersonalDataEvent
}

/** Datos Personales (spec 004): lectura en vivo de `users/{uid}` y edición campo por campo. */
@HiltViewModel
class PersonalDataViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase,
    private val networkMonitor: NetworkMonitor,
    private val validateName: ValidateNameUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePhone: ValidatePhoneUseCase,
    private val validateAddress: ValidateAddressUseCase,
    private val validateBirthDate: ValidateBirthDateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalDataUiState())
    val uiState: StateFlow<PersonalDataUiState> = _uiState.asStateFlow()

    private val _events = Channel<PersonalDataEvent>(Channel.BUFFERED)
    val events: Flow<PersonalDataEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeUserProfile().collect { result ->
                when (result) {
                    // Mientras se edita no se pisan los campos: solo cambia la referencia para comparar.
                    is AppResult.Success -> updateState {
                        if (it.isEditing) it.copy(saved = result.data) else it.withFieldsFrom(result.data)
                    }
                    is AppResult.Failure -> showMessage(result.error.toUiText())
                }
            }
        }
    }

    fun onAction(action: PersonalDataAction) {
        when (action) {
            PersonalDataAction.ToggleEditing -> toggleEditing()
            is PersonalDataAction.NameChanged -> updateState {
                it.copy(name = action.value, nameFeedback = null, formFeedback = null)
            }
            is PersonalDataAction.PhoneChanged -> updateState { it.copy(phone = action.value, phoneFeedback = null) }
            is PersonalDataAction.AddressChanged -> updateState { it.copy(address = action.value, addressFeedback = null) }
            is PersonalDataAction.BirthDateChanged -> updateState {
                it.copy(birthDate = action.value, birthDateFeedback = null)
            }
            PersonalDataAction.Save -> save()
        }
    }

    private fun toggleEditing() {
        val state = _uiState.value
        val saved = state.saved ?: return
        if (state.isSaving) return
        updateState {
            if (it.isEditing) it.withFieldsFrom(saved) else it.copy(isEditing = true)
        }
    }

    private fun save() {
        val state = _uiState.value
        val saved = state.saved ?: return
        if (state.isSaving) return
        if (!state.isEditing) {
            // Sin el lápiz, el botón también abre la edición: nada que guardar todavía.
            toggleEditing()
            return
        }
        if (!state.hasChanges) return

        val nameError = validateName(state.name)
        val emailError = validateEmail(state.email)
        val phoneError = validatePhone(state.phone)
        val addressError = validateAddress(state.address)
        val birthDateError = validateBirthDate(state.birthDate)
        if (listOf(nameError, emailError, phoneError, addressError, birthDateError).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameFeedback = nameError?.toFieldFeedback(),
                    emailFeedback = emailError?.toFieldFeedback(),
                    phoneFeedback = phoneError?.toFieldFeedback(),
                    addressFeedback = addressError?.toFieldFeedback(),
                    birthDateFeedback = birthDateError?.toFieldFeedback(),
                    formFeedback = formFeedbackFor(nameError, emailError),
                )
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            // Sin red, Firestore encola la escritura y no termina: mejor avisar y no intentarlo.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(isSaving = false) }
                showMessage(DataError.Network.toUiText())
                return@launch
            }
            when (val result = updateUserProfile(saved.uid, state.toChanges(saved))) {
                is AppResult.Success -> {
                    // Firestore ya emitió el perfil con la escritura local: se muestran los datos guardados
                    // (p. ej. el teléfono normalizado) en lugar de lo que se escribió.
                    updateState { current ->
                        val latest = current.copy(isSaving = false)
                        latest.saved?.let { latest.withFieldsFrom(it) } ?: latest.copy(isEditing = false)
                    }
                    showMessage(UiText.Resource(R.string.personal_data_saved))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSaving = false) }
                    showMessage(result.error.toUiText())
                }
            }
        }
    }

    private fun PersonalDataUiState.withFieldsFrom(profile: UserProfile) = PersonalDataUiState(
        saved = profile,
        name = profile.name,
        email = profile.email,
        phone = profile.phone.orEmpty(),
        address = profile.address.orEmpty(),
        birthDate = profile.birthDate,
        isSaving = isSaving,
    )

    /** Solo lo que difiere de [saved]. Vaciar un campo opcional lo borra. */
    private fun PersonalDataUiState.toChanges(saved: UserProfile) = ProfileChanges(
        name = name.trim().takeIf { it != saved.name },
        phone = optionalChange(validatePhone.normalize(phone.trim()), saved.phone),
        address = optionalChange(address.trim(), saved.address),
        birthDate = when {
            birthDate == saved.birthDate -> null
            birthDate == null -> Field.Clear
            else -> Field.Set(birthDate)
        },
    )

    private fun optionalChange(new: String, old: String?): Field<String>? = when {
        new == old.orEmpty() -> null
        new.isEmpty() -> Field.Clear
        else -> Field.Set(new)
    }

    /** Aplica [transform] y recalcula `hasChanges`. */
    private fun updateState(transform: (PersonalDataUiState) -> PersonalDataUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val saved = next.saved
            next.copy(hasChanges = saved != null && next.isEditing && !next.toChanges(saved).isEmpty)
        }
    }

    private fun showMessage(message: UiText) {
        viewModelScope.launch { _events.send(PersonalDataEvent.ShowMessage(message)) }
    }
}
