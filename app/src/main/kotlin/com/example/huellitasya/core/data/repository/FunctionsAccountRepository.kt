package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

/**
 * [AccountRepository] sobre la Cloud Function `deleteAccount` (`functions/src/index.ts`).
 * El código de error y `details.reason` son el contrato con el backend.
 */
internal class FunctionsAccountRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AccountRepository {

    override suspend fun deleteAccount(): AppResult<Unit, AuthError> =
        try {
            functions.getHttpsCallable("deleteAccount").call().await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toAuthError())
        }

    private fun Exception.toAuthError(): AuthError {
        if (this is FirebaseNetworkException || this is IOException || cause is IOException) {
            return AuthError.Network
        }
        if (this !is FirebaseFunctionsException) return AuthError.Unknown

        val reason = (details as? Map<*, *>)?.get("reason") as? String
        return when (code) {
            Code.FAILED_PRECONDITION -> if (reason == "requires_recent_login") AuthError.RequiresRecentLogin else AuthError.Unknown
            Code.UNAUTHENTICATED -> AuthError.RequiresRecentLogin
            // "unavailable" sin motivo del backend = el dispositivo no llegó al servidor.
            Code.UNAVAILABLE -> if (reason == null) AuthError.Network else AuthError.Unknown
            else -> AuthError.Unknown
        }
    }
}
