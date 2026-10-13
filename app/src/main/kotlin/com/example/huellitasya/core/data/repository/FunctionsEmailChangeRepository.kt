package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.repository.ConfirmedEmailChange
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

/**
 * [EmailChangeRepository] sobre las Cloud Functions `requestEmailChangeCode` y
 * `confirmEmailChange` (`functions/src/index.ts`). Los códigos de error y `details.reason` son el
 * contrato con el backend.
 */
internal class FunctionsEmailChangeRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : EmailChangeRepository {

    override suspend fun requestCode(newEmail: String): AppResult<Unit, EmailChangeError> =
        call("requestEmailChangeCode", mapOf("newEmail" to newEmail)) { }

    override suspend fun confirm(code: String): AppResult<ConfirmedEmailChange, EmailChangeError> =
        call("confirmEmailChange", mapOf("code" to code)) { data ->
            val map = data as? Map<*, *>
            ConfirmedEmailChange(
                email = checkNotNull(map?.get("email") as? String) { "Respuesta sin email" },
                customToken = checkNotNull(map?.get("customToken") as? String) { "Respuesta sin customToken" },
            )
        }

    private suspend fun <T> call(
        name: String,
        payload: Map<String, Any>,
        parse: (Any?) -> T,
    ): AppResult<T, EmailChangeError> =
        try {
            val result = functions.getHttpsCallable(name).call(payload).await()
            AppResult.Success(parse(result.getData()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toEmailChangeError())
        }

    private fun Exception.toEmailChangeError(): EmailChangeError {
        if (this is FirebaseNetworkException || this is IOException || cause is IOException) {
            return EmailChangeError.Network
        }
        if (this !is FirebaseFunctionsException) return EmailChangeError.Unknown

        val details = details as? Map<*, *>
        val reason = details?.get("reason") as? String
        return when (code) {
            Code.FAILED_PRECONDITION, Code.UNAUTHENTICATED -> EmailChangeError.RequiresRecentLogin
            Code.ALREADY_EXISTS -> EmailChangeError.EmailAlreadyInUse
            Code.INVALID_ARGUMENT -> when (reason) {
                "invalid_code" -> EmailChangeError.InvalidCode
                "invalid_email" -> EmailChangeError.InvalidEmail
                else -> EmailChangeError.Unknown
            }
            Code.DEADLINE_EXCEEDED -> EmailChangeError.ExpiredCode
            Code.RESOURCE_EXHAUSTED -> when (reason) {
                "resend_cooldown" -> EmailChangeError.TooManyRequests(
                    retryAfterSeconds = (details?.get("retryAfterSeconds") as? Number)?.toInt() ?: 60,
                )
                else -> EmailChangeError.TooManyAttempts
            }
            // "unavailable" sin motivo del backend = el dispositivo no llegó al servidor.
            Code.UNAVAILABLE -> if (reason == null) EmailChangeError.Network else EmailChangeError.Unknown
            else -> EmailChangeError.Unknown
        }
    }
}
