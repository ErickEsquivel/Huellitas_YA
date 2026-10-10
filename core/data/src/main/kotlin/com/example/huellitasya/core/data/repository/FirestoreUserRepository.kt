package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

internal class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override suspend fun createIfMissing(user: User): AppResult<Unit, DataError> =
        try {
            val document = firestore.collection(USERS_COLLECTION).document(user.uid)
            if (!document.get().await().exists()) {
                document.set(user.toFirestoreMap()).await()
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Failure(e.toDataError())
        }

    /** Esquema `users/{uid}` definido en la spec 002. */
    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "photoUrl" to photoUrl,
        "provider" to provider.name.lowercase(Locale.ROOT),
        "createdAt" to FieldValue.serverTimestamp(),
    )

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
