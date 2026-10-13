package com.example.huellitasya.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
)

sealed interface ProfileEvent {
    data class ShowMessage(val message: UiText) : ProfileEvent
}

/** Pestaña Perfil (spec 004): muestra en vivo `users/{uid}`. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
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
}
