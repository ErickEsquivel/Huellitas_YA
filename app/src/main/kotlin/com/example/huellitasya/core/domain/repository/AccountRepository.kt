package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError

/**
 * Eliminación de la cuenta (spec 004). La hace el backend (Cloud Function `deleteAccount`)
 * porque borrar la foto, el perfil y el usuario de Auth juntos requiere el Admin SDK.
 */
interface AccountRepository {
    /**
     * Borra la foto, `users/{uid}` y el usuario de Auth. Falla con
     * [AuthError.RequiresRecentLogin] si el inicio de sesión no es reciente.
     */
    suspend fun deleteAccount(): AppResult<Unit, AuthError>

    /** Envía el correo "Tu contraseña cambió" (Cloud Function `notifyPasswordChanged`). */
    suspend fun notifyPasswordChanged(): AppResult<Unit, AuthError>
}
