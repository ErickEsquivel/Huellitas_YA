package com.example.huellitasya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.domain.usecase.ObserveSessionUseCase
import com.example.huellitasya.core.domain.usecase.ShouldShowPetsIntroUseCase
import com.example.huellitasya.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

/** Qué grafo muestra la app (specs 002 y 005). */
sealed interface MainUiState {
    /** Se resuelve la sesión o el destino inicial: el splash (o nada) cubre este estado. */
    data object Loading : MainUiState
    data object SignedOut : MainUiState

    /** @param startWithIntro `true` si la cuenta no tiene mascotas: el grafo principal abre en Presentación. */
    data class SignedIn(val startWithIntro: Boolean) : MainUiState
}

/**
 * Gate de sesión: decide si la app muestra el grafo de auth o el principal (spec 002) y, al
 * entrar, si el principal empieza en Presentación o en Home (spec 005).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val shouldShowPetsIntro: ShouldShowPetsIntroUseCase,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainUiState> = observeSession()
        // Solo cuenta quién inició sesión: si cambian sus datos (nombre, correo), el grafo no se recrea.
        .distinctUntilChanged { old, new -> old.uid == new.uid }
        .transformLatest { session ->
            when (session) {
                SessionState.Loading -> emit(MainUiState.Loading)
                SessionState.SignedOut -> emit(MainUiState.SignedOut)
                is SessionState.SignedIn -> {
                    emit(MainUiState.Loading)
                    // Se decide una vez por sesión: registrar la primera mascota no recrea el grafo.
                    emit(MainUiState.SignedIn(startWithIntro = shouldShowPetsIntro()))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    private val SessionState.uid: String?
        get() = when (this) {
            SessionState.Loading -> LOADING_KEY
            SessionState.SignedOut -> null
            is SessionState.SignedIn -> user.uid
        }

    private companion object {
        const val LOADING_KEY = "loading"
    }
}
