package com.example.huellitasya.core.model

/** Estado de la sesión que decide qué grafo de navegación muestra la app. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: User) : SessionState
}
