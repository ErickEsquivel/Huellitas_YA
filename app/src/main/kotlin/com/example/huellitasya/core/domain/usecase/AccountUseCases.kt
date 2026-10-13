package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.onSuccess
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import javax.inject.Inject

/** Longitud del código del cambio de correo: la misma que la recuperación de contraseña. */
const val EMAIL_CHANGE_CODE_LENGTH = PASSWORD_RESET_CODE_LENGTH

/** Paso 1 del cambio de correo (spec 004): envía el código al correo nuevo. */
class RequestEmailChangeCodeUseCase @Inject constructor(
    private val emailChangeRepository: EmailChangeRepository,
) {
    suspend operator fun invoke(newEmail: String): AppResult<Unit, EmailChangeError> =
        emailChangeRepository.requestCode(newEmail.trim())
}

/**
 * Paso 2 del cambio de correo: valida el código. El backend ya cambió el correo en Auth y en
 * Firestore; aquí solo se recarga el usuario de la sesión para que tenga el correo nuevo.
 */
class ConfirmEmailChangeUseCase @Inject constructor(
    private val emailChangeRepository: EmailChangeRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(code: String): AppResult<String, EmailChangeError> =
        emailChangeRepository.confirm(code).onSuccess { authRepository.reload() }
}

/** Reautenticación antes de cambiar el correo o la contraseña, o de eliminar la cuenta (spec 004). */
class ReauthenticateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend fun withPassword(password: String): AppResult<Unit, AuthError> =
        authRepository.reauthenticateWithPassword(password)

    suspend fun withGoogle(idToken: String): AppResult<Unit, AuthError> =
        authRepository.reauthenticateWithGoogle(idToken)
}

/**
 * Seguridad (spec 004): confirma la contraseña actual, cambia la contraseña y envía el aviso por
 * correo. Una contraseña actual incorrecta falla con [AuthError.InvalidCredentials]. El aviso es
 * best-effort: si no sale, el cambio sigue siendo válido.
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): AppResult<Unit, AuthError> {
        val reauth = authRepository.reauthenticateWithPassword(currentPassword)
        if (reauth is AppResult.Failure) return reauth
        return authRepository.updatePassword(newPassword).onSuccess {
            accountRepository.notifyPasswordChanged()
        }
    }
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
