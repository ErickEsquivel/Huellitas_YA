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

    suspend fun signOut()
}
