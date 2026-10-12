package com.example.huellitasya.feature.auth.chooser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.onFailure
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.SignInWithGoogleUseCase
import com.example.huellitasya.core.ui.UiText
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

data class AuthChooserUiState(
    val isLoading: Boolean = false,
)

sealed interface AuthChooserEvent {
    /** La Route debe abrir el selector de cuentas y devolver el resultado con [AuthChooserViewModel.onGoogleIdTokenResult]. */
    data object LaunchGoogleSignIn : AuthChooserEvent
    data class ShowMessage(val message: UiText) : AuthChooserEvent

    /** Firebase falló por red: la pantalla muestra el overlay "Sin conexión". */
    data object NetworkError : AuthChooserEvent
}

/**
 * Pantalla "Bienvenido": elige correo, Google o registro. Solo Google tiene lógica aquí;
 * al autenticarse, la app cambia de grafo por el cambio de sesión (spec 002).
 */
@HiltViewModel
class AuthChooserViewModel @Inject constructor(
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthChooserUiState())
    val uiState: StateFlow<AuthChooserUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthChooserEvent>(Channel.BUFFERED)
    val events: Flow<AuthChooserEvent> = _events.receiveAsFlow()

    fun onGoogleClick() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch { _events.send(AuthChooserEvent.LaunchGoogleSignIn) }
    }

    fun onGoogleIdTokenResult(result: AppResult<String, AuthError>) {
        when (result) {
            is AppResult.Failure -> fail(result.error)
            is AppResult.Success -> viewModelScope.launch {
                signInWithGoogle(result.data).onFailure(::fail)
            }
        }
    }

    private fun fail(error: AuthError) {
        _uiState.update { it.copy(isLoading = false) }
        if (error == AuthError.Network) {
            viewModelScope.launch { _events.send(AuthChooserEvent.NetworkError) }
            return
        }
        val message = error.toUiText() ?: return
        viewModelScope.launch { _events.send(AuthChooserEvent.ShowMessage(message)) }
    }
}
