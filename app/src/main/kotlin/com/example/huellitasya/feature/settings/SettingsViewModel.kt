package com.example.huellitasya.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.DeleteAccountUseCase
import com.example.huellitasya.core.domain.usecase.ObserveSessionUseCase
import com.example.huellitasya.core.domain.usecase.ReauthenticateUseCase
import com.example.huellitasya.core.domain.usecase.SignOutUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.SessionState
import com.example.huellitasya.core.ui.PasswordPromptState
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

data class SettingsUiState(
    val provider: AuthProvider = AuthProvider.EMAIL,
    val isLoggingOut: Boolean = false,
    /** Diálogo "¿Seguro deseas eliminarlo?" (Figma 3050:1515). */
    val showDeleteConfirmation: Boolean = false,
    /** Diálogo "Confirma tu contraseña" (cuentas de correo); `null` si está cerrado. */
    val passwordPrompt: PasswordPromptState? = null,
    val isDeleting: Boolean = false,
)

sealed interface SettingsAction {
    data object Logout : SettingsAction
    data object SecurityClick : SettingsAction
    data object DeleteAccountClick : SettingsAction
    data object ConfirmDelete : SettingsAction
    data object DismissDelete : SettingsAction
    data class PasswordChanged(val value: String) : SettingsAction
    data object ConfirmPassword : SettingsAction
    data object DismissPassword : SettingsAction
}

sealed interface SettingsEvent {
    /** Cuenta de correo: abre Seguridad (cambio de contraseña). */
    data object NavigateToSecurity : SettingsEvent

    /** La Route debe abrir el selector de cuentas y devolver el resultado con [SettingsViewModel.onGoogleIdTokenResult]. */
    data object LaunchGoogleReauth : SettingsEvent
    data class ShowMessage(val message: UiText) : SettingsEvent
}

/**
 * Configuración. Cerrar sesión o eliminar la cuenta no navega: la app reacciona al cambio de
 * `SessionState` y vuelve a Bienvenido con el historial limpio (specs 002 y 005).
 *
 * Eliminar: confirmar → reautenticar (contraseña o Google) → Cloud Function `deleteAccount`.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val signOut: SignOutUseCase,
    private val reauthenticate: ReauthenticateUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val networkMonitor: NetworkMonitor,
    private val validatePassword: ValidatePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                if (session is SessionState.SignedIn) _uiState.update { it.copy(provider = session.user.provider) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.Logout -> logout()
            SettingsAction.SecurityClick -> viewModelScope.launch {
                // Las cuentas Google no tienen contraseña en Huellitas: se gestiona en Google.
                if (_uiState.value.provider == AuthProvider.GOOGLE) {
                    _events.send(SettingsEvent.ShowMessage(UiText.Resource(R.string.settings_security_google)))
                } else {
                    _events.send(SettingsEvent.NavigateToSecurity)
                }
            }
            SettingsAction.DeleteAccountClick -> _uiState.update {
                if (it.isBusy) it else it.copy(showDeleteConfirmation = true)
            }
            SettingsAction.DismissDelete -> _uiState.update { it.copy(showDeleteConfirmation = false) }
            SettingsAction.ConfirmDelete -> confirmDelete()
            is SettingsAction.PasswordChanged -> _uiState.update {
                it.copy(passwordPrompt = it.passwordPrompt?.copy(password = action.value, feedback = null))
            }
            SettingsAction.ConfirmPassword -> confirmPassword()
            SettingsAction.DismissPassword -> _uiState.update {
                if (it.passwordPrompt?.isLoading == true) it else it.copy(passwordPrompt = null)
            }
        }
    }

    /** Resultado del selector de cuentas abierto por [SettingsEvent.LaunchGoogleReauth]. */
    fun onGoogleIdTokenResult(result: AppResult<String, AuthError>) {
        when (result) {
            is AppResult.Success -> {
                _uiState.update { it.copy(isDeleting = true) }
                viewModelScope.launch {
                    when (val reauth = reauthenticate.withGoogle(result.data)) {
                        is AppResult.Success -> delete()
                        is AppResult.Failure -> fail(reauth.error)
                    }
                }
            }
            // Cerrar el selector no es un error: no se muestra nada.
            is AppResult.Failure -> if (result.error != AuthError.Cancelled) fail(result.error)
        }
    }

    private fun logout() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch { signOut() }
    }

    private fun confirmDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
        viewModelScope.launch {
            if (!networkMonitor.isOnline.first()) {
                showMessage(AuthError.Network)
                return@launch
            }
            when (_uiState.value.provider) {
                AuthProvider.EMAIL -> _uiState.update { it.copy(passwordPrompt = PasswordPromptState()) }
                AuthProvider.GOOGLE -> _events.send(SettingsEvent.LaunchGoogleReauth)
            }
        }
    }

    private fun confirmPassword() {
        val prompt = _uiState.value.passwordPrompt ?: return
        if (prompt.isLoading) return

        validatePassword.forLogin(prompt.password)?.let { error ->
            _uiState.update { it.copy(passwordPrompt = prompt.copy(feedback = error.toFieldFeedback())) }
            return
        }

        _uiState.update { it.copy(passwordPrompt = prompt.copy(isLoading = true)) }
        viewModelScope.launch {
            when (val result = reauthenticate.withPassword(prompt.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(passwordPrompt = null, isDeleting = true) }
                    delete()
                }
                is AppResult.Failure -> {
                    // Como en Login, la contraseña rechazada se marca bajo el campo.
                    val feedback = result.error.takeIf { it == AuthError.InvalidCredentials }?.toFieldFeedback()
                    _uiState.update { it.copy(passwordPrompt = it.passwordPrompt?.copy(isLoading = false, feedback = feedback)) }
                    if (feedback == null) showMessage(result.error)
                }
            }
        }
    }

    /** Si sale bien, la sesión se cierra y esta pantalla desaparece: `isDeleting` se queda en `true`. */
    private suspend fun delete() {
        when (val result = deleteAccount()) {
            is AppResult.Success -> Unit
            is AppResult.Failure -> fail(result.error)
        }
    }

    private fun fail(error: AuthError) {
        _uiState.update { it.copy(isDeleting = false) }
        // Tras reautenticar, RequiresRecentLogin solo puede venir de un reloj muy desfasado:
        // se informa como error genérico para que el usuario lo intente de nuevo.
        showMessage(if (error == AuthError.RequiresRecentLogin) AuthError.Unknown else error)
    }

    private fun showMessage(error: AuthError) {
        val message = error.toUiText() ?: return
        viewModelScope.launch { _events.send(SettingsEvent.ShowMessage(message)) }
    }

    private val SettingsUiState.isBusy: Boolean get() = isLoggingOut || isDeleting
}
