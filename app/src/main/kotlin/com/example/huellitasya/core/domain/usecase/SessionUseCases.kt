package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Traduce el usuario autenticado a [SessionState]. `Loading` lo aporta quien lo observa como valor inicial. */
class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<SessionState> =
        authRepository.currentUser
            .map { user -> if (user == null) SessionState.SignedOut else SessionState.SignedIn(user) }
            .distinctUntilChanged()
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
