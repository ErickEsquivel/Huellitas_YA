package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.User

interface UserRepository {
    /** Crea el documento `users/{uid}` solo si todavía no existe (no pisa `createdAt`). */
    suspend fun createIfMissing(user: User): AppResult<Unit, DataError>
}
