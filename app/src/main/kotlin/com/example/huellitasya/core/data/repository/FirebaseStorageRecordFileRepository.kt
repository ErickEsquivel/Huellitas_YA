package com.example.huellitasya.core.data.repository

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.UploadProgress
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Archivos de los registros médicos en `users/{uid}/pets/{petId}/records/{recordId}` (spec 006). */
internal class FirebaseStorageRecordFileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
) : RecordFileRepository {

    private fun fileReference(uid: String, petId: String, recordId: String) =
        storage.reference.child("users/$uid/pets/$petId/records/$recordId")

    override suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError> = withContext(Dispatchers.IO) {
        try {
            val uri = localUri.toUri()
            val resolver = context.contentResolver
            var name: String? = null
            var size = -1L
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
            val contentType = resolver.getType(uri)
            if (name.isNullOrBlank() || contentType == null || size < 0) {
                AppResult.Failure(DataError.Unknown)
            } else {
                AppResult.Success(LocalFileInfo(name = name!!, contentType = contentType, sizeBytes = size))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }
    }

    override fun upload(
        uid: String,
        petId: String,
        recordId: String,
        localUri: String,
        contentType: String,
    ): Flow<AppResult<UploadProgress, DataError>> = callbackFlow {
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()
        // `putFile` lee el `content://` y conoce el tamaño total, así el avance es real.
        val task = fileReference(uid, petId, recordId).putFile(localUri.toUri(), metadata)
        task.addOnProgressListener { snapshot ->
            if (snapshot.totalByteCount > 0) {
                trySend(AppResult.Success(UploadProgress.InProgress(snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount)))
            }
        }.addOnSuccessListener {
            trySend(AppResult.Success(UploadProgress.Done))
            close()
        }.addOnFailureListener { error ->
            trySend(AppResult.Failure(error.toDataError()))
            close()
        }
        awaitClose { if (task.isInProgress) task.cancel() }
    }

    override suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError> =
        try {
            AppResult.Success(fileReference(uid, petId, recordId).downloadUrl.await().toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }

    override suspend fun delete(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> =
        try {
            fileReference(uid, petId, recordId).delete().await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: StorageException) {
            // Si el archivo ya no existe, no hay nada que borrar.
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) AppResult.Success(Unit) else AppResult.Failure(e.toDataError())
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }
}
