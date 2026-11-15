package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.error.PasswordResetError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import com.example.huellitasya.core.domain.repository.ConfirmedEmailChange
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import com.example.huellitasya.core.domain.repository.PasswordResetRepository
import javax.inject.Inject

/*
 * Implementaciones que se registran en Hilt (spec 008): en cada llamada consultan el interruptor
 * y delegan en la versión con Cloud Functions (modo Blaze) o en la gratuita (modo gratuito).
 * Las dos versiones siguen existiendo; ninguna se borra.
 */

internal class ModeAwarePasswordResetRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val functions: FunctionsPasswordResetRepository,
    private val authLink: AuthLinkPasswordResetRepository,
) : PasswordResetRepository {

    private val active: PasswordResetRepository
        get() = if (mode.current() == BackendMode.Blaze) functions else authLink

    override suspend fun requestCode(email: String): AppResult<Unit, PasswordResetError> = active.requestCode(email)

    override suspend fun verifyCode(email: String, code: String): AppResult<String, PasswordResetError> =
        active.verifyCode(email, code)

    override suspend fun resetPassword(email: String, resetToken: String, newPassword: String): AppResult<Unit, PasswordResetError> =
        active.resetPassword(email, resetToken, newPassword)
}

internal class ModeAwareEmailChangeRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val functions: FunctionsEmailChangeRepository,
    private val authLink: AuthLinkEmailChangeRepository,
) : EmailChangeRepository {

    private val active: EmailChangeRepository
        get() = if (mode.current() == BackendMode.Blaze) functions else authLink

    override suspend fun requestCode(newEmail: String): AppResult<Unit, EmailChangeError> = active.requestCode(newEmail)

    override suspend fun confirm(code: String): AppResult<ConfirmedEmailChange, EmailChangeError> = active.confirm(code)
}

internal class ModeAwareAccountRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val functions: FunctionsAccountRepository,
    private val client: ClientAccountRepository,
) : AccountRepository {

    private val active: AccountRepository
        get() = if (mode.current() == BackendMode.Blaze) functions else client

    override suspend fun deleteAccount(): AppResult<Unit, AuthError> = active.deleteAccount()

    override suspend fun notifyPasswordChanged(): AppResult<Unit, AuthError> = active.notifyPasswordChanged()
}
