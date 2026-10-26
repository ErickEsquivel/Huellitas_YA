package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.RecordFileRepository
import com.example.huellitasya.core.domain.repository.RecordRepository
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordDraft
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.model.UploadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

val TestRecord = MedicalRecord(
    id = "record-1",
    petId = TestPet.id,
    category = RecordCategory.VACCINES,
    fileName = "Vacuna_Rabia_2026.pdf",
    contentType = "application/pdf",
    sizeBytes = 120_000,
    date = LocalDate.of(2026, 9, 10),
    type = RecordType.VACCINATION,
    vet = "Dr. A. Ramírez",
    fileUrl = "https://storage.test/record-1",
)

val TestLocalFile = LocalFileInfo(name = "Carnet.pdf", contentType = "application/pdf", sizeBytes = 200_000)

/**
 * [RecordRepository] en memoria. [records] hace de colección `.../records`: crear o borrar un
 * registro la cambia y la emite, igual que el snapshot listener de Firestore.
 */
class FakeRecordRepository : RecordRepository {
    val records = MutableStateFlow<AppResult<List<MedicalRecord>, DataError>>(AppResult.Success(emptyList()))
    var writeResult: AppResult<Unit, DataError> = AppResult.Success(Unit)
    val created = mutableListOf<Triple<String, RecordDraft, LocalFileInfo>>()
    val deleted = mutableListOf<String>()
    private var nextId = 1

    override fun observeRecords(uid: String, petId: String): Flow<AppResult<List<MedicalRecord>, DataError>> = records

    override fun newRecordId(uid: String, petId: String): String = "record-${nextId++}"

    override suspend fun createRecord(
        uid: String,
        petId: String,
        recordId: String,
        draft: RecordDraft,
        file: LocalFileInfo,
        fileUrl: String,
    ): AppResult<Unit, DataError> {
        created += Triple(recordId, draft, file)
        val current = (records.value as? AppResult.Success)?.data
        if (writeResult is AppResult.Success && current != null) {
            val record = MedicalRecord(recordId, petId, draft.category, file.name, file.contentType, file.sizeBytes, draft.date, draft.type, draft.vet, fileUrl)
            records.value = AppResult.Success(listOf(record) + current)
        }
        return writeResult
    }

    override suspend fun deleteRecord(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> {
        deleted += recordId
        val current = (records.value as? AppResult.Success)?.data
        if (writeResult is AppResult.Success && current != null) {
            records.value = AppResult.Success(current.filterNot { it.id == recordId })
        }
        return writeResult
    }
}

class FakeRecordFileRepository : RecordFileRepository {
    var inspectResult: AppResult<LocalFileInfo, DataError> = AppResult.Success(TestLocalFile)

    /** Lo que emite cada subida, en orden. */
    var uploadEvents: List<AppResult<UploadProgress, DataError>> = listOf(
        AppResult.Success(UploadProgress.InProgress(0.5f)),
        AppResult.Success(UploadProgress.Done),
    )
    var urlResult: AppResult<String, DataError> = AppResult.Success("https://storage.test/record")
    var deleteResult: AppResult<Unit, DataError> = AppResult.Success(Unit)

    /** (recordId, localUri) de cada subida. */
    val uploads = mutableListOf<Pair<String, String>>()
    val deletedFiles = mutableListOf<String>()

    override suspend fun inspect(localUri: String): AppResult<LocalFileInfo, DataError> = inspectResult

    override fun upload(
        uid: String,
        petId: String,
        recordId: String,
        localUri: String,
        contentType: String,
    ): Flow<AppResult<UploadProgress, DataError>> {
        uploads += recordId to localUri
        return flowOf(*uploadEvents.toTypedArray())
    }

    override suspend fun downloadUrl(uid: String, petId: String, recordId: String): AppResult<String, DataError> = urlResult

    override suspend fun delete(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> {
        deletedFiles += recordId
        return deleteResult
    }
}
