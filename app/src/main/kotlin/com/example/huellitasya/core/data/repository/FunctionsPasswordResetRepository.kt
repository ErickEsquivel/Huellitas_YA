package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.repository.PasswordResetRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

/**
 * [PasswordResetRepository] sobre las Cloud Functions de `functions/src/index.ts`
 * (región `southamerica-east1`). Los códigos de error y el campo `details.reason` son el
 * contrato con el backend.
 */
internal class FunctionsPasswordResetRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : PasswordResetRepository {

    override suspend fun requestCode(email: String): AppResult<Unit, PasswordResetError> =
        call("requestPasswordResetCode", mapOf("email" to email)) { }

    override suspend fun verifyCode(email: String, code: String): AppResult<String, PasswordResetError> =
        call("verifyPasswordResetCode", mapOf("email" to email, "code" to code)) { data ->
            checkNotNull((data as? Map<*, *>)?.get("resetToken") as? String) { "Respuesta sin resetToken" }
        }

    override suspend fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String,
    ): AppResult<Unit, PasswordResetError> =
        call(
            "resetPasswordWithToken",
            mapOf("email" to email, "resetToken" to resetToken, "newPassword" to newPassword),
        ) { }

    private suspend fun <T> call(
        name: String,
        payload: Map<String, Any>,
        parse: (Any?) -> T,
    ): AppResult<T, PasswordResetError> =
        try {
            val result = functions.getHttpsCallable(name).call(payload).await()
            AppResult.Success(parse(result.getData()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toPasswordResetError())
        }

    private fun Exception.toPasswordResetError(): PasswordResetError {
        if (this is FirebaseNetworkException || this is IOException || cause is IOException) {
            return PasswordResetError.Network
        }
        if (this !is FirebaseFunctionsException) return PasswordResetError.Unknown

        val details = details as? Map<*, *>
        val reason = details?.get("reason") as? String
        return when (code) {
            Code.INVALID_ARGUMENT -> when (reason) {
                "weak_password" -> PasswordResetError.WeakPassword
                "invalid_code" -> PasswordResetError.InvalidCode
                else -> PasswordResetError.Unknown
            }
            Code.DEADLINE_EXCEEDED, Code.PERMISSION_DENIED -> PasswordResetError.ExpiredCode
            Code.RESOURCE_EXHAUSTED -> when (reason) {
                "resend_cooldown" -> PasswordResetError.TooManyRequests(
                    retryAfterSeconds = (details?.get("retryAfterSeconds") as? Number)?.toInt() ?: 60,
                )
                else -> PasswordResetError.TooManyAttempts
            }
            // "unavailable" sin motivo del backend = el dispositivo no llegó al servidor.
            Code.UNAVAILABLE -> if (reason == null) PasswordResetError.Network else PasswordResetError.Unknown
            else -> PasswordResetError.Unknown
        }
    }
}
