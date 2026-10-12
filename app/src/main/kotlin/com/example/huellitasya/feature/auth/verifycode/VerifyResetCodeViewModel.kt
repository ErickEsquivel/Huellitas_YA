package com.example.huellitasya.feature.auth.verifycode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.usecase.PASSWORD_RESET_CODE_LENGTH
import com.example.huellitasya.core.domain.usecase.RequestPasswordResetCodeUseCase
import com.example.huellitasya.core.domain.usecase.VerifyPasswordResetCodeUseCase
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifyResetCodeUiState(
    val code: String = "",
    val codeFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
    /** Segundos que faltan para poder pedir otro código (0 = "Reenviar código" activo). */
    val resendSecondsLeft: Int = VerifyResetCodeViewModel.RESEND_COOLDOWN_SECONDS,
    val isResending: Boolean = false,
) {
    val isComplete: Boolean get() = code.length == PASSWORD_RESET_CODE_LENGTH
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isResending && !isLoading
}

sealed interface VerifyResetCodeAction {
    data class CodeChanged(val value: String) : VerifyResetCodeAction
    data object Submit : VerifyResetCodeAction
    data object Resend : VerifyResetCodeAction
}

sealed interface VerifyResetCodeEvent {
    data class ShowMessage(val message: UiText) : VerifyResetCodeEvent

    /** Código correcto: pasa a "Crear nueva contraseña" con el token de un solo uso. */
    data class NavigateToNewPassword(val email: String, val resetToken: String) : VerifyResetCodeEvent

    /** Falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : VerifyResetCodeEvent
}

/** "Verifica tu correo" (spec 002): valida el código de 4 dígitos y permite reenviarlo cada 60 s. */
@HiltViewModel
class VerifyResetCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyCode: VerifyPasswordResetCodeUseCase,
    private val requestCode: RequestPasswordResetCodeUseCase,
) : ViewModel() {

    /** Correo al que se envió el código (argumento de `VerifyResetCodeDestination`). */
    val email: String = checkNotNull(savedStateHandle[EMAIL_ARG]) { "Falta el correo" }

    private val _uiState = MutableStateFlow(VerifyResetCodeUiState())
    val uiState: StateFlow<VerifyResetCodeUiState> = _uiState.asStateFlow()

    private val _events = Channel<VerifyResetCodeEvent>(Channel.BUFFERED)
    val events: Flow<VerifyResetCodeEvent> = _events.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        // El código se acaba de enviar en la pantalla anterior.
        startResendCountdown(RESEND_COOLDOWN_SECONDS)
    }

    fun onAction(action: VerifyResetCodeAction) {
        when (action) {
            is VerifyResetCodeAction.CodeChanged -> _uiState.update {
                it.copy(code = action.value.filter(Char::isDigit).take(PASSWORD_RESET_CODE_LENGTH), codeFeedback = null)
            }
            VerifyResetCodeAction.Submit -> submit()
            VerifyResetCodeAction.Resend -> resend()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!state.isComplete) {
            _uiState.update {
                it.copy(codeFeedback = FieldFeedback(FieldStatus.Warning, UiText.Resource(R.string.error_reset_code_incomplete)))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = verifyCode(email, state.code)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is AppResult.Success -> _events.send(VerifyResetCodeEvent.NavigateToNewPassword(email, result.data))
                is AppResult.Failure -> showError(result.error)
            }
        }
    }

    private fun resend() {
        if (!_uiState.value.canResend) return
        _uiState.update { it.copy(isResending = true) }
        viewModelScope.launch {
            val result = requestCode(email)
            _uiState.update { it.copy(isResending = false) }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(code = "", codeFeedback = null) }
                    startResendCountdown(RESEND_COOLDOWN_SECONDS)
                    _events.send(VerifyResetCodeEvent.ShowMessage(UiText.Resource(R.string.auth_verify_resent)))
                }
                is AppResult.Failure -> {
                    val error = result.error
                    if (error is PasswordResetError.TooManyRequests) startResendCountdown(error.retryAfterSeconds)
                    showError(error)
                }
            }
        }
    }

    private suspend fun showError(error: PasswordResetError) {
        when (error) {
            PasswordResetError.Network -> _events.send(VerifyResetCodeEvent.NetworkError)
            // Errores del código: van bajo las casillas, en rojo (Figma "VC ERROR").
            PasswordResetError.InvalidCode,
            PasswordResetError.ExpiredCode,
            PasswordResetError.TooManyAttempts -> _uiState.update {
                it.copy(codeFeedback = FieldFeedback(FieldStatus.Error, error.toUiText()))
            }
            else -> error.toUiText()?.let { _events.send(VerifyResetCodeEvent.ShowMessage(it)) }
        }
    }

    private fun startResendCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (left in seconds downTo 0) {
                _uiState.update { it.copy(resendSecondsLeft = left) }
                if (left > 0) delay(1_000)
            }
        }
    }

    companion object {
        const val EMAIL_ARG = "email"
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
