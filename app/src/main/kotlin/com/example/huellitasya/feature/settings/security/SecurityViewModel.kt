package com.example.huellitasya.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.ChangePasswordUseCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val requirements: PasswordRequirements = NoRequirementsMet,
    val currentFeedback: FieldFeedback? = null,
    val newFeedback: FieldFeedback? = null,
    val confirmationFeedback: FieldFeedback? = null,
    /** Aviso del formulario completo ("Rellene todos los datos para continuar"). */
    val formFeedback: FieldFeedback? = null,
    val isLoading: Boolean = false,
) {
    /** Figma pinta "Actualizar información" en verde cuando la checklist está completa. */
    val isComplete: Boolean get() = currentPassword.isNotEmpty() && requirements.allMet
}

private val NoRequirementsMet = PasswordRequirements(
    hasMinLength = false,
    hasUppercase = false,
    hasDigitOrSymbol = false,
    matchesConfirmation = false,
)

sealed interface SecurityAction {
    data class CurrentPasswordChanged(val value: String) : SecurityAction
    data class NewPasswordChanged(val value: String) : SecurityAction
    data class ConfirmationChanged(val value: String) : SecurityAction
    data object Submit : SecurityAction
}

sealed interface SecurityEvent {
    data class ShowMessage(val message: UiText) : SecurityEvent
}

/**
 * Configuración → Seguridad (spec 004, Figma 3263:1086): cambio de contraseña con la contraseña
 * actual, la nueva y su confirmación. Al terminar, el backend envía el aviso "Tu contraseña cambió".
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val changePassword: ChangePasswordUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _events = Channel<SecurityEvent>(Channel.BUFFERED)
    val events: Flow<SecurityEvent> = _events.receiveAsFlow()

    fun onAction(action: SecurityAction) {
        when (action) {
            is SecurityAction.CurrentPasswordChanged -> _uiState.update {
                it.copy(currentPassword = action.value, currentFeedback = null, formFeedback = null)
            }
            is SecurityAction.NewPasswordChanged -> _uiState.update {
                it.copy(
                    newPassword = action.value,
                    requirements = validatePassword.requirements(action.value, it.confirmation),
                    newFeedback = null,
                    formFeedback = null,
                )
            }
            is SecurityAction.ConfirmationChanged -> _uiState.update {
                it.copy(
                    confirmation = action.value,
                    requirements = validatePassword.requirements(it.newPassword, action.value),
                    confirmationFeedback = null,
                    formFeedback = null,
                )
            }
            SecurityAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.currentPassword.isEmpty() || state.newPassword.isEmpty() || state.confirmation.isEmpty()) {
            _uiState.update {
                it.copy(
                    currentFeedback = if (it.currentPassword.isEmpty()) FieldFeedback(FieldStatus.Warning) else null,
                    newFeedback = if (it.newPassword.isEmpty()) FieldFeedback(FieldStatus.Warning) else null,
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
                    newFeedback = if (requirements.hasMinLength && requirements.hasUppercase && requirements.hasDigitOrSymbol) {
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
        if (state.newPassword == state.currentPassword) {
            _uiState.update {
                it.copy(newFeedback = FieldFeedback(FieldStatus.Warning, UiText.Resource(R.string.error_password_same)))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(isLoading = false) }
                showMessage(AuthError.Network.toUiText())
                return@launch
            }
            when (val result = changePassword(state.currentPassword, state.newPassword)) {
                is AppResult.Success -> {
                    _uiState.value = SecurityUiState()
                    showMessage(UiText.Resource(R.string.security_success))
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    when (result.error) {
                        // Figma 3278:1170: "La contraseña ingresada no es correcta" bajo la contraseña actual.
                        AuthError.InvalidCredentials -> _uiState.update {
                            it.copy(
                                currentFeedback = FieldFeedback(
                                    FieldStatus.Error,
                                    UiText.Resource(R.string.error_current_password_incorrect),
                                ),
                            )
                        }
                        AuthError.WeakPassword -> _uiState.update {
                            it.copy(newFeedback = result.error.toFieldFeedback())
                        }
                        else -> showMessage(result.error.toUiText())
                    }
                }
            }
        }
    }

    private fun showMessage(message: UiText?) {
        message ?: return
        viewModelScope.launch { _events.send(SecurityEvent.ShowMessage(message)) }
    }
}
