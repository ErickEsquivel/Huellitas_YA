package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emite el usuario autenticado actual, o `null` si no hay sesión. */
    val currentUser: Flow<User?>

    suspend fun signInWithEmail(email: String, password: String): AppResult<User, AuthError>

    suspend fun registerWithEmail(name: String, email: String, password: String): AppResult<User, AuthError>

    /** Autentica con el ID token obtenido del selector de cuentas de Google. */
    suspend fun signInWithGoogle(idToken: String): AppResult<User, AuthError>

    /** Confirma la identidad con la contraseña antes de una operación sensible (spec 004). */
    suspend fun reauthenticateWithPassword(password: String): AppResult<Unit, AuthError>

    /** Confirma la identidad con el ID token de Google antes de una operación sensible (spec 004). */
    suspend fun reauthenticateWithGoogle(idToken: String): AppResult<Unit, AuthError>

    /**
     * Envía un enlace de verificación a [newEmail]. El correo de la cuenta cambia solo cuando
     * el usuario lo abre (`verifyBeforeUpdateEmail`).
     */
    suspend fun requestEmailChange(newEmail: String): AppResult<Unit, AuthError>

    /** Mantiene el nombre de Auth igual al del perfil, para que `User.name` de la sesión no quede desfasado. */
    suspend fun updateDisplayName(name: String): AppResult<Unit, AuthError>

    suspend fun signOut()
}
