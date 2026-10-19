package com.example.huellitasya.core.domain.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError

/** Fotos de mascotas en Firebase Storage (spec 005). */
interface PetPhotoRepository {
    /**
     * Comprime la imagen elegida y la sube a `users/{uid}/pets/{petId}.jpg`.
     *
     * @param localUri `content://` del selector de fotos (igual que en [ProfilePhotoRepository]).
     * @return la URL de descarga para guardar en `photoUrl`.
     */
    suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError>

    /** Borra la foto de la mascota. Si no tenía foto, también es un éxito. */
    suspend fun delete(uid: String, petId: String): AppResult<Unit, DataError>
}
