package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.image.JpegCompressor
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Fotos de mascotas en `users/{uid}/pets/{petId}.jpg` (spec 005). */
internal class FirebaseStoragePetPhotoRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val compressor: JpegCompressor,
) : PetPhotoRepository {

    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> =
        uploadJpeg(photoReference(uid, petId), localUri, compressor)

    override suspend fun delete(uid: String, petId: String): AppResult<Unit, DataError> =
        try {
            photoReference(uid, petId).delete().await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            // Sin foto no hay nada que borrar.
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) AppResult.Success(Unit) else AppResult.Failure(e.toDataError())
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }

    private fun photoReference(uid: String, petId: String) = storage.reference.child("users/$uid/pets/$petId.jpg")
}
