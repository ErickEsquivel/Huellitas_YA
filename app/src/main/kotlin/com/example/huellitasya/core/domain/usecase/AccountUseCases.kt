package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AuthRepository
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
