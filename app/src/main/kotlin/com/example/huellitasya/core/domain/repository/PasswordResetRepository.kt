package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.PasswordResetError

/**
 * Restablecimiento de contraseña con un código de 4 dígitos enviado por correo.
 * Lo implementa el backend (Cloud Functions), porque cambiar la contraseña de un usuario sin
 * sesión requiere el Admin SDK.
 */
interface PasswordResetRepository {
    /** Envía el código al correo. Responde igual exista o no la cuenta (privacidad). */
    suspend fun requestCode(email: String): AppResult<Unit, PasswordResetError>

    /** Valida el código y devuelve un token de un solo uso para cambiar la contraseña. */
    suspend fun verifyCode(email: String, code: String): AppResult<String, PasswordResetError>

    suspend fun resetPassword(email: String, resetToken: String, newPassword: String): AppResult<Unit, PasswordResetError>
}
