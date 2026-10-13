package com.example.huellitasya.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.onFailure
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.RegisterUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val nameFeedback: FieldFeedback? = null,
    val emailFeedback: FieldFeedback? = null,
    val passwordFeedback: FieldFeedback? = null,
    val confirmPasswordFeedback: FieldFeedback? = null,
    val termsFeedback: FieldFeedback? = null,
    /** Aviso del formulario completo ("Rellene todos los datos para continuar"). */
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && password.isNotEmpty() &&
            confirmPassword.isNotEmpty() && termsAccepted
}

sealed interface RegisterAction {
    data class NameChanged(val value: String) : RegisterAction
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class ConfirmPasswordChanged(val value: String) : RegisterAction
    data class TermsChanged(val accepted: Boolean) : RegisterAction
    data object Submit : RegisterAction
}

sealed interface RegisterEvent {
    data class ShowMessage(val message: UiText) : RegisterEvent

    /** Firebase falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : RegisterEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val validateName: ValidateNameUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.NameChanged -> _uiState.update {
                it.copy(name = action.value, nameFeedback = null, formFeedback = null)
            }
            is RegisterAction.EmailChanged -> _uiState.update {
                it.copy(email = action.value, emailFeedback = null, formFeedback = null)
            }
            is RegisterAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordFeedback = null, confirmPasswordFeedback = null, formFeedback = null)
            }
            is RegisterAction.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = action.value, confirmPasswordFeedback = null, formFeedback = null)
            }
            is RegisterAction.TermsChanged -> _uiState.update {
                it.copy(termsAccepted = action.accepted, termsFeedback = null)
            }
            RegisterAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val nameError = validateName(state.name)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword.forRegistration(state.password)
        val confirmError = validatePassword.confirmation(state.password, state.confirmPassword)
        val termsError = if (state.termsAccepted) null else ValidationError.TermsNotAccepted

        if (listOf(nameError, emailError, passwordError, confirmError, termsError).any { it != null }) {
            _uiState.update {
                it.copy(
                    nameFeedback = nameError?.toFieldFeedback(),
                    emailFeedback = emailError?.toFieldFeedback(),
                    passwordFeedback = passwordError?.toFieldFeedback(),
                    confirmPasswordFeedback = confirmError?.toFieldFeedback(),
                    termsFeedback = termsError?.toFieldFeedback(),
                    formFeedback = formFeedbackFor(nameError, emailError, passwordError, confirmError),
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            register(state.name, state.email, state.password).onFailure(::showError)
        }
    }

    private fun showError(error: AuthError) {
        _uiState.update { it.copy(isLoading = false) }
        when (error) {
            AuthError.EmailAlreadyInUse -> _uiState.update { it.copy(emailFeedback = error.toFieldFeedback()) }
            AuthError.WeakPassword -> _uiState.update { it.copy(passwordFeedback = error.toFieldFeedback()) }
            AuthError.Network -> viewModelScope.launch { _events.send(RegisterEvent.NetworkError) }
            else -> error.toUiText()?.let { message ->
                viewModelScope.launch { _events.send(RegisterEvent.ShowMessage(message)) }
            }
        }
    }
}
