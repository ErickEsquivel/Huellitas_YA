package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.image.JpegCompressor
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject

/** Fotos de mascotas en `users/{uid}/pets/{petId}.jpg` (spec 005). */
internal class FirebaseStoragePetPhotoRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val compressor: JpegCompressor,
) : PetPhotoRepository {

    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> =
        uploadJpeg(photoReference(uid, petId), localUri, compressor)

    private fun photoReference(uid: String, petId: String) = storage.reference.child("users/$uid/pets/$petId.jpg")
}
