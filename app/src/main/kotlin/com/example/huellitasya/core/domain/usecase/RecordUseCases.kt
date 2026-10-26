package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.common.map
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.domain.repository.RecordRepository
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordDraft
import com.example.huellitasya.core.model.UploadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Registros de la mascota del usuario con sesión, en vivo (spec 006). Sin sesión no emite nada. */
class ObservePetRecordsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val recordRepository: RecordRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(petId: String): Flow<AppResult<List<MedicalRecord>, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user -> if (user == null) emptyFlow() else recordRepository.observeRecords(user.uid, petId) }
}

/** Los registros de una categoría, del más nuevo al más antiguo (la lista de la spec 006). */
class ObserveRecordsUseCase @Inject constructor(
    private val observePetRecords: ObservePetRecordsUseCase,
) {
    operator fun invoke(petId: String, category: RecordCategory): Flow<AppResult<List<MedicalRecord>, DataError>> =
        observePetRecords(petId).map { result -> result.map { records -> records.filter { it.category == category } } }
}

/** Cuántos archivos hay en cada categoría: el conteo de las tarjetas del perfil de la mascota. */
class ObserveRecordCountsUseCase @Inject constructor(
    private val observePetRecords: ObservePetRecordsUseCase,
) {
    operator fun invoke(petId: String): Flow<AppResult<Map<RecordCategory, Int>, DataError>> =
        observePetRecords(petId).map { result ->
            result.map { records -> RecordCategory.entries.associateWith { category -> records.count { it.category == category } } }
        }
}

/** Nombre, tipo y tamaño del archivo elegido en el selector, antes de subirlo. */
class InspectRecordFileUseCase @Inject constructor(
    private val fileRepository: RecordFileRepository,
) {
    suspend operator fun invoke(localUri: String): AppResult<LocalFileInfo, DataError> = fileRepository.inspect(localUri)
}

/**
 * Sube un registro médico (spec 006): genera su id, sube el archivo informando el avance a
 * [onProgress] (0–1), pide su URL y recién entonces crea el documento. Si algo falla después de
 * subir el archivo, lo borra (best-effort) para no dejar archivos huérfanos. Corre en
 * [ApplicationScope]: si el usuario sale de la pantalla, la subida termina igual.
 */
class UploadRecordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val recordRepository: RecordRepository,
    private val fileRepository: RecordFileRepository,
    @param:ApplicationScope private val appScope: CoroutineScope,
) {
    suspend operator fun invoke(
        petId: String,
        draft: RecordDraft,
        localUri: String,
        file: LocalFileInfo,
        onProgress: (Float) -> Unit,
    ): AppResult<Unit, DataError> = appScope.async {
        val uid = authRepository.currentUser.first()?.uid
            ?: return@async AppResult.Failure(DataError.PermissionDenied)
        val recordId = recordRepository.newRecordId(uid, petId)

        var uploadError: DataError? = null
        fileRepository.upload(uid, petId, recordId, localUri, file.contentType).collect { event ->
            when (event) {
                is AppResult.Success -> (event.data as? UploadProgress.InProgress)?.let { onProgress(it.fraction) }
                is AppResult.Failure -> uploadError = event.error
            }
        }
        uploadError?.let { return@async AppResult.Failure(it) }
        onProgress(1f)

        val fileUrl = when (val url = fileRepository.downloadUrl(uid, petId, recordId)) {
            is AppResult.Success -> url.data
            is AppResult.Failure -> {
                fileRepository.delete(uid, petId, recordId)
                return@async url
            }
        }
        recordRepository.createRecord(uid, petId, recordId, draft, file, fileUrl).also { result ->
            if (result is AppResult.Failure) fileRepository.delete(uid, petId, recordId)
        }
    }.await()
}
