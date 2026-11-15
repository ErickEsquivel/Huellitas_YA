package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.repository.ConfirmedEmailChange
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import com.example.huellitasya.core.domain.repository.PasswordResetRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Espera mínima antes de reenviar un enlace, igual que el reenvío del código (spec 002). */
private const val LINK_RESEND_SECONDS = 60

/**
 * Modo gratuito de la recuperación de contraseña (spec 008): Firebase Auth envía un enlace y la
 * contraseña nueva se crea en su página. No hay código ni token, así que [verifyCode] y
 * [resetPassword] no se usan en este modo.
 */
internal class AuthLinkPasswordResetRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : PasswordResetRepository {

    override suspend fun requestCode(email: String): AppResult<Unit, PasswordResetError> =
        try {
            auth.sendPasswordResetEmail(email).await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthInvalidUserException) {
            // Sin la protección contra enumeración, Firebase avisa que la cuenta no existe.
            // Por privacidad se responde igual que si existiera (spec 002).
            AppResult.Success(Unit)
        } catch (_: FirebaseTooManyRequestsException) {
            AppResult.Failure(PasswordResetError.TooManyRequests(LINK_RESEND_SECONDS))
        } catch (_: FirebaseNetworkException) {
            AppResult.Failure(PasswordResetError.Network)
        } catch (_: Exception) {
            AppResult.Failure(PasswordResetError.Unknown)
        }

    override suspend fun verifyCode(email: String, code: String): AppResult<String, PasswordResetError> =
        AppResult.Failure(PasswordResetError.Unknown)

    override suspend fun resetPassword(email: String, resetToken: String, newPassword: String): AppResult<Unit, PasswordResetError> =
        AppResult.Failure(PasswordResetError.Unknown)
}

/**
 * Modo gratuito del cambio de correo (spec 008): `verifyBeforeUpdateEmail` envía un enlace al
 * correo nuevo y el cambio se aplica cuando el usuario lo abre. No hay código, así que [confirm]
 * no se usa en este modo.
 */
internal class AuthLinkEmailChangeRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : EmailChangeRepository {

    override suspend fun requestCode(newEmail: String): AppResult<Unit, EmailChangeError> {
        val user = auth.currentUser ?: return AppResult.Failure(EmailChangeError.RequiresRecentLogin)
        if (newEmail.equals(user.email, ignoreCase = true)) return AppResult.Failure(EmailChangeError.InvalidEmail)
        return try {
            user.verifyBeforeUpdateEmail(newEmail).await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            AppResult.Failure(EmailChangeError.RequiresRecentLogin)
        } catch (_: FirebaseAuthUserCollisionException) {
            AppResult.Failure(EmailChangeError.EmailAlreadyInUse)
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            AppResult.Failure(EmailChangeError.InvalidEmail)
        } catch (_: FirebaseTooManyRequestsException) {
            AppResult.Failure(EmailChangeError.TooManyRequests(LINK_RESEND_SECONDS))
        } catch (_: FirebaseNetworkException) {
            AppResult.Failure(EmailChangeError.Network)
        } catch (_: Exception) {
            AppResult.Failure(EmailChangeError.Unknown)
        }
    }

    override suspend fun confirm(code: String): AppResult<ConfirmedEmailChange, EmailChangeError> =
        AppResult.Failure(EmailChangeError.Unknown)
}
