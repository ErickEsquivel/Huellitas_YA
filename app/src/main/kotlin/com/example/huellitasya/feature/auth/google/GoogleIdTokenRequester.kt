package com.example.huellitasya.feature.auth.google

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

/**
 * Abre el selector de cuentas de Google (Credential Manager) y devuelve el ID token.
 *
 * Vive en la capa de UI porque Credential Manager necesita el `Context` de un Activity
 * para mostrar su hoja inferior. El token resultante se entrega al ViewModel, que es quien
 * autentica con Firebase mediante `SignInWithGoogleUseCase`.
 *
 * @param serverClientId "ID de cliente web" de Firebase (`R.string.default_web_client_id`).
 *   Es `null` si `google-services.json` aún no trae el cliente OAuth web (spec 001): en ese
 *   caso el inicio con Google falla con un aviso en lugar de impedir que la app compile.
 */
internal class GoogleIdTokenRequester(
    private val activityContext: Context,
    private val serverClientId: String?,
) {
    private val credentialManager = CredentialManager.create(activityContext)

    suspend fun requestIdToken(): AppResult<String, AuthError> {
        if (serverClientId == null) {
            Log.w(TAG, "Falta default_web_client_id: habilita Google en Firebase Auth y vuelve a descargar google-services.json")
            return AppResult.Failure(AuthError.GoogleSignInFailed)
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        return try {
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                AppResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                AppResult.Failure(AuthError.GoogleSignInFailed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            AppResult.Failure(AuthError.Cancelled)
        } catch (e: GetCredentialException) {
            AppResult.Failure(AuthError.GoogleSignInFailed)
        } catch (e: GoogleIdTokenParsingException) {
            AppResult.Failure(AuthError.GoogleSignInFailed)
        }
    }
}

private const val TAG = "GoogleSignIn"

@Composable
internal fun rememberGoogleIdTokenRequester(serverClientId: String?): GoogleIdTokenRequester {
    val context = LocalContext.current
    return remember(context, serverClientId) { GoogleIdTokenRequester(context, serverClientId) }
}
