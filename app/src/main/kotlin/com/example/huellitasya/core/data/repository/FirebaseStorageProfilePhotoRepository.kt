package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.image.JpegCompressor
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

/** Sube la foto de perfil a `users/{uid}/profile.jpg` (spec 004, "Foto de perfil"). */
internal class FirebaseStorageProfilePhotoRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val compressor: JpegCompressor,
) : ProfilePhotoRepository {

    override suspend fun upload(uid: String, localUri: String): AppResult<String, DataError> =
        uploadJpeg(storage.reference.child("users/$uid/profile.jpg"), localUri, compressor)
}
