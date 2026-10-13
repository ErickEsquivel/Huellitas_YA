package com.example.huellitasya.feature.auth.newpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.ResetPasswordUseCase
import com.example.huellitasya.core.domain.validation.PasswordRequirements
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.UiText
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

data class NewPasswordUiState(
    val password: String = "",
    val confirmation: String = "",
    val requirements: PasswordRequirements = PasswordRequirements(
        hasMinLength = false,
        hasUppercase = false,
        hasDigitOrSymbol = false,
        matchesConfirmation = false,
    ),
    val passwordFeedback: FieldFeedback? = null,
    val confirmationFeedback: FieldFeedback? = null,
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    /** El botón se pinta en verde cuando la checklist está completa. */
    val isComplete: Boolean get() = requirements.allMet
}

sealed interface NewPasswordAction {
    data class PasswordChanged(val value: String) : NewPasswordAction
    data class ConfirmationChanged(val value: String) : NewPasswordAction
    data object Submit : NewPasswordAction
}

sealed interface NewPasswordEvent {
    data class ShowMessage(val message: UiText) : NewPasswordEvent

    /** Contraseña cambiada: se vuelve a Inicio de sesión con un mensaje de éxito. */
    data object PasswordReset : NewPasswordEvent

    /** Falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : NewPasswordEvent
}

/** "Crear nueva contraseña" (spec 002): checklist en vivo y cambio con el token verificado. */
@HiltViewModel
class NewPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resetPassword: ResetPasswordUseCase,
    private val validatePassword: ValidatePasswordUseCase,
) : ViewModel() {

    private val email: String = checkNotNull(savedStateHandle[EMAIL_ARG]) { "Falta el correo" }
    private val resetToken: String = checkNotNull(savedStateHandle[RESET_TOKEN_ARG]) { "Falta el token" }

    private val _uiState = MutableStateFlow(NewPasswordUiState())
    val uiState: StateFlow<NewPasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewPasswordEvent>(Channel.BUFFERED)
    val events: Flow<NewPasswordEvent> = _events.receiveAsFlow()

    fun onAction(action: NewPasswordAction) {
        when (action) {
            is NewPasswordAction.PasswordChanged -> _uiState.update {
                it.copy(
                    password = action.value,
                    requirements = validatePassword.requirements(action.value, it.confirmation),
                    passwordFeedback = null,
                    formFeedback = null,
                )
            }
            is NewPasswordAction.ConfirmationChanged -> _uiState.update {
                it.copy(
                    confirmation = action.value,
                    requirements = validatePassword.requirements(it.password, action.value),
                    confirmationFeedback = null,
                    formFeedback = null,
                )
            }
            NewPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.password.isEmpty() || state.confirmation.isEmpty()) {
            _uiState.update {
                it.copy(
                    passwordFeedback = if (it.password.isEmpty()) FieldFeedback(FieldStatus.Warning) else null,
                    confirmationFeedback = if (it.confirmation.isEmpty()) FieldFeedback(FieldStatus.Warning) else null,
                    formFeedback = MissingFieldsFeedback,
                )
            }
            return
        }
        val requirements = state.requirements
        if (!requirements.allMet) {
            // La checklist ya dice qué falta: aquí solo se resaltan los campos.
            _uiState.update {
                it.copy(
                    passwordFeedback = if (requirements.hasMinLength && requirements.hasUppercase && requirements.hasDigitOrSymbol) {
                        null
                    } else {
                        FieldFeedback(FieldStatus.Warning)
                    },
                    confirmationFeedback = if (requirements.matchesConfirmation) {
                        null
                    } else {
                        ValidationError.PasswordsDoNotMatch.toFieldFeedback()
                    },
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = resetPassword(email, resetToken, state.password)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is AppResult.Success -> _events.send(NewPasswordEvent.PasswordReset)
                is AppResult.Failure -> when (val error = result.error) {
                    PasswordResetError.Network -> _events.send(NewPasswordEvent.NetworkError)
                    PasswordResetError.WeakPassword -> _uiState.update {
                        it.copy(passwordFeedback = FieldFeedback(FieldStatus.Error, error.toUiText()))
                    }
                    else -> error.toUiText()?.let { _events.send(NewPasswordEvent.ShowMessage(it)) }
                }
            }
        }
    }

    companion object {
        const val EMAIL_ARG = "email"
        const val RESET_TOKEN_ARG = "resetToken"
    }
}
