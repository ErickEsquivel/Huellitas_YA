package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.mapper.toMedicalRecord
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.RecordRepository
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordDraft
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

/** `users/{uid}/pets/{petId}/records/{recordId}` (spec 006). */
internal class FirestoreRecordRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RecordRepository {

    private fun recordsCollection(uid: String, petId: String) =
        firestore.collection(USERS).document(uid).collection(PETS).document(petId).collection(RECORDS)

    override fun observeRecords(uid: String, petId: String): Flow<AppResult<List<MedicalRecord>, DataError>> = callbackFlow {
        val registration = recordsCollection(uid, petId).addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())
                snapshot == null -> AppResult.Success(emptyList())
                // Se ordena aquí y no con `orderBy`: uno recién subido aún no tiene la hora del servidor
                // y así aparece primero en lugar de desaparecer.
                else -> AppResult.Success(
                    snapshot.documents
                        .sortedByDescending { it.createdAtOrNow() }
                        .mapNotNull { it.toMedicalRecord(petId) },
                )
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override fun newRecordId(uid: String, petId: String): String = recordsCollection(uid, petId).document().id

    override suspend fun createRecord(
        uid: String,
        petId: String,
        recordId: String,
        draft: RecordDraft,
        file: LocalFileInfo,
    ): AppResult<Unit, DataError> = runData {
        val data = mapOf(
            CATEGORY to draft.category.name.lowercase(Locale.ROOT),
            FILE_NAME to file.name,
            CONTENT_TYPE to file.contentType,
            SIZE_BYTES to file.sizeBytes,
            DATE to draft.date.toString(),
            TYPE to draft.type.name.lowercase(Locale.ROOT),
            VET to draft.vet,
            CREATED_AT to FieldValue.serverTimestamp(),
        )
        recordsCollection(uid, petId).document(recordId).set(data).await()
    }

    override suspend fun deleteRecord(uid: String, petId: String, recordId: String): AppResult<Unit, DataError> = runData {
        recordsCollection(uid, petId).document(recordId).delete().await()
    }

    private fun DocumentSnapshot.createdAtOrNow(): Timestamp =
        getTimestamp(CREATED_AT, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: Timestamp.now()

    private inline fun runData(block: () -> Unit): AppResult<Unit, DataError> =
        try {
            block()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }

    private companion object {
        const val USERS = "users"
        const val PETS = "pets"
        const val RECORDS = "records"
        const val CATEGORY = "category"
        const val FILE_NAME = "fileName"
        const val CONTENT_TYPE = "contentType"
        const val SIZE_BYTES = "sizeBytes"
        const val DATE = "date"
        const val TYPE = "type"
        const val VET = "vet"
        const val CREATED_AT = "createdAt"
    }
}
