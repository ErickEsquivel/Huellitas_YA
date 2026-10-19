package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.mapper.toDataError
import com.example.huellitasya.core.data.mapper.toPet
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.PetRepository
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetDraft
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

/** `users/{uid}/pets/{petId}` (spec 005). */
internal class FirestorePetRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PetRepository {

    private fun petsCollection(uid: String) =
        firestore.collection(USERS_COLLECTION).document(uid).collection(PETS_COLLECTION)

    override fun observePets(uid: String): Flow<AppResult<List<Pet>, DataError>> = callbackFlow {
        val registration = petsCollection(uid).addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())
                snapshot == null -> AppResult.Success(emptyList())
                // Se ordena aquí y no con `orderBy`: una mascota recién creada aún no tiene la hora
                // del servidor, y así aparece al final de la lista en lugar de desaparecer.
                else -> AppResult.Success(
                    snapshot.documents
                        .sortedBy { it.createdAtOrNow() }
                        .mapNotNull { it.toPet() },
                )
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override fun observePet(uid: String, petId: String): Flow<AppResult<Pet, DataError>> = callbackFlow {
        val registration = petsCollection(uid).document(petId).addSnapshotListener { snapshot, error ->
            val pet = snapshot?.takeIf { it.exists() }?.toPet()
            val result = when {
                error != null -> AppResult.Failure(error.toDataError())
                pet == null -> AppResult.Failure(DataError.NotFound)
                else -> AppResult.Success(pet)
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    override fun newPetId(uid: String): String = petsCollection(uid).document().id

    override suspend fun createPet(
        uid: String,
        petId: String,
        draft: PetDraft,
        photoUrl: String?,
    ): AppResult<Unit, DataError> = runData {
        val data = draft.toFirestoreMap() + mapOf(
            PHOTO_URL to photoUrl,
            CREATED_AT to FieldValue.serverTimestamp(),
            UPDATED_AT to FieldValue.serverTimestamp(),
        )
        petsCollection(uid).document(petId).set(data).await()
    }

    /** Esquema de la spec 005: enums en minúscula y la fecha como `yyyy-MM-dd`. */
    private fun PetDraft.toFirestoreMap(): Map<String, Any?> = mapOf(
        NAME to name,
        SPECIES to species.name.lowercase(Locale.ROOT),
        BREED to breed,
        BIRTH_DATE to birthDate.toString(),
        SEX to sex.name.lowercase(Locale.ROOT),
        WEIGHT_KG to weightKg,
    )

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
        const val USERS_COLLECTION = "users"
        const val PETS_COLLECTION = "pets"
        const val NAME = "name"
        const val SPECIES = "species"
        const val BREED = "breed"
        const val BIRTH_DATE = "birthDate"
        const val SEX = "sex"
        const val WEIGHT_KG = "weightKg"
        const val PHOTO_URL = "photoUrl"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }
}
