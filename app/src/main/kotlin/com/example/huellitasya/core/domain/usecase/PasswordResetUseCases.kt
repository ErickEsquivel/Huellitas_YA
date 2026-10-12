package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.repository.PasswordResetRepository
import javax.inject.Inject

/** Longitud del código que llega por correo (Figma "Verifica tu correo"). */
const val PASSWORD_RESET_CODE_LENGTH = 4

class RequestPasswordResetCodeUseCase @Inject constructor(
    private val repository: PasswordResetRepository,
) {
    suspend operator fun invoke(email: String): AppResult<Unit, PasswordResetError> =
        repository.requestCode(email.trim())
}

class VerifyPasswordResetCodeUseCase @Inject constructor(
    private val repository: PasswordResetRepository,
) {
    /** Devuelve el token para [ResetPasswordUseCase]. */
    suspend operator fun invoke(email: String, code: String): AppResult<String, PasswordResetError> =
        repository.verifyCode(email.trim(), code)
}

class ResetPasswordUseCase @Inject constructor(
    private val repository: PasswordResetRepository,
) {
    suspend operator fun invoke(email: String, resetToken: String, newPassword: String): AppResult<Unit, PasswordResetError> =
        repository.resetPassword(email.trim(), resetToken, newPassword)
}
