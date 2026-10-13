package com.example.huellitasya.feature.auth.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qué muestra el overlay "Sin conexión" (Figma "ERROR INTERNET Overlay"). */
enum class ConnectionUiState {
    /** Hay internet: no se muestra nada. */
    Online,

    /** Estado "error": "¡Oh No! Sin Conexión" + "Reintentar". */
    Offline,

    /** Estado "loading": spinner + "Conectando...". */
    Reconnecting,
}

/**
 * Estado de conexión compartido por todas las pantallas de autenticación (spec 002, "Sin conexión").
 * Vive en la entrada del grafo `AuthGraph`, así el overlay no se reinicia al navegar entre pantallas.
 *
 * El overlay aparece si el sistema no tiene internet validado o si Firebase respondió con error de
 * red (p. ej. un wifi que el sistema da por bueno pero no tiene salida).
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val networkErrorReported = MutableStateFlow(false)
    private val isReconnecting = MutableStateFlow(false)

    val uiState: StateFlow<ConnectionUiState> = combine(
        networkMonitor.isOnline,
        networkErrorReported,
        isReconnecting,
    ) { isOnline, errorReported, reconnecting ->
        when {
            reconnecting -> ConnectionUiState.Reconnecting
            !isOnline || errorReported -> ConnectionUiState.Offline
            else -> ConnectionUiState.Online
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ConnectionUiState.Online)

    init {
        // Cuando el sistema recupera la red, el error reportado deja de ser válido.
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline -> if (isOnline) networkErrorReported.value = false }
        }
    }

    /** Las pantallas lo llaman cuando una operación de Firebase falla por red (`AuthError.Network`). */
    fun onNetworkError() {
        networkErrorReported.value = true
    }

    /** "Reintentar": muestra "Conectando..." y cierra el overlay si hay internet; si no, vuelve al error. */
    fun onRetry() {
        if (isReconnecting.value) return
        viewModelScope.launch {
            isReconnecting.value = true
            val isOnline = networkMonitor.isOnline.first()
            // Tiempo mínimo para que "Conectando..." se alcance a ver y el botón no parezca no hacer nada.
            delay(MIN_RECONNECTING_MILLIS)
            if (isOnline) networkErrorReported.value = false
            isReconnecting.value = false
        }
    }

    internal companion object {
        const val MIN_RECONNECTING_MILLIS = 1_200L
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
