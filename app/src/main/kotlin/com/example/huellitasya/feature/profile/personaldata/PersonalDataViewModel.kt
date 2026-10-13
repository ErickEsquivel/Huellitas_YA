package com.example.huellitasya.feature.profile.personaldata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.domain.usecase.ReauthenticateUseCase
import com.example.huellitasya.core.domain.usecase.RequestEmailChangeCodeUseCase
import com.example.huellitasya.core.domain.usecase.UpdateUserProfileUseCase
import com.example.huellitasya.core.domain.validation.ValidateAddressUseCase
import com.example.huellitasya.core.domain.validation.ValidateBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.domain.validation.ValidatePhoneUseCase
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.PasswordPromptState
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
    /** Diálogo "Confirma tu contraseña" antes de cambiar el correo; `null` si está cerrado. */
    val reauth: PasswordPromptState? = null,
) {
    /** El correo de una cuenta Google pertenece a Google: no se puede cambiar aquí (spec 004). */
    val canEditEmail: Boolean get() = saved?.provider == AuthProvider.EMAIL
}

sealed interface PersonalDataAction {
    /** Lápiz de "Datos Dueño": activa la edición o, si ya está activa, descarta los cambios. */
    data object ToggleEditing : PersonalDataAction
    data class NameChanged(val value: String) : PersonalDataAction
    data class EmailChanged(val value: String) : PersonalDataAction
    data class PhoneChanged(val value: String) : PersonalDataAction
    data class AddressChanged(val value: String) : PersonalDataAction
    data class BirthDateChanged(val value: LocalDate?) : PersonalDataAction
    data object Save : PersonalDataAction
    data class ReauthPasswordChanged(val value: String) : PersonalDataAction
    data object ConfirmReauth : PersonalDataAction
    data object DismissReauth : PersonalDataAction
}

sealed interface PersonalDataEvent {
    data class ShowMessage(val message: UiText) : PersonalDataEvent

    /** Se envió el código al correo nuevo: la Route abre "Verifica tu correo". */
    data class NavigateToVerifyEmail(val newEmail: String) : PersonalDataEvent
}

/**
 * Datos Personales (spec 004): lectura en vivo de `users/{uid}`, edición campo por campo y
 * cambio de correo con un código enviado al correo nuevo.
 */
@HiltViewModel
class PersonalDataViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase,
    private val requestEmailChangeCode: RequestEmailChangeCodeUseCase,
    private val reauthenticate: ReauthenticateUseCase,
    private val networkMonitor: NetworkMonitor,
    private val validateName: ValidateNameUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePhone: ValidatePhoneUseCase,
    private val validateAddress: ValidateAddressUseCase,
    private val validateBirthDate: ValidateBirthDateUseCase,
    private val validatePassword: ValidatePasswordUseCase,
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
            is PersonalDataAction.EmailChanged -> updateState {
                if (it.canEditEmail) it.copy(email = action.value, emailFeedback = null, formFeedback = null) else it
            }
            is PersonalDataAction.PhoneChanged -> updateState { it.copy(phone = action.value, phoneFeedback = null) }
            is PersonalDataAction.AddressChanged -> updateState { it.copy(address = action.value, addressFeedback = null) }
            is PersonalDataAction.BirthDateChanged -> updateState {
                it.copy(birthDate = action.value, birthDateFeedback = null)
            }
            PersonalDataAction.Save -> save()
            is PersonalDataAction.ReauthPasswordChanged -> _uiState.update {
                it.copy(reauth = it.reauth?.copy(password = action.value, feedback = null))
            }
            PersonalDataAction.ConfirmReauth -> confirmReauth()
            PersonalDataAction.DismissReauth -> _uiState.update {
                if (it.reauth?.isLoading == true) it else it.copy(reauth = null)
            }
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
        // Solo el lápiz activa la edición (spec 004): en modo lectura el botón no hace nada.
        if (!state.isEditing) return
        if (!state.hasChanges) return

        val nameError = validateName(state.name)
        // El correo de una cuenta Google no se edita aquí: no debe bloquear el guardado del resto.
        val emailError = if (state.canEditEmail) validateEmail(state.email) else null
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

            val changes = state.toChanges(saved)
            if (!changes.isEmpty) {
                val result = updateUserProfile(saved.uid, changes)
                if (result is AppResult.Failure) {
                    _uiState.update { it.copy(isSaving = false) }
                    showMessage(result.error.toUiText())
                    return@launch
                }
            }

            val newEmail = state.email.trim()
            if (state.canEditEmail && newEmail != saved.email) {
                changeEmail(newEmail)
            } else {
                finishSaving(UiText.Resource(R.string.personal_data_saved))
            }
        }
    }

    /**
     * Pide el código para el correo nuevo. El correo no cambia hasta validar el código en
     * "Verifica tu correo"; mientras tanto se sale del modo edición con lo ya guardado.
     */
    private suspend fun changeEmail(newEmail: String) {
        when (val result = requestEmailChangeCode(newEmail)) {
            is AppResult.Success -> {
                finishSaving(message = null)
                _events.send(PersonalDataEvent.NavigateToVerifyEmail(newEmail))
            }
            is AppResult.Failure -> {
                _uiState.update { it.copy(isSaving = false) }
                when (val error = result.error) {
                    EmailChangeError.RequiresRecentLogin -> _uiState.update { it.copy(reauth = PasswordPromptState()) }
                    EmailChangeError.EmailAlreadyInUse, EmailChangeError.InvalidEmail -> _uiState.update {
                        it.copy(emailFeedback = FieldFeedback(FieldStatus.Error, error.toUiText()))
                    }
                    else -> error.toUiText()?.let(::showMessage)
                }
            }
        }
    }

    private fun confirmReauth() {
        val state = _uiState.value
        val reauth = state.reauth ?: return
        if (reauth.isLoading) return

        validatePassword.forLogin(reauth.password)?.let { error ->
            _uiState.update { it.copy(reauth = reauth.copy(feedback = error.toFieldFeedback())) }
            return
        }

        _uiState.update { it.copy(reauth = reauth.copy(isLoading = true)) }
        viewModelScope.launch {
            when (val result = reauthenticate.withPassword(reauth.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(reauth = null, isSaving = true) }
                    changeEmail(_uiState.value.email.trim())
                }
                is AppResult.Failure -> {
                    // Figma muestra el rechazo bajo el campo de contraseña, como en Login.
                    val feedback = result.error.takeIf { it == AuthError.InvalidCredentials }?.toFieldFeedback()
                    _uiState.update { it.copy(reauth = it.reauth?.copy(isLoading = false, feedback = feedback)) }
                    if (feedback == null) result.error.toUiText()?.let(::showMessage)
                }
            }
        }
    }

    /** Sale del modo edición mostrando lo guardado (p. ej. el teléfono normalizado). */
    private fun finishSaving(message: UiText?) {
        updateState { current ->
            val latest = current.copy(isSaving = false, reauth = null)
            latest.saved?.let { latest.withFieldsFrom(it) } ?: latest.copy(isEditing = false)
        }
        message?.let(::showMessage)
    }

    private fun PersonalDataUiState.withFieldsFrom(profile: UserProfile) = PersonalDataUiState(
        saved = profile,
        name = profile.name,
        email = profile.email,
        phone = profile.phone.orEmpty(),
        address = profile.address.orEmpty(),
        birthDate = profile.birthDate,
        isSaving = isSaving,
        reauth = reauth,
    )

    /** Solo lo que difiere de [saved]. Vaciar un campo opcional lo borra. El correo va aparte. */
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
            val hasChanges = saved != null && next.isEditing &&
                (!next.toChanges(saved).isEmpty || (next.canEditEmail && next.email.trim() != saved.email))
            next.copy(hasChanges = hasChanges)
        }
    }

    private fun showMessage(message: UiText) {
        viewModelScope.launch { _events.send(PersonalDataEvent.ShowMessage(message)) }
    }
}
