package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError

/** Foto de perfil en Firebase Storage (spec 004). */
interface ProfilePhotoRepository {
    /**
     * Comprime la imagen elegida y la sube a `users/{uid}/profile.jpg`.
     *
     * @param localUri `content://` que devuelve el selector de fotos; la capa de datos lo resuelve,
     *   así el dominio no depende de `android.net.Uri`.
     * @return la URL de descarga para guardar en `photoUrl`.
     */
    suspend fun upload(uid: String, localUri: String): AppResult<String, DataError>
}
