package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.onSuccess
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.example.huellitasya.core.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject

/** Cambio de correo con verificación (spec 004, "Cambio de correo"). */
class RequestEmailChangeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(newEmail: String): AppResult<Unit, AuthError> =
        authRepository.requestEmailChange(newEmail.trim())
}

/** Reautenticación antes de cambiar el correo o eliminar la cuenta (spec 004). */
class ReauthenticateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend fun withPassword(password: String): AppResult<Unit, AuthError> =
        authRepository.reauthenticateWithPassword(password)

    suspend fun withGoogle(idToken: String): AppResult<Unit, AuthError> =
        authRepository.reauthenticateWithGoogle(idToken)
}

/**
 * Elimina la cuenta (spec 004, "Eliminar cuenta") y cierra la sesión local, porque el borrado en
 * el servidor no la invalida al instante. Corre en [ApplicationScope]: al cambiar la sesión se
 * destruye la pantalla que lo pidió.
 */
class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(): AppResult<Unit, AuthError> =
        appScope.async {
            accountRepository.deleteAccount().onSuccess { authRepository.signOut() }
        }.await()
}
