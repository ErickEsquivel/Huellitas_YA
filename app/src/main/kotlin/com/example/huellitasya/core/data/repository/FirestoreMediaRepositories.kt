package com.example.huellitasya.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.image.JpegCompressor
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.media.FirestoreMediaStore
import com.example.huellitasya.core.data.media.MediaRef
import com.example.huellitasya.core.data.media.MediaUrls
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import com.example.huellitasya.core.domain.repository.PetPhotoRepository
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.UploadProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/*
 * Modo gratuito de las fotos y los archivos de registros (spec 008): se guardan en Firestore con
 * [FirestoreMediaStore] y su URL es `huellitas-media://…`. Abajo, las versiones que registra Hilt
 * eligen entre Storage y Firestore según el interruptor.
 */

private inline fun <T> runMedia(block: () -> T): AppResult<T, DataError> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toDataError())
    }

/** Comprime la foto igual que para Storage (JPEG de 1024 px como máximo) y la guarda en Firestore. */
private suspend fun FirestoreMediaStore.uploadJpeg(
    ref: MediaRef,
    localUri: String,
    compressor: JpegCompressor,
): AppResult<String, DataError> = runMedia {
    val bytes = compressor.compress(localUri)
    if (bytes.size > JpegCompressor.MAX_BYTES) return AppResult.Failure(DataError.FileTooLarge)
    write(ref, bytes, JpegCompressor.JPEG_CONTENT_TYPE)
    MediaUrls.of(ref, version = System.currentTimeMillis())
}

internal class FirestoreProfilePhotoRepository @Inject constructor(
    private val store: FirestoreMediaStore,
    private val compressor: JpegCompressor,
) : ProfilePhotoRepository {

    override suspend fun upload(uid: String, localUri: String): AppResult<String, DataError> =
        store.uploadJpeg(MediaUrls.profilePhoto(uid), localUri, compressor)
}

internal class FirestorePetPhotoRepository @Inject constructor(
    private val store: FirestoreMediaStore,
    private val compressor: JpegCompressor,
) : PetPhotoRepository {

    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> =
        store.uploadJpeg(MediaUrls.petPhoto(uid, petId), localUri, compressor)

    override suspend fun delete(uid: String, petId: String): AppResult<Unit, DataError> =
        runMedia { store.delete(MediaUrls.petPhoto(uid, petId)) }
}

/** Los archivos van sin comprimir, como en Storage, para que el documento siga legible (spec 006). */
internal class FirestoreRecordFileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: FirestoreMediaStore,
    private val storage: FirebaseStorageRecordFileRepository,
) : RecordFileRepository {

    /** Solo usa el `ContentResolver`, no Storage: sirve igual en los dos modos. */
    override suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError> = storage.inspect(localUri)

    override fun upload(
        uid: String,
        petId: String,
        recordId: String,
        localUri: String,
        contentType: String,
    ): Flow<AppResult<UploadProgress, DataError>> = channelFlow {
        val result = runMedia {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(localUri.toUri())?.use { it.readBytes() }
            } ?: error("No se pudo leer $localUri")
            store.write(MediaUrls.recordFile(uid, petId, recordId), bytes, contentType) { fraction ->
                trySend(AppResult.Success(UploadProgress.InProgress(fraction)))
            }
        }
        send(
            when (result) {
                is AppResult.Success -> AppResult.Success(UploadProgress.Done)
                is AppResult.Failure -> result
            },
        )
    }

    override suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError> =
        AppResult.Success(MediaUrls.of(MediaUrls.recordFile(uid, petId, recordId)))

    override suspend fun delete(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> =
        runMedia { store.delete(MediaUrls.recordFile(uid, petId, recordId)) }
}

internal class ModeAwareProfilePhotoRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val storage: FirebaseStorageProfilePhotoRepository,
    private val firestore: FirestoreProfilePhotoRepository,
) : ProfilePhotoRepository {

    override suspend fun upload(uid: String, localUri: String): AppResult<String, DataError> =
        if (mode.current() == BackendMode.Blaze) storage.upload(uid, localUri) else firestore.upload(uid, localUri)
}

/**
 * Borrar limpia los dos lugares: una foto subida en un modo puede borrarse en el otro. En el modo
 * gratuito no se toca Storage, porque no responde.
 */
internal class ModeAwarePetPhotoRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val storage: FirebaseStoragePetPhotoRepository,
    private val firestore: FirestorePetPhotoRepository,
) : PetPhotoRepository {

    override suspend fun upload(uid: String, petId: String, localUri: String): AppResult<String, DataError> =
        if (mode.current() == BackendMode.Blaze) storage.upload(uid, petId, localUri) else firestore.upload(uid, petId, localUri)

    override suspend fun delete(uid: String, petId: String): AppResult<Unit, DataError> {
        if (mode.current() == BackendMode.Blaze) {
            val fromStorage = storage.delete(uid, petId)
            if (fromStorage is AppResult.Failure) return fromStorage
        }
        return firestore.delete(uid, petId)
    }
}

internal class ModeAwareRecordFileRepository @Inject constructor(
    private val mode: BackendModeProvider,
    private val storage: FirebaseStorageRecordFileRepository,
    private val firestore: FirestoreRecordFileRepository,
) : RecordFileRepository {

    private val active: RecordFileRepository
        get() = if (mode.current() == BackendMode.Blaze) storage else firestore

    override suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError> = storage.inspect(localUri)

    override fun upload(
        uid: String,
        petId: String,
        recordId: String,
        localUri: String,
        contentType: String,
    ): Flow<AppResult<UploadProgress, DataError>> = active.upload(uid, petId, recordId, localUri, contentType)

    override suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError> =
        active.downloadUrl(uid, petId, recordId)

    override suspend fun delete(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> {
        if (mode.current() == BackendMode.Blaze) {
            val fromStorage = storage.delete(uid, petId, recordId)
            if (fromStorage is AppResult.Failure) return fromStorage
        }
        return firestore.delete(uid, petId, recordId)
    }
}
