package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.mapper.toUserProfile
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.User
import com.example.huellitasya.core.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

internal class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    private fun userDocument(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    override suspend fun createIfMissing(user: User): AppResult<Unit, DataError> = runData {
        val document = userDocument(user.uid)
        if (!document.get().await().exists()) {
            document.set(user.toFirestoreMap()).await()
        }
    }

    override fun observeProfile(uid: String): Flow<AppResult<UserProfile, DataError>> = callbackFlow {
        val registration = userDocument(uid).addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())
                snapshot == null || !snapshot.exists() -> AppResult.Failure(DataError.NotFound)
                else -> AppResult.Success(snapshot.toUserProfile())
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateProfile(uid: String, changes: ProfileChanges): AppResult<Unit, DataError> = runData {
        val updates = buildMap<String, Any> {
            changes.name?.let { put(NAME, it) }
            changes.phone?.let { put(PHONE, it.toFirestoreValue()) }
            changes.address?.let { put(ADDRESS, it.toFirestoreValue()) }
            // Fecha sin hora ni zona horaria: `yyyy-MM-dd`.
            changes.birthDate?.let { put(BIRTH_DATE, it.toFirestoreValue { date -> date.toString() }) }
            put(UPDATED_AT, FieldValue.serverTimestamp())
        }
        userDocument(uid).update(updates).await()
    }

    override suspend fun updatePhotoUrl(uid: String, url: String): AppResult<Unit, DataError> = runData {
        userDocument(uid).update(mapOf(PHOTO_URL to url, UPDATED_AT to FieldValue.serverTimestamp())).await()
    }

    override suspend fun syncEmail(uid: String, email: String): AppResult<Unit, DataError> = runData {
        val document = userDocument(uid)
        val snapshot = document.get().await()
        if (snapshot.exists() && snapshot.getString(EMAIL) != email) {
            document.update(mapOf(EMAIL to email, UPDATED_AT to FieldValue.serverTimestamp())).await()
        }
    }

    /** Esquema `users/{uid}` definido en las specs 002 y 004. */
    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        NAME to name,
        EMAIL to email,
        PHOTO_URL to photoUrl,
        "provider" to provider.name.lowercase(Locale.ROOT),
        "createdAt" to FieldValue.serverTimestamp(),
    )

    /** `Clear` borra el campo del documento en lugar de guardarlo vacío. */
    private fun <T> Field<T>.toFirestoreValue(transform: (T) -> Any = { it as Any }): Any = when (this) {
        is Field.Set -> transform(value)
        Field.Clear -> FieldValue.delete()
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
        const val USERS_COLLECTION = "users"
        const val NAME = "name"
        const val EMAIL = "email"
        const val PHONE = "phone"
        const val ADDRESS = "address"
        const val BIRTH_DATE = "birthDate"
        const val PHOTO_URL = "photoUrl"
        const val UPDATED_AT = "updatedAt"
    }
}
