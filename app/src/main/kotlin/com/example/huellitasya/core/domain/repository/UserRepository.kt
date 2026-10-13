package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.User
import com.example.huellitasya.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** Crea el documento `users/{uid}` solo si todavía no existe (no pisa `createdAt`). */
    suspend fun createIfMissing(user: User): AppResult<Unit, DataError>

    /** Emite el perfil cada vez que cambia `users/{uid}`; sin red, desde la caché. */
    fun observeProfile(uid: String): Flow<AppResult<UserProfile, DataError>>

    /** Guarda solo los campos de [changes] y actualiza `updatedAt`. */
    suspend fun updateProfile(uid: String, changes: ProfileChanges): AppResult<Unit, DataError>

    /** Copia en el perfil el correo de Auth si cambió (tras confirmar un cambio de correo, spec 004). */
    suspend fun syncEmail(uid: String, email: String): AppResult<Unit, DataError>
}
