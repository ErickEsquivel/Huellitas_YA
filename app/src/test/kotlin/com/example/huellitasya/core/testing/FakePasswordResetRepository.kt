package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.repository.PasswordResetRepository

/** [PasswordResetRepository] en memoria: cada operación devuelve el resultado configurado. */
class FakePasswordResetRepository : PasswordResetRepository {
    var requestResult: AppResult<Unit, PasswordResetError> = AppResult.Success(Unit)
    var verifyResult: AppResult<String, PasswordResetError> = AppResult.Success("token-123")
    var resetResult: AppResult<Unit, PasswordResetError> = AppResult.Success(Unit)

    val requestedEmails = mutableListOf<String>()
    val verifyCalls = mutableListOf<Pair<String, String>>()
    val resetCalls = mutableListOf<Triple<String, String, String>>()

    override suspend fun requestCode(email: String): AppResult<Unit, PasswordResetError> {
        requestedEmails += email
        return requestResult
    }

    override suspend fun verifyCode(email: String, code: String): AppResult<String, PasswordResetError> {
        verifyCalls += email to code
        return verifyResult
    }

    override suspend fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String,
    ): AppResult<Unit, PasswordResetError> {
        resetCalls += Triple(email, resetToken, newPassword)
        return resetResult
    }
}
