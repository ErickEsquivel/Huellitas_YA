package com.example.huellitasya.feature.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.usecase.RequestPasswordResetCodeUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
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

data class ForgotPasswordUiState(
    val email: String = "",
    val emailFeedback: FieldFeedback? = null,
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    val isComplete: Boolean get() = email.isNotBlank()
}

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object Submit : ForgotPasswordAction
}

sealed interface ForgotPasswordEvent {
    data class ShowMessage(val message: UiText) : ForgotPasswordEvent

    /** Se pidió el código: la pantalla pasa a "Verifica tu correo". */
    data class NavigateToVerifyCode(val email: String) : ForgotPasswordEvent

    /** Falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : ForgotPasswordEvent
}

/**
 * Pide el código de 4 dígitos por correo (spec 002, "Recuperar contraseña"). Por privacidad el
 * backend responde igual exista o no la cuenta, así que siempre se avanza a "Verifica tu correo".
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestCode: RequestPasswordResetCodeUseCase,
    private val validateEmail: ValidateEmailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events: Flow<ForgotPasswordEvent> = _events.receiveAsFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> _uiState.update {
                it.copy(email = action.value, emailFeedback = null, formFeedback = null)
            }
            ForgotPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update {
                it.copy(emailFeedback = emailError.toFieldFeedback(), formFeedback = formFeedbackFor(emailError))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = requestCode(state.email)
            _uiState.update { it.copy(isLoading = false) }
            when {
                // Si ya se envió un código hace poco, ese sigue sirviendo: se avanza igual.
                result is AppResult.Success ||
                    (result is AppResult.Failure && result.error is PasswordResetError.TooManyRequests) ->
                    _events.send(ForgotPasswordEvent.NavigateToVerifyCode(state.email.trim()))
                result is AppResult.Failure && result.error == PasswordResetError.Network ->
                    _events.send(ForgotPasswordEvent.NetworkError)
                result is AppResult.Failure ->
                    result.error.toUiText()?.let { _events.send(ForgotPasswordEvent.ShowMessage(it)) }
            }
        }
    }
}
