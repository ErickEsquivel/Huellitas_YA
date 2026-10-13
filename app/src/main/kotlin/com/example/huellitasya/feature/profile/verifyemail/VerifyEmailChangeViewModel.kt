package com.example.huellitasya.feature.profile.verifyemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.usecase.ConfirmEmailChangeUseCase
import com.example.huellitasya.core.domain.usecase.EMAIL_CHANGE_CODE_LENGTH
import com.example.huellitasya.core.domain.usecase.RequestEmailChangeCodeUseCase
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

data class VerifyEmailChangeUiState(
    val code: String = "",
    val codeFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
    /** Segundos que faltan para poder pedir otro código (0 = "Reenviar código" activo). */
    val resendSecondsLeft: Int = VerifyEmailChangeViewModel.RESEND_COOLDOWN_SECONDS,
    val isResending: Boolean = false,
) {
    val isComplete: Boolean get() = code.length == EMAIL_CHANGE_CODE_LENGTH
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isResending && !isLoading
}

sealed interface VerifyEmailChangeAction {
    data class CodeChanged(val value: String) : VerifyEmailChangeAction
    data object Submit : VerifyEmailChangeAction
    data object Resend : VerifyEmailChangeAction
}

sealed interface VerifyEmailChangeEvent {
    data class ShowMessage(val message: UiText) : VerifyEmailChangeEvent

    /** Código correcto: el backend ya cambió el correo. Se vuelve a Datos Personales. */
    data class EmailChanged(val email: String) : VerifyEmailChangeEvent
}

/**
 * "Verifica tu correo" del cambio de correo (spec 004): valida el código de 4 dígitos que llegó
 * al correo nuevo y permite reenviarlo cada 60 s. Misma mecánica que la recuperación de
 * contraseña (spec 002).
 */
@HiltViewModel
class VerifyEmailChangeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmEmailChange: ConfirmEmailChangeUseCase,
    private val requestEmailChangeCode: RequestEmailChangeCodeUseCase,
) : ViewModel() {

    /** Correo nuevo, al que se envió el código (argumento de `VerifyEmailChangeDestination`). */
    val newEmail: String = checkNotNull(savedStateHandle[NEW_EMAIL_ARG]) { "Falta el correo nuevo" }

    private val _uiState = MutableStateFlow(VerifyEmailChangeUiState())
    val uiState: StateFlow<VerifyEmailChangeUiState> = _uiState.asStateFlow()

    private val _events = Channel<VerifyEmailChangeEvent>(Channel.BUFFERED)
    val events: Flow<VerifyEmailChangeEvent> = _events.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        // El código se acaba de enviar desde Datos Personales.
        startResendCountdown(RESEND_COOLDOWN_SECONDS)
    }

    fun onAction(action: VerifyEmailChangeAction) {
        when (action) {
            is VerifyEmailChangeAction.CodeChanged -> _uiState.update {
                it.copy(code = action.value.filter(Char::isDigit).take(EMAIL_CHANGE_CODE_LENGTH), codeFeedback = null)
            }
            VerifyEmailChangeAction.Submit -> submit()
            VerifyEmailChangeAction.Resend -> resend()
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
            when (val result = confirmEmailChange(state.code)) {
                // `isLoading` se queda en `true`: la pantalla se cierra.
                is AppResult.Success -> _events.send(VerifyEmailChangeEvent.EmailChanged(result.data))
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    showError(result.error)
                }
            }
        }
    }

    private fun resend() {
        if (!_uiState.value.canResend) return
        _uiState.update { it.copy(isResending = true) }
        viewModelScope.launch {
            val result = requestEmailChangeCode(newEmail)
            _uiState.update { it.copy(isResending = false) }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(code = "", codeFeedback = null) }
                    startResendCountdown(RESEND_COOLDOWN_SECONDS)
                    _events.send(VerifyEmailChangeEvent.ShowMessage(UiText.Resource(R.string.verify_email_resent)))
                }
                is AppResult.Failure -> {
                    val error = result.error
                    if (error is EmailChangeError.TooManyRequests) startResendCountdown(error.retryAfterSeconds)
                    showError(error)
                }
            }
        }
    }

    private suspend fun showError(error: EmailChangeError) {
        when (error) {
            // Errores del código: van bajo las casillas, en rojo (como "VC ERROR" de Figma).
            EmailChangeError.InvalidCode,
            EmailChangeError.ExpiredCode,
            EmailChangeError.TooManyAttempts -> _uiState.update {
                it.copy(codeFeedback = FieldFeedback(FieldStatus.Error, error.toUiText()))
            }
            // Reenviar pasados 5 min exige reautenticar: se pide volver a guardar el correo.
            EmailChangeError.RequiresRecentLogin -> _events.send(
                VerifyEmailChangeEvent.ShowMessage(UiText.Resource(R.string.verify_email_reauth_needed)),
            )
            else -> error.toUiText()?.let { _events.send(VerifyEmailChangeEvent.ShowMessage(it)) }
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
        const val NEW_EMAIL_ARG = "newEmail"
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
