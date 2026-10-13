package com.example.huellitasya.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.onFailure
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.SignInWithEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailFeedback: FieldFeedback? = null,
    val passwordFeedback: FieldFeedback? = null,
    /** Aviso del formulario completo ("Rellene todos los datos para continuar"). */
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    /** Figma pinta el botón en gris mientras falten datos y en verde cuando está listo. */
    val isComplete: Boolean get() = email.isNotBlank() && password.isNotEmpty()
}

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEvent {
    data class ShowMessage(val message: UiText) : LoginEvent

    /** Firebase falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : LoginEvent
}

/**
 * Tras un login exitoso no se navega desde aquí: la app observa la sesión y cambia al grafo
 * principal (spec 002). Por eso `isLoading` se queda en `true` en caso de éxito.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithEmail: SignInWithEmailUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _uiState.update {
                it.copy(email = action.value, emailFeedback = null, formFeedback = null)
            }
            is LoginAction.PasswordChanged -> _uiState.update {
                it.copy(password = action.value, passwordFeedback = null, formFeedback = null)
            }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailError = validateEmail(state.email)
        val passwordError = validatePassword.forLogin(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailFeedback = emailError?.toFieldFeedback(),
                    passwordFeedback = passwordError?.toFieldFeedback(),
                    formFeedback = formFeedbackFor(emailError, passwordError),
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            signInWithEmail(state.email, state.password).onFailure(::showError)
        }
    }

    private fun showError(error: AuthError) {
        _uiState.update { it.copy(isLoading = false) }
        when (error) {
            // Figma muestra el rechazo bajo el campo de contraseña.
            AuthError.InvalidCredentials -> _uiState.update { it.copy(passwordFeedback = error.toFieldFeedback()) }
            AuthError.Network -> viewModelScope.launch { _events.send(LoginEvent.NetworkError) }
            else -> error.toUiText()?.let { message ->
                viewModelScope.launch { _events.send(LoginEvent.ShowMessage(message)) }
            }
        }
    }
}
