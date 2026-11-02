package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.subtypeValue
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.mapper.toReminder
import com.example.huellitasya.core.data.mapper.toTimestamp
import com.example.huellitasya.core.data.mapper.wireValue
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.ReminderRepository
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderDraft
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import javax.inject.Inject

/** `users/{uid}/reminders/{reminderId}` (spec 007). */
internal class FirestoreReminderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ReminderRepository {

    private fun remindersCollection(uid: String) =
        firestore.collection(USERS).document(uid).collection(REMINDERS)

    override fun observeReminders(uid: String): Flow<AppResult<List<Reminder>, DataError>> = callbackFlow {
        val registration = remindersCollection(uid).addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())
                snapshot == null -> AppResult.Success(emptyList())
                else -> AppResult.Success(snapshot.documents.mapNotNull { it.toReminder() }.sortedBy { it.dateTime })
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override fun newReminderId(uid: String): String = remindersCollection(uid).document().id

    override suspend fun createReminder(uid: String, reminderId: String, draft: ReminderDraft): AppResult<Unit, DataError> =
        runData {
            val data = buildMap {
                put(PET_ID, draft.petId)
                put(KIND, draft.kind.wireValue())
                subtypeValue(draft.vaccineType, draft.vetReason)?.let { put(SUBTYPE, it) }
                draft.customTitle?.let { put(TITLE, it) }
                put(DATE_TIME, draft.dateTime.toTimestamp())
                put(CREATED_AT, FieldValue.serverTimestamp())
                put(UPDATED_AT, FieldValue.serverTimestamp())
            }
            remindersCollection(uid).document(reminderId).set(data).await()
        }

    override suspend fun updateDateTime(uid: String, reminderId: String, dateTime: LocalDateTime): AppResult<Unit, DataError> =
        runData {
            remindersCollection(uid).document(reminderId)
                .update(mapOf(DATE_TIME to dateTime.toTimestamp(), UPDATED_AT to FieldValue.serverTimestamp()))
                .await()
        }

    override suspend fun deleteReminder(uid: String, reminderId: String): AppResult<Unit, DataError> = runData {
        remindersCollection(uid).document(reminderId).delete().await()
    }

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
        const val REMINDERS = "reminders"
        const val PET_ID = "petId"
        const val KIND = "kind"
        const val SUBTYPE = "subtype"
        const val TITLE = "title"
        const val DATE_TIME = "dateTime"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}
