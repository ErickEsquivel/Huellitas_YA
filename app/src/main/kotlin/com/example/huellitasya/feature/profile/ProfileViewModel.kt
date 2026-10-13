package com.example.huellitasya.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.onFailure
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.ChangeProfilePhotoUseCase
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.ui.UiText
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

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isUploadingPhoto: Boolean = false,
)

sealed interface ProfileAction {
    /** El usuario eligió una foto en el selector del sistema (`content://`). */
    data class PhotoPicked(val uri: String) : ProfileAction
}

sealed interface ProfileEvent {
    data class ShowMessage(val message: UiText) : ProfileEvent
}

/** Pestaña Perfil (spec 004): muestra en vivo `users/{uid}` y cambia la foto. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
    private val changeProfilePhoto: ChangeProfilePhotoUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeUserProfile().collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(profile = result.data) }
                    is AppResult.Failure -> _events.send(ProfileEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.PhotoPicked -> uploadPhoto(action.uri)
        }
    }

    private fun uploadPhoto(uri: String) {
        val state = _uiState.value
        val uid = state.profile?.uid ?: return
        if (state.isUploadingPhoto) return

        _uiState.update { it.copy(isUploadingPhoto = true) }
        viewModelScope.launch {
            if (!networkMonitor.isOnline.first()) {
                _events.send(ProfileEvent.ShowMessage(DataError.Network.toUiText()))
            } else {
                // Si sale bien, el snapshot listener trae la URL nueva.
                changeProfilePhoto(uid, uri).onFailure { error ->
                    _events.send(ProfileEvent.ShowMessage(error.toUiText()))
                }
            }
            _uiState.update { it.copy(isUploadingPhoto = false) }
        }
    }
}
